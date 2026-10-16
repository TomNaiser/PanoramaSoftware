param(
    [ValidateSet('Read','Apply','Reset')][string]$Action='Read',
    [string]$Port='COM4',
    [string]$ConfigPath=(Join-Path $PSScriptRoot '..\config\MotionConfig.device.json'),
    [switch]$Overwrite,
    [switch]$PersistSnapshot,
    [string]$LogPath=''
)
# Full snapshot transactions only. No motion, shutter, CLEAR_RUN or ESTOP.
# Firmware requires disarmed motors, no job and fresh stationary feedback.
$ErrorActionPreference='Stop'
if($PersistSnapshot -and $Action -ne 'Apply'){throw 'PersistSnapshot nur mit Apply verwenden.'}
$path=[IO.Path]::GetFullPath($ConfigPath)
if($Action -eq 'Read' -and (Test-Path -LiteralPath $path) -and !$Overwrite) {
    throw 'Datei existiert; anderen ConfigPath verwenden oder bewusst -Overwrite angeben.'
}
$inputConfig=$null
if($Action -eq 'Apply') {$inputConfig=Get-Content -LiteralPath $path -Raw | ConvertFrom-Json}
$serial=[IO.Ports.SerialPort]::new($Port,57600,[IO.Ports.Parity]::None,8,[IO.Ports.StopBits]::One)
$serial.WriteTimeout=1000; $serial.DtrEnable=$false; $serial.RtsEnable=$false
$state=@{sequence=2000;buffer='';reply=@{};values=@{};info=$null;status=$null;firmware=$null;boot=$null;heartbeat=0;nacks=0;badCrc=0}
$timer=[Diagnostics.Stopwatch]::new(); $writer=$null; $token=0; $staging=$false
function Crc([string]$text) {
    [int]$crc=65535
    foreach($byte in [Text.Encoding]::ASCII.GetBytes($text)) {
        $crc=$crc -bxor ([int]$byte -shl 8)
        for($i=0;$i -lt 8;$i++) {$crc=if($crc -band 32768){(($crc -shl 1) -bxor 4129) -band 65535}else{($crc -shl 1) -band 65535}}
    }; return $crc
}
function Record([string]$direction,[string]$type,$fields) {
    if($writer) {$writer.WriteLine((@{elapsedMs=$timer.ElapsedMilliseconds;direction=$direction;type=$type;fields=@($fields)} | ConvertTo-Json -Compress));$writer.Flush()}
}
function Send([string]$type,[string[]]$fields=@()) {
    $state.sequence=$state.sequence%65535+1
    $body='@P2|'+$state.sequence+'|'+$type
    if($fields.Count){$body+='|'+($fields -join '|')}
    $bytes=$body+'|'+(Crc $body).ToString('X4')+"`r`n"
    $serial.Write($bytes); Record 'TX' $type $fields
    return @{sequence=$state.sequence;bytes=$bytes}
}
function Receive {
    $state.buffer+=$serial.ReadExisting()
    while($state.buffer.Contains("`n")) {
        $end=$state.buffer.IndexOf("`n");$line=$state.buffer.Substring(0,$end).Trim();$state.buffer=$state.buffer.Substring($end+1)
        $start=$line.LastIndexOf('@P2|',[StringComparison]::Ordinal)
        if($start -gt 0){Record 'RX' 'FRAME_RESYNC' @($line);$line=$line.Substring($start)}
        $separator=$line.LastIndexOf('|')
        if(!$line.StartsWith('@P2|') -or $separator -lt 0){continue}
        $body=$line.Substring(0,$separator)
        if($line.Substring($separator+1) -cne (Crc $body).ToString('X4')){$state.badCrc++;Record 'RX' 'CRC_BAD' @($line);continue}
        $parts=$body.Split('|');$type=$parts[2];$fields=@($parts | Select-Object -Skip 3)
        Record 'RX' $type $fields
        switch($type) {
            'ACK' {$state.reply[$fields[0]]=@($type,$fields[1])}
            'NACK' {$state.nacks++;$state.reply[$fields[0]]=@($type,$fields[1])}
            'MOTION_CONFIG' {
                if($fields.Count -ne 11){throw 'Unvollstaendige Konfigurationsantwort.'}
                $state.values[[int]$fields[3]]=$fields
            }
            'MOTION_CONFIG_INFO' {if($fields.Count -ne 5){throw 'Unvollstaendige Konfigurationsidentitaet.'};$state.info=$fields}
            'STATUS' {$state.status=$fields}
            'DEVICE_INFO' {$state.firmware=$fields}
            'DEVICE_SESSION' {
                if($state.boot -and $state.boot -ne $fields[0]){throw 'Geraet waehrend Konfiguration neu gestartet; keine Fortsetzung.'}
                $state.boot=$fields[0]
            }
        }
    }
    if($state.buffer.Length -gt 4096){throw 'Empfangspuffer ohne vollstaendigen Rahmen.'}
    if($timer.ElapsedMilliseconds-$state.heartbeat -ge 750) {
        $run=if($state.status){$state.status[0]}else{'0'}
        $null=Send 'HEARTBEAT' @($run);$state.heartbeat=$timer.ElapsedMilliseconds
    }
}
function Request([string]$type,[string[]]$fields=@(),[int]$timeoutMs=4000) {
    $request=Send $type $fields;$until=$timer.ElapsedMilliseconds+$timeoutMs
    $retryAt=$timer.ElapsedMilliseconds+1000;$attempts=0
    while(!$state.reply.ContainsKey("$($request.sequence)") -and $timer.ElapsedMilliseconds -lt $until) {
        Receive; Start-Sleep -Milliseconds 10
        if($timer.ElapsedMilliseconds -ge $retryAt -and $attempts -lt 2) {
            # Identical config transaction/sequence, never a new motion job.
            $serial.Write($request.bytes); Record 'TX' 'CONFIG_REQUEST_RETRY' @($type,$request.sequence)
            $retryAt=$timer.ElapsedMilliseconds+1000;$attempts++
        }
    }
    if(!$state.reply.ContainsKey("$($request.sequence)")){throw "Keine Bestaetigung fuer $type. Konfigurationsstatus zuruecklesen; nicht blind neu anwenden."}
    $reply=$state.reply["$($request.sequence)"]
    if($reply[0] -ne 'ACK'){throw "$type abgelehnt: $($reply[1])"}
    return $reply[1]
}
function Snapshot {
    if(!$state.info -or [int]$state.info[0] -ne 1){throw 'Geraet unterstuetzt Konfigurationsschema 1 nicht.'}
    [uint32]$revision=$state.info[1];[int]$count=$state.info[2]
    if($count -lt 1 -or $count -gt 128){throw 'Unerwarteter Parameterumfang.'}
    $parameters=[ordered]@{}; $state.values.Clear()
    for($i=0;$i -lt $count;$i++) {
        for($readAttempt=0;$readAttempt -lt 2 -and !$state.values.ContainsKey($i);$readAttempt++) {
            $null=Request 'GET_MOTION_CONFIG' @("$i")
            $until=$timer.ElapsedMilliseconds+2000
            while(!$state.values.ContainsKey($i) -and $timer.ElapsedMilliseconds -lt $until){Receive;Start-Sleep -Milliseconds 10}
        }
        if(!$state.values.ContainsKey($i)){throw "Parameter $i fehlt."}
        $v=$state.values[$i]
        if([int]$v[0] -ne 1 -or [uint32]$v[1] -ne $revision -or [int]$v[2] -ne $count -or [int]$v[6] -le 0 -or $parameters.Contains($v[4])) {
            throw 'Inkonsistente Konfigurationsantwort; keine Uebernahme.'
        }
        [decimal]$scale=$v[6]
        $parameters[$v[4]]=[ordered]@{value=([decimal]$v[5]/$scale);unit=$v[7];minimum=([decimal]$v[8]/$scale);maximum=([decimal]$v[9]/$scale);default=([decimal]$v[10]/$scale)}
    }
    return [ordered]@{schemaVersion=1;baseRevision=$revision;firmware=$state.firmware[0];source=$state.info[3];parameters=$parameters}
}
try {
    if((Crc '123456789') -ne 0x29B1){throw 'CRC-Selbsttest fehlgeschlagen.'}
    if($LogPath){$writer=[IO.StreamWriter]::new([IO.Path]::GetFullPath($LogPath),$false,[Text.UTF8Encoding]::new($false))}
    $serial.Open();$timer.Start();$null=Request 'HELLO' @('PC_MOTION_CONFIG') 8000
    $until=$timer.ElapsedMilliseconds+2000
    while((!$state.info -or !$state.status -or !$state.firmware) -and $timer.ElapsedMilliseconds -lt $until){Receive;Start-Sleep -Milliseconds 10}
    $before=Snapshot
    if($Action -eq 'Read') {
        $before | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $path -Encoding utf8
        Write-Output "Wirksame Konfiguration: Revision $($before.baseRevision), $($before.parameters.Count) Parameter, $path"
    }else{
        if(!$state.status -or $state.status[0] -ne '0' -or $state.status[1] -notin @('IDLE','STOPPED')){throw 'Kein freier, gestoppter Aufbau; Konfiguration nicht angewendet.'}
        if($Action -eq 'Apply' -and (!$inputConfig -or $inputConfig.schemaVersion -ne 1 -or !$inputConfig.parameters)){throw 'Vollstaendige Konfigurationsdatei mit schemaVersion 1 erforderlich.'}
        if($inputConfig -and $null -ne $inputConfig.baseRevision -and [uint32]$inputConfig.baseRevision -ne $before.baseRevision){throw 'Datei basiert auf veralteter Revision; aktuelle Werte zuerst lesen.'}
        if($inputConfig -and @($inputConfig.parameters.PSObject.Properties).Count -ne $before.parameters.Count){throw 'Unvollstaendige oder fremde Parameterliste.'}
        $wanted=[Collections.Generic.List[int]]::new();$changes=@();$i=0
        foreach($name in $before.parameters.Keys) {
            $p=$before.parameters[$name];$v=$state.values[$i];[decimal]$scale=$v[6]
            if($Action -eq 'Reset') {$value=[decimal]$p.default}
            else{
                $property=$inputConfig.parameters.PSObject.Properties[$name]
                if(!$property -or $property.Value.unit -cne $p.unit){throw "Parameter oder Einheit fehlt: $name"}
                $raw=$property.Value.value
                if($raw -is [string] -or $raw -is [bool] -or $null -eq $raw){throw "Numerischer Wert erforderlich: $name"}
                [decimal]$value=$raw
            }
            $scaled=$value*$scale
            if($value -lt $p.minimum -or $value -gt $p.maximum -or $scaled -ne [decimal]::Truncate($scaled)){throw "Wert ausserhalb Bereich oder Aufloesung: $name"}
            $wanted.Add([int]$scaled)
            if($value -ne $p.value){$changes+= "$name : $($p.value) -> $value $($p.unit)"}
            ++$i
        }
        if(!$changes.Count -and $Action -ne 'Reset' -and !$PersistSnapshot){Write-Output 'Keine Aenderung; nichts gespeichert.'}
        else{
            $changes | Write-Output
            $token=[uint32](Get-Random -Minimum 1 -Maximum 2147483647)
            $null=Request 'BEGIN_MOTION_CONFIG' @("$token",'1',"$($before.baseRevision)");$staging=$true
            for($i=0;$i -lt $wanted.Count;$i++){$null=Request 'SET_MOTION_PARAMETER' @("$token","$i","$($wanted[$i])")}
            $state.info=$null
            $null=Request 'APPLY_MOTION_CONFIG' @("$token","$($before.baseRevision)") 15000
            $staging=$false
            $until=$timer.ElapsedMilliseconds+2000
            while(!$state.info -and $timer.ElapsedMilliseconds -lt $until){Receive;Start-Sleep -Milliseconds 10}
            if(!$state.info -or [uint32]$state.info[1] -ne $before.baseRevision+1){throw 'Uebernahmequittung ohne passende neue Revision; Geraet erneut lesen.'}
            $after=Snapshot;$i=0
            foreach($name in $after.parameters.Keys){if([decimal]$after.parameters[$name].value*[decimal]$state.values[$i][6] -ne $wanted[$i]){throw "Ruecklesen stimmt nicht ueberein: $name"};++$i}
            Write-Output "Gespeichert und vollstaendig zurueckgelesen: Revision $($after.baseRevision). Keine Bewegung ausgeloest."
        }
    }
    if($state.nacks -or $state.badCrc){Write-Warning "Transportbefunde: NACK=$($state.nacks), CRC=$($state.badCrc). Log pruefen."}
}finally{
    if($staging -and $serial.IsOpen){try{$null=Request 'CANCEL_MOTION_CONFIG' @("$token")}catch{Write-Warning 'Staging nicht bestaetigt verworfen; bleibt bis Ablauf unwirksam. Geraet zuerst lesen.'}}
    if($serial.IsOpen){$serial.Close()};$serial.Dispose();if($writer){$writer.Dispose()}
}

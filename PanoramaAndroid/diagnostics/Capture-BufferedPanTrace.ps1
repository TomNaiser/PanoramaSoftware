param([string]$Port='COM3',[Parameter(Mandatory=$true)][string]$LogPath)
# Read diagnostic RAM only, after the owning move has confirmed ESTOP and
# closed Bluetooth. DUMP_PAN is ignored by firmware while motors are enabled.
$ErrorActionPreference='Stop'
$path=[IO.Path]::GetFullPath($LogPath)
foreach($file in @($path,[IO.Path]::ChangeExtension($path,'.log'),[IO.Path]::ChangeExtension($path,'.summary.json'))) {
    if(Test-Path -LiteralPath $file){throw 'Vorhandene Messung wird nicht ueberschrieben.'}
}
$serial=[IO.Ports.SerialPort]::new($Port,115200,[IO.Ports.Parity]::None,8,[IO.Ports.StopBits]::One)
$serial.DtrEnable=$true;$serial.RtsEnable=$false;$serial.WriteTimeout=1000
$raw=[IO.StreamWriter]::new([IO.Path]::ChangeExtension($path,'.log'),$false,[Text.UTF8Encoding]::new($false))
$rows=[Collections.Generic.List[object]]::new();$bytes=[Collections.Generic.List[byte]]::new()
$header=$null;$footer=$null;$buffer='';$sampleMs=0;$maxGap=0
try {
    $serial.Open();Start-Sleep -Milliseconds 100;$serial.Write("DUMP_PAN`n")
    $timer=[Diagnostics.Stopwatch]::StartNew();$retried=$false
    while(!$footer -and $timer.ElapsedMilliseconds -lt 12000) {
        $buffer+=$serial.ReadExisting()
        while($buffer.Contains("`n")) {
            $end=$buffer.IndexOf("`n");$line=$buffer.Substring(0,$end).Trim();$buffer=$buffer.Substring($end+1)
            if(!$line){continue};$raw.WriteLine($line);$f=$line.Split(',')
            if($f[0] -eq '[PANBUF]') {
                if($header -or $f.Count -ne 10 -or $f[1] -ne '1'){throw 'Unerwarteter Messkopf.'}
                $header=$f
            } elseif($f[0] -eq '[PANSAMPLE]') {
                if(!$header -or $f.Count -ne 5 -or [int]$f[1] -ne $rows.Count){throw 'Messfolge unvollstaendig.'}
                [uint16]$dt=$f[2];[int32]$position=$f[3];[int16]$pwm=$f[4]
                $sampleMs+=$dt;$maxGap=[math]::Max($maxGap,$dt)
                $rows.Add([pscustomobject]@{sampleMs=$sampleMs;panMdeg=$position;panAppliedPwm=$pwm;deltaMs=$dt})
                $bytes.AddRange([BitConverter]::GetBytes($position));$bytes.AddRange([BitConverter]::GetBytes($pwm));$bytes.AddRange([BitConverter]::GetBytes($dt))
            } elseif($f[0] -eq '[PANEND]') {$footer=$f}
        }
        if(!$header -and !$retried -and $timer.ElapsedMilliseconds -gt 2000){$serial.Write("DUMP_PAN`n");$retried=$true}
        Start-Sleep -Milliseconds 5
    }
    if(!$header -or !$footer -or $footer.Count -ne 3 -or $rows.Count -ne [int]$header[2] -or $rows.Count -ne [int]$footer[1] -or $rows.Count -lt 3){throw 'Keine vollstaendige eingefrorene Messung empfangen.'}
    [int]$crc=65535
    foreach($byte in $bytes) {
        $crc=$crc -bxor ([int]$byte -shl 8)
        for($i=0;$i -lt 8;$i++){$crc=if($crc -band 32768){(($crc -shl 1) -bxor 4129) -band 65535}else{($crc -shl 1) -band 65535}}
    }
    if($crc.ToString('X4') -cne $header[9] -or $header[9] -cne $footer[2]){throw 'Messdaten-CRC stimmt nicht.'}
    $rows | Export-Csv -LiteralPath $path -NoTypeInformation -Encoding utf8
    $summary=[ordered]@{records=$rows.Count;durationMs=$sampleMs;maximumSampleGapMs=$maxGap;overwritten=[int]$header[3];badTiming=[int]$header[4];invalidSensorSamples=[int]$header[5];targetPanMdeg=[int]$header[6];tiltMdeg=[int]$header[7];configRevision=[int]$header[8];crc16=$header[9];integrityVerified=$true;firstPositionMdeg=$rows[0].panMdeg;lastPositionMdeg=$rows[$rows.Count-1].panMdeg}
    $summary | ConvertTo-Json | Set-Content -LiteralPath ([IO.Path]::ChangeExtension($path,'.summary.json')) -Encoding utf8
    if($summary.overwritten -or $summary.badTiming -or $summary.invalidSensorSamples -or $maxGap -gt 30){throw 'Messqualitaet unzureichend; Rohdaten erhalten, keine Modellfreigabe.'}
    $summary | ConvertTo-Json
} finally {
    $raw.Dispose();if($serial.IsOpen){$serial.Close()};$serial.Dispose()
}

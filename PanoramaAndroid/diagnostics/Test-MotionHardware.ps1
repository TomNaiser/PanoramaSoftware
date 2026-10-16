param(
    [string]$Port = 'COM4',
    [ValidateSet('Monitor','Move','Jog','Stop','ClearTestStop','Pulse','Cruise','CaptureSteps')][string]$Action = 'Monitor',
    [ValidateSet(-1,1)][int]$JogDirection = -1,
    [ValidateRange(500,15000)][int]$JogHoldMs = 2000,
    [ValidateRange(0,10)][double]$JogSpeedDps = 0,
    [switch]$AllowEmptyAndroidStop,
    [switch]$JogLeaseExpiry,
    [ValidateSet('Pan','Tilt')][string]$PulseAxis = 'Tilt',
    [ValidateRange(-40,40)][int]$SignedPwm = 0,
    [ValidateRange(100,3000)][int]$PulseMs = 1000,
    [ValidateRange(500,5000)][int]$PulseTravelMdeg = 2000,
    [string]$AbortFile = '',
    [int]$PanDeltaMdeg = 0,
    [int]$TiltDeltaMdeg = 0,
    [Nullable[int]]$PanTargetMdeg = $null,
    [Nullable[int]]$TiltTargetMdeg = $null,
    [ValidateRange(1,1000)][int]$Tempo = 200,
    [ValidateRange(1,1000)][Nullable[int]]$PanTempo = $null,
    [ValidateRange(1,1000)][Nullable[int]]$TiltTempo = $null,
    [ValidateRange(2,180)][int]$DurationSeconds = 5,
    [ValidatePattern('^2\.4\.\d+$')][string]$CaptureExpectedFirmware='2.4.40',
    [ValidateRange(1,3)][int]$CaptureColumns=1,
    [ValidateRange(2,8)][int]$CaptureRows=3,
    [ValidateRange(0,5000)][int]$CapturePanStepMdeg=0,
    [ValidateRange(-4000,-500)][int]$CaptureTiltStepMdeg=-1000,
    [ValidateRange(-5000,5000)][int]$CapturePanStartDeltaMdeg=0,
    [ValidateRange(-5000,5000)][int]$CaptureTiltStartDeltaMdeg=0,
    [ValidateRange(1,1000)][int]$CapturePanTempo=500,
    [ValidateRange(1,1000)][int]$CaptureTiltTempo=500,
    [ValidateRange(0,5000)][int]$CaptureSettleMs=1000,
    [ValidateRange(10,2000)][int]$CaptureTriggerMs=100,
    [ValidateRange(0,10)][int]$StartDelaySeconds = 0,
    [ValidateRange(1,46)][int]$MaximumStepDegrees = 2,
    [switch]$WaitForArrival,
    [switch]$IncrementalStep,
    [ValidateRange(0,3000)][int]$DwellMs = 1000,
    [ValidateRange(20,1000)][int]$DiagnosticIntervalMs = 150,
    [string]$LogPath = (Join-Path $PSScriptRoot 'motion-hardware.jsonl')
)

# Real device diagnostics. Monitor never sends movement/camera commands.
# Move defaults to <=2 degrees; an explicit column test can authorize a
# nominal 45-degree column return plus tolerance inside stored limits.
# Every Move/Pulse/Cruise ends in ESTOP.
# Announce physical tests to the operator before invoking movement.
$ErrorActionPreference = 'Stop'
if($Action -ne 'CaptureSteps' -and $DurationSeconds -gt 30){throw 'Nur ein begrenzter Aufnahmelauf darf mehr als 30 Sekunden dauern.'}
$captureExpectedImages=$CaptureColumns*$CaptureRows
. (Join-Path $PSScriptRoot 'P2-ReceiveLine.ps1')
$serial = [System.IO.Ports.SerialPort]::new($Port,57600,[System.IO.Ports.Parity]::None,8,[System.IO.Ports.StopBits]::One)
$serial.ReadTimeout = 500; $serial.WriteTimeout = 1000
$serial.NewLine = "`n"; $serial.DtrEnable = ($Port -eq 'COM3'); $serial.RtsEnable = $false
$state = @{ sequence=1000; buffer=''; status=$null; capabilities=$null; firmware=$null; motion=$null; diagnostics=$null; responses=@{}; sent=@{}; heartbeat=0; latestStatus=0; faults=@(); frames=0; badCrc=0; benchCapabilities=$null; bench=$null; samples=@{} }
$state.stopping = $false
$state.sequence = Get-Random -Minimum 1000 -Maximum 50000
$state.manualStatus = $null
$state.manualStatusSupported = $false
$moveSequence = $null
$jogToken=0; $jogStopped=$false; $jogStartedMs=0; $nextJogKeep=0; $jogOrigin=0
$state.benchRevision = 0
$timer = [Diagnostics.Stopwatch]::new()
$writer = $null; $moveAttempted = $false; $captureTestLoaded=$false; $captureImages=@()

function Get-Crc([string]$body) {
    [int]$crc = 65535
    foreach ($byte in [Text.Encoding]::ASCII.GetBytes($body)) {
        $crc = $crc -bxor ([int]$byte -shl 8) # Promote byte before shifting; PowerShell otherwise truncates.
        for ($bit=0; $bit -lt 8; $bit++) {
            $crc = if ($crc -band 32768) { (($crc -shl 1) -bxor 4129) -band 65535 } else { ($crc -shl 1) -band 65535 }
        }
    }
    return $crc
}
function Write-Record([string]$direction,[string]$kind,$fields) {
    $record = @{ elapsedMs=$timer.ElapsedMilliseconds; time=[DateTimeOffset]::Now.ToString('o'); direction=$direction; type=$kind; fields=@($fields) }
    $writer.WriteLine(($record | ConvertTo-Json -Compress -Depth 4)); $writer.Flush()
}
function Send-Frame([string]$kind,[string[]]$fields=@()) {
    $state.sequence = $state.sequence % 65535 + 1
    $body = '@P2|' + $state.sequence + '|' + $kind
    if ($fields.Count) { $body += '|' + ($fields -join '|') }
    $wireFrame = $body + '|' + (Get-Crc $body).ToString('X4') + "`r`n"
    $state.sent["$($state.sequence)"] = @{ bytes=$wireFrame; kind=$kind }
    $serial.Write($wireFrame)
    Write-Record 'TX' 'WIRE' @($wireFrame.Trim())
    Write-Record 'TX' $kind $fields
    return $state.sequence
}
function Read-Frames {
    if($AbortFile -and !$state.stopping -and (Test-Path -LiteralPath $AbortFile)) { throw 'Messung durch Abbruchdatei gestoppt.' }
    $state.buffer += $serial.ReadExisting()
    while ($state.buffer.Contains("`n")) {
        $end = $state.buffer.IndexOf("`n")
        $line = $state.buffer.Substring(0,$end).Trim(); $state.buffer = $state.buffer.Substring($end+1)
        $candidate=Select-P2FrameSuffix $line
        if($candidate -cne $line){Write-Record 'RX' 'FRAME_RESYNC' @($line);$line=$candidate}
        $separator = $line.LastIndexOf('|')
        if (!$line.StartsWith('@P2|') -or $separator -lt 0) { if ($line) { Write-Record 'RX' 'RAW' @($line) }; continue }
        $body = $line.Substring(0,$separator)
        if ($line.Substring($separator+1) -cne (Get-Crc $body).ToString('X4')) { $state.badCrc++; Write-Record 'RX' 'CRC_BAD' @($line); continue }
        $parts = $body.Split('|'); $kind = $parts[2]
        $fields = if ($parts.Length -gt 3) { @($parts[3..($parts.Length-1)]) } else { @() }
        $state.frames++; Write-Record 'RX' $kind $fields
        switch ($kind) {
            'ACK' { $state.responses[$fields[0]] = @('ACK',$fields[1]) }
            'NACK' { $state.responses[$fields[0]] = @('NACK',$fields[1]) }
            'DEVICE_INFO' { $state.firmware = $fields }
            'CAPABILITIES' { $state.capabilities = $fields }
            'STATUS' {
                $state.status = $fields; $state.latestStatus = $timer.ElapsedMilliseconds
                if ($fields[1] -eq 'FAULT') { $state.faults += $fields[13] }
            }
            'IMAGE_COMPLETED' { $script:captureImages+=,@($fields) }
            'DIAGNOSTICS' { $state.diagnostics = $fields }
            'MOTION_DIAGNOSTICS' { $state.motion = $fields }
            'MANUAL_STATUS_CAPABILITIES' { $state.manualStatusSupported = $fields.Count -eq 1 -and $fields[0] -eq '1' }
            'MANUAL_STATUS' { $state.manualStatus = $fields }
            'BENCH_CAPABILITIES' { $state.benchCapabilities = $fields }
            'BENCH_STATUS' { $state.bench = $fields; $state.benchRevision++ }
            'BENCH_SAMPLE' {
                if($fields.Count -ne 6) { throw 'Ungueltige Messprobe.' }
                $state.samples[[int]$fields[0]] = $fields
            }
        }
    }
    if ($state.buffer.Length -gt 4096) { throw 'Unvollstaendiger Empfangspuffer zu gross.' }
    if ($timer.ElapsedMilliseconds - $state.heartbeat -ge $(if($Action -in @('Pulse','Cruise')){500}else{1000})) {
        $runId = if ($state.status) { $state.status[0] } else { '0' }
        $null = Send-Frame 'HEARTBEAT' @($runId); $state.heartbeat = $timer.ElapsedMilliseconds
    }
}
function Wait-Response([int]$sequence) {
    $hello = $state.sent["$sequence"].kind -eq 'HELLO'
    $diagnostic = $state.sent["$sequence"].kind -eq 'GET_DIAGNOSTICS'
    $stop = $state.sent["$sequence"].kind -eq 'ESTOP'
    $deadline = $timer.ElapsedMilliseconds + $(if ($hello) { 8000 } else { 3000 })
    $nextRetry = $timer.ElapsedMilliseconds + $(if($diagnostic -or $stop){500}else{1500}); $retries = 0
    while (!$state.responses.ContainsKey("$sequence") -and $timer.ElapsedMilliseconds -lt $deadline) {
        Read-Frames; Start-Sleep -Milliseconds 10
        if ((($hello -and $retries -lt 3) -or (($diagnostic -or $stop) -and $retries -lt 2)) -and $timer.ElapsedMilliseconds -ge $nextRetry) {
            # Read-only queries and the idempotent emergency stop can be
            # repeated with identical sequence/payload. Never retry motion.
            $serial.Write($state.sent["$sequence"].bytes)
            Write-Record 'TX' $(if($stop){'ESTOP_RETRY'}elseif($diagnostic){'DIAGNOSTICS_RETRY'}else{'HELLO_RETRY'}) @("$sequence")
            $nextRetry = $timer.ElapsedMilliseconds + $(if($diagnostic -or $stop){500}else{1500}); $retries++
        }
    }
    if (!$state.responses.ContainsKey("$sequence")) { throw "Keine Bestaetigung fuer Sequenz $sequence." }
    $reply = $state.responses["$sequence"]
    if ($reply[0] -ne 'ACK') { throw "Befehl abgelehnt: $($reply[1])." }
    return $reply[1]
}
function Read-BenchStatus {
    $revision=$state.benchRevision
    $null=Wait-Response (Send-Frame 'GET_BENCH_STATUS')
    $until=$timer.ElapsedMilliseconds+1000
    while($state.benchRevision -eq $revision -and $timer.ElapsedMilliseconds -lt $until) { Read-Frames; Start-Sleep -Milliseconds 10 }
    if($state.benchRevision -eq $revision) { throw 'Aktueller Messstatus fehlt.' }
}
try {
    if ((Get-Crc '123456789') -ne 0x29B1) { throw 'CRC-16-CCITT-FALSE-Selbsttest fehlgeschlagen; kein Geraetezugriff.' }
    $logDirectory = Split-Path -Parent ([IO.Path]::GetFullPath($LogPath))
    $null = New-Item -ItemType Directory -Path $logDirectory -Force
    $writer = [IO.StreamWriter]::new([IO.Path]::GetFullPath($LogPath),$false,[Text.UTF8Encoding]::new($false))
    $serial.Open(); $timer.Start()
    $null = Wait-Response (Send-Frame 'HELLO' @('PC_MOTION_TEST'))
    $deadline = $timer.ElapsedMilliseconds + 3000
    while ((!$state.status -or !$state.capabilities -or !$state.firmware -or ($Action -in @('Pulse','Cruise') -and !$state.benchCapabilities)) -and $timer.ElapsedMilliseconds -lt $deadline) { Read-Frames; Start-Sleep -Milliseconds 10 }
    if (!$state.status -or !$state.capabilities -or !$state.firmware) { throw 'Firmware, Grenzen oder Status fehlen.' }
    if ($Action -eq 'Stop') { $null = Wait-Response (Send-Frame 'ESTOP' @($state.status[0],'PC_TEST_STOP')) }
    if ($Action -eq 'ClearTestStop') {
        if($state.status[0] -ne '0' -or $state.status[1] -ne 'STOPPED' -or
           ($state.status[13] -notin @('PC_TEST_END','PC_TEST_STOP') -and
              !($AllowEmptyAndroidStop -and $state.status[13] -eq 'ANDROID_ESTOP' -and $state.status[4] -eq '0' -and $state.status[5] -eq '0')) -or
           $timer.ElapsedMilliseconds-$state.latestStatus -gt 1000) {
            throw 'Nur einen aktuellen eigenen Testhalt ohne Auftrag zuruecksetzen.'
        }
        $null = Wait-Response (Send-Frame 'CLEAR_RUN')
        $null = Wait-Response (Send-Frame 'GET_STATUS')
        if($state.status[0] -ne '0' -or $state.status[1] -ne 'IDLE') {
            throw 'Leerer Bereitschaftszustand nach Testhalt nicht bestaetigt.'
        }
    }
    if ($Action -in @('Pulse','Cruise')) {
        if($state.firmware[0] -notin @('2.4.10','2.4.11','2.4.12','2.4.13','2.4.14','2.4.15','2.4.16','2.4.17','2.4.18','2.4.19','2.4.20','2.4.21','2.4.22','2.4.23','2.4.24','2.4.25','2.4.26','2.4.27','2.4.28','2.4.29','2.4.30') -or $state.firmware[3] -ne 'ARDUINO_NANO_R4_USB_TEST' -or
           !$state.benchCapabilities -or ($state.benchCapabilities -join '|') -ne '3|40|3000|5000|20|192|5000|500|500') {
            throw 'Nur die eindeutig bestaetigten USB-Messbuilds 2.4.10 bis 2.4.30 erlauben Messpulse.'
        }
        if($Action -eq 'Cruise' -and ([math]::Abs($SignedPwm) -gt 30 -or $PulseMs -lt 600 -or $PulseTravelMdeg -lt 1000)) { throw 'Ungueltige Messung mit Anlaufphase.' }
        if($Action -eq 'Pulse' -and [math]::Abs($SignedPwm) -gt 30 -and $PulseMs -gt 500) { throw 'Losbrech-Messpuls ueber PWM 30 darf maximal 500 ms dauern.' }
        # A previous test with no plan may be cleared; user jobs are never cleared.
        if($state.status[1] -eq 'STOPPED' -and $state.status[0] -eq '0' -and $state.status[13] -in @('PC_TEST_END','PC_TEST_STOP')) {
            $null=Wait-Response (Send-Frame 'CLEAR_RUN')
            $until=$timer.ElapsedMilliseconds+1000
            while($state.status[1] -ne 'IDLE' -and $timer.ElapsedMilliseconds -lt $until) { Read-Frames; Start-Sleep -Milliseconds 10 }
        }
        if(!$SignedPwm -or $state.status[1] -ne 'IDLE' -or $state.capabilities[4] -ne '1') { throw 'Keine Freigabe fuer Messpuls.' }
        Read-BenchStatus
        if(!$state.bench -or $state.bench[1] -ne '0' -or $state.bench[2] -ne '0') { throw 'Messmodus ist noch aktiv.' }
        if($StartDelaySeconds) {
            Write-Output "Vorwarnzeit: $StartDelaySeconds Sekunden; noch keine Bewegung."
            $startAt=$timer.ElapsedMilliseconds+$StartDelaySeconds*1000
            while($timer.ElapsedMilliseconds -lt $startAt) { Read-Frames; Start-Sleep -Milliseconds 10 }
        }
        if($state.status[1] -ne 'IDLE' -or $timer.ElapsedMilliseconds-$state.latestStatus -gt 1000) { throw 'Aktuelle Messfreigabe fehlt.' }
        $axis=if($PulseAxis -eq 'Tilt'){1}else{0}
        $position=[int]$state.status[9+$axis]
        $endpoint=$position+$(if($SignedPwm -gt 0){$PulseTravelMdeg}else{-$PulseTravelMdeg})
        if($position -lt [int]$state.capabilities[$axis*2] -or $position -gt [int]$state.capabilities[$axis*2+1] -or
           $endpoint -lt [int]$state.capabilities[$axis*2]+5000 -or $endpoint -gt [int]$state.capabilities[$axis*2+1]-5000) { throw 'Messweg laesst nicht genug Reserve vor den Limits.' }
        Write-Output "Messung $Action $PulseAxis, laufende PWM=$SignedPwm/255, maximal $PulseMs ms oder $PulseTravelMdeg mdeg ab $position mdeg."
        $moveAttempted=$true
        $command=if($Action -eq 'Cruise'){'BENCH_CRUISE'}else{'BENCH_PULSE'}
        $null=Wait-Response (Send-Frame $command @("$axis","$SignedPwm","$PulseMs","$PulseTravelMdeg"))
        $deadline=$timer.ElapsedMilliseconds+$PulseMs+1500
        do {
            Read-Frames
            if($state.faults.Count -or $timer.ElapsedMilliseconds-$state.latestStatus -gt 2000) { throw 'Fehler oder Statusverlust waehrend Messpuls.' }
            Read-BenchStatus
            $null=Wait-Response (Send-Frame 'GET_BENCH_OUTPUT')
            Start-Sleep -Milliseconds 100
        } while(($state.bench[1] -ne '0' -or $state.bench[2] -ne '0') -and $timer.ElapsedMilliseconds -lt $deadline)
        if($state.bench[1] -ne '0' -or $state.bench[2] -ne '0' -or $state.bench[3] -notin @('TIME_BOUND','TRAVEL_BOUND')) {
            throw "Messung nicht regulaer beendet: $($state.bench -join '|')."
        }
        $count=[int]$state.bench[0]
        if($count -lt 2 -or $count -gt 192) { throw 'Ungueltige Laenge der Messspur.' }
        for($index=0;$index -lt $count;$index+=8) {
            $null=Wait-Response (Send-Frame 'GET_BENCH_TRACE' @("$index"))
            $expected=[math]::Min($index+8,$count); $until=$timer.ElapsedMilliseconds+1500
            while($state.samples.Count -lt $expected -and $timer.ElapsedMilliseconds -lt $until) { Read-Frames; Start-Sleep -Milliseconds 10 }
            if($state.samples.Count -lt $expected) { throw 'Messspur unvollstaendig.' }
        }
        $previous=-1
        $csv=for($index=0;$index -lt $count;$index++) {
            if(!$state.samples.ContainsKey($index)) { throw 'Messindex fehlt.' }
            $sample=$state.samples[$index]; $elapsed=[int]$sample[1]
            if($elapsed -le $previous) { throw 'Messzeit ist nicht streng aufsteigend.' }; $previous=$elapsed
            [pscustomobject]@{ elapsedMs=$elapsed; panMdeg=[int]$sample[2]; tiltMdeg=[int]$sample[3]; signedPwm=[int]$sample[4]; stage=[int]$sample[5] }
        }
        $csv | Export-Csv -LiteralPath ([IO.Path]::ChangeExtension([IO.Path]::GetFullPath($LogPath),'.csv')) -NoTypeInformation -Encoding utf8
        [pscustomobject]@{ firmware=$state.firmware; bench=$state.bench; samples=$count; first=$csv[0]; last=$csv[-1]; validFrames=$state.frames; invalidCrc=$state.badCrc; log=[IO.Path]::GetFullPath($LogPath) } | ConvertTo-Json -Depth 4
        return
    }
    if($Action -eq 'CaptureSteps') {
        # Three real shutter pulses, two small downward targets. This mode is
        # explicitly for testing the full executor, not manual motor rides.
        if($state.firmware[0] -ne $CaptureExpectedFirmware -or $state.status[0] -ne '0' -or $state.status[1] -notin @('IDLE','STOPPED') -or $state.capabilities[4] -ne '1'){throw 'Kein freier Aufbau fuer Aufnahme-Schritttest.'}
        if($state.status[1] -eq 'STOPPED'){$null=Wait-Response (Send-Frame 'CLEAR_RUN')}
        if($StartDelaySeconds){
            Write-Output "Aufnahme-Schritttest in $StartDelaySeconds Sekunden: $captureExpectedImages Ausloesungen, $CaptureColumns Spalten, $CaptureRows Zeilen."
            $startAt=$timer.ElapsedMilliseconds+$StartDelaySeconds*1000
            while($timer.ElapsedMilliseconds -lt $startAt){Read-Frames;Start-Sleep -Milliseconds 10}
        }
        if($state.status[1] -ne 'IDLE' -or $timer.ElapsedMilliseconds-$state.latestStatus -gt 1000){throw 'Aktuelle Freigabe fehlt.'}
        $pan=[int]$state.status[9]+$CapturePanStartDeltaMdeg;$tilt=[int]$state.status[10]+$CaptureTiltStartDeltaMdeg
        $lastPan=$pan+($CaptureColumns-1)*$CapturePanStepMdeg;$lastTilt=$tilt+($CaptureRows-1)*$CaptureTiltStepMdeg
        if($pan -lt [int]$state.capabilities[0] -or $lastPan -gt [int]$state.capabilities[1] -or $lastTilt -lt [int]$state.capabilities[2] -or $tilt -gt [int]$state.capabilities[3]){throw 'Testplan ausserhalb der Grenzen.'}
        $null=Wait-Response (Send-Frame 'LOAD_PLAN' @('243803','1','0',"$pan","$tilt","$CaptureColumns","$CaptureRows","$CapturePanStepMdeg","$CaptureTiltStepMdeg","$CaptureSettleMs","$CaptureTriggerMs",'500',"$CapturePanTempo","$CaptureTiltTempo",'0','5000'))
        $captureTestLoaded=$true; $moveAttempted=$true
        $null=Wait-Response (Send-Frame 'START' @('243803'))
    }
    if ($Action -eq 'Move') {
        $effectivePanTempo=if($null -ne $PanTempo){[int]$PanTempo}else{$Tempo}
        $effectiveTiltTempo=if($null -ne $TiltTempo){[int]$TiltTempo}else{$Tempo}
        if(($null -ne $PanTargetMdeg -or $null -ne $TiltTargetMdeg) -and ($PanDeltaMdeg -ne 0 -or $TiltDeltaMdeg -ne 0)) { throw 'Absolute Ziele und relative Schritte sind getrennte Testbefehle.' }
        if ($state.firmware[0] -notin @('2.4.0','2.4.1','2.4.2','2.4.3','2.4.4','2.4.5','2.4.6','2.4.7','2.4.8','2.4.9','2.4.10','2.4.11','2.4.12','2.4.13','2.4.14','2.4.15','2.4.16','2.4.17','2.4.18','2.4.19','2.4.20','2.4.21','2.4.22','2.4.23','2.4.24','2.4.25','2.4.26','2.4.27','2.4.28','2.4.29','2.4.30','2.4.31','2.4.32','2.4.33','2.4.34','2.4.35','2.4.36','2.4.37','2.4.38','2.4.39','2.4.40','2.4.41','2.4.42','2.4.43','2.4.44','2.4.45','2.4.46','2.4.47','2.4.48','2.4.49','2.4.50','2.4.51','2.4.52','2.4.53','2.4.54','2.4.55','2.4.56','2.4.57','2.4.58','2.4.59','2.4.60','2.4.61','2.4.62','2.4.63','2.4.64','2.4.65')) { throw 'Unerwartete Firmware; Testfahrt nicht freigegeben.' }
        if ($state.status[1] -notin @('IDLE','PLAN_READY','STOPPED','COMPLETED','PAUSED') -or $state.capabilities[4] -ne '1') { throw 'Keine Freigabe fuer manuelle Testfahrt.' }
        if ($timer.ElapsedMilliseconds - $state.latestStatus -gt 1000) { throw 'Istposition nicht aktuell.' }
        $pan = if($null -ne $PanTargetMdeg){[int]$PanTargetMdeg}else{[int]$state.status[9]+$PanDeltaMdeg}
        $tilt = if($null -ne $TiltTargetMdeg){[int]$TiltTargetMdeg}else{[int]$state.status[10]+$TiltDeltaMdeg}
        if ([math]::Abs($pan-[int]$state.status[9]) -gt $MaximumStepDegrees*1000 -or [math]::Abs($tilt-[int]$state.status[10]) -gt $MaximumStepDegrees*1000 -or ($pan -eq [int]$state.status[9] -and $tilt -eq [int]$state.status[10])) { throw "Test verlangt einen Schritt von maximal $MaximumStepDegrees Grad." }
        if ($pan -lt [int]$state.capabilities[0] -or $pan -gt [int]$state.capabilities[1] -or $tilt -lt [int]$state.capabilities[2] -or $tilt -gt [int]$state.capabilities[3]) { throw 'Testziel liegt ausserhalb der bestaetigten Grenzen.' }
        Write-Output "Testziel Pan=$pan mdeg, Tilt=$tilt mdeg, Tempo Pan=$effectivePanTempo / Tilt=$effectiveTiltTempo permille."
        if ($StartDelaySeconds) {
            Write-Output "Vorwarnzeit: $StartDelaySeconds Sekunden; noch keine Bewegung."
            $startAt = $timer.ElapsedMilliseconds + $StartDelaySeconds * 1000
            while ($timer.ElapsedMilliseconds -lt $startAt) { Read-Frames; Start-Sleep -Milliseconds 10 }
            if ($timer.ElapsedMilliseconds - $state.latestStatus -gt 1000) { throw 'Istposition nach Vorwarnzeit nicht aktuell.' }
            if ($state.status[1] -notin @('IDLE','PLAN_READY','STOPPED','COMPLETED','PAUSED')) { throw 'Fahrfreigabe nach Vorwarnzeit entfallen.' }
            # Relative target is based on the current position immediately before motion.
            $pan = if($null -ne $PanTargetMdeg){[int]$PanTargetMdeg}else{[int]$state.status[9]+$PanDeltaMdeg}
            $tilt = if($null -ne $TiltTargetMdeg){[int]$TiltTargetMdeg}else{[int]$state.status[10]+$TiltDeltaMdeg}
            if ([math]::Abs($pan-[int]$state.status[9]) -gt $MaximumStepDegrees*1000 -or [math]::Abs($tilt-[int]$state.status[10]) -gt $MaximumStepDegrees*1000){throw 'Aktueller Schritt ueberschreitet die Testfreigabe.'}
            if ($pan -lt [int]$state.capabilities[0] -or $pan -gt [int]$state.capabilities[1] -or $tilt -lt [int]$state.capabilities[2] -or $tilt -gt [int]$state.capabilities[3]) { throw 'Aktuelles Testziel ausserhalb der Grenzen.' }
        }
        if($IncrementalStep -and [version]$state.firmware[0] -lt [version]'2.4.38'){throw 'Schritt-Zeitgrenze erfordert Firmware ab 2.4.38.'}
        $moveFields=@("$pan","$tilt","$effectivePanTempo","$effectiveTiltTempo")
        if($IncrementalStep){$moveFields+= '1'}
        $moveAttempted = $true
        $moveSequence = Send-Frame 'MANUAL_MOVE' $moveFields
        $null = Wait-Response $moveSequence
    }
    if($Action -eq 'Jog') {
        if($state.firmware[0] -notin @('2.4.43','2.4.44','2.4.45','2.4.46','2.4.47','2.4.48','2.4.49','2.4.50','2.4.51','2.4.52','2.4.53','2.4.54','2.4.55','2.4.56','2.4.57','2.4.58','2.4.59','2.4.60','2.4.61','2.4.62','2.4.63','2.4.64','2.4.65') -or $state.status[0] -ne '0' -or $state.status[1] -ne 'IDLE' -or
           $state.capabilities[4] -ne '1' -or $timer.ElapsedMilliseconds-$state.latestStatus -gt 1000) { throw 'Kein aktueller freier Aufbau fuer Jog-Test.' }
        $axis=if($PulseAxis -eq 'Tilt'){1}else{0}; $jogOrigin=[int]$state.status[9+$axis]
        $jogToken=Get-Random -Minimum 100000 -Maximum 2147483647
        Write-Output "Begrenzter Jog-Test $PulseAxis Richtung $JogDirection, maximal $JogHoldMs ms und $MaximumStepDegrees Grad."
        $moveAttempted=$true; $jogStartedMs=$timer.ElapsedMilliseconds; $nextJogKeep=$jogStartedMs+150
        if($JogSpeedDps -gt 0) {
            if($state.firmware[0] -notin @('2.4.49','2.4.50','2.4.51','2.4.52','2.4.53','2.4.54','2.4.55','2.4.56','2.4.57','2.4.58','2.4.59','2.4.60','2.4.61','2.4.62','2.4.63','2.4.64','2.4.65') -or $JogSpeedDps -lt 0.5) { throw 'Absolute Haltegeschwindigkeit nicht freigegeben.' }
            $jogSpeedMdeg=[int][math]::Round($JogSpeedDps*1000)
            $moveSequence=Send-Frame 'MANUAL_JOG_START_VELOCITY' @("$jogToken","$axis","$JogDirection","$jogSpeedMdeg")
        } else { $moveSequence=Send-Frame 'MANUAL_JOG_START' @("$jogToken","$axis","$JogDirection","$Tempo") }
        $null=Wait-Response $moveSequence
    }
    $deadline = $timer.ElapsedMilliseconds + $DurationSeconds * 1000; $nextDiagnostics = 0
    $arrivalAt=$null
    while ($timer.ElapsedMilliseconds -lt $deadline) {
        Read-Frames
        if($Action -ne 'CaptureSteps' -and $moveAttempted -and $state.status -and $state.status[13] -eq 'STEP_MOVE_TIMEOUT'){throw 'Firmware-Schrittzeitgrenze erreicht: STEP_MOVE_TIMEOUT.'}
        if ($moveAttempted -and $state.faults.Count) { throw "Geraetefehler: $($state.faults[-1])." }
        if($Action -eq 'Jog' -and !$jogStopped) {
            if([math]::Abs([int]$state.status[9+$axis]-$jogOrigin) -ge $MaximumStepDegrees*1000 -or
               $timer.ElapsedMilliseconds-$jogStartedMs -ge $JogHoldMs) {
                $null=Wait-Response (Send-Frame 'MANUAL_JOG_STOP' @("$jogToken")); $jogStopped=$true
            } elseif(!$JogLeaseExpiry -and $timer.ElapsedMilliseconds -ge $nextJogKeep) {
                $s=$state.manualStatus
                if($s -and [int]$s[0] -eq $moveSequence -and $s[2] -ne 'ACTIVE') { throw 'Dauerfahrt vor Loslassen beendet.' }
                $null=Wait-Response (Send-Frame 'MANUAL_JOG_KEEPALIVE' @("$jogToken")); $nextJogKeep=$timer.ElapsedMilliseconds+150
            }
        }
        if ($timer.ElapsedMilliseconds - $state.latestStatus -gt 2000) { throw 'Aktueller Geraetestatus ausgeblieben.' }
        if ($timer.ElapsedMilliseconds -ge $nextDiagnostics) {
            $null = Wait-Response (Send-Frame 'GET_DIAGNOSTICS')
            $nextDiagnostics = $timer.ElapsedMilliseconds + $DiagnosticIntervalMs
        }
        if($Action -eq 'CaptureSteps' -and $state.status[1] -eq 'COMPLETED' -and $captureImages.Count -eq $captureExpectedImages){break}
        if($Action -eq 'Move' -and $moveAttempted -and $WaitForArrival -and $state.diagnostics -and $state.motion) {
            $d=$state.diagnostics;$m=$state.motion
            $arrived=[int]$d[7] -eq $pan -and [int]$d[8] -eq $tilt -and
                [int]$d[5] -ge [int]$d[6] -and [int]$d[6] -gt 0 -and
                $d[13] -eq '1' -and $d[14] -eq '1' -and $m[6] -eq '0' -and $m[7] -eq '0'
            if($Action -eq 'Move' -and $state.manualStatusSupported) {
                $s=$state.manualStatus
                $arrived=$null -ne $s -and $s.Count -eq 7 -and [int]$s[0] -eq $moveSequence -and
                    $s[1] -eq 'MOVE' -and $s[2] -eq 'REACHED' -and
                    [int]$s[3] -eq $pan -and [int]$s[4] -eq $tilt -and
                    $m[6] -eq '0' -and $m[7] -eq '0'
            }
            if($arrived) {
                if($null -eq $arrivalAt){$arrivalAt=$timer.ElapsedMilliseconds;Write-Record 'TEST' 'ARRIVAL_CONFIRMED' @("$pan","$tilt")}
                if($timer.ElapsedMilliseconds-$arrivalAt -ge $DwellMs){break}
            }else{$arrivalAt=$null}
        }
        Start-Sleep -Milliseconds 10
    }
    if($Action -eq 'CaptureSteps' -and ($state.status[1] -ne 'COMPLETED' -or $captureImages.Count -ne $captureExpectedImages)){throw 'Aufnahme-Schritttest nicht vollstaendig abgeschlossen.'}
    if($Action -eq 'CaptureSteps'){$null=Wait-Response (Send-Frame 'GET_DEBUG_LOG' @('0'));$traceUntil=$timer.ElapsedMilliseconds+1200;while($timer.ElapsedMilliseconds -lt $traceUntil){Read-Frames;Start-Sleep -Milliseconds 10}}
    if($Action -eq 'Move' -and $moveAttempted -and $WaitForArrival -and ($null -eq $arrivalAt -or $timer.ElapsedMilliseconds-$arrivalAt -lt $DwellMs)){throw 'Zielruhe und Haltezeit wurden innerhalb des Testbudgets nicht bestaetigt.'}
    if($Action -eq 'Jog') {
        $s=$state.manualStatus
        if(!$jogStopped -or !$s -or [int]$s[0] -ne $moveSequence -or $s[1] -ne 'JOG' -or $s[2] -ne 'CANCELLED' -or
           $state.motion[6] -ne '0' -or $state.motion[7] -ne '0') { throw 'Jog-Stop nicht durch Betriebsstatus und ausgeschaltete Ausgaenge bestaetigt.' }
        if(!$JogLeaseExpiry -and ([int]$state.status[9+$axis]-$jogOrigin)*$JogDirection -lt 200) { throw 'Jog erzeugte keinen ausreichenden gerichteten Fortschritt.' }
        if($JogLeaseExpiry -and [int]$s[5] -gt [int]$s[6]+100) { throw 'Jog-Lease ohne rechtzeitige Abschaltung.' }
    }
    [pscustomobject]@{ firmware=$state.firmware; capabilities=$state.capabilities; status=$state.status; diagnostics=$state.diagnostics; motion=$state.motion; faults=@($state.faults | Select-Object -Unique); validFrames=$state.frames; invalidCrc=$state.badCrc; log=[IO.Path]::GetFullPath($LogPath) } | ConvertTo-Json -Depth 4
}
finally {
    $state.stopping = $true
    $stopFailure = $null
    if ($moveAttempted -and $serial.IsOpen) {
        try {
            $runId = if ($state.status) { $state.status[0] } else { '0' }
            $null = Wait-Response (Send-Frame 'ESTOP' @($runId,'PC_TEST_END'))
            Write-Output 'Test beendet; ESTOP bestaetigt, beide Motorausgaenge deaktiviert.'
            if($captureTestLoaded){$null=Wait-Response (Send-Frame 'CLEAR_RUN');Write-Output 'Eigener Aufnahme-Testplan entfernt.'}
        } catch { $stopFailure = $_.Exception.Message; Write-Warning "Stopbestaetigung fehlt: $stopFailure" }
    }
    if ($serial.IsOpen) { $serial.Close() }; $serial.Dispose()
    if ($writer) { $writer.Dispose() }
    if ($stopFailure) { throw "Testabschluss ohne bestaetigten ESTOP: $stopFailure" }
}

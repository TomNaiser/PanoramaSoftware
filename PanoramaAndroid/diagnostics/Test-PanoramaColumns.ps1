param(
    [string]$ControlPort='COM4', [string]$TracePort='COM3',
    [Parameter(Mandatory=$true)][string]$OutputDirectory,
    [ValidateRange(1,3)][int]$Repetitions=2,
    [ValidateRange(-150000,150000)][int]$PanStartMdeg=120278,
    [ValidateRange(1,207)][int]$StartIndex=1,
    [switch]$ContinueTargetMisses,
    [string]$AbortFile=''
)
$ErrorActionPreference='Stop'
$directory=[IO.Path]::GetFullPath($OutputDirectory)
if(Test-Path -LiteralPath $directory){throw 'Vorhandene Spaltenreihe wird nicht ueberschrieben.'}
$null=New-Item -ItemType Directory -Path $directory
$trace=Join-Path $directory 'columns-trace.csv';$stop=Join-Path $directory 'capture.stop'
$capturePath=Join-Path $PSScriptRoot 'Capture-MotionTrace.ps1'
$plan=@();$index=0
# Matches PanoramaPlanV2::targetFor: row changes first; a new column
# returns to its starting tilt. Each measured column uses negative tilt steps.
foreach($repeat in 1..$Repetitions) {
    foreach($direction in @('down')) {
        $columns=@(0,1,2)
        foreach($column in $columns) {
            foreach($row in 0..22) {
                $tilt=24000-2000*$row
                $plan+=[pscustomobject]@{index=++$index;repeat=$repeat;direction=$direction;
                    column=$column;row=$row;panTargetMdeg=$PanStartMdeg+3000*$column;tiltTargetMdeg=$tilt;
                    kind=if($row -eq 0){'column-start'}else{'row-step'};
                    logName=('{0:D3}-{1}-c{2}-r{3}.jsonl' -f $index,$direction,$column,$row)}
            }
        }
    }
}
if($StartIndex -gt $plan.Count){throw 'Startindex liegt ausserhalb des Spaltenplans.'}
$plan | Export-Csv -LiteralPath (Join-Path $directory 'plan.csv') -NoTypeInformation -Encoding utf8
[pscustomobject]@{firmware='2.4.30';repetitions=$Repetitions;startIndex=$StartIndex;panStartMdeg=$PanStartMdeg;
    panStepMdeg=3000;tiltStartMdeg=24000;tiltStepMdeg=-2000;rows=23;columns=3;tempoPermille=500;
    continueTargetMisses=[bool]$ContinueTargetMisses;additionalDwellMs=0} | ConvertTo-Json |
    Set-Content -LiteralPath (Join-Path $directory 'configuration.json') -Encoding utf8
$job=Start-Job -ScriptBlock {
    param($script,$port,$log,$stopFile)
    & $script -Port $port -DurationSeconds 2400 -LogPath $log -StopFile $stopFile
} -ArgumentList $capturePath,$TracePort,$trace,$stop
$completed=@();$previous=$null;$consecutiveMisses=0
try {
    $timer=[Diagnostics.Stopwatch]::StartNew();$ready=$false
    while($timer.ElapsedMilliseconds -lt 10000) {
        if($job.State -ne 'Running'){throw 'USB-Aufzeichnung vor Fahrtbeginn beendet.'}
        if(Test-Path -LiteralPath $trace) {
            $head=@(Get-Content -LiteralPath $trace -TotalCount 2)
            if($head.Count -eq 2 -and $head[1] -match '^\d+,'){$ready=$true;break}
        }
        Start-Sleep -Milliseconds 50
    }
    if(!$ready){throw 'Keine USB-Reglerproben; keine Spaltenfahrt.'}
    foreach($point in $plan) {
        if($point.index -lt $StartIndex){continue}
        if($job.State -ne 'Running'){throw 'USB-Aufzeichnung waehrend Spaltenreihe ausgefallen.'}
        if($AbortFile -and (Test-Path -LiteralPath $AbortFile)){throw 'Spaltenreihe durch Abbruchdatei gestoppt.'}
        $log=Join-Path $directory $point.logName
        $shared=$previous -and $previous.panTargetMdeg -eq $point.panTargetMdeg -and $previous.tiltTargetMdeg -eq $point.tiltTargetMdeg
        Write-Output ('Punkt {0}/{1}: Durchlauf {2}, {3}, Spalte {4}, Pan {5:N1}, Tilt {6:N1}' -f
            $point.index,$plan.Count,$point.repeat,$point.direction,($point.column+1),($point.panTargetMdeg/1000),($point.tiltTargetMdeg/1000))
        $pointFailure=''
        try {
        if($shared) {
            # The previous sweep already measured this physical endpoint.
            & (Join-Path $PSScriptRoot 'Test-MotionHardware.ps1') -Port $ControlPort -Action Monitor -DurationSeconds 2 -AbortFile $AbortFile -LogPath $log | Out-Null
        }else{
            $delay=if($completed.Count -eq 0){5}else{0}
            & (Join-Path $PSScriptRoot 'Test-MotionHardware.ps1') -Port $ControlPort -Action Move `
                -PanTargetMdeg $point.panTargetMdeg -TiltTargetMdeg $point.tiltTargetMdeg `
                -MaximumStepDegrees 46 -Tempo 500 -DurationSeconds 30 -WaitForArrival -DwellMs 0 `
                -StartDelaySeconds $delay -DiagnosticIntervalMs 150 -AbortFile $AbortFile -LogPath $log | Out-Null
        }
        }catch {
            if(!$ContinueTargetMisses -or $_.Exception.Message -ne 'Geraetefehler: MOTION_TARGET_MISSE.'){throw}
            $pointFailure='MOTION_TARGET_MISSED'
        }
        $records=@(Get-Content -LiteralPath $log | ConvertFrom-Json)
        $status=$records | Where-Object {$_.direction -eq 'RX' -and $_.type -eq 'STATUS'} | Select-Object -Last 1
        $info=$records | Where-Object type -eq 'DEVICE_INFO' | Select-Object -First 1
        if(!$status -or $info.fields[0] -ne '2.4.30' -or $info.fields[3] -ne 'ARDUINO_NANO_R4_BT_TRACE' -or $status.fields[1] -eq 'FAULT'){throw 'Unerwarteter Firmware- oder Endstatus.'}
        $move=$records | Where-Object {$_.direction -eq 'TX' -and $_.type -eq 'MANUAL_MOVE'} | Select-Object -First 1
        $arrival=$records | Where-Object type -eq 'ARRIVAL_CONFIRMED' | Select-Object -Last 1
        $stopTx=$records | Where-Object {$_.direction -eq 'TX' -and $_.type -eq 'ESTOP'} | Select-Object -Last 1
        $ack=$records | Where-Object {$_.direction -eq 'RX' -and $_.type -eq 'ACK' -and $_.fields[1] -eq 'STOPPED' -and $stopTx -and $_.elapsedMs -ge $stopTx.elapsedMs} | Select-Object -Last 1
        if(!$shared -and (!$ack -or (!$arrival -and !$pointFailure))){throw 'Zielruhe oder Notstop nicht bestaetigt; keine weitere Position.'}
        if($pointFailure) {
            $lastMotion=$records | Where-Object type -eq 'MOTION_DIAGNOSTICS' | Select-Object -Last 1
            if($status.fields[1] -ne 'STOPPED' -or !$lastMotion -or $lastMotion.fields[6] -ne '0' -or $lastMotion.fields[7] -ne '0' -or
                [math]::Abs([int]$status.fields[10]-$point.tiltTargetMdeg) -gt 1000){throw 'Zielabweichung ohne begrenzte, bestaetigte Abschaltung; Reihe beendet.'}
            ++$consecutiveMisses
        }else{$consecutiveMisses=0}
        $diags=@($records | Where-Object {$_.type -eq 'DIAGNOSTICS' -and $move -and $_.elapsedMs -ge $move.elapsedMs})
        $completed+=[pscustomobject]@{index=$point.index;repeat=$point.repeat;direction=$point.direction;column=$point.column;row=$point.row;
            kind=$point.kind;sharedEndpoint=[bool]$shared;pointFailure=$pointFailure;arrivalConfirmed=[bool]$arrival;panTargetMdeg=$point.panTargetMdeg;tiltTargetMdeg=$point.tiltTargetMdeg;
            finalPanMdeg=[int]$status.fields[9];finalTiltMdeg=[int]$status.fields[10];
            panErrorMdeg=[int]$status.fields[9]-$point.panTargetMdeg;tiltErrorMdeg=[int]$status.fields[10]-$point.tiltTargetMdeg;
            arrivalMs=if($arrival){[int]$arrival.elapsedMs-[int]$move.elapsedMs}else{$null};
            firstDiagnosticSequence=if($diags.Count){[long]$diags[0].fields[4]}else{$null};
            crcErrors=@($records | Where-Object type -eq 'CRC_BAD').Count;nackFrames=@($records | Where-Object type -eq 'NACK').Count;
            stopConfirmed=[bool]$ack;logName=$point.logName}
        $completed | Export-Csv -LiteralPath (Join-Path $directory 'positions.csv') -NoTypeInformation -Encoding utf8
        Write-Output ('  Halt {0} ms; Restfehler Tilt {1} mdeg, Pan {2} mdeg; NACK {3}' -f $completed[-1].arrivalMs,$completed[-1].tiltErrorMdeg,$completed[-1].panErrorMdeg,$completed[-1].nackFrames)
        $previous=$point
        if($consecutiveMisses -ge 3){throw 'Drei aufeinanderfolgende Zielabweichungen; keine weitere Position.'}
    }
    Set-Content -LiteralPath (Join-Path $directory 'completed.txt') -Value $completed.Count -Encoding ascii
}
catch {
    [pscustomobject]@{message=$_.Exception.Message;completed=$completed.Count;point=$point;time=[DateTimeOffset]::Now.ToString('o')} |
        ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $directory 'failed.json') -Encoding utf8
    throw
}
finally {
    Set-Content -LiteralPath $stop -Value 'CAPTURE_FINISHED' -Encoding ascii
    $null=Wait-Job -Job $job -Timeout 10
    if($job.State -eq 'Running'){Stop-Job -Job $job;throw 'USB-Aufzeichnung beendet sich nicht rechtzeitig.'}
    try {Receive-Job -Job $job -ErrorAction Stop}finally{Remove-Job -Job $job}
}

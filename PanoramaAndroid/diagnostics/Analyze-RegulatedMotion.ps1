param(
    [Parameter(Mandatory=$true)][string]$Directory,
    [string]$FilePrefix='regulated-2417',
    [string]$FileName='',
    [double]$ExpectedSpeedDps=2.5
)
$ErrorActionPreference='Stop'
function Get-SpeedMean($rows) {
    if(!$rows.Count){return $null}
    return ($rows | ForEach-Object {[math]::Abs($_.velocityMdegPerSecond)/1000} | Measure-Object -Average).Average
}
$files=if($FileName){@(Get-Item -LiteralPath (Join-Path $Directory $FileName))}else{@(Get-ChildItem -LiteralPath $Directory -Filter ($FilePrefix+'-*.jsonl'))}
$results=@(foreach($file in $files) {
    $records=@(Get-Content -LiteralPath $file.FullName | ConvertFrom-Json)
    $move=$records | Where-Object {$_.direction -eq 'TX' -and $_.type -eq 'MANUAL_MOVE'} | Select-Object -First 1
    if(!$move){continue}
    $start=$records | Where-Object {$_.direction -eq 'RX' -and $_.type -eq 'STATUS' -and $_.elapsedMs -lt $move.elapsedMs} | Select-Object -Last 1
    if(!$start){throw ('Startstatus fehlt: '+$file.Name)}
    $diag=$null
    $rows=@(foreach($r in $records) {
        if($r.direction -ne 'RX' -or $r.elapsedMs -lt $move.elapsedMs){continue}
        if($r.type -eq 'DIAGNOSTICS'){$diag=$r}
        if($r.type -eq 'MOTION_DIAGNOSTICS' -and $diag) {
            if($r.fields.Count -lt 12){throw 'Fahrphasen fehlen.'}
            [pscustomobject]@{ms=[int]$r.elapsedMs-[int]$move.elapsedMs;
                actualTiltMdeg=[int]$diag.fields[10];targetTiltMdeg=[int]$diag.fields[8];
                velocityMdegPerSecond=[int]$r.fields[3];panSignedPwm=[int]$r.fields[6];
                tiltSignedPwm=[int]$r.fields[7];tiltDriveStage=[int]$r.fields[11]}
        }
    })
    if(!$rows.Count){throw ('Diagnose fehlt: '+$file.Name)}
    $base=Join-Path $Directory ([IO.Path]::GetFileNameWithoutExtension($file.Name))
    $rows | Export-Csv -LiteralPath ($base+'-analysis.csv') -NoTypeInformation -Encoding UTF8
    $steady=@($rows | Where-Object {$_.ms -ge 1000 -and $_.tiltDriveStage -eq 2})
    $startup=@($rows | Where-Object {$_.ms -lt 1000 -and $_.tiltDriveStage -in @(1,2)})
    $hold=$rows | Where-Object tiltDriveStage -eq 4 | Select-Object -First 1
    $status=$records | Where-Object {$_.direction -eq 'RX' -and $_.type -eq 'STATUS'} | Select-Object -Last 1
    $stop=$records | Where-Object {$_.direction -eq 'TX' -and $_.type -eq 'ESTOP'} | Select-Object -Last 1
    $ack=$records | Where-Object {$_.direction -eq 'RX' -and $_.type -eq 'ACK' -and $_.fields[1] -eq 'STOPPED' -and $stop -and $_.elapsedMs -ge $stop.elapsedMs} | Select-Object -Last 1
    $sign=if([int]$move.fields[1] -gt [int]$start.fields[10]){1}else{-1}
    $faults=@($records | Where-Object {$_.type -eq 'STATUS' -and $_.fields[1] -eq 'FAULT'}).Count
    $crc=@($records | Where-Object type -eq 'CRC_BAD').Count
    $nacks=@($records | Where-Object type -eq 'NACK').Count
    $opposite=@($rows | Where-Object {$_.tiltSignedPwm*$sign -lt 0}).Count
    $afterBrake=@($rows | Where-Object {$_.tiltDriveStage -ge 3 -and $_.tiltSignedPwm -ne 0}).Count
    $error=[int]$status.fields[10]-[int]$move.fields[1]
    $mean=Get-SpeedMean $steady
    $speedInBand=if($steady.Count -ge 5){$mean -ge $ExpectedSpeedDps*0.75 -and $mean -le $ExpectedSpeedDps*1.25}else{$null}
    $pauseMs=0
    $cruiseRows=@($rows | Where-Object {$_.tiltDriveStage -eq 2 -and [math]::Abs($_.targetTiltMdeg-$_.actualTiltMdeg) -gt 300})
    for($i=0;$i -lt $cruiseRows.Count;$i++) {
        $min=$max=$cruiseRows[$i].actualTiltMdeg
        for($j=$i+1;$j -lt $cruiseRows.Count;$j++) {
            $min=[math]::Min($min,$cruiseRows[$j].actualTiltMdeg);$max=[math]::Max($max,$cruiseRows[$j].actualTiltMdeg)
            if($max-$min -gt 120){break}
            $pauseMs=[math]::Max($pauseMs,$cruiseRows[$j].ms-$cruiseRows[$i].ms)
        }
    }
    $last=$rows[-1]
    $pass=[bool]$hold -and [bool]$ack -and !$faults -and !$crc -and !$nacks -and !$opposite -and !$afterBrake -and
        [math]::Abs($error) -le 400 -and $last.panSignedPwm -eq 0 -and $last.tiltSignedPwm -eq 0 -and $speedInBand -ne $false -and $pauseMs -lt 350
    [pscustomobject]@{name=[IO.Path]::GetFileNameWithoutExtension($file.Name).Substring($FilePrefix.Length+1);
        startTiltMdeg=[int]$start.fields[10];targetTiltMdeg=[int]$move.fields[1];finalStatusTiltMdeg=[int]$status.fields[10];
        finalStatusErrorMdeg=$error;firstConfirmedHoldMs=if($hold){$hold.ms}else{$null};steadySamples=$steady.Count;
        steadyMeanDps=$mean;steadySpeedInBand=$speedInBand;maximumObservedCruisePauseMs=$pauseMs;
        startupPeakSampledDps=($startup | ForEach-Object {[math]::Abs($_.velocityMdegPerSecond)/1000} | Measure-Object -Maximum).Maximum;
        peakSampledVelocityDps=($rows | ForEach-Object {[math]::Abs($_.velocityMdegPerSecond)/1000} | Measure-Object -Maximum).Maximum;
        steadyPwm=@($steady.tiltSignedPwm | Select-Object -Unique);oppositeTiltPwmSamples=$opposite;
        nonzeroPwmAfterBrakeSamples=$afterBrake;faultFrames=$faults;crcErrors=$crc;nackFrames=$nacks;
        stopConfirmed=[bool]$ack;pass=$pass}
})
if(!$results.Count){throw 'Keine geregelte Fahrt gefunden.'}
$summaryName=if($FileName){[IO.Path]::GetFileNameWithoutExtension($FileName)+'-summary.json'}else{$FilePrefix+'-summary.json'}
$results | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $Directory $summaryName) -Encoding UTF8
$results | Format-Table name,startupPeakSampledDps,steadyMeanDps,finalStatusErrorMdeg,firstConfirmedHoldMs,maximumObservedCruisePauseMs,pass -AutoSize
if(@($results | Where-Object pass -eq $false).Count){throw 'Mindestens eine Fahrt erfuellt die Auswertungskriterien nicht.'}

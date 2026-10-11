param(
    [Parameter(Mandatory=$true)][string]$Path,
    [double]$ExpectedSpeedDps=2.5,
    [switch]$AllowStartupDip
)
$ErrorActionPreference='Stop'
# Assess the filtered startup speed, not single short-slope encoder spikes.
# This is a telemetry criterion; it cannot establish optical smoothness.
$rows=@(Import-Csv -LiteralPath $Path)
if($rows.Count -lt 20){throw 'Zu wenige Reglerproben.'}
for($i=0;$i -lt $rows.Count;$i++) {
    if([int]$rows[$i].sensorsValid -ne 1 -or [int]$rows[$i].dropped -ne 0){throw 'Messspur mit Sensorfehler oder verworfenen Proben.'}
    if($i -gt 0 -and ([long]$rows[$i].sequence-[long]$rows[$i-1].sequence -ne 1 -or
        [long]$rows[$i].sampleMs-[long]$rows[$i-1].sampleMs -gt 100)){throw 'Messspur nicht zusammenhaengend.'}
}
$results=@();$i=0
while($i -lt $rows.Count) {
    if($rows[$i].enabled -ne '1'){$i++;continue}
    $first=$i;$target=[int]$rows[$i].targetMdeg
    while($i -lt $rows.Count -and $rows[$i].enabled -eq '1' -and [int]$rows[$i].targetMdeg -eq $target){$i++}
    $run=@($rows[$first..($i-1)])
    $origin=[int]$run[0].tiltMdeg
    $sign=if($target -gt $origin){1}else{-1}
    $startMs=[long]$run[0].sampleMs
    $reachedSpeed=$false;$dipStart=$null;$longest=0;$firstDip=$null
    $minimum=100.0;$maximum=0.0;$handoff=$null
    for($j=0;$j -lt $run.Count;$j++) {
        $r=$run[$j];$ms=[long]$r.sampleMs-$startMs
        if(!$handoff -and $r.stage -eq '2'){$handoff=$r}
        # First degree only; brake and settle are intentionally excluded.
        if([int]$r.stage -notin @(1,2) -or ([int]$r.tiltMdeg-$origin)*$sign -gt 1000){continue}
        $speed=$sign*[double]$r.velocityMdegPerSecond/1000.0
        $maximum=[math]::Max($maximum,$speed)
        if($speed -ge 0.7*$ExpectedSpeedDps){$reachedSpeed=$true}
        if($reachedSpeed -and $r.stage -eq '2') {
            $minimum=[math]::Min($minimum,$speed)
            if($speed -lt 0.4*$ExpectedSpeedDps) {
                if($null -eq $dipStart){$dipStart=$ms}
                $end=if($j+1 -lt $run.Count){[long]$run[$j+1].sampleMs-$startMs}else{$ms}
                $duration=$end-$dipStart
                if($duration -gt $longest){$longest=$duration}
                if($duration -ge 100 -and $null -eq $firstDip){$firstDip=$dipStart}
            }else{$dipStart=$null}
        }
    }
    if(!$handoff) {
        # A column transition can move only Pan while Tilt already rests.
        if([math]::Abs($target-$origin) -le 300){continue}
        throw 'Keine bestaetigte Tilt-Fahrtphase in der Messspur.'
    }
    $before=$run | Where-Object {[long]$_.sampleMs -lt [long]$handoff.sampleMs} | Select-Object -Last 1
    $hold=$run | Where-Object stage -eq '4' | Select-Object -First 1
    $results+=[pscustomobject]@{
        direction=if($sign -gt 0){'up'}else{'down'};targetMdeg=$target;records=$run.Count;
        firstSequence=[long]$run[0].sequence;lastSequence=[long]$run[-1].sequence;
        handoffMs=[long]$handoff.sampleMs-$startMs;
        pwmBeforeHandoff=if($before){[int]$before.appliedPwm}else{$null};pwmAtHandoff=[int]$handoff.appliedPwm;
        integralAtHandoff=[double]$handoff.integralMilliPwm/1000;
        filteredSpeedAtHandoffDps=[double]$handoff.velocityMdegPerSecond/1000;
        shortSpeedAtHandoffDps=[double]$handoff.shortVelocityMdegPerSecond/1000;
        firstDegreePeakFilteredDps=$maximum;minimumFilteredAfterAccelerationDps=if($minimum -lt 100){$minimum}else{$null};
        longestStartupLowSpeedMs=$longest;firstStartupDipMs=$firstDip;startupSpeedDip=($longest -ge 100);
        holdConfirmed=[bool]$hold;
        oppositeAppliedPwmSamples=@($run | Where-Object {[int]$_.appliedPwm*$sign -lt 0}).Count;
        poweredBrakeSamples=@($run | Where-Object {[int]$_.stage -ge 3 -and [int]$_.appliedPwm -ne 0}).Count
    }
}
if(!$results.Count){throw 'Keine Fahrt in der Messspur.'}
$results | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath ([IO.Path]::ChangeExtension($Path,'analysis.json')) -Encoding utf8
$results | Format-Table direction,handoffMs,pwmBeforeHandoff,pwmAtHandoff,longestStartupLowSpeedMs,startupSpeedDip,holdConfirmed -AutoSize
if(!$AllowStartupDip -and @($results | Where-Object startupSpeedDip).Count){throw 'Tempodelle nach erstem Anfahren in der dichten Messspur erkannt.'}

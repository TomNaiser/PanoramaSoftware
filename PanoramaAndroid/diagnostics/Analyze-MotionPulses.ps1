param([Parameter(Mandatory=$true)][string]$Directory)
$ErrorActionPreference='Stop'
function Get-Median($values) {
    $sorted=@($values | Sort-Object)
    if(!$sorted.Count){return $null}
    if($sorted.Count%2){return [double]$sorted[[int]($sorted.Count/2)]}
    return ([double]$sorted[$sorted.Count/2-1]+[double]$sorted[$sorted.Count/2])/2
}
function Get-Slope($rows,[string]$positionField) {
    if($rows.Count -lt 2){return $null}
    $origin=[double]$rows[0].elapsedMs
    $timeMean=($rows | ForEach-Object {([double]$_.elapsedMs-$origin)/1000} | Measure-Object -Average).Average
    $positionMean=($rows | ForEach-Object {[double]$_.$positionField/1000} | Measure-Object -Average).Average
    $covariance=0.0; $variance=0.0
    foreach($row in $rows){
        $time=([double]$row.elapsedMs-$origin)/1000-$timeMean
        $covariance+=$time*([double]$row.$positionField/1000-$positionMean); $variance+=$time*$time
    }
    if($variance -le 0){return $null}; return $covariance/$variance
}
$results=foreach($file in Get-ChildItem -LiteralPath $Directory -Filter '*.csv') {
    if($file.Name -eq 'analysis.csv'){continue}
    $log=[IO.Path]::ChangeExtension($file.FullName,'.jsonl')
    if(!(Test-Path -LiteralPath $log)){continue}
    $pulse=Get-Content -LiteralPath $log | ConvertFrom-Json | Where-Object {$_.direction -eq 'TX' -and $_.type -eq 'BENCH_PULSE'} | Select-Object -Last 1
    if(!$pulse){continue}
    $rows=@(Import-Csv -LiteralPath $file.FullName)
    if($rows.Count -lt 3){throw "Messspur zu kurz: $($file.Name)"}
    $field=if($pulse.fields[0] -eq '1'){'tiltMdeg'}else{'panMdeg'}
    $requested=[int]$pulse.fields[1]; $direction=if($requested -gt 0){1}else{-1}
    $powered=@(0..($rows.Count-1) | Where-Object {[int]$rows[$_].signedPwm -ne 0})
    if(!$powered.Count){throw "Keine Motoransteuerung in Messspur: $($file.Name)"}
    $offIndex=$powered[-1]+1
    if($offIndex -ge $rows.Count){throw "Nachlaufspur fehlt: $($file.Name)"}
    $offTime=[int]$rows[$offIndex].elapsedMs
    $start=Get-Median @($rows | Select-Object -First 3 | ForEach-Object {[double]$_.$field})
    $end=Get-Median @($rows | Select-Object -Last 5 | ForEach-Object {[double]$_.$field})
    $plateau=@(0..($offIndex-1) | Where-Object {[int]$rows[$_].signedPwm -eq $requested})
    $velocities=@(); $steadySlope=$null; $steadyDuration=0
    if($plateau.Count){
        $plateauTime=[int]$rows[$plateau[0]].elapsedMs
        $steady=@($rows | Where-Object {[int]$_.elapsedMs -ge $plateauTime+200 -and [int]$_.elapsedMs -lt $offTime})
        if($steady.Count -ge 2){$steadySlope=(Get-Slope $steady $field)*$direction; $steadyDuration=[int]$steady[-1].elapsedMs-[int]$steady[0].elapsedMs}
        for($i=$plateau[0];$i -lt $offIndex;$i++){
            $now=[int]$rows[$i].elapsedMs
            if($now -lt $plateauTime+200){continue}
            $window=@($rows | Where-Object {[int]$_.elapsedMs -ge $now-200 -and [int]$_.elapsedMs -le $now})
            if($window.Count -ge 5){$velocities+=((Get-Slope $window $field)*$direction)}
        }
    }
    $delay=$null
    foreach($row in $rows){
        if([int]$row.elapsedMs -ge $offTime){break}
        if(([double]$row.$field-$start)*$direction -ge 250){$delay=[int]$row.elapsedMs; break}
    }
    $fraction=$null; $variation=$null
    if($velocities.Count){
        $mean=($velocities | Measure-Object -Average).Average
        $fraction=@($velocities | Where-Object {$_ -lt 0.2}).Count/$velocities.Count
        if($mean -gt 0.2){
            $variance=($velocities | ForEach-Object {($_-$mean)*($_-$mean)} | Measure-Object -Average).Average
            $variation=[math]::Sqrt($variance)/$mean
        }
    }
    $offPosition=[double]$rows[$offIndex].$field
    [pscustomobject]@{file=$file.Name;axis=if($field -eq 'tiltMdeg'){'Tilt'}else{'Pan'};
        signedPwm=$requested;startDeg=[math]::Round($start/1000,3);endDeg=[math]::Round($end/1000,3);
        forwardTravelDeg=[math]::Round(($end-$start)*$direction/1000,3);
        delayToQuarterDegreeMs=$delay;motorOffMs=$offTime;steadyDurationMs=$steadyDuration;
        steadyForwardSpeedDps=$steadySlope;velocityWindows=$velocities.Count;
        slowWindowFraction=$fraction;speedCoefficientOfVariation=$variation;
        coastTravelDeg=[math]::Round(($end-$offPosition)*$direction/1000,3)}
}
if(!$results){throw 'Keine auswertbaren Messpulse vorhanden.'}
$csvResults=foreach($row in $results) {
    $values=[ordered]@{}
    foreach($property in $row.PSObject.Properties) {
        $values[$property.Name]=if($property.Value -is [double]) {$property.Value.ToString('R',[Globalization.CultureInfo]::InvariantCulture)}else{$property.Value}
    }
    [pscustomobject]$values
}
$csvResults | Export-Csv -LiteralPath (Join-Path $Directory 'analysis.csv') -NoTypeInformation -Encoding utf8
$results | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath (Join-Path $Directory 'analysis.json') -Encoding utf8
$results | Format-Table file,startDeg,signedPwm,forwardTravelDeg,steadyForwardSpeedDps,slowWindowFraction -AutoSize

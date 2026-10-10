param([Parameter(Mandatory=$true)][string]$Directory)
$ErrorActionPreference='Stop'
function Get-Slope($rows,[string]$field) {
    if($rows.Count -lt 2){return $null}
    $origin=[double]$rows[0].elapsedMs
    $meanTime=($rows | ForEach-Object {([double]$_.elapsedMs-$origin)/1000} | Measure-Object -Average).Average
    $meanPosition=($rows | ForEach-Object {[double]$_.$field/1000} | Measure-Object -Average).Average
    $covariance=0.0; $variance=0.0
    foreach($row in $rows){
        $time=([double]$row.elapsedMs-$origin)/1000-$meanTime
        $covariance+=$time*([double]$row.$field/1000-$meanPosition); $variance+=$time*$time
    }
    if($variance -le 0){return $null}; return $covariance/$variance
}
$results=@(foreach($file in Get-ChildItem -LiteralPath $Directory -Filter '*.csv') {
    if($file.Name -eq 'cruise-analysis.csv'){continue}
    $log=[IO.Path]::ChangeExtension($file.FullName,'.jsonl')
    if(!(Test-Path -LiteralPath $log)){continue}
    $records=Get-Content -LiteralPath $log | ConvertFrom-Json
    $command=$records | Where-Object {$_.direction -eq 'TX' -and $_.type -eq 'BENCH_CRUISE'} | Select-Object -Last 1
    if(!$command){continue}
    $ack=$records | Where-Object {$_.direction -eq 'RX' -and $_.type -eq 'ACK'} | Select-Object -Last 1
    if(!$ack -or $ack.fields[1] -ne 'STOPPED'){throw "Notstopbestaetigung fehlt: $($file.Name)"}
    if(@($records | Where-Object {$_.type -eq 'CRC_BAD' -or ($_.type -eq 'STATUS' -and $_.fields[1] -eq 'FAULT')}).Count){throw 'Ungueltige Messung'}
    $rows=@(Import-Csv -LiteralPath $file.FullName)
    $field=if($command.fields[0] -eq '1'){'tiltMdeg'}else{'panMdeg'}
    $pwm=[int]$command.fields[1]; $direction=if($pwm -gt 0){1}else{-1}
    $cruise=@($rows | Where-Object {[int]$_.stage -eq 2})
    $tail=@($rows | Where-Object {[int]$_.stage -eq 3})
    if(!$cruise.Count -or !$tail.Count){throw "Anlauf/Weiterfahrt/Abschalten nicht vollstaendig: $($file.Name)"}
    $plateau=@($cruise | Where-Object {[int]$_.signedPwm -eq $pwm})
    $steady=@(); $final=@(); $speed=$null; $finalSpeed=$null
    if($plateau.Count){
        $steady=@($plateau | Where-Object {[int]$_.elapsedMs -ge [int]$plateau[0].elapsedMs+300})
        if($steady.Count -ge 11){
            $speed=(Get-Slope $steady $field)*$direction
            $final=@($steady | Where-Object {[int]$_.elapsedMs -ge [int]$steady[-1].elapsedMs-400})
            if($final.Count -ge 11){$finalSpeed=(Get-Slope $final $field)*$direction}
        }
    }
    $duration=if($steady.Count -ge 2){[int]$steady[-1].elapsedMs-[int]$steady[0].elapsedMs}else{0}
    [pscustomobject]@{file=$file.Name;signedPwm=$pwm;startDeg=[double]$rows[0].$field/1000;
        endDeg=[double]$rows[-1].$field/1000;cruiseStartMs=[int]$cruise[0].elapsedMs;
        cruiseTravelDeg=([double]$cruise[-1].$field-[double]$cruise[0].$field)*$direction/1000;
        plateauStartMs=if($plateau.Count){[int]$plateau[0].elapsedMs}else{$null};
        fitDurationMs=$duration;forwardSpeedDps=$speed;finalForwardSpeedDps=$finalSpeed;
        coastTravelDeg=([double]$rows[-1].$field-[double]$tail[0].$field)*$direction/1000;
        withinSlowSpeedBand=if($null -ne $finalSpeed){$finalSpeed -ge 0.5 -and $finalSpeed -le 3.0}else{$null}}
})
if(!$results.Count){throw 'Keine auswertbare Anlaufmessung vorhanden'}
$results | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath (Join-Path $Directory 'cruise-analysis.json') -Encoding utf8
$csv=foreach($row in $results){
    $values=[ordered]@{}
    foreach($property in $row.PSObject.Properties){$values[$property.Name]=if($property.Value -is [double]){$property.Value.ToString('R',[Globalization.CultureInfo]::InvariantCulture)}else{$property.Value}}
    [pscustomobject]$values
}
$csv | Export-Csv -LiteralPath (Join-Path $Directory 'cruise-analysis.csv') -NoTypeInformation -Encoding utf8
$results | Format-Table file,signedPwm,cruiseStartMs,fitDurationMs,forwardSpeedDps,finalForwardSpeedDps,coastTravelDeg -AutoSize

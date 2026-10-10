param(
    [string]$Port='COM3',
    [int[]]$TiltPositionsMdeg=@(-30000,-15000,0,15000),
    [int[]]$PwmLevels=@(15,20,25,30,40),
    [ValidateRange(10,40)][int]$PositioningPwm=40,
    [switch]$Cruise,
    [ValidateSet('up','down')][string[]]$Directions=@('up','down'),
    [string]$OutputDirectory=(Join-Path $PSScriptRoot '../abnahme/2026-10-10/tilt-range'),
    [switch]$Execute
)
$ErrorActionPreference='Stop'
if($TiltPositionsMdeg.Count -lt 1 -or $TiltPositionsMdeg.Count -gt 8 -or
   $PwmLevels.Count -lt 1 -or $PwmLevels.Count -gt 5 -or
   @($PwmLevels | Where-Object {$_ -lt 10 -or $_ -gt 40}).Count) { throw 'Ungueltiger Messplan.' }
if($Cruise -and @($PwmLevels | Where-Object {$_ -gt 30}).Count) { throw 'Weiterfahrtmessung erlaubt maximal PWM 30.' }
if(!$Directions.Count -or @($Directions | Select-Object -Unique).Count -ne $Directions.Count) { throw 'Ungueltige Messrichtungen.' }
$measurementAction=if($Cruise){'Cruise'}else{'Pulse'}
$measurementDuration=if($Cruise){3000}else{1500}
$measurementTravel=if($Cruise){5000}else{3000}
if(!$Execute) {
    [pscustomobject]@{axis='Tilt';positionsMdeg=$TiltPositionsMdeg;pwmLevels=$PwmLevels;
        directions=$Directions;measurementAction=$measurementAction;pulseMs=$measurementDuration;highPwmPulseMs=500;pulseTravelMdeg=$measurementTravel;
        positioningPwm=$PositioningPwm;positioningPulseMs=$(if($PositioningPwm -gt 30){500}else{1500});positioningMaxTravelMdeg=3000;positionToleranceMdeg=2000;
        stopFile=(Join-Path $OutputDirectory 'ABORT');execute=$false} | ConvertTo-Json -Depth 3
    return
}
$output=(New-Item -ItemType Directory -Path $OutputDirectory -Force).FullName
$abort=Join-Path $output 'ABORT'
if(Test-Path -LiteralPath $abort) { throw 'Abbruchdatei vorhanden; keine Fahrt.' }
$test=Join-Path $PSScriptRoot 'Test-MotionHardware.ps1'
$counter=0; $current=$null; $first=$true
$manifest=Join-Path $output 'manifest.jsonl'
if(Test-Path -LiteralPath $manifest) { throw 'Dieses Messverzeichnis enthaelt bereits einen Messplan; neues Verzeichnis verwenden.' }

function Read-LastStatus([string]$path) {
    $records=Get-Content -LiteralPath $path | ConvertFrom-Json
    $ack=$records | Where-Object {$_.direction -eq 'RX' -and $_.type -eq 'ACK'} | Select-Object -Last 1
    if(!$ack -or $ack.fields[1] -ne 'STOPPED') { throw 'Bestaetigter Notstop der Messung fehlt.' }
    $status=$records | Where-Object {$_.direction -eq 'RX' -and $_.type -eq 'STATUS'} | Select-Object -Last 1
    if(!$status -or $status.fields[1] -eq 'FAULT') { throw 'Gueltiger Status fehlt.' }
    return $status.fields
}
function Invoke-Pulse([string]$kind,[int]$nominal,[int]$pwm,[int]$duration,[int]$travel) {
    if(Test-Path -LiteralPath $abort) { throw 'Messserie abgebrochen.' }
    $script:counter++
    $path=Join-Path $output ('{0:d3}-{1}-{2}-{3}.jsonl' -f $script:counter,$kind,$nominal,$pwm)
    $delay=if($script:first){5}else{0}; $script:first=$false
    $action=if($kind -eq 'measurement'){$measurementAction}else{'Pulse'}
    $null = & $test -Port $Port -Action $action -PulseAxis Tilt -SignedPwm $pwm -PulseMs $duration -PulseTravelMdeg $travel -StartDelaySeconds $delay -AbortFile $abort -LogPath $path
    $script:current=Read-LastStatus $path
    Write-Output "Puls $script:counter beendet: Tilt $($script:current[10]) mdeg, Notstop bestaetigt."
    @{index=$script:counter;kind=$kind;nominalTiltMdeg=$nominal;signedPwm=$pwm;pulseMs=$duration;
      travelBoundMdeg=$travel;finalTiltMdeg=[int]$script:current[10];log=[IO.Path]::GetFileName($path)} |
        ConvertTo-Json -Compress | Add-Content -LiteralPath $manifest -Encoding utf8
}
try {
    $preflight=Join-Path $output 'preflight.jsonl'
    & $test -Port $Port -Action Monitor -DurationSeconds 2 -LogPath $preflight
    $records=Get-Content -LiteralPath $preflight | ConvertFrom-Json
    $caps=($records | Where-Object {$_.type -eq 'CAPABILITIES'} | Select-Object -Last 1).fields
    $script:current=($records | Where-Object {$_.type -eq 'STATUS'} | Select-Object -Last 1).fields
    if(!$caps -or $caps[4] -ne '1' -or !$script:current -or $script:current[0] -ne '0' -or
       $script:current[1] -notin @('IDLE','STOPPED')) { throw 'Keine freie Rig fuer Messserie.' }
    foreach($position in $TiltPositionsMdeg) {
        if($position-10000 -lt [int]$caps[2] -or $position+10000 -gt [int]$caps[3]) {
            throw 'Messstellen brauchen Platz fuer beide Pulsrichtungen innerhalb der Limits.'
        }
    }
    @{port=$Port;nominalPositionsMdeg=$TiltPositionsMdeg;pwmLevels=$PwmLevels;directions=$Directions;measurementAction=$measurementAction;pulseMs=$measurementDuration;highPwmPulseMs=500;
      pulseTravelMdeg=$measurementTravel;positioningPwm=$PositioningPwm;positionToleranceMdeg=2000;limits=$caps;startTiltMdeg=[int]$script:current[10]} |
        ConvertTo-Json -Depth 3 | Set-Content -LiteralPath (Join-Path $output 'plan.json') -Encoding utf8
    foreach($position in $TiltPositionsMdeg) {
        Write-Output "Messstelle Tilt $position mdeg."
        foreach($level in $PwmLevels) {
            # Position with separately bounded actuator pulses. This avoids
            # depending on the still uncalibrated position controller.
            $attempt=0
            while([math]::Abs([int]$script:current[10]-$position) -gt 2000) {
                if(++$attempt -gt 20) { throw 'Messstelle nicht innerhalb begrenzter Versuche erreicht.' }
                $before=[int]$script:current[10]; $delta=$position-$before
                $pwm=if($delta -gt 0){$PositioningPwm}else{-$PositioningPwm}
                $travel=[int][math]::Min(3000,[math]::Abs($delta))
                $duration=if($PositioningPwm -gt 30){500}else{1500}
                Invoke-Pulse 'positioning' $position $pwm $duration $travel
                if([math]::Abs([int]$script:current[10]-$before) -lt 250) {
                    throw 'Antrieb ohne ausreichend Fortschritt; keine weitere Leistungserhoehung.'
                }
            }
            foreach($name in $Directions) {
                $direction=if($name -eq 'up'){1}else{-1}
                Write-Output "Puls an Messstelle $position mdeg, PWM=$($level*$direction)/255."
                $duration=if($level -gt 30){500}else{$measurementDuration}
                Invoke-Pulse 'measurement' $position ($level*$direction) $duration $measurementTravel
            }
        }
    }
    Write-Output 'Messserie abgeschlossen; alle Messpulse mit bestaetigtem Notstop beendet.'
}
finally {
    & $test -Port $Port -Action Stop -DurationSeconds 2 -LogPath (Join-Path $output 'final-stop.jsonl')
}

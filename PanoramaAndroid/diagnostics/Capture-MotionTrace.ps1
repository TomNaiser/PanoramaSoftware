param(
    [string]$Port='COM3',
    [ValidateRange(5,3600)][int]$DurationSeconds=75,
    [Parameter(Mandatory=$true)][string]$LogPath,
    [string]$StopFile='',
    [switch]$AllowSensorFaults
)
# Read-only USB sideband of the explicit Bluetooth trace build. This script
# never writes serial bytes and cannot request movement or camera exposure.
$ErrorActionPreference='Stop'
$path=[IO.Path]::GetFullPath($LogPath)
if(Test-Path -LiteralPath $path){throw 'Vorhandene Messspur wird nicht ueberschrieben.'}
if($StopFile -and (Test-Path -LiteralPath $StopFile)){throw 'Stopdatei der Messspur ist bereits vorhanden.'}
$serial=[IO.Ports.SerialPort]::new($Port,115200,[IO.Ports.Parity]::None,8,[IO.Ports.StopBits]::One)
$serial.DtrEnable=$true; $serial.RtsEnable=$false
$writer=$null; $rawWriter=$null; $buffer=''
$timer=[Diagnostics.Stopwatch]::new()
$count=0; $bad=0; $lost=0; $lastSequence=-1L; $lastMs=-1L
$sideband=@()
$maxGap=0L; $maxActiveGap=0L; $lastActive=$false; $lastDrop=0L; $invalidSensor=0
try {
    $writer=[IO.StreamWriter]::new($path,$false,[Text.UTF8Encoding]::new($false))
    $rawWriter=[IO.StreamWriter]::new([IO.Path]::ChangeExtension($path,'.log'),$false,[Text.UTF8Encoding]::new($false))
    $writer.WriteLine('sequence,sampleMs,tiltMdeg,targetMdeg,velocityMdegPerSecond,shortVelocityMdegPerSecond,quietVelocityMdegPerSecond,integralMilliPwm,controllerPwm,appliedPwm,stage,enabled,sensorsValid,dropped,panMdeg,panTargetMdeg,panReferenceMdeg,panVelocityMdegPerSecond,panReferenceVelocityMdegPerSecond,panShortVelocityMdegPerSecond,panQuietVelocityMdegPerSecond,panIntegralMilliPwm,panControllerPwm,panAppliedPwm,panPhase,panMeanMdeg,panProfileTimeMs,panProgressAgeMs,panReversing,sensorFlags,panReadError,tiltReadError,panReadFailures,tiltReadFailures,motionSequence,motionSampleMs,busLevels,busRecoveries,hostElapsedMs')
    $serial.Open(); $timer.Start()
    Write-Output "USB-Messspur auf $Port bereit; ausschliesslich Empfang."
    while($timer.ElapsedMilliseconds -lt $DurationSeconds*1000) {
        # Complete any partially received line before honoring the owner stop.
        if($StopFile -and (Test-Path -LiteralPath $StopFile) -and !$buffer.Trim()){break}
        $buffer+=$serial.ReadExisting()
        while($buffer.Contains("`n")) {
            $end=$buffer.IndexOf("`n"); $line=$buffer.Substring(0,$end).Trim(); $buffer=$buffer.Substring($end+1)
            if(!$line){continue}
            $rawWriter.WriteLine($line)
            if($line -match '^\[DEBUG-BUS(?:-ACK)?\],\d+(?:,\d+)+$' -or
               ($count -eq 0 -and $line -match '^I2C error: [0-6]$')) {
                $sideband+=$line; continue
            }
            $fields=$line.Split(',')
            $legacy=$fields.Count -eq 15 -and $fields[0] -ceq '[DEBUG-MT1]'
            $dual=$fields.Count -eq 37 -and $fields[0] -ceq '[DEBUG-MT2]'
            $bus=$fields.Count -eq 39 -and $fields[0] -ceq '[DEBUG-MT3]'
            if(!$legacy -and !$dual -and !$bus){++$bad;continue}
            $numbers=@(); $valid=$true
            for($index=1;$index -lt $fields.Count;++$index) {
                $parsed=0L
                if(![long]::TryParse($fields[$index],[ref]$parsed)){$valid=$false;break}
                $numbers+=$parsed
            }
            if(!$valid){++$bad;continue}
            $sequence=$numbers[0]; $sampleMs=$numbers[1]; $active=$numbers[11] -eq 1
            if($lastSequence -ge 0) {
                if($sequence -le $lastSequence -or $sampleMs -le $lastMs){throw 'Messfolge wurde zurueckgesetzt oder ist nicht zeitlich aufsteigend.'}
                $lost+=$sequence-$lastSequence-1
                $gap=$sampleMs-$lastMs; $maxGap=[math]::Max($maxGap,$gap)
                if($active -and $lastActive){$maxActiveGap=[math]::Max($maxActiveGap,$gap)}
            }
            if($numbers[12] -ne 1){++$invalidSensor}
            $lastSequence=$sequence; $lastMs=$sampleMs; $lastActive=$active; $lastDrop=$numbers[13]
            $values=$fields[1..($fields.Count-1)]
            if($legacy){$values+=@('')*24}
            elseif($dual){$values+=@('')*2}
            $writer.WriteLine(($values -join ',')+','+$timer.ElapsedMilliseconds)
            ++$count
        }
        $writer.Flush(); $rawWriter.Flush()
        if($buffer.Length -gt 4096){throw 'Unvollstaendiger USB-Empfangspuffer zu gross.'}
        Start-Sleep -Milliseconds 5
    }
    if($buffer.Trim()) { $rawWriter.WriteLine($buffer); ++$bad }
    $summary=[pscustomobject]@{port=$Port;records=$count;malformedLines=$bad;lostSequences=$lost;
        deviceDropped=$lastDrop;maximumSampleGapMs=$maxGap;maximumActiveSampleGapMs=$maxActiveGap;
        invalidSensorSamples=$invalidSensor;sensorFaultCapture=[bool]$AllowSensorFaults;sideband=$sideband;log=$path}
    $summary | ConvertTo-Json | Set-Content -LiteralPath ([IO.Path]::ChangeExtension($path,'.summary.json')) -Encoding utf8
    $summary | ConvertTo-Json
    if($count -lt 20 -or $bad -or $lost -or $lastDrop -or ($invalidSensor -and !$AllowSensorFaults)){throw 'USB-Spur ist nicht vollstaendig bzw. fehlerfrei.'}
}
finally {
    if($serial.IsOpen){$serial.Close()}; $serial.Dispose()
    if($writer){$writer.Dispose()}; if($rawWriter){$rawWriter.Dispose()}
}

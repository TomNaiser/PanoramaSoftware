param(
    [Parameter(Mandatory=$true)][ValidateRange(-10000,10000)][int]$PanDeltaMdeg,
    [Parameter(Mandatory=$true)][string]$OutputPrefix,
    [ValidateRange(1,1000)][int]$Tempo=1000,
    [ValidatePattern('^2\.4\.\d+$')][string]$ExpectedFirmware='2.4.67'
)
# Characterization only: preserve a bounded TARGET_MISSED as measured data.
# Sensor/stall/transport failures stop the experiment. Every attempt must
# have a confirmed ESTOP before RAM is requested or another case can run.
$ErrorActionPreference='Stop'
$prefix=[IO.Path]::GetFullPath($OutputPrefix);$log=$prefix+'.jsonl';$csv=$prefix+'.csv'
foreach($path in @($log,$csv,$prefix+'.measurement.json')){if(Test-Path -LiteralPath $path){throw 'Messung existiert bereits.'}}
$failure=''
try {
    & (Join-Path $PSScriptRoot 'Test-MotionHardware.ps1') -Action Move -PanDeltaMdeg $PanDeltaMdeg `
        -MaximumStepDegrees ([int][math]::Ceiling([math]::Abs($PanDeltaMdeg)/1000.0)) -Tempo $Tempo `
        -DurationSeconds 12 -WaitForArrival -DwellMs 500 -StartDelaySeconds 3 -LogPath $log | Out-Null
} catch {
    $failure=$_.Exception.Message
    if($failure -notin @('Geraetefehler: MOTION_TARGET_MISSED.','Zielruhe und Haltezeit wurden innerhalb des Testbudgets nicht bestaetigt.')){throw}
}
$records=@(Get-Content -LiteralPath $log | ConvertFrom-Json)
$info=$records | Where-Object type -eq 'DEVICE_INFO' | Select-Object -First 1
$status=$records | Where-Object {$_.direction -eq 'RX' -and $_.type -eq 'STATUS'} | Select-Object -Last 1
$stop=$records | Where-Object {$_.direction -eq 'TX' -and $_.type -eq 'ESTOP'} | Select-Object -Last 1
$ack=$records | Where-Object {$_.direction -eq 'RX' -and $_.type -eq 'ACK' -and $_.fields[1] -eq 'STOPPED' -and $_.elapsedMs -ge $stop.elapsedMs} | Select-Object -Last 1
$move=$records | Where-Object {$_.direction -eq 'TX' -and $_.type -eq 'MANUAL_MOVE'} | Select-Object -First 1
if(!$info -or $info.fields[0] -ne $ExpectedFirmware -or $info.fields[3] -ne 'ARDUINO_NANO_R4_BT_BUFFERED_TRACE' -or !$status -or !$stop -or !$ack -or !$move -or
    $status.fields[0] -ne '0' -or $status.fields[1] -ne 'STOPPED' -or $status.fields[13] -ne 'PC_TEST_END') {throw 'Messfahrt ohne passende Firmware oder bestaetigten Abschaltzustand.'}
if(@($records | Where-Object {$_.type -in @('CRC_BAD','NACK')}).Count){throw 'Transportfehler; Messreihe gestoppt.'}
& (Join-Path $PSScriptRoot 'Capture-BufferedPanTrace.ps1') -LogPath $csv | Out-Null
$summary=Get-Content -LiteralPath ($prefix+'.summary.json') -Raw | ConvertFrom-Json
$errorMdeg=[int]$summary.lastPositionMdeg-[int]$move.fields[0]
if([math]::Abs($errorMdeg) -gt 1000){throw 'Zielabweichung groesser als ein Grad; Messreihe gestoppt.'}
[ordered]@{file=[IO.Path]::GetFileName($csv);panDeltaMdeg=$PanDeltaMdeg;tempo=$Tempo;targetMdeg=[int]$move.fields[0];
    endpointErrorMdeg=$errorMdeg;targetMiss=($failure -eq 'Geraetefehler: MOTION_TARGET_MISSED.');arrivalDeadlineExceeded=($failure -like 'Zielruhe*');firmware=$info.fields;records=$summary.records;configRevision=$summary.configRevision;tiltMdeg=$summary.tiltMdeg;crcVerified=$summary.integrityVerified} |
    ConvertTo-Json -Depth 4 | Set-Content -LiteralPath ($prefix+'.measurement.json') -Encoding utf8
Write-Output ('Gemessen und gestoppt: {0}, Endabweichung {1} mdeg, {2} Proben, Zielabweichungsfehler={3}' -f [IO.Path]::GetFileName($prefix),$errorMdeg,$summary.records,[bool]$failure)

param(
    [string]$ControlPort='COM4',
    [string]$TracePort='COM3',
    [ValidateRange(-10000,10000)][int]$PanDeltaMdeg=0,
    [ValidateRange(-10000,10000)][int]$TiltDeltaMdeg=0,
    [ValidateRange(1,1000)][int]$Tempo=500,
    [ValidateRange(0,10)][int]$StartDelaySeconds=5,
    [Parameter(Mandatory=$true)][string]$OutputPrefix,
    [string]$AbortFile=''
)
# One bounded move, Bluetooth control and independent read-only USB capture.
# Test-MotionHardware owns preflight, limits, arrival and the final ESTOP.
$ErrorActionPreference='Stop'
if(!$PanDeltaMdeg -and !$TiltDeltaMdeg){throw 'Mindestens eine Achse muss einen Weg haben.'}
$prefix=[IO.Path]::GetFullPath($OutputPrefix)
$csv=$prefix+'.csv'; $stop=$prefix+'.stop'; $log=$prefix+'.jsonl'
foreach($path in @($csv,$stop,$log)) {
    if(Test-Path -LiteralPath $path){throw 'Vorhandener Messlauf wird nicht ueberschrieben.'}
}
$job=Start-Job -ScriptBlock {
    param($script,$port,$path,$stopFile)
    & $script -Port $port -DurationSeconds 90 -LogPath $path -StopFile $stopFile
} -ArgumentList (Join-Path $PSScriptRoot 'Capture-MotionTrace.ps1'),$TracePort,$csv,$stop
try {
    $ready=$false; $timer=[Diagnostics.Stopwatch]::StartNew()
    while($timer.ElapsedMilliseconds -lt 10000) {
        if($job.State -in @('Failed','Completed','Stopped')){throw 'Messspur hat vor der Fahrt geendet.'}
        if(Test-Path -LiteralPath $csv) {
            $line=Get-Content -LiteralPath $csv -Tail 1
            if($line -match '^\d+,') {
                $fields=$line.Split(',')
                if($fields.Count -eq 39 -and $fields[12] -eq '1' -and $fields[29] -eq '3'){$ready=$true;break}
            }
        }
        Start-Sleep -Milliseconds 50
    }
    if(!$ready){throw 'Keine frischen gueltigen Reglerproben beider Achsen; keine Fahrt.'}
    $preflight=$prefix+'.preflight.jsonl'
    & (Join-Path $PSScriptRoot 'Test-MotionHardware.ps1') -Port $ControlPort -Action Monitor -DurationSeconds 2 -LogPath $preflight | Out-Null
    $preflightFrames=@(Get-Content -LiteralPath $preflight | ConvertFrom-Json)
    if(@($preflightFrames | Where-Object {$_.type -in @('CRC_BAD','NACK')}).Count){throw 'Messverbindung nicht fehlerfrei; keine Fahrt.'}
    $maximum=[int][math]::Ceiling([math]::Max([math]::Abs($PanDeltaMdeg),[math]::Abs($TiltDeltaMdeg))/1000.0)
    & (Join-Path $PSScriptRoot 'Test-MotionHardware.ps1') -Port $ControlPort -Action Move `
        -PanDeltaMdeg $PanDeltaMdeg -TiltDeltaMdeg $TiltDeltaMdeg -MaximumStepDegrees $maximum `
        -Tempo $Tempo -DurationSeconds 15 -WaitForArrival -DwellMs 0 `
        -StartDelaySeconds $StartDelaySeconds -DiagnosticIntervalMs 150 -AbortFile $AbortFile -LogPath $log
    $controlFrames=@(Get-Content -LiteralPath $log | ConvertFrom-Json)
    if(@($controlFrames | Where-Object {$_.type -in @('CRC_BAD','NACK')}).Count){throw 'Messfahrt mit Transportfehler; keine Modellidentifikation aus dieser Spur.'}
}
finally {
    Set-Content -LiteralPath $stop -Value 'CAPTURE_FINISHED' -Encoding ascii
    $null=Wait-Job -Job $job -Timeout 10
    if($job.State -eq 'Running'){Stop-Job -Job $job;throw 'Messspur beendet sich nicht rechtzeitig.'}
    try { Receive-Job -Job $job -ErrorAction Stop } finally { Remove-Job -Job $job }
}

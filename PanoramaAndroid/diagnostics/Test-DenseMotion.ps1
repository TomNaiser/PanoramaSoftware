param(
    [ValidateSet('Up','DownUp')][string]$Sequence='Up',
    [string]$ControlPort='COM4',
    [string]$TracePort='COM3',
    [Parameter(Mandatory=$true)][string]$OutputPrefix,
    [string]$AbortFile=''
)
# The normal bounded hardware test owns all movement and ESTOP handling.
# A read-only USB job is started first, checked, and stopped only after the
# last motion script has returned (including its independent finally stop).
$ErrorActionPreference='Stop'
$prefix=[IO.Path]::GetFullPath($OutputPrefix)
$csv=$prefix+'.csv'; $stop=$prefix+'.stop'
if((Test-Path -LiteralPath $csv) -or (Test-Path -LiteralPath $stop)) { throw 'Vorhandener Messlauf wird nicht ueberschrieben.' }
$capturePath=Join-Path $PSScriptRoot 'Capture-MotionTrace.ps1'
$job=Start-Job -ScriptBlock {
    param($script,$port,$log,$stopFile)
    & $script -Port $port -DurationSeconds 120 -LogPath $log -StopFile $stopFile
} -ArgumentList $capturePath,$TracePort,$csv,$stop
try {
    $timer=[Diagnostics.Stopwatch]::StartNew(); $ready=$false
    while($timer.ElapsedMilliseconds -lt 10000) {
        if($job.State -in @('Failed','Completed','Stopped')) { throw 'USB-Aufzeichnung hat vor der Fahrt geendet.' }
        if(Test-Path -LiteralPath $csv) {
            $head=@(Get-Content -LiteralPath $csv -TotalCount 2)
            if($head.Count -eq 2 -and $head[1] -match '^\d+,') { $ready=$true;break }
        }
        Start-Sleep -Milliseconds 50
    }
    if(!$ready){throw 'Keine bestaetigten USB-Reglerproben; keine Fahrt.'}
    Write-Output 'USB-Reglerproben bestaetigt; Aufnahme bleibt bis zum Fahrtende aktiv.'
    $steps=if($Sequence -eq 'DownUp'){@(-10000,10000)}else{@(10000)}
    foreach($step in $steps) {
        $direction=if($step -gt 0){'up'}else{'down'}
        $delay=if($step -gt 0){10}else{5}
        & (Join-Path $PSScriptRoot 'Test-MotionHardware.ps1') -Port $ControlPort -Action Move `
            -TiltDeltaMdeg $step -MaximumStepDegrees 10 -Tempo 500 -DurationSeconds 10 `
            -StartDelaySeconds $delay -DiagnosticIntervalMs 150 -AbortFile $AbortFile `
            -LogPath ($prefix+'-'+$direction+'-10deg.jsonl')
        & (Join-Path $PSScriptRoot 'Analyze-RegulatedMotion.ps1') -Directory ([IO.Path]::GetDirectoryName($prefix)) `
            -FileName ([IO.Path]::GetFileName($prefix)+'-'+$direction+'-10deg.jsonl') `
            -FilePrefix ([IO.Path]::GetFileName($prefix))
    }
}
finally {
    Set-Content -LiteralPath $stop -Value 'CAPTURE_FINISHED' -Encoding ascii
    $null=Wait-Job -Job $job -Timeout 10
    if($job.State -eq 'Running'){Stop-Job -Job $job;throw 'USB-Aufzeichnung beendet sich nicht rechtzeitig.'}
    try { Receive-Job -Job $job -ErrorAction Stop } finally { Remove-Job -Job $job }
}

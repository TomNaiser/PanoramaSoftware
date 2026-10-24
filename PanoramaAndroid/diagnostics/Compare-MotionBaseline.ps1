param(
    [string]$RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path,
    [string]$BaselineCommit = ''
)
$ErrorActionPreference = 'Stop'

# Static differential check. It verifies source/parameter differences, not
# physical damping or the settling time of the mounted camera.
function Read-GitFile([string]$directory, [string]$revision, [string]$path) {
    $lines = & git -C $directory show "${revision}:$path"
    if ($LASTEXITCODE -ne 0) { throw "Git source missing: $path" }
    return $lines -join "`n"
}
function Function-Code([string]$source, [string]$name, [switch]$Optional) {
    $pattern = '(?ms)^(?:void|double) ' + [regex]::Escape($name) + '\([^\r\n]*\)\s*\{.*?^\}'
    $code = [regex]::Match($source, $pattern).Value
    if (!$code) { if ($Optional) { return $null }; throw "Function missing: $name" }
    $code = [regex]::Replace($code, '//[^\r\n]*|/\*[\s\S]*?\*/', '')
    return [regex]::Replace($code, '\s', '')
}
function Number([string]$source, [string]$pattern) {
    $match = [regex]::Match($source, $pattern)
    if (!$match.Success) { throw "Parameter missing: $pattern" }
    return [double]::Parse($match.Groups[1].Value, [Globalization.CultureInfo]::InvariantCulture)
}

$firmwareDirectory = Join-Path $RepositoryRoot 'PanoduinoControlGitRepo'
$pcDirectory = Join-Path $RepositoryRoot 'PanoramaControlGitRepo'
if (!$BaselineCommit) {
    $roots = @(& git -C $RepositoryRoot rev-list --max-parents=0 HEAD)
    if ($LASTEXITCODE -ne 0 -or $roots.Count -ne 1) {
        throw 'Kein eindeutiger Anfangsstand. -BaselineCommit explizit angeben.'
    }
    $BaselineCommit = $roots[0]
}
$initialCommit = $BaselineCommit
$firmwareLink = & git -C $RepositoryRoot ls-tree $initialCommit PanoduinoControlGitRepo
$pcLink = & git -C $RepositoryRoot ls-tree $initialCommit PanoramaControlGitRepo
$firmwareRevision = ($firmwareLink -split '\s+')[2]
$pcRevision = ($pcLink -split '\s+')[2]
$oldFirmware = Read-GitFile $firmwareDirectory $firmwareRevision 'PanoDuinoControl/PanoDuinoControl.ino'
$oldClient = Read-GitFile $pcDirectory $pcRevision 'PanoramaControl/muControllerInterface.cs'
$oldParameters = Read-GitFile $pcDirectory $pcRevision 'PanoramaControl/Params.cs'
$currentFirmware = Get-Content (Join-Path $firmwareDirectory 'PanoDuinoControl/PanoDuinoControl.ino') -Raw
$controller = Get-Content (Join-Path $firmwareDirectory 'PanoDuinoControl/V2Controller.cpp') -Raw

$profiled = $currentFirmware.Contains('#include "MotionController.h"')
$functions = foreach ($name in @('CalculatePID_x','CalculatePID_y','mse_x','mse_y','ResetMovement_x','ResetMovement_y')) {
    $previousCode = Function-Code $oldFirmware $name
    $currentCode = Function-Code $currentFirmware $name -Optional:$profiled
    [pscustomobject]@{ function=$name; identical=$previousCode -ceq $currentCode; status=$(if (!$currentCode) { 'replaced-by-profile-controller' } elseif ($previousCode -ceq $currentCode) { 'unchanged' } else { 'modified' }) }
}
$oldPanCutoff = Number $oldClient 'SetShutDownThreshold\(COMPortName, BTbaudrate, 0, ([\d.]+)\)'
$oldTiltCutoff = Number $oldClient 'SetShutDownThreshold\(COMPortName, BTbaudrate, 1, ([\d.]+)\)'
$panCutoff = if ($profiled) { $null } else { Number $currentFirmware 'float ShutDownThreshold_x\s*=\s*([\d.]+)' }
$tiltCutoff = if ($profiled) { $null } else { Number $currentFirmware 'float ShutDownThreshold_y\s*=\s*([\d.]+)' }

# mse_x/y sum nine squared historical errors; they do not calculate a mean.
# A constant 0.5-degree error gives 9 * 0.5^2 = 2.25. With otherwise active
# motion this selected PWM=0 in 2.3.7. The new profile controller does not use mse.
$exampleSquaredErrorSum = 9 * 0.5 * 0.5
[pscustomobject]@{
    initialCommit=$initialCommit
    firmwareRevision=$firmwareRevision
    pcRevision=$pcRevision
    currentRegulation=$(if ($profiled) { 'profiled-2.4' } else { 'legacy-pid' })
    functions=$functions
    effectiveCutoff=@{ initialPan=$oldPanCutoff; initialTilt=$oldTiltCutoff; currentPan=$panCutoff; currentTilt=$tiltCutoff }
    steadyHalfDegreeExample=@{
        squaredErrorSum=$exampleSquaredErrorSum
        initialPanPwmSuppressed=$exampleSquaredErrorSum -lt $oldPanCutoff
        initialTiltPwmSuppressed=$exampleSquaredErrorSum -lt $oldTiltCutoff
        currentPanPwmSuppressed=$(if ($profiled) { $null } else { $exampleSquaredErrorSum -lt $panCutoff })
        currentTiltPwmSuppressed=$(if ($profiled) { $null } else { $exampleSquaredErrorSum -lt $tiltCutoff })
    }
    initialPcSpeed=@{
        pan=Number $oldParameters 'ServoSpeedX\s*=\s*([\d.]+)'
        tilt=Number $oldParameters 'ServoSpeedY\s*=\s*([\d.]+)'
    }
    currentManualForcesFullSpeed=$controller.Contains('_hardware.setTargetMdeg(pan, tilt, 1000, 1000, true)')
} | ConvertTo-Json -Depth 5

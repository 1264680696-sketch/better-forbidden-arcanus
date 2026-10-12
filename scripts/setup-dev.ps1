param([string]$InstanceDirectory = (Resolve-Path "$PSScriptRoot/../../..").Path)
$ErrorActionPreference = 'Stop'
$taskProject = (Resolve-Path "$PSScriptRoot/..").Path
$taskLibs = Join-Path $taskProject 'libs'
New-Item -ItemType Directory -Path $taskLibs -Force | Out-Null
foreach ($taskPattern in @('*forbidden_arcanus-2.6.1.jar', 'valhelsia_core-neoforge-1.21.1-1.1.5.jar')) {
    $taskFiles = @(Get-ChildItem -LiteralPath (Join-Path $InstanceDirectory 'mods') -Filter $taskPattern -File)
    if ($taskFiles.Count -ne 1) { throw "Expected exactly one dependency matching $taskPattern" }
    Copy-Item -LiteralPath $taskFiles[0].FullName -Destination $taskLibs
    Write-Output "Development dependency: $($taskFiles[0].Name)"
}

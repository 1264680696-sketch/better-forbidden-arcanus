param([int]$Port = 18741)
$ErrorActionPreference = 'Stop'
$taskUrl = "http://127.0.0.1:$Port"
$taskExistingEditor = $null
try { $taskExistingEditor = Invoke-RestMethod "$taskUrl/api/document" -TimeoutSec 2 } catch { }
if ($taskExistingEditor -and $taskExistingEditor.bookId -eq 'better_forbidden_arcanus:forbidden_arcanus') {
    Write-Output "Editor already running: $taskUrl"
    Start-Process -FilePath $taskUrl
    exit 0
}
$taskPythonCommand = Get-Command python -ErrorAction SilentlyContinue
$taskBundledPython = Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe'
if (Test-Path -LiteralPath $taskBundledPython) {
    $taskPython = $taskBundledPython
} elseif ($taskPythonCommand) {
    $taskPython = $taskPythonCommand.Source
} else {
    throw '需要 Python 3.10 或更新版本。请安装 Python 后重新打开编辑器。'
}
& $taskPython "$PSScriptRoot/server.py" --port $Port --open
if ($LASTEXITCODE -ne 0) { throw '编辑器未能启动。请查看上面的提示。' }

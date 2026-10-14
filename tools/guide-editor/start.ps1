param([int]$Port = 18741, [switch]$NoBrowser)
$ErrorActionPreference = 'Stop'
$taskRoot = (Resolve-Path "$PSScriptRoot/../..").Path
$taskUrl = "http://127.0.0.1:$Port"
$taskExistingEditor = $null
try { $taskExistingEditor = Invoke-RestMethod "$taskUrl/api/document" -TimeoutSec 2 } catch { }
if ($taskExistingEditor -and $taskExistingEditor.bookId -eq 'better_forbidden_arcanus:forbidden_arcanus') {
    if ($taskExistingEditor.projectRoot -and $taskExistingEditor.projectRoot -ne $taskRoot) { throw '此端口是另一个工程的编辑器，请使用 -Port 18742。' }
    Write-Output "Editor already running: $taskUrl"
    if (-not $NoBrowser) { Start-Process -FilePath $taskUrl }
    exit 0
}
$taskBundledPython = Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe'
$taskPythonCommand = Get-Command python -ErrorAction SilentlyContinue
if (Test-Path -LiteralPath $taskBundledPython) { $taskPython = $taskBundledPython }
elseif ($taskPythonCommand) { $taskPython = $taskPythonCommand.Source }
else { throw '需要 Python 3.10 或更新版本。请安装 Python 后重新打开编辑器。' }
$taskRuntime = Join-Path $PSScriptRoot '.runtime'
New-Item -ItemType Directory -Path $taskRuntime -Force | Out-Null
$taskServerPath = Join-Path $PSScriptRoot 'server.py'
$taskServerProcess = Start-Process -FilePath $taskPython -ArgumentList @('-u', ('"{0}"' -f $taskServerPath), '--port', "$Port") -WindowStyle Hidden -WorkingDirectory $taskRoot -RedirectStandardOutput (Join-Path $taskRuntime "server-$Port.log") -RedirectStandardError (Join-Path $taskRuntime "server-$Port-error.log") -PassThru
for ($taskAttempt = 0; $taskAttempt -lt 50; $taskAttempt++) {
    Start-Sleep -Milliseconds 200
    if ($taskServerProcess.HasExited) { throw ('编辑器未能启动：' + (Get-Content -LiteralPath (Join-Path $taskRuntime "server-$Port-error.log") -Raw)) }
    try { $taskReady = Invoke-RestMethod "$taskUrl/api/document" -TimeoutSec 1 } catch { continue }
    if ($taskReady.bookId -eq 'better_forbidden_arcanus:forbidden_arcanus' -and $taskReady.projectRoot -eq $taskRoot) {
        Set-Content -LiteralPath (Join-Path $taskRuntime "server-$Port.pid") -Value $taskServerProcess.Id -Encoding ascii
        Write-Output "Editor running in background: $taskUrl"
        Write-Output '关闭这个窗口不会停止编辑器。需要停止时双击“关闭指南编辑器.bat”。'
        if (-not $NoBrowser) { Start-Process -FilePath $taskUrl }
        exit 0
    }
}
throw '编辑器启动超时，请查看 tools/guide-editor/.runtime 中的日志。'

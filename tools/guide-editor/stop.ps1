param([int]$Port = 18741)
$ErrorActionPreference = 'Stop'
$taskRecord = Join-Path $PSScriptRoot ".runtime/server-$Port.pid"
if (-not (Test-Path -LiteralPath $taskRecord)) { Write-Output '没有本编辑器的后台进程记录。'; exit 0 }
$taskServerPid = [int](Get-Content -LiteralPath $taskRecord -Raw).Trim()
$taskProcess = Get-CimInstance Win32_Process -Filter "ProcessId = $taskServerPid" -ErrorAction SilentlyContinue
if (-not $taskProcess) { Remove-Item -LiteralPath $taskRecord; Write-Output '编辑器已经停止。'; exit 0 }
$taskServerPath = Join-Path $PSScriptRoot 'server.py'
if (-not $taskProcess.CommandLine.Contains($taskServerPath) -or $taskProcess.CommandLine -notmatch "--port\s+$Port(?:\s|$)") { throw '进程记录不匹配，没有停止任何其他程序。' }
Stop-Process -Id $taskServerPid
Remove-Item -LiteralPath $taskRecord
Write-Output '指南编辑器已停止。工程文件不受影响。'

param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Tasks = @('build'))
$ErrorActionPreference = 'Stop'
$taskProject = (Resolve-Path "$PSScriptRoot/..").Path
$taskJdk = if ($env:BFA_JDK_HOME) { $env:BFA_JDK_HOME } else { 'C:\Program Files\Java\jdk-21' }
if (-not (Test-Path -LiteralPath (Join-Path $taskJdk 'bin/java.exe'))) { throw 'Set BFA_JDK_HOME to a JDK 21 directory.' }
$env:JAVA_HOME = $taskJdk
$env:GRADLE_USER_HOME = Join-Path $taskProject '.gradle-user-home'
# Windows AF_UNIX sockets have a short path limit; the desktop sandbox's TEMP
# exceeds it once Java appends its selector socket filename.
$taskTemp = Join-Path (Resolve-Path "$taskProject/../..").Path '.bfa-tmp'
New-Item -ItemType Directory -Path $taskTemp -Force | Out-Null
$env:TEMP = $taskTemp
$env:TMP = $taskTemp
if ($Tasks -contains 'runClientSmoke') {
    # A fresh client would otherwise wait indefinitely at its first-run
    # accessibility onboarding screen before reaching the title screen.
    $taskSmokeDirectory = Join-Path $taskProject 'runs/client-smoke'
    New-Item -ItemType Directory -Path $taskSmokeDirectory -Force | Out-Null
    $taskOptionsPath = Join-Path $taskSmokeDirectory 'options.txt'
    if (-not (Test-Path -LiteralPath $taskOptionsPath)) {
        [System.IO.File]::WriteAllText($taskOptionsPath, "onboardAccessibility:false`n", [System.Text.UTF8Encoding]::new($false))
    }
}
# Java does not automatically use the Windows proxy that browsers/Python use.
$taskProxy = Get-ItemProperty -LiteralPath 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings' -ErrorAction SilentlyContinue
if ($taskProxy.ProxyEnable -eq 1 -and $taskProxy.ProxyServer -match '^(?:https?://)?([\w.\-]+):(\d+)$') {
    $taskProxyHost = $Matches[1]
    $taskProxyPort = $Matches[2]
    $env:GRADLE_OPTS = "$env:GRADLE_OPTS -Dhttp.proxyHost=$taskProxyHost -Dhttp.proxyPort=$taskProxyPort -Dhttps.proxyHost=$taskProxyHost -Dhttps.proxyPort=$taskProxyPort"
}
Push-Location $taskProject
try {
    & (Join-Path $taskProject 'gradlew.bat') @Tasks
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally { Pop-Location }

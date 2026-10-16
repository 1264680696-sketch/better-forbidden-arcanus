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
foreach ($taskClientRun in @('runClientSmoke', 'runClientGuide', 'runClientBook')) {
    if ($Tasks -notcontains $taskClientRun) { continue }
    # A fresh client would otherwise wait indefinitely at its first-run
    # accessibility onboarding screen before reaching the title screen.
    $taskSmokeDirectory = Join-Path $taskProject $(if ($taskClientRun -eq 'runClientGuide') { 'runs/client-guide' } elseif ($taskClientRun -eq 'runClientBook') { 'runs/client-book' } else { 'runs/client-smoke' })
    New-Item -ItemType Directory -Path $taskSmokeDirectory -Force | Out-Null
    $taskOptionsPath = Join-Path $taskSmokeDirectory 'options.txt'
    if (-not (Test-Path -LiteralPath $taskOptionsPath)) {
        [System.IO.File]::WriteAllText($taskOptionsPath, "onboardAccessibility:false`nlang:zh_cn`nguiScale:2`nrenderDistance:2`nmaxFps:30`ntutorialStep:none`n", [System.Text.UTF8Encoding]::new($false))
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
    # Some mod-loading failures exit Java with code zero. Require the game's own
    # completion marker instead of trusting Gradle's process exit alone.
    if ($Tasks -contains 'runGameTestServer') {
        $taskTestLog = Join-Path $taskProject 'runs/gametest/logs/latest.log'
        if (-not (Test-Path -LiteralPath $taskTestLog) -or
            -not (Select-String -LiteralPath $taskTestLog -Pattern 'All \d+ required tests passed' -Quiet)) {
            throw 'Game tests did not reach successful completion. Check runs/gametest/logs/latest.log.'
        }
    }
    if ($Tasks -contains 'runClientSmoke') {
        $taskClientLog = Join-Path $taskProject 'runs/client-smoke/logs/latest.log'
        if (-not (Test-Path -LiteralPath $taskClientLog) -or
            -not (Select-String -LiteralPath $taskClientLog -SimpleMatch 'BFA_CLIENT_SMOKE_PASS' -Quiet)) {
            throw 'Client smoke test did not reach its completion marker. Check runs/client-smoke/logs/latest.log.'
        }
    }
    if ($Tasks -contains 'runClientGuide') {
        $taskGuideLog = Join-Path $taskProject 'runs/client-guide/logs/latest.log'
        if (-not (Test-Path -LiteralPath $taskGuideLog) -or
            -not (Select-String -LiteralPath $taskGuideLog -SimpleMatch 'BFA_GUIDE_PASS' -Quiet)) {
            throw 'Guide check did not finish. Check runs/client-guide/logs/latest.log.'
        }
    }
    if ($Tasks -contains 'runClientBook') {
        $taskBookLog = Join-Path $taskProject 'runs/client-book/logs/latest.log'
        if (-not (Test-Path -LiteralPath $taskBookLog) -or
            -not (Select-String -LiteralPath $taskBookLog -SimpleMatch 'BFA_LEGACY_BOOK_PASS' -Quiet)) {
            throw 'Legacy book check did not finish. Check runs/client-book/logs/latest.log.'
        }
    }
} finally { Pop-Location }

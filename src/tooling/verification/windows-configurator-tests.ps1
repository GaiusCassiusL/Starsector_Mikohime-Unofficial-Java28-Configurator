[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))
$helper = Join-Path $repoRoot 'configurator\windows\Configurator.Helpers.ps1'
$profiles = Join-Path $repoRoot 'configurator\shared\jvm'
$work = Join-Path $repoRoot 'src\build\windows-config-tests'
$passed = 0

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) {
        throw "FAILED: $Message"
    }
    $script:passed++
    Write-Host "  [PASS] $Message"
}

function Invoke-Helper {
    param([string]$Action)
    $output = & $helper -Action $Action
    if (-not $?) {
        throw "Helper action failed: $Action"
    }
    return $output
}

try {
    Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Path $work | Out-Null

    $tokens = $null
    $parseErrors = $null
    [void][Management.Automation.Language.Parser]::ParseFile(
        $helper,
        [ref]$tokens,
        [ref]$parseErrors
    )
    Assert-True ($parseErrors.Count -eq 0) 'helper script parses'

    $cmdSource = Join-Path $repoRoot 'configurator\windows'
    foreach ($cmdPath in Get-ChildItem -LiteralPath $cmdSource -Filter '*.cmd') {
        $cmdLines = [IO.File]::ReadAllLines($cmdPath.FullName)
        $labels = @(
            $cmdLines |
                Where-Object { $_ -match '^:([A-Za-z0-9_]+)\s*$' } |
                ForEach-Object { $Matches[1].ToLowerInvariant() }
        )
        $references = @(
            foreach ($line in $cmdLines) {
                foreach ($match in [regex]::Matches($line, '(?i)\b(?:call|goto)\s+:([A-Za-z0-9_]+)')) {
                    $match.Groups[1].Value.ToLowerInvariant()
                }
            }
        )
        $missingLabels = @($references | Where-Object { $_ -notin $labels } | Sort-Object -Unique)
        Assert-True ($missingLabels.Count -eq 0) "$($cmdPath.Name) label references resolve: $($missingLabels -join ', ')"
    }
    $mainCmd = [IO.File]::ReadAllText((Join-Path $cmdSource 'Configure_Me.cmd'))
    $uiCmd = [IO.File]::ReadAllText((Join-Path $cmdSource 'Configurator.Ui.cmd'))
    Assert-True ($mainCmd.Contains('call "!UiModule!" ChooseComponentDownload')) 'component manager opens the selection page'
    Assert-True ($uiCmd.Contains(':ChooseComponentDownload')) 'component download selection action exists'
    Assert-True ($mainCmd.Contains('echo 3. Advanced setup ^(all options^)')) 'main menu names option 3 Advanced setup'
    Assert-True ("$mainCmd`n$uiCmd" -notmatch '(?im)^echo B\..*(?:main menu)') 'main-menu returns do not use B'
    Assert-True ($uiCmd.Contains('echo X. Back to the main menu')) 'background menu uses X to return to main'
    Assert-True ($uiCmd.Contains('-Action OpenUrl')) 'download pages use the URL-opening helper'
    Assert-True ($uiCmd -notmatch '(?im)^\s*start\s+""') 'download pages do not inspect stale START status'
    foreach ($label in @('Fast Rendering    :', 'FR Resource Cache :', 'Prepatcher        :', 'VRAM Optimizer    :')) {
        Assert-True ($uiCmd.Contains($label)) "optional component label is aligned: $label"
    }

    $unsafeInput = Join-Path $work 'input.txt'
    [IO.File]::WriteAllText($unsafeInput, 'unsafe&name')
    $env:HELPER_INPUT_PATH = $unsafeInput
    Assert-True ((Invoke-Helper TestUnsafeInput) -eq 'Yes') 'unsafe CMD input is rejected'
    [IO.File]::WriteAllText($unsafeInput, 'jdk-28+13Miko')
    Assert-True ($null -eq (Invoke-Helper TestUnsafeInput)) 'safe CMD input is accepted'

    foreach ($major in 17, 27, 28) {
        $rendered = Join-Path $work "java-$major.rendered.args"
        $env:JVM_PROFILE_PATH = Join-Path $profiles "java-$major.args"
        $env:JVM_PROFILE_OUTPUT = $rendered
        $env:JVM_COMPILER_DIRECTIVES = '..\\mikohime/.rouge_owo'
        Invoke-Helper RenderProfile | Out-Null
        $text = [IO.File]::ReadAllText($rendered)
        Assert-True (-not $text.Contains('@MIKOHIME_COMPILER_DIRECTIVES@')) "Java $major profile tokens resolve"
        Assert-True ($text.Length -gt 0) "Java $major profile renders"
        if ($major -eq 28) {
            Assert-True ($text.Contains('-Xlog:async')) 'Java 28 profile enables asynchronous JVM logging'
            Assert-True ($text.Contains('-XX:CompilerDirectivesFile=..\\mikohime/.rouge_owo')) 'Java 28 compiler directives resolve'
        }
    }

    $argsPath = Join-Path $work 'Miko_Simple.txt'
    @(
        '-Xlog:async'
        '-XX:CompilerDirectivesFile=..\\mikohime/.rouge_owo'
        '#-DAsyncLogger.WaitStrategy=busyspin'
        '-Xms8192m'
        '-Xmx8192m'
        '-classpath example.jar'
        'com.fs.starfarer.StarfarerLauncher'
    ) | Set-Content -LiteralPath $argsPath -Encoding UTF8
    $env:JVM_ARGS_PATH = $argsPath
    $env:JVM_ARGS_MAJOR = '28'
    Invoke-Helper ValidateArgs | Out-Null
    Assert-True $true 'valid Java 28 generated arguments pass validation'

    Add-Content -LiteralPath $argsPath -Value '-Xmx4096m'
    & $helper -Action ValidateArgs *> $null
    Assert-True ($LASTEXITCODE -ne 0) 'duplicate heap arguments fail validation'

    $recommendations = Get-Content (Join-Path $repoRoot 'configurator\shared\memory-recommendations.tsv') |
        Where-Object { $_ -and -not $_.StartsWith('#') }
    Assert-True ($recommendations -contains '0|4096|4 GB - recommended for this system') 'below 8 GB recommends 4 GB'
    Assert-True ($recommendations -contains '8000|6144|6 GB - recommended for this system') '8-13 GB recommends 6 GB'
    Assert-True ($recommendations -contains '14000|8192|8 GB - recommended for this system') '14-28 GB recommends 8 GB'
    Assert-True ($recommendations -contains '29000|16384|16 GB - recommended for this system') '29 GB or more recommends 16 GB'
    $env:HELPER_MEMORY_MIB = '8192'
    Assert-True ((Invoke-Helper GetMemorySummary) -eq '8|6144') '8 GB memory summary does not round up'
    $env:UPDATE_TEST_AVAILABLE = 'Yes'
    $env:UPDATE_TEST_INSTALLED = 'v1.0.37'
    $env:UPDATE_TEST_LATEST = 'v1.0.40'
    Assert-True ((Invoke-Helper CompareVersions) -eq 'Update available: v1.0.40') 'older component reports an available update'
    $env:UPDATE_TEST_INSTALLED = 'v1.0.40'
    Assert-True ((Invoke-Helper CompareVersions) -eq 'Current (v1.0.40)') 'matching component reports current'
    $env:UPDATE_TEST_INSTALLED = 'v1.1.0'
    Assert-True ((Invoke-Helper CompareVersions) -eq 'Newer than published release v1.0.40') 'newer local component is distinguished'
    $env:UPDATE_TEST_INSTALLED = 'version unknown'
    Assert-True ((Invoke-Helper CompareVersions) -eq 'Installed version unknown (latest v1.0.40)') 'unknown local version is reported'
    $env:UPDATE_TEST_AVAILABLE = 'No'
    Assert-True ((Invoke-Helper CompareVersions) -eq 'Not installed (latest v1.0.40)') 'missing component reports latest release'

    $fixture = Join-Path $work 'game'
    New-Item -ItemType Directory -Path @(
        $fixture
        (Join-Path $fixture 'starsector-core')
        (Join-Path $fixture 'mikohime')
        (Join-Path $fixture 'mikohime\bg')
        (Join-Path $fixture 'mikohime\configurator')
        (Join-Path $fixture 'mikohime\configurator\windows')
    ) -Force | Out-Null
    New-Item -ItemType File -Path (Join-Path $fixture 'starsector.exe') | Out-Null
    Copy-Item -LiteralPath (Join-Path $cmdSource 'Configure_Me.cmd') -Destination $fixture
    Get-ChildItem -LiteralPath $cmdSource -File |
        Where-Object { $_.Name -ne 'Configure_Me.cmd' } |
        Copy-Item -Destination (Join-Path $fixture 'mikohime\configurator\windows')
    Copy-Item -LiteralPath (Join-Path $repoRoot 'configurator\shared') -Destination (Join-Path $fixture 'mikohime\configurator') -Recurse
    Get-ChildItem -LiteralPath (Join-Path $fixture 'mikohime\configurator\shared\logging') -File |
        ForEach-Object {
            $text = [IO.File]::ReadAllText($_.FullName) -replace "`r`n", "`n"
            [IO.File]::WriteAllText($_.FullName, $text, [Text.UTF8Encoding]::new($false))
        }
    Copy-Item -LiteralPath (Join-Path $repoRoot 'distribution\windows\configuration\DefaultPath') -Destination (Join-Path $fixture 'mikohime\DefaultPath')
    [IO.File]::WriteAllText((Join-Path $fixture 'mikohime\bg\gamma_bg.jpg'), 'GAMMA')

    foreach ($major in 17, 27, 28) {
        $javaFolder = "jdk-test-$major"
        $javaBin = Join-Path $fixture "$javaFolder\bin"
        New-Item -ItemType Directory -Path $javaBin -Force | Out-Null
        New-Item -ItemType File -Path (Join-Path $javaBin 'java.exe') -Force | Out-Null
        $env:MIKO_JAVA = $javaFolder
        $env:MIKO_JAVA_VERSION = "$major"
        $env:MIKO_HEAP_MIB = '8192'
        $env:MIKO_LOGGING = if ($major -eq 28) { 'Minimal' } else { 'Full' }
        $env:MIKO_BACKGROUND = if ($major -eq 28) { 'gamma' } else { '' }
        Push-Location $fixture
        try {
            $runOutput = (& cmd.exe /d /c 'call .\Configure_Me.cmd --non-interactive' 2>&1 | Out-String).Trim()
            $exitCode = $LASTEXITCODE
        }
        finally {
            Pop-Location
        }
        Assert-True ($exitCode -eq 0) "Java $major non-interactive generation succeeds ($runOutput)"
        $generated = Get-Content -LiteralPath (Join-Path $fixture 'Miko_Simple.txt')
        Assert-True ($generated -contains '-Xms8192m') "Java $major generated minimum heap"
        Assert-True ($generated -contains '-Xmx8192m') "Java $major generated maximum heap"
        Assert-True ($generated -contains 'com.fs.starfarer.StarfarerLauncher') "Java $major generated launcher class"
        if ($major -eq 28) {
            Assert-True ($generated -contains '-Xlog:async') 'Java 28 keeps asynchronous logging enabled'
            Assert-True ($generated -contains '#-DAsyncLogger.WaitStrategy=busyspin') 'Java 28 keeps busy-spin disabled'
            Assert-True ($generated -contains '-XX:CompilerDirectivesFile=..\\mikohime/.rouge_owo') 'Java 28 compiler directives resolve'
            Assert-True ((Get-Content -LiteralPath (Join-Path $fixture 'mikohime\launcher_bg.jpg') -Raw) -eq 'GAMMA') 'background is committed with configuration'
        }
    }

    New-Item -ItemType Directory -Path (Join-Path $fixture '.configure_me.lock') | Out-Null
    Push-Location $fixture
    try {
        $lockedOutput = (& cmd.exe /d /c 'call .\Configure_Me.cmd --non-interactive' 2>&1 | Out-String)
        $lockedExit = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }
    Assert-True ($lockedExit -ne 0) 'non-interactive mode rejects an existing lock'
    Assert-True (-not $lockedOutput.Contains('Press any key')) 'non-interactive lock failure does not pause'
    Remove-Item -LiteralPath (Join-Path $fixture '.configure_me.lock') -Recurse -Force

    Remove-Item -LiteralPath (Join-Path $fixture 'starsector.exe')
    Push-Location $fixture
    try {
        $invalidOutput = (& cmd.exe /d /c 'call .\Configure_Me.cmd --non-interactive' 2>&1 | Out-String)
        $invalidExit = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }
    Assert-True ($invalidExit -ne 0) 'non-interactive mode rejects an invalid installation'
    Assert-True (-not $invalidOutput.Contains('Press any key')) 'non-interactive installation failure does not pause'

    Write-Host "`nAll $passed Windows configurator checks passed."
}
finally {
    Remove-Item Env:MIKO_JAVA -ErrorAction SilentlyContinue
    Remove-Item Env:MIKO_JAVA_VERSION -ErrorAction SilentlyContinue
    Remove-Item Env:MIKO_HEAP_MIB -ErrorAction SilentlyContinue
    Remove-Item Env:MIKO_LOGGING -ErrorAction SilentlyContinue
    Remove-Item Env:MIKO_BACKGROUND -ErrorAction SilentlyContinue
    Remove-Item Env:UPDATE_TEST_AVAILABLE -ErrorAction SilentlyContinue
    Remove-Item Env:UPDATE_TEST_INSTALLED -ErrorAction SilentlyContinue
    Remove-Item Env:UPDATE_TEST_LATEST -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction SilentlyContinue
}

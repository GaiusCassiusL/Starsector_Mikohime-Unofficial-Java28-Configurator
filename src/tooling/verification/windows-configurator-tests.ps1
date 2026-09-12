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
    Assert-True (-not $mainCmd.Contains('View detected system details')) 'main menu omits redundant system-details option'
    Assert-True ($mainCmd.Contains('choice /c 123456H /n /m "Select an option: "')) 'main menu choices match the shortened menu'
    foreach ($label in @('Java                  :', 'Memory                :', 'Processor             :', 'Optional enhancements :')) {
        Assert-True ($mainCmd.Contains($label)) "main status label is aligned: $label"
    }
    foreach ($label in @('Detected physical CPU cores  :', 'Available logical processors :', 'Recommended low-core tuning  :')) {
        Assert-True ($uiCmd.Contains($label)) "CPU menu label is aligned: $label"
    }
    foreach ($label in @('Detected system memory        :', 'Recommended for this computer :')) {
        Assert-True ($mainCmd.Contains($label)) "memory menu label is aligned: $label"
    }
    foreach ($label in @(
        'Java              :'
        'Game memory       :'
        'Fast Rendering    :'
        'FR Resource Cache :'
        'Prepatcher        :'
        'Limited-CPU mode  :'
        'Large Pages       :'
        'Troubleshooting   :'
    )) {
        Assert-True ($mainCmd.Contains($label)) "basic review label is aligned: $label"
    }
    Assert-True ($mainCmd.Contains('Java 27 or 28     :')) 'component manager Java label aligns with component rows'
    $generationCmd = [IO.File]::ReadAllText((Join-Path $cmdSource 'Configurator.Generation.cmd'))
    foreach ($label in @(
        'Memory allocation    :'
        'Java installation    :'
        'VM tuning            :'
        'Compact headers      :'
        'CPU management       :'
        'Physical CPU cores   :'
        'Logical processors   :'
        'CPU instructions     :'
        'Large Pages          :'
        'Logging              :'
        'Launcher background  :'
        'Fast Rendering       :'
        'FR Resource Cache    :'
        'StarsectorPrepatcher :'
    )) {
        Assert-True ($generationCmd.Contains($label)) "advanced review label is aligned: $label"
    }
    Assert-True ($mainCmd.Contains('call :OfferRecommendedResourceCache')) 'recommended setup checks for missing FR Resource Cache'
    Assert-True ($mainCmd.Contains('if /I not "!FastRenderingAvailable!"=="Yes" exit /b 0')) 'recommended cache offer requires Fast Rendering'
    Assert-True ($mainCmd.Contains('if /I "!ResourceCacheAvailable!"=="Yes" exit /b 0')) 'recommended cache offer skips an existing installation'
    Assert-True ($mainCmd.Contains('choice /c YN /n /m "Download, install, and enable FR Resource Cache? [Y/N] "')) 'recommended cache download requires consent'
    Assert-True ($mainCmd.Contains('call "!UiModule!" DownloadLatestResourceCache')) 'recommended setup uses the verified cache downloader'
    Assert-True ($mainCmd.Contains("call `"!EnvironmentModule!`" RefreshEnvironment`r`ncall :EnableRecommendedEnhancements")) 'recommended setup redetects and enables installed enhancements'
    Assert-True ($mainCmd.Contains('will continue with Fast Rendering enabled and Resource Cache disabled')) 'recommended setup continues safely after download failure'
    Assert-True ("$mainCmd`n$uiCmd" -notmatch '(?im)^echo B\..*(?:main menu)') 'main-menu returns do not use B'
    Assert-True ($uiCmd.Contains('echo X. Back to the main menu')) 'background menu uses X to return to main'
    Assert-True ($uiCmd.Contains('-Action OpenUrl')) 'download pages use the URL-opening helper'
    Assert-True ($uiCmd -notmatch '(?im)^\s*start\s+""') 'download pages do not inspect stale START status'
    Assert-True ($uiCmd.Contains('echo 5. Toadsector')) 'background menu includes Toadsector'
    Assert-True ($uiCmd.Contains('call :CopyBackground "mikohime\bg\toadsector.jpg" "Toadsector"')) 'background menu installs Toadsector'
    Assert-True ($mainCmd.Contains('if /I "!MIKO_BACKGROUND!"=="toadsector"')) 'non-interactive mode supports Toadsector'
    $toadsectorBackground = Join-Path $repoRoot 'distribution\shared\resources\bg\toadsector.jpg'
    Assert-True (Test-Path -LiteralPath $toadsectorBackground -PathType Leaf) 'Toadsector background is packaged'
    Assert-True ((Get-FileHash -LiteralPath $toadsectorBackground -Algorithm SHA256).Hash -eq '8674FE23355ECAE0E5A3568A3C0461E9F8CA1902BD0669E379CE6E90105E4C09') 'Toadsector background matches the supplied image'
    Assert-True ($mainCmd.Contains('echo A. Manage OpenAL Soft audio add-on')) 'component manager exposes OpenAL management'
    Assert-True ($uiCmd.Contains(':ManageOpenAlAddon')) 'OpenAL management menu exists'
    Assert-True ($uiCmd.Contains('Playback ^> Sample Format       : 32-bit float')) 'OpenAL utility screen recommends 32-bit float playback'
    Assert-True ($uiCmd.Contains('Playback ^> Resampler Quality   : Maximum quality')) 'OpenAL utility screen recommends maximum resampler quality'
    Assert-True ($uiCmd.Contains('HRTF ^> HRTF Render Method      : Maximum quality')) 'OpenAL utility screen recommends maximum HRTF quality'
    foreach ($label in @('Fast Rendering    :', 'FR Resource Cache :', 'Prepatcher        :', 'VRAM Optimizer    :', 'OpenAL Soft       :')) {
        Assert-True ($uiCmd.Contains($label)) "optional component label is aligned: $label"
    }

    $components = [IO.File]::ReadAllText((Join-Path $repoRoot 'configurator\shared\components.properties'))
    Assert-True ($components.Contains('OpenAlAddonDownloadUrl=https://github.com/GaiusCassiusL/Starsector_Mikohime-OpenAL-Addon/releases/download/')) 'OpenAL download uses the pinned GitHub release'
    Assert-True ($components.Contains('OpenAlAddonDownloadSha256=FEDD0546B0D5E7E86F65BE496CA27FF072E25D57C6436E8DA4357BE785C9977F')) 'OpenAL download pins the release checksum'
    Assert-True ($uiCmd.Contains('The 17 MB add-on will be downloaded from GitHub before installation.')) 'OpenAL menu discloses the optional download'
    $helperText = [IO.File]::ReadAllText($helper)
    Assert-True ($helperText.Contains('function Download-OpenAlAddon')) 'OpenAL helper supports on-demand download'
    Assert-True ($helperText.Contains('The OpenAL add-on archive contains an unexpected file set.')) 'OpenAL download rejects unexpected archive entries'
    Assert-True ($helperText.Contains('Download-OpenAlAddon')) 'OpenAL installation downloads a missing payload'
    Assert-True ($helperText.Contains('Download-OpenAlAddon -Force')) 'OpenAL repair redownloads an invalid cached payload'

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

    $openAlGame = Join-Path $work 'openal-game'
    $openAlAddon = Join-Path $openAlGame 'mikohime\openal'
    $openAlPayload = Join-Path $openAlAddon 'payload'
    New-Item -ItemType Directory -Path @(
        (Join-Path $openAlPayload 'root')
        (Join-Path $openAlPayload 'win')
        (Join-Path $openAlGame 'mikohime\windows')
    ) -Force | Out-Null
    $openAlScriptSource = Join-Path $openAlPayload 'root\Configure_Audio.bat'
    $openAlDllSource = Join-Path $openAlPayload 'win\OpenAL32.dll'
    $openAlDllDestination = Join-Path $openAlGame 'mikohime\windows\OpenAL32.dll'
    [IO.File]::WriteAllText($openAlScriptSource, 'packaged script')
    [IO.File]::WriteAllText($openAlDllSource, 'OpenAL replacement')
    [IO.File]::WriteAllText($openAlDllDestination, 'original OpenAL')
    $openAlManifest = [ordered]@{
        name = 'OpenAL test payload'
        version = '1.25.1'
        files = @(
            [ordered]@{
                source = 'root/Configure_Audio.bat'
                destination = 'Configure_Audio.bat'
                sha256 = (Get-FileHash -LiteralPath $openAlScriptSource -Algorithm SHA256).Hash
            }
            [ordered]@{
                source = 'win/OpenAL32.dll'
                destination = 'mikohime/windows/OpenAL32.dll'
                sha256 = (Get-FileHash -LiteralPath $openAlDllSource -Algorithm SHA256).Hash
            }
        )
    }
    [IO.File]::WriteAllText(
        (Join-Path $openAlAddon 'addon-manifest.json'),
        ($openAlManifest | ConvertTo-Json -Depth 5),
        [Text.UTF8Encoding]::new($false)
    )
    $env:OPENAL_ADDON_ROOT = $openAlAddon
    Push-Location $openAlGame
    try {
        Assert-True ((Invoke-Helper GetOpenAlAddonStatus) -eq 'Not installed|1.25.1') 'OpenAL reports not installed before deployment'
        Invoke-Helper InstallOpenAlAddon | Out-Null
        Assert-True ((Invoke-Helper GetOpenAlAddonStatus) -eq 'Installed|1.25.1') 'OpenAL reports installed after deployment'
        Assert-True ([IO.File]::ReadAllText($openAlDllDestination) -eq 'OpenAL replacement') 'OpenAL installation replaces the DLL'
        $openAlBackup = Join-Path $openAlAddon 'DLLBK\mikohime\windows\OpenAL32.dll'
        Assert-True ([IO.File]::ReadAllText($openAlBackup) -eq 'original OpenAL') 'OpenAL installation backs up the original DLL'
        $backupHash = (Get-FileHash -LiteralPath $openAlBackup -Algorithm SHA256).Hash
        [IO.File]::WriteAllText($openAlDllDestination, 'locally modified')
        Assert-True ((Invoke-Helper GetOpenAlAddonStatus) -eq 'Modified|1.25.1') 'OpenAL detects modified installed files'
        Invoke-Helper InstallOpenAlAddon | Out-Null
        Assert-True ((Get-FileHash -LiteralPath $openAlBackup -Algorithm SHA256).Hash -eq $backupHash) 'OpenAL repair preserves the first original backup'
        Assert-True ([IO.File]::ReadAllText($openAlDllDestination) -eq 'OpenAL replacement') 'OpenAL repair reapplies the payload'
        Invoke-Helper UninstallOpenAlAddon | Out-Null
        Assert-True ([IO.File]::ReadAllText($openAlDllDestination) -eq 'original OpenAL') 'OpenAL uninstall restores the original DLL'
        Assert-True (-not (Test-Path -LiteralPath (Join-Path $openAlGame 'Configure_Audio.bat'))) 'OpenAL uninstall removes files created by the add-on'
        Assert-True (Test-Path -LiteralPath $openAlDllSource) 'OpenAL uninstall preserves the packaged payload'
        Assert-True ((Invoke-Helper GetOpenAlAddonStatus) -eq 'Not installed|1.25.1') 'OpenAL reports not installed after restoration'
        Invoke-Helper InstallOpenAlAddon | Out-Null
        Remove-Item -LiteralPath (Join-Path $openAlAddon 'install-state.json') -Force
        Assert-True ((Invoke-Helper GetOpenAlAddonStatus) -eq 'Incomplete installation|1.25.1') 'OpenAL detects an orphaned backup'
        Invoke-Helper RecoverOpenAlAddon | Out-Null
        Assert-True ([IO.File]::ReadAllText($openAlDllDestination) -eq 'original OpenAL') 'OpenAL recovery restores available originals'
        Assert-True (-not (Test-Path -LiteralPath (Join-Path $openAlGame 'Configure_Audio.bat'))) 'OpenAL recovery removes unchanged created files'
        Assert-True ((Invoke-Helper GetOpenAlAddonStatus) -eq 'Not installed|1.25.1') 'OpenAL reports not installed after recovery'
        Invoke-Helper InstallOpenAlAddon | Out-Null
        [IO.File]::WriteAllText((Join-Path $openAlAddon 'install-state.json'), '{ invalid state')
        Assert-True ((Invoke-Helper GetOpenAlAddonStatus) -eq 'Incomplete installation|1.25.1') 'OpenAL treats corrupt state with backups as incomplete'
        Invoke-Helper RecoverOpenAlAddon | Out-Null
        Assert-True ([IO.File]::ReadAllText($openAlDllDestination) -eq 'original OpenAL') 'OpenAL recovery restores originals when state is corrupt'
        Assert-True (-not (Test-Path -LiteralPath (Join-Path $openAlAddon 'install-state.json'))) 'OpenAL recovery removes corrupt state'
        Invoke-Helper InstallOpenAlAddon | Out-Null
        $openAlManifest.version = '1.25.2'
        [IO.File]::WriteAllText(
            (Join-Path $openAlAddon 'addon-manifest.json'),
            ($openAlManifest | ConvertTo-Json -Depth 5),
            [Text.UTF8Encoding]::new($false)
        )
        Assert-True ((Invoke-Helper GetOpenAlAddonStatus) -eq 'Different version|1.25.1') 'OpenAL detects an installation from a different payload version'
        Invoke-Helper UninstallOpenAlAddon | Out-Null
        Assert-True ([IO.File]::ReadAllText($openAlDllDestination) -eq 'original OpenAL') 'OpenAL uninstalls a recorded older payload version'
        $openAlManifest.version = '1.25.1'
        [IO.File]::WriteAllText(
            (Join-Path $openAlAddon 'addon-manifest.json'),
            ($openAlManifest | ConvertTo-Json -Depth 5),
            [Text.UTF8Encoding]::new($false)
        )
        Invoke-Helper InstallOpenAlAddon | Out-Null
        [IO.File]::WriteAllText((Join-Path $openAlGame 'Configure_Audio.bat'), 'user replacement')
        Invoke-Helper UninstallOpenAlAddon | Out-Null
        Assert-True ([IO.File]::ReadAllText((Join-Path $openAlGame 'Configure_Audio.bat')) -eq 'user replacement') 'OpenAL uninstall preserves modified files it did not replace originally'
        Remove-Item -LiteralPath (Join-Path $openAlGame 'Configure_Audio.bat') -Force
    }
    finally {
        Pop-Location
        Remove-Item Env:OPENAL_ADDON_ROOT -ErrorAction SilentlyContinue
    }

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
    [IO.File]::WriteAllText((Join-Path $fixture 'mikohime\bg\toadsector.jpg'), 'TOADSECTOR')

    foreach ($major in 17, 27, 28) {
        $javaFolder = "jdk-test-$major"
        $javaBin = Join-Path $fixture "$javaFolder\bin"
        New-Item -ItemType Directory -Path $javaBin -Force | Out-Null
        New-Item -ItemType File -Path (Join-Path $javaBin 'java.exe') -Force | Out-Null
        $env:MIKO_JAVA = $javaFolder
        $env:MIKO_JAVA_VERSION = "$major"
        $env:MIKO_HEAP_MIB = '8192'
        $env:MIKO_LOGGING = if ($major -eq 28) { 'Minimal' } else { 'Full' }
        $env:MIKO_BACKGROUND = if ($major -eq 28) { 'toadsector' } else { '' }
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
            Assert-True ((Get-Content -LiteralPath (Join-Path $fixture 'mikohime\launcher_bg.jpg') -Raw) -eq 'TOADSECTOR') 'Toadsector background is committed with configuration'
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
    $global:LASTEXITCODE = 0
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
    Remove-Item Env:OPENAL_ADDON_ROOT -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction SilentlyContinue
}

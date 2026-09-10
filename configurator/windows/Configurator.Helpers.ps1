param(
    [Parameter(Mandatory = $true)]
    [ValidateSet(
        'CheckComponentUpdates',
        'CompareVersions',
        'DetectUnsafeNames',
        'DetectVramOptimizer',
        'GetJarVersion',
        'GetMemorySummary',
        'GetPrepatcherVersion',
        'GetSystemInfo',
        'InstallJdk',
        'InstallResourceCache',
        'ListJavaFolders',
        'ListPrepatcherFolders',
        'OpenUrl',
        'RemoveDirectory',
        'RenderProfile',
        'TestUnsafeInput',
        'ValidateArgs'
    )]
    [string]$Action
)

$ErrorActionPreference = 'Stop'

function Get-RequiredEnvironmentValue {
    param([string]$Name)

    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Required environment variable is missing: $Name"
    }
    return $value
}

function Get-SafeComponentDirectories {
    param([string[]]$Patterns, [string]$Root = '.')

    $bad = [char[]](33, 34, 37, 38, 40, 41, 60, 62, 94, 124)
    foreach ($pattern in $Patterns) {
        Get-ChildItem -LiteralPath $Root -Directory -Filter $pattern -ErrorAction SilentlyContinue |
            Where-Object { $_.Name.IndexOfAny($bad) -lt 0 }
    }
}

function Get-VersionFromJar {
    param([string]$Path)

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $jar = [IO.Compression.ZipFile]::OpenRead($Path)
    try {
        $manifest = $jar.GetEntry('META-INF/MANIFEST.MF')
        if ($null -ne $manifest) {
            $reader = [IO.StreamReader]::new($manifest.Open())
            try {
                $text = $reader.ReadToEnd()
            }
            finally {
                $reader.Dispose()
            }
            $attribute = [regex]::Match(
                $text,
                '(?im)^(?:Implementation-Version|Bundle-Version|Specification-Version):\s*([^\r\n]+)'
            )
            if ($attribute.Success) {
                $version = [regex]::Match(
                    $attribute.Groups[1].Value,
                    '\d+(?:\.\d+){1,3}(?:[-+][0-9A-Za-z.-]+)?'
                )
                if ($version.Success) {
                    return $version.Value
                }
            }
        }

        $rendererVersion = $jar.GetEntry('com/genir/renderer/Version.class')
        if ($null -ne $rendererVersion) {
            $stream = $rendererVersion.Open()
            try {
                $memory = [IO.MemoryStream]::new()
                $stream.CopyTo($memory)
                $text = [Text.Encoding]::ASCII.GetString($memory.ToArray())
            }
            finally {
                $stream.Dispose()
            }
            $version = [regex]::Match($text, 'v(\d+(?:\.\d+){1,3})')
            if ($version.Success) {
                return $version.Groups[1].Value
            }
        }
    }
    finally {
        $jar.Dispose()
    }
    return $null
}

function ConvertTo-Version {
    param([string]$Text)

    if ([string]::IsNullOrWhiteSpace($Text)) {
        return $null
    }
    $match = [regex]::Match($Text, '\d+(?:\.\d+){0,3}')
    if (-not $match.Success) {
        return $null
    }
    try {
        return [version]$match.Value
    }
    catch {
        return $null
    }
}

function Compare-ComponentVersions {
    param(
        [string]$Available,
        [string]$InstalledVersions,
        [string]$LatestVersion
    )

    $latest = ConvertTo-Version $LatestVersion
    if ($null -eq $latest) {
        return 'Unable to determine the latest release'
    }
    $latestLabel = "v$latest"
    if ($Available -ne 'Yes') {
        return "Not installed (latest $latestLabel)"
    }

    $installedParts = @($InstalledVersions.Trim(';').Split(';', [StringSplitOptions]::RemoveEmptyEntries))
    if ($installedParts.Count -eq 0) {
        return "Installed version unknown (latest $latestLabel)"
    }
    $installed = foreach ($part in $installedParts) {
        $version = ConvertTo-Version $part
        if ($null -eq $version) {
            return "Installed version unknown (latest $latestLabel)"
        }
        $version
    }
    if ($installed | Where-Object { $_ -lt $latest }) {
        return "Update available: $latestLabel"
    }
    if ($installed | Where-Object { $_ -gt $latest }) {
        return "Newer than published release $latestLabel"
    }
    return "Current ($latestLabel)"
}

function Get-LatestReleaseVersion {
    param([string]$ApiUrl)

    $headers = @{
        Accept = 'application/vnd.github+json'
        'User-Agent' = 'Mikohime-Java-Configurator'
        'X-GitHub-Api-Version' = '2022-11-28'
    }
    $release = Invoke-RestMethod -Uri $ApiUrl -Headers $headers -TimeoutSec 15
    $version = ConvertTo-Version ([string]$release.tag_name)
    if ($null -eq $version) {
        throw "Release tag does not contain a version: $($release.tag_name)"
    }
    return "v$version"
}

function Write-ComponentUpdateStatus {
    param(
        [string]$Name,
        [string]$AvailableVariable,
        [string]$InstalledVariable,
        [string]$ApiVariable
    )

    $available = Get-RequiredEnvironmentValue $AvailableVariable
    $installed = [Environment]::GetEnvironmentVariable($InstalledVariable)
    try {
        $latest = Get-LatestReleaseVersion (Get-RequiredEnvironmentValue $ApiVariable)
        $status = Compare-ComponentVersions $available $installed $latest
    }
    catch {
        $status = 'Unable to check'
    }
    Write-Output ('{0,-18}: {1}' -f $Name, $status)
}

function Get-VramOptimizer {
    if (-not (Test-Path -LiteralPath 'mods' -PathType Container)) {
        return
    }

    foreach ($directory in Get-ChildItem -LiteralPath 'mods' -Directory) {
        $metadata = Join-Path $directory.FullName 'mod_info.json'
        if (-not (Test-Path -LiteralPath $metadata -PathType Leaf)) {
            continue
        }
        $raw = [IO.File]::ReadAllText($metadata)
        if ($raw -notmatch '(?im)"id"\s*:\s*"VramOptimizer"') {
            continue
        }

        $version = 'version unknown'
        $versionFile = Join-Path $directory.FullName 'VOpt.version'
        if (Test-Path -LiteralPath $versionFile -PathType Leaf) {
            try {
                $data = Get-Content -LiteralPath $versionFile -Raw | ConvertFrom-Json
                $value = $data.modVersion
                if ([int]$value.major -ge 0 -and [int]$value.minor -ge 0 -and [int]$value.patch -ge 0) {
                    $version = "v$($value.major).$($value.minor).$($value.patch)"
                }
            }
            catch {
                $version = 'version unknown'
            }
        }
        if ($version -eq 'version unknown') {
            $label = Join-Path $directory.FullName 'VOpt_VERSION.txt'
            if (Test-Path -LiteralPath $label -PathType Leaf) {
                $match = [regex]::Match([IO.File]::ReadAllText($label), '(?i)\bv?(\d+(?:\.\d+){1,3})\b')
                if ($match.Success) {
                    $version = "v$($match.Groups[1].Value)"
                }
            }
        }
        Write-Output "Yes|$version"
        return
    }
}

function Install-Jdk {
    $url = Get-RequiredEnvironmentValue 'JDK_INSTALL_URL'
    $expectedHash = Get-RequiredEnvironmentValue 'JDK_INSTALL_HASH'
    $archivePath = Get-RequiredEnvironmentValue 'JDK_INSTALL_ZIP'
    $extractPath = Get-RequiredEnvironmentValue 'JDK_INSTALL_EXTRACT'
    $destination = Get-RequiredEnvironmentValue 'JDK_INSTALL_DESTINATION'
    $major = Get-RequiredEnvironmentValue 'JDK_INSTALL_MAJOR'

    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri $url -OutFile $archivePath
    $actualHash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash
    if ($actualHash -ne $expectedHash) {
        throw 'The downloaded JDK checksum does not match.'
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::ExtractToDirectory($archivePath, $extractPath)
    $javaFiles = @(
        Get-ChildItem -LiteralPath $extractPath -Filter java.exe -Recurse |
            Where-Object { -not $_.PSIsContainer -and $_.Directory.Name -eq 'bin' }
    )
    if ($javaFiles.Count -ne 1) {
        throw 'Unable to locate exactly one JDK installation in the archive.'
    }

    $java = $javaFiles[0]
    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $java.FullName
    $startInfo.Arguments = '-version'
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardError = $true
    $startInfo.RedirectStandardOutput = $true
    $process = [Diagnostics.Process]::Start($startInfo)
    $versionText = $process.StandardError.ReadToEnd() + $process.StandardOutput.ReadToEnd()
    $process.WaitForExit()
    $versionMatch = [regex]::Match($versionText, 'version "(\d+)')
    if ($process.ExitCode -ne 0 -or -not $versionMatch.Success -or $versionMatch.Groups[1].Value -ne $major) {
        throw 'The downloaded archive contains the wrong Java version.'
    }

    $jdkHome = $java.Directory.Parent.FullName
    if (Test-Path -LiteralPath $destination) {
        throw 'The JDK destination was created while the download was running.'
    }
    [IO.Directory]::Move($jdkHome, $destination)
}

function Install-ResourceCache {
    $api = Get-RequiredEnvironmentValue 'RESOURCE_CACHE_API'
    $assetPrefix = Get-RequiredEnvironmentValue 'RESOURCE_CACHE_ASSET_PREFIX'
    $archivePath = Get-RequiredEnvironmentValue 'RESOURCE_CACHE_ZIP'
    $stagedJar = Get-RequiredEnvironmentValue 'RESOURCE_CACHE_STAGED_JAR'
    $destination = Get-RequiredEnvironmentValue 'RESOURCE_CACHE_DESTINATION'

    $ProgressPreference = 'SilentlyContinue'
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $headers = @{
        Accept = 'application/vnd.github+json'
        'User-Agent' = 'Mikohime-Java-Configurator'
        'X-GitHub-Api-Version' = '2022-11-28'
    }
    $release = Invoke-RestMethod -Uri $api -Headers $headers
    $assets = @($release.assets | Where-Object { $_.name -like '*.zip' })
    if ($assets.Count -ne 1) {
        throw 'The latest release must contain exactly one ZIP asset.'
    }
    $asset = $assets[0]
    if (-not ([string]$asset.browser_download_url).StartsWith($assetPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'The release asset URL is not trusted.'
    }
    $digestMatch = [regex]::Match([string]$asset.digest, '^sha256:([0-9a-fA-F]{64})$')
    if (-not $digestMatch.Success) {
        throw 'GitHub did not provide a valid SHA-256 digest for the release asset.'
    }
    if ([long]$asset.size -le 0 -or [long]$asset.size -gt 10MB) {
        throw 'The release asset size is invalid.'
    }

    Invoke-WebRequest -UseBasicParsing -Uri $asset.browser_download_url -OutFile $archivePath
    if ((Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash -ne $digestMatch.Groups[1].Value) {
        throw 'The downloaded release checksum does not match GitHub digest.'
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($archivePath)
    try {
        $entries = @(
            $archive.Entries |
                Where-Object {
                    [IO.Path]::GetFileName($_.FullName) -eq 'fr-resource-cache-agent.jar' -and
                    $_.Length -gt 0 -and $_.Length -le 10MB
                }
        )
        if ($entries.Count -ne 1) {
            throw 'The release does not contain exactly one valid agent JAR.'
        }
        $input = $entries[0].Open()
        $output = [IO.File]::Create($stagedJar)
        try {
            $input.CopyTo($output)
        }
        finally {
            $output.Dispose()
            $input.Dispose()
        }
    }
    finally {
        $archive.Dispose()
    }

    $jar = [IO.Compression.ZipFile]::OpenRead($stagedJar)
    try {
        $manifest = $jar.GetEntry('META-INF/MANIFEST.MF')
        if ($null -eq $manifest) {
            throw 'The agent JAR has no manifest.'
        }
        $reader = [IO.StreamReader]::new($manifest.Open())
        try {
            $manifestText = $reader.ReadToEnd()
        }
        finally {
            $reader.Dispose()
        }
        if ($manifestText -notmatch '(?m)^Premain-Class:\s*dev\.frresourcecache\.FrResourceCacheAgent\s*$') {
            throw 'The agent JAR manifest is not valid.'
        }
    }
    finally {
        $jar.Dispose()
    }

    if (Test-Path -LiteralPath $destination) {
        throw 'The destination file was created while the download was running.'
    }
    [IO.File]::Move($stagedJar, $destination)
}

function Render-Profile {
    $profile = Get-RequiredEnvironmentValue 'JVM_PROFILE_PATH'
    $output = Get-RequiredEnvironmentValue 'JVM_PROFILE_OUTPUT'
    $directives = Get-RequiredEnvironmentValue 'JVM_COMPILER_DIRECTIVES'
    $text = [IO.File]::ReadAllText($profile)
    $text = $text.Replace('@MIKOHIME_COMPILER_DIRECTIVES@', $directives)
    $unresolved = [regex]::Match($text, '@[A-Z][A-Z0-9_]*@')
    if ($unresolved.Success) {
        throw "Unresolved JVM profile token: $($unresolved.Value)"
    }
    if ([string]::IsNullOrWhiteSpace($text)) {
        throw 'The JVM profile is empty.'
    }
    $normalized = ($text -replace "\r\n?", "`n").TrimEnd("`n") -replace "`n", "`r`n"
    [IO.File]::WriteAllText($output, "$normalized`r`n", [Text.UTF8Encoding]::new($false))
}

function Validate-Args {
    $path = Get-RequiredEnvironmentValue 'JVM_ARGS_PATH'
    $major = Get-RequiredEnvironmentValue 'JVM_ARGS_MAJOR'
    $lines = @([IO.File]::ReadAllLines($path))
    if ($lines | Where-Object { $_ -match '@[A-Z][A-Z0-9_]*@' }) {
        throw 'The generated JVM arguments contain an unresolved profile token.'
    }
    foreach ($pattern in @('^-Xms', '^-Xmx', '^-classpath\s', '^com\.fs\.starfarer\.StarfarerLauncher$')) {
        if (@($lines | Where-Object { $_ -match $pattern }).Count -ne 1) {
            throw "Expected exactly one generated argument matching $pattern"
        }
    }
    if ($major -eq '28') {
        foreach ($line in @(
            '-Xlog:async',
            '#-DAsyncLogger.WaitStrategy=busyspin'
        )) {
            if (@($lines | Where-Object { $_ -eq $line }).Count -ne 1) {
                throw "Expected exactly one Java 28 argument: $line"
            }
        }
        if (@($lines | Where-Object { $_ -match '^-XX:CompilerDirectivesFile=' }).Count -ne 1) {
            throw 'Expected exactly one Java 28 compiler-directives argument.'
        }
        if ($lines -contains '-DAsyncLogger.WaitStrategy=busyspin') {
            throw 'Java 28 busy-spin logger wait must remain disabled.'
        }
    }
}

try {
    switch ($Action) {
        'CheckComponentUpdates' {
            [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
            Write-ComponentUpdateStatus 'Fast Rendering' 'UPDATE_FAST_RENDERING_AVAILABLE' 'UPDATE_FAST_RENDERING_VERSION' 'UPDATE_FAST_RENDERING_API'
            Write-ComponentUpdateStatus 'FR Resource Cache' 'UPDATE_RESOURCE_CACHE_AVAILABLE' 'UPDATE_RESOURCE_CACHE_VERSION' 'UPDATE_RESOURCE_CACHE_API'
            Write-ComponentUpdateStatus 'Prepatcher' 'UPDATE_PREPATCHER_AVAILABLE' 'UPDATE_PREPATCHER_VERSIONS' 'UPDATE_PREPATCHER_API'
            Write-ComponentUpdateStatus 'VRAM Optimizer' 'UPDATE_VRAM_AVAILABLE' 'UPDATE_VRAM_VERSION' 'UPDATE_VRAM_API'
        }
        'CompareVersions' {
            Write-Output (Compare-ComponentVersions `
                (Get-RequiredEnvironmentValue 'UPDATE_TEST_AVAILABLE') `
                ([Environment]::GetEnvironmentVariable('UPDATE_TEST_INSTALLED')) `
                (Get-RequiredEnvironmentValue 'UPDATE_TEST_LATEST'))
        }
        'DetectUnsafeNames' {
            $bad = [char[]](33, 34, 37, 38, 40, 41, 60, 62, 94, 124)
            $unsafe = Get-ChildItem -Directory |
                Where-Object {
                    ($_.Name -like 'jdk-27*' -or $_.Name -like 'jdk-28*') -and
                    $_.Name.IndexOfAny($bad) -ge 0
                }
            if (-not $unsafe -and (Test-Path -LiteralPath 'mods' -PathType Container)) {
                $unsafe = Get-ChildItem -LiteralPath 'mods' -Directory -Filter 'StarsectorPrepatcher*' |
                    Where-Object { $_.Name.IndexOfAny($bad) -ge 0 }
            }
            if ($unsafe) {
                Write-Output 'Yes'
            }
        }
        'DetectVramOptimizer' {
            Get-VramOptimizer
        }
        'GetJarVersion' {
            $version = Get-VersionFromJar (Get-RequiredEnvironmentValue 'HELPER_JAR_PATH')
            if ($version) {
                Write-Output "v$version"
            }
        }
        'GetMemorySummary' {
            $memory = [int64](Get-RequiredEnvironmentValue 'HELPER_MEMORY_MIB')
            Write-Output "$([int64][Math]::Ceiling($memory / 1024.0))|$([int64][Math]::Floor($memory * 0.75))"
        }
        'GetPrepatcherVersion' {
            $minimum = [version](Get-RequiredEnvironmentValue 'HELPER_MINIMUM_VERSION')
            $path = Get-RequiredEnvironmentValue 'HELPER_METADATA_PATH'
            if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
                Write-Output 'Unreadable|'
                break
            }
            try {
                $raw = [string](Get-Content -LiteralPath $path -Raw | ConvertFrom-Json).version
                $match = [regex]::Match($raw, '\d+(?:\.\d+){1,3}')
                if (-not $match.Success) {
                    Write-Output 'Unreadable|'
                }
                else {
                    $version = [version]$match.Value
                    $status = if ($version -ge $minimum) { 'Compatible' } else { 'Incompatible' }
                    Write-Output "$status|$($version.ToString())"
                }
            }
            catch {
                Write-Output 'Unreadable|'
            }
        }
        'GetSystemInfo' {
            $cpu = Get-CimInstance Win32_Processor
            $memory = [int64]((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1MB)
            $cores = [int](($cpu | Measure-Object NumberOfCores -Sum).Sum)
            $logical = ([Convert]::ToString((Get-Process -Id $PID).ProcessorAffinity.ToInt64(), 2) -replace '0', '').Length
            Write-Output "$memory|$cores|$logical"
        }
        'InstallJdk' {
            Install-Jdk
        }
        'InstallResourceCache' {
            Install-ResourceCache
        }
        'ListJavaFolders' {
            Get-SafeComponentDirectories -Patterns @('jdk-27*', 'jdk-28*') |
                Select-Object -ExpandProperty Name -Unique
        }
        'ListPrepatcherFolders' {
            if (Test-Path -LiteralPath 'mods' -PathType Container) {
                Get-SafeComponentDirectories -Patterns @('StarsectorPrepatcher*') -Root 'mods' |
                    Select-Object -ExpandProperty Name -Unique
            }
        }
        'OpenUrl' {
            $url = [Uri](Get-RequiredEnvironmentValue 'HELPER_OPEN_URL')
            if ($url.Scheme -notin @('http', 'https')) {
                throw 'Only HTTP and HTTPS URLs may be opened.'
            }
            $startInfo = [Diagnostics.ProcessStartInfo]::new()
            $startInfo.FileName = $url.AbsoluteUri
            $startInfo.UseShellExecute = $true
            [Diagnostics.Process]::Start($startInfo) | Out-Null
        }
        'RemoveDirectory' {
            $path = [IO.Path]::GetFullPath((Get-RequiredEnvironmentValue 'HELPER_REMOVE_PATH'))
            $root = [IO.Path]::GetFullPath((Get-Location).Path) + [IO.Path]::DirectorySeparatorChar
            if (-not $path.StartsWith($root, [StringComparison]::OrdinalIgnoreCase)) {
                throw 'Refusing to remove a directory outside the game root.'
            }
            if (Test-Path -LiteralPath $path -PathType Container) {
                Remove-Item -LiteralPath $path -Recurse -Force
            }
        }
        'RenderProfile' {
            Render-Profile
        }
        'TestUnsafeInput' {
            $text = [IO.File]::ReadAllText((Get-RequiredEnvironmentValue 'HELPER_INPUT_PATH'))
            if ($text.IndexOfAny([char[]](33, 34, 37, 38, 40, 41, 60, 62, 94, 124)) -ge 0) {
                Write-Output 'Yes'
            }
        }
        'ValidateArgs' {
            Validate-Args
        }
    }
}
catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

param(
    [Parameter(Mandatory = $true)]
    [ValidateSet(
        'CheckComponentUpdates',
        'CompareVersions',
        'DetectUnsafeNames',
        'DetectVramOptimizer',
        'GetJarVersion',
        'GetMemorySummary',
        'GetOpenAlAddonStatus',
        'GetPrepatcherVersion',
        'GetSystemInfo',
        'InstallJdk',
        'InstallOpenAlAddon',
        'InstallResourceCache',
        'ListJavaFolders',
        'ListPrepatcherFolders',
        'OpenUrl',
        'OpenAudioConfigurator',
        'RecoverOpenAlAddon',
        'RemoveDirectory',
        'RenderProfile',
        'TestUnsafeInput',
        'UninstallOpenAlAddon',
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

function Resolve-ContainedPath {
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][string]$Relative
    )

    if ([IO.Path]::IsPathRooted($Relative) -or
        $Relative.Split([char[]]'\/') -contains '..') {
        throw "Unsafe relative path: $Relative"
    }
    $rootPath = [IO.Path]::GetFullPath($Root).TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
    $path = [IO.Path]::GetFullPath((Join-Path $rootPath $Relative))
    if (-not $path.StartsWith($rootPath, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Path escapes its required root: $Relative"
    }
    return $path
}

function Get-OpenAlRoot {
    $gameRoot = [IO.Path]::GetFullPath((Get-Location).Path)
    $gameRootPrefix = $gameRoot.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
    $configuredRoot = [Environment]::GetEnvironmentVariable('OPENAL_ADDON_ROOT')
    $addonRoot = if ([string]::IsNullOrWhiteSpace($configuredRoot)) {
        Join-Path $gameRoot 'mikohime\openal'
    }
    else {
        [IO.Path]::GetFullPath($configuredRoot)
    }
    if (-not ($addonRoot.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar).StartsWith(
        $gameRootPrefix,
        [StringComparison]::OrdinalIgnoreCase
    )) {
        throw 'The OpenAL add-on folder must be inside the game root.'
    }
    return [pscustomobject]@{
        GameRoot = $gameRoot
        AddonRoot = $addonRoot
        ManifestPath = Join-Path $addonRoot 'addon-manifest.json'
        StatePath = Join-Path $addonRoot 'install-state.json'
        BackupRoot = Join-Path $addonRoot 'DLLBK'
    }
}

function Download-OpenAlAddon {
    param([switch]$Force)

    $root = Get-OpenAlRoot
    if (-not $Force -and (Test-Path -LiteralPath $root.ManifestPath -PathType Leaf)) {
        return
    }

    $url = [Uri](Get-RequiredEnvironmentValue 'OpenAlAddonDownloadUrl')
    if ($url.Scheme -ne 'https' -or $url.Host -ne 'github.com') {
        throw 'The OpenAL add-on download URL is not trusted.'
    }
    $expectedHash = Get-RequiredEnvironmentValue 'OpenAlAddonDownloadSha256'
    if ($expectedHash -notmatch '^[0-9A-Fa-f]{64}$') {
        throw 'The OpenAL add-on download checksum is invalid.'
    }
    $version = Get-RequiredEnvironmentValue 'OpenAlAddonDownloadVersion'
    $mappings = @(
        [pscustomobject]@{ archive = 'mikohime/alsoft-config/alsoft-config.exe'; source = 'cfg/alsoft-config.exe'; destination = 'mikohime/alsoft-config/alsoft-config.exe' }
        [pscustomobject]@{ archive = 'mikohime/alsoft-config/libgcc_s_seh-1.dll'; source = 'cfg/libgcc_s_seh-1.dll'; destination = 'mikohime/alsoft-config/libgcc_s_seh-1.dll' }
        [pscustomobject]@{ archive = 'mikohime/alsoft-config/libstdc++-6.dll'; source = 'cfg/libstdc++-6.dll'; destination = 'mikohime/alsoft-config/libstdc++-6.dll' }
        [pscustomobject]@{ archive = 'mikohime/alsoft-config/libwinpthread-1.dll'; source = 'cfg/libwinpthread-1.dll'; destination = 'mikohime/alsoft-config/libwinpthread-1.dll' }
        [pscustomobject]@{ archive = 'mikohime/alsoft-config/platforms/qwindows.dll'; source = 'cfg/platforms/qwindows.dll'; destination = 'mikohime/alsoft-config/platforms/qwindows.dll' }
        [pscustomobject]@{ archive = 'mikohime/alsoft-config/Qt6Core.dll'; source = 'cfg/Qt6Core.dll'; destination = 'mikohime/alsoft-config/Qt6Core.dll' }
        [pscustomobject]@{ archive = 'mikohime/alsoft-config/Qt6Gui.dll'; source = 'cfg/Qt6Gui.dll'; destination = 'mikohime/alsoft-config/Qt6Gui.dll' }
        [pscustomobject]@{ archive = 'mikohime/alsoft-config/Qt6Widgets.dll'; source = 'cfg/Qt6Widgets.dll'; destination = 'mikohime/alsoft-config/Qt6Widgets.dll' }
        [pscustomobject]@{ archive = 'mikohime/alsoft-config/zlib1.dll'; source = 'cfg/zlib1.dll'; destination = 'mikohime/alsoft-config/zlib1.dll' }
        [pscustomobject]@{ archive = 'mikohime/windows/OpenAL32.dll'; source = 'win/OpenAL32.dll'; destination = 'mikohime/windows/OpenAL32.dll' }
        [pscustomobject]@{ archive = 'mikohime/windows/OpenAL64.dll'; source = 'win/OpenAL64.dll'; destination = 'mikohime/windows/OpenAL64.dll' }
        [pscustomobject]@{ archive = 'Configure_Audio.bat'; source = 'root/Configure_Audio.bat'; destination = 'Configure_Audio.bat' }
        [pscustomobject]@{ archive = 'oalinst.exe'; source = 'root/oalinst.exe'; destination = 'oalinst.exe' }
    )

    $transactionRoot = Join-Path $root.GameRoot ('mikohime\.openal-download-' + [guid]::NewGuid().ToString('N'))
    $archivePath = Join-Path $transactionRoot 'openal.zip'
    $stagedRoot = Join-Path $transactionRoot 'staged'
    $previousPayload = Join-Path $transactionRoot 'previous-payload'
    [void](New-Item -ItemType Directory -Path $stagedRoot -Force)
    try {
        $ProgressPreference = 'SilentlyContinue'
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -UseBasicParsing -Uri $url.AbsoluteUri -OutFile $archivePath
        if ((Get-Item -LiteralPath $archivePath).Length -le 0 -or
            (Get-Item -LiteralPath $archivePath).Length -gt 25MB -or
            (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash -ne $expectedHash) {
            throw 'The downloaded OpenAL add-on archive failed validation.'
        }

        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $archive = [IO.Compression.ZipFile]::OpenRead($archivePath)
        try {
            $files = @($archive.Entries | Where-Object { -not [string]::IsNullOrEmpty($_.Name) })
            $expectedEntries = @($mappings.archive | Sort-Object)
            $actualEntries = @($files.FullName | ForEach-Object { $_.Replace('\', '/') } | Sort-Object)
            if (($expectedEntries -join "`n") -ne ($actualEntries -join "`n")) {
                throw 'The OpenAL add-on archive contains an unexpected file set.'
            }

            $manifestFiles = foreach ($mapping in $mappings) {
                $entry = $archive.GetEntry([string]$mapping.archive)
                if ($null -eq $entry -or $entry.Length -le 0 -or $entry.Length -gt 15MB) {
                    throw "The OpenAL add-on archive entry is invalid: $($mapping.archive)"
                }
                $destination = Resolve-ContainedPath (Join-Path $stagedRoot 'payload') ([string]$mapping.source)
                $parent = Split-Path -Parent $destination
                [void](New-Item -ItemType Directory -Path $parent -Force)
                $input = $entry.Open()
                $output = [IO.File]::Create($destination)
                try {
                    $input.CopyTo($output)
                }
                finally {
                    $output.Dispose()
                    $input.Dispose()
                }
                [pscustomobject]@{
                    source = [string]$mapping.source
                    destination = [string]$mapping.destination
                    sha256 = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash
                }
            }
        }
        finally {
            $archive.Dispose()
        }

        $manifest = [pscustomobject]@{
            name = 'OpenAL Soft Windows add-on'
            version = $version
            downloadUrl = $url.AbsoluteUri
            downloadSha256 = $expectedHash.ToUpperInvariant()
            files = @($manifestFiles)
        }
        [IO.File]::WriteAllText(
            (Join-Path $stagedRoot 'addon-manifest.json'),
            ($manifest | ConvertTo-Json -Depth 6) + "`r`n",
            [Text.UTF8Encoding]::new($false)
        )
        [IO.File]::WriteAllText(
            (Join-Path $stagedRoot 'README.txt'),
            "Downloaded OpenAL Soft $version add-on.`r`nSource: $($url.AbsoluteUri)`r`n`r`nRecommended settings:`r`n  Playback > Sample Format       : 32-bit float`r`n  Playback > Resampler Quality   : Maximum quality`r`n  HRTF > HRTF Render Method      : Maximum quality`r`n",
            [Text.UTF8Encoding]::new($false)
        )

        [void](New-Item -ItemType Directory -Path $root.AddonRoot -Force)
        $payloadPath = Join-Path $root.AddonRoot 'payload'
        if (Test-Path -LiteralPath $payloadPath) {
            [IO.Directory]::Move($payloadPath, $previousPayload)
        }
        try {
            [IO.Directory]::Move((Join-Path $stagedRoot 'payload'), $payloadPath)
            [IO.File]::Copy((Join-Path $stagedRoot 'addon-manifest.json'), $root.ManifestPath, $true)
            [IO.File]::Copy((Join-Path $stagedRoot 'README.txt'), (Join-Path $root.AddonRoot 'README.txt'), $true)
        }
        catch {
            Remove-Item -LiteralPath $payloadPath -Recurse -Force -ErrorAction SilentlyContinue
            Remove-Item -LiteralPath $root.ManifestPath -Force -ErrorAction SilentlyContinue
            Remove-Item -LiteralPath (Join-Path $root.AddonRoot 'README.txt') -Force -ErrorAction SilentlyContinue
            if (Test-Path -LiteralPath $previousPayload) {
                [IO.Directory]::Move($previousPayload, $payloadPath)
            }
            throw
        }
    }
    finally {
        Remove-Item -LiteralPath $transactionRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
}

function Get-OpenAlContext {
    param([switch]$VerifyPayload)

    $root = Get-OpenAlRoot
    $gameRoot = $root.GameRoot
    $addonRoot = $root.AddonRoot
    $manifestPath = $root.ManifestPath
    if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
        throw "OpenAL add-on manifest is missing: $manifestPath"
    }
    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    if ([string]::IsNullOrWhiteSpace([string]$manifest.version) -or @($manifest.files).Count -eq 0) {
        throw 'The OpenAL add-on manifest is invalid.'
    }

    $destinations = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
    foreach ($file in @($manifest.files)) {
        if ([string]::IsNullOrWhiteSpace([string]$file.source) -or
            [string]::IsNullOrWhiteSpace([string]$file.destination) -or
            [string]$file.sha256 -notmatch '^[0-9A-Fa-f]{64}$') {
            throw 'The OpenAL add-on manifest contains an invalid file entry.'
        }
        $source = Resolve-ContainedPath (Join-Path $addonRoot 'payload') ([string]$file.source)
        [void](Resolve-ContainedPath $gameRoot ([string]$file.destination))
        if (-not $destinations.Add([string]$file.destination)) {
            throw "The OpenAL add-on manifest repeats a destination: $($file.destination)"
        }
        if (-not (Test-Path -LiteralPath $source -PathType Leaf) -or
            ($VerifyPayload -and
                (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash -ne [string]$file.sha256)) {
            throw "OpenAL payload validation failed: $($file.source)"
        }
    }

    return [pscustomobject]@{
        GameRoot = $gameRoot
        AddonRoot = $addonRoot
        Manifest = $manifest
        StatePath = Join-Path $addonRoot 'install-state.json'
        BackupRoot = Join-Path $addonRoot 'DLLBK'
    }
}

function Read-OpenAlState {
    param($Context, [switch]$RequireCurrentManifest, [switch]$VerifyBackups)

    if (-not (Test-Path -LiteralPath $Context.StatePath -PathType Leaf)) {
        throw 'The OpenAL add-on installation state is missing.'
    }
    $state = Get-Content -LiteralPath $Context.StatePath -Raw | ConvertFrom-Json
    if ([string]::IsNullOrWhiteSpace([string]$state.version) -or @($state.files).Count -eq 0) {
        throw 'The OpenAL add-on installation state is invalid.'
    }
    if ($RequireCurrentManifest -and (
        [string]$state.version -ne [string]$Context.Manifest.version -or
        @($state.files).Count -ne @($Context.Manifest.files).Count
    )) {
        throw 'The OpenAL add-on installation state does not match this payload.'
    }
    $destinations = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
    foreach ($entry in @($state.files)) {
        if ([string]::IsNullOrWhiteSpace([string]$entry.destination) -or
            [string]$entry.sha256 -notmatch '^[0-9A-Fa-f]{64}$' -or
            -not $destinations.Add([string]$entry.destination)) {
            throw 'The OpenAL add-on installation state contains an invalid file entry.'
        }
        [void](Resolve-ContainedPath $Context.GameRoot ([string]$entry.destination))
        if ([bool]$entry.existed) {
            if ([string]$entry.originalSha256 -notmatch '^[0-9A-Fa-f]{64}$') {
                throw "The OpenAL backup state is invalid: $($entry.destination)"
            }
            $backup = Resolve-ContainedPath $Context.BackupRoot ([string]$entry.destination)
            if (-not (Test-Path -LiteralPath $backup -PathType Leaf) -or
                ($VerifyBackups -and
                    (Get-FileHash -LiteralPath $backup -Algorithm SHA256).Hash -ne [string]$entry.originalSha256)) {
                throw "The OpenAL backup is missing or damaged: $($entry.destination)"
            }
        }
    }
    if ($RequireCurrentManifest) {
        foreach ($manifestFile in @($Context.Manifest.files)) {
            $matches = @($state.files | Where-Object {
                [string]$_.destination -eq [string]$manifestFile.destination -and
                [string]$_.sha256 -eq [string]$manifestFile.sha256
            })
            if ($matches.Count -ne 1) {
                throw "The OpenAL add-on state is missing a file: $($manifestFile.destination)"
            }
        }
    }
    return $state
}

function Copy-FileWithParent {
    param([string]$Source, [string]$Destination)

    $parent = Split-Path -Parent $Destination
    if (-not (Test-Path -LiteralPath $parent -PathType Container)) {
        [void](New-Item -ItemType Directory -Path $parent -Force)
    }
    [IO.File]::Copy($Source, $Destination, $true)
}

function Restore-OpenAlSnapshot {
    param($Context, [array]$Snapshot)

    for ($index = $Snapshot.Count - 1; $index -ge 0; $index--) {
        $item = $Snapshot[$index]
        $destination = Resolve-ContainedPath $Context.GameRoot ([string]$item.destination)
        if ([bool]$item.existed) {
            Copy-FileWithParent ([string]$item.snapshot) $destination
        }
        elseif (Test-Path -LiteralPath $destination -PathType Leaf) {
            Remove-Item -LiteralPath $destination -Force
        }
    }
}

function Install-OpenAlAddon {
    $root = Get-OpenAlRoot
    $downloadRequired = -not (Test-Path -LiteralPath $root.ManifestPath -PathType Leaf)
    if (-not $downloadRequired) {
        try {
            [void](Get-OpenAlContext -VerifyPayload)
        }
        catch {
            $downloadRequired = $true
        }
    }
    if ($downloadRequired) {
        Download-OpenAlAddon -Force
    }
    $context = Get-OpenAlContext -VerifyPayload
    $isRepair = Test-Path -LiteralPath $context.StatePath -PathType Leaf
    if (-not $isRepair -and (Test-Path -LiteralPath $context.BackupRoot)) {
        throw 'An incomplete OpenAL installation backup exists. Restore or remove it before installing.'
    }

    $state = $null
    if ($isRepair) {
        $state = Read-OpenAlState $context -RequireCurrentManifest -VerifyBackups
    }
    else {
        [void](New-Item -ItemType Directory -Path $context.BackupRoot -Force)
        try {
            $stateFiles = foreach ($file in @($context.Manifest.files)) {
                $destination = Resolve-ContainedPath $context.GameRoot ([string]$file.destination)
                if (Test-Path -LiteralPath $destination -PathType Container) {
                    throw "A directory blocks the OpenAL destination: $($file.destination)"
                }
                $existed = Test-Path -LiteralPath $destination -PathType Leaf
                $originalHash = $null
                if ($existed) {
                    $originalHash = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash
                    $backup = Resolve-ContainedPath $context.BackupRoot ([string]$file.destination)
                    Copy-FileWithParent $destination $backup
                }
                [pscustomobject]@{
                    source = [string]$file.source
                    destination = [string]$file.destination
                    sha256 = [string]$file.sha256
                    existed = $existed
                    originalSha256 = $originalHash
                }
            }
            $state = [pscustomobject]@{
                version = [string]$context.Manifest.version
                installedAt = [DateTime]::UtcNow.ToString('o')
                files = @($stateFiles)
            }
        }
        catch {
            Remove-Item -LiteralPath $context.BackupRoot -Recurse -Force -ErrorAction SilentlyContinue
            throw
        }
    }

    $transactionRoot = Join-Path $context.AddonRoot ('.openal-transaction-' + [guid]::NewGuid().ToString('N'))
    $snapshotRoot = Join-Path $transactionRoot 'current'
    [void](New-Item -ItemType Directory -Path $snapshotRoot -Force)
    $snapshot = @()
    try {
        foreach ($file in @($context.Manifest.files)) {
            $destination = Resolve-ContainedPath $context.GameRoot ([string]$file.destination)
            $existed = Test-Path -LiteralPath $destination -PathType Leaf
            $snapshotPath = Resolve-ContainedPath $snapshotRoot ([string]$file.destination)
            if ($existed) {
                Copy-FileWithParent $destination $snapshotPath
            }
            $snapshot += [pscustomobject]@{
                destination = [string]$file.destination
                existed = $existed
                snapshot = $snapshotPath
            }
        }
        foreach ($file in @($context.Manifest.files)) {
            $source = Resolve-ContainedPath (Join-Path $context.AddonRoot 'payload') ([string]$file.source)
            $destination = Resolve-ContainedPath $context.GameRoot ([string]$file.destination)
            Copy-FileWithParent $source $destination
            if ((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash -ne [string]$file.sha256) {
                throw "OpenAL installation verification failed: $($file.destination)"
            }
        }
        if (-not $isRepair) {
            $stateTemp = Join-Path $transactionRoot 'install-state.json'
            [IO.File]::WriteAllText(
                $stateTemp,
                ($state | ConvertTo-Json -Depth 6) + "`r`n",
                [Text.UTF8Encoding]::new($false)
            )
            [IO.File]::Copy($stateTemp, $context.StatePath, $true)
        }
    }
    catch {
        $originalError = $_
        try {
            Restore-OpenAlSnapshot $context $snapshot
        }
        catch {
            throw "$($originalError.Exception.Message) Rollback also failed: $($_.Exception.Message)"
        }
        if (-not $isRepair) {
            Remove-Item -LiteralPath $context.StatePath -Force -ErrorAction SilentlyContinue
            Remove-Item -LiteralPath $context.BackupRoot -Recurse -Force -ErrorAction SilentlyContinue
        }
        throw $originalError
    }
    finally {
        Remove-Item -LiteralPath $transactionRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
}

function Uninstall-OpenAlAddon {
    $context = Get-OpenAlContext
    $state = Read-OpenAlState $context -VerifyBackups
    $transactionRoot = Join-Path $context.AddonRoot ('.openal-transaction-' + [guid]::NewGuid().ToString('N'))
    $snapshotRoot = Join-Path $transactionRoot 'current'
    [void](New-Item -ItemType Directory -Path $snapshotRoot -Force)
    $snapshot = @()
    try {
        foreach ($entry in @($state.files)) {
            $destination = Resolve-ContainedPath $context.GameRoot ([string]$entry.destination)
            $existed = Test-Path -LiteralPath $destination -PathType Leaf
            $snapshotPath = Resolve-ContainedPath $snapshotRoot ([string]$entry.destination)
            if ($existed) {
                Copy-FileWithParent $destination $snapshotPath
            }
            $snapshot += [pscustomobject]@{
                destination = [string]$entry.destination
                existed = $existed
                snapshot = $snapshotPath
            }
        }
        $stateFiles = @($state.files)
        for ($index = $stateFiles.Count - 1; $index -ge 0; $index--) {
            $entry = $stateFiles[$index]
            $destination = Resolve-ContainedPath $context.GameRoot ([string]$entry.destination)
            if ([bool]$entry.existed) {
                $backup = Resolve-ContainedPath $context.BackupRoot ([string]$entry.destination)
                Copy-FileWithParent $backup $destination
            }
            elseif (Test-Path -LiteralPath $destination -PathType Leaf) {
                if ((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash -eq [string]$entry.sha256) {
                    Remove-Item -LiteralPath $destination -Force
                }
                else {
                    Write-Host "Preserved modified file: $($entry.destination)" -ForegroundColor Yellow
                }
            }
        }
    }
    catch {
        $originalError = $_
        try {
            Restore-OpenAlSnapshot $context $snapshot
        }
        catch {
            throw "$($originalError.Exception.Message) Rollback also failed: $($_.Exception.Message)"
        }
        throw $originalError
    }
    finally {
        Remove-Item -LiteralPath $transactionRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
    Remove-Item -LiteralPath $context.StatePath -Force
    Remove-Item -LiteralPath $context.BackupRoot -Recurse -Force
}

function Recover-OpenAlAddon {
    $context = Get-OpenAlContext
    if (Test-Path -LiteralPath $context.StatePath -PathType Leaf) {
        $validState = $false
        try {
            [void](Read-OpenAlState $context -VerifyBackups)
            $validState = $true
        }
        catch {
            $validState = $false
        }
        if ($validState) {
            throw 'A valid installation state exists. Use uninstall instead of recovery.'
        }
    }
    if (-not (Test-Path -LiteralPath $context.BackupRoot -PathType Container)) {
        throw 'No incomplete OpenAL installation backup was found.'
    }

    $transactionRoot = Join-Path $context.AddonRoot ('.openal-transaction-' + [guid]::NewGuid().ToString('N'))
    $snapshotRoot = Join-Path $transactionRoot 'current'
    [void](New-Item -ItemType Directory -Path $snapshotRoot -Force)
    $snapshot = @()
    try {
        foreach ($file in @($context.Manifest.files)) {
            $destination = Resolve-ContainedPath $context.GameRoot ([string]$file.destination)
            $existed = Test-Path -LiteralPath $destination -PathType Leaf
            $snapshotPath = Resolve-ContainedPath $snapshotRoot ([string]$file.destination)
            if ($existed) {
                Copy-FileWithParent $destination $snapshotPath
            }
            $snapshot += [pscustomobject]@{
                destination = [string]$file.destination
                existed = $existed
                snapshot = $snapshotPath
            }
        }
        foreach ($file in @($context.Manifest.files)) {
            $destination = Resolve-ContainedPath $context.GameRoot ([string]$file.destination)
            $backup = Resolve-ContainedPath $context.BackupRoot ([string]$file.destination)
            if (Test-Path -LiteralPath $backup -PathType Leaf) {
                Copy-FileWithParent $backup $destination
            }
            elseif ((Test-Path -LiteralPath $destination -PathType Leaf) -and
                (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash -eq [string]$file.sha256) {
                Remove-Item -LiteralPath $destination -Force
            }
        }
        Remove-Item -LiteralPath $context.StatePath -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath $context.BackupRoot -Recurse -Force
    }
    catch {
        $originalError = $_
        try {
            Restore-OpenAlSnapshot $context $snapshot
        }
        catch {
            throw "$($originalError.Exception.Message) Rollback also failed: $($_.Exception.Message)"
        }
        throw $originalError
    }
    finally {
        Remove-Item -LiteralPath $transactionRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
}

function Get-OpenAlAddonStatus {
    try {
        $root = Get-OpenAlRoot
        if (-not (Test-Path -LiteralPath $root.ManifestPath -PathType Leaf)) {
            $version = [Environment]::GetEnvironmentVariable('OpenAlAddonDownloadVersion')
            if ([string]::IsNullOrWhiteSpace($version)) {
                $version = 'unknown'
            }
            $status = if ((Test-Path -LiteralPath $root.StatePath) -or
                (Test-Path -LiteralPath $root.BackupRoot)) { 'Payload missing' } else { 'Not downloaded' }
            Write-Output "$status|$version"
            return
        }
        $context = Get-OpenAlContext
        if (-not (Test-Path -LiteralPath $context.StatePath -PathType Leaf)) {
            if (Test-Path -LiteralPath $context.BackupRoot) {
                Write-Output "Incomplete installation|$($context.Manifest.version)"
            }
            else {
                Write-Output "Not installed|$($context.Manifest.version)"
            }
            return
        }
        $state = Read-OpenAlState $context
        if ([string]$state.version -ne [string]$context.Manifest.version) {
            Write-Output "Different version|$($state.version)"
            return
        }
        foreach ($entry in @($state.files)) {
            $destination = Resolve-ContainedPath $context.GameRoot ([string]$entry.destination)
            if (-not (Test-Path -LiteralPath $destination -PathType Leaf) -or
                (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash -ne [string]$entry.sha256) {
                Write-Output "Modified|$($context.Manifest.version)"
                return
            }
        }
        Write-Output "Installed|$($context.Manifest.version)"
    }
    catch {
        if ($null -ne $root -and (Test-Path -LiteralPath $root.BackupRoot -PathType Container)) {
            $version = [Environment]::GetEnvironmentVariable('OpenAlAddonDownloadVersion')
            try {
                if (Test-Path -LiteralPath $root.ManifestPath -PathType Leaf) {
                    $version = [string](Get-Content -LiteralPath $root.ManifestPath -Raw | ConvertFrom-Json).version
                }
            }
            catch {
            }
            if ([string]::IsNullOrWhiteSpace($version)) {
                $version = 'unknown'
            }
            Write-Output "Incomplete installation|$version"
        }
        else {
            Write-Output 'Unavailable|unknown'
        }
    }
}

function Open-AudioConfigurator {
    $context = Get-OpenAlContext
    [void](Read-OpenAlState $context -RequireCurrentManifest)
    $executable = Resolve-ContainedPath $context.GameRoot 'mikohime\alsoft-config\alsoft-config.exe'
    if (-not (Test-Path -LiteralPath $executable -PathType Leaf)) {
        throw 'The OpenAL Soft configuration utility is not installed.'
    }
    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $executable
    $startInfo.WorkingDirectory = Split-Path -Parent $executable
    $startInfo.UseShellExecute = $true
    [Diagnostics.Process]::Start($startInfo) | Out-Null
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
        'GetOpenAlAddonStatus' {
            Get-OpenAlAddonStatus
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
        'InstallOpenAlAddon' {
            Install-OpenAlAddon
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
        'OpenAudioConfigurator' {
            Open-AudioConfigurator
        }
        'RecoverOpenAlAddon' {
            Recover-OpenAlAddon
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
        'UninstallOpenAlAddon' {
            Uninstall-OpenAlAddon
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

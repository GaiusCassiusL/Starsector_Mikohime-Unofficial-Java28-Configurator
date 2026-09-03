@ECHO OFF
SETLOCAL EnableExtensions DisableDelayedExpansion
TITLE Mikohime Java Configurator
CD /D "%~dp0"
if not "%CD:!=%"=="%CD%" (
    echo This configurator cannot safely run from a path containing an exclamation mark.
    echo Move or rename the Starsector folder, then run the configurator again.
    exit /b 1
)
SETLOCAL EnableDelayedExpansion

REM ============================================================================
REM Constants and generated JVM arguments
REM ============================================================================

set "SharedConfigRoot=configurator\shared"
if not exist "!SharedConfigRoot!\components.properties" set "SharedConfigRoot=%~dp0..\shared"
if not exist "!SharedConfigRoot!\components.properties" (
    echo Missing shared configurator data: components.properties
    exit /b 1
)

set "MinHeapPrefix=-Xms"
set "MaxHeapPrefix=-Xmx"
set "ThreadStackSize=-Xss4m"
set "LargePagesArgument=-XX:+UseLargePages"
set "Avx2Argument=-XX:+UseCMoveUnconditionally"

for /f "usebackq eol=# tokens=1,2,* delims=|" %%A in ("!SharedConfigRoot!\memory-presets.tsv") do (
    set "HeapValue[%%A]=%%B"
    set "HeapDescription[%%A]=%%C"
)
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("!SharedConfigRoot!\components.properties") do set "%%A=%%B"
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("!SharedConfigRoot!\java-versions.properties") do set "%%A=%%B"

set "BaseClassPath="
for /f "usebackq eol=# delims=" %%A in ("!SharedConfigRoot!\classpath.entries") do set "BaseClassPath=!BaseClassPath!;%%A"
set "BaseClassPath=!BaseClassPath:~1!"
set "BaseClasspathArgument=-classpath !BaseClassPath!"
set "FastRenderingAgent=-javaagent:fr.agent.jar"
set "FastRenderingClasspath=-classpath fr.jar;!BaseClassPath!"
set "ResourceCacheAgent=-javaagent:fr-resource-cache-agent.jar=gameRoot=.,installRoot=..,cacheDir=..\\fr-resource-cache,flushDelaySeconds=30,memoryCacheMiB=128,maxFileSize=1048576"
set "ResourceCacheDestination=starsector-core\fr-resource-cache-agent.jar"
set "Jdk27DownloadUrl=!Jdk27WindowsUrl!"
set "Jdk27Sha256=!Jdk27WindowsSha256!"
set "Jdk28DownloadUrl=!Jdk28WindowsUrl!"
set "Jdk28Sha256=!Jdk28WindowsSha256!"

set "SavesPathArgument=-Dcom.fs.starfarer.settings.paths.saves=..\\saves"
set "ScreenshotsPathArgument=-Dcom.fs.starfarer.settings.paths.screenshots=..\\screenshots"
set "ModsPathArgument=-Dcom.fs.starfarer.settings.paths.mods=..\\mods"
set "LogsPathArgument=-Dcom.fs.starfarer.settings.paths.logs=."
set "LauncherClass=com.fs.starfarer.StarfarerLauncher"

set "TransactionId=!RANDOM!_!RANDOM!_!RANDOM!"
set "LockDirectory=.configure_me.lock"
set "SimpleOutputFile=Miko_Simple.!TransactionId!.pending"
set "LauncherOutputFile=Miko_Rouge.!TransactionId!.pending"
set "LoggingOutputFile=mikohime\mikohime.properties.!TransactionId!.pending"
set "InfoOutputFile=Miko_Info.!TransactionId!.pending"
set "JavaVersionOutput=JavaVersionCheck_!TransactionId!.pending"
set "InputTransferFile=.configure_input.!TransactionId!.tmp"
set "JavaOptionCount=0"
set "PrepatcherCount=0"

call :InitializeColors
call :ValidateInstallation
if errorlevel 1 exit /b 1
call :AcquireConfiguratorLock
if errorlevel 1 exit /b 1
call :CheckWriteAccess
if errorlevel 1 (
    call :ReleaseConfiguratorLock
    exit /b 1
)
call :RefreshEnvironment

REM ============================================================================
REM Main menu and configuration workflow
REM ============================================================================

:MainMenu
cls
call :PrintHeader
call :PrintDetectedEnvironment
echo(
echo 1. Configure Java and launcher
echo 2. Change launcher background
echo 3. Refresh detected components
echo 4. Exit
if /I "!ModernJavaAvailable!"=="No" echo J. Download and install Java
if /I "!ResourceCacheAvailable!"=="No" echo R. Download and install FR Resource Cache
set "MissingDownloadAvailable=No"
if /I "!FastRenderingAvailable!"=="No" set "MissingDownloadAvailable=Yes"
if /I "!ResourceCacheAvailable!"=="No" set "MissingDownloadAvailable=Yes"
if "!PrepatcherCount!"=="0" set "MissingDownloadAvailable=Yes"
if /I "!VramOptimizerAvailable!"=="No" set "MissingDownloadAvailable=Yes"
if /I "!MissingDownloadAvailable!"=="Yes" echo D. Open missing component download page
echo(
if /I "!ModernJavaAvailable!"=="No" (
    if /I "!ResourceCacheAvailable!"=="No" (
        choice /c 1234JDR /n /m "Select an option: "
        if errorlevel 7 (
            call :InstallResourceCache
            goto :MainMenu
        )
        if errorlevel 6 (
            call :OpenMissingDownloadPage
            goto :MainMenu
        )
        if errorlevel 5 (
            call :InstallJavaMenu
            goto :MainMenu
        )
    ) else if /I "!MissingDownloadAvailable!"=="Yes" (
        choice /c 1234JD /n /m "Select an option: "
        if errorlevel 6 (
            call :OpenMissingDownloadPage
            goto :MainMenu
        )
        if errorlevel 5 (
            call :InstallJavaMenu
            goto :MainMenu
        )
    ) else (
        choice /c 1234J /n /m "Select an option: "
        if errorlevel 5 (
            call :InstallJavaMenu
            goto :MainMenu
        )
    )
) else (
    if /I "!ResourceCacheAvailable!"=="No" (
        choice /c 1234DR /n /m "Select an option: "
        if errorlevel 6 (
            call :InstallResourceCache
            goto :MainMenu
        )
        if errorlevel 5 (
            call :OpenMissingDownloadPage
            goto :MainMenu
        )
    ) else if /I "!MissingDownloadAvailable!"=="Yes" (
        choice /c 1234D /n /m "Select an option: "
        if errorlevel 5 (
            call :OpenMissingDownloadPage
            goto :MainMenu
        )
    ) else (
        choice /c 1234 /n /m "Select an option: "
    )
)
if errorlevel 4 goto :ExitConfigurator
if errorlevel 3 (
    call :RefreshEnvironment
    goto :MainMenu
)
if errorlevel 2 (
    call :BackgroundMenu
    goto :MainMenu
)
goto :ConfigureJava

:ConfigureJava
call :ResetSelections
call :ChooseJava
if errorlevel 2 goto :MainMenu

:ConfigureRendering
call :ResolvePrepatcherSelection
if errorlevel 2 goto :MainMenu
if errorlevel 1 goto :ConfigureJava
call :ChooseRendering
if errorlevel 2 goto :MainMenu
if errorlevel 1 (
    if not "!PrepatcherCount!"=="0" goto :ConfigureRendering
    goto :ConfigureJava
)

:ConfigureMemory
call :ChooseMemory
if errorlevel 2 goto :MainMenu
if errorlevel 1 goto :ConfigureRendering

:ConfigureCpu
call :ChooseCpuSettings
if errorlevel 2 goto :MainMenu
if errorlevel 1 goto :ConfigureMemory

:ConfigureSystem
call :ChooseSystemSettings
if errorlevel 2 goto :MainMenu
if errorlevel 1 goto :ConfigureCpu

:ConfigureLogging
call :ChooseLogging
if errorlevel 2 goto :MainMenu
if errorlevel 1 goto :ConfigureSystem

:ReviewConfiguration
call :WriteInfoPending
if errorlevel 1 goto :ConfigurationBuildFailed
cls
call :PrintHeader
echo Step 7 of 7 - Review
echo --------------------------------------------------------------------------
type "%InfoOutputFile%"
echo --------------------------------------------------------------------------
echo Y. Create the configuration
echo B. Back to logging options
echo R. Restart configuration
echo X. Cancel and return to the main menu
echo(
choice /c YBRX /n /m "Select an option: "
if errorlevel 4 (
    call :CleanupPendingFiles
    goto :MainMenu
)
if errorlevel 3 (
    call :CleanupPendingFiles
    goto :ConfigureJava
)
if errorlevel 2 (
    call :CleanupPendingFiles
    goto :ConfigureLogging
)

call :BuildPendingFiles
if errorlevel 1 goto :ConfigurationBuildFailed
call :CommitGeneratedFiles
if errorlevel 1 goto :ConfigurationCommitFailed
goto :ConfigurationComplete

:ConfigurationBuildFailed
call :CleanupPendingFiles
echo(
echo !ColorRed!Unable to build or validate the pending configuration.!ColorReset!
echo Existing configuration files were not replaced.
echo Check the installation files, free disk space, and folder permissions.
pause
goto :ReviewConfiguration

:ConfigurationCommitFailed
call :CleanupPendingFiles
echo(
echo !ColorRed!Unable to install the new configuration.!ColorReset!
echo Previous files were restored where possible.
echo Check free disk space and folder permissions before trying again.
pause
goto :MainMenu

:ConfigurationComplete
cls
call :PrintHeader
echo !ColorGreen!Configuration completed successfully.!ColorReset!
echo(
echo Created or updated:
echo   Miko_Simple.txt
echo   Miko_Rouge.bat
echo   Miko_Info.txt
echo   mikohime\mikohime.properties
echo(
echo Launch Starsector with Miko_Rouge.bat.
echo Administrator access is only needed when Windows protects the installation folder.
echo(
echo 1. Return to the main menu
echo 2. Exit
choice /c 12 /n /m "Select an option: "
if errorlevel 2 goto :ExitConfigurator
call :RefreshEnvironment
goto :MainMenu

:ExitConfigurator
call :CleanupJdkInstallation
call :CleanupResourceCacheInstallation
call :CleanupPendingFiles
call :ReleaseConfiguratorLock
exit /b 0

REM ============================================================================
REM User-interface helpers
REM ============================================================================

:InitializeColors
set "ColorGreen="
set "ColorYellow="
set "ColorRed="
set "ColorReset="
if defined NO_COLOR exit /b 0
for /f "delims=" %%E in ('echo prompt $E^| cmd') do set "AnsiEscape=%%E"
if not defined AnsiEscape exit /b 0
set "ColorGreen=!AnsiEscape![92m"
set "ColorYellow=!AnsiEscape![93m"
set "ColorRed=!AnsiEscape![91m"
set "ColorReset=!AnsiEscape![0m"
exit /b 0

:PrintHeader
echo ==============================================================================
echo                           Mikohime Java Configurator
echo   Currently maintained by Yue - Unofficial Configure_Me.cmd by Gaius Cassius
echo ==============================================================================
exit /b 0

:PrintDetectedEnvironment
echo Detected environment:
echo   Java installations : !JavaOptionCount!
if /I "!ModernJavaAvailable!"=="Yes" (
    echo   Java 27 or 28     : !ColorGreen!Installed!ColorReset!
) else (
    echo   Java 27 or 28     : !ColorYellow!Not found!ColorReset!
    echo     Recommendation : Download and install Java 27 or Java 28 ^(select J below^)
)
if /I "!FastRenderingAvailable!"=="Yes" (
    echo   Fast Rendering   : !ColorGreen!Installed!ColorReset!
) else (
    echo   Fast Rendering   : !ColorYellow!Not found!ColorReset!
    echo     Repository     : !FastRenderingReleasesUrl!
)
if /I "!ResourceCacheAvailable!"=="Yes" (
    echo   FR Resource Cache: !ColorGreen!Installed!ColorReset!
) else (
    echo   FR Resource Cache: !ColorYellow!Not found!ColorReset!
    echo     Repository     : !ResourceCacheReleasesUrl!
    echo     Install        : Select R below
)
if "!PrepatcherCount!"=="0" (
    echo   Prepatcher       : !ColorYellow!Not found!ColorReset!
    if not "!IncompatiblePrepatcherCount!"=="0" echo     Compatibility  : !IncompatiblePrepatcherCount! older or unreadable installation^(s^) ignored
    echo     Repository     : !PrepatcherReleasesUrl!
) else (
    echo   Prepatcher       : !ColorGreen!Installed!ColorReset!
    if not "!IncompatiblePrepatcherCount!"=="0" echo     Compatibility  : !IncompatiblePrepatcherCount! older or unreadable installation^(s^) ignored
)
if /I "!VramOptimizerAvailable!"=="Yes" (
    echo   VRAM Optimizer   : !ColorGreen!Installed!ColorReset!
) else (
    echo   VRAM Optimizer   : !ColorYellow!Recommended!ColorReset!
    echo     Repository     : !VramOptimizerReleasesUrl!
)
if defined PhysicalMemoryMiB (
    set /a "PhysicalMemoryGiB=(PhysicalMemoryMiB+1023)/1024"
    echo   Physical memory  : Approximately !PhysicalMemoryGiB! GB
) else (
    echo   Physical memory  : !ColorYellow!Unable to detect!ColorReset!
)
if defined PhysicalCoreCount (
    echo   Physical CPU cores : !PhysicalCoreCount!
) else (
    echo   Physical CPU cores : !ColorYellow!Unable to detect!ColorReset!
)
echo   Logical processors : !LogicalProcessorCount!
if /I "!UnsafeComponentNamesFound!"=="Yes" (
    echo   Component names   : !ColorYellow!Folders containing CMD metacharacters are ignored!ColorReset!
)
exit /b 0

:ResetSelections
set "JavaPath="
set "JavaVersion="
set "FastRenderingStatus=Disabled"
set "ResourceCacheStatus=Disabled"
set "SelectedHeapMiB="
set "SelectedHeapDescription="
set "LowCoreMode=No"
set "OldCpuMode=No"
set "LargePagesEnabled=No"
set "LoggingMode=Full"
call :SetAutomaticPrepatcherSelection
exit /b 0

:InstallJavaMenu
cls
call :PrintHeader
echo Download and install Java
echo --------------------------------------------------------------------------
echo The selected JDK will be downloaded and extracted beside starsector.exe.
echo No system-wide Java installation will be performed.
echo(
echo 1. Java 27 ^(27+22-ea, approximately 137 MB^)
echo 2. Java 28 ^(28+13-ea, approximately 153 MB^)
echo B. Back
echo(
choice /c 12B /n /m "Select an option: "
if errorlevel 3 exit /b 0
if errorlevel 2 (
    set "SelectedJdkName=Java 28 (28+13-ea)"
    set "SelectedJdkMajor=28"
    set "SelectedJdkUrl=!Jdk28DownloadUrl!"
    set "SelectedJdkSha256=!Jdk28Sha256!"
    set "SelectedJdkFolder=!Jdk28Folder!"
) else (
    set "SelectedJdkName=Java 27 (27+22-ea)"
    set "SelectedJdkMajor=27"
    set "SelectedJdkUrl=!Jdk27DownloadUrl!"
    set "SelectedJdkSha256=!Jdk27Sha256!"
    set "SelectedJdkFolder=!Jdk27Folder!"
)

if exist "!SelectedJdkFolder!\bin\java.exe" (
    call :GetJavaMajor "!SelectedJdkFolder!" ExistingJdkMajor
    if not errorlevel 1 if "!ExistingJdkMajor!"=="!SelectedJdkMajor!" (
        echo(
        echo !ColorGreen!!SelectedJdkName! is already installed.!ColorReset!
        pause
        call :RefreshEnvironment
        exit /b 0
    )
)
if exist "!SelectedJdkFolder!\." (
    echo(
    echo !ColorRed!The destination folder already exists but is not the expected JDK:!ColorReset!
    echo   !SelectedJdkFolder!
    echo Rename or remove that folder before trying again.
    pause
    exit /b 1
)

echo(
echo Install !SelectedJdkName! into:
echo   !CD!\!SelectedJdkFolder!
echo(
echo 1. Download and install
echo 2. Cancel
choice /c 12 /n /m "Select an option: "
if errorlevel 2 exit /b 0

call :DownloadSelectedJdk
if errorlevel 1 (
    echo(
    echo !ColorRed!Java installation failed.!ColorReset!
    echo No existing Java installation was changed.
    pause
    exit /b 1
)

call :RefreshEnvironment
echo(
echo !ColorGreen!!SelectedJdkName! was installed successfully.!ColorReset!
pause
exit /b 0

:DownloadSelectedJdk
set "JdkInstallRoot=.jdk-install-!TransactionId!"
set "JdkDownloadFile=!JdkInstallRoot!\jdk.zip"
set "JdkExtractFolder=!JdkInstallRoot!\extracted"
set "JdkDestinationCreated=No"
call :CleanupJdkInstallation
mkdir "!JdkExtractFolder!" 2>nul
if errorlevel 1 exit /b 1

echo(
echo Downloading !SelectedJdkName!...
echo This may take several minutes.

set "JDK_INSTALL_URL=!SelectedJdkUrl!"
set "JDK_INSTALL_HASH=!SelectedJdkSha256!"
set "JDK_INSTALL_ZIP=!CD!\!JdkDownloadFile!"
set "JDK_INSTALL_EXTRACT=!CD!\!JdkExtractFolder!"
set "JDK_INSTALL_DESTINATION=!CD!\!SelectedJdkFolder!"
set "JDK_INSTALL_MAJOR=!SelectedJdkMajor!"

powershell.exe -NoLogo -NoProfile -NonInteractive -Command "$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri $env:JDK_INSTALL_URL -OutFile $env:JDK_INSTALL_ZIP; $stream=[IO.File]::OpenRead($env:JDK_INSTALL_ZIP); try { $sha=[Security.Cryptography.SHA256]::Create(); try { $actualHash=([BitConverter]::ToString($sha.ComputeHash($stream))).Replace('-','') } finally { $sha.Dispose() } } finally { $stream.Dispose() }; if ($actualHash -ne $env:JDK_INSTALL_HASH) { throw 'The downloaded JDK checksum does not match.' }; Add-Type -AssemblyName System.IO.Compression.FileSystem; [IO.Compression.ZipFile]::ExtractToDirectory($env:JDK_INSTALL_ZIP,$env:JDK_INSTALL_EXTRACT); $javaFiles=@(Get-ChildItem -LiteralPath $env:JDK_INSTALL_EXTRACT -Filter java.exe -Recurse | Where-Object { -not $_.PSIsContainer -and $_.Directory.Name -eq 'bin' }); if ($javaFiles.Count -ne 1) { throw 'Unable to locate exactly one JDK installation in the archive.' }; $java=$javaFiles[0]; $startInfo=New-Object Diagnostics.ProcessStartInfo; $startInfo.FileName=$java.FullName; $startInfo.Arguments='-version'; $startInfo.UseShellExecute=$false; $startInfo.RedirectStandardError=$true; $startInfo.RedirectStandardOutput=$true; $process=[Diagnostics.Process]::Start($startInfo); $versionText=$process.StandardError.ReadToEnd() + $process.StandardOutput.ReadToEnd(); $process.WaitForExit(); if ($process.ExitCode -ne 0) { throw 'The downloaded Java executable failed its version check.' }; $versionMatch=[regex]::Match($versionText, 'version \"(\d+)'); if (-not $versionMatch.Success -or $versionMatch.Groups[1].Value -ne $env:JDK_INSTALL_MAJOR) { throw 'The downloaded archive contains the wrong Java version.' }; $jdkHome=$java.Directory.Parent.FullName; if (Test-Path -LiteralPath $env:JDK_INSTALL_DESTINATION) { throw 'The JDK destination was created while the download was running.' }; [IO.Directory]::Move($jdkHome,$env:JDK_INSTALL_DESTINATION)"
if errorlevel 1 (
    call :CleanupJdkInstallation
    exit /b 1
)
set "JdkDestinationCreated=Yes"
call :CleanupJdkInstallation

if not exist "!SelectedJdkFolder!\bin\java.exe" goto :DownloadedJdkInvalid
call :GetJavaMajor "!SelectedJdkFolder!" InstalledJdkMajor
if errorlevel 1 goto :DownloadedJdkInvalid
if not "!InstalledJdkMajor!"=="!SelectedJdkMajor!" goto :DownloadedJdkInvalid
exit /b 0

:DownloadedJdkInvalid
if /I "!JdkDestinationCreated!"=="Yes" powershell.exe -NoLogo -NoProfile -NonInteractive -Command "Remove-Item -LiteralPath $env:JDK_INSTALL_DESTINATION -Recurse -Force" 2>nul
exit /b 1

:CleanupJdkInstallation
if defined JdkInstallRoot if exist "!JdkInstallRoot!\." rmdir /S /Q "!JdkInstallRoot!" 2>nul
exit /b 0

:InstallResourceCache
cls
call :PrintHeader
echo Download and install FR Resource Cache
echo --------------------------------------------------------------------------
if exist "!ResourceCacheDestination!" (
    for %%F in ("!ResourceCacheDestination!") do if %%~zF GTR 0 (
        echo !ColorGreen!FR Resource Cache is already installed.!ColorReset!
        pause
        call :RefreshEnvironment
        exit /b 0
    )
    echo !ColorRed!The destination file already exists but is empty or invalid:!ColorReset!
    echo   !ResourceCacheDestination!
    echo Remove or rename that file before trying again.
    pause
    exit /b 1
)
echo The latest published release will be downloaded from:
echo   !ResourceCacheReleasesUrl!
echo(
echo The release digest and packaged agent JAR will be validated before the
echo JAR is installed into starsector-core.
echo(
echo 1. Download and install
echo 2. Cancel
choice /c 12 /n /m "Select an option: "
if errorlevel 2 exit /b 0

call :DownloadLatestResourceCache
if errorlevel 1 (
    echo(
    echo !ColorRed!FR Resource Cache installation failed.!ColorReset!
    echo No existing FR Resource Cache installation was changed.
    pause
    exit /b 1
)

call :RefreshEnvironment
if /I not "!ResourceCacheAvailable!"=="Yes" (
    echo(
    echo !ColorRed!The installed FR Resource Cache could not be detected.!ColorReset!
    pause
    exit /b 1
)
echo(
echo !ColorGreen!The latest FR Resource Cache release was installed successfully.!ColorReset!
pause
exit /b 0

:DownloadLatestResourceCache
set "ResourceCacheInstallRoot=.resource-cache-install-!TransactionId!"
set "ResourceCacheDownloadFile=!ResourceCacheInstallRoot!\release.zip"
set "ResourceCacheStagedJar=!ResourceCacheInstallRoot!\fr-resource-cache-agent.jar"
call :CleanupResourceCacheInstallation
mkdir "!ResourceCacheInstallRoot!" 2>nul
if errorlevel 1 exit /b 1

echo(
echo Finding the latest FR Resource Cache release...
set "RESOURCE_CACHE_API=!ResourceCacheLatestReleaseApi!"
set "RESOURCE_CACHE_ASSET_PREFIX=!ResourceCacheAssetUrlPrefix!"
set "RESOURCE_CACHE_ZIP=!CD!\!ResourceCacheDownloadFile!"
set "RESOURCE_CACHE_STAGED_JAR=!CD!\!ResourceCacheStagedJar!"
set "RESOURCE_CACHE_DESTINATION=!CD!\!ResourceCacheDestination!"

powershell.exe -NoLogo -NoProfile -NonInteractive -Command "$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $headers=@{ Accept='application/vnd.github+json'; 'User-Agent'='Mikohime-Java-Configurator'; 'X-GitHub-Api-Version'='2022-11-28' }; $release=Invoke-RestMethod -Uri $env:RESOURCE_CACHE_API -Headers $headers; $assets=@($release.assets | Where-Object { $_.name -like '*.zip' }); if ($assets.Count -ne 1) { throw 'The latest release must contain exactly one ZIP asset.' }; $asset=$assets[0]; if (-not ([string]$asset.browser_download_url).StartsWith($env:RESOURCE_CACHE_ASSET_PREFIX,[StringComparison]::OrdinalIgnoreCase)) { throw 'The release asset URL is not trusted.' }; $digestMatch=[regex]::Match([string]$asset.digest,'^sha256:([0-9a-fA-F]{64})$'); if (-not $digestMatch.Success) { throw 'GitHub did not provide a valid SHA-256 digest for the release asset.' }; if ([long]$asset.size -le 0 -or [long]$asset.size -gt 10MB) { throw 'The release asset size is invalid.' }; Invoke-WebRequest -UseBasicParsing -Uri $asset.browser_download_url -OutFile $env:RESOURCE_CACHE_ZIP; $stream=[IO.File]::OpenRead($env:RESOURCE_CACHE_ZIP); try { $sha=[Security.Cryptography.SHA256]::Create(); try { $actualHash=([BitConverter]::ToString($sha.ComputeHash($stream))).Replace('-','') } finally { $sha.Dispose() } } finally { $stream.Dispose() }; if ($actualHash -ne $digestMatch.Groups[1].Value) { throw 'The downloaded release checksum does not match GitHub''s digest.' }; Add-Type -AssemblyName System.IO.Compression.FileSystem; $archive=[IO.Compression.ZipFile]::OpenRead($env:RESOURCE_CACHE_ZIP); try { $entries=@($archive.Entries | Where-Object { [IO.Path]::GetFileName($_.FullName) -eq 'fr-resource-cache-agent.jar' -and $_.Length -gt 0 -and $_.Length -le 10MB }); if ($entries.Count -ne 1) { throw 'The release does not contain exactly one valid agent JAR.' }; $input=$entries[0].Open(); $output=[IO.File]::Create($env:RESOURCE_CACHE_STAGED_JAR); try { $input.CopyTo($output) } finally { $output.Dispose(); $input.Dispose() } } finally { $archive.Dispose() }; $jar=[IO.Compression.ZipFile]::OpenRead($env:RESOURCE_CACHE_STAGED_JAR); try { $manifest=$jar.GetEntry('META-INF/MANIFEST.MF'); if ($null -eq $manifest) { throw 'The agent JAR has no manifest.' }; $reader=New-Object IO.StreamReader($manifest.Open()); try { $manifestText=$reader.ReadToEnd() } finally { $reader.Dispose() }; if ($manifestText -notmatch '(?m)^Premain-Class:\s*dev\.frresourcecache\.FrResourceCacheAgent\s*$') { throw 'The agent JAR manifest is not valid.' } } finally { $jar.Dispose() }; if (Test-Path -LiteralPath $env:RESOURCE_CACHE_DESTINATION) { throw 'The destination file was created while the download was running.' }; [IO.File]::Move($env:RESOURCE_CACHE_STAGED_JAR,$env:RESOURCE_CACHE_DESTINATION)"
if errorlevel 1 (
    call :CleanupResourceCacheInstallation
    exit /b 1
)
call :CleanupResourceCacheInstallation
if not exist "!ResourceCacheDestination!" exit /b 1
for %%F in ("!ResourceCacheDestination!") do if %%~zF LEQ 0 exit /b 1
exit /b 0

:CleanupResourceCacheInstallation
if defined ResourceCacheInstallRoot if exist "!ResourceCacheInstallRoot!\." rmdir /S /Q "!ResourceCacheInstallRoot!" 2>nul
exit /b 0

:ChooseJava
:ChooseJavaAgain
cls
call :PrintHeader
echo Step 1 of 7 - Java
echo --------------------------------------------------------------------------
set "DisplayedJavaOptionCount=0"
if "!JavaOptionCount!"=="0" (
    echo No supported Java installations were detected automatically.
) else (
    set "DisplayedJavaOptionCount=!JavaOptionCount!"
    if !DisplayedJavaOptionCount! GTR 9 set "DisplayedJavaOptionCount=9"
    for /L %%N in (1,1,!DisplayedJavaOptionCount!) do (
        echo %%N. !JavaOptionDescription[%%N]! - !JavaOptionPath[%%N]!
    )
    if !JavaOptionCount! GTR 9 echo Additional installations are available through the custom-folder option.
)
echo C. Specify a Java folder manually
echo X. Cancel and return to the main menu
echo(
choice /c 123456789CX /n /m "Select an option: "
set "MenuChoice=!errorlevel!"
if "!MenuChoice!"=="11" exit /b 2
if "!MenuChoice!"=="10" goto :ChooseCustomJava
if !MenuChoice! GTR !DisplayedJavaOptionCount! (
    echo !ColorYellow!Invalid selection.!ColorReset!
    pause
    goto :ChooseJavaAgain
)
set "JavaPath=!JavaOptionPath[%MenuChoice%]!"
set "JavaVersion=!JavaOptionVersion[%MenuChoice%]!"
exit /b 0

:ChooseCustomJava
cls
call :PrintHeader
echo Step 1 of 7 - Custom Java folder
echo --------------------------------------------------------------------------
echo Type one folder name located directly inside the Starsector folder, then
echo press Enter. Do not enter a full path. Enter X to cancel.
echo(
set "CustomJavaPath="
setlocal DisableDelayedExpansion
set "CustomJavaInput="
set /p "CustomJavaInput=Folder name: "
> "%InputTransferFile%" set CustomJavaInput 2>nul
endlocal
set "UnsafeInputFound=No"
for /f %%U in ('powershell.exe -NoLogo -NoProfile -NonInteractive -Command "$t=[IO.File]::ReadAllText('!InputTransferFile!'); $bad=[char[]](33,34,37,38,40,41,60,62,94,124); if ($t.IndexOfAny($bad) -ge 0) { 'Yes' }" 2^>nul') do set "UnsafeInputFound=%%U"
if /I "!UnsafeInputFound!"=="Yes" (
    del /Q "!InputTransferFile!" 2>nul
    goto :UnsafeCustomJava
)
for /f "usebackq tokens=1,* delims==" %%A in ("!InputTransferFile!") do if /I "%%A"=="CustomJavaInput" set "CustomJavaPath=%%B"
del /Q "!InputTransferFile!" 2>nul
if /I "!CustomJavaPath!"=="X" exit /b 2
if not defined CustomJavaPath goto :InvalidCustomJava
if "!CustomJavaPath!"=="." goto :InvalidCustomJava
if "!CustomJavaPath!"==".." goto :InvalidCustomJava
if not "!CustomJavaPath:\=!"=="!CustomJavaPath!" goto :InvalidCustomJava
if not "!CustomJavaPath:/=!"=="!CustomJavaPath!" goto :InvalidCustomJava
if not "!CustomJavaPath::=!"=="!CustomJavaPath!" goto :InvalidCustomJava
if not exist "!CustomJavaPath!\bin\java.exe" goto :InvalidCustomJava
call :GetJavaMajor "!CustomJavaPath!" DetectedCustomJavaMajor
if errorlevel 1 (
    echo Unable to run "!CustomJavaPath!\bin\java.exe" and determine its version.
    pause
    goto :ChooseCustomJava
)
call :IsSupportedJavaMajor "!DetectedCustomJavaMajor!"
if errorlevel 1 (
    echo Java !DetectedCustomJavaMajor! is not supported by this configurator.
    echo Supported versions: Java 17, Java 27, and Java 28.
    pause
    goto :ChooseCustomJava
)
set "JavaPath=!CustomJavaPath!"
set "JavaVersion=!DetectedCustomJavaMajor!"
exit /b 0

:InvalidCustomJava
echo !ColorYellow!Invalid Java folder.!ColorReset!
echo Use one immediate folder name containing bin\java.exe.
pause
goto :ChooseCustomJava

:UnsafeCustomJava
echo !ColorYellow!Folder names containing CMD metacharacters are not supported.!ColorReset!
echo Rename the Java folder or choose another installation.
pause
goto :ChooseCustomJava

:ResolvePrepatcherSelection
if "!PrepatcherCount!"=="0" exit /b 0

:ChoosePrepatcherAgain
cls
call :PrintHeader
echo Step 2 of 7 - StarsectorPrepatcher
echo --------------------------------------------------------------------------
if "!PrepatcherCount!"=="1" (
    echo StarsectorPrepatcher was detected.
) else (
    echo Multiple StarsectorPrepatcher installations were detected.
)
echo Select the installation to enable, or disable StarsectorPrepatcher:
echo(
set "DisplayedPrepatcherCount=!PrepatcherCount!"
if !DisplayedPrepatcherCount! GTR 9 set "DisplayedPrepatcherCount=9"
for /L %%N in (1,1,!DisplayedPrepatcherCount!) do echo %%N. Enable !PrepatcherCandidate[%%N]! ^(v!PrepatcherCandidateVersion[%%N]!^)
if !PrepatcherCount! GTR 9 echo Only the first nine installations can be selected.
echo D. Disable StarsectorPrepatcher
echo B. Back
echo X. Cancel and return to the main menu
echo(
choice /c 123456789DBX /n /m "Select an option: "
set "MenuChoice=!errorlevel!"
if "!MenuChoice!"=="12" exit /b 2
if "!MenuChoice!"=="11" exit /b 1
if "!MenuChoice!"=="10" (
    set "PrepatcherFolder="
    set "PrepatcherAgent="
    set "PrepatcherStatus=Disabled"
    exit /b 0
)
if !MenuChoice! GTR !DisplayedPrepatcherCount! (
    echo !ColorYellow!Invalid selection.!ColorReset!
    pause
    goto :ChoosePrepatcherAgain
)
call :SelectPrepatcher "!PrepatcherCandidate[%MenuChoice%]!"
set "PrepatcherStatus=Enabled"
exit /b 0

:ChooseRendering
:ChooseRenderingAgain
cls
call :PrintHeader
echo Step 2 of 7 - Optional components
echo --------------------------------------------------------------------------
if /I "!PrepatcherStatus!"=="Enabled" (
    echo StarsectorPrepatcher: !ColorGreen!Enabled ^(!PrepatcherFolder!^)!ColorReset!
) else if not "!PrepatcherCount!"=="0" (
    echo StarsectorPrepatcher: Disabled
) else (
    echo StarsectorPrepatcher: !ColorYellow!Not installed!ColorReset!
    echo Download: !PrepatcherReleasesUrl!
)
echo(
if /I not "!FastRenderingAvailable!"=="Yes" (
    set "FastRenderingStatus=Disabled"
    set "ResourceCacheStatus=Disabled"
    echo !ColorYellow!Fast Rendering was not found and will remain disabled.!ColorReset!
    echo Download: !FastRenderingReleasesUrl!
    echo(
    echo C. Continue
    echo D. Open missing component download page
    echo B. Back
    echo X. Cancel and return to the main menu
    choice /c CDBX /n /m "Select an option: "
    if errorlevel 4 exit /b 2
    if errorlevel 3 exit /b 1
    if errorlevel 2 (
        call :OpenMissingDownloadPage
        goto :ChooseRenderingAgain
    )
    exit /b 0
)

echo Fast Rendering was detected. Enable it?
echo 1. Yes
echo 2. No
if "!PrepatcherCount!"=="0" echo D. Open Prepatcher download page
echo B. Back
echo X. Cancel and return to the main menu
if "!PrepatcherCount!"=="0" (
    choice /c 12DBX /n /m "Select an option: "
    if errorlevel 5 exit /b 2
    if errorlevel 4 exit /b 1
    if errorlevel 3 (
        call :OpenMissingDownloadPage
        goto :ChooseRenderingAgain
    )
) else (
    choice /c 12BX /n /m "Select an option: "
    if errorlevel 4 exit /b 2
    if errorlevel 3 exit /b 1
)
if errorlevel 2 (
    set "FastRenderingStatus=Disabled"
    set "ResourceCacheStatus=Disabled"
    exit /b 0
)
set "FastRenderingStatus=Enabled"

if /I not "!ResourceCacheAvailable!"=="Yes" (
    set "ResourceCacheStatus=Disabled"
    echo(
    echo !ColorYellow!FR Resource Cache was not found and will remain disabled.!ColorReset!
    echo Download: !ResourceCacheReleasesUrl!
    echo(
    echo C. Continue
    echo R. Download and install FR Resource Cache
    echo D. Open FR Resource Cache download page
    echo B. Back
    echo X. Cancel and return to the main menu
    choice /c CRDBX /n /m "Select an option: "
    if errorlevel 5 exit /b 2
    if errorlevel 4 goto :ChooseRenderingAgain
    if errorlevel 3 (
        start "" "!ResourceCacheReleasesUrl!"
        if errorlevel 1 echo !ColorRed!Unable to open the FR Resource Cache download page.!ColorReset!
        goto :ChooseRenderingAgain
    )
    if errorlevel 2 (
        call :InstallResourceCache
        goto :ChooseRenderingAgain
    )
    exit /b 0
)
echo(
echo FR Resource Cache was detected. Enable it?
echo 1. Yes
echo 2. No
echo B. Back
echo X. Cancel and return to the main menu
choice /c 12BX /n /m "Select an option: "
if errorlevel 4 exit /b 2
if errorlevel 3 goto :ChooseRenderingAgain
if errorlevel 2 (
    set "ResourceCacheStatus=Disabled"
) else (
    set "ResourceCacheStatus=Enabled"
)
exit /b 0

:OpenMissingDownloadPage
set "MissingDownloadCount=0"
if /I "!FastRenderingAvailable!"=="No" set /a MissingDownloadCount+=1
if /I "!ResourceCacheAvailable!"=="No" set /a MissingDownloadCount+=1
if "!PrepatcherCount!"=="0" set /a MissingDownloadCount+=1
if /I "!VramOptimizerAvailable!"=="No" set /a MissingDownloadCount+=1
if !MissingDownloadCount! GTR 1 goto :ChooseMissingDownload
if /I "!FastRenderingAvailable!"=="No" (
    start "" "!FastRenderingReleasesUrl!"
    if errorlevel 1 echo !ColorRed!Unable to open the Fast Rendering download page.!ColorReset!
    exit /b 0
)
if /I "!ResourceCacheAvailable!"=="No" (
    start "" "!ResourceCacheReleasesUrl!"
    if errorlevel 1 echo !ColorRed!Unable to open the FR Resource Cache download page.!ColorReset!
    exit /b 0
)
if "!PrepatcherCount!"=="0" (
    start "" "!PrepatcherReleasesUrl!"
    if errorlevel 1 echo !ColorRed!Unable to open the StarsectorPrepatcher download page.!ColorReset!
    exit /b 0
)
if /I "!VramOptimizerAvailable!"=="No" (
    start "" "!VramOptimizerReleasesUrl!"
    if errorlevel 1 echo !ColorRed!Unable to open the VRAM Optimizer download page.!ColorReset!
)
exit /b 0

:ChooseMissingDownload
cls
call :PrintHeader
echo Open a download page
echo --------------------------------------------------------------------------
echo 1. Fast Rendering
echo 2. FR Resource Cache
echo 3. StarsectorPrepatcher
echo 4. VRAM Optimizer
echo B. Back
choice /c 1234B /n /m "Select an option: "
if errorlevel 5 exit /b 0
if errorlevel 4 (
    start "" "!VramOptimizerReleasesUrl!"
) else if errorlevel 3 (
    start "" "!PrepatcherReleasesUrl!"
) else if errorlevel 2 (
    start "" "!ResourceCacheReleasesUrl!"
) else (
    start "" "!FastRenderingReleasesUrl!"
)
if errorlevel 1 echo !ColorRed!Unable to open the download page.!ColorReset!
exit /b 0

:ChooseMemory
:ChooseMemoryAgain
cls
call :PrintHeader
echo Step 3 of 7 - Memory
echo --------------------------------------------------------------------------
if defined PhysicalMemoryMiB (
    echo Detected physical memory: approximately !PhysicalMemoryGiB! GB
) else (
    echo Physical memory could not be detected.
)
echo(
echo 1. 2 GB  - Vanilla default
echo 2. 3 GB
echo 3. 4 GB  - Recommended starting point
echo 4. 6 GB
echo 5. 8 GB
echo 6. 10 GB
echo C. Enter a custom amount
echo A. Advanced memory options ^(11-26 GB^)
echo B. Back
echo X. Cancel and return to the main menu
echo(
choice /c 123456CABX /n /m "Select an option: "
set "MenuChoice=!errorlevel!"
if "!MenuChoice!"=="10" exit /b 2
if "!MenuChoice!"=="9" exit /b 1
if "!MenuChoice!"=="8" goto :ChooseAdvancedMemory
if "!MenuChoice!"=="7" goto :ChooseCustomMemory
call :SetHeapSelection "!MenuChoice!"
call :ConfirmMemorySelection
if errorlevel 1 goto :ChooseMemoryAgain
exit /b 0

:ChooseCustomMemory
echo(
echo Type the memory limit in MB, from 512 through 1048576, then press Enter.
echo Enter B to return to the memory menu.
set "CustomHeapMiB="
setlocal DisableDelayedExpansion
set "CustomHeapInput="
set /p "CustomHeapInput=Memory in MB: "
> "%InputTransferFile%" set CustomHeapInput 2>nul
endlocal
set "UnsafeInputFound=No"
for /f %%U in ('powershell.exe -NoLogo -NoProfile -NonInteractive -Command "$t=[IO.File]::ReadAllText('!InputTransferFile!'); $bad=[char[]](33,34,37,38,40,41,60,62,94,124); if ($t.IndexOfAny($bad) -ge 0) { 'Yes' }" 2^>nul') do set "UnsafeInputFound=%%U"
if /I "!UnsafeInputFound!"=="Yes" (
    del /Q "!InputTransferFile!" 2>nul
    goto :InvalidCustomMemory
)
for /f "usebackq tokens=1,* delims==" %%A in ("!InputTransferFile!") do if /I "%%A"=="CustomHeapInput" set "CustomHeapMiB=%%B"
del /Q "!InputTransferFile!" 2>nul
if /I "!CustomHeapMiB!"=="B" goto :ChooseMemoryAgain
call :ValidatePositiveInteger "!CustomHeapMiB!"
if errorlevel 1 goto :InvalidCustomMemory
if !CustomHeapMiB! LSS 512 goto :InvalidCustomMemory
if !CustomHeapMiB! GTR 1048576 goto :InvalidCustomMemory
set "SelectedHeapMiB=!CustomHeapMiB!"
set "SelectedHeapDescription=!CustomHeapMiB! MB (custom)"
call :ConfirmMemorySelection
if errorlevel 1 goto :ChooseMemoryAgain
exit /b 0

:InvalidCustomMemory
echo !ColorYellow!Invalid memory amount. Enter a whole number from 512 through 1048576.!ColorReset!
pause
goto :ChooseCustomMemory

:ChooseAdvancedMemory
cls
call :PrintHeader
echo Step 3 of 7 - Advanced memory
echo --------------------------------------------------------------------------
echo These settings are intended for unusually large mod lists and systems
echo with substantially more physical RAM than the selected heap.
echo(
echo 1. 11 GB
echo 2. 16 GB
echo 3. 20 GB
echo 4. 26 GB
echo E. Extreme memory options ^(64/128 GB; not recommended^)
echo B. Back
echo X. Cancel and return to the main menu
echo(
choice /c 1234EBX /n /m "Select an option: "
set "MenuChoice=!errorlevel!"
if "!MenuChoice!"=="7" exit /b 2
if "!MenuChoice!"=="6" goto :ChooseMemoryAgain
if "!MenuChoice!"=="5" goto :ChooseExtremeMemory
set /a "HeapIndex=MenuChoice+10"
call :SetHeapSelection "!HeapIndex!"
call :ConfirmMemorySelection
if errorlevel 1 goto :ChooseAdvancedMemory
exit /b 0

:ChooseExtremeMemory
cls
call :PrintHeader
echo Step 3 of 7 - Extreme memory
echo --------------------------------------------------------------------------
echo !ColorYellow!WARNING: These heap sizes are unnecessary for almost all Starsector setups.!ColorReset!
echo They require substantially more physical RAM than the selected heap and
echo may reduce performance by increasing garbage-collection work.
echo(
echo 1. 64 GB
echo 2. 128 GB
echo B. Back
echo X. Cancel and return to the main menu
choice /c 12BX /n /m "Select an option: "
if errorlevel 4 exit /b 2
if errorlevel 3 goto :ChooseAdvancedMemory
set "MenuChoice=!errorlevel!"
set /a "HeapIndex=MenuChoice+14"
call :SetHeapSelection "!HeapIndex!"
call :ConfirmMemorySelection
if errorlevel 1 goto :ChooseExtremeMemory
exit /b 0

:SetHeapSelection
set "SelectedHeapMiB=!HeapValue[%~1]!"
set "SelectedHeapDescription=!HeapDescription[%~1]!"
exit /b 0

:ConfirmMemorySelection
if not defined PhysicalMemoryMiB exit /b 0
if not defined SafeHeapMiB exit /b 0
if !SelectedHeapMiB! LEQ !SafeHeapMiB! exit /b 0
echo(
echo !ColorYellow!WARNING: The selected heap is !SelectedHeapMiB! MB. The recommended maximum
echo for this system is !SafeHeapMiB! MB ^(75%% of !PhysicalMemoryMiB! MB physical RAM^).
echo Windows, JVM native memory, mods, and other programs need the remaining RAM.
echo !ColorReset!
echo 1. Choose a different amount
echo 2. Use this amount anyway
choice /c 12 /n /m "Select an option: "
if errorlevel 2 exit /b 0
exit /b 1

:ChooseCpuSettings
:ChooseCpuSettingsAgain
cls
call :PrintHeader
echo Step 4 of 7 - CPU tuning
echo --------------------------------------------------------------------------
if !LogicalProcessorCount! LEQ 4 (
    set "RecommendedLowCoreMode=Yes"
) else (
    set "RecommendedLowCoreMode=No"
)
if defined PhysicalCoreCount (
    echo Detected physical CPU cores: !PhysicalCoreCount!
) else (
    echo Detected physical CPU cores: Unable to detect
)
echo Available logical processors: !LogicalProcessorCount!
echo Low-core tuning uses the logical processors currently available to the JVM,
echo including any Windows affinity or resource limits.
echo Recommended low-core tuning: !RecommendedLowCoreMode!
echo(
echo 1. Use the recommended setting
echo 2. Override the recommendation
echo B. Back
echo X. Cancel and return to the main menu
choice /c 12BX /n /m "Select an option: "
if errorlevel 4 exit /b 2
if errorlevel 3 exit /b 1
if errorlevel 2 (
    if /I "!RecommendedLowCoreMode!"=="Yes" (
        set "LowCoreMode=No"
    ) else (
        set "LowCoreMode=Yes"
    )
) else (
    set "LowCoreMode=!RecommendedLowCoreMode!"
)

set "OldCpuMode=No"
if /I not "!LowCoreMode!"=="Yes" exit /b 0
echo(
echo Does this system use an older Intel Core 1000, 2000, or 3000-series CPU?
echo Select Yes if Starsector otherwise fails with error 0xc000001d.
echo 1. No
echo 2. Yes
echo B. Back
echo X. Cancel and return to the main menu
choice /c 12BX /n /m "Select an option: "
if errorlevel 4 exit /b 2
if errorlevel 3 goto :ChooseCpuSettingsAgain
if errorlevel 2 set "OldCpuMode=Yes"
exit /b 0

:ChooseSystemSettings
cls
call :PrintHeader
echo Step 5 of 7 - Windows options
echo --------------------------------------------------------------------------
echo Large Pages may improve performance on supported Windows editions.
echo Status: !LargePagesPrivilegeStatus!
echo With -Xms equal to -Xmx and AlwaysPreTouch enabled, Java reserves and
echo commits the entire selected heap at startup. Large heaps require enough
echo contiguous large-page memory and may fail even when the privilege is assigned.
echo(
echo 1. Enable Large Pages
echo 2. Disable Large Pages - Recommended
echo B. Back
echo X. Cancel and return to the main menu
choice /c 12BX /n /m "Select an option: "
if errorlevel 4 exit /b 2
if errorlevel 3 exit /b 1
if errorlevel 2 (
    set "LargePagesEnabled=No"
) else (
    if /I "!LargePagesPrivilegeStatus!"=="Not assigned" (
        echo(
        echo !ColorRed!Large Pages cannot be enabled because Lock pages in memory is not
        echo assigned to this account. Assign the policy, sign out, and sign in again.!ColorReset!
        pause
        goto :ChooseSystemSettings
    )
    echo(
    echo !ColorYellow!Java will attempt to commit the full !SelectedHeapMiB! MB heap at startup.!ColorReset!
    echo 1. Enable Large Pages
    echo 2. Keep Large Pages disabled
    choice /c 12 /n /m "Confirm: "
    if errorlevel 2 (
        set "LargePagesEnabled=No"
    ) else (
        set "LargePagesEnabled=Yes"
    )
)
exit /b 0

:ChooseLogging
cls
call :PrintHeader
echo Step 6 of 7 - Logging
echo --------------------------------------------------------------------------
echo 1. Full console and file logging
echo 2. Reduced routine console output; retain file logging
echo 3. Reduced console output and disabled JVM diagnostics; retain file logging
echo B. Back
echo X. Cancel and return to the main menu
choice /c 123BX /n /m "Select an option: "
if errorlevel 5 exit /b 2
if errorlevel 4 exit /b 1
if errorlevel 3 (
    set "LoggingMode=Minimal"
) else if errorlevel 2 (
    set "LoggingMode=Reduced"
) else (
    set "LoggingMode=Full"
)
exit /b 0

:BackgroundMenu
cls
call :PrintHeader
echo Launcher background
echo --------------------------------------------------------------------------
echo 1. Default Mikohime 25+
echo 2. Mikosector
echo 3. Mimikko
echo 4. Gamma
echo B. Back
choice /c 1234B /n /m "Select an option: "
if errorlevel 5 exit /b 0
if errorlevel 4 (
    call :CopyBackground "mikohime\bg\gamma_bg.jpg" "Gamma"
) else if errorlevel 3 (
    call :CopyBackground "mikohime\bg\mimikko_bg.jpg" "Mimikko"
) else if errorlevel 2 (
    call :CopyBackground "mikohime\bg\pather_bg.jpg" "Mikosector"
) else (
    call :CopyBackground "mikohime\bg\default_bg.jpg" "Default Mikohime 25+"
)
pause
exit /b 0

:CopyBackground
if not exist "%~1" (
    echo Background source is missing: %~1
    exit /b 1
)
copy /Y "%~1" "mikohime\launcher_bg.jpg" >nul
if errorlevel 1 (
    echo Failed to install the %~2 background.
    exit /b 1
)
echo Installed the %~2 background.
exit /b 0

REM ============================================================================
REM Environment detection
REM ============================================================================

:RefreshEnvironment
call :DetectUnsafeComponentNames
call :DetectJavaInstallations
call :DetectOptionalComponents
call :DetectSystemResources
exit /b 0

:DetectJavaInstallations
for /L %%N in (1,1,!JavaOptionCount!) do (
    set "JavaOptionPath[%%N]="
    set "JavaOptionVersion[%%N]="
    set "JavaOptionDescription[%%N]="
)
set "JavaOptionCount=0"
set "ModernJavaAvailable=No"
if exist "jre\bin\java.exe" call :ProbeJavaFolder "jre" "Starsector bundled Java"
for /f "usebackq delims=" %%D in (`powershell.exe -NoLogo -NoProfile -NonInteractive -Command "$bad=[char[]](33,34,37,38,40,41,60,62,94,124); Get-ChildItem -Directory -Filter 'jdk-27*' | Where-Object { $_.Name.IndexOfAny($bad) -lt 0 } | ForEach-Object Name" 2^>nul`) do if exist "%%D\bin\java.exe" call :ProbeJavaFolder "%%D" "Java installation"
for /f "usebackq delims=" %%D in (`powershell.exe -NoLogo -NoProfile -NonInteractive -Command "$bad=[char[]](33,34,37,38,40,41,60,62,94,124); Get-ChildItem -Directory -Filter 'jdk-28*' | Where-Object { $_.Name.IndexOfAny($bad) -lt 0 } | ForEach-Object Name" 2^>nul`) do if exist "%%D\bin\java.exe" call :ProbeJavaFolder "%%D" "Java installation"
exit /b 0

:DetectUnsafeComponentNames
set "UnsafeComponentNamesFound=No"
for /f %%U in ('powershell.exe -NoLogo -NoProfile -NonInteractive -Command "$bad=[char[]](33,34,37,38,40,41,60,62,94,124); if (Get-ChildItem -Directory | Where-Object { ($_.Name -like 'jdk-27*' -or $_.Name -like 'jdk-28*') -and $_.Name.IndexOfAny($bad) -ge 0 }) { 'Yes' } elseif (Test-Path 'mods') { if (Get-ChildItem 'mods' -Directory -Filter 'StarsectorPrepatcher*' | Where-Object { $_.Name.IndexOfAny($bad) -ge 0 }) { 'Yes' } }" 2^>nul') do set "UnsafeComponentNamesFound=%%U"
exit /b 0

:ProbeJavaFolder
call :GetJavaMajor "%~1" ProbedJavaMajor
if errorlevel 1 exit /b 0
call :IsSupportedJavaMajor "!ProbedJavaMajor!"
if errorlevel 1 exit /b 0
if "!ProbedJavaMajor!"=="27" set "ModernJavaAvailable=Yes"
if "!ProbedJavaMajor!"=="28" set "ModernJavaAvailable=Yes"
set /a JavaOptionCount+=1
set "JavaOptionPath[!JavaOptionCount!]=%~1"
set "JavaOptionVersion[!JavaOptionCount!]=!ProbedJavaMajor!"
set "JavaOptionDescription[!JavaOptionCount!]=%~2 - Java !ProbedJavaMajor!"
exit /b 0

:GetJavaMajor
set "DetectedJavaVersion="
set "DetectedJavaMajor="
del /Q "!JavaVersionOutput!" 2>nul
"%~1\bin\java.exe" -version >"!JavaVersionOutput!" 2>&1
if errorlevel 1 goto :GetJavaMajorFailed
for /f tokens^=2^ delims^=^" %%V in ('findstr /C:"version " "!JavaVersionOutput!"') do if not defined DetectedJavaVersion set "DetectedJavaVersion=%%V"
if not defined DetectedJavaVersion goto :GetJavaMajorFailed
for /f "tokens=1 delims=.-+" %%M in ("!DetectedJavaVersion!") do set "DetectedJavaMajor=%%M"
del /Q "!JavaVersionOutput!" 2>nul
set "%~2=!DetectedJavaMajor!"
exit /b 0

:GetJavaMajorFailed
del /Q "!JavaVersionOutput!" 2>nul
set "%~2="
exit /b 1

:IsSupportedJavaMajor
if "%~1"=="17" exit /b 0
if "%~1"=="27" exit /b 0
if "%~1"=="28" exit /b 0
exit /b 1

:DetectOptionalComponents
set "FastRenderingAvailable=No"
if exist "starsector-core\fr.jar" if exist "starsector-core\fr.agent.jar" set "FastRenderingAvailable=Yes"

set "ResourceCacheAvailable=No"
if exist "starsector-core\fr-resource-cache-agent.jar" (
    for %%F in ("starsector-core\fr-resource-cache-agent.jar") do if %%~zF GTR 0 set "ResourceCacheAvailable=Yes"
)

set "VramOptimizerAvailable=No"
for /f %%V in ('powershell.exe -NoLogo -NoProfile -NonInteractive -Command "if (Test-Path 'mods') { foreach ($directory in Get-ChildItem 'mods' -Directory) { $metadata=Join-Path $directory.FullName 'mod_info.json'; if ((Test-Path -LiteralPath $metadata -PathType Leaf) -and ([IO.File]::ReadAllText($metadata) -match '(?im)\"id\"\s*:\s*\"VramOptimizer\"')) { 'Yes'; break } } }" 2^>nul') do set "VramOptimizerAvailable=%%V"

for /L %%N in (1,1,!PrepatcherCount!) do (
    set "PrepatcherCandidate[%%N]="
    set "PrepatcherCandidateVersion[%%N]="
)
set "PrepatcherCount=0"
set "IncompatiblePrepatcherCount=0"
set "PrepatcherFolder="
set "PrepatcherAgent="
set "PrepatcherStatus=Disabled"
for /f "usebackq delims=" %%D in (`powershell.exe -NoLogo -NoProfile -NonInteractive -Command "$bad=[char[]](33,34,37,38,40,41,60,62,94,124); if (Test-Path 'mods') { Get-ChildItem 'mods' -Directory -Filter 'StarsectorPrepatcher*' | Where-Object { $_.Name.IndexOfAny($bad) -lt 0 } | ForEach-Object Name }" 2^>nul`) do call :AddPrepatcherCandidate "%%D"
call :SetAutomaticPrepatcherSelection
exit /b 0

:AddPrepatcherCandidate
if not exist "mods\%~1\agent\StarsectorPrepatcherAgent.jar" exit /b 0
for %%F in ("mods\%~1\agent\StarsectorPrepatcherAgent.jar") do if %%~zF LEQ 0 exit /b 0
set "PrepatcherMetadataPath=mods\%~1\mod_info.json"
set "PrepatcherVersionStatus="
set "DetectedPrepatcherVersion="
for /f "tokens=1,2 delims=|" %%S in ('powershell.exe -NoLogo -NoProfile -NonInteractive -Command "$minimum=[version]$env:MinimumPrepatcherVersion; $path=$env:PrepatcherMetadataPath; if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { 'Unreadable|' } else { try { $raw=[string](Get-Content -LiteralPath $path -Raw | ConvertFrom-Json).version; $match=[regex]::Match($raw, '\d+(?:\.\d+){1,3}'); if (-not $match.Success) { 'Unreadable|' } else { $version=[version]$match.Value; if ($version -ge $minimum) { 'Compatible|' + $version.ToString() } else { 'Incompatible|' + $version.ToString() } } } catch { 'Unreadable|' } }" 2^>nul') do (
    set "PrepatcherVersionStatus=%%S"
    set "DetectedPrepatcherVersion=%%T"
)
if /I not "!PrepatcherVersionStatus!"=="Compatible" (
    set /a IncompatiblePrepatcherCount+=1
    exit /b 0
)
set /a PrepatcherCount+=1
set "PrepatcherCandidate[!PrepatcherCount!]=%~1"
set "PrepatcherCandidateVersion[!PrepatcherCount!]=!DetectedPrepatcherVersion!"
exit /b 0

:SetAutomaticPrepatcherSelection
set "PrepatcherFolder="
set "PrepatcherAgent="
set "PrepatcherStatus=Disabled"
exit /b 0

:SelectPrepatcher
set "PrepatcherFolder=%~1"
set "PrepatcherAgent=-javaagent:../mods/%~1/agent/StarsectorPrepatcherAgent.jar"
set "PrepatcherStatus=Disabled"
exit /b 0

:DetectSystemResources
set "PhysicalMemoryMiB="
set "PhysicalMemoryGiB="
set "SafeHeapMiB="
set "PhysicalCoreCount="
set "LogicalProcessorCount=%NUMBER_OF_PROCESSORS%"
set "LargePagesPrivilegeStatus=Unable to determine"
for /f "usebackq tokens=1-3 delims=|" %%M in (`powershell.exe -NoLogo -NoProfile -NonInteractive -Command "$cpu = Get-CimInstance Win32_Processor; $memory = [int64]((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1MB); $cores = [int](($cpu | Measure-Object NumberOfCores -Sum).Sum); $logical = ([Convert]::ToString((Get-Process -Id $PID).ProcessorAffinity.ToInt64(), 2) -replace '0', '').Length; Write-Output ($memory.ToString() + '|' + $cores.ToString() + '|' + $logical.ToString())" 2^>nul`) do (
    set "PhysicalMemoryMiB=%%M"
    set "PhysicalCoreCount=%%N"
    set "LogicalProcessorCount=%%O"
)
if defined PhysicalMemoryMiB (
    call :ValidatePositiveInteger "!PhysicalMemoryMiB!"
    if errorlevel 1 set "PhysicalMemoryMiB="
)
if defined PhysicalCoreCount (
    call :ValidatePositiveInteger "!PhysicalCoreCount!"
    if errorlevel 1 set "PhysicalCoreCount="
)
if defined LogicalProcessorCount (
    call :ValidatePositiveInteger "!LogicalProcessorCount!"
    if errorlevel 1 set "LogicalProcessorCount="
)
if not defined LogicalProcessorCount set "LogicalProcessorCount=1"
if defined PhysicalMemoryMiB for /f "tokens=1,2 delims=|" %%M in ('powershell.exe -NoLogo -NoProfile -NonInteractive -Command "$m=[int64]!PhysicalMemoryMiB!; Write-Output (([int64](($m+1023)/1024)).ToString() + '|' + ([int64](($m*75)/100)).ToString())" 2^>nul') do (
    set "PhysicalMemoryGiB=%%M"
    set "SafeHeapMiB=%%N"
)
whoami /priv 2>nul | findstr /I /C:"SeLockMemoryPrivilege" >nul
if not errorlevel 1 (
    set "LargePagesPrivilegeStatus=Assigned (a new process enables it when requested)"
) else (
    set "LargePagesPrivilegeStatus=Not assigned"
)
exit /b 0

REM ============================================================================
REM Pending file generation
REM ============================================================================

:BuildPendingFiles
call :CleanupPendingFiles
call :WriteInfoPending
if errorlevel 1 exit /b 1
if "!JavaVersion!"=="27" (
    call :WriteJava27Simple
) else (
    call :WriteModernJavaSimple
)
if errorlevel 1 exit /b 1
call :WriteLoggingPending
if errorlevel 1 exit /b 1
call :WriteLauncherPending
if errorlevel 1 exit /b 1
call :ValidateAllPendingFiles
exit /b %errorlevel%

:WriteInfoPending
type nul >"!InfoOutputFile!"
>>"!InfoOutputFile!" echo Memory allocation : !SelectedHeapDescription!
>>"!InfoOutputFile!" echo Java installation : !JavaPath! ^(Java !JavaVersion!^)
if "!JavaVersion!"=="28" (
    >>"!InfoOutputFile!" echo VM tuning         : Java 28 safe G1 preset
    >>"!InfoOutputFile!" echo Compact headers   : Disabled
    >>"!InfoOutputFile!" echo NMethod relocation: Disabled
) else if "!JavaVersion!"=="17" (
    >>"!InfoOutputFile!" echo VM tuning         : Java 17 safe G1 preset
) else (
    >>"!InfoOutputFile!" echo VM tuning         : Mikohime Java 27 preset
)
if /I "!LowCoreMode!"=="Yes" (
    >>"!InfoOutputFile!" echo CPU management    : Low-core tuning enabled
) else (
    >>"!InfoOutputFile!" echo CPU management    : Normal automatic tuning
)
if defined PhysicalCoreCount (
>>"!InfoOutputFile!" echo Physical CPU cores : !PhysicalCoreCount!
) else (
>>"!InfoOutputFile!" echo Physical CPU cores : Unable to detect
)
>>"!InfoOutputFile!" echo Logical processors : !LogicalProcessorCount!
if /I "!OldCpuMode!"=="Yes" (
    >>"!InfoOutputFile!" echo CPU instructions  : AVX disabled for older CPU compatibility
) else (
    >>"!InfoOutputFile!" echo CPU instructions  : Automatic
)
if /I "!LargePagesEnabled!"=="Yes" (
    >>"!InfoOutputFile!" echo Large Pages       : Enabled
) else (
    >>"!InfoOutputFile!" echo Large Pages       : Disabled
)
>>"!InfoOutputFile!" echo Logging           : !LoggingMode!
>>"!InfoOutputFile!" echo Fast Rendering     : !FastRenderingStatus!
if /I "!FastRenderingStatus!"=="Enabled" >>"!InfoOutputFile!" echo FR Resource Cache  : !ResourceCacheStatus!
if /I "!PrepatcherStatus!"=="Enabled" (
    >>"!InfoOutputFile!" echo StarsectorPrepatcher: Enabled ^(!PrepatcherFolder!^)
) else if not "!PrepatcherCount!"=="0" (
    >>"!InfoOutputFile!" echo StarsectorPrepatcher: Disabled
) else (
    >>"!InfoOutputFile!" echo StarsectorPrepatcher: Not installed
)
call :ValidatePendingFile "!InfoOutputFile!"
exit /b %errorlevel%

:WriteJava27Simple
if not exist "mikohime\DefaultVM" (
    echo Missing required Java 27 preset: mikohime\DefaultVM
    exit /b 1
)
findstr /V /B /L /C:"-XX:+UseCriticalCompilerThreadPriority" /C:"-XX:ThreadPriorityPolicy=1" /C:"-XX:MaxGCPauseMillis=" /C:"#-XX:MaxGCPauseMillis=" /C:"-XX:+ShowCodeDetailsInExceptionMessages" /C:"#-XX:+ShowCodeDetailsInExceptionMessages" /C:"-XX:+ExtensiveErrorReports" /C:"#-XX:+ExtensiveErrorReports" /C:"-XX:+ErrorLogSecondaryErrorDetails" /C:"#-XX:+ErrorLogSecondaryErrorDetails" /C:"-XX:+PrintCommandLineFlags" /C:"#-XX:+PrintCommandLineFlags" /C:"-Xlog:async" /C:"#-Xlog:async" /C:"-Xlog:gc+init" /C:"#-Xlog:gc+init" /C:"-DAsyncLogger.WaitStrategy=busyspin" /C:"#-DAsyncLogger.WaitStrategy=busyspin" /C:"-Dsun.java2d.renderer.useLogger=true" /C:"#-Dsun.java2d.renderer.useLogger=true" "mikohime\DefaultVM" >"!SimpleOutputFile!"
if errorlevel 1 exit /b 1
>>"!SimpleOutputFile!" echo -XX:+UseCriticalCompilerThreadPriority
>>"!SimpleOutputFile!" echo -XX:ThreadPriorityPolicy=1
>>"!SimpleOutputFile!" echo #-XX:MaxGCPauseMillis=100
call :AppendJvmDiagnosticLoggingOptions
call :AppendJvmLoggerOptions
>>"!SimpleOutputFile!" echo !ThreadStackSize!
>>"!SimpleOutputFile!" echo !MinHeapPrefix!!SelectedHeapMiB!m
>>"!SimpleOutputFile!" echo !MaxHeapPrefix!!SelectedHeapMiB!m
if /I "!LowCoreMode!"=="Yes" (
    >>"!SimpleOutputFile!" echo -XX:CICompilerCount=2
    >>"!SimpleOutputFile!" echo -XX:ConcGCThreads=1
)
if /I "!LargePagesEnabled!"=="Yes" >>"!SimpleOutputFile!" echo !LargePagesArgument!
if /I "!OldCpuMode!"=="Yes" (
    call :ApplyOldCpuCompatibility
) else (
    >>"!SimpleOutputFile!" echo !Avx2Argument!
)
call :AppendAgentsAndClasspath
call :AppendGamePaths
exit /b 0

:ApplyOldCpuCompatibility
findstr /V /X /L /C:"-XX:UseAVX=3" /C:"-XX:AVX3Threshold=0" /C:"-XX:CopyAVX3Threshold=0" /C:"-XX:+UseFMA" /C:"-XX:+UseBMI1Instructions" /C:"-XX:+UseBMI2Instructions" "!SimpleOutputFile!">"!SimpleOutputFile!.tmp"
if errorlevel 1 exit /b 1
move /Y "!SimpleOutputFile!.tmp" "!SimpleOutputFile!" >nul
if errorlevel 1 exit /b 1
>>"!SimpleOutputFile!" echo -XX:UseAVX=0
exit /b 0

:WriteModernJavaSimple
type nul >"!SimpleOutputFile!"
call :AppendModernDiagnostics
if "!JavaVersion!"=="28" (
    >>"!SimpleOutputFile!" echo -XX:-UseCompactObjectHeaders
    >>"!SimpleOutputFile!" echo -XX:-NMethodRelocation
    >>"!SimpleOutputFile!" echo -XX:+DisableExplicitGC
)
call :AppendModernGcOptions
if "!JavaVersion!"=="28" >>"!SimpleOutputFile!" echo -XX:+AlwaysPreTouchStacks
>>"!SimpleOutputFile!" echo -XX:+AlwaysPreTouch
if /I "!OldCpuMode!"=="Yes" >>"!SimpleOutputFile!" echo -XX:UseAVX=0
if /I "!LowCoreMode!"=="Yes" (
    >>"!SimpleOutputFile!" echo -XX:CICompilerCount=2
    >>"!SimpleOutputFile!" echo -XX:ConcGCThreads=1
)
if /I "!LargePagesEnabled!"=="Yes" >>"!SimpleOutputFile!" echo !LargePagesArgument!
call :AppendModernSystemProperties
if "!JavaVersion!"=="28" >>"!SimpleOutputFile!" echo --enable-final-field-mutation=ALL-UNNAMED
>>"!SimpleOutputFile!" echo !ThreadStackSize!
>>"!SimpleOutputFile!" echo !MinHeapPrefix!!SelectedHeapMiB!m
>>"!SimpleOutputFile!" echo !MaxHeapPrefix!!SelectedHeapMiB!m
call :AppendAgentsAndClasspath
call :AppendGamePaths
exit /b 0

:AppendModernDiagnostics
>>"!SimpleOutputFile!" echo -XX:+UnlockDiagnosticVMOptions
>>"!SimpleOutputFile!" echo -XX:+UnlockExperimentalVMOptions
call :AppendJvmDiagnosticLoggingOptions
>>"!SimpleOutputFile!" echo -XX:+TieredCompilation
>>"!SimpleOutputFile!" echo -XX:TieredStopAtLevel=4
exit /b 0

:AppendJvmDiagnosticLoggingOptions
set "JvmLoggingPrefix="
if /I "!LoggingMode!"=="Minimal" set "JvmLoggingPrefix=#"
>>"!SimpleOutputFile!" echo !JvmLoggingPrefix!-XX:+ShowCodeDetailsInExceptionMessages
>>"!SimpleOutputFile!" echo !JvmLoggingPrefix!-XX:+ExtensiveErrorReports
if "!JavaVersion!"=="28" >>"!SimpleOutputFile!" echo !JvmLoggingPrefix!-XX:+ErrorLogSecondaryErrorDetails
>>"!SimpleOutputFile!" echo !JvmLoggingPrefix!-XX:+PrintCommandLineFlags
>>"!SimpleOutputFile!" echo !JvmLoggingPrefix!-Xlog:async
>>"!SimpleOutputFile!" echo !JvmLoggingPrefix!-Xlog:gc+init
exit /b 0

:AppendJvmLoggerOptions
set "JvmLoggingPrefix="
if /I "!LoggingMode!"=="Minimal" set "JvmLoggingPrefix=#"
>>"!SimpleOutputFile!" echo !JvmLoggingPrefix!-DAsyncLogger.WaitStrategy=busyspin
>>"!SimpleOutputFile!" echo !JvmLoggingPrefix!-Dsun.java2d.renderer.useLogger=true
exit /b 0

:AppendModernGcOptions
>>"!SimpleOutputFile!" echo -XX:+UseG1GC
>>"!SimpleOutputFile!" echo #-XX:MaxGCPauseMillis=100
>>"!SimpleOutputFile!" echo -XX:+UseStringDeduplication
exit /b 0

:AppendModernSystemProperties
>>"!SimpleOutputFile!" echo -XX:ReservedCodeCacheSize=256m
>>"!SimpleOutputFile!" echo -Djava.library.path="..\\mikohime/windows"
>>"!SimpleOutputFile!" echo -XX:-BytecodeVerificationLocal
>>"!SimpleOutputFile!" echo -XX:-BytecodeVerificationRemote
>>"!SimpleOutputFile!" echo -Dlog4j1.compatibility=true
>>"!SimpleOutputFile!" echo -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
call :AppendJvmLoggerOptions
>>"!SimpleOutputFile!" echo -Dlog4j2.enableThreadlocals=true
>>"!SimpleOutputFile!" echo -Dlog4j2.enableDirectEncoders=true
>>"!SimpleOutputFile!" echo -Dlog4j2.garbagefreeThreadContextMap=true
>>"!SimpleOutputFile!" echo -Djava.util.Arrays.useLegacyMergeSort=true
>>"!SimpleOutputFile!" echo -Dsun.java2d.renderer.useRef=weak
>>"!SimpleOutputFile!" echo -Dlog4j.configuration=..\\mikohime/mikohime.properties
>>"!SimpleOutputFile!" echo -Djava.xml.config.file=..\\mikohime/miko_jxp.properties
>>"!SimpleOutputFile!" echo -Dcom.fs.starfarer.launcher_bg=..\\mikohime/launcher_bg.jpg
>>"!SimpleOutputFile!" echo --add-opens=java.base/sun.nio.ch=ALL-UNNAMED
>>"!SimpleOutputFile!" echo --add-opens=java.base/java.nio=ALL-UNNAMED
>>"!SimpleOutputFile!" echo --add-opens=java.base/java.util=ALL-UNNAMED
>>"!SimpleOutputFile!" echo --add-opens=java.base/java.util.concurrent=ALL-UNNAMED
>>"!SimpleOutputFile!" echo --add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED
>>"!SimpleOutputFile!" echo --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED
>>"!SimpleOutputFile!" echo --add-opens=java.base/java.lang.reflect=ALL-UNNAMED
>>"!SimpleOutputFile!" echo --add-opens=java.base/java.lang.ref=ALL-UNNAMED
>>"!SimpleOutputFile!" echo --add-opens=java.base/java.lang=ALL-UNNAMED
>>"!SimpleOutputFile!" echo --add-opens=java.management/javax.management=ALL-UNNAMED
>>"!SimpleOutputFile!" echo --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED
>>"!SimpleOutputFile!" echo --add-opens=java.base/java.text=ALL-UNNAMED
>>"!SimpleOutputFile!" echo --add-opens=java.desktop/java.awt.font=ALL-UNNAMED
>>"!SimpleOutputFile!" echo --add-opens=java.desktop/java.awt=ALL-UNNAMED
>>"!SimpleOutputFile!" echo --enable-native-access=ALL-UNNAMED
exit /b 0

:AppendAgentsAndClasspath
if /I "!ResourceCacheStatus!"=="Enabled" >>"!SimpleOutputFile!" echo !ResourceCacheAgent!
if /I "!FastRenderingStatus!"=="Enabled" >>"!SimpleOutputFile!" echo !FastRenderingAgent!
if /I "!PrepatcherStatus!"=="Enabled" >>"!SimpleOutputFile!" echo "!PrepatcherAgent!"
if /I "!FastRenderingStatus!"=="Enabled" (
    >>"!SimpleOutputFile!" echo !FastRenderingClasspath!
) else (
    >>"!SimpleOutputFile!" echo !BaseClasspathArgument!
)
exit /b 0

:AppendGamePaths
>>"!SimpleOutputFile!" echo !SavesPathArgument!
>>"!SimpleOutputFile!" echo !ScreenshotsPathArgument!
>>"!SimpleOutputFile!" echo !ModsPathArgument!
>>"!SimpleOutputFile!" echo !LogsPathArgument!
>>"!SimpleOutputFile!" echo !LauncherClass!
exit /b 0

:WriteLoggingPending
del /Q "!LoggingOutputFile!" 2>nul
if /I "!LoggingMode!"=="Full" (
    >"!LoggingOutputFile!" echo log4j.rootLogger=INFO, ConsoleAppender, file
) else (
    >"!LoggingOutputFile!" echo log4j.rootLogger=INFO, file
    >>"!LoggingOutputFile!" echo(
    >>"!LoggingOutputFile!" echo # Suppress extremely verbose routine resource-loading messages
    >>"!LoggingOutputFile!" echo log4j.logger.com.fs.starfarer.loading=WARN
    >>"!LoggingOutputFile!" echo log4j.logger.com.genir.renderer.overrides.loading=WARN
    >>"!LoggingOutputFile!" echo log4j.logger.com.fs.starfarer.campaign.rules.Rules=WARN
)
>>"!LoggingOutputFile!" echo(
>>"!LoggingOutputFile!" echo #log4j.throwableRenderer=com.fs.starfarer.log.CustomLogj4ExceptionLogger
>>"!LoggingOutputFile!" echo(
>>"!LoggingOutputFile!" echo # Console appender
>>"!LoggingOutputFile!" echo log4j.appender.ConsoleAppender=org.apache.log4j.ConsoleAppender
>>"!LoggingOutputFile!" echo log4j.appender.ConsoleAppender.layout=org.apache.log4j.PatternLayout
>>"!LoggingOutputFile!" echo log4j.appender.ConsoleAppender.layout.ConversionPattern=%%-4r [%%t] %%-5p %%c %%x - %%m%%n
>>"!LoggingOutputFile!" echo(
>>"!LoggingOutputFile!" echo # Rolling file appender
>>"!LoggingOutputFile!" echo log4j.appender.file=org.apache.log4j.RollingFileAppender
>>"!LoggingOutputFile!" echo log4j.appender.file.File=${com.fs.starfarer.settings.paths.logs}/starsector.log
>>"!LoggingOutputFile!" echo log4j.appender.file.layout=org.apache.log4j.PatternLayout
>>"!LoggingOutputFile!" echo log4j.appender.file.layout.ConversionPattern=%%-4r [%%t] %%-5p %%c %%x - %%m%%n
>>"!LoggingOutputFile!" echo log4j.appender.file.MaxFileSize=50000KB
>>"!LoggingOutputFile!" echo log4j.appender.file.MaxBackupIndex=3
call :ValidatePendingFile "!LoggingOutputFile!"
exit /b %errorlevel%

:WriteLauncherPending
type nul >"!LauncherOutputFile!"
type "mikohime\DefaultPath">>"!LauncherOutputFile!"
if errorlevel 1 exit /b 1
>>"!LauncherOutputFile!" echo(
>>"!LauncherOutputFile!" echo "..\!JavaPath!\bin\java.exe" @..\Miko_Simple.txt
>>"!LauncherOutputFile!" echo if errorlevel 1 pause
call :ValidatePendingFile "!LauncherOutputFile!"
exit /b %errorlevel%

REM ============================================================================
REM Validation and transactional commit
REM ============================================================================

:ValidateAllPendingFiles
call :ValidatePendingFile "!SimpleOutputFile!"
if errorlevel 1 exit /b 1
call :ValidatePendingFile "!LauncherOutputFile!"
if errorlevel 1 exit /b 1
call :ValidatePendingFile "!LoggingOutputFile!"
if errorlevel 1 exit /b 1
call :ValidatePendingFile "!InfoOutputFile!"
if errorlevel 1 exit /b 1

findstr /X /L /C:"!LauncherClass!" "!SimpleOutputFile!" >nul
if errorlevel 1 exit /b 1
findstr /B /L /C:"-Xms" "!SimpleOutputFile!" >nul
if errorlevel 1 exit /b 1
findstr /B /L /C:"-Xmx" "!SimpleOutputFile!" >nul
if errorlevel 1 exit /b 1
findstr /B /L /C:"-classpath" "!SimpleOutputFile!" >nul
if errorlevel 1 exit /b 1
findstr /L /C:"@..\Miko_Simple.txt" "!LauncherOutputFile!" >nul
if errorlevel 1 exit /b 1
findstr /X /L /C:"log4j.appender.file.MaxBackupIndex=3" "!LoggingOutputFile!" >nul
if errorlevel 1 exit /b 1

if /I "!ResourceCacheStatus!"=="Enabled" (
    if /I not "!FastRenderingStatus!"=="Enabled" exit /b 1
    call :ValidatePendingFile "starsector-core\fr-resource-cache-agent.jar"
    if errorlevel 1 exit /b 1
    find /I "!ResourceCacheAgent!" "!SimpleOutputFile!" >nul
    if errorlevel 1 exit /b 1
) else (
    find /I "-javaagent:fr-resource-cache-agent.jar" "!SimpleOutputFile!" >nul
    if not errorlevel 1 exit /b 1
)

if /I "!PrepatcherStatus!"=="Enabled" (
    call :ValidatePendingFile "mods\!PrepatcherFolder!\agent\StarsectorPrepatcherAgent.jar"
    if errorlevel 1 exit /b 1
    find /I "!PrepatcherAgent!" "!SimpleOutputFile!" >nul
    if errorlevel 1 exit /b 1
) else (
    find /I "StarsectorPrepatcherAgent.jar" "!SimpleOutputFile!" >nul
    if not errorlevel 1 exit /b 1
)
exit /b 0

:ValidatePendingFile
if not exist "%~1" exit /b 1
for %%F in ("%~1") do if %%~zF LEQ 0 exit /b 1
exit /b 0

:CommitGeneratedFiles
set "BackupSuffix=.configure_backup_!TransactionId!"
set "CommitTarget[1]=Miko_Simple.txt"
set "CommitPending[1]=!SimpleOutputFile!"
set "CommitTarget[2]=Miko_Rouge.bat"
set "CommitPending[2]=!LauncherOutputFile!"
set "CommitTarget[3]=mikohime\mikohime.properties"
set "CommitPending[3]=!LoggingOutputFile!"
set "CommitTarget[4]=Miko_Info.txt"
set "CommitPending[4]=!InfoOutputFile!"

for /L %%N in (1,1,4) do (
    set "CommitBackup[%%N]=!CommitTarget[%%N]!!BackupSuffix!"
    set "CommitHadOriginal[%%N]=No"
    if exist "!CommitTarget[%%N]!" (
        copy /Y "!CommitTarget[%%N]!" "!CommitBackup[%%N]!" >nul
        if errorlevel 1 goto :CommitPreparationFailed
        set "CommitHadOriginal[%%N]=Yes"
    )
)

for /L %%N in (1,1,4) do (
    move /Y "!CommitPending[%%N]!" "!CommitTarget[%%N]!" >nul
    if errorlevel 1 goto :CommitRollback
)
call :DeleteCommitBackups
exit /b 0

:CommitPreparationFailed
call :DeleteCommitBackups
exit /b 1

:CommitRollback
for /L %%N in (1,1,4) do (
    if /I "!CommitHadOriginal[%%N]!"=="Yes" if exist "!CommitBackup[%%N]!" move /Y "!CommitBackup[%%N]!" "!CommitTarget[%%N]!" >nul
    if /I "!CommitHadOriginal[%%N]!"=="No" del /Q "!CommitTarget[%%N]!" 2>nul
)
call :DeleteCommitBackups
exit /b 1

:DeleteCommitBackups
for /L %%N in (1,1,4) do if defined CommitBackup[%%N] del /Q "!CommitBackup[%%N]!" 2>nul
exit /b 0

:CleanupPendingFiles
del /Q "!SimpleOutputFile!" "!LauncherOutputFile!" "!LoggingOutputFile!" "!InfoOutputFile!" 2>nul
del /Q "!JavaVersionOutput!" 2>nul
del /Q "!InputTransferFile!" 2>nul
exit /b 0

REM ============================================================================
REM Installation and input validation
REM ============================================================================

:AcquireConfiguratorLock
2>nul mkdir "!LockDirectory!"
if errorlevel 1 (
    echo Another configurator instance is already running, or a stale lock exists:
    echo   !CD!\!LockDirectory!
    echo Close the other instance. If none is running, remove that directory manually.
    pause
    exit /b 1
)
>"!LockDirectory!\owner.txt" echo Started !DATE! !TIME! by !USERNAME! on !COMPUTERNAME!
exit /b 0

:ReleaseConfiguratorLock
if defined LockDirectory if exist "!LockDirectory!\owner.txt" del /Q "!LockDirectory!\owner.txt" 2>nul
if defined LockDirectory if exist "!LockDirectory!\." rmdir "!LockDirectory!" 2>nul
exit /b 0

:ValidateInstallation
if not exist "starsector.exe" goto :InvalidInstallation
if not exist "starsector-core\." goto :InvalidInstallation
if not exist "mikohime\." goto :InvalidInstallation
if not exist "mikohime\DefaultPath" goto :InvalidInstallation
exit /b 0

:InvalidInstallation
echo This is not a complete Starsector installation.
echo Required: starsector.exe, starsector-core, mikohime, and mikohime\DefaultPath
echo Place this script in the Starsector installation folder and restore missing files.
pause
exit /b 1

:CheckWriteAccess
set "WriteTest=.configure_write_test_!RANDOM!_!RANDOM!.tmp"
>"!WriteTest!" echo test 2>nul
if errorlevel 1 goto :WriteAccessFailed
del /Q "!WriteTest!" 2>nul
set "WriteTest=mikohime\.configure_write_test_!RANDOM!_!RANDOM!.tmp"
>"!WriteTest!" echo test 2>nul
if errorlevel 1 goto :WriteAccessFailed
del /Q "!WriteTest!" 2>nul
exit /b 0

:WriteAccessFailed
del /Q "!WriteTest!" 2>nul
echo This configurator cannot write to the Starsector installation.
echo Move Starsector to a writable folder or run this script as administrator.
pause
exit /b 1

:ValidatePositiveInteger
if "%~1"=="" exit /b 1
for /f "delims=0123456789" %%A in ("%~1") do exit /b 1
if "%~1"=="0" exit /b 1
exit /b 0

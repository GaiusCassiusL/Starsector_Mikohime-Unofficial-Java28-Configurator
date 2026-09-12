@ECHO OFF
if "%~1"=="" exit /b 64
set "ConfiguratorUiAction=%~1"
shift /1
goto :%ConfiguratorUiAction%

REM UI and menu helpers. This module intentionally has no module-level SETLOCAL.

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
echo                A mod by Yue (Himemiko) - Fork by Gaius Cassius
echo ==============================================================================
exit /b 0

:PrintDetectedEnvironment
echo Detected environment:
echo   Java installations : !JavaOptionCount!
for /L %%N in (1,1,!JavaOptionCount!) do echo     Java !JavaOptionVersion[%%N]! - !JavaOptionPath[%%N]!
if /I "!ModernJavaAvailable!"=="Yes" (
    echo   Java 27 or 28      : !ColorGreen!Installed!ColorReset!
) else (
    echo   Java 27 or 28      : !ColorYellow!Not found!ColorReset!
    echo     Recommendation   : Download and install Java 27 or Java 28 ^(select J below^)
)
if /I "!FastRenderingAvailable!"=="Yes" (
    echo   Fast Rendering     : !ColorGreen!Installed ^(!FastRenderingVersion!^)!ColorReset!
) else (
    echo   Fast Rendering     : !ColorYellow!Not found!ColorReset!
    echo     Repository       : !FastRenderingReleasesUrl!
)
if /I "!ResourceCacheAvailable!"=="Yes" (
    echo   FR Resource Cache  : !ColorGreen!Installed ^(!ResourceCacheVersion!^)!ColorReset!
) else (
    echo   FR Resource Cache  : !ColorYellow!Not found!ColorReset!
    echo     Repository       : !ResourceCacheReleasesUrl!
    echo     Install          : Select R below
)
if "!PrepatcherCount!"=="0" (
    echo   Prepatcher         : !ColorYellow!Not found!ColorReset!
    if not "!IncompatiblePrepatcherCount!"=="0" echo     Compatibility    : !IncompatiblePrepatcherCount! older or unreadable installation^(s^) ignored
    echo     Repository       : !PrepatcherReleasesUrl!
) else (
    if "!PrepatcherCount!"=="1" (echo   Prepatcher         : !ColorGreen!Installed ^(v!PrepatcherCandidateVersion[1]!^)!ColorReset!) else (echo   Prepatcher         : !ColorGreen!!PrepatcherCount! compatible installations!ColorReset!)
    if not "!IncompatiblePrepatcherCount!"=="0" echo     Compatibility    : !IncompatiblePrepatcherCount! older or unreadable installation^(s^) ignored
)
if /I "!VramOptimizerAvailable!"=="Yes" (
    echo   VRAM Optimizer     : !ColorGreen!Installed ^(!VramOptimizerVersion!^)!ColorReset!
) else (
    echo   VRAM Optimizer     : !ColorYellow!Recommended!ColorReset!
    echo     Repository       : !VramOptimizerReleasesUrl!
)
if defined PhysicalMemoryMiB (
    set /a "PhysicalMemoryGiB=(PhysicalMemoryMiB+1023)/1024"
    echo   Physical memory    : Approximately !PhysicalMemoryGiB! GB
) else (
    echo   Physical memory    : !ColorYellow!Unable to detect!ColorReset!
)
if defined PhysicalCoreCount (
    echo   Physical CPU cores : !PhysicalCoreCount!
) else (
    echo   Physical CPU cores : !ColorYellow!Unable to detect!ColorReset!
)
echo   Logical processors  : !LogicalProcessorCount!
if /I "!UnsafeComponentNamesFound!"=="Yes" (
    echo   Component names    : !ColorYellow!Folders containing CMD metacharacters are ignored!ColorReset!
)
exit /b 0

:PrintComponentStatus
if /I "%~1"=="Compact" (
    if /I "!FastRenderingAvailable!"=="Yes" (echo     Fast Rendering    : Installed ^(!FastRenderingVersion!^)) else (echo     Fast Rendering    : Not installed)
    if /I "!ResourceCacheAvailable!"=="Yes" (echo     FR Resource Cache : Installed ^(!ResourceCacheVersion!^)) else (echo     FR Resource Cache : Not installed)
    if "!PrepatcherCount!"=="0" (echo     Prepatcher        : Not installed) else if "!PrepatcherCount!"=="1" (echo     Prepatcher        : Installed ^(v!PrepatcherCandidateVersion[1]!^)) else (echo     Prepatcher        : !PrepatcherCount! compatible installations)
    if /I "!VramOptimizerAvailable!"=="Yes" (echo     VRAM Optimizer    : Installed ^(!VramOptimizerVersion!^)) else (echo     VRAM Optimizer    : Not installed)
    echo     OpenAL Soft       : !OpenAlAddonStatus! ^(v!OpenAlAddonVersion!^)
    exit /b 0
)
if /I "%~1"=="Detailed" (
    if /I "!FastRenderingAvailable!"=="Yes" (echo   Fast Rendering    : Installed ^(!FastRenderingVersion!^) - improves rendering performance) else (echo   Fast Rendering    : Not installed - optional)
    if /I "!ResourceCacheAvailable!"=="Yes" (echo   FR Resource Cache : Installed ^(!ResourceCacheVersion!^) - requires Fast Rendering) else (echo   FR Resource Cache : Not installed - optional)
    if "!PrepatcherCount!"=="0" (echo   Prepatcher        : Not installed - optional mod compatibility tool) else if "!PrepatcherCount!"=="1" (echo   Prepatcher        : Installed ^(v!PrepatcherCandidateVersion[1]!^)) else (echo   Prepatcher        : !PrepatcherCount! compatible installations)
    if /I "!VramOptimizerAvailable!"=="Yes" (echo   VRAM Optimizer    : Installed ^(!VramOptimizerVersion!^)) else (echo   VRAM Optimizer    : Not installed - recommended separately)
    echo   OpenAL Soft       : !OpenAlAddonStatus! ^(v!OpenAlAddonVersion!^) - optional Windows audio replacement
    exit /b 0
)
if /I "!FastRenderingAvailable!"=="Yes" (echo Fast Rendering    : Yes ^(!FastRenderingVersion!^)) else (echo Fast Rendering    : No)
if /I "!ResourceCacheAvailable!"=="Yes" (echo FR Resource Cache : Yes ^(!ResourceCacheVersion!^)) else (echo FR Resource Cache : No)
if "!PrepatcherCount!"=="0" (echo Prepatcher        : No) else if "!PrepatcherCount!"=="1" (echo Prepatcher        : Yes ^(v!PrepatcherCandidateVersion[1]!^)) else (echo Prepatcher        : Yes ^(!PrepatcherCount! compatible installations^))
if /I "!VramOptimizerAvailable!"=="Yes" (echo VRAM Optimizer    : Yes ^(!VramOptimizerVersion!^)) else (echo VRAM Optimizer    : No)
echo OpenAL Soft       : !OpenAlAddonStatus! ^(v!OpenAlAddonVersion!^)
exit /b 0

:CheckComponentUpdates
cls
call :PrintHeader
echo Component update check
echo --------------------------------------------------------------------------
echo Contacting GitHub Releases. This does not download or replace any mods.
echo(
if not exist "!UpdateStatusFile!" (
    set "UPDATE_FAST_RENDERING_AVAILABLE=!FastRenderingAvailable!"
    set "UPDATE_FAST_RENDERING_VERSION=!FastRenderingVersion!"
    set "UPDATE_FAST_RENDERING_API=!FastRenderingLatestReleaseApi!"
    set "UPDATE_RESOURCE_CACHE_AVAILABLE=!ResourceCacheAvailable!"
    set "UPDATE_RESOURCE_CACHE_VERSION=!ResourceCacheVersion!"
    set "UPDATE_RESOURCE_CACHE_API=!ResourceCacheLatestReleaseApi!"
    if "!PrepatcherCount!"=="0" (
        set "UPDATE_PREPATCHER_AVAILABLE=No"
        set "UPDATE_PREPATCHER_VERSIONS="
    ) else (
        set "UPDATE_PREPATCHER_AVAILABLE=Yes"
        set "UPDATE_PREPATCHER_VERSIONS="
        for /L %%N in (1,1,!PrepatcherCount!) do set "UPDATE_PREPATCHER_VERSIONS=!UPDATE_PREPATCHER_VERSIONS!;!PrepatcherCandidateVersion[%%N]!"
    )
    set "UPDATE_PREPATCHER_API=!PrepatcherLatestReleaseApi!"
    set "UPDATE_VRAM_AVAILABLE=!VramOptimizerAvailable!"
    set "UPDATE_VRAM_VERSION=!VramOptimizerVersion!"
    set "UPDATE_VRAM_API=!VramOptimizerLatestReleaseApi!"
    powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action CheckComponentUpdates >"!UpdateStatusFile!"
    if errorlevel 1 del /Q "!UpdateStatusFile!" 2>nul
)
if exist "!UpdateStatusFile!" (
    type "!UpdateStatusFile!"
) else (
    echo Unable to check component versions.
)
echo(
echo Results are cached until installed components are refreshed.
pause
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
call "!EnvironmentModule!" SetAutomaticPrepatcherSelection
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
    call "!EnvironmentModule!" GetJavaMajor "!SelectedJdkFolder!" ExistingJdkMajor
    if not errorlevel 1 if "!ExistingJdkMajor!"=="!SelectedJdkMajor!" (
        echo(
        echo !ColorGreen!!SelectedJdkName! is already installed.!ColorReset!
        pause
        call "!EnvironmentModule!" RefreshEnvironment
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
echo 2. Download manually
echo 3. Cancel
choice /c 123 /n /m "Select an option: "
if errorlevel 3 exit /b 0
if errorlevel 2 (
    call :OfferManualJdkDownload
    exit /b 0
)

call :DownloadSelectedJdk
if errorlevel 1 (
    echo(
    echo !ColorRed!Java installation failed.!ColorReset!
    echo No existing Java installation was changed.
    call :OfferManualJdkDownload
    exit /b 1
)

call "!EnvironmentModule!" RefreshEnvironment
echo(
echo !ColorGreen!!SelectedJdkName! was installed successfully.!ColorReset!
pause
exit /b 0

:OfferManualJdkDownload
echo(
echo Manual download for !SelectedJdkName!:
echo   !SelectedJdkUrl!
echo(
echo Extract the downloaded archive beside starsector.exe so Java is located at:
echo   !CD!\!SelectedJdkFolder!\bin\java.exe
echo(
echo 1. Open the download in your web browser
echo 2. Return without opening it
choice /c 12 /n /m "Select an option: "
if errorlevel 2 exit /b 0
call :OpenDownloadPage "!SelectedJdkUrl!"
if errorlevel 1 (
    echo Copy the URL shown above and open it manually.
)
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

powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action InstallJdk
if errorlevel 1 (
    call :CleanupJdkInstallation
    exit /b 1
)
set "JdkDestinationCreated=Yes"
call :CleanupJdkInstallation

if not exist "!SelectedJdkFolder!\bin\java.exe" goto :DownloadedJdkInvalid
call "!EnvironmentModule!" GetJavaMajor "!SelectedJdkFolder!" InstalledJdkMajor
if errorlevel 1 goto :DownloadedJdkInvalid
if not "!InstalledJdkMajor!"=="!SelectedJdkMajor!" goto :DownloadedJdkInvalid
exit /b 0

:DownloadedJdkInvalid
if /I "!JdkDestinationCreated!"=="Yes" (
    set "HELPER_REMOVE_PATH=!JDK_INSTALL_DESTINATION!"
    powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action RemoveDirectory 2>nul
)
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
        call "!EnvironmentModule!" RefreshEnvironment
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

call "!EnvironmentModule!" RefreshEnvironment
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

powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action InstallResourceCache
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

:ManageOpenAlAddon
:ManageOpenAlAddonAgain
cls
call :PrintHeader
echo OpenAL Soft 1.25.1 audio add-on
echo --------------------------------------------------------------------------
echo Status: !OpenAlAddonStatus!
echo(
echo Installing downloads the add-on, replaces the game OpenAL DLLs, and
echo installs the audio utility. Original files are retained in DLLBK.
echo(
echo I. Install or repair the add-on
echo R. Recover from an incomplete installation
echo U. Uninstall and restore original files
echo C. Open the audio configuration utility
echo B. Back
echo X. Return to the main menu
echo H. Help
choice /c IRUCBXH /n /m "Select an option: "
if errorlevel 7 (
    echo(
    echo Repairs reapply the packaged files without replacing the original backups.
    echo Uninstall restores files that existed before installation and removes only
    echo files created by the add-on. The downloaded payload remains available.
    echo oalinst.exe is a separate system-wide Creative installer. Mikohime never
    echo runs it automatically and cannot undo system changes made by it.
    pause
    goto :ManageOpenAlAddonAgain
)
if errorlevel 6 exit /b 2
if errorlevel 5 exit /b 0
if errorlevel 4 (
    cls
    call :PrintHeader
    echo Recommended OpenAL Soft settings
    echo --------------------------------------------------------------------------
    echo(
    echo   Playback ^> Sample Format       : 32-bit float
    echo   Playback ^> Resampler Quality   : Maximum quality
    echo   HRTF ^> HRTF Render Method      : Maximum quality
    echo(
    echo These settings prioritize audio quality and may use slightly more CPU.
    echo HRTF is primarily beneficial when using headphones.
    echo Apply the changes in the utility before closing it.
    echo(
    pause
    powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action OpenAudioConfigurator
    if errorlevel 1 pause
    goto :ManageOpenAlAddonAgain
)
if errorlevel 3 (
    if /I not "!OpenAlAddonStatus!"=="Installed" if /I not "!OpenAlAddonStatus!"=="Modified" if /I not "!OpenAlAddonStatus!"=="Different version" (
        echo(
        echo The OpenAL Soft add-on does not have a usable installation record.
        pause
        goto :ManageOpenAlAddonAgain
    )
    echo(
    echo This restores the original files recorded during the first installation.
    if /I "!OpenAlAddonStatus!"=="Modified" echo Warning: files changed after installation will be replaced.
    choice /c YN /n /m "Uninstall the OpenAL Soft add-on? [Y/N] "
    if errorlevel 2 goto :ManageOpenAlAddonAgain
    powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action UninstallOpenAlAddon
    if errorlevel 1 (
        echo !ColorRed!Unable to uninstall the OpenAL Soft add-on.!ColorReset!
    ) else (
        echo !ColorGreen!OpenAL Soft was uninstalled and original files were restored.!ColorReset!
    )
    pause
    exit /b 0
)
if errorlevel 2 (
    if /I not "!OpenAlAddonStatus!"=="Incomplete installation" (
        echo(
        echo Recovery is only available for an incomplete installation.
        pause
        goto :ManageOpenAlAddonAgain
    )
    echo(
    echo Recovery restores available backups and removes unchanged add-on files.
    choice /c YN /n /m "Recover the original OpenAL files? [Y/N] "
    if errorlevel 2 goto :ManageOpenAlAddonAgain
    powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action RecoverOpenAlAddon
    if errorlevel 1 (
        echo !ColorRed!Unable to recover the incomplete OpenAL installation.!ColorReset!
    ) else (
        echo !ColorGreen!The incomplete OpenAL installation was recovered.!ColorReset!
    )
    pause
    exit /b 0
)
echo(
if /I "!OpenAlAddonStatus!"=="Incomplete installation" (
    echo Recover the incomplete installation before installing again.
    pause
    goto :ManageOpenAlAddonAgain
)
if /I "!OpenAlAddonStatus!"=="Different version" (
    echo Uninstall the recorded version before installing this download version.
    pause
    goto :ManageOpenAlAddonAgain
)
if /I "!OpenAlAddonStatus!"=="Modified" echo This repair replaces modified add-on files but preserves the original backups.
if /I "!OpenAlAddonStatus!"=="Installed" echo The add-on is already installed. Continuing performs a verified repair.
if /I "!OpenAlAddonStatus!"=="Not downloaded" echo The 17 MB add-on will be downloaded from GitHub before installation.
if /I "!OpenAlAddonStatus!"=="Payload missing" echo The add-on payload will be downloaded again before repair.
choice /c YN /n /m "Download and install, or repair, the OpenAL Soft add-on? [Y/N] "
if errorlevel 2 goto :ManageOpenAlAddonAgain
powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action InstallOpenAlAddon
if errorlevel 1 (
    echo !ColorRed!Unable to install or repair the OpenAL Soft add-on.!ColorReset!
) else (
    echo !ColorGreen!OpenAL Soft 1.25.1 was installed successfully.!ColorReset!
    echo Configure_Audio.bat and oalinst.exe are now in the Starsector folder.
)
pause
exit /b 0

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
call "!EnvironmentModule!" SelectPrepatcher "!PrepatcherCandidate[%MenuChoice%]!"
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
        call :OpenDownloadPage "!ResourceCacheReleasesUrl!"
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
if !MissingDownloadCount! GTR 1 goto :ChooseComponentDownload
if /I "!FastRenderingAvailable!"=="No" (
    call :OpenDownloadPage "!FastRenderingReleasesUrl!"
    exit /b 0
)
if /I "!ResourceCacheAvailable!"=="No" (
    call :OpenDownloadPage "!ResourceCacheReleasesUrl!"
    exit /b 0
)
if "!PrepatcherCount!"=="0" (
    call :OpenDownloadPage "!PrepatcherReleasesUrl!"
    exit /b 0
)
if /I "!VramOptimizerAvailable!"=="No" (
    call :OpenDownloadPage "!VramOptimizerReleasesUrl!"
)
exit /b 0

:ChooseComponentDownload
cls
call :PrintHeader
echo Component download pages
echo --------------------------------------------------------------------------
echo 1. Fast Rendering
echo 2. FR Resource Cache
echo 3. StarsectorPrepatcher
echo 4. VRAM Optimizer
echo B. Back
choice /c 1234B /n /m "Select an option: "
if errorlevel 5 exit /b 0
if errorlevel 4 (
    call :OpenDownloadPage "!VramOptimizerReleasesUrl!"
) else if errorlevel 3 (
    call :OpenDownloadPage "!PrepatcherReleasesUrl!"
) else if errorlevel 2 (
    call :OpenDownloadPage "!ResourceCacheReleasesUrl!"
) else (
    call :OpenDownloadPage "!FastRenderingReleasesUrl!"
)
exit /b 0

:OpenDownloadPage
set "HELPER_OPEN_URL=%~1"
powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action OpenUrl >nul 2>&1
set "OpenDownloadResult=!errorlevel!"
set "HELPER_OPEN_URL="
if not "!OpenDownloadResult!"=="0" echo !ColorRed!Unable to open the download page.!ColorReset!
exit /b !OpenDownloadResult!

:PrintHeapPreset
echo %~1. !HeapDescription[%~2]!
exit /b 0

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
    echo Detected physical CPU cores  : !PhysicalCoreCount!
) else (
    echo Detected physical CPU cores  : Unable to detect
)
echo Available logical processors : !LogicalProcessorCount!
echo Low-core tuning uses the logical processors currently available to the JVM,
echo including any Windows affinity or resource limits.
echo Recommended low-core tuning  : !RecommendedLowCoreMode!
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
echo 5. Toadsector
echo X. Back to the main menu
choice /c 12345X /n /m "Select an option: "
if errorlevel 6 exit /b 0
if errorlevel 5 (
    call :CopyBackground "mikohime\bg\toadsector.jpg" "Toadsector"
) else if errorlevel 4 (
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

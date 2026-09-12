@ECHO OFF
SETLOCAL EnableExtensions DisableDelayedExpansion
TITLE Mikohime Java Configurator
set "NonInteractiveMode=No"
if /I "%~1"=="--non-interactive" set "NonInteractiveMode=Yes"
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

set "SharedConfigRoot=mikohime\configurator\shared"
if not exist "!SharedConfigRoot!\components.properties" set "SharedConfigRoot=%~dp0..\shared"
if not exist "!SharedConfigRoot!\components.properties" (
    echo Missing shared configurator data: components.properties
    exit /b 1
)
set "WindowsConfigRoot=!SharedConfigRoot!\..\windows"
set "HelperScript=!WindowsConfigRoot!\Configurator.Helpers.ps1"
set "UiModule=!WindowsConfigRoot!\Configurator.Ui.cmd"
set "EnvironmentModule=!WindowsConfigRoot!\Configurator.Environment.cmd"
set "GenerationModule=!WindowsConfigRoot!\Configurator.Generation.cmd"
if not exist "!HelperScript!" (
    echo Missing Windows configurator helper: !HelperScript!
    exit /b 1
)
if not exist "!UiModule!" (
    echo Missing Windows configurator module: !UiModule!
    exit /b 1
)
if not exist "!EnvironmentModule!" (
    echo Missing Windows configurator module: !EnvironmentModule!
    exit /b 1
)
if not exist "!GenerationModule!" (
    echo Missing Windows configurator module: !GenerationModule!
    exit /b 1
)
if not exist "!SharedConfigRoot!\memory-recommendations.tsv" (
    echo Missing shared configurator data: memory-recommendations.tsv
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
set "BackgroundOutputFile=mikohime\launcher_bg.!TransactionId!.pending"
set "JavaVersionOutput=JavaVersionCheck_!TransactionId!.pending"
set "InputTransferFile=.configure_input.!TransactionId!.tmp"
set "UpdateStatusFile=.component_updates.!TransactionId!.tmp"
set "JavaOptionCount=0"
set "PrepatcherCount=0"

call "!UiModule!" InitializeColors
call "!GenerationModule!" ValidateInstallation
if errorlevel 1 exit /b 1
call "!GenerationModule!" CheckWriteAccess
if errorlevel 1 exit /b 1
call "!GenerationModule!" AcquireConfiguratorLock
if errorlevel 1 exit /b 1
call "!EnvironmentModule!" RefreshEnvironment
if /I "!NonInteractiveMode!"=="Yes" goto :RunNonInteractive

REM ============================================================================
REM Main menu and configuration workflow
REM ============================================================================
:MainMenu
:MainMenuScreen
cls
call "!UiModule!" PrintHeader
echo System status
echo --------------------------------------------------------------------------
if /I "!ModernJavaAvailable!"=="Yes" (
    echo   Java                  : !ColorGreen!Ready - Java 27 or 28 detected!ColorReset!
) else if !JavaOptionCount! GTR 0 (
    echo   Java                  : !ColorYellow!Usable - Java 17 detected; Java 28 is recommended!ColorReset!
) else (
    echo   Java                  : !ColorRed!Action needed - install a supported Java version!ColorReset!
)
for /L %%N in (1,1,!JavaOptionCount!) do echo     Java !JavaOptionVersion[%%N]! - !JavaOptionPath[%%N]!
if defined PhysicalMemoryGiB echo   Memory                : Approximately !PhysicalMemoryGiB! GB
if defined PhysicalCoreCount echo   Processor             : !PhysicalCoreCount! physical cores, !LogicalProcessorCount! logical processors
echo   Optional enhancements :
call "!UiModule!" PrintComponentStatus "Compact"
echo     These are optional; select 3 to manage them.
echo(
echo 1. Apply recommended configuration
echo 2. Basic setup
echo 3. Advanced setup ^(all options^)
echo 4. Manage Java and optional components
echo 5. Change launcher background
echo 6. Exit
echo H. Help
echo(
choice /c 123456H /n /m "Select an option: "
if errorlevel 7 (
    call :MainHelp
    goto :MainMenuScreen
)
if errorlevel 6 goto :ExitConfigurator
if errorlevel 5 (
    call "!UiModule!" BackgroundMenu
    goto :MainMenuScreen
)
if errorlevel 4 (
    call :ManageComponents
    goto :MainMenuScreen
)
if errorlevel 3 goto :ConfigureJava
if errorlevel 2 goto :BasicConfiguration
goto :RecommendedConfiguration

:RecommendedConfiguration
call :SetRecommendedDefaults
if not defined JavaPath (
    cls
    call "!UiModule!" PrintHeader
    echo !ColorYellow!A supported Java installation is required.!ColorReset!
    echo Use Manage Java and optional components to install Java 27 or Java 28.
    echo(
    pause
    goto :MainMenuScreen
)
call :OfferRecommendedResourceCache
set "ConfigurationKind=Recommended"
goto :RecommendedReview

:BasicConfiguration
call "!UiModule!" ResetSelections
call :BasicChooseJava
if errorlevel 2 goto :MainMenuScreen
call :BasicChooseOptionalComponents
if errorlevel 2 goto :MainMenuScreen
if errorlevel 1 goto :BasicConfiguration
call :BasicChooseMemory
if errorlevel 2 goto :MainMenuScreen
if errorlevel 1 goto :BasicConfiguration
set "RecommendedLowCoreMode=No"
if !LogicalProcessorCount! LEQ 4 set "RecommendedLowCoreMode=Yes"
set "LowCoreMode=!RecommendedLowCoreMode!"
set "OldCpuMode=No"
set "LargePagesEnabled=No"
set "LoggingMode=Full"
set "ConfigurationKind=Customized"
goto :RecommendedReview

:BasicChooseJava
:BasicChooseJavaAgain
cls
call "!UiModule!" PrintHeader
echo Basic setup 1 of 3 - Java
echo --------------------------------------------------------------------------
echo Java 28 is recommended. Java 27 is a compatibility alternative, and
echo Java 17 is a legacy fallback.
echo(
set "DisplayedJavaOptionCount=!JavaOptionCount!"
if !DisplayedJavaOptionCount! GTR 9 set "DisplayedJavaOptionCount=9"
for /L %%N in (1,1,!DisplayedJavaOptionCount!) do (
    call :SetJavaRole "!JavaOptionVersion[%%N]!"
    echo %%N. !JavaOptionDescription[%%N]! - !JavaRole!
    echo    !JavaOptionPath[%%N]!
)
if "!DisplayedJavaOptionCount!"=="0" echo No supported Java installations were detected.
echo C. Specify a Java folder manually
echo X. Back to the main menu
echo H. What should I choose?
echo(
choice /c 123456789CXH /n /m "Select an option: "
set "MenuChoice=!errorlevel!"
if "!MenuChoice!"=="12" (
    echo(
    echo Choose Java 28 unless a mod specifically requires Java 27 or Java 17.
    echo Miko-named Java folders may include compatibility support unavailable in
    echo standard builds. The configurator applies those options only when appropriate.
    pause
    goto :BasicChooseJavaAgain
)
if "!MenuChoice!"=="11" exit /b 2
if "!MenuChoice!"=="10" (
    call :ChooseCustomJava
    exit /b !errorlevel!
)
if !MenuChoice! GTR !DisplayedJavaOptionCount! (
    echo !ColorYellow!Invalid selection.!ColorReset!
    pause
    goto :BasicChooseJavaAgain
)
set "JavaPath=!JavaOptionPath[%MenuChoice%]!"
set "JavaVersion=!JavaOptionVersion[%MenuChoice%]!"
exit /b 0

:SetJavaRole
set "JavaRole=Legacy fallback"
if "%~1"=="27" set "JavaRole=Compatibility alternative"
if "%~1"=="28" set "JavaRole=Recommended"
exit /b 0

:BasicChooseOptionalComponents
:BasicChooseOptionalComponentsAgain
cls
call "!UiModule!" PrintHeader
echo Basic setup 2 of 3 - Optional enhancements
echo --------------------------------------------------------------------------
call "!UiModule!" PrintComponentStatus "Detailed"
echo(
echo 1. Enable detected recommended enhancements
echo 2. Leave optional enhancements disabled
echo 3. Choose each installed enhancement
echo B. Back
echo X. Cancel and return to the main menu
echo H. Explain these components
choice /c 123BXH /n /m "Select an option: "
if errorlevel 6 (
    echo(
    echo Fast Rendering improves rendering performance. Its resource cache can improve
    echo loading behavior and works only with Fast Rendering. Prepatcher supports mods
    echo that patch game classes. VRAM Optimizer is managed by its own mod installation.
    pause
    goto :BasicChooseOptionalComponentsAgain
)
if errorlevel 5 exit /b 2
if errorlevel 4 exit /b 1
if errorlevel 3 (
    call "!UiModule!" ResolvePrepatcherSelection
    if errorlevel 2 exit /b 2
    if errorlevel 1 goto :BasicChooseOptionalComponentsAgain
    call "!UiModule!" ChooseRendering
    exit /b !errorlevel!
)
if errorlevel 2 (
    set "FastRenderingStatus=Disabled"
    set "ResourceCacheStatus=Disabled"
    set "PrepatcherFolder="
    set "PrepatcherAgent="
    set "PrepatcherStatus=Disabled"
    exit /b 0
)
call :EnableRecommendedEnhancements
exit /b 0

:BasicChooseMemory
:BasicChooseMemoryAgain
call :SetHeapRecommendation
cls
call "!UiModule!" PrintHeader
echo Basic setup 3 of 3 - Game memory
echo --------------------------------------------------------------------------
if defined PhysicalMemoryGiB echo Detected system memory        : Approximately !PhysicalMemoryGiB! GB
echo Recommended for this computer : !RecommendedHeapDescription!
echo(
echo 1. Use the recommended amount
call "!UiModule!" PrintHeapPreset "2" "1"
call "!UiModule!" PrintHeapPreset "3" "3"
call "!UiModule!" PrintHeapPreset "4" "4"
call "!UiModule!" PrintHeapPreset "5" "5"
echo A. Advanced memory settings
echo B. Back
echo X. Cancel and return to the main menu
echo H. Why does this matter?
choice /c 12345ABXH /n /m "Select an option: "
set "MenuChoice=!errorlevel!"
if "!MenuChoice!"=="9" (
    echo(
    echo This is the maximum memory available to Starsector. Too little can cause
    echo crashes with large mod lists; too much leaves too little for Windows and
    echo other programs. The recommended amount is a safe starting point.
    pause
    goto :BasicChooseMemoryAgain
)
if "!MenuChoice!"=="8" exit /b 2
if "!MenuChoice!"=="7" exit /b 1
if "!MenuChoice!"=="6" (
    call :ChooseAdvancedMemory
    exit /b !errorlevel!
)
if "!MenuChoice!"=="5" call :SetHeapSelection "5"
if "!MenuChoice!"=="4" call :SetHeapSelection "4"
if "!MenuChoice!"=="3" call :SetHeapSelection "3"
if "!MenuChoice!"=="2" call :SetHeapSelection "1"
if "!MenuChoice!"=="1" (
    set "SelectedHeapMiB=!RecommendedHeapMiB!"
    set "SelectedHeapDescription=!RecommendedHeapDescription!"
)
call :ConfirmMemorySelection
if errorlevel 1 goto :BasicChooseMemoryAgain
exit /b 0

:SetHeapRecommendation
set "RecommendedHeapMiB="
set "RecommendedHeapDescription="
set "RecommendationMemoryMiB=0"
if defined PhysicalMemoryMiB set "RecommendationMemoryMiB=!PhysicalMemoryMiB!"
for /f "usebackq eol=# tokens=1,2,* delims=|" %%A in ("!SharedConfigRoot!\memory-recommendations.tsv") do (
    if !RecommendationMemoryMiB! GEQ %%A (
        set "RecommendedHeapMiB=%%B"
        set "RecommendedHeapDescription=%%C"
    )
)
if not defined RecommendedHeapMiB exit /b 1
exit /b 0

:SetRecommendedDefaults
call "!UiModule!" ResetSelections
call :SelectRecommendedJavaVersion "28"
if not defined JavaPath call :SelectRecommendedJavaVersion "27"
if not defined JavaPath call :SelectRecommendedJavaVersion "17"
call :SetHeapRecommendation
set "SelectedHeapMiB=!RecommendedHeapMiB!"
set "SelectedHeapDescription=!RecommendedHeapDescription!"
if !LogicalProcessorCount! LEQ 4 set "LowCoreMode=Yes"
call :EnableRecommendedEnhancements
exit /b 0

:EnableRecommendedEnhancements
if /I "!FastRenderingAvailable!"=="Yes" set "FastRenderingStatus=Enabled"
if /I "!FastRenderingStatus!"=="Enabled" if /I "!ResourceCacheAvailable!"=="Yes" set "ResourceCacheStatus=Enabled"
if !PrepatcherCount! GTR 0 (
    call "!EnvironmentModule!" SelectPrepatcher "!PrepatcherCandidate[1]!"
    set "PrepatcherStatus=Enabled"
)
exit /b 0

:OfferRecommendedResourceCache
if /I not "!FastRenderingAvailable!"=="Yes" exit /b 0
if /I "!ResourceCacheAvailable!"=="Yes" exit /b 0
cls
call "!UiModule!" PrintHeader
echo Recommended enhancement available
echo --------------------------------------------------------------------------
echo Fast Rendering is installed, but FR Resource Cache is not installed.
echo FR Resource Cache is a recommended companion that can improve loading
echo behavior when Fast Rendering is enabled.
echo(
echo The latest release will be downloaded from:
echo   !ResourceCacheReleasesUrl!
echo(
choice /c YN /n /m "Download, install, and enable FR Resource Cache? [Y/N] "
if errorlevel 2 exit /b 0
call "!UiModule!" DownloadLatestResourceCache
if errorlevel 1 (
    echo(
    echo !ColorYellow!FR Resource Cache could not be installed. Recommended setup
    echo will continue with Fast Rendering enabled and Resource Cache disabled.!ColorReset!
    pause
    exit /b 0
)
call "!EnvironmentModule!" RefreshEnvironment
call :EnableRecommendedEnhancements
if /I not "!ResourceCacheAvailable!"=="Yes" (
    echo(
    echo !ColorYellow!FR Resource Cache was downloaded but could not be detected.
    echo Recommended setup will continue with Resource Cache disabled.!ColorReset!
    pause
    exit /b 0
)
echo(
echo !ColorGreen!FR Resource Cache was installed and enabled successfully.!ColorReset!
pause
exit /b 0

:SelectRecommendedJavaVersion
for /L %%N in (1,1,!JavaOptionCount!) do if not defined JavaPath if "!JavaOptionVersion[%%N]!"=="%~1" (
    set "JavaPath=!JavaOptionPath[%%N]!"
    set "JavaVersion=!JavaOptionVersion[%%N]!"
)
exit /b 0

:RecommendedReview
call "!GenerationModule!" NormalizeSelections
if errorlevel 1 goto :ConfigurationBuildFailed
cls
call "!UiModule!" PrintHeader
echo Review - !ConfigurationKind! setup
echo --------------------------------------------------------------------------
echo Java              : !JavaPath! ^(Java !JavaVersion!^)
echo Game memory       : !SelectedHeapDescription!
echo Fast Rendering    : !FastRenderingStatus!
if /I "!FastRenderingStatus!"=="Enabled" echo FR Resource Cache : !ResourceCacheStatus!
if /I "!PrepatcherStatus!"=="Enabled" (
    echo Prepatcher        : Enabled ^(!PrepatcherFolder!^)
) else (
    echo Prepatcher        : Disabled
)
echo Limited-CPU mode  : !LowCoreMode!
echo Large Pages       : Disabled ^(safe default^)
echo Troubleshooting   : Full logging retained
echo(
set "SafetyConcern=No"
if defined SafeHeapMiB if !SelectedHeapMiB! GTR !SafeHeapMiB! (
    echo !ColorYellow!Potential concern: selected memory exceeds the safe recommendation.!ColorReset!
    set "SafetyConcern=Yes"
)
if /I "!SafetyConcern!"=="No" echo !ColorGreen!Recommended safety settings followed: Yes!ColorReset!
echo(
echo Y. Apply configuration
echo B. Back to basic setup
echo A. Open advanced configuration
echo X. Cancel
choice /c YBAX /n /m "Select an option: "
if errorlevel 4 goto :MainMenuScreen
if errorlevel 3 goto :ConfigureJava
if errorlevel 2 goto :BasicConfiguration
goto :ApplyReviewedConfiguration

:ManageComponents
:ManageComponentsAgain
cls
call "!UiModule!" PrintHeader
echo Manage Java and optional components
echo --------------------------------------------------------------------------
echo Java 27 or 28     : !ModernJavaAvailable!
call "!UiModule!" PrintComponentStatus "Management"
echo(
echo J. Download and install Java
echo R. Download and install Fast Rendering resource cache
echo U. Check installed component versions
echo D. Open a component download page
echo A. Manage OpenAL Soft audio add-on
echo X. Back to the main menu
echo H. Help
choice /c JRUDAXH /n /m "Select an option: "
if errorlevel 7 (
    echo(
    echo Optional components are not required to create a working launcher.
    echo Install only the enhancements you want, then return to configuration.
    pause
    goto :ManageComponentsAgain
)
if errorlevel 6 exit /b 0
if errorlevel 5 (
    call "!UiModule!" ManageOpenAlAddon
    set "OpenAlMenuResult=!errorlevel!"
    call "!EnvironmentModule!" RefreshEnvironment
    if "!OpenAlMenuResult!"=="2" exit /b 0
    goto :ManageComponentsAgain
)
if errorlevel 4 (
    call "!UiModule!" ChooseComponentDownload
    call "!EnvironmentModule!" RefreshEnvironment
    goto :ManageComponentsAgain
)
if errorlevel 3 (
    call "!UiModule!" CheckComponentUpdates
    goto :ManageComponentsAgain
)
if errorlevel 2 (
    call "!UiModule!" InstallResourceCache
    call "!EnvironmentModule!" RefreshEnvironment
    goto :ManageComponentsAgain
)
call "!UiModule!" InstallJavaMenu
call "!EnvironmentModule!" RefreshEnvironment
goto :ManageComponentsAgain

:MainHelp
cls
call "!UiModule!" PrintHeader
echo Help
echo --------------------------------------------------------------------------
echo Recommended configuration makes safe choices from the detected computer
echo and installed components. Basic setup asks only about Java, enhancements,
echo and game memory. Customize configuration exposes every option, including
echo CPU tuning, Large Pages, and logging.
echo(
echo Nothing is replaced until you approve the final review.
echo(
pause
exit /b 0

:ConfigureJava
call "!UiModule!" ResetSelections
call :ChooseJava
if errorlevel 2 goto :MainMenu

:ConfigureRendering
call "!UiModule!" ResolvePrepatcherSelection
if errorlevel 2 goto :MainMenu
if errorlevel 1 goto :ConfigureJava
call "!UiModule!" ChooseRendering
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
call "!UiModule!" ChooseCpuSettings
if errorlevel 2 goto :MainMenu
if errorlevel 1 goto :ConfigureMemory

:ConfigureSystem
call "!UiModule!" ChooseSystemSettings
if errorlevel 2 goto :MainMenu
if errorlevel 1 goto :ConfigureCpu

:ConfigureLogging
call "!UiModule!" ChooseLogging
if errorlevel 2 goto :MainMenu
if errorlevel 1 goto :ConfigureSystem

:ReviewConfiguration
call "!GenerationModule!" NormalizeSelections
if errorlevel 1 goto :ConfigurationBuildFailed
call "!GenerationModule!" WriteInfoPending
if errorlevel 1 goto :ConfigurationBuildFailed
cls
call "!UiModule!" PrintHeader
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
    call "!GenerationModule!" CleanupPendingFiles
    goto :MainMenu
)
if errorlevel 3 (
    call "!GenerationModule!" CleanupPendingFiles
    goto :ConfigureJava
)
if errorlevel 2 (
    call "!GenerationModule!" CleanupPendingFiles
    goto :ConfigureLogging
)

goto :ApplyReviewedConfiguration

:ApplyReviewedConfiguration
call "!GenerationModule!" NormalizeSelections
if errorlevel 1 goto :ConfigurationBuildFailed
call "!GenerationModule!" BuildPendingFiles
if errorlevel 1 goto :ConfigurationBuildFailed
call "!GenerationModule!" CommitGeneratedFiles
if errorlevel 1 goto :ConfigurationCommitFailed
goto :ConfigurationComplete

:RunNonInteractive
call "!UiModule!" ResetSelections
if not defined MIKO_JAVA (
    echo MIKO_JAVA must name a Java folder beside starsector.exe.
    goto :NonInteractiveFailed
)
set "JavaPath=!MIKO_JAVA!"
if not exist "!JavaPath!\bin\java.exe" (
    echo MIKO_JAVA does not contain bin\java.exe: !JavaPath!
    goto :NonInteractiveFailed
)
if defined MIKO_JAVA_VERSION (
    set "JavaVersion=!MIKO_JAVA_VERSION!"
) else (
    call "!EnvironmentModule!" GetJavaMajor "!JavaPath!" JavaVersion
    if errorlevel 1 goto :NonInteractiveFailed
)
call "!EnvironmentModule!" IsSupportedJavaMajor "!JavaVersion!"
if errorlevel 1 goto :NonInteractiveFailed
if not defined MIKO_HEAP_MIB (
    call :SetHeapRecommendation
    set "SelectedHeapMiB=!RecommendedHeapMiB!"
    set "SelectedHeapDescription=!RecommendedHeapDescription!"
) else (
    call "!GenerationModule!" ValidatePositiveInteger "!MIKO_HEAP_MIB!"
    if errorlevel 1 goto :NonInteractiveFailed
    if !MIKO_HEAP_MIB! LSS 512 goto :NonInteractiveFailed
    if !MIKO_HEAP_MIB! GTR 1048576 goto :NonInteractiveFailed
    set "SelectedHeapMiB=!MIKO_HEAP_MIB!"
    set "SelectedHeapDescription=!MIKO_HEAP_MIB! MB (non-interactive)"
)
if "!MIKO_FAST_RENDERING!"=="1" if /I "!FastRenderingAvailable!"=="Yes" set "FastRenderingStatus=Enabled"
if "!MIKO_RESOURCE_CACHE!"=="1" if /I "!ResourceCacheAvailable!"=="Yes" set "ResourceCacheStatus=Enabled"
if "!MIKO_PREPATCHER!"=="1" if !PrepatcherCount! GTR 0 (
    call "!EnvironmentModule!" SelectPrepatcher "!PrepatcherCandidate[1]!"
    set "PrepatcherStatus=Enabled"
)
if "!MIKO_LOW_CORE!"=="1" set "LowCoreMode=Yes"
if "!MIKO_OLD_CPU!"=="1" set "OldCpuMode=Yes"
if "!MIKO_LARGE_PAGES!"=="1" set "LargePagesEnabled=Yes"
if defined MIKO_LOGGING set "LoggingMode=!MIKO_LOGGING!"
set "BackgroundSource="
set "BackgroundLabel="
if /I "!MIKO_BACKGROUND!"=="default" (
    set "BackgroundSource=mikohime\bg\default_bg.jpg"
    set "BackgroundLabel=Default Mikohime 25+"
)
if /I "!MIKO_BACKGROUND!"=="pather" (
    set "BackgroundSource=mikohime\bg\pather_bg.jpg"
    set "BackgroundLabel=Mikosector"
)
if /I "!MIKO_BACKGROUND!"=="mimikko" (
    set "BackgroundSource=mikohime\bg\mimikko_bg.jpg"
    set "BackgroundLabel=Mimikko"
)
if /I "!MIKO_BACKGROUND!"=="gamma" (
    set "BackgroundSource=mikohime\bg\gamma_bg.jpg"
    set "BackgroundLabel=Gamma"
)
if /I "!MIKO_BACKGROUND!"=="toadsector" (
    set "BackgroundSource=mikohime\bg\toadsector.jpg"
    set "BackgroundLabel=Toadsector"
)
if defined MIKO_BACKGROUND if not defined BackgroundSource goto :NonInteractiveFailed
call "!GenerationModule!" NormalizeSelections
if errorlevel 1 goto :NonInteractiveFailed
call "!GenerationModule!" BuildPendingFiles
if errorlevel 1 goto :NonInteractiveFailed
call "!GenerationModule!" CommitGeneratedFiles
if errorlevel 1 goto :NonInteractiveFailed
call "!GenerationModule!" ReleaseConfiguratorLock
exit /b 0

:NonInteractiveFailed
call "!GenerationModule!" CleanupPendingFiles
call "!GenerationModule!" ReleaseConfiguratorLock
exit /b 1

:ConfigurationBuildFailed
call "!GenerationModule!" CleanupPendingFiles
echo(
echo !ColorRed!Unable to build or validate the pending configuration.!ColorReset!
echo Existing configuration files were not replaced.
echo Check the installation files, free disk space, and folder permissions.
pause
goto :ReviewConfiguration

:ConfigurationCommitFailed
call "!GenerationModule!" CleanupPendingFiles
echo(
echo !ColorRed!Unable to install the new configuration.!ColorReset!
echo Previous files were restored where possible.
echo Check free disk space and folder permissions before trying again.
pause
goto :MainMenu

:ConfigurationComplete
cls
call "!UiModule!" PrintHeader
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
echo X. Return to the main menu
echo E. Exit
choice /c XE /n /m "Select an option: "
if errorlevel 2 goto :ExitConfigurator
call "!EnvironmentModule!" RefreshEnvironment
goto :MainMenu

:ExitConfigurator
call "!UiModule!" CleanupJdkInstallation
call "!UiModule!" CleanupResourceCacheInstallation
call "!GenerationModule!" CleanupPendingFiles
del /Q "!UpdateStatusFile!" 2>nul
call "!GenerationModule!" ReleaseConfiguratorLock
exit /b 0

REM ============================================================================
REM Main workflow input routines requiring local delayed-expansion control
REM ============================================================================

:ChooseJava
:ChooseJavaAgain
cls
call "!UiModule!" PrintHeader
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
call "!UiModule!" PrintHeader
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
set "HELPER_INPUT_PATH=!CD!\!InputTransferFile!"
for /f %%U in ('powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action TestUnsafeInput 2^>nul') do set "UnsafeInputFound=%%U"
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
call "!EnvironmentModule!" GetJavaMajor "!CustomJavaPath!" DetectedCustomJavaMajor
if errorlevel 1 (
    echo Unable to run "!CustomJavaPath!\bin\java.exe" and determine its version.
    pause
    goto :ChooseCustomJava
)
call "!EnvironmentModule!" IsSupportedJavaMajor "!DetectedCustomJavaMajor!"
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

:ChooseMemory
:ChooseMemoryAgain
cls
call "!UiModule!" PrintHeader
echo Step 3 of 7 - Memory
echo --------------------------------------------------------------------------
if defined PhysicalMemoryMiB (
    echo Detected physical memory: approximately !PhysicalMemoryGiB! GB
) else (
    echo Physical memory could not be detected.
)
echo(
for /L %%N in (1,1,6) do call "!UiModule!" PrintHeapPreset "%%N" "%%N"
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
set "HELPER_INPUT_PATH=!CD!\!InputTransferFile!"
for /f %%U in ('powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action TestUnsafeInput 2^>nul') do set "UnsafeInputFound=%%U"
if /I "!UnsafeInputFound!"=="Yes" (
    del /Q "!InputTransferFile!" 2>nul
    goto :InvalidCustomMemory
)
for /f "usebackq tokens=1,* delims==" %%A in ("!InputTransferFile!") do if /I "%%A"=="CustomHeapInput" set "CustomHeapMiB=%%B"
del /Q "!InputTransferFile!" 2>nul
if /I "!CustomHeapMiB!"=="B" goto :ChooseMemoryAgain
call "!GenerationModule!" ValidatePositiveInteger "!CustomHeapMiB!"
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
call "!UiModule!" PrintHeader
echo Step 3 of 7 - Advanced memory
echo --------------------------------------------------------------------------
echo These settings are intended for unusually large mod lists and systems
echo with substantially more physical RAM than the selected heap.
echo(
call "!UiModule!" PrintHeapPreset "1" "11"
call "!UiModule!" PrintHeapPreset "2" "12"
call "!UiModule!" PrintHeapPreset "3" "13"
call "!UiModule!" PrintHeapPreset "4" "14"
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
call "!UiModule!" PrintHeader
echo Step 3 of 7 - Extreme memory
echo --------------------------------------------------------------------------
echo !ColorYellow!WARNING: These heap sizes are unnecessary for almost all Starsector setups.!ColorReset!
echo They require substantially more physical RAM than the selected heap and
echo may reduce performance by increasing garbage-collection work.
echo(
call "!UiModule!" PrintHeapPreset "1" "15"
call "!UiModule!" PrintHeapPreset "2" "16"
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

@ECHO OFF
if "%~1"=="" exit /b 64
set "ConfiguratorGenerationAction=%~1"
shift /1
goto :%ConfiguratorGenerationAction%

REM Generation, transaction, installation, and runtime validation helpers.

:BuildPendingFiles
call :NormalizeSelections
if errorlevel 1 exit /b 1
call :CleanupPendingFiles
call :WriteInfoPending
if errorlevel 1 exit /b 1
call :WriteJvmArguments
if errorlevel 1 exit /b 1
call :WriteLoggingPending
if errorlevel 1 exit /b 1
call :WriteLauncherPending
if errorlevel 1 exit /b 1
if defined BackgroundSource (
    call :WriteBackgroundPending
    if errorlevel 1 exit /b 1
)
call :ValidateAllPendingFiles
exit /b %errorlevel%

:NormalizeSelections
if not defined JavaPath exit /b 1
call "!EnvironmentModule!" IsSupportedJavaMajor "!JavaVersion!"
if errorlevel 1 exit /b 1
call :ValidatePositiveInteger "!SelectedHeapMiB!"
if errorlevel 1 exit /b 1
if !SelectedHeapMiB! LSS 512 exit /b 1
if !SelectedHeapMiB! GTR 1048576 exit /b 1
if /I not "!FastRenderingAvailable!"=="Yes" set "FastRenderingStatus=Disabled"
if /I not "!FastRenderingStatus!"=="Enabled" set "ResourceCacheStatus=Disabled"
if /I not "!ResourceCacheAvailable!"=="Yes" set "ResourceCacheStatus=Disabled"
if /I "!PrepatcherStatus!"=="Enabled" (
    if not defined PrepatcherFolder exit /b 1
    if not exist "mods\!PrepatcherFolder!\agent\StarsectorPrepatcherAgent.jar" exit /b 1
)
if /I not "!LowCoreMode!"=="Yes" set "OldCpuMode=No"
if /I "!LargePagesEnabled!"=="Yes" if /I "!LargePagesPrivilegeStatus!"=="Not assigned" exit /b 1
if /I "!LoggingMode!"=="Full" exit /b 0
if /I "!LoggingMode!"=="Reduced" exit /b 0
if /I "!LoggingMode!"=="Minimal" exit /b 0
exit /b 1

:WriteInfoPending
type nul >"!InfoOutputFile!"
>>"!InfoOutputFile!" echo Memory allocation : !SelectedHeapDescription!
>>"!InfoOutputFile!" echo Java installation : !JavaPath! ^(Java !JavaVersion!^)
if "!JavaVersion!"=="28" (
    >>"!InfoOutputFile!" echo VM tuning         : Java 28 safe G1 preset
    >>"!InfoOutputFile!" echo Compact headers   : Enabled
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
if defined BackgroundLabel >>"!InfoOutputFile!" echo Launcher background: !BackgroundLabel!
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

:ApplyOldCpuCompatibility
findstr /V /X /L /C:"-XX:UseAVX=3" /C:"-XX:AVX3Threshold=0" /C:"-XX:CopyAVX3Threshold=0" /C:"-XX:+UseFMA" /C:"-XX:+UseBMI1Instructions" /C:"-XX:+UseBMI2Instructions" "!SimpleOutputFile!">"!SimpleOutputFile!.tmp"
if errorlevel 1 exit /b 1
move /Y "!SimpleOutputFile!.tmp" "!SimpleOutputFile!" >nul
if errorlevel 1 exit /b 1
>>"!SimpleOutputFile!" echo -XX:UseAVX=0
exit /b 0

:WriteJvmArguments
call :LoadJvmProfile "!JavaVersion!"
if errorlevel 1 exit /b 1
if "!JavaVersion!"=="27" (
    >>"!SimpleOutputFile!" echo -XX:+UseCriticalCompilerThreadPriority
    >>"!SimpleOutputFile!" echo -XX:ThreadPriorityPolicy=1
    >>"!SimpleOutputFile!" echo #-XX:MaxGCPauseMillis=100
    >>"!SimpleOutputFile!" echo -XX:CompilerDirectivesFile=..\\mikohime/.rouge_owo
    if /I "!JavaPath!"=="jdk-27+22Miko" >>"!SimpleOutputFile!" echo -XX:+AllowUnverifiedAgentClasses
)
call :AppendJvmDiagnosticLoggingOptions
if "!JavaVersion!"=="28" (
    if /I "!JavaPath!"=="jdk-28+13Miko" >>"!SimpleOutputFile!" echo -XX:+AllowUnverifiedAgentClasses
)
if "!JavaVersion!"=="27" (
    if /I "!OldCpuMode!"=="Yes" (
        call :ApplyOldCpuCompatibility
        if errorlevel 1 exit /b 1
    ) else (
        >>"!SimpleOutputFile!" echo !Avx2Argument!
    )
) else if /I "!OldCpuMode!"=="Yes" (
    >>"!SimpleOutputFile!" echo -XX:UseAVX=0
)
if /I "!LowCoreMode!"=="Yes" (
    >>"!SimpleOutputFile!" echo -XX:CICompilerCount=2
    >>"!SimpleOutputFile!" echo -XX:ConcGCThreads=1
)
if /I "!LargePagesEnabled!"=="Yes" >>"!SimpleOutputFile!" echo !LargePagesArgument!
call :AppendJvmLoggerOptions
call :AppendPlatformSystemProperties
>>"!SimpleOutputFile!" echo !ThreadStackSize!
>>"!SimpleOutputFile!" echo !MinHeapPrefix!!SelectedHeapMiB!m
>>"!SimpleOutputFile!" echo !MaxHeapPrefix!!SelectedHeapMiB!m
call :AppendAgentsAndClasspath
call :AppendGamePaths
exit /b 0

:LoadJvmProfile
set "JvmProfilePath=!SharedConfigRoot!\jvm\java-%~1.args"
if not exist "!JvmProfilePath!" (
    echo Missing JVM profile: !JvmProfilePath!
    exit /b 1
)
set "JVM_PROFILE_PATH=!CD!\!JvmProfilePath!"
set "JVM_PROFILE_OUTPUT=!CD!\!SimpleOutputFile!"
set "JVM_COMPILER_DIRECTIVES=..\\mikohime/.rouge_owo"
powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action RenderProfile
if errorlevel 1 exit /b 1
call :ValidatePendingFile "!SimpleOutputFile!"
exit /b 0

:AppendJvmDiagnosticLoggingOptions
set "JvmLoggingPrefix="
if /I "!LoggingMode!"=="Minimal" set "JvmLoggingPrefix=#"
>>"!SimpleOutputFile!" echo !JvmLoggingPrefix!-XX:+ShowCodeDetailsInExceptionMessages
>>"!SimpleOutputFile!" echo !JvmLoggingPrefix!-XX:+ExtensiveErrorReports
if "!JavaVersion!"=="28" >>"!SimpleOutputFile!" echo !JvmLoggingPrefix!-XX:+ErrorLogSecondaryErrorDetails
>>"!SimpleOutputFile!" echo !JvmLoggingPrefix!-XX:+PrintCommandLineFlags
if not "!JavaVersion!"=="28" >>"!SimpleOutputFile!" echo -Xlog:async
>>"!SimpleOutputFile!" echo !JvmLoggingPrefix!-Xlog:gc+init
exit /b 0

:AppendJvmLoggerOptions
set "JvmLoggingPrefix="
if /I "!LoggingMode!"=="Minimal" set "JvmLoggingPrefix=#"
set "BusySpinPrefix=!JvmLoggingPrefix!"
if "!JavaVersion!"=="28" set "BusySpinPrefix=#"
>>"!SimpleOutputFile!" echo !BusySpinPrefix!-DAsyncLogger.WaitStrategy=busyspin
>>"!SimpleOutputFile!" echo !JvmLoggingPrefix!-Dsun.java2d.renderer.useLogger=true
exit /b 0

:AppendPlatformSystemProperties
>>"!SimpleOutputFile!" echo -Djava.library.path="..\\mikohime/windows"
>>"!SimpleOutputFile!" echo -Dlog4j.configuration=..\\mikohime/mikohime.properties
>>"!SimpleOutputFile!" echo -Djava.xml.config.file=..\\mikohime/miko_jxp.properties
>>"!SimpleOutputFile!" echo -Dcom.fs.starfarer.launcher_bg=..\\mikohime/launcher_bg.jpg
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
set "LoggingTemplate=!SharedConfigRoot!\logging\!LoggingMode!.properties"
if not exist "!LoggingTemplate!" exit /b 1
copy /Y "!LoggingTemplate!" "!LoggingOutputFile!" >nul
if errorlevel 1 exit /b 1
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

:WriteBackgroundPending
if not exist "!BackgroundSource!" exit /b 1
copy /Y "!BackgroundSource!" "!BackgroundOutputFile!" >nul
if errorlevel 1 exit /b 1
call :ValidatePendingFile "!BackgroundOutputFile!"
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
if defined BackgroundSource (
    call :ValidatePendingFile "!BackgroundOutputFile!"
    if errorlevel 1 exit /b 1
)
set "JVM_ARGS_PATH=!CD!\!SimpleOutputFile!"
set "JVM_ARGS_MAJOR=!JavaVersion!"
powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action ValidateArgs
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
set "CommitFileCount=4"
set "CommitTarget[5]="
set "CommitPending[5]="
if defined BackgroundSource (
    set "CommitFileCount=5"
    set "CommitTarget[5]=mikohime\launcher_bg.jpg"
    set "CommitPending[5]=!BackgroundOutputFile!"
)

for /L %%N in (1,1,!CommitFileCount!) do (
    set "CommitBackup[%%N]=!CommitTarget[%%N]!!BackupSuffix!"
    set "CommitHadOriginal[%%N]=No"
    if exist "!CommitTarget[%%N]!" (
        copy /Y "!CommitTarget[%%N]!" "!CommitBackup[%%N]!" >nul
        if errorlevel 1 goto :CommitPreparationFailed
        set "CommitHadOriginal[%%N]=Yes"
    )
)

for /L %%N in (1,1,!CommitFileCount!) do (
    move /Y "!CommitPending[%%N]!" "!CommitTarget[%%N]!" >nul
    if errorlevel 1 goto :CommitRollback
)
call :DeleteCommitBackups
exit /b 0

:CommitPreparationFailed
call :DeleteCommitBackups
exit /b 1

:CommitRollback
for /L %%N in (1,1,!CommitFileCount!) do (
    if /I "!CommitHadOriginal[%%N]!"=="Yes" if exist "!CommitBackup[%%N]!" move /Y "!CommitBackup[%%N]!" "!CommitTarget[%%N]!" >nul
    if /I "!CommitHadOriginal[%%N]!"=="No" del /Q "!CommitTarget[%%N]!" 2>nul
)
call :DeleteCommitBackups
exit /b 1

:DeleteCommitBackups
for /L %%N in (1,1,!CommitFileCount!) do if defined CommitBackup[%%N] del /Q "!CommitBackup[%%N]!" 2>nul
exit /b 0

:CleanupPendingFiles
del /Q "!SimpleOutputFile!" "!LauncherOutputFile!" "!LoggingOutputFile!" "!InfoOutputFile!" "!BackgroundOutputFile!" 2>nul
del /Q "!JavaVersionOutput!" 2>nul
del /Q "!InputTransferFile!" 2>nul
exit /b 0

REM ============================================================================
REM Installation and input validation
REM ============================================================================

:AcquireConfiguratorLock
if exist "!LockDirectory!\." goto :ConfiguratorLockExists
mkdir "!LockDirectory!" >nul 2>&1
if errorlevel 1 (
    if exist "!LockDirectory!\." goto :ConfiguratorLockExists
    goto :WriteAccessFailed
)
>"!LockDirectory!\owner.txt" echo Started !DATE! !TIME! by !USERNAME! on !COMPUTERNAME!
exit /b 0

:ConfiguratorLockExists
echo Another configurator instance is already running, or a stale lock exists:
echo   !CD!\!LockDirectory!
echo Close the other instance. If none is running, remove that directory manually.
if /I not "!NonInteractiveMode!"=="Yes" pause
exit /b 1

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
if /I not "!NonInteractiveMode!"=="Yes" pause
exit /b 1

:CheckWriteAccess
set "WriteTest=.configure_write_test_!RANDOM!_!RANDOM!.tmp"
del /Q "!WriteTest!" >nul 2>&1
copy /Y nul "!WriteTest!" >nul 2>&1
if not exist "!WriteTest!" goto :WriteAccessFailed
del /Q "!WriteTest!" 2>nul
set "WriteTest=mikohime\.configure_write_test_!RANDOM!_!RANDOM!.tmp"
del /Q "!WriteTest!" >nul 2>&1
copy /Y nul "!WriteTest!" >nul 2>&1
if not exist "!WriteTest!" goto :WriteAccessFailed
del /Q "!WriteTest!" 2>nul
exit /b 0

:WriteAccessFailed
del /Q "!WriteTest!" 2>nul
echo Administrator access is required for this Starsector installation.
echo If Starsector is installed under Program Files or Program Files (x86),
echo close this window,
echo right-click Configure_Me.cmd, and select "Run as administrator".
echo Alternatively, move Starsector to a folder your account can write to.
if /I not "!NonInteractiveMode!"=="Yes" pause
exit /b 1

:ValidatePositiveInteger
if "%~1"=="" exit /b 1
for /f "delims=0123456789" %%A in ("%~1") do exit /b 1
if "%~1"=="0" exit /b 1
exit /b 0

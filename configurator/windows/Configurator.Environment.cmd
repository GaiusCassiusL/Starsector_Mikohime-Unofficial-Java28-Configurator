@ECHO OFF
if "%~1"=="" exit /b 64
set "ConfiguratorEnvironmentAction=%~1"
shift /1
goto :%ConfiguratorEnvironmentAction%

REM Environment and component detection. Shared mutations must persist in the launcher.

:RefreshEnvironment
if defined UpdateStatusFile del /Q "!UpdateStatusFile!" 2>nul
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
for /f "usebackq delims=" %%D in (`powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action ListJavaFolders 2^>nul`) do if exist "%%D\bin\java.exe" call :ProbeJavaFolder "%%D" "Java installation"
exit /b 0

:DetectUnsafeComponentNames
set "UnsafeComponentNamesFound=No"
for /f %%U in ('powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action DetectUnsafeNames 2^>nul') do set "UnsafeComponentNamesFound=%%U"
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
set "FastRenderingVersion=version unknown"
if exist "starsector-core\fr.jar" if exist "starsector-core\fr.agent.jar" (
    set "FastRenderingAvailable=Yes"
    call :GetJarVersion "starsector-core\fr.jar" FastRenderingVersion
    if /I "!FastRenderingVersion!"=="version unknown" call :GetJarVersion "starsector-core\fr.agent.jar" FastRenderingVersion
)

set "ResourceCacheAvailable=No"
set "ResourceCacheVersion=version unknown"
if exist "starsector-core\fr-resource-cache-agent.jar" (
    for %%F in ("starsector-core\fr-resource-cache-agent.jar") do if %%~zF GTR 0 (
        set "ResourceCacheAvailable=Yes"
        call :GetJarVersion "starsector-core\fr-resource-cache-agent.jar" ResourceCacheVersion
    )
)

set "VramOptimizerAvailable=No"
set "VramOptimizerVersion=version unknown"
for /f "tokens=1,2 delims=|" %%V in ('powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action DetectVramOptimizer 2^>nul') do (
    set "VramOptimizerAvailable=%%V"
    set "VramOptimizerVersion=%%W"
)

for /L %%N in (1,1,!PrepatcherCount!) do (
    set "PrepatcherCandidate[%%N]="
    set "PrepatcherCandidateVersion[%%N]="
)
set "PrepatcherCount=0"
set "IncompatiblePrepatcherCount=0"
set "PrepatcherFolder="
set "PrepatcherAgent="
set "PrepatcherStatus=Disabled"
for /f "usebackq delims=" %%D in (`powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action ListPrepatcherFolders 2^>nul`) do call :AddPrepatcherCandidate "%%D"
call :SetAutomaticPrepatcherSelection
exit /b 0

:GetJarVersion
set "%~2=version unknown"
set "HELPER_JAR_PATH=!CD!\%~1"
for /f "usebackq delims=" %%V in (`powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action GetJarVersion 2^>nul`) do set "%~2=%%V"
exit /b 0

:AddPrepatcherCandidate
if not exist "mods\%~1\agent\StarsectorPrepatcherAgent.jar" exit /b 0
for %%F in ("mods\%~1\agent\StarsectorPrepatcherAgent.jar") do if %%~zF LEQ 0 exit /b 0
set "PrepatcherMetadataPath=mods\%~1\mod_info.json"
set "PrepatcherVersionStatus="
set "DetectedPrepatcherVersion="
set "HELPER_MINIMUM_VERSION=!MinimumPrepatcherVersion!"
set "HELPER_METADATA_PATH=!CD!\!PrepatcherMetadataPath!"
for /f "tokens=1,2 delims=|" %%S in ('powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action GetPrepatcherVersion 2^>nul') do (
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
for /f "usebackq tokens=1-3 delims=|" %%M in (`powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action GetSystemInfo 2^>nul`) do (
    set "PhysicalMemoryMiB=%%M"
    set "PhysicalCoreCount=%%N"
    set "LogicalProcessorCount=%%O"
)
if defined PhysicalMemoryMiB (
    call "!GenerationModule!" ValidatePositiveInteger "!PhysicalMemoryMiB!"
    if errorlevel 1 set "PhysicalMemoryMiB="
)
if defined PhysicalCoreCount (
    call "!GenerationModule!" ValidatePositiveInteger "!PhysicalCoreCount!"
    if errorlevel 1 set "PhysicalCoreCount="
)
if defined LogicalProcessorCount (
    call "!GenerationModule!" ValidatePositiveInteger "!LogicalProcessorCount!"
    if errorlevel 1 set "LogicalProcessorCount="
)
if not defined LogicalProcessorCount set "LogicalProcessorCount=1"
set "HELPER_MEMORY_MIB=!PhysicalMemoryMiB!"
if defined PhysicalMemoryMiB for /f "tokens=1,2 delims=|" %%M in ('powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "!HelperScript!" -Action GetMemorySummary 2^>nul') do (
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

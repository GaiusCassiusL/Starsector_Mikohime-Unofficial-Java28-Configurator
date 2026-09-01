@echo off
setlocal
cd /d "%~dp0"

if exist ".jdk\21\bin\javac.exe" (
  set "JAVA_HOME=%CD%\.jdk\21"
)

if not defined JAVA_HOME (
  echo JAVA_HOME is not set and no local JDK 21 was found in .jdk\21. 1>&2
  exit /b 1
)

if not exist "%JAVA_HOME%\bin\javac.exe" (
  echo JAVA_HOME does not point to a JDK: %JAVA_HOME% 1>&2
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
call ".\gradlew.bat" clean verify --no-daemon --console=plain
exit /b %ERRORLEVEL%

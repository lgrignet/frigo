@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%"

if not defined JAVA_HOME (
  if exist "C:\Program Files\Java\jdk-21.0.10\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.10"
  )
)

if not defined JAVA_HOME (
  echo ERROR: JAVA_HOME n'est pas defini. Configure Java 21 puis relance.
  popd
  exit /b 1
)

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo ERROR: JAVA_HOME est invalide: %JAVA_HOME%
  popd
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"

call .\gradlew.bat clean assembleRelease bundleRelease
if errorlevel 1 (
  echo.
  echo Build release en echec.
  popd
  exit /b 1
)

echo.
echo Build release termine.
echo APKs: .\app\build\outputs\apk\release\
echo AAB : .\app\build\outputs\bundle\release\app-release.aab

popd
exit /b 0

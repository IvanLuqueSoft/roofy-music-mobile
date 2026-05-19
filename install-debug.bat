@echo off
setlocal

cd /d "%~dp0"

set "JAVA_HOME=C:\roofy-music-projects\.tools\temurin21\jdk-21.0.11+10"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo.
echo [Roofy Music] Checking connected Android devices...
where adb >nul 2>nul
if errorlevel 1 (
  echo ERROR: adb was not found on PATH.
  echo Install Android platform-tools or open this from an Android Studio terminal.
  exit /b 1
)

adb devices | findstr /R /C:"device$" >nul
if errorlevel 1 (
  echo ERROR: No authorized Android device found.
  echo Enable USB debugging, connect your phone, and accept the RSA prompt.
  adb devices
  exit /b 1
)

echo.
echo [Roofy Music] Building and installing Foss debug APK...
call gradlew.bat :app:installFossDebug
if errorlevel 1 (
  echo.
  echo ERROR: Build or install failed.
  exit /b 1
)

echo.
echo [Roofy Music] Installed successfully.
endlocal

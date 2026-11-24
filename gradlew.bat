@echo off
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle is not installed. Install Gradle 8.9 and Android SDK Platform 35.
  exit /b 1
)
gradle %*

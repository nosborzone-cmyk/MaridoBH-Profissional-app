@echo off
set GRADLE_VERSION=8.7
set GRADLE_BASE=%USERPROFILE%\.gradle\manual
set GRADLE_HOME=%GRADLE_BASE%\gradle-%GRADLE_VERSION%
set GRADLE_BIN=%GRADLE_HOME%\bin\gradle.bat
if not exist "%GRADLE_BIN%" (
  echo Para Windows local, instale o Gradle 8.7 ou use o GitHub Actions para compilar na nuvem.
  echo Este projeto foi preparado principalmente para build online no GitHub Actions.
  exit /b 1
)
"%GRADLE_BIN%" %*

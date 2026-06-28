@echo off
IF EXIST gradlew.bat (
  call gradlew.bat assembleDebug
) ELSE (
  gradle assembleDebug
)

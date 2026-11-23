@echo off
setlocal
cd /d "%~dp0"
echo Building MURIM LIFE single-JAR unified mod...
call gradlew.bat --no-daemon clean build --stacktrace > build_log_unified.txt 2>&1
if errorlevel 1 (
  echo BUILD FAILED. Send build_log_unified.txt to ChatGPT.
  type build_log_unified.txt
  pause
  exit /b 1
)
echo BUILD SUCCESS. Find the v1.26.3 compiled JAR in build\libs\.
echo NeoForge and Epic Fight 21.17.3.1 must be present.
pause

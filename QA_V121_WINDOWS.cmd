@echo off
setlocal
cd /d "%~dp0"
if not exist qa-classes mkdir qa-classes
javac -encoding UTF-8 -d qa-classes scripts\JavaParseOnly.java
if errorlevel 1 exit /b 1
java -cp qa-classes JavaParseOnly src\main\java
if errorlevel 1 exit /b 1
javac -encoding UTF-8 -d qa-classes src\main\java\kr\murimlife\npc\combat\MotionBlend.java src\main\java\kr\murimlife\npc\combat\ClashRules.java src\testkit\java\MurimV121PureTests.java
if errorlevel 1 exit /b 1
java -cp qa-classes MurimV121PureTests
if errorlevel 1 exit /b 1
py -3 scripts\qa_v121.py
if errorlevel 1 exit /b 1
echo v1.21 static QA completed. NeoForge compile and Minecraft runtime are not included in this check.
pause

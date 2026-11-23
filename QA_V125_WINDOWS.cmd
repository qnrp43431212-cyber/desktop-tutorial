@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>&1 || (echo Java 21 JDK required. & exit /b 1)
where py >nul 2>&1 || (echo Python 3 required. & exit /b 1)
if not exist qa-classes mkdir qa-classes
javac -encoding UTF-8 -d qa-classes scripts\JavaParseOnly.java scripts\JavaDuplicateCheck.java
if errorlevel 1 exit /b 1
java -cp qa-classes JavaParseOnly src\main\java
if errorlevel 1 exit /b 1
java -cp qa-classes JavaDuplicateCheck src\main\java
if errorlevel 1 exit /b 1
javac -encoding UTF-8 -d qa-classes src\main\java\kr\murimlife\cult\CultRankRules.java src\testkit\java\MurimV125CultTests.java
if errorlevel 1 exit /b 1
java -cp qa-classes MurimV125CultTests
if errorlevel 1 exit /b 1
py -3 scripts\qa_v125.py
if errorlevel 1 exit /b 1
py -3 scripts\qa_v124.py
if errorlevel 1 exit /b 1
echo PASS static source check only. Run BUILD_WINDOWS.cmd for NeoForge compilation.
pause

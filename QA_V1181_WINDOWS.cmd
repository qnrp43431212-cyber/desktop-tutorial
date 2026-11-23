@echo off
setlocal
cd /d "%~dp0"
if not exist qa-classes mkdir qa-classes
javac -encoding UTF-8 -d qa-classes scripts\JavaSyntaxCheck.java
if errorlevel 1 exit /b 1
java -cp qa-classes JavaSyntaxCheck src\main\java
if errorlevel 1 exit /b 1
javac -encoding UTF-8 -d qa-classes src\main\java\kr\murimlife\worldgen\PlanningCore.java src\testkit\java\kr\murimlife\worldgen\PlanningCoreTest.java
if errorlevel 1 exit /b 1
java -cp qa-classes kr.murimlife.worldgen.PlanningCoreTest
if errorlevel 1 exit /b 1
py -3 scripts\static_qa_v1181.py
if errorlevel 1 exit /b 1
echo Static QA passed. API type checks and Minecraft gameplay tests still required.
pause

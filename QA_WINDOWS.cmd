@echo off
setlocal
cd /d "%~dp0"
if not exist qa-classes mkdir qa-classes
javac -encoding UTF-8 -d qa-classes src\main\java\kr\murimlife\worldgen\PlanningCore.java src\testkit\java\kr\murimlife\worldgen\PlanningCoreTest.java
if errorlevel 1 (echo QA COMPILATION FAILED & pause & exit /b 1)
java -cp qa-classes kr.murimlife.worldgen.PlanningCoreTest
pause

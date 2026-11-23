@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>&1 || (echo Java 21 JDK is required. & exit /b 1)
where py >nul 2>&1 || (echo Python 3 is required. & exit /b 1)
if not exist qa-classes mkdir qa-classes
if not exist .qa_v123_temp mkdir .qa_v123_temp
javac -encoding UTF-8 -d qa-classes scripts\JavaParseOnly.java scripts\JavaDuplicateCheck.java
if errorlevel 1 goto fail
java -cp qa-classes JavaParseOnly src\main\java
if errorlevel 1 goto fail
java -cp qa-classes JavaDuplicateCheck src\main\java
if errorlevel 1 goto fail
javac -encoding UTF-8 -d .qa_v123_temp src\main\java\kr\murimlife\economy\CurrencyRules.java src\testkit\java\MurimV123PureTests.java
if errorlevel 1 goto fail
java -cp .qa_v123_temp MurimV123PureTests
if errorlevel 1 goto fail
py -3 scripts\validate_assets.py
if errorlevel 1 goto fail
py -3 scripts\verify_v16_assets.py
if errorlevel 1 goto fail
py -3 scripts\npc_static_qa.py
if errorlevel 1 goto fail
py -3 scripts\npc_v120_qa.py
if errorlevel 1 goto fail
py -3 scripts\qa_v121.py
if errorlevel 1 goto fail
py -3 scripts\qa_v122.py
if errorlevel 1 goto fail
py -3 scripts\qa_v123.py
if errorlevel 1 goto fail
echo PASS: v1.23 source checks completed (not a full NeoForge build).
rmdir /s /q .qa_v123_temp
pause
exit /b 0
:fail
echo QA FAILED. Please send this console output to ChatGPT.
pause
exit /b 1

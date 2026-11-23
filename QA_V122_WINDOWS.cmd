@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>&1 || (echo Java 21 JDK javac not found. & exit /b 1)
where py >nul 2>&1 || (echo Python not found. & exit /b 1)
if not exist qa-classes mkdir qa-classes
javac -encoding UTF-8 -d qa-classes scripts\JavaParseOnly.java scripts\JavaDuplicateCheck.java
if errorlevel 1 exit /b 1
java -cp qa-classes JavaParseOnly src\main\java
if errorlevel 1 exit /b 1
java -cp qa-classes JavaDuplicateCheck src\main\java
if errorlevel 1 exit /b 1
javac -encoding UTF-8 -d qa-classes src\main\java\kr\murimlife\npc\combat\ClashRules.java src\main\java\kr\murimlife\npc\combat\MotionBlend.java src\main\java\kr\murimlife\npc\combat\WeaponTactics.java src\main\java\kr\murimlife\npc\dialogue\DialogueRules.java src\testkit\java\MurimV121PureTests.java src\testkit\java\MurimV122PureTests.java
if errorlevel 1 exit /b 1
java -cp qa-classes MurimV121PureTests
if errorlevel 1 exit /b 1
java -cp qa-classes MurimV122PureTests
if errorlevel 1 exit /b 1
py -3 scripts\validate_assets.py
if errorlevel 1 exit /b 1
py -3 scripts\verify_v16_assets.py
if errorlevel 1 exit /b 1
py -3 scripts\npc_static_qa.py
if errorlevel 1 exit /b 1
py -3 scripts\npc_v120_qa.py
if errorlevel 1 exit /b 1
py -3 scripts\qa_v121.py
if errorlevel 1 exit /b 1
py -3 scripts\qa_v122.py
if errorlevel 1 exit /b 1
echo All Java syntax, independent rules, asset and v1.22 consistency checks passed.
echo Full NeoForge build and in-game testing still require BUILD_WINDOWS.cmd and Minecraft.
pause

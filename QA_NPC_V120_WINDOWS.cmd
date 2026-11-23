@echo off
setlocal
cd /d "%~dp0"
if not exist build\npc_qa_v120 mkdir build\npc_qa_v120
javac -encoding UTF-8 -d build\npc_qa_v120 scripts\JavaSyntaxCheck.java
if errorlevel 1 goto :fail
java -cp build\npc_qa_v120 JavaSyntaxCheck src\main\java
if errorlevel 1 goto :fail
javac -encoding UTF-8 -d build\npc_qa_v120 src\main\java\kr\murimlife\npc\NpcSpawnRules.java src\main\java\kr\murimlife\npc\NpcLifecycleRules.java src\testkit\java\kr\murimlife\npc\NpcSpawnRulesTest.java src\testkit\java\kr\murimlife\npc\NpcLifecycleRulesTest.java
if errorlevel 1 goto :fail
java -cp build\npc_qa_v120 kr.murimlife.npc.NpcSpawnRulesTest
if errorlevel 1 goto :fail
java -cp build\npc_qa_v120 kr.murimlife.npc.NpcLifecycleRulesTest
if errorlevel 1 goto :fail
python scripts\npc_v120_qa.py
if errorlevel 1 goto :fail
echo ALL STATIC TESTS PASS. NeoForge API compile and Minecraft runtime are NOT verified.
pause
exit /b 0
:fail
echo TEST FAILURE. Please send log or screenshot.
pause
exit /b 1

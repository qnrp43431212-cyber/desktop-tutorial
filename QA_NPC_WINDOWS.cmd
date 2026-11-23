@echo off
setlocal
cd /d "%~dp0"
if not exist qa-classes mkdir qa-classes
javac -encoding UTF-8 -d qa-classes src\main\java\kr\murimlife\npc\NpcSpawnRules.java src\testkit\java\kr\murimlife\npc\NpcSpawnRulesTest.java
if errorlevel 1 (echo NPC JAVA TEST FAILED & pause & exit /b 1)
java -cp qa-classes kr.murimlife.npc.NpcSpawnRulesTest
if errorlevel 1 (echo NPC JAVA TEST FAILED & pause & exit /b 1)
py -3 scripts\npc_static_qa.py
if errorlevel 1 (echo NPC DATA TEST FAILED & pause & exit /b 1)
echo NPC preparatory static QA PASSED. This does NOT prove entities will spawn in Minecraft.
pause

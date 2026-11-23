@echo off
setlocal
set "ROOT=%~dp0"
set "OUT=%TEMP%\murimlife_v126_qa_%RANDOM%"
mkdir "%OUT%" 2>nul
pushd "%OUT%"
echo === v1.26 parser ===
javac -encoding UTF-8 -d . "%ROOT%scripts\JavaParseOnly.java" "%ROOT%scripts\JavaDuplicateCheck.java"
if errorlevel 1 goto :bad
java -cp . JavaParseOnly "%ROOT%src\main\java"
if errorlevel 1 goto :bad
java -cp . JavaDuplicateCheck "%ROOT%src\main\java"
if errorlevel 1 goto :bad
javac -encoding UTF-8 -d . "%ROOT%src\main\java\kr\murimlife\qi\QiDeviationRules.java" "%ROOT%src\main\java\kr\murimlife\cult\CultMentorRules.java" "%ROOT%src\main\java\kr\murimlife\medicine\ElixirCatalog.java" "%ROOT%src\testkit\java\MurimV126CultivationTests.java"
if errorlevel 1 goto :bad
java -cp . MurimV126CultivationTests
if errorlevel 1 goto :bad
popd
py -3 "%ROOT%scripts\qa_v126.py"
if errorlevel 1 goto :badlast
rmdir /s /q "%OUT%"
echo ALL SOURCE-ONLY CHECKS PASS. Full NeoForge build is NOT covered.
pause
exit /b 0
:bad
popd
:badlast
echo QA FAILED. See output above.
pause
exit /b 1

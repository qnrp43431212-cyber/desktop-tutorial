@echo off
REM NeoForge MDK Gradle Wrapper is required. Place official gradlew.bat and gradle/wrapper directory here first.
if not exist gradlew.bat (
 echo Missing official NeoForge MDK Gradle Wrapper files. See README_KO.md
 exit /b 1
)
call gradlew.bat build

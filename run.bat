@echo off
REM Compiles and starts the Digital Evidence & Case Management System (Windows)
cd /d "%~dp0"
if not exist out mkdir out
javac -d out -sourcepath src src\dems\Main.java
if errorlevel 1 (
    echo.
    echo Compilation failed. Make sure a JDK ^(not just a JRE^) is installed: run  javac -version
    pause
    exit /b 1
)
java -cp out dems.Main

@echo off
rem ReadyGo automated tests (Windows). Results: tests\results\
setlocal
cd /d "%~dp0.."

if not exist out mkdir out
javac -encoding UTF-8 --release 17 -d out src\readygo\*.java || exit /b 1
javac -encoding UTF-8 --release 17 -cp out -d out\tests tests\ReadyGoTests.java tests\readygo\*.java || exit /b 1
java -cp "out;out\tests" ReadyGoTests
exit /b %ERRORLEVEL%

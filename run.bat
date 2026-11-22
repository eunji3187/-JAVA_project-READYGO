@echo off
rem ReadyGo run (Windows)
rem   run.bat          Seoul (default region), KMA API
rem   run.bat Busan    the given region (Korean region name)
setlocal
cd /d "%~dp0"

if not exist out mkdir out
javac -encoding UTF-8 --release 17 -d out src\readygo\*.java || exit /b 1
java -cp out readygo.Main %*

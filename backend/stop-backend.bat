@echo off
setlocal

set FOUND=0
for /f "tokens=5" %%p in ('netstat -ano ^| findstr /R /C:":8080 .*LISTENING"') do (
  set FOUND=1
  echo Stopping process on port 8080. PID: %%p
  taskkill /PID %%p /F
)

if "%FOUND%"=="0" (
  echo No process is listening on port 8080.
)

pause

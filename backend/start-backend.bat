@echo off
setlocal
cd /d "%~dp0"

for /f "tokens=5" %%p in ('netstat -ano ^| findstr /R /C:":8080 .*LISTENING"') do (
  echo Restarting existing process on port 8080. PID: %%p
  taskkill /PID %%p /F >nul
)

echo Building backend package...
call mvn -q -DskipTests package
if errorlevel 1 (
  echo Backend build failed.
  pause
  exit /b 1
)

echo Starting hotel backend on http://127.0.0.1:8080
start "hotel-backend" /D "%~dp0" java -jar "target\hotel-management-backend-0.0.1-SNAPSHOT.jar"
timeout /t 6 /nobreak >nul
start "" "http://127.0.0.1:8080/api/auth/me"
echo Backend started. Keep the hotel-backend window open while testing.
pause

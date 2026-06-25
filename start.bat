@echo off
setlocal enabledelayedexpansion

set "ROOT_DIR=%~dp0"
set "BACKEND_DIR=%ROOT_DIR%driving-school-backend"
set "FRONTEND_DIR=%ROOT_DIR%driving-school-frontend"
set "LOG_DIR=%ROOT_DIR%logs"

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo ========================================
echo   Driving School - Starting...
echo ========================================
echo.

:: Auto detect local IP
echo [Init] Detecting local IP...
set "LOCAL_IP="
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /R "IPv4.*[0-9]*\.[0-9]*\.[0-9]*\.[0-9]*"') do (
    for /f "tokens=1" %%b in ("%%a") do (
        set "IP=%%b"
        if not "!IP:~0,4!"=="127." if not "!IP:~0,4!"=="169." (
            if "!IP:~0,8!"=="192.168." set "LOCAL_IP=!IP!"
            if "!IP:~0,3!"=="10." if not defined LOCAL_IP set "LOCAL_IP=!IP!"
        )
    )
)

if defined LOCAL_IP (
    echo       Local IP: %LOCAL_IP%
    powershell -Command "(Get-Content '%ROOT_DIR%driving-school-miniapp\src\utils\config.js') -replace 'http://[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+:8080/api', 'http://%LOCAL_IP%:8080/api' | Set-Content '%ROOT_DIR%driving-school-miniapp\src\utils\config.js' -Encoding UTF8" >nul 2>&1
    powershell -Command "(Get-Content '%ROOT_DIR%driving-school-miniapp\src\utils\config.js') -replace 'http://127\.0\.0\.1:8080/api', 'http://%LOCAL_IP%:8080/api' | Set-Content '%ROOT_DIR%driving-school-miniapp\src\utils\config.js' -Encoding UTF8" >nul 2>&1
    powershell -Command "(Get-Content '%ROOT_DIR%driving-school-frontend\vite.config.js') -replace 'http://[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+:8080', 'http://%LOCAL_IP%:8080' | Set-Content '%ROOT_DIR%driving-school-frontend\vite.config.js' -Encoding UTF8" >nul 2>&1
    powershell -Command "(Get-Content '%ROOT_DIR%driving-school-frontend\vite.config.js') -replace 'http://127\.0\.0\.1:8080', 'http://%LOCAL_IP%:8080' | Set-Content '%ROOT_DIR%driving-school-frontend\vite.config.js' -Encoding UTF8" >nul 2>&1
    echo       Config updated
    echo [Build] Building miniapp...
    cd /d "%ROOT_DIR%driving-school-miniapp" && call npx uni build -p mp-weixin >nul 2>&1
    echo       Miniapp built
) else (
    echo       [Warning] No local IP found, using default config
)

:: Check ports
netstat -ano | findstr ":8080.*LISTEN" >nul 2>&1
if not errorlevel 1 (
    echo [Backend] Port 8080 is in use. Run stop.bat first.
    pause & exit /b 1
)
netstat -ano | findstr ":3000.*LISTEN" >nul 2>&1
if not errorlevel 1 (
    echo [Frontend] Port 3000 is in use. Run stop.bat first.
    pause & exit /b 1
)

:: Start backend in background (hidden window)
echo [Start] Backend starting...
powershell -Command "Start-Process -FilePath 'cmd.exe' -ArgumentList '/c','cd /d \"%BACKEND_DIR%\" && mvn spring-boot:run > \"%LOG_DIR%\backend.log\" 2>&1' -WindowStyle Hidden"

:: Start frontend in background (hidden window)
echo [Start] Frontend starting...
powershell -Command "Start-Process -FilePath 'cmd.exe' -ArgumentList '/c','cd /d \"%FRONTEND_DIR%\" && npm run dev > \"%LOG_DIR%\frontend.log\" 2>&1' -WindowStyle Hidden"

:: Wait for services
echo [Wait] Waiting for services...
timeout /t 5 /nobreak >nul

:: Check and save PIDs
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTEN"') do (
    echo %%a > "%LOG_DIR%\backend.pid"
    echo       Backend PID: %%a
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3000.*LISTEN"') do (
    echo %%a > "%LOG_DIR%\frontend.pid"
    echo       Frontend PID: %%a
)

echo.
echo ========================================
echo   Started!
echo ========================================
echo.
echo   Backend:  http://localhost:8080/api
echo   Frontend: http://localhost:3000
echo   Logs:     logs\backend.log, logs\frontend.log
echo.
echo   Run stop.bat to stop all services.
echo.
timeout /t 5 /nobreak >nul

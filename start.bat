@echo off
chcp 65001 >nul 2>&1
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

if not defined LOCAL_IP (
    echo       [Warning] No local IP found, using default config
) else (
    echo       Local IP: !LOCAL_IP!
    powershell -Command "(Get-Content '!ROOT_DIR!driving-school-miniapp\src\utils\config.js') -replace 'http://[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+:8080/api', 'http://!LOCAL_IP!:8080/api' | Set-Content '!ROOT_DIR!driving-school-miniapp\src\utils\config.js' -Encoding UTF8" >nul 2>&1
    powershell -Command "(Get-Content '!ROOT_DIR!driving-school-miniapp\src\utils\config.js') -replace 'http://127\.0\.0\.1:8080/api', 'http://!LOCAL_IP!:8080/api' | Set-Content '!ROOT_DIR!driving-school-miniapp\src\utils\config.js' -Encoding UTF8" >nul 2>&1
    powershell -Command "(Get-Content '!ROOT_DIR!driving-school-frontend\vite.config.js') -replace 'http://[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+:8080', 'http://!LOCAL_IP!:8080' | Set-Content '!ROOT_DIR!driving-school-frontend\vite.config.js' -Encoding UTF8" >nul 2>&1
    powershell -Command "(Get-Content '!ROOT_DIR!driving-school-frontend\vite.config.js') -replace 'http://127\.0\.0\.1:8080', 'http://!LOCAL_IP!:8080' | Set-Content '!ROOT_DIR!driving-school-frontend\vite.config.js' -Encoding UTF8" >nul 2>&1
    echo       Config updated
    echo [Build] Building miniapp...
    cd /d "!ROOT_DIR!driving-school-miniapp" && call npx uni build -p mp-weixin >nul 2>&1
    if errorlevel 1 (
        echo       [Error] Miniapp build failed!
    ) else (
        echo       Miniapp built
    )
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

:: Wait for services with retry (max 30s)
echo [Wait] Waiting for services...
set /a "RETRY=0"
:WAIT_LOOP
ping -n 4 127.0.0.1 >nul 2>&1
set /a "RETRY+=1"
set "BACKEND_OK="
set "FRONTEND_OK="
netstat -ano | findstr ":8080.*LISTEN" >nul 2>&1 && set "BACKEND_OK=1"
netstat -ano | findstr ":3000.*LISTEN" >nul 2>&1 && set "FRONTEND_OK=1"
if not defined BACKEND_OK if !RETRY! lss 10 goto WAIT_LOOP
if not defined FRONTEND_OK if !RETRY! lss 10 goto WAIT_LOOP

:: Check and save PIDs
if defined BACKEND_OK (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTEN"') do (
        if not defined BACKEND_PID (
            set "BACKEND_PID=%%a"
            echo %%a > "!LOG_DIR!\backend.pid"
            echo       Backend PID: %%a
        )
    )
) else (
    echo       [Warning] Backend not ready yet, check logs\backend.log
)
if defined FRONTEND_OK (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3000.*LISTEN"') do (
        if not defined FRONTEND_PID (
            set "FRONTEND_PID=%%a"
            echo %%a > "!LOG_DIR!\frontend.pid"
            echo       Frontend PID: %%a
        )
    )
) else (
    echo       [Warning] Frontend not ready yet, check logs\frontend.log
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
exit /b 0

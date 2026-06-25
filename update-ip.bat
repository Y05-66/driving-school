@echo off
setlocal enabledelayedexpansion

echo ========================================
echo   Auto Detect IP and Update Config
echo ========================================
echo.

:: Detect local IP
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
    for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /R "IPv4.*[0-9]*\.[0-9]*\.[0-9]*\.[0-9]*"') do (
        for /f "tokens=1" %%b in ("%%a") do (
            set "IP=%%b"
            if not "!IP:~0,4!"=="127." if not "!IP:~0,4!"=="169." if not defined LOCAL_IP set "LOCAL_IP=!IP!"
        )
    )
)

if not defined LOCAL_IP (
    echo [Error] No valid local IP found
    pause
    exit /b 1
)

echo Local IP: %LOCAL_IP%
echo.

:: Update miniapp config
echo [1/3] Update miniapp API...
powershell -Command "(Get-Content '%~dp0driving-school-miniapp\src\utils\config.js') -replace 'http://[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+:8080/api', 'http://%LOCAL_IP%:8080/api' | Set-Content '%~dp0driving-school-miniapp\src\utils\config.js' -Encoding UTF8"
powershell -Command "(Get-Content '%~dp0driving-school-miniapp\src\utils\config.js') -replace 'http://127\.0\.0\.1:8080/api', 'http://%LOCAL_IP%:8080/api' | Set-Content '%~dp0driving-school-miniapp\src\utils\config.js' -Encoding UTF8"
echo       http://%LOCAL_IP%:8080/api

:: Update frontend config
echo [2/3] Update frontend proxy...
powershell -Command "(Get-Content '%~dp0driving-school-frontend\vite.config.js') -replace 'http://[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+:8080', 'http://%LOCAL_IP%:8080' | Set-Content '%~dp0driving-school-frontend\vite.config.js' -Encoding UTF8"
powershell -Command "(Get-Content '%~dp0driving-school-frontend\vite.config.js') -replace 'http://127\.0\.0\.1:8080', 'http://%LOCAL_IP%:8080' | Set-Content '%~dp0driving-school-frontend\vite.config.js' -Encoding UTF8"
echo       http://%LOCAL_IP%:8080

:: Build miniapp
echo [3/3] Build miniapp...
cd /d "%~dp0driving-school-miniapp" && call npx uni build -p mp-weixin >nul 2>&1
echo       Done

echo.
echo ========================================
echo   Update Complete!
echo ========================================
echo.
echo   Miniapp API: http://%LOCAL_IP%:8080/api
echo   Frontend:    http://%LOCAL_IP%:8080
echo.
echo   Note: Restart frontend dev server to apply changes
echo.
pause

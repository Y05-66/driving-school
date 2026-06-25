@echo off
echo.
echo   Stopping services...
echo.

:: Kill processes on port 8080 (backend)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTEN"') do (
    taskkill /F /PID %%a >nul 2>&1 && echo   Backend stopped (PID: %%a)
)

:: Kill processes on port 3000 (frontend)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3000" ^| findstr "LISTEN"') do (
    taskkill /F /PID %%a >nul 2>&1 && echo   Frontend stopped (PID: %%a)
)

echo.
echo   Done!
echo.
timeout /t 2 /nobreak >nul

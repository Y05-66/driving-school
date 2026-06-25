@echo off
chcp 65001 >nul
echo.
echo ========================================
echo   驾校管理系统小程序 - 启动脚本
echo ========================================
echo.

:: 获取本机 IP
echo [1/3] 获取本机 IP...
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /r "IPv4.*[0-9]*\.[0-9]*\.[0-9]*\.[0-9]*"') do (
    for /f "tokens=1" %%b in ("%%a") do (
        set LOCAL_IP=%%b
        goto :found_ip
    )
)
:found_ip
echo       本机 IP: %LOCAL_IP%
echo.

:: 更新配置
echo [2/3] 更新配置文件...
node update-ip.js
echo.

:: 启动小程序
echo [3/3] 启动微信小程序开发...
echo.
npm run dev:mp-weixin

pause

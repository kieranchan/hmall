@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:menu
cls
echo ====================================
echo       Nginx Manager
echo ====================================
echo.
echo Please select an option:
echo 1 - Start Nginx
echo 2 - Stop Nginx
echo 3 - Restart Nginx
echo 4 - Reload Config
echo 5 - Check Status
echo 0 - Exit
echo.
set /p choice=Enter your choice (0-5): 

if "%choice%"=="1" goto start
if "%choice%"=="2" goto stop
if "%choice%"=="3" goto restart
if "%choice%"=="4" goto reload
if "%choice%"=="5" goto status
if "%choice%"=="0" goto end
goto invalid

:start
echo.
echo Starting Nginx...
cd /d "%~dp0"

REM Check if already running
tasklist | findstr /i "nginx.exe" >nul
if not errorlevel 1 (
    echo Nginx is already running!
    goto menu_pause
)

start nginx.exe
timeout /t 2 >nul
echo Nginx started successfully!
goto menu_pause

:stop
echo.
echo Stopping Nginx...
cd /d "%~dp0"

REM Check if running
tasklist | findstr /i "nginx.exe" >nul
if errorlevel 1 (
    echo Nginx is not running!
    goto menu_pause
)

nginx.exe -s stop
timeout /t 2 >nul
echo Nginx stopped!
goto menu_pause

:restart
echo.
echo Restarting Nginx...
cd /d "%~dp0"

REM Check if running
tasklist | findstr /i "nginx.exe" >nul
if errorlevel 1 (
    echo Nginx is not running, starting it...
    start nginx.exe
    timeout /t 2 >nul
    echo Nginx started!
) else (
    nginx.exe -s stop
    timeout /t 2 >nul
    start nginx.exe
    timeout /t 2 >nul
    echo Nginx restarted!
)
goto menu_pause

:reload
echo.
echo Reloading configuration...
cd /d "%~dp0"

REM Check if running
tasklist | findstr /i "nginx.exe" >nul
if errorlevel 1 (
    echo Nginx is not running! Please start it first.
    goto menu_pause
)

nginx.exe -s reload
if errorlevel 1 (
    echo Failed to reload configuration!
) else (
    echo Configuration reloaded!
)
goto menu_pause

:status
echo.
echo Checking Nginx process...
tasklist | findstr /i "nginx.exe"
if errorlevel 1 (
    echo.
    echo [STATUS] Nginx is NOT running
) else (
    echo.
    echo [STATUS] Nginx is running
)
goto menu_pause

:invalid
echo.
echo Invalid option! Please try again.
timeout /t 2 >nul
goto menu

:menu_pause
echo.
echo Press any key to return to menu...
pause >nul
goto menu

:end
echo.
echo Goodbye!
timeout /t 1 >nul
exit
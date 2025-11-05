@echo off
setlocal enabledelayedexpansion
echo ========================================
echo Database Setup Script
echo ========================================
echo.

REM Find MySQL installation
set "MYSQL_PATH="
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
    set "MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    goto :found
)
if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" (
    set "MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
    goto :found
)
if exist "C:\xampp\mysql\bin\mysql.exe" (
    set "MYSQL_PATH=C:\xampp\mysql\bin\mysql.exe"
    goto :found
)
if exist "C:\wamp64\bin\mysql\mysql8.0.36\bin\mysql.exe" (
    set "MYSQL_PATH=C:\wamp64\bin\mysql\mysql8.0.36\bin\mysql.exe"
    goto :found
)

:found
if "!MYSQL_PATH!"=="" (
    echo ERROR: MySQL not found!
    echo Please install MySQL or update the paths in this script.
    pause
    exit /b 1
)

echo Found MySQL
echo.

REM Get MySQL password
set /p MYSQL_PASSWORD="Enter MySQL root password (default: 123456): "
if "%MYSQL_PASSWORD%"=="" set MYSQL_PASSWORD=123456

echo.
echo Importing database from dump file...
"!MYSQL_PATH!" -u root -p%MYSQL_PASSWORD% < Dump20251102.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo SUCCESS! Database setup complete.
    echo ========================================
    echo.
    echo You can now:
    echo 1. Start the server: cd server ^&^& mvn exec:java
    echo 2. Start the client: cd client ^&^& mvn javafx:run
    echo.
) else (
    echo.
    echo ========================================
    echo ERROR: Database setup failed!
    echo ========================================
    echo.
    echo Possible issues:
    echo 1. Wrong MySQL password
    echo 2. MySQL service not running
    echo 3. Permission denied
    echo.
)

pause

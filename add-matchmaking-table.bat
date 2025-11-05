@echo off
echo ========================================
echo   Adding matchmaking_queue table
echo ========================================
echo.

REM Try common MySQL installation paths
set MYSQL_PATH=

if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
    set MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe
) else if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" (
    set MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe
) else if exist "C:\xampp\mysql\bin\mysql.exe" (
    set MYSQL_PATH=C:\xampp\mysql\bin\mysql.exe
) else (
    echo MySQL not found in common paths!
    echo Please run this SQL manually:
    echo.
    type db\add-matchmaking-queue.sql
    pause
    exit /b 1
)

echo Using MySQL at: %MYSQL_PATH%
echo.

"%MYSQL_PATH%" -u root -p123456 < db\add-matchmaking-queue.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   Table created successfully!
    echo ========================================
) else (
    echo.
    echo ========================================
    echo   Error creating table!
    echo ========================================
)

pause

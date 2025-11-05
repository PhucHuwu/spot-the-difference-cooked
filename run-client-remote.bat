@echo off
chcp 65001 >nul
echo ========================================
echo    SPOT THE DIFFERENCE - REMOTE CLIENT
echo ========================================
echo.

REM Kiểm tra file config
if not exist "client\src\main\resources\client-config.properties" (
    echo ❌ Không tìm thấy file client-config.properties
    echo Vui lòng chạy script này từ thư mục gốc của project
    pause
    exit /b 1
)

echo 📋 Đọc cấu hình hiện tại...
type client\src\main\resources\client-config.properties
echo.
echo ========================================
echo.

set /p CONFIRM="Bạn đã sửa server.host trong client-config.properties chưa? (y/n): "
if /i not "%CONFIRM%"=="y" (
    echo.
    echo ℹ️  Hãy sửa file: client\src\main\resources\client-config.properties
    echo    Thay server.host=127.0.0.1 thành IP của máy server
    echo    Ví dụ: server.host=192.168.1.100
    echo.
    pause
    exit /b 0
)

echo.
echo 🔨 Compile client...
call mvn -pl client compile -q

if %ERRORLEVEL% neq 0 (
    echo ❌ Lỗi compile! Kiểm tra lại code.
    pause
    exit /b 1
)

echo ✅ Compile thành công!
echo.
echo 🚀 Khởi động client...
echo.

call mvn -pl client javafx:run

pause

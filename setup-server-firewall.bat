@echo off
chcp 65001 >nul
echo ========================================
echo    CẤU HÌNH FIREWALL CHO SERVER
echo ========================================
echo.

REM Kiểm tra quyền Admin
net session >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ❌ Script này cần chạy với quyền Administrator!
    echo.
    echo Cách chạy:
    echo 1. Click phải vào file này
    echo 2. Chọn "Run as administrator"
    echo.
    pause
    exit /b 1
)

echo ℹ️  Đang tạo rule cho Firewall...
echo.

REM Xóa rule cũ nếu có
netsh advfirewall firewall delete rule name="Game Server Port 5050" >nul 2>&1

REM Tạo rule mới
netsh advfirewall firewall add rule name="Game Server Port 5050" dir=in action=allow protocol=TCP localport=5050

if %ERRORLEVEL% equ 0 (
    echo ✅ Đã mở Firewall cho port 5050 thành công!
    echo.
    echo 📋 Địa chỉ IP của máy này:
    echo.
    ipconfig | findstr /C:"IPv4"
    echo.
    echo 💡 Client ở máy khác sẽ dùng IP này để kết nối
) else (
    echo ❌ Lỗi khi cấu hình Firewall!
)

echo.
echo ========================================
pause

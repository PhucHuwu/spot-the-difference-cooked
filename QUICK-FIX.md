# 🚀 Quick Fix - Lỗi Đăng Nhập

## TL;DR - Làm Ngay 3 Bước Này:

### 1️⃣ Setup Database (1 phút)
```batch
setup-database.bat
```
Nhập password MySQL (mặc định: `123456`)

### 2️⃣ Start Server (30 giây)
```bash
cd server
mvn compile exec:java
```

Đợi thấy:
```
[INFO] Server successfully started and listening on port 5050
```

### 3️⃣ Start Client & Login (30 giây)
```bash
cd client
mvn javafx:run
```

Login với:
- Username: `player1` (bất kỳ)
- Password: `123456` (bất kỳ)

**DONE!** ✅

---

## ❌ Nếu Vẫn Lỗi:

### Lỗi: "Table doesn't exist"
```bash
# Xóa database cũ và tạo lại
mysql -u root -p
DROP DATABASE spotgame;
exit

# Chạy lại
setup-database.bat
```

### Lỗi: "Access denied"
Sửa file `server/src/main/resources/server-config.properties`:
```properties
db.password=YOUR_MYSQL_PASSWORD_HERE
```

### Lỗi: "Connection refused"
- Kiểm tra MySQL đã chạy chưa
- Windows: `services.msc` → MySQL → Start
- XAMPP: Mở XAMPP → Start MySQL

---

## 📝 Chi Tiết Đầy Đủ
Xem file `FIX-LOGIN-ISSUE.md` để biết chi tiết.

## 🔧 Thay Đổi Kỹ Thuật
Xem file `CHANGES-SUMMARY.md` để biết các thay đổi code.


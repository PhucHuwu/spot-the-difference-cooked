# Fix Lỗi Đăng Nhập - Hướng Dẫn Chi Tiết

## Vấn Đề
Bạn không thể đăng nhập vào game vì:
1. Database chưa được setup đúng cách
2. Thiếu bảng `matchmaking_queue`
3. Server không log lỗi rõ ràng

## Giải Pháp Đã Thực Hiện

### 1. Đã Thêm Logger System
- Tạo class `Logger.java` để thay thế `System.out.println` và `System.err.println`
- Giờ đây server sẽ log chi tiết mọi lỗi xảy ra
- Dễ dàng debug khi có vấn đề

### 2. Đã Thêm Bảng `matchmaking_queue`
- Cập nhật `db/schema.sql` với bảng mới
- Bảng này cần thiết cho chức năng matchmaking

### 3. Đã Fix Các Lỗi SQL
- Sửa `join_time` thành `joined_at` trong QueueService
- Đảm bảo tất cả queries đều đúng với schema

## Cách Sử Dụng

### Bước 1: Setup Database

**Cách 1: Sử dụng script tự động (Khuyến nghị)**
```batch
setup-database.bat
```
Script sẽ tự động:
- Tìm MySQL trên máy bạn
- Tạo database `spotgame`
- Tạo tất cả các bảng cần thiết

**Cách 2: Thủ công**
```bash
# Mở MySQL command line hoặc MySQL Workbench
mysql -u root -p

# Chạy script
source db/schema.sql
# hoặc
\. db/schema.sql
```

### Bước 2: Kiểm Tra Database
```sql
USE spotgame;
SHOW TABLES;
```

Bạn phải thấy các bảng sau:
- `users`
- `matches`
- `image_sets`
- `image_differences`
- `matchmaking_queue` ← **Bảng mới quan trọng!**

### Bước 3: Kiểm Tra Config
Mở `server/src/main/resources/server-config.properties`:
```properties
server.port=5050
db.url=jdbc:mysql://localhost:3306/spotgame
db.user=root
db.password=123456  ← Đảm bảo đúng mật khẩu MySQL của bạn
turn.seconds=15
content.dir=admin/content/imagesets
```

### Bước 4: Khởi Động Server
```bash
cd server
mvn compile exec:java
```

Server sẽ hiển thị log chi tiết:
```
[2025-11-05 11:49:00] [INFO] Server starting on port 5050
[2025-11-05 11:49:00] [INFO] Database URL: jdbc:mysql://localhost:3306/spotgame
[2025-11-05 11:49:00] [INFO] Content directory: admin/content/imagesets
[2025-11-05 11:49:00] [INFO] Server successfully started and listening on port 5050
```

**Nếu thấy lỗi database:**
```
[ERROR] SQLException: Unknown database 'spotgame'
→ Chạy lại setup-database.bat

[ERROR] Access denied for user 'root'@'localhost'
→ Sửa mật khẩu trong server-config.properties

[ERROR] Table 'spotgame.matchmaking_queue' doesn't exist
→ Database cũ, cần chạy lại schema.sql
```

### Bước 5: Khởi Động Client
```bash
cd client
mvn javafx:run
```

### Bước 6: Đăng Nhập
1. Nhập username bất kỳ (ví dụ: `player1`)
2. Nhập password bất kỳ (ví dụ: `123456`)
3. Nhấn Login

**Lần đầu đăng nhập:**
- Hệ thống tự động tạo tài khoản mới
- Bạn sẽ vào lobby ngay lập tức

**Lần sau:**
- Nhập đúng username và password đã tạo
- Nếu sai password sẽ báo "Sai mật khẩu"

## Kiểm Tra Lỗi

### Nếu vẫn không đăng nhập được:

1. **Kiểm tra server có chạy không:**
   - Xem console có log "Server successfully started" không
   - Thử telnet: `telnet localhost 5050`

2. **Kiểm tra database:**
   ```sql
   USE spotgame;
   SELECT * FROM users;  -- Xem có user nào không
   ```

3. **Kiểm tra log server:**
   - Server giờ log rất chi tiết
   - Tìm dòng `[ERROR]` để biết lỗi gì

4. **Kiểm tra client config:**
   `client/src/main/resources/client-config.properties`
   ```properties
   server.host=localhost
   server.port=5050  ← Phải khớp với server
   ```

## Các Thay Đổi Kỹ Thuật

### Files Đã Sửa:
1. `db/schema.sql` - Thêm bảng matchmaking_queue
2. `server/src/main/java/com/ltm/game/server/Logger.java` - **NEW** Logger system
3. `server/src/main/java/com/ltm/game/server/ServerProperties.java` - Dùng Logger
4. `server/src/main/java/com/ltm/game/server/GameServer.java` - Dùng Logger + chi tiết hơn
5. `server/src/main/java/com/ltm/game/server/ClientHandler.java` - Log exceptions
6. `server/src/main/java/com/ltm/game/server/QueueService.java` - Fix SQL + Logger
7. `setup-database.bat` - **NEW** Script setup tự động

### Tuân Thủ Workspace Rules:
- ✅ Không dùng `System.out.println`
- ✅ Không dùng `System.err.println`
- ✅ Không dùng `printStackTrace()`
- ✅ Dùng proper logging
- ✅ Code style đúng chuẩn

## Troubleshooting

### Lỗi: "Communications link failure"
```
Nguyên nhân: MySQL chưa chạy
Giải pháp: Khởi động MySQL service
- Windows: services.msc → tìm MySQL → Start
- XAMPP: Mở XAMPP Control Panel → Start MySQL
```

### Lỗi: "Unknown database 'spotgame'"
```
Nguyên nhân: Database chưa được tạo
Giải pháp: Chạy setup-database.bat
```

### Lỗi: "Table doesn't exist"
```
Nguyên nhân: Schema chưa được apply
Giải pháp: 
1. Drop database: DROP DATABASE spotgame;
2. Chạy lại: setup-database.bat
```

### Lỗi: "Access denied"
```
Nguyên nhân: Sai username/password MySQL
Giải pháp: Sửa server-config.properties với đúng thông tin
```

## Kết Luận

Sau khi làm theo các bước trên, bạn sẽ:
- ✅ Database được setup đầy đủ
- ✅ Server chạy ổn định với logging chi tiết
- ✅ Có thể đăng nhập và chơi game
- ✅ Dễ dàng debug nếu có lỗi

Nếu vẫn gặp vấn đề, hãy check log của server - giờ nó sẽ cho biết chính xác lỗi gì!


# 📝 TÀI LIỆU CẤU HÌNH - SPOT THE DIFFERENCE GAME

## 📂 Các File Cấu Hình

### 1. Server Configuration
**File:** `server/src/main/resources/server-config.properties`

```properties
server.host=0.0.0.0          # Lắng nghe trên tất cả network interfaces
server.port=5050             # Port của server
db.url=jdbc:mysql://localhost:3306/spotgame  # Database URL
db.user=root                 # MySQL username
db.password=123456           # MySQL password
turn.seconds=15              # Thời gian mỗi lượt chơi (giây)
content.dir=admin/content/imagesets  # Thư mục chứa ảnh game
```

**Lưu ý:**
- `server.host=0.0.0.0` cho phép server nhận kết nối từ tất cả IP (local và remote)
- Database phải chạy trên cùng máy với server
- Sau khi sửa, cần rebuild server: `mvn -pl server package`

---

### 2. Client Configuration (Local - Cùng máy với Server)
**File:** `client/src/main/resources/client-config.properties`

```properties
server.host=127.0.0.1        # Localhost
server.port=5050             # Port của server
```

**Dùng khi:**
- Chạy client và server trên cùng 1 máy
- Test local

**File mẫu:** `client-config.properties.local`

---

### 3. Client Configuration (Remote - Máy khác)
**File:** `client/src/main/resources/client-config.properties`

```properties
server.host=192.168.30.118   # IP của máy server
server.port=5050             # Port của server
```

**Dùng khi:**
- Chạy client ở máy khác, kết nối đến server từ xa
- **Thay `192.168.30.118` bằng IP thực tế của máy server**

**File mẫu:** `client-config.properties.remote`

**Sau khi sửa:**
```powershell
mvn -pl client compile
mvn -pl client javafx:run
```

---

### 4. Admin Configuration
**File:** `admin/src/main/resources/admin-config.properties`

```properties
db.url=jdbc:mysql://localhost:3306/spotgame  # Database URL
db.user=root                                  # MySQL username
db.password=123456                            # MySQL password
storage.dir=admin/content/imagesets           # Thư mục lưu ảnh
```

**Lưu ý:**
- Admin tool phải chạy trên cùng máy với database
- Thư mục `admin/content/imagesets` sẽ tự động được tạo

---

## 🔧 CẤU HÌNH THEO TÌNH HUỐNG

### Tình huống 1: Tất cả chạy trên 1 máy (Development)

**Server:** `server-config.properties`
```properties
server.host=0.0.0.0
server.port=5050
db.url=jdbc:mysql://localhost:3306/spotgame
db.user=root
db.password=123456
```

**Client:** `client-config.properties`
```properties
server.host=127.0.0.1
server.port=5050
```

---

### Tình huống 2: Server ở máy A, Client ở máy B

**Server (Máy A):** `server-config.properties`
```properties
server.host=0.0.0.0          # QUAN TRỌNG: Phải là 0.0.0.0
server.port=5050
db.url=jdbc:mysql://localhost:3306/spotgame
db.user=root
db.password=123456
```

**Client (Máy B):** `client-config.properties`
```properties
server.host=192.168.30.118   # IP của máy A (kiểm tra bằng ipconfig)
server.port=5050
```

**Các bước:**
1. Trên máy A: Chạy `setup-server-firewall.bat` (quyền Admin) để mở port
2. Trên máy A: Lấy IP bằng `ipconfig`
3. Trên máy B: Sửa `server.host` trong client-config.properties
4. Trên máy B: Compile và chạy client

---

### Tình huống 3: Nhiều Client (Máy A, B, C, D...)

**Server (1 máy):** Cấu hình như tình huống 2

**Mỗi Client:** Đều cấu hình như sau
```properties
server.host=192.168.30.118   # IP của máy server
server.port=5050
```

**Lưu ý:**
- Tất cả client phải trong cùng mạng LAN với server
- Mỗi client cần copy toàn bộ project về máy mình
- Sau khi sửa config, phải compile lại

---

## 🚀 SCRIPTS TIỆN ÍCH

### Trên máy SERVER:

1. **Setup Firewall** (chỉ chạy 1 lần, cần quyền Admin):
   ```powershell
   .\setup-server-firewall.bat
   ```

2. **Chạy Server:**
   ```powershell
   .\run-server.bat
   ```

### Trên máy CLIENT:

1. **Client Local** (cùng máy với server):
   ```powershell
   .\run-client1.bat
   ```

2. **Client Remote** (máy khác):
   - Sửa `client-config.properties` trước
   - Chạy:
   ```powershell
   .\run-client-remote.bat
   ```

---

## 🔍 KIỂM TRA CẤU HÌNH

### Kiểm tra IP của server:
```powershell
ipconfig
```
Tìm dòng "IPv4 Address" trong adapter đang dùng (Wi-Fi hoặc Ethernet)

### Test kết nối từ client đến server:
```powershell
Test-NetConnection -ComputerName 192.168.30.118 -Port 5050
```
Kết quả phải là `TcpTestSucceeded : True`

### Kiểm tra server đang lắng nghe port 5050:
```powershell
netstat -an | findstr :5050
```
Phải thấy: `0.0.0.0:5050` hoặc `[::]:5050`

---

## ⚠️ LƯU Ý QUAN TRỌNG

1. **Sau khi sửa server-config.properties:**
   ```powershell
   mvn -pl server clean package
   java -jar server/target/server-0.1.0-SNAPSHOT.jar
   ```

2. **Sau khi sửa client-config.properties:**
   ```powershell
   mvn -pl client compile
   mvn -pl client javafx:run
   ```

3. **IP động vs IP tĩnh:**
   - IP của server có thể thay đổi sau mỗi lần khởi động máy
   - Nên cấu hình IP tĩnh cho máy server để tránh phải đổi config liên tục

4. **Firewall:**
   - Windows Firewall phải cho phép port 5050
   - Antivirus có thể chặn kết nối → cần thêm exception

5. **Network:**
   - Tất cả máy phải trong cùng mạng LAN
   - Không hoạt động qua Internet (cần thêm port forwarding trên router)

---

## 📋 CHECKLIST DEPLOY

### Setup Server (Máy A):
- [ ] Cài MySQL, tạo database `spotgame`
- [ ] Import file `Dump20251102.sql`
- [ ] Sửa `server-config.properties` (server.host=0.0.0.0)
- [ ] Rebuild server: `mvn -pl server clean package`
- [ ] Chạy `setup-server-firewall.bat` (Admin)
- [ ] Lấy IP: `ipconfig`
- [ ] Khởi động server: `.\run-server.bat`

### Setup Client (Máy B, C, D...):
- [ ] Copy project về máy
- [ ] Cài JDK 17+ và Maven
- [ ] Sửa `client-config.properties` (server.host=<IP_CUA_MAY_A>)
- [ ] Compile: `mvn -pl client compile`
- [ ] Test kết nối: `Test-NetConnection -ComputerName <IP_SERVER> -Port 5050`
- [ ] Chạy client: `.\run-client-remote.bat`

---

**🎮 Happy Gaming! 🎮**

# Hướng Dẫn Deploy Client Trên Máy Khác

## Bước 1: Trên Máy SERVER

### 1.1. Tìm địa chỉ IP của máy server
```powershell
ipconfig
```
Tìm dòng **"IPv4 Address"**, ví dụ: `192.168.1.100`

### 1.2. Mở Firewall cho port 5050
Chạy PowerShell **với quyền Administrator**:
```powershell
New-NetFirewallRule -DisplayName "Game Server Port 5050" -Direction Inbound -LocalPort 5050 -Protocol TCP -Action Allow
```

### 1.3. Khởi động Server
```powershell
cd d:\spot-the-difference-cooked
java -jar server/target/server-0.1.0-SNAPSHOT.jar
```

---

## Bước 2: Trên Máy CLIENT (máy khác)

### 2.1. Copy toàn bộ project sang máy client
Sao chép thư mục `spot-the-difference-cooked` sang máy client

### 2.2. Sửa file cấu hình
Mở file: `client/src/main/resources/client-config.properties`

Sửa `server.host` thành IP của máy server:
```properties
server.host=192.168.1.100
server.port=5050
```
(Thay `192.168.1.100` bằng IP thực tế của máy server)

### 2.3. Compile client
```powershell
cd d:\spot-the-difference-cooked
mvn -pl client compile
```

### 2.4. Chạy client
```powershell
mvn -pl client javafx:run
```

Hoặc dùng script có sẵn:
```powershell
.\run-client-remote.bat
```

---

## Kiểm Tra Kết Nối

### Trên máy client, test kết nối đến server:
```powershell
Test-NetConnection -ComputerName <IP_SERVER> -Port 5050
```

Nếu kết quả là `TcpTestSucceeded : True` → OK ✅

---

## Troubleshooting

### ❌ Không kết nối được server

1. **Kiểm tra Firewall trên máy server:**
   ```powershell
   Get-NetFirewallRule -DisplayName "Game Server Port 5050"
   ```

2. **Kiểm tra server đang chạy:**
   ```powershell
   netstat -an | findstr :5050
   ```

3. **Kiểm tra ping:**
   ```powershell
   ping <IP_SERVER>
   ```

4. **Tắt Firewall tạm thời để test:**
   ```powershell
   # Tắt (chạy với quyền Admin)
   Set-NetFirewallProfile -Profile Domain,Public,Private -Enabled False
   
   # Bật lại sau khi test
   Set-NetFirewallProfile -Profile Domain,Public,Private -Enabled True
   ```

### ❌ Lỗi "Cannot connect to server"

- Kiểm tra lại IP trong `client-config.properties`
- Đảm bảo cả 2 máy trong cùng mạng
- Compile lại client sau khi sửa config: `mvn -pl client compile`

---

## Lưu Ý Quan Trọng

- ✅ Cả 2 máy phải trong **cùng mạng LAN** hoặc cùng WiFi
- ✅ IP của server có thể thay đổi sau mỗi lần khởi động → Cần kiểm tra lại bằng `ipconfig`
- ✅ Mỗi lần sửa `client-config.properties` phải **compile lại** client
- ✅ Nếu muốn nhiều client cùng lúc, chạy nhiều terminal với lệnh `mvn -pl client javafx:run`

---

## IP Tĩnh (Tùy chọn - Khuyến nghị)

Để tránh IP server thay đổi, nên cấu hình IP tĩnh cho máy server:

1. Mở **Control Panel** → **Network and Sharing Center**
2. Click vào connection đang dùng → **Properties**
3. Chọn **Internet Protocol Version 4 (TCP/IPv4)** → **Properties**
4. Chọn **"Use the following IP address"**
5. Nhập IP tĩnh (ví dụ: `192.168.1.100`)
6. Subnet mask: `255.255.255.0`
7. Default gateway: (IP của router, ví dụ: `192.168.1.1`)

Sau đó client không cần đổi config nữa! 🎯

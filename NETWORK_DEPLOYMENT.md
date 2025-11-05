# 🌐 Kết Nối Client Qua Internet (Khác Mạng LAN)

## ⚠️ Lưu Ý Quan Trọng

Với cấu hình hiện tại, **client chỉ kết nối được trong cùng mạng LAN** (cùng WiFi/Router).

Nếu client ở **mạng khác** (WiFi khác, 4G, nhà khác...), cần cấu hình thêm để cho phép kết nối qua Internet.

---

## 🔧 Giải Pháp 1: Port Forwarding (Khuyến nghị)

### Bước 1: Lấy địa chỉ IP Public của Server

Trên máy server, truy cập: https://whatismyipaddress.com/

Hoặc chạy:
```powershell
(Invoke-WebRequest -Uri "https://api.ipify.org").Content
```

Ví dụ IP Public: `203.162.4.191`

### Bước 2: Cấu hình Port Forwarding trên Router

**Truy cập Router Admin:**
1. Mở trình duyệt, truy cập địa chỉ gateway (thường là `192.168.1.1` hoặc `192.168.30.1`)
2. Đăng nhập với tài khoản admin (xem mặt sau router)

**Cấu hình Port Forwarding:**
1. Tìm mục **Port Forwarding** hoặc **Virtual Server** trong cài đặt
2. Thêm rule mới:
   - **Service Name:** Game Server
   - **External Port:** 5050
   - **Internal IP:** 192.168.30.118 (IP của máy server trong LAN)
   - **Internal Port:** 5050
   - **Protocol:** TCP
3. Lưu cấu hình và restart router (nếu cần)

### Bước 3: Cấu hình Server

File: `server/src/main/resources/server-config.properties`
```properties
server.host=0.0.0.0  # Đã OK rồi
server.port=5050
```

### Bước 4: Client ở mạng khác kết nối

File: `client/src/main/resources/client-config.properties`
```properties
server.host=203.162.4.191  # IP Public của server (thay bằng IP thực tế)
server.port=5050
```

Sau đó compile và chạy:
```powershell
mvn -pl client compile
mvn -pl client javafx:run
```

### ⚠️ Rủi Ro Bảo Mật:

- ⚠️ Server sẽ public ra Internet → dễ bị tấn công
- ⚠️ Không có mã hóa (plaintext password)
- ⚠️ Không có rate limiting

**Giải pháp bảo mật:**
- Thêm authentication token
- Dùng SSL/TLS để mã hóa
- Implement rate limiting
- Whitelist IP nếu biết trước client IP

---

## 🔧 Giải Pháp 2: VPN (An toàn hơn)

Tạo VPN để "ảo hóa" thành cùng mạng LAN:

### Option A: Sử dụng Hamachi (Free, dễ setup)

**Trên Server:**
1. Tải Hamachi: https://vpn.net
2. Tạo network mới (ví dụ: `SpotTheGameServer`)
3. Lấy IPv4 address của Hamachi (ví dụ: `25.12.34.56`)

**Trên Client:**
1. Tải Hamachi
2. Join network `SpotTheGameServer`
3. Sửa `client-config.properties`:
   ```properties
   server.host=25.12.34.56  # IP Hamachi của server
   server.port=5050
   ```

**Ưu điểm:**
- ✅ Bảo mật hơn (mã hóa end-to-end)
- ✅ Dễ setup, không cần config router
- ✅ Client "ảo hóa" trong cùng LAN

**Nhược điểm:**
- ❌ Free plan giới hạn 5 người
- ❌ Tốc độ phụ thuộc vào relay server

### Option B: Sử dụng ZeroTier (Free, unlimited users)

**Trên Server:**
1. Truy cập: https://my.zerotier.com
2. Tạo network mới
3. Copy Network ID (ví dụ: `8056c2e21c000001`)
4. Cài ZeroTier client: https://www.zerotier.com/download/
5. Join network: `zerotier-cli join 8056c2e21c000001`
6. Lấy IP ZeroTier (ví dụ: `172.22.x.x`)

**Trên Client:**
1. Cài ZeroTier client
2. Join network với Network ID trên
3. Sửa `client-config.properties`:
   ```properties
   server.host=172.22.x.x  # IP ZeroTier của server
   server.port=5050
   ```

**Ưu điểm:**
- ✅ Free, unlimited users
- ✅ Bảo mật cao (peer-to-peer encrypted)
- ✅ Tốc độ nhanh (direct connection nếu được)

---

## 🔧 Giải Pháp 3: Ngrok (Dùng cho demo/test)

Dùng Ngrok để tạo tunnel public cho server local:

**Trên Server:**
1. Tải Ngrok: https://ngrok.com/download
2. Chạy:
   ```powershell
   ngrok tcp 5050
   ```
3. Copy địa chỉ forwarding (ví dụ: `0.tcp.ngrok.io:12345`)

**Trên Client:**
```properties
server.host=0.tcp.ngrok.io
server.port=12345
```

**Ưu điểm:**
- ✅ Setup cực nhanh (1 phút)
- ✅ Không cần config router
- ✅ Phù hợp demo/test

**Nhược điểm:**
- ❌ Free plan giới hạn 40 connections/phút
- ❌ URL thay đổi mỗi lần restart
- ❌ Tốc độ không ổn định

---

## 📊 So Sánh Các Giải Pháp

| Giải pháp | Bảo mật | Tốc độ | Độ phức tạp | Chi phí | Phù hợp |
|-----------|---------|---------|-------------|---------|---------|
| **Port Forwarding** | ⚠️ Trung bình | ⭐⭐⭐⭐⭐ Rất nhanh | 🔧 Trung bình | 💰 Free | Production |
| **Hamachi** | ✅ Cao | ⭐⭐⭐ Tốt | 🔧 Dễ | 💰 Free (≤5 users) | Nhóm nhỏ |
| **ZeroTier** | ✅ Rất cao | ⭐⭐⭐⭐ Rất tốt | 🔧 Dễ | 💰 Free | Production nhỏ |
| **Ngrok** | ⚠️ Trung bình | ⭐⭐ Trung bình | 🔧 Rất dễ | 💰 Free (giới hạn) | Demo/Test |

---

## 🎯 Khuyến Nghị Theo Tình Huống

### 1. Game với bạn bè (2-10 người):
→ **ZeroTier** (miễn phí, dễ dùng, bảo mật)

### 2. Game với nhiều người (>10):
→ **Port Forwarding** + thêm bảo mật (SSL, whitelist IP)

### 3. Demo/Test nhanh:
→ **Ngrok** (setup trong 1 phút)

### 4. Production thực sự:
→ **Deploy lên Cloud Server** (AWS, Azure, DigitalOcean)
   - Server có IP Public cố định
   - Bandwidth lớn, ổn định
   - Có thể scale

---

## 🚀 Hướng Dẫn Setup ZeroTier (Khuyến nghị)

### Server Setup:

```powershell
# Bước 1: Tải ZeroTier
# https://www.zerotier.com/download/

# Bước 2: Cài đặt và join network
zerotier-cli join YOUR_NETWORK_ID

# Bước 3: Kiểm tra IP
zerotier-cli listnetworks
# Lấy "Managed IPs" (ví dụ: 172.22.134.56)

# Bước 4: Mở Firewall cho ZeroTier
New-NetFirewallRule -DisplayName "ZeroTier Game Server" -Direction Inbound -LocalPort 5050 -Protocol TCP -Action Allow -RemoteAddress 172.22.0.0/16
```

### Client Setup:

```powershell
# Bước 1: Cài ZeroTier
# https://www.zerotier.com/download/

# Bước 2: Join cùng network
zerotier-cli join YOUR_NETWORK_ID

# Bước 3: Sửa config
# File: client/src/main/resources/client-config.properties
# server.host=172.22.134.56  (IP ZeroTier của server)
# server.port=5050

# Bước 4: Compile và chạy
mvn -pl client compile
mvn -pl client javafx:run
```

---

## ⚡ Test Kết Nối

### Từ client (sau khi setup VPN/Port Forwarding):

```powershell
# Test ping
ping <SERVER_IP>

# Test port
Test-NetConnection -ComputerName <SERVER_IP> -Port 5050
```

Nếu `TcpTestSucceeded : True` → Kết nối OK! ✅

---

**💡 Lưu ý:** Với production thực sự, nên deploy server lên Cloud (AWS EC2, Azure VM, DigitalOcean Droplet) để có IP Public cố định và bandwidth tốt hơn.

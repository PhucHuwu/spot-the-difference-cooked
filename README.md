# 🎮 Spot The Difference - Quick Start Guide

## 📋 Yêu cầu hệ thống

- Java 17+
- Maven 3.6+
- Docker & Docker Compose (cho database)

## 🚀 Chạy nhanh với Makefile

### Xem tất cả commands

```bash
make help
```

### Chạy toàn bộ ứng dụng (Database + Server + Client)

```bash
make all
# hoặc
make run
```

### Chạy từng thành phần riêng lẻ

#### 1. Start Database

```bash
make db
```

#### 2. Run Server

```bash
make server
```

#### 3. Run Client

```bash
make client
```

#### 4. Run Admin Tool

```bash
make admin
```

### Development Mode (DB + Server chạy với Maven)

```bash
make dev
```

### Dừng tất cả services

```bash
make stop
```

### Các commands hữu ích khác

**Build project:**

```bash
make build
```

**Clean build artifacts:**

```bash
make clean
```

**Kiểm tra trạng thái services:**

```bash
make status
```

**Xem database logs:**

```bash
make logs
```

**Reset database:**

```bash
make db-reset
```

**Package applications:**

```bash
make package
```

## 📂 Cấu trúc Project sau Refactoring

```
game/
├── Makefile                    # Build & run automation
├── client/                     # JavaFX Game Client
│   └── src/main/java/com/ltm/game/client/
│       ├── ClientApp.java      # Main application
│       ├── controllers/        # FXML Controllers
│       ├── views/             # Game views
│       ├── models/            # Data models
│       └── services/          # Business services
├── server/                    # Game Server
│   └── src/main/java/com/example/server/
├── shared/                    # Shared models & protocol
│   └── src/main/java/com/ltm/game/shared/
├── admin/                     # Admin tool for uploading images
└── docker-compose.yaml        # Database setup
```

## 🎯 Workflow thông thường

### Lần đầu setup:

```bash
# 1. Build tất cả
make build

# 2. Start database
make db

# 3. Chạy server (terminal 1)
make server

# 4. Chạy client (terminal 2)
make client
```

### Development:

```bash
# Chạy tất cả một lần
make all
```

### Kết thúc:

```bash
# Dừng tất cả
make stop
```

## 🔧 Cấu hình

### Database

- Host: localhost
- Port: 3306
- Database: spotgame
- Username: root
- Password: root

### Server

- Port: 5050

## 🐛 Troubleshooting

### Database không start được

```bash
# Kiểm tra Docker đang chạy
docker ps

# Reset database
make db-reset
```

### Server không kết nối được database

```bash
# Kiểm tra database đã sẵn sàng chưa
make logs

# Đợi cho healthcheck pass
```

### Client không kết nối được server

```bash
# Kiểm tra server đang chạy
make status

# Xem log server
ps aux | grep server
```

## 📝 Notes

- Makefile tự động build trước khi chạy các services
- Database data được persist trong Docker volume
- Server tạo fat JAR với tất cả dependencies
- Client chạy qua Maven JavaFX plugin

## 🎨 Package Name Convention

Toàn bộ project đã được refactor sang package name mới:

- **Old**: `com.example.*`
- **New**: `com.ltm.game.*`

---

**Happy Gaming! 🎮**
 
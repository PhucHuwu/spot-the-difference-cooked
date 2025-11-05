# 🎮 Hệ Thống Matchmaking - Tương Tự Liên Minh Huyền Thoại

## 📋 Tổng Quan

Hệ thống matchmaking đã được triển khai đầy đủ với luồng hoạt động tương tự như trong Liên Minh Huyền Thoại (League of Legends), bao gồm:

1. ✅ Tìm trận (Queue System)
2. ✅ Ghép đối thủ tự động (Auto Matchmaking)
3. ✅ Popup "Match Found" với countdown
4. ✅ Accept/Decline mechanism
5. ✅ Xử lý timeout và disconnect
6. ✅ Hiệu ứng countdown 3-2-1-GO trước khi vào game

---

## 🔄 Luồng Hoạt Động Chi Tiết

### 1. **Người Chơi Nhấn "Tìm Trận"**

**Client Side:**
- File: `client/src/main/java/com/ltm/game/client/controllers/LobbyController.java`
- Method: `handleFindMatch()`
- Action: Gửi message `Protocol.QUEUE_JOIN` lên server

```java
@FXML
private void handleFindMatch() {
    networkClient.send(new Message(Protocol.QUEUE_JOIN, Map.of()));
    // Hiển thị dialog tìm trận với hiệu ứng loading
}
```

**Server Side:**
- File: `server/src/main/java/com/ltm/game/server/QueueService.java`
- Method: `joinQueue(String username)`
- Action: 
  - Xóa entry cũ (nếu có)
  - Thêm người chơi vào bảng `matchmaking_queue` với status `'waiting'`
  - Trigger matchmaking ngay lập tức

```sql
INSERT INTO matchmaking_queue (username, status) VALUES (?, 'waiting')
```

---

### 2. **Server Tự Động Ghép Trận**

**Scheduling:**
- Server chạy matchmaking scheduler mỗi **1 giây**
- Method: `tryMatchmaking()` - tự động quét queue

**Logic Ghép Trận:**
```java
// Lấy 2 người chơi đang chờ (FIFO - First In First Out)
SELECT username FROM matchmaking_queue 
WHERE status = 'waiting' 
ORDER BY join_time 
LIMIT 2
```

Khi tìm thấy 2 người:
1. Cập nhật status → `'matched'`
2. Tạo `matchId` = `"player1_vs_player2"`
3. Tạo acceptance map để tracking ai đã accept
4. Gửi notification `QUEUE_MATCHED` về cả 2 client
5. Khởi động timer 10 giây chờ accept

---

### 3. **Hiển Thị Popup "Match Found"**

**Client Side:**
- File: `client/src/main/java/com/ltm/game/client/controllers/MatchFoundController.java`
- FXML: `client/src/main/resources/fxml/match-found.fxml`

**Tính Năng:**
- ✨ **Hiệu ứng xoay vòng tròn** (3 rings: outer, middle, inner)
- ⏱️ **Countdown 10 giây** với animation
- ⚠️ **Warning khi còn ≤3 giây** (màu đỏ + pulse effect)
- 👥 **Hiển thị tên 2 người chơi** (YOU vs OPPONENT)
- 🎨 **League of Legends style UI** với gradient và glow effects

**UI Components:**
```xml
<Label fx:id="countdownLabel" text="10" styleClass="countdown-timer"/>
<Label fx:id="player1Label" text="YOU" styleClass="player-name"/>
<Label fx:id="player2Label" text="OPPONENT" styleClass="player-name"/>
<Button fx:id="acceptButton" text="✓ ACCEPT" styleClass="accept-button"/>
<Button fx:id="declineButton" text="✗ DECLINE" styleClass="decline-button"/>
```

---

### 4. **Accept/Decline Mechanism**

#### **4A. Người Chơi Bấm ACCEPT**

**Client Side:**
```java
@FXML
private void handleAccept() {
    accepted = true;
    acceptButton.setDisable(true);
    declineButton.setDisable(true);
    
    // Gửi accept lên server
    networkClient.send(new Message(Protocol.MATCH_ACCEPT, Map.of()));
    
    // Đổi countdown thành ✓
    countdownLabel.setText("✓");
    
    // Chờ server phản hồi (không tự đóng dialog)
}
```

**Server Side:**
```java
public void handleMatchAccept(String username) {
    // Tìm match của player này
    // Đánh dấu player đã accept
    acceptanceMap.put(username, true);
    
    // Kiểm tra cả 2 đã accept chưa
    if (allAccepted) {
        // Hủy timeout timer
        // Gửi MATCH_READY về cả 2 client
        // Delay 3 giây rồi start game
        
        Thread.sleep(3000);
        gameService.startGame(player1, player2);
    }
}
```

#### **4B. Người Chơi Bấm DECLINE**

**Client Side:**
```java
@FXML
private void handleDecline() {
    networkClient.send(new Message(Protocol.MATCH_DECLINE, Map.of()));
    closeDialog(); // Đóng popup ngay
}
```

**Server Side:**
```java
public void handleMatchDecline(String username) {
    // Tìm match
    String otherPlayer = (username == player1) ? player2 : player1;
    
    // Hủy timeout timer
    // Thông báo cho người còn lại
    otherSession.send(new Message(Protocol.MATCH_DECLINE, 
        Map.of("decliner", username)));
    
    // Xóa match
    pendingMatches.remove(matchId);
    
    // Reset cả 2 về status 'waiting'
    resetToWaiting(player1);
    resetToWaiting(player2);
}
```

#### **4C. Timeout (Không Ai Accept Trong 10 Giây)**

**Server Side:**
```java
private void handleMatchTimeout(String matchId, String player1, String player2) {
    // Gửi MATCH_DECLINE với reason="timeout" cho cả 2
    // Xóa match
    // Reset cả 2 về 'waiting'
}
```

**Client Side:**
```java
private void autoDecline() {
    countdownLabel.setText("TIME OUT");
    countdownLabel.setStyle("-fx-text-fill: #FF4444;");
    
    // Tự động decline và đóng dialog sau 1 giây
    PauseTransition.delay(1s).then(() -> {
        sendDeclineResponse();
        closeDialog();
    });
}
```

---

### 5. **Cả 2 Accept → Bắt Đầu Game**

**Flow:**
1. Server nhận accept từ cả 2 → gửi `MATCH_READY`
2. Client hiển thị countdown **3-2-1-GO** (như LOL)
3. Server delay 3 giây rồi gọi `gameService.startGame()`
4. Server gửi `GAME_START` kèm:
   - `roomId`
   - `imgLeft`, `imgRight` (Base64)
   - `imageWidth`, `imageHeight`
5. Client đóng popup và chuyển sang màn hình game

**Countdown 3-2-1-GO Animation:**
```java
public void onMatchStarting() {
    Timeline goCountdown = new Timeline(
        new KeyFrame(Duration.seconds(1), e -> {
            if (countdownValue > 0) {
                countdownLabel.setText(String.valueOf(countdownValue));
                // Pulse animation
                countdownValue--;
            } else {
                countdownLabel.setText("GO!");
                // GO animation + đóng dialog
            }
        })
    );
    goCountdown.setCycleCount(4); // 3, 2, 1, GO
    goCountdown.play();
}
```

---

### 6. **Xử Lý Disconnect/Mất Kết Nối**

**Server Side:**
```java
public void handleDisconnect(String username) {
    // Xóa khỏi queue
    removeFromQueue(username);
    
    // Tìm pending match của player này
    if (inPendingMatch) {
        String otherPlayer = findOtherPlayer(username);
        
        // Hủy timeout
        // Thông báo cho người còn lại
        otherSession.send(new Message(Protocol.MATCH_DECLINE,
            Map.of("reason", "disconnect", "decliner", username)));
        
        // Xóa match
        pendingMatches.remove(matchId);
        
        // Reset người còn lại về 'waiting'
        resetToWaiting(otherPlayer);
    }
}
```

**Client Side - Hiển Thị Thông Báo:**
```java
public void onOpponentDeclined(String reason, String decliner) {
    if ("disconnect".equals(reason)) {
        player1Label.setText("🔌 MATCH CANCELLED");
        player2Label.setText(decliner + " DISCONNECTED");
        // Style màu đỏ
    }
    // Tự động đóng sau 2 giây
}
```

---

## 📊 Database Schema

### Bảng `matchmaking_queue`

```sql
CREATE TABLE matchmaking_queue (
    username VARCHAR(50) PRIMARY KEY,
    status ENUM('waiting', 'matched') NOT NULL DEFAULT 'waiting',
    join_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
);
```

**Các Status:**
- `'waiting'` - Đang chờ ghép trận
- `'matched'` - Đã được ghép, đang chờ accept

---

## 🎨 UI/UX Features

### Match Found Dialog

**Hiệu Ứng:**
1. **Animated Rings** - 3 vòng tròn xoay và pulse
2. **Countdown Timer** - Font size lớn, màu gradient
3. **Warning State** - Màu đỏ + pulse khi còn ≤3 giây
4. **Accept/Decline Buttons** - Hover effect + glow
5. **Player Names** - Hiển thị rõ ràng với style khác nhau
6. **Status Indicators** - Dot (●) màu pending/accepted

**Style Classes:**
- `.match-found-title` - Title "MATCH FOUND"
- `.countdown-timer` - Số countdown lớn
- `.accept-button` - Nút xanh lá với glow
- `.decline-button` - Nút đỏ
- `.ring-outer`, `.ring-middle`, `.ring-inner` - 3 vòng tròn

---

## 🔧 Protocols (Shared)

File: `shared/src/main/java/com/ltm/game/shared/Protocol.java`

```java
// Queue
public static final String QUEUE_JOIN = "queue/join";
public static final String QUEUE_LEAVE = "queue/leave";
public static final String QUEUE_STATUS = "queue/status";
public static final String QUEUE_MATCHED = "queue/matched";

// Match
public static final String MATCH_ACCEPT = "match/accept";
public static final String MATCH_DECLINE = "match/decline";
public static final String MATCH_READY = "match/ready";

// Game
public static final String GAME_START = "game/start";
```

---

## 🚀 Cách Test

### 1. Khởi động Server
```powershell
cd d:\spot-the-difference-cooked
java -jar server/target/server-0.1.0-SNAPSHOT.jar
```

### 2. Khởi động 2 Client
**Terminal 1:**
```powershell
cd d:\spot-the-difference-cooked
mvn -pl client javafx:run
```

**Terminal 2:**
```powershell
cd d:\spot-the-difference-cooked
mvn -pl client javafx:run
```

### 3. Test Scenarios

#### **Scenario 1: Happy Path (Cả 2 Accept)**
1. Client 1: Login → Nhấn "🏆 1V1 RANKED"
2. Client 2: Login → Nhấn "🏆 1V1 RANKED"
3. ✅ Popup "Match Found" xuất hiện ở cả 2
4. Cả 2 bấm "✓ ACCEPT"
5. ✅ Countdown 3-2-1-GO
6. ✅ Game bắt đầu

#### **Scenario 2: 1 Người Decline**
1. Client 1 & 2: Tìm trận
2. Popup xuất hiện
3. Client 1: Bấm "✗ DECLINE"
4. ✅ Client 1: Dialog đóng ngay
5. ✅ Client 2: Hiển thị "Player1 DECLINED", đóng sau 2s
6. ✅ Cả 2 quay lại lobby, có thể tìm trận tiếp

#### **Scenario 3: Timeout**
1. Client 1 & 2: Tìm trận
2. Popup xuất hiện
3. **Không ai bấm gì**
4. Countdown chạy từ 10 → 3 (chuyển đỏ) → 0
5. ✅ Hiển thị "TIME OUT" ở cả 2
6. ✅ Auto decline và quay lại lobby

#### **Scenario 4: Disconnect**
1. Client 1 & 2: Tìm trận
2. Popup xuất hiện
3. Client 1: Tắt cửa sổ / mất mạng
4. ✅ Client 2: Hiển thị "Player1 DISCONNECTED"
5. ✅ Client 2 quay lại lobby

---

## 📁 File Structure

```
spot-the-difference-cooked/
├── shared/
│   └── src/main/java/com/ltm/game/shared/
│       └── Protocol.java                    # Định nghĩa protocols
│
├── server/
│   └── src/main/java/com/ltm/game/server/
│       ├── QueueService.java                # ⭐ Core matchmaking logic
│       ├── GameService.java                 # Start game
│       └── ClientHandler.java               # Handle messages
│
├── client/
│   ├── src/main/java/com/ltm/game/client/
│   │   ├── ClientApp.java                   # Message routing
│   │   └── controllers/
│   │       ├── LobbyController.java         # Hiển thị popup
│   │       └── MatchFoundController.java    # ⭐ Accept/Decline logic
│   │
│   └── src/main/resources/
│       └── fxml/
│           ├── match-found.fxml             # ⭐ UI popup
│           └── lobby.fxml                   # Nút tìm trận
│
└── db/
    └── schema.sql                           # Bảng matchmaking_queue
```

---

## 🎯 Key Features Implemented

✅ **Real-time Matchmaking** - Scheduler chạy mỗi giây  
✅ **FIFO Queue** - Người vào trước được ưu tiên  
✅ **Accept/Decline** - Cả 2 phải accept mới start game  
✅ **Timeout Protection** - Auto decline sau 10s  
✅ **Disconnect Handling** - Thông báo cho người còn lại  
✅ **3-2-1-GO Countdown** - Như League of Legends  
✅ **Beautiful UI** - Animated rings, gradient, glow effects  
✅ **Status Tracking** - Biết ai đã accept, ai chưa  
✅ **Clean Code** - Separation of concerns, easy to maintain  

---

## 🔮 Có Thể Mở Rộng

- [ ] **ELO/Rank Matching** - Ghép người cùng rank
- [ ] **Party System** - Tìm trận theo nhóm
- [ ] **Ban/Pick Phase** - Chọn map trước khi chơi
- [ ] **Queue Dodge Penalty** - Phạt người decline nhiều lần
- [ ] **Multiple Queue Types** - Normal, Ranked, Custom
- [ ] **Estimated Wait Time** - Dự đoán thời gian chờ

---

**🎮 Hệ thống đã sẵn sàng sử dụng! Chúc bạn test vui vẻ!**

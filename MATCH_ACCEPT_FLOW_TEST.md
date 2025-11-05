# Match Accept Flow - Testing Guide

## 🎯 Tóm tắt thay đổi

### ✅ Đã fix:

1. **Delay 3 giây sau khi cả 2 accept** (như League of Legends)
   - Server delay 3s trước khi gửi GAME_START
   - Client hiển thị countdown 3-2-1-GO! với animation đẹp mắt

2. **Timeout auto return về lobby**
   - Nếu không ai accept trong 10s → cả 2 auto out về lobby
   - Hiển thị message "⏱ MATCH CANCELLED - TIMEOUT"

3. **1 người decline → cả 2 out về lobby**
   - Hiển thị message "❌ [username] DECLINED"
   - Return về lobby sau 2 giây

4. **Chỉ khi CẢ 2 accept mới vào game**
   - Server kiểm tra cả 2 người đã accept
   - Nếu chỉ 1 người accept → đợi người kia
   - Nếu 1 người decline → cả 2 out

---

## 📋 Test Cases

### Test Case 1: **CẢ 2 ACCEPT - VÀO GAME BÌNH THƯỜNG** ✅

**Bước test:**
1. Start server: `cd server && mvn exec:java`
2. Mở 2 client windows:
   - Window 1: `cd client && mvn javafx:run`
   - Window 2: `cd client && mvn javafx:run`
3. Login 2 accounts KHÁC NHAU (vd: `user1`, `user2`)
4. Cả 2 click "🏆 1VS1 RANKED"
5. Match Found dialog xuất hiện
6. **CẢ 2 CLICK "✓ ACCEPT"**

**Kết quả mong đợi:**
- ✅ Match Found dialog hiển thị countdown: **3 → 2 → 1 → GO!**
- ✅ Sau 3 giây, cả 2 vào màn hình game
- ✅ Dialog tự động đóng
- ✅ Có thể chơi game bình thường

---

### Test Case 2: **TIMEOUT - KHÔNG AI ACCEPT** ⏱

**Bước test:**
1. Start server + 2 clients (như Test Case 1)
2. Login 2 accounts khác nhau
3. Cả 2 click "🏆 1VS1 RANKED"
4. Match Found dialog xuất hiện
5. **KHÔNG CLICK GÌ CẢ - ĐỢI HẾT 10 GIÂY**

**Kết quả mong đợi:**
- ✅ Countdown chạy từ 10 → 9 → ... → 1 → 0
- ✅ Khi countdown <= 3: Warning text hiển thị, số màu đỏ
- ✅ Khi timeout:
  - Label hiển thị "⏱ MATCH CANCELLED - TIMEOUT"
  - Buttons ẩn đi
  - Sau 2s, dialog đóng
- ✅ CẢ 2 người về lại lobby (không vào game)

---

### Test Case 3: **1 NGƯỜI ACCEPT, 1 NGƯỜI TIMEOUT** ⏱

**Bước test:**
1. Start server + 2 clients
2. Login 2 accounts khác nhau
3. Cả 2 click "🏆 1VS1 RANKED"
4. Match Found dialog xuất hiện
5. **Player 1 CLICK "✓ ACCEPT"**
6. **Player 2 KHÔNG CLICK GÌ - ĐỢI TIMEOUT**

**Kết quả mong đợi:**
- ✅ Player 1: Hiển thị "✓" (đã accept, đang đợi)
- ✅ Player 2: Countdown tiếp tục chạy
- ✅ Khi timeout:
  - CẢ 2 người nhận MATCH_DECLINE (reason: timeout)
  - CẢ 2 hiển thị "⏱ MATCH CANCELLED - TIMEOUT"
  - CẢ 2 về lobby sau 2s
- ✅ KHÔNG vào game

---

### Test Case 4: **1 NGƯỜI DECLINE** ❌

**Bước test:**
1. Start server + 2 clients
2. Login 2 accounts khác nhau
3. Cả 2 click "🏆 1VS1 RANKED"
4. Match Found dialog xuất hiện
5. **Player 1 CLICK "✗ DECLINE"**

**Kết quả mong đợi:**
- ✅ Player 1: Dialog đóng ngay lập tức, về lobby
- ✅ Player 2:
  - Hiển thị "❌ MATCH CANCELLED - [player1] DECLINED"
  - Dialog đóng sau 2s
  - Về lobby
- ✅ CẢ 2 KHÔNG vào game

---

### Test Case 5: **1 NGƯỜI ACCEPT, 1 NGƯỜI DECLINE** ⚠️

**Bước test:**
1. Start server + 2 clients
2. Login 2 accounts khác nhau
3. Cả 2 click "🏆 1VS1 RANKED"
4. Match Found dialog xuất hiện
5. **Player 1 CLICK "✓ ACCEPT"**
6. **Player 2 CLICK "✗ DECLINE"** (ngay sau đó)

**Kết quả mong đợi:**
- ✅ Player 1:
  - Đã hiển thị "✓" (đang đợi)
  - Nhận MATCH_DECLINE
  - Hiển thị "❌ MATCH CANCELLED - [player2] DECLINED"
  - Về lobby sau 2s
- ✅ Player 2: Dialog đóng ngay, về lobby
- ✅ CẢ 2 KHÔNG vào game

---

### Test Case 6: **CẢ 2 ACCEPT → 1 NGƯỜI DISCONNECT TRONG 3S DELAY** 🔌 [NEW FIX]

**Bước test:**
1. Start server + 2 clients
2. Login 2 accounts khác nhau
3. Cả 2 click "🏆 1VS1 RANKED"
4. Match Found dialog xuất hiện
5. **CẢ 2 CLICK "✓ ACCEPT"**
6. Countdown 3-2-1-GO bắt đầu
7. **TRONG LÚC COUNTDOWN, 1 NGƯỜI ĐÓNG CLIENT APP (ALT+F4 hoặc X)**

**Kết quả mong đợi:**
- ✅ Server detect match cancelled (pendingMatches đã bị xóa)
- ✅ Thread 3s delay CHECK lại và **KHÔNG start game**
- ✅ Người còn lại không bị stuck, quay về lobby
- ✅ Log server: "Match was cancelled during 3s delay. Not starting game."

**Lý do fix:**
- Trước đây: Thread 3s delay vẫn gọi `startGame()` dù match đã cancelled
- Bây giờ: Check `pendingMatches.containsKey(matchId)` trước khi start

---

## 🐛 Known Issues & Edge Cases

### ⚠️ Test với cùng 1 account (KHÔNG HỖ TRỢ)

**KHÔNG được test:**
- Login 2 lần với cùng username (vd: `user1` trên cả 2 windows)

**Lý do:**
- Server block duplicate login
- Connection sẽ bị drop
- Không phải bug của match accept flow

**Giải pháp:**
- Luôn test với **2 ACCOUNTS KHÁC NHAU**

---

## 📊 Flow Diagram

```
[QUEUE JOIN]
     ↓
[QUEUE_MATCHED] → Match Found Dialog xuất hiện (10s countdown)
     ↓
     ├─→ CẢ 2 ACCEPT
     │   └─→ MATCH_READY → 3-2-1-GO countdown → GAME_START (sau 3s)
     │
     ├─→ 1 NGƯỜI DECLINE
     │   └─→ MATCH_DECLINE → CẢ 2 về lobby
     │
     └─→ TIMEOUT (10s hết)
         └─→ MATCH_DECLINE (reason: timeout) → CẢ 2 về lobby
```

---

## 🔧 Debugging Tips

### Nếu vấn đề vẫn xảy ra:

1. **Check server logs:**
   ```
   [QUEUE] Matched: user1 vs user2
   [MATCH] user1 accepted match
   [MATCH] user2 accepted match
   [MATCH] Both players accepted. Starting game
   ```

2. **Check client behavior:**
   - Dialog có xuất hiện không?
   - Countdown có chạy không?
   - Có nhận được GAME_START không?

3. **Common issues:**
   - Server chưa start → client không kết nối được
   - Login cùng username → bị drop connection
   - Dialog không close → check ensureMatchDialogClosed()

---

---

## 🔧 **CRITICAL FIX APPLIED** ✅

### **Vấn đề gốc bạn báo:**
> "Tôi muốn fix khi cả 2 ấn **ACCEPT** thì mới vào UI ingame, tôi thấy vẫn tự động vào màn hình UI ingame"

### **Root Cause:**
Khi cả 2 accept → Server delay 3 giây trước khi start game. **NHƯNG** nếu trong 3s đó:
- 1 người decline
- 1 người disconnect (đóng app)
- Match bị timeout

→ Thread delay 3s **VẪN GỌI** `gameService.startGame()` → Cả 2 **VẪN VÀO GAME** dù đã decline!

### **Fix đã áp dụng:**

#### **1. Server: Check pendingMatches trước khi start game** ✅
```java
// QueueService.java - Line 193-199
Thread.sleep(3000);

// Double-check match still exists (not declined during 3s delay)
if (!pendingMatches.containsKey(matchId)) {
    Logger.info("[MATCH] Match was cancelled during 3s delay. Not starting game.");
    return; // KHÔNG start game
}

gameService.startGame(player1, player2);
```

#### **2. Server: Handle disconnect trong match** ✅
```java
// ClientHandler.java - Line 49-52
finally {
    if (session.username != null) {
        lobby.onDisconnect(session);
        queueService.handleDisconnect(session.username); // NEW
    }
}
```

#### **3. QueueService: Clean up pendingMatches khi disconnect** ✅
```java
// QueueService.java - handleDisconnect()
public void handleDisconnect(String username) {
    // Remove from queue
    removeFromQueue(username);
    
    // Find and cancel pending match
    if (acceptanceMap.containsKey(username)) {
        // Notify other player
        otherSession.send(MATCH_DECLINE, {reason: "disconnect"});
        
        // Clean up pendingMatches
        pendingMatches.remove(matchId);
    }
}
```

#### **4. Client: Hiển thị message khi opponent disconnect** ✅
```java
// MatchFoundController.java - Line 253-257
else if ("disconnect".equals(reason)) {
    player1Label.setText("🔌 MATCH CANCELLED");
    player2Label.setText((decliner) + " DISCONNECTED");
}
```

---

## ✅ Success Criteria

Test case được coi là **PASS** khi:

1. ✅ **Cả 2 accept → vào game sau đúng 3 giây** với countdown animation
2. ✅ **Timeout → cả 2 về lobby**, không vào game
3. ✅ **1 người decline → cả 2 về lobby**, không vào game
4. ✅ **1 accept + 1 decline → cả 2 về lobby**, không vào game
5. ✅ **1 người disconnect trong 3s countdown → KHÔNG start game** 🆕
6. ✅ **Messages hiển thị đúng** cho từng trường hợp
7. ✅ **Dialog tự động đóng** và không bị stuck

---

## 📞 Support

Nếu gặp vấn đề, cung cấp thông tin:
1. Test case nào bị fail
2. Behavior thực tế (khác với expected)
3. Server logs (nếu có)
4. Screenshot/video (nếu có thể)


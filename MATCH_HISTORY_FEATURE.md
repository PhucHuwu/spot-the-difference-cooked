# Match History Feature - Riot Games Style

## Tổng quan
Feature lịch sử trận đấu được thiết kế theo phong cách Riot Games (League of Legends), hiển thị trong một dialog popup ở giữa màn hình lobby với các thông tin đầy đủ về các trận đấu đã chơi.

## Cách sử dụng
1. Ở màn hình lobby, nhấn vào button **"LỊCH SỬ TRẬN ĐẤU"** (bên dưới button "TÌM TRẬN")
2. Dialog lịch sử trận đấu sẽ hiện ra ở giữa màn hình
3. Xem thông tin thống kê và lịch sử các trận đấu
4. Nhấn nút **✕** để đóng dialog

## Giao diện

### Dialog Style
- **Background**: Dark (#010A13) với overlay mờ
- **Border**: Gold (#C89B3C) với glow effect
- **Size**: 1000x700px
- **Position**: Giữa màn hình lobby

### Header Section
- Icon: ⚔ (crossed swords)
- Title: **LỊCH SỬ TRẬN ĐẤU** (Match History)
- Close button: ✕ (màu đỏ)

### Stats Overview (5 boxes)
1. **TỔNG TRẬN** - Total matches (Gold border)
2. **THẮNG** - Wins (Green border - #4CAF50)
3. **THUA** - Losses (Red border - #F44336)
4. **HÒA** - Draws (Yellow border - #FFC107)
5. **TỈ LỆ THẮNG** - Win rate % (Cyan border - #0ac8b9)

### Match History Table (20 trận gần nhất)
**Columns:**
- **THỜI GIAN**: Ngày giờ đấu (format: dd/MM/yyyy HH:mm)
- **ĐỐI THỦ**: Tên đối thủ
- **KẾT QUẢ**: Badge màu (THẮNG/THUA/HÒA)
- **TỈ SỐ**: Score (myScore - opponentScore)
- **THỜI LƯỢNG**: Duration (format: m:ss)
- **HẠNG MỤC**: Badges (⭐ MVP, 💎 PERFECT)

## Màu sắc theo Riot Games

### Main Colors
- **Gold**: #C89B3C (primary, borders, highlights)
- **Dark Gold**: #785A28 (gradients)
- **Text Light**: #F0E6D2 (main text)
- **Text Gold**: #C8AA6E (secondary text)

### Result Colors
- **Win**: #4CAF50 (green)
- **Loss**: #F44336 (red)
- **Draw**: #FFC107 (yellow)

### Badge Colors
- **MVP**: Gold gradient (#FFD700 → #FFA500)
- **PERFECT**: Gold gradient (#C89B3C → #785A28)

## Technical Implementation

### Files Created/Modified

#### Protocol
- `shared/Protocol.java`: Added MATCH_HISTORY_REQUEST, MATCH_HISTORY_DATA

#### Models
- `client/models/MatchHistoryRow.java`: Model cho dữ liệu lịch sử trận đấu

#### Controllers
- `client/controllers/MatchHistoryController.java`: Controller cho dialog
- `client/controllers/LobbyController.java`: Added handleShowMatchHistory()
- `client/ClientApp.java`: Added routing cho MATCH_HISTORY_DATA

#### Server
- `server/ClientHandler.java`: Added onMatchHistoryRequest() handler
  - Query database với JOIN để lấy tên đối thủ
  - Calculate duration từ started_at và finished_at
  - Check MVP (highest score) và PERFECT (score >= 5)

#### FXML & CSS
- `client/resources/fxml/match-history-dialog.fxml`: UI layout
- `client/resources/fxml/lobby.fxml`: Updated bottom section (HBox → VBox, added button)
- `client/resources/styles/match-history.css`: Riot Games styling

### Database Query
```sql
SELECT m.*, 
  CASE 
    WHEN m.player_a = ? THEN u2.username 
    ELSE u1.username 
  END as opponent,
  CASE 
    WHEN m.player_a = ? AND m.score_a > m.score_b THEN 'THẮNG'
    WHEN m.player_a = ? AND m.score_a < m.score_b THEN 'THUA'
    WHEN m.player_b = ? AND m.score_b > m.score_a THEN 'THẮNG'
    WHEN m.player_b = ? AND m.score_b < m.score_a THEN 'THUA'
    ELSE 'HÒA'
  END as result,
  ... (my_score, opponent_score)
FROM matches m
JOIN users u1 ON m.player_a = u1.username
JOIN users u2 ON m.player_b = u2.username
WHERE m.player_a = ? OR m.player_b = ?
ORDER BY m.finished_at DESC LIMIT 20
```

### Message Flow
1. User clicks **LỊCH SỬ TRẬN ĐẤU** button in lobby
2. LobbyController calls handleShowMatchHistory()
3. Opens dialog, loads MatchHistoryController
4. Controller sends MATCH_HISTORY_REQUEST to server
5. Server queries database, sends MATCH_HISTORY_DATA response
6. ClientApp routes message to MatchHistoryController
7. Controller updates UI with match data

## Features

### Statistics Display
- Real-time calculation of wins/losses/draws
- Win rate percentage with 1 decimal place
- Color-coded stat boxes

### Match Table
- Shows last 20 matches
- Sortable by date (newest first)
- Color-coded result badges
- MVP badge for winner (highest score)
- PERFECT badge for flawless games (5+ score)

### UX Enhancements
- Smooth dialog open/close
- Transparent background overlay
- Gold glow effects on hover
- Responsive table scrolling
- Empty state message: "Chưa có trận đấu nào"

## Future Enhancements
- Filter by date range
- Search by opponent name
- Detailed match replay
- Statistics charts/graphs
- Export match history
- Match details popup on row click

## Build & Test
```bash
# Build project
mvn clean install -DskipTests

# Run server
cd server
java -jar target/server-0.1.0-SNAPSHOT.jar

# Run client
cd client
mvn javafx:run
```

## Dependencies
- JavaFX 21.0.3
- MySQL 8.4.0
- Gson 2.10.1

## Notes
- Dialog uses transparent stage for overlay effect
- Controller reference managed via ClientApp.setMatchHistoryController()
- Auto-cleanup on dialog close
- All text in Vietnamese except badges

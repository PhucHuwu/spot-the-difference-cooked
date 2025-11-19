package com.ltm.game.server;

import com.ltm.game.shared.Message;
import com.ltm.game.shared.Protocol;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.*;

public class QueueService {
    private final GameService gameService;
    private final LobbyService lobbyService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Theo dõi việc chấp nhận trận đấu: matchId -> Tập hợp người chơi đã chấp nhận
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> pendingMatches = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> matchTimeouts = new ConcurrentHashMap<>();

    public QueueService(GameService gameService, LobbyService lobbyService) {
        this.gameService = gameService;
        this.lobbyService = lobbyService;
        startMatchmaking();
    }

    public void joinQueue(String username) {
        try (Connection c = Database.getConnection()) {
            // Xóa bất kỳ mục nào đã tồn tại
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM matchmaking_queue WHERE username = ?")) {
                ps.setString(1, username);
                ps.executeUpdate();
            }

            // Thêm vào hàng đợi
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO matchmaking_queue (username, status) VALUES (?, 'waiting')")) {
                ps.setString(1, username);
                ps.executeUpdate();
            }

            Logger.info("[QUEUE] " + username + " joined queue");
            tryMatchmaking();
        } catch (Exception e) {
            Logger.error("[QUEUE] Error joining queue for " + username, e);
        }
    }

    public void leaveQueue(String username) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM matchmaking_queue WHERE username = ?")) {
            ps.setString(1, username);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                Logger.info("[QUEUE] " + username + " left queue");
            }
        } catch (Exception e) {
            Logger.error("[QUEUE] Error leaving queue for " + username, e);
        }
    }

    public Map<String, Object> getQueueStatus(String username) {
    try (Connection c = Database.getConnection();
         PreparedStatement ps = c.prepareStatement(
             "SELECT join_time, TIMESTAMPDIFF(SECOND, join_time, NOW()) as wait_seconds " +
             "FROM matchmaking_queue WHERE username = ? AND status = 'waiting'")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int waitSeconds = rs.getInt("wait_seconds");
                    return Map.of(
                            "inQueue", true,
                            "waitSeconds", waitSeconds
                    );
                }
            }
        } catch (Exception e) {
            Logger.error("[QUEUE] Error getting status for " + username, e);
        }
        return Map.of("inQueue", false);
    }

    private void startMatchmaking() {
        scheduler.scheduleAtFixedRate(this::tryMatchmaking, 1, 1, TimeUnit.SECONDS);
    }

    private void tryMatchmaking() {
        try (Connection c = Database.getConnection()) {
            // Lấy 2 người chơi đang chờ
        try (PreparedStatement ps = c.prepareStatement(
            "SELECT username FROM matchmaking_queue WHERE status = 'waiting' ORDER BY join_time LIMIT 2")) {
                try (ResultSet rs = ps.executeQuery()) {
                    String player1 = null;
                    String player2 = null;

                    if (rs.next()) {
                        player1 = rs.getString("username");
                    }
                    if (rs.next()) {
                        player2 = rs.getString("username");
                    }

                    if (player1 != null && player2 != null && !player1.equals(player2)) {
                        matchPlayers(c, player1, player2);
                    }
                }
            }
        } catch (Exception e) {
            Logger.error("[QUEUE] Matchmaking error", e);
        }
    }

    private void matchPlayers(Connection c, String player1, String player2) throws Exception {
        // Đánh dấu là đã ghép đôi
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE matchmaking_queue SET status = 'matched' WHERE username IN (?, ?)")) {
            ps.setString(1, player1);
            ps.setString(2, player2);
            ps.executeUpdate();
        }

        String matchId = player1 + "_vs_" + player2;
        Logger.info("[QUEUE] Matched: " + player1 + " vs " + player2 + " (matchId: " + matchId + ")");

        // Theo dõi trận đấu đang chờ xử lý này
        ConcurrentHashMap<String, Boolean> acceptanceMap = new ConcurrentHashMap<>();
        acceptanceMap.put(player1, false);
        acceptanceMap.put(player2, false);
        pendingMatches.put(matchId, acceptanceMap);
        Logger.info("[MATCH] Created acceptance map: " + acceptanceMap + " for matchId: " + matchId);

        // Thông báo cho cả hai người chơi
        ClientSession s1 = lobbyService.getOnline(player1);
        ClientSession s2 = lobbyService.getOnline(player2);

        if (s1 != null) {
            s1.send(new Message(Protocol.QUEUE_MATCHED, Map.of("opponent", player2, "matchId", matchId)).toJson());
        }
        if (s2 != null) {
            s2.send(new Message(Protocol.QUEUE_MATCHED, Map.of("opponent", player1, "matchId", matchId)).toJson());
        }

        // Đặt thời gian chờ cho việc chấp nhận trận đấu (10 giây)
        ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {
            handleMatchTimeout(matchId, player1, player2);
        }, 10, TimeUnit.SECONDS);

        matchTimeouts.put(matchId, timeoutTask);
    }

    public void handleMatchAccept(String username) {
        Logger.info("[MATCH] " + username + " accepted match");

        // Tìm trận đấu mà người chơi này đang tham gia
        for (Map.Entry<String, ConcurrentHashMap<String, Boolean>> entry : pendingMatches.entrySet()) {
            String matchId = entry.getKey();
            ConcurrentHashMap<String, Boolean> acceptanceMap = entry.getValue();

            if (acceptanceMap.containsKey(username)) {
                acceptanceMap.put(username, true);
                Logger.info("[MATCH] Acceptance map after " + username + " accepted: " + acceptanceMap);

                // Kiểm tra xem cả hai người chơi đã chấp nhận chưa
                boolean allAccepted = acceptanceMap.size() == 2 && acceptanceMap.values().stream().allMatch(accepted -> accepted);
                Logger.info("[MATCH] All accepted check: " + allAccepted + " (size: " + acceptanceMap.size() + ", values: " + acceptanceMap + ")");

                if (allAccepted) {
                    // Cả hai đã chấp nhận - thông báo cho cả hai người chơi bắt đầu đếm ngược!
                    String[] players = matchId.split("_vs_");
                    String player1 = players[0];
                    String player2 = players[1];

                    Logger.info("[MATCH] Both players accepted. Sending MATCH_READY: " + matchId);

                    // Hủy thời gian chờ 10s (cho việc chấp nhận)
                    ScheduledFuture<?> timeout = matchTimeouts.remove(matchId);
                    if (timeout != null) {
                        timeout.cancel(false);
                    }

                    // Thông báo cho CẢ HAI người chơi rằng trận đấu đã sẵn sàng (bắt đầu đếm ngược)
                    ClientSession s1 = lobbyService.getOnline(player1);
                    ClientSession s2 = lobbyService.getOnline(player2);

                    if (s1 != null) {
                        s1.send(new Message(Protocol.MATCH_READY, Map.of()).toJson());
                    }
                    if (s2 != null) {
                        s2.send(new Message(Protocol.MATCH_READY, Map.of()).toJson());
                    }

                    // Bắt đầu trò chơi sau 11 giây trễ (thời gian đếm ngược: 10→0)
                    new Thread(() -> {
                        try {
                            Thread.sleep(11000); // 11 giây để khớp với âm thanh đếm ngược

                            // Kiểm tra lại trận đấu vẫn tồn tại (không bị từ chối trong 11s trễ)
                            if (!pendingMatches.containsKey(matchId)) {
                                Logger.info("[MATCH] Match " + matchId + " was cancelled during 11s delay. Not starting game.");
                                return;
                            }

                            gameService.startGame(player1, player2);

                            // Dọn dẹp
                            pendingMatches.remove(matchId);
                            removeFromQueue(player1);
                            removeFromQueue(player2);
                        } catch (Exception e) {
                            Logger.error("[MATCH] Error starting game: " + matchId, e);
                        }
                    }).start();
                } else {
                    // Chỉ một người chơi chấp nhận cho đến nay - hiển thị màn hình chờ
                    ClientSession acceptingSession = lobbyService.getOnline(username);
                    if (acceptingSession != null) {
                        acceptingSession.send(new Message(Protocol.MATCH_WAITING, Map.of()).toJson());
                    }
                }
                break;
            }
        }
    }

    public void handleMatchDecline(String username) {
        Logger.info("[MATCH] " + username + " declined match");

        // Tìm trận đấu mà người chơi này đang tham gia
        for (Map.Entry<String, ConcurrentHashMap<String, Boolean>> entry : pendingMatches.entrySet()) {
            String matchId = entry.getKey();
            ConcurrentHashMap<String, Boolean> acceptanceMap = entry.getValue();

            if (acceptanceMap.containsKey(username)) {
                String[] players = matchId.split("_vs_");
                String player1 = players[0];
                String player2 = players[1];
                String otherPlayer = username.equals(player1) ? player2 : player1;

                // Hủy thời gian chờ
                ScheduledFuture<?> timeout = matchTimeouts.remove(matchId);
                if (timeout != null) {
                    timeout.cancel(false);
                }

                // Thông báo cho người chơi khác
                ClientSession otherSession = lobbyService.getOnline(otherPlayer);
                if (otherSession != null) {
                    otherSession.send(new Message(Protocol.MATCH_DECLINE,
                        Map.of("decliner", username)).toJson());
                }

                // Dọn dẹp
                pendingMatches.remove(matchId);

                // XÓA cả hai người chơi khỏi hàng đợi (không đặt lại về trạng thái chờ)
                removeFromQueue(player1);
                removeFromQueue(player2);
                
                Logger.info("[MATCH] Removed both players from queue after decline by " + username);

                break;
            }
        }
    }

    private void handleMatchTimeout(String matchId, String player1, String player2) {
        ConcurrentHashMap<String, Boolean> acceptanceMap = pendingMatches.get(matchId);
        if (acceptanceMap == null) {
            return; // Đã được xử lý
        }

        Logger.info("[MATCH] Timeout for match: " + matchId);

        // Kiểm tra ai chưa chấp nhận
        boolean p1Accepted = acceptanceMap.getOrDefault(player1, false);
        boolean p2Accepted = acceptanceMap.getOrDefault(player2, false);

        if (!p1Accepted || !p2Accepted) {
            // Gửi thông báo hết thời gian chờ cho cả hai người chơi
            ClientSession s1 = lobbyService.getOnline(player1);
            ClientSession s2 = lobbyService.getOnline(player2);

            if (s1 != null) {
                s1.send(new Message(Protocol.MATCH_DECLINE,
                    Map.of("reason", "timeout")).toJson());
            }
            if (s2 != null) {
                s2.send(new Message(Protocol.MATCH_DECLINE,
                    Map.of("reason", "timeout")).toJson());
            }

            // Dọn dẹp
            pendingMatches.remove(matchId);
            matchTimeouts.remove(matchId);

            // XÓA cả hai người chơi khỏi hàng đợi (không đặt lại về trạng thái chờ)
            removeFromQueue(player1);
            removeFromQueue(player2);
            
            Logger.info("[MATCH] Removed both players from queue after timeout");
        }
    }

    private void resetToWaiting(String username) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE matchmaking_queue SET status = 'waiting' WHERE username = ?")) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (Exception e) {
            Logger.error("[QUEUE] Error resetting " + username + " to waiting", e);
        }
    }

    private void removeFromQueue(String username) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM matchmaking_queue WHERE username = ?")) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (Exception e) {
            Logger.error("[QUEUE] Error removing " + username + " from queue", e);
        }
    }

    /**
     * Xử lý ngắt kết nối của người chơi - dọn dẹp hàng đợi và trận đấu đang chờ
     */
    public void handleDisconnect(String username) {
        Logger.info("[QUEUE] Handling disconnect for: " + username);

        // Xóa khỏi hàng đợi
        removeFromQueue(username);

        // Kiểm tra nếu người chơi đang trong bất kỳ trận đấu đang chờ nào
        for (Map.Entry<String, ConcurrentHashMap<String, Boolean>> entry : pendingMatches.entrySet()) {
            String matchId = entry.getKey();
            ConcurrentHashMap<String, Boolean> acceptanceMap = entry.getValue();

            if (acceptanceMap.containsKey(username)) {
                String[] players = matchId.split("_vs_");
                String player1 = players[0];
                String player2 = players[1];
                String otherPlayer = username.equals(player1) ? player2 : player1;

                Logger.info("[QUEUE] Player " + username + " disconnected during match " + matchId);

                // Hủy thời gian chờ
                ScheduledFuture<?> timeout = matchTimeouts.remove(matchId);
                if (timeout != null) {
                    timeout.cancel(false);
                }

                // Thông báo cho người chơi khác
                ClientSession otherSession = lobbyService.getOnline(otherPlayer);
                if (otherSession != null) {
                    otherSession.send(new Message(Protocol.MATCH_DECLINE,
                        Map.of("reason", "disconnect", "decliner", username)).toJson());
                }

                // Dọn dẹp
                pendingMatches.remove(matchId);

                // XÓA người chơi khác khỏi hàng đợi (không đặt lại về trạng thái chờ)
                removeFromQueue(otherPlayer);
                
                Logger.info("[MATCH] Removed other player from queue after disconnect");

                break;
            }
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}


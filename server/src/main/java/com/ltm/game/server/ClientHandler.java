package com.ltm.game.server;

import com.ltm.game.shared.Message;
import com.ltm.game.shared.models.LeaderboardEntry;
import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.ltm.game.shared.Protocol;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final LobbyService lobby;
    private final GameService gameService;
    private final QueueService queueService;
    private final ClientSession session;

    public ClientHandler(Socket socket, LobbyService lobby, GameService gameService, QueueService queueService) throws Exception {
        this.socket = socket;
        this.lobby = lobby;
        this.gameService = gameService;
        this.queueService = queueService;
        this.session = new ClientSession(socket);
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Message msg = Message.fromJson(line);
                handle(msg);
            }
        } catch (Exception e) {
            if (session.username != null) {
                Logger.error("Client error for user " + session.username, e);
            } else {
                Logger.debug("Client disconnected before authentication");
            }
        } finally {
            if (session.username != null) {
                lobby.onDisconnect(session);
                queueService.handleDisconnect(session.username);
            }
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private void handle(Message msg) {
        switch (msg.type) {
            case Protocol.AUTH_LOGIN -> onLogin(msg);
            case Protocol.AUTH_LOGOUT -> onLogout();
            case Protocol.LOBBY_REQUEST -> onLobbyRequest();
            case Protocol.INVITE_SEND -> lobby.onInviteSend(session, (Map<?,?>) msg.payload);
            case Protocol.INVITE_RESPONSE -> gameService.onInviteResponse(session, (Map<?,?>) msg.payload);
            case Protocol.QUEUE_JOIN -> onQueueJoin();
            case Protocol.QUEUE_LEAVE -> onQueueLeave();
            case Protocol.QUEUE_STATUS -> onQueueStatus();
            case Protocol.MATCH_ACCEPT -> onMatchAccept();
            case Protocol.MATCH_DECLINE -> onMatchDecline();
            case Protocol.GAME_CLICK -> gameService.onGameClick(session, (Map<?,?>) msg.payload);
            case Protocol.GAME_QUIT -> gameService.onGameQuit(session, (Map<?,?>) msg.payload);
            case Protocol.LEADERBOARD -> onLeaderboard();
            case Protocol.MATCH_HISTORY_REQUEST -> onMatchHistoryRequest();
            default -> {}
        }
    }

    private void onLobbyRequest() {
        if (session.username != null) {
            lobby.sendLobbyList(session);
        }
    }

    private void onQueueJoin() {
        if (session.username != null) {
            queueService.joinQueue(session.username);
            session.send(new Message(Protocol.QUEUE_STATUS, Map.of("inQueue", true, "waitSeconds", 0)).toJson());
        }
    }

    private void onQueueLeave() {
        if (session.username != null) {
            queueService.leaveQueue(session.username);
            session.send(new Message(Protocol.QUEUE_STATUS, Map.of("inQueue", false)).toJson());
        }
    }

    private void onQueueStatus() {
        if (session.username != null) {
            Map<String, Object> status = queueService.getQueueStatus(session.username);
            session.send(new Message(Protocol.QUEUE_STATUS, status).toJson());
        }
    }

    private void onMatchAccept() {
        if (session.username != null) {
            queueService.handleMatchAccept(session.username);
        }
    }

    private void onMatchDecline() {
        if (session.username != null) {
            queueService.handleMatchDecline(session.username);
        }
    }

    private void onLogin(Message msg) {
        Map<?,?> p = (Map<?,?>) msg.payload;
        String username = String.valueOf(p.get("username"));
        String password = String.valueOf(p.get("password"));
        var result = lobby.authenticate(username, password);
        Map<String,Object> resp = new HashMap<>();
        resp.put("success", result.success);
        resp.put("message", result.message);
        resp.put("user", result.user);
        session.username = result.success ? result.user.username : null;
        session.inGame = false;
        session.send(new Message(Protocol.AUTH_RESULT, resp).toJson());
        if (result.success) {
            lobby.onLogin(session, result.user);
        }
    }

    private void onLogout() {
        if (session.username != null) {
            Logger.info("User " + session.username + " logged out");
            lobby.onDisconnect(session);
            queueService.handleDisconnect(session.username);
            session.username = null;
            session.inGame = false;
        }
    }

    private void onLeaderboard() {
        try {
            List<LeaderboardEntry> entries = new ArrayList<>();
            try (Connection c = Database.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT username, total_points, total_wins FROM users " +
                     "ORDER BY total_points DESC, total_wins DESC LIMIT 100")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add(new LeaderboardEntry(
                            rs.getString("username"),
                            rs.getInt("total_points"),
                            rs.getInt("total_wins")
                        ));
                    }
                }
            }
            session.send(new Message(Protocol.LEADERBOARD, entries).toJson());
        } catch (Exception e) {
            Logger.error("Error getting leaderboard", e);
        }
    }
    
    private void onMatchHistoryRequest() {
        if (session.username == null) return;
        
        try {
            // First get user ID from username
            Integer userId = null;
            try (Connection c = Database.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT id FROM users WHERE username = ?")) {
                ps.setString(1, session.username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getInt("id");
                    }
                }
            }
            
            if (userId == null) {
                Logger.error("User not found: " + session.username);
                return;
            }
            
            List<Map<String, Object>> matches = new ArrayList<>();
            
            try (Connection c = Database.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT m.id, m.score_a, m.score_b, m.result, m.created_at, " +
                     "CASE " +
                     "  WHEN m.player_a_id = ? THEN ua.username " +
                     "  ELSE ub.username " +
                     "END as opponent, " +
                     "CASE " +
                     "  WHEN m.player_a_id = ? AND m.score_a > m.score_b THEN 'THẮNG' " +
                     "  WHEN m.player_a_id = ? AND m.score_a < m.score_b THEN 'THUA' " +
                     "  WHEN m.player_b_id = ? AND m.score_b > m.score_a THEN 'THẮNG' " +
                     "  WHEN m.player_b_id = ? AND m.score_b < m.score_a THEN 'THUA' " +
                     "  ELSE 'HÒA' " +
                     "END as match_result, " +
                     "CASE " +
                     "  WHEN m.player_a_id = ? THEN m.score_a " +
                     "  ELSE m.score_b " +
                     "END as my_score, " +
                     "CASE " +
                     "  WHEN m.player_a_id = ? THEN m.score_b " +
                     "  ELSE m.score_a " +
                     "END as opponent_score " +
                     "FROM matches m " +
                     "JOIN users ua ON m.player_a_id = ua.id " +
                     "JOIN users ub ON m.player_b_id = ub.id " +
                     "WHERE m.player_a_id = ? OR m.player_b_id = ? " +
                     "ORDER BY m.created_at DESC LIMIT 20")) {
                
                // Set all parameters (userId appears 9 times in query)
                for (int i = 1; i <= 9; i++) {
                    ps.setInt(i, userId);
                }
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> match = new HashMap<>();
                        match.put("matchId", String.valueOf(rs.getInt("id")));
                        match.put("opponent", rs.getString("opponent"));
                        match.put("result", rs.getString("match_result"));
                        match.put("myScore", rs.getInt("my_score"));
                        match.put("opponentScore", rs.getInt("opponent_score"));
                        
                        // Since we don't have duration in DB, use a placeholder
                        match.put("duration", "N/A");
                        
                        // Use created_at as match date
                        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
                        match.put("date", createdAt != null ? createdAt.toLocalDateTime().toString() : "");
                        
                        // Check for MVP (highest score) and PERFECT (all differences found)
                        int myScore = rs.getInt("my_score");
                        int opponentScore = rs.getInt("opponent_score");
                        match.put("mvp", myScore > opponentScore);
                        match.put("perfect", myScore >= 7); // 7+ points = perfect game
                        
                        matches.add(match);
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("matches", matches);
            session.send(new Message(Protocol.MATCH_HISTORY_DATA, response).toJson());
            
        } catch (Exception e) {
            Logger.error("Error getting match history for " + session.username, e);
        }
    }
}


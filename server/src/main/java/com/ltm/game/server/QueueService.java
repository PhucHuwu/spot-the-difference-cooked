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

    // Track match acceptance: matchId -> Set of players who accepted
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> pendingMatches = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> matchTimeouts = new ConcurrentHashMap<>();

    public QueueService(GameService gameService, LobbyService lobbyService) {
        this.gameService = gameService;
        this.lobbyService = lobbyService;
        startMatchmaking();
    }

    public void joinQueue(String username) {
        try (Connection c = Database.getConnection()) {
            // Remove any existing entry
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM matchmaking_queue WHERE username = ?")) {
                ps.setString(1, username);
                ps.executeUpdate();
            }

            // Add to queue
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
            // Get 2 waiting players
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
        // Mark as matched
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE matchmaking_queue SET status = 'matched' WHERE username IN (?, ?)")) {
            ps.setString(1, player1);
            ps.setString(2, player2);
            ps.executeUpdate();
        }

        String matchId = player1 + "_vs_" + player2;
        Logger.info("[QUEUE] Matched: " + player1 + " vs " + player2 + " (matchId: " + matchId + ")");

        // Track this pending match
        ConcurrentHashMap<String, Boolean> acceptanceMap = new ConcurrentHashMap<>();
        acceptanceMap.put(player1, false);
        acceptanceMap.put(player2, false);
        pendingMatches.put(matchId, acceptanceMap);
        Logger.info("[MATCH] Created acceptance map: " + acceptanceMap + " for matchId: " + matchId);

        // Notify both players
        ClientSession s1 = lobbyService.getOnline(player1);
        ClientSession s2 = lobbyService.getOnline(player2);

        if (s1 != null) {
            s1.send(new Message(Protocol.QUEUE_MATCHED, Map.of("opponent", player2, "matchId", matchId)).toJson());
        }
        if (s2 != null) {
            s2.send(new Message(Protocol.QUEUE_MATCHED, Map.of("opponent", player1, "matchId", matchId)).toJson());
        }

        // Set timeout for match acceptance (10 seconds)
        ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {
            handleMatchTimeout(matchId, player1, player2);
        }, 10, TimeUnit.SECONDS);

        matchTimeouts.put(matchId, timeoutTask);
    }

    public void handleMatchAccept(String username) {
        Logger.info("[MATCH] " + username + " accepted match");

        // Find which match this player is in
        for (Map.Entry<String, ConcurrentHashMap<String, Boolean>> entry : pendingMatches.entrySet()) {
            String matchId = entry.getKey();
            ConcurrentHashMap<String, Boolean> acceptanceMap = entry.getValue();

            if (acceptanceMap.containsKey(username)) {
                acceptanceMap.put(username, true);
                Logger.info("[MATCH] Acceptance map after " + username + " accepted: " + acceptanceMap);

                // Check if both players accepted
                boolean allAccepted = acceptanceMap.size() == 2 && acceptanceMap.values().stream().allMatch(accepted -> accepted);
                Logger.info("[MATCH] All accepted check: " + allAccepted + " (size: " + acceptanceMap.size() + ", values: " + acceptanceMap + ")");

                if (allAccepted) {
                    // Both accepted - start game!
                    String[] players = matchId.split("_vs_");
                    String player1 = players[0];
                    String player2 = players[1];

                    Logger.info("[MATCH] Both players accepted. Starting game: " + matchId);

                    // Cancel timeout
                    ScheduledFuture<?> timeout = matchTimeouts.remove(matchId);
                    if (timeout != null) {
                        timeout.cancel(false);
                    }

                    // Notify both players that match is ready
                    ClientSession s1 = lobbyService.getOnline(player1);
                    ClientSession s2 = lobbyService.getOnline(player2);

                    if (s1 != null) {
                        s1.send(new Message(Protocol.MATCH_READY, Map.of()).toJson());
                    }
                    if (s2 != null) {
                        s2.send(new Message(Protocol.MATCH_READY, Map.of()).toJson());
                    }

                    // Start game after 3 seconds delay (like League of Legends)
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000);

                            // Double-check match still exists (not declined during 3s delay)
                            if (!pendingMatches.containsKey(matchId)) {
                                Logger.info("[MATCH] Match " + matchId + " was cancelled during 3s delay. Not starting game.");
                                return;
                            }

                            gameService.startGame(player1, player2);

                            // Clean up
                            pendingMatches.remove(matchId);
                            removeFromQueue(player1);
                            removeFromQueue(player2);
                        } catch (Exception e) {
                            Logger.error("[MATCH] Error starting game: " + matchId, e);
                        }
                    }).start();
                }
                break;
            }
        }
    }

    public void handleMatchDecline(String username) {
        Logger.info("[MATCH] " + username + " declined match");

        // Find which match this player is in
        for (Map.Entry<String, ConcurrentHashMap<String, Boolean>> entry : pendingMatches.entrySet()) {
            String matchId = entry.getKey();
            ConcurrentHashMap<String, Boolean> acceptanceMap = entry.getValue();

            if (acceptanceMap.containsKey(username)) {
                String[] players = matchId.split("_vs_");
                String player1 = players[0];
                String player2 = players[1];
                String otherPlayer = username.equals(player1) ? player2 : player1;

                // Cancel timeout
                ScheduledFuture<?> timeout = matchTimeouts.remove(matchId);
                if (timeout != null) {
                    timeout.cancel(false);
                }

                // Notify other player
                ClientSession otherSession = lobbyService.getOnline(otherPlayer);
                if (otherSession != null) {
                    otherSession.send(new Message(Protocol.MATCH_DECLINE,
                        Map.of("decliner", username)).toJson());
                }

                // Clean up
                pendingMatches.remove(matchId);

                // Reset both players to waiting status
                resetToWaiting(player1);
                resetToWaiting(player2);

                break;
            }
        }
    }

    private void handleMatchTimeout(String matchId, String player1, String player2) {
        ConcurrentHashMap<String, Boolean> acceptanceMap = pendingMatches.get(matchId);
        if (acceptanceMap == null) {
            return; // Already handled
        }

        Logger.info("[MATCH] Timeout for match: " + matchId);

        // Check who didn't accept
        boolean p1Accepted = acceptanceMap.getOrDefault(player1, false);
        boolean p2Accepted = acceptanceMap.getOrDefault(player2, false);

        if (!p1Accepted || !p2Accepted) {
            // Send timeout notification to both players
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

            // Clean up
            pendingMatches.remove(matchId);
            matchTimeouts.remove(matchId);

            // Reset both players to waiting
            resetToWaiting(player1);
            resetToWaiting(player2);
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
     * Handle player disconnect - clean up queue and pending matches
     */
    public void handleDisconnect(String username) {
        Logger.info("[QUEUE] Handling disconnect for: " + username);

        // Remove from queue
        removeFromQueue(username);

        // Check if player is in any pending match
        for (Map.Entry<String, ConcurrentHashMap<String, Boolean>> entry : pendingMatches.entrySet()) {
            String matchId = entry.getKey();
            ConcurrentHashMap<String, Boolean> acceptanceMap = entry.getValue();

            if (acceptanceMap.containsKey(username)) {
                String[] players = matchId.split("_vs_");
                String player1 = players[0];
                String player2 = players[1];
                String otherPlayer = username.equals(player1) ? player2 : player1;

                Logger.info("[QUEUE] Player " + username + " disconnected during match " + matchId);

                // Cancel timeout
                ScheduledFuture<?> timeout = matchTimeouts.remove(matchId);
                if (timeout != null) {
                    timeout.cancel(false);
                }

                // Notify other player
                ClientSession otherSession = lobbyService.getOnline(otherPlayer);
                if (otherSession != null) {
                    otherSession.send(new Message(Protocol.MATCH_DECLINE,
                        Map.of("reason", "disconnect", "decliner", username)).toJson());
                }

                // Clean up
                pendingMatches.remove(matchId);

                // Reset other player to waiting
                resetToWaiting(otherPlayer);

                break;
            }
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}


package com.ltm.game.server;

import com.ltm.game.shared.Message;
import com.ltm.game.shared.models.User;
import com.ltm.game.shared.Protocol;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GameService {
    private final LobbyService lobby;
    private final int turnSeconds;
    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(2);
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final UserRepository userRepo = new UserRepository();
    private final MatchRepository matchRepo = new MatchRepository();
    private final ImageSetRepository imageRepo = new ImageSetRepository();

    public GameService(LobbyService lobby, java.util.Properties props) {
        this.lobby = lobby;
        this.turnSeconds = Integer.parseInt(props.getProperty("turn.seconds", "15"));
    }

    public void startGame(String userA, String userB) {
        GameRoom room;
        ImageSetRepository.ImageSet set = null;
        try {
            set = imageRepo.loadRandom();
        } catch (Exception e) {
            System.err.println("Failed to load random image set: "+e.getMessage());
        }
        if (set != null && set.diffs() != null && !set.diffs().isEmpty() && set.left() != null && set.right() != null) {
            room = new GameRoom(UUID.randomUUID().toString(), userA, userB, set.diffs());
            room.imageSetId = String.valueOf(set.id());
            room.imageWidth = set.width();
            room.imageHeight = set.height();
            room.imgLeft = set.left();
            room.imgRight = set.right();
        } else {
            room = new GameRoom(UUID.randomUUID().toString(), userA, userB, sampleDifferences());
            room.imageSetId = "builtin-1";
            room.imageWidth = 300; room.imageHeight = 300;
            room.imgLeft = null; room.imgRight = null;
        }
        rooms.put(room.id, room);
        lobby.setBusy(userA, true);
        lobby.setBusy(userB, true);
        notifyStart(room);
        scheduleTurnTimeout(room);
    }

    public void onGameClick(ClientSession session, Map<?,?> payload) {
        String roomId = String.valueOf(payload.get("roomId"));
        double x = Double.parseDouble(String.valueOf(payload.get("x")));
        double y = Double.parseDouble(String.valueOf(payload.get("y")));
        System.out.println("GAME_CLICK received: roomId=" + roomId + ", user=" + session.username + ", x=" + x + ", y=" + y);
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            System.err.println("Room not found: " + roomId);
            return;
        }
        synchronized (room) {
            System.out.println("Current turn: " + room.currentTurn + ", clicking user: " + session.username);
            if (!room.currentTurn.equals(session.username)) {
                System.out.println("Not this player's turn - ignoring click");
                session.send(new Message(Protocol.ERROR, Map.of("message", "Không phải lượt của bạn!")).toJson());
                return;
            }
            
            // Try to find a difference at the clicked position
            boolean hit = room.tryHit(x, y, session.username);
            System.out.println("Hit result: " + hit + " at (" + x + ", " + y + ")");
            
            if (hit) {
                // Correct hit: score already updated in tryHit()
                System.out.println("✓ Correct! " + session.username + " scores. Scores: " + room.playerA + "=" + room.scoreA + ", " + room.playerB + "=" + room.scoreB);
                
                // Check if game is finished
                if (room.isFinished()) {
                    System.out.println("🏁 Game finished! All differences found. Total found: " + room.found.size() + "/" + room.differences.size());
                    room.finished = true; // Mark as finished immediately
                    // Send final update with all found differences
                    broadcastUpdate(room, 0);
                    // Small delay to ensure update is received before end message
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println("🏁 Sending GAME_END message...");
                    endGame(room, "all-found");
                    return;
                }
                
                // Correct hit: keep turn, reset timer
                room.turnSeq++;
                System.out.println("Correct hit! " + session.username + " keeps turn (seq=" + room.turnSeq + ")");
                scheduleTurnTimeout(room);
                broadcastUpdate(room, turnSeconds * 1000);
            } else {
                // Missed: switch turn to opponent
                System.out.println("✗ Miss! No difference at (" + x + ", " + y + ") - switching turn");
                room.currentTurn = room.currentTurn.equals(room.playerA) ? room.playerB : room.playerA;
                room.turnSeq++;
                System.out.println("Turn switched to: " + room.currentTurn + " (seq=" + room.turnSeq + ")");
                
                // Reset timer and broadcast update with NEW turnSeq
                scheduleTurnTimeout(room);
                broadcastUpdate(room, turnSeconds * 1000);
            }
        }
    }

    public void onGameQuit(ClientSession session, Map<?,?> payload) {
        String roomId = String.valueOf(payload.get("roomId"));
        GameRoom room = rooms.get(roomId);
        if (room == null) return;
        synchronized (room) {
            System.out.println("🚪 Player " + session.username + " quit game. Current scores: " + room.playerA + "=" + room.scoreA + ", " + room.playerB + "=" + room.scoreB);
            // Send final update with current scores before ending
            broadcastUpdate(room, 0);
            // Longer delay to ensure update is received and processed
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("🚪 Ending game due to quit...");
            endGame(room, session.username + "-quit");
        }
    }

    /**
     * Handle player disconnect during game - find their active room and end game with forfeit
     */
    public void handleDisconnect(String username) {
        // Find if this player is in any active game room
        GameRoom activeRoom = null;
        for (GameRoom room : rooms.values()) {
            if ((room.playerA.equals(username) || room.playerB.equals(username)) && !room.finished) {
                activeRoom = room;
                break;
            }
        }
        
        if (activeRoom != null) {
            synchronized (activeRoom) {
                if (!activeRoom.finished) {
                    Logger.info("[GAME] Player " + username + " disconnected during game. Current scores: " + activeRoom.playerA + "=" + activeRoom.scoreA + ", " + activeRoom.playerB + "=" + activeRoom.scoreB);
                    // Send final update with current scores before ending
                    broadcastUpdate(activeRoom, 0);
                    // Longer delay to ensure update is received and processed
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    Logger.info("[GAME] Ending game with forfeit");
                    endGame(activeRoom, username + "-quit");
                }
            }
        }
    }

    public void onInviteResponse(ClientSession session, Map<?,?> payload) {
        String fromUser = String.valueOf(payload.get("fromUser"));
        boolean accepted = Boolean.parseBoolean(String.valueOf(payload.get("accepted")));
        ClientSession inviter = lobby.getOnline(fromUser);
        if (inviter != null) {
            inviter.send(new Message(Protocol.INVITE_RESPONSE, Map.of("fromUser", session.username, "accepted", accepted)).toJson());
        }
        if (accepted && inviter != null) {
            startGame(fromUser, session.username);
        }
    }

    private void notifyStart(GameRoom room) {
        Map<String,Object> payload = new HashMap<>();
        payload.put("roomId", room.id);
        payload.put("imageSetId", room.imageSetId);
        payload.put("imageWidth", room.imageWidth);
        payload.put("imageHeight", room.imageHeight);
        payload.put("differences", room.differences);
        try {
            if (room.imgLeft != null && room.imgRight != null) {
                String b64L = java.util.Base64.getEncoder().encodeToString(room.imgLeft);
                String b64R = java.util.Base64.getEncoder().encodeToString(room.imgRight);
                payload.put("imgLeft", b64L);
                payload.put("imgRight", b64R);
            }
        } catch (Exception ignore) {}
        payload.put("currentTurn", room.currentTurn);
        sendToPlayers(room, new Message(Protocol.GAME_START, payload));
        broadcastUpdate(room, turnSeconds * 1000);
    }

    private void broadcastUpdate(GameRoom room, long remainingMs) {
        Map<String,Object> upd = new HashMap<>();
        upd.put("scores", Map.of(room.playerA, room.scoreA, room.playerB, room.scoreB));
        upd.put("found", room.found);
        upd.put("lastFinder", room.lastFinder);
        upd.put("nextTurn", room.currentTurn);
        upd.put("remainingTurnMs", remainingMs);
        sendToPlayers(room, new Message(Protocol.GAME_UPDATE, upd));
    }

    private void sendToPlayers(GameRoom room, Message msg) {
        String json = msg.toJson();
        ClientSession a = lobby.getOnline(room.playerA);
        ClientSession b = lobby.getOnline(room.playerB);
        if (a != null) a.send(json);
        if (b != null) b.send(json);
    }

    private void scheduleTurnTimeout(GameRoom room) {
        room.nextDeadline = System.currentTimeMillis() + turnSeconds * 1000L;
        long capturedSeq = room.turnSeq;
        System.out.println("📅 Scheduling timeout for seq=" + capturedSeq + ", turn=" + room.currentTurn + ", delay=" + turnSeconds + "s");
        scheduler.schedule(() -> onTimeout(room.id, capturedSeq), turnSeconds, TimeUnit.SECONDS);
    }

    private void onTimeout(String roomId, long seq) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            System.out.println("⏰ Timeout fired but room not found: " + roomId);
            return;
        }
        synchronized (room) {
            System.out.println("⏰ Timeout fired: seq=" + seq + ", current room.turnSeq=" + room.turnSeq + ", finished=" + room.finished);
            if (room.turnSeq != seq || room.finished) {
                System.out.println("  ➡ Timeout IGNORED (stale or finished)");
                return; // already advanced or game finished
            }
            
            System.out.println("  ➡ Timeout VALID! Auto-switching turn from " + room.currentTurn);
            // Switch turn directly (increment seq to invalidate this timeout)
            room.currentTurn = room.currentTurn.equals(room.playerA) ? room.playerB : room.playerA;
            room.turnSeq++;
            System.out.println("  ➡ Turn auto-switched to: " + room.currentTurn + " (new seq=" + room.turnSeq + ")");
            
            scheduleTurnTimeout(room);
            broadcastUpdate(room, turnSeconds * 1000); // Send full time for new turn
        }
    }

    private void endGame(GameRoom room, String reason) {
        room.finished = true;
        
        System.out.println("🏁 endGame() called");
        System.out.println("   Reason: " + reason);
        System.out.println("   PlayerA: " + room.playerA + " = " + room.scoreA);
        System.out.println("   PlayerB: " + room.playerB + " = " + room.scoreB);
        System.out.println("   Total differences: " + room.differences.size());
        
        String winner;
        int finalScoreA = room.scoreA;
        int finalScoreB = room.scoreB;
        
        // If someone quit, the other player wins and gets all remaining points
        if (reason.endsWith("-quit")) {
            String quitter = reason.substring(0, reason.indexOf("-quit"));
            winner = quitter.equals(room.playerA) ? room.playerB : room.playerA;
            
            // Award winner with total differences count (as if they found all)
            int totalDifferences = room.differences.size();
            
            if (winner.equals(room.playerA)) {
                // Player A wins, Player B quit
                finalScoreA = totalDifferences;
                finalScoreB = 0; // Quitter gets 0 points in match
            } else {
                // Player B wins, Player A quit
                finalScoreB = totalDifferences;
                finalScoreA = 0; // Quitter gets 0 points in match
            }
            
            Logger.info("[GAME] Player " + quitter + " quit, " + winner + " wins by forfeit and gets full score (" + totalDifferences + "-0). Adjusted scores: " + room.playerA + "=" + finalScoreA + ", " + room.playerB + "=" + finalScoreB);
        } else {
            // Normal game end - determine by score
            if (room.scoreA > room.scoreB) {
                winner = room.playerA;
            } else if (room.scoreB > room.scoreA) {
                winner = room.playerB;
            } else {
                winner = "DRAW";
            }
        }
        
        System.out.println("   Winner: " + winner);
        System.out.println("   Final scores: " + room.playerA + "=" + finalScoreA + ", " + room.playerB + "=" + finalScoreB);
        
        // Send GAME_END with adjusted scores
        Map<String,Object> payload = new HashMap<>();
        payload.put("reason", reason);
        payload.put("scores", Map.of(room.playerA, finalScoreA, room.playerB, finalScoreB));
        payload.put("result", winner);
        payload.put("found", room.found);
        
        System.out.println("🏁 Sending GAME_END with payload: " + payload);
        sendToPlayers(room, new Message(Protocol.GAME_END, payload));
        rooms.remove(room.id);
        lobby.setBusy(room.playerA, false);
        lobby.setBusy(room.playerB, false);
        
        try {
            // Lưu kết quả trận với điểm số đã điều chỉnh
            User a = userRepo.findByUsername(room.playerA);
            User b = userRepo.findByUsername(room.playerB);
            if (a != null && b != null) {
                // Save match with adjusted scores and explicit winner
                matchRepo.saveMatch(a.id, b.id, finalScoreA, finalScoreB, winner, room.playerA, room.playerB);
                
                Logger.info("[GAME] Saved match: " + room.playerA + " (" + finalScoreA + ") vs " + room.playerB + " (" + finalScoreB + "), winner: " + winner);
                
                // Cập nhật stats người chơi
                if (reason.endsWith("-quit")) {
                    // Forfeit case: Winner gets +3 points, quitter gets -2 points
                    String quitter = reason.substring(0, reason.indexOf("-quit"));
                    
                    if (winner.equals(room.playerA)) {
                        // Player A won, Player B quit
                        userRepo.updateStats(a.id, 3, true, false, false);  // Winner: +3 points, +1 win
                        userRepo.updateStats(b.id, -2, false, true, false); // Quitter: -2 points, +1 loss
                        Logger.info("[GAME] Stats updated - Winner " + room.playerA + ": +3 points, Quitter " + room.playerB + ": -2 points");
                    } else {
                        // Player B won, Player A quit
                        userRepo.updateStats(a.id, -2, false, true, false); // Quitter: -2 points, +1 loss
                        userRepo.updateStats(b.id, 3, true, false, false);  // Winner: +3 points, +1 win
                        Logger.info("[GAME] Stats updated - Winner " + room.playerB + ": +3 points, Quitter " + room.playerA + ": -2 points");
                    }
                } else {
                    // Normal game: Winner gets +3 points, loser gets 0 points
                    if (winner.equals(room.playerA)) {
                        userRepo.updateStats(a.id, 3, true, false, false);  // Winner: +3 points
                        userRepo.updateStats(b.id, 0, false, true, false);  // Loser: 0 points
                        Logger.info("[GAME] Stats updated - Winner " + room.playerA + ": +3 points, Loser " + room.playerB + ": 0 points");
                    } else if (winner.equals(room.playerB)) {
                        userRepo.updateStats(a.id, 0, false, true, false);  // Loser: 0 points
                        userRepo.updateStats(b.id, 3, true, false, false);  // Winner: +3 points
                        Logger.info("[GAME] Stats updated - Winner " + room.playerB + ": +3 points, Loser " + room.playerA + ": 0 points");
                    } else {
                        // Draw case: Both get +1 point
                        userRepo.updateStats(a.id, 1, false, false, true);
                        userRepo.updateStats(b.id, 1, false, false, true);
                        Logger.info("[GAME] Stats updated - Draw: Both players get +1 point");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to save match result: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Broadcast lobby list again after a small delay to ensure all clients receive updated status
        new Thread(() -> {
            try {
                Thread.sleep(100);
                lobby.broadcastLobby();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private List<Map<String,Object>> sampleDifferences() {
        // Simple built-in difference points with LARGER radius for easier clicking
        List<Map<String,Object>> diffs = new ArrayList<>();
        diffs.add(Map.of("x", 100, "y", 120, "radius", 50));
        diffs.add(Map.of("x", 200, "y", 80, "radius", 50));
        diffs.add(Map.of("x", 260, "y", 160, "radius", 50));
        return diffs;
    }

    static class GameRoom {
        final String id;
        final String playerA;
        final String playerB;
        String currentTurn;
        int scoreA = 0;
        int scoreB = 0;
        final List<Map<String,Object>> differences; // x,y,radius
        final List<Map<String,Object>> found = new ArrayList<>();
        String lastFinder = null;
        boolean finished = false;
        long nextDeadline = 0;
        long turnSeq = 0;
        String imageSetId;
        int imageWidth;
        int imageHeight;
        byte[] imgLeft;
        byte[] imgRight;

        GameRoom(String id, String a, String b, List<Map<String,Object>> diffs) {
            this.id = id; this.playerA = a; this.playerB = b; this.currentTurn = a; this.differences = diffs;
        }

        boolean tryHit(double x, double y, String username) {
            System.out.println("🎯 Checking hit at click position: (" + x + ", " + y + ")");
            System.out.println("📍 Total differences to check: " + differences.size());
            
            for (int i = 0; i < differences.size(); i++) {
                Map<String,Object> d = differences.get(i);
                boolean already = found.stream().anyMatch(f -> f.get("x").equals(d.get("x")) && f.get("y").equals(d.get("y")));
                if (already) {
                    System.out.println("  Diff #" + i + ": ALREADY FOUND - skipping");
                    continue;
                }
                
                double targetX = ((Number)d.get("x")).doubleValue();
                double targetY = ((Number)d.get("y")).doubleValue();
                double r = ((Number)d.get("radius")).doubleValue();
                double dx = x - targetX;
                double dy = y - targetY;
                double distance = Math.sqrt(dx*dx + dy*dy);
                
                System.out.println("  Diff #" + i + ": center=(" + targetX + ", " + targetY + "), radius=" + r);
                System.out.println("    Distance from click: " + String.format("%.2f", distance) + " (need <= " + r + ")");
                
                if (dx*dx + dy*dy <= r*r) {
                    System.out.println("    ✅ HIT! Adding to found list.");
                    // Add the difference with finder info
                    Map<String,Object> foundDiff = new HashMap<>(d);
                    foundDiff.put("finder", username);
                    found.add(foundDiff);
                    lastFinder = username;
                    if (username.equals(playerA)) scoreA++; else scoreB++;
                    return true;
                } else {
                    System.out.println("    ❌ TOO FAR");
                }
            }
            System.out.println("❌ No hit found at (" + x + ", " + y + ")");
            return false;
        }

        boolean isFinished() { return found.size() >= differences.size(); }

        void switchTurn() { currentTurn = currentTurn.equals(playerA) ? playerB : playerA; turnSeq++; }
    }
}

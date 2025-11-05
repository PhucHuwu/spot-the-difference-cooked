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
                    System.out.println("Game finished! All differences found.");
                    broadcastUpdate(room, 0);
                    endGame(room, "all-found");
                    return;
                }
            } else {
                // Missed
                System.out.println("✗ Miss! No difference at (" + x + ", " + y + ")");
            }
            
            // Always switch turn after any click (hit or miss)
            // Increment turnSeq FIRST to invalidate old timeout, then schedule new one
            room.currentTurn = room.currentTurn.equals(room.playerA) ? room.playerB : room.playerA;
            room.turnSeq++;
            System.out.println("Turn switched to: " + room.currentTurn + " (seq=" + room.turnSeq + ")");
            
            // Reset timer and broadcast update with NEW turnSeq
            scheduleTurnTimeout(room);
            broadcastUpdate(room, turnSeconds * 1000);
        }
    }

    public void onGameQuit(ClientSession session, Map<?,?> payload) {
        String roomId = String.valueOf(payload.get("roomId"));
        GameRoom room = rooms.get(roomId);
        if (room == null) return;
        synchronized (room) {
            endGame(room, session.username + "-quit");
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
        Map<String,Object> payload = new HashMap<>();
        payload.put("reason", reason);
        payload.put("scores", Map.of(room.playerA, room.scoreA, room.playerB, room.scoreB));
        String winner;
        if (room.scoreA > room.scoreB) winner = room.playerA;
        else if (room.scoreB > room.scoreA) winner = room.playerB;
        else winner = "DRAW";
        payload.put("result", winner);
        sendToPlayers(room, new Message(Protocol.GAME_END, payload));
        rooms.remove(room.id);
        lobby.setBusy(room.playerA, false);
        lobby.setBusy(room.playerB, false);
        
        try {
            // Lưu kết quả trận
            User a = userRepo.findByUsername(room.playerA);
            User b = userRepo.findByUsername(room.playerB);
            if (a != null && b != null) {
                matchRepo.saveMatch(a.id, b.id, room.scoreA, room.scoreB);
                
                // Cập nhật stats người chơi
                if (winner.equals(room.playerA)) {
                    userRepo.updateStats(a.id, room.scoreA, true, false, false);
                    userRepo.updateStats(b.id, room.scoreB, false, true, false);
                } else if (winner.equals(room.playerB)) {
                    userRepo.updateStats(a.id, room.scoreA, false, true, false);
                    userRepo.updateStats(b.id, room.scoreB, true, false, false);
                } else {
                    userRepo.updateStats(a.id, room.scoreA, false, false, true);
                    userRepo.updateStats(b.id, room.scoreB, false, false, true);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to save match result: " + e.getMessage());
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

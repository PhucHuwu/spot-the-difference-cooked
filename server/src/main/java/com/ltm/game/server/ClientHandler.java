package com.ltm.game.server;

import com.ltm.game.shared.Message;
import com.ltm.game.shared.models.LeaderboardEntry;
import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.ltm.game.shared.Protocol;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {
    private static final Gson GSON = new Gson();

    private final Socket socket;
    private final LobbyService lobby;
    private final GameService gameService;
    private final ClientSession session;

    public ClientHandler(Socket socket, LobbyService lobby, GameService gameService) throws Exception {
        this.socket = socket;
        this.lobby = lobby;
        this.gameService = gameService;
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
            // client disconnected or error
        } finally {
            if (session.username != null) {
                lobby.onDisconnect(session);
            }
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private void handle(Message msg) {
        switch (msg.type) {
            case Protocol.AUTH_LOGIN -> onLogin(msg);
            case Protocol.INVITE_SEND -> lobby.onInviteSend(session, (Map<?,?>) msg.payload);
            case Protocol.INVITE_RESPONSE -> gameService.onInviteResponse(session, (Map<?,?>) msg.payload);
            case Protocol.GAME_CLICK -> gameService.onGameClick(session, (Map<?,?>) msg.payload);
            case Protocol.GAME_QUIT -> gameService.onGameQuit(session, (Map<?,?>) msg.payload);
            case Protocol.LEADERBOARD -> onLeaderboard();
            default -> {}
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
            System.err.println("Error getting leaderboard: " + e.getMessage());
        }
    }
}

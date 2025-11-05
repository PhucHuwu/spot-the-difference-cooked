package com.ltm.game.server;

import com.ltm.game.shared.Message;
import com.ltm.game.shared.Protocol;
import com.ltm.game.shared.models.User;
import com.ltm.game.shared.models.UserStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyService {
    private final ConcurrentHashMap<String, ClientSession> online = new ConcurrentHashMap<>();
    private final UserRepository userRepo = new UserRepository();

    public static class AuthResult {
        public final boolean success; public final String message; public final User user;
        public AuthResult(boolean success, String message, User user) {this.success=success; this.message=message; this.user=user;}
    }

    public AuthResult authenticate(String username, String password) {
        try {
            User u = userRepo.findByUsername(username);
            if (u == null) {
                // auto-register for demo
                userRepo.createUser(username, password);
                u = userRepo.findByUsername(username);
            } else {
                if (!userRepo.verifyPassword(username, password)) {
                    return new AuthResult(false, "Sai mật khẩu", null);
                }
            }
            return new AuthResult(true, "OK", u);
        } catch (Exception e) {
            return new AuthResult(false, "Lỗi hệ thống: "+e.getMessage(), null);
        }
    }

    public void onLogin(ClientSession session, User user) {
        online.put(user.username, session);
        broadcastLobby();
    }

    public void onDisconnect(ClientSession session) {
        if (session.username != null) {
            online.remove(session.username);
            broadcastLobby();
        }
    }

    public void setBusy(String username, boolean busy) {
        ClientSession s = online.get(username);
        if (s != null) {
            s.inGame = busy;
            broadcastLobby();
        }
    }

    public void onInviteSend(ClientSession from, Map<?,?> payload) {
        String toUser = String.valueOf(payload.get("toUser"));
        ClientSession target = online.get(toUser);
        if (target != null) {
            target.send(new Message(Protocol.INVITE_RECEIVED, Map.of("fromUser", from.username)).toJson());
        }
    }

    public void onInviteResponse(ClientSession session, Map<?,?> payload) {
        // This will be handled by GameService when accepted; for now just forward response to inviter
        String fromUser = String.valueOf(payload.get("fromUser"));
        boolean accepted = Boolean.parseBoolean(String.valueOf(payload.get("accepted")));
        ClientSession inviter = online.get(fromUser);
        if (inviter != null) {
            inviter.send(new Message(Protocol.INVITE_RESPONSE, Map.of("fromUser", session.username, "accepted", accepted)).toJson());
        }
    }

    public List<UserStatus> currentLobby() {
        List<UserStatus> list = new ArrayList<>();
        online.forEach((name, sess) -> list.add(new UserStatus(name, userRepo.safeGetTotalPoints(name), sess.inGame?"In-game":"Online")));
        return list;
    }

    public void broadcastLobby() {
        List<UserStatus> list = currentLobby();
        Message msg = new Message(Protocol.LOBBY_LIST, list);
        String json = msg.toJson();
        online.values().forEach(s -> s.send(json));
    }

    public void sendLobbyList(ClientSession session) {
        List<UserStatus> list = currentLobby();
        Message msg = new Message(Protocol.LOBBY_LIST, list);
        session.send(msg.toJson());
    }

    public ClientSession getOnline(String username) { return online.get(username); }
}

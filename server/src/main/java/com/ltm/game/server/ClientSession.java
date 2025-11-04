package com.ltm.game.server;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientSession {
    public final Socket socket;
    public final PrintWriter out;
    public volatile String username;
    public volatile boolean inGame = false;

    public ClientSession(Socket socket) throws Exception {
        this.socket = socket;
        this.out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);
    }

    public void send(String json) {
        out.println(json);
    }
}

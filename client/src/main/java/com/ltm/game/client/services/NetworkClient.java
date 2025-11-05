package com.ltm.game.client.services;

import com.ltm.game.shared.Message;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class NetworkClient {
    private final Consumer<Message> onMessage;
    private PrintWriter out;
    private Socket socket;
    private BufferedReader reader;
    private final Map<String, Consumer<Message>> handlers = new ConcurrentHashMap<>();

    public NetworkClient(Consumer<Message> onMessage) {
        this.onMessage = onMessage;
    }

    public void connect() {
        try {
            Properties props = new Properties();
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("client-config.properties")) {
                if (in != null) props.load(in);
            }
            
            String host = System.getenv("SERVER_HOST");
            if (host == null) {
                host = props.getProperty("server.host", "127.0.0.1");
            }
            
            String portStr = System.getenv("SERVER_PORT");
            int port;
            if (portStr != null) {
                port = Integer.parseInt(portStr);
            } else {
                port = Integer.parseInt(props.getProperty("server.port", "5050"));
            }
            
            socket = new Socket(host, port);
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            
            Thread t = new Thread(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[NetworkClient] Received: " + line);
                        Message msg = Message.fromJson(line);
                        System.out.println("[NetworkClient] Parsed message type: " + msg.type);
                        Consumer<Message> handler = handlers.get(msg.type);
                        if (handler != null) {
                            System.out.println("[NetworkClient] Using specific handler for: " + msg.type);
                            handler.accept(msg);
                        } else {
                            System.out.println("[NetworkClient] Using default onMessage handler for: " + msg.type);
                            onMessage.accept(msg);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[NetworkClient] Reader thread error: " + e.getMessage());
                    e.printStackTrace();
                    Platform.runLater(() -> System.err.println("Disconnected: " + e.getMessage()));
                }
            }, "net-reader");
            t.setDaemon(true);
            t.start();
        } catch (Exception e) {
            throw new RuntimeException("Cannot connect to server: " + e.getMessage(), e);
        }
    }

    public void send(Message msg) {
        if (out == null) {
            System.err.println("[NetworkClient] ERROR: out is null! Cannot send message.");
            return;
        }
        String json = msg.toJson();
        System.out.println("[NetworkClient] Sending: " + json);
        out.println(json);
        if (out.checkError()) {
            System.err.println("[NetworkClient] ERROR: Failed to send message!");
        } else {
            System.out.println("[NetworkClient] Message sent successfully");
        }
    }

    public void addHandler(String type, Consumer<Message> handler) {
        handlers.put(type, handler);
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            System.err.println("[NetworkClient] Error closing socket: " + e.getMessage());
        }
        try { if (reader != null) reader.close(); } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
    }
}


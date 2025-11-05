package com.ltm.game.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class GameServer {
    public static void main(String[] args) throws Exception {
        Properties props = ServerProperties.load();
        int port = Integer.parseInt(props.getProperty("server.port", "5050"));
        LobbyService lobby = new LobbyService();
        GameService gameService = new GameService(lobby, props);
        QueueService queueService = new QueueService(gameService, lobby);

        Logger.info("Server starting on port " + port);
        Logger.info("Database URL: " + props.getProperty("db.url"));
        Logger.info("Content directory: " + props.getProperty("content.dir"));
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            Logger.info("Server successfully started and listening on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                Logger.debug("New client connection from " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket, lobby, gameService, queueService);
                new Thread(handler, "client-" + socket.getPort()).start();
            }
        } catch (Exception e) {
            Logger.error("Server error", e);
            throw e;
        }
    }
}

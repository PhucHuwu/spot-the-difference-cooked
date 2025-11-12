package com.ltm.game.server;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class GameServer {
    public static void main(String[] args) throws Exception {
        Properties props = ServerProperties.load();
        String host = props.getProperty("server.host", "0.0.0.0");
        int port = Integer.parseInt(props.getProperty("server.port", "5050"));
        LobbyService lobby = new LobbyService();
        GameService gameService = new GameService(lobby, props);
        QueueService queueService = new QueueService(gameService, lobby);

        InetAddress bindAddress = "0.0.0.0".equals(host) ? null : InetAddress.getByName(host);
        Logger.info("Server starting on " + host + ":" + port);
        Logger.info("Database URL: " + props.getProperty("db.url"));
        Logger.info("Content directory: " + props.getProperty("content.dir"));
        
        try (ServerSocket serverSocket = bindAddress == null 
                ? new ServerSocket(port) 
                : new ServerSocket(port, 50, bindAddress)) {
            Logger.info("Server successfully started and listening on " + host + ":" + port);
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

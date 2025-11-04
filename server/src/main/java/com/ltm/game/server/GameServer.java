package com.ltm.game.server;

import com.ltm.game.shared.Message;
import com.ltm.game.shared.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class GameServer {
    public static void main(String[] args) throws Exception {
        Properties props = ServerProperties.load();
        int port = Integer.parseInt(props.getProperty("server.port", "5050"));
        LobbyService lobby = new LobbyService();
        GameService gameService = new GameService(lobby, props);

        System.out.println("Server starting on port " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, lobby, gameService);
                new Thread(handler, "client-" + socket.getPort()).start();
            }
        }
    }
}

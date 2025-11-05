package com.ltm.game.client;

import com.ltm.game.shared.Message;
import com.ltm.game.shared.Protocol;
import com.ltm.game.client.controllers.*;
import com.ltm.game.client.services.AudioService;
import com.ltm.game.client.services.NetworkClient;
import com.ltm.game.client.views.GameView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class ClientApp extends Application {
    private Stage stage;
    private NetworkClient networkClient;
    private AudioService audioService;
    
    private String username;
    private int totalPoints;
    private int totalWins;
    private String currentRoomId;
    
    private LoginController loginController;
    private LobbyController lobbyController;
    private GameView gameView;
    private ResultController resultController;
    private LeaderboardController leaderboardController;
    
    private List<Map<String, Object>> pendingLobbyList;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        this.audioService = new AudioService();
        this.networkClient = new NetworkClient(this::onMessage);
        networkClient.connect();
        
        stage.setTitle("Spot The Difference");
        
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        stage.setWidth(screenBounds.getWidth());
        stage.setHeight(screenBounds.getHeight());
        
        showLogin();
        stage.show();
    }

    private void handleLogout() {
        System.out.println("[LOGOUT] Started");
        
        try {
            System.out.println("[LOGOUT] Resetting state...");
            this.username = null;
            this.totalPoints = 0;
            this.totalWins = 0;
            this.currentRoomId = null;
            this.pendingLobbyList = null;
            
            final NetworkClient oldClient = this.networkClient;
            
            System.out.println("[LOGOUT] Starting background disconnect and reconnect...");
            new Thread(() -> {
                try {
                    if (oldClient != null) {
                        oldClient.disconnect();
                        System.out.println("[LOGOUT] Old client disconnected");
                    }
                    Thread.sleep(300);
                    
                    this.networkClient = new NetworkClient(this::onMessage);
                    networkClient.connect();
                    System.out.println("[LOGOUT] New client connected");
                    
                    Platform.runLater(() -> {
                        System.out.println("[LOGOUT] Showing login screen on JavaFX thread");
                        showLogin();
                    });
                } catch (Exception e) {
                    System.err.println("[LOGOUT] Error in background: " + e.getMessage());
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        this.networkClient = new NetworkClient(this::onMessage);
                        try {
                            networkClient.connect();
                            showLogin();
                        } catch (Exception ex) {
                            System.err.println("[LOGOUT] Fallback failed: " + ex.getMessage());
                        }
                    });
                }
            }, "logout-thread").start();
            
            System.out.println("[LOGOUT] Logout initiated, waiting for reconnection...");
        } catch (Exception e) {
            System.err.println("[LOGOUT] Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showLogin() {
        try {
            System.out.println("[showLogin] Starting...");
            audioService.stopAll();
            System.out.println("[showLogin] Audio stopped");
            
            System.out.println("[showLogin] Loading FXML...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());
            System.out.println("[showLogin] FXML loaded");
            
            loginController = loader.getController();
            loginController.setNetworkClient(networkClient);
            loginController.setOnLoginSuccess((data) -> {
                this.username = data.username;
                this.totalPoints = data.totalPoints;
                this.totalWins = data.totalWins;
                showLobby();
            });
            System.out.println("[showLogin] Controller configured");
            
            stage.setScene(scene);
            System.out.println("[showLogin] Scene set - Login screen displayed");
        } catch (Exception e) {
            System.err.println("[showLogin] ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showLobby() {
        try {
            audioService.stopGameMusic();
            // audioService.playBackgroundMusic(); // Tắt nhạc sảnh
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/lobby.fxml"));
            Scene scene = new Scene(loader.load());
            
            lobbyController = loader.getController();
            lobbyController.setNetworkClient(networkClient);
            lobbyController.setAudioService(audioService);
            lobbyController.setUsername(username);
            lobbyController.setTotalPoints(totalPoints);
            lobbyController.setTotalWins(totalWins);
            lobbyController.setStatus("Online");
            
            lobbyController.setOnLogout(v -> {
                handleLogout();
            });
            
            lobbyController.setOnShowLeaderboard(v -> showLeaderboard());
            
            stage.setScene(scene);
            
            if (pendingLobbyList != null) {
                lobbyController.updateLobbyList(pendingLobbyList);
                pendingLobbyList = null;
            }
            
            // Request fresh lobby list and leaderboard from server
            networkClient.send(new Message(Protocol.LOBBY_REQUEST, null));
            lobbyController.requestLeaderboardData();
        } catch (Exception e) {
            System.err.println("Error loading lobby view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showGame() {
        audioService.stopBackgroundMusic();
        
        // Update status to In-game
        if (lobbyController != null) {
            lobbyController.setStatus("In-game");
        }
        
        gameView = new GameView((x, y) -> {
            System.out.println("onClick callback triggered: x=" + x + ", y=" + y + ", currentRoomId=" + currentRoomId);
            if (currentRoomId != null) {
                System.out.println("Sending GAME_CLICK to server...");
                networkClient.send(new Message(Protocol.GAME_CLICK, Map.of("roomId", currentRoomId, "x", x, "y", y)));
            } else {
                System.err.println("currentRoomId is null - cannot send click!");
            }
        }, username, audioService);
        
        stage.setScene(new Scene(gameView.getRoot()));
    }

    private void showResult(Map<?, ?> payload) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/result.fxml"));
            Scene scene = new Scene(loader.load());
            
            resultController = loader.getController();
            resultController.setAudioService(audioService);
            resultController.setNetworkClient(networkClient);
            resultController.setOnBackToLobby(v -> showLobby());
            resultController.setOnShowLeaderboard(v -> showLeaderboard());
            resultController.setOnRematch(opponent -> {
                showLobby();
            });
            resultController.setGameResult(payload, username);
            
            stage.setScene(scene);
        } catch (Exception e) {
            System.err.println("Error loading result view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showLeaderboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leaderboard.fxml"));
            Scene scene = new Scene(loader.load());
            
            leaderboardController = loader.getController();
            leaderboardController.setNetworkClient(networkClient);
            leaderboardController.setOnBack(v -> showLobby());
            leaderboardController.requestLeaderboardData();
            
            stage.setScene(scene);
        } catch (Exception e) {
            System.err.println("Error loading leaderboard view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onMessage(Message msg) {
        Platform.runLater(() -> {
            switch (msg.type) {
                case Protocol.AUTH_RESULT -> {
                    if (loginController != null) {
                        loginController.handleAuthResult((Map<?, ?>) msg.payload);
                    }
                }
                case Protocol.LOBBY_LIST -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> list = (List<Map<String, Object>>) msg.payload;
                    if (lobbyController == null) {
                        pendingLobbyList = list;
                    } else {
                        lobbyController.updateLobbyList(list);
                    }
                }
                case Protocol.INVITE_RECEIVED -> {
                    String from = String.valueOf(((Map<?, ?>) msg.payload).get("fromUser"));
                    if (lobbyController != null) {
                        lobbyController.showInviteDialog(from);
                    }
                }
                case Protocol.INVITE_RESPONSE -> {
                    boolean accepted = Boolean.parseBoolean(String.valueOf(((Map<?, ?>) msg.payload).get("accepted")));
                    if (!accepted && lobbyController != null) {
                        lobbyController.showInviteRejected();
                    }
                }
                case Protocol.QUEUE_MATCHED -> {
                    String opponent = String.valueOf(((Map<?, ?>) msg.payload).get("opponent"));
                    if (lobbyController != null) {
                        lobbyController.onQueueMatched(opponent);
                    }
                }
                case Protocol.GAME_START -> {
                    Map<?, ?> p = (Map<?, ?>) msg.payload;
                    currentRoomId = String.valueOf(p.get("roomId"));
                    showGame();
                    
                    try {
                        String b64L = (String) p.get("imgLeft");
                        String b64R = (String) p.get("imgRight");
                        Integer w = p.get("imageWidth") != null ? ((Number) p.get("imageWidth")).intValue() : 0;
                        Integer h = p.get("imageHeight") != null ? ((Number) p.get("imageHeight")).intValue() : 0;
                        System.out.println("GAME_START: imgLeft=" + (b64L != null ? b64L.length() : "null") + " chars, imgRight=" + (b64R != null ? b64R.length() : "null") + " chars, w=" + w + ", h=" + h);
                        byte[] leftBytes = (b64L != null) ? java.util.Base64.getDecoder().decode(b64L) : null;
                        byte[] rightBytes = (b64R != null) ? java.util.Base64.getDecoder().decode(b64R) : null;
                        System.out.println("Decoded: leftBytes=" + (leftBytes != null ? leftBytes.length : "null") + " bytes, rightBytes=" + (rightBytes != null ? rightBytes.length : "null") + " bytes");
                        if (gameView != null) {
                            gameView.setImages(leftBytes, rightBytes, w, h);
                            System.out.println("Called gameView.setImages()");
                        } else {
                            System.err.println("gameView is null!");
                        }
                    } catch (Exception e) {
                        System.err.println("Error decoding images: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                case Protocol.GAME_UPDATE -> {
                    if (gameView != null) {
                        gameView.updateFromPayload((Map<?, ?>) msg.payload);
                    }
                }
                case Protocol.GAME_END -> {
                    if (gameView != null) {
                        gameView.updateFromPayload((Map<?, ?>) msg.payload);
                    }
                    showResult((Map<?, ?>) msg.payload);
                }
                case Protocol.LEADERBOARD -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> entries = (List<Map<String, Object>>) msg.payload;
                    if (lobbyController != null) {
                        lobbyController.updateLeaderboard(entries);
                    }
                    if (leaderboardController != null) {
                        leaderboardController.updateLeaderboard(entries);
                    }
                }
                case Protocol.ERROR -> {
                    String errorMsg = msg.payload != null ? String.valueOf(((Map<?, ?>) msg.payload).get("message")) : "Unknown error";
                    System.out.println("Error from server: " + errorMsg);
                }
            }
        });
    }
}


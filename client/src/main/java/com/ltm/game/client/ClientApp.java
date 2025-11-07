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
    private MatchHistoryController matchHistoryController;
    
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
        stage.setUserData(this); // Set ClientApp reference for controllers to access
        
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        stage.setWidth(screenBounds.getWidth());
        stage.setHeight(screenBounds.getHeight());
        
        stage.setOnCloseRequest(event -> {
            Platform.exit();
        });
        
        showLogin();
        stage.show();
    }

    @Override
    public void stop() {
        cleanup();
    }

    private void cleanup() {
        if (audioService != null) {
            audioService.stopBackgroundMusic();
            audioService.stopGameMusic();
        }
        if (networkClient != null) {
            networkClient.disconnect();
        }
    }

    private void handleLogout() {
        // Send logout message to server
        if (networkClient != null) {
            networkClient.send(new Message(Protocol.AUTH_LOGOUT, null));
        }
        
        this.username = null;
        this.totalPoints = 0;
        this.totalWins = 0;
        this.currentRoomId = null;
        this.pendingLobbyList = null;
        this.lobbyController = null;
        this.gameView = null;
        this.resultController = null;
        this.leaderboardController = null;

        audioService.stopAll();
        showLogin();
    }
    
    private void showLogin() {
        try {
            audioService.stopAll();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());

            loginController = loader.getController();
            loginController.setNetworkClient(networkClient);
            loginController.setOnLoginSuccess((data) -> {
                this.username = data.username;
                this.totalPoints = data.totalPoints;
                this.totalWins = data.totalWins;
                showLobby();
            });

            stage.setScene(scene);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load login screen", e);
        }
    }

    private void showLobby() {
        try {
            audioService.stopGameMusic();
            audioService.playLobbyMusic();

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

            networkClient.send(new Message(Protocol.LOBBY_REQUEST, null));
            lobbyController.requestLeaderboardData();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load lobby screen", e);
        }
    }

    private void showGame() {
        audioService.stopBackgroundMusic();

        if (lobbyController != null) {
            lobbyController.setStatus("In-game");
        }

        gameView = new GameView((x, y) -> {
            if (currentRoomId != null) {
                networkClient.send(new Message(Protocol.GAME_CLICK, Map.of("roomId", currentRoomId, "x", x, "y", y)));
            }
        }, username, audioService, () -> {
            // Handle quit game
            if (currentRoomId != null) {
                networkClient.send(new Message(Protocol.GAME_QUIT, Map.of("roomId", currentRoomId)));
            }
        });

        stage.setScene(new Scene(gameView.getRoot()));
    }

    private void showResult(Map<?, ?> payload) {
        try {
            // Stop all game audio when showing result
            audioService.stopAll();
            
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
            throw new RuntimeException("Failed to load result screen", e);
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
            throw new RuntimeException("Failed to load leaderboard screen", e);
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
                case Protocol.MATCH_WAITING -> {
                    if (lobbyController != null) {
                        lobbyController.onMatchWaiting();
                    }
                }
                case Protocol.MATCH_READY -> {
                    if (lobbyController != null) {
                        lobbyController.onMatchReady();
                    }
                }
                case Protocol.MATCH_DECLINE -> {
                    if (lobbyController != null) {
                        lobbyController.onMatchDeclined((Map<?, ?>) msg.payload);
                    }
                }
                case Protocol.GAME_START -> {
                    Map<?, ?> p = (Map<?, ?>) msg.payload;
                    currentRoomId = String.valueOf(p.get("roomId"));

                    // Ensure Match Found dialog is closed before showing game
                    if (lobbyController != null) {
                        lobbyController.ensureMatchDialogClosed();
                    }

                    showGame();

                    try {
                        String b64L = (String) p.get("imgLeft");
                        String b64R = (String) p.get("imgRight");
                        Integer w = p.get("imageWidth") != null ? ((Number) p.get("imageWidth")).intValue() : 0;
                        Integer h = p.get("imageHeight") != null ? ((Number) p.get("imageHeight")).intValue() : 0;
                        byte[] leftBytes = (b64L != null) ? java.util.Base64.getDecoder().decode(b64L) : null;
                        byte[] rightBytes = (b64R != null) ? java.util.Base64.getDecoder().decode(b64R) : null;
                        if (gameView != null) {
                            gameView.setImages(leftBytes, rightBytes, w, h);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to decode game images", e);
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
                case Protocol.MATCH_HISTORY_DATA -> {
                    if (matchHistoryController != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) msg.payload;
                        matchHistoryController.handleMatchHistoryData(data);
                    }
                }
                case Protocol.ERROR -> {
                }
            }
        });
    }
    
    public void setMatchHistoryController(MatchHistoryController controller) {
        this.matchHistoryController = controller;
    }
}


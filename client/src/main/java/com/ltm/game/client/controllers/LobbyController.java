package com.ltm.game.client.controllers;

import com.ltm.game.shared.Message;
import com.ltm.game.shared.Protocol;
import com.ltm.game.client.models.LobbyUserRow;
import com.ltm.game.client.models.LeaderboardRow;
import com.ltm.game.client.services.NetworkClient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.util.Callback;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class LobbyController {
    @FXML
    private Label headerUserInfo;
    
    @FXML
    private Label statsWinsLabel;
    
    @FXML
    private Label statsPointsLabel;
    
    @FXML
    private Label rankIconLabel;
    
    @FXML
    private Label rankPositionLabel;
    
    @FXML
    private Label rankDescriptionLabel;
    
    @FXML
    private TableView<LobbyUserRow> lobbyTable;
    
    @FXML
    private TableColumn<LobbyUserRow, String> colUser;
    
    @FXML
    private TableColumn<LobbyUserRow, String> colPoints;
    
    @FXML
    private TableColumn<LobbyUserRow, String> colStatus;
    
    @FXML
    private TableColumn<LobbyUserRow, Void> colAction;
    
    @FXML
    private TextField searchField;
    
    @FXML
    private BorderPane rootPane;

    @FXML
    private TableView<LeaderboardRow> leaderboardTable;

    @FXML
    private TableColumn<LeaderboardRow, String> colRank;

    @FXML
    private TableColumn<LeaderboardRow, String> colPlayer;

    @FXML
    private TableColumn<LeaderboardRow, String> colScore;

    @FXML
    private TableColumn<LeaderboardRow, String> colWins;

    private ObservableList<LobbyUserRow> lobbyData = FXCollections.observableArrayList();
    private FilteredList<LobbyUserRow> filteredLobby;
    private ObservableList<LeaderboardRow> leaderboardData = FXCollections.observableArrayList();

    private NetworkClient networkClient;
    private com.ltm.game.client.services.AudioService audioService;
    private String username;
    private String myPoints = "0";
    private String myWins = "0";
    private String myStatus = "Online";

    private Consumer<Void> onLogout;
    private Consumer<Void> onShowLeaderboard;

    public void setNetworkClient(NetworkClient client) {
        this.networkClient = client;
    }
    
    public void setAudioService(com.ltm.game.client.services.AudioService audioService) {
        this.audioService = audioService;
    }

    public void setUsername(String username) {
        this.username = username;
        updateHeaderUserInfo();
    }

    public void setTotalPoints(int points) {
        this.myPoints = String.valueOf(points);
        updateHeaderUserInfo();
        updateStatsDisplay();
    }

    public void setTotalWins(int wins) {
        this.myWins = String.valueOf(wins);
        updateStatsDisplay();
    }

    public void setStatus(String status) {
        this.myStatus = status;
        updateHeaderUserInfo();
    }

    public void setOnLogout(Consumer<Void> callback) {
        this.onLogout = callback;
    }

    public void setOnShowLeaderboard(Consumer<Void> callback) {
        this.onShowLeaderboard = callback;
    }

    @FXML
    private void initialize() {
        String cssPath = getClass().getResource("/styles/lobby-tables.css").toExternalForm();
        
        filteredLobby = new FilteredList<>(lobbyData, r -> true);
        lobbyTable.setItems(filteredLobby);
        lobbyTable.getStylesheets().add(cssPath);

        colUser.setCellValueFactory(c -> c.getValue().usernameProperty());
        colUser.setCellFactory(col -> new TableCell<LobbyUserRow, String>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) {
                    setText(null);
                    getStyleClass().remove("player-name-self");
                    getStyleClass().remove("player-name");
                } else {
                    setText(name);
                    getStyleClass().removeAll("player-name-self", "player-name");
                    if (name.equals(username)) {
                        getStyleClass().add("player-name-self");
                    } else {
                        getStyleClass().add("player-name");
                    }
                }
            }
        });
        
        colPoints.setCellValueFactory(c -> c.getValue().totalPointsProperty());
        colPoints.setCellFactory(col -> new TableCell<LobbyUserRow, String>() {
            @Override
            protected void updateItem(String points, boolean empty) {
                super.updateItem(points, empty);
                if (empty || points == null) {
                    setText(null);
                    getStyleClass().remove("points-cell");
                } else {
                    setText(points);
                    if (!getStyleClass().contains("points-cell")) {
                        getStyleClass().add("points-cell");
                    }
                }
            }
        });
        
        colStatus.setCellValueFactory(c -> c.getValue().statusProperty());
        colStatus.setCellFactory(col -> new TableCell<LobbyUserRow, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    getStyleClass().removeAll("status-online", "status-ingame");
                } else {
                    setText(status);
                    getStyleClass().removeAll("status-online", "status-ingame");
                    if (status.equals("Online")) {
                        getStyleClass().add("status-online");
                    } else if (status.equals("In-game")) {
                        getStyleClass().add("status-ingame");
                    }
                }
            }
        });
        colAction.setCellFactory(makeActionCellFactory());

        leaderboardTable.setItems(leaderboardData);
        leaderboardTable.getStylesheets().add(cssPath);
        
        colRank.setCellValueFactory(c -> c.getValue().rankProperty());
        colRank.setCellFactory(col -> new TableCell<LeaderboardRow, String>() {
            @Override
            protected void updateItem(String rank, boolean empty) {
                super.updateItem(rank, empty);
                if (empty || rank == null) {
                    setText(null);
                    getStyleClass().removeAll("rank-1", "rank-2", "rank-3", "rank-top10");
                } else {
                    setText(rank);
                    getStyleClass().removeAll("rank-1", "rank-2", "rank-3", "rank-top10");
                    try {
                        int rankNum = Integer.parseInt(rank);
                        if (rankNum == 1) {
                            getStyleClass().add("rank-1");
                        } else if (rankNum == 2) {
                            getStyleClass().add("rank-2");
                        } else if (rankNum == 3) {
                            getStyleClass().add("rank-3");
                        } else if (rankNum <= 10) {
                            getStyleClass().add("rank-top10");
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        });
        
        colPlayer.setCellValueFactory(c -> c.getValue().usernameProperty());
        colPlayer.setCellFactory(col -> new TableCell<LeaderboardRow, String>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) {
                    setText(null);
                    getStyleClass().removeAll("player-name-self", "player-name");
                } else {
                    setText(name);
                    getStyleClass().removeAll("player-name-self", "player-name");
                    if (name.equals(username)) {
                        getStyleClass().add("player-name-self");
                    } else {
                        getStyleClass().add("player-name");
                    }
                }
            }
        });
        
        colScore.setCellValueFactory(c -> c.getValue().totalPointsProperty());
        colScore.setCellFactory(col -> new TableCell<LeaderboardRow, String>() {
            @Override
            protected void updateItem(String score, boolean empty) {
                super.updateItem(score, empty);
                if (empty || score == null) {
                    setText(null);
                    getStyleClass().remove("points-cell");
                } else {
                    setText(score);
                    if (!getStyleClass().contains("points-cell")) {
                        getStyleClass().add("points-cell");
                    }
                }
            }
        });
        
        colWins.setCellValueFactory(c -> c.getValue().totalWinsProperty());

        searchField.textProperty().addListener((obs, old, q) -> {
            String query = q == null ? "" : q.trim().toLowerCase();
            filteredLobby.setPredicate(row ->
                query.isEmpty() || row.getUsername().toLowerCase().contains(query));
        });
    }

    @FXML
    private void handleLogout() {
        myStatus = "Online";
        updateHeaderUserInfo();
        if (onLogout != null) {
            onLogout.accept(null);
        }
    }

    @FXML
    private void handleFindMatch() {
        networkClient.send(new Message(Protocol.QUEUE_JOIN, null));
        showQueueDialog();
    }

    @FXML
    private void handleShowLeaderboard() {
        if (onShowLeaderboard != null) {
            onShowLeaderboard.accept(null);
        }
    }
    
    private void showStyledAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
    
    private Stage queueDialog;
    private Label queueTimerLabel;
    private javafx.animation.Timeline queueTimer;
    private javafx.animation.Timeline outerRingAnimation;
    private javafx.animation.Timeline innerRingAnimation;
    private javafx.animation.Timeline pulseAnimation;
    private int queueWaitSeconds = 0;
    
    private void showQueueDialog() {
        if (queueDialog != null && queueDialog.isShowing()) {
            return;
        }
        
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/queue-dialog.fxml")
            );
            javafx.scene.Parent root = loader.load();
            
            queueTimerLabel = (Label) root.lookup("#queueTimerLabel");
            Button cancelButton = (Button) root.lookup("#cancelQueueButton");
            
            if (cancelButton != null) {
                cancelButton.setOnAction(e -> leaveQueue());
            }
            
            queueDialog = new Stage();
            queueDialog.initModality(Modality.APPLICATION_MODAL);
            queueDialog.setTitle("Đang tìm trận");
            queueDialog.setResizable(false);
            queueDialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            queueDialog.setOnCloseRequest(e -> {
                e.consume();
                leaveQueue();
            });
            
            Scene dialogScene = new Scene(root);
            dialogScene.setFill(Color.TRANSPARENT);
            dialogScene.getStylesheets().add(
                getClass().getResource("/styles/queue-dialog.css").toExternalForm()
            );
            queueDialog.setScene(dialogScene);
            
            startQueueTimer();
            startQueueAnimations(root);
            
            queueDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            showStyledAlert("Lỗi", "Không thể hiển thị hàng chờ: " + e.getMessage(), 
                Alert.AlertType.ERROR);
        }
    }
    
    private void startQueueTimer() {
        queueWaitSeconds = 0;
        queueTimer = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                queueWaitSeconds++;
                if (queueTimerLabel != null) {
                    int minutes = queueWaitSeconds / 60;
                    int seconds = queueWaitSeconds % 60;
                    queueTimerLabel.setText(String.format("%02d:%02d", minutes, seconds));
                }
            })
        );
        queueTimer.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        queueTimer.play();
    }
    
    private void startQueueAnimations(javafx.scene.Parent root) {
        javafx.scene.Node outerRing = root.lookup(".search-ring-outer");
        javafx.scene.Node innerRing = root.lookup(".search-ring-inner");
        javafx.scene.Node middleRing = root.lookup(".search-ring-middle");
        javafx.scene.Node statusIndicator = root.lookup(".status-indicator");
        
        if (outerRing != null) {
            outerRingAnimation = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                    new javafx.animation.KeyValue(outerRing.rotateProperty(), 0)),
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(20),
                    new javafx.animation.KeyValue(outerRing.rotateProperty(), 360))
            );
            outerRingAnimation.setCycleCount(javafx.animation.Timeline.INDEFINITE);
            outerRingAnimation.play();
        }
        
        if (innerRing != null) {
            innerRingAnimation = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                    new javafx.animation.KeyValue(innerRing.rotateProperty(), 0)),
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(8),
                    new javafx.animation.KeyValue(innerRing.rotateProperty(), -360))
            );
            innerRingAnimation.setCycleCount(javafx.animation.Timeline.INDEFINITE);
            innerRingAnimation.play();
        }
        
        if (middleRing != null) {
            pulseAnimation = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                    new javafx.animation.KeyValue(middleRing.scaleXProperty(), 1.0),
                    new javafx.animation.KeyValue(middleRing.scaleYProperty(), 1.0),
                    new javafx.animation.KeyValue(middleRing.opacityProperty(), 0.5)),
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1.5),
                    new javafx.animation.KeyValue(middleRing.scaleXProperty(), 1.1),
                    new javafx.animation.KeyValue(middleRing.scaleYProperty(), 1.1),
                    new javafx.animation.KeyValue(middleRing.opacityProperty(), 0.8)),
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3),
                    new javafx.animation.KeyValue(middleRing.scaleXProperty(), 1.0),
                    new javafx.animation.KeyValue(middleRing.scaleYProperty(), 1.0),
                    new javafx.animation.KeyValue(middleRing.opacityProperty(), 0.5))
            );
            pulseAnimation.setCycleCount(javafx.animation.Timeline.INDEFINITE);
            pulseAnimation.play();
        }
        
        if (statusIndicator != null) {
            javafx.animation.Timeline blinkAnimation = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                    new javafx.animation.KeyValue(statusIndicator.opacityProperty(), 1.0)),
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(0.6),
                    new javafx.animation.KeyValue(statusIndicator.opacityProperty(), 0.3)),
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1.2),
                    new javafx.animation.KeyValue(statusIndicator.opacityProperty(), 1.0))
            );
            blinkAnimation.setCycleCount(javafx.animation.Timeline.INDEFINITE);
            blinkAnimation.play();
        }
    }
    
    private void stopQueueAnimations() {
        if (outerRingAnimation != null) {
            outerRingAnimation.stop();
            outerRingAnimation = null;
        }
        if (innerRingAnimation != null) {
            innerRingAnimation.stop();
            innerRingAnimation = null;
        }
        if (pulseAnimation != null) {
            pulseAnimation.stop();
            pulseAnimation = null;
        }
    }
    
    private void leaveQueue() {
        if (queueTimer != null) {
            queueTimer.stop();
        }
        stopQueueAnimations();
        networkClient.send(new Message(Protocol.QUEUE_LEAVE, null));
        if (queueDialog != null) {
            queueDialog.close();
            queueDialog = null;
        }
    }
    
    private Stage matchFoundDialog;
    private MatchFoundController matchFoundController;

    public void onQueueMatched(String opponent) {
        if (queueTimer != null) {
            queueTimer.stop();
        }
        stopQueueAnimations();
        if (queueDialog != null) {
            queueDialog.close();
            queueDialog = null;
        }

        javafx.application.Platform.runLater(() -> {
            showMatchFoundDialog(opponent);
        });
    }

    private void showMatchFoundDialog(String opponent) {
        if (matchFoundDialog != null && matchFoundDialog.isShowing()) {
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/match-found.fxml")
            );
            javafx.scene.Parent root = loader.load();

            matchFoundController = loader.getController();
            matchFoundController.setNetworkClient(networkClient);
            matchFoundController.setUsername(username);
            matchFoundController.setOpponentName(opponent);

            matchFoundDialog = new Stage();
            matchFoundDialog.initModality(Modality.APPLICATION_MODAL);
            matchFoundDialog.setTitle("Match Found");
            matchFoundDialog.setResizable(false);
            matchFoundDialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            matchFoundDialog.setOnCloseRequest(e -> {
                e.consume();
            });

            matchFoundController.setDialogStage(matchFoundDialog);

            Scene dialogScene = new Scene(root);
            dialogScene.setFill(Color.TRANSPARENT);
            dialogScene.getStylesheets().add(
                getClass().getResource("/styles/match-found-dialog.css").toExternalForm()
            );
            matchFoundDialog.setScene(dialogScene);

            matchFoundController.startCountdown();

            matchFoundDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            showStyledAlert("Lỗi", "Không thể hiển thị Match Found dialog: " + e.getMessage(),
                Alert.AlertType.ERROR);
        }
    }

    public void onMatchReady() {
        javafx.application.Platform.runLater(() -> {
            if (matchFoundController != null) {
                matchFoundController.onMatchStarting();
            }
        });
    }

    public void onMatchDeclined(Map<?, ?> payload) {
        javafx.application.Platform.runLater(() -> {
            String reason = payload != null ? String.valueOf(payload.get("reason")) : "";
            String decliner = payload != null ? String.valueOf(payload.get("decliner")) : "";

            if (matchFoundController != null) {
                matchFoundController.onOpponentDeclined(reason, decliner);
            } else {
                String message = "Match cancelled";
                if ("timeout".equals(reason)) {
                    message = "⏱ Match cancelled - No one accepted in time";
                } else if (decliner != null && !decliner.isEmpty() && !"null".equals(decliner)) {
                    message = "❌ " + decliner + " declined the match";
                }

                showStyledAlert("Match Cancelled", message, Alert.AlertType.WARNING);
            }
        });
    }

    public void ensureMatchDialogClosed() {
        javafx.application.Platform.runLater(() -> {
            if (matchFoundController != null) {
                matchFoundController.closeDialog();
                matchFoundController = null;
            }
            if (matchFoundDialog != null && matchFoundDialog.isShowing()) {
                matchFoundDialog.close();
                matchFoundDialog = null;
            }
        });
    }
    

    public void updateLobbyList(List<Map<String, Object>> list) {
        lobbyData.clear();
        for (var u : list) {
            String name = String.valueOf(u.get("username"));
            String pts = String.valueOf(u.get("totalPoints"));
            String st = String.valueOf(u.get("status"));
            if (name != null && name.equals(username)) continue;
            lobbyData.add(new LobbyUserRow(name, pts, formatStatus(st)));
        }
    }

    private String formatStatus(String status) {
        if (status == null) return "Online";
        switch (status.toUpperCase()) {
            case "IDLE":
                return "Online";
            case "IN_GAME":
                return "In-game";
            default:
                return status;
        }
    }

    public void updateLeaderboard(List<Map<String, Object>> entries) {
        leaderboardData.clear();
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            String rank = String.valueOf(i + 1);
            String playerName = String.valueOf(entry.get("username"));
            String totalPoints = String.valueOf(entry.get("totalPoints"));
            String totalWins = String.valueOf(entry.get("totalWins"));
            leaderboardData.add(new LeaderboardRow(rank, playerName, totalPoints, totalWins));
            
            if (playerName.equals(username)) {
                this.myPoints = totalPoints;
                this.myWins = totalWins;
                int myRank = i + 1;
                javafx.application.Platform.runLater(() -> {
                    updateHeaderUserInfo();
                    updateStatsDisplay();
                    updateRankDisplay(myRank);
                });
            }
        }
    }

    public void requestLeaderboardData() {
        if (networkClient != null) {
            networkClient.send(new Message(Protocol.LEADERBOARD, null));
        }
    }

    public void showInviteDialog(String fromUser) {
        Stage inviteDialog = new Stage();
        inviteDialog.initModality(Modality.APPLICATION_MODAL);
        inviteDialog.setTitle("Lời mời thi đấu");
        inviteDialog.setResizable(false);
        
        VBox dialogContent = new VBox(20);
        dialogContent.setAlignment(Pos.CENTER);
        dialogContent.setPadding(new Insets(50, 60, 50, 60));
        dialogContent.setStyle(
            "-fx-background-color: transparent;"
        );
        
        Label iconLabel = new Label("⚔️");
        iconLabel.setStyle(
            "-fx-font-size: 56px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 10, 0.7, 0, 2);"
        );
        
        Label headerLabel = new Label("Lời mời thi đấu");
        headerLabel.setStyle(
            "-fx-font-size: 28px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #2c3e50;" +
            "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.8), 8, 0.7, 0, 1);"
        );
        
        Label messageLabel = new Label(fromUser + " mời bạn thi đấu!");
        messageLabel.setStyle(
            "-fx-font-size: 20px;" +
            "-fx-text-fill: #34495e;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 18px 25px;" +
            "-fx-background-color: rgba(255,255,255,0.9);" +
            "-fx-background-radius: 15px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0.6, 0, 3);"
        );
        
        Label countdownLabel = new Label("10");
        countdownLabel.setStyle(
            "-fx-font-size: 36px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #e74c3c;" +
            "-fx-padding: 15px 25px;" +
            "-fx-background-color: rgba(255,255,255,0.95);" +
            "-fx-background-radius: 50px;" +
            "-fx-effect: dropshadow(gaussian, rgba(231,76,60,0.6), 12, 0.8, 0, 4);"
        );
        
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button acceptBtn = new Button("✓ Chấp nhận");
        acceptBtn.setStyle(
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;" +
            "-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9);" +
            "-fx-background-radius: 30px;" +
            "-fx-padding: 14px 35px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(52,152,219,0.6), 10, 0.7, 0, 4);"
        );
        
        Button declineBtn = new Button("✗ Từ chối");
        declineBtn.setStyle(
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;" +
            "-fx-background-color: linear-gradient(to bottom, #95a5a6, #7f8c8d);" +
            "-fx-background-radius: 30px;" +
            "-fx-padding: 14px 35px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(149,165,166,0.6), 10, 0.7, 0, 4);"
        );
        
        final int[] countdown = {10};
        
        Runnable autoDecline = () -> {
            networkClient.send(new Message(Protocol.INVITE_RESPONSE, Map.of("fromUser", fromUser, "accepted", false)));
            inviteDialog.close();
        };
        
        Timeline[] countdownTimerRef = new Timeline[1];
        countdownTimerRef[0] = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> {
                countdown[0]--;
                if (countdown[0] > 0) {
                    countdownLabel.setText(String.valueOf(countdown[0]));
                    if (countdown[0] <= 3) {
                        countdownLabel.setStyle(
                            "-fx-font-size: 42px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-text-fill: #e74c3c;" +
                            "-fx-padding: 15px 25px;" +
                            "-fx-background-color: rgba(255,255,255,0.95);" +
                            "-fx-background-radius: 50px;" +
                            "-fx-effect: dropshadow(gaussian, rgba(231,76,60,0.8), 15, 0.9, 0, 5);"
                        );
                    }
                } else {
                    countdownTimerRef[0].stop();
                    autoDecline.run();
                }
            })
        );
        countdownTimerRef[0].setCycleCount(10);
        
        Timeline countdownTimer = countdownTimerRef[0];
        
        acceptBtn.setOnAction(e -> {
            countdownTimer.stop();
            networkClient.send(new Message(Protocol.INVITE_RESPONSE, Map.of("fromUser", fromUser, "accepted", true)));
            inviteDialog.close();
        });
        
        declineBtn.setOnAction(e -> {
            countdownTimer.stop();
            autoDecline.run();
        });
        
        countdownTimer.play();
        
        buttonBox.getChildren().addAll(acceptBtn, declineBtn);
        dialogContent.getChildren().addAll(iconLabel, headerLabel, messageLabel, countdownLabel, buttonBox);
        
        Scene dialogScene = new Scene(dialogContent);
        dialogScene.setFill(Color.TRANSPARENT);
        inviteDialog.setScene(dialogScene);
        inviteDialog.show();
    }

    public void showInviteRejected() {
        Stage notifyDialog = new Stage();
        notifyDialog.initModality(Modality.APPLICATION_MODAL);
        notifyDialog.setTitle("Thông báo");
        notifyDialog.setResizable(false);
        
        VBox notifyContent = new VBox(20);
        notifyContent.setAlignment(Pos.CENTER);
        notifyContent.setPadding(new Insets(30, 40, 30, 40));
        notifyContent.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #f093fb, #f5576c);" +
            "-fx-background-radius: 15px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0.8, 0, 5);"
        );
        
        Label iconLabel = new Label("😔");
        iconLabel.setStyle("-fx-font-size: 40px;");
        
        Label messageLabel = new Label("Lời mời bị từ chối");
        messageLabel.setStyle(
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0.5, 0, 2);"
        );
        
        Button okBtn = new Button("OK");
        okBtn.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;" +
            "-fx-background-color: rgba(255,255,255,0.3);" +
            "-fx-background-radius: 20px;" +
            "-fx-padding: 10px 25px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0.5, 0, 2);"
        );
        okBtn.setOnAction(e -> notifyDialog.close());
        
        notifyContent.getChildren().addAll(iconLabel, messageLabel, okBtn);
        
        Scene notifyScene = new Scene(notifyContent);
        notifyScene.setFill(Color.TRANSPARENT);
        notifyDialog.setScene(notifyScene);
        notifyDialog.show();
    }

    private void updateHeaderUserInfo() {
        if (headerUserInfo != null) {
            headerUserInfo.setText("Xin chào, " + username + "  •  Tổng điểm: " + myPoints + "  •  " + myStatus);
        }
    }

    private void updateStatsDisplay() {
        if (statsPointsLabel != null) {
            statsPointsLabel.setText(myPoints);
        }
        if (statsWinsLabel != null) {
            statsWinsLabel.setText(myWins);
        }
    }
    
    private void updateRankDisplay(int rank) {
        if (rankPositionLabel != null) {
            rankPositionLabel.setText("#" + rank);
        }
        
        if (rankIconLabel != null && rankDescriptionLabel != null) {
            String icon;
            String description;
            
            if (rank == 1) {
                icon = "🥇";
                description = "Top 1 - Huyền thoại";
            } else if (rank == 2) {
                icon = "🥈";
                description = "Top 2 - Cao thủ";
            } else if (rank == 3) {
                icon = "🥉";
                description = "Top 3 - Tinh anh";
            } else if (rank <= 10) {
                icon = "💎";
                description = "Top 10 - Kim cương";
            } else if (rank <= 50) {
                icon = "⭐";
                description = "Top 50 - Vàng";
            } else {
                icon = "🎮";
                description = "Vị trí: " + rank;
            }
            
            rankIconLabel.setText(icon);
            rankDescriptionLabel.setText(description);
        }
    }

    private Callback<TableColumn<LobbyUserRow, Void>, TableCell<LobbyUserRow, Void>> makeActionCellFactory() {
        return col -> new TableCell<>() {
            private final Button inviteBtn = new Button("Mời");
            private final HBox box = new HBox(6, inviteBtn);
            
            {
                inviteBtn.setOnAction(e -> {
                    LobbyUserRow row = getTableView().getItems().get(getIndex());
                    if (!row.getUsername().equals(username)) {
                        networkClient.send(new Message(Protocol.INVITE_SEND, Map.of("toUser", row.getUsername())));
                    }
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        };
    }
}


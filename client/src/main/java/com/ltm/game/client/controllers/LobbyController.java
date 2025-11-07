package com.ltm.game.client.controllers;

import com.ltm.game.shared.Message;
import com.ltm.game.shared.Protocol;
import com.ltm.game.client.ClientApp;
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
                getClass().getResource("/fxml/queue-dialog-lol.fxml")
            );
            javafx.scene.Parent root = loader.load();
            
            queueTimerLabel = (Label) root.lookup("#queueTimerLabel");
            Label titleLabel = (Label) root.lookup("#titleLabel");
            Button cancelButton = (Button) root.lookup("#cancelQueueButton");
            VBox cancelButtonContainer = (VBox) root.lookup("#cancelButtonContainer");
            VBox actionButtons = (VBox) root.lookup("#actionButtons");
            
            // Set initial state: Searching
            if (titleLabel != null) {
                titleLabel.setText("ĐANG TÌM TRẬN");
            }
            if (queueTimerLabel != null) {
                queueTimerLabel.setText("Đang tìm đối thủ...");
            }
            
            // Get circle nodes for animation
            javafx.scene.shape.Circle outerRing = (javafx.scene.shape.Circle) root.lookup("#outerRing");
            javafx.scene.shape.Circle innerRing = (javafx.scene.shape.Circle) root.lookup("#innerRing");
            
            if (cancelButton != null) {
                cancelButton.setOnAction(e -> leaveQueue());
            }
            
            queueDialog = new Stage();
            Stage ownerStage = (Stage) rootPane.getScene().getWindow();
            // DON'T use initOwner - it creates dependency that blocks interaction
            // queueDialog.initOwner(ownerStage); 
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
                getClass().getResource("/styles/queue-dialog-lol.css").toExternalForm()
            );
            queueDialog.setScene(dialogScene);
            
            // Start rotation animations for circles
            if (outerRing != null) {
                javafx.animation.RotateTransition rotateOuter = new javafx.animation.RotateTransition(javafx.util.Duration.seconds(8), outerRing);
                rotateOuter.setFromAngle(0);
                rotateOuter.setToAngle(360);
                rotateOuter.setCycleCount(javafx.animation.Animation.INDEFINITE);
                rotateOuter.setInterpolator(javafx.animation.Interpolator.LINEAR);
                rotateOuter.play();
            }
            
            if (innerRing != null) {
                javafx.animation.RotateTransition rotateInner = new javafx.animation.RotateTransition(javafx.util.Duration.seconds(6), innerRing);
                rotateInner.setFromAngle(360);
                rotateInner.setToAngle(0);
                rotateInner.setCycleCount(javafx.animation.Animation.INDEFINITE);
                rotateInner.setInterpolator(javafx.animation.Interpolator.LINEAR);
                rotateInner.play();
            }
            
            startQueueTimer();
            
            queueDialog.show();
            
            // Hàm để căn giữa dialog
            Runnable centerDialog = () -> {
                if (queueDialog != null && queueDialog.isShowing()) {
                    queueDialog.setX(ownerStage.getX() + (ownerStage.getWidth() - queueDialog.getWidth()) / 2);
                    queueDialog.setY(ownerStage.getY() + (ownerStage.getHeight() - queueDialog.getHeight()) / 2);
                }
            };
            
            // Căn giữa dialog ban đầu
            centerDialog.run();
            
            // Listener để dialog tự động di chuyển theo cửa sổ game
            ownerStage.xProperty().addListener((obs, oldVal, newVal) -> {
                if (queueDialog != null && queueDialog.isShowing()) {
                    centerDialog.run();
                }
            });
            ownerStage.yProperty().addListener((obs, oldVal, newVal) -> {
                if (queueDialog != null && queueDialog.isShowing()) {
                    centerDialog.run();
                }
            });
            ownerStage.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (queueDialog != null && queueDialog.isShowing()) {
                    centerDialog.run();
                }
            });
            ownerStage.heightProperty().addListener((obs, oldVal, newVal) -> {
                if (queueDialog != null && queueDialog.isShowing()) {
                    centerDialog.run();
                }
            });
            
            // Listener để điều khiển z-order khi game window thay đổi focus
            ownerStage.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (queueDialog != null && queueDialog.isShowing()) {
                    if (isNowFocused) {
                        queueDialog.toFront(); // Đưa lên trước khi game được focus
                    } else {
                        queueDialog.toBack(); // Đẩy xuống sau khi game mất focus
                    }
                }
            });
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
    private Stage matchWaitingDialog;
    private MatchWaitingController matchWaitingController;

    public void onQueueMatched(String opponent) {
        if (queueTimer != null) {
            queueTimer.stop();
        }
        stopQueueAnimations();
        if (queueDialog != null) {
            queueDialog.close();
            queueDialog = null;
        }

        // Stop all background music and play match found sound only
        if (audioService != null) {
            audioService.stopBackgroundMusic();
            audioService.playMatchFoundSound();
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
                getClass().getResource("/fxml/match-found-lol.fxml")
            );
            javafx.scene.Parent root = loader.load();

            matchFoundController = loader.getController();
            matchFoundController.setNetworkClient(networkClient);
            matchFoundController.setUsername(username);
            matchFoundController.setOpponentName(opponent);

            matchFoundDialog = new Stage();
            // No initOwner - independent Stage for full window control
            matchFoundDialog.setTitle("Tìm Thấy Trận");
            matchFoundDialog.setResizable(false);
            matchFoundDialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            matchFoundDialog.setOnCloseRequest(e -> {
                e.consume();
            });

            matchFoundController.setDialogStage(matchFoundDialog);
            matchFoundController.setOwnerStage((Stage) rootPane.getScene().getWindow());
            
            // Set callback to show waiting dialog when user accepts
            matchFoundController.setOnAcceptCallback(() -> showMatchWaitingDialog(opponent));

            Scene dialogScene = new Scene(root);
            dialogScene.setFill(Color.TRANSPARENT);
            dialogScene.getStylesheets().add(
                getClass().getResource("/styles/match-found-lol.css").toExternalForm()
            );
            matchFoundDialog.setScene(dialogScene);

            matchFoundDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            showStyledAlert("Lỗi", "Không thể hiển thị Match Found dialog: " + e.getMessage(),
                Alert.AlertType.ERROR);
        }
    }

    private void showMatchWaitingDialog(String opponent) {
        if (matchWaitingDialog != null && matchWaitingDialog.isShowing()) {
            return;
        }

        // Stop lobby music when entering waiting screen
        if (audioService != null) {
            audioService.stopBackgroundMusic();
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/match-waiting.fxml")
            );
            javafx.scene.Parent root = loader.load();

            matchWaitingController = loader.getController();
            matchWaitingController.setNetworkClient(networkClient);
            matchWaitingController.setAudioService(audioService);
            matchWaitingController.setOpponentName(opponent);

            matchWaitingDialog = new Stage();
            matchWaitingDialog.setTitle("Đang chờ...");
            matchWaitingDialog.setResizable(false);
            matchWaitingDialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            matchWaitingDialog.setOnCloseRequest(e -> {
                e.consume();
            });

            matchWaitingController.setDialogStage(matchWaitingDialog);
            matchWaitingController.setOwnerStage((Stage) rootPane.getScene().getWindow());

            Scene dialogScene = new Scene(root);
            dialogScene.setFill(Color.TRANSPARENT);
            dialogScene.getStylesheets().add(
                getClass().getResource("/styles/match-waiting.css").toExternalForm()
            );
            matchWaitingDialog.setScene(dialogScene);

            matchWaitingDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            showStyledAlert("Lỗi", "Không thể hiển thị Match Waiting dialog: " + e.getMessage(),
                Alert.AlertType.ERROR);
        }
    }

    public void onMatchReady() {
        javafx.application.Platform.runLater(() -> {
            // Stop lobby music when both clients are ready
            if (audioService != null) {
                audioService.stopBackgroundMusic();
            }
            if (matchWaitingController != null) {
                matchWaitingController.onMatchReady();
            }
            if (matchFoundController != null) {
                matchFoundController.onMatchStarting();
            }
        });
    }

    public void onMatchWaiting() {
        javafx.application.Platform.runLater(() -> {
            if (matchWaitingController != null) {
                matchWaitingController.onOpponentAccepted();
            }
        });
    }

    public void onMatchDeclined(Map<?, ?> payload) {
        javafx.application.Platform.runLater(() -> {
            String reason = payload != null ? String.valueOf(payload.get("reason")) : "";
            String decliner = payload != null ? String.valueOf(payload.get("decliner")) : "";

            // Resume lobby music
            if (audioService != null) {
                audioService.playLobbyMusic();
            }

            // Close waiting dialog if open
            if (matchWaitingController != null) {
                matchWaitingController.onMatchDeclined(reason);
                matchWaitingController = null;
            }
            if (matchWaitingDialog != null && matchWaitingDialog.isShowing()) {
                matchWaitingDialog.close();
                matchWaitingDialog = null;
            }

            // Close match found dialog if open
            if (matchFoundController != null) {
                matchFoundController.onOpponentDeclined(reason, decliner);
            } else {
                String message = "Trận đấu đã bị hủy";
                if ("timeout".equals(reason)) {
                    message = "⏱ Trận đấu bị hủy - Không ai chấp nhận trong thời gian quy định";
                } else if (decliner != null && !decliner.isEmpty() && !"null".equals(decliner)) {
                    message = "❌ " + decliner + " đã từ chối trận đấu";
                }

                showStyledAlert("Trận đấu bị hủy", message, Alert.AlertType.WARNING);
            }
        });
    }

    public void ensureMatchDialogClosed() {
        javafx.application.Platform.runLater(() -> {
            if (matchWaitingController != null) {
                matchWaitingController.closeDialog();
                matchWaitingController = null;
            }
            if (matchWaitingDialog != null && matchWaitingDialog.isShowing()) {
                matchWaitingDialog.close();
                matchWaitingDialog = null;
            }
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
        inviteDialog.initOwner(rootPane.getScene().getWindow()); // Gắn dialog vào cửa sổ game
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
        notifyDialog.initOwner(rootPane.getScene().getWindow()); // Gắn dialog vào cửa sổ game
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
    
    @FXML
    private void handleShowMatchHistory() {
        System.out.println("[LobbyController] handleShowMatchHistory called");
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/match-history-dialog.fxml")
            );
            System.out.println("[LobbyController] Loading FXML...");
            StackPane dialogRoot = loader.load();
            System.out.println("[LobbyController] FXML loaded successfully");
            
            MatchHistoryController controller = loader.getController();
            controller.setNetworkClient(networkClient);
            System.out.println("[LobbyController] Controller set up");
            
            // Set the controller reference in ClientApp if possible
            if (rootPane.getScene() != null && rootPane.getScene().getWindow() instanceof Stage) {
                Stage ownerStage = (Stage) rootPane.getScene().getWindow();
                Object userData = ownerStage.getUserData();
                if (userData instanceof ClientApp) {
                    ((ClientApp) userData).setMatchHistoryController(controller);
                    System.out.println("[LobbyController] Match history controller registered in ClientApp");
                }
            }
            
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.NONE); // Non-modal - không block owner window
            dialogStage.initOwner(rootPane.getScene().getWindow());
            dialogStage.setTitle("Lịch sử trận đấu");
            dialogStage.setResizable(true); // Cho phép resize
            // Removed setAlwaysOnTop - dialog sẽ chỉ hiện trên owner, không đè lên apps khác
            
            // Set min/max size để giữ layout đẹp
            dialogStage.setMinWidth(800);
            dialogStage.setMinHeight(600);
            dialogStage.setMaxWidth(1400);
            dialogStage.setMaxHeight(900);
            
            Scene scene = new Scene(dialogRoot, 1000, 700);
            scene.setFill(Color.TRANSPARENT);
            dialogStage.setScene(scene);
            dialogStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            
            System.out.println("[LobbyController] Dialog stage created, about to show...");
            
            // Center dialog relative to owner window
            Stage owner = (Stage) rootPane.getScene().getWindow();
            
            // Store initial sizes for scaling ratio
            final double initialOwnerWidth = owner.getWidth();
            final double initialOwnerHeight = owner.getHeight();
            final double initialDialogWidth = 1000;
            final double initialDialogHeight = 700;
            
            dialogStage.setOnShown(e -> {
                dialogStage.setX(owner.getX() + (owner.getWidth() - dialogStage.getWidth()) / 2);
                dialogStage.setY(owner.getY() + (owner.getHeight() - dialogStage.getHeight()) / 2);
            });
            
            // Create listeners that can be removed later
            javafx.beans.value.ChangeListener<Number> xListener = (obs, oldVal, newVal) -> {
                if (dialogStage.isShowing()) {
                    dialogStage.setX(newVal.doubleValue() + (owner.getWidth() - dialogStage.getWidth()) / 2);
                }
            };
            
            javafx.beans.value.ChangeListener<Number> yListener = (obs, oldVal, newVal) -> {
                if (dialogStage.isShowing()) {
                    dialogStage.setY(newVal.doubleValue() + (owner.getHeight() - dialogStage.getHeight()) / 2);
                }
            };
            
            javafx.beans.value.ChangeListener<Number> widthListener = (obs, oldVal, newVal) -> {
                if (dialogStage.isShowing()) {
                    // Scale dialog width proportionally to owner window resize
                    double scaleRatio = newVal.doubleValue() / initialOwnerWidth;
                    double newDialogWidth = Math.max(dialogStage.getMinWidth(), 
                                          Math.min(dialogStage.getMaxWidth(), 
                                                   initialDialogWidth * scaleRatio));
                    dialogStage.setWidth(newDialogWidth);
                    dialogStage.setX(owner.getX() + (newVal.doubleValue() - newDialogWidth) / 2);
                }
            };
            
            javafx.beans.value.ChangeListener<Number> heightListener = (obs, oldVal, newVal) -> {
                if (dialogStage.isShowing()) {
                    // Scale dialog height proportionally to owner window resize
                    double scaleRatio = newVal.doubleValue() / initialOwnerHeight;
                    double newDialogHeight = Math.max(dialogStage.getMinHeight(), 
                                           Math.min(dialogStage.getMaxHeight(), 
                                                    initialDialogHeight * scaleRatio));
                    dialogStage.setHeight(newDialogHeight);
                    dialogStage.setY(owner.getY() + (newVal.doubleValue() - newDialogHeight) / 2);
                }
            };
            
            // Add listeners
            owner.xProperty().addListener(xListener);
            owner.yProperty().addListener(yListener);
            owner.widthProperty().addListener(widthListener);
            owner.heightProperty().addListener(heightListener);
            
            // Hide dialog when owner window is minimized or hidden
            javafx.beans.value.ChangeListener<Boolean> iconifiedListener = (obs, wasIconified, isIconified) -> {
                if (isIconified) {
                    dialogStage.hide();
                } else {
                    dialogStage.show();
                }
            };
            
            owner.iconifiedProperty().addListener(iconifiedListener);
            
            // Clean up listeners and reference when dialog closes
            dialogStage.setOnHidden(e -> {
                // Remove listeners to prevent memory leak
                owner.xProperty().removeListener(xListener);
                owner.yProperty().removeListener(yListener);
                owner.widthProperty().removeListener(widthListener);
                owner.heightProperty().removeListener(heightListener);
                owner.iconifiedProperty().removeListener(iconifiedListener);
                
                // Clean up controller reference
                if (rootPane.getScene() != null && rootPane.getScene().getWindow() instanceof Stage) {
                    Stage ownerStage = (Stage) rootPane.getScene().getWindow();
                    Object userData = ownerStage.getUserData();
                    if (userData instanceof ClientApp) {
                        ((ClientApp) userData).setMatchHistoryController(null);
                    }
                }
            });
            
            dialogStage.show();
            System.out.println("[LobbyController] Dialog stage shown successfully");
            
        } catch (Exception e) {
            System.err.println("[LobbyController] Error showing match history dialog:");
            e.printStackTrace();
        }
    }
}


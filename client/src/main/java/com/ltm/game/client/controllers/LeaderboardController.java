package com.ltm.game.client.controllers;

import com.ltm.game.shared.Message;
import com.ltm.game.shared.Protocol;
import com.ltm.game.client.services.NetworkClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class LeaderboardController {
    @FXML
    private VBox entriesContainer;

    private NetworkClient networkClient;
    private Consumer<Void> onBack;

    public void setNetworkClient(NetworkClient client) {
        this.networkClient = client;
    }

    public void setOnBack(Consumer<Void> callback) {
        this.onBack = callback;
    }

    @FXML
    private void initialize() {
        if (networkClient != null) {
            requestLeaderboardData();
        }
    }

    public void requestLeaderboardData() {
        if (networkClient != null) {
            networkClient.send(new Message(Protocol.LEADERBOARD, null));
            
            networkClient.addHandler(Protocol.LEADERBOARD, msg -> {
                Platform.runLater(() -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> entries = (List<Map<String, Object>>) msg.payload;
                    updateLeaderboard(entries);
                });
            });
        }
    }

    private void updateLeaderboard(List<Map<String, Object>> entries) {
        entriesContainer.getChildren().clear();
        
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            int rank = i + 1;
            String playerName = String.valueOf(entry.get("username"));
            int totalPoints = ((Number) entry.get("totalPoints")).intValue();
            int totalWins = ((Number) entry.get("totalWins")).intValue();
            
            HBox entryRow = new HBox(12);
            entryRow.setAlignment(Pos.CENTER_LEFT);
            entryRow.setPadding(new Insets(10, 15, 10, 15));
            entryRow.setMaxWidth(650);
            
            String bgColor;
            if (rank == 1) {
                bgColor = "linear-gradient(to right, rgba(255,215,0,0.45), rgba(255,165,0,0.35))";
            } else if (rank == 2) {
                bgColor = "linear-gradient(to right, rgba(192,192,192,0.45), rgba(169,169,169,0.35))";
            } else if (rank == 3) {
                bgColor = "linear-gradient(to right, rgba(205,127,50,0.45), rgba(184,115,51,0.35))";
            } else {
                bgColor = "rgba(30,90,158,0.3)";
            }
            
            entryRow.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                "-fx-background-radius: 10px;" +
                "-fx-border-color: rgba(100,180,255,0.4);" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 10px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 5, 0.6, 0, 2);"
            );
            
            Label rankLabel = new Label();
            if (rank == 1) {
                rankLabel.setText("🥇");
                rankLabel.setStyle("-fx-font-size: 28px;");
            } else if (rank == 2) {
                rankLabel.setText("🥈");
                rankLabel.setStyle("-fx-font-size: 28px;");
            } else if (rank == 3) {
                rankLabel.setText("🥉");
                rankLabel.setStyle("-fx-font-size: 28px;");
            } else {
                rankLabel.setText(String.valueOf(rank));
                rankLabel.setStyle(
                    "-fx-font-size: 20px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: white;" +
                    "-fx-min-width: 32px;" +
                    "-fx-alignment: center;"
                );
            }
            rankLabel.setMinWidth(38);
            rankLabel.setAlignment(Pos.CENTER);
            
            Label avatarLabel = new Label("👤");
            avatarLabel.setStyle(
                "-fx-font-size: 24px;" +
                "-fx-background-color: rgba(255,255,255,0.2);" +
                "-fx-background-radius: 22px;" +
                "-fx-min-width: 44px;" +
                "-fx-min-height: 44px;" +
                "-fx-alignment: center;"
            );
            
            Label nameLabel = new Label(playerName);
            nameLabel.setStyle(
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 2, 0.6, 0, 1);"
            );
            nameLabel.setMinWidth(200);
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            VBox pointsBox = new VBox(2);
            pointsBox.setAlignment(Pos.CENTER);
            
            Label pointsLabel = new Label(String.valueOf(totalPoints));
            pointsLabel.setStyle(
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #FFD700;" +
                "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.6), 4, 0.7, 0, 2);"
            );
            
            Label pointsTextLabel = new Label("điểm");
            pointsTextLabel.setStyle(
                "-fx-font-size: 10px;" +
                "-fx-text-fill: rgba(255,255,255,0.75);"
            );
            
            pointsBox.getChildren().addAll(pointsLabel, pointsTextLabel);
            
            VBox winsBox = new VBox(2);
            winsBox.setAlignment(Pos.CENTER);
            
            Label winsLabel = new Label(String.valueOf(totalWins));
            winsLabel.setStyle(
                "-fx-font-size: 19px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4CAF50;" +
                "-fx-effect: dropshadow(gaussian, rgba(76,175,80,0.5), 3, 0.6, 0, 1);"
            );
            
            Label winsTextLabel = new Label("thắng");
            winsTextLabel.setStyle(
                "-fx-font-size: 10px;" +
                "-fx-text-fill: rgba(255,255,255,0.75);"
            );
            
            winsBox.getChildren().addAll(winsLabel, winsTextLabel);
            
            entryRow.getChildren().addAll(
                rankLabel, 
                avatarLabel, 
                nameLabel, 
                spacer, 
                pointsBox,
                winsBox
            );
            
            entriesContainer.getChildren().add(entryRow);
        }
    }

    @FXML
    private void handleBack() {
        if (onBack != null) {
            onBack.accept(null);
        }
    }
}


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

    public void updateLeaderboard(List<Map<String, Object>> entries) {
        entriesContainer.getChildren().clear();
        
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            int rank = i + 1;
            String playerName = String.valueOf(entry.get("username"));
            int totalPoints = ((Number) entry.get("totalPoints")).intValue();
            int totalWins = ((Number) entry.get("totalWins")).intValue();
            
            HBox entryRow = new HBox(18);
            entryRow.setAlignment(Pos.CENTER_LEFT);
            entryRow.setPadding(new Insets(16, 24, 16, 24));
            entryRow.setMaxWidth(1000);
            
            // Riot Games color scheme
            String bgGradient;
            String borderColor;
            if (rank == 1) {
                bgGradient = "linear-gradient(to right, rgba(240,199,94,0.25), rgba(200,170,110,0.15))";
                borderColor = "#C8AA6E"; // Gold
            } else if (rank == 2) {
                bgGradient = "linear-gradient(to right, rgba(200,200,200,0.22), rgba(169,169,169,0.12))";
                borderColor = "#C8C8C8"; // Silver
            } else if (rank == 3) {
                bgGradient = "linear-gradient(to right, rgba(205,127,50,0.22), rgba(184,115,51,0.12))";
                borderColor = "#CD7F32"; // Bronze
            } else {
                bgGradient = "linear-gradient(to right, rgba(15,25,35,0.95), rgba(20,35,45,0.90))";
                borderColor = "#1E5A5A"; // Dark teal
            }
            
            entryRow.setStyle(
                "-fx-background-color: " + bgGradient + ";" +
                "-fx-background-radius: 3px;" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 3px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 12, 0.6, 0, 4);"
            );
            
            // Rank indicator
            Label rankLabel = new Label();
            rankLabel.setMinWidth(45);
            rankLabel.setAlignment(Pos.CENTER);
            
            if (rank <= 3) {
                String medal;
                String rankColor;
                if (rank == 1) {
                    medal = "①";
                    rankColor = "#F0C75E"; // Bright gold
                } else if (rank == 2) {
                    medal = "②";
                    rankColor = "#C8C8C8"; // Silver
                } else {
                    medal = "③";
                    rankColor = "#CD7F32"; // Bronze
                }
                rankLabel.setText(medal);
                rankLabel.setStyle(
                    "-fx-font-size: 32px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: " + rankColor + ";" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 8, 0.7, 0, 2);"
                );
            } else {
                rankLabel.setText(String.valueOf(rank));
                rankLabel.setStyle(
                    "-fx-font-size: 18px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #8C8C8C;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 5, 0.6, 0, 1);"
                );
            }
            
            // Player name
            Label nameLabel = new Label(playerName);
            nameLabel.setStyle(
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #F0E6D2;" + // Cream text (Riot standard)
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 6, 0.7, 0, 2);"
            );
            nameLabel.setMinWidth(280);
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            // Points display
            VBox pointsBox = new VBox(2);
            pointsBox.setAlignment(Pos.CENTER);
            
            Label pointsLabel = new Label(String.valueOf(totalPoints));
            pointsLabel.setStyle(
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #C8AA6E;" + // Riot gold
                "-fx-effect: dropshadow(gaussian, rgba(200,170,110,0.5), 8, 0.7, 0, 2);"
            );
            
            Label pointsTextLabel = new Label("ĐIỂM");
            pointsTextLabel.setStyle(
                "-fx-font-size: 9px;" +
                "-fx-font-weight: 600;" +
                "-fx-text-fill: rgba(240,230,210,0.6);"
            );
            
            pointsBox.getChildren().addAll(pointsLabel, pointsTextLabel);
            
            // Wins display
            VBox winsBox = new VBox(2);
            winsBox.setAlignment(Pos.CENTER);
            winsBox.setPadding(new Insets(0, 0, 0, 15));
            
            Label winsLabel = new Label(String.valueOf(totalWins));
            winsLabel.setStyle(
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #0AC8B9;" + // Riot teal
                "-fx-effect: dropshadow(gaussian, rgba(10,200,185,0.4), 6, 0.6, 0, 2);"
            );
            
            Label winsTextLabel = new Label("THẮNG");
            winsTextLabel.setStyle(
                "-fx-font-size: 9px;" +
                "-fx-font-weight: 600;" +
                "-fx-text-fill: rgba(240,230,210,0.6);"
            );
            
            winsBox.getChildren().addAll(winsLabel, winsTextLabel);
            
            entryRow.getChildren().addAll(
                rankLabel, 
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


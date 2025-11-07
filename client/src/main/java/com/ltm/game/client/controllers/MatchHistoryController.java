package com.ltm.game.client.controllers;

import com.ltm.game.client.models.MatchHistoryRow;
import com.ltm.game.client.services.NetworkClient;
import com.ltm.game.shared.Message;
import com.ltm.game.shared.Protocol;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class MatchHistoryController {
    @FXML private Button closeButton;
    @FXML private Label totalMatchesLabel;
    @FXML private Label winsLabel;
    @FXML private Label lossesLabel;
    @FXML private Label drawsLabel;
    @FXML private TableView<MatchHistoryRow> historyTable;
    @FXML private TableColumn<MatchHistoryRow, String> colDate;
    @FXML private TableColumn<MatchHistoryRow, String> colOpponent;
    @FXML private TableColumn<MatchHistoryRow, String> colResult;
    @FXML private TableColumn<MatchHistoryRow, String> colScore;
    
    private NetworkClient networkClient;
    private ObservableList<MatchHistoryRow> matchHistory = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        // Set column resize policy to fill all available space
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        setupTableColumns();
        historyTable.setItems(matchHistory);
    }
    
    public void setNetworkClient(NetworkClient client) {
        this.networkClient = client;
        requestMatchHistory();
    }
    
    private void requestMatchHistory() {
        if (networkClient != null) {
            Message request = new Message(Protocol.MATCH_HISTORY_REQUEST, null);
            networkClient.send(request);
        }
    }
    
    private void setupTableColumns() {
        // Date column
        colDate.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getFormattedDate()));
        colDate.setStyle("-fx-alignment: CENTER;");
        
        // Opponent column
        colOpponent.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getOpponent()));
        colOpponent.setStyle("-fx-alignment: CENTER-LEFT;");
        
        // Result column with colored badges
        colResult.setCellFactory(col -> new TableCell<MatchHistoryRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    MatchHistoryRow row = getTableRow().getItem();
                    Label badge = new Label(row.getResult());
                    badge.setAlignment(Pos.CENTER);
                    badge.setPrefWidth(100);
                    
                    switch (row.getResult()) {
                        case "THẮNG":
                            badge.getStyleClass().add("result-win");
                            break;
                        case "THUA":
                            badge.getStyleClass().add("result-loss");
                            break;
                        case "HÒA":
                            badge.getStyleClass().add("result-draw");
                            break;
                    }
                    
                    HBox container = new HBox(badge);
                    container.setAlignment(Pos.CENTER);
                    setGraphic(container);
                    setText(null);
                }
            }
        });
        
        // Score column
        colScore.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getScore()));
        colScore.setStyle("-fx-alignment: CENTER;");
    }
    
    public void handleMatchHistoryData(Map<String, Object> data) {
        javafx.application.Platform.runLater(() -> {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matches = (List<Map<String, Object>>) data.get("matches");
            
            if (matches != null) {
                matchHistory.clear();
                int wins = 0, losses = 0, draws = 0;
                
                for (Map<String, Object> match : matches) {
                    String result = (String) match.get("result");
                    
                    // Parse scores as integers (handle both Integer and Double from JSON)
                    Object myScoreObj = match.get("myScore");
                    Object opponentScoreObj = match.get("opponentScore");
                    
                    int myScore = (myScoreObj instanceof Number) 
                        ? ((Number) myScoreObj).intValue() 
                        : Integer.parseInt(String.valueOf(myScoreObj));
                    
                    int opponentScore = (opponentScoreObj instanceof Number) 
                        ? ((Number) opponentScoreObj).intValue() 
                        : Integer.parseInt(String.valueOf(opponentScoreObj));
                    
                    MatchHistoryRow row = new MatchHistoryRow(
                        (String) match.get("matchId"),
                        (String) match.get("opponent"),
                        result,
                        String.valueOf(myScore),
                        String.valueOf(opponentScore),
                        (String) match.get("duration"),
                        LocalDateTime.parse((String) match.get("date")),
                        (Boolean) match.getOrDefault("mvp", false),
                        (Boolean) match.getOrDefault("perfect", false)
                    );
                    matchHistory.add(row);
                    
                    // Count stats
                    if ("THẮNG".equals(result)) wins++;
                    else if ("THUA".equals(result)) losses++;
                    else if ("HÒA".equals(result)) draws++;
                }
                
                // Update stats labels
                int total = matchHistory.size();
                totalMatchesLabel.setText(String.valueOf(total));
                winsLabel.setText(String.valueOf(wins));
                lossesLabel.setText(String.valueOf(losses));
                drawsLabel.setText(String.valueOf(draws));
            }
        });
    }
    
    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
    
    @FXML
    private void handleCloseHover() {
        closeButton.setStyle(closeButton.getStyle() + 
            "-fx-background-color: rgba(244, 67, 54, 0.4);");
    }
    
    @FXML
    private void handleCloseExit() {
        closeButton.setStyle(closeButton.getStyle().replace(
            "-fx-background-color: rgba(244, 67, 54, 0.4);", 
            "-fx-background-color: rgba(244, 67, 54, 0.2);"));
    }
}

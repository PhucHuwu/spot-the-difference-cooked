package com.ltm.game.client.controllers;

import com.ltm.game.client.services.AudioService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.Map;
import java.util.function.Consumer;

public class ResultController {
    @FXML
    private Label resultIcon;
    
    @FXML
    private Label resultTitle;
    
    @FXML
    private Label yourNameLabel;
    
    @FXML
    private Label yourScoreLabel;
    
    @FXML
    private Label oppNameLabel;
    
    @FXML
    private Label oppScoreLabel;
    
    @FXML
    private Label reasonLabel;
    
    @FXML
    private javafx.scene.control.Button rematchButton;

    private Consumer<Void> onBackToLobby;
    private Consumer<Void> onShowLeaderboard;
    private AudioService audioService;

    public void setAudioService(AudioService service) {
        this.audioService = service;
    }

    public void setOnBackToLobby(Consumer<Void> callback) {
        this.onBackToLobby = callback;
    }

    public void setOnShowLeaderboard(Consumer<Void> callback) {
        this.onShowLeaderboard = callback;
    }

    public void setGameResult(Map<?, ?> payload, String myUsername) {
        System.out.println("🏆 setGameResult called");
        System.out.println("   Payload: " + payload);
        System.out.println("   My username: " + myUsername);
        
        if (audioService != null) {
            audioService.playCelebrationSound();
        }

        String reason = String.valueOf(payload.get("reason"));
        String result = String.valueOf(payload.get("result")); // Winner từ server
        Map<?, ?> scores = (Map<?, ?>) payload.get("scores");
        
        System.out.println("   Reason: " + reason);
        System.out.println("   Result (winner): " + result);
        System.out.println("   Scores map: " + scores);
        
        int myScore = 0;
        int opponentScore = 0;
        String opponentName = "";
        
        for (var entry : scores.entrySet()) {
            String player = String.valueOf(entry.getKey());
            int score = ((Number) entry.getValue()).intValue();
            System.out.println("   Player: " + player + ", Score: " + score);
            if (player.equals(myUsername)) {
                myScore = score;
            } else {
                opponentName = player;
                opponentScore = score;
            }
        }
        
        System.out.println("   My score: " + myScore + ", Opponent score: " + opponentScore);
        
        // Ưu tiên kết quả từ server (xử lý cả trường hợp forfeit/quit)
        boolean isWinner;
        boolean isDraw;
        
        if (result != null && !result.equals("null")) {
            // Server đã xác định winner (có thể do quit/disconnect)
            isWinner = result.equals(myUsername);
            isDraw = result.equals("DRAW");
        } else {
            // Fallback: so sánh điểm (trường hợp server cũ hoặc không có result)
            isWinner = myScore > opponentScore;
            isDraw = myScore == opponentScore;
        }
        
        System.out.println("   Is winner: " + isWinner + ", Is draw: " + isDraw);
        
        // Apply Riot Games styling based on result
        if (isDraw) {
            resultIcon.setText("⚖");
            resultTitle.setText("HÒA");
            resultTitle.setStyle(resultTitle.getStyle() + "-fx-text-fill: #D4C5AA;"); // Light gray-gold
        } else if (isWinner) {
            resultIcon.setText("★");
            resultTitle.setText("CHIẾN THẮNG");
            resultTitle.setStyle(resultTitle.getStyle() + "-fx-text-fill: #F0E6D2;"); // Riot cream/white
        } else {
            resultIcon.setText("✖");
            resultTitle.setText("THẤT BẠI");
            resultTitle.setStyle(resultTitle.getStyle() + "-fx-text-fill: #E84A4F;"); // Bright red
        }
        
        yourNameLabel.setText(myUsername);
        yourScoreLabel.setText(String.valueOf(myScore));
        
        // Riot color scheme: Bright gold for winner, Bright red for loser, Light gray for draw
        String scoreColor = isWinner ? "#F0C75E" : (isDraw ? "#D4C5AA" : "#E84A4F");
        yourScoreLabel.setStyle(yourScoreLabel.getStyle() + "-fx-text-fill: " + scoreColor + ";");
        
        oppNameLabel.setText(opponentName);
        oppScoreLabel.setText(String.valueOf(opponentScore));
        
        String oppScoreColor = !isWinner && !isDraw ? "#F0C75E" : (isDraw ? "#D4C5AA" : "#E84A4F");
        oppScoreLabel.setStyle(oppScoreLabel.getStyle() + "-fx-text-fill: " + oppScoreColor + ";");
        
        String reasonText = "";
        if (reason.equals("all-found")) {
            reasonText = "✓ TẤT CẢ ĐIỂM KHÁC BIỆT ĐÃ ĐƯỢC TÌM THẤY";
        } else if (reason.endsWith("-quit")) {
            // Hiển thị tên người quit thay vì chỉ nói "đối thủ"
            String quitter = reason.substring(0, reason.indexOf("-quit"));
            if (quitter.equals(myUsername)) {
                reasonText = "» Bạn đã rời khỏi trận đấu";
            } else {
                reasonText = "» " + quitter + " đã rời khỏi trận đấu";
            }
        } else if (reason.equals("quit")) {
            reasonText = "» Đối thủ đã rời khỏi trận đấu";
        } else {
            reasonText = "✓ TRẬN ĐẤU KẾT THÚC";
        }
        
        reasonLabel.setText(reasonText);
    }

    @FXML
    private void handleBackToLobby() {
        if (onBackToLobby != null) {
            onBackToLobby.accept(null);
        }
    }

    @FXML
    private void handleViewLeaderboard() {
        if (onShowLeaderboard != null) {
            onShowLeaderboard.accept(null);
        }
    }
    
    @FXML
    private void handleContinue() {
        // Simply go back to lobby
        if (onBackToLobby != null) {
            onBackToLobby.accept(null);
        }
    }
}


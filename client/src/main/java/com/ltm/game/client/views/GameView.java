package com.ltm.game.client.views;

import com.ltm.game.client.services.AudioService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class GameView {
    private final StackPane root = new StackPane();
    private final Canvas canvas = new Canvas(900, 450);
    private final Label scoreLabel = new Label("Score: 0");
    private final Label timerLabel = new Label("⏱ 15");
    private final Label turnLabel = new Label("Lượt của bạn");
    
    private AudioService audioService;
    private String nextTurn = "";
    private int scoreA = 0, scoreB = 0;
    private String playerA = "";
    private String playerB = "";
    private String myUsername = "";
    private int remainingSeconds = 15;
    private Timeline countdownTimer;
    private final List<Map<String,Object>> found = new ArrayList<>();
    private Double lastClickX = null;
    private Double lastClickY = null;
    private boolean justClicked = false;
    
    private Image leftImg;
    private Image rightImg;
    private int imgW = 300;
    private int imgH = 300;
    private final double boxX1 = 50, boxY = 100, boxX2 = 500, boxSize = 350;

    public GameView(BiConsumer<Double, Double> onClick, String myUsername, AudioService audioService) {
        this.myUsername = myUsername;
        this.audioService = audioService;
        
        if (audioService != null) {
            audioService.playGameMusic();
            audioService.loadGameSounds();
        }
        
        ImageView bgImageView = new ImageView();
        try {
            Image bgImage = new Image(getClass().getResourceAsStream("/images/v2/forest-8227410.jpg"));
            bgImageView.setImage(bgImage);
            bgImageView.setPreserveRatio(false);
            bgImageView.fitWidthProperty().bind(root.widthProperty());
            bgImageView.fitHeightProperty().bind(root.heightProperty());
        } catch (Exception e) {
            System.err.println("Could not load background image: " + e.getMessage());
        }
        
        Pane overlay = new Pane();
        overlay.setStyle("-fx-background-color: rgba(20,20,40,0.65);");
        overlay.prefWidthProperty().bind(root.widthProperty());
        overlay.prefHeightProperty().bind(root.heightProperty());
        
        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: transparent;");
        
        HBox topBar = createTopBar();
        mainLayout.setTop(topBar);
        
        VBox centerContainer = new VBox(10);
        centerContainer.setAlignment(Pos.CENTER);
        centerContainer.setPadding(new Insets(10, 15, 10, 15));
        centerContainer.getChildren().addAll(canvas, turnLabel);
        
        turnLabel.setStyle(
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 4, 0.7, 0, 2);"
        );
        
        mainLayout.setCenter(centerContainer);
        
        root.getChildren().addAll(bgImageView, overlay, mainLayout);
        
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (remainingSeconds > 0) {
                remainingSeconds--;
                updateTimerDisplay();
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        
        drawBase();
        canvas.setOnMouseClicked(e -> {
            double x = e.getX();
            double y = e.getY();
            System.out.println("Canvas clicked at: (" + x + ", " + y + ")");
            Double mappedX = null, mappedY = null;
            if (x >= boxX1 && x <= boxX1 + boxSize && y >= boxY && y <= boxY + boxSize) {
                double lx = x - boxX1; double ly = y - boxY;
                mappedX = lx * (imgW / boxSize);
                mappedY = ly * (imgH / boxSize);
                System.out.println("Left box clicked - mapped to image coords: (" + mappedX + ", " + mappedY + ")");
            } else if (x >= boxX2 && x <= boxX2 + boxSize && y >= boxY && y <= boxY + boxSize) {
                double rx = x - boxX2; double ry = y - boxY;
                mappedX = rx * (imgW / boxSize);
                mappedY = ry * (imgH / boxSize);
                System.out.println("Right box clicked - mapped to image coords: (" + mappedX + ", " + mappedY + ")");
            }
            if (mappedX != null) {
                System.out.println("Sending onClick callback with coords: (" + mappedX + ", " + mappedY + ")");
                lastClickX = mappedX;
                lastClickY = mappedY;
                justClicked = true;
                render();
                onClick.accept(mappedX, mappedY);
            } else {
                System.out.println("Click outside image boxes - ignoring");
            }
        });
    }
    
    private HBox createTopBar() {
        HBox topBar = new HBox(30);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        
        VBox scoreBox = new VBox(5);
        scoreBox.setAlignment(Pos.CENTER);
        scoreBox.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #4facfe, #00f2fe);" +
            "-fx-background-radius: 20px;" +
            "-fx-padding: 12px 35px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0.7, 0, 4);"
        );
        
        Label scoreIcon = new Label("🎯");
        scoreIcon.setStyle("-fx-font-size: 32px;");
        
        scoreLabel.setStyle(
            "-fx-font-size: 22px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 3, 0.7, 0, 2);"
        );
        
        HBox scoreContent = new HBox(10);
        scoreContent.setAlignment(Pos.CENTER);
        scoreContent.getChildren().addAll(scoreIcon, scoreLabel);
        scoreBox.getChildren().add(scoreContent);
        
        VBox timerBox = new VBox(5);
        timerBox.setAlignment(Pos.CENTER);
        timerBox.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #fa709a, #fee140);" +
            "-fx-background-radius: 20px;" +
            "-fx-padding: 12px 30px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0.7, 0, 4);"
        );
        
        timerLabel.setStyle(
            "-fx-font-size: 24px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 3, 0.7, 0, 2);"
        );
        timerBox.getChildren().add(timerLabel);
        
        Region spacer1 = new Region();
        Region spacer2 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        
        topBar.getChildren().addAll(spacer1, scoreBox, timerBox, spacer2);
        
        return topBar;
    }

    private void drawBase() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        
        g.setFill(Color.TRANSPARENT);
        g.fillRect(0, 0, 900, 450);
        
        g.setFill(Color.WHITE);
        g.fillRoundRect(boxX1 - 10, boxY - 10, boxSize + 20, boxSize + 20, 25, 25);
        g.fillRoundRect(boxX2 - 10, boxY - 10, boxSize + 20, boxSize + 20, 25, 25);
        
        if (leftImg != null) {
            g.save();
            Rectangle clip1 = new Rectangle(boxX1, boxY, boxSize, boxSize);
            clip1.setArcWidth(15);
            clip1.setArcHeight(15);
            g.beginPath();
            g.rect(boxX1, boxY, boxSize, boxSize);
            g.closePath();
            g.clip();
            g.drawImage(leftImg, boxX1, boxY, boxSize, boxSize);
            g.restore();
        } else { 
            g.setFill(Color.web("#e8e8e8")); 
            g.fillRoundRect(boxX1, boxY, boxSize, boxSize, 15, 15);
        }
        
        if (rightImg != null) {
            g.save();
            Rectangle clip2 = new Rectangle(boxX2, boxY, boxSize, boxSize);
            clip2.setArcWidth(15);
            clip2.setArcHeight(15);
            g.beginPath();
            g.rect(boxX2, boxY, boxSize, boxSize);
            g.closePath();
            g.clip();
            g.drawImage(rightImg, boxX2, boxY, boxSize, boxSize);
            g.restore();
        } else { 
            g.setFill(Color.web("#e8e8e8")); 
            g.fillRoundRect(boxX2, boxY, boxSize, boxSize, 15, 15);
        }
        
        g.setStroke(Color.WHITE);
        g.setLineWidth(8);
        g.strokeRoundRect(boxX1 - 5, boxY - 5, boxSize + 10, boxSize + 10, 20, 20);
        g.strokeRoundRect(boxX2 - 5, boxY - 5, boxSize + 10, boxSize + 10, 20, 20);
        g.setLineWidth(1);
    }

    public void updateFromPayload(Map<?,?> p) {
        String oldNextTurn = nextTurn;
        
        Object scoresObj = p.get("scores");
        if (scoresObj instanceof Map<?,?> s) {
            var entries = new ArrayList<>(s.entrySet());
            if (entries.size() >= 2) {
                var e1 = (Map.Entry<?,?>) entries.get(0);
                var e2 = (Map.Entry<?,?>) entries.get(1);
                playerA = String.valueOf(e1.getKey());
                playerB = String.valueOf(e2.getKey());
                scoreA = ((Number)e1.getValue()).intValue();
                scoreB = ((Number)e2.getValue()).intValue();
            }
        }
        
        String newNextTurn = p.get("nextTurn") != null ? String.valueOf(p.get("nextTurn")) : nextTurn;
        boolean turnChanged = !oldNextTurn.isEmpty() && !newNextTurn.equals(oldNextTurn);
        
        nextTurn = newNextTurn;
        
        if (p.get("remainingTurnMs") != null) {
            long ms = ((Number)p.get("remainingTurnMs")).longValue();
            remainingSeconds = (int) Math.max(0, ms / 1000);
            
            countdownTimer.stop();
            if (remainingSeconds > 0) {
                countdownTimer.playFromStart();
            }
            updateTimerDisplay();
        }
        
        if (p.get("found") instanceof List<?> f) {
            int oldFoundCount = found.size();
            found.clear();
            lastClickX = null;
            lastClickY = null;
            
            Map<String,Object> latestFind = null;
            for (Object o: f) {
                if (o instanceof Map<?,?> m) {
                    @SuppressWarnings("unchecked")
                    Map<String,Object> findMap = (Map<String,Object>) m;
                    found.add(findMap);
                    latestFind = findMap;
                }
            }
            
            if (found.size() > oldFoundCount && latestFind != null) {
                String finder = latestFind.get("finder") != null ? String.valueOf(latestFind.get("finder")) : "";
                if (finder.equals(myUsername) && audioService != null) {
                    audioService.playCorrectSound();
                    System.out.println("Playing correct sound - player found a difference!");
                }
                justClicked = false;
            } else if (justClicked && found.size() == oldFoundCount) {
                if (audioService != null) {
                    audioService.playWrongSound();
                    System.out.println("Playing wrong sound - clicked but missed!");
                }
                justClicked = false;
            } else if (turnChanged) {
                justClicked = false;
            }
        }
        render();
    }
    
    private void updateTimerDisplay() {
        boolean isMyTurn = nextTurn.equals(myUsername);
        
        if (isMyTurn) {
            turnLabel.setText("⚡ LƯỢT CỦA BẠN ⚡");
            turnLabel.setStyle(
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #00ff88;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,255,136,0.8), 6, 0.8, 0, 0);"
            );
        } else {
            turnLabel.setText("⏳ Đợi đối thủ...");
            turnLabel.setStyle(
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #bdc3c7;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 4, 0.7, 0, 2);"
            );
        }
        
        timerLabel.setText(String.format("⏱ %d", remainingSeconds));
    }

    private void render() {
        drawBase();
        GraphicsContext g = canvas.getGraphicsContext2D();
        
        for (Map<String,Object> d : found) {
            double x = ((Number)d.get("x")).doubleValue();
            double y = ((Number)d.get("y")).doubleValue();
            double r = ((Number)d.get("radius")).doubleValue();
            double sx = x * (boxSize / imgW);
            double sy = y * (boxSize / imgH);
            double rr = r * (boxSize / imgW);
            
            String finder = d.get("finder") != null ? String.valueOf(d.get("finder")) : "";
            Color circleColor;
            if (finder.equals(myUsername)) {
                circleColor = Color.web("#00ff88");
            } else if (finder.equals(playerA) || finder.equals(playerB)) {
                circleColor = Color.web("#ff3838");
            } else {
                circleColor = Color.ORANGE;
            }
            
            g.setStroke(circleColor.deriveColor(0, 1, 1, 0.15));
            g.setLineWidth(14);
            g.strokeOval(boxX1 + sx - rr - 5, boxY + sy - rr - 5, rr*2 + 10, rr*2 + 10);
            g.strokeOval(boxX2 + sx - rr - 5, boxY + sy - rr - 5, rr*2 + 10, rr*2 + 10);
            
            g.setStroke(circleColor.deriveColor(0, 1, 1, 0.4));
            g.setLineWidth(8);
            g.strokeOval(boxX1 + sx - rr - 2, boxY + sy - rr - 2, rr*2 + 4, rr*2 + 4);
            g.strokeOval(boxX2 + sx - rr - 2, boxY + sy - rr - 2, rr*2 + 4, rr*2 + 4);
            
            g.setStroke(circleColor);
            g.setLineWidth(5);
            g.strokeOval(boxX1 + sx - rr, boxY + sy - rr, rr*2, rr*2);
            g.strokeOval(boxX2 + sx - rr, boxY + sy - rr, rr*2, rr*2);
            g.setLineWidth(1);
        }
        
        if (lastClickX != null && lastClickY != null) {
            double sx = lastClickX * (boxSize / imgW);
            double sy = lastClickY * (boxSize / imgH);
            g.setStroke(Color.YELLOW);
            g.setLineWidth(3);
            double crossSize = 12;
            g.strokeLine(boxX1 + sx - crossSize, boxY + sy - crossSize, boxX1 + sx + crossSize, boxY + sy + crossSize);
            g.strokeLine(boxX1 + sx + crossSize, boxY + sy - crossSize, boxX1 + sx - crossSize, boxY + sy + crossSize);
            g.strokeLine(boxX2 + sx - crossSize, boxY + sy - crossSize, boxX2 + sx + crossSize, boxY + sy + crossSize);
            g.strokeLine(boxX2 + sx + crossSize, boxY + sy - crossSize, boxX2 + sx - crossSize, boxY + sy + crossSize);
            g.setLineWidth(1);
        }
        
        String myScore = "", opponentScore = "";
        if (playerA.equals(myUsername)) {
            myScore = String.valueOf(scoreA);
            opponentScore = String.valueOf(scoreB);
        } else if (playerB.equals(myUsername)) {
            myScore = String.valueOf(scoreB);
            opponentScore = String.valueOf(scoreA);
        } else {
            myScore = "0";
            opponentScore = "0";
        }
        
        String scoreText = String.format("%s | %s", myScore, opponentScore);
        scoreLabel.setText(scoreText);
    }

    public StackPane getRoot() { 
        return root; 
    }

    public void setImages(byte[] leftBytes, byte[] rightBytes, int width, int height) {
        System.out.println("GameView.setImages called: leftBytes=" + (leftBytes != null ? leftBytes.length : "null") + ", rightBytes=" + (rightBytes != null ? rightBytes.length : "null") + ", w=" + width + ", h=" + height);
        this.imgW = width > 0 ? width : this.imgW;
        this.imgH = height > 0 ? height : this.imgH;
        try {
            this.leftImg = leftBytes != null ? new Image(new ByteArrayInputStream(leftBytes)) : null;
            this.rightImg = rightBytes != null ? new Image(new ByteArrayInputStream(rightBytes)) : null;
            System.out.println("Images created: leftImg=" + (leftImg != null) + ", rightImg=" + (rightImg != null));
            if (leftImg != null) System.out.println("Left image size: " + leftImg.getWidth() + "x" + leftImg.getHeight());
            if (rightImg != null) System.out.println("Right image size: " + rightImg.getWidth() + "x" + rightImg.getHeight());
        } catch (Exception e) {
            System.err.println("Error creating Image from bytes: " + e.getMessage());
            e.printStackTrace();
        }
        render();
    }
}

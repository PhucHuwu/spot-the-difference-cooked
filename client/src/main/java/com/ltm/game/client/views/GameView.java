package com.ltm.game.client.views;

import com.ltm.game.client.services.AudioService;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.effect.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
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
    
    // Nhãn thông tin người chơi
    private final Label playerANameLabel = new Label();
    private final Label playerBNameLabel = new Label();
    private final Label playerAScoreLabel = new Label("0");
    private final Label playerBScoreLabel = new Label("0");
    private final Circle playerAAvatar = new Circle(25);
    private final Circle playerBAvatar = new Circle(25);
    
    // Đồng hồ đếm ngược và chỉ báo lượt
    private final Label timerLabel = new Label("15");
    private final Label turnIndicator = new Label("YOUR TURN");
    private final StackPane timerBox = new StackPane();
    
    // Viền phát sáng hoạt hình cho ảnh
    private final Rectangle leftBorderGlow = new Rectangle();
    private final Rectangle rightBorderGlow = new Rectangle();
    private Timeline glowAnimation;
    
    private AudioService audioService;
    private String nextTurn = "";
    private int scoreA = 0, scoreB = 0;
    private String playerA = "";
    private String playerB = "";
    private String myUsername = "";
    private Runnable onQuitGame;
    private int remainingSeconds = 15;
    private Timeline countdownTimer;
    private final List<Map<String,Object>> found = new ArrayList<>();
    private boolean justClicked = false;
    
    private Image leftImg;
    private Image rightImg;
    private int imgW = 400;
    private int imgH = 400;
    private final double boxX1 = 25, boxY = 25, boxX2 = 475, boxSize = 400;

    public GameView(BiConsumer<Double, Double> onClick, String myUsername, AudioService audioService, Runnable onQuitGame) {
        this.myUsername = myUsername;
        this.audioService = audioService;
        this.onQuitGame = onQuitGame;
        
        if (audioService != null) {
            audioService.playGameMusic();
            audioService.loadGameSounds();
        }
        
        // Hình nền với lớp phủ gradient tối
        ImageView bgImageView = new ImageView();
        try {
            Image bgImage = new Image(getClass().getResourceAsStream("/images/v2/forest-8227410.jpg"));
            bgImageView.setImage(bgImage);
            bgImageView.setPreserveRatio(false);
            bgImageView.fitWidthProperty().bind(root.widthProperty());
            bgImageView.fitHeightProperty().bind(root.heightProperty());
        } catch (Exception e) {
            System.err.println("Không thể tải ảnh nền: " + e.getMessage());
        }
        
        // Lớp phủ tối để tăng độ tương phản
        Pane overlay = new Pane();
        overlay.setStyle("-fx-background-color: rgba(10,15,25,0.75);");
        overlay.prefWidthProperty().bind(root.widthProperty());
        overlay.prefHeightProperty().bind(root.heightProperty());
        
        // Bố cục chính
        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: transparent;");
        
        // Tạo header theo phong cách Riot
        VBox header = createRiotStyleHeader();
        mainLayout.setTop(header);
        
        // Giữa: Canvas với chỉ báo lượt
        VBox centerContainer = new VBox(15);
        centerContainer.setAlignment(Pos.CENTER);
        centerContainer.setPadding(new Insets(10, 15, 15, 15));
        
        // Thêm chỉ báo lượt phía trên canvas
        turnIndicator.setStyle(
            "-fx-font-family: 'Arial Black', sans-serif;" +
            "-fx-font-size: 24px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFFFFF;" +
            "-fx-padding: 8px 40px;" +
            "-fx-background-color: linear-gradient(to right, #0ac8b9, #0077d4);" +
            "-fx-background-radius: 25px;" +
            "-fx-effect: dropshadow(gaussian, rgba(10,200,185,0.8), 15, 0.7, 0, 0);"
        );
        
        centerContainer.getChildren().addAll(turnIndicator, canvas);
        mainLayout.setCenter(centerContainer);
        
        root.getChildren().addAll(bgImageView, overlay, mainLayout);
        
        // Khởi tạo đồng hồ đếm ngược
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (remainingSeconds > 0) {
                remainingSeconds--;
                updateTimerDisplay();
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        
        // Thiết lập hiệu ứng phát sáng viền
        setupBorderGlowAnimation();
        
        // Vẽ ban đầu
        drawBase();
        
        // Xử lý click trên canvas
        canvas.setOnMouseClicked(e -> {
            // Kiểm tra xem có phải lượt của mình không
            if (!nextTurn.equals(myUsername)) {
                System.out.println("Không phải lượt của bạn! Lượt hiện tại: " + nextTurn);
                return;
            }
            
            double x = e.getX();
            double y = e.getY();
            System.out.println("Canvas được click tại: (" + x + ", " + y + ")");
            Double mappedX = null, mappedY = null;

            // Chỉ cho phép click vào ô TRÁI, không cho click ô PHẢI (ảnh gốc)
            if (x >= boxX1 && x <= boxX1 + boxSize && y >= boxY && y <= boxY + boxSize) {
                double lx = x - boxX1; double ly = y - boxY;
                mappedX = lx * (imgW / boxSize);
                mappedY = ly * (imgH / boxSize);
                System.out.println("Click ô trái - chuyển đổi sang tọa độ ảnh: (" + mappedX + ", " + mappedY + ")");
            } else if (x >= boxX2 && x <= boxX2 + boxSize && y >= boxY && y <= boxY + boxSize) {
                System.out.println("Click ô phải - bỏ qua");
                return;
            }
            
            if (mappedX != null) {
                System.out.println("Gửi callback onClick với tọa độ: (" + mappedX + ", " + mappedY + ")");
                justClicked = true;
                render();
                onClick.accept(mappedX, mappedY);
            } else {
                System.out.println("Click bên ngoài vùng ảnh - bỏ qua");
            }
        });
    }
    
    private VBox createRiotStyleHeader() {
        VBox header = new VBox();
        header.setPadding(new Insets(15, 20, 10, 20));
        header.setStyle(
            "-fx-background-color: linear-gradient(to bottom, rgba(0,0,0,0.8), rgba(0,0,0,0.4));" +
            "-fx-border-color: rgba(10,200,185,0.3);" +
            "-fx-border-width: 0 0 2 0;"
        );
        
        // Hàng trên: Người chơi vs Người chơi với đồng hồ ở giữa
        HBox topRow = new HBox(20);
        topRow.setAlignment(Pos.CENTER);
        topRow.setPadding(new Insets(5, 0, 10, 0));
        
        // Người chơi A (Trái)
        HBox playerABox = createPlayerBox(playerAAvatar, playerANameLabel, playerAScoreLabel, true);
        
        // VS + Đồng hồ (Giữa)
        VBox centerBox = new VBox(5);
        centerBox.setAlignment(Pos.CENTER);
        
        // Đồng hồ với phong cách lục giác
        timerBox.setMinSize(80, 80);
        timerBox.setMaxSize(80, 80);
        timerBox.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #1a1a2e, #0f0f1e);" +
            "-fx-border-color: #0ac8b9;" +
            "-fx-border-width: 3;" +
            "-fx-border-radius: 40;" +
            "-fx-background-radius: 40;" +
            "-fx-effect: dropshadow(gaussian, rgba(10,200,185,0.6), 20, 0.7, 0, 0);"
        );
        
        timerLabel.setStyle(
            "-fx-font-family: 'Arial Black', sans-serif;" +
            "-fx-font-size: 32px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #0ac8b9;" +
            "-fx-effect: dropshadow(gaussian, rgba(10,200,185,0.8), 8, 0.7, 0, 0);"
        );
        timerBox.getChildren().add(timerLabel);
        
        Label vsLabel = new Label("VS");
        vsLabel.setStyle(
            "-fx-font-family: 'Impact', 'Arial Black', sans-serif;" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #ff4654;" +
            "-fx-effect: dropshadow(gaussian, rgba(255,70,84,0.8), 8, 0.7, 0, 0);"
        );
        
        centerBox.getChildren().addAll(timerBox, vsLabel);
        
        // Người chơi B (Phải)
        HBox playerBBox = createPlayerBox(playerBAvatar, playerBNameLabel, playerBScoreLabel, false);
        
        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);
        
        // Nút thoát
        javafx.scene.control.Button quitButton = new javafx.scene.control.Button("✖ THOÁT");
        quitButton.setStyle(
            "-fx-font-family: 'Arial Black', sans-serif;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #ffffff;" +
            "-fx-background-color: linear-gradient(to bottom, #e84057, #d13447, #b82846);" +
            "-fx-background-radius: 8px;" +
            "-fx-border-color: #ff546c;" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-padding: 8px 20px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(232,64,87,0.6), 10, 0.7, 0, 3);"
        );
        quitButton.setOnMouseEntered(e -> quitButton.setStyle(
            "-fx-font-family: 'Arial Black', sans-serif;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #ffffff;" +
            "-fx-background-color: linear-gradient(to bottom, #ff546c, #e84057, #d13447);" +
            "-fx-background-radius: 8px;" +
            "-fx-border-color: #ff6a7f;" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-padding: 8px 20px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(255,84,108,0.9), 15, 0.8, 0, 0);"
        ));
        quitButton.setOnMouseExited(e -> quitButton.setStyle(
            "-fx-font-family: 'Arial Black', sans-serif;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #ffffff;" +
            "-fx-background-color: linear-gradient(to bottom, #e84057, #d13447, #b82846);" +
            "-fx-background-radius: 8px;" +
            "-fx-border-color: #ff546c;" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-padding: 8px 20px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(232,64,87,0.6), 10, 0.7, 0, 3);"
        ));
        quitButton.setOnAction(e -> {
            if (onQuitGame != null) {
                onQuitGame.run();
            }
        });
        
        topRow.getChildren().addAll(leftSpacer, playerABox, centerBox, playerBBox, rightSpacer, quitButton);
        
        header.getChildren().add(topRow);
        
        return header;
    }
    
    private HBox createPlayerBox(Circle avatar, Label nameLabel, Label scoreLabel, boolean isLeft) {
        HBox playerBox = new HBox(12);
        playerBox.setAlignment(isLeft ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        playerBox.setPadding(new Insets(8, 15, 8, 15));
        playerBox.setStyle(
            "-fx-background-color: linear-gradient(to " + (isLeft ? "right" : "left") + ", rgba(255,255,255,0.08), rgba(255,255,255,0.02));" +
            "-fx-background-radius: 15px;" +
            "-fx-border-color: rgba(255,255,255,0.15);" +
            "-fx-border-radius: 15px;" +
            "-fx-border-width: 1;"
        );
        
        // Vòng tròn avatar với gradient
        avatar.setFill(new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#667eea")),
            new Stop(1, Color.web("#764ba2"))
        ));
        avatar.setStroke(Color.web("#FFFFFF"));
        avatar.setStrokeWidth(2.5);
        avatar.setEffect(new DropShadow(15, Color.web("#667eea", 0.6)));
        
        // Nhãn tên
        nameLabel.setStyle(
            "-fx-font-family: 'Arial', sans-serif;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFFFFF;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 3, 0.7, 0, 1);"
        );
        nameLabel.setText("Player");
        
        // Hộp điểm số
        StackPane scoreBox = new StackPane();
        scoreBox.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #f39c12, #e67e22);" +
            "-fx-background-radius: 12px;" +
            "-fx-padding: 4px 12px;" +
            "-fx-effect: dropshadow(gaussian, rgba(230,126,34,0.6), 8, 0.7, 0, 2);"
        );
        
        scoreLabel.setStyle(
            "-fx-font-family: 'Arial Black', sans-serif;" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #FFFFFF;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 2, 0.7, 0, 1);"
        );
        scoreBox.getChildren().add(scoreLabel);
        
        VBox infoBox = new VBox(3);
        infoBox.setAlignment(isLeft ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        infoBox.getChildren().addAll(nameLabel, scoreBox);
        
        if (isLeft) {
            playerBox.getChildren().addAll(avatar, infoBox);
        } else {
            playerBox.getChildren().addAll(infoBox, avatar);
        }
        
        return playerBox;
    }
    
    private void setupBorderGlowAnimation() {
        // Phương thức này sẽ được gọi để tạo hiệu ứng viền khi đến lượt người chơi
        // Hiện tại, tạo một hiệu ứng nhấp nháy đơn giản
        glowAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(timerBox.scaleXProperty(), 1.0),
                new KeyValue(timerBox.scaleYProperty(), 1.0)
            ),
            new KeyFrame(Duration.seconds(0.5), 
                new KeyValue(timerBox.scaleXProperty(), 1.08),
                new KeyValue(timerBox.scaleYProperty(), 1.08)
            ),
            new KeyFrame(Duration.seconds(1.0), 
                new KeyValue(timerBox.scaleXProperty(), 1.0),
                new KeyValue(timerBox.scaleYProperty(), 1.0)
            )
        );
        glowAnimation.setCycleCount(Animation.INDEFINITE);
    }

    private void drawBase() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        
        // Xóa canvas
        g.setFill(Color.TRANSPARENT);
        g.fillRect(0, 0, 900, 450);
        
        // Vẽ khung ngoài theo phong cách lục giác với gradient
        drawHexagonalFrame(g, boxX1, boxY, boxSize);
        drawHexagonalFrame(g, boxX2, boxY, boxSize);
        
        // Vẽ nền trắng cho ảnh
        g.setFill(Color.WHITE);
        g.fillRoundRect(boxX1, boxY, boxSize, boxSize, 15, 15);
        g.fillRoundRect(boxX2, boxY, boxSize, boxSize, 15, 15);
        
        // Vẽ ảnh với cắt xén
        if (leftImg != null) {
            g.save();
            g.beginPath();
            g.rect(boxX1, boxY, boxSize, boxSize);
            g.closePath();
            g.clip();
            g.drawImage(leftImg, boxX1, boxY, boxSize, boxSize);
            g.restore();
        } else {
            g.setFill(Color.web("#2c3e50"));
            g.fillRoundRect(boxX1, boxY, boxSize, boxSize, 15, 15);
        }
        
        if (rightImg != null) {
            g.save();
            g.beginPath();
            g.rect(boxX2, boxY, boxSize, boxSize);
            g.closePath();
            g.clip();
            g.drawImage(rightImg, boxX2, boxY, boxSize, boxSize);
            g.restore();
        } else {
            g.setFill(Color.web("#2c3e50"));
            g.fillRoundRect(boxX2, boxY, boxSize, boxSize, 15, 15);
        }
        
        // Viền phát sáng bên trong (phong cách Riot)
        g.setStroke(Color.web("#0ac8b9", 0.8));
        g.setLineWidth(4);
        g.strokeRoundRect(boxX1 + 2, boxY + 2, boxSize - 4, boxSize - 4, 15, 15);
        g.strokeRoundRect(boxX2 + 2, boxY + 2, boxSize - 4, boxSize - 4, 15, 15);
        
        // Viền ngoài
        g.setStroke(Color.web("#FFFFFF"));
        g.setLineWidth(3);
        g.strokeRoundRect(boxX1, boxY, boxSize, boxSize, 15, 15);
        g.strokeRoundRect(boxX2, boxY, boxSize, boxSize, 15, 15);
    }
    
    private void drawHexagonalFrame(GraphicsContext g, double x, double y, double size) {
        // Vẽ hiệu ứng phát sáng bên ngoài
        double padding = 12;
        g.setStroke(Color.web("#0ac8b9", 0.3));
        g.setLineWidth(8);
        g.strokeRoundRect(x - padding, y - padding, size + padding * 2, size + padding * 2, 20, 20);
        
        g.setStroke(Color.web("#0077d4", 0.2));
        g.setLineWidth(12);
        g.strokeRoundRect(x - padding - 4, y - padding - 4, size + padding * 2 + 8, size + padding * 2 + 8, 22, 22);
    }

    public void updateFromPayload(Map<?,?> p) {
        try {
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
                
                // Dừng đếm ngược nếu game sắp kết thúc (remainingTurnMs = 0 hoặc âm)
                if (remainingSeconds <= 0 || ms <= 0) {
                    countdownTimer.stop();
                } else {
                    countdownTimer.stop();
                    countdownTimer.playFromStart();
                }
                updateTimerDisplay();
            }
            
            if (p.get("found") instanceof List<?> f) {
                int oldFoundCount = found.size();
                found.clear();
                
                Map<String,Object> latestFind = null;
                for (Object o: f) {
                    if (o instanceof Map<?,?> m) {
                        @SuppressWarnings("unchecked")
                        Map<String,Object> findMap = (Map<String,Object>) m;
                        found.add(findMap);
                        latestFind = findMap;
                    }
                }
                
                System.out.println("📊 Found differences count: " + found.size() + " (was: " + oldFoundCount + ")");
                
                if (found.size() > oldFoundCount && latestFind != null) {
                    String finder = latestFind.get("finder") != null ? String.valueOf(latestFind.get("finder")) : "";
                    if (finder.equals(myUsername) && audioService != null) {
                        audioService.playCorrectSound();
                        System.out.println("🔊 Phát âm thanh đúng - người chơi tìm thấy điểm khác biệt!");
                    }
                    justClicked = false;
                } else if (justClicked && found.size() == oldFoundCount) {
                    if (audioService != null) {
                        audioService.playWrongSound();
                        System.out.println("🔊 Phát âm thanh sai - click nhưng không trúng!");
                    }
                    justClicked = false;
                } else if (turnChanged) {
                    justClicked = false;
                }
            }
            render();
        } catch (Exception e) {
            System.err.println("Lỗi trong GameView.updateFromPayload: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateTimerDisplay() {
        boolean isMyTurn = nextTurn.equals(myUsername);
        
        // Cập nhật đồng hồ
        timerLabel.setText(String.valueOf(remainingSeconds));
        
        // Cập nhật chỉ báo lượt
        if (isMyTurn) {
            turnIndicator.setText("⚡ LƯỢT CỦA BẠN ⚡");
            turnIndicator.setStyle(
                "-fx-font-family: 'Arial Black', sans-serif;" +
                "-fx-font-size: 24px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #FFFFFF;" +
                "-fx-padding: 8px 40px;" +
                "-fx-background-color: linear-gradient(to right, #0ac8b9, #0077d4);" +
                "-fx-background-radius: 25px;" +
                "-fx-effect: dropshadow(gaussian, rgba(10,200,185,0.9), 20, 0.8, 0, 0);"
            );
            
            // Bắt đầu hiệu ứng phát sáng
            if (glowAnimation != null && glowAnimation.getStatus() != Animation.Status.RUNNING) {
                glowAnimation.play();
            }
        } else {
            turnIndicator.setText("⏳ LƯỢT ĐỐI THỦ");
            turnIndicator.setStyle(
                "-fx-font-family: 'Arial', sans-serif;" +
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #95a5a6;" +
                "-fx-padding: 8px 40px;" +
                "-fx-background-color: rgba(255,255,255,0.1);" +
                "-fx-background-radius: 25px;" +
                "-fx-border-color: rgba(149,165,166,0.3);" +
                "-fx-border-radius: 25px;" +
                "-fx-border-width: 2;"
            );
            
            // Dừng hiệu ứng phát sáng
            if (glowAnimation != null) {
                glowAnimation.stop();
                timerBox.setScaleX(1.0);
                timerBox.setScaleY(1.0);
            }
        }
        
        // Trạng thái cảnh báo khi thời gian ít
        if (remainingSeconds <= 5 && isMyTurn) {
            timerBox.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #ff4654, #c0392b);" +
                "-fx-border-color: #ff4654;" +
                "-fx-border-width: 3;" +
                "-fx-border-radius: 40;" +
                "-fx-background-radius: 40;" +
                "-fx-effect: dropshadow(gaussian, rgba(255,70,84,0.9), 25, 0.8, 0, 0);"
            );
            timerLabel.setStyle(
                "-fx-font-family: 'Arial Black', sans-serif;" +
                "-fx-font-size: 32px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #FFFFFF;" +
                "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.8), 8, 0.7, 0, 0);"
            );
        } else {
            timerBox.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #1a1a2e, #0f0f1e);" +
                "-fx-border-color: #0ac8b9;" +
                "-fx-border-width: 3;" +
                "-fx-border-radius: 40;" +
                "-fx-background-radius: 40;" +
                "-fx-effect: dropshadow(gaussian, rgba(10,200,185,0.6), 20, 0.7, 0, 0);"
            );
            timerLabel.setStyle(
                "-fx-font-family: 'Arial Black', sans-serif;" +
                "-fx-font-size: 32px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #0ac8b9;" +
                "-fx-effect: dropshadow(gaussian, rgba(10,200,185,0.8), 8, 0.7, 0, 0);"
            );
        }
    }

    private void render() {
        drawBase();
        GraphicsContext g = canvas.getGraphicsContext2D();
        
        // Vẽ các điểm khác biệt đã tìm thấy với hiệu ứng phong cách Riot
        for (Map<String,Object> d : found) {
            double x = ((Number)d.get("x")).doubleValue();
            double y = ((Number)d.get("y")).doubleValue();
            double r = ((Number)d.get("radius")).doubleValue();
            double sx = x * (boxSize / imgW);
            double sy = y * (boxSize / imgH);
            double rr = r * (boxSize / imgW);
            
            String finder = d.get("finder") != null ? String.valueOf(d.get("finder")) : "";
            Color circleColor;
            Color glowColor;
            
            if (finder.equals(myUsername)) {
                circleColor = Color.web("#0ac8b9"); // Xanh lục cho người chơi
                glowColor = Color.web("#0ac8b9", 0.4);
            } else if (finder.equals(playerA) || finder.equals(playerB)) {
                circleColor = Color.web("#ff4654"); // Đỏ cho đối thủ
                glowColor = Color.web("#ff4654", 0.4);
            } else {
                circleColor = Color.web("#f39c12"); // Cam cho người khác
                glowColor = Color.web("#f39c12", 0.4);
            }
            
            // Phát sáng ngoài (lớn nhất)
            g.setStroke(glowColor.deriveColor(0, 1, 1, 0.1));
            g.setLineWidth(20);
            g.strokeOval(boxX1 + sx - rr - 8, boxY + sy - rr - 8, rr*2 + 16, rr*2 + 16);
            g.strokeOval(boxX2 + sx - rr - 8, boxY + sy - rr - 8, rr*2 + 16, rr*2 + 16);
            
            // Phát sáng giữa
            g.setStroke(glowColor.deriveColor(0, 1, 1, 0.3));
            g.setLineWidth(12);
            g.strokeOval(boxX1 + sx - rr - 4, boxY + sy - rr - 4, rr*2 + 8, rr*2 + 8);
            g.strokeOval(boxX2 + sx - rr - 4, boxY + sy - rr - 4, rr*2 + 8, rr*2 + 8);
            
            // Phát sáng trong
            g.setStroke(circleColor.deriveColor(0, 1, 1.2, 0.6));
            g.setLineWidth(7);
            g.strokeOval(boxX1 + sx - rr - 1, boxY + sy - rr - 1, rr*2 + 2, rr*2 + 2);
            g.strokeOval(boxX2 + sx - rr - 1, boxY + sy - rr - 1, rr*2 + 2, rr*2 + 2);
            
            // Vòng tròn chính (sáng)
            g.setStroke(circleColor);
            g.setLineWidth(4);
            g.strokeOval(boxX1 + sx - rr, boxY + sy - rr, rr*2, rr*2);
            g.strokeOval(boxX2 + sx - rr, boxY + sy - rr, rr*2, rr*2);
        }
        
        
        // Cập nhật tên và điểm số người chơi
        updatePlayerInfo();
    }
    
    private void updatePlayerInfo() {
        // Cập nhật tên người chơi
        if (!playerA.isEmpty()) {
            playerANameLabel.setText(playerA.equals(myUsername) ? playerA + " (BẠN)" : playerA);
        }
        if (!playerB.isEmpty()) {
            playerBNameLabel.setText(playerB.equals(myUsername) ? playerB + " (BẠN)" : playerB);
        }
        
        // Cập nhật điểm số
        playerAScoreLabel.setText(String.valueOf(scoreA));
        playerBScoreLabel.setText(String.valueOf(scoreB));
        
        // Làm nổi bật hộp người chơi hiện tại
        if (playerA.equals(myUsername)) {
            styleActivePlayer(playerAAvatar, true);
            styleActivePlayer(playerBAvatar, false);
        } else if (playerB.equals(myUsername)) {
            styleActivePlayer(playerAAvatar, false);
            styleActivePlayer(playerBAvatar, true);
        }
    }
    
    private void styleActivePlayer(Circle avatar, boolean isActive) {
        if (isActive) {
            avatar.setEffect(new DropShadow(20, Color.web("#0ac8b9", 0.8)));
            avatar.setStrokeWidth(3);
            avatar.setStroke(Color.web("#0ac8b9"));
        } else {
            avatar.setEffect(new DropShadow(15, Color.web("#667eea", 0.6)));
            avatar.setStrokeWidth(2.5);
            avatar.setStroke(Color.web("#FFFFFF"));
        }
    }

    public StackPane getRoot() { 
        return root; 
    }

    public void setImages(byte[] leftBytes, byte[] rightBytes, int width, int height) {
        System.out.println("GameView.setImages được gọi: leftBytes=" + (leftBytes != null ? leftBytes.length : "null") + ", rightBytes=" + (rightBytes != null ? rightBytes.length : "null") + ", w=" + width + ", h=" + height);
        this.imgW = width > 0 ? width : this.imgW;
        this.imgH = height > 0 ? height : this.imgH;
        try {
            this.leftImg = leftBytes != null ? new Image(new ByteArrayInputStream(leftBytes)) : null;
            this.rightImg = rightBytes != null ? new Image(new ByteArrayInputStream(rightBytes)) : null;
            System.out.println("Ảnh được tạo: leftImg=" + (leftImg != null) + ", rightImg=" + (rightImg != null));
            if (leftImg != null) System.out.println("Kích thước ảnh trái: " + leftImg.getWidth() + "x" + leftImg.getHeight());
            if (rightImg != null) System.out.println("Kích thước ảnh phải: " + rightImg.getWidth() + "x" + rightImg.getHeight());
        } catch (Exception e) {
            System.err.println("Lỗi tạo Image từ bytes: " + e.getMessage());
            e.printStackTrace();
        }
        render();
    }
    
    public void cleanup() {
        // Dừng tất cả hoạt hình và đồng hồ
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        if (glowAnimation != null) {
            glowAnimation.stop();
        }
        System.out.println("GameView cleanup hoàn thành");
    }
}

package com.ltm.game.admin;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class UploaderApp extends Application {
    private Image leftImg; private Image rightImg;
    private File leftFile; private File rightFile;
    private final List<ImageSetRepository.DPoint> points = new ArrayList<>();
    private final Canvas overlay = new Canvas(900, 450);
    private final TextField nameField = new TextField();
    private final Spinner<Integer> radiusSpinner = new Spinner<>(5, 100, 40); // Increased default to 40, max to 100
    private final Label status = new Label();
    private Label pointsCountLabel; // Track points count label

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        stage.setTitle("⚔️ ADMIN PANEL - Spot The Difference");
        
        // Legacy preview ImageViews (no longer shown; we now draw directly on the Canvas)
        var leftView = new ImageView();
        var rightView = new ImageView();
        leftView.setFitWidth(400); leftView.setFitHeight(400); leftView.setPreserveRatio(true);
        rightView.setFitWidth(400); rightView.setFitHeight(400); rightView.setPreserveRatio(true);

        // LoL-styled buttons
        Button loadLeft = createLoLButton("📂  CHỌN ẢNH TRÁI", false);
        Button loadRight = createLoLButton("📂  CHỌN ẢNH PHẢI", false);
        Button clearPts = createLoLButton("🗑️  XÓA TẤT CẢ", false);
        Button saveBtn = createLoLButton("💾  LƯU", true);

        nameField.setPromptText("Nhập tên bộ ảnh...");
        nameField.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-padding: 12px 15px;" +
            "-fx-background-color: rgba(20, 30, 50, 0.7);" +
            "-fx-text-fill: #F0E6D2;" +
            "-fx-prompt-text-fill: #8B7355;" +
            "-fx-background-radius: 8px;" +
            "-fx-border-color: rgba(212, 175, 55, 0.5);" +
            "-fx-border-width: 1.5px;" +
            "-fx-border-radius: 8px;"
        );

        loadLeft.setOnAction(e -> chooseImage(stage, leftView, true));
        loadRight.setOnAction(e -> chooseImage(stage, rightView, false));
        clearPts.setOnAction(e -> { points.clear(); redraw(leftView, rightView); updatePointsCount(); });
        saveBtn.setOnAction(e -> saveToDb());

        overlay.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (leftImg == null || rightImg == null) return;
            // Accept clicks inside either 400x400 preview: left at (10,10) or right at (440,10)
            double cx = e.getX() - 10; double cy = e.getY() - 10; // left box local
            boolean inLeft = !(cx < 0 || cy < 0 || cx > 400 || cy > 400);
            double rx = e.getX() - 440; double ry = e.getY() - 10; // right box local
            boolean inRight = !(rx < 0 || ry < 0 || rx > 400 || ry > 400);
            if (!inLeft && !inRight) return;
            // Use coordinates relative to whichever box was clicked (both images assumed same size)
            double localX = inLeft ? cx : rx;
            double localY = inLeft ? cy : ry;
            // Normalize to original image pixels
            double ix = (localX / 400.0) * leftImg.getWidth();
            double iy = (localY / 400.0) * leftImg.getHeight();
            int rDisp = radiusSpinner.getValue();
            int rImg = (int)Math.round(rDisp * (leftImg.getWidth() / 400.0));
            points.add(new ImageSetRepository.DPoint(ix, iy, rImg));
            redraw(leftView, rightView);
            updatePointsCount();
        });

        BorderPane root = new BorderPane();
        root.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #0A1428, #091428);"
        );
        
        // Elegant LoL-styled Header with hextech pattern
        VBox header = new VBox(12);
        header.setPadding(new Insets(25, 30, 20, 30));
        header.setStyle(
            "-fx-background-color: linear-gradient(to right, rgba(10, 20, 40, 0.95), rgba(15, 25, 45, 0.98));" +
            "-fx-border-color: linear-gradient(to right, #C89B3C, #785A28);" +
            "-fx-border-width: 0 0 3 0;" +
            "-fx-effect: dropshadow(gaussian, rgba(200, 155, 60, 0.3), 15, 0.4, 0, 3);"
        );
        
        Label titleLabel = new Label("⚔️  BẢNG ĐIỀU KHIỂN QUẢN TRỊ  ⚔️");
        titleLabel.setStyle(
            "-fx-font-size: 32px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #D4AF37;" +
            "-fx-effect: dropshadow(gaussian, rgba(212, 175, 55, 0.8), 10, 0.8, 0, 2);"
        );
        
        Label subtitle = new Label("HỆ THỐNG QUẢN LÝ BỘ ẢNH");
        subtitle.setStyle(
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #8B7355;" +
            "-fx-letter-spacing: 3px;"
        );
        
        Label tip = new Label("💡 Click vào vùng ảnh để đánh dấu điểm khác biệt • Điều chỉnh bán kính bên phải");
        tip.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #C8AA6E;" +
            "-fx-font-style: italic;"
        );
        tip.setWrapText(true);
        
        header.getChildren().addAll(titleLabel, subtitle, tip);
        root.setTop(header);

        // Right panel - Elegant LoL styled control panel
        VBox tools = new VBox(18);
        tools.setPadding(new Insets(25, 20, 25, 20));
        tools.setStyle(
            "-fx-background-color: linear-gradient(to bottom, rgba(15, 25, 45, 0.92), rgba(10, 20, 40, 0.95));" +
            "-fx-border-color: rgba(212, 175, 55, 0.25);" +
            "-fx-border-width: 0 0 0 2;" +
            "-fx-effect: innershadow(gaussian, rgba(0, 0, 0, 0.4), 10, 0.3, 0, 0);"
        );
        tools.setMinWidth(320);
        
        // Name field section with icon
        HBox nameHeaderBox = new HBox(8);
        nameHeaderBox.setAlignment(Pos.CENTER_LEFT);
        Label nameIcon = new Label("📋");
        nameIcon.setStyle("-fx-font-size: 16px;");
        Label nameLabel = new Label("THÔNG TIN BỘ ẢNH");
        nameLabel.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #D4AF37;" +
            "-fx-letter-spacing: 2px;"
        );
        nameHeaderBox.getChildren().addAll(nameIcon, nameLabel);
        
        // Buttons section with icon
        HBox uploadHeaderBox = new HBox(8);
        uploadHeaderBox.setAlignment(Pos.CENTER_LEFT);
        Label uploadIcon = new Label("📤");
        uploadIcon.setStyle("-fx-font-size: 16px;");
        Label uploadLabel = new Label("TẢI ẢNH LÊN");
        uploadLabel.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #D4AF37;" +
            "-fx-letter-spacing: 2px;"
        );
        uploadHeaderBox.getChildren().addAll(uploadIcon, uploadLabel);
        VBox uploadBox = new VBox(10, loadLeft, loadRight);
        
        // Radius control section with icon
        HBox radiusHeaderBox = new HBox(8);
        radiusHeaderBox.setAlignment(Pos.CENTER_LEFT);
        Label radiusIcon = new Label("⚙️");
        radiusIcon.setStyle("-fx-font-size: 16px;");
        Label radiusLabel = new Label("CÀI ĐẶT ĐÁNH DẤU");
        radiusLabel.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #D4AF37;" +
            "-fx-letter-spacing: 2px;"
        );
        radiusHeaderBox.getChildren().addAll(radiusIcon, radiusLabel);
        
        radiusSpinner.setPrefWidth(140);
        radiusSpinner.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-background-color: rgba(20, 30, 50, 0.8);" +
            "-fx-text-fill: #F0E6D2;" +
            "-fx-border-color: rgba(212, 175, 55, 0.5);" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 8px;"
        );
        
        clearPts.setMaxWidth(Region.USE_PREF_SIZE);
        clearPts.setPrefWidth(140);
        
        HBox radiusBox = new HBox(12, radiusSpinner, clearPts);
        radiusBox.setAlignment(Pos.CENTER_LEFT);
        
        // Points count label - more prominent
        pointsCountLabel = new Label("📍 ĐIỂM ĐÃ ĐÁNH DẤU: 0");
        pointsCountLabel.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #F0E6D2;" +
            "-fx-padding: 15px 18px;" +
            "-fx-background-color: linear-gradient(to right, rgba(212, 175, 55, 0.15), rgba(200, 155, 60, 0.12));" +
            "-fx-background-radius: 8px;" +
            "-fx-border-color: rgba(212, 175, 55, 0.4);" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-effect: dropshadow(gaussian, rgba(212, 175, 55, 0.3), 8, 0.5, 0, 2);"
        );
        
        
        Separator sep = new Separator();
        sep.setStyle(
            "-fx-background-color: rgba(212, 175, 55, 0.3);" +
            "-fx-padding: 5px 0;"
        );
        
        // Status label - cleaner design
        status.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #C8AA6E;" +
            "-fx-padding: 15px;" +
            "-fx-background-color: rgba(20, 30, 50, 0.6);" +
            "-fx-background-radius: 8px;" +
            "-fx-border-color: rgba(212, 175, 55, 0.25);" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 8px;"
        );
        status.setWrapText(true);
        status.setPrefHeight(110);
        status.setVisible(false); // Initially hidden
        status.setManaged(false); // Don't take up space when hidden
        
        // Listener to show/hide status based on text
        status.textProperty().addListener((obs, oldText, newText) -> {
            boolean hasText = newText != null && !newText.trim().isEmpty();
            status.setVisible(hasText);
            status.setManaged(hasText);
        });
        
        tools.getChildren().addAll(
            nameHeaderBox, nameField,
            uploadHeaderBox, uploadBox,
            radiusHeaderBox, radiusBox,
            pointsCountLabel,
            sep,
            saveBtn,
            status
        );
        
        root.setLeft(tools);
        root.setCenter(overlay);

        Scene scene = new Scene(root, 1200, 560);
        stage.setScene(scene);
        stage.show();
    }
    
    private Button createLoLButton(String text, boolean isPrimary) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        
        if (isPrimary) {
            btn.setStyle(
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 14px 24px;" +
                "-fx-background-color: linear-gradient(to bottom, #D4AF37, #C89B3C 50%, #785A28);" +
                "-fx-text-fill: #0A1428;" +
                "-fx-background-radius: 8px;" +
                "-fx-cursor: hand;" +
                "-fx-border-color: rgba(212, 175, 55, 0.6);" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 8px;" +
                "-fx-effect: dropshadow(gaussian, rgba(212, 175, 55, 0.6), 12, 0.7, 0, 3);"
            );
        } else {
            btn.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 11px 20px;" +
                "-fx-background-color: rgba(20, 30, 50, 0.7);" +
                "-fx-text-fill: #D4AF37;" +
                "-fx-background-radius: 8px;" +
                "-fx-border-color: rgba(212, 175, 55, 0.4);" +
                "-fx-border-radius: 8px;" +
                "-fx-border-width: 1.5px;" +
                "-fx-cursor: hand;"
            );
        }
        
        btn.setOnMouseEntered(e -> {
            if (isPrimary) {
                btn.setStyle(
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 14px 24px;" +
                    "-fx-background-color: linear-gradient(to bottom, #E5C158, #D4AF37 50%, #8B6F3C);" +
                    "-fx-text-fill: #0A1428;" +
                    "-fx-background-radius: 8px;" +
                    "-fx-cursor: hand;" +
                    "-fx-border-color: rgba(229, 193, 88, 0.8);" +
                    "-fx-border-width: 1.5px;" +
                    "-fx-border-radius: 8px;" +
                    "-fx-effect: dropshadow(gaussian, rgba(229, 193, 88, 0.8), 15, 0.8, 0, 3);"
                );
            } else {
                btn.setStyle(
                    "-fx-font-size: 13px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 11px 20px;" +
                    "-fx-background-color: rgba(30, 40, 60, 0.85);" +
                    "-fx-text-fill: #E5C158;" +
                    "-fx-background-radius: 8px;" +
                    "-fx-border-color: rgba(229, 193, 88, 0.6);" +
                    "-fx-border-radius: 8px;" +
                    "-fx-border-width: 1.5px;" +
                    "-fx-cursor: hand;"
                );
            }
        });
        btn.setOnMouseExited(e -> {
            if (isPrimary) {
                btn.setStyle(
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 14px 24px;" +
                    "-fx-background-color: linear-gradient(to bottom, #D4AF37, #C89B3C 50%, #785A28);" +
                    "-fx-text-fill: #0A1428;" +
                    "-fx-background-radius: 8px;" +
                    "-fx-cursor: hand;" +
                    "-fx-border-color: rgba(212, 175, 55, 0.6);" +
                    "-fx-border-width: 1.5px;" +
                    "-fx-border-radius: 8px;" +
                    "-fx-effect: dropshadow(gaussian, rgba(212, 175, 55, 0.6), 12, 0.7, 0, 3);"
                );
            } else {
                btn.setStyle(
                    "-fx-font-size: 13px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 11px 20px;" +
                    "-fx-background-color: rgba(20, 30, 50, 0.7);" +
                    "-fx-text-fill: #D4AF37;" +
                    "-fx-background-radius: 8px;" +
                    "-fx-border-color: rgba(212, 175, 55, 0.4);" +
                    "-fx-border-radius: 8px;" +
                    "-fx-border-width: 1.5px;" +
                    "-fx-cursor: hand;"
                );
            }
        });
        
        return btn;
    }

    private void chooseImage(Stage stage, ImageView view, boolean left) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            try {
                Image img = new Image(new FileInputStream(file));
                view.setImage(img);
                if (left) { leftImg = img; leftFile = file; }
                else { rightImg = img; rightFile = file; }
                redraw(null, null);
            } catch (Exception ex) {
                status.setText("Lỗi mở ảnh: "+ex.getMessage());
            }
        }
    }

    private void redraw(ImageView leftView, ImageView rightView) {
        GraphicsContext g = overlay.getGraphicsContext2D();
        g.clearRect(0,0,overlay.getWidth(), overlay.getHeight());
        // Draw elegant gradient background
        g.setFill(Color.web("#0A1428"));
        g.fillRect(0,0,overlay.getWidth(), overlay.getHeight());
        
        // Draw subtle hextech pattern lines
        g.setStroke(Color.web("#1A2538", 0.3));
        g.setLineWidth(1);
        for (int i = 0; i < 450; i += 30) {
            g.strokeLine(0, i, 900, i);
        }
        
        // Draw previews with elegant frames
        if (leftImg != null) {
            g.drawImage(leftImg, 15, 15, 390, 390);
        } else {
            drawPlaceholder(g, 15, 15, 390, 390, "⬅️  ẢNH TRÁI");
        }
        if (rightImg != null) {
            g.drawImage(rightImg, 445, 15, 390, 390);
        } else {
            drawPlaceholder(g, 445, 15, 390, 390, "➡️  ẢNH PHẢI");
        }
        
        // Elegant LoL-styled frames with double border
        g.setStroke(Color.web("#D4AF37"));
        g.setLineWidth(3);
        g.strokeRect(15, 15, 390, 390);
        g.strokeRect(445, 15, 390, 390);
        
        // Inner glow effect
        g.setStroke(Color.web("#785A28", 0.4));
        g.setLineWidth(1);
        g.strokeRect(13, 13, 394, 394);
        g.strokeRect(443, 13, 394, 394);
        
        // Elegant footer with gradient
        g.setFill(Color.web("#0F1F35", 0.95));
        g.fillRect(15, 405, 390, 30);
        g.fillRect(445, 405, 390, 30);
        
        // Footer border
        g.setStroke(Color.web("#D4AF37", 0.5));
        g.setLineWidth(1);
        g.strokeLine(15, 405, 405, 405);
        g.strokeLine(445, 405, 835, 405);
        
        g.setFill(Color.web("#D4AF37"));
        String leftMeta = (leftFile!=null? leftFile.getName(): "");
        String rightMeta = (rightFile!=null? rightFile.getName(): "");
        if (leftImg != null) leftMeta += String.format("  [%dx%d]", (int)leftImg.getWidth(), (int)leftImg.getHeight());
        if (rightImg != null) rightMeta += String.format("  [%dx%d]", (int)rightImg.getWidth(), (int)rightImg.getHeight());
        g.fillText(leftMeta.isEmpty()? "": leftMeta, 22, 425);
        g.fillText(rightMeta.isEmpty()? "": rightMeta, 452, 425);
        
        // Draw difference points with glowing effect
        if (leftImg != null) {
            for (var p : points) {
                // Convert image-space coordinates to preview space (390x390)
                double dx = (p.x / leftImg.getWidth()) * 390.0;
                double dy = (p.y / leftImg.getHeight()) * 390.0;
                double rr = p.radius * (390.0 / leftImg.getWidth());
                
                // Outer glow
                g.setStroke(Color.web("#D4AF37", 0.3));
                g.setLineWidth(6);
                g.strokeOval(15 + dx - rr, 15 + dy - rr, rr*2, rr*2);
                g.strokeOval(445 + dx - rr, 15 + dy - rr, rr*2, rr*2);
                
                // Main circle
                g.setStroke(Color.web("#D4AF37"));
                g.setLineWidth(3);
                g.strokeOval(15 + dx - rr, 15 + dy - rr, rr*2, rr*2);
                g.strokeOval(445 + dx - rr, 15 + dy - rr, rr*2, rr*2);
            }
        }
    }

    private void drawPlaceholder(GraphicsContext g, double x, double y, double w, double h, String text) {
        // Elegant dark gradient background
        g.setFill(Color.web("#0F1F35"));
        g.fillRect(x, y, w, h);
        
        // Subtle inner shadow effect
        g.setFill(Color.web("#050A15", 0.3));
        g.fillRect(x, y, w, 50);
        
        // Elegant dashed border
        g.setStroke(Color.web("#785A28", 0.8));
        g.setLineWidth(3);
        g.setLineDashes(15, 10);
        g.strokeRect(x+8, y+8, w-16, h-16);
        g.setLineDashes(0);
        
        // Center icon - large emoji
        g.setFont(javafx.scene.text.Font.font("Segoe UI Emoji", 48));
        g.setFill(Color.web("#8B7355", 0.5));
        String icon = text.contains("TRÁI") ? "📷" : "📷";
        g.fillText(icon, x + w/2 - 24, y + h/2 - 20);
        
        // Center text with better visibility
        g.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 18));
        g.setFill(Color.web("#D4AF37", 0.7));
        double textWidth = text.length() * 10;
        g.fillText(text, x + w/2 - textWidth/2, y + h/2 + 40);
        
        // Helper text
        g.setFont(javafx.scene.text.Font.font("Segoe UI", 12));
        g.setFill(Color.web("#8B7355", 0.6));
        String hint = "Click nút bên dưới để chọn ảnh";
        g.fillText(hint, x + w/2 - 110, y + h/2 + 70);
    }

    private void saveToDb() {
        try {
            if (leftImg == null || rightImg == null) { status.setText("Chọn đủ 2 ảnh trước"); return; }
            if (points.isEmpty()) { status.setText("Thêm ít nhất 1 điểm khác biệt"); return; }
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (name.isEmpty()) name = "set-"+System.currentTimeMillis();

            // Copy files into storage directory and save relative paths to DB
            java.util.Properties props = new java.util.Properties();
            try (var in = getClass().getClassLoader().getResourceAsStream("admin-config.properties")) {
                if (in != null) props.load(in);
            }
            String storageDir = props.getProperty("storage.dir", "content/imagesets");
            // Resolve to absolute path - need to go up to project root first
            File userDir = new File(System.getProperty("user.dir"));
            System.out.println("Current user.dir: " + userDir.getAbsolutePath());
            
            // Check if we're in admin/ subdirectory
            File base;
            if (userDir.getName().equals("admin")) {
                // Running from admin module, go up one level
                base = new File(userDir.getParentFile(), storageDir);
            } else {
                // Running from project root
                base = new File(userDir, storageDir);
            }
            
            System.out.println("Attempting to create directory: " + base.getAbsolutePath());
            if (!base.exists()) {
                boolean created = base.mkdirs();
                System.out.println("Directory created: " + created);
                if (!created && !base.exists()) {
                    status.setText("Không thể tạo thư mục: "+base.getAbsolutePath());
                    return;
                }
            }
            System.out.println("Storage directory exists: " + base.exists() + ", isDirectory: " + base.isDirectory());
            status.setText("Thư mục lưu trữ: "+base.getAbsolutePath());
            String uniq = String.valueOf(System.currentTimeMillis());
            String leftName = uniq+"_left"+ getExt(leftFile.getName());
            String rightName = uniq+"_right"+ getExt(rightFile.getName());
            File leftDst = new File(base, leftName);
            File rightDst = new File(base, rightName);
            
            Files.copy(leftFile.toPath(), leftDst.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.copy(rightFile.toPath(), rightDst.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            if (!leftDst.exists() || !rightDst.exists()) {
                status.setText("Copy file thất bại. Kiểm tra quyền ghi thư mục.");
                return;
            }

            int width = (int)Math.round(leftImg.getWidth());
            int height = (int)Math.round(leftImg.getHeight());

            ImageSetRepository repo = new ImageSetRepository();
            long setId = repo.insertImageSet(name, width, height, leftName, rightName);
            repo.insertDifferences(setId, points);
            status.setText("Đã lưu bộ ảnh #"+setId+" ("+leftDst.getAbsolutePath()+") với "+points.size()+" điểm");
            points.clear();
            redraw(null, null);
            updatePointsCount();
        } catch (Exception e) {
            status.setText("Lỗi: "+e.getClass().getSimpleName()+" - "+e.getMessage());
            e.printStackTrace();
        }
    }

    private void updatePointsCount() {
        if (pointsCountLabel != null) {
            pointsCountLabel.setText("📍 ĐIỂM ĐÃ ĐÁNH DẤU: " + points.size());
        }
    }

    private static String getExt(String filename) {
        int i = filename.lastIndexOf('.');
        return i>=0? filename.substring(i) : "";
    }
}

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

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        stage.setTitle("🎨 Admin - Upload Ảnh Tìm Điểm Khác Biệt");
        
        // Legacy preview ImageViews (no longer shown; we now draw directly on the Canvas)
        var leftView = new ImageView();
        var rightView = new ImageView();
        leftView.setFitWidth(400); leftView.setFitHeight(400); leftView.setPreserveRatio(true);
        rightView.setFitWidth(400); rightView.setFitHeight(400); rightView.setPreserveRatio(true);

        // Styled buttons
        Button loadLeft = createStyledButton("📂 Chọn ảnh trái", "#3498db");
        Button loadRight = createStyledButton("📂 Chọn ảnh phải", "#9b59b6");
        Button clearPts = createStyledButton("🗑 Xóa điểm", "#e74c3c");
        Button saveBtn = createStyledButton("💾 Lưu vào DB", "#27ae60");

        nameField.setPromptText("Nhập tên bộ ảnh...");
        nameField.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-padding: 10px;" +
            "-fx-background-radius: 8px;" +
            "-fx-border-color: #3498db;" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;"
        );

        loadLeft.setOnAction(e -> chooseImage(stage, leftView, true));
        loadRight.setOnAction(e -> chooseImage(stage, rightView, false));
        clearPts.setOnAction(e -> { points.clear(); redraw(leftView, rightView); });
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
        });

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #ecf0f1;");
        
        // Header with title and instructions
        VBox header = new VBox(10);
        header.setPadding(new Insets(20, 20, 15, 20));
        header.setStyle(
            "-fx-background-color: linear-gradient(to right, #3498db, #9b59b6);" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0.5, 0, 2);"
        );
        
        Label titleLabel = new Label("🎨 QUẢN TRỊ - UPLOAD BỘ ẢNH");
        titleLabel.setStyle(
            "-fx-font-size: 24px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0.7, 0, 2);"
        );
        
        Label tip = new Label("💡 Hướng dẫn: Click vào vùng ảnh để đánh dấu điểm khác biệt. Điều chỉnh bán kính vòng tròn bên phải.");
        tip.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: rgba(255,255,255,0.95);" +
            "-fx-font-style: italic;"
        );
        tip.setWrapText(true);
        
        header.getChildren().addAll(titleLabel, tip);
        root.setTop(header);

        // Right panel - tools
        VBox tools = new VBox(15);
        tools.setPadding(new Insets(20));
        tools.setStyle(
            "-fx-background-color: white;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0.5, -2, 0);"
        );
        tools.setMinWidth(280);
        
        // Name field section
        Label nameLabel = new Label("📝 Tên bộ ảnh:");
        nameLabel.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #2c3e50;"
        );
        
        // Buttons section
        Label uploadLabel = new Label("📤 Tải ảnh lên:");
        uploadLabel.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #2c3e50;"
        );
        VBox uploadBox = new VBox(8, loadLeft, loadRight);
        
        // Radius control section
        Label radiusLabel = new Label("⭕ Bán kính vòng tròn:");
        radiusLabel.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #2c3e50;"
        );
        
        radiusSpinner.setPrefWidth(120);
        radiusSpinner.setStyle(
            "-fx-font-size: 13px;"
        );
        
        HBox radiusBox = new HBox(10, radiusSpinner, clearPts);
        radiusBox.setAlignment(Pos.CENTER_LEFT);
        
        // Points count label
        Label pointsCountLabel = new Label("📍 Số điểm đã đánh dấu: 0");
        pointsCountLabel.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #7f8c8d;" +
            "-fx-padding: 8px;" +
            "-fx-background-color: #ecf0f1;" +
            "-fx-background-radius: 6px;"
        );
        
        // Update points count on redraw
        radiusSpinner.valueProperty().addListener((obs, old, val) -> {
            pointsCountLabel.setText("📍 Số điểm đã đánh dấu: " + points.size());
        });
        
        Separator sep = new Separator();
        
        // Status label
        status.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #34495e;" +
            "-fx-padding: 10px;" +
            "-fx-background-color: #f8f9fa;" +
            "-fx-background-radius: 6px;" +
            "-fx-border-color: #dee2e6;" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 6px;"
        );
        status.setWrapText(true);
        status.setPrefHeight(80);
        
        tools.getChildren().addAll(
            nameLabel, nameField,
            uploadLabel, uploadBox,
            radiusLabel, radiusBox,
            pointsCountLabel,
            sep,
            saveBtn,
            status
        );
        
        root.setRight(tools);
        root.setCenter(overlay);

        Scene scene = new Scene(root, 1200, 560);
        stage.setScene(scene);
        stage.show();
    }
    
    private Button createStyledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12px 20px;" +
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 8px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0.5, 0, 2);"
        );
        
        btn.setOnMouseEntered(e -> btn.setStyle(
            btn.getStyle() + "-fx-scale-x: 1.02; -fx-scale-y: 1.02;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
            btn.getStyle().replace("-fx-scale-x: 1.02; -fx-scale-y: 1.02;", "")
        ));
        
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
        // Draw background
        g.setFill(Color.web("#f7f7f7"));
        g.fillRect(0,0,overlay.getWidth(), overlay.getHeight());
        // Draw previews (images first so guides and points are on top)
        if (leftImg != null) {
            g.drawImage(leftImg, 10, 10, 400, 400);
        } else {
            drawPlaceholder(g, 10, 10, 400, 400, "Chọn ảnh trái");
        }
        if (rightImg != null) {
            g.drawImage(rightImg, 440, 10, 400, 400);
        } else {
            drawPlaceholder(g, 440, 10, 400, 400, "Chọn ảnh phải");
        }
        // Boxes
        g.setStroke(Color.GRAY);
        g.strokeRect(10, 10, 400, 400);
        g.strokeRect(440,10, 400, 400);
        // Filenames + dims footer
        g.setFill(Color.color(0,0,0,0.6));
        g.fillRect(10, 410, 400, 24);
        g.fillRect(440, 410, 400, 24);
        g.setFill(Color.WHITE);
        String leftMeta = (leftFile!=null? leftFile.getName(): "");
        String rightMeta = (rightFile!=null? rightFile.getName(): "");
        if (leftImg != null) leftMeta += String.format("  (%dx%d)", (int)leftImg.getWidth(), (int)leftImg.getHeight());
        if (rightImg != null) rightMeta += String.format("  (%dx%d)", (int)rightImg.getWidth(), (int)rightImg.getHeight());
        g.fillText(leftMeta.isEmpty()? "": leftMeta, 16, 426);
        g.fillText(rightMeta.isEmpty()? "": rightMeta, 446, 426);
        g.setStroke(Color.RED);
        if (leftImg != null) {
            for (var p : points) {
                // Convert image-space coordinates to preview space (400x400)
                double dx = (p.x / leftImg.getWidth()) * 400.0;
                double dy = (p.y / leftImg.getHeight()) * 400.0;
                double rr = p.radius * (400.0 / leftImg.getWidth());
                g.strokeOval(10 + dx - rr, 10 + dy - rr, rr*2, rr*2);
                g.strokeOval(440 + dx - rr, 10 + dy - rr, rr*2, rr*2);
            }
        }
    }

    private void drawPlaceholder(GraphicsContext g, double x, double y, double w, double h, String text) {
        g.setFill(Color.web("#ffffff"));
        g.fillRect(x, y, w, h);
        g.setStroke(Color.web("#cccccc"));
        g.setLineDashes(6);
        g.strokeRect(x+2, y+2, w-4, h-4);
        g.setLineDashes(0);
        g.setFill(Color.web("#888"));
        g.fillText(text, x + 12, y + h/2);
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
        } catch (Exception e) {
            status.setText("Lỗi: "+e.getClass().getSimpleName()+" - "+e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getExt(String filename) {
        int i = filename.lastIndexOf('.');
        return i>=0? filename.substring(i) : "";
    }
}

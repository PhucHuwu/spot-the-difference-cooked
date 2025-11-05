package com.ltm.game.client.controllers;

import com.ltm.game.shared.Message;
import com.ltm.game.shared.Protocol;
import com.ltm.game.client.services.NetworkClient;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

import java.util.Map;
import java.util.function.Consumer;

public class LoginController {
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    private NetworkClient networkClient;
    private Consumer<UserLoginData> onLoginSuccess;
    
    public static class UserLoginData {
        public final String username;
        public final int totalPoints;
        public final int totalWins;
        
        public UserLoginData(String username, int totalPoints, int totalWins) {
            this.username = username;
            this.totalPoints = totalPoints;
            this.totalWins = totalWins;
        }
    }

    public void setNetworkClient(NetworkClient client) {
        this.networkClient = client;
    }

    public void setOnLoginSuccess(Consumer<UserLoginData> callback) {
        this.onLoginSuccess = callback;
    }

    @FXML
    private void initialize() {
        if (passwordField != null) {
            passwordField.setOnAction(e -> handleLogin());
        }
        
        addFocusAnimation(usernameField);
        addFocusAnimation(passwordField);
    }
    
    private void addFocusAnimation(javafx.scene.Node node) {
        node.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), node);
            if (isNowFocused) {
                st.setToX(1.02);
                st.setToY(1.02);
            } else {
                st.setToX(1.0);
                st.setToY(1.0);
            }
            st.play();
        });
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        System.out.println("[LOGIN] handleLogin called - username: " + username);
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin");
            return;
        }
        
        if (networkClient == null) {
            System.err.println("[LOGIN] ERROR: networkClient is null!");
            showError("Lỗi kết nối mạng");
            return;
        }
        
        System.out.println("[LOGIN] Sending AUTH_LOGIN message...");
        networkClient.send(new Message(Protocol.AUTH_LOGIN, Map.of(
            "username", username,
            "password", password
        )));
        System.out.println("[LOGIN] Message sent!");
    }

    public void handleAuthResult(Map<?, ?> payload) {
        System.out.println("[LOGIN] handleAuthResult called with payload: " + payload);
        
        boolean success = Boolean.parseBoolean(String.valueOf(payload.get("success")));
        System.out.println("[LOGIN] Auth success: " + success);
        
        if (success) {
            Map<?, ?> user = (Map<?, ?>) payload.get("user");
            final String username = String.valueOf(user.get("username"));
            System.out.println("[LOGIN] User logged in: " + username);
            
            Object tp = user.get("totalPoints");
            int pointsTemp = 0;
            if (tp != null) {
                if (tp instanceof Number) {
                    pointsTemp = ((Number) tp).intValue();
                } else {
                    try {
                        pointsTemp = Integer.parseInt(String.valueOf(tp).split("\\.")[0]);
                    } catch (Exception e) {
                        pointsTemp = 0;
                    }
                }
            }
            final int totalPoints = pointsTemp;
            
            Object tw = user.get("totalWins");
            int winsTemp = 0;
            if (tw != null) {
                if (tw instanceof Number) {
                    winsTemp = ((Number) tw).intValue();
                } else {
                    try {
                        winsTemp = Integer.parseInt(String.valueOf(tw).split("\\.")[0]);
                    } catch (Exception e) {
                        winsTemp = 0;
                    }
                }
            }
            final int totalWins = winsTemp;
            
            System.out.println("[LOGIN] Stats - Points: " + totalPoints + ", Wins: " + totalWins);
            
            if (onLoginSuccess != null) {
                System.out.println("[LOGIN] Calling onLoginSuccess callback...");
                javafx.application.Platform.runLater(() -> {
                    onLoginSuccess.accept(new UserLoginData(username, totalPoints, totalWins));
                    System.out.println("[LOGIN] Callback executed!");
                });
            } else {
                System.err.println("[LOGIN] ERROR: onLoginSuccess callback is null!");
            }
        } else {
            System.err.println("[LOGIN] Login failed: " + payload.get("message"));
            showError(String.valueOf(payload.get("message")));
        }
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}


package com.ltm.game.client.controllers;

import com.ltm.game.client.services.NetworkClient;
import com.ltm.game.shared.Message;
import com.ltm.game.shared.Protocol;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Map;

public class MatchFoundController {

    @FXML
    private Circle outerRing;

    @FXML
    private Circle middleRing;

    @FXML
    private Circle innerRing;

    @FXML
    private Label countdownLabel;

    @FXML
    private Label player1Label;

    @FXML
    private Label player2Label;

    @FXML
    private Button acceptButton;

    @FXML
    private Button declineButton;

    @FXML
    private Label warningLabel;

    private NetworkClient networkClient;
    private String opponentName;
    private String username;
    private Stage dialogStage;
    private Timeline countdownTimer;
    private Timeline animationTimeline;
    private int countdown = 10;
    private boolean accepted = false;

    public void initialize() {
        setupAnimations();
    }

    public void setNetworkClient(NetworkClient client) {
        this.networkClient = client;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setOpponentName(String name) {
        this.opponentName = name;
        player1Label.setText(username != null ? username : "YOU");
        player2Label.setText(name);
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;

        // Close dialog on stage close
        stage.setOnCloseRequest(e -> {
            if (!accepted) {
                sendDeclineResponse();
            }
            cleanup();
        });
    }

    public void startCountdown() {
        countdownTimer = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> {
                countdown--;
                countdownLabel.setText(String.valueOf(countdown));

                // Warning animation when time is low
                if (countdown <= 3) {
                    countdownLabel.getStyleClass().add("warning");
                    warningLabel.setVisible(true);
                    warningLabel.setManaged(true);

                    // Pulse effect on warning
                    ScaleTransition pulse = new ScaleTransition(Duration.millis(300), countdownLabel);
                    pulse.setFromX(1.0);
                    pulse.setFromY(1.0);
                    pulse.setToX(1.15);
                    pulse.setToY(1.15);
                    pulse.setAutoReverse(true);
                    pulse.setCycleCount(2);
                    pulse.play();
                }

                if (countdown <= 0) {
                    countdownTimer.stop();
                    autoDecline();
                }
            })
        );
        countdownTimer.setCycleCount(10);
        countdownTimer.play();
    }

    private void setupAnimations() {
        // Outer ring rotation (clockwise)
        RotateTransition outerRotate = new RotateTransition(Duration.seconds(8), outerRing);
        outerRotate.setByAngle(360);
        outerRotate.setCycleCount(Animation.INDEFINITE);
        outerRotate.setInterpolator(Interpolator.LINEAR);

        // Middle ring rotation (counter-clockwise)
        RotateTransition middleRotate = new RotateTransition(Duration.seconds(6), middleRing);
        middleRotate.setByAngle(-360);
        middleRotate.setCycleCount(Animation.INDEFINITE);
        middleRotate.setInterpolator(Interpolator.LINEAR);

        // Inner ring pulse
        ScaleTransition innerPulse = new ScaleTransition(Duration.seconds(2), innerRing);
        innerPulse.setFromX(1.0);
        innerPulse.setFromY(1.0);
        innerPulse.setToX(1.1);
        innerPulse.setToY(1.1);
        innerPulse.setAutoReverse(true);
        innerPulse.setCycleCount(Animation.INDEFINITE);

        // Inner ring opacity pulse
        FadeTransition innerFade = new FadeTransition(Duration.seconds(2), innerRing);
        innerFade.setFromValue(0.6);
        innerFade.setToValue(1.0);
        innerFade.setAutoReverse(true);
        innerFade.setCycleCount(Animation.INDEFINITE);

        ParallelTransition allAnimations = new ParallelTransition(
            outerRotate, middleRotate, innerPulse, innerFade
        );
        allAnimations.play();

        // Store for cleanup
        this.animationTimeline = new Timeline();
        this.animationTimeline.getKeyFrames().add(
            new KeyFrame(Duration.ZERO, e -> allAnimations.play())
        );
    }

    @FXML
    private void handleAccept() {
        if (accepted) {
            return; // Prevent double-click
        }

        accepted = true;
        acceptButton.setDisable(true);
        declineButton.setDisable(true);

        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        // Visual feedback
        acceptButton.getStyleClass().add("accept-button:pressed");

        // Send accept to server
        networkClient.send(new Message(Protocol.MATCH_ACCEPT, Map.of()));

        // Change label to "Waiting for opponent..."
        countdownLabel.setText("✓");
        countdownLabel.getStyleClass().remove("warning");

        // Green glow effect
        ScaleTransition acceptScale = new ScaleTransition(Duration.millis(300), acceptButton);
        acceptScale.setToX(1.1);
        acceptScale.setToY(1.1);
        acceptScale.setAutoReverse(true);
        acceptScale.setCycleCount(2);
        acceptScale.play();

        // Don't close dialog yet - wait for server to start game or opponent to decline
    }

    @FXML
    private void handleDecline() {
        if (accepted) {
            return;
        }

        sendDeclineResponse();
        closeDialog();
    }

    private void sendDeclineResponse() {
        if (networkClient != null) {
            networkClient.send(new Message(Protocol.MATCH_DECLINE, Map.of()));
        }
    }

    private void autoDecline() {
        Platform.runLater(() -> {
            countdownLabel.setText("TIME OUT");
            countdownLabel.setStyle("-fx-text-fill: #FF4444;");

            // Auto decline and close after 1 second
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> {
                sendDeclineResponse();
                closeDialog();
            });
            pause.play();
        });
    }

    public void closeDialog() {
        cleanup();
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void cleanup() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        if (animationTimeline != null) {
            animationTimeline.stop();
        }
    }

    public void onOpponentDeclined(String reason, String decliner) {
        Platform.runLater(() -> {
            cleanup();
            countdownLabel.setText("✗");
            countdownLabel.setStyle("-fx-text-fill: #FF4444; -fx-font-size: 48px;");

            // Update labels with message
            if ("timeout".equals(reason)) {
                player1Label.setText("⏱ MATCH CANCELLED");
                player2Label.setText("TIMEOUT");
                player1Label.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 18px; -fx-font-weight: bold;");
                player2Label.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 18px; -fx-font-weight: bold;");
            } else if ("disconnect".equals(reason)) {
                player1Label.setText("🔌 MATCH CANCELLED");
                player2Label.setText((decliner != null ? decliner.toUpperCase() : "OPPONENT") + " DISCONNECTED");
                player1Label.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 18px; -fx-font-weight: bold;");
                player2Label.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 18px; -fx-font-weight: bold;");
            } else if (decliner != null && !decliner.isEmpty() && !"null".equals(decliner)) {
                player1Label.setText("❌ MATCH CANCELLED");
                player2Label.setText(decliner.toUpperCase() + " DECLINED");
                player1Label.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 18px; -fx-font-weight: bold;");
                player2Label.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 18px; -fx-font-weight: bold;");
            } else {
                player1Label.setText("❌ MATCH");
                player2Label.setText("CANCELLED");
                player1Label.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 18px; -fx-font-weight: bold;");
                player2Label.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 18px; -fx-font-weight: bold;");
            }

            // Hide buttons
            acceptButton.setVisible(false);
            declineButton.setVisible(false);

            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(e -> {
                closeDialog();
            });
            pause.play();
        });
    }

    public void onMatchStarting() {
        Platform.runLater(() -> {
            cleanup();

            // Show 3-2-1-GO countdown like League of Legends
            final int[] countdownValue = {3};

            Timeline goCountdown = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    if (countdownValue[0] > 0) {
                        countdownLabel.setText(String.valueOf(countdownValue[0]));
                        countdownLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 72px; -fx-font-weight: bold;");

                        // Pulse animation
                        ScaleTransition pulse = new ScaleTransition(Duration.millis(400), countdownLabel);
                        pulse.setFromX(1.5);
                        pulse.setFromY(1.5);
                        pulse.setToX(1.0);
                        pulse.setToY(1.0);
                        pulse.play();

                        countdownValue[0]--;
                    } else {
                        // Show GO!
                        countdownLabel.setText("GO!");
                        countdownLabel.setStyle("-fx-text-fill: #00FF88; -fx-font-size: 82px; -fx-font-weight: bold;");

                        ScaleTransition goAnimation = new ScaleTransition(Duration.millis(500), countdownLabel);
                        goAnimation.setFromX(2.0);
                        goAnimation.setFromY(2.0);
                        goAnimation.setToX(1.2);
                        goAnimation.setToY(1.2);
                        goAnimation.play();

                        // Close after GO!
                        PauseTransition pause = new PauseTransition(Duration.millis(500));
                        pause.setOnFinished(evt -> {
                            closeDialog();
                        });
                        pause.play();
                    }
                })
            );
            goCountdown.setCycleCount(4); // 3, 2, 1, GO
            goCountdown.play();
        });
    }
}


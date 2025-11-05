package com.ltm.game.client.services;

import javafx.scene.media.AudioClip;

public class AudioService {
    private AudioClip backgroundMusic;
    private AudioClip gameMusic;
    private AudioClip correctSound;
    private AudioClip wrongSound;
    private AudioClip celebrationSound;
    
    private boolean isMuted = false;
    private double savedBackgroundVolume = 0.3;
    private double savedGameVolume = 0.25;

    public void playBackgroundMusic() {
        stopBackgroundMusic();
        try {
            String musicPath = getClass().getResource("/sounds/y_ke_que.mp3").toExternalForm();
            backgroundMusic = new AudioClip(musicPath);
            backgroundMusic.setCycleCount(AudioClip.INDEFINITE);
            if (isMuted) {
                backgroundMusic.setVolume(0.0);
            } else {
                backgroundMusic.setVolume(savedBackgroundVolume);
            }
            backgroundMusic.play();
        } catch (Exception e) {
            // Error loading background music
        }
    }

    public void playLobbyMusic() {
        stopBackgroundMusic();
        try {
            String musicPath = getClass().getResource("/sounds/nhac_ingame.mp3").toExternalForm();
            backgroundMusic = new AudioClip(musicPath);
            backgroundMusic.setCycleCount(AudioClip.INDEFINITE);
            if (isMuted) {
                backgroundMusic.setVolume(0.0);
            } else {
                backgroundMusic.setVolume(savedBackgroundVolume);
            }
            backgroundMusic.play();
        } catch (Exception e) {
            // Error loading lobby music
        }
    }

    public void stopBackgroundMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
        }
    }

    public void playGameMusic() {
        stopGameMusic();
        try {
            String musicPath = getClass().getResource("/sounds/nhac_ingame.mp3").toExternalForm();
            gameMusic = new AudioClip(musicPath);
            gameMusic.setCycleCount(AudioClip.INDEFINITE);
            if (isMuted) {
                gameMusic.setVolume(0.0);
            } else {
                gameMusic.setVolume(savedGameVolume);
            }
            gameMusic.play();
        } catch (Exception e) {
            // Error loading game music
        }
    }

    public void stopGameMusic() {
        if (gameMusic != null) {
            gameMusic.stop();
        }
    }

    public void loadGameSounds() {
        try {
            String correctPath = getClass().getResource("/sounds/ye_đoan_dung_roi.mp3").toExternalForm();
            correctSound = new AudioClip(correctPath);
            correctSound.setVolume(0.6);
        } catch (Exception e) {
            // Error loading correct sound
        }

        try {
            String wrongPath = getClass().getResource("/sounds/phai_chiu.mp3").toExternalForm();
            wrongSound = new AudioClip(wrongPath);
            wrongSound.setVolume(0.5);
        } catch (Exception e) {
            // Error loading wrong sound
        }
    }

    public void playCorrectSound() {
        if (isMuted) {
            return;
        }
        if (correctSound != null) {
            correctSound.play();
        }
    }

    public void playWrongSound() {
        if (isMuted) {
            return;
        }
        if (wrongSound != null) {
            wrongSound.play();
        }
    }

    public void playCelebrationSound() {
        if (isMuted) {
            return;
        }
        try {
            String soundPath = getClass().getResource("/sounds/ving_quang.mp3").toExternalForm();
            celebrationSound = new AudioClip(soundPath);
            celebrationSound.setVolume(0.2);
            celebrationSound.play();
        } catch (Exception e) {
            // Error playing celebration sound
        }
    }

    public void stopAll() {
        stopBackgroundMusic();
        stopGameMusic();
    }
    
    public void setMuted(boolean muted) {
        this.isMuted = muted;
        
        if (muted) {
            if (backgroundMusic != null && backgroundMusic.isPlaying()) {
                double currentVolume = backgroundMusic.getVolume();
                if (currentVolume > 0.0) {
                    savedBackgroundVolume = currentVolume;
                }
                backgroundMusic.setVolume(0.0);
            }
            if (gameMusic != null && gameMusic.isPlaying()) {
                double currentVolume = gameMusic.getVolume();
                if (currentVolume > 0.0) {
                    savedGameVolume = currentVolume;
                }
                gameMusic.setVolume(0.0);
            }
        } else {
            if (backgroundMusic != null && backgroundMusic.isPlaying()) {
                backgroundMusic.setVolume(savedBackgroundVolume);
            }
            if (gameMusic != null && gameMusic.isPlaying()) {
                gameMusic.setVolume(savedGameVolume);
            }
        }
    }
    
    public boolean isMuted() {
        return isMuted;
    }
}


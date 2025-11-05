package com.ltm.game.client.services;

import javafx.scene.media.AudioClip;

public class AudioService {
    private AudioClip backgroundMusic;
    private AudioClip gameMusic;
    private AudioClip correctSound;
    private AudioClip wrongSound;
    private AudioClip celebrationSound;
    private AudioClip countdownSound;
    private AudioClip matchFoundSound;
    private AudioClip matchStartSound;
    
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
            String correctPath = getClass().getResource("/sounds/Ping sound effect.mp3").toExternalForm();
            correctSound = new AudioClip(correctPath);
            correctSound.setVolume(0.7);
        } catch (Exception e) {
            System.err.println("Error loading correct sound: " + e.getMessage());
        }

        try {
            String wrongPath = getClass().getResource("/sounds/Missing ping Lol.mp3").toExternalForm();
            wrongSound = new AudioClip(wrongPath);
            wrongSound.setVolume(0.6);
        } catch (Exception e) {
            System.err.println("Error loading wrong sound: " + e.getMessage());
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

    public void playCountdownSound() {
        stopCountdownSound();
        if (isMuted) {
            return;
        }
        try {
            String soundPath = getClass().getResource("/sounds/Countdown Timer.mp3").toExternalForm();
            countdownSound = new AudioClip(soundPath);
            countdownSound.setVolume(0.7);
            countdownSound.play();
        } catch (Exception e) {
            System.err.println("Error playing countdown sound: " + e.getMessage());
        }
    }

    public void stopCountdownSound() {
        if (countdownSound != null) {
            countdownSound.stop();
        }
    }

    public void playMatchFoundSound() {
        if (isMuted) {
            return;
        }
        try {
            String soundPath = getClass().getResource("/sounds/sound_khi an tim tran.mp3").toExternalForm();
            matchFoundSound = new AudioClip(soundPath);
            matchFoundSound.setVolume(0.7);
            matchFoundSound.play();
        } catch (Exception e) {
            System.err.println("Error playing match found sound: " + e.getMessage());
        }
    }

    public void playMatchStartSound() {
        if (isMuted) {
            return;
        }
        try {
            String soundPath = getClass().getResource("/sounds/sound in matches.mp3").toExternalForm();
            matchStartSound = new AudioClip(soundPath);
            matchStartSound.setVolume(0.8);
            matchStartSound.play();
        } catch (Exception e) {
            System.err.println("Error playing match start sound: " + e.getMessage());
        }
    }

    public void stopAll() {
        stopBackgroundMusic();
        stopGameMusic();
        stopCountdownSound();
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


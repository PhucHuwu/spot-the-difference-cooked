package com.ltm.game.server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void info(String message) {
        log("INFO", message);
    }

    public static void error(String message) {
        log("ERROR", message);
    }

    public static void error(String message, Exception e) {
        log("ERROR", message + " - " + e.getClass().getSimpleName() + ": " + e.getMessage());
        if (e.getCause() != null) {
            log("ERROR", "Caused by: " + e.getCause().getMessage());
        }
    }

    public static void debug(String message) {
        log("DEBUG", message);
    }

    private static void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logMessage = String.format("[%s] [%s] %s", timestamp, level, message);
        if (level.equals("ERROR")) {
            java.util.logging.Logger.getLogger("GameServer").severe(logMessage);
        } else {
            java.util.logging.Logger.getLogger("GameServer").info(logMessage);
        }
    }
}


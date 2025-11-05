package com.ltm.game.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class PasswordUtil {
    private PasswordUtil() {}

    static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // Fallback to plain when hashing fails (shouldn't happen)
            return input;
        }
    }
}

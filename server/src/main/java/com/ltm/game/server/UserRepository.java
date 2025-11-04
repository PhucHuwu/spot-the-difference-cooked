package com.ltm.game.server;

import com.ltm.game.shared.models.User;

import java.sql.*;
import java.util.Optional;

public class UserRepository {
    private Connection getConn() throws Exception {
        return Database.getConnection();
    }

    public User findByUsername(String username) throws Exception {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT id, username, total_points, total_wins, total_losses, total_draws FROM users WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.id = rs.getInt("id");
                    u.username = rs.getString("username");
                    u.totalPoints = rs.getInt("total_points");
                    u.totalWins = rs.getInt("total_wins");
                    u.totalLosses = rs.getInt("total_losses");
                    u.totalDraws = rs.getInt("total_draws");
                    return u;
                }
            }
        }
        return null;
    }

    public boolean verifyPassword(String username, String password) throws Exception {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT password_hash FROM users WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hash = rs.getString(1);
                    // WARNING: demo only (plain text); replace with proper hashing e.g., BCrypt
                    return hash.equals(password);
                }
            }
        }
        return false;
    }

    public void createUser(String username, String password) throws Exception {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("INSERT INTO users(username, password_hash) VALUES (?,?)")) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
        }
    }

    public int safeGetTotalPoints(String username) {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT total_points FROM users WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public void updateStats(int userId, int points, boolean win, boolean loss, boolean draw) throws Exception {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET total_points = total_points + ?, " +
                "total_wins = total_wins + ?, " +
                "total_losses = total_losses + ?, " +
                "total_draws = total_draws + ? " +
                "WHERE id = ?")) {
            ps.setInt(1, points);
            ps.setInt(2, win ? 1 : 0);
            ps.setInt(3, loss ? 1 : 0);
            ps.setInt(4, draw ? 1 : 0);
            ps.setInt(5, userId);
            ps.executeUpdate();
        }
    }
}

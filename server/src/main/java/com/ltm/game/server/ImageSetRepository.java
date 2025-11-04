package com.ltm.game.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImageSetRepository {

    public ImageSet loadRandom() throws Exception {
        // Prefer most recent valid sets (with paths and at least 1 difference).
        String pickSql = "SELECT id, width, height, img_left_path, img_right_path FROM image_sets ORDER BY id DESC LIMIT 20";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(pickSql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    int w = rs.getInt("width");
                    int h = rs.getInt("height");
                    String leftPath = rs.getString("img_left_path");
                    String rightPath = rs.getString("img_right_path");
                    List<Map<String,Object>> diffs = loadDiffs(c, id);
                    if (leftPath == null || rightPath == null || diffs.isEmpty()) continue;
                    byte[] leftBytes = readContent(leftPath);
                    byte[] rightBytes = readContent(rightPath);
                    if (leftBytes == null || rightBytes == null || leftBytes.length == 0 || rightBytes.length == 0) continue;
                    return new ImageSet(id, w, h, leftBytes, rightBytes, diffs);
                }
            }
        }
        return null;
    }

    private byte[] readContent(String relative) throws Exception {
        if (relative == null) return null;
        String base = ServerProperties.load().getProperty("content.dir", "admin/content/imagesets");
        // Resolve from user.dir (project root)
        Path p = Paths.get(System.getProperty("user.dir"), base, relative);
        System.out.println("Loading image: " + p.toAbsolutePath() + " (exists: " + Files.exists(p) + ")");
        if (!Files.exists(p)) {
            System.err.println("Image file not found: "+p.toAbsolutePath());
            return null;
        }
        byte[] bytes = Files.readAllBytes(p);
        System.out.println("Loaded " + bytes.length + " bytes from " + relative);
        return bytes;
    }

    private List<Map<String,Object>> loadDiffs(Connection c, long setId) throws Exception {
        List<Map<String,Object>> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT x, y, radius FROM image_differences WHERE set_id = ?")) {
            ps.setLong(1, setId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(Map.of(
                            "x", rs.getInt("x"),
                            "y", rs.getInt("y"),
                            "radius", rs.getInt("radius")
                    ));
                }
            }
        }
        return list;
    }

    public record ImageSet(long id, int width, int height, byte[] left, byte[] right, List<Map<String,Object>> diffs) {}
}

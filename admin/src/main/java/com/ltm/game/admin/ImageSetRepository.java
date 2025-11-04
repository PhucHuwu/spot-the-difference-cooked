package com.ltm.game.admin;

import java.sql.*;
import java.util.List;

public class ImageSetRepository {

    public long insertImageSet(String name, int width, int height, String leftPath, String rightPath) throws Exception {
        String sql = "INSERT INTO image_sets(name, width, height, img_left_path, img_right_path) VALUES (?,?,?,?,?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, width);
            ps.setInt(3, height);
            ps.setString(4, leftPath);
            ps.setString(5, rightPath);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new IllegalStateException("Failed to insert image set");
    }

    public void insertDifferences(long setId, List<DPoint> points) throws Exception {
        if (points == null || points.isEmpty()) return;
        String sql = "INSERT INTO image_differences(set_id, x, y, radius) VALUES (?,?,?,?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (DPoint p : points) {
                ps.setLong(1, setId);
                ps.setInt(2, (int)Math.round(p.x));
                ps.setInt(3, (int)Math.round(p.y));
                ps.setInt(4, p.radius);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public static class DPoint {
        public final double x; public final double y; public final int radius;
        public DPoint(double x, double y, int radius) { this.x=x; this.y=y; this.radius=radius; }
    }
}

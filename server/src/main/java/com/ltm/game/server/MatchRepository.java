package com.ltm.game.server;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class MatchRepository {
    public void saveMatch(int playerAId, int playerBId, int scoreA, int scoreB) throws Exception {
        String result = scoreA>scoreB?"A":scoreB>scoreA?"B":"DRAW";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO matches(player_a_id, player_b_id, score_a, score_b, result) VALUES (?,?,?,?,?)")) {
            ps.setInt(1, playerAId);
            ps.setInt(2, playerBId);
            ps.setInt(3, scoreA);
            ps.setInt(4, scoreB);
            ps.setString(5, result);
            ps.executeUpdate();
        }
    }
}

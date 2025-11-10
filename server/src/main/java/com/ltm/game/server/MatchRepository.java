package com.ltm.game.server;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class MatchRepository {
    /**
     * Save match result with explicit winner determination
     * @param playerAId ID of player A
     * @param playerBId ID of player B
     * @param scoreA Score of player A
     * @param scoreB Score of player B
     * @param winner Name of the winner ("playerA", "playerB", or "DRAW")
     * @param playerAName Username of player A
     * @param playerBName Username of player B
     */
    public void saveMatch(int playerAId, int playerBId, int scoreA, int scoreB, String winner, String playerAName, String playerBName) throws Exception {
        // Determine result based on explicit winner, not just scores
        String result;
        if (winner.equals(playerAName)) {
            result = "A";
        } else if (winner.equals(playerBName)) {
            result = "B";
        } else if (winner.equals("DRAW")) {
            result = "DRAW";
        } else {
            // Fallback to score-based determination
            result = scoreA > scoreB ? "A" : scoreB > scoreA ? "B" : "DRAW";
        }
        
        System.out.println("💾 Saving match: playerA=" + playerAName + " (id=" + playerAId + ", score=" + scoreA + "), " +
                           "playerB=" + playerBName + " (id=" + playerBId + ", score=" + scoreB + "), " +
                           "winner=" + winner + ", result=" + result);
        
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO matches(player_a_id, player_b_id, score_a, score_b, result) VALUES (?,?,?,?,?)")) {
            ps.setInt(1, playerAId);
            ps.setInt(2, playerBId);
            ps.setInt(3, scoreA);
            ps.setInt(4, scoreB);
            ps.setString(5, result);
            ps.executeUpdate();
            System.out.println("✅ Match saved successfully");
        }
    }
}

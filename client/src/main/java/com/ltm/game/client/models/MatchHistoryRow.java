package com.ltm.game.client.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MatchHistoryRow {
    private String matchId;
    private String opponent;
    private String result; // "THẮNG", "THUA", "HÒA"
    private String myScore;
    private String opponentScore;
    private String duration;
    private LocalDateTime matchDate;
    private boolean isMvp;
    private boolean isPerfect;
    
    public MatchHistoryRow(String matchId, String opponent, String result, 
                          String myScore, String opponentScore, String duration,
                          LocalDateTime matchDate, boolean isMvp, boolean isPerfect) {
        this.matchId = matchId;
        this.opponent = opponent;
        this.result = result;
        this.myScore = myScore;
        this.opponentScore = opponentScore;
        this.duration = duration;
        this.matchDate = matchDate;
        this.isMvp = isMvp;
        this.isPerfect = isPerfect;
    }
    
    public String getMatchId() { return matchId; }
    public String getOpponent() { return opponent; }
    public String getResult() { return result; }
    public String getMyScore() { return myScore; }
    public String getOpponentScore() { return opponentScore; }
    public String getDuration() { return duration; }
    public LocalDateTime getMatchDate() { return matchDate; }
    public boolean isMvp() { return isMvp; }
    public boolean isPerfect() { return isPerfect; }
    
    public String getFormattedDate() {
        return matchDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
    
    public String getScore() {
        return myScore + " - " + opponentScore;
    }
}

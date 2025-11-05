package com.ltm.game.client.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class LeaderboardRow {
    private final SimpleStringProperty rank = new SimpleStringProperty("");
    private final SimpleStringProperty username = new SimpleStringProperty("");
    private final SimpleStringProperty totalPoints = new SimpleStringProperty("");
    private final SimpleStringProperty totalWins = new SimpleStringProperty("");

    public LeaderboardRow(String rank, String username, String totalPoints, String totalWins) {
        this.rank.set(rank);
        this.username.set(username);
        this.totalPoints.set(totalPoints);
        this.totalWins.set(totalWins);
    }

    public String getRank() {
        return rank.get();
    }

    public StringProperty rankProperty() {
        return rank;
    }

    public String getUsername() {
        return username.get();
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public String getTotalPoints() {
        return totalPoints.get();
    }

    public StringProperty totalPointsProperty() {
        return totalPoints;
    }

    public String getTotalWins() {
        return totalWins.get();
    }

    public StringProperty totalWinsProperty() {
        return totalWins;
    }
}


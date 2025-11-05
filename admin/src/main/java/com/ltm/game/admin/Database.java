package com.ltm.game.admin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class Database {
    private static String url;
    private static String user;
    private static String pass;
    static {
        try {
            Properties props = new Properties();
            try (var in = Database.class.getClassLoader().getResourceAsStream("admin-config.properties")) {
                if (in != null) props.load(in);
            }
            url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/spotgame");
            user = props.getProperty("db.user", "root");
            pass = props.getProperty("db.password", "root");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(url, user, pass);
    }
}

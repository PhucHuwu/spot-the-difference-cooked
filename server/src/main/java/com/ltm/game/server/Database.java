package com.ltm.game.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class Database {
    private static String url;
    private static String user;
    private static String pass;
    static {
        Properties props = ServerProperties.load();
        url = props.getProperty("db.url");
        user = props.getProperty("db.user");
        pass = props.getProperty("db.password");
    }
    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(url, user, pass);
    }
}

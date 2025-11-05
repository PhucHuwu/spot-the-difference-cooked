package com.ltm.game.server;

import java.io.InputStream;
import java.util.Properties;

public class ServerProperties {
    public static Properties load() {
        Properties props = new Properties();
        try (InputStream in = ServerProperties.class.getClassLoader().getResourceAsStream("server-config.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            Logger.error("Failed to load server-config.properties", e);
        }
        
        String dbUrl = System.getenv("DB_URL");
        if (dbUrl != null) {
            props.setProperty("db.url", dbUrl);
        }
        
        String dbUser = System.getenv("DB_USER");
        if (dbUser != null) {
            props.setProperty("db.user", dbUser);
        }
        
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword != null) {
            props.setProperty("db.password", dbPassword);
        }
        
        return props;
    }
}

package com.ygohappy123.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AppConfig {
    private static final Properties properties = new Properties();

    static {
        try (FileInputStream input = new FileInputStream("db.properties")) {
            properties.load(input);
        } catch (IOException e) {
            System.out.println("Failed to load local.properties. Using default values.");
            e.printStackTrace();
        }
    }

    public static String getJdbcUrl() {
        return properties.getProperty("db.url", "jdbc:sqlserver://localhost:1433;Database=VeMayBay");
    }

    public static String getSqlDriver() {
        return properties.getProperty("db.driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }
}



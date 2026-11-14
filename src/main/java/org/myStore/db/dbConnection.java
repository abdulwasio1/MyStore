package org.myStore.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

public class dbConnection {

    public static Connection getConnection() throws SQLException {

        Properties properties = new Properties();

        try (InputStream input = dbConnection.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (input == null) {
                throw new SQLException("db.properties file not found!");
            }

            properties.load(input);

            String url = properties.getProperty("db.url");
            String username = properties.getProperty("db.username");
            String password = properties.getProperty("db.password");

            return DriverManager.getConnection(url, username, password);

        } catch (IOException e) {
            throw new SQLException("Could not load database configuration.", e);
        }
    }
}

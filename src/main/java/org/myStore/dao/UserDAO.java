package org.myStore.dao;

import org.myStore.auth.Role;
import org.myStore.db.dbConnection;
import org.myStore.entities.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    // Find user using username
    public User findByUsername(String username) throws SQLException {

        String query = "SELECT * FROM users WHERE username = ?";

        try (
                Connection con = dbConnection.getConnection();
                PreparedStatement pst = con.prepareStatement(query)
        ) {

            pst.setString(1, username);

            try (ResultSet rs = pst.executeQuery()) {

                if (rs.next()) {

                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            Role.valueOf(rs.getString("role"))
                    );
                }
            }
        }

        return null;
    }

    // Check whether username already exists
    public static boolean usernameExists(String username)
            throws SQLException {

        String query = "SELECT 1 FROM users WHERE username = ?";

        try (
                Connection con = dbConnection.getConnection();
                PreparedStatement pst = con.prepareStatement(query)
        ) {

            pst.setString(1, username);

            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Check whether an admin account already exists
    public boolean adminExists() throws SQLException {

        String query = "SELECT 1 FROM users WHERE role = 'ADMIN' LIMIT 1";

        try (
                Connection con = dbConnection.getConnection();
                PreparedStatement pst = con.prepareStatement(query);
                ResultSet rs = pst.executeQuery()
        ) {

            return rs.next();
        }
    }

    // Create a user account
    public static void createUser(User user) throws SQLException {

        String query = """
                INSERT INTO users (username, password_hash, role)
                VALUES (?, ?, ?)
                """;

        try (
                Connection con = dbConnection.getConnection();
                PreparedStatement pst = con.prepareStatement(query)
        ) {

            pst.setString(1, user.getUsername());
            pst.setString(2, user.getPasswordHash());
            pst.setString(3, user.getRole().name());

            pst.executeUpdate();
        }
    }
}
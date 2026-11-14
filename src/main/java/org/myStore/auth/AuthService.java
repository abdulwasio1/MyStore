package org.myStore.auth;

import org.myStore.dao.UserDAO;
import org.myStore.entities.User;

import java.sql.SQLException;

public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        userDAO = new UserDAO();
    }

    // Authenticate a user
    public User login(String username, String password)
            throws SQLException {

        if (username == null || password == null
                || username.isBlank() || password.isEmpty()) {

            return null;
        }

        User user = userDAO.findByUsername(username.trim());

        if (user == null) {
            return null;
        }

        boolean validPassword = PasswordUtil.verifyPassword(
                password,
                user.getPasswordHash()
        );

        if (validPassword) {
            return user;
        }

        return null;
    }

    // Only an admin can create staff accounts
    public void createStaffAccount(String username, String password)
            throws SQLException {

        Session.requireAdmin();

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username cannot be empty."
            );
        }

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters."
            );
        }

        username = username.trim();

        if (userDAO.usernameExists(username)) {
            throw new IllegalArgumentException(
                    "Username already exists."
            );
        }

        String hashedPassword =
                PasswordUtil.hashPassword(password);

        User user = new User(
                0,
                username,
                hashedPassword,
                Role.STAFF
        );

        userDAO.createUser(user);
    }
}
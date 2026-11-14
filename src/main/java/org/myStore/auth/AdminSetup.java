package org.myStore.auth;

import org.myStore.dao.UserDAO;
import org.myStore.entities.User;

import java.sql.SQLException;
import java.util.Scanner;

public class AdminSetup {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        UserDAO userDAO = new UserDAO();

        try {

            // Prevent creating another first admin
            if (userDAO.adminExists()) {
                System.out.println("An ADMIN account already exists.");
                return;
            }

            System.out.println("===== INITIAL ADMIN SETUP =====");

            System.out.print("Enter admin username: ");
            String username = sc.nextLine().trim();

            if (username.isEmpty()) {
                System.out.println("Username cannot be empty.");
                return;
            }

            if (userDAO.usernameExists(username)) {
                System.out.println("Username already exists.");
                return;
            }

            System.out.print("Enter admin password: ");
            String password = sc.nextLine();

            if (password.length() < 8) {
                System.out.println(
                        "Password must contain at least 8 characters."
                );
                return;
            }

            String hashedPassword =
                    PasswordUtil.hashPassword(password);

            User admin = new User(
                    0,
                    username,
                    hashedPassword,
                    Role.ADMIN
            );

            userDAO.createUser(admin);

            System.out.println("\nAdmin account created successfully!");
            System.out.println("You can now log in through Main.java.");

        } catch (SQLException e) {

            System.out.println("Database error: " + e.getMessage());

        }
    }
}
package org.myStore.auth;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    // Convert a normal password into a BCrypt hash
    public static String hashPassword(String password) {

        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public static class GenerateAdminHash {

        public static void main(String[] args) {

            String hash = PasswordUtil.hashPassword("admin123");

            System.out.println(hash);
        }
    }

    // Verify an entered password against a stored hash
    public static boolean verifyPassword(
            String enteredPassword,
            String storedHash) {

        if (enteredPassword == null || storedHash == null) {
            return false;
        }

        try {
            return BCrypt.checkpw(enteredPassword, storedHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
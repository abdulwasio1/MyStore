package org.myStore.auth;

import org.myStore.entities.User;

public class Session {

    private static User currentUser;

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean hasRole(Role role) {

        return currentUser != null
                && currentUser.getRole() == role;
    }

    // Protect operations that require admin access
    public static void requireAdmin() {

        if (!hasRole(Role.ADMIN)) {
            throw new SecurityException(
                    "Access denied! Admin permission required."
            );
        }
    }
}
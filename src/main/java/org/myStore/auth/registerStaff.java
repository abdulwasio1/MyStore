import org.myStore.auth.PasswordUtil;
import org.myStore.auth.Role;
import org.myStore.dao.UserDAO;
import org.myStore.entities.User;

import java.sql.SQLException;

// Public registration: every new account is STAFF
public void registerStaff(String username, String password)
        throws SQLException {

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

    if (UserDAO.usernameExists(username)) {
        throw new IllegalArgumentException(
                "Username already exists."
        );
    }

    // Hash the password
    String hashedPassword = PasswordUtil.hashPassword(password);

    // Role is fixed to STAFF
    User user = new User(
            0,
            username,
            hashedPassword,
            Role.STAFF
    );

    UserDAO.createUser(user);
}

void main() {
}
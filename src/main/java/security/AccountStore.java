package security;

import util.DBConnection;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;

public final class AccountStore {
    private static final int ITERATIONS = 210000;
    private static final int HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    private AccountStore() { }

    public static void initializeAndBootstrap() throws Exception {
        String adminUser = required("CAMPUSPARK_ADMIN_USER");
        String adminPassword = requiredPassword("CAMPUSPARK_ADMIN_PASSWORD");
        String viewerUser = required("CAMPUSPARK_VIEWER_USER");
        String viewerPassword = requiredPassword("CAMPUSPARK_VIEWER_PASSWORD");
        if (adminUser.equalsIgnoreCase(viewerUser)) throw new IllegalStateException("Admin and viewer usernames must differ.");
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement create = connection.prepareStatement(
                 "CREATE TABLE IF NOT EXISTS app_accounts ("
               + "username VARCHAR(64) NOT NULL PRIMARY KEY,"
               + "role ENUM('ADMIN','VIEWER') NOT NULL,"
               + "salt VARBINARY(32) NOT NULL,"
               + "password_hash VARBINARY(64) NOT NULL,"
               + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
               + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")) { create.execute(); }
        upsert(adminUser, "ADMIN", adminPassword);
        upsert(viewerUser, "VIEWER", viewerPassword);
        Arrays.fill(adminPassword.toCharArray(), '\0');
        Arrays.fill(viewerPassword.toCharArray(), '\0');
    }

    public static AppAccount authenticate(String username, String password) throws Exception {
        if (username == null || password == null || username.length() > 64 || password.length() > 256) return null;
        byte[] salt; byte[] expected; String role;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement query = connection.prepareStatement("SELECT role, salt, password_hash FROM app_accounts WHERE username = ?")) {
            query.setString(1, username.trim());
            try (ResultSet rows = query.executeQuery()) {
                if (!rows.next()) return null;
                role = rows.getString("role"); salt = rows.getBytes("salt"); expected = rows.getBytes("password_hash");
            }
        }
        char[] chars = password.toCharArray(); byte[] actual;
        try { actual = derive(chars, salt); } finally { Arrays.fill(chars, '\0'); }
        boolean match = MessageDigest.isEqual(expected, actual);
        Arrays.fill(expected, (byte) 0); Arrays.fill(actual, (byte) 0);
        return match ? new AppAccount(username.trim(), role) : null;
    }

    private static void upsert(String username, String role, String password) throws Exception {
        byte[] salt = new byte[16]; RANDOM.nextBytes(salt);
        char[] chars = password.toCharArray(); byte[] hash;
        try { hash = derive(chars, salt); } finally { Arrays.fill(chars, '\0'); }
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement save = connection.prepareStatement(
                 "INSERT INTO app_accounts (username, role, salt, password_hash) VALUES (?, ?, ?, ?) "
               + "ON DUPLICATE KEY UPDATE role = VALUES(role), salt = VALUES(salt), password_hash = VALUES(password_hash)")) {
            save.setString(1, username); save.setString(2, role); save.setBytes(3, salt); save.setBytes(4, hash); save.executeUpdate();
        } finally { Arrays.fill(hash, (byte) 0); }
    }

    private static byte[] derive(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, HASH_BITS);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
    private static String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) throw new IllegalStateException(key + " must be configured.");
        if (value.length() > 64) throw new IllegalStateException(key + " must be 64 characters or fewer.");
        return value.trim();
    }
    private static String requiredPassword(String key) {
        String value = System.getenv(key);
        if (value == null || value.length() < 12 || value.length() > 256)
            throw new IllegalStateException(key + " must be configured and contain 12 to 256 characters.");
        return value;
    }
}

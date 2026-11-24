package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        String url = setting("CAMPUSPARK_DB_URL",
            "jdbc:mysql://127.0.0.1:3306/college_parking?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata");
        String user = setting("CAMPUSPARK_DB_USER", "root");
        String password = System.getenv("CAMPUSPARK_DB_PASSWORD");
        if (password == null || password.isEmpty()) {
            throw new SQLException("CAMPUSPARK_DB_PASSWORD is not set. Run start-server.ps1 and enter the local MySQL password.");
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("MySQL Connector/J is missing from this web app", ex);
        }
        return DriverManager.getConnection(url, user, password);
    }

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}

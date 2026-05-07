package cruise.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {

    private static Connection conn;

    public static Connection get() throws SQLException {
        if (conn == null || conn.isClosed()) {
            Properties p = new Properties();
            try (InputStream in = DBConnection.class.getClassLoader()
                    .getResourceAsStream("db.properties")) {
                if (in != null) p.load(in);
            } catch (IOException ignored) {}

            String url = "jdbc:mysql://"
                    + p.getProperty("host", "localhost") + ":"
                    + p.getProperty("port", "3306") + "/"
                    + p.getProperty("database", "COSC457")
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            conn = DriverManager.getConnection(
                    url,
                    p.getProperty("user", "root"),
                    p.getProperty("password", ""));
        }
        return conn;
    }

    public static String adminPassword() {
        Properties p = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in != null) p.load(in);
        } catch (IOException ignored) {}
        return p.getProperty("admin_password", "admin123");
    }

    public static void close() {
        try { if (conn != null && !conn.isClosed()) conn.close(); }
        catch (SQLException ignored) {}
    }
}

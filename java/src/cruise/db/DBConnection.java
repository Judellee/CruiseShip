package cruise.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {

    private static Connection conn;

    private static Properties loadProps() {
        Properties p = new Properties();
        // External db.properties next to the JAR takes priority
        File external = new File("db.properties");
        if (external.exists()) {
            try (InputStream in = new FileInputStream(external)) {
                p.load(in);
                return p;
            } catch (IOException ignored) {}
        }
        // Fall back to bundled copy inside the JAR
        try (InputStream in = DBConnection.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in != null) p.load(in);
        } catch (IOException ignored) {}
        return p;
    }

    public static Connection get() throws SQLException {
        if (conn == null || conn.isClosed()) {
            Properties p = loadProps();
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
        return loadProps().getProperty("admin_password", "admin123");
    }

    public static void close() {
        try { if (conn != null && !conn.isClosed()) conn.close(); }
        catch (SQLException ignored) {}
    }
}

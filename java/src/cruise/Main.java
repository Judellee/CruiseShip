package cruise;

import cruise.db.DBConnection;
import cruise.ui.StartupFrame;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {}

            // Verify DB connection before showing UI
            try {
                DBConnection.get();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Cannot connect to database.\n\nCheck db.properties and ensure MySQL is running.\n\n" + e.getMessage(),
                        "Connection Failed", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            new StartupFrame().setVisible(true);
        });
    }
}

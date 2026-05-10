package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.Properties;

public class MainFrame extends JFrame {

    public MainFrame() {
        super("Cruise Ship Management System — COSC 457");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1050, 700);
        setMinimumSize(new Dimension(800, 550));
        setLocationRelativeTo(null);
        buildUI();
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                int opt = JOptionPane.showConfirmDialog(MainFrame.this,
                        "Exit application?", "Confirm Exit", JOptionPane.YES_NO_OPTION);
                if (opt == JOptionPane.YES_OPTION) {
                    DBConnection.close();
                    dispose();
                    System.exit(0);
                }
            }
        });
    }

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));

        tabs.addTab("  Ships  ",        new ShipsPanel());
        tabs.addTab("  Ship Details  ", new ShipDetailsPanel());
        tabs.addTab("  Employees  ",    new EmployeesPanel());
        tabs.addTab("  Crew  ",         new CrewPanel());
        tabs.addTab("  Voyages  ",      new VoyagesPanel());
        tabs.addTab("  Passengers  ",   new PassengersPanel());
        tabs.addTab("  Reservations  ", new ReservationsPanel());
        tabs.addTab("  Maintenance  ",  new MaintenancePanel());
        tabs.addTab("  Supplies  ",     new SuppliesPanel());
        tabs.addTab("  Financials  ",   new FinancialsPanel());
        tabs.addTab("  Safety  ",          new SafetyPanel());

        add(tabs, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        String dbName = "COSC457";
        try {
            Properties p = new Properties();
            File ext = new File("db.properties");
            InputStream in = ext.exists() ? new FileInputStream(ext)
                    : getClass().getClassLoader().getResourceAsStream("db.properties");
            if (in != null) { p.load(in); in.close(); }
            dbName = p.getProperty("database", dbName);
        } catch (IOException ignored) {}
        JLabel statusLabel = new JLabel("  Connected  |  Database: " + dbName + "  |  COSC 457 Database Management Systems");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JButton logoutBtn = new JButton("Log Out");
        logoutBtn.setFocusPainted(false);
        logoutBtn.addActionListener(e -> {
            new StartupFrame().setVisible(true);
            dispose();
        });

        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(logoutBtn,   BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);
    }
}

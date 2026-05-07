package cruise.ui;

import javax.swing.*;
import java.awt.*;

public class PassengerFrame extends JFrame {

    private final int    passengerId;
    private final String passengerName;

    public PassengerFrame(int passengerId, String passengerName) {
        super("Cruise Ship Management — Passenger Portal");
        this.passengerId   = passengerId;
        this.passengerName = passengerName;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1000, 680);
        setMinimumSize(new Dimension(800, 550));
        setLocationRelativeTo(null);
        buildUI();
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                new StartupFrame().setVisible(true);
                dispose();
            }
        });
    }

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));

        tabs.addTab("  Browse Voyages  ", new BrowseVoyagesPanel());
        tabs.addTab("  Our Ships  ",      new BrowseShipsPanel());
        tabs.addTab("  Book a Voyage  ",  new BookingPanel(passengerId));
        tabs.addTab("  My Reservations  ", new LookupPanel(passengerId));

        add(tabs, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        JLabel statusLabel = new JLabel(
                "  Welcome, " + passengerName + "  |  COSC 457 Database Management Systems");
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

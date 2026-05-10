package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BrowseShipsPanel extends JPanel {

    private final JPanel shipButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    private final JLabel shipNameLabel   = new JLabel("Select a ship to view details");
    private final JLabel shipInfoLabel   = new JLabel(" ");
    private final JTabbedPane detailTabs = new JTabbedPane();

    private final DefaultTableModel diningModel = viewModel("Dining Venue", "Capacity");
    private final DefaultTableModel facilModel  = viewModel("Facility");
    private final DefaultTableModel eventModel  = viewModel("Event", "Date/Time", "Venue");
    private final DefaultTableModel deckModel   = viewModel("Deck Number");

    public BrowseShipsPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel heading = new JLabel("Our Ships");
        heading.setFont(new Font("SansSerif", Font.BOLD, 15));

        shipNameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        shipNameLabel.setForeground(new Color(10, 60, 120));
        shipInfoLabel.setForeground(Color.DARK_GRAY);

        detailTabs.addTab("  Dining  ",     new JScrollPane(makeTable(diningModel)));
        detailTabs.addTab("  Facilities  ", new JScrollPane(makeTable(facilModel)));
        detailTabs.addTab("  Events  ",     new JScrollPane(makeTable(eventModel)));
        detailTabs.addTab("  Decks  ",      new JScrollPane(makeTable(deckModel)));
        detailTabs.setVisible(false);

        JPanel infoPanel = new JPanel(new BorderLayout(4, 4));
        infoPanel.add(shipNameLabel, BorderLayout.NORTH);
        infoPanel.add(shipInfoLabel, BorderLayout.CENTER);
        infoPanel.add(detailTabs,    BorderLayout.SOUTH);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(heading,        BorderLayout.NORTH);
        topPanel.add(shipButtonPanel, BorderLayout.CENTER);

        add(topPanel,  BorderLayout.NORTH);
        add(infoPanel, BorderLayout.CENTER);

        loadShipButtons();
    }

    private void loadShipButtons() {
        shipButtonPanel.removeAll();
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT s.ShipID, s.ShipName, s.Capacity, t.TypeName " +
                "FROM Ship s JOIN ShipType t ON s.ShipTypeID=t.ShipTypeID ORDER BY s.ShipName")) {
            while (rs.next()) {
                int id = rs.getInt(1);
                String name = rs.getString(2);
                int cap  = rs.getInt(3);
                String type = rs.getString(4);
                JButton btn = new JButton(name);
                btn.setFocusPainted(false);
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btn.addActionListener(e -> showShip(id, name, cap, type));
                shipButtonPanel.add(btn);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        revalidate(); repaint();
    }

    private void showShip(int shipId, String name, int capacity, String type) {
        shipNameLabel.setText(name);
        shipInfoLabel.setText("Type: " + type + "   |   Capacity: " + capacity + " passengers");

        // Dining
        diningModel.setRowCount(0);
        runQuery("SELECT VenueName, Capacity FROM DiningVenue WHERE ShipID=" + shipId +
                 " ORDER BY VenueName", diningModel);

        // Facilities
        facilModel.setRowCount(0);
        runQuery("SELECT FacilityName FROM Facility WHERE ShipID=" + shipId +
                 " ORDER BY FacilityName", facilModel);

        // Events
        eventModel.setRowCount(0);
        runQuery("SELECT EventName, DATE_FORMAT(EventDateTime, '%Y-%m-%d %H:%i'), Venue " +
                 "FROM EntertainmentEvent WHERE ShipID=" + shipId +
                 " ORDER BY EventDateTime", eventModel);

        // Decks
        deckModel.setRowCount(0);
        runQuery("SELECT DeckNumber FROM Deck WHERE ShipID=" + shipId +
                 " ORDER BY DeckNumber", deckModel);

        detailTabs.setVisible(true);
        revalidate();
    }

    private void runQuery(String sql, DefaultTableModel m) {
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            int cols = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Object[] row = new Object[cols];
                for (int i = 0; i < cols; i++) row[i] = rs.getObject(i + 1);
                m.addRow(row);
            }
        } catch (SQLException ignored) {}
    }

    private JTable makeTable(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(22);
        t.setEnabled(false);
        return t;
    }

    private static DefaultTableModel viewModel(String... cols) {
        return new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
    }
}

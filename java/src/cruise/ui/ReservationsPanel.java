package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ReservationsPanel extends JPanel {

    private final DefaultTableModel model;
    private final JTable table;

    public ReservationsPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"ID", "Passenger", "Ship", "Departure", "Cabin", "Status", "Date Made"};
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(22);

        JLabel heading = new JLabel("Reservations");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));

        JButton newBtn     = new JButton("New Reservation");
        JButton refreshBtn = new JButton("Refresh");

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnPanel.add(newBtn);
        btnPanel.add(Box.createHorizontalStrut(20));
        btnPanel.add(refreshBtn);

        add(heading, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        loadData();

        newBtn.addActionListener(e -> {
            ReservationDialog d = new ReservationDialog(SwingUtilities.getWindowAncestor(this));
            d.setVisible(true); if (d.saved) loadData();
        });
        refreshBtn.addActionListener(e -> loadData());
    }

    private void loadData() {
        model.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT r.ReservationID, " +
                "       CONCAT(p.FirstName,' ',p.LastName) AS Passenger, " +
                "       s.ShipName, v.DepartureDate, c.CabinNumber, " +
                "       r.Status, r.ReservationDate " +
                "FROM Reservation r " +
                "JOIN Passenger p ON r.PassengerID = p.PassengerID " +
                "JOIN Voyage v    ON r.VoyageID    = v.VoyageID " +
                "JOIN Ship s      ON v.ShipID       = s.ShipID " +
                "JOIN Cabin c     ON r.CabinID      = c.CabinID " +
                "ORDER BY r.ReservationDate DESC")) {
            while (rs.next())
                model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)});
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    static class ReservationDialog extends JDialog {
        boolean saved = false;
        private final JComboBox<String[]> passengerCombo = new JComboBox<>();
        private final JComboBox<String[]> voyageCombo    = new JComboBox<>();
        private final JComboBox<String[]> cabinCombo     = new JComboBox<>();
        private final JComboBox<String>   statusCombo    = new JComboBox<>(new String[]{"Confirmed", "Pending", "Cancelled"});

        ReservationDialog(Window parent) {
            super(parent, "New Reservation", ModalityType.APPLICATION_MODAL);
            buildUI(); pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void buildUI() {
            loadCombo(passengerCombo,
                "SELECT PassengerID, CONCAT(FirstName,' ',LastName) FROM Passenger ORDER BY LastName");
            loadCombo(voyageCombo,
                "SELECT v.VoyageID, CONCAT(s.ShipName,' — ',v.DepartureDate) " +
                "FROM Voyage v JOIN Ship s ON v.ShipID=s.ShipID ORDER BY v.DepartureDate");
            loadCombo(cabinCombo,
                "SELECT CabinID, CONCAT(CabinNumber,' (',CabinType,')') FROM Cabin ORDER BY CabinNumber");
            for (JComboBox<String[]> cb : new JComboBox[]{passengerCombo, voyageCombo, cabinCombo})
                cb.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));

            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Passenger:", passengerCombo);
            ShipsPanel.addRow(form, 1, "Voyage:",    voyageCombo);
            ShipsPanel.addRow(form, 2, "Cabin:",     cabinCombo);
            ShipsPanel.addRow(form, 3, "Status:",    statusCombo);

            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 4, save, cancel);
            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);
            add(form);
        }

        private void loadCombo(JComboBox<String[]> cb, String sql) {
            try (Statement st = DBConnection.get().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) cb.addItem(new String[]{rs.getString(1), rs.getString(2)});
            } catch (SQLException ignored) {}
        }

        private void save() {
            if (passengerCombo.getSelectedItem() == null || voyageCombo.getSelectedItem() == null
                    || cabinCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "All fields are required."); return;
            }
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT INTO Reservation (ReservationDate, Status, PassengerID, VoyageID, CabinID) " +
                    "VALUES (CURDATE(), ?, ?, ?, ?)")) {
                ps.setString(1, (String) statusCombo.getSelectedItem());
                ps.setInt(2, Integer.parseInt(((String[]) passengerCombo.getSelectedItem())[0]));
                ps.setInt(3, Integer.parseInt(((String[]) voyageCombo.getSelectedItem())[0]));
                ps.setInt(4, Integer.parseInt(((String[]) cabinCombo.getSelectedItem())[0]));
                ps.executeUpdate(); saved = true; dispose();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

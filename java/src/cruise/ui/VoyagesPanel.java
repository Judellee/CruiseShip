package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class VoyagesPanel extends JPanel {

    private final DefaultTableModel model;
    private final JTable table;

    public VoyagesPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"ID", "Ship", "Itinerary", "Season", "Departure", "Return"};
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(22);

        JLabel heading = new JLabel("Voyages");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));

        JButton addBtn    = new JButton("Add");
        JButton deleteBtn = new JButton("Delete");
        JButton refreshBtn = new JButton("Refresh");

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnPanel.add(addBtn); btnPanel.add(deleteBtn);
        btnPanel.add(Box.createHorizontalStrut(20)); btnPanel.add(refreshBtn);

        add(heading, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        loadData();

        addBtn.addActionListener(e -> {
            VoyageDialog d = new VoyageDialog(getWindow());
            d.setVisible(true); if (d.saved) loadData();
        });
        deleteBtn.addActionListener(e -> deleteSelected());
        refreshBtn.addActionListener(e -> loadData());
    }

    private void loadData() {
        model.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT v.VoyageID, s.ShipName, i.ItineraryName, se.SeasonName, " +
                "       v.DepartureDate, v.ReturnDate " +
                "FROM Voyage v " +
                "JOIN Ship s      ON v.ShipID      = s.ShipID " +
                "JOIN Itinerary i ON v.ItineraryID = i.ItineraryID " +
                "JOIN Season se   ON v.SeasonID    = se.SeasonID " +
                "ORDER BY v.DepartureDate")) {
            while (rs.next())
                model.addRow(new Object[]{rs.getInt(1), rs.getString(2),
                        rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)});
        } catch (SQLException e) { showError(e); }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Please select a row first."); return; }
        int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
        if (JOptionPane.showConfirmDialog(this,
                "Delete voyage #" + id + "?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (PreparedStatement ps = DBConnection.get().prepareStatement(
                "DELETE FROM Voyage WHERE VoyageID=?")) {
            ps.setInt(1, id); ps.executeUpdate(); loadData();
        } catch (SQLException e) { showError(e); }
    }

    private Window getWindow() { return SwingUtilities.getWindowAncestor(this); }
    private void showError(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    static class VoyageDialog extends JDialog {
        boolean saved = false;
        private final JComboBox<String[]> shipCombo = new JComboBox<>();
        private final JComboBox<String[]> itinCombo = new JComboBox<>();
        private final JComboBox<String[]> seasonCombo = new JComboBox<>();
        private final JTextField departureField = new JTextField(12);
        private final JTextField returnField    = new JTextField(12);

        VoyageDialog(Window parent) {
            super(parent, "Add Voyage", ModalityType.APPLICATION_MODAL);
            buildUI(); pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void buildUI() {
            loadCombo(shipCombo,   "SELECT ShipID, ShipName FROM Ship ORDER BY ShipName");
            loadCombo(itinCombo,   "SELECT ItineraryID, ItineraryName FROM Itinerary ORDER BY ItineraryName");
            loadCombo(seasonCombo, "SELECT SeasonID, SeasonName FROM Season ORDER BY SeasonName");
            for (JComboBox<String[]> cb : new JComboBox[]{shipCombo, itinCombo, seasonCombo})
                cb.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));

            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Ship:",                   shipCombo);
            ShipsPanel.addRow(form, 1, "Itinerary:",              itinCombo);
            ShipsPanel.addRow(form, 2, "Season:",                 seasonCombo);
            ShipsPanel.addRow(form, 3, "Departure (YYYY-MM-DD):", departureField);
            ShipsPanel.addRow(form, 4, "Return (YYYY-MM-DD):",    returnField);

            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 5, save, cancel);
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
            String dep = departureField.getText().trim(), ret = returnField.getText().trim();
            if (dep.isEmpty() || shipCombo.getSelectedItem() == null
                    || itinCombo.getSelectedItem() == null || seasonCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "All fields except Return date are required."); return;
            }
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT INTO Voyage (DepartureDate, ReturnDate, ShipID, ItineraryID, SeasonID) VALUES (?,?,?,?,?)")) {
                ps.setString(1, dep);
                ps.setString(2, ret.isEmpty() ? null : ret);
                ps.setInt(3, Integer.parseInt(((String[]) shipCombo.getSelectedItem())[0]));
                ps.setInt(4, Integer.parseInt(((String[]) itinCombo.getSelectedItem())[0]));
                ps.setInt(5, Integer.parseInt(((String[]) seasonCombo.getSelectedItem())[0]));
                ps.executeUpdate();
                saved = true; dispose();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

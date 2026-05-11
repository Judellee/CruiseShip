package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ShipsPanel extends JPanel {

    private final DefaultTableModel model;
    private final JTable table;

    public ShipsPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"ID", "Ship Name", "Capacity", "Type"};
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(22);

        JLabel heading = new JLabel("Ships");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));

        JButton addBtn     = new JButton("Add");
        JButton editBtn    = new JButton("Edit");
        JButton deleteBtn  = new JButton("Delete");
        JButton refreshBtn = new JButton("Refresh");

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnPanel.add(addBtn);
        btnPanel.add(editBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(Box.createHorizontalStrut(20));
        btnPanel.add(refreshBtn);

        add(heading,                BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(btnPanel,               BorderLayout.SOUTH);

        loadData();

        addBtn.addActionListener(e -> {
            ShipDialog d = new ShipDialog(getWindow(), -1);
            d.setVisible(true);
            if (d.saved) loadData();
        });
        editBtn.addActionListener(e -> {
            int id = selectedId(); if (id < 0) return;
            ShipDialog d = new ShipDialog(getWindow(), id);
            d.setVisible(true);
            if (d.saved) loadData();
        });
        deleteBtn.addActionListener(e -> deleteSelected());
        refreshBtn.addActionListener(e -> loadData());
    }

    private void loadData() {
        model.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT s.ShipID, s.ShipName, s.Capacity, t.TypeName " +
                "FROM Ship s JOIN ShipType t ON s.ShipTypeID = t.ShipTypeID ORDER BY s.ShipName")) {
            while (rs.next())
                model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4)});
        } catch (SQLException e) { showError(e); }
    }

    private void deleteSelected() {
        int id = selectedId(); if (id < 0) return;
        int row = table.convertRowIndexToModel(table.getSelectedRow());
        String name = (String) model.getValueAt(row, 1);
        if (JOptionPane.showConfirmDialog(this,
                "Delete ship \"" + name + "\"? This will also remove its decks, cabins,\n" +
                "crew assignments, schedules, maintenance, supplies, and events.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            Connection con = DBConnection.get();
            exec(con, "DELETE FROM CrewCabin WHERE CabinID IN (SELECT CabinID FROM Cabin WHERE ShipID=?)", id);
            exec(con, "DELETE FROM Cabin WHERE ShipID=?", id);
            exec(con, "DELETE FROM Deck WHERE ShipID=?", id);
            exec(con, "DELETE FROM WorkSchedule WHERE ShipID=?", id);
            exec(con, "DELETE FROM ShipCrew WHERE ShipID=?", id);
            exec(con, "DELETE FROM MaintenanceRecord WHERE MaintenanceID IN (SELECT MaintenanceID FROM Maintenance WHERE ShipID=?)", id);
            exec(con, "DELETE FROM Maintenance WHERE ShipID=?", id);
            exec(con, "DELETE FROM DockAssignment WHERE ShipID=?", id);
            exec(con, "DELETE FROM DiningVenue WHERE ShipID=?", id);
            exec(con, "DELETE FROM Facility WHERE ShipID=?", id);
            exec(con, "DELETE FROM EntertainmentEvent WHERE ShipID=?", id);
            exec(con, "DELETE FROM EmergencyDrill WHERE ShipID=?", id);
            exec(con, "DELETE FROM Supplies WHERE ShipID=?", id);
            exec(con, "DELETE FROM Ship WHERE ShipID=?", id);
            loadData();
        } catch (SQLException e) { showError(e); }
    }

    private void exec(Connection con, String sql, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    private int selectedId() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Please select a row first."); return -1; }
        return (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
    }

    private Window getWindow() { return SwingUtilities.getWindowAncestor(this); }
    private void showError(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    // ── Form dialog ───────────────────────────────────────────────────────────

    static class ShipDialog extends JDialog {
        boolean saved = false;
        private final JTextField nameField     = new JTextField(20);
        private final JTextField capacityField = new JTextField(10);
        private final JComboBox<String[]> typeCombo = new JComboBox<>();
        private final int shipId;

        ShipDialog(Window parent, int shipId) {
            super(parent, shipId < 0 ? "Add Ship" : "Edit Ship", ModalityType.APPLICATION_MODAL);
            this.shipId = shipId;
            buildUI();
            if (shipId > 0) loadExisting();
            pack();
            setResizable(false);
            setLocationRelativeTo(parent);
        }

        private void buildUI() {
            try (Statement st = DBConnection.get().createStatement();
                 ResultSet rs = st.executeQuery("SELECT ShipTypeID, TypeName FROM ShipType ORDER BY TypeName")) {
                while (rs.next()) typeCombo.addItem(new String[]{rs.getString(1), rs.getString(2)});
            } catch (SQLException ignored) {}
            typeCombo.setRenderer((list, val, idx, sel, foc) ->
                    new JLabel(val != null ? val[1] : ""));

            JPanel form = formPanel();
            addRow(form, 0, "Ship Name:",  nameField);
            addRow(form, 1, "Capacity:",   capacityField);
            addRow(form, 2, "Ship Type:",  typeCombo);

            JButton save   = new JButton("Save");
            JButton cancel = new JButton("Cancel");
            addButtons(form, 3, save, cancel);

            save.addActionListener(e -> save());
            cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);
            add(form);
        }

        private void loadExisting() {
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT ShipName, Capacity, ShipTypeID FROM Ship WHERE ShipID=?")) {
                ps.setInt(1, shipId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    nameField.setText(rs.getString(1));
                    capacityField.setText(String.valueOf(rs.getInt(2)));
                    String typeId = String.valueOf(rs.getInt(3));
                    for (int i = 0; i < typeCombo.getItemCount(); i++)
                        if (typeCombo.getItemAt(i)[0].equals(typeId)) { typeCombo.setSelectedIndex(i); break; }
                }
            } catch (SQLException ignored) {}
        }

        private void save() {
            String name = nameField.getText().trim();
            String capStr = capacityField.getText().trim();
            if (name.isEmpty() || capStr.isEmpty() || typeCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "All fields are required."); return;
            }
            int cap;
            try { cap = Integer.parseInt(capStr); }
            catch (NumberFormatException e) { JOptionPane.showMessageDialog(this, "Capacity must be a whole number."); return; }
            int typeId = Integer.parseInt(((String[]) typeCombo.getSelectedItem())[0]);
            try {
                if (shipId < 0) {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "INSERT INTO Ship (ShipName, Capacity, ShipTypeID) VALUES (?,?,?)")) {
                        ps.setString(1, name); ps.setInt(2, cap); ps.setInt(3, typeId);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "UPDATE Ship SET ShipName=?, Capacity=?, ShipTypeID=? WHERE ShipID=?")) {
                        ps.setString(1, name); ps.setInt(2, cap); ps.setInt(3, typeId); ps.setInt(4, shipId);
                        ps.executeUpdate();
                    }
                }
                saved = true; dispose();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Shared form helpers (used by all dialogs) ─────────────────────────────

    static JPanel formPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        return p;
    }

    static void addRow(JPanel p, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0; c.gridy = row; c.anchor = GridBagConstraints.EAST;
        p.add(new JLabel(label), c);
        c.gridx = 1; c.anchor = GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;
        p.add(field, c);
    }

    static void addButtons(JPanel p, int row, JButton... btns) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(10, 5, 5, 5);
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        for (JButton b : btns) bp.add(b);
        p.add(bp, c);
    }
}

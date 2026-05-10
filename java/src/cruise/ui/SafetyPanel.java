package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class SafetyPanel extends JPanel {

    private final DefaultTableModel drillModel =
            tModel("Drill ID", "Date", "Type", "Ship");
    private final JTable drillTable = makeTable(drillModel);

    public SafetyPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel heading = new JLabel("Emergency Drills");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        JButton addBtn  = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton delBtn  = new JButton("Delete");

        addBtn.addActionListener(e -> {
            new DrillDialog(SwingUtilities.getWindowAncestor(this), -1, null, null, -1)
                    .setVisible(true);
            loadDrills();
        });
        editBtn.addActionListener(e -> editDrill());
        delBtn.addActionListener(e -> {
            int row = drillTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Select a drill first."); return; }
            int id = (int) drillModel.getValueAt(row, 0);
            if (JOptionPane.showConfirmDialog(this, "Delete this drill?", "Confirm",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "DELETE FROM EmergencyDrill WHERE DrillID=?")) {
                ps.setInt(1, id); ps.executeUpdate(); loadDrills();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btnRow.add(addBtn); btnRow.add(editBtn); btnRow.add(delBtn);

        JPanel content = new JPanel(new BorderLayout(4, 4));
        content.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        content.add(btnRow,                    BorderLayout.NORTH);
        content.add(new JScrollPane(drillTable), BorderLayout.CENTER);

        add(heading, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);

        loadDrills();
    }

    private void editDrill() {
        int row = drillTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a drill first."); return; }
        int    id   = (int)    drillModel.getValueAt(row, 0);
        String date = (String) drillModel.getValueAt(row, 1);
        String type = (String) drillModel.getValueAt(row, 2);
        int shipId  = shipIdByName((String) drillModel.getValueAt(row, 3));
        new DrillDialog(SwingUtilities.getWindowAncestor(this), id, date, type, shipId).setVisible(true);
        loadDrills();
    }

    private void loadDrills() {
        drillModel.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT d.DrillID, d.DrillDate, d.DrillType, s.ShipName " +
                "FROM EmergencyDrill d JOIN Ship s ON d.ShipID = s.ShipID " +
                "ORDER BY d.DrillDate DESC")) {
            while (rs.next())
                drillModel.addRow(new Object[]{
                        rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)});
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int shipIdByName(String name) {
        try (PreparedStatement ps = DBConnection.get().prepareStatement(
                "SELECT ShipID FROM Ship WHERE ShipName=? LIMIT 1")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ignored) {}
        return -1;
    }

    private static JComboBox<String[]> shipCombo() {
        JComboBox<String[]> cb = new JComboBox<>();
        cb.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT ShipID, ShipName FROM Ship ORDER BY ShipName")) {
            while (rs.next()) cb.addItem(new String[]{rs.getString(1), rs.getString(2)});
        } catch (SQLException ignored) {}
        return cb;
    }

    private static void selectCombo(JComboBox<String[]> cb, int id) {
        for (int i = 0; i < cb.getItemCount(); i++)
            if (Integer.parseInt(cb.getItemAt(i)[0]) == id) { cb.setSelectedIndex(i); return; }
    }

    private static JTable makeTable(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(22);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return t;
    }

    private static DefaultTableModel tModel(String... cols) {
        return new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private class DrillDialog extends JDialog {
        private final int editId;
        private final JComboBox<String[]> shipC = shipCombo();
        private final JTextField dateField = new JTextField(12);
        private final JComboBox<String> typeCombo = new JComboBox<>(
                new String[]{"Fire", "Lifeboat", "Man Overboard", "Abandon Ship", "Medical", "Other"});

        DrillDialog(Window owner, int editId, String date, String type, int shipId) {
            super(owner, editId < 0 ? "Add Drill" : "Edit Drill", ModalityType.APPLICATION_MODAL);
            this.editId = editId;
            if (date   != null) dateField.setText(date);
            if (type   != null) typeCombo.setSelectedItem(type);
            if (shipId > 0)    selectCombo(shipC, shipId);

            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Ship",                    shipC);
            ShipsPanel.addRow(form, 1, "Drill Date (YYYY-MM-DD)", dateField);
            ShipsPanel.addRow(form, 2, "Drill Type",              typeCombo);

            JButton save   = new JButton("Save");
            JButton cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 3, save, cancel);
            save.addActionListener(e -> save());
            cancel.addActionListener(e -> dispose());

            setContentPane(form); pack(); setLocationRelativeTo(owner);
        }

        private void save() {
            if (shipC.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Select a ship."); return;
            }
            String date = dateField.getText().trim();
            if (date.isEmpty()) { JOptionPane.showMessageDialog(this, "Date is required."); return; }
            int shipId = Integer.parseInt(((String[]) shipC.getSelectedItem())[0]);
            try {
                if (editId < 0) {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "INSERT INTO EmergencyDrill (DrillDate, DrillType, ShipID) VALUES (?,?,?)")) {
                        ps.setString(1, date);
                        ps.setString(2, (String) typeCombo.getSelectedItem());
                        ps.setInt(3, shipId);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "UPDATE EmergencyDrill SET DrillDate=?, DrillType=?, ShipID=? WHERE DrillID=?")) {
                        ps.setString(1, date);
                        ps.setString(2, (String) typeCombo.getSelectedItem());
                        ps.setInt(3, shipId);
                        ps.setInt(4, editId);
                        ps.executeUpdate();
                    }
                }
                dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

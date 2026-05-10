package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class SuppliesPanel extends JPanel {

    public SuppliesPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel heading = new JLabel("Supplies");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("  Supplies  ",  buildSuppliesTab());
        tabs.addTab("  Suppliers  ", buildSuppliersTab());

        add(heading, BorderLayout.NORTH);
        add(tabs,    BorderLayout.CENTER);
    }

    // ── Supplies sub-tab ──────────────────────────────────────────────────────

    private JPanel buildSuppliesTab() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Supply Name", "Ship", "Qty in Stock", "Supplier"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(22);

        JButton addBtn     = new JButton("Add");
        JButton refreshBtn = new JButton("Refresh");

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btns.add(addBtn);
        btns.add(Box.createHorizontalStrut(20));
        btns.add(refreshBtn);

        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        p.add(btns, BorderLayout.SOUTH);

        Runnable load = () -> {
            model.setRowCount(0);
            try (Statement st = DBConnection.get().createStatement();
                 ResultSet rs = st.executeQuery(
                    "SELECT su.SupplyID, su.SupplyName, s.ShipName, su.QuantityInStock, " +
                    "       IFNULL(sp.SupplierName, '—') " +
                    "FROM Supplies su " +
                    "JOIN Ship s ON su.ShipID = s.ShipID " +
                    "LEFT JOIN Supplier sp ON su.SupplierID = sp.SupplierID " +
                    "ORDER BY s.ShipName, su.SupplyName")) {
                while (rs.next())
                    model.addRow(new Object[]{rs.getInt(1), rs.getString(2),
                            rs.getString(3), rs.getInt(4), rs.getString(5)});
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(p, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        load.run();

        addBtn.addActionListener(e -> {
            SupplyDialog d = new SupplyDialog(SwingUtilities.getWindowAncestor(p));
            d.setVisible(true); if (d.saved) load.run();
        });
        refreshBtn.addActionListener(e -> load.run());
        return p;
    }

    // ── Suppliers sub-tab ─────────────────────────────────────────────────────

    private JPanel buildSuppliersTab() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Supplier Name", "Contact", "Phone", "Email"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(22);

        JButton addBtn    = new JButton("Add");
        JButton editBtn   = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btns.add(addBtn); btns.add(editBtn); btns.add(deleteBtn);

        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        p.add(btns, BorderLayout.SOUTH);

        Runnable load = () -> {
            model.setRowCount(0);
            try (Statement st = DBConnection.get().createStatement();
                 ResultSet rs = st.executeQuery(
                    "SELECT SupplierID, SupplierName, IFNULL(ContactName,'—'), " +
                    "       IFNULL(Phone,'—'), IFNULL(Email,'—') " +
                    "FROM Supplier ORDER BY SupplierName")) {
                while (rs.next())
                    model.addRow(new Object[]{rs.getInt(1), rs.getString(2),
                            rs.getString(3), rs.getString(4), rs.getString(5)});
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(p, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        load.run();

        addBtn.addActionListener(e -> {
            SupplierDialog d = new SupplierDialog(SwingUtilities.getWindowAncestor(p), -1);
            d.setVisible(true); if (d.saved) load.run();
        });
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(p, "Select a supplier first."); return; }
            int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
            SupplierDialog d = new SupplierDialog(SwingUtilities.getWindowAncestor(p), id);
            d.setVisible(true); if (d.saved) load.run();
        });
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(p, "Select a supplier first."); return; }
            int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
            if (JOptionPane.showConfirmDialog(p, "Delete this supplier?", "Confirm",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "DELETE FROM Supplier WHERE SupplierID=?")) {
                ps.setInt(1, id); ps.executeUpdate(); load.run();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(p, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        return p;
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    static class SupplyDialog extends JDialog {
        boolean saved = false;
        private final JTextField nameField = new JTextField(20);
        private final JTextField qtyField  = new JTextField(8);
        private final JComboBox<String[]> shipCombo     = new JComboBox<>();
        private final JComboBox<String[]> supplierCombo = new JComboBox<>();

        SupplyDialog(Window parent) {
            super(parent, "Add Supply", ModalityType.APPLICATION_MODAL);
            try (Statement st = DBConnection.get().createStatement()) {
                ResultSet rs = st.executeQuery("SELECT ShipID, ShipName FROM Ship ORDER BY ShipName");
                while (rs.next()) shipCombo.addItem(new String[]{rs.getString(1), rs.getString(2)});
                rs = st.executeQuery("SELECT SupplierID, SupplierName FROM Supplier ORDER BY SupplierName");
                supplierCombo.addItem(new String[]{"-1", "None"});
                while (rs.next()) supplierCombo.addItem(new String[]{rs.getString(1), rs.getString(2)});
            } catch (SQLException ignored) {}
            shipCombo.setRenderer((l, v, i, s, f)     -> new JLabel(v != null ? v[1] : ""));
            supplierCombo.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));

            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Supply Name:", nameField);
            ShipsPanel.addRow(form, 1, "Ship:",        shipCombo);
            ShipsPanel.addRow(form, 2, "Qty:",         qtyField);
            ShipsPanel.addRow(form, 3, "Supplier:",    supplierCombo);

            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 4, save, cancel);
            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);
            add(form); pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void save() {
            String name = nameField.getText().trim(), qtyStr = qtyField.getText().trim();
            if (name.isEmpty() || qtyStr.isEmpty() || shipCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Name, ship, and qty are required."); return;
            }
            int qty;
            try { qty = Integer.parseInt(qtyStr); }
            catch (NumberFormatException e) { JOptionPane.showMessageDialog(this, "Qty must be a number."); return; }
            int supplierId = Integer.parseInt(((String[]) supplierCombo.getSelectedItem())[0]);
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT INTO Supplies (SupplyName, ShipID, QuantityInStock, SupplierID) VALUES (?,?,?,?)")) {
                ps.setString(1, name);
                ps.setInt(2, Integer.parseInt(((String[]) shipCombo.getSelectedItem())[0]));
                ps.setInt(3, qty);
                if (supplierId < 0) ps.setNull(4, Types.INTEGER);
                else ps.setInt(4, supplierId);
                ps.executeUpdate(); saved = true; dispose();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    static class SupplierDialog extends JDialog {
        boolean saved = false;
        private final JTextField nameField    = new JTextField(20);
        private final JTextField contactField = new JTextField(20);
        private final JTextField phoneField   = new JTextField(15);
        private final JTextField emailField   = new JTextField(25);
        private final int editId;

        SupplierDialog(Window parent, int editId) {
            super(parent, editId < 0 ? "Add Supplier" : "Edit Supplier", ModalityType.APPLICATION_MODAL);
            this.editId = editId;
            if (editId > 0) loadExisting();

            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Supplier Name:", nameField);
            ShipsPanel.addRow(form, 1, "Contact Name:",  contactField);
            ShipsPanel.addRow(form, 2, "Phone:",         phoneField);
            ShipsPanel.addRow(form, 3, "Email:",         emailField);

            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 4, save, cancel);
            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);
            add(form); pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void loadExisting() {
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT SupplierName, ContactName, Phone, Email FROM Supplier WHERE SupplierID=?")) {
                ps.setInt(1, editId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    nameField.setText(rs.getString(1));
                    contactField.setText(rs.getString(2) != null ? rs.getString(2) : "");
                    phoneField.setText(rs.getString(3) != null ? rs.getString(3) : "");
                    emailField.setText(rs.getString(4) != null ? rs.getString(4) : "");
                }
            } catch (SQLException ignored) {}
        }

        private void save() {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "Supplier name is required."); return; }
            try {
                if (editId < 0) {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "INSERT INTO Supplier (SupplierName, ContactName, Phone, Email) VALUES (?,?,?,?)")) {
                        ps.setString(1, name);
                        ps.setString(2, nul(contactField));
                        ps.setString(3, nul(phoneField));
                        ps.setString(4, nul(emailField));
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "UPDATE Supplier SET SupplierName=?, ContactName=?, Phone=?, Email=? WHERE SupplierID=?")) {
                        ps.setString(1, name);
                        ps.setString(2, nul(contactField));
                        ps.setString(3, nul(phoneField));
                        ps.setString(4, nul(emailField));
                        ps.setInt(5, editId);
                        ps.executeUpdate();
                    }
                }
                saved = true; dispose();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private String nul(JTextField f) {
            String v = f.getText().trim();
            return v.isEmpty() ? null : v;
        }
    }
}

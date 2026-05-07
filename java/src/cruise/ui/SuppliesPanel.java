package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class SuppliesPanel extends JPanel {

    private final DefaultTableModel model;
    private final JTable table;

    public SuppliesPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"ID", "Supply Name", "Ship", "Qty in Stock"};
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(22);

        JLabel heading = new JLabel("Supplies Inventory");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));

        JButton addBtn     = new JButton("Add Supply");
        JButton refreshBtn = new JButton("Refresh");

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnPanel.add(addBtn);
        btnPanel.add(Box.createHorizontalStrut(20));
        btnPanel.add(refreshBtn);

        add(heading, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        loadData();

        addBtn.addActionListener(e -> {
            SupplyDialog d = new SupplyDialog(SwingUtilities.getWindowAncestor(this));
            d.setVisible(true); if (d.saved) loadData();
        });
        refreshBtn.addActionListener(e -> loadData());
    }

    private void loadData() {
        model.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT su.SupplyID, su.SupplyName, s.ShipName, su.QuantityInStock " +
                "FROM Supplies su JOIN Ship s ON su.ShipID = s.ShipID " +
                "ORDER BY s.ShipName, su.SupplyName")) {
            while (rs.next())
                model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4)});
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    static class SupplyDialog extends JDialog {
        boolean saved = false;
        private final JTextField nameField = new JTextField(20);
        private final JTextField qtyField  = new JTextField(8);
        private final JComboBox<String[]> shipCombo = new JComboBox<>();

        SupplyDialog(Window parent) {
            super(parent, "Add Supply", ModalityType.APPLICATION_MODAL);
            try (Statement st = DBConnection.get().createStatement();
                 ResultSet rs = st.executeQuery("SELECT ShipID, ShipName FROM Ship ORDER BY ShipName")) {
                while (rs.next()) shipCombo.addItem(new String[]{rs.getString(1), rs.getString(2)});
            } catch (SQLException ignored) {}
            shipCombo.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));

            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Supply Name:",      nameField);
            ShipsPanel.addRow(form, 1, "Ship:",             shipCombo);
            ShipsPanel.addRow(form, 2, "Qty in Stock:",     qtyField);

            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 3, save, cancel);
            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);
            add(form); pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void save() {
            String name = nameField.getText().trim(), qtyStr = qtyField.getText().trim();
            if (name.isEmpty() || qtyStr.isEmpty() || shipCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "All fields are required."); return;
            }
            int qty;
            try { qty = Integer.parseInt(qtyStr); }
            catch (NumberFormatException e) { JOptionPane.showMessageDialog(this, "Qty must be a number."); return; }
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT INTO Supplies (SupplyName, ShipID, QuantityInStock) VALUES (?,?,?)")) {
                ps.setString(1, name);
                ps.setInt(2, Integer.parseInt(((String[]) shipCombo.getSelectedItem())[0]));
                ps.setInt(3, qty);
                ps.executeUpdate(); saved = true; dispose();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

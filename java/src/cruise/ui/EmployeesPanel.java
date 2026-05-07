package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class EmployeesPanel extends JPanel {

    private final DefaultTableModel model;
    private final JTable table;

    public EmployeesPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"ID", "First Name", "Last Name", "Title", "Hire Date"};
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(22);

        JLabel heading = new JLabel("Employees");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));

        JButton addBtn    = new JButton("Add");
        JButton editBtn   = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        JButton refreshBtn = new JButton("Refresh");

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnPanel.add(addBtn); btnPanel.add(editBtn); btnPanel.add(deleteBtn);
        btnPanel.add(Box.createHorizontalStrut(20)); btnPanel.add(refreshBtn);

        add(heading, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        loadData();

        addBtn.addActionListener(e -> {
            EmployeeDialog d = new EmployeeDialog(getWindow(), -1);
            d.setVisible(true); if (d.saved) loadData();
        });
        editBtn.addActionListener(e -> {
            int id = selectedId(); if (id < 0) return;
            EmployeeDialog d = new EmployeeDialog(getWindow(), id);
            d.setVisible(true); if (d.saved) loadData();
        });
        deleteBtn.addActionListener(e -> deleteSelected());
        refreshBtn.addActionListener(e -> loadData());
    }

    private void loadData() {
        model.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT e.EmployeeID, e.FirstName, e.LastName, j.Title, e.HireDate " +
                "FROM Employee e JOIN JobPosition j ON e.PositionID = j.PositionID ORDER BY e.LastName")) {
            while (rs.next())
                model.addRow(new Object[]{rs.getInt(1), rs.getString(2),
                        rs.getString(3), rs.getString(4), rs.getString(5)});
        } catch (SQLException e) { showError(e); }
    }

    private void deleteSelected() {
        int id = selectedId(); if (id < 0) return;
        int row = table.convertRowIndexToModel(table.getSelectedRow());
        String name = model.getValueAt(row, 1) + " " + model.getValueAt(row, 2);
        if (JOptionPane.showConfirmDialog(this,
                "Delete employee \"" + name + "\"?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (PreparedStatement ps = DBConnection.get().prepareStatement(
                "DELETE FROM Employee WHERE EmployeeID=?")) {
            ps.setInt(1, id); ps.executeUpdate(); loadData();
        } catch (SQLException e) { showError(e); }
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

    static class EmployeeDialog extends JDialog {
        boolean saved = false;
        private final JTextField firstField  = new JTextField(15);
        private final JTextField lastField   = new JTextField(15);
        private final JTextField hireField   = new JTextField(10);
        private final JComboBox<String[]> posCombo = new JComboBox<>();
        private final int empId;

        EmployeeDialog(Window parent, int empId) {
            super(parent, empId < 0 ? "Add Employee" : "Edit Employee", ModalityType.APPLICATION_MODAL);
            this.empId = empId;
            buildUI();
            if (empId > 0) loadExisting();
            pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void buildUI() {
            try (Statement st = DBConnection.get().createStatement();
                 ResultSet rs = st.executeQuery("SELECT PositionID, Title FROM JobPosition ORDER BY Title")) {
                while (rs.next()) posCombo.addItem(new String[]{rs.getString(1), rs.getString(2)});
            } catch (SQLException ignored) {}
            posCombo.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));

            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "First Name:", firstField);
            ShipsPanel.addRow(form, 1, "Last Name:",  lastField);
            ShipsPanel.addRow(form, 2, "Position:",   posCombo);
            ShipsPanel.addRow(form, 3, "Hire Date (YYYY-MM-DD):", hireField);

            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 4, save, cancel);
            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);
            add(form);
        }

        private void loadExisting() {
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT FirstName, LastName, PositionID, HireDate FROM Employee WHERE EmployeeID=?")) {
                ps.setInt(1, empId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    firstField.setText(rs.getString(1));
                    lastField.setText(rs.getString(2));
                    String pid = rs.getString(3);
                    hireField.setText(rs.getString(4));
                    for (int i = 0; i < posCombo.getItemCount(); i++)
                        if (posCombo.getItemAt(i)[0].equals(pid)) { posCombo.setSelectedIndex(i); break; }
                }
            } catch (SQLException ignored) {}
        }

        private void save() {
            String first = firstField.getText().trim(), last = lastField.getText().trim();
            String hire  = hireField.getText().trim();
            if (first.isEmpty() || last.isEmpty() || posCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "First name, last name, and position are required."); return;
            }
            int posId = Integer.parseInt(((String[]) posCombo.getSelectedItem())[0]);
            try {
                if (empId < 0) {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "INSERT INTO Employee (FirstName, LastName, PositionID, HireDate) VALUES (?,?,?,?)")) {
                        ps.setString(1, first); ps.setString(2, last);
                        ps.setInt(3, posId); ps.setString(4, hire.isEmpty() ? null : hire);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "UPDATE Employee SET FirstName=?, LastName=?, PositionID=?, HireDate=? WHERE EmployeeID=?")) {
                        ps.setString(1, first); ps.setString(2, last);
                        ps.setInt(3, posId); ps.setString(4, hire.isEmpty() ? null : hire); ps.setInt(5, empId);
                        ps.executeUpdate();
                    }
                }
                saved = true; dispose();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

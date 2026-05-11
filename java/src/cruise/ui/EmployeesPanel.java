package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class EmployeesPanel extends JPanel {

    public EmployeesPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel heading = new JLabel("Employees");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("  Employees  ",     buildEmployeesTab());
        tabs.addTab("  Job Positions  ", buildPositionsTab());
        tabs.addTab("  Captains  ",      buildCaptainsTab());

        add(heading, BorderLayout.NORTH);
        add(tabs,    BorderLayout.CENTER);
    }

    // ── Employees sub-tab ─────────────────────────────────────────────────────

    private JPanel buildEmployeesTab() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "First Name", "Last Name", "Title", "Hire Date"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(22);

        JButton addBtn    = new JButton("Add");
        JButton editBtn   = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        JButton refreshBtn = new JButton("Refresh");

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btns.add(addBtn); btns.add(editBtn); btns.add(deleteBtn);
        btns.add(Box.createHorizontalStrut(20)); btns.add(refreshBtn);

        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        p.add(btns, BorderLayout.SOUTH);

        Runnable load = () -> {
            model.setRowCount(0);
            try (Statement st = DBConnection.get().createStatement();
                 ResultSet rs = st.executeQuery(
                    "SELECT e.EmployeeID, e.FirstName, e.LastName, j.Title, e.HireDate " +
                    "FROM Employee e JOIN JobPosition j ON e.PositionID=j.PositionID ORDER BY e.LastName")) {
                while (rs.next())
                    model.addRow(new Object[]{rs.getInt(1), rs.getString(2),
                            rs.getString(3), rs.getString(4), rs.getString(5)});
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(p, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        load.run();

        addBtn.addActionListener(e -> {
            EmployeeDialog d = new EmployeeDialog(SwingUtilities.getWindowAncestor(p), -1);
            d.setVisible(true); if (d.saved) load.run();
        });
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(p, "Select a row first."); return; }
            int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
            EmployeeDialog d = new EmployeeDialog(SwingUtilities.getWindowAncestor(p), id);
            d.setVisible(true); if (d.saved) load.run();
        });
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(p, "Select a row first."); return; }
            int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
            if (JOptionPane.showConfirmDialog(p, "Delete this employee? Their crew assignments and schedules will also be removed.",
                    "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try {
                Connection con = DBConnection.get();
                exec(con, "DELETE FROM CaptainShipType WHERE CaptainID IN (SELECT CaptainID FROM Captain WHERE EmployeeID=?)", id);
                exec(con, "DELETE FROM Captain WHERE EmployeeID=?", id);
                exec(con, "DELETE FROM CrewCabin WHERE EmployeeID=?", id);
                exec(con, "DELETE FROM ShipCrew WHERE EmployeeID=?", id);
                exec(con, "DELETE FROM WorkSchedule WHERE EmployeeID=?", id);
                exec(con, "DELETE FROM Employee WHERE EmployeeID=?", id);
                load.run();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(p, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        refreshBtn.addActionListener(e -> load.run());
        return p;
    }

    // ── Job Positions sub-tab ─────────────────────────────────────────────────

    private JPanel buildPositionsTab() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Title"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
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
                 ResultSet rs = st.executeQuery("SELECT PositionID, Title FROM JobPosition ORDER BY Title")) {
                while (rs.next()) model.addRow(new Object[]{rs.getInt(1), rs.getString(2)});
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(p, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        load.run();

        addBtn.addActionListener(e -> {
            String title = JOptionPane.showInputDialog(p, "Position title:");
            if (title == null || title.trim().isEmpty()) return;
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT INTO JobPosition (Title) VALUES (?)")) {
                ps.setString(1, title.trim()); ps.executeUpdate(); load.run();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(p, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(p, "Select a row first."); return; }
            int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
            String current = (String) model.getValueAt(table.convertRowIndexToModel(row), 1);
            String title = (String) JOptionPane.showInputDialog(p, "Position title:", "Edit Position",
                    JOptionPane.PLAIN_MESSAGE, null, null, current);
            if (title == null || title.trim().isEmpty()) return;
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "UPDATE JobPosition SET Title=? WHERE PositionID=?")) {
                ps.setString(1, title.trim()); ps.setInt(2, id); ps.executeUpdate(); load.run();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(p, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(p, "Select a row first."); return; }
            int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
            if (JOptionPane.showConfirmDialog(p, "Delete this position?", "Confirm",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "DELETE FROM JobPosition WHERE PositionID=?")) {
                ps.setInt(1, id); ps.executeUpdate(); load.run();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(p, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        return p;
    }

    private static void exec(Connection con, String sql, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    // ── Captains sub-tab ──────────────────────────────────────────────────────

    private JPanel buildCaptainsTab() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Captain ID", "Employee Name", "License #", "Certified Ship Types"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(22);

        JButton addBtn    = new JButton("Add");
        JButton deleteBtn = new JButton("Delete");
        JButton refreshBtn = new JButton("Refresh");

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btns.add(addBtn); btns.add(deleteBtn);
        btns.add(Box.createHorizontalStrut(20)); btns.add(refreshBtn);

        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        p.add(btns, BorderLayout.SOUTH);

        Runnable load = () -> {
            model.setRowCount(0);
            try (Statement st = DBConnection.get().createStatement();
                 ResultSet rs = st.executeQuery(
                    "SELECT c.CaptainID, CONCAT(e.FirstName,' ',e.LastName), c.LicenseNumber, " +
                    "       GROUP_CONCAT(st.TypeName ORDER BY st.TypeName SEPARATOR ', ') " +
                    "FROM Captain c " +
                    "JOIN Employee e ON c.EmployeeID = e.EmployeeID " +
                    "LEFT JOIN CaptainShipType cst ON c.CaptainID = cst.CaptainID " +
                    "LEFT JOIN ShipType st ON cst.ShipTypeID = st.ShipTypeID " +
                    "GROUP BY c.CaptainID, e.FirstName, e.LastName, c.LicenseNumber " +
                    "ORDER BY e.LastName")) {
                while (rs.next())
                    model.addRow(new Object[]{rs.getInt(1), rs.getString(2),
                            rs.getString(3), rs.getString(4) != null ? rs.getString(4) : "None"});
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(p, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        load.run();

        addBtn.addActionListener(e -> {
            CaptainDialog d = new CaptainDialog(SwingUtilities.getWindowAncestor(p));
            d.setVisible(true); if (d.saved) load.run();
        });
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(p, "Select a captain first."); return; }
            int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
            if (JOptionPane.showConfirmDialog(p, "Remove captain record #" + id + "?", "Confirm",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "DELETE FROM CaptainShipType WHERE CaptainID=?")) {
                ps.setInt(1, id); ps.executeUpdate();
            } catch (SQLException ex) { /* cascade */ }
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "DELETE FROM Captain WHERE CaptainID=?")) {
                ps.setInt(1, id); ps.executeUpdate(); load.run();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(p, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        refreshBtn.addActionListener(e -> load.run());
        return p;
    }

    // ── Employee Dialog ───────────────────────────────────────────────────────

    static class EmployeeDialog extends JDialog {
        boolean saved = false;
        private final JTextField firstField = new JTextField(15);
        private final JTextField lastField  = new JTextField(15);
        private final JTextField hireField  = new JTextField(10);
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
                ps.setInt(1, empId); ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    firstField.setText(rs.getString(1));
                    lastField.setText(rs.getString(2));
                    String pid = rs.getString(3);
                    hireField.setText(rs.getString(4) != null ? rs.getString(4) : "");
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

    // ── Captain Dialog ────────────────────────────────────────────────────────

    static class CaptainDialog extends JDialog {
        boolean saved = false;
        private final JComboBox<String[]> empCombo     = new JComboBox<>();
        private final JTextField licenseField           = new JTextField(20);
        private JList<String[]> shipTypeList;
        private java.util.List<String[]> allShipTypes   = new java.util.ArrayList<>();

        CaptainDialog(Window parent) {
            super(parent, "Add Captain", ModalityType.APPLICATION_MODAL);
            buildUI(); pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void buildUI() {
            try (Statement st = DBConnection.get().createStatement()) {
                ResultSet rs = st.executeQuery(
                    "SELECT e.EmployeeID, CONCAT(e.FirstName,' ',e.LastName) " +
                    "FROM Employee e JOIN JobPosition j ON e.PositionID=j.PositionID " +
                    "WHERE j.Title='Captain' ORDER BY e.LastName");
                while (rs.next()) empCombo.addItem(new String[]{rs.getString(1), rs.getString(2)});
                rs = st.executeQuery("SELECT ShipTypeID, TypeName FROM ShipType ORDER BY TypeName");
                while (rs.next()) allShipTypes.add(new String[]{rs.getString(1), rs.getString(2)});
            } catch (SQLException ignored) {}
            empCombo.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));

            DefaultListModel<String[]> listModel = new DefaultListModel<>();
            for (String[] st : allShipTypes) listModel.addElement(st);
            shipTypeList = new JList<>(listModel);
            shipTypeList.setCellRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));
            shipTypeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            shipTypeList.setVisibleRowCount(4);

            JPanel form = new JPanel(new java.awt.GridBagLayout());
            form.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
            java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
            c.insets = new java.awt.Insets(4, 6, 4, 6);
            c.anchor = java.awt.GridBagConstraints.WEST;

            c.gridx = 0; c.gridy = 0; form.add(new JLabel("Employee:"),      c);
            c.gridx = 1;              form.add(empCombo,                      c);
            c.gridx = 0; c.gridy = 1; form.add(new JLabel("License #:"),     c);
            c.gridx = 1;              form.add(licenseField,                  c);
            c.gridx = 0; c.gridy = 2; c.anchor = java.awt.GridBagConstraints.NORTHWEST;
                                      form.add(new JLabel("Certified Types:"),c);
            c.gridx = 1; c.anchor = java.awt.GridBagConstraints.WEST;
                                      form.add(new JScrollPane(shipTypeList), c);

            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            btnRow.add(save); btnRow.add(cancel);
            c.gridx = 0; c.gridy = 3; c.gridwidth = 2;
            c.anchor = java.awt.GridBagConstraints.EAST;
            form.add(btnRow, c);

            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);
            add(form);
        }

        private void save() {
            if (empCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Select an employee."); return;
            }
            String license = licenseField.getText().trim();
            if (license.isEmpty()) {
                JOptionPane.showMessageDialog(this, "License number is required."); return;
            }
            int empId = Integer.parseInt(((String[]) empCombo.getSelectedItem())[0]);
            try {
                int captainId;
                try (PreparedStatement ps = DBConnection.get().prepareStatement(
                        "INSERT INTO Captain (EmployeeID, LicenseNumber) VALUES (?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, empId); ps.setString(2, license);
                    ps.executeUpdate();
                    ResultSet keys = ps.getGeneratedKeys(); keys.next();
                    captainId = keys.getInt(1);
                }
                for (String[] st : shipTypeList.getSelectedValuesList()) {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "INSERT INTO CaptainShipType (CaptainID, ShipTypeID) VALUES (?,?)")) {
                        ps.setInt(1, captainId);
                        ps.setInt(2, Integer.parseInt(st[0]));
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

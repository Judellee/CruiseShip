package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class MaintenancePanel extends JPanel {

    public MaintenancePanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel heading = new JLabel("Maintenance");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("  Records  ", buildRecordsTab());
        tabs.addTab("  Tasks  ",   buildTasksTab());

        add(heading, BorderLayout.NORTH);
        add(tabs,    BorderLayout.CENTER);
    }

    // ── Records sub-tab ───────────────────────────────────────────────────────

    private JPanel buildRecordsTab() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Task", "Ship", "Date", "Technician", "Notes"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(22);
        table.getColumnModel().getColumn(5).setPreferredWidth(200);

        JButton addBtn     = new JButton("Add");
        JButton editBtn    = new JButton("Edit");
        JButton deleteBtn  = new JButton("Delete");
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
                    "SELECT mr.RecordID, m.MaintenanceName, s.ShipName, mr.MaintenanceDate, " +
                    "       IFNULL(CONCAT(e.FirstName,' ',e.LastName),'—') AS Technician, mr.Notes " +
                    "FROM MaintenanceRecord mr " +
                    "JOIN Maintenance m  ON mr.MaintenanceID = m.MaintenanceID " +
                    "JOIN Ship s         ON m.ShipID         = s.ShipID " +
                    "LEFT JOIN Employee e ON mr.EmployeeID   = e.EmployeeID " +
                    "ORDER BY mr.MaintenanceDate DESC")) {
                while (rs.next())
                    model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getString(5), rs.getString(6)});
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(p, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        load.run();

        addBtn.addActionListener(e -> {
            RecordDialog d = new RecordDialog(SwingUtilities.getWindowAncestor(p), -1);
            d.setVisible(true); if (d.saved) load.run();
        });
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(p, "Select a record first."); return; }
            int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
            RecordDialog d = new RecordDialog(SwingUtilities.getWindowAncestor(p), id);
            d.setVisible(true); if (d.saved) load.run();
        });
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(p, "Select a record first."); return; }
            int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
            if (JOptionPane.showConfirmDialog(p, "Delete this maintenance record?", "Confirm",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "DELETE FROM MaintenanceRecord WHERE RecordID=?")) {
                ps.setInt(1, id); ps.executeUpdate(); load.run();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(p, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        refreshBtn.addActionListener(e -> load.run());
        return p;
    }

    // ── Tasks sub-tab ─────────────────────────────────────────────────────────

    private JPanel buildTasksTab() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Task Name", "Ship"}, 0) {
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
                    "SELECT m.MaintenanceID, m.MaintenanceName, s.ShipName " +
                    "FROM Maintenance m JOIN Ship s ON m.ShipID = s.ShipID " +
                    "ORDER BY s.ShipName, m.MaintenanceName")) {
                while (rs.next())
                    model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3)});
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(p, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        load.run();

        addBtn.addActionListener(e -> {
            TaskDialog d = new TaskDialog(SwingUtilities.getWindowAncestor(p), -1, null, -1);
            d.setVisible(true); if (d.saved) load.run();
        });
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(p, "Select a task first."); return; }
            int modelRow = table.convertRowIndexToModel(row);
            int id = (int) model.getValueAt(modelRow, 0);
            String name = (String) model.getValueAt(modelRow, 1);
            TaskDialog d = new TaskDialog(SwingUtilities.getWindowAncestor(p), id, name, -1);
            d.setVisible(true); if (d.saved) load.run();
        });
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(p, "Select a task first."); return; }
            int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
            try {
                int count;
                try (PreparedStatement chk = DBConnection.get().prepareStatement(
                        "SELECT COUNT(*) FROM MaintenanceRecord WHERE MaintenanceID=?")) {
                    chk.setInt(1, id); ResultSet rs = chk.executeQuery();
                    count = rs.next() ? rs.getInt(1) : 0;
                }
                String msg = count > 0
                    ? "Delete this task? " + count + " associated record(s) will also be deleted."
                    : "Delete this task?";
                if (JOptionPane.showConfirmDialog(p, msg, "Confirm",
                        JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
                try (PreparedStatement ps = DBConnection.get().prepareStatement(
                        "DELETE FROM MaintenanceRecord WHERE MaintenanceID=?")) {
                    ps.setInt(1, id); ps.executeUpdate();
                }
                try (PreparedStatement ps = DBConnection.get().prepareStatement(
                        "DELETE FROM Maintenance WHERE MaintenanceID=?")) {
                    ps.setInt(1, id); ps.executeUpdate();
                }
                load.run();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(p, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        refreshBtn.addActionListener(e -> load.run());
        return p;
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    static class RecordDialog extends JDialog {
        boolean saved = false;
        private final int editId;
        private final JComboBox<String[]> taskCombo = new JComboBox<>();
        private final JComboBox<String[]> techCombo = new JComboBox<>();
        private final JTextField dateField  = new JTextField(12);
        private final JTextArea  notesArea  = new JTextArea(3, 25);

        RecordDialog(Window parent, int editId) {
            super(parent, editId < 0 ? "Add Maintenance Record" : "Edit Maintenance Record",
                    ModalityType.APPLICATION_MODAL);
            this.editId = editId;
            buildUI();
            if (editId > 0) loadExisting();
            pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void buildUI() {
            loadCombo(taskCombo,
                "SELECT MaintenanceID, CONCAT(MaintenanceName,' (',ShipName,')') " +
                "FROM Maintenance m JOIN Ship s ON m.ShipID=s.ShipID ORDER BY MaintenanceName");
            for (JComboBox<String[]> cb : new JComboBox[]{taskCombo, techCombo})
                cb.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));
            taskCombo.addActionListener(e -> reloadTech());
            reloadTech();

            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Maintenance Task:",  taskCombo);
            ShipsPanel.addRow(form, 1, "Technician:",        techCombo);
            ShipsPanel.addRow(form, 2, "Date (YYYY-MM-DD):", dateField);
            notesArea.setLineWrap(true); notesArea.setWrapStyleWord(true);
            ShipsPanel.addRow(form, 3, "Notes:", new JScrollPane(notesArea));

            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 4, save, cancel);
            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);
            add(form);
        }

        private void reloadTech() {
            techCombo.removeAllItems();
            techCombo.addItem(new String[]{"-1", "— None —"});
            if (taskCombo.getSelectedItem() == null) return;
            String taskId = ((String[]) taskCombo.getSelectedItem())[0];
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT DISTINCT e.EmployeeID, CONCAT(e.FirstName,' ',e.LastName) " +
                    "FROM Employee e " +
                    "JOIN ShipCrew sc ON e.EmployeeID = sc.EmployeeID " +
                    "JOIN Maintenance m ON m.ShipID = sc.ShipID " +
                    "WHERE m.MaintenanceID = ? ORDER BY e.LastName")) {
                ps.setInt(1, Integer.parseInt(taskId));
                ResultSet rs = ps.executeQuery();
                while (rs.next()) techCombo.addItem(new String[]{rs.getString(1), rs.getString(2)});
            } catch (SQLException ignored) {}
        }

        private void loadCombo(JComboBox<String[]> cb, String sql) {
            try (Statement st = DBConnection.get().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) cb.addItem(new String[]{rs.getString(1), rs.getString(2)});
            } catch (SQLException ignored) {}
        }

        private void loadExisting() {
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT MaintenanceID, EmployeeID, MaintenanceDate, Notes FROM MaintenanceRecord WHERE RecordID=?")) {
                ps.setInt(1, editId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String taskId = String.valueOf(rs.getInt(1));
                    for (int i = 0; i < taskCombo.getItemCount(); i++)
                        if (taskCombo.getItemAt(i)[0].equals(taskId)) { taskCombo.setSelectedIndex(i); break; }
                    reloadTech(); // ensure tech list matches selected task's ship
                    int empId = rs.getInt(2);
                    if (!rs.wasNull()) {
                        String eid = String.valueOf(empId);
                        for (int i = 0; i < techCombo.getItemCount(); i++)
                            if (techCombo.getItemAt(i)[0].equals(eid)) { techCombo.setSelectedIndex(i); break; }
                    }
                    dateField.setText(rs.getString(3) != null ? rs.getString(3) : "");
                    notesArea.setText(rs.getString(4) != null ? rs.getString(4) : "");
                }
            } catch (SQLException ignored) {}
        }

        private void save() {
            String date = dateField.getText().trim();
            if (date.isEmpty() || taskCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Task and date are required."); return;
            }
            int taskId = Integer.parseInt(((String[]) taskCombo.getSelectedItem())[0]);
            int techId = Integer.parseInt(((String[]) techCombo.getSelectedItem())[0]);
            String notes = notesArea.getText().trim();
            try {
                if (editId < 0) {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "INSERT INTO MaintenanceRecord (MaintenanceID, MaintenanceDate, EmployeeID, Notes) VALUES (?,?,?,?)")) {
                        ps.setInt(1, taskId);
                        ps.setString(2, date);
                        if (techId < 0) ps.setNull(3, Types.INTEGER); else ps.setInt(3, techId);
                        ps.setString(4, notes.isEmpty() ? null : notes);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "UPDATE MaintenanceRecord SET MaintenanceID=?, MaintenanceDate=?, EmployeeID=?, Notes=? WHERE RecordID=?")) {
                        ps.setInt(1, taskId);
                        ps.setString(2, date);
                        if (techId < 0) ps.setNull(3, Types.INTEGER); else ps.setInt(3, techId);
                        ps.setString(4, notes.isEmpty() ? null : notes);
                        ps.setInt(5, editId);
                        ps.executeUpdate();
                    }
                }
                saved = true; dispose();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    static class TaskDialog extends JDialog {
        boolean saved = false;
        private final int editId;
        private final JTextField nameField = new JTextField(20);
        private final JComboBox<String[]> shipCombo = new JComboBox<>();

        TaskDialog(Window parent, int editId, String existingName, int existingShipId) {
            super(parent, editId < 0 ? "Add Task" : "Edit Task", ModalityType.APPLICATION_MODAL);
            this.editId = editId;
            try (Statement st = DBConnection.get().createStatement();
                 ResultSet rs = st.executeQuery("SELECT ShipID, ShipName FROM Ship ORDER BY ShipName")) {
                while (rs.next()) shipCombo.addItem(new String[]{rs.getString(1), rs.getString(2)});
            } catch (SQLException ignored) {}
            shipCombo.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));

            if (existingName != null) nameField.setText(existingName);
            if (editId > 0) loadExistingShip();

            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Task Name:", nameField);
            ShipsPanel.addRow(form, 1, "Ship:",      shipCombo);

            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 2, save, cancel);
            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);
            add(form); pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void loadExistingShip() {
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT ShipID FROM Maintenance WHERE MaintenanceID=?")) {
                ps.setInt(1, editId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String sid = String.valueOf(rs.getInt(1));
                    for (int i = 0; i < shipCombo.getItemCount(); i++)
                        if (shipCombo.getItemAt(i)[0].equals(sid)) { shipCombo.setSelectedIndex(i); break; }
                }
            } catch (SQLException ignored) {}
        }

        private void save() {
            String name = nameField.getText().trim();
            if (name.isEmpty() || shipCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Task name and ship are required."); return;
            }
            int shipId = Integer.parseInt(((String[]) shipCombo.getSelectedItem())[0]);
            try {
                if (editId < 0) {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "INSERT INTO Maintenance (MaintenanceName, ShipID) VALUES (?,?)")) {
                        ps.setString(1, name); ps.setInt(2, shipId); ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "UPDATE Maintenance SET MaintenanceName=?, ShipID=? WHERE MaintenanceID=?")) {
                        ps.setString(1, name); ps.setInt(2, shipId); ps.setInt(3, editId);
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

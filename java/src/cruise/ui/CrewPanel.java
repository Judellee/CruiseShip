package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class CrewPanel extends JPanel {

    private final DefaultTableModel assignModel   = makeModel("ID", "Employee", "Title", "Ship", "Start Date", "End Date");
    private final DefaultTableModel cabinModel    = makeModel("ID", "Employee", "Cabin", "Cabin Type", "Ship", "Assigned Date");
    private final DefaultTableModel scheduleModel = makeModel("ID", "Employee", "Ship", "Work Date", "Shift Start", "Shift End");

    public CrewPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel heading = new JLabel("Crew Management");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("  Ship Assignments  ", buildSubPanel(assignModel,   "assign"));
        tabs.addTab("  Crew Cabins  ",      buildSubPanel(cabinModel,    "cabin"));
        tabs.addTab("  Work Schedules  ",   buildSubPanel(scheduleModel, "schedule"));

        add(heading, BorderLayout.NORTH);
        add(tabs,    BorderLayout.CENTER);

        loadAll();
    }

    private void loadAll() {
        loadAssignments(); loadCabins(); loadSchedules();
    }

    private void loadAssignments() {
        assignModel.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT sc.AssignmentID, CONCAT(e.FirstName,' ',e.LastName), j.Title, " +
                "       s.ShipName, sc.StartDate, sc.EndDate " +
                "FROM ShipCrew sc " +
                "JOIN Employee e    ON sc.EmployeeID = e.EmployeeID " +
                "JOIN JobPosition j ON e.PositionID  = j.PositionID " +
                "JOIN Ship s        ON sc.ShipID     = s.ShipID " +
                "ORDER BY s.ShipName, e.LastName")) {
            while (rs.next())
                assignModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6)});
        } catch (SQLException e) { showError(e); }
    }

    private void loadCabins() {
        cabinModel.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT cc.CrewCabinID, CONCAT(e.FirstName,' ',e.LastName), " +
                "       c.CabinNumber, c.CabinType, s.ShipName, cc.AssignedDate " +
                "FROM CrewCabin cc " +
                "JOIN Employee e ON cc.EmployeeID = e.EmployeeID " +
                "JOIN Cabin c    ON cc.CabinID    = c.CabinID " +
                "JOIN Ship s     ON c.ShipID      = s.ShipID " +
                "ORDER BY s.ShipName, e.LastName")) {
            while (rs.next())
                cabinModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6)});
        } catch (SQLException e) { showError(e); }
    }

    private void loadSchedules() {
        scheduleModel.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT ws.ScheduleID, CONCAT(e.FirstName,' ',e.LastName), " +
                "       s.ShipName, ws.WorkDate, ws.ShiftStart, ws.ShiftEnd " +
                "FROM WorkSchedule ws " +
                "JOIN Employee e ON ws.EmployeeID = e.EmployeeID " +
                "JOIN Ship s     ON ws.ShipID     = s.ShipID " +
                "ORDER BY ws.WorkDate DESC, e.LastName")) {
            while (rs.next())
                scheduleModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6)});
        } catch (SQLException e) { showError(e); }
    }

    private JPanel buildSubPanel(DefaultTableModel m, String type) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTable table = new JTable(m);
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

        p.add(new JScrollPane(table), BorderLayout.CENTER);
        p.add(btns, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> { showForm(type, -1); loadAll(); });
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(p, "Select a row first."); return; }
            int id = (int) m.getValueAt(table.convertRowIndexToModel(row), 0);
            showForm(type, id); loadAll();
        });
        deleteBtn.addActionListener(e -> { deleteRow(type, table, m); loadAll(); });
        refreshBtn.addActionListener(e -> loadAll());

        return p;
    }

    private void showForm(String type, int id) {
        Window win = SwingUtilities.getWindowAncestor(this);
        JDialog dlg;
        switch (type) {
            case "assign":   dlg = new AssignDialog(win, id); break;
            case "cabin":    dlg = new CrewCabinDialog(win, id); break;
            default:         dlg = new ScheduleDialog(win, id); break;
        }
        dlg.setVisible(true);
    }

    private void deleteRow(String type, JTable table, DefaultTableModel m) {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a row first."); return; }
        int id = (int) m.getValueAt(table.convertRowIndexToModel(row), 0);
        if (JOptionPane.showConfirmDialog(this, "Delete this record?", "Confirm",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        String sql;
        switch (type) {
            case "assign":   sql = "DELETE FROM ShipCrew WHERE AssignmentID=?"; break;
            case "cabin":    sql = "DELETE FROM CrewCabin WHERE CrewCabinID=?"; break;
            default:         sql = "DELETE FROM WorkSchedule WHERE ScheduleID=?"; break;
        }
        try (PreparedStatement ps = DBConnection.get().prepareStatement(sql)) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { showError(e); }
    }

    private void showError(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    private static DefaultTableModel makeModel(String... cols) {
        return new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private JComboBox<String[]> empCombo() {
        JComboBox<String[]> cb = new JComboBox<>();
        cb.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT EmployeeID, CONCAT(FirstName,' ',LastName) FROM Employee ORDER BY LastName")) {
            while (rs.next()) cb.addItem(new String[]{rs.getString(1), rs.getString(2)});
        } catch (SQLException ignored) {}
        return cb;
    }

    private JComboBox<String[]> shipCombo() {
        JComboBox<String[]> cb = new JComboBox<>();
        cb.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT ShipID, ShipName FROM Ship ORDER BY ShipName")) {
            while (rs.next()) cb.addItem(new String[]{rs.getString(1), rs.getString(2)});
        } catch (SQLException ignored) {}
        return cb;
    }

    private JComboBox<String[]> cabinCombo() {
        JComboBox<String[]> cb = new JComboBox<>();
        cb.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT c.CabinID, CONCAT(c.CabinNumber,' (',c.CabinType,') - ',s.ShipName) " +
                "FROM Cabin c JOIN Ship s ON c.ShipID=s.ShipID ORDER BY s.ShipName, c.CabinNumber")) {
            while (rs.next()) cb.addItem(new String[]{rs.getString(1), rs.getString(2)});
        } catch (SQLException ignored) {}
        return cb;
    }

    // ── Ship Assignment Dialog ────────────────────────────────────────────────

    class AssignDialog extends JDialog {
        private final JComboBox<String[]> ec = empCombo();
        private final JComboBox<String[]> sc = shipCombo();
        private final JTextField startField = new JTextField(12);
        private final JTextField endField   = new JTextField(12);
        private final int assignId;

        AssignDialog(Window parent, int assignId) {
            super(parent, assignId < 0 ? "Add Ship Assignment" : "Edit Assignment", ModalityType.APPLICATION_MODAL);
            this.assignId = assignId;
            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Employee:",               ec);
            ShipsPanel.addRow(form, 1, "Ship:",                   sc);
            ShipsPanel.addRow(form, 2, "Start Date (YYYY-MM-DD):", startField);
            ShipsPanel.addRow(form, 3, "End Date (YYYY-MM-DD):",   endField);
            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 4, save, cancel);
            if (assignId > 0) loadExisting();
            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);
            add(form); pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void loadExisting() {
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT EmployeeID, ShipID, StartDate, EndDate FROM ShipCrew WHERE AssignmentID=?")) {
                ps.setInt(1, assignId); ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    selectCombo(ec, rs.getString(1));
                    selectCombo(sc, rs.getString(2));
                    startField.setText(rs.getString(3));
                    endField.setText(rs.getString(4) != null ? rs.getString(4) : "");
                }
            } catch (SQLException ignored) {}
        }

        private void save() {
            if (ec.getSelectedItem() == null || sc.getSelectedItem() == null || startField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Employee, ship, and start date are required."); return;
            }
            int empId  = Integer.parseInt(((String[]) ec.getSelectedItem())[0]);
            int shipId = Integer.parseInt(((String[]) sc.getSelectedItem())[0]);
            String start = startField.getText().trim();
            String end   = endField.getText().trim();
            try {
                if (assignId < 0) {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "INSERT INTO ShipCrew (EmployeeID, ShipID, StartDate, EndDate) VALUES (?,?,?,?)")) {
                        ps.setInt(1, empId); ps.setInt(2, shipId); ps.setString(3, start);
                        ps.setString(4, end.isEmpty() ? null : end); ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "UPDATE ShipCrew SET EmployeeID=?, ShipID=?, StartDate=?, EndDate=? WHERE AssignmentID=?")) {
                        ps.setInt(1, empId); ps.setInt(2, shipId); ps.setString(3, start);
                        ps.setString(4, end.isEmpty() ? null : end); ps.setInt(5, assignId); ps.executeUpdate();
                    }
                }
                dispose();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Crew Cabin Dialog ─────────────────────────────────────────────────────

    class CrewCabinDialog extends JDialog {
        private final JComboBox<String[]> ec = empCombo();
        private final JComboBox<String[]> cc = cabinCombo();
        private final JTextField dateField = new JTextField(12);
        private final int crewCabinId;

        CrewCabinDialog(Window parent, int crewCabinId) {
            super(parent, crewCabinId < 0 ? "Add Crew Cabin" : "Edit Crew Cabin", ModalityType.APPLICATION_MODAL);
            this.crewCabinId = crewCabinId;
            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Employee:",                 ec);
            ShipsPanel.addRow(form, 1, "Cabin:",                    cc);
            ShipsPanel.addRow(form, 2, "Assigned Date (YYYY-MM-DD):", dateField);
            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 3, save, cancel);
            if (crewCabinId > 0) {
                try (PreparedStatement ps = DBConnection.get().prepareStatement(
                        "SELECT EmployeeID, CabinID, AssignedDate FROM CrewCabin WHERE CrewCabinID=?")) {
                    ps.setInt(1, crewCabinId); ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        selectCombo(ec, rs.getString(1));
                        selectCombo(cc, rs.getString(2));
                        dateField.setText(rs.getString(3) != null ? rs.getString(3) : "");
                    }
                } catch (SQLException ignored) {}
            }
            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);
            add(form); pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void save() {
            if (ec.getSelectedItem() == null || cc.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Employee and cabin are required."); return;
            }
            int empId   = Integer.parseInt(((String[]) ec.getSelectedItem())[0]);
            int cabinId = Integer.parseInt(((String[]) cc.getSelectedItem())[0]);
            String date = dateField.getText().trim();
            try {
                if (crewCabinId < 0) {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "INSERT INTO CrewCabin (EmployeeID, CabinID, AssignedDate) VALUES (?,?,?)")) {
                        ps.setInt(1, empId); ps.setInt(2, cabinId);
                        ps.setString(3, date.isEmpty() ? null : date); ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "UPDATE CrewCabin SET EmployeeID=?, CabinID=?, AssignedDate=? WHERE CrewCabinID=?")) {
                        ps.setInt(1, empId); ps.setInt(2, cabinId);
                        ps.setString(3, date.isEmpty() ? null : date); ps.setInt(4, crewCabinId); ps.executeUpdate();
                    }
                }
                dispose();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Work Schedule Dialog ──────────────────────────────────────────────────

    class ScheduleDialog extends JDialog {
        private final JComboBox<String[]> ec = empCombo();
        private final JComboBox<String[]> sc = shipCombo();
        private final JTextField dateField  = new JTextField(12);
        private final JTextField startField = new JTextField(8);
        private final JTextField endField   = new JTextField(8);
        private final int scheduleId;

        ScheduleDialog(Window parent, int scheduleId) {
            super(parent, scheduleId < 0 ? "Add Work Schedule" : "Edit Work Schedule", ModalityType.APPLICATION_MODAL);
            this.scheduleId = scheduleId;
            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Employee:",               ec);
            ShipsPanel.addRow(form, 1, "Ship:",                   sc);
            ShipsPanel.addRow(form, 2, "Work Date (YYYY-MM-DD):", dateField);
            ShipsPanel.addRow(form, 3, "Shift Start (HH:MM):",    startField);
            ShipsPanel.addRow(form, 4, "Shift End (HH:MM):",      endField);
            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 5, save, cancel);
            if (scheduleId > 0) {
                try (PreparedStatement ps = DBConnection.get().prepareStatement(
                        "SELECT EmployeeID, ShipID, WorkDate, ShiftStart, ShiftEnd FROM WorkSchedule WHERE ScheduleID=?")) {
                    ps.setInt(1, scheduleId); ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        selectCombo(ec, rs.getString(1));
                        selectCombo(sc, rs.getString(2));
                        dateField.setText(rs.getString(3));
                        startField.setText(rs.getString(4) != null ? rs.getString(4) : "");
                        endField.setText(rs.getString(5) != null ? rs.getString(5) : "");
                    }
                } catch (SQLException ignored) {}
            }
            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);
            add(form); pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void save() {
            if (ec.getSelectedItem() == null || sc.getSelectedItem() == null || dateField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Employee, ship, and date are required."); return;
            }
            int empId  = Integer.parseInt(((String[]) ec.getSelectedItem())[0]);
            int shipId = Integer.parseInt(((String[]) sc.getSelectedItem())[0]);
            String date  = dateField.getText().trim();
            String start = startField.getText().trim();
            String end   = endField.getText().trim();
            try {
                if (scheduleId < 0) {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "INSERT INTO WorkSchedule (EmployeeID, ShipID, WorkDate, ShiftStart, ShiftEnd) VALUES (?,?,?,?,?)")) {
                        ps.setInt(1, empId); ps.setInt(2, shipId); ps.setString(3, date);
                        ps.setString(4, start.isEmpty() ? null : start);
                        ps.setString(5, end.isEmpty() ? null : end);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "UPDATE WorkSchedule SET EmployeeID=?, ShipID=?, WorkDate=?, ShiftStart=?, ShiftEnd=? WHERE ScheduleID=?")) {
                        ps.setInt(1, empId); ps.setInt(2, shipId); ps.setString(3, date);
                        ps.setString(4, start.isEmpty() ? null : start);
                        ps.setString(5, end.isEmpty() ? null : end);
                        ps.setInt(6, scheduleId); ps.executeUpdate();
                    }
                }
                dispose();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private static void selectCombo(JComboBox<String[]> cb, String id) {
        for (int i = 0; i < cb.getItemCount(); i++)
            if (cb.getItemAt(i)[0].equals(id)) { cb.setSelectedIndex(i); return; }
    }
}

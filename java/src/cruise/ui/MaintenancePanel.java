package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class MaintenancePanel extends JPanel {

    private final DefaultTableModel model;
    private final JTable table;

    public MaintenancePanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"ID", "Task", "Ship", "Date", "Technician", "Notes"};
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(22);
        table.getColumnModel().getColumn(5).setPreferredWidth(200);

        JLabel heading = new JLabel("Maintenance Records");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));

        JButton addBtn     = new JButton("Add Record");
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
            MaintenanceDialog d = new MaintenanceDialog(SwingUtilities.getWindowAncestor(this));
            d.setVisible(true); if (d.saved) loadData();
        });
        refreshBtn.addActionListener(e -> loadData());
    }

    private void loadData() {
        model.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT mr.RecordID, m.MaintenanceName, s.ShipName, mr.MaintenanceDate, " +
                "       CONCAT(e.FirstName,' ',e.LastName) AS Technician, mr.Notes " +
                "FROM MaintenanceRecord mr " +
                "JOIN Maintenance m ON mr.MaintenanceID = m.MaintenanceID " +
                "JOIN Ship s        ON m.ShipID          = s.ShipID " +
                "JOIN Employee e    ON mr.EmployeeID     = e.EmployeeID " +
                "ORDER BY mr.MaintenanceDate DESC")) {
            while (rs.next())
                model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6)});
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    static class MaintenanceDialog extends JDialog {
        boolean saved = false;
        private final JComboBox<String[]> taskCombo = new JComboBox<>();
        private final JComboBox<String[]> techCombo = new JComboBox<>();
        private final JTextField dateField  = new JTextField(12);
        private final JTextArea  notesArea  = new JTextArea(3, 25);

        MaintenanceDialog(Window parent) {
            super(parent, "Add Maintenance Record", ModalityType.APPLICATION_MODAL);
            buildUI(); pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void buildUI() {
            loadCombo(taskCombo, "SELECT MaintenanceID, MaintenanceName FROM Maintenance ORDER BY MaintenanceName");
            loadCombo(techCombo,
                "SELECT EmployeeID, CONCAT(FirstName,' ',LastName) FROM Employee ORDER BY LastName");
            for (JComboBox<String[]> cb : new JComboBox[]{taskCombo, techCombo})
                cb.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));

            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Maintenance Task:",   taskCombo);
            ShipsPanel.addRow(form, 1, "Technician:",         techCombo);
            ShipsPanel.addRow(form, 2, "Date (YYYY-MM-DD):",  dateField);
            notesArea.setLineWrap(true); notesArea.setWrapStyleWord(true);
            ShipsPanel.addRow(form, 3, "Notes:", new JScrollPane(notesArea));

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
            String date = dateField.getText().trim();
            if (date.isEmpty() || taskCombo.getSelectedItem() == null || techCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Task, technician, and date are required."); return;
            }
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT INTO MaintenanceRecord (MaintenanceID, MaintenanceDate, EmployeeID, Notes) VALUES (?,?,?,?)")) {
                ps.setInt(1, Integer.parseInt(((String[]) taskCombo.getSelectedItem())[0]));
                ps.setString(2, date);
                ps.setInt(3, Integer.parseInt(((String[]) techCombo.getSelectedItem())[0]));
                ps.setString(4, notesArea.getText().trim());
                ps.executeUpdate(); saved = true; dispose();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

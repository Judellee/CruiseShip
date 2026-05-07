package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class SafetyPanel extends JPanel {

    // ── Emergency Drills ──────────────────────────────────────────────────────
    private final DefaultTableModel drillModel =
            tModel("Drill ID", "Date", "Type", "Ship");
    private final JTable drillTable = makeTable(drillModel);

    // ── Ports ─────────────────────────────────────────────────────────────────
    private final DefaultTableModel portModel =
            tModel("Port ID", "Port Name", "Country");
    private final JTable portTable = makeTable(portModel);

    // ── Itineraries ───────────────────────────────────────────────────────────
    private final DefaultTableModel itinModel =
            tModel("ID", "Itinerary Name");
    private final JTable itinTable = makeTable(itinModel);

    private final DefaultTableModel stopModel =
            tModel("Stop ID", "Order", "Port", "Country");
    private final JTable stopTable = makeTable(stopModel);

    public SafetyPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("  Emergency Drills  ", buildDrillsTab());
        tabs.addTab("  Ports  ",            buildPortsTab());
        tabs.addTab("  Itineraries  ",      buildItinerariesTab());

        add(tabs, BorderLayout.CENTER);
        loadAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Emergency Drills tab
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildDrillsTab() {
        JButton addBtn  = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton delBtn  = new JButton("Delete");

        addBtn.addActionListener(e -> {
            new DrillDialog(SwingUtilities.getWindowAncestor(this), -1, null, null, -1)
                    .setVisible(true);
            loadDrills();
        });
        editBtn.addActionListener(e -> editDrill());
        delBtn.addActionListener(e -> deleteRow(drillTable, drillModel,
                "DELETE FROM EmergencyDrill WHERE DrillID=?", "drill"));

        return tabPanel(drillTable, addBtn, editBtn, delBtn);
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

    // ─────────────────────────────────────────────────────────────────────────
    // Ports tab
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildPortsTab() {
        JButton addBtn  = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton delBtn  = new JButton("Delete");

        addBtn.addActionListener(e -> {
            new PortDialog(SwingUtilities.getWindowAncestor(this), -1, null, null).setVisible(true);
            loadPorts();
        });
        editBtn.addActionListener(e -> editPort());
        delBtn.addActionListener(e -> deleteRow(portTable, portModel,
                "DELETE FROM Port WHERE PortID=?", "port"));

        return tabPanel(portTable, addBtn, editBtn, delBtn);
    }

    private void editPort() {
        int row = portTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a port first."); return; }
        int    id      = (int)    portModel.getValueAt(row, 0);
        String name    = (String) portModel.getValueAt(row, 1);
        String country = (String) portModel.getValueAt(row, 2);
        new PortDialog(SwingUtilities.getWindowAncestor(this), id, name, country).setVisible(true);
        loadPorts();
    }

    private void loadPorts() {
        portModel.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT PortID, PortName, Country FROM Port ORDER BY Country, PortName")) {
            while (rs.next())
                portModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3)});
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Itineraries & Stops tab
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildItinerariesTab() {
        // Left: itinerary list with add/delete
        JButton addItinBtn = new JButton("Add Itinerary");
        JButton delItinBtn = new JButton("Delete Itinerary");

        addItinBtn.addActionListener(e -> addItinerary());
        delItinBtn.addActionListener(e ->
            deleteRow(itinTable, itinModel,
                    "DELETE FROM Itinerary WHERE ItineraryID=?", "itinerary"));

        JPanel itinBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        itinBtnRow.add(addItinBtn);
        itinBtnRow.add(delItinBtn);

        JPanel itinPanel = new JPanel(new BorderLayout(4, 4));
        itinPanel.setBorder(BorderFactory.createTitledBorder("Itineraries"));
        itinPanel.add(itinBtnRow,              BorderLayout.NORTH);
        itinPanel.add(new JScrollPane(itinTable), BorderLayout.CENTER);

        // Right: stops for selected itinerary
        JButton addStopBtn = new JButton("Add Stop");
        JButton delStopBtn = new JButton("Delete Stop");

        addStopBtn.addActionListener(e -> addStop());
        delStopBtn.addActionListener(e -> deleteRow(stopTable, stopModel,
                "DELETE FROM Stop WHERE StopID=?", "stop"));

        JPanel stopBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        stopBtnRow.add(addStopBtn);
        stopBtnRow.add(delStopBtn);

        JPanel stopPanel = new JPanel(new BorderLayout(4, 4));
        stopPanel.setBorder(BorderFactory.createTitledBorder("Stops (select an itinerary)"));
        stopPanel.add(stopBtnRow,               BorderLayout.NORTH);
        stopPanel.add(new JScrollPane(stopTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, itinPanel, stopPanel);
        split.setResizeWeight(0.4);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        wrapper.add(split, BorderLayout.CENTER);

        itinTable.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) loadStops();
        });

        return wrapper;
    }

    private void addItinerary() {
        String name = JOptionPane.showInputDialog(this, "Itinerary Name:");
        if (name == null || name.trim().isEmpty()) return;
        try (PreparedStatement ps = DBConnection.get().prepareStatement(
                "INSERT INTO Itinerary (ItineraryName) VALUES (?)")) {
            ps.setString(1, name.trim());
            ps.executeUpdate();
            loadItineraries();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addStop() {
        int itinRow = itinTable.getSelectedRow();
        if (itinRow < 0) { JOptionPane.showMessageDialog(this, "Select an itinerary first."); return; }
        int itinId = (int) itinModel.getValueAt(itinRow, 0);

        JComboBox<String[]> portC = portCombo();
        JTextField orderField = new JTextField(4);

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 6));
        form.add(new JLabel("Stop Order:")); form.add(orderField);
        form.add(new JLabel("Port:"));       form.add(portC);

        int opt = JOptionPane.showConfirmDialog(this, form, "Add Stop",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION || portC.getSelectedItem() == null) return;

        String orderStr = orderField.getText().trim();
        if (orderStr.isEmpty()) { JOptionPane.showMessageDialog(this, "Stop order is required."); return; }

        try {
            int portId = Integer.parseInt(((String[]) portC.getSelectedItem())[0]);
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT INTO Stop (StopOrder, ItineraryID, PortID) VALUES (?,?,?)")) {
                ps.setInt(1, Integer.parseInt(orderStr));
                ps.setInt(2, itinId);
                ps.setInt(3, portId);
                ps.executeUpdate();
                loadStops();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Stop order must be a number.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadItineraries() {
        itinModel.setRowCount(0);
        stopModel.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT ItineraryID, ItineraryName FROM Itinerary ORDER BY ItineraryName")) {
            while (rs.next())
                itinModel.addRow(new Object[]{rs.getInt(1), rs.getString(2)});
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadStops() {
        stopModel.setRowCount(0);
        int row = itinTable.getSelectedRow();
        if (row < 0) return;
        int itinId = (int) itinModel.getValueAt(row, 0);
        try (PreparedStatement ps = DBConnection.get().prepareStatement(
                "SELECT s.StopID, s.StopOrder, p.PortName, p.Country " +
                "FROM Stop s JOIN Port p ON s.PortID = p.PortID " +
                "WHERE s.ItineraryID=? ORDER BY s.StopOrder")) {
            ps.setInt(1, itinId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                stopModel.addRow(new Object[]{
                        rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4)});
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void loadAll() {
        loadDrills();
        loadPorts();
        loadItineraries();
    }

    private void deleteRow(JTable tbl, DefaultTableModel mdl, String sql, String label) {
        int row = tbl.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a " + label + " first."); return; }
        int id = (int) mdl.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete this " + label + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try (PreparedStatement ps = DBConnection.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            loadAll();
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

    private static JComboBox<String[]> portCombo() {
        JComboBox<String[]> cb = new JComboBox<>();
        cb.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT PortID, CONCAT(PortName, ', ', Country) FROM Port ORDER BY PortName")) {
            while (rs.next()) cb.addItem(new String[]{rs.getString(1), rs.getString(2)});
        } catch (SQLException ignored) {}
        return cb;
    }

    private static void selectCombo(JComboBox<String[]> cb, int id) {
        for (int i = 0; i < cb.getItemCount(); i++) {
            if (Integer.parseInt(cb.getItemAt(i)[0]) == id) { cb.setSelectedIndex(i); return; }
        }
    }

    private static JPanel tabPanel(JTable tbl, JButton... buttons) {
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        for (JButton b : buttons) btnRow.add(b);
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        p.add(btnRow,                BorderLayout.NORTH);
        p.add(new JScrollPane(tbl),  BorderLayout.CENTER);
        return p;
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

    // ─────────────────────────────────────────────────────────────────────────
    // Dialogs
    // ─────────────────────────────────────────────────────────────────────────

    private class DrillDialog extends JDialog {
        private final int editId;
        private final JComboBox<String[]> shipC = shipCombo();
        private final JTextField dateField = new JTextField(12);
        private final JComboBox<String> typeCombo = new JComboBox<>(
                new String[]{"Fire", "Lifeboat", "Man Overboard", "Abandon Ship", "Medical", "Other"});

        DrillDialog(Window owner, int editId, String date, String type, int shipId) {
            super(owner, editId < 0 ? "Add Drill" : "Edit Drill", ModalityType.APPLICATION_MODAL);
            this.editId = editId;

            if (date != null) dateField.setText(date);
            if (type != null) typeCombo.setSelectedItem(type);
            if (shipId > 0)  selectCombo(shipC, shipId);

            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Ship", shipC);
            ShipsPanel.addRow(form, 1, "Drill Date (YYYY-MM-DD)", dateField);
            ShipsPanel.addRow(form, 2, "Drill Type", typeCombo);

            JButton save   = new JButton("Save");
            JButton cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 3, save, cancel);

            save.addActionListener(e -> save());
            cancel.addActionListener(e -> dispose());

            setContentPane(form);
            pack();
            setLocationRelativeTo(owner);
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

    private class PortDialog extends JDialog {
        private final int editId;
        private final JTextField nameField    = new JTextField(20);
        private final JTextField countryField = new JTextField(20);

        PortDialog(Window owner, int editId, String name, String country) {
            super(owner, editId < 0 ? "Add Port" : "Edit Port", ModalityType.APPLICATION_MODAL);
            this.editId = editId;

            if (name    != null) nameField.setText(name);
            if (country != null) countryField.setText(country);

            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Port Name", nameField);
            ShipsPanel.addRow(form, 1, "Country",   countryField);

            JButton save   = new JButton("Save");
            JButton cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 2, save, cancel);

            save.addActionListener(e -> save());
            cancel.addActionListener(e -> dispose());

            setContentPane(form);
            pack();
            setLocationRelativeTo(owner);
        }

        private void save() {
            String name    = nameField.getText().trim();
            String country = countryField.getText().trim();
            if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "Port name is required."); return; }
            try {
                if (editId < 0) {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "INSERT INTO Port (PortName, Country) VALUES (?,?)")) {
                        ps.setString(1, name);
                        ps.setString(2, country.isEmpty() ? null : country);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "UPDATE Port SET PortName=?, Country=? WHERE PortID=?")) {
                        ps.setString(1, name);
                        ps.setString(2, country.isEmpty() ? null : country);
                        ps.setInt(3, editId);
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

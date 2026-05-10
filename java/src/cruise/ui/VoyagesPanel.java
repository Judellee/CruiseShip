package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class VoyagesPanel extends JPanel {

    // ── Voyages ───────────────────────────────────────────────────────────────
    private final DefaultTableModel voyageModel =
            tModel("ID", "Ship", "Itinerary", "Season", "Departure", "Return");
    private final JTable voyageTable = makeTable(voyageModel);

    // ── Itineraries & Stops ───────────────────────────────────────────────────
    private final DefaultTableModel itinModel = tModel("ID", "Itinerary Name");
    private final JTable itinTable = makeTable(itinModel);
    private final DefaultTableModel stopModel = tModel("Stop ID", "Order", "Port", "Country");
    private final JTable stopTable = makeTable(stopModel);

    // ── Ports ─────────────────────────────────────────────────────────────────
    private final DefaultTableModel portModel = tModel("Port ID", "Port Name", "Country");
    private final JTable portTable = makeTable(portModel);

    // ── Excursions ────────────────────────────────────────────────────────────
    private final DefaultTableModel excursionModel =
            tModel("ID", "Name", "Price", "Port", "Country", "Season");
    private final JTable excursionTable = makeTable(excursionModel);

    public VoyagesPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("  Voyages  ",             buildVoyagesTab());
        tabs.addTab("  Itineraries & Stops  ", buildItinerariesTab());
        tabs.addTab("  Ports  ",               buildPortsTab());
        tabs.addTab("  Excursions  ",          buildExcursionsTab());

        add(tabs, BorderLayout.CENTER);
        loadAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Voyages tab
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildVoyagesTab() {
        voyageTable.setAutoCreateRowSorter(true);
        voyageTable.getTableHeader().setReorderingAllowed(false);

        JButton addBtn    = new JButton("Add");
        JButton deleteBtn = new JButton("Delete");
        JButton refreshBtn = new JButton("Refresh");

        addBtn.addActionListener(e -> {
            VoyageDialog d = new VoyageDialog(SwingUtilities.getWindowAncestor(this));
            d.setVisible(true);
            if (d.saved) loadVoyages();
        });
        deleteBtn.addActionListener(e -> {
            int row = voyageTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Select a voyage first."); return; }
            int id = (int) voyageModel.getValueAt(voyageTable.convertRowIndexToModel(row), 0);
            if (JOptionPane.showConfirmDialog(this, "Delete voyage #" + id + "?", "Confirm",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "DELETE FROM Voyage WHERE VoyageID=?")) {
                ps.setInt(1, id); ps.executeUpdate(); loadVoyages();
            } catch (SQLException ex) { showError(ex); }
        });
        refreshBtn.addActionListener(e -> loadVoyages());

        return tabPanel(voyageTable, addBtn, deleteBtn, refreshBtn);
    }

    private void loadVoyages() {
        voyageModel.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT v.VoyageID, s.ShipName, i.ItineraryName, se.SeasonName, " +
                "       v.DepartureDate, v.ReturnDate " +
                "FROM Voyage v " +
                "JOIN Ship s      ON v.ShipID      = s.ShipID " +
                "JOIN Itinerary i ON v.ItineraryID = i.ItineraryID " +
                "JOIN Season se   ON v.SeasonID    = se.SeasonID " +
                "ORDER BY v.DepartureDate")) {
            while (rs.next())
                voyageModel.addRow(new Object[]{rs.getInt(1), rs.getString(2),
                        rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)});
        } catch (SQLException e) { showError(e); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Itineraries & Stops tab
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildItinerariesTab() {
        JButton addItinBtn = new JButton("Add Itinerary");
        JButton delItinBtn = new JButton("Delete Itinerary");

        addItinBtn.addActionListener(e -> addItinerary());
        delItinBtn.addActionListener(e -> deleteRow(itinTable, itinModel,
                "DELETE FROM Itinerary WHERE ItineraryID=?", "itinerary"));

        JPanel itinBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        itinBtnRow.add(addItinBtn);
        itinBtnRow.add(delItinBtn);

        JPanel itinPanel = new JPanel(new BorderLayout(4, 4));
        itinPanel.setBorder(BorderFactory.createTitledBorder("Itineraries"));
        itinPanel.add(itinBtnRow,                 BorderLayout.NORTH);
        itinPanel.add(new JScrollPane(itinTable), BorderLayout.CENTER);

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
        stopPanel.add(stopBtnRow,                 BorderLayout.NORTH);
        stopPanel.add(new JScrollPane(stopTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, itinPanel, stopPanel);
        split.setResizeWeight(0.4);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        wrapper.add(split, BorderLayout.CENTER);

        itinTable.getSelectionModel().addListSelectionListener(
                (ListSelectionEvent e) -> { if (!e.getValueIsAdjusting()) loadStops(); });

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
        } catch (SQLException e) { showError(e); }
    }

    private void addStop() {
        int itinRow = itinTable.getSelectedRow();
        if (itinRow < 0) { JOptionPane.showMessageDialog(this, "Select an itinerary first."); return; }
        int itinId = (int) itinModel.getValueAt(itinRow, 0);

        JComboBox<String[]> portC = buildPortCombo();
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
        } catch (SQLException ex) { showError(ex); }
    }

    private void loadItineraries() {
        itinModel.setRowCount(0);
        stopModel.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT ItineraryID, ItineraryName FROM Itinerary ORDER BY ItineraryName")) {
            while (rs.next())
                itinModel.addRow(new Object[]{rs.getInt(1), rs.getString(2)});
        } catch (SQLException e) { showError(e); }
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
        } catch (SQLException e) { showError(e); }
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
        } catch (SQLException e) { showError(e); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Excursions tab
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildExcursionsTab() {
        JButton addBtn  = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton delBtn  = new JButton("Delete");

        addBtn.addActionListener(e -> {
            new ExcursionDialog(SwingUtilities.getWindowAncestor(this), -1, null, null, -1, -1)
                    .setVisible(true);
            loadExcursions();
        });
        editBtn.addActionListener(e -> {
            int row = excursionTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Select an excursion first."); return; }
            int    id       = (int)    excursionModel.getValueAt(row, 0);
            String name     = (String) excursionModel.getValueAt(row, 1);
            Object price    = excursionModel.getValueAt(row, 2);
            int    portId   = portIdByName((String) excursionModel.getValueAt(row, 3));
            int    seasonId = seasonIdByName((String) excursionModel.getValueAt(row, 5));
            new ExcursionDialog(SwingUtilities.getWindowAncestor(this), id, name,
                    price == null ? null : price.toString(), portId, seasonId).setVisible(true);
            loadExcursions();
        });
        delBtn.addActionListener(e -> deleteRow(excursionTable, excursionModel,
                "DELETE FROM Excursion WHERE ExcursionID=?", "excursion"));

        return tabPanel(excursionTable, addBtn, editBtn, delBtn);
    }

    private void loadExcursions() {
        excursionModel.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT e.ExcursionID, e.ExcursionName, e.Price, p.PortName, p.Country, " +
                "       se.SeasonName " +
                "FROM Excursion e " +
                "JOIN Port p ON e.PortID = p.PortID " +
                "LEFT JOIN Season se ON e.SeasonID = se.SeasonID " +
                "ORDER BY p.PortName, e.ExcursionName")) {
            while (rs.next())
                excursionModel.addRow(new Object[]{
                        rs.getInt(1), rs.getString(2), rs.getObject(3),
                        rs.getString(4), rs.getString(5),
                        rs.getString(6) != null ? rs.getString(6) : "All"});
        } catch (SQLException e) { showError(e); }
    }

    private int portIdByName(String portName) {
        try (PreparedStatement ps = DBConnection.get().prepareStatement(
                "SELECT PortID FROM Port WHERE PortName=? LIMIT 1")) {
            ps.setString(1, portName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ignored) {}
        return -1;
    }

    private int seasonIdByName(String seasonName) {
        if (seasonName == null || seasonName.equals("All")) return -1;
        try (PreparedStatement ps = DBConnection.get().prepareStatement(
                "SELECT SeasonID FROM Season WHERE SeasonName=? LIMIT 1")) {
            ps.setString(1, seasonName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ignored) {}
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void loadAll() {
        loadVoyages();
        loadItineraries();
        loadPorts();
        loadExcursions();
    }

    private void deleteRow(JTable tbl, DefaultTableModel mdl, String sql, String label) {
        int row = tbl.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a " + label + " first."); return; }
        int id = (int) mdl.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this, "Delete this " + label + "?", "Confirm",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (PreparedStatement ps = DBConnection.get().prepareStatement(sql)) {
            ps.setInt(1, id); ps.executeUpdate(); loadAll();
        } catch (SQLException e) { showError(e); }
    }

    private void showError(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    private static JComboBox<String[]> buildPortCombo() {
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
        for (int i = 0; i < cb.getItemCount(); i++)
            if (Integer.parseInt(cb.getItemAt(i)[0]) == id) { cb.setSelectedIndex(i); return; }
    }

    private static JPanel tabPanel(JTable tbl, JButton... buttons) {
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        for (JButton b : buttons) btnRow.add(b);
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        p.add(btnRow,               BorderLayout.NORTH);
        p.add(new JScrollPane(tbl), BorderLayout.CENTER);
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

    static class VoyageDialog extends JDialog {
        boolean saved = false;
        private final JComboBox<String[]> shipCombo   = new JComboBox<>();
        private final JComboBox<String[]> itinCombo   = new JComboBox<>();
        private final JComboBox<String[]> seasonCombo = new JComboBox<>();
        private final JTextField departureField = new JTextField(12);
        private final JTextField returnField    = new JTextField(12);

        VoyageDialog(Window parent) {
            super(parent, "Add Voyage", ModalityType.APPLICATION_MODAL);
            buildUI(); pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void buildUI() {
            loadCombo(shipCombo,   "SELECT ShipID, ShipName FROM Ship ORDER BY ShipName");
            loadCombo(itinCombo,   "SELECT ItineraryID, ItineraryName FROM Itinerary ORDER BY ItineraryName");
            loadCombo(seasonCombo, "SELECT SeasonID, SeasonName FROM Season ORDER BY SeasonName");
            for (JComboBox<String[]> cb : new JComboBox[]{shipCombo, itinCombo, seasonCombo})
                cb.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));

            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Ship:",                   shipCombo);
            ShipsPanel.addRow(form, 1, "Itinerary:",              itinCombo);
            ShipsPanel.addRow(form, 2, "Season:",                 seasonCombo);
            ShipsPanel.addRow(form, 3, "Departure (YYYY-MM-DD):", departureField);
            ShipsPanel.addRow(form, 4, "Return (YYYY-MM-DD):",    returnField);

            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 5, save, cancel);
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
            String dep = departureField.getText().trim(), ret = returnField.getText().trim();
            if (dep.isEmpty() || shipCombo.getSelectedItem() == null
                    || itinCombo.getSelectedItem() == null || seasonCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "All fields except Return date are required."); return;
            }
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT INTO Voyage (DepartureDate, ReturnDate, ShipID, ItineraryID, SeasonID) VALUES (?,?,?,?,?)")) {
                ps.setString(1, dep);
                ps.setString(2, ret.isEmpty() ? null : ret);
                ps.setInt(3, Integer.parseInt(((String[]) shipCombo.getSelectedItem())[0]));
                ps.setInt(4, Integer.parseInt(((String[]) itinCombo.getSelectedItem())[0]));
                ps.setInt(5, Integer.parseInt(((String[]) seasonCombo.getSelectedItem())[0]));
                ps.executeUpdate();
                saved = true; dispose();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 2, save, cancel);
            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            setContentPane(form); pack(); setLocationRelativeTo(owner);
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
            } catch (SQLException ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private class ExcursionDialog extends JDialog {
        private final int editId;
        private final JTextField nameField  = new JTextField(20);
        private final JTextField priceField = new JTextField(10);
        private final JComboBox<String[]> portC    = buildPortCombo();
        private final JComboBox<String[]> seasonC  = buildSeasonCombo();

        ExcursionDialog(Window owner, int editId, String name, String price, int portId, int seasonId) {
            super(owner, editId < 0 ? "Add Excursion" : "Edit Excursion",
                    ModalityType.APPLICATION_MODAL);
            this.editId = editId;
            if (name  != null) nameField.setText(name);
            if (price != null) priceField.setText(price);
            if (portId   > 0) selectCombo(portC,   portId);
            if (seasonId > 0) selectCombo(seasonC, seasonId);

            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Excursion Name",    nameField);
            ShipsPanel.addRow(form, 1, "Price",             priceField);
            ShipsPanel.addRow(form, 2, "Port",              portC);
            ShipsPanel.addRow(form, 3, "Season (optional)", seasonC);

            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 4, save, cancel);
            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            setContentPane(form); pack(); setLocationRelativeTo(owner);
        }

        private JComboBox<String[]> buildSeasonCombo() {
            JComboBox<String[]> cb = new JComboBox<>();
            cb.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));
            cb.addItem(new String[]{"-1", "All Seasons"});
            try (Statement st = DBConnection.get().createStatement();
                 ResultSet rs = st.executeQuery("SELECT SeasonID, SeasonName FROM Season ORDER BY SeasonName")) {
                while (rs.next()) cb.addItem(new String[]{rs.getString(1), rs.getString(2)});
            } catch (SQLException ignored) {}
            return cb;
        }

        private void save() {
            String name = nameField.getText().trim();
            if (name.isEmpty() || portC.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Name and port are required."); return;
            }
            int portId = Integer.parseInt(((String[]) portC.getSelectedItem())[0]);
            String priceStr = priceField.getText().trim();
            int seasonId = Integer.parseInt(((String[]) seasonC.getSelectedItem())[0]);
            try {
                if (editId < 0) {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "INSERT INTO Excursion (ExcursionName, Price, PortID, SeasonID) VALUES (?,?,?,?)")) {
                        ps.setString(1, name);
                        if (priceStr.isEmpty()) ps.setNull(2, Types.DECIMAL);
                        else ps.setDouble(2, Double.parseDouble(priceStr));
                        ps.setInt(3, portId);
                        if (seasonId < 0) ps.setNull(4, Types.INTEGER);
                        else ps.setInt(4, seasonId);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "UPDATE Excursion SET ExcursionName=?, Price=?, PortID=?, SeasonID=? WHERE ExcursionID=?")) {
                        ps.setString(1, name);
                        if (priceStr.isEmpty()) ps.setNull(2, Types.DECIMAL);
                        else ps.setDouble(2, Double.parseDouble(priceStr));
                        ps.setInt(3, portId);
                        if (seasonId < 0) ps.setNull(4, Types.INTEGER);
                        else ps.setInt(4, seasonId);
                        ps.setInt(5, editId);
                        ps.executeUpdate();
                    }
                }
                dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid price.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

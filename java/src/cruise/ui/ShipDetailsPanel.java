package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ShipDetailsPanel extends JPanel {

    private final JComboBox<String[]> shipFilter = new JComboBox<>();
    private final JTabbedPane innerTabs = new JTabbedPane();

    // Sub-models
    private final DefaultTableModel deckModel   = tableModel("ID", "Deck Number", "Ship");
    private final DefaultTableModel cabinModel  = tableModel("ID", "Cabin No.", "Type", "Deck", "Ship");
    private final DefaultTableModel diningModel = tableModel("ID", "Venue Name", "Capacity", "Ship");
    private final DefaultTableModel facilModel  = tableModel("ID", "Facility Name", "Ship");
    private final DefaultTableModel eventModel  = tableModel("ID", "Event Name", "Date/Time", "Venue", "Ship");

    public ShipDetailsPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel heading = new JLabel("Ship Details");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));

        // Ship filter
        shipFilter.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));
        loadShipFilter();
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filterPanel.add(new JLabel("Filter by Ship:"));
        filterPanel.add(shipFilter);
        JButton filterBtn = new JButton("Apply");
        filterPanel.add(filterBtn);

        JPanel top = new JPanel(new BorderLayout());
        top.add(heading,     BorderLayout.NORTH);
        top.add(filterPanel, BorderLayout.CENTER);

        // Sub-tabs
        innerTabs.addTab("  Decks  ",      buildSubPanel(deckModel,   "deck"));
        innerTabs.addTab("  Cabins  ",     buildSubPanel(cabinModel,  "cabin"));
        innerTabs.addTab("  Dining  ",     buildSubPanel(diningModel, "dining"));
        innerTabs.addTab("  Facilities  ", buildSubPanel(facilModel,  "facility"));
        innerTabs.addTab("  Events  ",     buildSubPanel(eventModel,  "event"));

        add(top,        BorderLayout.NORTH);
        add(innerTabs,  BorderLayout.CENTER);

        loadAll();
        filterBtn.addActionListener(e -> loadAll());
        shipFilter.addActionListener(e -> loadAll());
    }

    private void loadShipFilter() {
        shipFilter.removeAllItems();
        shipFilter.addItem(new String[]{"0", "All Ships"});
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT ShipID, ShipName FROM Ship ORDER BY ShipName")) {
            while (rs.next()) shipFilter.addItem(new String[]{rs.getString(1), rs.getString(2)});
        } catch (SQLException ignored) {}
    }

    private int selectedShipId() {
        String[] sel = (String[]) shipFilter.getSelectedItem();
        if (sel == null) return 0;
        return Integer.parseInt(sel[0]);
    }

    private void loadAll() {
        loadDecks(); loadCabins(); loadDining(); loadFacilities(); loadEvents();
    }

    private void loadDecks() {
        deckModel.setRowCount(0);
        String sql = "SELECT d.DeckID, d.DeckNumber, s.ShipName FROM Deck d " +
                     "JOIN Ship s ON d.ShipID=s.ShipID " +
                     (selectedShipId() > 0 ? "WHERE d.ShipID=" + selectedShipId() + " " : "") +
                     "ORDER BY s.ShipName, d.DeckNumber";
        runQuery(sql, deckModel);
    }

    private void loadCabins() {
        cabinModel.setRowCount(0);
        String sql = "SELECT c.CabinID, c.CabinNumber, c.CabinType, d.DeckNumber, s.ShipName " +
                     "FROM Cabin c JOIN Ship s ON c.ShipID=s.ShipID " +
                     "LEFT JOIN Deck d ON c.DeckID=d.DeckID " +
                     (selectedShipId() > 0 ? "WHERE c.ShipID=" + selectedShipId() + " " : "") +
                     "ORDER BY s.ShipName, c.CabinNumber";
        runQuery(sql, cabinModel);
    }

    private void loadDining() {
        diningModel.setRowCount(0);
        String sql = "SELECT dv.DiningVenueID, dv.VenueName, dv.Capacity, s.ShipName " +
                     "FROM DiningVenue dv JOIN Ship s ON dv.ShipID=s.ShipID " +
                     (selectedShipId() > 0 ? "WHERE dv.ShipID=" + selectedShipId() + " " : "") +
                     "ORDER BY s.ShipName, dv.VenueName";
        runQuery(sql, diningModel);
    }

    private void loadFacilities() {
        facilModel.setRowCount(0);
        String sql = "SELECT f.FacilityID, f.FacilityName, s.ShipName " +
                     "FROM Facility f JOIN Ship s ON f.ShipID=s.ShipID " +
                     (selectedShipId() > 0 ? "WHERE f.ShipID=" + selectedShipId() + " " : "") +
                     "ORDER BY s.ShipName, f.FacilityName";
        runQuery(sql, facilModel);
    }

    private void loadEvents() {
        eventModel.setRowCount(0);
        String sql = "SELECT e.EventID, e.EventName, DATE_FORMAT(e.EventDateTime, '%Y-%m-%d %H:%i'), e.Venue, s.ShipName " +
                     "FROM EntertainmentEvent e JOIN Ship s ON e.ShipID=s.ShipID " +
                     (selectedShipId() > 0 ? "WHERE e.ShipID=" + selectedShipId() + " " : "") +
                     "ORDER BY e.EventDateTime";
        runQuery(sql, eventModel);
    }

    private void runQuery(String sql, DefaultTableModel m) {
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            int cols = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Object[] row = new Object[cols];
                for (int i = 0; i < cols; i++) row[i] = rs.getObject(i + 1);
                m.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
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

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btns.add(addBtn); btns.add(editBtn); btns.add(deleteBtn);

        p.add(new JScrollPane(table), BorderLayout.CENTER);
        p.add(btns, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> showForm(type, -1, table, m));
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(p, "Select a row first."); return; }
            int id = (int) m.getValueAt(table.convertRowIndexToModel(row), 0);
            showForm(type, id, table, m);
        });
        deleteBtn.addActionListener(e -> deleteRow(type, table, m));

        return p;
    }

    private void showForm(String type, int id, JTable table, DefaultTableModel m) {
        Window win = SwingUtilities.getWindowAncestor(this);
        JDialog dlg;
        switch (type) {
            case "deck":    dlg = new DeckDialog(win, id); break;
            case "cabin":   dlg = new CabinDialog(win, id); break;
            case "dining":  dlg = new DiningDialog(win, id); break;
            case "facility":dlg = new FacilityDialog(win, id); break;
            default:        dlg = new EventDialog(win, id); break;
        }
        dlg.setVisible(true);
        loadAll();
    }

    private void deleteRow(String type, JTable table, DefaultTableModel m) {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a row first."); return; }
        int id = (int) m.getValueAt(table.convertRowIndexToModel(row), 0);
        if (JOptionPane.showConfirmDialog(this, "Delete this record?", "Confirm",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        String sql;
        switch (type) {
            case "deck":     sql = "DELETE FROM Deck WHERE DeckID=?"; break;
            case "cabin":    sql = "DELETE FROM Cabin WHERE CabinID=?"; break;
            case "dining":   sql = "DELETE FROM DiningVenue WHERE DiningVenueID=?"; break;
            case "facility": sql = "DELETE FROM Facility WHERE FacilityID=?"; break;
            default:         sql = "DELETE FROM EntertainmentEvent WHERE EventID=?"; break;
        }
        try (PreparedStatement ps = DBConnection.get().prepareStatement(sql)) {
            ps.setInt(1, id); ps.executeUpdate(); loadAll();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static DefaultTableModel tableModel(String... cols) {
        return new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
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

    // ── Deck Dialog ───────────────────────────────────────────────────────────

    class DeckDialog extends JDialog {
        private final JTextField numField = new JTextField(8);
        private final JComboBox<String[]> sc = shipCombo();
        private final int deckId;

        DeckDialog(Window parent, int deckId) {
            super(parent, deckId < 0 ? "Add Deck" : "Edit Deck", ModalityType.APPLICATION_MODAL);
            this.deckId = deckId;
            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Deck Number:", numField);
            ShipsPanel.addRow(form, 1, "Ship:", sc);
            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 2, save, cancel);
            if (deckId > 0) {
                try (PreparedStatement ps = DBConnection.get().prepareStatement(
                        "SELECT DeckNumber, ShipID FROM Deck WHERE DeckID=?")) {
                    ps.setInt(1, deckId); ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        numField.setText(String.valueOf(rs.getInt(1)));
                        String sid = rs.getString(2);
                        for (int i = 0; i < sc.getItemCount(); i++)
                            if (sc.getItemAt(i)[0].equals(sid)) { sc.setSelectedIndex(i); break; }
                    }
                } catch (SQLException ignored) {}
            }
            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);
            add(form); pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void save() {
            String num = numField.getText().trim();
            if (num.isEmpty() || sc.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "All fields required."); return;
            }
            int shipId = Integer.parseInt(((String[]) sc.getSelectedItem())[0]);
            try {
                if (deckId < 0) {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "INSERT INTO Deck (DeckNumber, ShipID) VALUES (?,?)")) {
                        ps.setInt(1, Integer.parseInt(num)); ps.setInt(2, shipId); ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "UPDATE Deck SET DeckNumber=?, ShipID=? WHERE DeckID=?")) {
                        ps.setInt(1, Integer.parseInt(num)); ps.setInt(2, shipId); ps.setInt(3, deckId);
                        ps.executeUpdate();
                    }
                }
                dispose();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Cabin Dialog ──────────────────────────────────────────────────────────

    class CabinDialog extends JDialog {
        private final JTextField numField  = new JTextField(10);
        private final JComboBox<String> typeCombo = new JComboBox<>(
                new String[]{"Interior", "Ocean View", "Balcony", "Suite"});
        private final JComboBox<String[]> sc = shipCombo();
        private JComboBox<String[]> dc;
        private final int cabinId;

        CabinDialog(Window parent, int cabinId) {
            super(parent, cabinId < 0 ? "Add Cabin" : "Edit Cabin", ModalityType.APPLICATION_MODAL);
            this.cabinId = cabinId;
            dc = deckComboForShip(getSelectedShipId(sc));
            sc.addActionListener(e -> {
                int idx = sc.getSelectedIndex();
                String[] sel = idx >= 0 ? (String[]) sc.getItemAt(idx) : null;
                int sid = sel != null ? Integer.parseInt(sel[0]) : 0;
                JComboBox<String[]> newDc = deckComboForShip(sid);
                Container form = dc.getParent();
                if (form != null) { form.remove(dc); }
                dc = newDc;
                if (form != null) {
                    GridBagConstraints c2 = new GridBagConstraints();
                    c2.gridx = 1; c2.gridy = 3; c2.anchor = GridBagConstraints.WEST;
                    c2.insets = new Insets(6, 6, 6, 6);
                    form.add(dc, c2);
                    form.revalidate(); form.repaint();
                }
            });
            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Cabin Number:", numField);
            ShipsPanel.addRow(form, 1, "Cabin Type:",   typeCombo);
            ShipsPanel.addRow(form, 2, "Ship:",         sc);
            ShipsPanel.addRow(form, 3, "Deck:",         dc);
            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 4, save, cancel);
            if (cabinId > 0) {
                try (PreparedStatement ps = DBConnection.get().prepareStatement(
                        "SELECT CabinNumber, CabinType, ShipID, DeckID FROM Cabin WHERE CabinID=?")) {
                    ps.setInt(1, cabinId); ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        numField.setText(rs.getString(1));
                        typeCombo.setSelectedItem(rs.getString(2));
                        String sid = rs.getString(3), did = rs.getString(4);
                        for (int i = 0; i < sc.getItemCount(); i++)
                            if (sc.getItemAt(i)[0].equals(sid)) { sc.setSelectedIndex(i); break; }
                        dc = deckComboForShip(Integer.parseInt(sid));
                        if (did != null)
                            for (int i = 0; i < dc.getItemCount(); i++)
                                if (dc.getItemAt(i)[0].equals(did)) { dc.setSelectedIndex(i); break; }
                    }
                } catch (SQLException ignored) {}
            }
            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);
            add(form); pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private int getSelectedShipId(JComboBox<String[]> combo) {
            String[] sel = (String[]) combo.getSelectedItem();
            return sel != null ? Integer.parseInt(sel[0]) : 0;
        }

        private JComboBox<String[]> deckComboForShip(int shipId) {
            JComboBox<String[]> cb = new JComboBox<>();
            cb.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? "Deck " + v[1] : ""));
            cb.addItem(new String[]{"0", "—"});
            if (shipId > 0) {
                try (PreparedStatement ps = DBConnection.get().prepareStatement(
                        "SELECT DeckID, DeckNumber FROM Deck WHERE ShipID=? ORDER BY DeckNumber")) {
                    ps.setInt(1, shipId);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) cb.addItem(new String[]{rs.getString(1), rs.getString(2)});
                } catch (SQLException ignored) {}
            }
            return cb;
        }

        private void save() {
            String num = numField.getText().trim();
            if (num.isEmpty() || sc.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Cabin number and ship are required."); return;
            }
            int shipId = Integer.parseInt(((String[]) sc.getSelectedItem())[0]);
            String[] dsel = (String[]) dc.getSelectedItem();
            Integer deckId = (dsel == null || dsel[0].equals("0")) ? null : Integer.parseInt(dsel[0]);
            String type = (String) typeCombo.getSelectedItem();
            try {
                if (cabinId < 0) {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "INSERT INTO Cabin (CabinNumber, CabinType, ShipID, DeckID) VALUES (?,?,?,?)")) {
                        ps.setString(1, num); ps.setString(2, type); ps.setInt(3, shipId);
                        if (deckId == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, deckId);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "UPDATE Cabin SET CabinNumber=?, CabinType=?, ShipID=?, DeckID=? WHERE CabinID=?")) {
                        ps.setString(1, num); ps.setString(2, type); ps.setInt(3, shipId);
                        if (deckId == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, deckId);
                        ps.setInt(5, cabinId); ps.executeUpdate();
                    }
                }
                dispose();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Dining Dialog ─────────────────────────────────────────────────────────

    class DiningDialog extends JDialog {
        private final JTextField nameField = new JTextField(20);
        private final JTextField capField  = new JTextField(8);
        private final JComboBox<String[]> sc = shipCombo();
        private final int venueId;

        DiningDialog(Window parent, int venueId) {
            super(parent, venueId < 0 ? "Add Dining Venue" : "Edit Dining Venue", ModalityType.APPLICATION_MODAL);
            this.venueId = venueId;
            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Venue Name:", nameField);
            ShipsPanel.addRow(form, 1, "Capacity:",   capField);
            ShipsPanel.addRow(form, 2, "Ship:",       sc);
            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 3, save, cancel);
            if (venueId > 0) {
                try (PreparedStatement ps = DBConnection.get().prepareStatement(
                        "SELECT VenueName, Capacity, ShipID FROM DiningVenue WHERE DiningVenueID=?")) {
                    ps.setInt(1, venueId); ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        nameField.setText(rs.getString(1));
                        capField.setText(rs.getString(2));
                        String sid = rs.getString(3);
                        for (int i = 0; i < sc.getItemCount(); i++)
                            if (sc.getItemAt(i)[0].equals(sid)) { sc.setSelectedIndex(i); break; }
                    }
                } catch (SQLException ignored) {}
            }
            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);
            add(form); pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void save() {
            String name = nameField.getText().trim(), cap = capField.getText().trim();
            if (name.isEmpty() || sc.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Name and ship are required."); return;
            }
            int shipId = Integer.parseInt(((String[]) sc.getSelectedItem())[0]);
            Integer capacity = cap.isEmpty() ? null : Integer.parseInt(cap);
            try {
                if (venueId < 0) {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "INSERT INTO DiningVenue (VenueName, Capacity, ShipID) VALUES (?,?,?)")) {
                        ps.setString(1, name);
                        if (capacity == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, capacity);
                        ps.setInt(3, shipId); ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "UPDATE DiningVenue SET VenueName=?, Capacity=?, ShipID=? WHERE DiningVenueID=?")) {
                        ps.setString(1, name);
                        if (capacity == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, capacity);
                        ps.setInt(3, shipId); ps.setInt(4, venueId); ps.executeUpdate();
                    }
                }
                dispose();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Facility Dialog ───────────────────────────────────────────────────────

    class FacilityDialog extends JDialog {
        private final JTextField nameField = new JTextField(20);
        private final JComboBox<String[]> sc = shipCombo();
        private final int facilityId;

        FacilityDialog(Window parent, int facilityId) {
            super(parent, facilityId < 0 ? "Add Facility" : "Edit Facility", ModalityType.APPLICATION_MODAL);
            this.facilityId = facilityId;
            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Facility Name:", nameField);
            ShipsPanel.addRow(form, 1, "Ship:",          sc);
            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 2, save, cancel);
            if (facilityId > 0) {
                try (PreparedStatement ps = DBConnection.get().prepareStatement(
                        "SELECT FacilityName, ShipID FROM Facility WHERE FacilityID=?")) {
                    ps.setInt(1, facilityId); ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        nameField.setText(rs.getString(1));
                        String sid = rs.getString(2);
                        for (int i = 0; i < sc.getItemCount(); i++)
                            if (sc.getItemAt(i)[0].equals(sid)) { sc.setSelectedIndex(i); break; }
                    }
                } catch (SQLException ignored) {}
            }
            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);
            add(form); pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void save() {
            String name = nameField.getText().trim();
            if (name.isEmpty() || sc.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "All fields required."); return;
            }
            int shipId = Integer.parseInt(((String[]) sc.getSelectedItem())[0]);
            try {
                if (facilityId < 0) {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "INSERT INTO Facility (FacilityName, ShipID) VALUES (?,?)")) {
                        ps.setString(1, name); ps.setInt(2, shipId); ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "UPDATE Facility SET FacilityName=?, ShipID=? WHERE FacilityID=?")) {
                        ps.setString(1, name); ps.setInt(2, shipId); ps.setInt(3, facilityId);
                        ps.executeUpdate();
                    }
                }
                dispose();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Event Dialog ──────────────────────────────────────────────────────────

    class EventDialog extends JDialog {
        private final JTextField nameField  = new JTextField(20);
        private final JTextField dtField    = new JTextField(18);
        private final JTextField venueField = new JTextField(20);
        private final JComboBox<String[]> sc = shipCombo();
        private final int eventId;

        EventDialog(Window parent, int eventId) {
            super(parent, eventId < 0 ? "Add Event" : "Edit Event", ModalityType.APPLICATION_MODAL);
            this.eventId = eventId;
            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "Event Name:",              nameField);
            ShipsPanel.addRow(form, 1, "Date/Time (YYYY-MM-DD HH:MM):", dtField);
            ShipsPanel.addRow(form, 2, "Venue:",                   venueField);
            ShipsPanel.addRow(form, 3, "Ship:",                    sc);
            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 4, save, cancel);
            if (eventId > 0) {
                try (PreparedStatement ps = DBConnection.get().prepareStatement(
                        "SELECT EventName, EventDateTime, Venue, ShipID FROM EntertainmentEvent WHERE EventID=?")) {
                    ps.setInt(1, eventId); ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        nameField.setText(rs.getString(1));
                        dtField.setText(rs.getString(2));
                        venueField.setText(rs.getString(3));
                        String sid = rs.getString(4);
                        for (int i = 0; i < sc.getItemCount(); i++)
                            if (sc.getItemAt(i)[0].equals(sid)) { sc.setSelectedIndex(i); break; }
                    }
                } catch (SQLException ignored) {}
            }
            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);
            add(form); pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void save() {
            String name = nameField.getText().trim();
            if (name.isEmpty() || sc.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Event name and ship are required."); return;
            }
            int shipId = Integer.parseInt(((String[]) sc.getSelectedItem())[0]);
            String dt = dtField.getText().trim();
            String venue = venueField.getText().trim();
            try {
                if (eventId < 0) {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "INSERT INTO EntertainmentEvent (EventName, EventDateTime, Venue, ShipID) VALUES (?,?,?,?)")) {
                        ps.setString(1, name);
                        ps.setString(2, dt.isEmpty() ? null : dt);
                        ps.setString(3, venue.isEmpty() ? null : venue);
                        ps.setInt(4, shipId); ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = DBConnection.get().prepareStatement(
                            "UPDATE EntertainmentEvent SET EventName=?, EventDateTime=?, Venue=?, ShipID=? WHERE EventID=?")) {
                        ps.setString(1, name);
                        ps.setString(2, dt.isEmpty() ? null : dt);
                        ps.setString(3, venue.isEmpty() ? null : venue);
                        ps.setInt(4, shipId); ps.setInt(5, eventId); ps.executeUpdate();
                    }
                }
                dispose();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

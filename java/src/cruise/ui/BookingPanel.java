package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class BookingPanel extends JPanel {

    private final int passengerId;

    private final JTextField firstField  = new JTextField(15);
    private final JTextField lastField   = new JTextField(15);
    private final JTextField emailField  = new JTextField(20);
    private final JTextField phoneField  = new JTextField(15);
    private final JComboBox<String[]> voyageCombo = new JComboBox<>();
    private final JComboBox<String[]> cabinCombo  = new JComboBox<>();
    private final JLabel confirmLabel = new JLabel(" ");

    public BookingPanel(int passengerId) {
        super(new BorderLayout(8, 8));
        this.passengerId = passengerId;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel heading = new JLabel("Book a Voyage");
        heading.setFont(new Font("SansSerif", Font.BOLD, 15));

        JPanel form = buildForm();
        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);

        add(heading, BorderLayout.NORTH);
        add(scroll,  BorderLayout.CENTER);

        loadCombos();
        if (passengerId > 0) prefillPassengerInfo();
    }

    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.anchor = GridBagConstraints.WEST;

        // Section: Passenger Info
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        JLabel sec1 = new JLabel("Your Information");
        sec1.setFont(new Font("SansSerif", Font.BOLD, 13));
        sec1.setForeground(new Color(10, 60, 120));
        p.add(sec1, c);

        c.gridwidth = 1;
        addRow(p, c, 1, "First Name *", firstField);
        addRow(p, c, 2, "Last Name *",  lastField);
        addRow(p, c, 3, "Email",        emailField);
        addRow(p, c, 4, "Phone",        phoneField);

        // Separator
        c.gridx = 0; c.gridy = 5; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
        p.add(new JSeparator(), c);
        c.fill = GridBagConstraints.NONE;

        // Section: Voyage Selection
        c.gridx = 0; c.gridy = 6; c.gridwidth = 2;
        JLabel sec2 = new JLabel("Voyage & Cabin");
        sec2.setFont(new Font("SansSerif", Font.BOLD, 13));
        sec2.setForeground(new Color(10, 60, 120));
        p.add(sec2, c);

        c.gridwidth = 1;
        voyageCombo.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));
        cabinCombo.setRenderer((l, v, i, s, f)  -> new JLabel(v != null ? v[1] : ""));
        addRow(p, c, 7, "Select Voyage *", voyageCombo);
        addRow(p, c, 8, "Select Cabin *",  cabinCombo);

        // Submit button
        JButton bookBtn = new JButton("Confirm Booking");
        bookBtn.setBackground(new Color(10, 60, 120));
        bookBtn.setForeground(Color.WHITE);
        bookBtn.setFocusPainted(false);
        bookBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        bookBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        c.gridx = 0; c.gridy = 9; c.gridwidth = 2; c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(16, 8, 6, 8);
        p.add(bookBtn, c);

        confirmLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        c.gridy = 10;
        c.insets = new Insets(4, 8, 4, 8);
        p.add(confirmLabel, c);

        bookBtn.addActionListener(e -> submitBooking());
        return p;
    }

    private void prefillPassengerInfo() {
        try (PreparedStatement ps = DBConnection.get().prepareStatement(
                "SELECT FirstName, LastName, Email, Phone FROM Passenger WHERE PassengerID=?")) {
            ps.setInt(1, passengerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                firstField.setText(rs.getString(1));
                lastField.setText(rs.getString(2));
                emailField.setText(rs.getString(3) != null ? rs.getString(3) : "");
                phoneField.setText(rs.getString(4) != null ? rs.getString(4) : "");
            }
        } catch (SQLException ignored) {}
        firstField.setEditable(false);
        lastField.setEditable(false);
        emailField.setEditable(false);
        phoneField.setEditable(false);
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0; c.gridy = row; c.anchor = GridBagConstraints.EAST; c.gridwidth = 1;
        p.add(new JLabel(label + ":"), c);
        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        p.add(field, c);
    }

    private void loadCombos() {
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT v.VoyageID, CONCAT(s.ShipName, ' — ', v.DepartureDate, ' (', se.SeasonName, ')') " +
                "FROM Voyage v JOIN Ship s ON v.ShipID=s.ShipID JOIN Season se ON v.SeasonID=se.SeasonID " +
                "ORDER BY v.DepartureDate")) {
            while (rs.next()) voyageCombo.addItem(new String[]{rs.getString(1), rs.getString(2)});
        } catch (SQLException ignored) {}

        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT c.CabinID, CONCAT(c.CabinNumber, ' — ', c.CabinType, ' (', s.ShipName, ')') " +
                "FROM Cabin c JOIN Ship s ON c.ShipID=s.ShipID ORDER BY s.ShipName, c.CabinNumber")) {
            while (rs.next()) cabinCombo.addItem(new String[]{rs.getString(1), rs.getString(2)});
        } catch (SQLException ignored) {}
    }

    private void submitBooking() {
        String first = firstField.getText().trim();
        String last  = lastField.getText().trim();
        if (first.isEmpty() || last.isEmpty()) {
            confirmLabel.setForeground(Color.RED);
            confirmLabel.setText("First and last name are required.");
            return;
        }
        if (voyageCombo.getSelectedItem() == null || cabinCombo.getSelectedItem() == null) {
            confirmLabel.setForeground(Color.RED);
            confirmLabel.setText("Please select a voyage and cabin.");
            return;
        }

        try {
            // Use existing passenger if logged in, otherwise insert new one
            int resolvedPassengerId;
            if (passengerId > 0) {
                resolvedPassengerId = passengerId;
            } else {
                try (PreparedStatement ps = DBConnection.get().prepareStatement(
                        "INSERT INTO Passenger (FirstName, LastName, Email, Phone) VALUES (?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, first);
                    ps.setString(2, last);
                    ps.setString(3, emailField.getText().trim());
                    ps.setString(4, phoneField.getText().trim());
                    ps.executeUpdate();
                    ResultSet keys = ps.getGeneratedKeys();
                    if (!keys.next()) throw new SQLException("Failed to create passenger.");
                    resolvedPassengerId = keys.getInt(1);
                }
            }

            // Insert reservation
            int voyageId = Integer.parseInt(((String[]) voyageCombo.getSelectedItem())[0]);
            int cabinId  = Integer.parseInt(((String[]) cabinCombo.getSelectedItem())[0]);
            int resId;
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT INTO Reservation (ReservationDate, Status, PassengerID, VoyageID, CabinID) VALUES (CURDATE(),'Confirmed',?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, resolvedPassengerId);
                ps.setInt(2, voyageId);
                ps.setInt(3, cabinId);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                keys.next();
                resId = keys.getInt(1);
            }

            // Auto-create ticket — price based on cabin type
            double ticketPrice;
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT CabinType FROM Cabin WHERE CabinID=?")) {
                ps.setInt(1, cabinId);
                ResultSet rs2 = ps.executeQuery();
                String cabinType = rs2.next() ? rs2.getString(1) : "Interior";
                ticketPrice = switch (cabinType.toLowerCase()) {
                    case "suite"      -> 1499.00;
                    case "balcony"    -> 999.00;
                    case "ocean view" -> 799.00;
                    default           -> 599.00;
                };
            }
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT INTO Ticket (IssueDate, ReservationID, TicketPrice) VALUES (CURDATE(),?,?)")) {
                ps.setInt(1, resId);
                ps.setDouble(2, ticketPrice);
                ps.executeUpdate();
            }

            confirmLabel.setForeground(new Color(0, 130, 0));
            confirmLabel.setText("Booking confirmed! Your Reservation ID is #" + resId + ". Please save this number.");
            clearForm();

        } catch (SQLException e) {
            confirmLabel.setForeground(Color.RED);
            confirmLabel.setText("Error: " + e.getMessage());
        }
    }

    private void clearForm() {
        if (passengerId <= 0) {
            firstField.setText(""); lastField.setText("");
            emailField.setText(""); phoneField.setText("");
        }
        voyageCombo.setSelectedIndex(0);
        cabinCombo.setSelectedIndex(0);
    }
}

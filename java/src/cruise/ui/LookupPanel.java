package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class LookupPanel extends JPanel {

    private final int passengerId;

    private final JTextField searchField = new JTextField(20);
    private final DefaultTableModel model;
    private final JTable table;
    private final JTextArea detailArea   = new JTextArea(6, 40);
    private final JButton addExcursionBtn  = new JButton("+ Add Excursion");
    private final JButton makePaymentBtn   = new JButton("Make Payment");

    public LookupPanel(int passengerId) {
        super(new BorderLayout(8, 8));
        this.passengerId = passengerId;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel heading = new JLabel("My Reservations");
        heading.setFont(new Font("SansSerif", Font.BOLD, 15));

        // Search bar
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchPanel.add(new JLabel("Last Name or Reservation ID:"));
        searchPanel.add(searchField);
        JButton searchBtn = new JButton("Search");
        searchPanel.add(searchBtn);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(heading,     BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.CENTER);

        // Results table
        String[] cols = {"Res. ID", "Passenger", "Ship", "Departure", "Cabin", "Status"};
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);
        table.getColumnModel().getColumn(0).setMaxWidth(70);

        detailArea.setEditable(false);
        detailArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        detailArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        detailArea.setText("Search for your reservation above.");

        addExcursionBtn.setFocusPainted(false);
        addExcursionBtn.setVisible(false);
        addExcursionBtn.addActionListener(e -> {
            Object resId  = addExcursionBtn.getClientProperty("resId");
            Object itinId = addExcursionBtn.getClientProperty("itinId");
            if (resId != null && itinId != null)
                openAddExcursionDialog((int) resId, (int) itinId);
        });

        makePaymentBtn.setFocusPainted(false);
        makePaymentBtn.setVisible(false);
        makePaymentBtn.addActionListener(e -> {
            Object resId = makePaymentBtn.getClientProperty("resId");
            if (resId != null) openMakePaymentDialog((int) resId);
        });

        JScrollPane tableScroll  = new JScrollPane(table);
        JScrollPane detailScroll = new JScrollPane(detailArea);

        JPanel detailPanel = new JPanel(new BorderLayout(4, 4));
        detailPanel.setBorder(BorderFactory.createTitledBorder("Reservation Details"));
        JPanel excBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        excBtnRow.add(addExcursionBtn);
        excBtnRow.add(makePaymentBtn);
        detailPanel.add(excBtnRow,    BorderLayout.NORTH);
        detailPanel.add(detailScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, detailPanel);
        split.setResizeWeight(0.5);

        add(topPanel, BorderLayout.NORTH);
        add(split,    BorderLayout.CENTER);

        searchBtn.addActionListener(e -> search());
        searchField.addActionListener(e -> search());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showDetail();
        });

        // Auto-load for logged-in passenger; hide search bar
        if (passengerId > 0) {
            topPanel.setVisible(false);
            loadForPassenger(passengerId);
        }
    }

    public void refresh() {
        if (passengerId > 0) loadForPassenger(passengerId);
    }

    private void loadForPassenger(int pid) {
        model.setRowCount(0);
        detailArea.setText("Select a reservation to see full details.");
        addExcursionBtn.setVisible(false);
        try (PreparedStatement ps = DBConnection.get().prepareStatement(
                "SELECT r.ReservationID, CONCAT(p.FirstName,' ',p.LastName), " +
                "       s.ShipName, v.DepartureDate, c.CabinNumber, r.Status " +
                "FROM Reservation r " +
                "JOIN Passenger p ON r.PassengerID = p.PassengerID " +
                "JOIN Voyage v    ON r.VoyageID    = v.VoyageID " +
                "JOIN Ship s      ON v.ShipID       = s.ShipID " +
                "JOIN Cabin c     ON r.CabinID      = c.CabinID " +
                "WHERE r.PassengerID = ? ORDER BY v.DepartureDate")) {
            ps.setInt(1, pid);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6)});
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void search() {
        String term = searchField.getText().trim();
        if (term.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a last name or reservation ID.");
            return;
        }
        model.setRowCount(0);
        detailArea.setText("Select a row to see full details.");
        try (PreparedStatement ps = DBConnection.get().prepareStatement(
                "SELECT r.ReservationID, CONCAT(p.FirstName,' ',p.LastName), " +
                "       s.ShipName, v.DepartureDate, c.CabinNumber, r.Status " +
                "FROM Reservation r " +
                "JOIN Passenger p ON r.PassengerID = p.PassengerID " +
                "JOIN Voyage v    ON r.VoyageID    = v.VoyageID " +
                "JOIN Ship s      ON v.ShipID       = s.ShipID " +
                "JOIN Cabin c     ON r.CabinID      = c.CabinID " +
                "WHERE p.LastName LIKE ? OR r.ReservationID = ? " +
                "ORDER BY v.DepartureDate")) {
            ps.setString(1, "%" + term + "%");
            try { ps.setInt(2, Integer.parseInt(term)); }
            catch (NumberFormatException e) { ps.setInt(2, -1); }
            ResultSet rs = ps.executeQuery();
            boolean any = false;
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6)});
                any = true;
            }
            if (!any) JOptionPane.showMessageDialog(this, "No reservations found for \"" + term + "\".");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showDetail() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int resId = (int) model.getValueAt(row, 0);

        StringBuilder sb = new StringBuilder();

        try (PreparedStatement ps = DBConnection.get().prepareStatement(
                "SELECT r.ReservationID, r.ReservationDate, r.Status, " +
                "       CONCAT(p.FirstName,' ',p.LastName), p.Email, p.Phone, " +
                "       s.ShipName, i.ItineraryName, v.DepartureDate, v.ReturnDate, " +
                "       c.CabinNumber, c.CabinType, v.VoyageID, v.ItineraryID " +
                "FROM Reservation r " +
                "JOIN Passenger p ON r.PassengerID = p.PassengerID " +
                "JOIN Voyage v    ON r.VoyageID    = v.VoyageID " +
                "JOIN Ship s      ON v.ShipID       = s.ShipID " +
                "JOIN Itinerary i ON v.ItineraryID  = i.ItineraryID " +
                "JOIN Cabin c     ON r.CabinID      = c.CabinID " +
                "WHERE r.ReservationID = ?")) {
            ps.setInt(1, resId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) { detailArea.setText("Reservation not found."); return; }

            sb.append("Reservation #").append(rs.getInt(1))
              .append("   Status: ").append(rs.getString(3)).append("\n")
              .append("Booked on: ").append(rs.getString(2)).append("\n\n")
              .append("Passenger:  ").append(rs.getString(4)).append("\n")
              .append("Email:      ").append(rs.getString(5) != null ? rs.getString(5) : "—").append("\n")
              .append("Phone:      ").append(rs.getString(6) != null ? rs.getString(6) : "—").append("\n\n")
              .append("Ship:       ").append(rs.getString(7)).append("\n")
              .append("Itinerary:  ").append(rs.getString(8)).append("\n")
              .append("Departure:  ").append(rs.getString(9)).append("\n")
              .append("Return:     ").append(rs.getString(10) != null ? rs.getString(10) : "—").append("\n\n")
              .append("Cabin:      ").append(rs.getString(11)).append("  (").append(rs.getString(12)).append(")\n");

            int itinId = rs.getInt(14);

            // Ticket info
            sb.append("\n── TICKET ──────────────────────────────\n");
            try (PreparedStatement ps2 = DBConnection.get().prepareStatement(
                    "SELECT TicketID, IssueDate, TicketPrice FROM Ticket WHERE ReservationID=?")) {
                ps2.setInt(1, resId);
                ResultSet rs2 = ps2.executeQuery();
                if (rs2.next()) {
                    sb.append("Ticket #").append(rs2.getInt(1))
                      .append("   Issued: ").append(rs2.getString(2))
                      .append("   Price: ");
                    Object price = rs2.getObject(3);
                    sb.append(price != null ? "$" + String.format("%.2f", ((Number)price).doubleValue()) : "TBD").append("\n");
                } else {
                    sb.append("No ticket on file.\n");
                }
            }

            // Payments
            sb.append("\n── PAYMENTS ─────────────────────────────\n");
            try (PreparedStatement ps2 = DBConnection.get().prepareStatement(
                    "SELECT PaymentID, Amount, PaymentDate, PaymentMethod FROM Payment WHERE ReservationID=? ORDER BY PaymentDate")) {
                ps2.setInt(1, resId);
                ResultSet rs2 = ps2.executeQuery();
                boolean any = false;
                while (rs2.next()) {
                    sb.append("  #").append(rs2.getInt(1))
                      .append("  $").append(String.format("%.2f", rs2.getDouble(2)))
                      .append("  ").append(rs2.getString(3))
                      .append("  (").append(rs2.getString(4)).append(")\n");
                    any = true;
                }
                if (!any) sb.append("  No payments recorded.\n");
            }

            // Booked excursions
            sb.append("\n── EXCURSIONS BOOKED ────────────────────\n");
            double excursionTotal = 0;
            try (PreparedStatement ps2 = DBConnection.get().prepareStatement(
                    "SELECT e.ExcursionName, p.PortName, e.Price " +
                    "FROM ReservationExcursion re " +
                    "JOIN Excursion e ON re.ExcursionID = e.ExcursionID " +
                    "JOIN Port p      ON e.PortID       = p.PortID " +
                    "WHERE re.ReservationID=?")) {
                ps2.setInt(1, resId);
                ResultSet rs2 = ps2.executeQuery();
                boolean any = false;
                while (rs2.next()) {
                    double excPrice = rs2.getDouble(3);
                    excursionTotal += excPrice;
                    sb.append("  • ").append(rs2.getString(1))
                      .append(" @ ").append(rs2.getString(2))
                      .append("  —  $").append(String.format("%.2f", excPrice)).append("\n");
                    any = true;
                }
                if (!any) sb.append("  None booked yet.\n");
            }

            // Total
            sb.append("\n── TOTAL ────────────────────────────────\n");
            Object ticketPriceRaw = null;
            try (PreparedStatement ps2 = DBConnection.get().prepareStatement(
                    "SELECT TicketPrice FROM Ticket WHERE ReservationID=?")) {
                ps2.setInt(1, resId);
                ResultSet rs2 = ps2.executeQuery();
                if (rs2.next()) ticketPriceRaw = rs2.getObject(1);
            }
            if (ticketPriceRaw != null) {
                double ticketPrice = ((Number) ticketPriceRaw).doubleValue();
                double grandTotal  = ticketPrice + excursionTotal;
                sb.append("  Ticket:      $").append(String.format("%.2f", ticketPrice)).append("\n");
                if (excursionTotal > 0)
                    sb.append("  Excursions:  $").append(String.format("%.2f", excursionTotal)).append("\n");
                sb.append("  ─────────────────────\n");
                sb.append("  Total:       $").append(String.format("%.2f", grandTotal)).append("\n");
            } else {
                sb.append("  Total:       TBD\n");
            }

            detailArea.setText(sb.toString());
            detailArea.setCaretPosition(0);

            // Show action buttons only when a row is selected
            addExcursionBtn.setVisible(true);
            addExcursionBtn.putClientProperty("resId",   resId);
            addExcursionBtn.putClientProperty("itinId",  itinId);

            makePaymentBtn.setVisible(true);
            makePaymentBtn.putClientProperty("resId", resId);

        } catch (SQLException e) {
            detailArea.setText("Error loading details: " + e.getMessage());
        }
    }

    private void openMakePaymentDialog(int resId) {
        JTextField amountField = new JTextField(10);
        String[] methods = {"Credit Card", "Debit Card", "Cash", "Bank Transfer"};
        JComboBox<String> methodCombo = new JComboBox<>(methods);

        JPanel form = new JPanel(new java.awt.GridLayout(2, 2, 8, 8));
        form.add(new JLabel("Amount ($):"));
        form.add(amountField);
        form.add(new JLabel("Payment Method:"));
        form.add(methodCombo);

        int opt = JOptionPane.showConfirmDialog(this, form,
                "Make a Payment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;

        String amtText = amountField.getText().trim();
        if (amtText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an amount.");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amtText);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Enter a valid positive amount.");
            return;
        }

        try (PreparedStatement ps = DBConnection.get().prepareStatement(
                "INSERT INTO Payment (Amount, PaymentDate, PaymentMethod, ReservationID) VALUES (?, CURDATE(), ?, ?)")) {
            ps.setDouble(1, amount);
            ps.setString(2, (String) methodCombo.getSelectedItem());
            ps.setInt(3, resId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Payment of $" + String.format("%.2f", amount) + " recorded.");
            showDetail();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openAddExcursionDialog(int resId, int itinId) {
        JComboBox<String[]> excCombo = new JComboBox<>();
        excCombo.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));
        try (PreparedStatement ps = DBConnection.get().prepareStatement(
                "SELECT e.ExcursionID, CONCAT(e.ExcursionName, ' @ ', p.PortName, '  — $', " +
                "FORMAT(e.Price,2)) " +
                "FROM Excursion e JOIN Port p ON e.PortID = p.PortID " +
                "WHERE e.PortID IN (SELECT PortID FROM Stop WHERE ItineraryID=?) " +
                "ORDER BY p.PortName, e.ExcursionName")) {
            ps.setInt(1, itinId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) excCombo.addItem(new String[]{rs.getString(1), rs.getString(2)});
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage()); return;
        }
        if (excCombo.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "No excursions available for this voyage's itinerary.");
            return;
        }
        int opt = JOptionPane.showConfirmDialog(this, excCombo,
                "Select Excursion to Add", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION || excCombo.getSelectedItem() == null) return;
        int excId = Integer.parseInt(((String[]) excCombo.getSelectedItem())[0]);
        try (PreparedStatement ps = DBConnection.get().prepareStatement(
                "INSERT INTO ReservationExcursion (ReservationID, ExcursionID) VALUES (?,?)")) {
            ps.setInt(1, resId);
            ps.setInt(2, excId);
            ps.executeUpdate();
            showDetail();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class FinancialsPanel extends JPanel {

    // ── Tickets ──────────────────────────────────────────────────────────────
    private final DefaultTableModel ticketModel =
            tModel("Ticket ID", "Issue Date", "Price", "Reservation ID", "Passenger");
    private final JTable ticketTable = makeTable(ticketModel);

    // ── Payments ─────────────────────────────────────────────────────────────
    private final DefaultTableModel paymentModel =
            tModel("Payment ID", "Amount", "Date", "Method", "Reservation ID", "Passenger");
    private final JTable paymentTable = makeTable(paymentModel);

    public FinancialsPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("  Tickets  ",  buildTicketsTab());
        tabs.addTab("  Payments  ", buildPaymentsTab());

        add(tabs, BorderLayout.CENTER);
        loadAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tickets tab
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildTicketsTab() {
        JButton addBtn = new JButton("Add Ticket");
        JButton delBtn = new JButton("Delete");

        addBtn.addActionListener(e -> {
            new TicketDialog(SwingUtilities.getWindowAncestor(this)).setVisible(true);
            loadTickets();
        });
        delBtn.addActionListener(e -> deleteSelected(ticketTable, ticketModel,
                "DELETE FROM Ticket WHERE TicketID=?", 0, "ticket"));

        return tabPanel(ticketTable, addBtn, delBtn);
    }

    private void loadTickets() {
        ticketModel.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT t.TicketID, t.IssueDate, t.TicketPrice, t.ReservationID, " +
                "       CONCAT(p.FirstName,' ',p.LastName) " +
                "FROM Ticket t " +
                "JOIN Reservation r ON t.ReservationID = r.ReservationID " +
                "JOIN Passenger p   ON r.PassengerID   = p.PassengerID " +
                "ORDER BY t.TicketID DESC")) {
            while (rs.next())
                ticketModel.addRow(new Object[]{
                        rs.getInt(1), rs.getString(2), rs.getObject(3),
                        rs.getInt(4), rs.getString(5)});
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Payments tab
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildPaymentsTab() {
        JButton addBtn = new JButton("Add Payment");
        JButton delBtn = new JButton("Delete");

        addBtn.addActionListener(e -> {
            new PaymentDialog(SwingUtilities.getWindowAncestor(this)).setVisible(true);
            loadPayments();
        });
        delBtn.addActionListener(e -> deleteSelected(paymentTable, paymentModel,
                "DELETE FROM Payment WHERE PaymentID=?", 0, "payment"));

        return tabPanel(paymentTable, addBtn, delBtn);
    }

    private void loadPayments() {
        paymentModel.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT py.PaymentID, py.Amount, py.PaymentDate, py.PaymentMethod, py.ReservationID, " +
                "       CONCAT(p.FirstName,' ',p.LastName) " +
                "FROM Payment py " +
                "JOIN Reservation r ON py.ReservationID = r.ReservationID " +
                "JOIN Passenger p   ON r.PassengerID    = p.PassengerID " +
                "ORDER BY py.PaymentDate DESC")) {
            while (rs.next())
                paymentModel.addRow(new Object[]{
                        rs.getInt(1), rs.getObject(2), rs.getString(3),
                        rs.getString(4), rs.getInt(5), rs.getString(6)});
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void loadAll() {
        loadTickets();
        loadPayments();
    }

    private void deleteSelected(JTable tbl, DefaultTableModel mdl,
                                String sql, int idCol, String label) {
        int row = tbl.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a " + label + " first."); return; }
        int id = (int) mdl.getValueAt(row, idCol);
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

    private static JPanel tabPanel(JTable tbl, JButton... buttons) {
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        for (JButton b : buttons) btnRow.add(b);

        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        p.add(btnRow,           BorderLayout.NORTH);
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

    static JComboBox<String[]> resCombo() {
        JComboBox<String[]> cb = new JComboBox<>();
        cb.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT r.ReservationID, CONCAT('#', r.ReservationID, ' — ', " +
                "p.FirstName,' ',p.LastName, ' / ', v.DepartureDate) " +
                "FROM Reservation r " +
                "JOIN Passenger p ON r.PassengerID = p.PassengerID " +
                "JOIN Voyage v    ON r.VoyageID    = v.VoyageID " +
                "ORDER BY r.ReservationID DESC")) {
            while (rs.next()) cb.addItem(new String[]{rs.getString(1), rs.getString(2)});
        } catch (SQLException ignored) {}
        return cb;
    }

    // ── Ticket Dialog ─────────────────────────────────────────────────────────

    private class TicketDialog extends JDialog {
        private final JComboBox<String[]> resC = resCombo();
        private final JTextField dateField  = new JTextField(12);
        private final JTextField priceField = new JTextField(10);

        TicketDialog(Window owner) {
            super(owner, "Add Ticket", ModalityType.APPLICATION_MODAL);
            JPanel form = ShipsPanel.formPanel();
            resC.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));
            ShipsPanel.addRow(form, 0, "Reservation", resC);
            ShipsPanel.addRow(form, 1, "Issue Date (YYYY-MM-DD)", dateField);
            ShipsPanel.addRow(form, 2, "Ticket Price", priceField);

            JButton save = new JButton("Save");
            JButton cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 3, save, cancel);

            save.addActionListener(e -> save());
            cancel.addActionListener(e -> dispose());

            setContentPane(form);
            pack();
            setLocationRelativeTo(owner);
        }

        private void save() {
            if (resC.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Select a reservation."); return;
            }
            int resId = Integer.parseInt(((String[]) resC.getSelectedItem())[0]);
            String date = dateField.getText().trim();
            String priceStr = priceField.getText().trim();
            if (date.isEmpty()) { JOptionPane.showMessageDialog(this, "Issue date required."); return; }
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT INTO Ticket (IssueDate, ReservationID, TicketPrice) VALUES (?,?,?)")) {
                ps.setString(1, date);
                ps.setInt(2, resId);
                if (priceStr.isEmpty()) ps.setNull(3, Types.DECIMAL);
                else ps.setDouble(3, Double.parseDouble(priceStr));
                ps.executeUpdate();
                dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Payment Dialog ────────────────────────────────────────────────────────

    private class PaymentDialog extends JDialog {
        private final JComboBox<String[]> resC    = resCombo();
        private final JTextField amountField = new JTextField(10);
        private final JTextField dateField   = new JTextField(12);
        private final JComboBox<String> methodCombo = new JComboBox<>(
                new String[]{"Credit Card", "Debit Card", "Cash", "Bank Transfer", "Other"});

        PaymentDialog(Window owner) {
            super(owner, "Add Payment", ModalityType.APPLICATION_MODAL);
            JPanel form = ShipsPanel.formPanel();
            resC.setRenderer((l, v, i, s, f) -> new JLabel(v != null ? v[1] : ""));
            ShipsPanel.addRow(form, 0, "Reservation", resC);
            ShipsPanel.addRow(form, 1, "Amount", amountField);
            ShipsPanel.addRow(form, 2, "Payment Date (YYYY-MM-DD)", dateField);
            ShipsPanel.addRow(form, 3, "Method", methodCombo);

            JButton save = new JButton("Save");
            JButton cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 4, save, cancel);

            save.addActionListener(e -> save());
            cancel.addActionListener(e -> dispose());

            setContentPane(form);
            pack();
            setLocationRelativeTo(owner);
        }

        private void save() {
            if (resC.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Select a reservation."); return;
            }
            String amtStr = amountField.getText().trim();
            String date   = dateField.getText().trim();
            if (amtStr.isEmpty() || date.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Amount and date are required."); return;
            }
            try {
                int resId = Integer.parseInt(((String[]) resC.getSelectedItem())[0]);
                try (PreparedStatement ps = DBConnection.get().prepareStatement(
                        "INSERT INTO Payment (Amount, PaymentDate, PaymentMethod, ReservationID) VALUES (?,?,?,?)")) {
                    ps.setDouble(1, Double.parseDouble(amtStr));
                    ps.setString(2, date);
                    ps.setString(3, (String) methodCombo.getSelectedItem());
                    ps.setInt(4, resId);
                    ps.executeUpdate();
                    dispose();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid amount.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

}

package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class PassengersPanel extends JPanel {

    private final DefaultTableModel model;
    private final JTable table;

    public PassengersPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"ID", "First Name", "Last Name", "Email", "Phone", "Bookings"};
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(22);

        JLabel heading = new JLabel("Passengers");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));

        JButton addBtn     = new JButton("Add");
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
            PassengerDialog d = new PassengerDialog(SwingUtilities.getWindowAncestor(this));
            d.setVisible(true); if (d.saved) loadData();
        });
        refreshBtn.addActionListener(e -> loadData());
    }

    private void loadData() {
        model.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT p.PassengerID, p.FirstName, p.LastName, p.Email, p.Phone, " +
                "       COUNT(r.ReservationID) AS Bookings " +
                "FROM Passenger p " +
                "LEFT JOIN Reservation r ON p.PassengerID = r.PassengerID " +
                "GROUP BY p.PassengerID ORDER BY p.LastName")) {
            while (rs.next())
                model.addRow(new Object[]{rs.getInt(1), rs.getString(2),
                        rs.getString(3), rs.getString(4), rs.getString(5), rs.getInt(6)});
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    static class PassengerDialog extends JDialog {
        boolean saved = false;
        private final JTextField firstField = new JTextField(15);
        private final JTextField lastField  = new JTextField(15);
        private final JTextField emailField = new JTextField(20);
        private final JTextField phoneField = new JTextField(15);

        PassengerDialog(Window parent) {
            super(parent, "Add Passenger", ModalityType.APPLICATION_MODAL);
            JPanel form = ShipsPanel.formPanel();
            ShipsPanel.addRow(form, 0, "First Name:", firstField);
            ShipsPanel.addRow(form, 1, "Last Name:",  lastField);
            ShipsPanel.addRow(form, 2, "Email:",      emailField);
            ShipsPanel.addRow(form, 3, "Phone:",      phoneField);

            JButton save = new JButton("Save"), cancel = new JButton("Cancel");
            ShipsPanel.addButtons(form, 4, save, cancel);
            save.addActionListener(e -> save()); cancel.addActionListener(e -> dispose());
            getRootPane().setDefaultButton(save);
            add(form); pack(); setResizable(false); setLocationRelativeTo(parent);
        }

        private void save() {
            String first = firstField.getText().trim(), last = lastField.getText().trim();
            if (first.isEmpty() || last.isEmpty()) {
                JOptionPane.showMessageDialog(this, "First and last name are required."); return;
            }
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT INTO Passenger (FirstName, LastName, Email, Phone) VALUES (?,?,?,?)")) {
                ps.setString(1, first); ps.setString(2, last);
                ps.setString(3, emailField.getText().trim());
                ps.setString(4, phoneField.getText().trim());
                ps.executeUpdate(); saved = true; dispose();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

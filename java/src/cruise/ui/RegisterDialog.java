package cruise.ui;

import cruise.db.DBConnection;
import cruise.util.PasswordUtil;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class RegisterDialog extends JDialog {

    private final JTextField firstField    = new JTextField(18);
    private final JTextField lastField     = new JTextField(18);
    private final JTextField emailField    = new JTextField(22);
    private final JTextField phoneField    = new JTextField(15);
    private final JTextField userField     = new JTextField(18);
    private final JPasswordField passField = new JPasswordField(18);
    private final JPasswordField confField = new JPasswordField(18);
    private final JLabel statusLabel       = new JLabel(" ");

    private int  createdPassengerId = -1;
    private String createdUsername  = null;

    public RegisterDialog(Window owner) {
        super(owner, "Create Account", ModalityType.APPLICATION_MODAL);
        buildUI();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 10, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 6, 5, 6);
        c.anchor = GridBagConstraints.WEST;

        addSection(form, c, 0, "Personal Information");
        addRow(form, c, 1, "First Name *",  firstField);
        addRow(form, c, 2, "Last Name *",   lastField);
        addRow(form, c, 3, "Email",         emailField);
        addRow(form, c, 4, "Phone",         phoneField);

        c.gridx = 0; c.gridy = 5; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(new JSeparator(), c);
        c.fill = GridBagConstraints.NONE; c.gridwidth = 1;

        addSection(form, c, 6, "Login Credentials");
        addRow(form, c, 7, "Username *",         userField);
        addRow(form, c, 8, "Password *",         passField);
        addRow(form, c, 9, "Confirm Password *", confField);

        JButton registerBtn = new JButton("Create Account");
        JButton cancelBtn   = new JButton("Cancel");
        registerBtn.setBackground(new Color(10, 60, 120));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);

        c.gridx = 0; c.gridy = 10; c.gridwidth = 2; c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(14, 6, 4, 6);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        btnRow.add(registerBtn);
        btnRow.add(cancelBtn);
        form.add(btnRow, c);

        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setForeground(Color.RED);
        c.gridy = 11; c.insets = new Insets(0, 6, 8, 6);
        form.add(statusLabel, c);

        registerBtn.addActionListener(e -> register());
        cancelBtn.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(registerBtn);

        setContentPane(form);
    }

    private void addSection(JPanel p, GridBagConstraints c, int row, String title) {
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(new Color(10, 60, 120));
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.insets = new Insets(8, 6, 2, 6);
        p.add(lbl, c);
        c.gridwidth = 1; c.insets = new Insets(5, 6, 5, 6);
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0; c.gridy = row; c.anchor = GridBagConstraints.EAST;
        p.add(new JLabel(label + ":"), c);
        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        p.add(field, c);
    }

    private void register() {
        String first    = firstField.getText().trim();
        String last     = lastField.getText().trim();
        String email    = emailField.getText().trim();
        String phone    = phoneField.getText().trim();
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());
        String confirm  = new String(confField.getPassword());

        if (first.isEmpty() || last.isEmpty()) {
            setStatus("First and last name are required."); return;
        }
        if (username.isEmpty() || password.isEmpty()) {
            setStatus("Username and password are required."); return;
        }
        if (password.length() < 6) {
            setStatus("Password must be at least 6 characters."); return;
        }
        if (!password.equals(confirm)) {
            setStatus("Passwords do not match."); return;
        }

        try {
            // Check username not already taken
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT COUNT(*) FROM PassengerAccount WHERE Username=?")) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                rs.next();
                if (rs.getInt(1) > 0) { setStatus("Username already taken."); return; }
            }

            // Insert Passenger
            int passengerId;
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT INTO Passenger (FirstName, LastName, Email, Phone) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, first);
                ps.setString(2, last);
                ps.setString(3, email.isEmpty()  ? null : email);
                ps.setString(4, phone.isEmpty()  ? null : phone);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) throw new SQLException("Failed to create passenger record.");
                passengerId = keys.getInt(1);
            }

            // Insert PassengerAccount
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT INTO PassengerAccount (Username, PasswordHash, PassengerID) VALUES (?,?,?)")) {
                ps.setString(1, username);
                ps.setString(2, PasswordUtil.hash(password));
                ps.setInt(3, passengerId);
                ps.executeUpdate();
            }

            createdPassengerId = passengerId;
            createdUsername    = username;
            dispose();

        } catch (SQLException e) {
            setStatus("Error: " + e.getMessage());
        }
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    public int  getPassengerId() { return createdPassengerId; }
    public String getUsername()  { return createdUsername; }
    public boolean wasSuccessful() { return createdPassengerId > 0; }
}

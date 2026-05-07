package cruise.ui;

import cruise.db.DBConnection;
import cruise.util.PasswordUtil;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class PassengerLoginDialog extends JDialog {

    private final JTextField     userField = new JTextField(18);
    private final JPasswordField passField = new JPasswordField(18);
    private final JLabel         statusLabel = new JLabel(" ");

    private int    passengerId   = -1;
    private String passengerName = null;

    public PassengerLoginDialog(Window owner) {
        super(owner, "Passenger Login", ModalityType.APPLICATION_MODAL);
        buildUI();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 28, 14, 28));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);

        JLabel title = new JLabel("Passenger Portal Login");
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setForeground(new Color(10, 60, 120));
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.anchor = GridBagConstraints.CENTER;
        panel.add(title, c);

        c.gridwidth = 1; c.insets = new Insets(8, 8, 6, 8);
        c.gridx = 0; c.gridy = 1; c.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("Username:"), c);
        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        panel.add(userField, c);

        c.insets = new Insets(6, 8, 6, 8);
        c.gridx = 0; c.gridy = 2; c.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("Password:"), c);
        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        panel.add(passField, c);

        JButton loginBtn    = new JButton("Log In");
        JButton registerBtn = new JButton("Create Account");
        JButton cancelBtn   = new JButton("Cancel");

        loginBtn.setBackground(new Color(10, 60, 120));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        loginBtn.setPreferredSize(new Dimension(90, 28));

        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(12, 8, 4, 8);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        btnRow.add(loginBtn);
        btnRow.add(cancelBtn);
        panel.add(btnRow, c);

        c.gridy = 4; c.insets = new Insets(0, 8, 4, 8);
        JPanel regRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        JLabel regLabel = new JLabel("Don't have an account?  ");
        regLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        registerBtn.setBorderPainted(false);
        registerBtn.setContentAreaFilled(false);
        registerBtn.setForeground(new Color(10, 60, 120));
        registerBtn.setFont(new Font("SansSerif", Font.BOLD, 11));
        registerBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        regRow.add(regLabel);
        regRow.add(registerBtn);
        panel.add(regRow, c);

        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setForeground(Color.RED);
        c.gridy = 5; c.insets = new Insets(0, 8, 8, 8);
        panel.add(statusLabel, c);

        loginBtn.addActionListener(e -> login());
        cancelBtn.addActionListener(e -> dispose());
        registerBtn.addActionListener(e -> openRegister());
        passField.addActionListener(e -> login());
        getRootPane().setDefaultButton(loginBtn);

        setContentPane(panel);
    }

    private void login() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Enter username and password."); return;
        }

        try (PreparedStatement ps = DBConnection.get().prepareStatement(
                "SELECT a.PassengerID, a.PasswordHash, CONCAT(p.FirstName,' ',p.LastName) " +
                "FROM PassengerAccount a JOIN Passenger p ON a.PassengerID = p.PassengerID " +
                "WHERE a.Username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                statusLabel.setText("Invalid username or password."); return;
            }
            String storedHash = rs.getString(2);
            if (!PasswordUtil.verify(password, storedHash)) {
                statusLabel.setText("Invalid username or password."); return;
            }
            passengerId   = rs.getInt(1);
            passengerName = rs.getString(3);
            dispose();
        } catch (SQLException e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void openRegister() {
        RegisterDialog reg = new RegisterDialog(this);
        reg.setVisible(true);
        if (reg.wasSuccessful()) {
            passengerId   = reg.getPassengerId();
            // Re-fetch name
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT CONCAT(FirstName,' ',LastName) FROM Passenger WHERE PassengerID=?")) {
                ps.setInt(1, passengerId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) passengerName = rs.getString(1);
            } catch (SQLException ignored) {}
            dispose();
        }
    }

    public int    getPassengerId()   { return passengerId; }
    public String getPassengerName() { return passengerName; }
    public boolean isLoggedIn()      { return passengerId > 0; }
}

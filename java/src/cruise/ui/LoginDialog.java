package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import java.awt.*;

public class LoginDialog extends JDialog {

    private boolean authenticated = false;
    private final JPasswordField passField = new JPasswordField(16);

    public LoginDialog(Frame parent) {
        super(parent, "CruiseMS — Login", true);
        buildUI();
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void buildUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);

        JLabel title = new JLabel("Cruise Ship Management System");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.anchor = GridBagConstraints.CENTER;
        panel.add(title, c);

        JLabel sub = new JLabel("COSC 457 — Database Management Systems");
        sub.setForeground(Color.GRAY);
        c.gridy = 1;
        panel.add(sub, c);

        c.gridy = 2; c.gridwidth = 1; c.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("Password:"), c);
        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        panel.add(passField, c);

        JButton loginBtn  = new JButton("Login");
        JButton cancelBtn = new JButton("Exit");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        btns.add(loginBtn);
        btns.add(cancelBtn);
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; c.anchor = GridBagConstraints.CENTER;
        panel.add(btns, c);

        loginBtn.addActionListener(e -> attemptLogin());
        cancelBtn.addActionListener(e -> dispose());
        passField.addActionListener(e -> attemptLogin());
        getRootPane().setDefaultButton(loginBtn);
        add(panel);
    }

    private void attemptLogin() {
        if (new String(passField.getPassword()).equals(DBConnection.adminPassword())) {
            authenticated = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Incorrect password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            passField.setText("");
            passField.requestFocus();
        }
    }

    public boolean isAuthenticated() { return authenticated; }
}

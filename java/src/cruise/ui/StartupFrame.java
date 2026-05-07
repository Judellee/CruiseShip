package cruise.ui;

import cruise.db.DBConnection;
import cruise.util.PasswordUtil;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class StartupFrame extends JFrame {

    private static final String CARD_HOME     = "home";
    private static final String CARD_PAXLOGIN = "pax-login";
    private static final String CARD_REGISTER = "register";
    private static final String CARD_STAFF    = "staff";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel     cardPanel  = new JPanel(cardLayout);

    public StartupFrame() {
        super("Cruise Ship Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(520, 500);
        setResizable(false);
        setLocationRelativeTo(null);

        cardPanel.add(buildHomeCard(),     CARD_HOME);
        cardPanel.add(buildPaxLoginCard(), CARD_PAXLOGIN);
        cardPanel.add(buildRegisterCard(), CARD_REGISTER);
        cardPanel.add(buildStaffCard(),    CARD_STAFF);

        setContentPane(cardPanel);
        cardLayout.show(cardPanel, CARD_HOME);
    }

    // ── Home card ─────────────────────────────────────────────────────────────

    private JPanel buildHomeCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(10, 60, 120));

        JPanel header = new JPanel(new GridBagLayout());
        header.setBackground(new Color(10, 60, 120));
        header.setBorder(BorderFactory.createEmptyBorder(30, 20, 10, 20));

        JLabel title = new JLabel("Cruise Ship Management System");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("COSC 457 — Database Management Systems");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(new Color(180, 210, 255));

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0; c.anchor = GridBagConstraints.CENTER;
        header.add(title, c);
        c.gridy = 1; c.insets = new Insets(4, 0, 0, 0);
        header.add(sub, c);

        JPanel btnArea = new JPanel(new GridBagLayout());
        btnArea.setBackground(new Color(10, 60, 120));
        btnArea.setBorder(BorderFactory.createEmptyBorder(20, 40, 30, 40));

        JButton passengerBtn = makeNavButton("Passenger Portal",
                "Browse voyages, book a cruise, or manage your reservation",
                new Color(255, 255, 255), new Color(10, 60, 120));
        JButton staffBtn = makeNavButton("Staff Login",
                "Admin access to manage ships, employees, and reservations",
                new Color(30, 90, 160), Color.WHITE);

        GridBagConstraints bc = new GridBagConstraints();
        bc.fill = GridBagConstraints.HORIZONTAL;
        bc.weightx = 1;
        bc.insets = new Insets(8, 8, 8, 8);
        bc.gridy = 0; btnArea.add(passengerBtn, bc);
        bc.gridy = 1; btnArea.add(staffBtn,     bc);

        JLabel footer = new JLabel("  COSC 457 Database Management Systems", SwingConstants.LEFT);
        footer.setForeground(new Color(140, 170, 210));
        footer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        footer.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        footer.setBackground(new Color(5, 40, 90));
        footer.setOpaque(true);

        root.add(header,  BorderLayout.NORTH);
        root.add(btnArea, BorderLayout.CENTER);
        root.add(footer,  BorderLayout.SOUTH);

        passengerBtn.addActionListener(e -> cardLayout.show(cardPanel, CARD_PAXLOGIN));
        staffBtn.addActionListener(e     -> cardLayout.show(cardPanel, CARD_STAFF));

        return root;
    }

    // ── Passenger login card ──────────────────────────────────────────────────

    private final JTextField     paxUserField = new JTextField(18);
    private final JPasswordField paxPassField = new JPasswordField(18);
    private final JLabel         paxStatus    = new JLabel(" ");

    private JPanel buildPaxLoginCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(10, 60, 120));
        root.add(buildDarkHeader("Passenger Portal"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(28, 40, 20, 40));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 8, 7, 8);

        addFormRow(form, c, 0, "Username:", paxUserField);
        addFormRow(form, c, 1, "Password:", paxPassField);

        JButton loginBtn = primaryButton("Log In");
        JButton backBtn  = new JButton("← Back");
        backBtn.setFocusPainted(false);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(14, 8, 4, 8);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        btnRow.setBackground(Color.WHITE);
        btnRow.add(loginBtn); btnRow.add(backBtn);
        form.add(btnRow, c);

        JLabel regPrompt = new JLabel("Don't have an account?");
        regPrompt.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JButton regLink = linkButton("Create one here");

        c.gridy = 3; c.insets = new Insets(2, 8, 4, 8);
        JPanel regRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        regRow.setBackground(Color.WHITE);
        regRow.add(regPrompt); regRow.add(regLink);
        form.add(regRow, c);

        paxStatus.setFont(new Font("SansSerif", Font.PLAIN, 11));
        paxStatus.setForeground(Color.RED);
        c.gridy = 4; c.insets = new Insets(0, 8, 8, 8);
        form.add(paxStatus, c);

        loginBtn.addActionListener(e -> attemptPaxLogin());
        paxPassField.addActionListener(e -> attemptPaxLogin());
        backBtn.addActionListener(e -> { clearPaxLogin(); cardLayout.show(cardPanel, CARD_HOME); });
        regLink.addActionListener(e -> cardLayout.show(cardPanel, CARD_REGISTER));

        root.add(form, BorderLayout.CENTER);
        return root;
    }

    private void attemptPaxLogin() {
        String username = paxUserField.getText().trim();
        String password = new String(paxPassField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            paxStatus.setText("Enter username and password."); return;
        }
        try (PreparedStatement ps = DBConnection.get().prepareStatement(
                "SELECT a.PassengerID, a.PasswordHash, CONCAT(p.FirstName,' ',p.LastName) " +
                "FROM PassengerAccount a JOIN Passenger p ON a.PassengerID = p.PassengerID " +
                "WHERE a.Username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (!rs.next() || !PasswordUtil.verify(password, rs.getString(2))) {
                paxStatus.setText("Invalid username or password."); return;
            }
            int pid = rs.getInt(1); String name = rs.getString(3);
            clearPaxLogin();
            new PassengerFrame(pid, name).setVisible(true);
            dispose();
        } catch (SQLException e) {
            paxStatus.setText("Error: " + e.getMessage());
        }
    }

    private void clearPaxLogin() {
        paxUserField.setText(""); paxPassField.setText(""); paxStatus.setText(" ");
    }

    // ── Register card ─────────────────────────────────────────────────────────

    private final JTextField     regFirst   = new JTextField(18);
    private final JTextField     regLast    = new JTextField(18);
    private final JTextField     regEmail   = new JTextField(18);
    private final JTextField     regPhone   = new JTextField(18);
    private final JTextField     regUser    = new JTextField(18);
    private final JPasswordField regPass    = new JPasswordField(18);
    private final JPasswordField regConf    = new JPasswordField(18);
    private final JLabel         regStatus  = new JLabel(" ");

    private JPanel buildRegisterCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(10, 60, 120));
        root.add(buildDarkHeader("Create Account"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(12, 40, 8, 40));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 8, 4, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        addFormRow(form, c, 0, "First Name *:", regFirst);
        addFormRow(form, c, 1, "Last Name *:",  regLast);
        addFormRow(form, c, 2, "Email:",        regEmail);
        addFormRow(form, c, 3, "Phone:",        regPhone);
        addFormRow(form, c, 4, "Username *:",   regUser);
        addFormRow(form, c, 5, "Password *:",   regPass);
        addFormRow(form, c, 6, "Confirm *:",    regConf);

        JButton createBtn = primaryButton("Create Account");
        JButton backBtn   = new JButton("← Back to Login");
        backBtn.setFocusPainted(false);

        c.gridx = 0; c.gridy = 7; c.gridwidth = 2; c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(12, 8, 4, 8);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        btnRow.setBackground(Color.WHITE);
        btnRow.add(createBtn); btnRow.add(backBtn);
        form.add(btnRow, c);

        regStatus.setFont(new Font("SansSerif", Font.PLAIN, 11));
        regStatus.setForeground(Color.RED);
        c.gridy = 8; c.insets = new Insets(0, 8, 6, 8);
        form.add(regStatus, c);

        createBtn.addActionListener(e -> attemptRegister());
        backBtn.addActionListener(e -> { clearRegister(); cardLayout.show(cardPanel, CARD_PAXLOGIN); });

        root.add(form, BorderLayout.CENTER);
        return root;
    }

    private void attemptRegister() {
        String first    = regFirst.getText().trim();
        String last     = regLast.getText().trim();
        String email    = regEmail.getText().trim();
        String phone    = regPhone.getText().trim();
        String username = regUser.getText().trim();
        String password = new String(regPass.getPassword());
        String confirm  = new String(regConf.getPassword());

        if (first.isEmpty() || last.isEmpty()) { regStatus.setText("First and last name required."); return; }
        if (username.isEmpty() || password.isEmpty()) { regStatus.setText("Username and password required."); return; }
        if (password.length() < 6) { regStatus.setText("Password must be at least 6 characters."); return; }
        if (!password.equals(confirm)) { regStatus.setText("Passwords do not match."); return; }

        try {
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT COUNT(*) FROM PassengerAccount WHERE Username=?")) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery(); rs.next();
                if (rs.getInt(1) > 0) { regStatus.setText("Username already taken."); return; }
            }
            int pid;
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT INTO Passenger (FirstName, LastName, Email, Phone) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, first); ps.setString(2, last);
                ps.setString(3, email.isEmpty() ? null : email);
                ps.setString(4, phone.isEmpty() ? null : phone);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) throw new SQLException("Failed to create passenger.");
                pid = keys.getInt(1);
            }
            try (PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT INTO PassengerAccount (Username, PasswordHash, PassengerID) VALUES (?,?,?)")) {
                ps.setString(1, username);
                ps.setString(2, PasswordUtil.hash(password));
                ps.setInt(3, pid);
                ps.executeUpdate();
            }
            clearRegister();
            new PassengerFrame(pid, first + " " + last).setVisible(true);
            dispose();
        } catch (SQLException e) {
            regStatus.setText("Error: " + e.getMessage());
        }
    }

    private void clearRegister() {
        regFirst.setText(""); regLast.setText(""); regEmail.setText("");
        regPhone.setText(""); regUser.setText("");
        regPass.setText("");  regConf.setText(""); regStatus.setText(" ");
    }

    // ── Staff login card ──────────────────────────────────────────────────────

    private final JPasswordField staffPassField = new JPasswordField(18);
    private final JLabel         staffStatus    = new JLabel(" ");

    private JPanel buildStaffCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(10, 60, 120));
        root.add(buildDarkHeader("Staff Login"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(40, 50, 30, 50));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);

        addFormRow(form, c, 0, "Admin Password:", staffPassField);

        JButton loginBtn = primaryButton("Log In");
        JButton backBtn  = new JButton("← Back");
        backBtn.setFocusPainted(false);

        c.gridx = 0; c.gridy = 1; c.gridwidth = 2; c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(16, 8, 4, 8);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        btnRow.setBackground(Color.WHITE);
        btnRow.add(loginBtn); btnRow.add(backBtn);
        form.add(btnRow, c);

        staffStatus.setFont(new Font("SansSerif", Font.PLAIN, 11));
        staffStatus.setForeground(Color.RED);
        c.gridy = 2; c.insets = new Insets(0, 8, 8, 8);
        form.add(staffStatus, c);

        loginBtn.addActionListener(e -> attemptStaffLogin());
        staffPassField.addActionListener(e -> attemptStaffLogin());
        backBtn.addActionListener(e -> { staffPassField.setText(""); staffStatus.setText(" ");
                                         cardLayout.show(cardPanel, CARD_HOME); });

        root.add(form, BorderLayout.CENTER);
        return root;
    }

    private void attemptStaffLogin() {
        if (new String(staffPassField.getPassword()).equals(DBConnection.adminPassword())) {
            staffPassField.setText(""); staffStatus.setText(" ");
            new MainFrame().setVisible(true);
            dispose();
        } else {
            staffStatus.setText("Incorrect password.");
            staffPassField.setText("");
            staffPassField.requestFocus();
        }
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private JPanel buildDarkHeader(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 14));
        p.setBackground(new Color(10, 60, 120));
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 17));
        lbl.setForeground(Color.WHITE);
        p.add(lbl);
        return p;
    }

    private void addFormRow(JPanel p, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridwidth = 1;
        c.gridx = 0; c.gridy = row; c.anchor = GridBagConstraints.EAST;
        p.add(new JLabel(label), c);
        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        p.add(field, c);
    }

    private JButton primaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(10, 60, 120));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        return btn;
    }

    private JButton linkButton(String text) {
        JButton btn = new JButton(text);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setForeground(new Color(10, 60, 120));
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        return btn;
    }

    private JButton makeNavButton(String title, String subtitle, Color bg, Color fg) {
        JButton btn = new JButton("<html><b>" + title + "</b><br>"
                + "<span style='font-size:10px;'>" + subtitle + "</span></html>");
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 60), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return btn;
    }
}

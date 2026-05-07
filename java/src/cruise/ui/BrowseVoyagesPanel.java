package cruise.ui;

import cruise.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class BrowseVoyagesPanel extends JPanel {

    private final DefaultTableModel model;
    private final JTable table;
    private final JTextArea detailArea = new JTextArea();

    public BrowseVoyagesPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel heading = new JLabel("Available Voyages");
        heading.setFont(new Font("SansSerif", Font.BOLD, 15));

        String[] cols = {"ID", "Ship", "Itinerary", "Season", "Departure", "Return", "Capacity"};
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(22);
        table.getColumnModel().getColumn(0).setMaxWidth(50);

        detailArea.setEditable(false);
        detailArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        detailArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        detailArea.setText("Select a voyage above to see its ports of call and available excursions.");

        JScrollPane tableScroll  = new JScrollPane(table);
        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.setBorder(BorderFactory.createTitledBorder("Voyage Details"));
        detailScroll.setPreferredSize(new Dimension(0, 180));

        JButton refreshBtn = new JButton("Refresh");
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.add(refreshBtn);

        add(heading,      BorderLayout.NORTH);
        add(tableScroll,  BorderLayout.CENTER);
        add(detailScroll, BorderLayout.SOUTH);

        loadData();

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showDetail();
        });
        refreshBtn.addActionListener(e -> loadData());
    }

    private void loadData() {
        model.setRowCount(0);
        try (Statement st = DBConnection.get().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT v.VoyageID, s.ShipName, i.ItineraryName, se.SeasonName, " +
                "       v.DepartureDate, v.ReturnDate, s.Capacity " +
                "FROM Voyage v " +
                "JOIN Ship s      ON v.ShipID      = s.ShipID " +
                "JOIN Itinerary i ON v.ItineraryID = i.ItineraryID " +
                "JOIN Season se   ON v.SeasonID    = se.SeasonID " +
                "ORDER BY v.DepartureDate")) {
            while (rs.next())
                model.addRow(new Object[]{
                        rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getInt(7)});
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showDetail() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int voyageId    = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
        int itinModelRow = table.convertRowIndexToModel(row);

        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(model.getValueAt(itinModelRow, 2)).append(" ===\n");
        sb.append("Ship: ").append(model.getValueAt(itinModelRow, 1))
          .append("   Season: ").append(model.getValueAt(itinModelRow, 3)).append("\n");
        sb.append("Departure: ").append(model.getValueAt(itinModelRow, 4))
          .append("   Return: ").append(model.getValueAt(itinModelRow, 5)).append("\n\n");

        // Get itinerary ID for this voyage
        try (PreparedStatement ps = DBConnection.get().prepareStatement(
                "SELECT ItineraryID FROM Voyage WHERE VoyageID=?")) {
            ps.setInt(1, voyageId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int itinId = rs.getInt(1);

                // Ports of call
                sb.append("PORTS OF CALL:\n");
                try (PreparedStatement ps2 = DBConnection.get().prepareStatement(
                        "SELECT st.StopOrder, p.PortName, p.Country " +
                        "FROM Stop st JOIN Port p ON st.PortID = p.PortID " +
                        "WHERE st.ItineraryID=? ORDER BY st.StopOrder")) {
                    ps2.setInt(1, itinId);
                    ResultSet rs2 = ps2.executeQuery();
                    while (rs2.next())
                        sb.append("  ").append(rs2.getInt(1)).append(". ")
                          .append(rs2.getString(2)).append(", ").append(rs2.getString(3)).append("\n");
                }

                // Excursions
                sb.append("\nAVAILABLE EXCURSIONS:\n");
                try (PreparedStatement ps3 = DBConnection.get().prepareStatement(
                        "SELECT e.ExcursionName, p.PortName, e.Price " +
                        "FROM Excursion e JOIN Port p ON e.PortID = p.PortID " +
                        "WHERE e.PortID IN (SELECT PortID FROM Stop WHERE ItineraryID=?) " +
                        "ORDER BY p.PortName, e.ExcursionName")) {
                    ps3.setInt(1, itinId);
                    ResultSet rs3 = ps3.executeQuery();
                    boolean any = false;
                    while (rs3.next()) {
                        sb.append("  • ").append(rs3.getString(1))
                          .append(" @ ").append(rs3.getString(2))
                          .append("  —  $").append(String.format("%.2f", rs3.getDouble(3))).append("\n");
                        any = true;
                    }
                    if (!any) sb.append("  No excursions listed.\n");
                }
            }
        } catch (SQLException e) {
            sb.append("\n[Error loading details: ").append(e.getMessage()).append("]");
        }

        detailArea.setText(sb.toString());
        detailArea.setCaretPosition(0);
    }
}

package dao;

import model.ParkingSlot;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ParkingSlotDAO {

    private ParkingSlot map(ResultSet rs) throws SQLException {
        return new ParkingSlot(
            rs.getInt("slot_id"),
            rs.getString("slot_number"),
            rs.getString("slot_type"),
            rs.getString("user_category"),
            rs.getBoolean("is_occupied"));
    }

    public List<ParkingSlot> getAll() throws SQLException {
        List<ParkingSlot> list = new ArrayList<>();
        String sql = "SELECT slot_id, slot_number, slot_type, user_category, is_occupied "
                   + "FROM parking_slots ORDER BY slot_id";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    // First free slot of the right category + type. Used inside a transaction;
    // FOR UPDATE locks the row so two people can't take the same slot.
    public ParkingSlot findFirstAvailable(Connection con, String category, String type) throws SQLException {
        String sql = "SELECT slot_id, slot_number, slot_type, user_category, is_occupied "
                   + "FROM parking_slots "
                   + "WHERE user_category = ? AND slot_type = ? AND is_occupied = FALSE "
                   + "ORDER BY slot_id LIMIT 1 FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, category);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void setOccupied(Connection con, int slotId, boolean occupied) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE parking_slots SET is_occupied = ? WHERE slot_id = ?")) {
            ps.setBoolean(1, occupied);
            ps.setInt(2, slotId);
            ps.executeUpdate();
        }
    }

    // Each row: {category, type, total, occupied, free}
    public List<String[]> getSummary() throws SQLException {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT user_category, slot_type, COUNT(*) AS total, "
                   + "SUM(is_occupied) AS occupied "
                   + "FROM parking_slots GROUP BY user_category, slot_type "
                   + "ORDER BY user_category DESC, slot_type";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int total = rs.getInt("total");
                int occ = rs.getInt("occupied");
                list.add(new String[] {
                    rs.getString("user_category"), rs.getString("slot_type"),
                    String.valueOf(total), String.valueOf(occ), String.valueOf(total - occ)
                });
            }
        }
        return list;
    }
}

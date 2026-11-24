package dao;

import model.ParkingRecord;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ParkingRecordDAO {

    private static final String BASE =
        "SELECT r.record_id, r.vehicle_id, r.slot_id, v.vehicle_number, s.slot_number, "
      + "r.entry_time, r.exit_time "
      + "FROM parking_records r "
      + "JOIN vehicles v ON r.vehicle_id = v.vehicle_id "
      + "JOIN parking_slots s ON r.slot_id = s.slot_id ";

    private ParkingRecord map(ResultSet rs) throws SQLException {
        Timestamp exit = rs.getTimestamp("exit_time");
        return new ParkingRecord(
            rs.getInt("record_id"),
            rs.getInt("vehicle_id"),
            rs.getInt("slot_id"),
            rs.getTimestamp("entry_time").toLocalDateTime(),
            exit == null ? null : exit.toLocalDateTime(),
            rs.getString("vehicle_number"),
            rs.getString("slot_number"));
    }

    public List<ParkingRecord> getAll() throws SQLException {
        List<ParkingRecord> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(BASE + "ORDER BY r.record_id DESC")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<ParkingRecord> searchByVehicle(String keyword) throws SQLException {
        List<ParkingRecord> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 BASE + "WHERE v.vehicle_number LIKE ? ORDER BY r.record_id DESC")) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // Records whose entry time falls between the two dates (both inclusive)
    public List<ParkingRecord> filterByDate(LocalDate from, LocalDate to) throws SQLException {
        List<ParkingRecord> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 BASE + "WHERE r.entry_time >= ? AND r.entry_time < ? ORDER BY r.record_id DESC")) {
            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // The open record (exit_time IS NULL) of a vehicle, or null if it is not parked
    public ParkingRecord findOpenByVehicle(Connection con, int vehicleId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                BASE + "WHERE r.vehicle_id = ? AND r.exit_time IS NULL")) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean isParked(int vehicleId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            return findOpenByVehicle(con, vehicleId) != null;
        }
    }

    public void insert(Connection con, int vehicleId, int slotId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO parking_records (vehicle_id, slot_id) VALUES (?, ?)")) {
            ps.setInt(1, vehicleId);
            ps.setInt(2, slotId);
            ps.executeUpdate();
        }
    }

    public void close(Connection con, int recordId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE parking_records SET exit_time = CURRENT_TIMESTAMP WHERE record_id = ?")) {
            ps.setInt(1, recordId);
            ps.executeUpdate();
        }
    }
}

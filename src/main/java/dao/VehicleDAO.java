package dao;

import model.Vehicle;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class VehicleDAO {

    private static final String COLS =
        "vehicle_id, vehicle_number, vehicle_type, owner_category, owner_name, user_id";

    private Vehicle map(ResultSet rs) throws SQLException {
        return new Vehicle(
            rs.getInt("vehicle_id"),
            rs.getString("vehicle_number"),
            rs.getString("vehicle_type"),
            rs.getString("owner_category"),
            rs.getString("owner_name"),
            rs.getInt("user_id"));          // NULL -> 0
    }

    private List<Vehicle> query(String sql, String... params) throws SQLException {
        List<Vehicle> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<Vehicle> getAll() throws SQLException {
        return query("SELECT " + COLS + " FROM vehicles ORDER BY vehicle_id");
    }

    public List<Vehicle> search(String keyword) throws SQLException {
        String like = "%" + keyword + "%";
        return query("SELECT " + COLS + " FROM vehicles "
                   + "WHERE vehicle_number LIKE ? OR owner_name LIKE ? ORDER BY vehicle_id",
                   like, like);
    }

    // Vehicles that are currently inside the parking (open record)
    public List<Vehicle> getParked() throws SQLException {
        return query("SELECT " + COLS + " FROM vehicles WHERE vehicle_id IN "
                   + "(SELECT vehicle_id FROM parking_records WHERE exit_time IS NULL) "
                   + "ORDER BY vehicle_number");
    }

    // Vehicles that are registered but not parked right now
    public List<Vehicle> getNotParked() throws SQLException {
        return query("SELECT " + COLS + " FROM vehicles WHERE vehicle_id NOT IN "
                   + "(SELECT vehicle_id FROM parking_records WHERE exit_time IS NULL) "
                   + "ORDER BY vehicle_number");
    }

    public Vehicle findById(int id) throws SQLException {
        String sql = "SELECT " + COLS + " FROM vehicles WHERE vehicle_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean existsByNumber(String number, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM vehicles WHERE vehicle_number = ? AND vehicle_id <> ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, number);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public void add(Vehicle v) throws SQLException {
        String sql = "INSERT INTO vehicles (vehicle_number, vehicle_type, owner_category, owner_name, user_id) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, v.getVehicleNumber());
            ps.setString(2, v.getVehicleType());
            ps.setString(3, v.getOwnerCategory());
            ps.setString(4, v.getOwnerName());
            if (v.getUserId() == 0) ps.setNull(5, Types.INTEGER);
            else ps.setInt(5, v.getUserId());
            ps.executeUpdate();
        }
    }

    public void update(Vehicle v) throws SQLException {
        String sql = "UPDATE vehicles SET vehicle_number = ?, vehicle_type = ?, "
                   + "owner_category = ?, owner_name = ? WHERE vehicle_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, v.getVehicleNumber());
            ps.setString(2, v.getVehicleType());
            ps.setString(3, v.getOwnerCategory());
            ps.setString(4, v.getOwnerName());
            ps.setInt(5, v.getVehicleId());
            ps.executeUpdate();
        }
    }

    public void delete(int vehicleId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM vehicles WHERE vehicle_id = ?")) {
            ps.setInt(1, vehicleId);
            ps.executeUpdate();
        }
    }
}

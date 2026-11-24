package service;

import dao.ParkingRecordDAO;
import dao.VehicleDAO;
import model.Vehicle;

import java.sql.SQLException;
import java.util.List;

public class VehicleService {
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final ParkingRecordDAO recordDAO = new ParkingRecordDAO();

    // Cleans and checks the input. Throws IllegalArgumentException with a user-friendly message.
    private void validate(Vehicle v) {
        if (v.getVehicleNumber() == null || v.getVehicleNumber().isBlank())
            throw new IllegalArgumentException("Vehicle number is required");
        v.setVehicleNumber(v.getVehicleNumber().trim().toUpperCase());
        if (!v.getVehicleNumber().matches("[A-Z0-9 -]{4,15}"))
            throw new IllegalArgumentException(
                "Vehicle number must be 4-15 characters (letters, digits, space or -)");
        if (v.getOwnerName() == null || v.getOwnerName().isBlank())
            throw new IllegalArgumentException("Owner name is required");
        v.setOwnerName(v.getOwnerName().trim());
    }

    public void addVehicle(Vehicle v) throws SQLException {
        validate(v);
        if (vehicleDAO.existsByNumber(v.getVehicleNumber(), 0))
            throw new IllegalArgumentException("This vehicle number is already registered");
        vehicleDAO.add(v);
    }

    public void updateVehicle(Vehicle v) throws SQLException {
        validate(v);
        Vehicle old = vehicleDAO.findById(v.getVehicleId());
        if (old == null) throw new IllegalArgumentException("Vehicle not found");
        if (vehicleDAO.existsByNumber(v.getVehicleNumber(), v.getVehicleId()))
            throw new IllegalArgumentException("This vehicle number is already registered");

        boolean changedGroup = !old.getVehicleType().equals(v.getVehicleType())
                            || !old.getOwnerCategory().equals(v.getOwnerCategory());
        if (changedGroup && recordDAO.isParked(v.getVehicleId()))
            throw new IllegalStateException(
                "Vehicle is parked right now - release it before changing type or category");
        vehicleDAO.update(v);
    }

    public void deleteVehicle(int vehicleId) throws SQLException {
        if (recordDAO.isParked(vehicleId))
            throw new IllegalStateException("Vehicle is parked right now - release it first");
        vehicleDAO.delete(vehicleId);
    }

    public List<Vehicle> getAllVehicles() throws SQLException {
        return vehicleDAO.getAll();
    }

    public List<Vehicle> searchVehicle(String keyword) throws SQLException {
        if (keyword == null || keyword.isBlank()) return vehicleDAO.getAll();
        return vehicleDAO.search(keyword.trim());
    }
}

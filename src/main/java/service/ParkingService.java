package service;

import dao.ParkingRecordDAO;
import dao.ParkingSlotDAO;
import dao.VehicleDAO;
import model.ParkingRecord;
import model.ParkingSlot;
import model.Vehicle;
import util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ParkingService {
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();
    private final ParkingRecordDAO recordDAO = new ParkingRecordDAO();

    // Parks the vehicle in the first free slot of its own category + type.
    // Returns the slot number. Slot update + record insert happen in ONE transaction.
    public String parkVehicle(int vehicleId) throws SQLException {
        Vehicle v = vehicleDAO.findById(vehicleId);
        if (v == null) throw new IllegalArgumentException("Vehicle not found");

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (recordDAO.findOpenByVehicle(con, vehicleId) != null)
                    throw new IllegalStateException("Vehicle is already parked");

                ParkingSlot slot = slotDAO.findFirstAvailable(con, v.getOwnerCategory(), v.getVehicleType());
                if (slot == null)
                    throw new IllegalStateException("No free slot left for "
                        + v.getOwnerCategory() + " " + v.getVehicleType());

                slotDAO.setOccupied(con, slot.getSlotId(), true);
                recordDAO.insert(con, vehicleId, slot.getSlotId());
                con.commit();
                return slot.getSlotNumber();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    // Releases the vehicle: closes its record and frees the slot. Returns the slot number.
    public String exitVehicle(int vehicleId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                ParkingRecord rec = recordDAO.findOpenByVehicle(con, vehicleId);
                if (rec == null)
                    throw new IllegalStateException("Vehicle is not parked");

                recordDAO.close(con, rec.getRecordId());
                slotDAO.setOccupied(con, rec.getSlotId(), false);
                con.commit();
                return rec.getSlotNumber();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    public List<ParkingSlot> getAllSlots() throws SQLException {
        return slotDAO.getAll();
    }

    public List<Vehicle> getParkedVehicles() throws SQLException {
        return vehicleDAO.getParked();
    }

    public List<Vehicle> getUnparkedVehicles() throws SQLException {
        return vehicleDAO.getNotParked();
    }

    public List<String[]> getSummary() throws SQLException {
        return slotDAO.getSummary();
    }

    public List<ParkingRecord> getRecords() throws SQLException {
        return recordDAO.getAll();
    }

    public List<ParkingRecord> searchRecords(String keyword) throws SQLException {
        if (keyword == null || keyword.isBlank()) return recordDAO.getAll();
        return recordDAO.searchByVehicle(keyword.trim());
    }

    public List<ParkingRecord> filterRecords(LocalDate from, LocalDate to) throws SQLException {
        if (from.isAfter(to)) throw new IllegalArgumentException("'From' date is after 'To' date");
        return recordDAO.filterByDate(from, to);
    }
}

package model;

import java.time.LocalDateTime;

public class ParkingRecord {
    private int recordId;
    private int vehicleId;
    private int slotId;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;     // null = still parked
    private String vehicleNumber;       // from join, for display
    private String slotNumber;          // from join, for display

    public ParkingRecord() {}

    public ParkingRecord(int recordId, int vehicleId, int slotId, LocalDateTime entryTime,
                         LocalDateTime exitTime, String vehicleNumber, String slotNumber) {
        this.recordId = recordId;
        this.vehicleId = vehicleId;
        this.slotId = slotId;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.vehicleNumber = vehicleNumber;
        this.slotNumber = slotNumber;
    }

    public int getRecordId()              { return recordId; }
    public int getVehicleId()             { return vehicleId; }
    public int getSlotId()                { return slotId; }
    public LocalDateTime getEntryTime()   { return entryTime; }
    public LocalDateTime getExitTime()    { return exitTime; }
    public String getVehicleNumber()      { return vehicleNumber; }
    public String getSlotNumber()         { return slotNumber; }

    public void setRecordId(int v)             { this.recordId = v; }
    public void setVehicleId(int v)            { this.vehicleId = v; }
    public void setSlotId(int v)               { this.slotId = v; }
    public void setEntryTime(LocalDateTime v)  { this.entryTime = v; }
    public void setExitTime(LocalDateTime v)   { this.exitTime = v; }
    public void setVehicleNumber(String v)     { this.vehicleNumber = v; }
    public void setSlotNumber(String v)        { this.slotNumber = v; }
}

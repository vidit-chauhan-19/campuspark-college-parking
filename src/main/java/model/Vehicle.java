package model;

public class Vehicle {
    private int vehicleId;
    private String vehicleNumber;
    private String vehicleType;     // 2-WHEELER / 4-WHEELER
    private String ownerCategory;   // STUDENT / FACULTY
    private String ownerName;
    private int userId;             // 0 = not linked to a user

    public Vehicle() {}

    public Vehicle(int vehicleId, String vehicleNumber, String vehicleType,
                   String ownerCategory, String ownerName, int userId) {
        this.vehicleId = vehicleId;
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
        this.ownerCategory = ownerCategory;
        this.ownerName = ownerName;
        this.userId = userId;
    }

    public int getVehicleId()         { return vehicleId; }
    public String getVehicleNumber()  { return vehicleNumber; }
    public String getVehicleType()    { return vehicleType; }
    public String getOwnerCategory()  { return ownerCategory; }
    public String getOwnerName()      { return ownerName; }
    public int getUserId()            { return userId; }

    public void setVehicleId(int v)           { this.vehicleId = v; }
    public void setVehicleNumber(String v)    { this.vehicleNumber = v; }
    public void setVehicleType(String v)      { this.vehicleType = v; }
    public void setOwnerCategory(String v)    { this.ownerCategory = v; }
    public void setOwnerName(String v)        { this.ownerName = v; }
    public void setUserId(int v)              { this.userId = v; }
}

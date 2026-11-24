package model;

public class ParkingSlot {
    private int slotId;
    private String slotNumber;
    private String slotType;       // 2-WHEELER / 4-WHEELER
    private String userCategory;   // STUDENT / FACULTY
    private boolean occupied;

    public ParkingSlot() {}

    public ParkingSlot(int slotId, String slotNumber, String slotType,
                       String userCategory, boolean occupied) {
        this.slotId = slotId;
        this.slotNumber = slotNumber;
        this.slotType = slotType;
        this.userCategory = userCategory;
        this.occupied = occupied;
    }

    public int getSlotId()           { return slotId; }
    public String getSlotNumber()    { return slotNumber; }
    public String getSlotType()      { return slotType; }
    public String getUserCategory()  { return userCategory; }
    public boolean isOccupied()      { return occupied; }

    public void setSlotId(int v)           { this.slotId = v; }
    public void setSlotNumber(String v)    { this.slotNumber = v; }
    public void setSlotType(String v)      { this.slotType = v; }
    public void setUserCategory(String v)  { this.userCategory = v; }
    public void setOccupied(boolean v)     { this.occupied = v; }
}

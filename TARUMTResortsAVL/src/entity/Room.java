package entity;

/*
 * Represents a hotel room and its current status.
 * Each room keeps its own status history.
 *
 * Status flow:
 * AVAILABLE -> OCCUPIED -> NEEDS_CLEANING
 * -> CLEANING_IN_PROGRESS -> INSPECTED -> AVAILABLE
 *
 * @author All
 */
public class Room {

    private String roomNumber;
    private String roomType;           
    private double nightlyRate;
    private String status;             
    private RoomHistory roomHistory;   

    public Room(String roomNumber, String roomType, double nightlyRate, String status) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.nightlyRate = nightlyRate;
        this.status = status;
        this.roomHistory = new RoomHistory(roomNumber);
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public double getNightlyRate() {
        return nightlyRate;
    }

    public String getStatus() {
        return status;
    }

    public RoomHistory getRoomHistory() {
        return roomHistory;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return roomNumber + " | " + roomType + " | RM" + nightlyRate + " | " + status;
    }

    // Same room number means the same room. （for hashcode can get same value)
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Room)) {
            return false;
        }
        Room other = (Room) obj;
        return this.roomNumber.equals(other.roomNumber);
    }
// Uses room number to generate the hash code.
    @Override
    public int hashCode() {
        return roomNumber.hashCode();
    }
}
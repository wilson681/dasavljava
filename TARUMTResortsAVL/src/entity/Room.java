package entity;

/**
 * Room.java
 * Entity class -- represents one room in the hotel.
 *
 * Possible values of status (status flow, driven/changed by the Control layer):
 *   AVAILABLE            -- vacant, can be assigned to a guest
 *   OCCUPIED             -- currently has a guest checked in
 *   NEEDS_CLEANING       -- guest has checked out, awaiting cleaning
 *   CLEANING_IN_PROGRESS -- currently being cleaned
 *   INSPECTED            -- inspected, about to become available
 */
public class Room {

    // ========== Data fields ==========
    private String roomNumber;         // room number, uniquely identifies this room
    private String roomType;           // room type: Standard / Deluxe / Suite
    private double nightlyRate;        // fixed room rate, price per night
    private String status;             // current status (see the values listed in the class comment)
    private RoomHistory roomHistory;   // this room's own status-change history (Entity-to-Entity reference, used by Module 3)

    /**
     * Constructor -- a room comes with its own empty RoomHistory as soon as it's
     * created, no need for a separate lookup table.
     */
    public Room(String roomNumber, String roomType, double nightlyRate, String status) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.nightlyRate = nightlyRate;
        this.status = status;
        this.roomHistory = new RoomHistory(roomNumber);
    }

    // ========== Getters ==========
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

    // ========== Setters ==========
    // roomNumber, roomType, and nightlyRate don't change after creation, so no setters
    // are provided; status keeps changing through the check-in/check-out/cleaning flow,
    // updated via calls from the Control layer.

    public void setStatus(String status) {
        this.status = status;
    }

    // ========== Overridden methods ==========

    /**
     * toString: shows a summary of this room on the console.
     */
    @Override
    public String toString() {
        return roomNumber + " | " + roomType + " | RM" + nightlyRate + " | " + status;
    }

    /**
     * equals: two rooms are the same based on room number alone.
     */
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

    /**
     * hashCode: per Java convention, overriding equals() requires overriding hashCode() too.
     */
    @Override
    public int hashCode() {
        return roomNumber.hashCode();
    }
}
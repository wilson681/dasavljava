package entity;

/**
 * Room.java - Entity class representing a resort room.
 *
 * @author Wilson
 */
public class Room {

    private String roomNo;
    private RoomType roomType;
    private double nightlyRate;
    private String status;

    public Room(String roomNo, RoomType roomType, double nightlyRate, String status) {
        this.roomNo = roomNo;
        this.roomType = roomType;
        this.nightlyRate = nightlyRate;
        this.status = status;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public double getNightlyRate() {
        return nightlyRate;
    }

    public void setNightlyRate(double nightlyRate) {
        this.nightlyRate = nightlyRate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Room other = (Room) obj;
        return this.roomNo.equals(other.roomNo);
    }

    @Override
    public int hashCode() {
        return roomNo.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%-6s %-10s %8.2f  %s", roomNo, roomType, nightlyRate, status);
    }
}
package entity;

/**
 * Records a successful room status rollback.
 * Each rollback is stored as a separate log entry.
 *
 * @author All
 */
public class RollbackLogEntry {

    private String roomNumber;
    private String fromStatus;   
    private String toStatus;    
    private String date;

    public RollbackLogEntry(String roomNumber, String fromStatus, String toStatus, String date) {
        this.roomNumber = roomNumber;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.date = date;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getFromStatus() {
        return fromStatus;  
    }

    public String getToStatus() {
        return toStatus;
    }

    public String getDate() {
        return date;
    }

    @Override
    public String toString() {
        return date + " | Room " + roomNumber + " | " + fromStatus + " -> " + toStatus;
    }

// Room, date and previous status are used to identify a rollback.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RollbackLogEntry)) {
            return false;
        }
        RollbackLogEntry other = (RollbackLogEntry) obj;
        return this.roomNumber.equals(other.roomNumber)
                && this.date.equals(other.date)
                && this.fromStatus.equals(other.fromStatus);
    }

    @Override
    public int hashCode() {
        int result = roomNumber.hashCode();
        result = 31 * result + date.hashCode();
        result = 31 * result + fromStatus.hashCode();
        return result;
    }
}

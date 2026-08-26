package entity;

/**
 * RollbackLogEntry.java
 * Entity class -- represents one successful Housekeeping status rollback (undo) event.
 *
 * Notes:
 * - This is a plain data class (POJO), only holds data for one rollback event.
 * - Contains no input (Scanner) or output (System.out) statements, per Entity class rules.
 * - Once a record is popped from the Stack in RoomHistory it's gone, so there's no way
 *   to later count how many times a room was rolled back -- this record fills that gap:
 *   an append-only log, with one new entry added only when a rollback succeeds.
 */
public class RollbackLogEntry {

    // ========== Data fields ==========
    private String roomNumber;   // which room was rolled back
    private String fromStatus;   // status before rollback (the one removed)
    private String toStatus;     // status restored to after rollback
    private String date;         // date the rollback occurred

    /**
     * Constructor.
     */
    public RollbackLogEntry(String roomNumber, String fromStatus, String toStatus, String date) {
        this.roomNumber = roomNumber;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.date = date;
    }

    // ========== Getters ==========
    // Once created, a record should not be modified, so only getters are provided, no setters.

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

    // ========== Overridden methods ==========

    /**
     * toString: shows a summary of this rollback record on the console.
     */
    @Override
    public String toString() {
        return date + " | Room " + roomNumber + " | " + fromStatus + " -> " + toStatus;
    }

    /**
     * equals: two records are the same by comparing room number, date, and from-status
     * together (the same room could be rolled back more than once on the same day, so
     * room number + date alone isn't precise enough).
     */
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

    /**
     * hashCode: per Java convention, overriding equals() requires overriding hashCode() too.
     */
    @Override
    public int hashCode() {
        int result = roomNumber.hashCode();
        result = 31 * result + date.hashCode();
        result = 31 * result + fromStatus.hashCode();
        return result;
    }
}

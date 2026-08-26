package entity;

import adt.LinkedStack;
import adt.StackInterface;

/**
 * RoomHistory.java
 * Entity class -- represents one room's own status-change history.
 *
 * Notes:
 * - This is a plain data class (POJO), wraps a Stack internally that stores this
 *   room's status-change records.
 * - Contains no input (Scanner) or output (System.out) statements, per Entity class rules.
 * - Contains no business logic like whether to undo or what to do after undoing --
 *   that belongs in the Control layer.
 * - When the Control layer performs a rollback, it pushes/pops statusStack directly.
 * - Per business requirements, only the most recent 20 records are kept (the Control
 *   layer checks the count and removes the oldest before pushing a new one; the Entity
 *   itself does not enforce this limit).
 */
public class RoomHistory {

    // ========== Data fields ==========
    private String roomNumber;                    // which room this history belongs to
    private StackInterface<String> statusStack;    // status-change records, the top of the stack is the most recent status

    /**
     * Constructor -- when newly created, this room has no history yet, so statusStack
     * is initialized empty.
     */
    public RoomHistory(String roomNumber) {
        this.roomNumber = roomNumber;
        this.statusStack = new LinkedStack<>();
    }

    // ========== Getters ==========
    public String getRoomNumber() {
        return roomNumber;
    }

    public StackInterface<String> getStatusStack() {
        return statusStack;
    }

    // ========== Overridden methods ==========

    /**
     * toString: shows a summary of this history record on the console.
     */
    @Override
    public String toString() {
        return "Room " + roomNumber + " History | Records: " + statusStack.size();
    }

    /**
     * equals: two history records are the same based on room number alone.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RoomHistory)) {
            return false;
        }
        RoomHistory other = (RoomHistory) obj;
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
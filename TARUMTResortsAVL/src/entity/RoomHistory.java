package entity;

import adt.LinkedStack;
import adt.StackInterface;


/*
 * Stores the status history of a room.
 * The latest status is kept at the top of the stack.
 */
public class RoomHistory {

    //Data fields
    private String roomNumber;                    
    private StackInterface<String> statusStack;    

    public RoomHistory(String roomNumber) {
        this.roomNumber = roomNumber;
        this.statusStack = new LinkedStack<>();
    }

    //Getters
    public String getRoomNumber() {
        return roomNumber;
    }

    public StackInterface<String> getStatusStack() {
        return statusStack;
    }

    @Override
    public String toString() {
        return "Room " + roomNumber + " History | Records: " + statusStack.size();
    }
    
// Room number is used to identify the room history.
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

// Uses room number to generate the hash code.
    @Override
    public int hashCode() {
        return roomNumber.hashCode();
    }
}
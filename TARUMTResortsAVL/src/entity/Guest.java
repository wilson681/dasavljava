package entity;

import adt.ArrayBasedList;
import adt.ListInterface;

/**
 * Guest.java
 * Entity class -- represents one checked-in/registered guest.
 */
public class Guest {

    // ========== Data fields ==========
    private String confirmationNumber;   // 8-digit confirmation number, uniquely identifies this guest
    private String name;                 // guest name
    private String phone;                // contact phone
    private String memberId;             // linked member ID (null if not a member)
    private String tier;                 // guest's current tier (Standard/Elite/Platinum/Diamond)
    private String registrationTime;     // registration time, e.g. "2026-08-01 09:00", used as a tiebreaker sort key for VIPs of the same tier
    private String checkInDate;          // check-in date, used for occupancy-rate reports
    private String checkOutDate;         // expected check-out date, used for occupancy-rate reports
    private int numberOfNights;          // expected number of nights for this stay, used to calculate room charges
    private ListInterface<String> bookedRooms;   // all room numbers booked for this stay
    // A guest may check out in several stages (e.g. two rooms checked out on different
    // days), each producing its own bill, so this is a list, not a single record -- do
    // not assume a guest has only one bill ever.
    private ListInterface<BillingRecord> billingRecords;
    private ListInterface<Booking> bookings;

    /**
     * Constructor -- a newly registered guest starts with no booked rooms and no
     * bills, so bookedRooms and billingRecords are initialized as empty Lists here.
     *
     * Note: the List implementation class name is a placeholder until the team
     * finalizes the ADT implementation (must not be named "ArrayList" -- that would
     * clash with java.util.ArrayList and look like direct use of the Java Collections
     * Framework).
     */
    public Guest(String confirmationNumber, String name, String phone,
                 String memberId, String tier, String registrationTime,
                 String checkInDate, String checkOutDate, int numberOfNights) {
        this.confirmationNumber = confirmationNumber;
        this.name = name;
        this.phone = phone;
        this.memberId = memberId;
        this.tier = tier;
        this.registrationTime = registrationTime;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.numberOfNights = numberOfNights;
        this.bookedRooms = new ArrayBasedList<>();
        this.billingRecords = new ArrayBasedList<>();
        this.bookings = new ArrayBasedList<>();
    }
/**
 * Search constructor.
 * Used when searching the hash table by confirmation number.
 */
public Guest(String confirmationNumber) {
    this.confirmationNumber = confirmationNumber;
    this.bookedRooms = new ArrayBasedList<>();
    this.billingRecords = new ArrayBasedList<>();
    this.bookings = new ArrayBasedList<>();
}


    // ========== Getters ==========
    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getTier() {
        return tier;
    }

    public String getRegistrationTime() {
        return registrationTime;
    }

    public String getCheckInDate() {
        return checkInDate;
    }

    public String getCheckOutDate() {
        return checkOutDate;
    }

    public int getNumberOfNights() {
        return numberOfNights;
    }

    public ListInterface<String> getBookedRooms() {
        return bookedRooms;
    }

    public ListInterface<BillingRecord> getBillingRecords() {
        return billingRecords;
    }

    // ========== Setters ==========
    // Name, phone, and confirmation number don't change after registration, so no
    // setters are provided for them -- only for fields that can change.

    public void setTier(String tier) {
        // Called by the Control layer to update this field when the guest is upgraded/downgraded.
        this.tier = tier;
    }

    /**
     * Adds a room number to this guest's booked-rooms list (a plain data operation,
     * no business logic like changing room status).
     */
    public void addRoom(String roomNumber) {
        bookedRooms.add(roomNumber);
    }

    /**
     * Removes a room number from this guest's booked-rooms list (a plain data
     * operation, no business logic like changing room status).
     */
    public void removeRoom(String roomNumber) {
        int position = bookedRooms.indexOf(roomNumber);
        if (position != -1) {
            bookedRooms.remove(position);
        }
    }

    /**
     * At checkout, adds this settled bill to the guest's record. A guest may check
     * out in stages, each producing its own bill, so this appends to the list rather
     * than overwriting the previous one (a plain data operation; how room charges,
     * extras, and points are calculated is the Control layer's job).
     */
    public void addBillingRecord(BillingRecord billingRecord) {
        billingRecords.add(billingRecord);
    }
/**
     * Links a booking to this guest. One confirmation number may cover several
     * bookings when the guest reserves more than one room.
     *
     * @param booking the booking to link
     */
    public void addBooking(Booking booking) {
        bookings.add(booking);
    }

    /**
     * @return every booking linked to this confirmation number
     */
    public ListInterface<Booking> getBookings() {
        return bookings;
    }

    // ========== Overridden methods ==========

    /**
     * toString: shows a summary of this guest on the console.
     */
    @Override
    public String toString() {
        return confirmationNumber + " | " + name + " | " + tier + " | Rooms: " + bookedRooms;
    }

    /**
     * equals: two guests are the same person based on confirmation number alone
     * (this method is used by ADT operations like contains() and remove()).
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Guest)) {
            return false;
        }
        Guest other = (Guest) obj;
        return this.confirmationNumber.equals(other.confirmationNumber);
    }

    /**
     * hashCode: kept consistent with equals(), both keyed on confirmationNumber.
     */
    @Override
    public int hashCode() {
        return confirmationNumber.hashCode();
    }
}
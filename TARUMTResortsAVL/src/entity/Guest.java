package entity;

import adt.ArrayBasedList;
import adt.ListInterface;

/*
 * Represents a registered hotel guest and their stay details.
 * One guest may have multiple rooms, bookings and billing records.
 *
 * @author All
 */
public class Guest {

    private String confirmationNumber;   
    private String name;               
    private String phone;               
    private String memberId;            
    private String tier;              
    private String registrationTime;     
    private String checkInDate;          
    private String checkOutDate;        
    private int numberOfNights;          
    private ListInterface<String> bookedRooms;   
    private ListInterface<BillingRecord> billingRecords;
    private ListInterface<Booking> bookings;

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
// Used to search a guest by confirmation number.
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

   
    public void setTier(String tier) {
        this.tier = tier;
    }

   
    public void addRoom(String roomNumber) {
        bookedRooms.add(roomNumber);
    }


    public void removeRoom(String roomNumber) {
        int position = bookedRooms.indexOf(roomNumber);
        if (position != -1) {
            bookedRooms.remove(position);
        }
    }
// A guest may have more than one bill.
    public void addBillingRecord(BillingRecord billingRecord) {
        billingRecords.add(billingRecord);
    }
 // One confirmation number may contain multiple bookings.
    public void addBooking(Booking booking) {
        bookings.add(booking);
    }

    public ListInterface<Booking> getBookings() {
        return bookings;
    }

    @Override
    public String toString() {
        return confirmationNumber + " | " + name + " | " + tier + " | Rooms: " + bookedRooms;
    }

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

   
    @Override
    public int hashCode() {
        return confirmationNumber.hashCode();
    }
}
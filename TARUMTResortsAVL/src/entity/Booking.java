package entity;

/*
 * Represents one booking request.
 * One guest may have multiple bookings, so each booking has its own bookingId.
 */
public class Booking implements Comparable<Booking> {

    private String bookingId;           
    private String confirmationNumber;   
    private String guestNameSnapshot;   
    private String phoneSnapshot;        
    private String memberId;             
    private String requestedRoomType; 
    private BookingStatus status; 
    private String source;
    private int arrivalSequence;   
    private int tierRankAtRequest;    
    private String assignedRoomNo; 
    private String checkInDate;       
    private String checkOutDate;       
    private int numberOfNights;       
    private String registeredAt;     
    private String allocatedAt;     
   
    public Booking(String bookingId, String confirmationNumber, String guestNameSnapshot,
                   String phoneSnapshot, String memberId, String requestedRoomType,
                   BookingStatus status, String source, int arrivalSequence, int tierRankAtRequest,
                   String registeredAt) {
        this.bookingId = bookingId;
        this.confirmationNumber = confirmationNumber;
        this.guestNameSnapshot = guestNameSnapshot;
        this.phoneSnapshot = phoneSnapshot;
        this.memberId = memberId;
        this.requestedRoomType = requestedRoomType;
        this.status = status;
        this.source = source;
        this.arrivalSequence = arrivalSequence;
        this.tierRankAtRequest = tierRankAtRequest;
        this.assignedRoomNo = null;
        this.checkInDate = null;
        this.checkOutDate = null;
        this.numberOfNights = 0;
        this.registeredAt = registeredAt;
        this.allocatedAt = null;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public String getGuestNameSnapshot() {
        return guestNameSnapshot;
    }

    public String getPhoneSnapshot() {
        return phoneSnapshot;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getRequestedRoomType() {
        return requestedRoomType;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public String getSource() {
        return source;
    }

    public int getArrivalSequence() {
        return arrivalSequence;
    }

    public int getTierRankAtRequest() {
        return tierRankAtRequest;
    }

    public String getAssignedRoomNo() {
        return assignedRoomNo;
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

    public String getRegisteredAt() {
        return registeredAt;
    }

    public String getAllocatedAt() {
        return allocatedAt;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public void setAssignedRoomNo(String assignedRoomNo) {
        this.assignedRoomNo = assignedRoomNo;
    }

    public void setStayPeriod(String checkInDate, String checkOutDate, int numberOfNights) {
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.numberOfNights = numberOfNights;
    }

    public void setNumberOfNights(int numberOfNights) {
        this.numberOfNights = numberOfNights;
    }

    public void setAllocatedAt(String allocatedAt) {
        this.allocatedAt = allocatedAt;
    }

    @Override
     public String toString() {
        return bookingId + " | " + confirmationNumber + " | " + guestNameSnapshot
                + " | " + requestedRoomType + " | " + status
                + " | Room: " + (assignedRoomNo == null ? "-" : assignedRoomNo)
                + " | " + (numberOfNights == 0 ? "-" : numberOfNights + " night(s)");
    }

 // Booking ID is used to identify each booking.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Booking)) {
            return false;
        }
        Booking other = (Booking) obj;
        return this.bookingId.equals(other.bookingId);
    }

  // Uses booking ID to generate the hash code.
    @Override
    public int hashCode() {
        return bookingId.hashCode();
    }

   /*
     * AVL priority order:
     * 1. Higher tier first
     * 2. Earlier arrival first
     * 3. Booking ID prevents two different bookings from comparing as equal
     */
    @Override
    public int compareTo(Booking other) {
        if (this.tierRankAtRequest != other.tierRankAtRequest) {
            return other.tierRankAtRequest - this.tierRankAtRequest;
        }
        if (this.arrivalSequence != other.arrivalSequence) {
            return this.arrivalSequence - other.arrivalSequence;
        }
        return this.bookingId.compareTo(other.bookingId);
    }
}

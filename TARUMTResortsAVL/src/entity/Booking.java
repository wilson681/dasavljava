package entity;

/**
 * Booking.java - Entity class representing a room booking request.
 *
 * @author Wilson
 */

public class Booking {

    private String confirmationNo;
    private String memberId;
    private String guestNameSnapshot;
    private RoomType requestedRoomType;
    private BookingStatus status;
    private String source;
    private int arrivalSequence;
    private int tierRankAtRequest;
    private String assignedRoomNo;

   public Booking(String confirmationNo, String memberId, String guestNameSnapshot,
                   RoomType requestedRoomType, BookingStatus status, String source,
                   int arrivalSequence, int tierRankAtRequest) {
        this.confirmationNo = confirmationNo;
        this.memberId = memberId;
        this.guestNameSnapshot = guestNameSnapshot;
        this.requestedRoomType = requestedRoomType;
        this.status = status;
        this.source = source;
        this.arrivalSequence = arrivalSequence;
        this.tierRankAtRequest = tierRankAtRequest;
        this.assignedRoomNo = null;
    }

    public String getConfirmationNo() {
        return confirmationNo;
    }

    public String getMemberId() {
        return memberId;
    }
    
    public String getGuestNameSnapshot() {
        return guestNameSnapshot;
    }
    

    public RoomType getRequestedRoomType() {
        return requestedRoomType;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
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

    public void setAssignedRoomNo(String assignedRoomNo) {
        this.assignedRoomNo = assignedRoomNo;
    }

    /**
     * Builds the composite priority key used by the VIP allocation tree.
     */
    public VipPriorityKey toPriorityKey() {
        return new VipPriorityKey(tierRankAtRequest, arrivalSequence, confirmationNo);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Booking other = (Booking) obj;
        return this.confirmationNo.equals(other.confirmationNo);
    }

    @Override
    public int hashCode() {
        return confirmationNo.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%-10s %-10s %-20s %-10s %-12s rank=%d seq=%d room=%s",
                confirmationNo, memberId, guestNameSnapshot, requestedRoomType, status,
                tierRankAtRequest, arrivalSequence,
                assignedRoomNo == null ? "-" : assignedRoomNo);
    }
}
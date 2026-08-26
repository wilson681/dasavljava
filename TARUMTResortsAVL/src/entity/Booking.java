package entity;

/**
 * Booking.java
 * Entity class -- represents one booking request.
 *
 * Notes:
 * - This is a plain data class (POJO), only holds data for one booking request.
 * - Contains no input (Scanner) or output (System.out) statements, per Entity class rules.
 * - Booking and Guest are different things: Guest is the guest's identity record,
 *   Booking is the act of this one booking request itself.
 * - One guest (one confirmationNumber) can open multiple Bookings at once (e.g.
 *   booking several rooms in one go), so Booking needs its own independent bookingId
 *   as a unique identifier -- it cannot reuse confirmationNumber.
 * - arrivalSequence is used by Module 1 (Walk-In FIFO queue), tierRankAtRequest is
 *   used by Module 2 (VIP priority room allocation).
 * - registeredAt/allocatedAt are real timestamps (format yyyy-MM-dd HH:mm:ss) used by
 *   reports to compute wait duration -- unlike arrivalSequence, which only orders items
 *   within the same queue/tree and cannot tell how many minutes were actually waited;
 *   these two fields are the real time source for reports.
 * - After a room is successfully allocated, the Control layer is responsible for
 *   syncing the result back into Guest.bookedRooms and Room.status -- neither is
 *   Booking's own responsibility.
 * - implements Comparable<Booking>: used for ordering in Module 2's AVL Tree. Rule:
 *   the higher the tierRank, the higher the priority (compared in reverse, so
 *   compareTo is smaller); for the same tierRank, compare arrivalSequence (earlier
 *   arrival gives a smaller compareTo) -- this way an in-order traversal naturally
 *   yields highest priority to lowest.
 */
public class Booking implements Comparable<Booking> {

    // ========== Data fields ==========
    private String bookingId;            // Booking's own unique ID, used to tell apart multiple Bookings for the same guest
    private String confirmationNumber;   // which guest this is linked to (matches Guest's key); multiple Bookings for the same guest share this
    private String guestNameSnapshot;    // snapshot of the guest's name, for direct printing without looking up Guest
    private String phoneSnapshot;        // snapshot of the guest's phone, collected at registration (copied from Member.phone for VIPs), needed when building Guest at allocation time
    private String memberId;             // linked member ID; null for Bookings from WALK_IN source, needed when building Guest at allocation time
    private String requestedRoomType;    // requested room type (Standard / Deluxe / Suite)
    private BookingStatus status;        // booking status (see the BookingStatus enum)
    private String source;               // where this booking came from (e.g. WALK_IN / VIP)
    private int arrivalSequence;         // arrival order, used by Module 1's FIFO queue / Module 2's same-tier comparison
    private int tierRankAtRequest;       // member tier rank, used by Module 2's priority ordering
    private String assignedRoomNo;       // assigned room number, null until allocated
    // Stay period for THIS booking. One confirmation number may cover several
    // bookings with different stay periods, so the dates belong here rather
    // than on Guest.
    private String checkInDate;          // null until the room is allocated
    private String checkOutDate;         // null until the room is allocated
    private int numberOfNights;          // collected at registration time; dates are filled in once actually allocated
    private String registeredAt;         // real timestamp (yyyy-MM-dd HH:mm:ss) at the moment this booking was created, for wait-time reports
    private String allocatedAt;          // real timestamp at the moment a room was allocated; null until then

    /**
     * Constructor -- a newly created booking request has no room assigned yet, so
     * assignedRoomNo is initialized to null.
     */
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

    // ========== Getters ==========
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
    // ========== Setters ==========
    // bookingId, confirmationNumber, guestNameSnapshot, requestedRoomType, source,
    // arrivalSequence, and tierRankAtRequest don't change after registration, so no
    // setters are provided.

    public void setStatus(BookingStatus status) {
        // Driven by the Control layer through the booking status flow
        // (PENDING -> CONFIRMED -> CHECKED_IN -> CHECKED_OUT / CANCELLED).
        this.status = status;
    }

    public void setAssignedRoomNo(String assignedRoomNo) {
        // Called by the Control layer after a room is successfully allocated, to record the assigned room number.
        this.assignedRoomNo = assignedRoomNo;
    }

    /**
     * Records the stay period for this booking. Called by the Control layer at
     * allocation time, when the guest states how many nights they are staying.
     *
     * @param checkInDate the check-in date
     * @param checkOutDate the check-out date
     * @param numberOfNights the number of nights for this booking
     */
    public void setStayPeriod(String checkInDate, String checkOutDate, int numberOfNights) {
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.numberOfNights = numberOfNights;
    }

    /**
     * Records how many nights the guest asked for at registration time, before
     * a room is actually allocated (allocation may happen immediately, or later
     * once a room frees up, when the guest is no longer there to ask).
     *
     * @param numberOfNights the number of nights requested
     */
    public void setNumberOfNights(int numberOfNights) {
        this.numberOfNights = numberOfNights;
    }

    /**
     * Records the real timestamp at which a room was allocated to this
     * booking. Called by the Control layer's tryAllocate() alongside
     * setStayPeriod(), so reports can compute registeredAt-to-allocatedAt
     * wait durations.
     *
     * @param allocatedAt the allocation timestamp (yyyy-MM-dd HH:mm:ss)
     */
    public void setAllocatedAt(String allocatedAt) {
        this.allocatedAt = allocatedAt;
    }

    // ========== Overridden methods ==========

    /**
     * toString: shows a summary of this booking request on the console.
     */
    @Override
     public String toString() {
        return bookingId + " | " + confirmationNumber + " | " + guestNameSnapshot
                + " | " + requestedRoomType + " | " + status
                + " | Room: " + (assignedRoomNo == null ? "-" : assignedRoomNo)
                + " | " + (numberOfNights == 0 ? "-" : numberOfNights + " night(s)");
    }

    /**
     * equals: two booking requests are the same based on Booking's own ID alone (not
     * confirmationNumber, since a guest may open several Bookings at once).
     */
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

    /**
     * hashCode: per Java convention, overriding equals() requires overriding hashCode() too.
     */
    @Override
    public int hashCode() {
        return bookingId.hashCode();
    }

    /**
     * compareTo: used for priority ordering in Module 2's AVL Tree.
     *
     * Rule: the higher the tierRank, the smaller the compareTo value (compared in
     * reverse); for the same tierRank, the smaller the arrivalSequence (earlier
     * arrival) also gives a smaller compareTo value -- both rules place higher
     * priority further to the left in the tree, so an in-order traversal naturally
     * yields highest priority to lowest.
     *
     * The final bookingId comparison is not a sorting requirement, it is a
     * correctness requirement.
     *
     * The AVL tree navigates purely via compareTo throughout (both getEntry() and
     * remove()) -- equals() is never called. So for the tree, "compareTo returns 0"
     * is equivalent to "same key". If two different Bookings compute to 0, the tree
     * cannot tell them apart, and remove() will navigate to whichever one it hits
     * first and delete the wrong one, with no error raised.
     *
     * The first two fields alone don't prevent this: tierRank naturally repeats
     * (members of the same tier), and arrivalSequence can also repeat (seed data and
     * real registrations each have their own counters). bookingId is unique per
     * booking, so adding it last guarantees compareTo == 0 holds only when it really
     * is the same booking -- which lines up with equals() (which compares bookingId),
     * matching Java's recommendation that compareTo should be consistent with equals.
     *
     * This last comparison is only used when the first two fields tie completely; the
     * sort order for cases that already have a clear ordering is unchanged.
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

package entity;

/**
 * Booking.java
 * Entity 类 —— 代表一笔订房请求
 *
 * @author 某某
 *
 * 说明:
 * - 这是纯数据类(POJO),只负责存放一笔订房请求的资料
 * - 不包含任何输入(Scanner)或输出(System.out)语句,符合Entity类规范
 * - Booking 跟 Guest 是两回事:Guest 是客人身份档案,Booking 是"这一次订房请求"这个动作本身
 * - 一位客人(一个 confirmationNumber)可以同时开多笔 Booking(比如一次订多间房),
 *   所以 Booking 自己要有一个独立的 bookingId 当唯一识别,不能再借用 confirmationNumber
 * - arrivalSequence 给模块1(Walk-In FIFO排队)用,tierRankAtRequest 给模块2(VIP优先级分房)用
 * - 分房成功后,由Control层负责把结果同步写回 Guest.bookedRooms 和 Room.status,
 *   这两件事不属于 Booking 自己的职责
 * - implements Comparable<Booking>:给模块2的 AVL Tree 排序用,规则是
 *   tierRank 越高越优先(反过来比,让compareTo值越小),同 tierRank 再比 arrivalSequence
 *   (越早到,compareTo值越小)——这样 in-order 遍历天生就是"优先级由高到低"
 */
public class Booking implements Comparable<Booking> {

    // ========== 数据字段 ==========
    private String bookingId;            // Booking自己专属的唯一编号,一人多笔Booking时用来互相区分
    private String confirmationNumber;   // 关联哪位客人(对应 Guest 的 key),同一位客人的多笔Booking会共用同一个
    private String guestNameSnapshot;    // 客人姓名快照,方便直接打印,不用反查 Guest
    private String phoneSnapshot;        // 客人电话快照,登记时收集(VIP从Member.phone抄过来),分到房建Guest时要用
    private String memberId;             // 关联的会员ID,WALK_IN来源的Booking是null,分到房建Guest时要用
    private String requestedRoomType;    // 要什么房型(Standard / Deluxe / Suite)
    private BookingStatus status;        // 订房状态(见 BookingStatus 枚举)
    private String source;               // 这单从哪来的(例如 WALK_IN / VIP)
    private int arrivalSequence;         // 到达顺序,模块1 FIFO排队/模块2同等级比较用
    private int tierRankAtRequest;       // 会员等级排名,模块2插队优先级用
    private String assignedRoomNo;       // 分到的房号,还没分是 null
    // Stay period for THIS booking. One confirmation number may cover several
    // bookings with different stay periods, so the dates belong here rather
    // than on Guest.
    private String checkInDate;          // null until the room is allocated
    private String checkOutDate;         // null until the room is allocated
    private int numberOfNights;          // collected at registration time; dates are filled in once actually allocated
    /**
     * 构造函数
     * 新建的订房请求,预设还没有分配到房间,所以 assignedRoomNo 初始化为 null
     */
    public Booking(String bookingId, String confirmationNumber, String guestNameSnapshot,
                   String phoneSnapshot, String memberId, String requestedRoomType,
                   BookingStatus status, String source, int arrivalSequence, int tierRankAtRequest) {
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
    // ========== Setters ==========
    // bookingId、confirmationNumber、guestNameSnapshot、requestedRoomType、source、
    // arrivalSequence、tierRankAtRequest 登记后不会改变,所以不提供setter

    public void setStatus(BookingStatus status) {
        // 由Control层驱动订房状态流转(PENDING -> CONFIRMED -> CHECKED_IN -> CHECKED_OUT / CANCELLED)
        this.status = status;
    }

    public void setAssignedRoomNo(String assignedRoomNo) {
        // 由Control层在分房成功后调用,写入分配到的房号
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
    // ========== Override 方法 ==========

    /**
     * toString: 方便在console显示这笔订房请求的摘要信息
     */
    @Override
     public String toString() {
        return bookingId + " | " + confirmationNumber + " | " + guestNameSnapshot
                + " | " + requestedRoomType + " | " + status
                + " | Room: " + (assignedRoomNo == null ? "-" : assignedRoomNo)
                + " | " + (numberOfNights == 0 ? "-" : numberOfNights + " night(s)");
    }

    /**
     * equals: 两笔订房请求是否视为"同一笔",以Booking自己的编号作为唯一依据
     * (不能用confirmationNumber,因为同一位客人可能同时开好几笔Booking)
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
     * hashCode: 依照Java规范,override了equals()就必须配套override hashCode()
     */
    @Override
    public int hashCode() {
        return bookingId.hashCode();
    }

    /**
     * compareTo: 给模块2的 AVL Tree 排优先级用,不是判断"是不是同一笔"(那是equals的事)
     * 规则:tierRank 越高,compareTo 值越小(反过来比);tierRank 相同,arrivalSequence 越小
     * (越早到)compareTo 值也越小——两条规则都让"优先级越高"排在树的越左边,
     * in-order 遍历天生就是"优先级由高到低"
     */
    @Override
    public int compareTo(Booking other) {
        if (this.tierRankAtRequest != other.tierRankAtRequest) {
            return other.tierRankAtRequest - this.tierRankAtRequest;
        }
        return this.arrivalSequence - other.arrivalSequence;
    }
}

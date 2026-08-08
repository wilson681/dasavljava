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
 * - arrivalSequence 给模块1(Walk-In FIFO排队)用,tierRankAtRequest 给模块2(VIP优先级分房)用
 * - 分房成功后,由Control层负责把结果同步写回 Guest.bookedRooms 和 Room.status,
 *   这两件事不属于 Booking 自己的职责
 */
public class Booking {

    // ========== 数据字段 ==========
    private String confirmationNumber;   // 关联哪位客人(对应 Guest 的 key)
    private String guestNameSnapshot;    // 客人姓名快照,方便直接打印,不用反查 Guest
    private String requestedRoomType;    // 要什么房型(Standard / Deluxe / Suite)
    private BookingStatus status;        // 订房状态(见 BookingStatus 枚举)
    private String source;               // 这单从哪来的(例如 WALK_IN / VIP)
    private int arrivalSequence;         // 到达顺序,模块1 FIFO排队用
    private int tierRankAtRequest;       // 会员等级排名,模块2插队优先级用
    private String assignedRoomNo;       // 分到的房号,还没分是 null

    /**
     * 构造函数
     * 新建的订房请求,预设还没有分配到房间,所以 assignedRoomNo 初始化为 null
     */
    public Booking(String confirmationNumber, String guestNameSnapshot,
                   String requestedRoomType, BookingStatus status, String source,
                   int arrivalSequence, int tierRankAtRequest) {
        this.confirmationNumber = confirmationNumber;
        this.guestNameSnapshot = guestNameSnapshot;
        this.requestedRoomType = requestedRoomType;
        this.status = status;
        this.source = source;
        this.arrivalSequence = arrivalSequence;
        this.tierRankAtRequest = tierRankAtRequest;
        this.assignedRoomNo = null;
    }

    // ========== Getters ==========
    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public String getGuestNameSnapshot() {
        return guestNameSnapshot;
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

    // ========== Setters ==========
    // confirmationNumber、guestNameSnapshot、requestedRoomType、source、
    // arrivalSequence、tierRankAtRequest 登记后不会改变,所以不提供setter

    public void setStatus(BookingStatus status) {
        // 由Control层驱动订房状态流转(PENDING -> CONFIRMED -> CHECKED_IN -> CHECKED_OUT / CANCELLED)
        this.status = status;
    }

    public void setAssignedRoomNo(String assignedRoomNo) {
        // 由Control层在分房成功后调用,写入分配到的房号
        this.assignedRoomNo = assignedRoomNo;
    }

    // ========== Override 方法 ==========

    /**
     * toString: 方便在console显示这笔订房请求的摘要信息
     */
    @Override
    public String toString() {
        return confirmationNumber + " | " + guestNameSnapshot + " | " + requestedRoomType
                + " | " + status + " | Room: " + (assignedRoomNo == null ? "-" : assignedRoomNo);
    }

    /**
     * equals: 两笔订房请求是否视为"同一笔",以确认号码作为唯一依据
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
        return this.confirmationNumber.equals(other.confirmationNumber);
    }

    /**
     * hashCode: 依照Java规范,override了equals()就必须配套override hashCode()
     */
    @Override
    public int hashCode() {
        return confirmationNumber.hashCode();
    }
}

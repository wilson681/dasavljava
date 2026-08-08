package entity;

/**
 * Guest.java
 * Entity 类 —— 代表一位登记入住的客人
 *
 * @author 某某
 *

 */
public class Guest {

    // ========== 数据字段 ==========
    private String confirmationNumber;   // 8位确认号码,唯一识别这位客人
    private String name;                  // 客人姓名
    private String phone;                 // 联系电话
    private String memberId;              // 关联的会员ID(如果不是会员,则为null)
    private String tier;                  // 客人当前等级(Standard/Silver/Gold/Platinum/Diamond)
    private String registrationTime;      // 登记时间,格式如 "2026-08-01 09:00",用于VIP同等级时的排序依据
    private ListInterface<String> bookedRooms;          // 这次入住,预订的所有房号
    private ListInterface<BillingRecord> billingHistory; // 这位客人名下的所有消费记录

    /**
     * 构造函数
     * 新登记的客人,预设还没有任何预订的房间和消费记录,
     * 所以 bookedRooms 和 billingHistory 在这里初始化成空的 List
     *
     * 注意: 这里的 List 实现类名称待team确定最终ADT实现方式后替换
     * (不可使用 ArrayList 这个名字,因为会与 java.util.ArrayList 撞名,
     *  容易被误判为直接使用了 Java Collections Framework)
     */
    public Guest(String confirmationNumber, String name, String phone,
                 String memberId, String tier, String registrationTime) {
        this.confirmationNumber = confirmationNumber;
        this.name = name;
        this.phone = phone;
        this.memberId = memberId;
        this.tier = tier;
        this.registrationTime = registrationTime;
        this.bookedRooms = new ArrayBasedList<>();
        this.billingHistory = new ArrayBasedList<>();
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

    public ListInterface<String> getBookedRooms() {
        return bookedRooms;
    }

    public ListInterface<BillingRecord> getBillingHistory() {
        return billingHistory;
    }

    // ========== Setters ==========
    // 姓名、电话、确认号码在登记后不会变,所以不提供setter,只提供会变动的字段的setter

    public void setTier(String tier) {
        // 客人升级/降级时,由Control层调用,更新这个字段
        this.tier = tier;
    }

    /**
     * 把一个房号加进这位客人的预订列表
     * (单纯的数据操作,不涉及房间状态改变等业务逻辑)
     */
    public void addRoom(String roomNumber) {
        bookedRooms.add(roomNumber);
    }

    /**
     * 把一个房号从这位客人的预订列表移除
     * (单纯的数据操作,不涉及房间状态改变等业务逻辑)
     */
    public void removeRoom(String roomNumber) {
        int position = bookedRooms.indexOf(roomNumber);
        if (position != -1) {
            bookedRooms.remove(position);
        }
    }

    /**
     * 新增一笔消费记录,加进这位客人的账单历史
     * (单纯的数据操作,不涉及计费规则等业务逻辑)
     */
    public void addBillingRecord(BillingRecord record) {
        billingHistory.add(record);
    }

    // ========== Override 方法 ==========

    /**
     * toString: 方便在console显示这位客人的摘要信息
     */
    @Override
    public String toString() {
        return confirmationNumber + " | " + name + " | " + tier + " | Rooms: " + bookedRooms;
    }

    /**
     * equals: 两位客人是否视为"同一个人",以确认号码作为唯一依据
     * (这个方法在ADT的contains()、remove()等操作中会被用到)
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
     * 两者都以confirmationNumber作为唯一依据,保持一致
     */
    @Override
    public int hashCode() {
        return confirmationNumber.hashCode();
    }
}
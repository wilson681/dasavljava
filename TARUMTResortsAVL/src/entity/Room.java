package entity;

/**
 * Room.java
 * Entity 类 —— 代表酒店里的一间房间
 *
 * @author 某某
 *
 * status 可能的值(状态流程,由Control层驱动改变):
 *   AVAILABLE              — 空房,可分配给客人
 *   OCCUPIED                — 已有客人入住
 *   NEEDS_CLEANING          — 客人已退房,待清洁
 *   CLEANING_IN_PROGRESS    — 清洁中
 *   INSPECTED               — 已检查,即将转为可用
 */
public class Room {

    // ========== 数据字段 ==========
    private String roomNumber;   // 房号,唯一识别这间房
    private String roomType;      // 房型: Standard / Deluxe / Suite
    private double nightlyRate;   // 固定房价,每晚多少钱
    private String status;        // 当前状态(见类注释列出的状态值)
    private RoomHistory roomHistory;   // 这间房自己的状态变更历史(Entity间引用,模块3用)

    /**
     * 构造函数
     * 房间一建出来,就自带一份空的 RoomHistory,不用额外的对照表去找
     */
    public Room(String roomNumber, String roomType, double nightlyRate, String status) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.nightlyRate = nightlyRate;
        this.status = status;
        this.roomHistory = new RoomHistory(roomNumber);
    }

    // ========== Getters ==========
    public String getRoomNumber() {
        return roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public double getNightlyRate() {
        return nightlyRate;
    }

    public String getStatus() {
        return status;
    }

    public RoomHistory getRoomHistory() {
        return roomHistory;
    }

    // ========== Setters ==========
    // roomNumber、roomType、nightlyRate 在创建后不会改变,所以不提供setter
    // status 会随着入住/退房/清洁流程不断改变,由Control层调用来更新

    public void setStatus(String status) {
        this.status = status;
    }

    // ========== Override 方法 ==========

    /**
     * toString: 方便在console显示这间房的摘要信息
     */
    @Override
    public String toString() {
        return roomNumber + " | " + roomType + " | RM" + nightlyRate + " | " + status;
    }

    /**
     * equals: 两间房是否视为"同一间",以房号作为唯一依据
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Room)) {
            return false;
        }
        Room other = (Room) obj;
        return this.roomNumber.equals(other.roomNumber);
    }

    /**
     * hashCode: 依照Java规范,override了equals()就必须配套override hashCode()
     */
    @Override
    public int hashCode() {
        return roomNumber.hashCode();
    }
}
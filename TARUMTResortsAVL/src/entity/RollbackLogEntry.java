package entity;

/**
 * RollbackLogEntry.java
 * Entity 类 —— 代表 Housekeeping 一次成功的状态回滚(undo)事件
 *
 * @author 某某
 *
 * 说明:
 * - 这是纯数据类(POJO),只负责存放一次回滚事件的资料
 * - 不包含任何输入(Scanner)或输出(System.out)语句,符合Entity类规范
 * - RoomHistory 里的 Stack 一 pop 掉记录就消失,没办法事后统计"这间房到底被回滚了几次"
 *   ——这份记录就是补这个洞的:只增不减的流水,只有回滚成功那一刻才会新增一笔
 */
public class RollbackLogEntry {

    // ========== 数据字段 ==========
    private String roomNumber;   // 哪一间房被回滚
    private String fromStatus;   // 回滚前的状态(被移除的那个)
    private String toStatus;     // 回滚后恢复到的状态
    private String date;         // 回滚发生的日期

    /**
     * 构造函数
     */
    public RollbackLogEntry(String roomNumber, String fromStatus, String toStatus, String date) {
        this.roomNumber = roomNumber;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.date = date;
    }

    // ========== Getters ==========
    // 记录一旦产生不应该被修改,所以只提供getter,不提供setter

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public String getDate() {
        return date;
    }

    // ========== Override 方法 ==========

    /**
     * toString: 方便在console显示这笔回滚记录的摘要信息
     */
    @Override
    public String toString() {
        return date + " | Room " + roomNumber + " | " + fromStatus + " -> " + toStatus;
    }

    /**
     * equals: 两笔回滚记录是否视为"同一笔",用房号+日期+回滚前状态一起比对
     * (同一间房同一天可能被回滚不只一次,单靠房号+日期不够精确)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RollbackLogEntry)) {
            return false;
        }
        RollbackLogEntry other = (RollbackLogEntry) obj;
        return this.roomNumber.equals(other.roomNumber)
                && this.date.equals(other.date)
                && this.fromStatus.equals(other.fromStatus);
    }

    /**
     * hashCode: 依照Java规范,override了equals()就必须配套override hashCode()
     */
    @Override
    public int hashCode() {
        int result = roomNumber.hashCode();
        result = 31 * result + date.hashCode();
        result = 31 * result + fromStatus.hashCode();
        return result;
    }
}

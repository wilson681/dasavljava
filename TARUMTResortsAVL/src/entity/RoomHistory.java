package entity;

/**
 * RoomHistory.java
 * Entity 类 —— 代表一间房间自己的状态变更历史记录
 *
 * @author 某某
 *
 * 说明:
 * - 这是纯数据类(POJO),内部包一个Stack,专门存这一间房的状态变更记录
 * - 不包含任何输入(Scanner)或输出(System.out)语句,符合Entity类规范
 * - 不包含"要不要撤销""撤销后怎么处理"这类业务逻辑,这些应写在Control层
 * - Control层做撤销(roll back)操作时,直接对 statusStack 进行 push/pop
 * - 根据业务需求,只保留最近20条记录(由Control层在push新记录前,
 *   检查数量并移除最旧的一条,Entity本身不做这个数量限制的判断)
 */
public class RoomHistory {

    // ========== 数据字段 ==========
    private String roomNumber;                    // 这份历史记录,属于哪一间房
    private StackInterface<String> statusStack;    // 状态变更记录,栈顶是最近一次的状态

    /**
     * 构造函数
     * 新建时,这间房还没有任何历史记录,所以statusStack初始化成空的
     *
     * 注意: 这里的 Stack 实现类名称待team确定最终ADT实现方式后替换
     */
    public RoomHistory(String roomNumber) {
        this.roomNumber = roomNumber;
        this.statusStack = new LinkedStack<>();
    }

    // ========== Getters ==========
    public String getRoomNumber() {
        return roomNumber;
    }

    public StackInterface<String> getStatusStack() {
        return statusStack;
    }

    // ========== Override 方法 ==========

    /**
     * toString: 方便在console显示这份历史记录的摘要信息
     */
    @Override
    public String toString() {
        return "Room " + roomNumber + " History | Records: " + statusStack.size();
    }

    /**
     * equals: 两份历史记录是否视为"同一份",以房号作为唯一依据
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RoomHistory)) {
            return false;
        }
        RoomHistory other = (RoomHistory) obj;
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
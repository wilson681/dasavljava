package entity;

/**
 * BillingRecord.java
 * Entity 类 —— 代表一笔消费/账单记录
 *
 * @author 某某
 *
 * 说明:
 * - 这是纯数据类(POJO),只负责存放一笔消费记录的资料
 * - 不包含任何输入(Scanner)或输出(System.out)语句,符合Entity类规范
 * - roomNumber 字段记录这笔消费发生在哪个房间,方便日后按房型统计营收
 * - 一位客人可能有多笔BillingRecord,存放在Guest的billingHistory里
 */
public class BillingRecord {

    // ========== 数据字段 ==========
    private String confirmationNumber;   // 这笔消费,属于哪位客人
    private String roomNumber;            // 这笔消费,发生在哪个房间(方便按房型统计)
    private String description;           // 消费项目描述,例如 "Room Service", "Spa"
    private double amount;                // 消费金额
    private String date;                  // 消费日期

    /**
     * 构造函数
     */
    public BillingRecord(String confirmationNumber, String roomNumber,
                          String description, double amount, String date) {
        this.confirmationNumber = confirmationNumber;
        this.roomNumber = roomNumber;
        this.description = description;
        this.amount = amount;
        this.date = date;
    }

    // ========== Getters ==========
    // 消费记录一旦产生,不应该被修改,所以只提供getter,不提供setter

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public String getDate() {
        return date;
    }

    // ========== Override 方法 ==========

    /**
     * toString: 方便在console显示这笔消费记录的摘要信息
     */
    @Override
    public String toString() {
        return date + " | Room " + roomNumber + " | " + description + " | RM" + amount;
    }

    /**
     * equals: 两笔消费记录,视为同一笔的判断依据
     * 用确认号码+日期+描述+金额一起比对(因为单纯一个字段不足以唯一识别一笔消费)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BillingRecord)) {
            return false;
        }
        BillingRecord other = (BillingRecord) obj;
        return this.confirmationNumber.equals(other.confirmationNumber)
                && this.date.equals(other.date)
                && this.description.equals(other.description)
                && this.amount == other.amount;
    }

    /**
     * hashCode: 依照Java规范,override了equals()就必须配套override hashCode()
     */
    @Override
    public int hashCode() {
        int result = confirmationNumber.hashCode();
        result = 31 * result + date.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + Double.hashCode(amount);
        return result;
    }
}
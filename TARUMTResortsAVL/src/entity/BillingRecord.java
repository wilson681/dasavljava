package entity;

/**
 * BillingRecord.java
 * Entity 类 —— 代表一次入住,退房时结算的总账单
 *
 * @author 某某
 *
 * 说明:
 * - 这是纯数据类(POJO),只负责存放一次入住结算后的账单资料
 * - 不包含任何输入(Scanner)或输出(System.out)语句,符合Entity类规范
 * - 一位客人一次入住,可能分好几次退房(比如两间房、不同天各自退),各自结出各自的
 *   账单,所以一个Guest对应的是一份BillingRecord清单,不是只有一张
 * - roomFee、totalAmount、pointsEarned 都是Control层算好之后传进来的结果,
 *   计算公式(房价x晚数、消费换算积分的比例)写死在Control层,Entity本身不负责计算
 */
public class BillingRecord {

    // ========== 数据字段 ==========
    private String billingId;             // 这张账单自己专属的唯一编号,一人多张账单时用来互相区分
    private String confirmationNumber;   // 这张账单,属于哪位客人(同一位客人的多张账单会共用同一个)
    private double roomFee;               // 房费 = 房价 x 住的晚数
    private double extraCharges;          // 额外消费,退房时前台手动输入的一个总数,不分类
    private double totalAmount;           // 总额 = roomFee + extraCharges
    private int pointsEarned;             // 这次消费赚了多少积分
    private String date;                  // 结账日期

    /**
     * 构造函数
     */
    public BillingRecord(String billingId, String confirmationNumber, double roomFee, double extraCharges,
                          double totalAmount, int pointsEarned, String date) {
        this.billingId = billingId;
        this.confirmationNumber = confirmationNumber;
        this.roomFee = roomFee;
        this.extraCharges = extraCharges;
        this.totalAmount = totalAmount;
        this.pointsEarned = pointsEarned;
        this.date = date;
    }

    // ========== Getters ==========
    // 账单一旦结清,不应该被修改,所以只提供getter,不提供setter

    public String getBillingId() {
        return billingId;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public double getRoomFee() {
        return roomFee;
    }

    public double getExtraCharges() {
        return extraCharges;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public String getDate() {
        return date;
    }

    // ========== Override 方法 ==========

    /**
     * toString: 方便在console显示这张账单的摘要信息
     */
    @Override
    public String toString() {
        return billingId + " | " + date + " | Room Fee: RM" + roomFee + " | Extra: RM" + extraCharges
                + " | Total: RM" + totalAmount + " | +" + pointsEarned + " pts";
    }

    /**
     * equals: 两张账单是否视为"同一张",以billingId作为唯一依据
     * (不能用confirmationNumber,因为同一位客人可能分好几次退房、各自有各自的账单)
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
        return this.billingId.equals(other.billingId);
    }

    /**
     * hashCode: 依照Java规范,override了equals()就必须配套override hashCode()
     */
    @Override
    public int hashCode() {
        return billingId.hashCode();
    }
}

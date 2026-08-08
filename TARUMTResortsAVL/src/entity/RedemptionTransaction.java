package entity;

/**
 * RedemptionTransaction.java
 * Entity 类 —— 代表一笔积分兑换记录
 *
 * @author 某某
 *
 * 说明:
 * - 这是纯数据类(POJO),只负责存放一笔兑换记录的资料
 * - 不包含任何输入(Scanner)或输出(System.out)语句,符合Entity类规范
 * - 这份记录,是"兑换"这个操作发生时,由Control层产生的结果,
 *   不需要预先从txt文件初始化
 */
public class RedemptionTransaction {

    // ========== 数据字段 ==========
    private String memberId;        // 哪位会员进行的兑换
    private String itemRedeemed;    // 兑换了什么东西(对应 RedemptionItem 的名称)
    private int pointsUsed;         // 这次兑换,花了多少积分
    private String date;            // 兑换日期

    /**
     * 构造函数
     */
    public RedemptionTransaction(String memberId, String itemRedeemed, int pointsUsed, String date) {
        this.memberId = memberId;
        this.itemRedeemed = itemRedeemed;
        this.pointsUsed = pointsUsed;
        this.date = date;
    }

    // ========== Getters ==========
    // 兑换记录一旦产生,不应该被修改,所以只提供getter,不提供setter

    public String getMemberId() {
        return memberId;
    }

    public String getItemRedeemed() {
        return itemRedeemed;
    }

    public int getPointsUsed() {
        return pointsUsed;
    }

    public String getDate() {
        return date;
    }

    // ========== Override 方法 ==========

    /**
     * toString: 方便在console显示这笔兑换记录的摘要信息
     */
    @Override
    public String toString() {
        return date + " | " + itemRedeemed + " | -" + pointsUsed + " pts";
    }

    /**
     * equals: 两笔兑换记录,视为同一笔的判断依据
     * 用会员ID+日期+兑换项目一起比对
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RedemptionTransaction)) {
            return false;
        }
        RedemptionTransaction other = (RedemptionTransaction) obj;
        return this.memberId.equals(other.memberId)
                && this.date.equals(other.date)
                && this.itemRedeemed.equals(other.itemRedeemed);
    }

    /**
     * hashCode: 依照Java规范,override了equals()就必须配套override hashCode()
     */
    @Override
    public int hashCode() {
        int result = memberId.hashCode();
        result = 31 * result + date.hashCode();
        result = 31 * result + itemRedeemed.hashCode();
        return result;
    }
}
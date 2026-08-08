package entity;

/**
 * PointsLedgerEntry.java
 * Entity 类 —— 代表会员一批积分的明细记录
 *
 * @author 某某
 *
 * 说明:
 * - 这是纯数据类(POJO),只负责存放一批积分"哪天赚的、什么时候过期"的资料
 * - 不包含任何输入(Scanner)或输出(System.out)语句,符合Entity类规范
 * - 跟 Member.totalPointsEarned(历史累计总数)不一样:
 *   这个类记的是"单独一笔明细",一位会员会有很多笔,串起来才能看出
 *   哪些积分快过期、最近有没有在赚分(给"积分到期提醒"和"降级判断"用)
 */
public class PointsLedgerEntry {

    // ========== 数据字段 ==========
    private String ledgerId;      // 这一笔明细自己专属的编号,唯一识别
    private String memberId;      // 属于哪位会员
    private int pointsAmount;     // 这一批赚了多少分
    private String earnedDate;    // 哪天赚的
    private String expiryDate;    // 什么时候过期

    /**
     * 构造函数
     */
    public PointsLedgerEntry(String ledgerId, String memberId, int pointsAmount,
                              String earnedDate, String expiryDate) {
        this.ledgerId = ledgerId;
        this.memberId = memberId;
        this.pointsAmount = pointsAmount;
        this.earnedDate = earnedDate;
        this.expiryDate = expiryDate;
    }

    // ========== Getters ==========
    // 这笔明细一旦产生,不应该被修改,所以只提供getter,不提供setter

    public String getLedgerId() {
        return ledgerId;
    }

    public String getMemberId() {
        return memberId;
    }

    public int getPointsAmount() {
        return pointsAmount;
    }

    public String getEarnedDate() {
        return earnedDate;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    // ========== Override 方法 ==========

    /**
     * toString: 方便在console显示这笔积分明细的摘要信息
     */
    @Override
    public String toString() {
        return earnedDate + " | +" + pointsAmount + " pts | Expires: " + expiryDate;
    }

    /**
     * equals: 两笔积分明细是否视为"同一笔",以ledgerId作为唯一依据
     * (不能用memberId,因为同一位会员会有很多笔明细)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PointsLedgerEntry)) {
            return false;
        }
        PointsLedgerEntry other = (PointsLedgerEntry) obj;
        return this.ledgerId.equals(other.ledgerId);
    }

    /**
     * hashCode: 依照Java规范,override了equals()就必须配套override hashCode()
     */
    @Override
    public int hashCode() {
        return ledgerId.hashCode();
    }
}

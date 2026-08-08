package entity;

import adt.DoublyLinkedList;
import adt.ListInterface;

/**
 * Member.java
 * Entity 类 —— 代表一位会员的资料
 *
 * @author 某某
 *
 * 说明:
 * - 这是纯数据类(POJO),只负责存放会员的资料
 * - 不包含任何输入(Scanner)或输出(System.out)语句,符合Entity类规范
 * - 不包含"要不要升级""升级门槛是多少"这类业务逻辑,这些应写在Control层
 *
 * 关于两个积分字段的设计说明:
 *   currentPoints    — 目前账户余额,可用来兑换东西,会随着累积/兑换而增减
 *   totalPointsEarned — 历史累计总额,只会增加不会减少,只用来判断会员等级(Tier)
 * 这样设计,是为了避免"客人兑换东西花掉积分,导致等级被降级"这种不合理的情况
 */
public class Member {

    // ========== 数据字段 ==========
    private String memberId;           // 会员ID,唯一识别这位会员
    private String name;                // 会员姓名
    private String phone;               // 会员联系电话
    private String tier;                // 会员等级(Elite/Platinum/Diamond)
    private int currentPoints;          // 当前可兑换的积分余额
    private int totalPointsEarned;      // 历史累计总积分(只用来判断升级,不受兑换影响)
    private ListInterface<PointsLedgerEntry> pointsLedger;   // 这位会员的积分批次明细,给到期提醒/降级判断用

    /**
     * 构造函数
     * 新会员,预设还没有任何积分批次明细,所以 pointsLedger 初始化成空的 List
     *
     * 注意: 这里的 List 实现类名称待team确定最终ADT实现方式后替换
     */
    public Member(String memberId, String name, String phone, String tier,
                  int currentPoints, int totalPointsEarned) {
        this.memberId = memberId;
        this.name = name;
        this.phone = phone;
        this.tier = tier;
        this.currentPoints = currentPoints;
        this.totalPointsEarned = totalPointsEarned;
        this.pointsLedger = new DoublyLinkedList<>();
    }

    // ========== Getters ==========
    public String getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getTier() {
        return tier;
    }

    public int getCurrentPoints() {
        return currentPoints;
    }

    public int getTotalPointsEarned() {
        return totalPointsEarned;
    }

    public ListInterface<PointsLedgerEntry> getPointsLedger() {
        return pointsLedger;
    }

    // ========== Setters ==========
    // memberId、name 创建后不会改变,所以不提供setter

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setTier(String tier) {
        // 由Control层判断是否达到升级门槛后,调用这个方法更新等级
        this.tier = tier;
    }

    public void setCurrentPoints(int currentPoints) {
        // 由Control层在累积/兑换积分时调用
        this.currentPoints = currentPoints;
    }

    public void setTotalPointsEarned(int totalPointsEarned) {
        // 由Control层在累积积分时调用(兑换不会影响这个字段)
        this.totalPointsEarned = totalPointsEarned;
    }

    /**
     * 新增一笔积分批次明细
     * (单纯的数据操作,几时该加一笔、加多少是Control层的事)
     */
    public void addPointsEntry(PointsLedgerEntry entry) {
        pointsLedger.add(entry);
    }

    // ========== Override 方法 ==========

    /**
     * toString: 方便在console显示这位会员的摘要信息
     */
    @Override
    public String toString() {
        return memberId + " | " + name + " | " + tier
                + " | Points: " + currentPoints + " (Total Earned: " + totalPointsEarned + ")";
    }

    /**
     * equals: 两位会员是否视为"同一位",以会员ID作为唯一依据
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Member)) {
            return false;
        }
        Member other = (Member) obj;
        return this.memberId.equals(other.memberId);
    }

    /**
     * hashCode: 依照Java规范,override了equals()就必须配套override hashCode()
     */
    @Override
    public int hashCode() {
        return memberId.hashCode();
    }
}
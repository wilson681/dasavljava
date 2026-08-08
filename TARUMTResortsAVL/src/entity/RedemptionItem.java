package entity;

/**
 * RedemptionItem.java
 * Entity 类 —— 代表兑换清单里的一个选项
 *
 * @author 某某
 *
 * 说明:
 * - 这是纯数据类(POJO),只负责存放一个"可兑换项目"的资料
 * - 不包含任何输入(Scanner)或输出(System.out)语句,符合Entity类规范
 * - 兑换清单目前先从txt文件读取固定选项,
 *   若未来系统支持职员自行新增兑换项目,这个类的结构不需要改变
 */
public class RedemptionItem {

    // ========== 数据字段 ==========
    private String itemName;         // 兑换项目名称,例如 "Free Breakfast"
    private int pointsRequired;      // 兑换这个项目,需要多少积分

    /**
     * 构造函数
     */
    public RedemptionItem(String itemName, int pointsRequired) {
        this.itemName = itemName;
        this.pointsRequired = pointsRequired;
    }

    // ========== Getters ==========
    public String getItemName() {
        return itemName;
    }

    public int getPointsRequired() {
        return pointsRequired;
    }

    // ========== Override 方法 ==========

    /**
     * toString: 方便在console显示这个兑换项目的摘要信息
     */
    @Override
    public String toString() {
        return itemName + " | " + pointsRequired + " pts";
    }

    /**
     * equals: 两个兑换项目是否视为"同一个",以项目名称作为唯一依据
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RedemptionItem)) {
            return false;
        }
        RedemptionItem other = (RedemptionItem) obj;
        return this.itemName.equals(other.itemName);
    }

    /**
     * hashCode: 依照Java规范,override了equals()就必须配套override hashCode()
     */
    @Override
    public int hashCode() {
        return itemName.hashCode();
    }
}
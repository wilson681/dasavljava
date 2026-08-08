package utility;

/**
 * TierRankUtility.java
 * Utility 类 —— 把会员等级(文字)换算成排名数字,给模块2的AVL Tree排序用。
 *
 * @author 某某
 *
 * 说明:
 * - 只含 static 方法,没有任何状态,符合 Utility 类规范
 * - 数字越大代表等级越高:Diamond=3 > Platinum=2 > Elite=1
 * - 非会员(Standard/未知等级)一律回传 0,排在所有VIP等级之后
 */
public class TierRankUtility {

    private TierRankUtility() {
        // 不给外部 new 出来,纯 static 工具类
    }

    /**
     * 把等级文字换算成排名数字。
     * @param tier 等级文字(Elite/Platinum/Diamond,大小写不拘)
     * @return 排名数字,越大代表等级越高;不认得的等级回传 0
     */
    public static int tierToRank(String tier) {
        if (tier == null) {
            return 0;
        }
        switch (tier.trim().toUpperCase()) {
            case "DIAMOND":
                return 3;
            case "PLATINUM":
                return 2;
            case "ELITE":
                return 1;
            default:
                return 0;
        }
    }
}

package control;

import adt.ListInterface;
import boundary.LoyaltyCLI;
import entity.Member;
import entity.PointsLedgerEntry;
import entity.RedemptionItem;
import entity.RedemptionTransaction;
import java.time.LocalDate;
import java.util.Iterator;
import utility.TierRankUtility;
import utility.ValidationUtility;

/**
 * LoyaltyControl.java - 模块5(Loyalty and Rewards Service)的业务逻辑。
 *
 * @author 某某
 *
 * 说明:
 * - 兑换记录不挂在Member身上,单独用一份共用清单存所有会员的RedemptionTransaction,
 *   查询时用memberId过滤即可,不需要为此改动Member entity
 * - 赚积分(awardPoints)本来是设计给退房结账那一刻触发的,退房流程现在还没做——
 *   现在先接一个"手动加分"的菜单动作当调用入口(职员自行调整/促销用),
 *   将来退房流程做出来,一样呼叫同一个awardPoints(),逻辑不用重写
 * - 只对"会调用collection ADT方法的操作"(兑换、手动加分)做输入校验
 */
public class LoyaltyControl {

    // 手动/自动加分,新一批积分预设多久后过期(单位:月)
    private static final int POINTS_VALIDITY_MONTHS = 12;

    private final LoyaltyCLI loyaltyCLI;
    private final ListInterface<Member> memberList;
    private final ListInterface<RedemptionItem> redemptionCatalog;
    private final ListInterface<RedemptionTransaction> redemptionHistory;

    private int ledgerCounter;

    public LoyaltyControl(LoyaltyCLI loyaltyCLI,
                           ListInterface<Member> memberList,
                           ListInterface<RedemptionItem> redemptionCatalog,
                           ListInterface<RedemptionTransaction> redemptionHistory) {
        if (loyaltyCLI == null || memberList == null || redemptionCatalog == null || redemptionHistory == null) {
            throw new IllegalArgumentException("LoyaltyControl dependencies cannot be null");
        }
        this.loyaltyCLI = loyaltyCLI;
        this.memberList = memberList;
        this.redemptionCatalog = redemptionCatalog;
        this.redemptionHistory = redemptionHistory;
        this.ledgerCounter = 0;
    }

    /**
     * 跑这个模块自己的选单循环,直到使用者选择返回。
     */
    public void run() {
        boolean running = true;
        while (running) {
            int choice = loyaltyCLI.displayMenuAndGetChoice();
            switch (choice) {
                case 1:
                    doViewExpiry();
                    break;
                case 2:
                    doRedeem();
                    break;
                case 3:
                    doAddPoints();
                    break;
                case 0:
                    running = false;
                    break;
                default:
                    loyaltyCLI.displayInvalidChoice();
            }
        }
    }

    // ========== 功能1:查看积分到期状况 ==========

    private void doViewExpiry() {
        String memberId = loyaltyCLI.promptMemberId();
        if (ValidationUtility.isBlank(memberId)) {
            loyaltyCLI.displayMemberNotFound(memberId);
            return;
        }
        Member member = findMemberById(memberId);
        if (member == null) {
            loyaltyCLI.displayMemberNotFound(memberId);
            return;
        }
        loyaltyCLI.displayPointsExpiry(member, member.getPointsLedger().getIterator());
    }

    // ========== 功能2:兑换积分 ==========

    private void doRedeem() {
        String memberId = loyaltyCLI.promptMemberId();
        if (ValidationUtility.isBlank(memberId)) {
            loyaltyCLI.displayMemberNotFound(memberId, "Redemption failed.");
            return;
        }
        Member member = findMemberById(memberId);
        if (member == null) {
            loyaltyCLI.displayMemberNotFound(memberId, "Redemption failed.");
            return;
        }

        // 先按pointsRequired由小到大把清单排好,再显示——自己写selection sort,
        // 不能用Collections.sort()
        sortCatalogByPoints();
        loyaltyCLI.displayCatalog(redemptionCatalog.getIterator(), member.getCurrentPoints());

        String itemName = loyaltyCLI.promptItemName();
        if (ValidationUtility.isBlank(itemName)) {
            loyaltyCLI.displayItemNotFound(itemName);
            return;
        }
        RedemptionItem item = findItemByName(itemName);
        if (item == null) {
            loyaltyCLI.displayItemNotFound(itemName);
            return;
        }

        if (member.getCurrentPoints() < item.getPointsRequired()) {
            loyaltyCLI.displayInsufficientPoints(member.getCurrentPoints(), item.getPointsRequired());
            return;
        }

        member.setCurrentPoints(member.getCurrentPoints() - item.getPointsRequired());

        // 兑换的东西只是符号化的项目(Free Breakfast、Spa Voucher这类),不会真的去动
        // Room/Booking这些其他模块的真实资料——这里只负责扣分、记一笔交易
        RedemptionTransaction transaction = new RedemptionTransaction(member.getMemberId(),
                item.getItemName(), item.getPointsRequired(), LocalDate.now().toString());
        redemptionHistory.add(transaction);

        loyaltyCLI.displayRedemptionResult(transaction, member.getCurrentPoints());
    }

    // ========== 功能3:手动加分 ==========

    private void doAddPoints() {
        String memberId = loyaltyCLI.promptMemberId();
        if (ValidationUtility.isBlank(memberId)) {
            loyaltyCLI.displayMemberNotFound(memberId, "Add points failed.");
            return;
        }
        Member member = findMemberById(memberId);
        if (member == null) {
            loyaltyCLI.displayMemberNotFound(memberId, "Add points failed.");
            return;
        }

        int pointsAmount = loyaltyCLI.promptPointsAmount();
        if (pointsAmount <= 0) {
            loyaltyCLI.displayInvalidPointsAmount(pointsAmount);
            return;
        }

        // 加分前先记住原本的等级,加完才能比对有没有变,顺便告诉使用者升级了没有
        String tierBefore = member.getTier();

        LocalDate earnedDate = LocalDate.now();
        LocalDate expiryDate = earnedDate.plusMonths(POINTS_VALIDITY_MONTHS);
        awardPoints(member, pointsAmount, earnedDate.toString(), expiryDate.toString());

        loyaltyCLI.displayAddPointsResult(member, pointsAmount, tierBefore);
    }

    // ========== 赚积分(手动加分/退房结账都会触发) ==========

    /**
     * 帮一位会员记一笔积分明细、更新余额,并按新的totalPointsEarned自动重算等级。
     * 现在由doAddPoints()(手动加分)调用;将来退房结账(billing/checkout)接上后,
     * 一样呼叫这个方法即可,不用重写加分逻辑。
     * 只会自动升级,不会自动降级——因为totalPointsEarned只增不减,这是Member entity
     * 原本的设计(避免客人兑换东西花掉积分导致被降级)。
     *
     * @param member 要记分的会员
     * @param pointsAmount 这一批赚了多少分
     * @param earnedDate 这批分什么时候赚的
     * @param expiryDate 这批分什么时候过期
     */
    public void awardPoints(Member member, int pointsAmount, String earnedDate, String expiryDate) {
        ledgerCounter++;
        String ledgerId = "PL" + String.format("%06d", ledgerCounter);
        PointsLedgerEntry entry = new PointsLedgerEntry(ledgerId, member.getMemberId(),
                pointsAmount, earnedDate, expiryDate);

        member.addPointsEntry(entry);
        member.setCurrentPoints(member.getCurrentPoints() + pointsAmount);
        member.setTotalPointsEarned(member.getTotalPointsEarned() + pointsAmount);

        String newTier = TierRankUtility.pointsToTier(member.getTotalPointsEarned());
        if (!newTier.equals(member.getTier())) {
            member.setTier(newTier);
        }
    }

    /**
     * 给退房结账用的入口——FrontDeskControl不用另外拿一份memberList自己查,
     * 直接给memberId跟这次结账赚了多少分,这里负责查会员、算到期日、呼叫awardPoints()。
     * 查不到会员就回传null,呼叫方自行决定要不要显示"找不到会员"这种提示
     * (正常不该发生,因为现在每位入住的客人都一定有memberId,查不到代表资料本身有问题)。
     *
     * @param memberId 要记分的会员ID
     * @param pointsAmount 这次消费赚了多少分
     * @return 更新后的会员物件,查不到就回传null
     */
    public Member awardPointsByMemberId(String memberId, int pointsAmount) {
        Member member = findMemberById(memberId);
        if (member == null) {
            return null;
        }
        LocalDate earnedDate = LocalDate.now();
        LocalDate expiryDate = earnedDate.plusMonths(POINTS_VALIDITY_MONTHS);
        awardPoints(member, pointsAmount, earnedDate.toString(), expiryDate.toString());
        return member;
    }

    // ========== 内部辅助方法 ==========

    /**
     * 在memberList里线性找出memberId相符的那位会员,找不到回传null。
     */
    private Member findMemberById(String memberId) {
        if (memberId == null) {
            return null;
        }
        Iterator<Member> iterator = memberList.getIterator();
        while (iterator.hasNext()) {
            Member member = iterator.next();
            if (member.getMemberId().equals(memberId)) {
                return member;
            }
        }
        return null;
    }

    /**
     * 在redemptionCatalog里线性找出itemName相符的那个兑换项目,找不到回传null。
     */
    private RedemptionItem findItemByName(String itemName) {
        if (itemName == null) {
            return null;
        }
        Iterator<RedemptionItem> iterator = redemptionCatalog.getIterator();
        while (iterator.hasNext()) {
            RedemptionItem item = iterator.next();
            if (item.getItemName().equalsIgnoreCase(itemName)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 把redemptionCatalog按pointsRequired由小到大原地排序(selection sort)。
     * 用ListInterface的position-based方法(getEntry/replace)操作,不靠java.util排序工具。
     */
    private void sortCatalogByPoints() {
        int n = redemptionCatalog.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int minPosition = i;
            RedemptionItem minItem = redemptionCatalog.getEntry(i);
            for (int j = i + 1; j <= n; j++) {
                RedemptionItem candidate = redemptionCatalog.getEntry(j);
                if (candidate.getPointsRequired() < minItem.getPointsRequired()) {
                    minPosition = j;
                    minItem = candidate;
                }
            }
            if (minPosition != i) {
                RedemptionItem temp = redemptionCatalog.getEntry(i);
                redemptionCatalog.replace(i, minItem);
                redemptionCatalog.replace(minPosition, temp);
            }
        }
    }
}

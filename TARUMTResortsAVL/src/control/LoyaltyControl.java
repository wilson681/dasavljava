package control;

import adt.ArrayBasedList;
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
                case 4:
                    doAdjustTier();
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

    if (memberList.isEmpty()) {
        loyaltyCLI.displayNoMembers();
        return;
    }

    // Display members sorted by Member ID
    loyaltyCLI.displayExpiryMemberTable(buildExpiryMemberRows());

    Member member = promptValidMember(null);

    if (member == null) {
        loyaltyCLI.displayCancelled();
        return;
    }

    loyaltyCLI.displayPointsExpiry(
            member,
            member.getPointsLedger().getIterator(),
            calculateActivePoints(member));
}

    // ========== 功能2:兑换积分 ==========

    private void doRedeem() {
        Member member = promptValidMember("Please try again to continue the redemption.");
        if (member == null) {
            loyaltyCLI.displayCancelled();
            return;
        }

        // 先按pointsRequired由小到大把清单排好,再显示——自己写selection sort,
        // 不能用Collections.sort()
        sortCatalogByPoints();
        // 兑换用的余额一定要是"扣掉已过期批次"的真正可用余额,不能直接拿currentPoints——
        // currentPoints只单纯累加/扣减,从来没有主动把过期的那部分减掉
        int activePoints = calculateActivePoints(member);
        loyaltyCLI.displayCatalog(redemptionCatalog.getIterator(), activePoints);

        // 目录已经按 points 升序排好,第一笔就是最便宜的——连它都买不起就直接
        // 告知,不要让使用者在清单里一个一个试
        if (!redemptionCatalog.isEmpty()
                && activePoints < redemptionCatalog.getEntry(1).getPointsRequired()) {
            loyaltyCLI.displayCannotAffordAnything(activePoints,
                    redemptionCatalog.getEntry(1).getPointsRequired());
            return;
        }

        RedemptionItem item = promptValidRedemptionChoice(activePoints);
        if (item == null) {
            loyaltyCLI.displayCancelled();
            return;
        }

        // 直接扣currentPoints本身(不是activePoints)——currentPoints里还留着"已经过期
        // 但从没被减掉"的那一块,每次要用余额时都靠calculateActivePoints()现算扣掉,
        // 不需要、也不应该主动把它从currentPoints里挖掉(那笔明细本身不能被改,只能算)
        member.setCurrentPoints(member.getCurrentPoints() - item.getPointsRequired());

        // 兑换的东西只是符号化的项目(Free Breakfast、Spa Voucher这类),不会真的去动
        // Room/Booking这些其他模块的真实资料——这里只负责扣分、记一笔交易
        RedemptionTransaction transaction = new RedemptionTransaction(member.getMemberId(),
                item.getItemName(), item.getPointsRequired(), LocalDate.now().toString());
        redemptionHistory.add(transaction);

        loyaltyCLI.displayRedemptionResult(transaction, calculateActivePoints(member));
    }

    // ========== 功能3:手动加分 ==========

    private void doAddPoints() {
        
        if (memberList.isEmpty()) {
            loyaltyCLI.displayNoMembers();
            return;
        }

        loyaltyCLI.displayPointsMemberTable(buildPointsMemberRows());
        
        Member member = promptValidMember("Please try again to continue adding points.");
        if (member == null) {
            loyaltyCLI.displayCancelled();
            return;
        }

        int pointsAmount = promptValidPointsAmount();
        if (pointsAmount == Integer.MIN_VALUE) {
            loyaltyCLI.displayCancelled();
            return;
        }

        // 加分前先记住原本的等级,加完才能比对有没有变,顺便告诉使用者升级了没有
        String tierBefore = member.getTier();

        LocalDate earnedDate = LocalDate.now();
        LocalDate expiryDate = earnedDate.plusMonths(POINTS_VALIDITY_MONTHS);
        awardPoints(member, pointsAmount, earnedDate.toString(), expiryDate.toString());

        loyaltyCLI.displayAddPointsResult(member, pointsAmount, tierBefore, calculateActivePoints(member));
    }
// ========== 功能4:手动调整等级 ==========

    /**
     * 让职员手动把某位会员降到较低的等级。
     * 只开放降级——升级应该靠消费赚积分取得,由职员直接给会绕过整个积分机制。
     * 只改 tier,不动 currentPoints 也不动 totalPointsEarned。
     *
     * 注意:awardPoints() 会依 totalPointsEarned 自动重算等级,所以这个降级
     * 会在该会员下一次赚分时被覆盖回去——定位是临时性处分,客人恢复消费後身份自动回复。
     */
    private void doAdjustTier() {

        if (memberList.isEmpty()) {
            loyaltyCLI.displayNoMembers();
            return;
        }

        loyaltyCLI.displayMemberTable(buildMemberRows());

        Member member = promptValidMemberToAdjust("Please try again to continue the tier adjustment.");
        if (member == null) {
            loyaltyCLI.displayCancelled();
            return;
        }

        String[] lowerTiers = buildLowerTiers(member.getTier());
        if (lowerTiers.length == 0) {
            loyaltyCLI.displayAlreadyLowestTier(member.getName(), member.getTier());
            return;
        }

        String oldTier = member.getTier();
        String newTier = promptValidTargetTier(oldTier, lowerTiers);
        if (newTier == null) {
            loyaltyCLI.displayCancelled();
            return;
        }

        if (!loyaltyCLI.promptConfirmAdjustment(member.getName(), oldTier, newTier)) {
            loyaltyCLI.displayAdjustmentCancelled();
            return;
        }

       member.setTier(newTier);

        String lastEarned = findLatestEarnedDate(member);
        loyaltyCLI.displayAdjustmentResult(
                buildMemberRow(member, lastEarned.isEmpty() ? "-" : lastEarned),
                oldTier, newTier,
                TierRankUtility.tierToDiscountPercent(oldTier),
                TierRankUtility.tierToDiscountPercent(newTier));
    }
    
    /**
     * 找出比目前等级低的所有等级。
     * 手动调整只开放降级——升级应该靠消费赚积分取得,由职员直接给会绕过整个积分机制。
     *
     * @param currentTier 会员目前的等级
     * @return 比它低的等级,由高到低(最接近的降一级排第一);
     *         已经在最低等级时回传空阵列
     */
    private String[] buildLowerTiers(String currentTier) {

        String[] allTiers = {"Diamond", "Platinum", "Elite", "Standard"};
        int currentRank = TierRankUtility.tierToRank(currentTier);

        int count = 0;
        for (int i = 0; i < allTiers.length; i++) {
            if (TierRankUtility.tierToRank(allTiers[i]) < currentRank) {
                count++;
            }
        }

        String[] result = new String[count];
        int index = 0;
        for (int i = 0; i < allTiers.length; i++) {
            if (TierRankUtility.tierToRank(allTiers[i]) < currentRank) {
                result[index] = allTiers[i];
                index++;
            }
        }
        return result;
    }
    
    // ========== 报表1:积分即将到期提醒 ==========

    /**
     * filter=未来N天内到期+等级,按到期日升序,跨全体会员扫描——
     * 跟功能1(doViewExpiry)不一样,那个是查单一会员的完整明细,这个是给营销团队
     * 一次拉出全体会员名单去发提醒邮件用。
     */
    void doPointsExpiryReport() {
        int withinDays = loyaltyCLI.promptExpiryWindowDays();
        String tierFilter = loyaltyCLI.promptReportTierFilter();

        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(withinDays);

        ListInterface<String> memberNames = new ArrayBasedList<>();
        ListInterface<String> memberIds = new ArrayBasedList<>();
        ListInterface<String> tiers = new ArrayBasedList<>();
        ListInterface<Integer> pointsAmounts = new ArrayBasedList<>();
        ListInterface<String> expiryDates = new ArrayBasedList<>();

        Iterator<Member> memberIterator = memberList.getIterator();
        while (memberIterator.hasNext()) {
            Member member = memberIterator.next();
            boolean tierMatches = "ALL".equalsIgnoreCase(tierFilter) || tierFilter.equalsIgnoreCase(member.getTier());
            if (!tierMatches) {
                continue;
            }

            Iterator<PointsLedgerEntry> ledgerIterator = member.getPointsLedger().getIterator();
            while (ledgerIterator.hasNext()) {
                PointsLedgerEntry entry = ledgerIterator.next();
                LocalDate expiry = LocalDate.parse(entry.getExpiryDate());
                if (!expiry.isBefore(today) && !expiry.isAfter(cutoff)) {
                    memberNames.add(member.getName());
                    memberIds.add(member.getMemberId());
                    tiers.add(member.getTier());
                    pointsAmounts.add(entry.getPointsAmount());
                    expiryDates.add(entry.getExpiryDate());
                }
            }
        }

        sortByExpiryDateAscending(memberNames, memberIds, tiers, pointsAmounts, expiryDates);

        loyaltyCLI.displayPointsExpiryReportHeader(withinDays, tierFilter);
        if (memberNames.isEmpty()) {
            loyaltyCLI.displayNoReportRecords();
        } else {
            for (int i = 1; i <= memberNames.getNumberOfEntries(); i++) {
                loyaltyCLI.displayPointsExpiryReportRow(memberNames.getEntry(i), memberIds.getEntry(i),
                        tiers.getEntry(i), pointsAmounts.getEntry(i), expiryDates.getEntry(i));
            }
        }
        loyaltyCLI.displayReportEnd();
    }

    /**
     * selection sort:按到期日由近到远,四条平行清单一起换位置。
     */
    private void sortByExpiryDateAscending(ListInterface<String> memberNames, ListInterface<String> memberIds,
                                            ListInterface<String> tiers, ListInterface<Integer> pointsAmounts,
                                            ListInterface<String> expiryDates) {
        int n = expiryDates.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int smallestPosition = i;
            for (int j = i + 1; j <= n; j++) {
                if (expiryDates.getEntry(j).compareTo(expiryDates.getEntry(smallestPosition)) < 0) {
                    smallestPosition = j;
                }
            }
            if (smallestPosition != i) {
                swap(memberNames, i, smallestPosition);
                swap(memberIds, i, smallestPosition);
                swap(tiers, i, smallestPosition);
                swap(pointsAmounts, i, smallestPosition);
                swap(expiryDates, i, smallestPosition);
            }
        }
    }

    // ========== 报表2:最多人兑换的产品报表 ==========

    /**
     * filter=日期区间,遍历现有redemptionHistory,按itemRedeemed分组统计次数
     * 跟消耗积分总额,按次数降序——不需要动任何entity,资料本来就有。
     */
    void doTopRedeemedItemsReport() {
        String fromDate = loyaltyCLI.promptReportFromDate();
        String toDate = loyaltyCLI.promptReportToDate();

        ListInterface<String> itemNames = new ArrayBasedList<>();
        ListInterface<Integer> redemptionCounts = new ArrayBasedList<>();
        ListInterface<Integer> totalPointsUsed = new ArrayBasedList<>();

        Iterator<RedemptionTransaction> iterator = redemptionHistory.getIterator();
        while (iterator.hasNext()) {
            RedemptionTransaction transaction = iterator.next();
            boolean dateMatches = transaction.getDate().compareTo(fromDate) >= 0
                    && transaction.getDate().compareTo(toDate) <= 0;
            if (!dateMatches) {
                continue;
            }

            int position = itemNames.indexOf(transaction.getItemRedeemed());
            if (position == -1) {
                itemNames.add(transaction.getItemRedeemed());
                redemptionCounts.add(1);
                totalPointsUsed.add(transaction.getPointsUsed());
            } else {
                redemptionCounts.replace(position, redemptionCounts.getEntry(position) + 1);
                totalPointsUsed.replace(position, totalPointsUsed.getEntry(position) + transaction.getPointsUsed());
            }
        }

        sortByRedemptionCountDescending(itemNames, redemptionCounts, totalPointsUsed);

        loyaltyCLI.displayTopRedeemedItemsReportHeader(fromDate, toDate);
        if (itemNames.isEmpty()) {
            loyaltyCLI.displayNoReportRecords();
        } else {
            for (int i = 1; i <= itemNames.getNumberOfEntries(); i++) {
                loyaltyCLI.displayTopRedeemedItemsReportRow(itemNames.getEntry(i),
                        redemptionCounts.getEntry(i), totalPointsUsed.getEntry(i));
            }
        }
        loyaltyCLI.displayReportEnd();
    }

    /**
     * selection sort:按兑换次数由大到小,三条平行清单一起换位置。
     */
    private void sortByRedemptionCountDescending(ListInterface<String> itemNames,
                                                  ListInterface<Integer> redemptionCounts,
                                                  ListInterface<Integer> totalPointsUsed) {
        int n = itemNames.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int largestPosition = i;
            for (int j = i + 1; j <= n; j++) {
                if (redemptionCounts.getEntry(j) > redemptionCounts.getEntry(largestPosition)) {
                    largestPosition = j;
                }
            }
            if (largestPosition != i) {
                swap(itemNames, i, largestPosition);
                swap(redemptionCounts, i, largestPosition);
                swap(totalPointsUsed, i, largestPosition);
            }
        }
    }

    /**
     * 把一份ListInterface里两个位置的值互换,给报表的平行清单排序共用。
     */
    private <T> void swap(ListInterface<T> list, int positionA, int positionB) {
        T temp = list.getEntry(positionA);
        list.replace(positionA, list.getEntry(positionB));
        list.replace(positionB, temp);
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

    /**
     * 给退房结算价格用——查这位会员**现在真正**的等级(不是Guest身上入住当天的快照),
     * 折扣要照实际状态算才准。查不到会员就回传null,呼叫方自行决定折扣是0%还是不显示。
     *
     * @param memberId 要查的会员ID
     * @return 该会员现在的等级文字,查不到就回传null
     */
    public String getTierByMemberId(String memberId) {
        Member member = findMemberById(memberId);
        return (member == null) ? null : member.getTier();
    }
/**
     * 把 memberList 里每一位会员组成一行表格资料,按「上次赚积分的日期」升序——
     * 最久没来的排最前面,让职员一眼看出哪些会员该被关注。
     */
    private String buildMemberRows() {

        ListInterface<Member> members = new ArrayBasedList<>();
        ListInterface<String> lastEarnedDates = new ArrayBasedList<>();

        for (int i = 1; i <= memberList.getNumberOfEntries(); i++) {
            Member member = memberList.getEntry(i);
            members.add(member);
            lastEarnedDates.add(findLatestEarnedDate(member));
        }

        sortByLastEarnedAscending(members, lastEarnedDates);

        String result = "";
        for (int i = 1; i <= members.getNumberOfEntries(); i++) {
            String shown = lastEarnedDates.getEntry(i).isEmpty()
                    ? "-" : lastEarnedDates.getEntry(i);
            result = result + buildMemberRow(members.getEntry(i), shown);
        }
        return result;
    }

    /**
     * selection sort:按上次赚分日期由早到晚,两条平行清单一起换位置。
     * 空字串(从未赚过分)在字典序里最小,自然排到最前面——那正是最该被关注的会员。
     */
    private void sortByLastEarnedAscending(ListInterface<Member> members,
                                            ListInterface<String> lastEarnedDates) {
        int n = lastEarnedDates.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int smallestPosition = i;
            for (int j = i + 1; j <= n; j++) {
                if (lastEarnedDates.getEntry(j)
                        .compareTo(lastEarnedDates.getEntry(smallestPosition)) < 0) {
                    smallestPosition = j;
                }
            }
            if (smallestPosition != i) {
                swap(members, i, smallestPosition);
                swap(lastEarnedDates, i, smallestPosition);
            }
        }
    }

    /**
     * 把一位会员组成一行表格资料,栏宽跟 LoyaltyCLI 的表头一致。
     */
    /**
     * 把一位会员组成一行表格资料,栏宽跟 LoyaltyCLI 的表头一致。
     */
    private String buildMemberRow(Member member, String lastEarned) {
        return String.format("%-9s| %-20s| %8d | %-11s| %s%n",
                member.getMemberId(),
                member.getName(),
                calculateActivePoints(member),
                member.getTier(),
                lastEarned);
    }
    
    /**
     * 把 memberList 组成「查看积分到期」用的简表,按 Member ID 升序。
     *
     * <p>跟 buildMemberRows() 分开是因为两个画面要看的东西不同:降级那张要看积分和
     * 上次来访(所以按最久没来排),这张只是让职员从名单里挑一个人来查明细,按 ID
     * 排最好找。</p>
     */
    private String buildExpiryMemberRows() {

        ListInterface<Member> members = new ArrayBasedList<>();
        for (int i = 1; i <= memberList.getNumberOfEntries(); i++) {
            members.add(memberList.getEntry(i));
        }

        sortByMemberIdAscending(members);

        String result = "";
        for (int i = 1; i <= members.getNumberOfEntries(); i++) {
            Member member = members.getEntry(i);
            result = result + String.format("%-9s| %-20s| %-11s%n",
                    member.getMemberId(),
                    member.getName(),
                    member.getTier());
        }
        return result;
    }
/**
     * 把 memberList 组成「手动加分」用的简表,按 Member ID 升序。
     *
     * <p>比到期查询那张多一栏可用积分——加分前先看到目前余额,才好判断要加多少。
     * 显示的是 calculateActivePoints(),不是 currentPoints,因为过期批次不该算进
     * 可用余额里。</p>
     */
    private String buildPointsMemberRows() {

        ListInterface<Member> members = new ArrayBasedList<>();
        for (int i = 1; i <= memberList.getNumberOfEntries(); i++) {
            members.add(memberList.getEntry(i));
        }

        sortByMemberIdAscending(members);

        String result = "";
        for (int i = 1; i <= members.getNumberOfEntries(); i++) {
            Member member = members.getEntry(i);
            result = result + String.format("%-9s| %-20s| %8d | %s%n",
                    member.getMemberId(),
                    member.getName(),
                    calculateActivePoints(member),
                    member.getTier());
        }
        return result;
    }
    /**
     * selection sort:按 Member ID 由小到大。
     * ID 是 M + 四位数字,长度固定,字典序即数值序。
     */
    private void sortByMemberIdAscending(ListInterface<Member> members) {
        int n = members.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int smallestPosition = i;
            for (int j = i + 1; j <= n; j++) {
                if (members.getEntry(j).getMemberId()
                        .compareTo(members.getEntry(smallestPosition).getMemberId()) < 0) {
                    smallestPosition = j;
                }
            }
            if (smallestPosition != i) {
                swap(members, i, smallestPosition);
            }
        }
    }
    
    // ========== 输入重试(格式类校验失败就原地重问,不中止整个操作) ==========

   /**
     * 问会员编号,一直问到查得到人为止。
     *
     * <p>「查无此人」跟「格式错」一样是可以恢复的错误——会员名单就印在这个 prompt
     * 正上方,打错字的人只要重打就好,不该被踢回主选单。只有输入空白才代表取消。</p>
     *
     * @param retryHint 附在 "not found" 后面、各功能自己的提示语;传 null 就只印
     *                  单纯的 "not found"
     * @return 查到的会员;使用者取消时回传 null
     */
    private Member promptValidMember(String retryHint) {

        while (true) {

            String memberId = loyaltyCLI.promptMemberId();

            if (ValidationUtility.isBlank(memberId)) {
                return null;
            }

            Member member = findMemberById(memberId);
            if (member != null) {
                return member;
            }

            if (retryHint == null) {
                loyaltyCLI.displayMemberNotFound(memberId);
            } else {
                loyaltyCLI.displayMemberNotFound(memberId, retryHint);
            }
        }
    }

    /**
     * 跟 promptValidMember() 同样的重试行为,只是用调整等级专用的提问文字。
     *
     * @param retryHint 附在 "not found" 后面的提示语
     * @return 查到的会员;使用者取消时回传 null
     */
    private Member promptValidMemberToAdjust(String retryHint) {

        while (true) {

            String memberId = loyaltyCLI.promptMemberIdToAdjust();

            if (ValidationUtility.isBlank(memberId)) {
                return null;
            }

            Member member = findMemberById(memberId);
            if (member != null) {
                return member;
            }

            loyaltyCLI.displayMemberNotFound(memberId, retryHint);
        }
    }

    /**
     * 兑换清单在doRedeem()里显示之前一定先按points排好序,清单显示的编号(No.)
     * 直接对应ListInterface的position(都是从1开始),不用另外查名字。
     * loyaltyCLI.promptItemNumber()空白时回传Integer.MIN_VALUE代表取消,
     * 跟"打了数字但超出范围"这种要重问的情况分开。
     */
    /**
     * 兑换清单显示前一定先按 points 排好序,清单的 No. 直接对应 ListInterface 的
     * position(都从 1 开始)。
     *
     * <p>「余额不足」跟「编号超出范围」一样属于可以重选的错误,所以两者都在这个
     * 回圈里处理,不会把使用者踢回主选单。只有输入空白才代表取消。</p>
     *
     * @param activePoints 这位会员目前真正可用的余额
     * @return 选到而且买得起的项目;取消时回传 null
     */
    private RedemptionItem promptValidRedemptionChoice(int activePoints) {

        while (true) {

            int itemNumber = loyaltyCLI.promptItemNumber();
            if (itemNumber == Integer.MIN_VALUE) {
                return null;
            }

            if (itemNumber < 1 || itemNumber > redemptionCatalog.getNumberOfEntries()) {
                loyaltyCLI.displayInvalidItemNumber(itemNumber,
                        redemptionCatalog.getNumberOfEntries());
                continue;
            }

            RedemptionItem item = redemptionCatalog.getEntry(itemNumber);
            if (activePoints < item.getPointsRequired()) {
                loyaltyCLI.displayInsufficientPoints(activePoints, item.getPointsRequired());
                continue;
            }

            return item;
        }
    }

    private int promptValidPointsAmount() {
        int pointsAmount;
        while (true) {
            pointsAmount = loyaltyCLI.promptPointsAmount();
            if (pointsAmount == Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            if (pointsAmount > 0) {
                return pointsAmount;
            }
            loyaltyCLI.displayInvalidPointsAmount(pointsAmount);
        }
    }

    /**
     * loyaltyCLI.promptTargetTier()空白回传null代表取消;非空白但不是合法选项
     * 回传""(空字串,不是null)代表格式错误、要重问——用这两种不同的回传值分开
     * "取消"跟"选错"这两种状况。
     */
    private String promptValidTargetTier(String currentTier, String[] options) {
        String newTier;
        while (true) {
            newTier = loyaltyCLI.promptTargetTier(currentTier, options);
            if (newTier == null) {
                return null;
            }
            if (!newTier.isEmpty()) {
                return newTier;
            }
            loyaltyCLI.displayInvalidTier();
        }
    }

    // ========== 内部辅助方法 ==========

    /**
     * 算出这位会员真正能拿去兑换/显示的余额——currentPoints本身只是单纯的
     * "历史累加-历史兑换",从来没有主动把过期批次扣掉,所以每次要用余额之前都要
     * 现算一次"已经过期、但还没被扣掉"的部分,从currentPoints里减掉再拿去用。
     * 不直接改pointsLedger或存一个"已扣过期"旗标——PointsLedgerEntry是不可变的明细,
     * 过期批次本身还是要留着给doViewExpiry()完整显示(只是被标成EXPIRED)。
     * 因为这个函数每次都是从currentPoints现减,不是累加状态,重复呼叫也不会重复扣。
     */
    private int calculateActivePoints(Member member) {
        int expiredTotal = 0;
        LocalDate today = LocalDate.now();
        Iterator<PointsLedgerEntry> iterator = member.getPointsLedger().getIterator();
        while (iterator.hasNext()) {
            PointsLedgerEntry entry = iterator.next();
            if (LocalDate.parse(entry.getExpiryDate()).isBefore(today)) {
                expiredTotal += entry.getPointsAmount();
            }
        }
        return member.getCurrentPoints() - expiredTotal;
    }
    /**
     * 找出这位会员积分明细里最晚的一笔 earnedDate。
     * 积分是在退房结帐那一刻入帐的,所以这个日期就代表他最後一次完成入住的时间,
     * 不需要在 Member 上另外多存一个「上次来访」栏位。
     *
     * @param member 要查的会员
     * @return 最晚的赚分日期;从未赚过分时回传空字串
     */
    private String findLatestEarnedDate(Member member) {

        String latest = "";
        Iterator<PointsLedgerEntry> iterator = member.getPointsLedger().getIterator();
        while (iterator.hasNext()) {
            String earned = iterator.next().getEarnedDate();
            // 日期是 yyyy-MM-dd,字典序即时间序,直接比字串就够
            if (earned != null && earned.compareTo(latest) > 0) {
                latest = earned;
            }
        }
        return latest;
    }
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
            if (ValidationUtility.idsMatch(member.getMemberId(), memberId)) {
                return member;
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

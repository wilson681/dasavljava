package boundary;

import entity.Member;
import entity.PointsLedgerEntry;
import entity.RedemptionItem;
import entity.RedemptionTransaction;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Scanner;
import utility.ValidationUtility;

/**
 * LoyaltyCLI.java - 模块5(Loyalty and Rewards Service)的 console 界面。
 *
 * @author 某某
 *
 * 说明:
 * - 只负责跟使用者对话(Scanner输入、println输出),不做任何业务判断
 * - 所有 console 印出来的文字都用英文,代码注释才用中文
 */
public class LoyaltyCLI {

    private static final String DIVIDER = "--------------------------------------------------------";
    // 报表表格自己的分隔线——以前借用 LEDGER/CATALOG 那两条,栏宽对不上会歪掉
    private static final String REPORT_TABLE_DIVIDER =
            "------------------------------------------------------------------------";
    private static final int MAX_BAR_WIDTH = 20;   // 星号柱状图最多印几颗

    private static final String LEDGER_TABLE_DIVIDER = "---- ---------- -------------- -------------- ----------";
    private static final String CATALOG_TABLE_DIVIDER = "---- -------------------------- -------------- ----------";
  
    private static final String MEMBER_TABLE_HEADER =
            String.format("%-9s| %-20s| %8s | %-11s| %s",
                    "MemberId", "MemberName", "Point", "VipTier", "Last Visited Date");
    private static final String MEMBER_TABLE_DIVIDER =
            "---------|---------------------|----------|------------|------------";
    private static final String EXPIRY_MEMBER_TABLE_HEADER =
        String.format("%-9s| %-20s| %-11s",
                "MemberId", "MemberName", "Tier");

    private static final String EXPIRY_MEMBER_TABLE_DIVIDER =
            "---------|---------------------|------------";
    private static final String POINTS_MEMBER_TABLE_HEADER =
            String.format("%-9s| %-20s| %8s | %s",
                    "MemberId", "MemberName", "Point", "Tier");

    private static final String POINTS_MEMBER_TABLE_DIVIDER =
            "---------|---------------------|----------|------------";
    private Scanner scanner;

    public LoyaltyCLI() {
        scanner = new Scanner(System.in);
    }

    public int displayMenuAndGetChoice() {
        System.out.println();
        System.out.println("===== Loyalty and Rewards Service =====");
        System.out.println();
        System.out.println("  1) View Points Expiry");
        System.out.println("  2) Redeem Points");
        System.out.println("  3) Add Points (Manual)");
        System.out.println("  4) Manual Tier Downgrade");
        System.out.println("  0) Back to Main Menu");
        System.out.println();
        System.out.print("Enter your choice: ");

        String input = scanner.nextLine().trim();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public void displayInvalidChoice() {
        System.out.println("Invalid input, please try again.");
    }

    public void displayCancelled() {
        System.out.println("Cancelled. Returning to menu.");
    }

    public String promptMemberId() {
        System.out.print("Enter member ID (blank to cancel): ");
        return scanner.nextLine().trim();
    }

    public void displayMemberNotFound(String memberId) {
        System.out.println("Member ID " + memberId + " not found.");
    }

    /**
     * 跟其他模块(如VIP登记)统一格式:"Member ID X not found. <动作> failed."
     * 给会导致某个动作直接中止的查无此人情境用(兑换、加分),纯查看资料(doViewExpiry)不用。
     */
    public void displayMemberNotFound(String memberId, String failedAction) {
        System.out.println("Member ID " + memberId + " not found. " + failedAction);
    }
    
    public void displayExpiryMemberTable(String rows) {
        System.out.println();
        System.out.println("===== Member Directory =====");
        System.out.println();
        System.out.println(EXPIRY_MEMBER_TABLE_HEADER);
        System.out.println(EXPIRY_MEMBER_TABLE_DIVIDER);
        System.out.print(rows);
        System.out.println();
    }
    public void displayPointsMemberTable(String rows) {
        System.out.println();
        System.out.println("===== Member Directory =====");
        System.out.println();
        System.out.println(POINTS_MEMBER_TABLE_HEADER);
        System.out.println(POINTS_MEMBER_TABLE_DIVIDER);
        System.out.print(rows);
        System.out.println();
    }
    // ========== 功能1:查看积分到期状况 ==========

    public void displayPointsExpiry(Member member, Iterator<PointsLedgerEntry> ledger, int activePoints) {
        System.out.println();
        System.out.println("===== Points Ledger: " + member.getName() + " (" + member.getMemberId() + ") =====");
        System.out.println("Current Balance: " + activePoints + " pts | Tier: " + member.getTier());
        System.out.println();
        if (!ledger.hasNext()) {
            System.out.println("No points batches on record.");
            return;
        }
        System.out.println(String.format("%-4s %-10s %-14s %-14s %s", "No.", "Points", "Earned", "Expires", "Status"));
        System.out.println(LEDGER_TABLE_DIVIDER);
        LocalDate today = LocalDate.now();
        int rank = 1;
        while (ledger.hasNext()) {
            PointsLedgerEntry entry = ledger.next();
            // 过期批次照样列出来(方便对账/查历史),但用EXPIRED标出来——
            // 单纯文字标记,不用ANSI加粗转义码,避免在NetBeans/不支援ANSI的console下变成乱码
            boolean expired = LocalDate.parse(entry.getExpiryDate()).isBefore(today);
            String status = expired ? "EXPIRED" : "";
            System.out.println(String.format("%-4d %-10d %-14s %-14s %s",
                    rank, entry.getPointsAmount(), entry.getEarnedDate(), entry.getExpiryDate(), status));
            rank++;
        }
    }

    // ========== 功能2:兑换积分 ==========

    public void displayCatalog(Iterator<RedemptionItem> catalog, int currentPoints) {
        System.out.println();
        System.out.println("===== Redemption Catalog (sorted by points required) =====");
        System.out.println("Your balance: " + currentPoints + " pts");
        System.out.println();
        System.out.println(String.format("%-4s %-26s %-14s %s", "No.", "Item", "Points Required", "Affordable"));
        System.out.println(CATALOG_TABLE_DIVIDER);
        int rank = 1;
        while (catalog.hasNext()) {
            RedemptionItem item = catalog.next();
            String affordable = currentPoints >= item.getPointsRequired() ? "Yes" : "No";
            System.out.println(String.format("%-4d %-26s %-14d %s",
                    rank, item.getItemName(), item.getPointsRequired(), affordable));
            rank++;
        }
    }

    public int promptItemNumber() {
        System.out.print("Enter the No. of the item to redeem (blank to cancel): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return Integer.MIN_VALUE;
        }
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public void displayInvalidItemNumber(int itemNumber, int catalogSize) {
        System.out.println("\"" + itemNumber + "\" is not a valid No. Enter a number between 1 and " + catalogSize + ".");
    }

    public void displayInsufficientPoints(int currentPoints, int required) {
        System.out.println("Not enough points. You have " + currentPoints
                + " pts, this item needs " + required + " pts.");
    }
    public void displayCannotAffordAnything(int currentPoints, int cheapestRequired) {
        System.out.println();
        System.out.println("You have " + currentPoints + " pts. The cheapest item needs "
                + cheapestRequired + " pts.");
        System.out.println("Nothing can be redeemed right now.");
    }
    public void displayRedemptionResult(RedemptionTransaction transaction, int remainingPoints) {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("  REDEMPTION SUCCESSFUL");
        System.out.println(DIVIDER);
        System.out.println("  Item                 : " + transaction.getItemRedeemed());
        System.out.println("  Points Used          : " + transaction.getPointsUsed());
        System.out.println("  Remaining Balance    : " + remainingPoints);
        System.out.println(DIVIDER);
    }

    // ========== 功能3:手动加分 ==========

    public int promptPointsAmount() {
        System.out.print("Enter points to add (blank to cancel): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return Integer.MIN_VALUE;
        }
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public void displayInvalidPointsAmount(int pointsAmount) {
        System.out.println("Invalid points amount (" + pointsAmount + "). Must be a positive whole number.");
    }

    public void displayAddPointsResult(Member member, int pointsAdded, String tierBefore, int activePoints) {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("  POINTS ADDED");
        System.out.println(DIVIDER);
        System.out.println("  Member               : " + member.getName() + " (" + member.getMemberId() + ")");
        System.out.println("  Points Added         : +" + pointsAdded);
        System.out.println("  New Balance          : " + activePoints);
        System.out.println("  Tier                 : " + member.getTier());
        if (!tierBefore.equals(member.getTier())) {
            System.out.println("  >> Upgraded from " + tierBefore + " to " + member.getTier() + "!");
        }
        System.out.println(DIVIDER);
    }
    // ========== 功能4:手动调整等级 ==========

    public void displayMemberTable(String rows) {
        System.out.println();
        System.out.println("===== Member Directory (longest inactive first) =====");
        System.out.println();
        System.out.println(MEMBER_TABLE_HEADER);
        System.out.println(MEMBER_TABLE_DIVIDER);
        System.out.print(rows);
    }

    public void displayNoMembers() {
        System.out.println("No members are registered yet.");
    }

    public String promptMemberIdToAdjust() {
        System.out.println();
        System.out.print("Enter the Member ID to adjust (blank to cancel): ");
        return scanner.nextLine().trim();
    }

    
   /**
     * 让使用者从「比目前低的等级」里选一个,用数字选避免拼错。
     *
     * @param currentTier 这位会员现在的等级
     * @param options 可选的较低等级
     * @return 选到的等级文字,选项无效则回传 null
     */
    public String promptTargetTier(String currentTier, String[] options) {
        System.out.println();
        System.out.println("Current tier: " + currentTier);
        System.out.println("Select the tier to downgrade to:");
        for (int i = 0; i < options.length; i++) {
            System.out.println("  " + (i + 1) + ") " + options[i]);
        }
        System.out.print("Enter your choice (blank to cancel): ");

        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return null;
        }
        try {
            int choice = Integer.parseInt(input);
            if (choice >= 1 && choice <= options.length) {
                return options[choice - 1];
            }
        } catch (NumberFormatException e) {
            // fall through to the invalid-but-not-blank return below
        }
        return "";
    }

    public void displayInvalidTier() {
        System.out.println("Invalid choice. No tier was changed.");
    }

    public boolean promptConfirmAdjustment(String name, String oldTier, String newTier) {
        while (true) {
            System.out.print("Move " + name + " from " + oldTier + " to " + newTier + "? (y/n): ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("y")) {
                return true;
            }
            if (input.equalsIgnoreCase("n")) {
                return false;
            }
            System.out.println("Invalid input. Please enter y or n.");
        }
    }

    public void displayAdjustmentCancelled() {
        System.out.println("Tier adjustment cancelled.");
    }

    public void displayAdjustmentResult(String row, String oldTier, String newTier,
                                        int oldDiscount, int newDiscount) {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("  TIER DOWNGRADE SUCCESSFUL");
        System.out.println(DIVIDER);
        System.out.println("  Tier                 : " + oldTier + "  ->  " + newTier);
        System.out.println("  Room Discount        : " + oldDiscount + "%  ->  " + newDiscount + "%");
        System.out.println(DIVIDER);
        System.out.println(MEMBER_TABLE_HEADER);
        System.out.println(MEMBER_TABLE_DIVIDER);
        System.out.print(row);
    }
    public void displayAlreadyLowestTier(String name, String tier) {
        System.out.println(name + " is already on " + tier + ", the lowest tier. "
                + "Nothing to downgrade.");
    }
    // ========== 报表共用输入/输出 ==========

    public String promptReportTierFilter() {
        System.out.println();
        System.out.println("Tier Filter: 1) All  2) Standard  3) Elite  4) Platinum  5) Diamond");
        System.out.print("Enter your choice: ");
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "2":
                return "Standard";
            case "3":
                return "Elite";
            case "4":
                return "Platinum";
            case "5":
                return "Diamond";
            default:
                return "ALL";
        }
    }

    public String promptReportFromDate() {
        while (true) {
            System.out.println();
            System.out.print("Enter from-date (yyyy-MM-dd, blank = no lower bound): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return "0000-00-00";
            }
            String normalised = ValidationUtility.normalizeDate(input);
            if (normalised != null) {
                return normalised;
            }
            System.out.println("Invalid date, please use yyyy-MM-dd (e.g. 2026-08-13).");
        }
    }

    public String promptReportToDate() {
        while (true) {
            System.out.println();
            System.out.print("Enter to-date (yyyy-MM-dd, blank = no upper bound): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return "9999-99-99";
            }
            String normalised = ValidationUtility.normalizeDate(input);
            if (normalised != null) {
                return normalised;
            }
            System.out.println("Invalid date, please use yyyy-MM-dd (e.g. 2026-08-13).");
        }
    }

    public void displayNoReportRecords() {
        System.out.println("No records match the selected criteria.");
    }

    /**
     * 报表收尾线。用报表自己那条(72字),不是模块通用的 DIVIDER(56字)——
     * 收尾线要跟上面表格的宽度对齐,不然框会缺一截。
     * 这个方法只有报表1、报表2在用。
     */
    public void displayReportEnd() {
        System.out.println(REPORT_TABLE_DIVIDER);
    }

    // ========== 报表1:积分即将到期提醒 ==========

    /**
     * 到期视窗天数。让使用者直接打数字而不是选1/2/3——"我要看未来几天"
     * 是随场景变的:平常看30天,做月底清单看90天,查全部就打3650。
     * 打错要重问,不能默默用一个使用者没打过的天数去跑报表。
     */
    public int promptExpiryWindowDays() {
        while (true) {
            System.out.println();
            System.out.print("Show points expiring within how many days (e.g. 30): ");
            String input = scanner.nextLine().trim();
            try {
                int days = Integer.parseInt(input);
                if (days >= 0) {
                    return days;
                }
            } catch (NumberFormatException e) {
                // 落到下面的重问
            }
            System.out.println("Invalid number of days, please enter 0 or a positive whole number.");
        }
    }

    public void displayPointsExpiryReportHeader(int withinDays, String tierFilter) {
        System.out.println();
        System.out.println(REPORT_TABLE_DIVIDER);
        System.out.println("          POINTS EXPIRY REPORT");
        System.out.println(REPORT_TABLE_DIVIDER);
        System.out.println("Generated at    : " + generatedAt());
        System.out.println("Expiring Within : " + withinDays + " day(s)");
        System.out.println("Tier Filter     : " + tierFilter);
        System.out.println(REPORT_TABLE_DIVIDER);
        System.out.println(String.format("%-20s %-9s %-9s %8s   %-10s %9s",
                "Member", "MemberId", "Tier", "Points", "Expires", "Days Left"));
        System.out.println(REPORT_TABLE_DIVIDER);
    }

    /**
     * 一批点数一行。剩几天是这一行的重点——日期本身要读者自己去跟今天相减,
     * 直接印天数才看得出急不急,今天到期的再多标一个记号。
     */
    public void displayPointsExpiryReportRow(String memberName, String memberId, String tier,
                                              int pointsAmount, String expiryDate, int daysLeft) {
        String marker = (daysLeft == 0) ? "   <-- TODAY" : "";
        System.out.println(String.format("%-20s %-9s %-9s %8d   %-10s %9d%s",
                memberName, memberId, tier, pointsAmount, expiryDate, daysLeft, marker));
    }

    /**
     * 只印两个数:要联络几位会员、总共多少点数会消失。
     * 表格行数不等于人数(一位会员可以有好几批点数),所以人数要另外印。
     */
    public void displayPointsExpirySummary(int membersAffected, int totalExpiringPoints) {
        System.out.println(REPORT_TABLE_DIVIDER);
        System.out.printf("  Members affected : %d%n", membersAffected);
        System.out.printf("  Points expiring  : %d%n", totalExpiringPoints);
    }

    public void displayExpiringByTierHeader() {
        System.out.println();
        System.out.println("EXPIRING BY TIER   (each * = 5% of points expiring)");
    }

    /**
     * 一个等级一行,即使那一级是0也照印——"Diamond 这次没有点数要过期"
     * 本身就是要看到的资讯,那一行不见了会以为是漏算。
     */
    public void displayExpiringByTierRow(String tier, int pointsExpiring, int totalExpiringPoints) {
        double share = (totalExpiringPoints <= 0) ? 0.0 : pointsExpiring * 100.0 / totalExpiringPoints;
        System.out.printf("  %-20s %7d  (%5.1f%%)  %s%n", tier, pointsExpiring, share, bar(share));
    }

    // ========== 报表2:最多人兑换的产品报表 ==========

    public void displayTopRedeemedItemsReportHeader(String fromDate, String toDate, String tierFilter) {
        System.out.println();
        System.out.println(REPORT_TABLE_DIVIDER);
        System.out.println("          TOP REDEEMED ITEMS REPORT");
        System.out.println(REPORT_TABLE_DIVIDER);
        System.out.println("Generated at : " + generatedAt());
        System.out.println("Date Range   : " + fromDate + " to " + toDate);
        System.out.println("Tier Filter  : " + tierFilter);
        System.out.println(REPORT_TABLE_DIVIDER);
        System.out.println(String.format("%-30s %13s %13s %13s",
                "Item", "Points Each", "Redemptions", "Points Used"));
        System.out.println(REPORT_TABLE_DIVIDER);
    }

    /**
     * 两行结论刻意分开印:"最受欢迎"看次数,"最烧点数"看点数总额,
     * 这两个常常不是同一个奖品,而后者才是点数负债的来源。
     */
    public void displayTopRedeemedItemsSummary(int totalRedemptions, int totalPointsSpent,
                                                String mostPopularItem, int mostPopularCount,
                                                String biggestSinkItem, int biggestSinkPoints) {
        double sinkShare = (totalPointsSpent <= 0) ? 0.0 : biggestSinkPoints * 100.0 / totalPointsSpent;

        System.out.println(REPORT_TABLE_DIVIDER);
        System.out.printf("  Total redemptions   : %d%n", totalRedemptions);
        System.out.printf("  Total points spent  : %d%n", totalPointsSpent);
        System.out.printf("  Most popular        : %s (%d redemption(s))%n",
                mostPopularItem, mostPopularCount);
        System.out.printf("  Biggest points sink : %s (%d pts, %.1f%% of all points spent)%n",
                biggestSinkItem, biggestSinkPoints, sinkShare);
    }

    /**
     * 柱状图画的是"多少人换过",不是"烧掉多少点"——报表叫 TOP REDEEMED,
     * 问的就是人气。点数那条线索交给上面的 Biggest points sink 那一行。
     */
    public void displayMostRedeemedItemsHeader() {
        System.out.println();
        System.out.println("MOST REDEEMED ITEMS   (each * = 5% of all redemptions)");
    }

    public void displayMostRedeemedItemsRow(String itemName, int redemptionCount, int totalRedemptions) {
        double share = (totalRedemptions <= 0) ? 0.0 : redemptionCount * 100.0 / totalRedemptions;
        System.out.printf("  %-20s %7d  (%5.1f%%)  %s%n", itemName, redemptionCount, share, bar(share));
    }

    // ========== 报表共用的显示工具 ==========

    /**
     * 报表产生的时间戳。报表是给管理层看的文件,要标明这份数字是什么时候的快照。
     */
    private String generatedAt() {
        return java.time.LocalDateTime.now().withNano(0)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 星号柱状图:一颗星 = 5% 占比。点数的量级差很大(几百到几千),
     * 按百分比画才比得出来。
     */
    private String bar(double percentage) {
        int stars = (int) (percentage / 5.0);
        if (stars > MAX_BAR_WIDTH) {
            stars = MAX_BAR_WIDTH;
        }
        String result = "";
        for (int i = 0; i < stars; i++) {
            result = result + "*";
        }
        return result;
    }

    /**
     * 一个奖品一行。Points Each 是目录上的单价,不是算出来的平均——印它是为了
     * 解释旁边那两栏:便宜的奖品换的人多、贵的奖品换的人少却吃掉大半点数。
     * 三栏之间对得起来(单价 x 次数 = 点数总额),读的人可以自己验。
     *
     * @param pointsRequired 目录单价;-1 代表这个奖品已经不在目录里了
     */
    public void displayTopRedeemedItemsReportRow(String itemName, int pointsRequired,
                                                  int redemptionCount, int totalPointsUsed) {
        String each = (pointsRequired < 0) ? "-" : String.valueOf(pointsRequired);
        System.out.println(String.format("%-30s %13s %13d %13d",
                itemName, each, redemptionCount, totalPointsUsed));
    }

    /**
     * 上面的表回答"换了什么",这个区块回答"谁在换"。
     *
     * 关键那一栏是 Avg Points——高等级会员点数多,会存着换贵的奖品;
     * 低等级点数少,倾向马上换掉换得起的便宜奖品。平均值把这个差别摊开来看,
     * 用等级filter一次只能看一级,这里是四级并排比。
     */
    public void displayRedemptionByTierHeader() {
        System.out.println();
        System.out.println("REDEMPTION BEHAVIOUR BY TIER");
        System.out.println(String.format("  %-16s %11s %13s %11s",
                "Tier", "Redemptions", "Points Used", "Avg Points"));
    }

    public void displayRedemptionByTierRow(String tier, int redemptions, int pointsUsed) {
        // 一次都没换过就没有平均可言,印"-"而不是0——0会被读成"平均花0点"
        String average = (redemptions == 0) ? "-" : String.valueOf(pointsUsed / redemptions);
        System.out.println(String.format("  %-16s %11d %13d %11s",
                tier, redemptions, pointsUsed, average));
    }
}

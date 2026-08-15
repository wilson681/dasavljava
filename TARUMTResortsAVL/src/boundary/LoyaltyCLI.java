package boundary;

import entity.Member;
import entity.PointsLedgerEntry;
import entity.RedemptionItem;
import entity.RedemptionTransaction;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Scanner;

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
        System.out.println();
        System.out.print("Enter from-date (yyyy-MM-dd, blank = no lower bound): ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? "0000-00-00" : input;
    }

    public String promptReportToDate() {
        System.out.print("Enter to-date (yyyy-MM-dd, blank = no upper bound): ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? "9999-99-99" : input;
    }

    public void displayNoReportRecords() {
        System.out.println("No records match the selected criteria.");
    }

    public void displayReportEnd() {
        System.out.println(DIVIDER);
    }

    // ========== 报表1:积分即将到期提醒 ==========

    public int promptExpiryWindowDays() {
        System.out.println();
        System.out.print("Show points expiring within how many days (e.g. 30): ");
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    public void displayPointsExpiryReportHeader(int withinDays, String tierFilter) {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("          POINTS EXPIRY REPORT");
        System.out.println(DIVIDER);
        System.out.println("Expiring Within : " + withinDays + " day(s)");
        System.out.println("Tier Filter     : " + tierFilter);
        System.out.println(DIVIDER);
        System.out.println(String.format("%-20s %-9s %-10s %-8s %s",
                "Member", "MemberId", "Tier", "Points", "Expires"));
        System.out.println(LEDGER_TABLE_DIVIDER);
    }

    public void displayPointsExpiryReportRow(String memberName, String memberId, String tier,
                                              int pointsAmount, String expiryDate) {
        System.out.println(String.format("%-20s %-9s %-10s %-8d %s",
                memberName, memberId, tier, pointsAmount, expiryDate));
    }

    // ========== 报表2:最多人兑换的产品报表 ==========

    public void displayTopRedeemedItemsReportHeader(String fromDate, String toDate) {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("          TOP REDEEMED ITEMS REPORT");
        System.out.println(DIVIDER);
        System.out.println("Date Range : " + fromDate + " to " + toDate);
        System.out.println(DIVIDER);
        System.out.println(String.format("%-26s %-16s %s", "Item", "Redemptions", "Total Points Used"));
        System.out.println(CATALOG_TABLE_DIVIDER);
    }

    public void displayTopRedeemedItemsReportRow(String itemName, int redemptionCount, int totalPointsUsed) {
        System.out.println(String.format("%-26s %-16d %d", itemName, redemptionCount, totalPointsUsed));
    }
}

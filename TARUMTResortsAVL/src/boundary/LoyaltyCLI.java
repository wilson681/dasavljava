package boundary;

import entity.Member;
import entity.PointsLedgerEntry;
import entity.RedemptionItem;
import entity.RedemptionTransaction;
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
    private static final String LEDGER_TABLE_DIVIDER = "---- ---------- -------------- --------------";
    private static final String CATALOG_TABLE_DIVIDER = "-------------------------- -------------- ----------";

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

    public String promptMemberId() {
        System.out.print("Enter member ID: ");
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

    // ========== 功能1:查看积分到期状况 ==========

    public void displayPointsExpiry(Member member, Iterator<PointsLedgerEntry> ledger) {
        System.out.println();
        System.out.println("===== Points Ledger: " + member.getName() + " (" + member.getMemberId() + ") =====");
        System.out.println("Current Balance: " + member.getCurrentPoints() + " pts | Tier: " + member.getTier());
        System.out.println();
        if (!ledger.hasNext()) {
            System.out.println("No points batches on record.");
            return;
        }
        System.out.println(String.format("%-4s %-10s %-14s %-14s", "No.", "Points", "Earned", "Expires"));
        System.out.println(LEDGER_TABLE_DIVIDER);
        int rank = 1;
        while (ledger.hasNext()) {
            PointsLedgerEntry entry = ledger.next();
            System.out.println(String.format("%-4d %-10d %-14s %-14s",
                    rank, entry.getPointsAmount(), entry.getEarnedDate(), entry.getExpiryDate()));
            rank++;
        }
    }

    // ========== 功能2:兑换积分 ==========

    public void displayCatalog(Iterator<RedemptionItem> catalog, int currentPoints) {
        System.out.println();
        System.out.println("===== Redemption Catalog (sorted by points required) =====");
        System.out.println("Your balance: " + currentPoints + " pts");
        System.out.println();
        System.out.println(String.format("%-26s %-14s %s", "Item", "Points Required", "Affordable"));
        System.out.println(CATALOG_TABLE_DIVIDER);
        while (catalog.hasNext()) {
            RedemptionItem item = catalog.next();
            String affordable = currentPoints >= item.getPointsRequired() ? "Yes" : "No";
            System.out.println(String.format("%-26s %-14d %s",
                    item.getItemName(), item.getPointsRequired(), affordable));
        }
    }

    public String promptItemName() {
        System.out.print("Enter item name to redeem: ");
        return scanner.nextLine().trim();
    }

    public void displayItemNotFound(String itemName) {
        System.out.println("Item \"" + itemName + "\" not found in the catalog.");
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
        System.out.print("Enter points to add: ");
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public void displayInvalidPointsAmount(int pointsAmount) {
        System.out.println("Invalid points amount (" + pointsAmount + "). Must be a positive whole number.");
    }

    public void displayAddPointsResult(Member member, int pointsAdded, String tierBefore) {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("  POINTS ADDED");
        System.out.println(DIVIDER);
        System.out.println("  Member               : " + member.getName() + " (" + member.getMemberId() + ")");
        System.out.println("  Points Added         : +" + pointsAdded);
        System.out.println("  New Balance          : " + member.getCurrentPoints());
        System.out.println("  Tier                 : " + member.getTier());
        if (!tierBefore.equals(member.getTier())) {
            System.out.println("  >> Upgraded from " + tierBefore + " to " + member.getTier() + "!");
        }
        System.out.println(DIVIDER);
    }
}

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
 * Handles user input and output for the Loyalty and Rewards Service module.
 *
 * @author Lim Wei Shern
 */
public class LoyaltyCLI {

    private static final String DIVIDER = "--------------------------------------------------------";
    private static final String REPORT_TABLE_DIVIDER = "------------------------------------------------------------------------";
    private static final int MAX_BAR_WIDTH = 20;

    private static final String LEDGER_TABLE_DIVIDER = "---- ---------- ----------- -------------- -------------- --------------";
    private static final String CATALOG_TABLE_DIVIDER = "---- -------------------------- -------------- ----------";

    private static final String MEMBER_TABLE_HEADER = String.format("%-9s| %-20s| %8s | %-11s| %s",
            "MemberId", "MemberName", "Point", "VipTier", "Last Visited Date");
    private static final String MEMBER_TABLE_DIVIDER = "---------|---------------------|----------|------------|------------";
    private static final String EXPIRY_MEMBER_TABLE_HEADER = String.format("%-9s| %-20s| %-11s",
            "MemberId", "MemberName", "Tier");

    private static final String EXPIRY_MEMBER_TABLE_DIVIDER = "---------|---------------------|------------";
    private static final String POINTS_MEMBER_TABLE_HEADER = String.format("%-9s| %-20s| %8s | %s",
            "MemberId", "MemberName", "Point", "Tier");

    private static final String POINTS_MEMBER_TABLE_DIVIDER = "---------|---------------------|----------|------------";
    private Scanner scanner;

    public LoyaltyCLI() {
        scanner = new Scanner(System.in);
    }

    // Main Menu
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

    // Member Selection
    public String promptMemberId() {
        System.out.print("Enter member ID (blank to cancel): ");
        return scanner.nextLine().trim();
    }

    public void displayMemberNotFound(String memberId) {
        System.out.println("Member ID " + memberId + " not found.");
    }

    /**
     * Displays a member-not-found message for an operation that cannot continue.
     *
     * @param memberId     the member ID entered
     * @param failedAction the action that could not be completed
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
    // Option 1: View Points Expiry

    /**
     * Displays the member's points ledger and marks each batch as active, partly
     * or fully used, or expired.
     */
    public void displayPointsExpiry(Member member, Iterator<PointsLedgerEntry> ledger, int activePoints) {
        System.out.println();
        System.out.println("===== Points Ledger: " + member.getName() + " (" + member.getMemberId() + ") =====");
        System.out.println("Current Balance: " + activePoints + " pts | Tier: " + member.getTier());
        System.out.println();
        if (!ledger.hasNext()) {
            System.out.println("No points batches on record.");
            return;
        }
        System.out.println(String.format("%-4s %-10s %-11s %-14s %-14s %s",
                "No.", "Points", "Remaining", "Earned", "Expires", "Status"));
        System.out.println(LEDGER_TABLE_DIVIDER);
        LocalDate today = LocalDate.now();
        int rank = 1;
        while (ledger.hasNext()) {
            PointsLedgerEntry entry = ledger.next();
            // Expired batches stay visible for history. Points are spent earliest
            // expiry first, so a batch can also be partly or fully used up.
            // Plain text markers only, no ANSI escapes, so the table still lines up
            // in consoles that do not support them.
            boolean expired = LocalDate.parse(entry.getExpiryDate()).isBefore(today);
            String status;
            if (expired) {
                status = "EXPIRED";
            } else if (entry.getRemainingPoints() == 0) {
                status = "USED";
            } else if (entry.getRemainingPoints() < entry.getPointsAmount()) {
                status = "PARTIALLY USED";
            } else {
                status = "ACTIVE";
            }
            System.out.println(String.format("%-4d %-10d %-11d %-14s %-14s %s",
                    rank, entry.getPointsAmount(), entry.getRemainingPoints(),
                    entry.getEarnedDate(), entry.getExpiryDate(), status));
            rank++;
        }
    }

    // Option 2: Redeem Points

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

    /**
     * Prompts for an item number from the redemption catalog.
     *
     * @return the selected number, Integer.MIN_VALUE if cancelled,
     *         or -1 if the input is not numeric
     */
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
        System.out
                .println("\"" + itemNumber + "\" is not a valid No. Enter a number between 1 and " + catalogSize + ".");
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

    // Option 3: Add Points Manually

    /**
     * Prompts for the number of points to add.
     *
     * @return the entered amount, Integer.MIN_VALUE if cancelled,
     *         or -1 if the input is not numeric
     */

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
    // Option 4: Manual Tier Downgrade

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
     * Prompts the user to select a lower membership tier.
     *
     * @param currentTier the member's current tier
     * @param options     available lower tiers
     * @return the selected tier, null if cancelled, or an empty string if invalid
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
            // Invalid non-blank input is handled by the return value below.
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
    // Report Input / Output

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

    /**
     * Prompts for the lower date bound used by a report.
     * Blank input represents no lower bound.
     */
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

    /**
     * Prompts for the upper date bound used by a report.
     * Blank input represents no upper bound.
     */
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
     * Displays the closing divider used by report tables.
     */
    public void displayReportEnd() {
        System.out.println(REPORT_TABLE_DIVIDER);
    }

    // Report 1: Points Expiry Report

    /**
     * Prompts for the number of days included in the expiry window.
     * Invalid values are rejected until a non-negative whole number is entered.
     *
     * @return the selected number of days
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
                // Invalid input is handled by the message below.
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
     * Displays one expiring points batch.
     * Batches expiring today are marked for emphasis.
     */
    public void displayPointsExpiryReportRow(String memberName, String memberId, String tier,
            int pointsAmount, String expiryDate, int daysLeft) {
        String marker = (daysLeft == 0) ? "   <-- TODAY" : "";
        System.out.println(String.format("%-20s %-9s %-9s %8d   %-10s %9d%s",
                memberName, memberId, tier, pointsAmount, expiryDate, daysLeft, marker));
    }

    /**
     * Displays the number of affected members and total expiring points.
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
     * Displays the expiring points and percentage share for one tier.
     */
    public void displayExpiringByTierRow(String tier, int pointsExpiring, int totalExpiringPoints) {
        double share = (totalExpiringPoints <= 0) ? 0.0 : pointsExpiring * 100.0 / totalExpiringPoints;
        System.out.printf("  %-20s %7d  (%5.1f%%)  %s%n", tier, pointsExpiring, share, bar(share));
    }

    // Report 2: Top Redeemed Items Report
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
     * Displays overall redemption totals and the leading items.
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
     * Displays the redemption share chart based on redemption count.
     */
    public void displayMostRedeemedItemsHeader() {
        System.out.println();
        System.out.println("MOST REDEEMED ITEMS   (each * = 5% of all redemptions)");
    }

    public void displayMostRedeemedItemsRow(String itemName, int redemptionCount, int totalRedemptions) {
        double share = (totalRedemptions <= 0) ? 0.0 : redemptionCount * 100.0 / totalRedemptions;
        System.out.printf("  %-20s %7d  (%5.1f%%)  %s%n", itemName, redemptionCount, share, bar(share));
    }

    // Report Display Helpers

    /**
     * Returns the timestamp used when generating a report.
     */
    private String generatedAt() {
        return java.time.LocalDateTime.now().withNano(0)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Builds a percentage bar where each '*' represents 5%.
     * The bar is limited to MAX_BAR_WIDTH stars.
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
     * Displays one item in the redeemed-items report.
     * A missing catalog item is displayed with "-" for its point cost.
     *
     * @param pointsRequired the catalog point cost, or -1 if the item
     *                       is no longer in the catalog
     */
    public void displayTopRedeemedItemsReportRow(String itemName, int pointsRequired,
            int redemptionCount, int totalPointsUsed) {
        String each = (pointsRequired < 0) ? "-" : String.valueOf(pointsRequired);
        System.out.println(String.format("%-30s %13s %13d %13d",
                itemName, each, redemptionCount, totalPointsUsed));
    }

    /**
     * Displays redemption statistics grouped by membership tier.
     */
    public void displayRedemptionByTierHeader() {
        System.out.println();
        System.out.println("REDEMPTION BEHAVIOUR BY TIER");
        System.out.println(String.format("  %-16s %11s %13s %11s",
                "Tier", "Redemptions", "Points Used", "Avg Points"));
    }

    public void displayRedemptionByTierRow(String tier, int redemptions, int pointsUsed) {
        // No average exists when there are no redemptions.
        String average = (redemptions == 0) ? "-" : String.valueOf(pointsUsed / redemptions);
        System.out.println(String.format("  %-16s %11d %13d %11s",
                tier, redemptions, pointsUsed, average));
    }
}

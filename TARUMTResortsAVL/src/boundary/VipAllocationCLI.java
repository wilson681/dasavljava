package boundary;

import entity.Booking;
import entity.Room;
import java.util.Iterator;
import java.util.Scanner;
import utility.ValidationUtility;

/**
 * VipAllocationCLI.java - console for module 2 (VIP & Loyalty Tier Priority Room Allocation)
 *
 * @author Chong Kim Seng
 */
public class VipAllocationCLI {

    private static final String DIVIDER = "--------------------------------------------------------";
    private static final String TABLE_DIVIDER =
            "---- ------------ ------------------ -------------------- ----------- ------------ ------";
    private static final String REPORT_DIVIDER =
            "----------------------------------------------------------------------------";

    private Scanner scanner;

    public VipAllocationCLI() {
        scanner = new Scanner(System.in);
    }

    /**
     * @return number enter by user, not number return -1
     */
    public int displayMenuAndGetChoice() {
        System.out.println();
        System.out.println("===== VIP & Loyalty Tier Priority Allocation =====");
        System.out.println();
        System.out.println("  1) VIP Registration");
        System.out.println("  2) Cancel Waiting");
        System.out.println("  3) View VIP Waiting List");
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

    // ========== module 1 :vip register ==========

    public String promptMemberId() {
        System.out.print("Enter member ID (blank to cancel): ");
        return scanner.nextLine().trim();
    }

    public String promptRoomType() {
        System.out.println();
        System.out.println("Room Type: 1) Standard  2) Deluxe  3) Suite");
        System.out.print("Select room type (blank to cancel): ");
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                return "Standard";
            case "2":
                return "Deluxe";
            case "3":
                return "Suite";
            default:
                return choice;
        }
    }

    public void displayMemberNotFound(String memberId) {
        System.out.println("Member ID " + memberId + " not found.");
    }

    public void displayInvalidRoomType(String roomType) {
        System.out.println("Room type \"" + roomType + "\" is invalid. Please try again.");
    }

    public void displayRegistrationResult(Booking booking, String tier) {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("  REGISTRATION SUCCESSFUL");
        System.out.println(DIVIDER);
        System.out.println("  Booking ID           : " + booking.getBookingId());
        System.out.println("  Confirmation Number  : " + booking.getConfirmationNumber());
        System.out.println("  Tier                 : " + tier);
        System.out.println("  Room Type            : " + booking.getRequestedRoomType());
        System.out.println(DIVIDER);
    }

    /**
     * After register one room for the same VIP, ask if they want to add another room under the same con num
     * (booking multiple room use same confirmationNumber)
     */
    public boolean promptAddAnotherRoom() {
        System.out.println();
        while (true) {
            System.out.print("Add another room for this guest? (y/n): ");
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

   
    public int promptNumberOfNights() {
        System.out.print("Enter number of nights (blank to cancel): ");
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

    public void displayInvalidNumberOfNights(int numberOfNights) {
        System.out.println("Invalid number of nights (" + numberOfNights + "). Must be a positive whole number.");
    }

    public void displayAllocationResult(Booking booking, Room room,
                                         double originalPrice, int discountPercent, double finalPrice) {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("  ROOM ALLOCATED");
        System.out.println(DIVIDER);
        System.out.println("  Guest                : " + booking.getGuestNameSnapshot());
        System.out.println("  Confirmation Number  : " + booking.getConfirmationNumber());
        System.out.println("  Room                 : " + room.getRoomNumber());
        System.out.println("  Original Price       : RM" + originalPrice);
        System.out.println("  Tier Discount        : " + discountPercent + "%");
        System.out.println("  Estimated Price      : RM" + finalPrice + "  (finalised at check-out)");
        System.out.println(DIVIDER);
    }

    // ========== module 2 : cancel Q ==========

    public String promptBookingIdToCancel() {
        System.out.print("Enter the booking ID to cancel (blank to cancel): ");
        return scanner.nextLine().trim();
    }

    public void displayCancelResult(boolean success) {
        System.out.println();
        if (success) {
            System.out.println("Cancelled successfully.");
        } else {
            System.out.println("Booking not found. Cancellation failed.");
        }
    }

    // ======module 3 view the vip waiting list ==========

    public void displayWaitingList(String roomType, Iterator<Booking> waitingList) {
        System.out.println();
        System.out.println("===== " + roomType + " VIP Waiting List (highest priority first) =====");
        System.out.println();
        if (!waitingList.hasNext()) {
            System.out.println("No one is currently waiting.");
            return;
        }
        System.out.println(String.format("%-4s %-12s %-18s %-20s %-11s %-12s %s",
                "No.", "Booking ID", "Confirmation No.", "Guest", "Room Type", "Status", "Room"));
        System.out.println(TABLE_DIVIDER);
        int rank = 1;
        while (waitingList.hasNext()) {
            Booking booking = waitingList.next();
            System.out.println(String.format("%-4d %-12s %-18s %-20s %-11s %-12s %s",
                    rank,
                    booking.getBookingId(),
                    booking.getConfirmationNumber(),
                    booking.getGuestNameSnapshot(),
                    booking.getRequestedRoomType(),
                    booking.getStatus(),
                    (booking.getAssignedRoomNo() == null ? "-" : booking.getAssignedRoomNo())));
            rank++;
        }
    }

    // ====report ==========

    /**
     * @return (0=Standard,1=Elite,2=Platinum,3=Diamond),"All" return -1
     *        
     */
    public int promptReportTierRank() {
        System.out.println();
        System.out.println("Tier Filter: 1) All  2) Standard  3) Elite  4) Platinum  5) Diamond");
        System.out.print("Enter your choice: ");
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "2":
                return 0;
            case "3":
                return 1;
            case "4":
                return 2;
            case "5":
                return 3;
            default:
                return -1;
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

    public void displayReportEnd() {
        System.out.println(REPORT_DIVIDER);
    }

    private String tierFilterLabel(int tierRankFilter) {
        switch (tierRankFilter) {
            case 0:
                return "Standard";
            case 1:
                return "Elite";
            case 2:
                return "Platinum";
            case 3:
                return "Diamond";
            default:
                return "All";
        }
    }

    // ====== report 1 ,vip wait list ==========

    public void displayVipWaitingListReportHeader(int tierRankFilter,
                                                   String generatedAt) {
        System.out.println();
        System.out.println(REPORT_DIVIDER);
        System.out.println("                    LIVE VIP WAITING QUEUE & SLA REPORT");
        System.out.println(REPORT_DIVIDER);
        System.out.println("Generated At  : " + generatedAt);
        System.out.println("Tier Filter   : " + tierFilterLabel(tierRankFilter));
        System.out.println("Priority Rule : Higher tier first; same tier follows arrival order");
        System.out.println(REPORT_DIVIDER);
        System.out.println(String.format("%-2s %-9s %-16s %-9s %-10s %-7s %-6s %s",
                "#", "Booking", "Guest", "Tier", "Room Type",
                "Waiting", "Target", "SLA Status"));
        System.out.println(REPORT_DIVIDER);
    }

    public void displayVipWaitingListReportRow(int priority, String bookingId,
                                                String guestName, String tier,
                                                String roomType, int waitMinutes,
                                                int targetMinutes, boolean breached) {
        System.out.println(String.format("%-2d %-9s %-16s %-9s %-10s %-7s %-6s %s",
                priority, bookingId, truncate(guestName, 16), tier, roomType,
                formatDuration(waitMinutes), "<=" + targetMinutes + "m",
                breached ? "BREACHED" : "WITHIN SLA"));
    }

    public void displayVipWaitingListReportSummary(int totalWaiting,
                                                    int withinSla,
                                                    int breached,
                                                    String nextGuest,
                                                    String nextRoomType,
                                                    String longestGuest,
                                                    int longestWaitMinutes) {
        System.out.println(REPORT_DIVIDER);
        System.out.println("SUMMARY");
        System.out.printf("  Total waiting    : %d%n", totalWaiting);
        System.out.printf("  Within SLA       : %d%n", withinSla);
        System.out.printf("  SLA breached     : %d%n", breached);
        System.out.printf("  Highest priority : %s (%s)%n", nextGuest, nextRoomType);
        System.out.printf("  Longest waiting  : %s (%s)%n",
                longestGuest, formatDuration(longestWaitMinutes));
        System.out.println();
        if (breached > 0) {
            System.out.printf("ACTION REQUIRED: %d request(s) exceeded the tier SLA target.%n",
                    breached);
        } else {
            System.out.println("STATUS: All current VIP requests remain within their SLA targets.");
        }
    }

    // ========== Report 2:Room Assignment Target Rate  ==========

    public void displayTierSlaReportHeader(int tierRankFilter,
                                           String fromDate, String toDate,
                                           String generatedAt,
                                           double complianceGoal,
                                           int diamondTarget,
                                           int platinumTarget,
                                           int eliteTarget,
                                           int standardTarget) {
        System.out.println();
        System.out.println(REPORT_DIVIDER);
        System.out.println("                     TIER ALLOCATION SLA PERFORMANCE REPORT");
        System.out.println(REPORT_DIVIDER);
        System.out.println("Generated At    : " + generatedAt);
        System.out.println("Tier Filter     : " + tierFilterLabel(tierRankFilter));
        System.out.println("Date Range      : " + dateRangeLabel(fromDate, toDate));
        System.out.printf("Compliance Goal : %.1f%%%n", complianceGoal);
        System.out.printf("SLA Targets     : Diamond<=%dm | Platinum<=%dm | Elite<=%dm | Standard<=%dm%n",
                diamondTarget, platinumTarget, eliteTarget, standardTarget);
        System.out.println("Status Rule     : PASS >= 90% | WATCH >= 75% | FAIL < 75%");
        System.out.println("Sort Order      : Lowest compliance first, then longest average wait");
        System.out.println(REPORT_DIVIDER);
        System.out.println(String.format(
                "%-9s  %-6s  %5s  %3s  %6s  %7s  %-8s  %-8s  %s",
                "Tier", "Target", "Total", "Met", "Breach",
                "Rate", "Avg", "Worst", "Status"));
        System.out.println(REPORT_DIVIDER);
    }

    public void displayTierSlaReportRow(String tier, int targetMinutes,
                                        int count, int metCount,
                                        int breachedCount,
                                        double compliancePercent,
                                        double averageWaitMinutes,
                                        int worstWaitMinutes,
                                        String status) {
       // Using a limited format like "Xd Yh" prevents the column from being stretched.
        System.out.println(String.format(
                "%-9s  %-6s  %5d  %3d  %6d  %6.1f%%  %-8s  %-8s  %s",
                tier, "<=" + targetMinutes + "m", count, metCount, breachedCount,
                compliancePercent, formatDuration((int) Math.round(averageWaitMinutes)),
                formatDuration(worstWaitMinutes), status));
    }

    public void displayTierSlaReportSummary(int totalAllocations,
                                            int totalMet,
                                            int totalBreached,
                                            double overallCompliance,
                                            String overallStatus,
                                            String weakestTier,
                                            double weakestCompliance,
                                            double complianceGoal) {
        System.out.println(REPORT_DIVIDER);
        System.out.println("SUMMARY");
        System.out.printf("  Total allocations  : %d%n", totalAllocations);
        System.out.printf("  Within SLA         : %d%n", totalMet);
        System.out.printf("  SLA breaches       : %d%n", totalBreached);
        System.out.printf("  Overall compliance : %.1f%% (%s)%n",
                overallCompliance, overallStatus);
        if (totalBreached == 0) {
            System.out.println("  Weakest tier       : None (all recorded allocations met SLA)");
        } else {
            System.out.printf("  Weakest tier       : %s (%.1f%% compliance)%n",
                    weakestTier, weakestCompliance);
        }
        System.out.println();
        if (totalBreached == 0) {
            System.out.printf("KEY FINDING: All allocations met their targets (goal: %.1f%%).%n",
                    complianceGoal);
        } else if (overallCompliance >= complianceGoal) {
            System.out.printf("KEY FINDING: The %.1f%% goal was achieved, but %d breach(es) still require review.%n",
                    complianceGoal, totalBreached);
        } else {
            System.out.printf("ACTION REQUIRED: %s is the weakest tier and overall compliance is below %.1f%%.%n",
                    weakestTier, complianceGoal);
        }
    }

    private String dateRangeLabel(String fromDate, String toDate) {
        boolean noLowerBound = "0000-00-00".equals(fromDate);
        boolean noUpperBound = "9999-99-99".equals(toDate);
        if (noLowerBound && noUpperBound) {
            return "ALL DATES";
        }
        if (noLowerBound) {
            return "Up to " + toDate;
        }
        if (noUpperBound) {
            return "From " + fromDate;
        }
        return fromDate + " to " + toDate;
    }

   
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        if (maxLength <= 3) {
            return value.substring(0, maxLength);
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private String formatDuration(int minutes) {
        if (minutes < 60) {
            return minutes + "m";
        }
        if (minutes < 24 * 60) {
            return (minutes / 60) + "h " + (minutes % 60) + "m";
        }
        int days = minutes / (24 * 60);
        int remainingHours = (minutes % (24 * 60)) / 60;
        return days + "d " + remainingHours + "h";
    }
}

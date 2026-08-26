package boundary;

import entity.Booking;
import entity.Room;
import java.util.Iterator;
import java.util.Scanner;
import utility.ValidationUtility;

/**
 * VipAllocationCLI.java - 模块2(VIP & Loyalty Tier Priority Room Allocation)的 console 界面。
 *
 * @author 某某
 *
 * 说明:
 * - 只负责跟使用者对话(Scanner输入、println输出),不做任何业务判断
 * - 参数/回传值里出现的 Booking、Room 只是拿来显示用,不会在这里被建立或修改
 * - 所有 console 印出来的文字都用英文,代码注释才用中文
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
     * 印出这个模块的选单,读使用者的选择。
     * @return 使用者输入的数字,不是数字则回传 -1
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

    // ========== 功能1:VIP登记 ==========

    public String promptMemberId() {
        System.out.print("Enter member ID (blank to cancel): ");
        return scanner.nextLine().trim();
    }

    public String promptRoomType() {
        System.out.println();
        System.out.println("Room Type: 1) Standard  2) Deluxe  3) Suite");
        System.out.print("Select room type (blank to cancel): ");
        // 把数字选项换成实际房型文字,打错或直接打文字都原样传出去,交给Control判断合不合法
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
     * 同一位VIP登记完一间房后,问要不要在同一个确认号下继续加订下一间房
     * (一次订多间房时用,让多笔 Booking 共用同一个 confirmationNumber)
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

    // ========== 分房结果(登记后自动触发,不再是独立菜单动作) ==========

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

    // ========== 功能2:取消排队 ==========

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

    // ========== 功能3:查看VIP等待名单 ==========

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

    // ========== 报表共用输入/输出 ==========

    /**
     * @return 等级排名数字(0=Standard,1=Elite,2=Platinum,3=Diamond),选"All"回传-1
     *         代表不限等级——不能用0当"All"的哨兵值,因为0现在是Standard真正的排名
     *         (业务规则改了之后,Standard会员也能走VIP登记这条路,只是排名垫底)
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

    // ========== 报表1:VIP等待名单实时报表 ==========

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

    // ========== 报表2:等级分房达标率报表 ==========

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
        // Avg/Worst 用跟 Wait Time 报表同一套 formatDuration(),不是裸分钟数+m——
        // 等待时间一旦跨天(比如1440分钟),裸数字会撑爆原本预留的栏宽,
        // 换成"Xd Yh"这种有上限的格式就不会再被大数字挤歪。
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

    /**
     * 把变长文字(比如客人姓名)硬性封顶在 maxLength 以内,超过就截断加"..."。
     * 固定宽度的 %-Ns 格式碰到超长字串不会自动截断,只会把那一列跟后面所有栏位
     * 一起往右推,导致整份报表看起来歪一边——这个方法保证不管资料多长,
     * 印出来的宽度永远不会超过表头预留的栏宽。
     * 用ASCII的三个句点,不用Unicode省略号"…"——那个字元在某些console的编码下
     * (比如Windows默认的非UTF-8 codepage)会印成乱码。
     */
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

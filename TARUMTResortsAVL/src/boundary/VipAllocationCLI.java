package boundary;

import entity.Booking;
import entity.Room;
import java.util.Iterator;
import java.util.Scanner;

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
        System.out.println("Member ID " + memberId + " not found. Registration failed.");
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
     * @return 等级排名数字(1=Elite,2=Platinum,3=Diamond),选"All"回传0代表不限等级
     */
    public int promptReportTierRank() {
        System.out.println();
        System.out.println("Tier Filter: 1) All  2) Elite  3) Platinum  4) Diamond");
        System.out.print("Enter your choice: ");
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "2":
                return 1;
            case "3":
                return 2;
            case "4":
                return 3;
            default:
                return 0;
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

    private String tierFilterLabel(int tierRankFilter) {
        switch (tierRankFilter) {
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

    public void displayVipWaitingListReportHeader(int tierRankFilter) {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("             VIP WAITING LIST REPORT");
        System.out.println(DIVIDER);
        System.out.println("Tier Filter : " + tierFilterLabel(tierRankFilter));
        System.out.println(DIVIDER);
        System.out.println(String.format("%-20s %-10s %-20s %s",
                "Guest", "Tier", "Arrival Time", "Wait (min)"));
        System.out.println("-------------------------------------------------------");
    }

    public void displayVipWaitingListReportRow(String guestName, String tier,
                                                String arrivalTime, int waitMinutes) {
        System.out.println(String.format("%-20s %-10s %-20s %d",
                guestName, tier, arrivalTime, waitMinutes));
    }

    // ========== 报表2:等级分房达标率报表 ==========

    public void displayTierSlaReportHeader(int tierRankFilter, String fromDate, String toDate) {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("             TIER ALLOCATION SLA REPORT");
        System.out.println(DIVIDER);
        System.out.println("Tier Filter : " + tierFilterLabel(tierRankFilter));
        System.out.println("Date Range  : " + fromDate + " to " + toDate);
        System.out.println(DIVIDER);
        System.out.println(String.format("%-10s %-8s %s", "Tier", "Count", "Avg Wait (min)"));
        System.out.println("-------------------------------------------------------");
    }

    public void displayTierSlaReportRow(String tier, int count, double averageWaitMinutes) {
        System.out.println(String.format("%-10s %-8d %.1f", tier, count, averageWaitMinutes));
    }
}

package boundary;

import entity.Booking;
import entity.Room;
import java.util.Iterator;
import java.util.Scanner;

/**
 * WalkInCLI.java - 模块1(Walk-In Registrations & Standard Booking)的 console 界面。
 *
 * @author 某某
 *
 * 说明:
 * - 只负责跟使用者对话(Scanner输入、println输出),不做任何业务判断
 * - 所有 console 印出来的文字都用英文,代码注释才用中文
 */
public class WalkInCLI {

    private static final String DIVIDER = "--------------------------------------------------------";
    private static final String TABLE_DIVIDER =
            "---- ------------ ------------------ -------------------- ----------- ------------ ------";

    private Scanner scanner;

    public WalkInCLI() {
        scanner = new Scanner(System.in);
    }

    public int displayMenuAndGetChoice() {
        System.out.println();
        System.out.println("===== Walk-In Registrations & Standard Booking =====");
        System.out.println();
        System.out.println("  1) Register New Guest");
        System.out.println("  2) Cancel Waiting");
        System.out.println("  3) View Waiting List");
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

    // ========== 功能1:登记新客人 ==========

    public String promptName() {
        System.out.print("Enter guest name: ");
        return scanner.nextLine().trim();
    }

    public void displayInvalidName(String name) {
        System.out.println("Guest name cannot be blank.");
    }

    public String promptPhone() {
        System.out.print("Enter phone number: ");
        return scanner.nextLine().trim();
    }

    public void displayInvalidPhone(String phone) {
        System.out.println("Phone number \"" + phone + "\" is invalid. Must contain digits only.");
    }

    public String promptRoomType() {
        System.out.println();
        System.out.println("Room Type: 1) Standard  2) Deluxe  3) Suite");
        System.out.print("Select room type: ");
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

    public void displayInvalidRoomType(String roomType) {
        System.out.println("Room type \"" + roomType + "\" is invalid. Please try again.");
    }

    public void displayRegistrationResult(Booking booking) {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("  REGISTRATION SUCCESSFUL");
        System.out.println(DIVIDER);
        System.out.println("  Booking ID           : " + booking.getBookingId());
        System.out.println("  Confirmation Number  : " + booking.getConfirmationNumber());
        System.out.println("  Room Type            : " + booking.getRequestedRoomType());
        System.out.println(DIVIDER);
    }

    /**
     * 同一位客人登记完一间房后,问要不要在同一个确认号下继续加订下一间房
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
        System.out.print("Enter number of nights: ");
        try {
            return Integer.parseInt(scanner.nextLine().trim());
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
        System.out.print("Enter the booking ID to cancel: ");
        return scanner.nextLine().trim();
    }

    public void displayInvalidBookingId(String bookingId) {
        System.out.println("\"" + bookingId + "\" is not a valid booking ID.");
    }

    public void displayCancelResult(boolean success) {
        System.out.println();
        if (success) {
            System.out.println("Cancelled successfully.");
        } else {
            System.out.println("Booking not found. Cancellation failed.");
        }
    }

    // ========== 功能3:查看排队名单 ==========

    public void displayWaitingList(String roomType, Iterator<Booking> waitingList) {
        System.out.println();
        System.out.println("===== " + roomType + " Walk-In Waiting List (arrival order) =====");
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

    // ========== 报表共用输入 ==========

    /**
     * @return "yyyy-MM-dd"格式的日期,留空回传"ALL"代表不限日期
     */
    public String promptReportDate() {
        System.out.println();
        System.out.print("Filter by date (yyyy-MM-dd, blank = all dates): ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? "ALL" : input;
    }

    public String promptReportRoomType() {
        System.out.println();
        System.out.println("Room Type Filter: 1) All  2) Standard  3) Deluxe  4) Suite");
        System.out.print("Enter your choice: ");
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "2":
                return "Standard";
            case "3":
                return "Deluxe";
            case "4":
                return "Suite";
            default:
                return "ALL";
        }
    }

    public void displayNoReportRecords() {
        System.out.println("No records match the selected criteria.");
    }

    public void displayReportEnd() {
        System.out.println(DIVIDER);
    }

    // ========== 报表1:每日入住登记明细表 ==========

    public void displayDailyRegistrationReportHeader(String dateFilter, String roomTypeFilter) {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("             DAILY REGISTRATION REPORT");
        System.out.println(DIVIDER);
        System.out.println("Date Filter      : " + dateFilter);
        System.out.println("Room Type Filter : " + roomTypeFilter);
        System.out.println(DIVIDER);
        System.out.println(String.format("%-20s %-20s %-11s %-10s %s",
                "Guest", "Registered At", "Room Type", "Allocated", "Wait (min)"));
        System.out.println("-------------------------------------------------------------------");
    }

    public void displayDailyRegistrationReportRow(String guestName, String registeredAt,
                                                   String roomType, boolean allocated, int waitMinutes) {
        System.out.println(String.format("%-20s %-20s %-11s %-10s %d",
                guestName, registeredAt, roomType, (allocated ? "Yes" : "No"), waitMinutes));
    }

    // ========== 报表2:等待时长分析报表 ==========

    public int promptMinWaitMinutes() {
        System.out.println();
        System.out.print("Enter minimum wait time to include (minutes): ");
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void displayWaitTimeAnalysisHeader(int minWaitMinutes, String dateFilter) {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("             WAIT TIME ANALYSIS REPORT");
        System.out.println(DIVIDER);
        System.out.println("Minimum Wait  : " + minWaitMinutes + " min");
        System.out.println("Date Filter   : " + dateFilter);
        System.out.println(DIVIDER);
        System.out.println(String.format("%-12s %-20s %-11s %s",
                "Booking ID", "Guest", "Room Type", "Wait (min)"));
        System.out.println("-------------------------------------------------------");
    }

    public void displayWaitTimeAnalysisRow(String bookingId, String guestName, String roomType, int waitMinutes) {
        System.out.println(String.format("%-12s %-20s %-11s %d",
                bookingId, guestName, roomType, waitMinutes));
    }

    public void displayHourlyBreakdownHeader() {
        System.out.println();
        System.out.println("Average Wait Time by Hour of Registration:");
        System.out.println(String.format("%-6s %-8s %s", "Hour", "Count", "Avg Wait (min)"));
        System.out.println("-----------------------");
    }

    public void displayHourlyBreakdownRow(int hour, int count, double averageWaitMinutes) {
        System.out.println(String.format("%02d:00  %-8d %.1f", hour, count, averageWaitMinutes));
    }
}

package boundary;

import entity.Room;
import java.util.Iterator;
import java.util.Scanner;

/**
 * HousekeepingCLI.java
 * Boundary class for the Housekeeping and Task Log module.
 *
 * Responsible only for user input and console output.
 *
 * @author YOUR FULL NAME
 */
public class HousekeepingCLI {

    private final Scanner scanner;

    public HousekeepingCLI() {
        scanner = new Scanner(System.in);
    }

    // =========================================================
    // Main Menu
    // =========================================================

    public int displayMenuAndGetChoice() {

        System.out.println();
        System.out.println("===== Housekeeping and Task Log =====");
        System.out.println();
        System.out.println("1. View Rooms Requiring Housekeeping");
        System.out.println("2. Update Room Status");
        System.out.println("3. Roll Back Latest Status");
        System.out.println("4. View Room Status History");
        System.out.println("0. Back to Main Menu");
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

    /**
     * Pauses the screen before returning to the
     * Housekeeping menu.
     */
    public void promptContinue() {

        System.out.println();
        System.out.print("Press Enter to continue...");
        scanner.nextLine();
    }

    // =========================================================
    // Option 1
    // View Rooms Requiring Housekeeping
    // =========================================================

    public void displayHousekeepingRooms(
            Iterator<Room> rooms) {

        System.out.println();
        System.out.println(
                "===== Rooms Requiring Housekeeping ====="
        );
        System.out.println();

        if (!rooms.hasNext()) {

            System.out.println(
                    "No rooms currently require housekeeping."
            );

            return;
        }

        System.out.println(
                String.format(
                        "%-10s %-15s %-25s",
                        "Room",
                        "Room Type",
                        "Status"
                )
        );

        System.out.println(
                "------------------------------------------------------"
        );

        while (rooms.hasNext()) {

            Room room = rooms.next();

            System.out.println(
                    String.format(
                            "%-10s %-15s %-25s",
                            room.getRoomNumber(),
                            room.getRoomType(),
                            room.getStatus()
                    )
            );
        }
    }

    /**
     * Shows every room on record (not filtered to the housekeeping pipeline) —
     * used before options 2/3/4 prompt for a room number, so the staff can see
     * a real room number to type instead of guessing.
     */
    public void displayAllRooms(
            Iterator<Room> rooms) {

        System.out.println();
        System.out.println(
                "===== All Rooms ====="
        );
        System.out.println();

        System.out.println(
                String.format(
                        "%-10s %-15s %-25s",
                        "Room",
                        "Room Type",
                        "Status"
                )
        );

        System.out.println(
                "------------------------------------------------------"
        );

        while (rooms.hasNext()) {

            Room room = rooms.next();

            System.out.println(
                    String.format(
                            "%-10s %-15s %-25s",
                            room.getRoomNumber(),
                            room.getRoomType(),
                            room.getStatus()
                    )
            );
        }
    }

    // =========================================================
    // Shared Input / Output
    // =========================================================

    public String promptRoomNumber() {

        System.out.println();
        System.out.print("Enter room number (blank to cancel): ");

        return scanner.nextLine().trim();
    }

    public void displayInvalidRoomNumber(
            String roomNumber) {

        System.out.println();

        System.out.println(
                "\"" + roomNumber + "\" is not a valid room number. Must contain digits only."
        );
    }

    public void displayRoomNotFound(
            String roomNumber) {

        System.out.println();

        System.out.println(
                "Room " + roomNumber + " not found."
        );
    }

    public void displayRoomNotInPipeline(
            String roomNumber,
            String currentStatus) {

        System.out.println();

        System.out.println(
                "Room " + roomNumber + " (status: " + currentStatus
                + ") is not in the housekeeping pipeline. Only rooms with"
                + " NEEDS_CLEANING, CLEANING_IN_PROGRESS, or INSPECTED status"
                + " can be updated here."
        );
    }

    // =========================================================
    // Option 2
    // Update Room Status
    // =========================================================

    public void displayCurrentRoomStatus(
            String roomNumber,
            String currentStatus) {

        System.out.println();

        System.out.println(
                "Room " + roomNumber
                + " current status: "
                + currentStatus
        );
    }

   /**
     * Prompts for the next room status.
     *
     * <p>The housekeeping pipeline is strictly linear, so at any point there
     * is exactly one legal next status. Only that one is offered, because
     * listing the other three and then rejecting them would be offering a
     * choice that does not exist.</p>
     *
     * @param suggestedNextStatus the only status this room may move to
     * @return the chosen status, or null when the user cancels
     */
    public String promptNewStatus(String suggestedNextStatus) {

        if (suggestedNextStatus == null) {
            return null;
        }

        while (true) {

            System.out.println();
            System.out.println("Select New Status:");
            System.out.println("1. " + suggestedNextStatus);
            System.out.println();
            System.out.print("Enter your choice (blank to cancel): ");

            String choice = scanner.nextLine().trim();

            if (choice.isEmpty()) {
                return null;
            }

            if (choice.equals("1")) {
                return suggestedNextStatus;
            }

            System.out.println("Invalid input, please enter 1 or leave blank to cancel.");
        }
    }

    public void displayInvalidStatus() {

        System.out.println();

        System.out.println(
                "Invalid room status selection or status transition."
        );
    }

    public void displayStatusUpdated(
            String roomNumber,
            String previousStatus,
            String newStatus) {

        System.out.println();
        System.out.println(
                "===== STATUS UPDATED ====="
        );

        System.out.println(
                "Room            : " + roomNumber
        );

        System.out.println(
                "Previous Status : " + previousStatus
        );

        System.out.println(
                "New Status      : " + newStatus
        );
    }

    // =========================================================
    // Option 3
    // Roll Back Latest Status
    // =========================================================

    public void displayRollbackNotAvailable(
            String roomNumber) {

        System.out.println();

        System.out.println(
                "Room " + roomNumber
                + " has no previous status to restore."
        );
    }

    public void displayRollbackResult(
            String roomNumber,
            String removedStatus,
            String restoredStatus) {

        System.out.println();
        System.out.println(
                "===== STATUS ROLLBACK SUCCESSFUL ====="
        );

        System.out.println(
                "Room            : " + roomNumber
        );

        System.out.println(
                "Removed Status  : " + removedStatus
        );

        System.out.println(
                "Restored Status : " + restoredStatus
        );
    }

    // =========================================================
    // Option 4
    // View Room Status History
    // =========================================================

    public void displayRoomStatusHistory(
            String roomNumber,
            String currentStatus,
            Iterator<String> statusHistory) {

        System.out.println();
        System.out.println(
                "===== ROOM STATUS HISTORY ====="
        );

        System.out.println(
                "Room           : " + roomNumber
        );

        System.out.println(
                "Current Status : " + currentStatus
        );

        System.out.println();

        if (!statusHistory.hasNext()) {

            System.out.println(
                    "No status history is available."
            );

            return;
        }

        // statusHistory是Stack的迭代器,吐出来的顺序是"最新的先出来"——横向显示要
        // 由旧到新排,所以每读到一笔就往目前累积字串的最前面插(用" -> "串接),
        // 读完整个迭代顺序自然就反过来,变成"最旧在最左、最新在最右"
        String timeline = "";

        while (statusHistory.hasNext()) {

            String status = statusHistory.next();

            timeline = status + (timeline.isEmpty() ? "" : " -> " + timeline);
        }

        System.out.println("Oldest                                                        Most Recent");
        System.out.println(timeline);
    }

    // =========================================================
    // Report 1
    // Housekeeping Status Report
    // =========================================================

    public String promptReportRoomType() {

        System.out.println();
        System.out.println("Room Type Filter:");
        System.out.println("1. All");
        System.out.println("2. Standard");
        System.out.println("3. Deluxe");
        System.out.println("4. Suite");
        System.out.println();
        System.out.print("Enter your choice (blank to cancel): ");

        String choice = scanner.nextLine().trim();

        if (choice.isEmpty()) {
            return null;
        }

        switch (choice) {

            case "1":
                return "ALL";

            case "2":
                return "Standard";

            case "3":
                return "Deluxe";

            case "4":
                return "Suite";

            default:
                return "";
        }
    }

    public String promptReportStatus() {

        System.out.println();
        System.out.println("Status Filter:");
        System.out.println("1. All");
        System.out.println("2. NEEDS_CLEANING");
        System.out.println("3. CLEANING_IN_PROGRESS");
        System.out.println("4. INSPECTED");
        System.out.println("5. AVAILABLE");
        System.out.println();
        System.out.print("Enter your choice (blank to cancel): ");

        String choice = scanner.nextLine().trim();

        if (choice.isEmpty()) {
            return null;
        }

        switch (choice) {

            case "1":
                return "ALL";

            case "2":
                return "NEEDS_CLEANING";

            case "3":
                return "CLEANING_IN_PROGRESS";

            case "4":
                return "INSPECTED";

            case "5":
                return "AVAILABLE";

            default:
                return "";
        }
    }

    public void displayHousekeepingStatusReport(
            Iterator<Room> rooms,
            String roomTypeFilter,
            String statusFilter) {

        System.out.println();
        System.out.println(
                "======================================================"
        );

        System.out.println(
                "             HOUSEKEEPING STATUS REPORT"
        );

        System.out.println(
                "======================================================"
        );

        System.out.println(
                "Room Type Filter : " + roomTypeFilter
        );

        System.out.println(
                "Status Filter    : " + statusFilter
        );

        System.out.println(
                "------------------------------------------------------"
        );

        if (!rooms.hasNext()) {

            System.out.println(
                    "No records match the selected criteria."
            );

            System.out.println(
                    "======================================================"
            );

            return;
        }

        System.out.println(
                String.format(
                        "%-10s %-15s %-25s",
                        "Room",
                        "Room Type",
                        "Status"
                )
        );

        System.out.println(
                "------------------------------------------------------"
        );

        while (rooms.hasNext()) {

            Room room = rooms.next();

            System.out.println(
                    String.format(
                            "%-10s %-15s %-25s",
                            room.getRoomNumber(),
                            room.getRoomType(),
                            room.getStatus()
                    )
            );
        }

        System.out.println(
                "======================================================"
        );
    }

    // =========================================================
    // Report 2
    // Room History Activity Report
    // =========================================================

    public int promptMinimumHistoryRecords() {

        System.out.println();

        System.out.print(
                "Enter minimum number of history records (blank to cancel): "
        );

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

    public void displayInvalidReportFilter() {

        System.out.println();

        System.out.println(
                "Invalid report filter."
        );
    }

    public void displayRoomHistoryReportHeader(
            String roomTypeFilter,
            int minimumRecords) {

        System.out.println();

        System.out.println(
                "======================================================"
        );

        System.out.println(
                "          ROOM HISTORY ACTIVITY REPORT"
        );

        System.out.println(
                "======================================================"
        );

        System.out.println(
                "Room Type Filter : " + roomTypeFilter
        );

        System.out.println(
                "Minimum Records  : " + minimumRecords
        );

        System.out.println(
                "------------------------------------------------------"
        );

        System.out.println(
                String.format(
                        "%-10s %-15s %-20s %-10s",
                        "Room",
                        "Room Type",
                        "Current Status",
                        "Records"
                )
        );

        System.out.println(
                "------------------------------------------------------"
        );
    }

    public void displayRoomHistoryReportRow(
            Room room,
            int numberOfRecords) {

        System.out.println(
                String.format(
                        "%-10s %-15s %-20s %-10d",
                        room.getRoomNumber(),
                        room.getRoomType(),
                        room.getStatus(),
                        numberOfRecords
                )
        );
    }

    public void displayNoReportRecords() {

        System.out.println(
                "No records match the selected criteria."
        );
    }

    public void displayReportEnd() {

        System.out.println(
                "======================================================"
        );
    }

    // =========================================================
    // Report 3
    // Rollback Frequency Report
    // =========================================================

    public String promptReportRoomNumber() {

        System.out.println();
        System.out.print(
                "Enter room number to filter by (or ALL): "
        );

        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            return "ALL";
        }

        return input;
    }

    public String promptReportFromDate() {

        System.out.println();
        System.out.print(
                "Enter from-date (yyyy-MM-dd, blank = no lower bound): "
        );

        String input = scanner.nextLine().trim();

        return input.isEmpty() ? "0000-00-00" : input;
    }

    public String promptReportToDate() {

        System.out.print(
                "Enter to-date (yyyy-MM-dd, blank = no upper bound): "
        );

        String input = scanner.nextLine().trim();

        return input.isEmpty() ? "9999-99-99" : input;
    }

    public void displayRollbackFrequencyReportHeader(
            String roomNumberFilter,
            String fromDate,
            String toDate) {

        System.out.println();

        System.out.println(
                "======================================================"
        );

        System.out.println(
                "          ROLLBACK FREQUENCY REPORT"
        );

        System.out.println(
                "======================================================"
        );

        System.out.println(
                "Room Filter : " + roomNumberFilter
        );

        System.out.println(
                "Date Range  : " + fromDate + " to " + toDate
        );

        System.out.println(
                "------------------------------------------------------"
        );

        System.out.println(
                String.format(
                        "%-15s %-15s",
                        "Room",
                        "Rollback Count"
                )
        );

        System.out.println(
                "------------------------------------------------------"
        );
    }

    public void displayRollbackFrequencyReportRow(
            String roomNumber,
            int rollbackCount) {

        System.out.println(
                String.format(
                        "%-15s %-15d",
                        roomNumber,
                        rollbackCount
                )
        );
    }
}
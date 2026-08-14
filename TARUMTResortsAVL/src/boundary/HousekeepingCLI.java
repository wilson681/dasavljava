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
        System.out.println("5. Generate Housekeeping Status Report");
        System.out.println("6. Generate Room History Activity Report");
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

    // =========================================================
    // Shared Input / Output
    // =========================================================

    public String promptRoomNumber() {

        System.out.println();
        System.out.print("Enter room number: ");

        return scanner.nextLine().trim();
    }

    public void displayRoomNotFound(
            String roomNumber) {

        System.out.println();

        System.out.println(
                "Room " + roomNumber + " not found."
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

    public String promptNewStatus() {

        System.out.println();
        System.out.println("Select New Status:");
        System.out.println("1. NEEDS_CLEANING");
        System.out.println("2. CLEANING_IN_PROGRESS");
        System.out.println("3. INSPECTED");
        System.out.println("4. AVAILABLE");
        System.out.println();
        System.out.print("Enter your choice: ");

        String choice = scanner.nextLine().trim();

        switch (choice) {

            case "1":
                return "NEEDS_CLEANING";

            case "2":
                return "CLEANING_IN_PROGRESS";

            case "3":
                return "INSPECTED";

            case "4":
                return "AVAILABLE";

            default:
                return null;
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

        System.out.println("Most Recent");
        System.out.println("     |");

        int recordNumber = 1;

        while (statusHistory.hasNext()) {

            String status = statusHistory.next();

            System.out.println(
                    String.format(
                            "%2d. %s",
                            recordNumber,
                            status
                    )
            );

            recordNumber++;
        }

        System.out.println("     |");
        System.out.println("Oldest");
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
        System.out.print("Enter your choice: ");

        String choice = scanner.nextLine().trim();

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
                return null;
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
        System.out.print("Enter your choice: ");

        String choice = scanner.nextLine().trim();

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
                return null;
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
                "Enter minimum number of history records: "
        );

        String input = scanner.nextLine().trim();

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
}
package boundary;

import entity.Room;
import java.util.Iterator;
import java.util.Scanner;
import utility.ValidationUtility;

/**
 * Handles user input and output for the Housekeeping and Task Log module.
 *
 * @author Hoo Theng Qin
 */
public class HousekeepingCLI {

        private static final int MAX_BAR_WIDTH = 20; // Maximum stars shown in a report bar.
        private final Scanner scanner;

        public HousekeepingCLI() {
                scanner = new Scanner(System.in);
        }

        // Main Menu
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
         * Pauses the screen until the user presses Enter.
         */
        public void promptContinue() {

                System.out.println();
                System.out.print("Press Enter to continue...");
                scanner.nextLine();
        }

        // Option 1: View Rooms Requiring Housekeeping

        public void displayHousekeepingRooms(
                        Iterator<Room> rooms) {

                System.out.println();
                System.out.println(
                                "===== Rooms Requiring Housekeeping =====");
                System.out.println();

                if (!rooms.hasNext()) {

                        System.out.println(
                                        "No rooms currently require housekeeping.");

                        return;
                }

                System.out.println(
                                String.format(
                                                "%-10s %-15s %-25s",
                                                "Room",
                                                "Room Type",
                                                "Status"));

                System.out.println(
                                "------------------------------------------------------");

                while (rooms.hasNext()) {

                        Room room = rooms.next();

                        System.out.println(
                                        String.format(
                                                        "%-10s %-15s %-25s",
                                                        room.getRoomNumber(),
                                                        room.getRoomType(),
                                                        room.getStatus()));
                }
        }

        /**
         * Displays all rooms before the user selects a room number.
         */
        public void displayAllRooms(
                        Iterator<Room> rooms) {

                System.out.println();
                System.out.println(
                                "===== All Rooms =====");
                System.out.println();

                System.out.println(
                                String.format(
                                                "%-10s %-15s %-25s",
                                                "Room",
                                                "Room Type",
                                                "Status"));

                System.out.println(
                                "------------------------------------------------------");

                while (rooms.hasNext()) {

                        Room room = rooms.next();

                        System.out.println(
                                        String.format(
                                                        "%-10s %-15s %-25s",
                                                        room.getRoomNumber(),
                                                        room.getRoomType(),
                                                        room.getStatus()));
                }
        }

        // Shared Input / Output

        public String promptRoomNumber() {

                System.out.println();
                System.out.print("Enter room number (blank to cancel): ");

                return scanner.nextLine().trim();
        }

        public void displayInvalidRoomNumber(
                        String roomNumber) {

                System.out.println();

                System.out.println(
                                "\"" + roomNumber + "\" is not a valid room number. Must contain digits only.");
        }

        public void displayRoomNotFound(
                        String roomNumber) {

                System.out.println();

                System.out.println(
                                "Room " + roomNumber + " not found.");
        }

        public void displayRoomNotInPipeline(
                        String roomNumber,
                        String currentStatus) {

                System.out.println();

                System.out.println(
                                "Room " + roomNumber + " (status: " + currentStatus
                                                + ") is not in the housekeeping pipeline. Only rooms with"
                                                + " NEEDS_CLEANING, CLEANING_IN_PROGRESS, or INSPECTED status"
                                                + " can be updated here.");
        }

        // Option 2: Update Room Status

        public void displayCurrentRoomStatus(
                        String roomNumber,
                        String currentStatus) {

                System.out.println();

                System.out.println(
                                "Room " + roomNumber
                                                + " current status: "
                                                + currentStatus);
        }

        /**
         * Prompts for the next valid housekeeping status.
         * Only the next status in the housekeeping pipeline is offered.
         *
         * @param suggestedNextStatus the next valid status
         * @return the selected status, or null if cancelled
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
                                "Invalid room status selection or status transition.");
        }

        public void displayStatusUpdated(
                        String roomNumber,
                        String previousStatus,
                        String newStatus) {

                System.out.println();
                System.out.println(
                                "===== STATUS UPDATED =====");

                System.out.println(
                                "Room            : " + roomNumber);

                System.out.println(
                                "Previous Status : " + previousStatus);

                System.out.println(
                                "New Status      : " + newStatus);
        }

        // Option 3: Roll Back Latest Status

        public void displayRollbackNotAvailable(
                        String roomNumber) {

                System.out.println();

                System.out.println(
                                "Room " + roomNumber
                                                + " has no previous status to restore.");
        }

        public void displayRollbackResult(
                        String roomNumber,
                        String removedStatus,
                        String restoredStatus) {

                System.out.println();
                System.out.println(
                                "===== STATUS ROLLBACK SUCCESSFUL =====");

                System.out.println(
                                "Room            : " + roomNumber);

                System.out.println(
                                "Removed Status  : " + removedStatus);

                System.out.println(
                                "Restored Status : " + restoredStatus);
        }

        // Option 4: View Room Status History

        public void displayRoomStatusHistory(
                        String roomNumber,
                        String currentStatus,
                        Iterator<String> statusHistory) {

                System.out.println();
                System.out.println(
                                "===== ROOM STATUS HISTORY =====");

                System.out.println(
                                "Room           : " + roomNumber);

                System.out.println(
                                "Current Status : " + currentStatus);

                System.out.println();

                if (!statusHistory.hasNext()) {

                        System.out.println(
                                        "No status history is available.");

                        return;
                }

                // Stack iteration runs from newest to oldest.
                // Prepending each status reverses the order for chronological display.
                String timeline = "";

                while (statusHistory.hasNext()) {

                        String status = statusHistory.next();

                        timeline = status + (timeline.isEmpty() ? "" : " -> " + timeline);
                }

                System.out.println("Oldest                                                        Most Recent");
                System.out.println(timeline);
        }

        // Report 1: Housekeeping Status Report

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
                System.out.println("2. AVAILABLE");
                System.out.println("3. OCCUPIED");
                System.out.println("4. NEEDS_CLEANING");
                System.out.println("5. CLEANING_IN_PROGRESS");
                System.out.println("6. INSPECTED");
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
                                return "AVAILABLE";

                        case "3":
                                return "OCCUPIED";

                        case "4":
                                return "NEEDS_CLEANING";

                        case "5":
                                return "CLEANING_IN_PROGRESS";

                        case "6":
                                return "INSPECTED";

                        default:
                                return "";
                }
        }

        public void displayHousekeepingStatusReportHeader(
                        String roomTypeFilter,
                        String statusFilter) {

                System.out.println();
                System.out.println(
                                "======================================================");

                System.out.println(
                                "             HOUSEKEEPING STATUS REPORT");

                System.out.println(
                                "======================================================");

                System.out.println(
                                "Generated at     : " + generatedAt());

                System.out.println(
                                "Room Type Filter : " + roomTypeFilter);

                System.out.println(
                                "Status Filter    : " + statusFilter);

                System.out.println(
                                "------------------------------------------------------");

                System.out.println(
                                String.format(
                                                "%-10s %-15s %-25s",
                                                "Room",
                                                "Room Type",
                                                "Status"));

                System.out.println(
                                "------------------------------------------------------");
        }

        public void displayHousekeepingStatusReportRow(
                        String roomNumber,
                        String roomType,
                        String status) {

                System.out.println(
                                String.format(
                                                "%-10s %-15s %-25s",
                                                roomNumber,
                                                roomType,
                                                status));
        }

        /**
         * Displays room status totals and housekeeping pipeline breakdown.
         * Detailed status analysis is shown only when all statuses are included.
         *
         * @param showStatusAnalysis true when the status breakdown should be displayed
         */
        public void displayHousekeepingStatusSummary(
                        int total,
                        int availableCount,
                        int occupiedCount,
                        int needsCleaningCount,
                        int cleaningInProgressCount,
                        int inspectedCount,
                        boolean showStatusAnalysis) {

                System.out.println(
                                "------------------------------------------------------");
                System.out.println("SUMMARY");
                System.out.printf("  Rooms in this report     : %d%n", total);

                if (!showStatusAnalysis) {
                        return;
                }

                int inPipeline = needsCleaningCount
                                + cleaningInProgressCount
                                + inspectedCount;

                System.out.printf("  Sellable now (AVAILABLE) : %d   (%s)%n",
                                availableCount, percentOf(availableCount, total));
                System.out.printf("  Occupied                 : %d   (%s)%n",
                                occupiedCount, percentOf(occupiedCount, total));
                System.out.printf("  In housekeeping pipeline : %d   (%s)%n",
                                inPipeline, percentOf(inPipeline, total));

                System.out.println();
                System.out.println("STATUS BREAKDOWN   (each * = 1 room)");
                printStatusLine("AVAILABLE", availableCount);
                printStatusLine("OCCUPIED", occupiedCount);
                printStatusLine("NEEDS_CLEANING", needsCleaningCount);
                printStatusLine("CLEANING_IN_PROGRESS", cleaningInProgressCount);
                printStatusLine("INSPECTED", inspectedCount);
        }

        private void printStatusLine(String status, int count) {
                System.out.printf("  %-22s %-22s %d%n", status, bar(count), count);
        }

        public void displayRoomTypeBreakdownHeader() {
                System.out.println();
                System.out.println("ROOMS BY ROOM TYPE   (each * = 1 room)");
        }

        /**
         * Displays the number of rooms for each room type.
         * Sellable-room information is shown only when all statuses are included.
         *
         * @param showSellable true when available-room information should be displayed
         */
        public void displayRoomTypeBreakdown(
                        String roomType,
                        int total,
                        int availableCount,
                        boolean showSellable) {

                if (!showSellable) {
                        System.out.printf("  %-10s %-22s %d%n", roomType, bar(total), total);
                        return;
                }

                String warning = (total > 0 && availableCount == 0)
                                ? "   <-- NONE SELLABLE"
                                : "";

                System.out.printf("  %-10s %-22s %d   (%d sellable)%s%n",
                                roomType, bar(total), total, availableCount, warning);
        }

        // Report 2: Room History Activity Report

        /**
         * Prompts for the minimum rollback count used by the report.
         *
         * @return the entered value, Integer.MIN_VALUE if cancelled,
         *         or -1 if the input is not numeric
         */
        public int promptMinimumRollbacks() {

                System.out.println();

                System.out.print(
                                "Show rooms with at least how many rollbacks? (0 = all, blank to cancel): ");

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
                                "Invalid report filter.");
        }

        public void displayRoomHistoryReportHeader(
                        String roomTypeFilter,
                        int minimumRollbacks) {

                System.out.println();
                System.out.println(
                                "======================================================");
                System.out.println(
                                "          ROOM HISTORY ACTIVITY REPORT");
                System.out.println(
                                "======================================================");
                System.out.println(
                                "Generated at      : " + generatedAt());
                System.out.println(
                                "Room Type Filter  : " + roomTypeFilter);
                System.out.println(
                                "Minimum Rollbacks : " + minimumRollbacks);
                System.out.println(
                                "------------------------------------------------------");
                System.out.println(
                                String.format(
                                                "%-6s %-9s %-22s %8s %10s %11s",
                                                "Room", "Type", "Current Status",
                                                "Updates", "Rollbacks", "Error Rate"));
                System.out.println(
                                "------------------------------------------------------");
        }

        /**
         * Displays room activity and rollback statistics.
         *
         * @param updates   total recorded status updates
         * @param rollbacks total status rollbacks
         */
        public void displayRoomHistoryReportRow(
                        String roomNumber,
                        String roomType,
                        String currentStatus,
                        int updates,
                        int rollbacks) {

                // Avoid division by zero when no updates are recorded.
                String errorRate = (updates == 0)
                                ? "-"
                                : String.format("%.1f%%", rollbacks * 100.0 / updates);

                System.out.println(
                                String.format(
                                                "%-6s %-9s %-22s %8d %10d %11s",
                                                roomNumber, roomType, currentStatus,
                                                updates, rollbacks, errorRate));
        }

        /**
         * Displays the overall room activity and rollback summary.
         */
        public void displayRoomHistorySummary(
                        int roomsWithActivity,
                        int totalUpdates,
                        int totalRollbacks,
                        String mostRolledBackRoom,
                        int mostRolledBackCount) {

                String overallErrorRate = (totalUpdates == 0)
                                ? "-"
                                : String.format("%.1f%%", totalRollbacks * 100.0 / totalUpdates);

                System.out.println(
                                "------------------------------------------------------");
                System.out.printf("  Rooms with activity  : %d%n", roomsWithActivity);
                System.out.printf("  Total status updates : %d%n", totalUpdates);
                System.out.printf("  Total rollbacks      : %d%n", totalRollbacks);
                System.out.printf("  Overall error rate   : %s%n", overallErrorRate);

                // Show the most rolled-back room only when a rollback exists.
                if (mostRolledBackCount > 0) {
                        System.out.printf("  Most rolled back     : Room %s (%d)   <-- NEEDS ATTENTION%n",
                                        mostRolledBackRoom, mostRolledBackCount);
                }
        }

        public void displayNoReportRecords() {

                System.out.println(
                                "No records match the selected criteria.");
        }

        public void displayReportEnd() {

                System.out.println(
                                "======================================================");
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
         * Formats a count as a percentage of the total.
         * Returns "-" when the total is zero.
         */
        private String percentOf(int count, int total) {
                if (total <= 0) {
                        return "-";
                }
                return String.format("%.1f%%", count * 100.0 / total);
        }

        /**
         * Builds a room-count bar where each '*' represents one room.
         * Values above MAX_BAR_WIDTH are truncated and marked with '+'.
         */
        private String bar(int value) {
                if (value <= 0) {
                        return "";
                }

                int stars = (value < MAX_BAR_WIDTH) ? value : MAX_BAR_WIDTH;

                String result = "";
                for (int i = 0; i < stars; i++) {
                        result = result + "*";
                }
                if (value > MAX_BAR_WIDTH) {
                        result = result + "+";
                }
                return result;
        }
}
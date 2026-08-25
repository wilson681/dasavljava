package boundary;

import entity.Room;
import java.util.Iterator;
import java.util.Scanner;
import utility.ValidationUtility;

/**
 * HousekeepingCLI.java
 * Boundary class for the Housekeeping and Task Log module.
 *
 * Responsible only for user input and console output.
 *
 * @author YOUR FULL NAME
 */
public class HousekeepingCLI {

    private static final int MAX_BAR_WIDTH = 20;   // 星号柱状图最多印几颗,超过就截断加"+"

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
        // 照房间的生命周期排:能卖 -> 有人住 -> 退房待清洁 -> 清洁中 -> 待检查 -> 又能卖
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
                "======================================================"
        );

        System.out.println(
                "             HOUSEKEEPING STATUS REPORT"
        );

        System.out.println(
                "======================================================"
        );

        System.out.println(
                "Generated at     : " + generatedAt()
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
                        status
                )
        );
    }

    /**
     * 明细表下方的汇总:主管真正要的是"现在还有几间能卖、几间卡在清洁流程",
     * 而不是自己一行一行去数。
     *
     * @param showStatusAnalysis 只有在"状态筛选=All"时才是true。已经筛到单一状态时,
     *                           占比一定是100%、其他状态一定是0,这些行印出来全是噪音,
     *                           所以那种情况只印总笔数就好
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
                "------------------------------------------------------"
        );
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
     * 按房型看这份报表里各有几间,状态筛选是All时额外标出"还有几间能卖"——
     * 一整型房都卖不出去是营运警讯,要主动标出来,不能让主管自己比对。
     *
     * @param showSellable 只有在"状态筛选=All"时才是true;已经筛到单一状态时
     *                     "能卖几间"不是0就是全部,标出来没有意义
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

    // =========================================================
    // Report 2
    // Room History Activity Report
    // =========================================================

    /**
     * @return 门槛值;留空回传 Integer.MIN_VALUE 代表取消,
     *         打了但不是数字回传 -1 交给 Control 判定无效、原地重问
     */
    public int promptMinimumRollbacks() {

        System.out.println();

        System.out.print(
                "Show rooms with at least how many rollbacks? (0 = all, blank to cancel): "
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
            int minimumRollbacks) {

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
                "Generated at      : " + generatedAt()
        );
        System.out.println(
                "Room Type Filter  : " + roomTypeFilter
        );
        System.out.println(
                "Minimum Rollbacks : " + minimumRollbacks
        );
        System.out.println(
                "------------------------------------------------------"
        );
        System.out.println(
                String.format(
                        "%-6s %-9s %-22s %8s %10s %11s",
                        "Room", "Type", "Current Status",
                        "Updates", "Rollbacks", "Error Rate"
                )
        );
        System.out.println(
                "------------------------------------------------------"
        );
    }

    /**
     * @param updates   这间房总共被记录过几次状态更新
     * @param rollbacks 其中有几次被回滚(打错了)
     */
    public void displayRoomHistoryReportRow(
            String roomNumber,
            String roomType,
            String currentStatus,
            int updates,
            int rollbacks) {

        // 一次更新都没有的房间不会进到这里,但保险起见还是挡一下除以零
        String errorRate = (updates == 0)
                ? "-"
                : String.format("%.1f%%", rollbacks * 100.0 / updates);

        System.out.println(
                String.format(
                        "%-6s %-9s %-22s %8d %10d %11s",
                        roomNumber, roomType, currentStatus,
                        updates, rollbacks, errorRate
                )
        );
    }

    /**
     * 明细表下方的汇总:主管要的是"整体出错率多高、哪间房最该处理",
     * 而不是自己一行一行去加。
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
                "------------------------------------------------------"
        );
        System.out.printf("  Rooms with activity  : %d%n", roomsWithActivity);
        System.out.printf("  Total status updates : %d%n", totalUpdates);
        System.out.printf("  Total rollbacks      : %d%n", totalRollbacks);
        System.out.printf("  Overall error rate   : %s%n", overallErrorRate);

        // 完全没有人出过错时不要硬印"最常出错的房间",那是误导
        if (mostRolledBackCount > 0) {
            System.out.printf("  Most rolled back     : Room %s (%d)   <-- NEEDS ATTENTION%n",
                    mostRolledBackRoom, mostRolledBackCount);
        }
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

    // =========================================================
    // 报表共用的显示工具
    // =========================================================

    /**
     * 报表产生的时间戳。报表是给管理层看的文件,一定要标明这份数字是什么时候的快照——
     * 房态每分钟都在变,没有时间戳的房况表隔天就不知道还能不能信。
     */
    private String generatedAt() {
        return java.time.LocalDateTime.now().withNano(0)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 算占比并排版成 "40.0%";总数是0时回传"-",不要印出 NaN 或 0.0%。
     */
    private String percentOf(int count, int total) {
        if (total <= 0) {
            return "-";
        }
        return String.format("%.1f%%", count * 100.0 / total);
    }

    /**
     * 星号柱状图:一颗星 = 一间房,直接数得出来,不做比例缩放
     * (缩放的话2间也印满20颗星,读的人反而看不懂一颗代表多少)。
     * 超过 MAX_BAR_WIDTH 就截断并加"+",真正的数字本来就印在旁边那一栏。
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
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
    // 报表明细表格比DIVIDER宽,自己一条,免得表格右边凸出去
    private static final String REPORT_DIVIDER =
            "-------------------------------------------------------------------";
    private static final int MAX_BAR_WIDTH = 20;   // 星号柱状图满格几颗星

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

    public void displayCancelled() {
        System.out.println("Cancelled. Returning to menu.");
    }

    // ========== 功能1:登记新客人 ==========

    public String promptName() {
        System.out.print("Enter guest name (blank to cancel): ");
        return scanner.nextLine().trim();
    }

    public String promptPhone() {
        System.out.print("Enter phone number (blank to cancel): ");
        return scanner.nextLine().trim();
    }

    public void displayInvalidPhone(String phone) {
        System.out.println("Phone number \"" + phone + "\" is invalid. Must contain digits only.");
    }
    public void displayInvalidName(String name) {
        System.out.println("\"" + name + "\" is not a valid name.");
        System.out.println("Letters and spaces only. Apostrophes, hyphens and \"A/L\" are allowed.");
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
     * 日期打错格式(例如少补0的 2026-8-8)如果直接放行,报表会筛不到任何东西、
     * 显示"No records match",使用者会以为是没资料,其实只是格式打错——所以格式
     * 不合法就当场重问,不让它带着错的筛选条件往下跑。
     *
     * @return "yyyy-MM-dd"格式的日期,留空回传"ALL"代表不限日期
     */
    public String promptReportDate() {
        while (true) {
            System.out.println();
            System.out.print("Filter by date (yyyy-MM-dd, blank = all dates): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return "ALL";
            }
            try {
                java.time.LocalDate.parse(input);
                return input;
            } catch (java.time.format.DateTimeParseException e) {
                System.out.println("Invalid date format, please use yyyy-MM-dd (e.g. 2026-08-13).");
            }
        }
    }

    public String promptReportRoomType() {
        while (true) {
            System.out.println();
            System.out.println("Room Type Filter: 1) All  2) Standard  3) Deluxe  4) Suite");
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
                    System.out.println("Invalid input, please enter 1 - 4.");
            }
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
        System.out.println("Generated at     : " + generatedAt());
        System.out.println("Date Filter      : " + dateFilter);
        System.out.println("Room Type Filter : " + roomTypeFilter);
        System.out.println(DIVIDER);
        System.out.println(String.format("%-20s %-20s %-11s %-10s %s",
                "Guest", "Registered At", "Room Type", "Allocated", "Wait (min)"));
        System.out.println(REPORT_DIVIDER);
    }

    /**
     * @param waitMinutes 已分房的实际等待分钟数;传负数代表"没有这个数字可印"
     *                    (还在排队,或时间戳算不出来),该栏改印"-"
     */
    public void displayDailyRegistrationReportRow(String guestName, String registeredAt,
                                                   String roomType, boolean allocated, int waitMinutes) {
        String waitText = (waitMinutes < 0) ? "-" : String.valueOf(waitMinutes);
        System.out.println(String.format("%-20s %-20s %-11s %-10s %s",
                guestName, registeredAt, roomType, (allocated ? "Yes" : "No"), waitText));
    }

    /**
     * 明细表下方的汇总区:把"这份报表要给经理看的结论"集中印出来,
     * 而不是只丢一张清单让人自己数。
     */
    public void displayDailyRegistrationSummary(int total, int allocatedCount, int waitingCount,
                                                 double averageWait, int longestWait, String longestWaitGuest,
                                                 int longestWaitingMinutes, String longestWaitingGuest,
                                                 String longestWaitingType) {
        double allocatedRate = (total == 0) ? 0.0 : (allocatedCount * 100.0 / total);

        System.out.println(REPORT_DIVIDER);
        System.out.println("SUMMARY");
        System.out.printf("  Total registrations       : %d%n", total);
        System.out.printf("  Allocated / Still waiting : %d / %d  (%.1f%% allocated)%n",
                allocatedCount, waitingCount, allocatedRate);
        System.out.printf("  Average wait              : %.1f min   (allocated only)%n", averageWait);
        System.out.printf("  Longest wait              : %d min     (%s)%n", longestWait, longestWaitGuest);

        // 还在等的人没有"等多久才分到房"这个数字,但"已经等了多久"才是真正要处理的营运问题,
        // 所以单独列一行,而且用天/小时印,一眼看得出严重程度
        if (waitingCount > 0) {
            System.out.printf("  Longest still waiting     : %-10s (%s, %s)   <-- ACTION NEEDED%n",
                    formatDuration(longestWaitingMinutes), longestWaitingGuest, longestWaitingType);
        }
    }

    /**
     * 按房型分布 + 星号柱状图,让"哪种房型最抢手"不用自己数。
     */
    public void displayRoomTypeBreakdown(int standardCount, int deluxeCount, int suiteCount) {
        System.out.println();
        System.out.println("REGISTRATIONS BY ROOM TYPE   (each * = 1 registration)");
        System.out.printf("  %-10s %-22s %d%n", "Standard", bar(standardCount), standardCount);
        System.out.printf("  %-10s %-22s %d%n", "Deluxe", bar(deluxeCount), deluxeCount);
        System.out.printf("  %-10s %-22s %d%n", "Suite", bar(suiteCount), suiteCount);
    }

    // ========== 报表2:等待时长分析报表 ==========

    /**
     * 跟另外两个报表输入一致:打错就当场重问,不要静默塞一个预设值进去,
     * 否则使用者会拿到一份"筛选条件根本不是他打的"的报表还不知道。
     *
     * @return 门槛分钟数,留空当作0(不设门槛)
     */
    public int promptMinWaitMinutes() {
        while (true) {
            System.out.println();
            System.out.print("Enter minimum wait time to include (minutes, blank = 0): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return 0;
            }
            try {
                int minutes = Integer.parseInt(input);
                if (minutes >= 0) {
                    return minutes;
                }
                System.out.println("Minimum wait cannot be negative, please try again.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input, please enter a whole number.");
            }
        }
    }

    public void displayWaitTimeAnalysisHeader(int minWaitMinutes, String dateFilter) {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("             WAIT TIME ANALYSIS REPORT");
        System.out.println(DIVIDER);
        System.out.println("Generated at  : " + generatedAt());
        System.out.println("Minimum Wait  : " + minWaitMinutes + " min");
        System.out.println("Date Filter   : " + dateFilter);
        System.out.println(DIVIDER);
        System.out.println(String.format("%-12s %-20s %-11s %-12s %s",
                "Booking ID", "Guest", "Room Type", "Wait (min)", "Status"));
        System.out.println(REPORT_DIVIDER);
    }

    /**
     * @param allocated false代表这笔还在排队,那个等待分钟数是"算到此刻为止"、
     *                  之后还会继续变大,所以要在Status栏标清楚,不能跟已分房的混着看
     */
    public void displayWaitTimeAnalysisRow(String bookingId, String guestName, String roomType,
                                            int waitMinutes, boolean allocated) {
        String waitText = (waitMinutes < 0) ? "-" : String.valueOf(waitMinutes);
        System.out.println(String.format("%-12s %-20s %-11s %-12s %s",
                bookingId, guestName, roomType, waitText,
                (allocated ? "Allocated" : "STILL WAITING")));
    }

    public void displayWaitTimeAnalysisSummary(int total, int allocatedCount, int waitingCount,
                                                double averageWait, int longestWait, String longestWaitGuest,
                                                int longestWaitingMinutes, String longestWaitingGuest) {
        System.out.println(REPORT_DIVIDER);
        System.out.println("SUMMARY");
        System.out.printf("  Records matching filter : %d  (%d allocated, %d still waiting)%n",
                total, allocatedCount, waitingCount);
        System.out.printf("  Average wait            : %.1f min   (allocated only)%n", averageWait);
        System.out.printf("  Longest wait            : %d min     (%s)%n", longestWait, longestWaitGuest);
        if (waitingCount > 0) {
            System.out.printf("  Still waiting longest   : %-10s (%s)%n",
                    formatDuration(longestWaitingMinutes), longestWaitingGuest);
        }
    }

    public void displayHourlyBreakdownHeader() {
        System.out.println();
        System.out.println("AVERAGE WAIT BY HOUR OF REGISTRATION  (allocated only; each * = 1 registration)");
        System.out.println(String.format("%-6s %-7s %-12s", "Hour", "Count", "Avg Wait"));
        System.out.println(REPORT_DIVIDER);
    }

    public void displayHourlyBreakdownRow(int hour, int count, double averageWaitMinutes) {
        System.out.println(String.format("%02d:00  %-7d %-12s %s",
                hour, count, String.format("%.1f min", averageWaitMinutes), bar(count)));
    }

    /**
     * @param busiestHours 笔数并列最多的时段(可能不只一个),空的代表根本没有资料可以聚合
     * @param hourCount    并列的时段有几个;超过一个才要写"each"
     * @param count        那些时段各自的笔数
     */
    public void displayBusiestHour(Iterator<Integer> busiestHours, int hourCount, int count) {
        if (hourCount == 0) {
            System.out.println("  No allocated registration to analyse by hour.");
            return;
        }

        String hours = "";
        while (busiestHours.hasNext()) {
            hours = hours.isEmpty()
                    ? String.format("%02d:00", busiestHours.next())
                    : hours + String.format(" / %02d:00", busiestHours.next());
        }

        System.out.printf("  Busiest hour : %s  (%d registration(s)%s)%n",
                hours, count, (hourCount > 1 ? " each" : ""));
    }

    // ========== 报表共用的显示工具 ==========

    /**
     * 报表产生的时间戳。报表是给管理层看的文件,一定要标明"这份数字是什么时候的快照",
     * 尤其是那些"还在等的等待时长"——那种数字换个时间跑就不一样。
     */
    private String generatedAt() {
        return java.time.LocalDateTime.now().withNano(0)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 把分钟数印成人看得懂的长度。等了7天多的时候,"11085 min"没有感觉,
     * "7d 16h"一眼就知道事情不对。
     */
    private String formatDuration(int minutes) {
        if (minutes < 0) {
            return "-";
        }
        int days = minutes / 1440;
        int hours = (minutes % 1440) / 60;
        int mins = minutes % 60;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + mins + "m";
        }
        return mins + " min";
    }

    /**
     * 星号柱状图:一颗星 = 一笔,直接数得出来。
     *
     * 以前是按比例缩放(最多的那格固定给满20颗星),看起来好看,但2笔也印20颗星,
     * 读的人根本不知道一颗星代表多少,还要回头看旁边的数字——那柱状图就白画了。
     * 改成一笔一颗之后,星号本身就是数量,标题再加一行图例说明就完全不用猜。
     *
     * 笔数超过 MAX_BAR_WIDTH 时截断并在结尾加"+",避免一行印几百颗星把表格撑爆;
     * 真正的数字本来就印在旁边那一栏,不会因为截断而看不到。
     */
    private String bar(int value) {
        if (value <= 0) {
            return "";
        }
        int stars = Math.min(value, MAX_BAR_WIDTH);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < stars; i++) {
            result.append('*');
        }
        if (value > MAX_BAR_WIDTH) {
            result.append('+');
        }
        return result.toString();
    }
}

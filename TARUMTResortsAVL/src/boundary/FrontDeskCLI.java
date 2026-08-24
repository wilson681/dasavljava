package boundary;

import entity.Booking;
import entity.BillingRecord;
import java.util.Iterator;
import java.util.Scanner;
import utility.ValidationUtility;

/**
 * FrontDeskCLI.java
 * Boundary for the Front-Desk Service module.
 *
 * Responsible only for user input and output.
 */
public class FrontDeskCLI {

    private static final int MAX_BAR_WIDTH = 20;   // 星号柱状图最多印几颗

    private Scanner scanner;

    public FrontDeskCLI() {
        scanner = new Scanner(System.in);
    }

    public int displayMenuAndGetChoice() {

        System.out.println();
        System.out.println("===== Front-Desk Service =====");
        System.out.println("1) Search Guest");
        System.out.println("2) Check Room Availability");
        System.out.println("3) View Billing Details");
        System.out.println("4) Process Check-Out");
        System.out.println("0) Back to Main Menu");
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
    
    public String promptConfirmationNumber() {
        System.out.print("Enter 8-digit confirmation number (blank to cancel): ");
        return scanner.nextLine().trim();
    }

    public void displayInvalidConfirmationNumber(String confirmationNumber) {
        System.out.println("\"" + confirmationNumber + "\" is not a valid confirmation number. It must be exactly 8 digits.");
    }

  public void displayGuestDetails(String confirmationNumber,
                                    String name,
                                    String phone,
                                    String guestType,
                                    String memberId,
                                    String tier,
                                    String vipStatus,
                                    String recordCreatedAt,
                                    String bookingLines,
                                    int bookingCount) {

        System.out.println();
        System.out.println("======================================================");
        System.out.println("                   GUEST DETAILS");
        System.out.println("======================================================");
        System.out.println("Confirmation Number : " + confirmationNumber);
        System.out.println("Name                : " + name);
        System.out.println("Phone               : " + phone);
        System.out.println("------------------------------------------------------");
        System.out.println("Guest Type          : " + guestType);
        System.out.println("Member ID           : " + memberId);
        System.out.println("Tier                : " + tier);
        System.out.println("VIP Status          : " + vipStatus);
        System.out.println("Record Created At   : " + recordCreatedAt);
        System.out.println("------------------------------------------------------");
        System.out.println("Bookings            : " + bookingCount);
        System.out.print(bookingLines);
        System.out.println("======================================================");
    }
    public void displayGuestNotFound() {
        System.out.println("Guest not found.");
    }

    public void displayBillingRecordNotFound(String confirmationNumber) {
        System.out.println("No billing record found for confirmation number " + confirmationNumber + ".");
    }
    /**
     * Tells the user a menu option is not available yet.
     *
     * @param featureName the name of the feature
     */
    public void displayNotImplemented(String featureName) {
        System.out.println();
        System.out.println(featureName + " - coming soon.");
    }
    /**
     * Prints the header block of the billing details screen.
     */
    public void displayBillingHeader(String confirmationNumber, String guestName,
                                     String guestType, String tier) {
        System.out.println();
        System.out.println("======================================================");
        System.out.println("                 BILLING DETAILS");
        System.out.println("======================================================");
        System.out.println("Confirmation Number : " + confirmationNumber);
        System.out.println("Guest               : " + guestName);
        System.out.println("Guest Type          : " + guestType);
        System.out.println("Tier                : " + tier);
    }

    /**
     * Prints the charges for rooms the guest is still occupying.
     *
     * Only called when the guest has at least one room still checked in —
     * FrontDeskControl skips this whole block entirely otherwise, instead of
     * printing an empty section under a header that doesn't apply.
     */
    public void displayCurrentCharges(String chargeLines, int roomCount,
                                      int totalNights, double totalCharges) {
        System.out.println("------------------------------------------------------");
        System.out.println("CURRENT CHARGES  (rooms still checked in)");
        System.out.println("------------------------------------------------------");
        System.out.printf("  %-8s %-12s %14s %10s %16s%n",
                "Room", "Type", "Rate/night", "Nights", "Subtotal");
        System.out.print(chargeLines);
        System.out.println("------------------------------------------------------");
        System.out.printf("  %d room(s), %d night(s) in total%n", roomCount, totalNights);
        System.out.printf("  Room charges so far              RM %12.2f%n", totalCharges);
        System.out.println("  Note: tier discount and extra charges are applied");
        System.out.println("        at check-out, so the final amount may differ.");
    }

    /**
     * Prints the bills already settled under this confirmation number.
     */
    public void displaySettledBills(String settledLines, int billCount,
                                    double settledTotal, int totalPoints) {
        System.out.println("------------------------------------------------------");
        System.out.println("SETTLED BILLS  (" + billCount + ")");
        System.out.println("------------------------------------------------------");

        if (billCount == 0) {
            System.out.println("  None - nothing has been checked out yet.");
            return;
        }

        System.out.print(settledLines);
        System.out.println("------------------------------------------------------");
        System.out.printf("  Total settled                    RM %12.2f%n", settledTotal);
        System.out.printf("  Total points earned              %15d%n", totalPoints);
    }

    /**
     * Prints the closing line of the billing details screen.
     */
    public void displayBillingFooter() {
        System.out.println("======================================================");
    }
    
    // ========== Room Availability ==========

    /**
     * Asks which room type to report on.
     *
     * <p>Option 4 returns null, which countRooms() reads as the "any type"
     * wildcard. Because null already carries that meaning, an invalid entry
     * must never be allowed to reach it.</p>
     *
     * @return the room type name, or null when the user wants every type
     */
    public String promptRoomTypeFilter() {
        while (true) {
            System.out.println();
            System.out.println("Filter by room type:");
            System.out.println("  1) Standard");
            System.out.println("  2) Deluxe");
            System.out.println("  3) Suite");
            System.out.println("  4) All types");
            System.out.print("Enter your choice: ");

            String input = scanner.nextLine().trim();
            if (input.equals("1")) {
                return "Standard";
            }
            if (input.equals("2")) {
                return "Deluxe";
            }
            if (input.equals("3")) {
                return "Suite";
            }
            if (input.equals("4")) {
                return null;
            }
            System.out.println("Invalid input, please enter 1 - 4.");
        }
    }
   /**
     * Prints the room availability query: which rooms are available right
     * now, and how many are unavailable.
     *
     * <p>This is an operational lookup, not an analysis. Occupancy rates,
     * revenue per room and idle capacity belong to the room utilisation report
     * instead, so the two use cases stay distinct.</p>
     */
    public void displayRoomAvailability(String typeFilter,
                                        String breakdownLines,
                                        String availableLines,
                                        int available,
                                        int occupied,
                                        int inHousekeeping) {

        System.out.println();
        System.out.println("======================================================");
        System.out.println("                ROOM AVAILABILITY");
        System.out.println("======================================================");
        System.out.println("Filter : " + ((typeFilter == null) ? "All room types" : typeFilter));
        System.out.println("------------------------------------------------------");
        System.out.printf("  %-12s %14s %14s%n", "Type", "Available", "Unavailable");
        System.out.print(breakdownLines);
        System.out.println("------------------------------------------------------");
        System.out.println("AVAILABLE ROOMS  (" + available + ")");
        System.out.println("------------------------------------------------------");

        if (available == 0) {
            System.out.println("  None - no room is available right now.");
        } else {
            System.out.printf("  %-8s %-12s %14s%n", "Room", "Type", "Rate/night");
            System.out.print(availableLines);
        }

        System.out.println("------------------------------------------------------");
        System.out.println("  " + available + " room(s) available right now.");
        System.out.println("  " + (occupied + inHousekeeping) + " room(s) unavailable: "
                + occupied + " occupied, " + inHousekeeping + " in housekeeping.");
        System.out.println("======================================================");
    }
    // ========== Check-Out ==========

    public void displayNoRoomsToCheckOut(String confirmationNumber) {
        System.out.println("No checked-in rooms found under confirmation number " + confirmationNumber + ".");
    }

    public void displayCheckedInBookings(Iterator<Booking> bookings) {
        System.out.println();
        System.out.println("===== Rooms currently checked in =====");
        while (bookings.hasNext()) {
            Booking booking = bookings.next();
            System.out.println("  Room " + booking.getAssignedRoomNo() + " | " + booking.getRequestedRoomType()
                    + " | " + booking.getNumberOfNights() + " night(s) | Check-in: "
                    + booking.getCheckInDate());
        }
    }

    public String promptRoomToCheckOut() {
        System.out.print("Enter room number to check out: ");
        return scanner.nextLine().trim();
    }

    public void displayRoomNotEligible(String roomNumber) {
        System.out.println("Room \"" + roomNumber
                + "\" is not currently checked in under this confirmation number.");
    }

    public void displayRoomAlreadySelected(String roomNumber) {
        System.out.println("Room \"" + roomNumber + "\" was already selected.");
    }

    public void displayRoomSelected(String roomNumber) {
        System.out.println("Room " + roomNumber + " added to this check-out.");
    }

    public boolean promptCheckOutAnotherRoom() {
        while (true) {
            System.out.print("Check out another room under this confirmation number? (y/n): ");
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

    public void displayNoRoomsSelected() {
        System.out.println("No rooms were selected. Check-out cancelled.");
    }

    public void displayAllRoomsSelected() {
        System.out.println("All checked-in rooms under this confirmation number have been selected.");
    }

    public double promptExtraCharges() {
        System.out.print("Enter extra charges (RM, 0 if none, blank to cancel): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    public void displayInvalidExtraCharges(double extraCharges) {
        System.out.println("Invalid extra charges amount. Must be zero or a positive number.");
    }

    public void displayCheckOutResult(BillingRecord billingRecord, int roomsCheckedOut,
                                       double originalRoomFee, int discountPercent, String memberTier) {
        System.out.println();
        System.out.println("======================================================");
        System.out.println("                 CHECK-OUT COMPLETE");
        System.out.println("======================================================");
        System.out.println("Billing ID          : " + billingRecord.getBillingId());
        System.out.println("Rooms Checked Out   : " + roomsCheckedOut);
        System.out.println("Original Room Fee   : RM" + originalRoomFee);
        System.out.println("Tier Discount       : " + discountPercent + "%");
        System.out.println("Discounted Room Fee : RM" + billingRecord.getRoomFee());
        System.out.println("Extra Charges       : RM" + billingRecord.getExtraCharges());
        System.out.println("Total Amount        : RM" + billingRecord.getTotalAmount());
        System.out.println("Points Earned       : +" + billingRecord.getPointsEarned());
        if (memberTier != null) {
            System.out.println("Member Tier         : " + memberTier);
        }
        System.out.println("======================================================");
    }

    // ========== 报表共用输入 ==========

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
        System.out.println("======================================================");
    }

    // ========== 报表1:Check-Out Revenue Report ==========

   public String promptReportTierFilter() {
        while (true) {
            System.out.println();
            System.out.println("Tier Filter: 1) All  2) Standard  3) Elite  4) Platinum  5) Diamond");
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    return "ALL";
                case "2":
                    return "Standard";
                case "3":
                    return "Elite";
                case "4":
                    return "Platinum";
                case "5":
                    return "Diamond";
                default:
                    System.out.println("Invalid input, please enter 1 - 5.");
            }
        }
    }

    public void displayCheckOutRevenueReportHeader(String fromDate, String toDate,
                                                    String tierFilter) {
        System.out.println();
        System.out.println("======================================================");
        System.out.println("             CHECK-OUT REVENUE REPORT");
        System.out.println("======================================================");
        System.out.println("Generated at      : " + generatedAt());
        System.out.println("Date Range        : " + fromDate + " to " + toDate);
        System.out.println("Tier Filter       : " + tierFilter);
        System.out.println("------------------------------------------------------");
        System.out.printf("  %-10s %-18s %-9s %10s %11s %10s%n",
                "Bill ID", "Guest", "Tier", "Room(RM)", "Extras(RM)", "Total(RM)");
        System.out.println("------------------------------------------------------");
    }

    /**
     * 房费和额外消费分开印:酒店最在意的就是这个比例——房费是重资产、低毛利,
     * 额外消费(餐饮、迷你吧)才是高毛利收入。只印总额会把这个资讯丢掉。
     */
    public void displayCheckOutRevenueReportRow(String billingId, String guestName, String tier,
                                                 double roomFee, double extraCharges, double totalAmount) {
        System.out.printf("  %-10s %-18s %-9s %10.2f %11.2f %10.2f%n",
                billingId, guestName, tier, roomFee, extraCharges, totalAmount);
    }

    /**
     * 房费和额外消费不另外开一整块——明细表已经有那两栏了,再开一块加总只是
     * 栏位求和,不算分析。并进 Total revenue 那一行,资讯留着、版面不浪费。
     */
    public void displayCheckOutRevenueReportSummary(int billCount, double totalRevenue,
                                                     double roomRevenue, double extraRevenue,
                                                     double averageRevenue, double highestSpend,
                                                     String highestSpendGuest, int totalPoints) {
        System.out.println("------------------------------------------------------");
        System.out.printf("  Bills settled         : %d%n", billCount);
        System.out.printf("  Total revenue         : RM %10.2f   (room %.2f + extras %.2f)%n",
                totalRevenue, roomRevenue, extraRevenue);
        System.out.printf("  Average per bill      : RM %10.2f%n", averageRevenue);
        System.out.printf("  Highest single bill   : RM %10.2f   (%s)%n", highestSpend, highestSpendGuest);
        // 积分是负债不是收入:客人以后可以拿去兑换,所以要跟收入分开看
        System.out.printf("  Loyalty points issued : %13d%n", totalPoints);
    }

    private void printMixLine(String label, double amount, double totalRevenue) {
        double share = (totalRevenue <= 0) ? 0.0 : amount * 100.0 / totalRevenue;
        System.out.printf("  %-15s RM %10.2f  (%5.1f%%)  %s%n",
                label, amount, share, bar(share));
    }

    /**
     * 按会员等级看收入分布。最后一行直接讲结论——哪一级贡献最多,
     * 不用读的人自己比柱状图长度。
     */
    public void displayRevenueByTier(Iterator<String> tierNames, Iterator<Double> tierRevenue,
                                      double totalRevenue, String topTierName, double topTierRevenue) {
        System.out.println();
        System.out.println("REVENUE BY TIER   (each * = 5% of total)");
        while (tierNames.hasNext() && tierRevenue.hasNext()) {
            printMixLine(tierNames.next(), tierRevenue.next(), totalRevenue);
        }

        double topShare = (totalRevenue <= 0) ? 0.0 : topTierRevenue * 100.0 / totalRevenue;
        System.out.printf("  Top contributor : %s (RM %.2f, %.1f%%)%n",
                topTierName, topTierRevenue, topShare);
    }

    // ========== 报表共用的显示工具 ==========

    /**
     * 报表产生的时间戳——报表是给管理层看的文件,一定要标明这份数字是什么时候的快照。
     */
    private String generatedAt() {
        return java.time.LocalDateTime.now().withNano(0)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 星号柱状图:一颗星 = 5% 占比,数得出来。金额的量级差很大(几百到几千),
     * 所以这里按百分比画,不像房间数那种可以一颗代表一间。
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

    // ========== 报表2:Room Utilisation & Status Report ==========

    public String promptReportRoomTypeFilter() {
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

    public void displayInHouseReportHeader(String checkOutBefore, String roomTypeFilter) {
        System.out.println();
        System.out.println("======================================================");
        System.out.println("       IN-HOUSE GUESTS & OUTSTANDING CHARGES");
        System.out.println("======================================================");
        System.out.println("Generated at     : " + generatedAt());
        System.out.println("Check-Out Before : " + checkOutBefore);
        System.out.println("Room Type Filter : " + roomTypeFilter);
        System.out.println("------------------------------------------------------");
        System.out.printf("  %-16s %-9s %-6s %-9s %-12s %7s %12s%n",
                "Guest", "Tier", "Room", "Type", "Check-Out", "Nights", "Accrued(RM)");
        System.out.println("------------------------------------------------------");
    }

    /**
     * 一行代表一间还被占用的房间。同一位客人订了两间房就会有两行——
     * 前台和客房部关心的单位是房间,不是人。
     *
     * @param accrued 到目前为止累积的房费,还没结帐
     */
    public void displayInHouseReportRow(String guestName, String tier, String roomNumber,
                                         String roomType, String checkOutDate,
                                         int nights, double accrued) {
        System.out.printf("  %-16s %-9s %-6s %-9s %-12s %7d %12.2f%n",
                guestName, tier, roomNumber, roomType, checkOutDate, nights, accrued);
    }

    /**
     * 汇总:三个部门各看一部分——
     * 前台看有几位客人在店、谁最快要走;财务看未结款有多少;
     * 客房部看占用率(还有多少房间被占着)。
     */
    public void displayInHouseSummary(int roomsOccupied, int totalRooms, double occupancyRate,
                                       double totalAccrued, double averageAccrued,
                                       String soonestGuest, String soonestDate, String soonestRoom) {
        System.out.println("------------------------------------------------------");
        System.out.printf("  Guests in house       : %d%n", roomsOccupied);
        System.out.printf("  Rooms occupied        : %d of %d   (%.1f%%)%n",
                roomsOccupied, totalRooms, occupancyRate);
        // 这笔钱还没进帐——客人还在住,帐单要等退房才产生
        System.out.printf("  Total accrued charges : RM %10.2f   (not yet settled)%n", totalAccrued);
        System.out.printf("  Average per room      : RM %10.2f%n", averageAccrued);
        System.out.printf("  Departing soonest     : %s  (%s, room %s)%n",
                soonestGuest, soonestDate, soonestRoom);
    }

    public void displayDepartureScheduleHeader() {
        System.out.println();
        System.out.println("DEPARTURE SCHEDULE   (rooms freeing up)");
    }

    public void displayDepartureScheduleRow(String checkOutDate, int roomCount, String roomNumbers) {
        System.out.printf("  %-12s %d room(s)   %s%n", checkOutDate, roomCount, roomNumbers);
    }


}
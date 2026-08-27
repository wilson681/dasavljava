package boundary;

import entity.Booking;
import entity.Room;
import java.util.Iterator;
import java.util.Scanner;
import utility.ValidationUtility;

// WalkInCLI.java - console for module 1 (Walk-In Registrations & Standard Booking)
// just talks to the user (Scanner in, println out), no business logic in here
//
// @author jagathis
public class WalkInCLI {

    private static final String DIVIDER = "--------------------------------------------------------";
    private static final String TABLE_DIVIDER =
            "---- ------------ ------------------ -------------------- ----------- ------------ ------";
    // report tables are wider than DIVIDER, needs its own line so it doesnt overflow
    private static final String REPORT_DIVIDER =
            "-------------------------------------------------------------------";
    private static final int MAX_BAR_WIDTH = 20;   // max stars in the bar chart

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

    // Feature 1: Register New Guest

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

    // after registering one room, ask if they want another room under the same
    // confirmation number (multi room booking)
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

    // allocation result stuff (auto triggered right after register, not its own menu item)

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

    // Feature 2: Cancel Waiting

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

    // Feature 3: View Waiting List

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

    // shared report input stuff

    // if a badly formatted date (like 2026-8-8 missing the leading 0) gets through, the
    // report just filters out everything and shows "No records match" - looks like theres
    // no data when really its just a typo. so reject bad format here and ask again instead
    //
    // returns yyyy-MM-dd, blank = "ALL" meaning no date filter
    public String promptReportDate() {
        while (true) {
            System.out.println();
            System.out.print("Filter by date (yyyy-MM-dd, blank = all dates): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return "ALL";
            }
            String normalised = ValidationUtility.normalizeDate(input);
            if (normalised != null) {
                return normalised;
            }
            System.out.println("Invalid date, please use yyyy-MM-dd (e.g. 2026-08-13).");
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

    // Report 1: Daily Registration Report

    public void displayDailyRegistrationReportHeader(String dateFilter, String roomTypeFilter) {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("             DAILY REGISTRATION REPORT");
        System.out.println(DIVIDER);
        System.out.println("Generated at     : " + generatedAt());
        System.out.println("Date Filter      : " + dateFilter);
        System.out.println("Room Type Filter : " + roomTypeFilter);
        System.out.println(DIVIDER);
        System.out.println(String.format("%-20s %-13s %-11s %-11s %s",
                "Guest", "Reg. Date", "Requested", "Allocated", "Room"));
        System.out.println(REPORT_DIVIDER);
    }

    // one row per registration. this report is only about registration - who came, what
    // they wanted, did they get it, which room. wait time stuff belongs to report 2, no
    // duration numbers printed here
    //
    // registeredDate is date only (yyyy-MM-dd), the time part is for report 2s hourly
    // breakdown. roomNumber is "-" if still waiting
    public void displayDailyRegistrationReportRow(String guestName, String registeredDate,
                                                   String requestedType, boolean allocated,
                                                   String roomNumber) {
        System.out.println(String.format("%-20s %-13s %-11s %-11s %s",
                guestName, registeredDate, requestedType,
                (allocated ? "Yes" : "No"), roomNumber));
    }

    public void displayDailyRegistrationSummary(int total, int allocatedCount, int waitingCount) {
        double successRate = (total == 0) ? 0.0 : (allocatedCount * 100.0 / total);

        System.out.println(REPORT_DIVIDER);
        System.out.println("SUMMARY");
        System.out.printf("  Total registrations : %d%n", total);
        System.out.printf("  Allocated / Waiting : %d / %d   (%.1f%% success rate)%n",
                allocatedCount, waitingCount, successRate);
    }

    // requested vs allocated per room type. the gap is unmet demand - if guests keep
    // asking for a type we cant give them, thats useful for deciding room mix
    public void displayDemandByRoomType(int standardCount, int standardAllocated,
                                         int deluxeCount, int deluxeAllocated,
                                         int suiteCount, int suiteAllocated) {
        System.out.println();
        System.out.println("DEMAND BY ROOM TYPE   (each * = 1 registration)");
        printDemandLine("Standard", standardCount, standardAllocated);
        printDemandLine("Deluxe", deluxeCount, deluxeAllocated);
        printDemandLine("Suite", suiteCount, suiteAllocated);
    }

    private void printDemandLine(String roomType, int requested, int allocated) {
        int waiting = requested - allocated;
        String unmet = (waiting > 0) ? ("   <-- " + waiting + " UNMET") : "";
        System.out.printf("  %-10s %-22s %d   (%d allocated, %d waiting)%s%n",
                roomType, bar(requested), requested, allocated, waiting, unmet);
    }

    // Report 2: Wait Time Analysis Report

    public void displayWaitTimeAnalysisHeader(String dateFilter, String roomTypeFilter) {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("             WAIT TIME ANALYSIS REPORT");
        System.out.println(DIVIDER);
        System.out.println("Generated at     : " + generatedAt());
        System.out.println("Date Filter      : " + dateFilter);
        System.out.println("Room Type Filter : " + roomTypeFilter);
        System.out.println(DIVIDER);
        System.out.println(String.format("%-12s %-20s %-11s %-12s %s",
                "Booking ID", "Guest", "Room Type", "Wait (min)", "Status"));
        System.out.println(REPORT_DIVIDER);
    }

    public void displayWaitByRoomTypeHeader() {
        System.out.println();
        System.out.println("AVERAGE WAIT BY ROOM TYPE   (allocated only)");
        System.out.println(String.format("%-11s %-13s %s", "Type", "Avg Wait", "Bookings"));
        System.out.println(REPORT_DIVIDER);
    }

    // one row per room type. average only counts allocated ones - still-waiting durations
    // keep growing so mixing them in would skew the number, shown separately in brackets
    // instead
    public void displayWaitByRoomTypeRow(String roomType, int allocated, int totalWait, int waiting) {
        String average = (allocated == 0)
                ? "-"
                : String.format("%.1f min", (double) totalWait / allocated);

        String stillWaiting = (waiting > 0) ? ("   <-- " + waiting + " still waiting") : "";

        System.out.println(String.format("%-11s %-13s %d allocated%s",
                roomType, average, allocated, stillWaiting));
    }

    public void displaySlowestRoomType(String roomType, double averageWait) {
        if ("-".equals(roomType)) {
            System.out.println("  No allocated booking to compare by room type.");
            return;
        }
        System.out.printf("  Slowest type : %s (%.1f min average)%n", roomType, averageWait);
    }

    // allocated=false means still waiting, that wait number is "as of right now" and
    // keeps growing - Status column needs to make that clear, dont read it the same way
    // as an allocated one
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

    // busiestHours can have more than one hour tied for most bookings, empty means no
    // data at all to aggregate. hourCount = how many are tied, only say "each" if more
    // than one
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

    // shared display helpers for reports

    // timestamp for when the report was generated. reports go to management so it needs
    // to show "this is a snapshot as of when", especially for the still-waiting numbers
    // that change every time you rerun it
    private String generatedAt() {
        return java.time.LocalDateTime.now().withNano(0)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // turns minutes into something readable. "11085 min" means nothing but "7d 16h"
    // makes it obvious somethings wrong
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

    // star bar chart, 1 star = 1 booking, literally countable
    // used to scale it before (biggest bar always fixed at 20 stars) but then 2 bookings
    // also printed 20 stars and nobody could tell what a star meant without checking the
    // number next to it anyway - kind of pointless
    // now its 1:1 so the stars ARE the count, header just explains that once
    // caps at MAX_BAR_WIDTH with a + after so it doesnt print hundreds of stars and blow
    // up the table, real number is right there in the next column regardless
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

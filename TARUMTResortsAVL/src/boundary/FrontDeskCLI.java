package boundary;

import entity.Booking;
import entity.BillingRecord;
import java.util.Iterator;
import java.util.Scanner;

/**
 * FrontDeskCLI.java
 * Boundary for the Front-Desk Service module.
 *
 * Responsible only for user input and output.
 */
public class FrontDeskCLI {

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
    
    public String promptConfirmationNumber() {
        System.out.print("Enter 8-digit confirmation number: ");
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
     */
    public void displayCurrentCharges(String chargeLines, int roomCount,
                                      int totalNights, double totalCharges) {
        System.out.println("------------------------------------------------------");
        System.out.println("CURRENT CHARGES  (rooms still checked in)");
        System.out.println("------------------------------------------------------");

        if (roomCount == 0) {
            System.out.println("  None - no room is currently checked in.");
            return;
        }

        System.out.printf("  %-6s %-10s %12s %8s %14s%n",
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
     * @return the room type name, or null when the user wants every type
     */
    public String promptRoomTypeFilter() {
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
        return null;
    }

   /**
     * Prints the room availability query: which rooms can be sold right now,
     * and how many are blocked.
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
        System.out.printf("  %-10s %10s %9s%n", "Type", "Available", "Blocked");
        System.out.print(breakdownLines);
        System.out.println("------------------------------------------------------");
        System.out.println("ROOMS READY TO SELL  (" + available + ")");
        System.out.println("------------------------------------------------------");

        if (available == 0) {
            System.out.println("  None - no room can be sold right now.");
        } else {
            System.out.printf("  %-6s %-10s %12s%n", "Room", "Type", "Rate/night");
            System.out.print(availableLines);
        }

        System.out.println("------------------------------------------------------");
        System.out.println("  " + available + " room(s) can be sold right now.");
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
        System.out.print("Check out another room under this confirmation number? (y/n): ");
        String input = scanner.nextLine().trim();
        return input.equalsIgnoreCase("y");
    }

    public void displayNoRoomsSelected() {
        System.out.println("No rooms were selected. Check-out cancelled.");
    }

    public double promptExtraCharges() {
        System.out.print("Enter extra charges (RM, 0 if none): ");
        try {
            return Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
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
}
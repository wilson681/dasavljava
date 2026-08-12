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

    public void displayCheckOutResult(BillingRecord billingRecord, int roomsCheckedOut, String memberTier) {
        System.out.println();
        System.out.println("======================================================");
        System.out.println("                 CHECK-OUT COMPLETE");
        System.out.println("======================================================");
        System.out.println("Billing ID          : " + billingRecord.getBillingId());
        System.out.println("Rooms Checked Out   : " + roomsCheckedOut);
        System.out.println("Room Fee            : RM" + billingRecord.getRoomFee());
        System.out.println("Extra Charges       : RM" + billingRecord.getExtraCharges());
        System.out.println("Total Amount        : RM" + billingRecord.getTotalAmount());
        System.out.println("Points Earned       : +" + billingRecord.getPointsEarned());
        if (memberTier != null) {
            System.out.println("Member Tier         : " + memberTier);
        }
        System.out.println("======================================================");
    }
}
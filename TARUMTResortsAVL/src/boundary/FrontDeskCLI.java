package boundary;

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
    public void displayGuestDetails(String confirmationNumber,
                                String name,
                                String phone,
                                String memberId,
                                String tier,
                                String roomNumber,
                                String roomType) {

        System.out.println();
        System.out.println("===== Guest Details =====");
        System.out.println("Confirmation Number : " + confirmationNumber);
        System.out.println("Name                : " + name);
        System.out.println("Phone               : " + phone);
        System.out.println("Member ID           : " + memberId);
        System.out.println("Tier                : " + tier);
        System.out.println("Room Number         : " + roomNumber);
        System.out.println("Room Type           : " + roomType);
    }
    public void displayGuestNotFound() {
        System.out.println("Guest not found.");
    }
    
}
package boundary;

import java.util.Scanner;

/**
 * Handles user input and output for the system main menu and report menu.
 *
 * @author Lim Wei Shern
 */
public class MainMenuCLI {

    private Scanner scanner;

    public MainMenuCLI() {
        scanner = new Scanner(System.in);
    }

    /**
     * Displays the startup banner.
     */
    public void displayWelcome() {
        System.out.println("========================================");
        System.out.println("      TAR UMT RESORTS MANAGEMENT");
        System.out.println("========================================");
        System.out.println("System started successfully.");
    }

    /**
     * Displays the main menu and reads the user's choice.
     *
     * @return the selected number, or -1 if the input is not numeric
     */
    public int displayMainMenuAndGetChoice() {
        System.out.println();
        System.out.println("===== Main Menu =====");
        System.out.println("1) Walk-In Registrations");
        System.out.println("2) VIP & Loyalty Tier Priority Allocation");
        System.out.println("3) Housekeeping and Task Log");
        System.out.println("4) Front-Desk Service");
        System.out.println("5) Loyalty and Rewards Service");
        System.out.println("6) Reports");
        System.out.println("0) Exit");
        System.out.print("Enter your choice: ");

        String input = scanner.nextLine().trim();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Displays an invalid menu choice message.
     */
    public void displayInvalidChoice() {
        System.out.println("Invalid input, please try again.");
    }

    /**
     * Displays the reports available across all modules and reads the user's choice.
     *
     * @return the selected report number, or -1 if the input is not numeric
     */
    public int displayReportsMenuAndGetChoice() {
        System.out.println();
        System.out.println("===== Reports =====");
        System.out.println();
        System.out.println("Walk-In Registrations & Standard Booking");
        System.out.println("   1) Daily Registration Report");
        System.out.println("   2) Wait Time Analysis Report");
        System.out.println("VIP & Loyalty Tier Priority Allocation");
        System.out.println("   3) Live VIP Waiting Queue & SLA Report");
        System.out.println("   4) Tier Allocation SLA Report");
        System.out.println("Housekeeping and Task Log");
        System.out.println("   5) Housekeeping Status Report");
        System.out.println("   6) Room History Activity Report");
        System.out.println("Front-Desk Service");
        System.out.println("   7) Check-Out Revenue Report");
        System.out.println("   8) In-House Guests & Outstanding Charges");
        System.out.println("Loyalty and Rewards Service");
        System.out.println("   9) Points Expiry Report");
        System.out.println("  10) Top Redeemed Items Report");
        System.out.println("   0) Back to Main Menu");
        System.out.print("Enter your choice: ");

        String input = scanner.nextLine().trim();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Pauses the screen until the user presses Enter.
     */
    public void promptContinue() {
        System.out.println();
        System.out.print("Press Enter to continue...");
        scanner.nextLine();
    }

     /**
     * Displays a message when a selected module is not ready.
     *
     * @param moduleName the module display name
     */
    public void displayModuleNotReady(String moduleName) {
        System.out.println(moduleName + " module is not ready yet.");
    }

    /**
     * Displays the system shutdown message.
     */
    public void displayExitMessage() {
        System.out.println("System shut down. Thank you for using TARUMT Resorts.");
    }
}

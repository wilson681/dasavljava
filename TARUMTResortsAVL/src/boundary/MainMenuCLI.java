package boundary;

import java.util.Scanner;

/**
 * MainMenuCLI.java - Displays the system main screen and main menu, and reads
 * the actor's menu choice.
 *
 * 注意:所有 console 上会印出来的文字都用英文——这是"系统运行时的输出",
 * 跟代码注释(给我们自己看,可以中文)是两回事。
 *
 * @author Wilson
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
     * Displays the main menu and reads the actor's choice.
     * @return the number the actor typed, or -1 if the input wasn't a number
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

        // 读一行输入,转成数字;打的不是数字就回传 -1,交给Control判断成无效选择
        String input = scanner.nextLine().trim();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Tells the actor their menu choice wasn't valid.
     */
    public void displayInvalidChoice() {
        System.out.println("Invalid input, please try again.");
    }

    /**
     * Tells the actor the module they picked isn't built yet.
     * @param moduleName the module's display name
     */
    public void displayModuleNotReady(String moduleName) {
        System.out.println(moduleName + " module is not ready yet.");
    }

    /**
     * Displays the shutdown message.
     */
    public void displayExitMessage() {
        System.out.println("System shut down. Thank you for using TARUMT Resorts.");
    }
}

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

    private Scanner scanner;

    public WalkInCLI() {
        scanner = new Scanner(System.in);
    }

    public int displayMenuAndGetChoice() {
        System.out.println();
        System.out.println("===== Walk-In Registrations & Standard Booking =====");
        System.out.println("1) Register New Guest");
        System.out.println("2) Allocate Room");
        System.out.println("3) Cancel Waiting");
        System.out.println("4) View Waiting List");
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

    // ========== 功能1:登记新客人 ==========

    public String promptName() {
        System.out.print("Enter guest name: ");
        return scanner.nextLine().trim();
    }

    public String promptPhone() {
        System.out.print("Enter phone number: ");
        return scanner.nextLine().trim();
    }

    public String promptRoomType() {
        System.out.println("Room Type: 1) Standard  2) Deluxe  3) Suite");
        System.out.print("Select room type: ");
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
        System.out.println("Registration successful!");
        System.out.println("Confirmation Number: " + booking.getConfirmationNumber());
        System.out.println("Room Type: " + booking.getRequestedRoomType());
    }

    // ========== 功能2:分房 ==========

    public void displayVipHasPriority(String roomType) {
        System.out.println("A VIP guest is waiting for " + roomType
                + " rooms. Please process the VIP allocation first.");
    }

    public void displayNoOneWaiting(String roomType) {
        System.out.println("No walk-in guest is currently waiting for " + roomType + " rooms.");
    }

    public void displayNoRoomAvailable(String roomType) {
        System.out.println("No " + roomType + " room is available right now.");
    }

    public int promptNumberOfNights() {
        System.out.print("Enter number of nights: ");
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public void displayAllocationResult(Booking booking, Room room) {
        System.out.println("Room allocated successfully!");
        System.out.println(booking.getGuestNameSnapshot() + " (Confirmation: " + booking.getConfirmationNumber()
                + ") has been allocated Room " + room.getRoomNumber());
    }

    // ========== 功能3:取消排队 ==========

    public String promptConfirmationNumberToCancel() {
        System.out.print("Enter the confirmation number to cancel: ");
        return scanner.nextLine().trim();
    }

    public void displayCancelResult(boolean success) {
        if (success) {
            System.out.println("Cancelled successfully.");
        } else {
            System.out.println("Booking not found. Cancellation failed.");
        }
    }

    // ========== 功能4:查看排队名单 ==========

    public void displayWaitingList(String roomType, Iterator<Booking> waitingList) {
        System.out.println("===== " + roomType + " Walk-In Waiting List (arrival order) =====");
        if (!waitingList.hasNext()) {
            System.out.println("No one is currently waiting.");
            return;
        }
        int rank = 1;
        while (waitingList.hasNext()) {
            Booking booking = waitingList.next();
            System.out.println(rank + ". " + booking);
            rank++;
        }
    }
}

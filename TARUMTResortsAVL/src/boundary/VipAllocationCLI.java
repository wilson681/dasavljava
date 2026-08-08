package boundary;

import entity.Booking;
import entity.Room;
import java.util.Iterator;
import java.util.Scanner;

/**
 * VipAllocationCLI.java - 模块2(VIP & Loyalty Tier Priority Room Allocation)的 console 界面。
 *
 * @author 某某
 *
 * 说明:
 * - 只负责跟使用者对话(Scanner输入、println输出),不做任何业务判断
 * - 参数/回传值里出现的 Booking、Room 只是拿来显示用,不会在这里被建立或修改
 * - 所有 console 印出来的文字都用英文,代码注释才用中文
 */
public class VipAllocationCLI {

    private Scanner scanner;

    public VipAllocationCLI() {
        scanner = new Scanner(System.in);
    }

    /**
     * 印出这个模块的选单,读使用者的选择。
     * @return 使用者输入的数字,不是数字则回传 -1
     */
    public int displayMenuAndGetChoice() {
        System.out.println();
        System.out.println("===== VIP & Loyalty Tier Priority Allocation =====");
        System.out.println("1) VIP Registration");
        System.out.println("2) Allocate Room");
        System.out.println("3) Cancel Waiting");
        System.out.println("4) View VIP Waiting List");
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

    // ========== 功能1:VIP登记 ==========

    public String promptMemberId() {
        System.out.print("Enter member ID: ");
        return scanner.nextLine().trim();
    }

    public String promptRoomType() {
        System.out.println("Room Type: 1) Standard  2) Deluxe  3) Suite");
        System.out.print("Select room type: ");
        // 把数字选项换成实际房型文字,打错或直接打文字都原样传出去,交给Control判断合不合法
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

    public void displayMemberNotFound(String memberId) {
        System.out.println("Member ID " + memberId + " not found. Registration failed.");
    }

    public void displayInvalidRoomType(String roomType) {
        System.out.println("Room type \"" + roomType + "\" is invalid. Please try again.");
    }

    public void displayRegistrationResult(Booking booking, String tier) {
        System.out.println("Registration successful!");
        System.out.println("Booking ID: " + booking.getBookingId());
        System.out.println("Confirmation Number: " + booking.getConfirmationNumber());
        System.out.println("Tier: " + tier + " | Room Type: " + booking.getRequestedRoomType());
    }

    // ========== 功能2:分房 ==========

    public void displayNoOneWaiting(String roomType) {
        System.out.println("No VIP is currently waiting for " + roomType + " rooms.");
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
        System.out.println("Guest: " + booking.getGuestNameSnapshot()
                + " | Confirmation Number: " + booking.getConfirmationNumber()
                + " | Room: " + room.getRoomNumber());
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

    // ========== 功能4:查看VIP等待名单 ==========

    public void displayWaitingList(String roomType, Iterator<Booking> waitingList) {
        System.out.println("===== " + roomType + " VIP Waiting List (highest priority first) =====");
        if (!waitingList.hasNext()) {
            System.out.println("No one is currently waiting.");
            return;
        }
        int rank = 1;
        while (waitingList.hasNext()) {
            Booking booking = waitingList.next();
            System.out.println(rank + ". Booking ID: " + booking.getBookingId()
                    + " | Confirmation Number: " + booking.getConfirmationNumber()
                    + " | Guest: " + booking.getGuestNameSnapshot()
                    + " | Room Type: " + booking.getRequestedRoomType()
                    + " | Status: " + booking.getStatus()
                    + " | Room: " + (booking.getAssignedRoomNo() == null ? "-" : booking.getAssignedRoomNo()));
            rank++;
        }
    }
}

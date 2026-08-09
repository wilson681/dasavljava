package control;

import adt.HashTableInterface;
import adt.ListInterface;
import boundary.FrontDeskCLI;
import entity.Guest;
import entity.Room;

/**
 * Control for the Front-Desk Service module.
 */
public class FrontDeskControl {

    private FrontDeskCLI frontDeskCLI;
    private HashTableInterface<Guest> guestTable;
    private ListInterface<Room> roomList;
    
    public FrontDeskControl(
        FrontDeskCLI frontDeskCLI,
        HashTableInterface<Guest> guestTable,
        ListInterface<Room> roomList) {

    this.frontDeskCLI = frontDeskCLI;
    this.guestTable = guestTable;
    this.roomList = roomList;
}

    public void run() {

        boolean running = true;

        while (running) {

            int choice = frontDeskCLI.displayMenuAndGetChoice();

            switch (choice) {

                case 1:
                    searchGuestByConfirmationNumber();
                    break;

                case 2:
                    System.out.println("Room Availability - coming soon.");
                    break;

                case 3:
                    System.out.println("Billing Details - coming soon.");
                    break;

                case 0:
                    running = false;
                    break;

                default:
                    frontDeskCLI.displayInvalidChoice();
                    break;
            }
        }
    }
    
    private void searchGuestByConfirmationNumber() {

    String confirmationNumber =
            frontDeskCLI.promptConfirmationNumber();

    Guest searchKey = new Guest(confirmationNumber);

    Guest foundGuest = guestTable.getEntry(searchKey);

    if (foundGuest == null) {
        frontDeskCLI.displayGuestNotFound();
    } else {

    String roomNumber = "-";
    String roomType = "-";

    if (!foundGuest.getBookedRooms().isEmpty()) {

        roomNumber =
                foundGuest.getBookedRooms().getEntry(1);
for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {

    Room room = roomList.getEntry(i);

    if (room.getRoomNumber().equals(roomNumber)) {
        roomType = room.getRoomType();
        break;
        }
    }
}

    frontDeskCLI.displayGuestDetails(
            foundGuest.getConfirmationNumber(),
            foundGuest.getName(),
            foundGuest.getPhone(),
            foundGuest.getMemberId(),
            foundGuest.getTier(),
            roomNumber,
            roomType
    );
}
            
}
    
}
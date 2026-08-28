package control;

import adt.ArrayBasedList;
import adt.AVLTree;
import adt.ChainingHashTable;
import adt.CircularLinkedQueue;
import adt.HashTableInterface;
import adt.ListInterface;
import adt.QueueInterface;
import adt.SearchTreeInterface;
import boundary.LoyaltyCLI;
import boundary.MainMenuCLI;
import boundary.VipAllocationCLI;
import boundary.WalkInCLI;
import boundary.FrontDeskCLI;
import boundary.HousekeepingCLI;
import dao.BillingRecordDao;
import dao.BookingDao;
import dao.GuestBookingDao;
import dao.GuestDao;
import dao.MemberDao;
import dao.PointsLedgerDao;
import dao.RedemptionHistoryDao;
import dao.RedemptionItemDao;
import dao.RollbackLogDao;
import dao.RoomDao;
import entity.Booking;
import entity.Guest;
import entity.Member;
import entity.RedemptionItem;
import entity.RedemptionTransaction;
import entity.Room;
import entity.RollbackLogEntry;

/**
 * Controls the system main menu and connects the five modules.
 * Shared ADT instances are created here and passed to the modules that
 * need them so all modules operate on the same data.
 *
 * @author Lim Wei Shern
 */
public class MainMenuControl {

    // Number of reports available in the Reports menu.
    private static final int REPORT_COUNT = 10;

    private final MainMenuCLI mainMenuCLI;

    // Shared VIP waiting trees, separated by room type.
    private final SearchTreeInterface<Booking> standardVipTree;
    private final SearchTreeInterface<Booking> deluxeVipTree;
    private final SearchTreeInterface<Booking> suiteVipTree;

    // Shared Walk-In queues, separated by room type.
    private final QueueInterface<Booking> standardWalkInQueue;
    private final QueueInterface<Booking> deluxeWalkInQueue;
    private final QueueInterface<Booking> suiteWalkInQueue;

    // Shared system data used across multiple modules.
    private final ListInterface<Member> memberList;
    private final ListInterface<Room> roomList;
    private final HashTableInterface<Guest> guestTable;

    // Shared Loyalty and Rewards data.
    private final ListInterface<RedemptionItem> redemptionItemList;
    private final ListInterface<RedemptionTransaction> redemptionTransactionList;

    // Stores successful housekeeping rollback events.
    private final ListInterface<RollbackLogEntry> rollbackLog;

    // Module controls are created once and reused throughout the program.
    // This preserves their counters and shared state between menu visits.
    private final WalkInControl walkInControl;
    private final VipAllocationControl vipAllocationControl;
    private final FrontDeskControl frontDeskControl;
    private final LoyaltyControl loyaltyControl;
    private final HousekeepingControl housekeepingControl;

    /**
     * Creates the shared ADTs, loads the initial data and assembles
     * the module controls.
     *
     * @param mainMenuCLI the boundary responsible for the system main menu
     */
    public MainMenuControl(MainMenuCLI mainMenuCLI) {
        if (mainMenuCLI == null) {
            throw new IllegalArgumentException("mainMenuCLI cannot be null");
        }
        this.mainMenuCLI = mainMenuCLI;

        this.standardVipTree = new AVLTree<>();
        this.deluxeVipTree = new AVLTree<>();
        this.suiteVipTree = new AVLTree<>();

        this.standardWalkInQueue = new CircularLinkedQueue<>();
        this.deluxeWalkInQueue = new CircularLinkedQueue<>();
        this.suiteWalkInQueue = new CircularLinkedQueue<>();

        this.memberList = new ArrayBasedList<>();
        this.roomList = new ArrayBasedList<>();
        this.guestTable = new ChainingHashTable<>();

        this.redemptionItemList = new ArrayBasedList<>();
        this.redemptionTransactionList = new ArrayBasedList<>();

        this.rollbackLog = new ArrayBasedList<>();

        // Load the main member and room records into the shared ADTs.
        new MemberDao().loadMembers(memberList);
        new RoomDao().loadRooms(roomList);
        // Each room owns its own status-history stack.
        // Record its initial status so rollback has a starting state.
        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {

            Room room = (Room) roomList.getEntry(i);

            if (room.getRoomHistory()
                    .getStatusStack()
                    .isEmpty()) {

                // Seed housekeeping rooms with OCCUPIED as the previous state
                // before recording their current housekeeping status.
                if (room.getStatus().equals("NEEDS_CLEANING")
                        || room.getStatus().equals("CLEANING_IN_PROGRESS")
                        || room.getStatus().equals("INSPECTED")) {
                    room.getRoomHistory()
                            .getStatusStack()
                            .push("OCCUPIED");
                }

                room.getRoomHistory()
                        .getStatusStack()
                        .push(room.getStatus());
            }
        }

        new RedemptionItemDao().loadRedemptionItems(redemptionItemList);

        // Load additional seed records required by the modules and reports.
        new PointsLedgerDao().loadPointsLedger(memberList);
        new RedemptionHistoryDao().loadRedemptionHistory(redemptionTransactionList);
        new RollbackLogDao().loadRollbackLog(rollbackLog);
        new BookingDao().loadWaitingBookings(memberList,
                standardWalkInQueue, deluxeWalkInQueue, suiteWalkInQueue,
                standardVipTree, deluxeVipTree, suiteVipTree);
        new GuestDao().loadGuests(guestTable);
        new GuestBookingDao().loadGuestBookings(guestTable, memberList);
        new BillingRecordDao().loadBillingRecords(guestTable);
        // Walk-In also receives the VIP trees so it can check VIP priority
        // before allocating a room.
        this.walkInControl = new WalkInControl(
                new WalkInCLI(), standardWalkInQueue, deluxeWalkInQueue, suiteWalkInQueue,
                standardVipTree, deluxeVipTree, suiteVipTree, roomList, guestTable, memberList);
        this.vipAllocationControl = new VipAllocationControl(
                new VipAllocationCLI(), standardVipTree, deluxeVipTree, suiteVipTree,
                memberList, roomList, guestTable);
        // Housekeeping needs both allocation controls so an available room
        // can be offered to waiting bookings again.
        this.housekeepingControl = new HousekeepingControl(
                new HousekeepingCLI(),
                roomList,
                vipAllocationControl,
                walkInControl,
                rollbackLog);
        // Front Desk uses Loyalty for points and tier information.
        this.loyaltyControl = new LoyaltyControl(
                new LoyaltyCLI(), memberList, redemptionItemList, redemptionTransactionList);
        // Front Desk also uses Housekeeping when checked-out rooms
        // need to enter the cleaning process.
        this.frontDeskControl = new FrontDeskControl(
                new FrontDeskCLI(), guestTable, roomList, loyaltyControl, housekeepingControl);
    }

    /**
     * Runs the main menu until the user chooses to exit.
     */
    public void run() {
        mainMenuCLI.displayWelcome();

        boolean running = true;
        while (running) {
            int choice = mainMenuCLI.displayMainMenuAndGetChoice();

            switch (choice) {
                case 1:
                    runWalkInModule();
                    break;
                case 2:
                    runVipAllocationModule();
                    break;
                case 3:
                    runHousekeepingModule();
                    break;
                case 4:
                    runFrontDeskModule();
                    break;
                case 5:
                    runLoyaltyModule();
                    break;
                case 6:
                    runReportsMenu();
                    break;
                case 0:
                    running = false;
                    break;
                default:
                    mainMenuCLI.displayInvalidChoice();
            }
        }

        mainMenuCLI.displayExitMessage();
    }

    private void runWalkInModule() {
        walkInControl.run();
    }

    private void runHousekeepingModule() {
        housekeepingControl.run();
    }

    private void runVipAllocationModule() {
        vipAllocationControl.run();
    }

    private void runFrontDeskModule() {
        frontDeskControl.run();
    }

    private void runLoyaltyModule() {
        loyaltyControl.run();
    }

    /**
     * Runs the Reports menu and routes each selection to the corresponding
     * module control. Report filtering, sorting and calculations remain
     * inside the module that owns the report.
     */
    private void runReportsMenu() {
        boolean running = true;
        while (running) {
            int choice = mainMenuCLI.displayReportsMenuAndGetChoice();

            // Return to the main menu.
            if (choice == 0) {
                running = false;
                continue;
            }

            // Invalid report selections are rejected before dispatching.
            if (choice < 1 || choice > REPORT_COUNT) {
                mainMenuCLI.displayInvalidChoice();
                continue;
            }

            switch (choice) {
                case 1:
                    walkInControl.doDailyRegistrationReport();
                    break;
                case 2:
                    walkInControl.doWaitTimeAnalysisReport();
                    break;
                case 3:
                    vipAllocationControl.doVipWaitingListReport();
                    break;
                case 4:
                    vipAllocationControl.doTierSlaReport();
                    break;
                case 5:
                    housekeepingControl.doGenerateHousekeepingStatusReport();
                    break;
                case 6:
                    housekeepingControl.doGenerateRoomHistoryReport();
                    break;
                case 7:
                    frontDeskControl.doCheckOutRevenueReport();
                    break;
                case 8:
                    frontDeskControl.doInHouseGuestsReport();
                    break;
                case 9:
                    loyaltyControl.doPointsExpiryReport();
                    break;
                case 10:
                    loyaltyControl.doTopRedeemedItemsReport();
                    break;
                default:
                    // All invalid values are handled by the range check above.
                    break;
            }

            // Pause after a report so the user can read it before the menu
            // is displayed again.
            mainMenuCLI.promptContinue();
        }
    }
}

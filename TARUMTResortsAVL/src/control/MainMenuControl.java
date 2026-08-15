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
import dao.MemberDao;
import dao.RedemptionItemDao;
import dao.RoomDao;
import entity.Booking;
import entity.Guest;
import entity.Member;
import entity.RedemptionItem;
import entity.RedemptionTransaction;
import entity.Room;
import entity.RollbackLogEntry;

/**
 * MainMenuControl.java - Controls the main menu flow: shows the main screen,
 * reads the actor's menu choice, and routes to each module's own Control.
 *
 * 这里同时是"总装配点"——各模块要共用的 ADT 实例,都在这里 new 一次, 再传给需要用到的模块
 * Control,让大家共用同一份、不是各自另外造一份。
 *
 * @author Wilson
 */
public class MainMenuControl {

    private final MainMenuCLI mainMenuCLI;

    // ===== 共用容器,按房型分开的三棵VIP树 =====
    private final SearchTreeInterface<Booking> standardVipTree;
    private final SearchTreeInterface<Booking> deluxeVipTree;
    private final SearchTreeInterface<Booking> suiteVipTree;

    // ===== 共用容器,按房型分开的三条Walk-In队伍 =====
    private final QueueInterface<Booking> standardWalkInQueue;
    private final QueueInterface<Booking> deluxeWalkInQueue;
    private final QueueInterface<Booking> suiteWalkInQueue;

    // ===== 共用容器,给各模块查资料用 =====
    private final ListInterface<Member> memberList;
    private final ListInterface<Room> roomList;
    private final HashTableInterface<Guest> guestTable;

    // ===== 共用容器,给模块5(Loyalty and Rewards)用 =====
    private final ListInterface<RedemptionItem> redemptionItemList;
    private final ListInterface<RedemptionTransaction> redemptionTransactionList;

    // ===== 共用容器,给模块3(Housekeeping)自己写、自己读的回滚事件流水,其他模块不会用到 =====
    private final ListInterface<RollbackLogEntry> rollbackLog;

    // ===== 各模块的Control只造一次、整个程式生命周期共用一份 =====
    // 之前每次进入模块都 new 一个新的Control,会把里面的bookingCounter/confirmationCounter
    // 归零重来,但共用队伍/树里还留着旧资料,导致新登记跟旧的撞号——所以改成在这里只建一次
    private final WalkInControl walkInControl;
    private final VipAllocationControl vipAllocationControl;
    private final FrontDeskControl frontDeskControl;
    private final LoyaltyControl loyaltyControl;
    private final HousekeepingControl housekeepingControl;

    /**
     * @param mainMenuCLI the boundary responsible for displaying the main
     * screen
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

        // 用DAO从txt把种子资料读进来,填满memberList/roomList/redemptionItemList,
        // 这样VIP/Walk-In/Loyalty模块才有真的会员/房间/兑换清单资料可以测试,不用硬编码
        new MemberDao().loadMembers(memberList);
        new RoomDao().loadRooms(roomList);
        /*
         * Each Room already owns its own RoomHistory.
         * After loading all rooms, record each room's initial status
         * into its Stack so rollback has a starting state.
         */
        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {

            Room room = (Room) roomList.getEntry(i);

            if (room.getRoomHistory()
                    .getStatusStack()
                    .isEmpty()) {

                room.getRoomHistory()
                        .getStatusStack()
                        .push(room.getStatus());
            }
        }

        new RedemptionItemDao().loadRedemptionItems(redemptionItemList);

        // WalkInControl也要拿到VIP的三棵树(只读,用来检查isEmpty())才能落实
        // "VIP永远优先"这条两个模块共用的规则
        this.walkInControl = new WalkInControl(
                new WalkInCLI(), standardWalkInQueue, deluxeWalkInQueue, suiteWalkInQueue,
                standardVipTree, deluxeVipTree, suiteVipTree, roomList, guestTable, memberList);
        this.vipAllocationControl = new VipAllocationControl(
                new VipAllocationCLI(), standardVipTree, deluxeVipTree, suiteVipTree,
                memberList, roomList, guestTable);
        // loyaltyControl要先造出来,因为退房结账(FrontDeskControl)要拿它的引用
        // 去调用awardPointsByMemberId(),不能反过来

        this.housekeepingControl = new HousekeepingControl(
                new HousekeepingCLI(),
                roomList,
                vipAllocationControl,
                walkInControl,
                rollbackLog);

        this.loyaltyControl = new LoyaltyControl(
                new LoyaltyCLI(), memberList, redemptionItemList, redemptionTransactionList);

        this.frontDeskControl = new FrontDeskControl(
                new FrontDeskCLI(), guestTable, roomList, loyaltyControl, housekeepingControl);
    }

    /**
     * Launches the main screen and runs the main menu loop until the actor
     * exits.
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
     * Reports 菜单——扁平列出全部11份报表,选哪个就直接呼叫该模块自己的报表方法
     * (那些方法是package-private,只放宽到让同package的这里叫得到,boundary层还是碰不到)。
     * 这里本身不装任何filter/sort逻辑,纯粹是分发,报表怎么算还是各模块自己的Control负责。
     */
    private void runReportsMenu() {
        boolean running = true;
        while (running) {
            int choice = mainMenuCLI.displayReportsMenuAndGetChoice();
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
                    housekeepingControl.doGenerateRollbackFrequencyReport();
                    break;
                case 8:
                    frontDeskControl.doCheckOutRevenueReport();
                    break;
                case 9:
                    frontDeskControl.doRoomUtilisationReport();
                    break;
                case 10:
                    loyaltyControl.doPointsExpiryReport();
                    break;
                case 11:
                    loyaltyControl.doTopRedeemedItemsReport();
                    break;
                case 0:
                    running = false;
                    break;
                default:
                    mainMenuCLI.displayInvalidChoice();
            }
        }
    }
}

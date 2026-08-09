package control;

import adt.ArrayBasedList;
import adt.AVLTree;
import adt.ChainingHashTable;
import adt.CircularLinkedQueue;
import adt.HashTableInterface;
import adt.ListInterface;
import adt.QueueInterface;
import adt.SearchTreeInterface;
import boundary.MainMenuCLI;
import boundary.VipAllocationCLI;
import boundary.WalkInCLI;
import boundary.FrontDeskCLI;
import dao.MemberDao;
import dao.RoomDao;
import entity.Booking;
import entity.Guest;
import entity.Member;
import entity.Room;

/**
 * MainMenuControl.java - Controls the main menu flow: shows the main screen,
 * reads the actor's menu choice, and routes to each module's own Control.
 *
 * 这里同时是"总装配点"——各模块要共用的 ADT 实例,都在这里 new 一次,
 * 再传给需要用到的模块 Control,让大家共用同一份、不是各自另外造一份。
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

    // ===== 各模块的Control只造一次、整个程式生命周期共用一份 =====
    // 之前每次进入模块都 new 一个新的Control,会把里面的bookingCounter/confirmationCounter
    // 归零重来,但共用队伍/树里还留着旧资料,导致新登记跟旧的撞号——所以改成在这里只建一次
    private final WalkInControl walkInControl;
    private final VipAllocationControl vipAllocationControl;
    private final FrontDeskControl frontDeskControl;

    /**
     * @param mainMenuCLI the boundary responsible for displaying the main screen
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

        // 用DAO从txt把种子资料读进来,填满memberList/roomList,
        // 这样VIP/Walk-In模块才有真的会员/房间资料可以测试,不用硬编码
        new MemberDao().loadMembers(memberList);
        new RoomDao().loadRooms(roomList);

        // WalkInControl也要拿到VIP的三棵树(只读,用来检查isEmpty())才能落实
        // "VIP永远优先"这条两个模块共用的规则
        this.walkInControl = new WalkInControl(
                new WalkInCLI(), standardWalkInQueue, deluxeWalkInQueue, suiteWalkInQueue,
                standardVipTree, deluxeVipTree, suiteVipTree, roomList, guestTable);
        this.vipAllocationControl = new VipAllocationControl(
                new VipAllocationCLI(), standardVipTree, deluxeVipTree, suiteVipTree,
                memberList, roomList, guestTable);
        this.frontDeskControl = new FrontDeskControl(
        new FrontDeskCLI(),
        guestTable,
        roomList);
    }

    /**
     * Launches the main screen and runs the main menu loop until the actor exits.
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
                    mainMenuCLI.displayModuleNotReady("Housekeeping and Task Log");
                    break;
                case 4:
                    runFrontDeskModule();
                    break;
                case 5:
                    mainMenuCLI.displayModuleNotReady("Loyalty and Rewards Service");
                    break;
                case 6:
                    mainMenuCLI.displayModuleNotReady("Reports");
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

    private void runVipAllocationModule() {
        vipAllocationControl.run();
    }

    private void runFrontDeskModule() {
        frontDeskControl.run();
    }
}

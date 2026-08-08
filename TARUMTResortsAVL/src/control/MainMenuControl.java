package control;

import adt.ArrayBasedList;
import adt.AVLTree;
import adt.ChainingHashTable;
import adt.HashTableInterface;
import adt.ListInterface;
import adt.SearchTreeInterface;
import boundary.MainMenuCLI;
import boundary.VipAllocationCLI;
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

    // ===== 共用容器,给各模块查资料用 =====
    private final ListInterface<Member> memberList;
    private final ListInterface<Room> roomList;
    private final HashTableInterface<Guest> guestTable;

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

        this.memberList = new ArrayBasedList<>();
        this.roomList = new ArrayBasedList<>();
        this.guestTable = new ChainingHashTable<>();
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
                    mainMenuCLI.displayModuleNotReady("Walk-In Registrations");
                    break;
                case 2:
                    runVipAllocationModule();
                    break;
                case 3:
                    mainMenuCLI.displayModuleNotReady("Housekeeping and Task Log");
                    break;
                case 4:
                    mainMenuCLI.displayModuleNotReady("Front-Desk Service");
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

    private void runVipAllocationModule() {
        VipAllocationCLI vipAllocationCLI = new VipAllocationCLI();
        VipAllocationControl vipAllocationControl = new VipAllocationControl(
                vipAllocationCLI, standardVipTree, deluxeVipTree, suiteVipTree,
                memberList, roomList, guestTable);
        vipAllocationControl.run();
    }
}

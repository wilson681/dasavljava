package control;

import adt.HashTableInterface;
import adt.ListInterface;
import adt.SearchTreeInterface;
import boundary.VipAllocationCLI;
import entity.Booking;
import entity.BookingStatus;
import entity.Guest;
import entity.Member;
import entity.Room;
import java.time.LocalDate;
import java.util.Iterator;
import utility.TierRankUtility;

/**
 * VipAllocationControl.java - 模块2(VIP & Loyalty Tier Priority Room Allocation)的业务逻辑。
 *
 * @author 某某
 *
 * 说明:
 * - 三棵 AVL Tree,按房型分开(Standard/Deluxe/Suite 各一棵),互相独立
 * - 分房时只处理VIP树,不会去管Walk-In队伍(模块1还没做,之后接上时VIP要永远优先)
 * - 只对"会调用collection ADT方法"的操作(登记、取消)做输入校验,查看名单这种不用
 */
public class VipAllocationControl {

    private final VipAllocationCLI vipAllocationCLI;
    private final SearchTreeInterface<Booking> standardTree;
    private final SearchTreeInterface<Booking> deluxeTree;
    private final SearchTreeInterface<Booking> suiteTree;
    private final ListInterface<Member> memberList;
    private final ListInterface<Room> roomList;
    private final HashTableInterface<Guest> guestTable;

    private int arrivalCounter;
    private int bookingCounter;
    private int confirmationCounter;

    public VipAllocationControl(VipAllocationCLI vipAllocationCLI,
                                 SearchTreeInterface<Booking> standardTree,
                                 SearchTreeInterface<Booking> deluxeTree,
                                 SearchTreeInterface<Booking> suiteTree,
                                 ListInterface<Member> memberList,
                                 ListInterface<Room> roomList,
                                 HashTableInterface<Guest> guestTable) {
        if (vipAllocationCLI == null || standardTree == null || deluxeTree == null
                || suiteTree == null || memberList == null || roomList == null || guestTable == null) {
            throw new IllegalArgumentException("VipAllocationControl dependencies cannot be null");
        }
        this.vipAllocationCLI = vipAllocationCLI;
        this.standardTree = standardTree;
        this.deluxeTree = deluxeTree;
        this.suiteTree = suiteTree;
        this.memberList = memberList;
        this.roomList = roomList;
        this.guestTable = guestTable;
        this.arrivalCounter = 0;
        this.bookingCounter = 0;
        this.confirmationCounter = 20000000;
    }

    /**
     * 跑这个模块自己的选单循环,直到使用者选择返回。
     */
    public void run() {
        boolean running = true;
        while (running) {
            int choice = vipAllocationCLI.displayMenuAndGetChoice();
            switch (choice) {
                case 1:
                    doRegister();
                    break;
                case 2:
                    doAllocate();
                    break;
                case 3:
                    doCancel();
                    break;
                case 4:
                    doViewWaitingList();
                    break;
                case 0:
                    running = false;
                    break;
                default:
                    vipAllocationCLI.displayInvalidChoice();
            }
        }
    }

    // ========== 功能1:VIP登记 ==========

    private void doRegister() {
        // 第一步:先查这个会员编号存不存在,不存在直接结束,不往下走
        String memberId = vipAllocationCLI.promptMemberId();
        Member member = findMemberById(memberId);
        if (member == null) {
            vipAllocationCLI.displayMemberNotFound(memberId);
            return;
        }

        // 第二步:问要什么房型,顺便决定这笔Booking该进哪一棵树
        String roomType = vipAllocationCLI.promptRoomType();
        SearchTreeInterface<Booking> tree = getTreeForRoomType(roomType);
        if (tree == null) {
            vipAllocationCLI.displayInvalidRoomType(roomType);
            return;
        }

        // 第三步:产生这笔Booking自己的编号跟客人的确认号码(各自累加,保证不重复)
        arrivalCounter++;
        bookingCounter++;
        confirmationCounter++;

        String bookingId = "VB" + String.format("%06d", bookingCounter);
        String confirmationNumber = String.valueOf(confirmationCounter);
        // 把会员等级(文字)换算成排名数字,插进树时靠这个数字决定优先级
        int tierRank = TierRankUtility.tierToRank(member.getTier());

        // 第四步:把这个人的姓名、电话、会员编号都从Member身上抄一份进Booking,
        // 因为这时候还没建Guest,这些资料要先存在Booking里,等真的分到房才拿出来用
        Booking booking = new Booking(bookingId, confirmationNumber, member.getName(),
                member.getPhone(), member.getMemberId(), roomType, BookingStatus.PENDING,
                "VIP", arrivalCounter, tierRank);

        // 第五步:插进对应房型的树——AVLTree.add()内部会自己比较tierRank/arrivalSequence,
        // 自动排到该在的位置,不用我们自己指定放哪
        tree.add(booking);
        vipAllocationCLI.displayRegistrationResult(booking, member.getTier());
    }

    // ========== 功能2:分房 ==========

    private void doAllocate() {
        // 第一步:决定要处理哪个房型的分房(这个模块目前只顾VIP树,不管Walk-In队伍)
        String roomType = vipAllocationCLI.promptRoomType();
        SearchTreeInterface<Booking> tree = getTreeForRoomType(roomType);
        if (tree == null) {
            vipAllocationCLI.displayInvalidRoomType(roomType);
            return;
        }

        // 树是空的,代表这个房型完全没人在排,不用往下做
        if (tree.isEmpty()) {
            vipAllocationCLI.displayNoOneWaiting(roomType);
            return;
        }

        // 第二步:拿"优先级最高"的那一笔——因为Booking.compareTo()把等级越高的值设得越小,
        // in-order遍历(由小到大)吐出来的第一个,天生就是优先级最高的那个,不用额外找
        Iterator<Booking> priorityIterator = tree.getInorderIterator();
        Booking topPriority = priorityIterator.next();

        // 第三步:去房间清单里找一间这个房型、状态刚好是AVAILABLE的空房
        Room availableRoom = findAvailableRoom(roomType);
        if (availableRoom == null) {
            vipAllocationCLI.displayNoRoomAvailable(roomType);
            return;
        }

        // 第四步:问住几晚,用来算入住/退房日期(给报表用,不是拿来判断能不能分房)
        int numberOfNights = vipAllocationCLI.promptNumberOfNights();

        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(numberOfNights);

        // 第五步:真正分房——改Booking状态、写入房号、改Room状态、把这笔Booking从树里移除
        // (它已经处理完了,不该继续留在"等待名单"里)
        topPriority.setStatus(BookingStatus.CHECKED_IN);
        topPriority.setAssignedRoomNo(availableRoom.getRoomNumber());
        availableRoom.setStatus("OCCUPIED");
        tree.remove(topPriority);

        // 第六步:这时候才真正建Guest(客人身份档案)——用Booking上存的姓名/电话/会员编号,
        // 组成完整的Guest物件,再塞进guestTable,前台(模块4)之后才查得到这个人
        Guest guest = new Guest(topPriority.getConfirmationNumber(), topPriority.getGuestNameSnapshot(),
                topPriority.getPhoneSnapshot(), topPriority.getMemberId(),
                findMemberById(topPriority.getMemberId()).getTier(),
                checkIn.toString() + " " + java.time.LocalTime.now().withNano(0).toString(),
                checkIn.toString(), checkOut.toString(), numberOfNights);
        guest.addRoom(availableRoom.getRoomNumber());
        guestTable.add(guest);

        vipAllocationCLI.displayAllocationResult(topPriority, availableRoom);
    }

    // ========== 功能3:取消排队 ==========

    private void doCancel() {
        String roomType = vipAllocationCLI.promptRoomType();
        SearchTreeInterface<Booking> tree = getTreeForRoomType(roomType);
        if (tree == null) {
            vipAllocationCLI.displayInvalidRoomType(roomType);
            return;
        }

        // 只有确认号码,不知道这笔Booking的tierRank/arrivalSequence是多少,
        // 没办法直接叫树去比大小导航——所以先用in-order扫过去,找到"真正的那个物件"
        String confirmationNumber = vipAllocationCLI.promptConfirmationNumberToCancel();
        Booking target = findBookingInTree(tree, confirmationNumber);
        if (target == null) {
            vipAllocationCLI.displayCancelResult(false);
            return;
        }

        // 找到真正的物件之后,拿它自己的tierRank/arrivalSequence去比较,
        // tree.remove()才能正确导航到它、做标准的AVL删除(必要时还会重新平衡)
        target.setStatus(BookingStatus.CANCELLED);
        tree.remove(target);
        vipAllocationCLI.displayCancelResult(true);
    }

    // ========== 功能4:查看VIP等待名单 ==========

    private void doViewWaitingList() {
        String roomType = vipAllocationCLI.promptRoomType();
        SearchTreeInterface<Booking> tree = getTreeForRoomType(roomType);
        if (tree == null) {
            vipAllocationCLI.displayInvalidRoomType(roomType);
            return;
        }

        // 直接把in-order遍历的iterator传给Boundary去印,不用自己先转成清单——
        // 因为compareTo设计的方向,这个顺序天生就是"优先级由高到低"
        vipAllocationCLI.displayWaitingList(roomType, tree.getInorderIterator());
    }

    // ========== 内部辅助方法 ==========

    /**
     * 依房型决定要操作哪一棵VIP树,房型不合法回传null。
     */
    private SearchTreeInterface<Booking> getTreeForRoomType(String roomType) {
        if (roomType == null) {
            return null;
        }
        switch (roomType.trim()) {
            case "Standard":
                return standardTree;
            case "Deluxe":
                return deluxeTree;
            case "Suite":
                return suiteTree;
            default:
                return null;
        }
    }

    /**
     * 在memberList里线性找出memberId相符的那位会员,找不到回传null。
     * (memberList本身只支持List的基本操作,没有直接"按ID查"的方法,
     *  所以Control自己拿iterator一个一个比对)
     */
    private Member findMemberById(String memberId) {
        if (memberId == null) {
            return null;
        }
        Iterator<Member> iterator = memberList.getIterator();
        while (iterator.hasNext()) {
            Member member = iterator.next();
            if (member.getMemberId().equals(memberId)) {
                return member;
            }
        }
        return null;
    }

    /**
     * 在roomList里找第一间"房型对得上、状态严格等于AVAILABLE"的房间。
     * 一定要用 equals("AVAILABLE"),不能只判断"不是OCCUPIED"——
     * 因为OUT_OF_ORDER的房间也"不是OCCUPIED",但不该被分配出去。
     */
    private Room findAvailableRoom(String roomType) {
        Iterator<Room> iterator = roomList.getIterator();
        while (iterator.hasNext()) {
            Room room = iterator.next();
            if (room.getRoomType().equals(roomType) && room.getStatus().equals("AVAILABLE")) {
                return room;
            }
        }
        return null;
    }

    /**
     * 在指定的树里,用confirmationNumber线性找出对应的Booking物件。
     * 找到的是"真正存在树里的那个物件"(不是重新拼凑出来的),
     * 这样才能拿去交给 tree.remove() 正确导航、删除。
     */
    private Booking findBookingInTree(SearchTreeInterface<Booking> tree, String confirmationNumber) {
        Iterator<Booking> iterator = tree.getInorderIterator();
        while (iterator.hasNext()) {
            Booking booking = iterator.next();
            if (booking.getConfirmationNumber().equals(confirmationNumber)) {
                return booking;
            }
        }
        return null;
    }
}

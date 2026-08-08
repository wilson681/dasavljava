package control;

import adt.HashTableInterface;
import adt.ListInterface;
import adt.QueueInterface;
import adt.SearchTreeInterface;
import boundary.WalkInCLI;
import entity.Booking;
import entity.BookingStatus;
import entity.Guest;
import entity.Room;
import java.time.LocalDate;
import java.util.Iterator;

/**
 * WalkInControl.java - 模块1(Walk-In Registrations & Standard Booking)的业务逻辑。
 *
 * @author 某某
 *
 * 说明:
 * - 三条 Circular Linked Queue,按房型分开(Standard/Deluxe/Suite 各一条),互相独立
 * - 分房时一定要先看VIP那边的树,VIP树只要有人在等,这个房型的Walk-In一律不能分房——
 *   这是两个模块共用的关键规则:VIP永远优先
 * - 只对"会调用collection ADT方法"的操作(登记、取消)做输入校验,查看名单这种不用
 */
public class WalkInControl {

    private final WalkInCLI walkInCLI;
    private final QueueInterface<Booking> standardQueue;
    private final QueueInterface<Booking> deluxeQueue;
    private final QueueInterface<Booking> suiteQueue;
    private final SearchTreeInterface<Booking> standardVipTree;
    private final SearchTreeInterface<Booking> deluxeVipTree;
    private final SearchTreeInterface<Booking> suiteVipTree;
    private final ListInterface<Room> roomList;
    private final HashTableInterface<Guest> guestTable;

    private int arrivalCounter;
    private int bookingCounter;
    private int confirmationCounter;

    public WalkInControl(WalkInCLI walkInCLI,
                          QueueInterface<Booking> standardQueue,
                          QueueInterface<Booking> deluxeQueue,
                          QueueInterface<Booking> suiteQueue,
                          SearchTreeInterface<Booking> standardVipTree,
                          SearchTreeInterface<Booking> deluxeVipTree,
                          SearchTreeInterface<Booking> suiteVipTree,
                          ListInterface<Room> roomList,
                          HashTableInterface<Guest> guestTable) {
        if (walkInCLI == null || standardQueue == null || deluxeQueue == null || suiteQueue == null
                || standardVipTree == null || deluxeVipTree == null || suiteVipTree == null
                || roomList == null || guestTable == null) {
            throw new IllegalArgumentException("WalkInControl dependencies cannot be null");
        }
        this.walkInCLI = walkInCLI;
        this.standardQueue = standardQueue;
        this.deluxeQueue = deluxeQueue;
        this.suiteQueue = suiteQueue;
        this.standardVipTree = standardVipTree;
        this.deluxeVipTree = deluxeVipTree;
        this.suiteVipTree = suiteVipTree;
        this.roomList = roomList;
        this.guestTable = guestTable;
        // 用20000000起跳当VIP的确认号,Walk-In从10000000起跳,避免两边号码重复
        this.arrivalCounter = 0;
        this.bookingCounter = 0;
        this.confirmationCounter = 10000000;
    }

    /**
     * 跑这个模块自己的选单循环,直到使用者选择返回。
     */
    public void run() {
        boolean running = true;
        while (running) {
            int choice = walkInCLI.displayMenuAndGetChoice();
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
                    walkInCLI.displayInvalidChoice();
            }
        }
    }

    // ========== 功能1:登记新客人 ==========

    private void doRegister() {
        // 散客不是会员,直接问姓名、电话,不用像VIP那样先查会员资料
        String name = walkInCLI.promptName();
        String phone = walkInCLI.promptPhone();

        String roomType = walkInCLI.promptRoomType();
        QueueInterface<Booking> queue = getQueueForRoomType(roomType);
        if (queue == null) {
            walkInCLI.displayInvalidRoomType(roomType);
            return;
        }

        arrivalCounter++;
        bookingCounter++;
        confirmationCounter++;

        String bookingId = "WB" + String.format("%06d", bookingCounter);
        String confirmationNumber = String.valueOf(confirmationCounter);

        // memberId是null、tierRank是0——散客没有等级,这两个字段跟VIP那边刻意留空/最低
        Booking booking = new Booking(bookingId, confirmationNumber, name, phone, null,
                roomType, BookingStatus.PENDING, "WALK_IN", arrivalCounter, 0);

        queue.enqueue(booking);
        walkInCLI.displayRegistrationResult(booking);
    }

    // ========== 功能2:分房 ==========

    private void doAllocate() {
        String roomType = walkInCLI.promptRoomType();
        QueueInterface<Booking> queue = getQueueForRoomType(roomType);
        SearchTreeInterface<Booking> vipTree = getVipTreeForRoomType(roomType);
        if (queue == null || vipTree == null) {
            walkInCLI.displayInvalidRoomType(roomType);
            return;
        }

        // 关键规则:VIP永远优先——只要这个房型的VIP树还有人在等,
        // Walk-In这边完全不动,不管Walk-In已经排了多久
        if (!vipTree.isEmpty()) {
            walkInCLI.displayVipHasPriority(roomType);
            return;
        }

        if (queue.isEmpty()) {
            walkInCLI.displayNoOneWaiting(roomType);
            return;
        }

        // 先peek队头,不要马上dequeue——要等确定真的有空房可以分,才正式把它从队伍拿掉
        Booking frontBooking = queue.getFront();

        Room availableRoom = findAvailableRoom(roomType);
        if (availableRoom == null) {
            walkInCLI.displayNoRoomAvailable(roomType);
            return;
        }

        int numberOfNights = walkInCLI.promptNumberOfNights();

        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(numberOfNights);

        frontBooking.setStatus(BookingStatus.CHECKED_IN);
        frontBooking.setAssignedRoomNo(availableRoom.getRoomNumber());
        availableRoom.setStatus("OCCUPIED");
        // 现在才真正把它从队伍拿掉,因为确定分房成功了
        queue.dequeue();

        Guest guest = new Guest(frontBooking.getConfirmationNumber(), frontBooking.getGuestNameSnapshot(),
                frontBooking.getPhoneSnapshot(), frontBooking.getMemberId(), "Standard",
                checkIn.toString() + " " + java.time.LocalTime.now().withNano(0).toString(),
                checkIn.toString(), checkOut.toString(), numberOfNights);
        guest.addRoom(availableRoom.getRoomNumber());
        guestTable.add(guest);

        walkInCLI.displayAllocationResult(frontBooking, availableRoom);
    }

    // ========== 功能3:取消排队 ==========

    private void doCancel() {
        String roomType = walkInCLI.promptRoomType();
        QueueInterface<Booking> queue = getQueueForRoomType(roomType);
        if (queue == null) {
            walkInCLI.displayInvalidRoomType(roomType);
            return;
        }

        String confirmationNumber = walkInCLI.promptConfirmationNumberToCancel();
        Booking target = findBookingInQueue(queue, confirmationNumber);
        if (target == null) {
            walkInCLI.displayCancelResult(false);
            return;
        }

        target.setStatus(BookingStatus.CANCELLED);
        // 普通enqueue/dequeue只能动队头/队尾,取消要用QueueInterface额外提供的remove()
        // 才能真正把队伍中间那一笔完全移除,不是只改状态
        queue.remove(target);
        walkInCLI.displayCancelResult(true);
    }

    // ========== 功能4:查看排队名单 ==========

    private void doViewWaitingList() {
        String roomType = walkInCLI.promptRoomType();
        QueueInterface<Booking> queue = getQueueForRoomType(roomType);
        if (queue == null) {
            walkInCLI.displayInvalidRoomType(roomType);
            return;
        }

        walkInCLI.displayWaitingList(roomType, queue.getIterator());
    }

    // ========== 内部辅助方法 ==========

    private QueueInterface<Booking> getQueueForRoomType(String roomType) {
        if (roomType == null) {
            return null;
        }
        switch (roomType.trim()) {
            case "Standard":
                return standardQueue;
            case "Deluxe":
                return deluxeQueue;
            case "Suite":
                return suiteQueue;
            default:
                return null;
        }
    }

    private SearchTreeInterface<Booking> getVipTreeForRoomType(String roomType) {
        if (roomType == null) {
            return null;
        }
        switch (roomType.trim()) {
            case "Standard":
                return standardVipTree;
            case "Deluxe":
                return deluxeVipTree;
            case "Suite":
                return suiteVipTree;
            default:
                return null;
        }
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
     * 在指定的队伍里,用confirmationNumber线性找出对应的Booking物件。
     * 找到的是"真正存在队伍里的那个物件",这样才能拿去交给 queue.remove() 正确比对、移除。
     */
    private Booking findBookingInQueue(QueueInterface<Booking> queue, String confirmationNumber) {
        Iterator<Booking> iterator = queue.getIterator();
        while (iterator.hasNext()) {
            Booking booking = iterator.next();
            if (booking.getConfirmationNumber().equals(confirmationNumber)) {
                return booking;
            }
        }
        return null;
    }
}

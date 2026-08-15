package control;

import adt.ArrayBasedList;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import utility.TierRankUtility;
import utility.ValidationUtility;

/**
 * VipAllocationControl.java - 模块2(VIP & Loyalty Tier Priority Room Allocation)的业务逻辑。
 *
 * @author 某某
 *
 * 说明:
 * - 三棵 AVL Tree,按房型分开(Standard/Deluxe/Suite 各一棵),互相独立
 * - 分房时只处理VIP树,不会去管Walk-In队伍(VIP树非空时,WalkInControl自己会挡住不分)
 * - 只对"会调用collection ADT方法"的操作(登记、取消)做输入校验,查看名单这种不用
 * - 分房不再是一个手动菜单动作,改成 tryAllocate() 这个检查:登记完当下会自动跑一次,
 *   之后房间从不可用变可用时(退房/清洁完成)也该跑一次——那个触发点现在还没有人会调用它
 *   (housekeeping/checkout都还没做),先把方法留成public,等那边接进来直接调用即可
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
                    doCancel();
                    break;
                case 3:
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
        if (ValidationUtility.isBlank(memberId)) {
            vipAllocationCLI.displayMemberNotFound(memberId);
            return;
        }
        Member member = findMemberById(memberId);
        if (member == null) {
            vipAllocationCLI.displayMemberNotFound(memberId);
            return;
        }

        // 把会员等级(文字)换算成排名数字,插进树时靠这个数字决定优先级——
        // 同一位会员这次登记的每一笔Booking等级都一样,只需要算一次
        int tierRank = TierRankUtility.tierToRank(member.getTier());

        // tierRank<=0代表这不是真正的VIP等级(比如Walk-In入住后自动开的Standard会员)——
        // 这种人虽然有memberId,但不该走VIP这条路登记。放行的话,他的Booking会被插进
        // VIP树,树一非空就会挡住WalkInControl.tryAllocate()对这个房型的所有Walk-In
        // 分房(那边的判断只看树是不是空的,不看里面tierRank多少),等于让一个不该有
        // VIP优先权的人变相卡住所有Walk-In——所以要在这里直接拦下来,请他去Walk-In登记
        if (tierRank <= 0) {
            vipAllocationCLI.displayNotVip(memberId, member.getTier());
            return;
        }

        // 这位VIP可能一次要订好几间房(不一定同房型),所以会员资料只查一次,
        // 底下用同一个confirmationNumber循环开多笔Booking,直到不用再加了
        confirmationCounter++;
        String confirmationNumber = String.valueOf(confirmationCounter);

        boolean continueBooking = true;
        while (continueBooking) {
            // 问要什么房型,顺便决定这笔Booking该进哪一棵树
            String roomType = vipAllocationCLI.promptRoomType();
            SearchTreeInterface<Booking> tree = getTreeForRoomType(roomType);
            if (tree == null) {
                vipAllocationCLI.displayInvalidRoomType(roomType);
            } else {
                // 住几晚要在客人还在面前的登记当下先问好,存进Booking——
                // 分房不再保证是当场发生的,之后可能是房间空出来才自动触发,
                // 到时候客人不一定还在,没办法临时问
                int numberOfNights = vipAllocationCLI.promptNumberOfNights();
                if (numberOfNights <= 0) {
                    vipAllocationCLI.displayInvalidNumberOfNights(numberOfNights);
                } else {
                    arrivalCounter++;
                    bookingCounter++;
                    String bookingId = "VB" + String.format("%06d", bookingCounter);

                    // 把这个人的姓名、电话、会员编号都从Member身上抄一份进Booking,
                    // 因为这时候还没建Guest,这些资料要先存在Booking里,等真的分到房才拿出来用
                    Booking booking = new Booking(bookingId, confirmationNumber, member.getName(),
                            member.getPhone(), member.getMemberId(), roomType, BookingStatus.PENDING,
                            "VIP", arrivalCounter, tierRank, currentTimestamp());
                    booking.setNumberOfNights(numberOfNights);

                    // 插进对应房型的树——AVLTree.add()内部会自己比较tierRank/arrivalSequence,
                    // 自动排到该在的位置,不用我们自己指定放哪
                    tree.add(booking);
                    vipAllocationCLI.displayRegistrationResult(booking, member.getTier());

                    // 登记完立刻检查一次这个房型能不能马上分房(树里排最前面的那笔、有空房)
                    tryAllocate(roomType);
                }
            }
            continueBooking = vipAllocationCLI.promptAddAnotherRoom();
        }
    }

    // ========== 分房检查(不再是手动菜单动作) ==========

    /**
     * 检查指定房型现在能不能分房,能就把树里优先级最高的那笔Booking分掉。
     * 两个触发时机:1) doRegister() 登记完当下跑一次;2) 之后房间从不可用变可用时
     * (退房/清洁完成,housekeeping/checkout模块接上后)也该跑一次——目前还没有人调用
     * 第2种情况,方法先留成public,等那边接进来直接调用。
     * 什么都不满足就静静地什么都不做(客人留在树里继续等),不当错误处理。
     */
    public void tryAllocate(String roomType) {
        SearchTreeInterface<Booking> tree = getTreeForRoomType(roomType);
        if (tree == null || tree.isEmpty()) {
            return;
        }

        Room availableRoom = findAvailableRoom(roomType);
        if (availableRoom == null) {
            return;
        }

        // 拿"优先级最高"的那一笔——因为Booking.compareTo()把等级越高的值设得越小,
        // in-order遍历(由小到大)吐出来的第一个,天生就是优先级最高的那个,不用额外找
        Iterator<Booking> priorityIterator = tree.getInorderIterator();
        Booking topPriority = priorityIterator.next();

        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(topPriority.getNumberOfNights());

        // 真正分房——改Booking状态、写入房号、改Room状态、把这笔Booking从树里移除
        // (它已经处理完了,不该继续留在"等待名单"里)
        topPriority.setStatus(BookingStatus.CHECKED_IN);
        topPriority.setAssignedRoomNo(availableRoom.getRoomNumber());
        topPriority.setAllocatedAt(currentTimestamp());
        availableRoom.setStatus("OCCUPIED");
        tree.remove(topPriority);

        // 这时候才真正建Guest(客人身份档案)——用Booking上存的姓名/电话/会员编号,
        // 组成完整的Guest物件,再塞进guestTable,前台(模块4)之后才查得到这个人。
        // 但如果这位VIP已经因为前一间房分房成功、guestTable里早就有他的Guest记录了,
        // 就不能再new一个塞进去(那样guestTable里会出现两个confirmationNumber相同的
        // Guest,查找时后者会盖住前者),而是把这间新房加进原本那个Guest的bookedRooms
        Member member = findMemberById(topPriority.getMemberId());
        Guest guest = findGuestByConfirmationNumber(topPriority.getConfirmationNumber());
        if (guest == null) {
            guest = new Guest(topPriority.getConfirmationNumber(), topPriority.getGuestNameSnapshot(),
                    topPriority.getPhoneSnapshot(), topPriority.getMemberId(), member.getTier(),
                    checkIn.toString() + " " + java.time.LocalTime.now().withNano(0).toString(),
                    checkIn.toString(), checkOut.toString(), topPriority.getNumberOfNights());
            guestTable.add(guest);
        }
        guest.addRoom(availableRoom.getRoomNumber());

        // Record the stay period on the booking itself and link it to the guest,
        // so the Front-Desk module can list every booking under one
        // confirmation number with its own dates.
        topPriority.setStayPeriod(checkIn.toString(), checkOut.toString(), topPriority.getNumberOfNights());
        guest.addBooking(topPriority);

        // 分房那一刻就先给客人看一下预估房费(等级折扣是"个性化促销"的一种,只影响
        // 价格显示,不碰房型/房间状态)——正式结算金额还是要等退房才真正定案,
        // 这里只是让客人提前知道大概要付多少
        double originalPrice = availableRoom.getNightlyRate() * topPriority.getNumberOfNights();
        int discountPercent = TierRankUtility.tierToDiscountPercent(member.getTier());
        double finalPrice = originalPrice - (originalPrice * discountPercent / 100.0);

        vipAllocationCLI.displayAllocationResult(topPriority, availableRoom,
                originalPrice, discountPercent, finalPrice);
    }

    // ========== 功能3:取消排队 ==========

    private void doCancel() {
        String roomType = vipAllocationCLI.promptRoomType();
        SearchTreeInterface<Booking> tree = getTreeForRoomType(roomType);
        if (tree == null) {
            vipAllocationCLI.displayInvalidRoomType(roomType);
            return;
        }

        // 用bookingId取消,不是confirmationNumber——同一个confirmationNumber可能同时
        // 有好几笔Booking在同一个房型的树里(一次订多间房),用confirmationNumber去找
        // 会有歧义,没办法让客人指定要取消的到底是哪一笔;bookingId每笔都是唯一的
        // 只有bookingId,不知道这笔Booking的tierRank/arrivalSequence是多少,
        // 没办法直接叫树去比大小导航——所以先用in-order扫过去,找到"真正的那个物件"
        String bookingId = vipAllocationCLI.promptBookingIdToCancel();
        if (ValidationUtility.isBlank(bookingId)) {
            vipAllocationCLI.displayInvalidBookingId(bookingId);
            return;
        }
        Booking target = findBookingInTree(tree, bookingId);
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

    // ========== 报表1:VIP等待名单实时报表 ==========

    /**
     * filter=等级,资料只取"现在还在树里"的(真正的实时等待名单)。排序刻意不用
     * 树本身的中序顺序(那是tierRank+arrivalSequence),改成按"已经等了多久"降序——
     * 这样报表层才是自己在做排序,不是单纯把树的既有顺序吐出来。
     */
    void doVipWaitingListReport() {
        int tierRankFilter = vipAllocationCLI.promptReportTierRank();

        ListInterface<Booking> filtered = new ArrayBasedList<>();
        ListInterface<Integer> waitMinutesList = new ArrayBasedList<>();

        collectTierFiltered(standardTree, tierRankFilter, filtered, waitMinutesList);
        collectTierFiltered(deluxeTree, tierRankFilter, filtered, waitMinutesList);
        collectTierFiltered(suiteTree, tierRankFilter, filtered, waitMinutesList);

        sortByWaitMinutesDescending(filtered, waitMinutesList);

        vipAllocationCLI.displayVipWaitingListReportHeader(tierRankFilter);
        if (filtered.isEmpty()) {
            vipAllocationCLI.displayNoReportRecords();
        } else {
            for (int i = 1; i <= filtered.getNumberOfEntries(); i++) {
                Booking booking = filtered.getEntry(i);
                vipAllocationCLI.displayVipWaitingListReportRow(booking.getGuestNameSnapshot(),
                        TierRankUtility.rankToTier(booking.getTierRankAtRequest()),
                        booking.getRegisteredAt(), waitMinutesList.getEntry(i));
            }
        }
        vipAllocationCLI.displayReportEnd();
    }

    private void collectTierFiltered(SearchTreeInterface<Booking> tree, int tierRankFilter,
                                      ListInterface<Booking> filtered, ListInterface<Integer> waitMinutesList) {
        Iterator<Booking> iterator = tree.getInorderIterator();
        while (iterator.hasNext()) {
            Booking booking = iterator.next();
            if (tierRankFilter == 0 || booking.getTierRankAtRequest() == tierRankFilter) {
                filtered.add(booking);
                waitMinutesList.add(minutesBetween(booking.getRegisteredAt(), currentTimestamp()));
            }
        }
    }

    // ========== 报表2:等级分房达标率报表 ==========

    /**
     * filter=等级+日期区间(按分房那天算),把"登记到分房"的平均耗时按等级分组比较,
     * 用来验证"VIP优先"这个承诺是不是真的兑现——如果高等级反而等更久,代表流程有问题。
     */
    void doTierSlaReport() {
        int tierRankFilter = vipAllocationCLI.promptReportTierRank();
        String fromDate = vipAllocationCLI.promptReportFromDate();
        String toDate = vipAllocationCLI.promptReportToDate();

        // 固定3档(Diamond=3, Platinum=2, Elite=1),索引0不用
        int[] count = new int[4];
        int[] totalWaitMinutes = new int[4];

        Iterator<Guest> guestIterator = guestTable.getIterator();
        while (guestIterator.hasNext()) {
            Guest guest = guestIterator.next();
            Iterator<Booking> bookingIterator = guest.getBookings().getIterator();
            while (bookingIterator.hasNext()) {
                Booking booking = bookingIterator.next();
                if (!"VIP".equals(booking.getSource()) || booking.getAllocatedAt() == null) {
                    continue;
                }
                int rank = booking.getTierRankAtRequest();
                if (rank < 1 || rank > 3) {
                    continue;
                }
                if (tierRankFilter != 0 && rank != tierRankFilter) {
                    continue;
                }
                String allocatedDate = booking.getAllocatedAt().substring(0, 10);
                if (allocatedDate.compareTo(fromDate) < 0 || allocatedDate.compareTo(toDate) > 0) {
                    continue;
                }
                count[rank]++;
                totalWaitMinutes[rank] += minutesBetween(booking.getRegisteredAt(), booking.getAllocatedAt());
            }
        }

        // 组成最多3行(有资料的等级才列),再按平均等待时长降序排(selection sort)
        ListInterface<String> tierNames = new ArrayBasedList<>();
        ListInterface<Integer> tierCounts = new ArrayBasedList<>();
        ListInterface<Double> tierAverages = new ArrayBasedList<>();
        for (int rank = 3; rank >= 1; rank--) {
            if (count[rank] > 0) {
                tierNames.add(TierRankUtility.rankToTier(rank));
                tierCounts.add(count[rank]);
                tierAverages.add(totalWaitMinutes[rank] / (double) count[rank]);
            }
        }
        sortTierSummaryByAverageDescending(tierNames, tierCounts, tierAverages);

        vipAllocationCLI.displayTierSlaReportHeader(tierRankFilter, fromDate, toDate);
        if (tierNames.isEmpty()) {
            vipAllocationCLI.displayNoReportRecords();
        } else {
            for (int i = 1; i <= tierNames.getNumberOfEntries(); i++) {
                vipAllocationCLI.displayTierSlaReportRow(tierNames.getEntry(i),
                        tierCounts.getEntry(i), tierAverages.getEntry(i));
            }
        }
        vipAllocationCLI.displayReportEnd();
    }

    private void sortTierSummaryByAverageDescending(ListInterface<String> tierNames,
                                                      ListInterface<Integer> tierCounts,
                                                      ListInterface<Double> tierAverages) {
        int n = tierNames.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int largestPosition = i;
            for (int j = i + 1; j <= n; j++) {
                if (tierAverages.getEntry(j) > tierAverages.getEntry(largestPosition)) {
                    largestPosition = j;
                }
            }
            if (largestPosition != i) {
                String tempName = tierNames.getEntry(i);
                tierNames.replace(i, tierNames.getEntry(largestPosition));
                tierNames.replace(largestPosition, tempName);

                Integer tempCount = tierCounts.getEntry(i);
                tierCounts.replace(i, tierCounts.getEntry(largestPosition));
                tierCounts.replace(largestPosition, tempCount);

                Double tempAverage = tierAverages.getEntry(i);
                tierAverages.replace(i, tierAverages.getEntry(largestPosition));
                tierAverages.replace(largestPosition, tempAverage);
            }
        }
    }

    // ========== 报表共用辅助方法 ==========

    private int minutesBetween(String start, String end) {
        LocalDateTime startTime = LocalDateTime.parse(start, TIMESTAMP_FORMAT);
        LocalDateTime endTime = LocalDateTime.parse(end, TIMESTAMP_FORMAT);
        return (int) java.time.Duration.between(startTime, endTime).toMinutes();
    }

    private void sortByWaitMinutesDescending(ListInterface<Booking> bookings, ListInterface<Integer> waitMinutes) {
        int n = bookings.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int largestPosition = i;
            for (int j = i + 1; j <= n; j++) {
                if (waitMinutes.getEntry(j) > waitMinutes.getEntry(largestPosition)) {
                    largestPosition = j;
                }
            }
            if (largestPosition != i) {
                Booking tempBooking = bookings.getEntry(i);
                bookings.replace(i, bookings.getEntry(largestPosition));
                bookings.replace(largestPosition, tempBooking);

                Integer tempMinutes = waitMinutes.getEntry(i);
                waitMinutes.replace(i, waitMinutes.getEntry(largestPosition));
                waitMinutes.replace(largestPosition, tempMinutes);
            }
        }
    }

    // ========== 内部辅助方法 ==========

    // 报表要严格照这个格式 parse 时间字串算等待分钟数,所以固定长度格式,
    // 不能用 LocalTime.toString()(秒数刚好整数时会省略,长度不固定)
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 组一个"yyyy-MM-dd HH:mm:ss"格式的当下时间字串,给 Booking 的 registeredAt/
     * allocatedAt 用,也给报表算等待分钟数用。
     */
    private String currentTimestamp() {
        return LocalDateTime.now().withNano(0).format(TIMESTAMP_FORMAT);
    }

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
     * 因为NEEDS_CLEANING/CLEANING_IN_PROGRESS/INSPECTED这些房间也"不是OCCUPIED",
     * 但清洁流程还没走完,不该被分配出去。
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
     * 用confirmationNumber去guestTable里查这位客人是否已经有Guest记录
     * (比如已经因为另一间房分房成功而建过了)。
     * Guest的equals()/hashCode()只看confirmationNumber,所以拿一个
     * 只填了confirmationNumber、其他栏位留null的样板去查,一样能正确命中。
     */
    private Guest findGuestByConfirmationNumber(String confirmationNumber) {
        Guest template = new Guest(confirmationNumber, null, null, null, null, null, null, null, 0);
        return guestTable.getEntry(template);
    }

    /**
     * 在指定的树里,用bookingId线性找出对应的Booking物件。
     * 找到的是"真正存在树里的那个物件"(不是重新拼凑出来的),
     * 这样才能拿去交给 tree.remove() 正确导航、删除。
     */
    private Booking findBookingInTree(SearchTreeInterface<Booking> tree, String bookingId) {
        Iterator<Booking> iterator = tree.getInorderIterator();
        while (iterator.hasNext()) {
            Booking booking = iterator.next();
            if (booking.getBookingId().equals(bookingId)) {
                return booking;
            }
        }
        return null;
    }
}

package control;

import adt.ArrayBasedList;
import adt.HashTableInterface;
import adt.ListInterface;
import adt.QueueInterface;
import adt.SearchTreeInterface;
import boundary.WalkInCLI;
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
 * WalkInControl.java - 模块1(Walk-In Registrations & Standard Booking)的业务逻辑。
 *
 * @author 某某
 *
 * 说明:
 * - 三条 Circular Linked Queue,按房型分开(Standard/Deluxe/Suite 各一条),互相独立
 * - 分房时一定要先看VIP那边的树,VIP树只要有人在等,这个房型的Walk-In一律不能分房——
 *   这是两个模块共用的关键规则:VIP永远优先
 * - 只对"会调用collection ADT方法"的操作(登记、取消)做输入校验,查看名单这种不用
 * - 分房不再是一个手动菜单动作,改成 tryAllocate() 这个检查:登记完当下会自动跑一次,
 *   之后房间从不可用变可用时(退房/清洁完成)也该跑一次——那个触发点现在还没有人会调用它
 *   (housekeeping/checkout都还没做),先把方法留成public,等那边接进来直接调用即可
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
    private final ListInterface<Member> memberList;

    private int arrivalCounter;
    private int bookingCounter;
    private int confirmationCounter;
    private int memberCounter;

    public WalkInControl(WalkInCLI walkInCLI,
                          QueueInterface<Booking> standardQueue,
                          QueueInterface<Booking> deluxeQueue,
                          QueueInterface<Booking> suiteQueue,
                          SearchTreeInterface<Booking> standardVipTree,
                          SearchTreeInterface<Booking> deluxeVipTree,
                          SearchTreeInterface<Booking> suiteVipTree,
                          ListInterface<Room> roomList,
                          HashTableInterface<Guest> guestTable,
                          ListInterface<Member> memberList) {
        if (walkInCLI == null || standardQueue == null || deluxeQueue == null || suiteQueue == null
                || standardVipTree == null || deluxeVipTree == null || suiteVipTree == null
                || roomList == null || guestTable == null || memberList == null) {
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
        this.memberList = memberList;
        // 用20000000起跳当VIP的确认号,Walk-In从10000000起跳,避免两边号码重复
        this.arrivalCounter = 0;
        this.bookingCounter = 0;
        this.confirmationCounter = 10000000;
        // 扫一次种子会员资料(MemberDao已经在这之前把它们读进memberList了),
        // 接着种子资料的编号继续往下用,格式才会跟M1001这些一致——这个计数器从此只有
        // WalkInControl自己在用,不会再重新扫描,所以不会有两边各自算出同一个号码的风险
        this.memberCounter = computeNextMemberNumber();
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
                    doCancel();
                    break;
                case 3:
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
        // 散客不是会员,直接问姓名、电话,不用像VIP那样先查会员资料——
        // 格式错误(电话非数字)原地重问到对为止;空白代表使用者要取消,直接放弃整个登记
        String name = promptValidName();
        if (name == null) {
            walkInCLI.displayCancelled();
            return;
        }
        String phone = promptValidPhone();
        if (phone == null) {
            walkInCLI.displayCancelled();
            return;
        }

        // 一位客人可能一次要订好几间房(不一定同房型),所以姓名/电话只问一次,
        // 底下用同一个confirmationNumber循环开多笔Booking,直到客人说不用再加了
        confirmationCounter++;
        String confirmationNumber = String.valueOf(confirmationCounter);

        boolean continueBooking = true;
        while (continueBooking) {
            String roomType = promptValidRoomType();
            if (roomType == null) {
                // 房型这里留空白,等同"不用再加房间了",直接结束这一轮登记,
                // 不用额外印取消讯息——跟平常按"add another room? n"退出是一样的效果
                break;
            }
            QueueInterface<Booking> queue = getQueueForRoomType(roomType);

            // 住几晚要在客人还在面前的登记当下先问好,存进Booking——
            // 分房不再保证是当场发生的,之后可能是房间空出来才自动触发,
            // 到时候客人不一定还在,没办法临时问
            int numberOfNights = promptValidNumberOfNights();
            if (numberOfNights == Integer.MIN_VALUE) {
                break;
            }

            arrivalCounter++;
            bookingCounter++;
            String bookingId = "WB" + String.format("%06d", bookingCounter);

            // memberId是null、tierRank是0——散客没有等级,这两个字段跟VIP那边刻意留空/最低
            Booking booking = new Booking(bookingId, confirmationNumber, name, phone, null,
                    roomType, BookingStatus.PENDING, "WALK_IN", arrivalCounter, 0, currentTimestamp());
            booking.setNumberOfNights(numberOfNights);

            queue.enqueue(booking);
            walkInCLI.displayRegistrationResult(booking);

            // 登记完立刻检查一次这个房型能不能马上分房(VIP没人等、队伍轮到他、有空房)
            tryAllocate(roomType);

            continueBooking = walkInCLI.promptAddAnotherRoom();
        }
    }

    // ========== 分房检查(不再是手动菜单动作) ==========

    /**
     * 检查指定房型现在能不能分房,能就把队头那笔Booking分掉。
     * 两个触发时机:1) doRegister() 登记完当下跑一次;2) 之后房间从不可用变可用时
     * (退房/清洁完成,housekeeping/checkout模块接上后)也该跑一次——目前还没有人调用
     * 第2种情况,方法先留成public,等那边接进来直接调用。
     * 什么都不满足就静静地什么都不做(客人留在队伍里继续等),不当错误处理。
     */
    public void tryAllocate(String roomType) {
        QueueInterface<Booking> queue = getQueueForRoomType(roomType);
        SearchTreeInterface<Booking> vipTree = getVipTreeForRoomType(roomType);
        if (queue == null || vipTree == null) {
            return;
        }

        // 关键规则:VIP永远优先——只要这个房型的VIP树还有人在等,
        // Walk-In这边完全不动,不管Walk-In已经排了多久
        if (!vipTree.isEmpty()) {
            return;
        }

        if (queue.isEmpty()) {
            return;
        }

        Room availableRoom = findAvailableRoom(roomType);
        if (availableRoom == null) {
            return;
        }

        // 先peek队头,不要马上dequeue——要等确定真的有空房可以分,才正式把它从队伍拿掉
        Booking frontBooking = queue.getFront();

        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(frontBooking.getNumberOfNights());

        frontBooking.setStatus(BookingStatus.CHECKED_IN);
        frontBooking.setAssignedRoomNo(availableRoom.getRoomNumber());
        frontBooking.setAllocatedAt(currentTimestamp());
        availableRoom.setStatus("OCCUPIED");
        // 现在才真正把它从队伍拿掉,因为确定分房成功了
        queue.dequeue();

        // 同一位客人(同一个confirmationNumber)可能早就因为前一间房分房成功,
        // 已经在guestTable里有Guest记录了——这时候不能再new一个塞进去(那样guestTable
        // 里会出现两个confirmationNumber相同的Guest,查找时后者会盖住前者),
        // 而是要把这间新房加进原本那个Guest的bookedRooms
        Guest guest = findGuestByConfirmationNumber(frontBooking.getConfirmationNumber());
        if (guest == null) {
            // Walk-In一走进这个分支,就代表他是"这辈子第一次被系统看到"的人(还没有
            // memberId)——一旦真的入住(不是还在排队),就自动帮他开一个最低等级的
            // Member。tier用"Standard",TierRankUtility.tierToRank()对认不得的tier
            // 一律回传0,所以不会因为多了memberId就意外插进VIP优先级队伍。
            // 下次这个人再来,理论上会自称会员、直接走VIP模块用memberId登记,
            // 不会再经过这里,所以这里不用查重。
            Member newMember = enrollAsStandardMember(frontBooking.getGuestNameSnapshot(),
                    frontBooking.getPhoneSnapshot());

            guest = new Guest(frontBooking.getConfirmationNumber(), frontBooking.getGuestNameSnapshot(),
                    frontBooking.getPhoneSnapshot(), newMember.getMemberId(), newMember.getTier(),
                    checkIn.toString() + " " + java.time.LocalTime.now().withNano(0).toString(),
                    checkIn.toString(), checkOut.toString(), frontBooking.getNumberOfNights());
            guestTable.add(guest);
        }
        guest.addRoom(availableRoom.getRoomNumber());

        // Record the stay period on the booking itself and link it to the guest,
        // so the Front-Desk module can list every booking under one
        // confirmation number with its own dates.
        frontBooking.setStayPeriod(checkIn.toString(), checkOut.toString(), frontBooking.getNumberOfNights());
        guest.addBooking(frontBooking);

        // 分房那一刻就先给客人看一下预估房费(等级折扣是"个性化促销"的一种,只影响
        // 价格显示,不碰房型/房间状态)——Walk-In新客人tier固定是Standard(0%折扣),
        // 正式结算金额还是要等退房才真正定案
        double originalPrice = availableRoom.getNightlyRate() * frontBooking.getNumberOfNights();
        int discountPercent = TierRankUtility.tierToDiscountPercent(guest.getTier());
        double finalPrice = originalPrice - (originalPrice * discountPercent / 100.0);

        walkInCLI.displayAllocationResult(frontBooking, availableRoom, originalPrice, discountPercent, finalPrice);
    }

    // ========== 功能3:取消排队 ==========

    private void doCancel() {
        // 先选房型、把这个房型的等待名单印出来,让使用者看清楚队伍里到底有什么
        // (含bookingId)再决定要取消哪一笔,不用盲打——队伍是空的话,印完"没人在等"
        // 的提示后直接回头重问房型,不会继续往下问一个注定查不到的bookingId
        QueueInterface<Booking> queue;
        String roomType;
        while (true) {
            roomType = promptValidRoomType();
            if (roomType == null) {
                walkInCLI.displayCancelled();
                return;
            }
            queue = getQueueForRoomType(roomType);
            walkInCLI.displayWaitingList(roomType, queue.getIterator());
            if (!queue.isEmpty()) {
                break;
            }
        }

        // 用bookingId取消,不是confirmationNumber——同一个confirmationNumber可能同时
        // 有好几笔Booking在同一个房型的队伍里(一次订多间房),用confirmationNumber去找
        // 只会抓到排最前面那一笔,没办法让客人指定要取消的到底是哪一间;bookingId每笔
        // 都是唯一的,不会有这个歧义
        String bookingId = promptValidBookingId();
        if (bookingId == null) {
            walkInCLI.displayCancelled();
            return;
        }
        Booking target = findBookingInQueue(queue, bookingId);
        if (target == null) {
            walkInCLI.displayCancelResult(false);
            return;
        }

        target.setStatus(BookingStatus.CANCELLED);
        // 普通enqueue/dequeue只能动队头/队尾,取消要用QueueInterface额外写的remove()
        // 才能真正把队伍中间那一笔完全移除,不是只改状态
        queue.remove(target);
        walkInCLI.displayCancelResult(true);
    }

    // ========== 功能4:查看排队名单 ==========

    private void doViewWaitingList() {
        String roomType = promptValidRoomType();
        if (roomType == null) {
            walkInCLI.displayCancelled();
            return;
        }
        QueueInterface<Booking> queue = getQueueForRoomType(roomType);
        walkInCLI.displayWaitingList(roomType, queue.getIterator());
    }

    // ========== 报表1:每日入住登记明细表 ==========

    /**
     * filter=日期+房型,按登记时间由早到晚(selection sort)。
     *
     * 这份报表只讲"登记"这件事:谁来了、要什么房型、拿到没有、拿到哪一间。
     * 所有跟"等多久"有关的统计都归报表2(Wait Time Analysis)——两份用的是同一批
     * 资料,分工不划清楚就会变成两份讲同一件事。
     *
     * 资料来源是"现在还在排队的" + "已经分房、挂在guestTable底下客人身上的"两边
     * 合起来,这样不管有没有分到房都看得到。
     */
    void doDailyRegistrationReport() {
        String dateFilter = walkInCLI.promptReportDate();
        String roomTypeFilter = walkInCLI.promptReportRoomType();

        ListInterface<Booking> filtered = new ArrayBasedList<>();
        // 房型不是ALL时,collectWalkInBookings()只会去对应那一条队伍拿,另外两条完全不碰
        ListInterface<Booking> allBookings = collectWalkInBookings(roomTypeFilter);
        for (int i = 1; i <= allBookings.getNumberOfEntries(); i++) {
            Booking booking = allBookings.getEntry(i);
            boolean dateMatches = "ALL".equals(dateFilter) || booking.getRegisteredAt().startsWith(dateFilter);
            boolean typeMatches = "ALL".equalsIgnoreCase(roomTypeFilter)
                    || booking.getRequestedRoomType().equalsIgnoreCase(roomTypeFilter);
            if (dateMatches && typeMatches) {
                filtered.add(booking);
            }
        }
        sortBookingsByRegisteredAt(filtered);

        walkInCLI.displayDailyRegistrationReportHeader(dateFilter, roomTypeFilter);
        if (filtered.isEmpty()) {
            walkInCLI.displayNoReportRecords();
            walkInCLI.displayReportEnd();
            return;
        }

        // 一边印明细一边累加,不用为了统计再多扫一遍。
        // 每一型房要分开数"要了几间"和"拿到几间",差额就是没被满足的需求。
        int allocatedCount = 0;
        int standardCount = 0;
        int deluxeCount = 0;
        int suiteCount = 0;
        int standardAllocated = 0;
        int deluxeAllocated = 0;
        int suiteAllocated = 0;

        for (int i = 1; i <= filtered.getNumberOfEntries(); i++) {
            Booking booking = filtered.getEntry(i);
            boolean allocated = booking.getAssignedRoomNo() != null;

            if (allocated) {
                allocatedCount++;
            }

            if ("Deluxe".equalsIgnoreCase(booking.getRequestedRoomType())) {
                deluxeCount++;
                if (allocated) {
                    deluxeAllocated++;
                }
            } else if ("Suite".equalsIgnoreCase(booking.getRequestedRoomType())) {
                suiteCount++;
                if (allocated) {
                    suiteAllocated++;
                }
            } else {
                standardCount++;
                if (allocated) {
                    standardAllocated++;
                }
            }

            // registeredAt 是 "yyyy-MM-dd HH:mm:ss",这里只取日期——
            // 时分秒是报表2按小时分析用的,这份报表显示到分秒等于踩过去
            walkInCLI.displayDailyRegistrationReportRow(
                    booking.getGuestNameSnapshot(),
                    booking.getRegisteredAt().substring(0, 10),
                    booking.getRequestedRoomType(),
                    allocated,
                    allocated ? booking.getAssignedRoomNo() : "-");
        }

        int total = filtered.getNumberOfEntries();
        walkInCLI.displayDailyRegistrationSummary(total, allocatedCount, total - allocatedCount);
        walkInCLI.displayDemandByRoomType(standardCount, standardAllocated,
                deluxeCount, deluxeAllocated, suiteCount, suiteAllocated);
        walkInCLI.displayReportEnd();
    }

    // ========== 报表2:等待时长分析报表 ==========

    /**
     * filter=日期+房型,按等待时长降序排序。
     *
     * 刻意不做"最少等待分钟"筛选:那会让 Average wait 变成"等超过门槛的人的平均",
     * 一份叫 Wait Time Analysis 的报表,平均值不该因为筛选条件而失真。
     * 换成房型之后,统计不会被截断,而且多回答一个问题:哪一型房的客人等最久。
     */
    void doWaitTimeAnalysisReport() {
        String dateFilter = walkInCLI.promptReportDate();
        String roomTypeFilter = walkInCLI.promptReportRoomType();

        ListInterface<Booking> filtered = new ArrayBasedList<>();
        ListInterface<Integer> waitMinutesList = new ArrayBasedList<>();

        int[] hourlyCount = new int[24];
        int[] hourlyTotalWait = new int[24];

        // 按房型的等待时长:三种房型固定三组,索引 0=Standard 1=Deluxe 2=Suite
        int[] typeAllocated = new int[3];
        int[] typeTotalWait = new int[3];
        int[] typeWaiting = new int[3];

        // 房型不是ALL时,collectWalkInBookings()只会去对应那一条队伍拿,另外两条完全不碰
        ListInterface<Booking> allBookings = collectWalkInBookings(roomTypeFilter);
        for (int i = 1; i <= allBookings.getNumberOfEntries(); i++) {
            Booking booking = allBookings.getEntry(i);
            boolean dateMatches = "ALL".equals(dateFilter) || booking.getRegisteredAt().startsWith(dateFilter);
            boolean typeMatches = "ALL".equalsIgnoreCase(roomTypeFilter)
                    || booking.getRequestedRoomType().equalsIgnoreCase(roomTypeFilter);
            if (!dateMatches || !typeMatches) {
                continue;
            }

            boolean allocated = booking.getAssignedRoomNo() != null;
            int waitMinutes = allocated
                    ? minutesBetween(booking.getRegisteredAt(), booking.getAllocatedAt())
                    : minutesBetween(booking.getRegisteredAt(), currentTimestamp());

            filtered.add(booking);
            waitMinutesList.add(waitMinutes);

            int typeIndex = roomTypeIndex(booking.getRequestedRoomType());

            // 按小时/按房型聚合都只收已分房的:还在等的等待时长是"算到此刻为止",
            // 会随着报表什么时候跑而一直变大,混进平均值会把数字整个拉爆
            // (改之前 09:00 那格显示过 3934 分钟,就是被一笔还在等的记录污染的)
            if (allocated) {
                if (waitMinutes >= 0) {
                    typeAllocated[typeIndex]++;
                    typeTotalWait[typeIndex] += waitMinutes;
                }
                int hour = Integer.parseInt(booking.getRegisteredAt().substring(11, 13));
                hourlyCount[hour]++;
                hourlyTotalWait[hour] += waitMinutes;
            } else {
                typeWaiting[typeIndex]++;
            }
        }

        sortByWaitMinutesDescending(filtered, waitMinutesList);

        walkInCLI.displayWaitTimeAnalysisHeader(dateFilter, roomTypeFilter);

        int allocatedCount = 0;
        int waitSumAllocated = 0;
        int waitSamples = 0;
        int longestWait = 0;
        String longestWaitGuest = "-";
        int longestWaiting = 0;
        String longestWaitingGuest = "-";

        if (filtered.isEmpty()) {
            walkInCLI.displayNoReportRecords();
        } else {
            for (int i = 1; i <= filtered.getNumberOfEntries(); i++) {
                Booking booking = filtered.getEntry(i);
                int waitMinutes = waitMinutesList.getEntry(i);
                boolean allocated = booking.getAssignedRoomNo() != null;

                if (allocated) {
                    allocatedCount++;
                    if (waitMinutes >= 0) {
                        waitSumAllocated = waitSumAllocated + waitMinutes;
                        waitSamples++;
                        if (waitMinutes > longestWait) {
                            longestWait = waitMinutes;
                            longestWaitGuest = booking.getGuestNameSnapshot();
                        }
                    }
                } else if (waitMinutes > longestWaiting) {
                    longestWaiting = waitMinutes;
                    longestWaitingGuest = booking.getGuestNameSnapshot();
                }

                walkInCLI.displayWaitTimeAnalysisRow(booking.getBookingId(), booking.getGuestNameSnapshot(),
                        booking.getRequestedRoomType(), waitMinutes, allocated);
            }

            int total = filtered.getNumberOfEntries();
            double averageWait = (waitSamples == 0) ? 0.0 : (double) waitSumAllocated / waitSamples;
            walkInCLI.displayWaitTimeAnalysisSummary(total, allocatedCount, total - allocatedCount,
                    averageWait, longestWait, longestWaitGuest, longestWaiting, longestWaitingGuest);
        }

        // 按房型比较平均等待——回答"哪一型房的客人等最久",那是系统性问题;
        // 小时分布回答"几点最忙",那是排班问题。两个角度不重叠。
        walkInCLI.displayWaitByRoomTypeHeader();
        walkInCLI.displayWaitByRoomTypeRow("Standard", typeAllocated[0], typeTotalWait[0], typeWaiting[0]);
        walkInCLI.displayWaitByRoomTypeRow("Deluxe", typeAllocated[1], typeTotalWait[1], typeWaiting[1]);
        walkInCLI.displayWaitByRoomTypeRow("Suite", typeAllocated[2], typeTotalWait[2], typeWaiting[2]);
        walkInCLI.displaySlowestRoomType(
                slowestRoomType(typeAllocated, typeTotalWait),
                slowestAverageWait(typeAllocated, typeTotalWait));

        // 先找出笔数最多是多少:柱状图要拿它当满格基准
        int maxHourlyCount = 0;
        for (int hour = 0; hour < 24; hour++) {
            if (hourlyCount[hour] > maxHourlyCount) {
                maxHourlyCount = hourlyCount[hour];
            }
        }

        // 再把"笔数刚好等于最大值"的时段全部收起来。资料少的时候很容易出现好几个
        // 时段并列最忙,只报第一个会误导——跟报表1的 Busiest room type 同一个处理方式
        ListInterface<Integer> busiestHours = new ArrayBasedList<>();
        if (maxHourlyCount > 0) {
            for (int hour = 0; hour < 24; hour++) {
                if (hourlyCount[hour] == maxHourlyCount) {
                    busiestHours.add(hour);
                }
            }
        }

        walkInCLI.displayHourlyBreakdownHeader();
        for (int hour = 0; hour < 24; hour++) {
            if (hourlyCount[hour] > 0) {
                double average = (double) hourlyTotalWait[hour] / hourlyCount[hour];
                walkInCLI.displayHourlyBreakdownRow(hour, hourlyCount[hour], average);
            }
        }
        walkInCLI.displayBusiestHour(busiestHours.getIterator(),
                busiestHours.getNumberOfEntries(), maxHourlyCount);
        walkInCLI.displayReportEnd();
    }

    // ========== 报表共用辅助方法 ==========

    /**
     * 把"现在还在三条队伍里排队的" + "已经分房、串在guestTable底下客人身上的
     * WALK_IN来源Booking"合起来,给两份报表共用。已取消的Booking在doCancel()
     * 被queue.remove()拿掉后就没有其他地方存着了,报表看不到——这是刻意的取舍,
     * 不是遗漏。
     *
     * @param roomTypeFilter "ALL"就三条队伍都收;指定房型就只走对应那一条队伍
     *                       (已分房的那部分不在队伍里,只能照旧扫guestTable)
     */
    private ListInterface<Booking> collectWalkInBookings(String roomTypeFilter) {
        ListInterface<Booking> result = new ArrayBasedList<>();

        // 三条队伍本来就按房型分开存,筛某个房型时直接选中那一条,另外两条根本不用碰——
        // 分区本身就是索引,不必先全部倒出来再逐笔比对房型
        if ("ALL".equalsIgnoreCase(roomTypeFilter)) {
            appendQueueBookings(standardQueue, result);
            appendQueueBookings(deluxeQueue, result);
            appendQueueBookings(suiteQueue, result);
        } else {
            QueueInterface<Booking> queue = getQueueForRoomType(roomTypeFilter);
            if (queue != null) {
                appendQueueBookings(queue, result);
            }
        }

        Iterator<Guest> guestIterator = guestTable.getIterator();
        while (guestIterator.hasNext()) {
            Guest guest = guestIterator.next();
            Iterator<Booking> bookingIterator = guest.getBookings().getIterator();
            while (bookingIterator.hasNext()) {
                Booking booking = bookingIterator.next();
                if ("WALK_IN".equals(booking.getSource())) {
                    result.add(booking);
                }
            }
        }
        return result;
    }

    private void appendQueueBookings(QueueInterface<Booking> queue, ListInterface<Booking> result) {
        Iterator<Booking> iterator = queue.getIterator();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
    }

    /**
     * 房型对到固定的阵列索引。三种房型是系统写死的,用阵列比用清单简单。
     *
     * @return 0=Standard(也含认不得的房型) 1=Deluxe 2=Suite
     */
    private int roomTypeIndex(String roomType) {
        if ("Deluxe".equalsIgnoreCase(roomType)) {
            return 1;
        }
        if ("Suite".equalsIgnoreCase(roomType)) {
            return 2;
        }
        return 0;
    }

    /**
     * 平均等待最久的那一型房。完全没有已分房的记录时回传 "-"。
     */
    private String slowestRoomType(int[] allocated, int[] totalWait) {
        String[] names = {"Standard", "Deluxe", "Suite"};
        int slowest = -1;
        double slowestAverage = -1.0;

        for (int i = 0; i < 3; i++) {
            if (allocated[i] == 0) {
                continue;
            }
            double average = (double) totalWait[i] / allocated[i];
            if (average > slowestAverage) {
                slowestAverage = average;
                slowest = i;
            }
        }
        return (slowest == -1) ? "-" : names[slowest];
    }

    /**
     * 上面那一型房的平均等待分钟数,给报表印在结论行上。
     */
    private double slowestAverageWait(int[] allocated, int[] totalWait) {
        double slowestAverage = 0.0;
        for (int i = 0; i < 3; i++) {
            if (allocated[i] == 0) {
                continue;
            }
            double average = (double) totalWait[i] / allocated[i];
            if (average > slowestAverage) {
                slowestAverage = average;
            }
        }
        return slowestAverage;
    }

    /**
     * 算两个"yyyy-MM-dd HH:mm:ss"时间字串相差几分钟。
     *
     * 资料档里只要有一行时间戳格式写坏,LocalDateTime.parse()就会抛
     * DateTimeParseException(unchecked),不接的话整个程序会直接退出、连主菜单
     * 都回不去。报表宁可那一格显示不出数字,也不该因为一行坏资料而整个挂掉,
     * 所以这里接住,回传-1让呼叫方当"算不出来"处理。
     */
    private int minutesBetween(String start, String end) {
        try {
            LocalDateTime startTime = LocalDateTime.parse(start, TIMESTAMP_FORMAT);
            LocalDateTime endTime = LocalDateTime.parse(end, TIMESTAMP_FORMAT);
            return (int) java.time.Duration.between(startTime, endTime).toMinutes();
        } catch (java.time.format.DateTimeParseException e) {
            return -1;
        }
    }

    /**
     * 按registeredAt由小到大原地排序(selection sort),不能用Collections.sort()。
     *
     * registeredAt 是固定宽度的 "yyyy-MM-dd HH:mm:ss",所以字串的字典序刚好
     * 等于时间的先后顺序,不用先 parse 成 LocalDateTime 再比。
     */
    private void sortBookingsByRegisteredAt(ListInterface<Booking> bookings) {
        int n = bookings.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int smallestPosition = i;
            for (int j = i + 1; j <= n; j++) {
                if (bookings.getEntry(j).getRegisteredAt()
                        .compareTo(bookings.getEntry(smallestPosition).getRegisteredAt()) < 0) {
                    smallestPosition = j;
                }
            }
            if (smallestPosition != i) {
                Booking temp = bookings.getEntry(i);
                bookings.replace(i, bookings.getEntry(smallestPosition));
                bookings.replace(smallestPosition, temp);
            }
        }
    }

    /**
     * 把bookings跟对应的waitMinutes两份清单,按等待分钟数由大到小同步排序(selection sort)。
     */
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

    // ========== 输入重试(格式类校验失败就原地重问,不中止整个操作) ==========

    /**
     * 姓名唯一的校验就是"不能是空白",所以空白本身就直接当成"使用者要取消",
     * 不用另外留一个专属的取消信号——回传null代表取消。
     * 打了东西但含数字或符号才是真的格式错误,原地重问。
     */
    private String promptValidName() {
        String name;
        while (true) {
            name = walkInCLI.promptName();
            if (ValidationUtility.isBlank(name)) {
                return null;
            }
            if (ValidationUtility.isValidName(name)) {
                return name;
            }
            walkInCLI.displayInvalidName(name);
        }
    }

    /**
     * 空白代表取消(回传null);打了东西但不是纯数字才是真的格式错误,原地重问。
     */
    private String promptValidPhone() {
        String phone;
        while (true) {
            phone = walkInCLI.promptPhone();
            if (ValidationUtility.isBlank(phone)) {
                return null;
            }
            if (ValidationUtility.isDigitsOnly(phone)) {
                return phone;
            }
            walkInCLI.displayInvalidPhone(phone);
        }
    }

    private String promptValidRoomType() {
        String roomType;
        while (true) {
            roomType = walkInCLI.promptRoomType();
            if (ValidationUtility.isBlank(roomType)) {
                return null;
            }
            if (getQueueForRoomType(roomType) != null) {
                return roomType;
            }
            walkInCLI.displayInvalidRoomType(roomType);
        }
    }

    /**
     * 空白由WalkInCLI转成Integer.MIN_VALUE(不会跟任何真实晚数或既有的-1无效值撞),
     * 用来代表"使用者要取消",跟"打了但格式不对/不是正数"这种要重问的情况分开。
     */
    private int promptValidNumberOfNights() {
        int numberOfNights;
        while (true) {
            numberOfNights = walkInCLI.promptNumberOfNights();
            if (numberOfNights == Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            if (numberOfNights > 0) {
                return numberOfNights;
            }
            walkInCLI.displayInvalidNumberOfNights(numberOfNights);
        }
    }

    private String promptValidBookingId() {
        String bookingId = walkInCLI.promptBookingIdToCancel();
        return ValidationUtility.isBlank(bookingId) ? null : bookingId;
    }

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
     * 帮一位第一次入住、还不是会员的Walk-In客人,开一个最低等级(Standard)的会员档案,
     * 存进memberList,再回传这个新Member给呼叫方拿memberId/tier去建Guest用。
     * currentPoints/totalPointsEarned都从0开始——实际赚多少分是模块5的事,这里只负责
     * "这个人现在是有memberId的会员了"这个身份的建立。
     */
    private Member enrollAsStandardMember(String name, String phone) {
        String memberId = "M" + memberCounter;
        memberCounter++;
        Member member = new Member(memberId, name, phone, "Standard", 0, 0);
        memberList.add(member);
        return member;
    }

    /**
     * 扫一次memberList,把"M"开头、后面接数字的会员编号(比如种子资料的M1001~M1005)
     * 都解析出数字部分,取最大值+1,当作WalkInControl自己接下来要用的编号起点——
     * 之后终生只有这个计数器在用这个号码段,不会再重新扫描,新会员编号才会跟种子资料
     * 格式一致,同时不会有"两个地方各自算出同一个号码"这种撞号风险。
     * memberList万一是空的(理论上不会,MemberDao已经先读过种子资料),给一个保底起点。
     */
    private int computeNextMemberNumber() {
        int maxNumber = 1000;
        Iterator<Member> iterator = memberList.getIterator();
        while (iterator.hasNext()) {
            Member member = iterator.next();
            String id = member.getMemberId();
            if (id != null && id.length() > 1 && id.charAt(0) == 'M') {
                try {
                    int number = Integer.parseInt(id.substring(1));
                    if (number > maxNumber) {
                        maxNumber = number;
                    }
                } catch (NumberFormatException e) {
                    // ID不是"M"+纯数字这个格式,跳过,不影响其他笔的计算
                }
            }
        }
        return maxNumber + 1;
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
     * 在指定的队伍里,用bookingId线性找出对应的Booking物件。
     * 找到的是"真正存在队伍里的那个物件",这样才能拿去交给 queue.remove() 正确比对、移除。
     */
    private Booking findBookingInQueue(QueueInterface<Booking> queue, String bookingId) {
        Iterator<Booking> iterator = queue.getIterator();
        while (iterator.hasNext()) {
            Booking booking = iterator.next();
            if (booking.getBookingId().equals(bookingId)) {
                return booking;
            }
        }
        return null;
    }
}

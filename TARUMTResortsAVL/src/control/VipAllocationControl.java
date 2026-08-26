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

    // Reporting-only service targets. They measure how quickly each tier was
    // served, but they never change AVL priority or trigger room allocation.
    private static final int STANDARD_SLA_MINUTES = 30;
    private static final int ELITE_SLA_MINUTES = 20;
    private static final int PLATINUM_SLA_MINUTES = 15;
    private static final int DIAMOND_SLA_MINUTES = 10;
    private static final double SLA_COMPLIANCE_GOAL_PERCENT = 90.0;

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
        // 第一步:先拿到会员本人。空白代表使用者要取消;打错编号就地重问,
        // 不把人踢回主选单——打错字是最常见的情况,重问才是对的
        Member member = promptValidMember();
        if (member == null) {
            vipAllocationCLI.displayCancelled();
            return;
        }

        // 把会员等级(文字)换算成排名数字,插进树时靠这个数字决定优先级——
        // 同一位会员这次登记的每一笔Booking等级都一样,只需要算一次
        // tierRank<=0(比如Walk-In入住后自动开的Standard会员)一样放行——业务规则是
        // "任何有memberId的会员,只要走VIP这条路登记,优先权本来就该高于Walk-In队伍",
        // 不是只有Elite/Platinum/Diamond才算数。Standard插进树后排名自然垫底(compareTo
        // 只看tierRank/arrivalSequence,0天生排在1/2/3后面),但仍然会跟真VIP一样,
        // 排在WalkInControl.tryAllocate()挡住的Walk-In队伍前面——这就是要的效果
        int tierRank = TierRankUtility.tierToRank(member.getTier());

        // 这位VIP可能一次要订好几间房(不一定同房型),所以会员资料只查一次,
        // 底下用同一个confirmationNumber循环开多笔Booking,直到不用再加了。
        // confirmationNumber 延后到确定第一笔Booking真的要建立时才发号(见下方),
        // 不能一进循环就先发——不然客人在房型/晚数这一步按空白取消,号码已经被
        // ++掉、却从头到尾没建过任何Booking,永远烧掉一个不会再出现的确认号。
        String confirmationNumber = null;

        boolean continueBooking = true;
        while (continueBooking) {
            // 问要什么房型,顺便决定这笔Booking该进哪一棵树
            String roomType = promptValidRoomType();
            if (roomType == null) {
                // 房型这里留空白,等同"不用再加房间了",直接结束这一轮登记,
                // 不用额外印取消讯息——跟平常按"add another room? n"退出是一样的效果
                break;
            }
            SearchTreeInterface<Booking> tree = getTreeForRoomType(roomType);

            // 住几晚要在客人还在面前的登记当下先问好,存进Booking——
            // 分房不再保证是当场发生的,之后可能是房间空出来才自动触发,
            // 到时候客人不一定还在,没办法临时问
            int numberOfNights = promptValidNumberOfNights();
            if (numberOfNights == Integer.MIN_VALUE) {
                break;
            }

            // 走到这里代表这一笔真的要建立了,第一次进来才发号,同一位会员
            // 之后再加订的房间沿用同一个confirmationNumber
            if (confirmationNumber == null) {
                confirmationCounter++;
                confirmationNumber = String.valueOf(confirmationCounter);
            }

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
        // 先选房型、把这个房型的等待名单印出来,让使用者看清楚树里到底有什么
        // (含bookingId)再决定要取消哪一笔,不用盲打——树是空的话,印完"没人在等"
        // 的提示后直接回头重问房型,不会继续往下问一个注定查不到的bookingId
        SearchTreeInterface<Booking> tree;
        String roomType;
        while (true) {
            roomType = promptValidRoomType();
            if (roomType == null) {
                vipAllocationCLI.displayCancelled();
                return;
            }
            tree = getTreeForRoomType(roomType);
            vipAllocationCLI.displayWaitingList(roomType, tree.getInorderIterator());
            if (!tree.isEmpty()) {
                break;
            }
        }

        // 用bookingId取消,不是confirmationNumber——同一个confirmationNumber可能同时
        // 有好几笔Booking在同一个房型的树里(一次订多间房),用confirmationNumber去找
        // 会有歧义,没办法让客人指定要取消的到底是哪一笔;bookingId每笔都是唯一的
        // 只有bookingId,不知道这笔Booking的tierRank/arrivalSequence是多少,
        // 没办法直接叫树去比大小导航——所以先用in-order扫过去,找到"真正的那个物件"
        String bookingId = promptValidBookingId();
        if (bookingId == null) {
            vipAllocationCLI.displayCancelled();
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
        String roomType = promptValidRoomType();
        if (roomType == null) {
            vipAllocationCLI.displayCancelled();
            return;
        }
        SearchTreeInterface<Booking> tree = getTreeForRoomType(roomType);

        // 直接把in-order遍历的iterator传给Boundary去印,不用自己先转成清单——
        // 因为compareTo设计的方向,这个顺序天生就是"优先级由高到低"
        vipAllocationCLI.displayWaitingList(roomType, tree.getInorderIterator());
    }

    // ========== 报表1:VIP等待名单实时报表 ==========

    /**
     * Real-time waiting report. Requests are globally sorted by the same
     * business priority used by the AVL trees: higher tier first, then earlier
     * arrival within the same tier. SLA targets are reporting indicators only.
     */
    void doVipWaitingListReport() {
        int tierRankFilter = vipAllocationCLI.promptReportTierRank();

        ListInterface<Booking> filtered = new ArrayBasedList<>();
        ListInterface<Integer> waitMinutesList = new ArrayBasedList<>();

        collectTierFiltered(standardTree, tierRankFilter, filtered, waitMinutesList);
        collectTierFiltered(deluxeTree, tierRankFilter, filtered, waitMinutesList);
        collectTierFiltered(suiteTree, tierRankFilter, filtered, waitMinutesList);

        sortByVipPriority(filtered, waitMinutesList);

        vipAllocationCLI.displayVipWaitingListReportHeader(
                tierRankFilter, currentTimestamp());
        if (filtered.isEmpty()) {
            vipAllocationCLI.displayNoReportRecords();
        } else {
            int breachedCount = 0;
            int longestPosition = 1;

            for (int i = 1; i <= filtered.getNumberOfEntries(); i++) {
                Booking booking = filtered.getEntry(i);
                int waitMinutes = waitMinutesList.getEntry(i);
                int targetMinutes = slaTargetMinutes(booking.getTierRankAtRequest());
                boolean breached = waitMinutes > targetMinutes;

                if (breached) {
                    breachedCount++;
                }
                if (waitMinutes > waitMinutesList.getEntry(longestPosition)) {
                    longestPosition = i;
                }

                vipAllocationCLI.displayVipWaitingListReportRow(i,
                        booking.getBookingId(), booking.getGuestNameSnapshot(),
                        TierRankUtility.rankToTier(booking.getTierRankAtRequest()),
                        booking.getRequestedRoomType(), waitMinutes,
                        targetMinutes, breached);
            }

            Booking nextPriority = filtered.getEntry(1);
            Booking longestWaiting = filtered.getEntry(longestPosition);
            vipAllocationCLI.displayVipWaitingListReportSummary(
                    filtered.getNumberOfEntries(),
                    filtered.getNumberOfEntries() - breachedCount,
                    breachedCount,
                    nextPriority.getGuestNameSnapshot(),
                    nextPriority.getRequestedRoomType(),
                    longestWaiting.getGuestNameSnapshot(),
                    waitMinutesList.getEntry(longestPosition));
        }
        vipAllocationCLI.displayReportEnd();
    }

    private void collectTierFiltered(SearchTreeInterface<Booking> tree, int tierRankFilter,
                                      ListInterface<Booking> filtered, ListInterface<Integer> waitMinutesList) {
        Iterator<Booking> iterator = tree.getInorderIterator();
        while (iterator.hasNext()) {
            Booking booking = iterator.next();
            // -1 = "All"(不限等级)。不能用0当哨兵,因为Standard会员现在也能走VIP
            // 登记这条路,0是Standard真正的排名,不是"没有filter"的意思
            if (tierRankFilter == -1 || booking.getTierRankAtRequest() == tierRankFilter) {
                filtered.add(booking);
                int waitMinutes = minutesBetween(booking.getRegisteredAt(), currentTimestamp());
                waitMinutesList.add(Math.max(0, waitMinutes));
            }
        }
    }

    // ========== 报表2:等级分房达标率报表 ==========

    /**
     * Measures allocation performance against the reporting-only SLA target
     * for each tier. The date filter uses the allocation date.
     */
    void doTierSlaReport() {
        int tierRankFilter = vipAllocationCLI.promptReportTierRank();
        String fromDate = vipAllocationCLI.promptReportFromDate();
        String toDate = vipAllocationCLI.promptReportToDate();

        // 固定4档(Diamond=3, Platinum=2, Elite=1, Standard=0)——Standard会员现在
        // 也能走VIP登记这条路(排名垫底但仍插进树里),所以0档也要统计,不能排除
        int[] count = new int[4];
        int[] totalWaitMinutes = new int[4];
        int[] metCount = new int[4];
        int[] breachedCount = new int[4];
        int[] worstWaitMinutes = new int[4];

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
                if (rank < 0 || rank > 3) {
                    continue;
                }
                // -1 = "All"(不限等级),tierRankFilter本身可能是0(Standard),
                // 不能再用0当哨兵去判断"有没有filter"
                if (tierRankFilter != -1 && rank != tierRankFilter) {
                    continue;
                }
                String allocatedDate = booking.getAllocatedAt().substring(0, 10);
                if (allocatedDate.compareTo(fromDate) < 0 || allocatedDate.compareTo(toDate) > 0) {
                    continue;
                }
                int waitMinutes = minutesBetween(
                        booking.getRegisteredAt(), booking.getAllocatedAt());
                if (waitMinutes < 0) {
                    continue;
                }

                count[rank]++;
                totalWaitMinutes[rank] += waitMinutes;
                if (waitMinutes <= slaTargetMinutes(rank)) {
                    metCount[rank]++;
                } else {
                    breachedCount[rank]++;
                }
                if (waitMinutes > worstWaitMinutes[rank]) {
                    worstWaitMinutes[rank] = waitMinutes;
                }
            }
        }

        // Store only ranks that have data, then use a self-written selection
        // sort to put the weakest SLA performance first.
        ListInterface<Integer> reportRanks = new ArrayBasedList<>();
        for (int rank = 3; rank >= 0; rank--) {
            if (count[rank] > 0) {
                reportRanks.add(rank);
            }
        }
        sortSlaRanksByRisk(reportRanks, count, metCount, totalWaitMinutes);

        vipAllocationCLI.displayTierSlaReportHeader(tierRankFilter,
                fromDate, toDate, currentTimestamp(),
                SLA_COMPLIANCE_GOAL_PERCENT,
                DIAMOND_SLA_MINUTES, PLATINUM_SLA_MINUTES,
                ELITE_SLA_MINUTES, STANDARD_SLA_MINUTES);
        if (reportRanks.isEmpty()) {
            vipAllocationCLI.displayNoReportRecords();
        } else {
            int totalAllocations = 0;
            int totalMet = 0;
            int totalBreached = 0;

            for (int i = 1; i <= reportRanks.getNumberOfEntries(); i++) {
                int rank = reportRanks.getEntry(i);
                double averageWait = totalWaitMinutes[rank] / (double) count[rank];
                double compliance = percentage(metCount[rank], count[rank]);

                vipAllocationCLI.displayTierSlaReportRow(
                        TierRankUtility.rankToTier(rank),
                        slaTargetMinutes(rank), count[rank], metCount[rank],
                        breachedCount[rank], compliance, averageWait,
                        worstWaitMinutes[rank], slaPerformanceStatus(compliance));

                totalAllocations += count[rank];
                totalMet += metCount[rank];
                totalBreached += breachedCount[rank];
            }

            int weakestRank = reportRanks.getEntry(1);
            double overallCompliance = percentage(totalMet, totalAllocations);
            double weakestCompliance = percentage(
                    metCount[weakestRank], count[weakestRank]);
            vipAllocationCLI.displayTierSlaReportSummary(
                    totalAllocations, totalMet, totalBreached,
                    overallCompliance, slaPerformanceStatus(overallCompliance),
                    TierRankUtility.rankToTier(weakestRank), weakestCompliance,
                    SLA_COMPLIANCE_GOAL_PERCENT);
        }
        vipAllocationCLI.displayReportEnd();
    }

    /**
     * Selection sort: lowest compliance first; when compliance ties, the tier
     * with the longer average wait comes first; final tie-break is higher tier.
     */
    private void sortSlaRanksByRisk(ListInterface<Integer> ranks,
                                    int[] count, int[] metCount,
                                    int[] totalWaitMinutes) {
        int n = ranks.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int riskiestPosition = i;
            for (int j = i + 1; j <= n; j++) {
                if (comesBeforeInSlaReport(
                        ranks.getEntry(j), ranks.getEntry(riskiestPosition),
                        count, metCount, totalWaitMinutes)) {
                    riskiestPosition = j;
                }
            }
            if (riskiestPosition != i) {
                Integer tempRank = ranks.getEntry(i);
                ranks.replace(i, ranks.getEntry(riskiestPosition));
                ranks.replace(riskiestPosition, tempRank);
            }
        }
    }

    private boolean comesBeforeInSlaReport(int rankA, int rankB,
                                            int[] count, int[] metCount,
                                            int[] totalWaitMinutes) {
        double complianceA = percentage(metCount[rankA], count[rankA]);
        double complianceB = percentage(metCount[rankB], count[rankB]);
        if (Double.compare(complianceA, complianceB) != 0) {
            return complianceA < complianceB;
        }

        double averageA = totalWaitMinutes[rankA] / (double) count[rankA];
        double averageB = totalWaitMinutes[rankB] / (double) count[rankB];
        if (Double.compare(averageA, averageB) != 0) {
            return averageA > averageB;
        }
        return rankA > rankB;
    }

    // ========== 报表共用辅助方法 ==========

    private int minutesBetween(String start, String end) {
        LocalDateTime startTime = LocalDateTime.parse(start, TIMESTAMP_FORMAT);
        LocalDateTime endTime = LocalDateTime.parse(end, TIMESTAMP_FORMAT);
        return (int) java.time.Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * Selection sort using Booking.compareTo(): higher tier first, then earlier
     * arrival. The wait-time list is swapped in parallel with the bookings.
     */
    private void sortByVipPriority(ListInterface<Booking> bookings,
                                   ListInterface<Integer> waitMinutes) {
        int n = bookings.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int highestPriorityPosition = i;
            for (int j = i + 1; j <= n; j++) {
                if (bookings.getEntry(j)
                        .compareTo(bookings.getEntry(highestPriorityPosition)) < 0) {
                    highestPriorityPosition = j;
                }
            }
            if (highestPriorityPosition != i) {
                Booking tempBooking = bookings.getEntry(i);
                bookings.replace(i, bookings.getEntry(highestPriorityPosition));
                bookings.replace(highestPriorityPosition, tempBooking);

                Integer tempMinutes = waitMinutes.getEntry(i);
                waitMinutes.replace(i, waitMinutes.getEntry(highestPriorityPosition));
                waitMinutes.replace(highestPriorityPosition, tempMinutes);
            }
        }
    }

    private int slaTargetMinutes(int tierRank) {
        switch (tierRank) {
            case 3:
                return DIAMOND_SLA_MINUTES;
            case 2:
                return PLATINUM_SLA_MINUTES;
            case 1:
                return ELITE_SLA_MINUTES;
            default:
                return STANDARD_SLA_MINUTES;
        }
    }

    private double percentage(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : numerator * 100.0 / denominator;
    }

    private String slaPerformanceStatus(double compliancePercent) {
        if (compliancePercent >= SLA_COMPLIANCE_GOAL_PERCENT) {
            return "PASS";
        }
        if (compliancePercent >= 75.0) {
            return "WATCH";
        }
        return "FAIL";
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
     * Prompts for a member ID until it resolves to a real member.
     *
     * <p>A wrong ID is almost always a typo, so it is treated like any other
     * format error and re-prompted in place. Only a blank entry cancels.</p>
     *
     * @return the member, or null when the user cancels
     */
    private Member promptValidMember() {

        while (true) {

            String memberId = vipAllocationCLI.promptMemberId();

            if (ValidationUtility.isBlank(memberId)) {
                return null;
            }

            Member member = findMemberById(memberId);
            if (member != null) {
                return member;
            }

            vipAllocationCLI.displayMemberNotFound(memberId);
        }
    }

    private String promptValidRoomType() {
        String roomType;
        while (true) {
            roomType = vipAllocationCLI.promptRoomType();
            if (ValidationUtility.isBlank(roomType)) {
                return null;
            }
            if (getTreeForRoomType(roomType) != null) {
                return roomType;
            }
            vipAllocationCLI.displayInvalidRoomType(roomType);
        }
    }

    private int promptValidNumberOfNights() {
        int numberOfNights;
        while (true) {
            numberOfNights = vipAllocationCLI.promptNumberOfNights();
            if (numberOfNights == Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            if (numberOfNights > 0) {
                return numberOfNights;
            }
            vipAllocationCLI.displayInvalidNumberOfNights(numberOfNights);
        }
    }

    private String promptValidBookingId() {
        String bookingId = vipAllocationCLI.promptBookingIdToCancel();
        return ValidationUtility.isBlank(bookingId) ? null : bookingId;
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
            if (ValidationUtility.idsMatch(member.getMemberId(), memberId)) {
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

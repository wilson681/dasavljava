package control;

import adt.ArrayBasedList;
import adt.LinkedStack;
import adt.ListInterface;
import adt.StackInterface;
import boundary.HousekeepingCLI;
import entity.Room;
import entity.RoomHistory;
import entity.RollbackLogEntry;
import java.time.LocalDate;
import utility.ValidationUtility;

/**
 * HousekeepingControl.java
 * Control class for the Housekeeping and Task Log module.
 *
 * @author YOUR FULL NAME
 */
public class HousekeepingControl {

    private static final int MAX_HISTORY_RECORDS = 20;

    private final HousekeepingCLI housekeepingCLI;
    private final ListInterface<Room> roomList;

    private final VipAllocationControl vipAllocationControl;
    private final WalkInControl walkInControl;

    // 只有 doRollbackStatus() 写、只有本模块自己的报表读,其他模块不会碰
    private final ListInterface<RollbackLogEntry> rollbackLog;

    public HousekeepingControl(
            HousekeepingCLI housekeepingCLI,
            ListInterface<Room> roomList,
            VipAllocationControl vipAllocationControl,
            WalkInControl walkInControl,
            ListInterface<RollbackLogEntry> rollbackLog) {

        if (housekeepingCLI == null
                || roomList == null
                || vipAllocationControl == null
                || walkInControl == null
                || rollbackLog == null) {

            throw new IllegalArgumentException(
                    "HousekeepingControl dependencies cannot be null"
            );
        }

        this.housekeepingCLI = housekeepingCLI;
        this.roomList = roomList;
        this.vipAllocationControl = vipAllocationControl;
        this.walkInControl = walkInControl;
        this.rollbackLog = rollbackLog;
    }

    // =========================================================
    // Main Menu
    // =========================================================

    public void run() {

        boolean running = true;

        while (running) {

            int choice = housekeepingCLI.displayMenuAndGetChoice();

            switch (choice) {

                case 1:
                    doViewHousekeepingRooms();
                    housekeepingCLI.promptContinue();
                    break;

                case 2:
                    doUpdateRoomStatus();
                    housekeepingCLI.promptContinue();
                    break;

                case 3:
                    doRollbackStatus();
                    housekeepingCLI.promptContinue();
                    break;

                case 4:
                    doViewStatusHistory();
                    housekeepingCLI.promptContinue();
                    break;

                case 0:
                    running = false;
                    break;

                default:
                    housekeepingCLI.displayInvalidChoice();
                    break;
            }
        }
    }

    // =========================================================
    // Front Desk Integration
    // =========================================================

    public void markRoomNeedsCleaning(String roomNumber) {

        Room room = findRoom(roomNumber);

        if (room == null) {
            return;
        }

        RoomHistory history = room.getRoomHistory();

        String latestHistoryStatus =
                history.getStatusStack().peek();

        /*
         * If another module changed the room status,
         * record that status before NEEDS_CLEANING.
         *
         * Example:
         *
         * AVAILABLE
         *    ↓
         * OCCUPIED
         *    ↓
         * NEEDS_CLEANING
         */
        if (latestHistoryStatus == null
                || !latestHistoryStatus.equals(
                        room.getStatus())) {

            prepareHistoryForPush(history);

            history.getStatusStack().push(
                    room.getStatus()
            );
        }

        latestHistoryStatus =
                history.getStatusStack().peek();

        if (!"NEEDS_CLEANING".equals(
                latestHistoryStatus)) {

            prepareHistoryForPush(history);

            history.getStatusStack().push(
                    "NEEDS_CLEANING"
            );
        }

        room.setStatus("NEEDS_CLEANING");
    }

    // =========================================================
    // Option 1
    // View Rooms Requiring Housekeeping
    // =========================================================

    private void doViewHousekeepingRooms() {

        housekeepingCLI.displayHousekeepingRooms(
                buildHousekeepingRoomList().getIterator()
        );
    }

    /**
     * 目前处在housekeeping流程里(NEEDS_CLEANING/CLEANING_IN_PROGRESS/INSPECTED)
     * 的房间清单——选项1(查看清单)、选项2(更新状态,只有这些房间能被操作)共用。
     */
    private ListInterface<Room> buildHousekeepingRoomList() {

        ListInterface<Room> housekeepingRooms =
                new ArrayBasedList<>();

        for (int i = 1;
                i <= roomList.getNumberOfEntries();
                i++) {

            Room room = roomList.getEntry(i);

            if (isHousekeepingStatus(
                    room.getStatus())) {

                housekeepingRooms.add(room);
            }
        }

        return housekeepingRooms;
    }

    /**
     * 全部房间的清单(不限housekeeping流程内),给选项3(回滚)、选项4(查历史)
     * 用——这两个操作理论上任何房间都能查,不像选项2只限housekeeping流程内的房间。
     */
    private ListInterface<Room> buildAllRoomsList() {

        ListInterface<Room> allRooms =
                new ArrayBasedList<>();

        for (int i = 1;
                i <= roomList.getNumberOfEntries();
                i++) {

            allRooms.add(roomList.getEntry(i));
        }

        return allRooms;
    }

    /**
     * 房号只能是数字,格式错误、查无此房都原地重问(不中止整个操作)——房号清单
     * 已经列在上面,查无此房大概率是打错字,不像"这个人存不存在"那种真的需要
     * 使用者自己确认的情况。空白才代表使用者要取消。
     */
    private Room promptValidRoom() {

        while (true) {

            String roomNumber = housekeepingCLI.promptRoomNumber();

            if (ValidationUtility.isBlank(roomNumber)) {
                return null;
            }

            if (!ValidationUtility.isDigitsOnly(roomNumber)) {
                housekeepingCLI.displayInvalidRoomNumber(roomNumber);
                continue;
            }

            Room room = findRoom(roomNumber);

            if (room != null) {
                return room;
            }

            housekeepingCLI.displayRoomNotFound(roomNumber);
        }
    }

    private boolean isHousekeepingStatus(String status) {

        if (status == null) {
            return false;
        }

        return status.equalsIgnoreCase(
                    "NEEDS_CLEANING")
                || status.equalsIgnoreCase(
                    "CLEANING_IN_PROGRESS")
                || status.equalsIgnoreCase(
                    "INSPECTED");
    }

    // =========================================================
    // Option 2
    // Update Room Status
    // =========================================================

    private void doUpdateRoomStatus() {

        housekeepingCLI.displayHousekeepingRooms(
                buildHousekeepingRoomList().getIterator()
        );

        Room room = promptValidRoom();

        if (room == null) {
            housekeepingCLI.displayCancelled();
            return;
        }

        String previousStatus =
                room.getStatus();

        housekeepingCLI.displayCurrentRoomStatus(
                room.getRoomNumber(),
                previousStatus
        );

        if (!isHousekeepingStatus(previousStatus)) {

            housekeepingCLI.displayRoomNotInPipeline(
                    room.getRoomNumber(),
                    previousStatus
            );
            return;
        }

        String newStatus =
                promptValidNewStatus(previousStatus);

        if (newStatus == null) {
            housekeepingCLI.displayCancelled();
            return;
        }

        RoomHistory history =
                room.getRoomHistory();

        String latestHistoryStatus =
                history.getStatusStack().peek();

        /*
         * Make sure the current status exists
         * at the top of the history Stack.
         */
        if (latestHistoryStatus == null
                || !latestHistoryStatus.equals(
                        previousStatus)) {

            prepareHistoryForPush(history);

            history.getStatusStack().push(
                    previousStatus
            );
        }

        /*
         * Push new status.
         */
        prepareHistoryForPush(history);

        history.getStatusStack().push(
                newStatus
        );

        room.setStatus(newStatus);

        housekeepingCLI.displayStatusUpdated(
                room.getRoomNumber(),
                previousStatus,
                newStatus
        );

        /*
         * Cleaning completed.
         */
        if (newStatus.equalsIgnoreCase(
                "AVAILABLE")) {

            String roomType =
                    room.getRoomType();

            /*
             * VIP priority first.
             */
            vipAllocationControl.tryAllocate(
                    roomType
            );

            /*
             * Then Walk-In.
             */
            walkInControl.tryAllocate(
                    roomType
            );
        }
    }

    /**
     * The pipeline only ever allows exactly one next status from any given
     * current status - this mirrors that same rule so the CLI can tell the
     * staff which single choice will actually work, instead of making them
     * guess through all 4 menu options.
     */
    private String nextValidStatus(String currentStatus) {

        if (currentStatus.equalsIgnoreCase("NEEDS_CLEANING")) {
            return "CLEANING_IN_PROGRESS";
        }

        if (currentStatus.equalsIgnoreCase("CLEANING_IN_PROGRESS")) {
            return "INSPECTED";
        }

        if (currentStatus.equalsIgnoreCase("INSPECTED")) {
            return "AVAILABLE";
        }

        return null;
    }

    private boolean isValidStatusTransition(
            String currentStatus,
            String newStatus) {

        if (currentStatus.equalsIgnoreCase(
                "NEEDS_CLEANING")) {

            return newStatus.equalsIgnoreCase(
                    "CLEANING_IN_PROGRESS"
            );
        }

        if (currentStatus.equalsIgnoreCase(
                "CLEANING_IN_PROGRESS")) {

            return newStatus.equalsIgnoreCase(
                    "INSPECTED"
            );
        }

        if (currentStatus.equalsIgnoreCase(
                "INSPECTED")) {

            return newStatus.equalsIgnoreCase(
                    "AVAILABLE"
            );
        }

        return false;
    }

    // =========================================================
    // Option 3
    // Roll Back Latest Status
    // =========================================================

    private void doRollbackStatus() {

        housekeepingCLI.displayAllRooms(
                buildAllRoomsList().getIterator()
        );

        Room room = promptValidRoom();

        if (room == null) {
            housekeepingCLI.displayCancelled();
            return;
        }

        String roomNumber = room.getRoomNumber();

        RoomHistory history =
                room.getRoomHistory();

        String latestHistoryStatus =
                history.getStatusStack().peek();

        /*
         * Do not rollback if another module has
         * already changed this room.
         *
         * Example:
         *
         * Stack = AVAILABLE
         * Room = OCCUPIED
         */
        if (latestHistoryStatus == null
                || !latestHistoryStatus.equals(
                        room.getStatus())) {

            housekeepingCLI
                    .displayRollbackNotAvailable(
                            roomNumber
                    );

            return;
        }

        if (history.getStatusStack().size()
                <= 1) {

            housekeepingCLI
                    .displayRollbackNotAvailable(
                            roomNumber
                    );

            return;
        }

        /*
         * LIFO:
         *
         * Remove latest.
         */
        String removedStatus =
                history.getStatusStack().pop();

        /*
         * Previous status becomes the top.
         */
        String restoredStatus =
                history.getStatusStack().peek();

        /*
         * Housekeeping may only roll back within its own
         * pipeline (NEEDS_CLEANING/CLEANING_IN_PROGRESS/
         * INSPECTED/AVAILABLE). OCCUPIED belongs to a real
         * guest stay (Booking/BillingRecord/points already
         * committed elsewhere), which Housekeeping has no
         * way to see or safely restore — put the popped
         * status back and refuse instead.
         */
        if (restoredStatus == null
                || restoredStatus.equalsIgnoreCase(
                        "OCCUPIED")) {

            history.getStatusStack().push(
                    removedStatus
            );

            housekeepingCLI
                    .displayRollbackNotAvailable(
                            roomNumber
                    );

            return;
        }

        room.setStatus(restoredStatus);

        /*
         * Only a rollback that actually goes through reaches
         * here (the two guard clauses above return early), so
         * this log stays a clean, permanent record of real
         * rollback events -- unlike the status Stack itself,
         * which loses the evidence the moment it is popped.
         */
        rollbackLog.add(new RollbackLogEntry(
                roomNumber,
                removedStatus,
                restoredStatus,
                LocalDate.now().toString()
        ));

        housekeepingCLI.displayRollbackResult(
                roomNumber,
                removedStatus,
                restoredStatus
        );
    }

    // =========================================================
    // Option 4
    // View Room Status History
    // =========================================================

    private void doViewStatusHistory() {

        housekeepingCLI.displayAllRooms(
                buildAllRoomsList().getIterator()
        );

        Room room = promptValidRoom();

        if (room == null) {
            housekeepingCLI.displayCancelled();
            return;
        }

        RoomHistory history =
                room.getRoomHistory();

        housekeepingCLI.displayRoomStatusHistory(
                room.getRoomNumber(),
                room.getStatus(),
                history.getStatusStack()
                        .getIterator()
        );
    }

    // =========================================================
    // Option 5
    // Housekeeping Status Report
    // =========================================================

    void doGenerateHousekeepingStatusReport() {

        String roomTypeFilter = promptValidReportRoomType();
        if (roomTypeFilter == null) {
            housekeepingCLI.displayCancelled();
            return;
        }

        String statusFilter = promptValidReportStatus();
        if (statusFilter == null) {
            housekeepingCLI.displayCancelled();
            return;
        }

        ListInterface<Room> result =
                new ArrayBasedList<>();

        /*
         * Linear Search
         *
         * Filter 1 = Room Type
         * Filter 2 = Room Status
         */
        for (int i = 1;
                i <= roomList.getNumberOfEntries();
                i++) {

            Room room =
                    roomList.getEntry(i);

            boolean roomTypeMatches =
                    "ALL".equalsIgnoreCase(
                            roomTypeFilter)
                    || room.getRoomType()
                            .equalsIgnoreCase(
                                    roomTypeFilter
                            );

            boolean statusMatches =
                    "ALL".equalsIgnoreCase(
                            statusFilter)
                    || room.getStatus()
                            .equalsIgnoreCase(
                                    statusFilter
                            );

            if (roomTypeMatches
                    && statusMatches) {

                result.add(room);
            }
        }

        /*
         * Selection Sort:
         * Room Number ascending.
         */
        sortRoomsByRoomNumber(result);

        housekeepingCLI.displayHousekeepingStatusReportHeader(
                roomTypeFilter, statusFilter);

        if (result.isEmpty()) {
            housekeepingCLI.displayNoReportRecords();
            housekeepingCLI.displayReportEnd();
            return;
        }

        /*
         * 一边印明细,一边把汇总要用的数字累加起来,不用为了统计再扫第二遍。
         * 按房型分开数,是为了回答"哪一型房已经完全卖不出去了"——那是这份
         * 报表真正的价值,光看明细表要自己一行一行比对才看得出来。
         */
        int availableCount = 0;
        int occupiedCount = 0;
        int needsCleaningCount = 0;
        int cleaningInProgressCount = 0;
        int inspectedCount = 0;

        int standardTotal = 0;
        int deluxeTotal = 0;
        int suiteTotal = 0;
        int standardAvailable = 0;
        int deluxeAvailable = 0;
        int suiteAvailable = 0;

        for (int i = 1; i <= result.getNumberOfEntries(); i++) {

            Room room = result.getEntry(i);
            String status = room.getStatus();
            String roomType = room.getRoomType();
            boolean sellable = "AVAILABLE".equalsIgnoreCase(status);

            if (sellable) {
                availableCount++;
            } else if ("OCCUPIED".equalsIgnoreCase(status)) {
                occupiedCount++;
            } else if ("NEEDS_CLEANING".equalsIgnoreCase(status)) {
                needsCleaningCount++;
            } else if ("CLEANING_IN_PROGRESS".equalsIgnoreCase(status)) {
                cleaningInProgressCount++;
            } else if ("INSPECTED".equalsIgnoreCase(status)) {
                inspectedCount++;
            }

            if ("Deluxe".equalsIgnoreCase(roomType)) {
                deluxeTotal++;
                if (sellable) {
                    deluxeAvailable++;
                }
            } else if ("Suite".equalsIgnoreCase(roomType)) {
                suiteTotal++;
                if (sellable) {
                    suiteAvailable++;
                }
            } else {
                standardTotal++;
                if (sellable) {
                    standardAvailable++;
                }
            }

            housekeepingCLI.displayHousekeepingStatusReportRow(
                    room.getRoomNumber(), roomType, status);
        }

        // 已经筛到单一状态时,"各状态占比""还有几间能卖"这些分析全都是必然的答案
        // (筛OCCUPIED就一定100%是OCCUPIED、一定0间能卖),印出来只是噪音,所以跳过
        boolean showStatusAnalysis = "ALL".equalsIgnoreCase(statusFilter);

        housekeepingCLI.displayHousekeepingStatusSummary(
                result.getNumberOfEntries(),
                availableCount,
                occupiedCount,
                needsCleaningCount,
                cleaningInProgressCount,
                inspectedCount,
                showStatusAnalysis);

        housekeepingCLI.displayRoomTypeBreakdownHeader();
        housekeepingCLI.displayRoomTypeBreakdown(
                "Standard", standardTotal, standardAvailable, showStatusAnalysis);
        housekeepingCLI.displayRoomTypeBreakdown(
                "Deluxe", deluxeTotal, deluxeAvailable, showStatusAnalysis);
        housekeepingCLI.displayRoomTypeBreakdown(
                "Suite", suiteTotal, suiteAvailable, showStatusAnalysis);

        housekeepingCLI.displayReportEnd();
    }

    private void sortRoomsByRoomNumber(
            ListInterface<Room> rooms) {

        int n =
                rooms.getNumberOfEntries();

        for (int i = 1;
                i <= n - 1;
                i++) {

            int smallestPosition = i;

            for (int j = i + 1;
                    j <= n;
                    j++) {

                Room currentSmallest =
                        rooms.getEntry(
                                smallestPosition
                        );

                Room candidate =
                        rooms.getEntry(j);

                if (candidate
                        .getRoomNumber()
                        .compareToIgnoreCase(
                                currentSmallest
                                        .getRoomNumber()
                        ) < 0) {

                    smallestPosition = j;
                }
            }

            if (smallestPosition != i) {

                Room temp =
                        rooms.getEntry(i);

                rooms.replace(
                        i,
                        rooms.getEntry(
                                smallestPosition
                        )
                );

                rooms.replace(
                        smallestPosition,
                        temp
                );
            }
        }
    }

    // =========================================================
    // Option 6
    // Room History Activity Report
    // =========================================================

    void doGenerateRoomHistoryReport() {

        String roomTypeFilter = promptValidReportRoomType();
        if (roomTypeFilter == null) {
            housekeepingCLI.displayCancelled();
            return;
        }

        int minimumRollbacks = promptValidMinimumRollbacks();
        if (minimumRollbacks == Integer.MIN_VALUE) {
            housekeepingCLI.displayCancelled();
            return;
        }

        ListInterface<Room> resultRooms = new ArrayBasedList<>();
        ListInterface<Integer> updateCounts = new ArrayBasedList<>();
        ListInterface<Integer> rollbackCounts = new ArrayBasedList<>();

        /*
         * Linear Search
         *
         * Filter 1 = Room Type
         * Filter 2 = Minimum Rollbacks
         * Filter 3 = 只收真的动过状态的房间(updates > 0);从开机到现在什么都没
         *            发生的房间放进"活动报表"里只会把表格撑长,没有资讯量
         */
        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {

            Room room = roomList.getEntry(i);

            boolean roomTypeMatches =
                    "ALL".equalsIgnoreCase(roomTypeFilter)
                    || room.getRoomType().equalsIgnoreCase(roomTypeFilter);

            if (!roomTypeMatches) {
                continue;
            }

            int rollbacks = countRollbacksForRoom(room.getRoomNumber());

            if (rollbacks < minimumRollbacks) {
                continue;
            }

            int updates = countStatusUpdates(room, rollbacks);

            if (updates == 0) {
                continue;
            }

            resultRooms.add(room);
            updateCounts.add(updates);
            rollbackCounts.add(rollbacks);
        }

        /*
         * Merge Sort: 回滚次数由多到少;次数一样时房号小的排前面。
         * 排序键是回滚次数,因为这份报表要回答的是"哪间房最常出错"。
         */
        sortHistoryReport(resultRooms, updateCounts, rollbackCounts);

        housekeepingCLI.displayRoomHistoryReportHeader(
                roomTypeFilter, minimumRollbacks);

        if (resultRooms.isEmpty()) {
            housekeepingCLI.displayNoReportRecords();
            housekeepingCLI.displayReportEnd();
            return;
        }

        int totalUpdates = 0;
        int totalRollbacks = 0;

        for (int i = 1; i <= resultRooms.getNumberOfEntries(); i++) {

            Room room = resultRooms.getEntry(i);
            int updates = updateCounts.getEntry(i);
            int rollbacks = rollbackCounts.getEntry(i);

            totalUpdates = totalUpdates + updates;
            totalRollbacks = totalRollbacks + rollbacks;

            housekeepingCLI.displayRoomHistoryReportRow(
                    room.getRoomNumber(),
                    room.getRoomType(),
                    room.getStatus(),
                    updates,
                    rollbacks);
        }

        // 排序后第一笔就是回滚最多的那间房
        housekeepingCLI.displayRoomHistorySummary(
                resultRooms.getNumberOfEntries(),
                totalUpdates,
                totalRollbacks,
                resultRooms.getEntry(1).getRoomNumber(),
                rollbackCounts.getEntry(1));

        housekeepingCLI.displayReportEnd();
    }

    /**
     * 数这间房在 rollbackLog 里出现几次。
     *
     * 为什么不看 statusStack:回滚会 pop 掉栈里那一笔,证据就消失了。
     * rollbackLog 是只增不删的流水帐,只有它记得住这间房被回滚过几次。
     */
    private int countRollbacksForRoom(String roomNumber) {

        int count = 0;

        for (int i = 1; i <= rollbackLog.getNumberOfEntries(); i++) {
            if (rollbackLog.getEntry(i).getRoomNumber()
                    .equalsIgnoreCase(roomNumber)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 还原这间房总共被记录过几次状态更新。
     *
     * 栈里现在剩几笔 = 推进过几次 - 回滚掉几次,反过来解:
     *     更新次数 = (栈里笔数 - 1) + 回滚次数
     * 减 1 是因为最底那笔是开机时的初始状态,不算一次"更新"。
     *
     * 注意:栈的容量上限是 MAX_HISTORY_RECORDS,满了之后最旧的会被挤掉,
     * 那种情况这个数字会少算。以本系统的资料量不会发生。
     */
    private int countStatusUpdates(Room room, int rollbacks) {

        int entries = room.getRoomHistory().getStatusStack().size();
        int pushesKept = (entries > 0) ? entries - 1 : 0;

        return pushesKept + rollbacks;
    }

    /**
     * 报表2的排序:回滚次数由多到少;次数一样时,房号小的排前面。
     *
     * 用 merge sort(分治)而不是 selection sort:
     * - 把区间对半切到只剩一个元素(天然有序),再两两合并回来,O(n log n);
     *   selection sort 每一轮都要扫过剩下全部元素找最大的,是 O(n^2)
     * - merge sort 是稳定排序:合并时"分不出先后"的情况优先取左半边,
     *   原本的相对顺序不会被打乱
     *
     * 这里排的是三条并行清单(房间 / 更新次数 / 回滚次数),合并时必须一起搬,
     * 否则第 i 笔的房号会对到别间房的数字。
     */
    private void sortHistoryReport(
            ListInterface<Room> rooms,
            ListInterface<Integer> updateCounts,
            ListInterface<Integer> rollbackCounts) {

        mergeSortHistoryReport(
                rooms, updateCounts, rollbackCounts,
                1, rooms.getNumberOfEntries());
    }

    /**
     * 递归把 left..right 这一段排好:先各自排好左右两半,再合并。
     */
    private void mergeSortHistoryReport(
            ListInterface<Room> rooms,
            ListInterface<Integer> updateCounts,
            ListInterface<Integer> rollbackCounts,
            int left,
            int right) {

        // 只剩一个元素(或空的),本身就是有序的,递归到底
        if (left >= right) {
            return;
        }

        int middle = (left + right) / 2;

        mergeSortHistoryReport(rooms, updateCounts, rollbackCounts, left, middle);
        mergeSortHistoryReport(rooms, updateCounts, rollbackCounts, middle + 1, right);

        mergeHistoryReport(rooms, updateCounts, rollbackCounts, left, middle, right);
    }

    /**
     * 合并 left..middle 和 middle+1..right 这两段(各自已经排好序)。
     * 两边各出一个比大小,该排前面的先放进暂存阵列,最后整段抄回清单。
     */
    private void mergeHistoryReport(
            ListInterface<Room> rooms,
            ListInterface<Integer> updateCounts,
            ListInterface<Integer> rollbackCounts,
            int left,
            int middle,
            int right) {

        int size = right - left + 1;

        // 普通 Java 阵列,不是 Collections Framework 的东西
        Room[] mergedRooms = new Room[size];
        int[] mergedUpdates = new int[size];
        int[] mergedRollbacks = new int[size];

        int i = left;          // 左半边目前看到哪
        int j = middle + 1;    // 右半边目前看到哪
        int k = 0;             // 暂存阵列该填第几格

        while (i <= middle && j <= right) {

            if (comesFirstInHistoryReport(
                    rooms.getEntry(i), rollbackCounts.getEntry(i),
                    rooms.getEntry(j), rollbackCounts.getEntry(j))) {

                mergedRooms[k] = rooms.getEntry(i);
                mergedUpdates[k] = updateCounts.getEntry(i);
                mergedRollbacks[k] = rollbackCounts.getEntry(i);
                i++;

            } else {

                mergedRooms[k] = rooms.getEntry(j);
                mergedUpdates[k] = updateCounts.getEntry(j);
                mergedRollbacks[k] = rollbackCounts.getEntry(j);
                j++;
            }
            k++;
        }

        // 其中一边先走完,另一边剩下的直接接上去(它们本来就是排好的)
        while (i <= middle) {
            mergedRooms[k] = rooms.getEntry(i);
            mergedUpdates[k] = updateCounts.getEntry(i);
            mergedRollbacks[k] = rollbackCounts.getEntry(i);
            i++;
            k++;
        }

        while (j <= right) {
            mergedRooms[k] = rooms.getEntry(j);
            mergedUpdates[k] = updateCounts.getEntry(j);
            mergedRollbacks[k] = rollbackCounts.getEntry(j);
            j++;
            k++;
        }

        for (k = 0; k < size; k++) {
            rooms.replace(left + k, mergedRooms[k]);
            updateCounts.replace(left + k, mergedUpdates[k]);
            rollbackCounts.replace(left + k, mergedRollbacks[k]);
        }
    }

    /**
     * 排序规则:回滚次数多的排前面;次数一样时,房号小的排前面。
     *
     * @return true 代表 a 该排在 b 前面
     */
    private boolean comesFirstInHistoryReport(
            Room roomA, int rollbacksA,
            Room roomB, int rollbacksB) {

        if (rollbacksA != rollbacksB) {
            return rollbacksA > rollbacksB;
        }

        return roomA.getRoomNumber()
                .compareToIgnoreCase(roomB.getRoomNumber()) <= 0;
    }

    // =========================================================
    // Stack History Management
    // =========================================================

    private void prepareHistoryForPush(
            RoomHistory history) {

        if (history.getStatusStack().size()
                >= MAX_HISTORY_RECORDS) {

            removeOldestStatus(
                    history.getStatusStack()
            );
        }
    }

    private void removeOldestStatus(
            StackInterface<String> statusStack) {

        StackInterface<String> temporaryStack =
                new LinkedStack<>();

        while (statusStack.size() > 1) {

            temporaryStack.push(
                    statusStack.pop()
            );
        }

        /*
         * Remove oldest.
         */
        statusStack.pop();

        /*
         * Restore newer records.
         */
        while (!temporaryStack.isEmpty()) {

            statusStack.push(
                    temporaryStack.pop()
            );
        }
    }

    // =========================================================
    // Input Retry (format-class validation failures re-prompt
    // in place instead of aborting the whole operation)
    // =========================================================

    /**
     * housekeepingCLI.promptNewStatus()空白回传null代表取消;非空白但不是1~4
     * 回传""(不是null)代表选项无效,要重问——跟"选了合法选项但转换本身不合法"
     * (isValidStatusTransition为false)一样都要重问,只有真正空白才取消。
     */
    private String promptValidNewStatus(String previousStatus) {

        String newStatus;

        while (true) {

            newStatus = housekeepingCLI.promptNewStatus(
                    nextValidStatus(previousStatus)
            );

            if (newStatus == null) {
                return null;
            }

            if (!newStatus.isEmpty() && isValidStatusTransition(previousStatus, newStatus)) {
                return newStatus;
            }

            housekeepingCLI.displayInvalidStatus();
        }
    }

    private String promptValidReportRoomType() {

        String roomTypeFilter;

        while (true) {

            roomTypeFilter = housekeepingCLI.promptReportRoomType();

            if (roomTypeFilter == null) {
                return null;
            }

            if (!roomTypeFilter.isEmpty()) {
                return roomTypeFilter;
            }

            housekeepingCLI.displayInvalidReportFilter();
        }
    }

    private String promptValidReportStatus() {

        String statusFilter;

        while (true) {

            statusFilter = housekeepingCLI.promptReportStatus();

            if (statusFilter == null) {
                return null;
            }

            if (!statusFilter.isEmpty()) {
                return statusFilter;
            }

            housekeepingCLI.displayInvalidReportFilter();
        }
    }

    private int promptValidMinimumRollbacks() {

        int minimumRollbacks;

        while (true) {

            minimumRollbacks = housekeepingCLI.promptMinimumRollbacks();

            if (minimumRollbacks == Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }

            if (minimumRollbacks >= 0) {
                return minimumRollbacks;
            }

            housekeepingCLI.displayInvalidReportFilter();
        }
    }

    // =========================================================
    // Linear Search
    // =========================================================

    private Room findRoom(String roomNumber) {

        if (roomNumber == null) {
            return null;
        }

        for (int i = 1;
                i <= roomList.getNumberOfEntries();
                i++) {

            Room room =
                    roomList.getEntry(i);

            if (room.getRoomNumber()
                    .equalsIgnoreCase(
                            roomNumber)) {

                return room;
            }
        }

        return null;
    }
}
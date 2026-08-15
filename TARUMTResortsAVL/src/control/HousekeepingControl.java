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

                case 5:
                    doGenerateHousekeepingStatusReport();
                    housekeepingCLI.promptContinue();
                    break;

                case 6:
                    doGenerateRoomHistoryReport();
                    housekeepingCLI.promptContinue();
                    break;

                case 7:
                    doGenerateRollbackFrequencyReport();
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

        housekeepingCLI.displayHousekeepingRooms(
                housekeepingRooms.getIterator()
        );
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

        String roomNumber =
                housekeepingCLI.promptRoomNumber();

        Room room = findRoom(roomNumber);

        if (room == null) {

            housekeepingCLI.displayRoomNotFound(
                    roomNumber
            );

            return;
        }

        String previousStatus =
                room.getStatus();

        housekeepingCLI.displayCurrentRoomStatus(
                room.getRoomNumber(),
                previousStatus
        );

        if (!isHousekeepingStatus(previousStatus)) {

            housekeepingCLI.displayInvalidStatus();
            return;
        }

        String newStatus =
                housekeepingCLI.promptNewStatus();

        if (newStatus == null) {

            housekeepingCLI.displayInvalidStatus();
            return;
        }

        if (!isValidStatusTransition(
                previousStatus,
                newStatus)) {

            housekeepingCLI.displayInvalidStatus();
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
                roomNumber,
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

        String roomNumber =
                housekeepingCLI.promptRoomNumber();

        Room room = findRoom(roomNumber);

        if (room == null) {

            housekeepingCLI.displayRoomNotFound(
                    roomNumber
            );

            return;
        }

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

        String roomNumber =
                housekeepingCLI.promptRoomNumber();

        Room room = findRoom(roomNumber);

        if (room == null) {

            housekeepingCLI.displayRoomNotFound(
                    roomNumber
            );

            return;
        }

        RoomHistory history =
                room.getRoomHistory();

        housekeepingCLI.displayRoomStatusHistory(
                roomNumber,
                room.getStatus(),
                history.getStatusStack()
                        .getIterator()
        );
    }

    // =========================================================
    // Option 5
    // Housekeeping Status Report
    // =========================================================

    private void doGenerateHousekeepingStatusReport() {

        String roomTypeFilter =
                housekeepingCLI
                        .promptReportRoomType();

        if (roomTypeFilter == null) {

            housekeepingCLI
                    .displayInvalidReportFilter();

            return;
        }

        String statusFilter =
                housekeepingCLI
                        .promptReportStatus();

        if (statusFilter == null) {

            housekeepingCLI
                    .displayInvalidReportFilter();

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

        housekeepingCLI
                .displayHousekeepingStatusReport(
                        result.getIterator(),
                        roomTypeFilter,
                        statusFilter
                );
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

    private void doGenerateRoomHistoryReport() {

        String roomTypeFilter =
                housekeepingCLI
                        .promptReportRoomType();

        if (roomTypeFilter == null) {

            housekeepingCLI
                    .displayInvalidReportFilter();

            return;
        }

        int minimumRecords =
                housekeepingCLI
                        .promptMinimumHistoryRecords();

        if (minimumRecords < 0) {

            housekeepingCLI
                    .displayInvalidReportFilter();

            return;
        }

        ListInterface<Room> resultRooms =
                new ArrayBasedList<>();

        ListInterface<Integer> recordCounts =
                new ArrayBasedList<>();

        /*
         * Linear Search
         *
         * Filter 1 = Room Type
         * Filter 2 = Minimum History Records
         */
        for (int i = 1;
                i <= roomList.getNumberOfEntries();
                i++) {

            Room room =
                    roomList.getEntry(i);

            int numberOfRecords =
                    room.getRoomHistory()
                            .getStatusStack()
                            .size();

            boolean roomTypeMatches =
                    "ALL".equalsIgnoreCase(
                            roomTypeFilter)
                    || room.getRoomType()
                            .equalsIgnoreCase(
                                    roomTypeFilter
                            );

            boolean recordCountMatches =
                    numberOfRecords
                    >= minimumRecords;

            if (roomTypeMatches
                    && recordCountMatches) {

                resultRooms.add(room);

                recordCounts.add(
                        numberOfRecords
                );
            }
        }

        /*
         * Selection Sort:
         * highest number of records first.
         */
        sortHistoryReport(
                resultRooms,
                recordCounts
        );

        housekeepingCLI
                .displayRoomHistoryReportHeader(
                        roomTypeFilter,
                        minimumRecords
                );

        if (resultRooms.isEmpty()) {

            housekeepingCLI
                    .displayNoReportRecords();

            housekeepingCLI
                    .displayReportEnd();

            return;
        }

        for (int i = 1;
                i <= resultRooms
                        .getNumberOfEntries();
                i++) {

            housekeepingCLI
                    .displayRoomHistoryReportRow(
                            resultRooms.getEntry(i),
                            recordCounts.getEntry(i)
                    );
        }

        housekeepingCLI.displayReportEnd();
    }

    private void sortHistoryReport(
            ListInterface<Room> rooms,
            ListInterface<Integer> recordCounts) {

        int n =
                rooms.getNumberOfEntries();

        for (int i = 1;
                i <= n - 1;
                i++) {

            int largestPosition = i;

            for (int j = i + 1;
                    j <= n;
                    j++) {

                int largestCount =
                        recordCounts.getEntry(
                                largestPosition
                        );

                int candidateCount =
                        recordCounts.getEntry(j);

                /*
                 * Higher record count first.
                 */
                if (candidateCount
                        > largestCount) {

                    largestPosition = j;

                } else if (candidateCount
                        == largestCount) {

                    /*
                     * Tie:
                     * smaller room number first.
                     */
                    Room candidateRoom =
                            rooms.getEntry(j);

                    Room largestRoom =
                            rooms.getEntry(
                                    largestPosition
                            );

                    if (candidateRoom
                            .getRoomNumber()
                            .compareToIgnoreCase(
                                    largestRoom
                                            .getRoomNumber()
                            ) < 0) {

                        largestPosition = j;
                    }
                }
            }

            if (largestPosition != i) {

                /*
                 * Swap Room.
                 */
                Room tempRoom =
                        rooms.getEntry(i);

                rooms.replace(
                        i,
                        rooms.getEntry(
                                largestPosition
                        )
                );

                rooms.replace(
                        largestPosition,
                        tempRoom
                );

                /*
                 * Swap corresponding history count.
                 */
                Integer tempCount =
                        recordCounts.getEntry(i);

                recordCounts.replace(
                        i,
                        recordCounts.getEntry(
                                largestPosition
                        )
                );

                recordCounts.replace(
                        largestPosition,
                        tempCount
                );
            }
        }
    }

    // =========================================================
    // Report 3
    // Rollback Frequency Report
    //
    // Answers "which room/shift keeps getting rolled back" --
    // something the Stack-based history alone cannot answer,
    // because a pop() destroys the evidence. This report reads
    // only from rollbackLog, the permanent, append-only record
    // of every rollback that actually succeeded.
    // =========================================================

    private void doGenerateRollbackFrequencyReport() {

        String roomNumberFilter =
                housekeepingCLI.promptReportRoomNumber();

        String fromDate =
                housekeepingCLI.promptReportFromDate();

        String toDate =
                housekeepingCLI.promptReportToDate();

        ListInterface<String> roomNumbers =
                new ArrayBasedList<>();

        ListInterface<Integer> rollbackCounts =
                new ArrayBasedList<>();

        /*
         * Linear Search + group-by-room, built without a Map:
         * scan rollbackLog once, and for each entry either bump
         * an existing room's counter or append a new one.
         */
        for (int i = 1;
                i <= rollbackLog.getNumberOfEntries();
                i++) {

            RollbackLogEntry entry =
                    rollbackLog.getEntry(i);

            boolean roomMatches =
                    "ALL".equalsIgnoreCase(roomNumberFilter)
                    || entry.getRoomNumber()
                            .equalsIgnoreCase(roomNumberFilter);

            boolean dateMatches =
                    entry.getDate().compareTo(fromDate) >= 0
                    && entry.getDate().compareTo(toDate) <= 0;

            if (!roomMatches || !dateMatches) {
                continue;
            }

            int existingPosition =
                    roomNumbers.indexOf(entry.getRoomNumber());

            if (existingPosition == -1) {
                roomNumbers.add(entry.getRoomNumber());
                rollbackCounts.add(1);
            } else {
                int updatedCount =
                        rollbackCounts.getEntry(existingPosition) + 1;
                rollbackCounts.replace(existingPosition, updatedCount);
            }
        }

        /*
         * Selection Sort:
         * highest rollback count first.
         */
        sortRollbackReport(roomNumbers, rollbackCounts);

        housekeepingCLI.displayRollbackFrequencyReportHeader(
                roomNumberFilter, fromDate, toDate);

        if (roomNumbers.isEmpty()) {
            housekeepingCLI.displayNoReportRecords();
            housekeepingCLI.displayReportEnd();
            return;
        }

        for (int i = 1; i <= roomNumbers.getNumberOfEntries(); i++) {
            housekeepingCLI.displayRollbackFrequencyReportRow(
                    roomNumbers.getEntry(i),
                    rollbackCounts.getEntry(i));
        }

        housekeepingCLI.displayReportEnd();
    }

    private void sortRollbackReport(
            ListInterface<String> roomNumbers,
            ListInterface<Integer> rollbackCounts) {

        int n = roomNumbers.getNumberOfEntries();

        for (int i = 1; i <= n - 1; i++) {

            int largestPosition = i;

            for (int j = i + 1; j <= n; j++) {

                int largestCount =
                        rollbackCounts.getEntry(largestPosition);
                int candidateCount =
                        rollbackCounts.getEntry(j);

                if (candidateCount > largestCount) {
                    largestPosition = j;
                } else if (candidateCount == largestCount) {
                    // Tie: smaller room number first.
                    if (roomNumbers.getEntry(j).compareToIgnoreCase(
                            roomNumbers.getEntry(largestPosition)) < 0) {
                        largestPosition = j;
                    }
                }
            }

            if (largestPosition != i) {

                String tempRoom = roomNumbers.getEntry(i);
                roomNumbers.replace(i, roomNumbers.getEntry(largestPosition));
                roomNumbers.replace(largestPosition, tempRoom);

                Integer tempCount = rollbackCounts.getEntry(i);
                rollbackCounts.replace(i, rollbackCounts.getEntry(largestPosition));
                rollbackCounts.replace(largestPosition, tempCount);
            }
        }
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
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
 * Controls the main operations of the Housekeeping and Task Log module.
 * Room status history is managed using a stack, while room and rollback
 * records are stored in lists.
 *
 * @author Hoo Theng Qin
 */
public class HousekeepingControl {

    private static final int MAX_HISTORY_RECORDS = 20;

    private final HousekeepingCLI housekeepingCLI;
    private final ListInterface<Room> roomList;

    private final VipAllocationControl vipAllocationControl;
    private final WalkInControl walkInControl;

    // Stores successful rollback events for the room history report.
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
                    "HousekeepingControl dependencies cannot be null");
        }

        this.housekeepingCLI = housekeepingCLI;
        this.roomList = roomList;
        this.vipAllocationControl = vipAllocationControl;
        this.walkInControl = walkInControl;
        this.rollbackLog = rollbackLog;
    }

    // Main Menu

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

    // Front Desk Integration

    /**
     * Marks a checked-out room as needing cleaning.
     * If another module changed the room status, the current status is
     * recorded before NEEDS_CLEANING is added to the history stack.
     */

    public void markRoomNeedsCleaning(String roomNumber) {

        Room room = findRoom(roomNumber);

        if (room == null) {
            return;
        }

        RoomHistory history = room.getRoomHistory();

        String latestHistoryStatus = history.getStatusStack().peek();

        // Synchronize the stack with the room's current status before
        // recording the housekeeping transition.
        if (latestHistoryStatus == null
                || !latestHistoryStatus.equals(
                        room.getStatus())) {

            prepareHistoryForPush(history);

            history.getStatusStack().push(
                    room.getStatus());
        }

        latestHistoryStatus = history.getStatusStack().peek();

        if (!"NEEDS_CLEANING".equals(
                latestHistoryStatus)) {

            prepareHistoryForPush(history);

            history.getStatusStack().push(
                    "NEEDS_CLEANING");
        }

        room.setStatus("NEEDS_CLEANING");
    }

    // Option 1: View Rooms Requiring Housekeeping

    private void doViewHousekeepingRooms() {

        housekeepingCLI.displayHousekeepingRooms(
                buildHousekeepingRoomList().getIterator());
    }

    /**
     * Builds a list of rooms currently in the housekeeping pipeline.
     * Used when viewing or updating rooms that require housekeeping.
     */
    private ListInterface<Room> buildHousekeepingRoomList() {

        ListInterface<Room> housekeepingRooms = new ArrayBasedList<>();

        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {

            Room room = roomList.getEntry(i);

            if (isHousekeepingStatus(
                    room.getStatus())) {

                housekeepingRooms.add(room);
            }
        }

        return housekeepingRooms;
    }

    /**
     * Builds a list of all rooms for operations that are not limited
     * to the housekeeping pipeline.
     */
    private ListInterface<Room> buildAllRoomsList() {

        ListInterface<Room> allRooms = new ArrayBasedList<>();

        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {

            allRooms.add(roomList.getEntry(i));
        }

        return allRooms;
    }

    /**
     * Re-prompts until a valid room number is entered.
     * Invalid formats and unknown room numbers are retried,
     * while blank input cancels the operation.
     *
     * @return the selected room, or null if cancelled
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

    // Option 2: Update Room Status

    private void doUpdateRoomStatus() {

        housekeepingCLI.displayHousekeepingRooms(
                buildHousekeepingRoomList().getIterator());

        Room room = promptValidRoom();

        if (room == null) {
            housekeepingCLI.displayCancelled();
            return;
        }

        String previousStatus = room.getStatus();

        housekeepingCLI.displayCurrentRoomStatus(
                room.getRoomNumber(),
                previousStatus);

        if (!isHousekeepingStatus(previousStatus)) {

            housekeepingCLI.displayRoomNotInPipeline(
                    room.getRoomNumber(),
                    previousStatus);
            return;
        }

        String newStatus = promptValidNewStatus(previousStatus);

        if (newStatus == null) {
            housekeepingCLI.displayCancelled();
            return;
        }

        RoomHistory history = room.getRoomHistory();

        String latestHistoryStatus = history.getStatusStack().peek();

        /*
         * Make sure the current status exists
         * at the top of the history Stack.
         */
        if (latestHistoryStatus == null
                || !latestHistoryStatus.equals(
                        previousStatus)) {

            prepareHistoryForPush(history);

            history.getStatusStack().push(
                    previousStatus);
        }

        /*
         * Push new status.
         */
        prepareHistoryForPush(history);

        history.getStatusStack().push(
                newStatus);

        room.setStatus(newStatus);

        housekeepingCLI.displayStatusUpdated(
                room.getRoomNumber(),
                previousStatus,
                newStatus);

        /*
         * Cleaning completed.
         */
        if (newStatus.equalsIgnoreCase(
                "AVAILABLE")) {

            String roomType = room.getRoomType();

            /*
             * VIP priority first.
             */
            vipAllocationControl.tryAllocate(
                    roomType);

            /*
             * Then Walk-In.
             */
            walkInControl.tryAllocate(
                    roomType);
        }
    }

    /**
     * Returns the next valid status in the linear housekeeping pipeline.
     *
     * @param currentStatus the room's current status
     * @return the next valid status, or null if no transition is available
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
                    "CLEANING_IN_PROGRESS");
        }

        if (currentStatus.equalsIgnoreCase(
                "CLEANING_IN_PROGRESS")) {

            return newStatus.equalsIgnoreCase(
                    "INSPECTED");
        }

        if (currentStatus.equalsIgnoreCase(
                "INSPECTED")) {

            return newStatus.equalsIgnoreCase(
                    "AVAILABLE");
        }

        return false;
    }

    // Option 3: Roll Back Latest Status

    private void doRollbackStatus() {

        housekeepingCLI.displayAllRooms(
                buildAllRoomsList().getIterator());

        Room room = promptValidRoom();

        if (room == null) {
            housekeepingCLI.displayCancelled();
            return;
        }

        String roomNumber = room.getRoomNumber();

        RoomHistory history = room.getRoomHistory();

        String latestHistoryStatus = history.getStatusStack().peek();

        // Rollback is allowed only when the stack still matches the room's
        // current status. A mismatch means another module changed the room.
        if (latestHistoryStatus == null
                || !latestHistoryStatus.equals(
                        room.getStatus())) {

            housekeepingCLI
                    .displayRollbackNotAvailable(
                            roomNumber);

            return;
        }

        if (history.getStatusStack().size() <= 1) {

            housekeepingCLI
                    .displayRollbackNotAvailable(
                            roomNumber);

            return;
        }

        // LIFO: remove the latest status.
        String removedStatus = history.getStatusStack().pop();

        // The previous status becomes the new top.
        String restoredStatus = history.getStatusStack().peek();

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
                    removedStatus);

            housekeepingCLI
                    .displayRollbackNotAvailable(
                            roomNumber);

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
                LocalDate.now().toString()));

        housekeepingCLI.displayRollbackResult(
                roomNumber,
                removedStatus,
                restoredStatus);
    }

    // Option 4: View Room Status History
    private void doViewStatusHistory() {

        housekeepingCLI.displayAllRooms(
                buildAllRoomsList().getIterator());

        Room room = promptValidRoom();

        if (room == null) {
            housekeepingCLI.displayCancelled();
            return;
        }

        RoomHistory history = room.getRoomHistory();

        housekeepingCLI.displayRoomStatusHistory(
                room.getRoomNumber(),
                room.getStatus(),
                history.getStatusStack()
                        .getIterator());
    }

    // Report 1: Housekeeping Status Report

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

        ListInterface<Room> result = new ArrayBasedList<>();

        /*
         * Linear Search
         *
         * Filter 1 = Room Type
         * Filter 2 = Room Status
         */
        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {

            Room room = roomList.getEntry(i);

            boolean roomTypeMatches = "ALL".equalsIgnoreCase(
                    roomTypeFilter)
                    || room.getRoomType()
                            .equalsIgnoreCase(
                                    roomTypeFilter);

            boolean statusMatches = "ALL".equalsIgnoreCase(
                    statusFilter)
                    || room.getStatus()
                            .equalsIgnoreCase(
                                    statusFilter);

            if (roomTypeMatches
                    && statusMatches) {

                result.add(room);
            }
        }

        // Sort matching rooms by room number in ascending order using selection sort.
        sortRoomsByRoomNumber(result);

        housekeepingCLI.displayHousekeepingStatusReportHeader(
                roomTypeFilter, statusFilter);

        if (result.isEmpty()) {
            housekeepingCLI.displayNoReportRecords();
            housekeepingCLI.displayReportEnd();
            return;
        }

        // Collect summary totals while displaying the filtered rooms
        // to avoid scanning the result list again.
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

        // Detailed status analysis is useful only when all statuses are included.
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

    /**
     * Sorts rooms by room number in ascending order using selection sort.
     *
     * @param rooms the list of rooms to sort
     */
    private void sortRoomsByRoomNumber(
            ListInterface<Room> rooms) {

        int n = rooms.getNumberOfEntries();

        for (int i = 1; i <= n - 1; i++) {

            int smallestPosition = i;

            for (int j = i + 1; j <= n; j++) {

                Room currentSmallest = rooms.getEntry(
                        smallestPosition);

                Room candidate = rooms.getEntry(j);

                if (candidate
                        .getRoomNumber()
                        .compareToIgnoreCase(
                                currentSmallest
                                        .getRoomNumber()) < 0) {

                    smallestPosition = j;
                }
            }

            if (smallestPosition != i) {

                Room temp = rooms.getEntry(i);

                rooms.replace(
                        i,
                        rooms.getEntry(
                                smallestPosition));

                rooms.replace(
                        smallestPosition,
                        temp);
            }
        }
    }

    // Report 2: Room History Activity Report

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

        // Linear search: keep rooms matching the room type and minimum
        // rollback filters. Rooms with no recorded activity are excluded.
        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {

            Room room = roomList.getEntry(i);

            boolean roomTypeMatches = "ALL".equalsIgnoreCase(roomTypeFilter)
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

        // Sort by rollback count in descending order.
        // Room number is used as the tie-breaker.
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

        // After sorting, the first room has the highest rollback count.
        housekeepingCLI.displayRoomHistorySummary(
                resultRooms.getNumberOfEntries(),
                totalUpdates,
                totalRollbacks,
                resultRooms.getEntry(1).getRoomNumber(),
                rollbackCounts.getEntry(1));

        housekeepingCLI.displayReportEnd();
    }

    /**
     * Counts successful rollback events recorded for a room.
     * The rollback log is used because popped stack entries are no longer
     * available in the status history.
     *
     * @param roomNumber the room number to check
     * @return the number of recorded rollbacks
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
     * Estimates the number of recorded status updates.
     *
     * updates = (current stack entries - 1) + successful rollbacks
     *
     * The first stack entry is the initial room status and is not an update.
     * The result may be lower after old history entries are removed because
     * of MAX_HISTORY_RECORDS.
     */
    private int countStatusUpdates(Room room, int rollbacks) {

        int entries = room.getRoomHistory().getStatusStack().size();
        int pushesKept = (entries > 0) ? entries - 1 : 0;

        return pushesKept + rollbacks;
    }

    /**
     * Sorts the room history report using merge sort.
     * Rooms with more rollbacks come first, with room number as the tie-breaker.
     * Parallel lists are moved together to keep room and count data aligned.
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
     * Recursively sorts the selected range using merge sort.
     */
    private void mergeSortHistoryReport(
            ListInterface<Room> rooms,
            ListInterface<Integer> updateCounts,
            ListInterface<Integer> rollbackCounts,
            int left,
            int right) {

        // A range with zero or one element is already sorted.
        if (left >= right) {
            return;
        }

        int middle = (left + right) / 2;

        mergeSortHistoryReport(rooms, updateCounts, rollbackCounts, left, middle);
        mergeSortHistoryReport(rooms, updateCounts, rollbackCounts, middle + 1, right);

        mergeHistoryReport(rooms, updateCounts, rollbackCounts, left, middle, right);
    }

    /**
     * Merges two sorted ranges while keeping the parallel lists aligned.
     */
    private void mergeHistoryReport(
            ListInterface<Room> rooms,
            ListInterface<Integer> updateCounts,
            ListInterface<Integer> rollbackCounts,
            int left,
            int middle,
            int right) {

        int size = right - left + 1;

        Room[] mergedRooms = new Room[size];
        int[] mergedUpdates = new int[size];
        int[] mergedRollbacks = new int[size];

        int i = left;
        int j = middle + 1;
        int k = 0;

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

        // Append any remaining entries from either half.
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
     * Compares two rooms using the history report ordering.
     * Higher rollback counts come first, followed by room number.
     *
     * @return true if roomA should appear before roomB
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

    // Stack History Management

    /**
     * Makes space before adding a new history entry.
     * The oldest status is removed when the history limit is reached.
     */

    private void prepareHistoryForPush(
            RoomHistory history) {

        if (history.getStatusStack().size() >= MAX_HISTORY_RECORDS) {

            removeOldestStatus(
                    history.getStatusStack());
        }
    }

    /**
     * Removes the oldest entry from a stack while preserving newer entries.
     * A temporary stack is used because the oldest entry cannot be accessed
     * directly from the top.
     */
    private void removeOldestStatus(
            StackInterface<String> statusStack) {

        StackInterface<String> temporaryStack = new LinkedStack<>();

        while (statusStack.size() > 1) {

            temporaryStack.push(
                    statusStack.pop());
        }
        // Remove the oldest remaining status.

        statusStack.pop();

        // Restore newer statuses in their original stack order.
        while (!temporaryStack.isEmpty()) {

            statusStack.push(
                    temporaryStack.pop());
        }
    }

    // Input Validation

    /**
     * Re-prompts until a valid status transition is selected.
     * null represents cancellation, while invalid non-blank input is retried.
     *
     * @param previousStatus the room's current status
     * @return the selected status, or null if cancelled
     */
    private String promptValidNewStatus(String previousStatus) {

        String newStatus;

        while (true) {

            newStatus = housekeepingCLI.promptNewStatus(
                    nextValidStatus(previousStatus));

            if (newStatus == null) {
                return null;
            }

            if (!newStatus.isEmpty() && isValidStatusTransition(previousStatus, newStatus)) {
                return newStatus;
            }

            housekeepingCLI.displayInvalidStatus();
        }
    }

    /**
     * Re-prompts until a valid room type filter is selected.
     *
     * @return the selected filter, or null if cancelled
     */

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

    /**
     * Re-prompts until a valid room status filter is selected.
     *
     * @return the selected filter, or null if cancelled
     */
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

    /**
     * Re-prompts until a non-negative rollback threshold is entered.
     *
     * @return the threshold, or Integer.MIN_VALUE if cancelled
     */
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

    // Linear Search

    /**
     * Searches for a room by room number using a linear scan.
     * The search takes O(n) time in the worst case.
     *
     * @param roomNumber the room number to find
     * @return the matching room, or null if not found
     */
    private Room findRoom(String roomNumber) {

        if (roomNumber == null) {
            return null;
        }

        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {

            Room room = roomList.getEntry(i);

            if (room.getRoomNumber()
                    .equalsIgnoreCase(
                            roomNumber)) {

                return room;
            }
        }

        return null;
    }
}
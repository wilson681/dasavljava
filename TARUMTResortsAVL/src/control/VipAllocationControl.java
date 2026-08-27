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
 *business logic for Module 2 (VIP & Loyalty Tier Priority Room Allocation).
 * @author Chong Kim Seng
 * Three AVL trees split by room type (Standard/Deluxe/Suite)
 *  when a room becomes available again ltr (checkout/cleaning done), it should also run,
 * housekeeping/checkout not done yet
 */
public class VipAllocationControl {

    // these SLA numbers just for report, dont affect AVL order or allocation
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

    // menu loop, 0 to exit
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

    // Feature 1: VIP Registration

    private void doRegister() {
        // get the member first, blank input = cancel
        // wrong member id just loops and asks again, doesnt kick back to main menu
        Member member = promptValidMember();
        if (member == null) {
            vipAllocationCLI.displayCancelled();
            return;
        }

        // this is the tier to rank conversion, it's what actually drives the priority ordering
        // once the booking gets inserted into the AVL tree
        int tierRank = TierRankUtility.tierToRank(member.getTier());

        // one VIP can book more than one room in a single session, so the member is only looked
        // up once here and the loop below reuses the same confirmationNumber for every room
        String confirmationNumber = null;

        boolean continueBooking = true;
        while (continueBooking) {
            // room type decides which one of the 3 trees this booking belongs to
            String roomType = promptValidRoomType();
            if (roomType == null) {
                break; // blank = no more rooms, stop the loop
            }
            SearchTreeInterface<Booking> tree = getTreeForRoomType(roomType);

            // can happen much later than registration
            int numberOfNights = promptValidNumberOfNights();
            if (numberOfNights == Integer.MIN_VALUE) {
                break;
            }

            // only generate confirmationNumber the first time a room is actually confirmed here,
            // so a cancel before this not burn a number for nothing 
            if (confirmationNumber == null) {
                confirmationCounter++;
                confirmationNumber = String.valueOf(confirmationCounter);
            }

            arrivalCounter++;
            bookingCounter++;
            String bookingId = "VB" + String.format("%06d", bookingCounter);

            // name/phone are just copied from Member for now, Guest record only gets built
            // later in tryAllocate() once a room is actually assigned
            Booking booking = new Booking(bookingId, confirmationNumber, member.getName(),
                    member.getPhone(), member.getMemberId(), roomType, BookingStatus.PENDING,
                    "VIP", arrivalCounter, tierRank, currentTimestamp());
            booking.setNumberOfNights(numberOfNights);

            // this is the actual AVL insert - add() will perform compare and rebalance
            tree.add(booking);
            vipAllocationCLI.displayRegistrationResult(booking, member.getTier());

            // try allocating right away in case a room for this type is already free
            tryAllocate(roomType);

            continueBooking = vipAllocationCLI.promptAddAnotherRoom();
        }
    }

    // Allocation check (not a manual menu action)

    // core allocation logic
    // called after register(), HousekeepingControl also calls this once a room turns AVAILABLE again
    // public cos of that 2nd caller
    // does nothing if nobody waiting or no room free
    public void tryAllocate(String roomType) {
        SearchTreeInterface<Booking> tree = getTreeForRoomType(roomType);
        if (tree == null || tree.isEmpty()) {
            return;
        }

        Room availableRoom = findAvailableRoom(roomType);
        if (availableRoom == null) {
            return;
        }

        // inorder walks smallest to largest, and compareTo() makes smallest = highest priority
        // so first node here is the one to allocate, no need extra search
        Iterator<Booking> priorityIterator = tree.getInorderIterator();
        Booking topPriority = priorityIterator.next();

        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(topPriority.getNumberOfNights());

        // actual handover happens here
        topPriority.setStatus(BookingStatus.CHECKED_IN);
        topPriority.setAssignedRoomNo(availableRoom.getRoomNumber());
        topPriority.setAllocatedAt(currentTimestamp());
        availableRoom.setStatus("OCCUPIED");
        tree.remove(topPriority);

        // guest only created here not at register
        // reuse existing guest entry if got one already (multi room case), dont duplicate
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

        // link booking to guest, front desk (module 4) needs this to list by confirmationNumber
        topPriority.setStayPeriod(checkIn.toString(), checkOut.toString(), topPriority.getNumberOfNights());
        guest.addBooking(topPriority);

        // discount applied here, just estimate for display, real bill settled at checkout
        double originalPrice = availableRoom.getNightlyRate() * topPriority.getNumberOfNights();
        int discountPercent = TierRankUtility.tierToDiscountPercent(member.getTier());
        double finalPrice = originalPrice - (originalPrice * discountPercent / 100.0);

        vipAllocationCLI.displayAllocationResult(topPriority, availableRoom,
                originalPrice, discountPercent, finalPrice);
    }

    // Feature 3: Cancel Waiting 

    private void doCancel() {
        // show waiting list first so user can see which bookingId to cancel
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

        // use bookingId not confirmationNumber, one confirmationNumber can have multiple bookings in tree
        // bookingId not part of compareTo() so cant navigate tree with it, scan inorder instead
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

        // need the real object here not rebuilt, remove() uses tierRank/arrivalSequence to navigate
        target.setStatus(BookingStatus.CANCELLED);
        tree.remove(target);
        vipAllocationCLI.displayCancelResult(true);
    }

    // Feature 4: View VIP Waiting List

    private void doViewWaitingList() {
        String roomType = promptValidRoomType();
        if (roomType == null) {
            vipAllocationCLI.displayCancelled();
            return;
        }
        SearchTreeInterface<Booking> tree = getTreeForRoomType(roomType);

        // inorder already = high priority first, straight to boundary
        vipAllocationCLI.displayWaitingList(roomType, tree.getInorderIterator());
    }

    // Report 1: Live VIP Waiting List Report

    // combine all 3 trees into one list, sort same way as AVL (tier first then earliest arrival)
    // SLA here just for flagging rows, no effect on sort
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
            // -1 = All, cant use 0 as sentinel since 0 is standard's real rank
            if (tierRankFilter == -1 || booking.getTierRankAtRequest() == tierRankFilter) {
                filtered.add(booking);
                int waitMinutes = minutesBetween(booking.getRegisteredAt(), currentTimestamp());
                waitMinutesList.add(Math.max(0, waitMinutes));
            }
        }
    }

    // Report 2: Tier Allocation SLA Compliance Report

    // compare each tier wait time vs SLA target, filtered by allocated date not registered date
    void doTierSlaReport() {
        int tierRankFilter = vipAllocationCLI.promptReportTierRank();
        String fromDate = vipAllocationCLI.promptReportFromDate();
        String toDate = vipAllocationCLI.promptReportToDate();

        // 4 tiers, index = tierRank (diamond3 platinum2 elite1 standard0)
        // standard included too since they can register as vip now
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
                // same -1=All trick as before, cant reuse 0
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

        // keep tiers with data only, rank worst first below
        // no Collections.sort allowed for this project (own ADTs only) so wrote selection sort manually
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

    // worst compliance first, then longer avg wait, then higher tier wins on tie
    // (a struggling diamond matters more than a struggling standard)
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

    // Shared Report Helper Methods

    private int minutesBetween(String start, String end) {
        LocalDateTime startTime = LocalDateTime.parse(start, TIMESTAMP_FORMAT);
        LocalDateTime endTime = LocalDateTime.parse(end, TIMESTAMP_FORMAT);
        return (int) java.time.Duration.between(startTime, endTime).toMinutes();
    }

    // selection sort again, uses compareTo() directly this time
    // swap waitMinutes together with bookings or the 2 lists go out of sync
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

    // Internal Helper Methods

    // fixed length on purpose, LocalTime.toString() drops trailing 0 seconds and messes up parsing in minutesBetween()
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // for registeredAt/allocatedAt + wait time math in reports
    private String currentTimestamp() {
        return LocalDateTime.now().withNano(0).format(TIMESTAMP_FORMAT);
    }

    // Input Retry (invalid input just asks again, doesn't abort the whole operation)

    // keep asking till id matches real member, typo shouldnt back to main menu
    // blank only way to cancel
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

    // map roomType string to correct tree, null if invalid
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

    // memberList has no lookup by id, just a List ADT, loop through manually
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

    // first free room of this type
    // must check equals AVAILABLE exactly not just != OCCUPIED, NEEDS_CLEANING also != OCCUPIED but cant give out
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

    // check if guest already got a record from earlier allocation
    // Guest equals/hashCode only check confirmationNumber so template obj with rest null still works as lookup key
    private Guest findGuestByConfirmationNumber(String confirmationNumber) {
        Guest template = new Guest(confirmationNumber, null, null, null, null, null, null, null, 0);
        return guestTable.getEntry(template);
    }

    // scan tree inorder for matching bookingId
    // must return actual node not rebuilt one, remove() needs real tierRank/sequence to navigate
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

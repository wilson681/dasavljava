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

// WalkInControl.java - business logic for module 1 (Walk-In Registrations & Standard Booking)
//
// @author jagathis
//
// note to self:
// - 3 separate Circular Linked Queues, one per room type (Standard/Deluxe/Suite), independent
// - always check the vip tree before allocating to walk-in - if anyone is waiting in vip
//   for this room type, walk-in gets nothing no matter how long theyve been queuing. vip
//   always wins, same rule module 2 has
// - only validate input for stuff that actually touches the ADT (register, cancel),
//   viewing the list doesnt need it
// - allocation isnt a menu action anymore, its tryAllocate() - runs right after
//   register(), and HousekeepingControl also calls it once a room turns AVAILABLE again
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
        // vip confirmation numbers start at 20000000, walk-in starts at 10000000, keeps
        // the 2 ranges from ever clashing
        this.arrivalCounter = 0;
        this.bookingCounter = 0;
        this.confirmationCounter = 10000000;
        // scan seed member data once (MemberDao already loaded them into memberList by
        // now), continue numbering from there so new ids stay consistent with the M1001
        // etc format - only WalkInControl touches this counter after this point, never
        // rescans, so theres no risk of 2 places generating the same id
        this.memberCounter = computeNextMemberNumber();
    }

    // menu loop, runs til user picks 0
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

    // Feature 1: Register New Guest

    private void doRegister() {
        // walk-in isnt a member, just ask name + phone directly, dont need to look up
        // member data like vip does. bad format (phone not numeric) just re-asks. blank
        // means cancel the whole thing
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

        // guest might book multiple rooms (different types even), so name/phone only
        // asked once, loop below reuses the same confirmationNumber til they say no more
        // rooms
        // confirmationNumber only generated once the first booking actually confirms
        // (see below), not at the start of the loop - else cancelling at the roomType or
        // nights step burns a number for nothing
        String confirmationNumber = null;

        boolean continueBooking = true;
        while (continueBooking) {
            String roomType = promptValidRoomType();
            if (roomType == null) {
                // blank roomType = done adding rooms, same effect as answering n to add
                // another room, no separate cancel message needed
                break;
            }
            QueueInterface<Booking> queue = getQueueForRoomType(roomType);

            // ask nights now while the guest is still here, cant ask later since
            // allocation might happen way after registration once a room frees up
            int numberOfNights = promptValidNumberOfNights();
            if (numberOfNights == Integer.MIN_VALUE) {
                break;
            }

            // reaching here means this booking is really happening, number only
            // generated first time, same guest adding more rooms reuses it
            if (confirmationNumber == null) {
                confirmationCounter++;
                confirmationNumber = String.valueOf(confirmationCounter);
            }

            arrivalCounter++;
            bookingCounter++;
            String bookingId = "WB" + String.format("%06d", bookingCounter);

            // memberId null, tierRank 0 - walk-in has no tier, deliberately left
            // empty/lowest to match the vip side
            Booking booking = new Booking(bookingId, confirmationNumber, name, phone, null,
                    roomType, BookingStatus.PENDING, "WALK_IN", arrivalCounter, 0, currentTimestamp());
            booking.setNumberOfNights(numberOfNights);

            queue.enqueue(booking);
            walkInCLI.displayRegistrationResult(booking);

            // check right after registering if this can be allocated now (no vip
            // waiting, their turn, room free)
            tryAllocate(roomType);

            continueBooking = walkInCLI.promptAddAnotherRoom();
        }
    }

    // Allocation check (not a manual menu action)

    // checks if this room type can allocate right now, if so gives the room to whoever
    // is at the front of the queue
    // called after doRegister(), and HousekeepingControl also calls it once a room goes
    // back to AVAILABLE
    // does nothing quietly if conditions arent met, guest just stays in the queue
    public void tryAllocate(String roomType) {
        QueueInterface<Booking> queue = getQueueForRoomType(roomType);
        SearchTreeInterface<Booking> vipTree = getVipTreeForRoomType(roomType);
        if (queue == null || vipTree == null) {
            return;
        }

        // the golden rule: vip always first. if theres anyone waiting in this room
        // types vip tree, walk-in doesnt move, doesnt matter how long theyve queued
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

        // peek the front first, dont dequeue yet - only actually remove once we know
        // theres a room to give
        Booking frontBooking = queue.getFront();

        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(frontBooking.getNumberOfNights());

        frontBooking.setStatus(BookingStatus.CHECKED_IN);
        frontBooking.setAssignedRoomNo(availableRoom.getRoomNumber());
        frontBooking.setAllocatedAt(currentTimestamp());
        availableRoom.setStatus("OCCUPIED");
        // now actually remove it, allocation confirmed
        queue.dequeue();

        // same guest (same confirmationNumber) mightve already got a Guest record from
        // an earlier room being allocated - cant just new another one (would end up with
        // 2 Guests sharing a key in guestTable, lookup gets confused), add the room to
        // the existing Guest instead
        Guest guest = findGuestByConfirmationNumber(frontBooking.getConfirmationNumber());
        if (guest == null) {
            // reaching this branch means the system has never seen this person before
            // (no memberId yet) - once they actually check in (not just queued), auto
            // enroll them as a Standard member. tierToRank() returns 0 for anything it
            // doesnt recognize including Standard, so having a memberId now doesnt
            // accidentally bump them into vip priority
            // next time this person comes back they should know theyre a member and go
            // through the vip module with their memberId, wont hit this path again so
            // no dedup check needed here
            Member newMember = enrollAsStandardMember(frontBooking.getGuestNameSnapshot(),
                    frontBooking.getPhoneSnapshot());

            guest = new Guest(frontBooking.getConfirmationNumber(), frontBooking.getGuestNameSnapshot(),
                    frontBooking.getPhoneSnapshot(), newMember.getMemberId(), newMember.getTier(),
                    checkIn.toString() + " " + java.time.LocalTime.now().withNano(0).toString(),
                    checkIn.toString(), checkOut.toString(), frontBooking.getNumberOfNights());
            guestTable.add(guest);
        }
        guest.addRoom(availableRoom.getRoomNumber());

        // record the stay period on the booking itself and link it to the guest, so the
        // front-desk module can list every booking under one confirmation number with
        // its own dates
        frontBooking.setStayPeriod(checkIn.toString(), checkOut.toString(), frontBooking.getNumberOfNights());
        guest.addBooking(frontBooking);

        // show estimated price right at allocation (tier discount is a promo thing, only
        // affects displayed price, doesnt touch room type/status) - new walk-in guest is
        // always Standard tier so 0% discount here, real amount still gets settled at
        // checkout
        double originalPrice = availableRoom.getNightlyRate() * frontBooking.getNumberOfNights();
        int discountPercent = TierRankUtility.tierToDiscountPercent(guest.getTier());
        double finalPrice = originalPrice - (originalPrice * discountPercent / 100.0);

        walkInCLI.displayAllocationResult(frontBooking, availableRoom, originalPrice, discountPercent, finalPrice);
    }

    // Feature 3: Cancel Waiting

    private void doCancel() {
        // show the room types waiting list first (with bookingId) so user can actually
        // see whats there before picking one to cancel, no blind typing. empty queue
        // just loops back to ask roomType again instead of asking for a bookingId that
        // cant possibly exist
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

        // cancel by bookingId not confirmationNumber - same confirmationNumber can have
        // several bookings in the same room type queue (multi room booking), searching
        // by confirmationNumber would only ever hit the first one and cant let the guest
        // pick which specific room to cancel. bookingId is unique per booking so no
        // ambiguity there
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
        // normal enqueue/dequeue only touch front/back, cancel needs the extra remove()
        // method on QueueInterface to actually pull one out from the middle
        queue.remove(target);
        walkInCLI.displayCancelResult(true);
    }

    // Feature 4: View Waiting List

    private void doViewWaitingList() {
        String roomType = promptValidRoomType();
        if (roomType == null) {
            walkInCLI.displayCancelled();
            return;
        }
        QueueInterface<Booking> queue = getQueueForRoomType(roomType);
        walkInCLI.displayWaitingList(roomType, queue.getIterator());
    }

    // Report 1: Daily Registration Report

    // filter = date + room type, sorted by registration time earliest first (selection sort)
    //
    // this report is only about registration itself: who came, what type, did they get
    // it, which room. anything about how long they waited belongs to report 2 - same
    // underlying data, but if the split isnt clear the 2 reports just end up saying the
    // same thing twice
    //
    // pulls from both still-queued bookings and already-allocated ones under guestTable,
    // so you see everyone regardless of outcome
    void doDailyRegistrationReport() {
        String dateFilter = walkInCLI.promptReportDate();
        String roomTypeFilter = walkInCLI.promptReportRoomType();

        ListInterface<Booking> filtered = new ArrayBasedList<>();
        // when roomType isnt ALL, collectWalkInBookings() only touches that one queue,
        // leaves the other 2 alone
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

        // tally while printing each row, no need for a second pass just for the stats
        // count requested vs allocated separately per room type, gap = unmet demand
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

            // registeredAt is "yyyy-MM-dd HH:mm:ss", only need the date part here - the
            // time is for report 2s hourly breakdown, showing it here would be scope creep
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

    // Report 2: Wait Time Analysis Report

    // filter = date + room type, sorted longest wait first
    //
    // deliberately no "minimum wait" filter - that would turn Average wait into "average
    // of people who waited past some threshold", and a report called Wait Time Analysis
    // shouldnt let its own filter distort the average
    // filtering by room type instead answers an extra question: which type waits longest
    void doWaitTimeAnalysisReport() {
        String dateFilter = walkInCLI.promptReportDate();
        String roomTypeFilter = walkInCLI.promptReportRoomType();

        ListInterface<Booking> filtered = new ArrayBasedList<>();
        ListInterface<Integer> waitMinutesList = new ArrayBasedList<>();

        int[] hourlyCount = new int[24];
        int[] hourlyTotalWait = new int[24];

        // wait time per room type: fixed 3 slots, index 0=Standard 1=Deluxe 2=Suite
        int[] typeAllocated = new int[3];
        int[] typeTotalWait = new int[3];
        int[] typeWaiting = new int[3];

        // when roomType isnt ALL, collectWalkInBookings() only touches that one queue,
        // leaves the other 2 alone
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

            // hourly/by-type aggregation only counts allocated ones - still-waiting
            // durations are "as of right now" and keep growing depending on when the
            // report runs, mixing them into the average would blow the number up
            // (before this fix the 09:00 slot once showed 3934 min, that was a
            // still-waiting record polluting it)
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

        // comparing average wait by room type answers "which type waits longest" - a
        // systemic issue. hourly distribution answers "whats the busiest hour" - a
        // staffing issue. 2 different angles, dont overlap
        walkInCLI.displayWaitByRoomTypeHeader();
        walkInCLI.displayWaitByRoomTypeRow("Standard", typeAllocated[0], typeTotalWait[0], typeWaiting[0]);
        walkInCLI.displayWaitByRoomTypeRow("Deluxe", typeAllocated[1], typeTotalWait[1], typeWaiting[1]);
        walkInCLI.displayWaitByRoomTypeRow("Suite", typeAllocated[2], typeTotalWait[2], typeWaiting[2]);
        walkInCLI.displaySlowestRoomType(
                slowestRoomType(typeAllocated, typeTotalWait),
                slowestAverageWait(typeAllocated, typeTotalWait));

        // find the max count first, the bar chart uses it as the full-bar reference
        int maxHourlyCount = 0;
        for (int hour = 0; hour < 24; hour++) {
            if (hourlyCount[hour] > maxHourlyCount) {
                maxHourlyCount = hourlyCount[hour];
            }
        }

        // collect every hour that ties the max. small datasets easily get several hours
        // tied for busiest, reporting just the first one would be misleading - same
        // handling as report 1s busiest room type
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

    // shared report helper methods

    // combines everyone still queued in the 3 queues with WALK_IN bookings already
    // allocated and sitting under guestTable, shared by both reports
    //
    // cancelled bookings get pulled out by queue.remove() in doCancel() and arent stored
    // anywhere else after that - reports just wont see them, thats intentional, not a bug
    //
    // roomTypeFilter "ALL" pulls all 3 queues, otherwise just the matching one (allocated
    // ones still need scanning guestTable regardless since theyre not in a queue anymore)
    private ListInterface<Booking> collectWalkInBookings(String roomTypeFilter) {
        ListInterface<Booking> result = new ArrayBasedList<>();

        // the 3 queues are already split by type, filtering just picks the right one, no
        // need to dump everything and compare type field by field - the split itself is
        // the index
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

    // maps room type to a fixed array index, 3 types are hardcoded so array beats a list
    // here. returns 0=Standard (also the fallback for anything unrecognized) 1=Deluxe 2=Suite
    private int roomTypeIndex(String roomType) {
        if ("Deluxe".equalsIgnoreCase(roomType)) {
            return 1;
        }
        if ("Suite".equalsIgnoreCase(roomType)) {
            return 2;
        }
        return 0;
    }

    // room type with the longest average wait, returns "-" if nothing allocated at all
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

    // average wait minutes for that slowest type, for the summary line
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

    // minutes between 2 "yyyy-MM-dd HH:mm:ss" timestamps
    //
    // if one line in the data file has a broken timestamp, LocalDateTime.parse() throws
    // DateTimeParseException (unchecked) and the whole program dies, cant even get back
    // to the main menu. rather have one report cell show blank than crash over a single
    // bad row, so catch it here and return -1 for "couldnt calculate"
    private int minutesBetween(String start, String end) {
        try {
            LocalDateTime startTime = LocalDateTime.parse(start, TIMESTAMP_FORMAT);
            LocalDateTime endTime = LocalDateTime.parse(end, TIMESTAMP_FORMAT);
            return (int) java.time.Duration.between(startTime, endTime).toMinutes();
        } catch (java.time.format.DateTimeParseException e) {
            return -1;
        }
    }

    // sorts in place by registeredAt ascending (selection sort, cant use Collections.sort)
    // registeredAt is fixed-width "yyyy-MM-dd HH:mm:ss" so plain string comparison
    // already matches chronological order, no need to parse into LocalDateTime first
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

    // sorts bookings and their waitMinutes together, longest wait first (selection sort)
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

    // internal helper methods

    // reports parse this exact format to calculate wait minutes, so fixed length matters
    // here - LocalTime.toString() drops trailing zero seconds which breaks that
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // current time as "yyyy-MM-dd HH:mm:ss", used for registeredAt/allocatedAt and for
    // report wait time math
    private String currentTimestamp() {
        return LocalDateTime.now().withNano(0).format(TIMESTAMP_FORMAT);
    }

    // Input retry (bad format just asks again, doesnt abort the whole thing)

    // only validation for name is "not blank", so blank itself just means cancel, no
    // need for a separate cancel signal - null return means cancel
    // typing something with numbers/symbols in it is the real format error, ask again
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

    // blank = cancel (null), non-digit input is the real error, ask again
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

    // WalkInCLI turns blank into Integer.MIN_VALUE (wont clash with a real night count
    // or the existing -1 invalid marker), used as the cancel signal separate from
    // "typed something but its not a valid positive number" which just re-asks
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

    // first room in roomList with matching type and status exactly AVAILABLE
    // must use equals("AVAILABLE"), not just "not OCCUPIED" - NEEDS_CLEANING /
    // CLEANING_IN_PROGRESS / INSPECTED are all "not OCCUPIED" too but cleaning isnt
    // done yet, cant hand those out
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

    // enrolls a first-time walk-in (not a member yet) as a Standard member, saves to
    // memberList, returns the new Member so the caller can grab memberId/tier for
    // building Guest
    // currentPoints/totalPointsEarned both start at 0, actually earning points is module
    // 5s job, this just handles "this person now has a memberId"
    private Member enrollAsStandardMember(String name, String phone) {
        String memberId = "M" + memberCounter;
        memberCounter++;
        Member member = new Member(memberId, name, phone, "Standard", 0, 0);
        memberList.add(member);
        return member;
    }

    // scans memberList once, parses the number part out of any "M"+digits id (like seed
    // data M1001~M1005), takes max+1 as the starting point for WalkInControls own
    // counter - only this counter uses this number range from here on, never rescans
    // again, keeps new ids consistent with the seed data format and avoids 2 places
    // generating the same id
    // falls back to a safe starting point if memberList is somehow empty (shouldnt
    // happen, MemberDao loads seed data first)
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
                    // id isnt M+digits format, skip it, doesnt affect the rest
                }
            }
        }
        return maxNumber + 1;
    }

    // checks guestTable by confirmationNumber for an existing Guest record (eg already
    // created from another room being allocated)
    // Guest equals/hashCode only look at confirmationNumber so a template with just that
    // field set and everything else null still works as the lookup key
    private Guest findGuestByConfirmationNumber(String confirmationNumber) {
        Guest template = new Guest(confirmationNumber, null, null, null, null, null, null, null, 0);
        return guestTable.getEntry(template);
    }

    // linear search the given queue for the Booking matching bookingId
    // returns the actual object thats really in the queue, needed for queue.remove() to
    // compare and remove it correctly
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

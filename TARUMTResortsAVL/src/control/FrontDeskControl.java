package control;

import adt.ArrayBasedList;
import adt.HashTableInterface;
import adt.ListInterface;
import boundary.FrontDeskCLI;
import entity.BillingRecord;
import entity.Booking;
import entity.BookingStatus;
import entity.Guest;
import entity.Member;
import entity.Room;
import java.time.LocalDate;
import java.util.Iterator;
import utility.TierRankUtility;
import utility.ValidationUtility;

/**
 * Controls the main operations of the Front-Desk Service module.
 * Guest records are searched using a hash table, while room data is
 * searched through a list.
 *
 * @author Lim Wei Shern
 */
public class FrontDeskControl {

    private FrontDeskCLI frontDeskCLI;
    private HashTableInterface<Guest> guestTable;
    private ListInterface<Room> roomList;
    private LoyaltyControl loyaltyControl;
    private HousekeepingControl housekeepingControl;

    private int billingCounter;

    public FrontDeskControl(FrontDeskCLI frontDeskCLI,
            HashTableInterface<Guest> guestTable,
            ListInterface<Room> roomList,
            LoyaltyControl loyaltyControl,
            HousekeepingControl housekeepingControl) {
        this.frontDeskCLI = frontDeskCLI;
        this.guestTable = guestTable;
        this.roomList = roomList;
        this.loyaltyControl = loyaltyControl;
        this.housekeepingControl = housekeepingControl;
        this.billingCounter = 0;
    }

    public void run() {

        boolean running = true;

        while (running) {

            int choice = frontDeskCLI.displayMenuAndGetChoice();

            switch (choice) {

                case 1:
                    searchGuestByConfirmationNumber();
                    break;

                case 2:
                    checkRoomAvailability();
                    break;

                case 3:
                    viewBillingDetails();
                    break;

                case 4:
                    doCheckOut();
                    break;

                case 0:
                    running = false;
                    break;

                default:
                    frontDeskCLI.displayInvalidChoice();
                    break;
            }
        }
    }

    private void searchGuestByConfirmationNumber() {

        String confirmationNumber = promptValidConfirmationNumber();
        if (confirmationNumber == null) {
            frontDeskCLI.displayCancelled();
            return;
        }
        Guest foundGuest = findGuest(confirmationNumber);

        if (foundGuest == null) {
            frontDeskCLI.displayGuestNotFound();
            return;
        }

        // Guests without a member ID are treated as walk-in guests.
        boolean isMember = foundGuest.getMemberId() != null;
        String guestType = isMember ? "Member" : "Walk-In Guest";
        String memberIdDisplay = isMember ? foundGuest.getMemberId() : "-  (not a member)";

        // Tiers above Standard are treated as VIP tiers.
        int rank = TierRankUtility.tierToRank(foundGuest.getTier());
        String vipStatus = (rank > 0)
                ? "VIP  (tier rank " + rank + ")"
                : "Standard  (no queue priority)";

        frontDeskCLI.displayGuestDetails(
                foundGuest.getConfirmationNumber(),
                foundGuest.getName(),
                foundGuest.getPhone(),
                guestType,
                memberIdDisplay,
                foundGuest.getTier(),
                vipStatus,
                orDash(foundGuest.getRegistrationTime()),
                buildBookingLines(foundGuest),
                foundGuest.getBookings().getNumberOfEntries());
    }

    /**
     * Processes check-out for selected rooms under one confirmation number.
     * Each check-out creates a separate billing record.
     */
    private void doCheckOut() {

        String confirmationNumber = promptValidConfirmationNumber();
        if (confirmationNumber == null) {
            frontDeskCLI.displayCancelled();
            return;
        }

        Guest guest = findGuest(confirmationNumber);
        if (guest == null) {
            frontDeskCLI.displayGuestNotFound();
            return;
        }

        ListInterface<Booking> checkedIn = findCheckedInBookings(guest);
        if (checkedIn.isEmpty()) {
            frontDeskCLI.displayNoRoomsToCheckOut(confirmationNumber);
            return;
        }

        frontDeskCLI.displayCheckedInBookings(checkedIn.getIterator());

        // Staff selects which checked-in rooms to check out.
        ListInterface<Booking> selected = new ArrayBasedList<>();
        double roomFee = 0.0;
        boolean continueSelecting = true;
        while (continueSelecting) {
            String roomNumber = frontDeskCLI.promptRoomToCheckOut();
            Booking match = findBookingByRoomNumber(checkedIn, roomNumber);
            if (match == null) {
                frontDeskCLI.displayRoomNotEligible(roomNumber);
            } else if (selected.contains(match)) {
                frontDeskCLI.displayRoomAlreadySelected(roomNumber);
            } else {
                Room room = findRoom(match.getAssignedRoomNo());
                double rate = (room == null) ? 0.0 : room.getNightlyRate();
                roomFee = roomFee + rate * match.getNumberOfNights();
                selected.add(match);
                frontDeskCLI.displayRoomSelected(roomNumber);
            }

            // Stop asking once all checked-in rooms have been selected.
            if (selected.getNumberOfEntries() >= checkedIn.getNumberOfEntries()) {
                frontDeskCLI.displayAllRoomsSelected();
                continueSelecting = false;
            } else {
                continueSelecting = frontDeskCLI.promptCheckOutAnotherRoom();
            }
        }

        if (selected.isEmpty()) {
            frontDeskCLI.displayNoRoomsSelected();
            return;
        }

        double extraCharges = promptValidExtraCharges();
        if (Double.isNaN(extraCharges)) {
            // No booking or billing data has been changed yet, so cancellation is safe.
            frontDeskCLI.displayCancelled();
            return;
        }

        // Use the member's current tier when calculating the check-out discount.
        String currentTier = (guest.getMemberId() == null) ? null
                : loyaltyControl.getTierByMemberId(guest.getMemberId());
        int discountPercent = TierRankUtility.tierToDiscountPercent(currentTier);
        double discountedRoomFee = roomFee - (roomFee * discountPercent / 100.0);

        double totalAmount = discountedRoomFee + extraCharges;
        int pointsEarned = (int) (totalAmount / 10);

        billingCounter++;
        String billingId = "BR" + String.format("%06d", billingCounter);
        BillingRecord billingRecord = new BillingRecord(billingId, confirmationNumber, discountedRoomFee,
                extraCharges, totalAmount, pointsEarned, LocalDate.now().toString());
        guest.addBillingRecord(billingRecord);

        for (int i = 1; i <= selected.getNumberOfEntries(); i++) {
            Booking booking = selected.getEntry(i);
            booking.setStatus(BookingStatus.CHECKED_OUT);
            Room room = findRoom(booking.getAssignedRoomNo());
            if (room != null) {
                housekeepingControl.markRoomNeedsCleaning(
                        room.getRoomNumber());
            }
        }

        Member member = (guest.getMemberId() == null) ? null
                : loyaltyControl.awardPointsByMemberId(guest.getMemberId(), pointsEarned);

        frontDeskCLI.displayCheckOutResult(billingRecord, selected.getNumberOfEntries(),
                roomFee, discountPercent, (member == null) ? null : member.getTier());
    }

    /**
     * Displays current room charges and previously settled bills.
     * Current charges do not include the final discount or extra charges.
     */
    private void viewBillingDetails() {

        String confirmationNumber = promptValidConfirmationNumber();
        if (confirmationNumber == null) {
            frontDeskCLI.displayCancelled();
            return;
        }

        Guest guest = findGuest(confirmationNumber);
        if (guest == null) {
            frontDeskCLI.displayBillingRecordNotFound(confirmationNumber);
            return;
        }

        boolean isMember = guest.getMemberId() != null;
        String guestType = isMember ? "Member" : "Walk-In Guest";

        frontDeskCLI.displayBillingHeader(confirmationNumber, guest.getName(),
                guestType, guest.getTier());

        int roomsCheckedIn = countCheckedInRooms(guest);
        if (roomsCheckedIn > 0) {
            frontDeskCLI.displayCurrentCharges(
                    buildChargeLines(guest),
                    roomsCheckedIn,
                    countCheckedInNights(guest),
                    calculateCurrentCharges(guest));
        }

        frontDeskCLI.displaySettledBills(
                buildSettledLines(guest),
                guest.getBillingRecords().getNumberOfEntries(),
                calculateSettledTotal(guest),
                calculateTotalPoints(guest));

        frontDeskCLI.displayBillingFooter();
    }

    /**
     * Displays rooms that are currently available for sale.
     * Rooms still in housekeeping are treated as unavailable.
     */
    private void checkRoomAvailability() {

        String typeFilter = frontDeskCLI.promptRoomTypeFilter();

        int inHousekeeping = countRooms(typeFilter, "NEEDS_CLEANING")
                + countRooms(typeFilter, "CLEANING_IN_PROGRESS")
                + countRooms(typeFilter, "INSPECTED");

        frontDeskCLI.displayRoomAvailability(
                typeFilter,
                buildTypeBreakdownLines(typeFilter),
                buildAvailableRoomLines(typeFilter),
                countRooms(typeFilter, "AVAILABLE"),
                countRooms(typeFilter, "OCCUPIED"),
                inHousekeeping);
    }
    // Report 1: Check-Out Revenue Report

    /**
     * Generates the check-out revenue report.
     * Bills are filtered by date and guest tier, then sorted by total amount.
     * The tier stored in the guest record is used for report grouping.
     */
    void doCheckOutRevenueReport() {
        String fromDate = frontDeskCLI.promptReportFromDate();
        String toDate = frontDeskCLI.promptReportToDate();
        String tierFilter = frontDeskCLI.promptReportTierFilter();

        ListInterface<BillingRecord> filteredBills = new ArrayBasedList<>();

        ListInterface<String> tierNames = new ArrayBasedList<>();
        ListInterface<Double> tierRevenue = new ArrayBasedList<>();

        double totalRevenue = 0.0;
        double totalRoomFee = 0.0;
        double totalExtras = 0.0;
        int totalPoints = 0;
        double highestSpend = 0.0;
        String highestSpendGuest = "-";

        Iterator<Guest> guestIterator = guestTable.getIterator();
        while (guestIterator.hasNext()) {
            Guest guest = guestIterator.next();
            Iterator<BillingRecord> billIterator = guest.getBillingRecords().getIterator();
            while (billIterator.hasNext()) {
                BillingRecord bill = billIterator.next();
                boolean dateMatches = bill.getDate().compareTo(fromDate) >= 0
                        && bill.getDate().compareTo(toDate) <= 0;
                boolean tierMatches = "ALL".equalsIgnoreCase(tierFilter)
                        || tierFilter.equalsIgnoreCase(guest.getTier());
                if (!dateMatches || !tierMatches) {
                    continue;
                }

                filteredBills.add(bill);

                totalRevenue = totalRevenue + bill.getTotalAmount();
                totalRoomFee = totalRoomFee + bill.getRoomFee();
                totalExtras = totalExtras + bill.getExtraCharges();
                totalPoints = totalPoints + bill.getPointsEarned();
                if (bill.getTotalAmount() > highestSpend) {
                    highestSpend = bill.getTotalAmount();
                    highestSpendGuest = guest.getName();
                }
                addToTierRevenue(tierNames, tierRevenue, guest.getTier(), bill.getTotalAmount());
            }
        }

        int n = filteredBills.getNumberOfEntries();
        BillingRecord[] bills = new BillingRecord[n];
        for (int i = 1; i <= n; i++) {
            bills[i - 1] = filteredBills.getEntry(i);
        }
        mergeSortBillsByAmountDescending(bills, 0, n - 1);

        frontDeskCLI.displayCheckOutRevenueReportHeader(fromDate, toDate, tierFilter);

        if (n == 0) {
            frontDeskCLI.displayNoReportRecords();
            frontDeskCLI.displayReportEnd();
            return;
        }

        for (int i = 0; i < n; i++) {
            // Retrieve the bill owner through the guest hash table.
            Guest owner = findGuest(bills[i].getConfirmationNumber());
            frontDeskCLI.displayCheckOutRevenueReportRow(
                    bills[i].getBillingId(),
                    (owner == null) ? "-" : owner.getName(),
                    (owner == null) ? "-" : owner.getTier(),
                    bills[i].getRoomFee(),
                    bills[i].getExtraCharges(),
                    bills[i].getTotalAmount());
        }

        frontDeskCLI.displayCheckOutRevenueReportSummary(n, totalRevenue, totalRoomFee, totalExtras,
                totalRevenue / n, highestSpend, highestSpendGuest, totalPoints);

        // Find the tier with the highest revenue before displaying the report.
        int topTierPosition = findTopTierPosition(tierRevenue);

        frontDeskCLI.displayRevenueByTier(
                tierNames.getIterator(), tierRevenue.getIterator(), totalRevenue,
                tierNames.getEntry(topTierPosition), tierRevenue.getEntry(topTierPosition));

        frontDeskCLI.displayReportEnd();
    }

    /**
     * Groups revenue by tier using parallel lists for tier names and amounts.
     */
    private void addToTierRevenue(ListInterface<String> tierNames, ListInterface<Double> tierRevenue,
            String tier, double amount) {
        int position = tierNames.indexOf(tier);
        if (position == -1) {
            tierNames.add(tier);
            tierRevenue.add(amount);
        } else {
            tierRevenue.replace(position, tierRevenue.getEntry(position) + amount);
        }
    }

    /**
     * Finds the position of the tier with the highest revenue.
     * The first tier is kept when two totals are equal.
     *
     * @return the 1-based position of the highest-revenue tier
     */
    private int findTopTierPosition(ListInterface<Double> tierRevenue) {
        int topPosition = 1;
        for (int i = 2; i <= tierRevenue.getNumberOfEntries(); i++) {
            if (tierRevenue.getEntry(i) > tierRevenue.getEntry(topPosition)) {
                topPosition = i;
            }
        }
        return topPosition;
    }

    /**
     * Sorts billing records by total amount in descending order using merge sort.
     * Equal amounts keep their original order, making the sort stable.
     */
    private void mergeSortBillsByAmountDescending(BillingRecord[] bills, int left, int right) {
        if (left >= right) {
            return;
        }
        int middle = (left + right) / 2;
        mergeSortBillsByAmountDescending(bills, left, middle);
        mergeSortBillsByAmountDescending(bills, middle + 1, right);

        BillingRecord[] merged = new BillingRecord[right - left + 1];
        int i = left;
        int j = middle + 1;
        int k = 0;
        while (i <= middle && j <= right) {
            // Take the left entry first when amounts are equal to preserve stability.
            if (bills[i].getTotalAmount() >= bills[j].getTotalAmount()) {
                merged[k] = bills[i];
                i++;
            } else {
                merged[k] = bills[j];
                j++;
            }
            k++;
        }
        while (i <= middle) {
            merged[k] = bills[i];
            i++;
            k++;
        }
        while (j <= right) {
            merged[k] = bills[j];
            j++;
            k++;
        }
        for (k = 0; k < merged.length; k++) {
            bills[left + k] = merged[k];
        }
    }

    // Report 2: In-House Guests & Outstanding Charges

    /**
     * Generates a report of guests who are still checked in.
     * Records are filtered by room type and expected check-out date,
     * then sorted by check-out date.
     *
     * Each row represents one occupied room.
     */
    void doInHouseGuestsReport() {
        String checkOutBefore = frontDeskCLI.promptReportToDate();
        String roomTypeFilter = frontDeskCLI.promptReportRoomTypeFilter();

        ListInterface<Booking> inHouse = new ArrayBasedList<>();
        ListInterface<Double> accrued = new ArrayBasedList<>();
        ListInterface<String> guestNames = new ArrayBasedList<>();
        ListInterface<String> guestTiers = new ArrayBasedList<>();

        // Linear search: keep checked-in bookings that match the
        // check-out date and room type filters.
        Iterator<Guest> guestIterator = guestTable.getIterator();
        while (guestIterator.hasNext()) {
            Guest guest = guestIterator.next();
            Iterator<Booking> bookingIterator = guest.getBookings().getIterator();
            while (bookingIterator.hasNext()) {
                Booking booking = bookingIterator.next();

                if (booking.getStatus() != BookingStatus.CHECKED_IN
                        || booking.getAssignedRoomNo() == null) {
                    continue;
                }

                Room room = findRoom(booking.getAssignedRoomNo());
                if (room == null) {
                    continue;
                }

                boolean typeMatches = "ALL".equalsIgnoreCase(roomTypeFilter)
                        || room.getRoomType().equalsIgnoreCase(roomTypeFilter);
                boolean dateMatches = booking.getCheckOutDate() == null
                        || booking.getCheckOutDate().compareTo(checkOutBefore) <= 0;

                if (!typeMatches || !dateMatches) {
                    continue;
                }

                inHouse.add(booking);
                accrued.add(room.getNightlyRate() * booking.getNumberOfNights());
                guestNames.add(guest.getName());
                guestTiers.add(guest.getTier());
            }
        }

        // Sort by expected check-out date so the earliest departures appear first.
        sortByCheckOutDate(inHouse, accrued, guestNames, guestTiers);

        frontDeskCLI.displayInHouseReportHeader(checkOutBefore, roomTypeFilter);

        int n = inHouse.getNumberOfEntries();
        if (n == 0) {
            frontDeskCLI.displayNoReportRecords();
            frontDeskCLI.displayReportEnd();
            return;
        }

        double totalAccrued = 0.0;
        for (int i = 1; i <= n; i++) {
            Booking booking = inHouse.getEntry(i);
            Room room = findRoom(booking.getAssignedRoomNo());

            totalAccrued = totalAccrued + accrued.getEntry(i);

            frontDeskCLI.displayInHouseReportRow(
                    guestNames.getEntry(i),
                    guestTiers.getEntry(i),
                    booking.getAssignedRoomNo(),
                    (room == null) ? "-" : room.getRoomType(),
                    orDash(booking.getCheckOutDate()),
                    booking.getNumberOfNights(),
                    accrued.getEntry(i));
        }

        // The first booking after sorting is the earliest departure.
        Booking soonest = inHouse.getEntry(1);
        double occupancyRate = roomList.getNumberOfEntries() == 0
                ? 0.0
                : n * 100.0 / roomList.getNumberOfEntries();

        frontDeskCLI.displayInHouseSummary(n, roomList.getNumberOfEntries(), occupancyRate,
                totalAccrued, totalAccrued / n,
                guestNames.getEntry(1), orDash(soonest.getCheckOutDate()),
                soonest.getAssignedRoomNo());

        displayDepartureSchedule(inHouse);

        frontDeskCLI.displayReportEnd();
    }

    /**
     * Groups occupied rooms by expected check-out date.
     * Since bookings are already sorted by date, equal dates are consecutive.
     */
    private void displayDepartureSchedule(ListInterface<Booking> inHouse) {

        frontDeskCLI.displayDepartureScheduleHeader();

        int i = 1;
        while (i <= inHouse.getNumberOfEntries()) {

            String date = orDash(inHouse.getEntry(i).getCheckOutDate());
            String rooms = "";
            int count = 0;

            // Collect consecutive bookings with the same check-out date.
            while (i <= inHouse.getNumberOfEntries()
                    && date.equals(orDash(inHouse.getEntry(i).getCheckOutDate()))) {
                rooms = rooms.isEmpty()
                        ? inHouse.getEntry(i).getAssignedRoomNo()
                        : rooms + ", " + inHouse.getEntry(i).getAssignedRoomNo();
                count++;
                i++;
            }

            frontDeskCLI.displayDepartureScheduleRow(date, count, rooms);
        }
    }

    /**
     * Sorts bookings by expected check-out date using merge sort.
     * Parallel lists are moved together to keep booking, charge, guest and
     * tier data aligned.
     */
    private void sortByCheckOutDate(ListInterface<Booking> bookings, ListInterface<Double> accrued,
            ListInterface<String> names, ListInterface<String> tiers) {
        mergeSortByCheckOutDate(bookings, accrued, names, tiers, 1, bookings.getNumberOfEntries());
    }

    private void mergeSortByCheckOutDate(ListInterface<Booking> bookings, ListInterface<Double> accrued,
            ListInterface<String> names, ListInterface<String> tiers,
            int left, int right) {
        if (left >= right) {
            return;
        }
        int middle = (left + right) / 2;
        mergeSortByCheckOutDate(bookings, accrued, names, tiers, left, middle);
        mergeSortByCheckOutDate(bookings, accrued, names, tiers, middle + 1, right);

        int size = right - left + 1;
        Booking[] mergedBookings = new Booking[size];
        double[] mergedAccrued = new double[size];
        String[] mergedNames = new String[size];
        String[] mergedTiers = new String[size];

        int i = left;
        int j = middle + 1;
        int k = 0;
        while (i <= middle && j <= right) {
            if (departsFirst(bookings.getEntry(i), bookings.getEntry(j))) {
                mergedBookings[k] = bookings.getEntry(i);
                mergedAccrued[k] = accrued.getEntry(i);
                mergedNames[k] = names.getEntry(i);
                mergedTiers[k] = tiers.getEntry(i);
                i++;
            } else {
                mergedBookings[k] = bookings.getEntry(j);
                mergedAccrued[k] = accrued.getEntry(j);
                mergedNames[k] = names.getEntry(j);
                mergedTiers[k] = tiers.getEntry(j);
                j++;
            }
            k++;
        }
        while (i <= middle) {
            mergedBookings[k] = bookings.getEntry(i);
            mergedAccrued[k] = accrued.getEntry(i);
            mergedNames[k] = names.getEntry(i);
            mergedTiers[k] = tiers.getEntry(i);
            i++;
            k++;
        }
        while (j <= right) {
            mergedBookings[k] = bookings.getEntry(j);
            mergedAccrued[k] = accrued.getEntry(j);
            mergedNames[k] = names.getEntry(j);
            mergedTiers[k] = tiers.getEntry(j);
            j++;
            k++;
        }
        for (k = 0; k < size; k++) {
            bookings.replace(left + k, mergedBookings[k]);
            accrued.replace(left + k, mergedAccrued[k]);
            names.replace(left + k, mergedNames[k]);
            tiers.replace(left + k, mergedTiers[k]);
        }
    }

    /**
     * Compares two bookings for report sorting.
     * Earlier check-out dates come first, with room number as the tie-breaker.
     * Non-null dates use yyyy-MM-dd format, so their string order matches date
     * order.
     *
     * @return true if a should appear before b
     */
    private boolean departsFirst(Booking a, Booking b) {
        String dateA = orDash(a.getCheckOutDate());
        String dateB = orDash(b.getCheckOutDate());
        int result = dateA.compareTo(dateB);
        if (result != 0) {
            return result < 0;
        }
        return a.getAssignedRoomNo().compareToIgnoreCase(b.getAssignedRoomNo()) <= 0;
    }

    /**
     * Returns only bookings that are currently checked in.
     */
    private ListInterface<Booking> findCheckedInBookings(Guest guest) {
        ListInterface<Booking> result = new ArrayBasedList<>();
        for (int i = 1; i <= guest.getBookings().getNumberOfEntries(); i++) {
            Booking booking = guest.getBookings().getEntry(i);
            if (booking.getStatus() == BookingStatus.CHECKED_IN) {
                result.add(booking);
            }
        }
        return result;
    }

    /**
     * Finds a checked-in booking by its assigned room number.
     */
    private Booking findBookingByRoomNumber(ListInterface<Booking> bookings, String roomNumber) {
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            Booking booking = bookings.getEntry(i);
            if (booking.getAssignedRoomNo() != null && booking.getAssignedRoomNo().equalsIgnoreCase(roomNumber)) {
                return booking;
            }
        }
        return null;
    }

    // Input validation

    /**
     * Re-prompts until a valid 8-digit confirmation number is entered.
     * Blank input cancels the operation.
     */
    private String promptValidConfirmationNumber() {
        String confirmationNumber;
        while (true) {
            confirmationNumber = frontDeskCLI.promptConfirmationNumber();
            if (ValidationUtility.isBlank(confirmationNumber)) {
                return null;
            }
            if (ValidationUtility.isEightDigitNumber(confirmationNumber)) {
                return confirmationNumber;
            }
            frontDeskCLI.displayInvalidConfirmationNumber(confirmationNumber);
        }
    }

    /**
     * Re-prompts until a valid non-negative amount is entered.
     * Blank input is represented by NaN and cancels the operation.
     */
    private double promptValidExtraCharges() {
        double extraCharges;
        while (true) {
            extraCharges = frontDeskCLI.promptExtraCharges();
            if (Double.isNaN(extraCharges)) {
                return Double.NaN;
            }
            if (extraCharges >= 0) {
                return extraCharges;
            }
            frontDeskCLI.displayInvalidExtraCharges(extraCharges);
        }
    }

    /**
     * Searches for a guest by confirmation number using the hash table.
     * Average lookup time is O(1).
     *
     * @param confirmationNumber the 8-digit confirmation number
     * @return the matching guest, or null if not found
     */
    private Guest findGuest(String confirmationNumber) {
        return guestTable.getEntry(new Guest(confirmationNumber));
    }

    /**
     * Searches for a room by room number using a linear scan.
     * The search takes O(n) time in the worst case.
     *
     * @param roomNumber the room number to find
     * @return the matching room, or null if not found
     */
    private Room findRoom(String roomNumber) {
        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
            Room room = roomList.getEntry(i);
            if (room.getRoomNumber().equals(roomNumber)) {
                return room;
            }
        }
        return null;
    }

    // Displays "-" instead of null.
    private String orDash(String value) {
        return (value == null) ? "-" : value;
    }

    /**
     * Builds the formatted booking details for a guest.
     */
    private String buildBookingLines(Guest guest) {

        if (guest.getBookings().isEmpty()) {
            return "  -  (no booking record linked to this confirmation number)"
                    + System.lineSeparator();
        }

        String result = "";
        for (int i = 1; i <= guest.getBookings().getNumberOfEntries(); i++) {

            Booking booking = guest.getBookings().getEntry(i);

            String roomNo = orDash(booking.getAssignedRoomNo());
            String roomType = booking.getRequestedRoomType();
            double rate = 0.0;

            if (booking.getAssignedRoomNo() != null) {
                Room room = findRoom(booking.getAssignedRoomNo());
                if (room != null) {
                    roomType = room.getRoomType();
                    rate = room.getNightlyRate();
                }
            }

            result = result + String.format("  %d. %-10s | %-9s | Room %-6s | %-11s%n",
                    i, booking.getBookingId(), roomType, roomNo, booking.getStatus());
            result = result + String.format("     Stay: %s to %s  (%d night(s))  |  RM %.2f / night%n",
                    orDash(booking.getCheckInDate()), orDash(booking.getCheckOutDate()),
                    booking.getNumberOfNights(), rate);
        }
        return result;
    }

    /**
     * Builds one charge line for each room the guest is still occupying.
     */
    private String buildChargeLines(Guest guest) {

        String result = "";
        for (int i = 1; i <= guest.getBookings().getNumberOfEntries(); i++) {

            Booking booking = guest.getBookings().getEntry(i);
            if (booking.getStatus() != BookingStatus.CHECKED_IN) {
                continue;
            }

            Room room = findRoom(booking.getAssignedRoomNo());
            double rate = (room == null) ? 0.0 : room.getNightlyRate();
            String type = (room == null) ? booking.getRequestedRoomType() : room.getRoomType();
            double subtotal = rate * booking.getNumberOfNights();

            result = result + String.format("  %-8s %-12s %14.2f %10d %16.2f%n",
                    booking.getAssignedRoomNo(), type, rate,
                    booking.getNumberOfNights(), subtotal);
        }
        return result;
    }

    /**
     * Calculates room charges for all currently checked-in bookings.
     */
    private double calculateCurrentCharges(Guest guest) {

        double total = 0.0;
        for (int i = 1; i <= guest.getBookings().getNumberOfEntries(); i++) {

            Booking booking = guest.getBookings().getEntry(i);
            if (booking.getStatus() != BookingStatus.CHECKED_IN) {
                continue;
            }

            Room room = findRoom(booking.getAssignedRoomNo());
            double rate = (room == null) ? 0.0 : room.getNightlyRate();
            total = total + rate * booking.getNumberOfNights();
        }
        return total;
    }

    // Counts rooms currently occupied by the guest.
    private int countCheckedInRooms(Guest guest) {

        int count = 0;
        for (int i = 1; i <= guest.getBookings().getNumberOfEntries(); i++) {
            if (guest.getBookings().getEntry(i).getStatus() == BookingStatus.CHECKED_IN) {
                count++;
            }
        }
        return count;
    }

    // Counts total nights across checked-in bookings.
    private int countCheckedInNights(Guest guest) {

        int nights = 0;
        for (int i = 1; i <= guest.getBookings().getNumberOfEntries(); i++) {
            Booking booking = guest.getBookings().getEntry(i);
            if (booking.getStatus() == BookingStatus.CHECKED_IN) {
                nights = nights + booking.getNumberOfNights();
            }
        }
        return nights;
    }

    /**
     * Builds the display lines for all settled billing records.
     */
    private String buildSettledLines(Guest guest) {

        String result = "";
        for (int i = 1; i <= guest.getBillingRecords().getNumberOfEntries(); i++) {

            BillingRecord bill = guest.getBillingRecords().getEntry(i);

            result = result + String.format("  %-10s  %s%n",
                    bill.getBillingId(), bill.getDate());
            result = result + String.format("    Room fee                     RM %12.2f%n",
                    bill.getRoomFee());
            result = result + String.format("    Extra charges                RM %12.2f%n",
                    bill.getExtraCharges());
            result = result + String.format("    Total paid                   RM %12.2f%n",
                    bill.getTotalAmount());
            result = result + String.format("    Points earned                %15d%n",
                    bill.getPointsEarned());
            result = result + System.lineSeparator();
        }
        return result;
    }

    private double calculateSettledTotal(Guest guest) {

        double total = 0.0;
        for (int i = 1; i <= guest.getBillingRecords().getNumberOfEntries(); i++) {
            total = total + guest.getBillingRecords().getEntry(i).getTotalAmount();
        }
        return total;
    }

    private int calculateTotalPoints(Guest guest) {

        int points = 0;
        for (int i = 1; i <= guest.getBillingRecords().getNumberOfEntries(); i++) {
            points = points + guest.getBillingRecords().getEntry(i).getPointsEarned();
        }
        return points;
    }

    /**
     * Counts rooms matching the given type and status.
     * A null filter means any value is accepted.
     *
     * @param typeFilter   room type to match, or null for any type
     * @param statusFilter room status to match, or null for any status
     * @return number of rooms matching both filters
     */
    private int countRooms(String typeFilter, String statusFilter) {

        int count = 0;
        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {

            Room room = roomList.getEntry(i);

            if (typeFilter != null && !room.getRoomType().equals(typeFilter)) {
                continue;
            }
            if (statusFilter != null && !room.getStatus().equals(statusFilter)) {
                continue;
            }
            count++;
        }
        return count;
    }

    /**
     * Builds the available and unavailable counts for each room type.
     */
    private String buildTypeBreakdownLines(String typeFilter) {

        String[] allTypes = { "Standard", "Deluxe", "Suite" };
        String result = "";

        for (int i = 0; i < allTypes.length; i++) {

            String type = allTypes[i];
            if (typeFilter != null && !type.equals(typeFilter)) {
                continue;
            }

            int unavailable = countRooms(type, null) - countRooms(type, "AVAILABLE");

            result = result + String.format("  %-12s %14d %14d%n",
                    type, countRooms(type, "AVAILABLE"), unavailable);
        }
        return result;
    }

    /**
     * Builds the list of rooms that are currently available.
     * Only rooms with AVAILABLE status can be sold; rooms still in
     * housekeeping are excluded.
     */
    private String buildAvailableRoomLines(String typeFilter) {

        String result = "";
        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {

            Room room = roomList.getEntry(i);

            if (typeFilter != null && !room.getRoomType().equals(typeFilter)) {
                continue;
            }
            if (!room.getStatus().equals("AVAILABLE")) {
                continue;
            }

            result = result + String.format("  %-8s %-12s %14.2f%n",
                    room.getRoomNumber(), room.getRoomType(), room.getNightlyRate());
        }
        return result;
    }
}
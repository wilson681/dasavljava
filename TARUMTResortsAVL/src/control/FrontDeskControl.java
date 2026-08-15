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
 * FrontDeskControl.java - Control class for the Front-Desk Service module.
 *
 * <p>Guest identification runs through a chaining hash table keyed on the
 * 8-digit confirmation number, so an exact-match lookup costs O(1) on average.
 * Room lookups deliberately keep a linear scan as the O(n) control group for
 * the search efficiency report.</p>
 *
 * @author YOUR FULL NAME
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

        // A guest carrying no member ID walked in off the street.
        boolean isMember = foundGuest.getMemberId() != null;
        String guestType = isMember ? "Member" : "Walk-In Guest";
        String memberIdDisplay = isMember ? foundGuest.getMemberId() : "-  (not a member)";

        // Any tier above Standard earns queue priority.
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
                foundGuest.getBookings().getNumberOfEntries()
        );
    }

    /**
     * Processes a check-out. One confirmation number can carry several rooms
     * with different stay lengths, so this only settles the rooms the staff
     * actually selects this time, not the guest's entire booking history —
     * a later, separate check-out under the same confirmation number produces
     * its own BillingRecord instead of overwriting this one.
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

        // Staff picks which of the checked-in rooms are actually leaving this
        // time — same room type v.s. "add another room" loop used at registration.
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

            // 这个确认号底下能退的房间已经全部选完了,没有剩下的可以再选,
            // 不该再问"要不要继续退另一间"——问了使用者也没得选
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
            // 房间已经选好了,但退房账单还没算、Booking状态也还没改成CHECKED_OUT,
            // 这个时间点取消完全没有副作用要清理
            frontDeskCLI.displayCancelled();
            return;
        }

        // 等级折扣是"个性化促销"的一种,只影响价格计算,不碰房型/房间状态——
        // 折扣用会员现在真正的等级算(不是Guest身上入住当天的快照),这样退房那一刻
        // 算出来的价格才是准的,不会因为快照过期而算错
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
                     room.getRoomNumber()
                );
            }
        }

        Member member = (guest.getMemberId() == null) ? null
                : loyaltyControl.awardPointsByMemberId(guest.getMemberId(), pointsEarned);

        frontDeskCLI.displayCheckOutResult(billingRecord, selected.getNumberOfEntries(),
                roomFee, discountPercent, (member == null) ? null : member.getTier());
    }

    /**
     * Shows the billing picture for one confirmation number: what the rooms
     * currently checked in have accumulated, and what has already been settled.
     *
     * <p>Rooms still checked in are priced live from the nightly rate and the
     * stay length held on each booking. Rooms already checked out are read from
     * their BillingRecord instead, because the settled amount includes the tier
     * discount and extra charges that were only known at check-out.</p>
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
     * Answers the operational question "can I sell a room right now?".
     *
     * <p>Deliberately limited to current sellability. Occupancy rate, revenue
     * per room and idle capacity are analytical measures and belong to the room
     * utilisation report, so this query and that report do not overlap.</p>
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
    // ========== 报表1:Check-Out Revenue Report ==========

    /**
     * filter=日期区间+客人等级+金额门槛,按总额降序(merge sort)。资料来源是
     * guestTable底下每位客人的billingRecords,等级用guest.getTier()——这是入住
     * 那天的等级快照,反映的是"这笔消费发生当下客人是什么等级",不是现在的等级。
     */
    void doCheckOutRevenueReport() {
        String fromDate = frontDeskCLI.promptReportFromDate();
        String toDate = frontDeskCLI.promptReportToDate();
        String tierFilter = frontDeskCLI.promptReportTierFilter();
        double minAmount = frontDeskCLI.promptReportMinAmount();

        ListInterface<BillingRecord> filteredBills = new ArrayBasedList<>();
        ListInterface<String> filteredNames = new ArrayBasedList<>();
        ListInterface<String> filteredTiers = new ArrayBasedList<>();

        ListInterface<String> tierNames = new ArrayBasedList<>();
        ListInterface<Double> tierRevenue = new ArrayBasedList<>();

        double totalRevenue = 0.0;
        int totalPoints = 0;
        double highestSpend = 0.0;

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
                boolean amountMatches = bill.getTotalAmount() >= minAmount;
                if (!dateMatches || !tierMatches || !amountMatches) {
                    continue;
                }

                filteredBills.add(bill);
                filteredNames.add(guest.getName());
                filteredTiers.add(guest.getTier());

                totalRevenue = totalRevenue + bill.getTotalAmount();
                totalPoints = totalPoints + bill.getPointsEarned();
                if (bill.getTotalAmount() > highestSpend) {
                    highestSpend = bill.getTotalAmount();
                }
                addToTierRevenue(tierNames, tierRevenue, guest.getTier(), bill.getTotalAmount());
            }
        }

        int n = filteredBills.getNumberOfEntries();
        BillingRecord[] bills = new BillingRecord[n];
        String[] names = new String[n];
        String[] tiers = new String[n];
        for (int i = 1; i <= n; i++) {
            bills[i - 1] = filteredBills.getEntry(i);
            names[i - 1] = filteredNames.getEntry(i);
            tiers[i - 1] = filteredTiers.getEntry(i);
        }
        mergeSortBillsByAmountDescending(bills, names, tiers, 0, n - 1);

        frontDeskCLI.displayCheckOutRevenueReportHeader(fromDate, toDate, tierFilter, minAmount);
        if (n == 0) {
            frontDeskCLI.displayNoReportRecords();
        } else {
            for (int i = 0; i < n; i++) {
                frontDeskCLI.displayCheckOutRevenueReportRow(bills[i].getBillingId(), names[i], tiers[i],
                        bills[i].getTotalAmount());
            }
        }
        frontDeskCLI.displayCheckOutRevenueReportSummary(totalRevenue,
                (n == 0) ? 0.0 : totalRevenue / n, totalPoints, highestSpend,
                tierNames.getIterator(), tierRevenue.getIterator());
        frontDeskCLI.displayReportEnd();
    }

    /**
     * 按tier把消费金额累加起来,tier数量不多、清单已知,用两条对应位置的清单存
     * (名称/金额)做分组,不用Map。
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
     * merge sort:按totalAmount由大到小,names/tiers两个平行阵列跟着一起换位置。
     */
    private void mergeSortBillsByAmountDescending(BillingRecord[] bills, String[] names, String[] tiers,
                                                   int left, int right) {
        if (left >= right) {
            return;
        }
        int middle = (left + right) / 2;
        mergeSortBillsByAmountDescending(bills, names, tiers, left, middle);
        mergeSortBillsByAmountDescending(bills, names, tiers, middle + 1, right);

        BillingRecord[] mergedBills = new BillingRecord[right - left + 1];
        String[] mergedNames = new String[right - left + 1];
        String[] mergedTiers = new String[right - left + 1];
        int i = left;
        int j = middle + 1;
        int k = 0;
        while (i <= middle && j <= right) {
            if (bills[i].getTotalAmount() >= bills[j].getTotalAmount()) {
                mergedBills[k] = bills[i];
                mergedNames[k] = names[i];
                mergedTiers[k] = tiers[i];
                i++;
            } else {
                mergedBills[k] = bills[j];
                mergedNames[k] = names[j];
                mergedTiers[k] = tiers[j];
                j++;
            }
            k++;
        }
        while (i <= middle) {
            mergedBills[k] = bills[i];
            mergedNames[k] = names[i];
            mergedTiers[k] = tiers[i];
            i++;
            k++;
        }
        while (j <= right) {
            mergedBills[k] = bills[j];
            mergedNames[k] = names[j];
            mergedTiers[k] = tiers[j];
            j++;
            k++;
        }
        for (k = 0; k < mergedBills.length; k++) {
            bills[left + k] = mergedBills[k];
            names[left + k] = mergedNames[k];
            tiers[left + k] = mergedTiers[k];
        }
    }

    // ========== 报表2:Room Utilisation & Status Report ==========

    /**
     * filter=房型+房态,按累积营收(merge sort)降序。累积营收是把这间房历史上
     * 每一笔CHECKED_IN/CHECKED_OUT的Booking(rate x晚数)加总——用现在的房价算,
     * 因为Room entity没有留历史房价。
     */
    void doRoomUtilisationReport() {
        String roomTypeFilter = frontDeskCLI.promptReportRoomTypeFilter();
        String statusFilter = frontDeskCLI.promptReportStatusFilter();

        ListInterface<Room> filteredRooms = new ArrayBasedList<>();
        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
            Room room = roomList.getEntry(i);
            boolean typeMatches = "ALL".equalsIgnoreCase(roomTypeFilter)
                    || room.getRoomType().equalsIgnoreCase(roomTypeFilter);
            boolean statusMatches = "ALL".equalsIgnoreCase(statusFilter)
                    || room.getStatus().equalsIgnoreCase(statusFilter);
            if (typeMatches && statusMatches) {
                filteredRooms.add(room);
            }
        }

        int n = filteredRooms.getNumberOfEntries();
        Room[] rooms = new Room[n];
        double[] revenue = new double[n];
        for (int i = 1; i <= n; i++) {
            rooms[i - 1] = filteredRooms.getEntry(i);
            revenue[i - 1] = calculateRoomRevenue(rooms[i - 1].getRoomNumber());
        }
        mergeSortRoomsByRevenueDescending(rooms, revenue, 0, n - 1);

        int occupied = 0;
        int available = 0;
        int inHousekeeping = 0;
        double idleLoss = 0.0;
        for (int i = 0; i < n; i++) {
            String status = rooms[i].getStatus();
            if ("OCCUPIED".equals(status)) {
                occupied++;
            } else if ("AVAILABLE".equals(status)) {
                available++;
                idleLoss = idleLoss + rooms[i].getNightlyRate();
            } else if ("NEEDS_CLEANING".equals(status) || "CLEANING_IN_PROGRESS".equals(status)
                    || "INSPECTED".equals(status)) {
                inHousekeeping++;
            }
        }
        double occupancyRate = (n == 0) ? 0.0 : (occupied * 100.0 / n);

        frontDeskCLI.displayRoomUtilisationReportHeader(roomTypeFilter, statusFilter,
                occupancyRate, occupied, available, inHousekeeping, idleLoss);
        if (n == 0) {
            frontDeskCLI.displayNoReportRecords();
        } else {
            for (int i = 0; i < n; i++) {
                frontDeskCLI.displayRoomUtilisationReportRow(rooms[i].getRoomNumber(),
                        rooms[i].getRoomType(), rooms[i].getStatus(), revenue[i]);
            }
        }
        frontDeskCLI.displayReportEnd();
    }

    /**
     * 把一间房历史上所有分配到它、已经入住过(CHECKED_IN或CHECKED_OUT)的Booking
     * 房费加总,当作这间房的累积营收。
     */
    private double calculateRoomRevenue(String roomNumber) {
        double total = 0.0;
        Room room = findRoom(roomNumber);
        double rate = (room == null) ? 0.0 : room.getNightlyRate();

        Iterator<Guest> guestIterator = guestTable.getIterator();
        while (guestIterator.hasNext()) {
            Guest guest = guestIterator.next();
            Iterator<Booking> bookingIterator = guest.getBookings().getIterator();
            while (bookingIterator.hasNext()) {
                Booking booking = bookingIterator.next();
                boolean sameRoom = roomNumber.equals(booking.getAssignedRoomNo());
                boolean wasOccupied = booking.getStatus() == BookingStatus.CHECKED_IN
                        || booking.getStatus() == BookingStatus.CHECKED_OUT;
                if (sameRoom && wasOccupied) {
                    total = total + rate * booking.getNumberOfNights();
                }
            }
        }
        return total;
    }

    /**
     * merge sort:按累积营收由大到小,revenue阵列跟着rooms一起换位置。
     */
    private void mergeSortRoomsByRevenueDescending(Room[] rooms, double[] revenue, int left, int right) {
        if (left >= right) {
            return;
        }
        int middle = (left + right) / 2;
        mergeSortRoomsByRevenueDescending(rooms, revenue, left, middle);
        mergeSortRoomsByRevenueDescending(rooms, revenue, middle + 1, right);

        Room[] mergedRooms = new Room[right - left + 1];
        double[] mergedRevenue = new double[right - left + 1];
        int i = left;
        int j = middle + 1;
        int k = 0;
        while (i <= middle && j <= right) {
            if (revenue[i] >= revenue[j]) {
                mergedRooms[k] = rooms[i];
                mergedRevenue[k] = revenue[i];
                i++;
            } else {
                mergedRooms[k] = rooms[j];
                mergedRevenue[k] = revenue[j];
                j++;
            }
            k++;
        }
        while (i <= middle) {
            mergedRooms[k] = rooms[i];
            mergedRevenue[k] = revenue[i];
            i++;
            k++;
        }
        while (j <= right) {
            mergedRooms[k] = rooms[j];
            mergedRevenue[k] = revenue[j];
            j++;
            k++;
        }
        for (k = 0; k < mergedRooms.length; k++) {
            rooms[left + k] = mergedRooms[k];
            revenue[left + k] = mergedRevenue[k];
        }
    }

    /**
     * Filters a guest's full booking history down to the ones still checked
     * in right now — history already checked out or cancelled isn't eligible.
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
     * Finds the checked-in booking that matches a given room number, so the
     * staff can select rooms to check out by number rather than booking ID.
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

    // ========== 输入重试(格式类校验失败就原地重问,不中止整个操作) ==========

    /**
     * 空白代表使用者要取消(回传null),跟"打了但不是8位数字"这种要重问的情况分开。
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
     * frontDeskCLI.promptExtraCharges()空白时回传Double.NaN代表取消,
     * 跟"打了但是负数/格式不对"这种要重问的情况分开。
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
     * Looks up a guest by confirmation number. The hash table derives the
     * bucket from the key, so the cost stays O(1) on average no matter how many
     * guests are registered.
     *
     * @param confirmationNumber the 8-digit confirmation number
     * @return the guest, or null when no guest carries that number
     */
    private Guest findGuest(String confirmationNumber) {
        return guestTable.getEntry(new Guest(confirmationNumber));
    }

    /**
     * Finds a room by room number with a linear scan of the room list. Kept
     * deliberately as the O(n) control group for the search efficiency report.
     *
     * @param roomNumber the room number to find
     * @return the room, or null when no such room exists
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

    /**
     * Replaces a null field with a dash so the console never prints "null".
     *
     * @param value the value to display
     * @return the value, or a dash when the value is null
     */
    private String orDash(String value) {
        return (value == null) ? "-" : value;
    }

    /**
     * Builds one display block per booking linked to this guest. Each booking
     * carries its own stay period, so the dates come from the booking rather
     * than from the guest record.
     *
     * @param guest the guest whose bookings are listed
     * @return the formatted booking lines, or a dash line when none exist
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
     * Builds one charge line per room the guest is still occupying.
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
     * Sums the nightly rate times the stay length for every room still
     * checked in.
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

    /**
     * @return how many rooms this guest is currently occupying
     */
    private int countCheckedInRooms(Guest guest) {

        int count = 0;
        for (int i = 1; i <= guest.getBookings().getNumberOfEntries(); i++) {
            if (guest.getBookings().getEntry(i).getStatus() == BookingStatus.CHECKED_IN) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return the total nights across every room this guest is occupying
     */
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
     * Builds one block per settled bill under this confirmation number.
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

    /**
     * @return the sum of every settled bill under this confirmation number
     */
    private double calculateSettledTotal(Guest guest) {

        double total = 0.0;
        for (int i = 1; i <= guest.getBillingRecords().getNumberOfEntries(); i++) {
            total = total + guest.getBillingRecords().getEntry(i).getTotalAmount();
        }
        return total;
    }

    /**
     * @return the loyalty points earned across every settled bill
     */
    private int calculateTotalPoints(Guest guest) {

        int points = 0;
        for (int i = 1; i <= guest.getBillingRecords().getNumberOfEntries(); i++) {
            points = points + guest.getBillingRecords().getEntry(i).getPointsEarned();
        }
        return points;
    }
    
    /**
     * Counts rooms matching an optional type and an optional status.
     *
     * <p>Passing null for either filter means "any value", so this one method
     * serves every count the availability screen and the utilisation report
     * need, instead of one method per combination.</p>
     *
     * @param typeFilter the room type to match, or null for any type
     * @param statusFilter the status to match, or null for any status
     * @return how many rooms match both filters
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
     * Builds one line per room type: how many are available, how many are not.
     *
     * @param typeFilter the single type to report, or null for every type
     * @return the formatted breakdown lines
     */
    private String buildTypeBreakdownLines(String typeFilter) {

        String[] allTypes = {"Standard", "Deluxe", "Suite"};
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
     * Lists every room that is available right now.
     *
     * <p>Only an exact AVAILABLE status counts as available. Testing for "not
     * OCCUPIED" instead would wrongly include rooms still in the housekeeping
     * pipeline and lead to overselling.</p>
     *
     * @param typeFilter the room type to list, or null for every type
     * @return the formatted room lines
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
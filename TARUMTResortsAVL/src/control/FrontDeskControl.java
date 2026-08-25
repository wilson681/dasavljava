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
     * filter=日期区间+客人等级,按总额降序(merge sort)。
     *
     * 刻意不做"最低金额"筛选:那会让 Total revenue 变成筛选后的子集,
     * 一份叫 Revenue Report 的东西,总收入不该因为筛选条件而失真。
     *
     * 这是给管理层看的营运报表,所以不做"单一客人"的筛选——那是
     * "3) View Billing Details" 那个 use case 的事,报表不该重复它。
     *
     * 等级用 guest.getTier(),那是入住那天的等级快照,反映"这笔消费发生当下客人
     * 是什么等级",不是现在的等级。
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
            // 帐单身上只有确认号,姓名/等级要靠它回查客人——hash table 查一次 O(1)
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

        // 贡献最多的那一级要先算出来,不能让 boundary 一边印一边比——
        // 那样它就得记住上一行的数字,变成在做业务判断了
        int topTierPosition = findTopTierPosition(tierRevenue);

        frontDeskCLI.displayRevenueByTier(
                tierNames.getIterator(), tierRevenue.getIterator(), totalRevenue,
                tierNames.getEntry(topTierPosition), tierRevenue.getEntry(topTierPosition));

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
     * 找出收入最高的那一级在清单里的位置。金额一样时取先出现的那一级
     * (等级本来就是按第一次出现的顺序加进清单的,不另外定优先)。
     *
     * @return 位置(从1开始);清单是空的时回传1,呼叫方在 n==0 时已经提前 return 了
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
     * merge sort:按 totalAmount 由大到小。
     *
     * 以前要同时搬三个平行阵列(帐单/客人名/等级),因为姓名和等级没存在帐单身上。
     * 现在改成印每一行时用确认号回查客人(hash table O(1)),就只剩一个阵列要搬,
     * 合并的程式码短了一半,也不会有"第 i 笔的帐单对到别人的名字"这种风险。
     *
     * 合并时用 >= 而不是 >:金额一样的两笔,左半边的先取用,保持稳定排序。
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

    // ========== 报表2:Room Utilisation & Status Report ==========

    /**
     * filter=预计退房日上限+房型,按预计退房日由近到远(merge sort)。
     *
     * 这份报表看的是"现在还住在店里、还没结帐"的客人——跟报表1(Check-Out Revenue)
     * 刚好互补:那份看已经收到的钱,这份看还没收的钱,合起来才是完整的财务图像。
     *
     * 一行代表一间还被占用的房间(同一位客人订了两间房就会有两行),因为前台和
     * 客房部关心的单位是房间,不是人。
     */
    void doInHouseGuestsReport() {
        String checkOutBefore = frontDeskCLI.promptReportToDate();
        String roomTypeFilter = frontDeskCLI.promptReportRoomTypeFilter();

        ListInterface<Booking> inHouse = new ArrayBasedList<>();
        ListInterface<Double> accrued = new ArrayBasedList<>();
        ListInterface<String> guestNames = new ArrayBasedList<>();
        ListInterface<String> guestTiers = new ArrayBasedList<>();

        /*
         * Linear Search
         *
         * Filter 1 = 还住在店里(CHECKED_IN);已退房的归报表1管
         * Filter 2 = 预计退房日在上限之前
         * Filter 3 = 房型
         */
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

        // Merge Sort:预计退房日由近到远——前台最先要处理的是今天就要走的客人
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

        // 排序后第一笔就是最快要走的那位
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
     * 把在店订单按预计退房日分组,让客房部看得出哪一天会空出几间房。
     * 资料已经按退房日排好序了,所以同一天的一定连在一起,扫一遍就能分组。
     */
    private void displayDepartureSchedule(ListInterface<Booking> inHouse) {

        frontDeskCLI.displayDepartureScheduleHeader();

        int i = 1;
        while (i <= inHouse.getNumberOfEntries()) {

            String date = orDash(inHouse.getEntry(i).getCheckOutDate());
            String rooms = "";
            int count = 0;

            // 同一天的连续几笔一起收掉
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
     * Merge Sort:预计退房日由近到远;同一天的房号小的排前面。
     *
     * 四条并行清单(订单/累计房费/客人名/等级)合并时必须一起搬,
     * 否则第 i 笔的房号会对到别人的名字。
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
     * 排序规则:退房日早的排前面;同一天的房号小的排前面。
     * 退房日是 "yyyy-MM-dd" 固定宽度,字串字典序刚好等于日期先后。
     *
     * @return true 代表 a 该排在 b 前面
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
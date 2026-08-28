package control;

import adt.ArrayBasedList;
import adt.ListInterface;
import boundary.LoyaltyCLI;
import entity.Member;
import entity.PointsLedgerEntry;
import entity.RedemptionItem;
import entity.RedemptionTransaction;
import java.time.LocalDate;
import java.util.Iterator;
import utility.TierRankUtility;
import utility.ValidationUtility;

/**
 * Controls the main operations of the Loyalty and Rewards Service module.
 * Member, redemption catalog and transaction records are managed through
 * list ADTs.
 *
 * @author Lim Wei Shern
 */
public class LoyaltyControl {

    // New points batches expire after this number of months.
    private static final int POINTS_VALIDITY_MONTHS = 12;

    private final LoyaltyCLI loyaltyCLI;
    private final ListInterface<Member> memberList;
    private final ListInterface<RedemptionItem> redemptionCatalog;
    private final ListInterface<RedemptionTransaction> redemptionHistory;

    private int ledgerCounter;

    public LoyaltyControl(LoyaltyCLI loyaltyCLI,
            ListInterface<Member> memberList,
            ListInterface<RedemptionItem> redemptionCatalog,
            ListInterface<RedemptionTransaction> redemptionHistory) {
        if (loyaltyCLI == null || memberList == null || redemptionCatalog == null || redemptionHistory == null) {
            throw new IllegalArgumentException("LoyaltyControl dependencies cannot be null");
        }
        this.loyaltyCLI = loyaltyCLI;
        this.memberList = memberList;
        this.redemptionCatalog = redemptionCatalog;
        this.redemptionHistory = redemptionHistory;
        this.ledgerCounter = 0;
    }

    // Main Menu
    public void run() {
        boolean running = true;
        while (running) {
            int choice = loyaltyCLI.displayMenuAndGetChoice();
            switch (choice) {
                case 1:
                    doViewExpiry();
                    break;
                case 2:
                    doRedeem();
                    break;
                case 3:
                    doAddPoints();
                    break;
                case 4:
                    doAdjustTier();
                    break;
                case 0:
                    running = false;
                    break;
                default:
                    loyaltyCLI.displayInvalidChoice();
            }
        }
    }

    // Option 1: View Points Expiry

    private void doViewExpiry() {

        if (memberList.isEmpty()) {
            loyaltyCLI.displayNoMembers();
            return;
        }

        loyaltyCLI.displayExpiryMemberTable(buildExpiryMemberRows());

        Member member = promptValidMember(null);

        if (member == null) {
            loyaltyCLI.displayCancelled();
            return;
        }

        loyaltyCLI.displayPointsExpiry(
                member,
                member.getPointsLedger().getIterator(),
                calculateActivePoints(member));
    }

    // Option 2: Redeem Points

    private void doRedeem() {
        Member member = promptValidMember("Please try again to continue the redemption.");
        if (member == null) {
            loyaltyCLI.displayCancelled();
            return;
        }

        // Sort the catalog by point cost in ascending order using selection sort.
        sortCatalogByPoints();
        // Active points exclude ledger batches that have already expired.
        int activePoints = calculateActivePoints(member);
        loyaltyCLI.displayCatalog(redemptionCatalog.getIterator(), activePoints);

        // The first catalog entry is the cheapest after sorting.
        if (!redemptionCatalog.isEmpty()
                && activePoints < redemptionCatalog.getEntry(1).getPointsRequired()) {
            loyaltyCLI.displayCannotAffordAnything(activePoints,
                    redemptionCatalog.getEntry(1).getPointsRequired());
            return;
        }

        RedemptionItem item = promptValidRedemptionChoice(activePoints);
        if (item == null) {
            loyaltyCLI.displayCancelled();
            return;
        }

        // Spend the actual points batches first (earliest expiry first), then reduce
        // the stored lifetime balance by the same amount so the two stay in step.
        consumePoints(member, item.getPointsRequired());
        member.setCurrentPoints(member.getCurrentPoints() - item.getPointsRequired());

        // Redemption records are stored separately from the Member entity.
        RedemptionTransaction transaction = new RedemptionTransaction(member.getMemberId(),
                item.getItemName(), item.getPointsRequired(), LocalDate.now().toString());
        redemptionHistory.add(transaction);

        loyaltyCLI.displayRedemptionResult(transaction, calculateActivePoints(member));
    }

    // Option 3: Add Points Manually

    private void doAddPoints() {

        if (memberList.isEmpty()) {
            loyaltyCLI.displayNoMembers();
            return;
        }

        loyaltyCLI.displayPointsMemberTable(buildPointsMemberRows());

        Member member = promptValidMember("Please try again to continue adding points.");
        if (member == null) {
            loyaltyCLI.displayCancelled();
            return;
        }

        int pointsAmount = promptValidPointsAmount();
        if (pointsAmount == Integer.MIN_VALUE) {
            loyaltyCLI.displayCancelled();
            return;
        }

        // Keep the original tier so any tier change can be displayed.
        String tierBefore = member.getTier();

        LocalDate earnedDate = LocalDate.now();
        LocalDate expiryDate = earnedDate.plusMonths(POINTS_VALIDITY_MONTHS);
        awardPoints(member, pointsAmount, earnedDate.toString(), expiryDate.toString());

        loyaltyCLI.displayAddPointsResult(member, pointsAmount, tierBefore, calculateActivePoints(member));
    }
    // Option 4: Manual Tier Downgrade

    /**
     * Manually moves a member to a lower tier.
     * The member's point balances are not changed.
     *
     * A later points award recalculates the tier from total points earned,
     * so a manual adjustment may be replaced by the calculated tier.
     */
    private void doAdjustTier() {

        if (memberList.isEmpty()) {
            loyaltyCLI.displayNoMembers();
            return;
        }

        loyaltyCLI.displayMemberTable(buildMemberRows());

        Member member = promptValidMemberToAdjust("Please try again to continue the tier adjustment.");
        if (member == null) {
            loyaltyCLI.displayCancelled();
            return;
        }

        String[] lowerTiers = buildLowerTiers(member.getTier());
        if (lowerTiers.length == 0) {
            loyaltyCLI.displayAlreadyLowestTier(member.getName(), member.getTier());
            return;
        }

        String oldTier = member.getTier();
        String newTier = promptValidTargetTier(oldTier, lowerTiers);
        if (newTier == null) {
            loyaltyCLI.displayCancelled();
            return;
        }

        if (!loyaltyCLI.promptConfirmAdjustment(member.getName(), oldTier, newTier)) {
            loyaltyCLI.displayAdjustmentCancelled();
            return;
        }

        member.setTier(newTier);

        String lastEarned = findLatestEarnedDate(member);
        loyaltyCLI.displayAdjustmentResult(
                buildMemberRow(member, lastEarned.isEmpty() ? "-" : lastEarned),
                oldTier, newTier,
                TierRankUtility.tierToDiscountPercent(oldTier),
                TierRankUtility.tierToDiscountPercent(newTier));
    }

    /**
     * Builds the list of tiers below the member's current tier.
     * The available tiers are returned from highest to lowest.
     *
     * @param currentTier the member's current tier
     * @return lower tiers, or an empty array if already at the lowest tier
     */
    private String[] buildLowerTiers(String currentTier) {

        String[] allTiers = { "Diamond", "Platinum", "Elite", "Standard" };
        int currentRank = TierRankUtility.tierToRank(currentTier);

        int count = 0;
        for (int i = 0; i < allTiers.length; i++) {
            if (TierRankUtility.tierToRank(allTiers[i]) < currentRank) {
                count++;
            }
        }

        String[] result = new String[count];
        int index = 0;
        for (int i = 0; i < allTiers.length; i++) {
            if (TierRankUtility.tierToRank(allTiers[i]) < currentRank) {
                result[index] = allTiers[i];
                index++;
            }
        }
        return result;
    }

    // Report 1: Points Expiry Report

    /**
     * Generates a report of points expiring within a selected period.
     * Entries are filtered by expiry date and member tier, then sorted
     * by expiry date in ascending order.
     */
    void doPointsExpiryReport() {
        int withinDays = loyaltyCLI.promptExpiryWindowDays();
        String tierFilter = loyaltyCLI.promptReportTierFilter();

        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(withinDays);

        ListInterface<String> memberNames = new ArrayBasedList<>();
        ListInterface<String> memberIds = new ArrayBasedList<>();
        ListInterface<String> tiers = new ArrayBasedList<>();
        ListInterface<Integer> pointsAmounts = new ArrayBasedList<>();
        ListInterface<String> expiryDates = new ArrayBasedList<>();

        // Keep unique member IDs because one member may have multiple
        // points batches expiring within the selected period.
        ListInterface<String> affectedMemberIds = new ArrayBasedList<>();

        // Parallel lists are used to group expiring points by tier.
        ListInterface<String> tierNames = new ArrayBasedList<>();
        ListInterface<Integer> tierPoints = new ArrayBasedList<>();

        int totalExpiringPoints = 0;

        Iterator<Member> memberIterator = memberList.getIterator();
        while (memberIterator.hasNext()) {
            Member member = memberIterator.next();
            boolean tierMatches = "ALL".equalsIgnoreCase(tierFilter) || tierFilter.equalsIgnoreCase(member.getTier());
            if (!tierMatches) {
                continue;
            }

            Iterator<PointsLedgerEntry> ledgerIterator = member.getPointsLedger().getIterator();
            while (ledgerIterator.hasNext()) {
                PointsLedgerEntry entry = ledgerIterator.next();
                LocalDate expiry = LocalDate.parse(entry.getExpiryDate());

                // Exclude points that are already expired or outside the window.
                if (expiry.isBefore(today) || expiry.isAfter(cutoff)) {
                    continue;
                }

                // Only what is still unspent can expire. A batch the member has
                // already redeemed in full has nothing left to warn about, and a
                // partly used one must report the remainder, not the original
                // amount, so this report agrees with the points ledger.
                int remainingPoints = entry.getRemainingPoints();
                if (remainingPoints <= 0) {
                    continue;
                }

                memberNames.add(member.getName());
                memberIds.add(member.getMemberId());
                tiers.add(member.getTier());
                pointsAmounts.add(remainingPoints);
                expiryDates.add(entry.getExpiryDate());

                totalExpiringPoints = totalExpiringPoints + remainingPoints;

                if (affectedMemberIds.indexOf(member.getMemberId()) == -1) {
                    affectedMemberIds.add(member.getMemberId());
                }

                addToTierPoints(tierNames, tierPoints, member.getTier(), remainingPoints);
            }
        }

        sortByExpiryDateAscending(memberNames, memberIds, tiers, pointsAmounts, expiryDates);

        loyaltyCLI.displayPointsExpiryReportHeader(withinDays, tierFilter);

        if (memberNames.isEmpty()) {
            loyaltyCLI.displayNoReportRecords();
            loyaltyCLI.displayReportEnd();
            return;
        }

        for (int i = 1; i <= memberNames.getNumberOfEntries(); i++) {
            // Days remaining are calculated at report time so the value stays current.
            int daysLeft = (int) java.time.temporal.ChronoUnit.DAYS.between(
                    today, LocalDate.parse(expiryDates.getEntry(i)));

            loyaltyCLI.displayPointsExpiryReportRow(memberNames.getEntry(i), memberIds.getEntry(i),
                    tiers.getEntry(i), pointsAmounts.getEntry(i), expiryDates.getEntry(i), daysLeft);
        }

        loyaltyCLI.displayPointsExpirySummary(
                affectedMemberIds.getNumberOfEntries(), totalExpiringPoints);

        loyaltyCLI.displayExpiringByTierHeader();
        loyaltyCLI.displayExpiringByTierRow("Diamond", tierPointsFor(tierNames, tierPoints, "Diamond"),
                totalExpiringPoints);
        loyaltyCLI.displayExpiringByTierRow("Platinum", tierPointsFor(tierNames, tierPoints, "Platinum"),
                totalExpiringPoints);
        loyaltyCLI.displayExpiringByTierRow("Elite", tierPointsFor(tierNames, tierPoints, "Elite"),
                totalExpiringPoints);
        loyaltyCLI.displayExpiringByTierRow("Standard", tierPointsFor(tierNames, tierPoints, "Standard"),
                totalExpiringPoints);

        loyaltyCLI.displayReportEnd();
    }

    /**
     * Groups expiring points by tier using parallel lists.
     */
    private void addToTierPoints(ListInterface<String> tierNames, ListInterface<Integer> tierPoints,
            String tier, int points) {
        int position = tierNames.indexOf(tier);
        if (position == -1) {
            tierNames.add(tier);
            tierPoints.add(points);
        } else {
            tierPoints.replace(position, tierPoints.getEntry(position) + points);
        }
    }

    /**
     * Returns the total expiring points recorded for a tier.
     *
     * @return the tier total, or 0 if the tier has no matching entries
     */
    private int tierPointsFor(ListInterface<String> tierNames, ListInterface<Integer> tierPoints,
            String tier) {
        int position = tierNames.indexOf(tier);
        return (position == -1) ? 0 : tierPoints.getEntry(position);
    }

    /**
     * Sorts expiring points by expiry date in ascending order
     * using selection sort.
     * Parallel lists are swapped together to keep their data aligned.
     */
    private void sortByExpiryDateAscending(ListInterface<String> memberNames, ListInterface<String> memberIds,
            ListInterface<String> tiers, ListInterface<Integer> pointsAmounts,
            ListInterface<String> expiryDates) {
        int n = expiryDates.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int smallestPosition = i;
            for (int j = i + 1; j <= n; j++) {
                if (expiryDates.getEntry(j).compareTo(expiryDates.getEntry(smallestPosition)) < 0) {
                    smallestPosition = j;
                }
            }
            if (smallestPosition != i) {
                swap(memberNames, i, smallestPosition);
                swap(memberIds, i, smallestPosition);
                swap(tiers, i, smallestPosition);
                swap(pointsAmounts, i, smallestPosition);
                swap(expiryDates, i, smallestPosition);
            }
        }
    }

    // Report 2: Top Redeemed Items Report

    /**
     * Generates a redemption report filtered by date and member tier.
     * Redemption records are grouped by item and sorted by redemption count
     * in descending order using selection sort.
     */
    void doTopRedeemedItemsReport() {
        String fromDate = loyaltyCLI.promptReportFromDate();
        String toDate = loyaltyCLI.promptReportToDate();
        String tierFilter = loyaltyCLI.promptReportTierFilter();

        ListInterface<String> itemNames = new ArrayBasedList<>();
        ListInterface<Integer> redemptionCounts = new ArrayBasedList<>();
        ListInterface<Integer> totalPointsUsed = new ArrayBasedList<>();

        // Parallel lists group redemption activity by member tier.
        ListInterface<String> tierNames = new ArrayBasedList<>();
        ListInterface<Integer> tierRedemptions = new ArrayBasedList<>();
        ListInterface<Integer> tierPointsUsed = new ArrayBasedList<>();

        int totalRedemptions = 0;
        int totalPointsSpent = 0;

        Iterator<RedemptionTransaction> iterator = redemptionHistory.getIterator();
        while (iterator.hasNext()) {
            RedemptionTransaction transaction = iterator.next();

            boolean dateMatches = transaction.getDate().compareTo(fromDate) >= 0
                    && transaction.getDate().compareTo(toDate) <= 0;

            // Redemption records store the member ID, so the member list
            // is searched to obtain the tier used for report grouping.
            Member member = findMemberById(transaction.getMemberId());
            boolean tierMatches = "ALL".equalsIgnoreCase(tierFilter)
                    || (member != null && tierFilter.equalsIgnoreCase(member.getTier()));

            if (!dateMatches || !tierMatches) {
                continue;
            }

            totalRedemptions++;
            totalPointsSpent = totalPointsSpent + transaction.getPointsUsed();

            int position = itemNames.indexOf(transaction.getItemRedeemed());
            if (position == -1) {
                itemNames.add(transaction.getItemRedeemed());
                redemptionCounts.add(1);
                totalPointsUsed.add(transaction.getPointsUsed());
            } else {
                redemptionCounts.replace(position, redemptionCounts.getEntry(position) + 1);
                totalPointsUsed.replace(position, totalPointsUsed.getEntry(position) + transaction.getPointsUsed());
            }

            // A transaction with a missing member can still contribute to
            // item totals, but it cannot be grouped by membership tier.
            if (member != null) {
                addToTierBehaviour(tierNames, tierRedemptions, tierPointsUsed,
                        member.getTier(), transaction.getPointsUsed());
            }
        }

        sortByRedemptionCountDescending(itemNames, redemptionCounts, totalPointsUsed);

        loyaltyCLI.displayTopRedeemedItemsReportHeader(fromDate, toDate, tierFilter);

        if (itemNames.isEmpty()) {
            loyaltyCLI.displayNoReportRecords();
            loyaltyCLI.displayReportEnd();
            return;
        }

        for (int i = 1; i <= itemNames.getNumberOfEntries(); i++) {
            // Point cost is retrieved from the current redemption catalog.
            loyaltyCLI.displayTopRedeemedItemsReportRow(itemNames.getEntry(i),
                    pointsRequiredFor(itemNames.getEntry(i)),
                    redemptionCounts.getEntry(i), totalPointsUsed.getEntry(i));
        }

        // The first entry has the highest redemption count after sorting.
        // The item using the most points is found separately.
        int biggestSink = findBiggestPointsSink(totalPointsUsed);

        loyaltyCLI.displayTopRedeemedItemsSummary(
                totalRedemptions, totalPointsSpent,
                itemNames.getEntry(1), redemptionCounts.getEntry(1),
                itemNames.getEntry(biggestSink), totalPointsUsed.getEntry(biggestSink));

        // The chart is based on redemption count rather than points used.
        loyaltyCLI.displayMostRedeemedItemsHeader();
        for (int i = 1; i <= itemNames.getNumberOfEntries(); i++) {
            loyaltyCLI.displayMostRedeemedItemsRow(itemNames.getEntry(i),
                    redemptionCounts.getEntry(i), totalRedemptions);
        }

        loyaltyCLI.displayRedemptionByTierHeader();
        printTierBehaviourRow(tierNames, tierRedemptions, tierPointsUsed, "Diamond");
        printTierBehaviourRow(tierNames, tierRedemptions, tierPointsUsed, "Platinum");
        printTierBehaviourRow(tierNames, tierRedemptions, tierPointsUsed, "Elite");
        printTierBehaviourRow(tierNames, tierRedemptions, tierPointsUsed, "Standard");

        loyaltyCLI.displayReportEnd();
    }

    /**
     * Groups redemption count and point usage by tier using parallel lists.
     */
    private void addToTierBehaviour(ListInterface<String> tierNames, ListInterface<Integer> tierRedemptions,
            ListInterface<Integer> tierPointsUsed, String tier, int pointsUsed) {
        int position = tierNames.indexOf(tier);
        if (position == -1) {
            tierNames.add(tier);
            tierRedemptions.add(1);
            tierPointsUsed.add(pointsUsed);
        } else {
            tierRedemptions.replace(position, tierRedemptions.getEntry(position) + 1);
            tierPointsUsed.replace(position, tierPointsUsed.getEntry(position) + pointsUsed);
        }
    }

    /**
     * Displays the redemption totals for one membership tier.
     * Tiers without matching transactions are displayed with zero values.
     */
    private void printTierBehaviourRow(ListInterface<String> tierNames, ListInterface<Integer> tierRedemptions,
            ListInterface<Integer> tierPointsUsed, String tier) {
        int position = tierNames.indexOf(tier);
        int redemptions = (position == -1) ? 0 : tierRedemptions.getEntry(position);
        int pointsUsed = (position == -1) ? 0 : tierPointsUsed.getEntry(position);
        loyaltyCLI.displayRedemptionByTierRow(tier, redemptions, pointsUsed);
    }

    /**
     * Searches the redemption catalog for an item's current point cost
     * using a linear scan.
     *
     * @param itemName the item name to find
     * @return the required points, or -1 if the item is not in the catalog
     */
    private int pointsRequiredFor(String itemName) {
        for (int i = 1; i <= redemptionCatalog.getNumberOfEntries(); i++) {
            RedemptionItem item = redemptionCatalog.getEntry(i);
            if (item.getItemName().equalsIgnoreCase(itemName)) {
                return item.getPointsRequired();
            }
        }
        return -1;
    }

    /**
     * Finds the 1-based position of the item with the highest
     * total point usage.
     *
     * @return the position of the item with the highest point usage
     */
    private int findBiggestPointsSink(ListInterface<Integer> totalPointsUsed) {
        int topPosition = 1;
        for (int i = 2; i <= totalPointsUsed.getNumberOfEntries(); i++) {
            if (totalPointsUsed.getEntry(i) > totalPointsUsed.getEntry(topPosition)) {
                topPosition = i;
            }
        }
        return topPosition;
    }

    /**
     * Sorts redemption items by redemption count in descending order
     * using selection sort.
     * Total points used is the tie-breaker, and all parallel lists
     * are swapped together.
     */
    private void sortByRedemptionCountDescending(ListInterface<String> itemNames,
            ListInterface<Integer> redemptionCounts,
            ListInterface<Integer> totalPointsUsed) {
        int n = itemNames.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int largestPosition = i;
            for (int j = i + 1; j <= n; j++) {
                if (rankedHigher(redemptionCounts.getEntry(j), totalPointsUsed.getEntry(j),
                        redemptionCounts.getEntry(largestPosition),
                        totalPointsUsed.getEntry(largestPosition))) {
                    largestPosition = j;
                }
            }
            if (largestPosition != i) {
                swap(itemNames, i, largestPosition);
                swap(redemptionCounts, i, largestPosition);
                swap(totalPointsUsed, i, largestPosition);
            }
        }
    }

    /**
     * Compares two redemption items for report sorting.
     * Higher redemption count comes first, followed by total points used.
     *
     * @return true if item A should appear before item B
     */
    private boolean rankedHigher(int countA, int pointsA, int countB, int pointsB) {
        if (countA != countB) {
            return countA > countB;
        }
        return pointsA > pointsB;
    }

    /**
     * Swaps two 1-based positions in a list.
     * Used to keep parallel lists aligned during sorting.
     */
    private <T> void swap(ListInterface<T> list, int positionA, int positionB) {
        T temp = list.getEntry(positionA);
        list.replace(positionA, list.getEntry(positionB));
        list.replace(positionB, temp);
    }

    // Points Management

    /**
     * Adds a new points batch, updates the member's balances and recalculates
     * the membership tier from total points earned.
     *
     * @param member       the member receiving the points
     * @param pointsAmount number of points earned
     * @param earnedDate   date the points were earned
     * @param expiryDate   date the points expire
     */
    public void awardPoints(Member member, int pointsAmount, String earnedDate, String expiryDate) {
        ledgerCounter++;
        String ledgerId = "PL" + String.format("%06d", ledgerCounter);
        PointsLedgerEntry entry = new PointsLedgerEntry(ledgerId, member.getMemberId(),
                pointsAmount, earnedDate, expiryDate);

        member.addPointsEntry(entry);
        member.setCurrentPoints(member.getCurrentPoints() + pointsAmount);
        member.setTotalPointsEarned(member.getTotalPointsEarned() + pointsAmount);

        String newTier = TierRankUtility.pointsToTier(member.getTotalPointsEarned());
        if (!newTier.equals(member.getTier())) {
            member.setTier(newTier);
        }
    }

    /**
     * Awards points to a member identified by member ID.
     * Used by the Front-Desk module after check-out.
     *
     * @param memberId     the member ID
     * @param pointsAmount number of points earned
     * @return the updated member, or null if the member is not found
     */
    public Member awardPointsByMemberId(String memberId, int pointsAmount) {
        Member member = findMemberById(memberId);
        if (member == null) {
            return null;
        }
        LocalDate earnedDate = LocalDate.now();
        LocalDate expiryDate = earnedDate.plusMonths(POINTS_VALIDITY_MONTHS);
        awardPoints(member, pointsAmount, earnedDate.toString(), expiryDate.toString());
        return member;
    }

    /**
     * Returns a member's current tier for use by the Front-Desk module.
     *
     * @param memberId the member ID
     * @return the current tier, or null if the member is not found
     */
    public String getTierByMemberId(String memberId) {
        Member member = findMemberById(memberId);
        return (member == null) ? null : member.getTier();
    }
    // Member Display Helpers

    /**
     * Builds member display rows ordered by the latest earned date.
     * Members with no earned-date record appear first.
     */
    private String buildMemberRows() {

        ListInterface<Member> members = new ArrayBasedList<>();
        ListInterface<String> lastEarnedDates = new ArrayBasedList<>();

        for (int i = 1; i <= memberList.getNumberOfEntries(); i++) {
            Member member = memberList.getEntry(i);
            members.add(member);
            lastEarnedDates.add(findLatestEarnedDate(member));
        }

        sortByLastEarnedAscending(members, lastEarnedDates);

        String result = "";
        for (int i = 1; i <= members.getNumberOfEntries(); i++) {
            String shown = lastEarnedDates.getEntry(i).isEmpty()
                    ? "-"
                    : lastEarnedDates.getEntry(i);
            result = result + buildMemberRow(members.getEntry(i), shown);
        }
        return result;
    }

    /**
     * Sorts members by latest earned date in ascending order
     * using selection sort.
     * The parallel date list is moved together with the member list.
     */
    private void sortByLastEarnedAscending(ListInterface<Member> members,
            ListInterface<String> lastEarnedDates) {
        int n = lastEarnedDates.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int smallestPosition = i;
            for (int j = i + 1; j <= n; j++) {
                if (lastEarnedDates.getEntry(j)
                        .compareTo(lastEarnedDates.getEntry(smallestPosition)) < 0) {
                    smallestPosition = j;
                }
            }
            if (smallestPosition != i) {
                swap(members, i, smallestPosition);
                swap(lastEarnedDates, i, smallestPosition);
            }
        }
    }

    /**
     * Builds one formatted row for the member table.
     */
    private String buildMemberRow(Member member, String lastEarned) {
        return String.format("%-9s| %-20s| %8d | %-11s| %s%n",
                member.getMemberId(),
                member.getName(),
                calculateActivePoints(member),
                member.getTier(),
                lastEarned);
    }

    /**
     * Builds the member directory used when viewing points expiry.
     * Members are sorted by member ID in ascending order.
     */
    private String buildExpiryMemberRows() {

        ListInterface<Member> members = new ArrayBasedList<>();
        for (int i = 1; i <= memberList.getNumberOfEntries(); i++) {
            members.add(memberList.getEntry(i));
        }

        sortByMemberIdAscending(members);

        String result = "";
        for (int i = 1; i <= members.getNumberOfEntries(); i++) {
            Member member = members.getEntry(i);
            result = result + String.format("%-9s| %-20s| %-11s%n",
                    member.getMemberId(),
                    member.getName(),
                    member.getTier());
        }
        return result;
    }

    /**
     * Builds the member directory used when manually adding points.
     * Active points are shown instead of the stored point balance.
     */
    private String buildPointsMemberRows() {

        ListInterface<Member> members = new ArrayBasedList<>();
        for (int i = 1; i <= memberList.getNumberOfEntries(); i++) {
            members.add(memberList.getEntry(i));
        }

        sortByMemberIdAscending(members);

        String result = "";
        for (int i = 1; i <= members.getNumberOfEntries(); i++) {
            Member member = members.getEntry(i);
            result = result + String.format("%-9s| %-20s| %8d | %s%n",
                    member.getMemberId(),
                    member.getName(),
                    calculateActivePoints(member),
                    member.getTier());
        }
        return result;
    }

    /**
     * Sorts members by member ID in ascending order using selection sort.
     */
    private void sortByMemberIdAscending(ListInterface<Member> members) {
        int n = members.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int smallestPosition = i;
            for (int j = i + 1; j <= n; j++) {
                if (members.getEntry(j).getMemberId()
                        .compareTo(members.getEntry(smallestPosition).getMemberId()) < 0) {
                    smallestPosition = j;
                }
            }
            if (smallestPosition != i) {
                swap(members, i, smallestPosition);
            }
        }
    }

    // Input Validation

    /**
     * Re-prompts until an existing member is selected.
     * Blank input cancels the operation.
     *
     * @param retryHint optional message shown after a failed lookup
     * @return the matching member, or null if cancelled
     */
    private Member promptValidMember(String retryHint) {

        while (true) {

            String memberId = loyaltyCLI.promptMemberId();

            if (ValidationUtility.isBlank(memberId)) {
                return null;
            }

            Member member = findMemberById(memberId);
            if (member != null) {
                return member;
            }

            if (retryHint == null) {
                loyaltyCLI.displayMemberNotFound(memberId);
            } else {
                loyaltyCLI.displayMemberNotFound(memberId, retryHint);
            }
        }
    }

    /**
     * Re-prompts until an existing member is selected for tier adjustment.
     * Blank input cancels the operation.
     *
     * @param retryHint message shown after a failed lookup
     * @return the matching member, or null if cancelled
     */
    private Member promptValidMemberToAdjust(String retryHint) {

        while (true) {

            String memberId = loyaltyCLI.promptMemberIdToAdjust();

            if (ValidationUtility.isBlank(memberId)) {
                return null;
            }

            Member member = findMemberById(memberId);
            if (member != null) {
                return member;
            }

            loyaltyCLI.displayMemberNotFound(memberId, retryHint);
        }
    }

    /**
     * Re-prompts until a valid and affordable catalog item is selected.
     * Catalog numbers correspond to the 1-based positions in the list.
     *
     * @param activePoints the member's currently usable points
     * @return the selected item, or null if cancelled
     */

    private RedemptionItem promptValidRedemptionChoice(int activePoints) {

        while (true) {

            int itemNumber = loyaltyCLI.promptItemNumber();
            if (itemNumber == Integer.MIN_VALUE) {
                return null;
            }

            if (itemNumber < 1 || itemNumber > redemptionCatalog.getNumberOfEntries()) {
                loyaltyCLI.displayInvalidItemNumber(itemNumber,
                        redemptionCatalog.getNumberOfEntries());
                continue;
            }

            RedemptionItem item = redemptionCatalog.getEntry(itemNumber);
            if (activePoints < item.getPointsRequired()) {
                loyaltyCLI.displayInsufficientPoints(activePoints, item.getPointsRequired());
                continue;
            }

            return item;
        }
    }

    /**
     * Re-prompts until a positive points amount is entered.
     *
     * @return the entered amount, or Integer.MIN_VALUE if cancelled
     */
    private int promptValidPointsAmount() {
        int pointsAmount;
        while (true) {
            pointsAmount = loyaltyCLI.promptPointsAmount();
            if (pointsAmount == Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            if (pointsAmount > 0) {
                return pointsAmount;
            }
            loyaltyCLI.displayInvalidPointsAmount(pointsAmount);
        }
    }

    /**
     * Re-prompts until a valid lower tier is selected.
     * null represents cancellation, while an empty string represents
     * invalid non-blank input.
     *
     * @return the selected tier, or null if cancelled
     */
    private String promptValidTargetTier(String currentTier, String[] options) {
        String newTier;
        while (true) {
            newTier = loyaltyCLI.promptTargetTier(currentTier, options);
            if (newTier == null) {
                return null;
            }
            if (!newTier.isEmpty()) {
                return newTier;
            }
            loyaltyCLI.displayInvalidTier();
        }
    }

    // Internal Helpers

    /**
     * Spends points from the member's ledger, earliest expiry first, so the batch
     * closest to being lost is used up before the ones that are still good for a
     * while. Consuming the batch is also what stops the same points from being
     * deducted twice: once at redemption and again when that batch expires.
     * A redemption larger than one batch spills over into the next.
     *
     * @param member the member redeeming points
     * @param amount how many points the redemption costs
     */
    private void consumePoints(Member member, int amount) {
        ListInterface<PointsLedgerEntry> batches = new ArrayBasedList<>();
        LocalDate today = LocalDate.now();
        Iterator<PointsLedgerEntry> iterator = member.getPointsLedger().getIterator();
        while (iterator.hasNext()) {
            PointsLedgerEntry entry = iterator.next();
            if (!LocalDate.parse(entry.getExpiryDate()).isBefore(today)
                    && entry.getRemainingPoints() > 0) {
                batches.add(entry);
            }
        }
        sortBatchesByExpiryDateAscending(batches);

        int outstanding = amount;
        for (int i = 1; i <= batches.getNumberOfEntries() && outstanding > 0; i++) {
            outstanding -= batches.getEntry(i).consume(outstanding);
        }
    }

    /**
     * Sorts points batches by expiry date in ascending order using selection sort.
     * The ledger happens to be stored in expiry order today, but sorting here means
     * the spending order stays correct even if a back-dated batch is added later.
     */
    private void sortBatchesByExpiryDateAscending(ListInterface<PointsLedgerEntry> batches) {
        int n = batches.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int smallestPosition = i;
            for (int j = i + 1; j <= n; j++) {
                if (batches.getEntry(j).getExpiryDate()
                        .compareTo(batches.getEntry(smallestPosition).getExpiryDate()) < 0) {
                    smallestPosition = j;
                }
            }
            if (smallestPosition != i) {
                swap(batches, i, smallestPosition);
            }
        }
    }

    /**
     * Calculates the usable point balance by adding up what is left in every batch
     * that has not expired yet.
     * Expired entries are kept for history but contribute nothing, and points that
     * were already redeemed are gone from their batch, so nothing is counted twice.
     */
    private int calculateActivePoints(Member member) {
        int active = 0;
        LocalDate today = LocalDate.now();
        Iterator<PointsLedgerEntry> iterator = member.getPointsLedger().getIterator();
        while (iterator.hasNext()) {
            PointsLedgerEntry entry = iterator.next();
            if (!LocalDate.parse(entry.getExpiryDate()).isBefore(today)) {
                active += entry.getRemainingPoints();
            }
        }
        return active;
    }

    /**
     * Finds the latest earned date in a member's points ledger.
     * Non-null dates use yyyy-MM-dd format, so string comparison
     * follows chronological order.
     *
     * @param member the member to check
     * @return the latest earned date, or an empty string if none exists
     */
    private String findLatestEarnedDate(Member member) {

        String latest = "";
        Iterator<PointsLedgerEntry> iterator = member.getPointsLedger().getIterator();
        while (iterator.hasNext()) {
            String earned = iterator.next().getEarnedDate();
            // Dates use yyyy-MM-dd format, so string comparison follows chronological order.
            if (earned != null && earned.compareTo(latest) > 0) {
                latest = earned;
            }
        }
        return latest;
    }

    /**
     * Searches for a member by member ID using a linear scan.
     * The search takes O(n) time in the worst case.
     *
     * @param memberId the member ID to find
     * @return the matching member, or null if not found
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
     * Sorts the redemption catalog by required points in ascending order
     * using selection sort.
     * The list is modified through its position-based ADT operations.
     */
    private void sortCatalogByPoints() {
        int n = redemptionCatalog.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int minPosition = i;
            RedemptionItem minItem = redemptionCatalog.getEntry(i);
            for (int j = i + 1; j <= n; j++) {
                RedemptionItem candidate = redemptionCatalog.getEntry(j);
                if (candidate.getPointsRequired() < minItem.getPointsRequired()) {
                    minPosition = j;
                    minItem = candidate;
                }
            }
            if (minPosition != i) {
                RedemptionItem temp = redemptionCatalog.getEntry(i);
                redemptionCatalog.replace(i, minItem);
                redemptionCatalog.replace(minPosition, temp);
            }
        }
    }
}

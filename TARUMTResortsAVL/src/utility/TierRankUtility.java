package utility;

/**
 * TierRankUtility.java
 * Utility class — converts a member tier (text) into a rank number, for
 * Module 2's AVL Tree ordering to use.
 *
 * Notes:
 * - Contains only static methods with no state, following the Utility class
 *   convention
 * - A bigger number means a higher tier: Diamond=3 > Platinum=2 > Elite=1
 * - Non-VIP members (Standard/unknown tier) always return 0, ranked after
 *   all VIP tiers
 */
public class TierRankUtility {

    private TierRankUtility() {
        // Prevents external instantiation; a pure static utility class.
    }

    /**
     * Converts a tier name into a rank number.
     * @param tier tier name (Elite/Platinum/Diamond, case-insensitive)
     * @return rank number, bigger means higher tier; unrecognized tiers
     *         return 0
     */
    public static int tierToRank(String tier) {
        if (tier == null) {
            return 0;
        }
        switch (tier.trim().toUpperCase()) {
            case "DIAMOND":
                return 3;
            case "PLATINUM":
                return 2;
            case "ELITE":
                return 1;
            default:
                return 0;
        }
    }

    /**
     * Converts a rank number back into a tier name, so reports can display
     * Booking.tierRankAtRequest (the tier rank at the time of booking) as
     * text — the current Member tier can't be used instead, because the
     * member may have upgraded or downgraded since; the report needs to
     * show the tier at the time of booking, not the tier now.
     * @param rank the rank number
     * @return the corresponding tier name; unrecognized numbers return
     *         "Standard"
     */
    public static String rankToTier(int rank) {
        switch (rank) {
            case 3:
                return "Diamond";
            case 2:
                return "Platinum";
            case 1:
                return "Elite";
            default:
                return "Standard";
        }
    }

    /**
     * Converts a member's lifetime total points (totalPointsEarned) into
     * the tier they should be at, for Module 5's upgrade/downgrade check.
     * The thresholds are kept low, matching the "RM10 spent = 1 point" earn
     * rate, so tiers are reachable within a reasonable number of stays.
     * @param totalPointsEarned lifetime total points earned, which only
     *        ever increases, so this function naturally only ever upgrades
     * @return the corresponding tier name (Standard/Elite/Platinum/Diamond)
     */
    public static String pointsToTier(int totalPointsEarned) {
        if (totalPointsEarned >= 5000) {
            return "Diamond";
        } else if (totalPointsEarned >= 1500) {
            return "Platinum";
        } else if (totalPointsEarned >= 300) {
            return "Elite";
        } else {
            return "Standard";
        }
    }

    /**
     * Converts a tier name into a room rate discount percentage — one of
     * Module 5's "personalized promotions", hardcoded, affecting only price
     * display/calculation and not real business logic like room type or
     * room status.
     * @param tier tier name, case-insensitive
     * @return discount percentage (integer 0~100); unrecognized tiers
     *         (including Standard) return 0, no discount
     */
    public static int tierToDiscountPercent(String tier) {
        if (tier == null) {
            return 0;
        }
        switch (tier.trim().toUpperCase()) {
            case "DIAMOND":
                return 15;
            case "PLATINUM":
                return 10;
            case "ELITE":
                return 5;
            default:
                return 0;
        }
    }
}

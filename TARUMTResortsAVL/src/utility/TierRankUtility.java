package utility;

/*
 * Converts member tiers into values used for priority, points and discounts.
 */
public class TierRankUtility {
 // Utility class should not be instantiated.
    private TierRankUtility() {
        
    }

    /*
     * Converts a tier into a rank for AVL priority ordering.
     * Diamond = 3, Platinum = 2, Elite = 1, Standard = 0.
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

      // Converts a stored rank back into its tier name.
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

    /*
     * Determines member tier from total points earned.
     * Elite = 300, Platinum = 1500, Diamond = 5000.
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

    /*
     * Returns the room discount based on member tier.
     * Elite = 5%, Platinum = 10%, Diamond = 15%.
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

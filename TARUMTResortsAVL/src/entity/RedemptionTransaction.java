package entity;

/**
 * RedemptionTransaction.java
 * Entity class -- represents one points-redemption transaction record.
 *
 * Notes:
 * - This is a plain data class (POJO), only holds data for one redemption record.
 * - Contains no input (Scanner) or output (System.out) statements, per Entity class rules.
 * - This record is produced by the Control layer when a "redeem" action happens;
 *   it does not need to be pre-loaded from a txt file.
 */
public class RedemptionTransaction {

    // ========== Data fields ==========
    private String memberId;        // which member made the redemption
    private String itemRedeemed;    // what was redeemed, matches RedemptionItem's name
    private int pointsUsed;         // how many points were spent on this redemption
    private String date;            // redemption date

    /**
     * Constructor.
     */
    public RedemptionTransaction(String memberId, String itemRedeemed, int pointsUsed, String date) {
        this.memberId = memberId;
        this.itemRedeemed = itemRedeemed;
        this.pointsUsed = pointsUsed;
        this.date = date;
    }

    // ========== Getters ==========
    // Once created, a redemption record should not be modified, so only getters are provided, no setters.

    public String getMemberId() {
        return memberId;
    }

    public String getItemRedeemed() {
        return itemRedeemed;
    }

    public int getPointsUsed() {
        return pointsUsed;
    }

    public String getDate() {
        return date;
    }

    // ========== Overridden methods ==========

    /**
     * toString: shows a summary of this redemption record on the console.
     */
    @Override
    public String toString() {
        return date + " | " + itemRedeemed + " | -" + pointsUsed + " pts";
    }

    /**
     * equals: two records are considered the same by comparing member ID, date,
     * and redeemed item together.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RedemptionTransaction)) {
            return false;
        }
        RedemptionTransaction other = (RedemptionTransaction) obj;
        return this.memberId.equals(other.memberId)
                && this.date.equals(other.date)
                && this.itemRedeemed.equals(other.itemRedeemed);
    }

    /**
     * hashCode: per Java convention, overriding equals() requires overriding hashCode() too.
     */
    @Override
    public int hashCode() {
        int result = memberId.hashCode();
        result = 31 * result + date.hashCode();
        result = 31 * result + itemRedeemed.hashCode();
        return result;
    }
}
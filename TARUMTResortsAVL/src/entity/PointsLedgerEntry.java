package entity;

/**
 * PointsLedgerEntry.java
 * Entity class -- represents one detail record of a member's points batch.
 *
 * Notes:
 * - This is a plain data class (POJO), only holds data for when a points batch
 *   was earned and when it expires.
 * - Contains no input (Scanner) or output (System.out) statements, per Entity class rules.
 * - Different from Member.totalPointsEarned (lifetime cumulative total): this class
 *   records one individual entry, and a member can have many entries. Together they
 *   show which points are about to expire and whether points were earned recently
 *   (used for expiry reminders and tier downgrade checks).
 */
public class PointsLedgerEntry {

    // ========== Data fields ==========
    private String ledgerId;      // this entry's own unique ID
    private String memberId;      // which member this belongs to
    private int pointsAmount;     // how many points this batch earned
    private String earnedDate;    // date earned
    private String expiryDate;    // expiry date

    /**
     * Constructor.
     */
    public PointsLedgerEntry(String ledgerId, String memberId, int pointsAmount,
                              String earnedDate, String expiryDate) {
        this.ledgerId = ledgerId;
        this.memberId = memberId;
        this.pointsAmount = pointsAmount;
        this.earnedDate = earnedDate;
        this.expiryDate = expiryDate;
    }

    // ========== Getters ==========
    // Once created, an entry should not be modified, so only getters are provided, no setters.

    public String getLedgerId() {
        return ledgerId;
    }

    public String getMemberId() {
        return memberId;
    }

    public int getPointsAmount() {
        return pointsAmount;
    }

    public String getEarnedDate() {
        return earnedDate;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    // ========== Overridden methods ==========

    /**
     * toString: shows a summary of this ledger entry on the console.
     */
    @Override
    public String toString() {
        return earnedDate + " | +" + pointsAmount + " pts | Expires: " + expiryDate;
    }

    /**
     * equals: two entries are the same based on ledgerId alone (not memberId,
     * since one member has many entries).
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PointsLedgerEntry)) {
            return false;
        }
        PointsLedgerEntry other = (PointsLedgerEntry) obj;
        return this.ledgerId.equals(other.ledgerId);
    }

    /**
     * hashCode: per Java convention, overriding equals() requires overriding hashCode() too.
     */
    @Override
    public int hashCode() {
        return ledgerId.hashCode();
    }
}

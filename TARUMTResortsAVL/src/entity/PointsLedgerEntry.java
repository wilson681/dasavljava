package entity;

/**
 * Represents one batch of points earned by a member.
 * Each entry stores the original points earned, how many of them are still
 * unspent, when the points were earned and when they expire.
 *
 * @author All
 */
public class PointsLedgerEntry {

    private String ledgerId;   
    private String memberId;   
    private int pointsAmount;  
    private String earnedDate; 
    private String expiryDate;   
    private int remainingPoints;

    public PointsLedgerEntry(String ledgerId, String memberId, int pointsAmount,
                              String earnedDate, String expiryDate) {
        this(ledgerId, memberId, pointsAmount, earnedDate, expiryDate, pointsAmount);
    }

    public PointsLedgerEntry(String ledgerId, String memberId, int pointsAmount,
                              String earnedDate, String expiryDate, int remainingPoints) {
        this.ledgerId = ledgerId;
        this.memberId = memberId;
        this.pointsAmount = pointsAmount;
        this.earnedDate = earnedDate;
        this.expiryDate = expiryDate;
        this.remainingPoints = remainingPoints;
    }

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

    /**
     * Returns how many points of this batch have not been spent yet.
     * A batch starts with its full amount and drops towards zero as the member
     * redeems, so an already spent batch adds nothing once it expires.
     *
     * @return the unspent points in this batch
     */
    public int getRemainingPoints() {
        return remainingPoints;
    }

    /**
     * Spends up to the requested amount from this batch.
     * A redemption walks the batches closest to expiring first and calls this on
     * each one until the whole cost is covered, so a single redemption may be
     * split across several batches.
     *
     * @param amount the points the redemption still needs
     * @return how many points were actually taken from this batch
     */
    public int consume(int amount) {
        int taken = Math.min(amount, remainingPoints);
        remainingPoints -= taken;
        return taken;
    }

  
    @Override
    public String toString() {
        return earnedDate + " | +" + pointsAmount + " pts | Expires: " + expiryDate;
    }
    // Ledger ID is used to identify each points entry.
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

    // Uses ledger ID to generate the hash code.
    @Override
    public int hashCode() {
        return ledgerId.hashCode();
    }
}

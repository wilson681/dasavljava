package entity;

/*
 * Represents one batch of points earned by a member.
 * Each entry stores when the points were earned and when they expire.
 */
public class PointsLedgerEntry {

    private String ledgerId;   
    private String memberId;   
    private int pointsAmount;  
    private String earnedDate; 
    private String expiryDate;   

    public PointsLedgerEntry(String ledgerId, String memberId, int pointsAmount,
                              String earnedDate, String expiryDate) {
        this.ledgerId = ledgerId;
        this.memberId = memberId;
        this.pointsAmount = pointsAmount;
        this.earnedDate = earnedDate;
        this.expiryDate = expiryDate;
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

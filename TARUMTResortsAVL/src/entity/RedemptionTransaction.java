package entity;

/*
 * Represents one points redemption transaction.
 *
 * @author All
 */
public class RedemptionTransaction {

    private String memberId;  
    private String itemRedeemed;   
    private int pointsUsed;        
    private String date;            

    public RedemptionTransaction(String memberId, String itemRedeemed, int pointsUsed, String date) {
        this.memberId = memberId;
        this.itemRedeemed = itemRedeemed;
        this.pointsUsed = pointsUsed;
        this.date = date;
    }

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

    @Override
    public String toString() {
        return date + " | " + itemRedeemed + " | -" + pointsUsed + " pts";
    }

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
 // Uses the same fields as equals() to generate the hash code.
    @Override
    public int hashCode() {
        int result = memberId.hashCode();
        result = 31 * result + date.hashCode();
        result = 31 * result + itemRedeemed.hashCode();
        return result;
    }
}
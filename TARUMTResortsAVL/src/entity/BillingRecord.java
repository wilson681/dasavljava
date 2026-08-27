package entity;

/*
 * Represents one settled bill for a guest.
 * A guest may have multiple billing records.
 *
 * @author All
 */
public class BillingRecord {

    private String billingId;            
    private String confirmationNumber;    
    private double roomFee;              
    private double extraCharges;          
    private double totalAmount;           
    private int pointsEarned;            
    private String date;                 

    public BillingRecord(String billingId, String confirmationNumber, double roomFee, double extraCharges,
                          double totalAmount, int pointsEarned, String date) {
        this.billingId = billingId;
        this.confirmationNumber = confirmationNumber;
        this.roomFee = roomFee;
        this.extraCharges = extraCharges;
        this.totalAmount = totalAmount;
        this.pointsEarned = pointsEarned;
        this.date = date;
    }

    public String getBillingId() {
        return billingId;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public double getRoomFee() {
        return roomFee;
    }

    public double getExtraCharges() {
        return extraCharges;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public String getDate() {
        return date;
    }

    @Override
    public String toString() {
        return billingId + " | " + date + " | Room Fee: RM" + roomFee + " | Extra: RM" + extraCharges
                + " | Total: RM" + totalAmount + " | +" + pointsEarned + " pts";
    }

   // Billing ID is used to identify each bill.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BillingRecord)) {
            return false;
        }
        BillingRecord other = (BillingRecord) obj;
        return this.billingId.equals(other.billingId);
    }

   // Uses billing ID to generate the hash code.
    @Override
    public int hashCode() {
        return billingId.hashCode();
    }
}

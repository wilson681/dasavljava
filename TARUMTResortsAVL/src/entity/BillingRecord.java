package entity;

/**
 * BillingRecord.java
 * Entity class -- represents the total bill settled at check-out for a stay.
 *
 * Notes:
 * - This is a plain data class (POJO), only holds data for one settled bill.
 * - Contains no input (Scanner) or output (System.out) statements, per Entity class rules.
 * - A guest's stay may be checked out in stages (e.g. two rooms checked out on
 *   different days), each producing its own bill, so one Guest maps to a list of
 *   BillingRecords, not just one.
 * - roomFee, totalAmount, and pointsEarned are all results already computed by the
 *   Control layer and passed in; the calculation formulas (rate x nights, spend-to-points
 *   ratio) live in the Control layer -- the Entity itself does no calculation.
 */
public class BillingRecord {

    // ========== Data fields ==========
    private String billingId;             // this bill's own unique ID, used to tell apart multiple bills for the same guest
    private String confirmationNumber;    // which guest this bill belongs to (multiple bills for the same guest share this)
    private double roomFee;               // room fee = rate x number of nights
    private double extraCharges;          // extra charges, a single total manually entered by the front desk at checkout, uncategorized
    private double totalAmount;           // total amount = roomFee + extraCharges
    private int pointsEarned;             // points earned from this spend
    private String date;                  // checkout date

    /**
     * Constructor.
     */
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

    // ========== Getters ==========
    // Once a bill is settled, it should not be modified, so only getters are provided, no setters.

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

    // ========== Overridden methods ==========

    /**
     * toString: shows a summary of this bill on the console.
     */
    @Override
    public String toString() {
        return billingId + " | " + date + " | Room Fee: RM" + roomFee + " | Extra: RM" + extraCharges
                + " | Total: RM" + totalAmount + " | +" + pointsEarned + " pts";
    }

    /**
     * equals: two bills are the same based on billingId alone (not confirmationNumber,
     * since a guest may check out in stages with separate bills each).
     */
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

    /**
     * hashCode: per Java convention, overriding equals() requires overriding hashCode() too.
     */
    @Override
    public int hashCode() {
        return billingId.hashCode();
    }
}

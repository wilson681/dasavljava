package entity;

import adt.DoublyLinkedList;
import adt.ListInterface;

/**
 * Member.java
 * Entity class -- represents one member's data.
 *
 * Notes:
 * - This is a plain data class (POJO), only holds a member's data.
 * - Contains no input (Scanner) or output (System.out) statements, per Entity class rules.
 * - Contains no business logic like whether to upgrade or what the upgrade threshold
 *   is -- that belongs in the Control layer.
 *
 * On the design of the two points fields:
 *   currentPoints     -- current account balance, usable for redemption, goes up and
 *                         down with earning/redeeming.
 *   totalPointsEarned -- lifetime cumulative total, only ever increases, used only to
 *                         determine member tier.
 * This design avoids the unreasonable situation where redeeming points would cause a
 * member's tier to be downgraded.
 */
public class Member {

    // ========== Data fields ==========
    private String memberId;           // member ID, uniquely identifies this member
    private String name;               // member name
    private String phone;              // member contact phone
    private String tier;               // member tier (Elite/Platinum/Diamond)
    private int currentPoints;         // current redeemable points balance
    private int totalPointsEarned;     // lifetime cumulative points (used only for tier upgrades, unaffected by redemption)
    private ListInterface<PointsLedgerEntry> pointsLedger;   // this member's points-batch entries, used for expiry reminders/downgrade checks

    /**
     * Constructor -- a new member starts with no points-batch entries, so pointsLedger
     * is initialized as an empty List.
     */
    public Member(String memberId, String name, String phone, String tier,
                  int currentPoints, int totalPointsEarned) {
        this.memberId = memberId;
        this.name = name;
        this.phone = phone;
        this.tier = tier;
        this.currentPoints = currentPoints;
        this.totalPointsEarned = totalPointsEarned;
        this.pointsLedger = new DoublyLinkedList<>();
    }

    // ========== Getters ==========
    public String getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getTier() {
        return tier;
    }

    public int getCurrentPoints() {
        return currentPoints;
    }

    public int getTotalPointsEarned() {
        return totalPointsEarned;
    }

    public ListInterface<PointsLedgerEntry> getPointsLedger() {
        return pointsLedger;
    }

    // ========== Setters ==========
    // memberId and name don't change after creation, so no setters are provided.

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setTier(String tier) {
        // Called by the Control layer to update the tier after it decides the upgrade threshold is met.
        this.tier = tier;
    }

    public void setCurrentPoints(int currentPoints) {
        // Called by the Control layer when points are earned/redeemed.
        this.currentPoints = currentPoints;
    }

    public void setTotalPointsEarned(int totalPointsEarned) {
        // Called by the Control layer when points are earned (redemption does not affect this field).
        this.totalPointsEarned = totalPointsEarned;
    }

    /**
     * Adds a new points-batch entry (a plain data operation; when to add one and
     * how much is the Control layer's job).
     */
    public void addPointsEntry(PointsLedgerEntry entry) {
        pointsLedger.add(entry);
    }

    // ========== Overridden methods ==========

    /**
     * toString: shows a summary of this member on the console.
     */
    @Override
    public String toString() {
        return memberId + " | " + name + " | " + tier
                + " | Points: " + currentPoints + " (Total Earned: " + totalPointsEarned + ")";
    }

    /**
     * equals: two members are the same based on member ID alone.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Member)) {
            return false;
        }
        Member other = (Member) obj;
        return this.memberId.equals(other.memberId);
    }

    /**
     * hashCode: per Java convention, overriding equals() requires overriding hashCode() too.
     */
    @Override
    public int hashCode() {
        return memberId.hashCode();
    }
}
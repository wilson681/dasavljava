package entity;

import adt.DoublyLinkedList;
import adt.ListInterface;

/*
 * Represents a hotel member and their loyalty points.
 *
 * currentPoints     - points currently available for redemption
 * totalPointsEarned - total points earned over time, used for member tier
 *
 * @author All
 */
public class Member {

    private String memberId;          
    private String name;              
    private String phone;             
    private String tier;               
    private int currentPoints;         
    private int totalPointsEarned;     
    private ListInterface<PointsLedgerEntry> pointsLedger;   


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

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public void setCurrentPoints(int currentPoints) {
        this.currentPoints = currentPoints;
    }

    public void setTotalPointsEarned(int totalPointsEarned) {
        this.totalPointsEarned = totalPointsEarned;
    }

// Adds a new points record to the member's ledger.
    public void addPointsEntry(PointsLedgerEntry entry) {
        pointsLedger.add(entry);
    }

    @Override
    public String toString() {
        return memberId + " | " + name + " | " + tier
                + " | Points: " + currentPoints + " (Total Earned: " + totalPointsEarned + ")";
    }

   // Member ID is used to identify a member.
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
// Uses member ID to generate the hash code.
    @Override
    public int hashCode() {
        return memberId.hashCode();
    }
}
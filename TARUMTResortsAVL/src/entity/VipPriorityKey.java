package entity;

/**
 * VipPriorityKey.java - Composite sorting key for VIP room allocation.
 *
 * @author Wilson
 */

public class VipPriorityKey implements Comparable<VipPriorityKey> {

    private final int tierRank;
    private final int arrivalSequence;
    private final String confirmationNo;

    public VipPriorityKey(int tierRank, int arrivalSequence, String confirmationNo) {
        this.tierRank = tierRank;
        this.arrivalSequence = arrivalSequence;
        this.confirmationNo = confirmationNo;
    }

    public int getTierRank() {
        return tierRank;
    }

    public int getArrivalSequence() {
        return arrivalSequence;
    }

    public String getConfirmationNo() {
        return confirmationNo;
    }

    @Override
    public int compareTo(VipPriorityKey other) {
       // Reversed: a higher tier rank means higher priority, so the key is smaller.
        if (this.tierRank != other.tierRank) {
            return Integer.compare(other.tierRank, this.tierRank);
        }
        // Natural order: a smaller sequence means earlier arrival, so the key is smaller.
        if (this.arrivalSequence != other.arrivalSequence) {
            return Integer.compare(this.arrivalSequence, other.arrivalSequence);
        }
        return this.confirmationNo.compareTo(other.confirmationNo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VipPriorityKey)) {
            return false;
        }
        return this.compareTo((VipPriorityKey) o) == 0;
    }

    @Override
    public int hashCode() {
        return confirmationNo.hashCode();
    }

    @Override
    public String toString() {
        return String.format("[rank=%d seq=%d conf=%s]",
                tierRank, arrivalSequence, confirmationNo);
    }
}
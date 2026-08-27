package entity;

/*
 * Represents one item in the redemption catalog.
 *
 * @author All
 */
public class RedemptionItem {

    private String itemName;         
    private int pointsRequired;     

    public RedemptionItem(String itemName, int pointsRequired) {
        this.itemName = itemName;
        this.pointsRequired = pointsRequired;
    }

    public String getItemName() {
        return itemName;
    }

    public int getPointsRequired() {
        return pointsRequired;
    }

    @Override
    public String toString() {
        return itemName + " | " + pointsRequired + " pts";
    }
// Item name is used to identify a redemption item.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RedemptionItem)) {
            return false;
        }
        RedemptionItem other = (RedemptionItem) obj;
        return this.itemName.equals(other.itemName);
    }

    // Uses item name to generate the hash code.
    @Override
    public int hashCode() {
        return itemName.hashCode();
    }
}
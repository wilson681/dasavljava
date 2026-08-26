package entity;

/**
 * RedemptionItem.java
 * Entity class -- represents one option in the redemption catalog.
 *
 * Notes:
 * - This is a plain data class (POJO), only holds data for one redeemable item.
 * - Contains no input (Scanner) or output (System.out) statements, per Entity class rules.
 * - The redemption catalog currently loads fixed options from a txt file; if staff
 *   can add new items in the future, this class's structure does not need to change.
 */
public class RedemptionItem {

    // ========== Data fields ==========
    private String itemName;         // item name, e.g. "Free Breakfast"
    private int pointsRequired;      // points required to redeem this item

    /**
     * Constructor.
     */
    public RedemptionItem(String itemName, int pointsRequired) {
        this.itemName = itemName;
        this.pointsRequired = pointsRequired;
    }

    // ========== Getters ==========
    public String getItemName() {
        return itemName;
    }

    public int getPointsRequired() {
        return pointsRequired;
    }

    // ========== Overridden methods ==========

    /**
     * toString: shows a summary of this item on the console.
     */
    @Override
    public String toString() {
        return itemName + " | " + pointsRequired + " pts";
    }

    /**
     * equals: two items are the same based on item name alone.
     */
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

    /**
     * hashCode: per Java convention, overriding equals() requires overriding hashCode() too.
     */
    @Override
    public int hashCode() {
        return itemName.hashCode();
    }
}
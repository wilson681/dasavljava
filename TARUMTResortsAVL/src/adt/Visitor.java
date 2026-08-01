package adt;

/**
 * Visitor.java - Callback used while traversing a collection ADT.
 *
 * <p>The ADT decides the visiting order only. What happens at each entry
 * (printing, filtering, accumulating) is injected by the caller, so the ADT
 * never needs System.out and never changes when a new requirement appears.</p>
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author Wilson
 */
public interface Visitor<K, V> {

    /**
     * Processes the entry currently being visited.
     *
     * <p>The tree calls this method once per entry, in traversal order.
     * Both key and value are guaranteed to be non-null.</p>
     *
     * @param key the key of the current entry
     * @param value the value of the current entry
     */
    void visit(K key, V value);
}
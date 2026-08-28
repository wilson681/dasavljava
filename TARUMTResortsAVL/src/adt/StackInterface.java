package adt;

import java.util.Iterator;

/**
 * Stack ADT for storing entries in Last-In-First-Out (LIFO) order.
 * Entries are added and removed only from the top of the stack.
 *
 * @author Hoo Theng Qin
 * @param <T> type of entry stored in the stack
 */
public interface StackInterface<T> {

     /**
     * Adds a new entry to the top of the stack.
     *
     * @param newEntry the entry to add
     */
    public void push(T newEntry);

   /**
     * Removes and returns the top entry.
     *
     * @return the top entry, or null if the stack is empty
     */
    public T pop();

    /**
     * Returns the top entry without removing it.
     *
     * @return the top entry, or null if the stack is empty
     */
    public T peek();

   /**
     * Checks whether the stack is empty.
     *
     * @return true if the stack is empty
     */
    public boolean isEmpty();

    /**
     * Checks whether the stack has reached its capacity.
     *
     * @return true if the stack is full
     */
    public boolean isFull();

     /**
     * Removes all entries from the stack.
     */
    public void clear();

    /**
     * Returns the number of entries currently stored.
     *
     * @return the number of entries
     */
    public int size();

    /**
     * Returns an iterator that traverses the stack from top to bottom.
     *
     * @return an iterator for the stored entries
     */
    public Iterator<T> getIterator();
}

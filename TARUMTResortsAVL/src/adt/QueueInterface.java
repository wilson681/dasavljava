package adt;

import java.util.Iterator;

/**
 * QueueInterface.java
 * ADT Queue - a linear collection where entries go in at the back and come out from the
 * front, FIFO (First-In-First-Out)
 *
 * @author jagathis
 *
 * Source: [if this spec is based on course sample code / textbook, note it here;
 *          if self-designed, delete this line]
 *
 * @param <T> element type stored
 */
public interface QueueInterface<T> {

    /**
     * adds a new entry to the back of the queue.
     * @param newEntry entry to add
     */
    public void enqueue(T newEntry);

    /**
     * removes and returns the entry at the front.
     * @return the front entry, null if the queue is empty
     */
    public T dequeue();

    /**
     * looks at the front entry without removing it.
     * @return the front entry, null if the queue is empty
     */
    public T getFront();

    /**
     * removes the entry equal to the given one from anywhere in the queue (not just the
     * front) - used for something like cancelling mid-queue.
     * @param anEntry entry to remove (matched with equals())
     * @return true if found and removed, false otherwise
     */
    public boolean remove(T anEntry);

    /**
     * checks if this queue is empty.
     * @return true if empty
     */
    public boolean isEmpty();

    /**
     * checks if this queue is full (only means something for a fixed-capacity
     * implementation, a dynamically growing one is always false).
     * @return true if full
     */
    public boolean isFull();

    /**
     * clears this queue, removes all entries.
     */
    public void clear();

    /**
     * gets the current number of entries in this queue.
     * @return entry count
     */
    public int size();

    /**
     * gets an iterator that walks front to back.
     * @return iterator for this queue
     */
    public Iterator<T> getIterator();
}

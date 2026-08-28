package adt;

import java.util.Iterator;

/**
 * List ADT for storing entries in a linear sequence.
 * Entries are accessed by position and do not need to be unique or sorted.
 * List positions start from 1.
 *
 * @author Lim Wei Shern
 * @param <T> type of entry stored in the list
 */
public interface ListInterface<T> {

    /**
     * Adds a new entry to the end of the list.
     *
     * @param newEntry the entry to add
     * @return true if the entry is added successfully
     */
    public boolean add(T newEntry);

    /**
     * Inserts a new entry at a given position.
     * Existing entries from that position onward are shifted back.
     *
     * @param newPosition the position to insert at, starting from 1
     * @param newEntry    the entry to add
     * @return true if added successfully, or false if the position is invalid
     */
    public boolean add(int newPosition, T newEntry);

    /**
     * Removes the entry at a given position.
     * Entries after it are shifted forward.
     *
     * @param givenPosition the position to remove
     * @return the removed entry, or null if the position is invalid
     */
    public T remove(int givenPosition);

    /**
     * Removes all entries from the list.
     */
    public void clear();

    /**
     * Replaces the entry at a given position.
     *
     * @param givenPosition the position to replace
     * @param newEntry      the new entry
     * @return true if replaced successfully, or false if the position is invalid
     */
    public boolean replace(int givenPosition, T newEntry);

    /**
     * Returns the entry at a given position without removing it.
     *
     * @param givenPosition the position to retrieve
     * @return the entry at that position, or null if the position is invalid
     */
    public T getEntry(int givenPosition);

    /**
     * Finds the first position of a matching entry.
     *
     * @param anEntry the entry to search for
     * @return the 1-based position of the entry, or -1 if not found
     */
    public int indexOf(T anEntry);

    /**
     * Checks whether a matching entry exists in the list.
     *
     * @param anEntry the entry to check
     * @return true if the entry exists
     */
    public boolean contains(T anEntry);

    /**
     * Returns the number of entries currently stored.
     *
     * @return the number of entries
     */
    public int getNumberOfEntries();

    /**
     * Checks whether the list is empty.
     *
     * @return true if the list is empty
     */
    public boolean isEmpty();

    /**
     * Checks whether the list has reached its capacity.
     *
     * @return true if the list is full
     */
    public boolean isFull();

    /**
     * Returns an iterator that traverses the list from first to last.
     *
     * @return an iterator for the stored entries
     */
    public Iterator<T> getIterator();
}

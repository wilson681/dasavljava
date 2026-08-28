package adt;

import java.util.Iterator;

/**
 * Hash table ADT for storing and searching entries.
 * Uses hashCode() and equals() to find matching entries.
 *
 * Adapted from: BMCS2063 Sample Code, Chapter 10,
 * Modified to store T objects directly and support iteration.
 *
 * @author Lim Wei Shern
 */
public interface HashTableInterface<T> {

    /**
     * Adds a new entry to the hash table.
     *
     * @param newEntry the entry to add
     * @return true if the entry is added successfully
     */
    public boolean add(T newEntry);

    /**
     * Removes an entry that matches the given entry.
     *
     * @param anEntry the entry to remove
     * @return the removed entry, or null if not found
     */
    public T remove(T anEntry);

     /**
     * Searches for an entry that matches the given entry.
     *
     * @param anEntry the entry to search for
     * @return the matching entry, or null if not found
     */
    public T getEntry(T anEntry);

   /**
     * Checks whether a matching entry exists in the hash table.
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
     * Checks whether the hash table is empty.
     *
     * @return true if the hash table is empty
     */
    public boolean isEmpty();

    /**
     * Removes all entries from the hash table.
     */
    public void clear();

    /**
     * Returns an iterator for traversing all stored entries.
     * The traversal order is not guaranteed.
     *
     * @return an iterator for the stored entries
     */
    public Iterator<T> getIterator();
}

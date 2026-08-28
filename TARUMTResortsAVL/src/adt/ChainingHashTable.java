package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Hash table implementation using separate chaining.
 * Each bucket stores a linked chain of entries that produce the same
 * bucket index. Matching entries are identified using hashCode() and equals().
 *
 * @author Lim Wei Shern
 * @param <T> type of entry stored in the hash table
 */
public class ChainingHashTable<T> implements HashTableInterface<T> {

    private static final int DEFAULT_CAPACITY = 31;
    private Node<T>[] buckets;    
    private int numberOfEntries;  

    public ChainingHashTable() {
        buckets = createBucketArray(DEFAULT_CAPACITY);
        numberOfEntries = 0;
    }

    /**
     * Creates the bucket array.
     * Java does not allow direct creation of generic arrays.
     */
    @SuppressWarnings("unchecked")
    private Node<T>[] createBucketArray(int capacity) {
        return new Node[capacity];
    }

    /**
     * Maps an entry's hash code to a valid bucket index.
     * The sign bit is cleared so negative hash codes cannot produce
     * a negative array index.
     *
     * @param entry the entry to hash
     * @return the bucket index
     */
    private int getBucketIndex(T entry) {
        return (entry.hashCode() & 0x7FFFFFFF) % buckets.length;
    }

    @Override
    public boolean add(T newEntry) {
        int index = getBucketIndex(newEntry);
        // Insert at the front of the bucket chain in O(1) time.
        Node<T> newNode = new Node<>(newEntry);
        newNode.next = buckets[index];
        buckets[index] = newNode;
        numberOfEntries++;
        return true;
    }

    @Override
    public T remove(T anEntry) {
        int index = getBucketIndex(anEntry);
        Node<T> current = buckets[index];
        Node<T> previous = null;
        while (current != null) {
            if (current.data.equals(anEntry)) {
                 // Update the bucket head when removing the first node.
                if (previous == null) {
                    buckets[index] = current.next;
                } else {
                    previous.next = current.next;
                }
                numberOfEntries--;
                return current.data;
            }
            previous = current;
            current = current.next;
        }
        return null;
    }

    @Override
    public T getEntry(T anEntry) {
        int index = getBucketIndex(anEntry);
        Node<T> current = buckets[index];
        while (current != null) {
            if (current.data.equals(anEntry)) {
                return current.data;
            }
            current = current.next;
        }
        return null;
    }

    @Override
    public boolean contains(T anEntry) {
        return getEntry(anEntry) != null;
    }

    @Override
    public int getNumberOfEntries() {
        return numberOfEntries;
    }

    @Override
    public boolean isEmpty() {
        return numberOfEntries == 0;
    }

    @Override
    public void clear() {
        buckets = createBucketArray(DEFAULT_CAPACITY);
        numberOfEntries = 0;
    }

    @Override
    public Iterator<T> getIterator() {
        // Traverse each bucket chain and copy the entries into an array.
        // Hash-table traversal order is not guaranteed.
        @SuppressWarnings("unchecked")
        T[] entries = (T[]) new Object[numberOfEntries];
        int position = 0;
        for (int i = 0; i < buckets.length; i++) {
            Node<T> current = buckets[i];
            while (current != null) {
                entries[position] = current.data;
                position++;
                current = current.next;
            }
        }
        return new ArrayIterator(entries);
    }

    /**
     * Node used to link entries stored in the same bucket.
     */
    private class Node<E> {
        private E data;
        private Node<E> next;

        private Node(E data) {
            this.data = data;
        }
    }

    /**
     * Iterates through the entries collected from the hash table.
     */
    private class ArrayIterator implements Iterator<T> {
        private T[] items;
        private int currentIndex;

        private ArrayIterator(T[] items) {
            this.items = items;
            this.currentIndex = 0;
        }

        @Override
        public boolean hasNext() {
            return currentIndex < items.length;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return items[currentIndex++];
        }
    }
}

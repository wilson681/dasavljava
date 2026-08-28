package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * List implementation using a resizable array.
 * Positions follow the ListInterface convention and start from 1.
 *
 * @author Lim Wei Shern
 * @param <T> type of entry stored in the list
 */
public class ArrayBasedList<T> implements ListInterface<T> {

    private static final int DEFAULT_CAPACITY = 25;
    private T[] entries;
    private int numberOfEntries;

    public ArrayBasedList() {
        entries = createArray(DEFAULT_CAPACITY);
        numberOfEntries = 0;
    }

    /**
     * Creates the underlying array.
     * Java does not allow direct creation of generic arrays.
     */
    @SuppressWarnings("unchecked")
    private T[] createArray(int capacity) {
        return (T[]) new Object[capacity];
    }

    /**
     * Doubles the array capacity when the current array is full.
     */
    private void ensureCapacity() {
        if (numberOfEntries == entries.length) {
            T[] biggerArray = createArray(entries.length * 2);
            for (int i = 0; i < numberOfEntries; i++) {
                biggerArray[i] = entries[i];
            }
            entries = biggerArray;
        }
    }

    @Override
    public boolean add(T newEntry) {
        ensureCapacity();
        entries[numberOfEntries] = newEntry;
        numberOfEntries++;
        return true;
    }

    @Override
    public boolean add(int newPosition, T newEntry) {
        // Valid positions range from 1 to numberOfEntries + 1.
        if (newPosition < 1 || newPosition > numberOfEntries + 1) {
            return false;
        }
        ensureCapacity();
        // Shift entries backward from the end to avoid overwriting data.
        for (int i = numberOfEntries; i > newPosition - 1; i--) {
            entries[i] = entries[i - 1];
        }
        entries[newPosition - 1] = newEntry;
        numberOfEntries++;
        return true;
    }

    @Override
    public T remove(int givenPosition) {
        if (givenPosition < 1 || givenPosition > numberOfEntries) {
            return null;
        }
        T removed = entries[givenPosition - 1];
        // Shift later entries forward to close the removed position.
        for (int i = givenPosition - 1; i < numberOfEntries - 1; i++) {
            entries[i] = entries[i + 1];
        }
        // Remove the unused reference from the old last position.
        entries[numberOfEntries - 1] = null;
        numberOfEntries--;
        return removed;
    }

    @Override
    public void clear() {
        entries = createArray(DEFAULT_CAPACITY);
        numberOfEntries = 0;
    }

    @Override
    public boolean replace(int givenPosition, T newEntry) {
        if (givenPosition < 1 || givenPosition > numberOfEntries) {
            return false;
        }
        entries[givenPosition - 1] = newEntry;
        return true;
    }

    @Override
    public T getEntry(int givenPosition) {
        if (givenPosition < 1 || givenPosition > numberOfEntries) {
            return null;
        }
        return entries[givenPosition - 1];
    }

    @Override
    public int indexOf(T anEntry) {
        // Search sequentially and use equals() to identify a matching entry.
        for (int i = 0; i < numberOfEntries; i++) {
            if (entries[i].equals(anEntry)) {
                return i + 1; // Convert array index to 1-based list position.
            }
        }
        return -1;
    }

    @Override
    public boolean contains(T anEntry) {
        return indexOf(anEntry) != -1;
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
    public boolean isFull() {
        // The array expands automatically, so the list has no fixed capacity.
        return false;
    }

    @Override
    public Iterator<T> getIterator() {
        return new ArrayListIterator();
    }

    /**
     * Iterates through the list from first to last.
     */
    private class ArrayListIterator implements Iterator<T> {
        private int currentIndex;

        private ArrayListIterator() {
            currentIndex = 0;
        }

        @Override
        public boolean hasNext() {
            return currentIndex < numberOfEntries;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return entries[currentIndex++];
        }
    }
}

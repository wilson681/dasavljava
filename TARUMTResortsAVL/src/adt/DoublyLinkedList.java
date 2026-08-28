package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * List implementation using a doubly linked structure.
 * Each node stores references to both the previous and next nodes.
 * List positions follow the ListInterface convention and start from 1.
 *
 * @author Lim Wei Shern
 * @param <T> type of entry stored in the list
 */
public class DoublyLinkedList<T> implements ListInterface<T> {

    private Node<T> head;
    private Node<T> tail;
    private int numberOfEntries;

    public DoublyLinkedList() {
        head = null;
        tail = null;
        numberOfEntries = 0;
    }

    @Override
    public boolean add(T newEntry) {
        Node<T> newNode = new Node<>(newEntry);
        if (isEmpty()) {
            head = newNode;
        } else {
            tail.next = newNode;
            newNode.previous = tail;
        }
        tail = newNode;
        numberOfEntries++;
        return true;
    }

    @Override
    public boolean add(int newPosition, T newEntry) {
        // Valid positions range from 1 to numberOfEntries + 1.
        if (newPosition < 1 || newPosition > numberOfEntries + 1) {
            return false;
        }
        // Appending at the end can reuse the existing add operation.
        if (newPosition == numberOfEntries + 1) {
            return add(newEntry);
        }
        Node<T> newNode = new Node<>(newEntry);
        Node<T> current = getNode(newPosition);
        Node<T> previous = current.previous;

        newNode.next = current;
        newNode.previous = previous;
        current.previous = newNode;
        // Update head when inserting before the first node.
        if (previous == null) {
            head = newNode;
        } else {
            previous.next = newNode;
        }
        numberOfEntries++;
        return true;
    }

    @Override
    public T remove(int givenPosition) {
        Node<T> target = getNode(givenPosition);
        if (target == null) {
            return null;
        }
        Node<T> previous = target.previous;
        Node<T> next = target.next;
        // Update head when removing the first node.
        if (previous == null) {
            head = next;
        } else {
            previous.next = next;
        }
        // Update tail when removing the last node.
        if (next == null) {
            tail = previous;
        } else {
            next.previous = previous;
        }
        numberOfEntries--;
        return target.data;
    }

    @Override
    public void clear() {
        head = null;
        tail = null;
        numberOfEntries = 0;
    }

    @Override
    public boolean replace(int givenPosition, T newEntry) {
        Node<T> target = getNode(givenPosition);
        if (target == null) {
            return false;
        }
        target.data = newEntry;
        return true;
    }

    @Override
    public T getEntry(int givenPosition) {
        Node<T> target = getNode(givenPosition);
        return target == null ? null : target.data;
    }

    @Override
    public int indexOf(T anEntry) {
        // Search sequentially and use equals() to identify a matching entry.
        Node<T> current = head;
        int position = 1;
        while (current != null) {
            if (current.data.equals(anEntry)) {
                return position;
            }
            current = current.next;
            position++;
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
       // Linked nodes grow dynamically, so the list has no fixed capacity.
        return false;
    }

    @Override
    public Iterator<T> getIterator() {
        return new DoublyLinkedListIterator();
    }

    /**
     * Returns the node at a given 1-based position.
     * Shared by operations that need to locate a node before accessing it.
     *
     * @param position the position to locate
     * @return the node at that position, or null if the position is invalid
     */
    private Node<T> getNode(int position) {
        if (position < 1 || position > numberOfEntries) {
            return null;
        }
        Node<T> current = head;
        for (int i = 1; i < position; i++) {
            current = current.next;
        }
        return current;
    }

    /**
     * Node used to link entries in both directions.
     */
    private class Node<E> {
        private E data;
        private Node<E> previous;
        private Node<E> next;

        private Node(E data) {
            this.data = data;
        }
    }

    /**
     * Iterates through the list from head to tail.
     */
    private class DoublyLinkedListIterator implements Iterator<T> {
        private Node<T> currentNode;

        private DoublyLinkedListIterator() {
            currentNode = head;
        }

        @Override
        public boolean hasNext() {
            return currentNode != null;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            T data = currentNode.data;
            currentNode = currentNode.next;
            return data;
        }
    }
}

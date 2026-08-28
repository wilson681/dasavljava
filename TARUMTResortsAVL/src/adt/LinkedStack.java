package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Stack implementation using a singly linked structure.
 * Push and pop are performed at the top of the stack in O(1) time.
 *
 * @author Hoo Theng Qin
 * @param <T> type of entry stored in the stack
 */
public class LinkedStack<T> implements StackInterface<T> {

    private Node<T> topNode;      
    private int numberOfEntries;  

    public LinkedStack() {
        topNode = null;
        numberOfEntries = 0;
    }

    @Override
    public void push(T newEntry) {
        Node<T> newNode = new Node<>(newEntry);
        newNode.next = topNode;
        topNode = newNode;
        numberOfEntries++;
    }

    @Override
    public T pop() {
        if (isEmpty()) {
            return null;
        }
        T data = topNode.data;
        topNode = topNode.next;
        numberOfEntries--;
        return data;
    }

    @Override
    public T peek() {
        if (isEmpty()) {
            return null;
        }
        return topNode.data;
    }

    @Override
    public boolean isEmpty() {
        return topNode == null;
    }

    @Override
    public boolean isFull() {
        // Linked nodes can grow dynamically, so the stack has no fixed capacity.
        return false;
    }

    @Override
    public void clear() {
        topNode = null;
        numberOfEntries = 0;
    }

    @Override
    public int size() {
        return numberOfEntries;
    }

    @Override
    public Iterator<T> getIterator() {
        return new LinkedStackIterator();
    }

    /**
     * Node used to link entries in the stack.
     */
    private class Node<E> {
        private E data;
        private Node<E> next;

        private Node(E data) {
            this.data = data;
        }
    }

     /**
     * Iterates through the stack from top to bottom.
     */
    private class LinkedStackIterator implements Iterator<T> {
        private Node<T> currentNode;

        private LinkedStackIterator() {
            currentNode = topNode;
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

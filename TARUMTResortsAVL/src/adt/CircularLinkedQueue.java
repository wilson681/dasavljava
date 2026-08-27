package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

// CircularLinkedQueue.java - QueueInterface implementation, singly linked list joined
// head to tail into a loop
//
// @author jagathis
//
// used for module 1 (Walk-In Registrations, queues up walk-in/standard bookings)
// only keeps a backNode (tail), its next points to the front, enqueue/dequeue both O(1)
public class CircularLinkedQueue<T> implements QueueInterface<T> {

    private Node<T> backNode;     // tail node (its next IS the front), null when empty
    private int numberOfEntries;  // current entry count, remove() needs this else its hard to tell how many laps it made

    public CircularLinkedQueue() {
        backNode = null;
        numberOfEntries = 0;
    }

    @Override
    public void enqueue(T newEntry) {
        Node<T> newNode = new Node<>(newEntry);
        if (isEmpty()) {
            // only node so far, points to itself, thats the loop
            newNode.next = newNode;
        } else {
            // new node goes right after backNode, takes over pointing to the old front
            newNode.next = backNode.next;
            backNode.next = newNode;
        }
        // new node always becomes the new backNode (tail)
        backNode = newNode;
        numberOfEntries++;
    }

    @Override
    public T dequeue() {
        if (isEmpty()) {
            return null;
        }
        Node<T> frontNode = backNode.next;
        if (frontNode == backNode) {
            // only node left, queue goes empty after this
            backNode = null;
        } else {
            // backNode now points to the new front (whatever came after the old one)
            backNode.next = frontNode.next;
        }
        numberOfEntries--;
        return frontNode.data;
    }

    @Override
    public T getFront() {
        if (isEmpty()) {
            return null;
        }
        return backNode.next.data;
    }

    @Override
    public boolean remove(T anEntry) {
        if (isEmpty()) {
            return false;
        }
        // circular list has no natural "start", so numberOfEntries caps this at 1 full
        // lap - otherwise if anEntry really isnt in there this loops forever
        Node<T> previous = backNode;
        Node<T> current = backNode.next;
        for (int i = 0; i < numberOfEntries; i++) {
            if (current.data.equals(anEntry)) {
                if (current == backNode && current.next == current) {
                    // only node in the queue, empty after removing it
                    backNode = null;
                } else {
                    // link the nodes around current directly, skip it
                    previous.next = current.next;
                    if (current == backNode) {
                        // removed node was the tail, tail moves to whatever was before it
                        backNode = previous;
                    }
                }
                numberOfEntries--;
                return true;
            }
            previous = current;
            current = current.next;
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return backNode == null;
    }

    @Override
    public boolean isFull() {
        // linked list grows dynamically, never full
        return false;
    }

    @Override
    public void clear() {
        backNode = null;
        numberOfEntries = 0;
    }

    @Override
    public int size() {
        return numberOfEntries;
    }

    @Override
    public Iterator<T> getIterator() {
        // same trick as AVLTree's getInorderIterator(): dump into a plain array first,
        // then wrap it as an Iterator
        @SuppressWarnings("unchecked")
        T[] entries = (T[]) new Object[numberOfEntries];
        if (!isEmpty()) {
            Node<T> current = backNode.next;
            for (int i = 0; i < numberOfEntries; i++) {
                entries[i] = current.data;
                current = current.next;
            }
        }
        return new ArrayIterator(entries);
    }

    // node for the circular list, holds data + pointer to next
    private class Node<E> {
        private E data;
        private Node<E> next;

        private Node(E data) {
            this.data = data;
        }
    }

    // dumps the traversal result into a plain array, wraps it as an Iterator
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

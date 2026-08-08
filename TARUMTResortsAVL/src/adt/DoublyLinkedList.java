package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * DoublyLinkedList.java
 * ListInterface 的实现之一 —— 用双向链表当底层结构。
 *
 * @author 某某
 *
 * 说明:
 * - 给模块5(Loyalty and Rewards,Member.pointsLedger)用
 * - 每个 Node 存资料 + 前一个节点 + 后一个节点两个指针
 *
 * @param <T> 存放的元素类型
 */
public class DoublyLinkedList<T> implements ListInterface<T> {

    private Node<T> head;         // 第一个节点
    private Node<T> tail;         // 最后一个节点
    private int numberOfEntries;  // 目前有几个条目

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
        // 位置从1开始,合法范围是 1 ~ numberOfEntries+1(+1是允许插在最后面变成新的一个)
        if (newPosition < 1 || newPosition > numberOfEntries + 1) {
            return false;
        }
        if (newPosition == numberOfEntries + 1) {
            // 插在最后面,直接借用add(T)那套"接到tail后面"的逻辑,不用另外处理
            return add(newEntry);
        }
        Node<T> newNode = new Node<>(newEntry);
        Node<T> current = getNode(newPosition);   // 原本占着这个位置、要被newNode往后挤的节点
        Node<T> previous = current.previous;
        newNode.next = current;
        newNode.previous = previous;
        current.previous = newNode;
        if (previous == null) {
            // current原本是head(没有previous),newNode插到它前面就变成新的head
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
        if (previous == null) {
            // target原本是head,拿掉之后head换成它后面那个(可能是null,变成空list)
            head = next;
        } else {
            previous.next = next;
        }
        if (next == null) {
            // target原本是tail,拿掉之后tail换成它前面那个(可能是null,变成空list)
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
        // 线性扫描,靠anEntry自己的equals()判断是不是同一笔
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
        // 链表动态扩充,不会满
        return false;
    }

    @Override
    public Iterator<T> getIterator() {
        return new DoublyLinkedListIterator();
    }

    /**
     * 依position(从1开始)从head沿着next走到对应节点,position不合法回传null。
     * add/remove/replace/getEntry这几个要"先定位到节点"的操作都靠这个共用,避免重复写走访逻辑。
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
     * Node —— 双向链表的节点,存资料 + 前一个/后一个节点的引用
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
     * DoublyLinkedListIterator —— 从head开始沿着next走,走到底(null)就结束
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

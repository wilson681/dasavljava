package adt;

import java.util.Iterator;

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
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean add(T newEntry) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean add(int newPosition, T newEntry) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public T remove(int givenPosition) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean replace(int givenPosition, T newEntry) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public T getEntry(int givenPosition) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public int indexOf(T anEntry) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean contains(T anEntry) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public int getNumberOfEntries() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean isFull() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Iterator<T> getIterator() {
        throw new UnsupportedOperationException("TODO");
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
}

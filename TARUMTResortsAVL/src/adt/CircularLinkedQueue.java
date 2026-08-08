package adt;

import java.util.Iterator;

/**
 * CircularLinkedQueue.java
 * QueueInterface 的实现 —— 用首尾相连成环的单向链表当底层结构。
 *
 * @author 某某
 *
 * 说明:
 * - 给模块1(Walk-In Registrations,排队处理散客/临时订房)用
 * - 只记一个 backNode(队尾),它的 next 指向队头,enqueue/dequeue 都是 O(1)
 *
 * @param <T> 存放的元素类型
 */
public class CircularLinkedQueue<T> implements QueueInterface<T> {

    private Node<T> backNode;     // 队尾节点(它的 next 就是队头)
    private int numberOfEntries;  // 目前有几个条目

    public CircularLinkedQueue() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void enqueue(T newEntry) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public T dequeue() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public T getFront() {
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
    public void clear() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Iterator<T> getIterator() {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Node —— 环状链表的节点,存资料 + 指向下一个节点的引用
     */
    private class Node<E> {
        private E data;
        private Node<E> next;

        private Node(E data) {
            this.data = data;
        }
    }
}

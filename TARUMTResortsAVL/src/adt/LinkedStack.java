package adt;

import java.util.Iterator;

/**
 * LinkedStack.java
 * StackInterface 的实现 —— 用单向链表当底层结构。
 *
 * @author 某某
 *
 * 说明:
 * - 给模块3(Housekeeping,RoomHistory.statusStack)用
 * - push/pop 都只操作链表最前面那个节点(topNode),两者都是 O(1)
 *
 * @param <T> 存放的元素类型
 */
public class LinkedStack<T> implements StackInterface<T> {

    private Node<T> topNode;      // 栈顶节点
    private int numberOfEntries;  // 目前有几个条目

    public LinkedStack() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void push(T newEntry) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public T pop() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public T peek() {
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
     * Node —— 单向链表的节点,存资料 + 指向下一个节点的引用
     */
    private class Node<E> {
        private E data;
        private Node<E> next;

        private Node(E data) {
            this.data = data;
        }
    }
}

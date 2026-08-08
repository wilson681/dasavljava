package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

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
        // 链表动态扩充,不会满
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
     * Node —— 单向链表的节点,存资料 + 指向下一个节点的引用
     */
    private class Node<E> {
        private E data;
        private Node<E> next;

        private Node(E data) {
            this.data = data;
        }
    }

    /**
     * LinkedStackIterator —— 从topNode开始沿着next走,天生就是"栈顶到栈底"的顺序
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

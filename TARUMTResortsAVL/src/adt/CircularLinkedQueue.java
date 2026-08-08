package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

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

    private Node<T> backNode;     // 队尾节点(它的 next 就是队头),空队列时是 null
    private int numberOfEntries;  // 目前有几个条目,remove()要用到,不然不好判断走了几圈

    public CircularLinkedQueue() {
        backNode = null;
        numberOfEntries = 0;
    }

    @Override
    public void enqueue(T newEntry) {
        Node<T> newNode = new Node<>(newEntry);
        if (isEmpty()) {
            // 只有一个节点时,自己指向自己,形成一个环
            newNode.next = newNode;
        } else {
            // 新节点插在backNode后面,并接手指向原本的队头(backNode.next)
            newNode.next = backNode.next;
            backNode.next = newNode;
        }
        // 新节点永远变成新的backNode(队尾)
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
            // 只剩这一个节点,拿掉之后队伍变空
            backNode = null;
        } else {
            // backNode继续指向"新的队头"(原本队头的下一个)
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
        // 环状链表没有天然的"头"，只能靠numberOfEntries限制最多绕一圈,
        // 不然如果anEntry真的不存在,会无限绕圈绕不出来
        Node<T> previous = backNode;
        Node<T> current = backNode.next;
        for (int i = 0; i < numberOfEntries; i++) {
            if (current.data.equals(anEntry)) {
                if (current == backNode && current.next == current) {
                    // 队伍里只有这一个节点,拿掉之后队伍变空
                    backNode = null;
                } else {
                    // 让current前后的节点直接接起来,把current跳过去
                    previous.next = current.next;
                    if (current == backNode) {
                        // 被移除的刚好是队尾,队尾要换成它前面那个
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
        // 链表动态扩充,不会满
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
        // 做法跟AVLTree的getInorderIterator()一样:先填进一个普通阵列,再包成Iterator
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

    /**
     * ArrayIterator —— 把遍历结果先存进一个普通阵列,再包成 Iterator 吐出去
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

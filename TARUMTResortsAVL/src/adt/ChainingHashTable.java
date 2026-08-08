package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * ChainingHashTable.java
 * HashTableInterface 的实现 —— 用数组当桶(bucket),每个桶用链表处理碰撞(separate chaining)。
 *
 * @author 某某
 *
 * 说明:
 * - 给模块4(Front-Desk Service,用确认号瞬间查到完整客人资料)用
 * - 靠 T 自己的 hashCode() 换算成桶的 index,同一个桶碰撞的条目串成链表
 * - 不能用 java.util.HashMap/HashSet,hash 函数跟碰撞处理都要自己写
 * - 查找/删除时传进来的 T,不用是"树里真正那个物件",只要它的 hashCode()/equals()
 *   跟目标一致就能找到——比如 Guest 的 hashCode/equals 都只看 confirmationNumber,
 *   所以传一个"只填了confirmationNumber"的Guest当搜寻用的样板也能正确找到
 *
 * @param <T> 存放的元素类型,需要有意义的 hashCode() 和 equals() 实现
 */
public class ChainingHashTable<T> implements HashTableInterface<T> {

    private static final int DEFAULT_CAPACITY = 31;
    private Node<T>[] buckets;    // 桶数组,每个桶是一条链表的起点(没东西就是null)
    private int numberOfEntries;  // 目前有几个条目

    public ChainingHashTable() {
        buckets = createBucketArray(DEFAULT_CAPACITY);
        numberOfEntries = 0;
    }

    /**
     * 开一个指定大小的桶阵列。用了跟ArrayBasedList一样的技巧绕过Java的泛型阵列限制。
     */
    @SuppressWarnings("unchecked")
    private Node<T>[] createBucketArray(int capacity) {
        return new Node[capacity];
    }

    /**
     * 把T的hashCode()换算成落在哪一个桶。hashCode()可能是负数,
     * 用Math.abs()转成非负数,再对桶的数量取余数。
     */
    private int getBucketIndex(T entry) {
        return Math.abs(entry.hashCode()) % buckets.length;
    }

    @Override
    public boolean add(T newEntry) {
        int index = getBucketIndex(newEntry);
        // 新节点直接插在该桶链表的最前面,是O(1)的做法(不用找链表尾巴)
        Node<T> newNode = new Node<>(newEntry);
        newNode.next = buckets[index];
        buckets[index] = newNode;
        numberOfEntries++;
        return true;
    }

    @Override
    public T remove(T anEntry) {
        int index = getBucketIndex(anEntry);
        Node<T> current = buckets[index];
        Node<T> previous = null;
        while (current != null) {
            if (current.data.equals(anEntry)) {
                if (previous == null) {
                    // 要移除的刚好是这个桶的第一个节点
                    buckets[index] = current.next;
                } else {
                    previous.next = current.next;
                }
                numberOfEntries--;
                return current.data;
            }
            previous = current;
            current = current.next;
        }
        return null;
    }

    @Override
    public T getEntry(T anEntry) {
        int index = getBucketIndex(anEntry);
        Node<T> current = buckets[index];
        while (current != null) {
            if (current.data.equals(anEntry)) {
                return current.data;
            }
            current = current.next;
        }
        return null;
    }

    @Override
    public boolean contains(T anEntry) {
        return getEntry(anEntry) != null;
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
    public void clear() {
        buckets = createBucketArray(DEFAULT_CAPACITY);
        numberOfEntries = 0;
    }

    @Override
    public Iterator<T> getIterator() {
        // 依序走过每个桶、每条链表,填进一个普通阵列再包成Iterator——
        // 跟AVLTree/CircularLinkedQueue的做法是同一套,只是走的路径不同
        @SuppressWarnings("unchecked")
        T[] entries = (T[]) new Object[numberOfEntries];
        int position = 0;
        for (int i = 0; i < buckets.length; i++) {
            Node<T> current = buckets[i];
            while (current != null) {
                entries[position] = current.data;
                position++;
                current = current.next;
            }
        }
        return new ArrayIterator(entries);
    }

    /**
     * Node —— 桶内链表的节点,存资料 + 指向同一个桶下一个节点的引用
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

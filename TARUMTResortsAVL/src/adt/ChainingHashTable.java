package adt;

import java.util.Iterator;

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
 *
 * @param <T> 存放的元素类型,需要有意义的 hashCode() 和 equals() 实现
 */
public class ChainingHashTable<T> implements HashTableInterface<T> {

    private static final int DEFAULT_CAPACITY = 31;
    private Node<T>[] buckets;    // 桶数组,每个桶是一条链表的起点
    private int numberOfEntries;  // 目前有几个条目

    public ChainingHashTable() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean add(T newEntry) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public T remove(T anEntry) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public T getEntry(T anEntry) {
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
    public void clear() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Iterator<T> getIterator() {
        throw new UnsupportedOperationException("TODO");
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
}

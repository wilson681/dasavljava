package adt;

import java.util.Iterator;

/**
 * ArrayBasedList.java
 * ListInterface 的实现之一 —— 用普通 Java 数组当底层结构。
 *
 * @author 某某
 *
 * 说明:
 * - 不能用 java.util.ArrayList,数组满了要自己写扩容逻辑
 * - 给 Guest.bookedRooms、Room主清单 这类"不算任何模块正式ADT"的场景用
 *
 * @param <T> 存放的元素类型
 */
public class ArrayBasedList<T> implements ListInterface<T> {

    private static final int DEFAULT_CAPACITY = 25;
    private T[] entries;          // 底层数组
    private int numberOfEntries;  // 目前有几个条目

    public ArrayBasedList() {
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
}

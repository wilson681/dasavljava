package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * ArrayBasedList.java
 * ListInterface 的实现之一 —— 用普通 Java 数组当底层结构。
 *
 * @author 某某
 *
 * 说明:
 * - 不能用 java.util.ArrayList,数组满了要自己写扩容逻辑
 * - 给 Guest.bookedRooms、Room主清单 这类"不算任何模块正式ADT"的场景用
 * - 位置(position)从 1 开始算,不是从 0——这是照 ListInterface 规格给的语意
 *
 * @param <T> 存放的元素类型
 */
public class ArrayBasedList<T> implements ListInterface<T> {

    private static final int DEFAULT_CAPACITY = 25;
    private T[] entries;          // 底层数组
    private int numberOfEntries;  // 目前有几个条目

    public ArrayBasedList() {
        entries = createArray(DEFAULT_CAPACITY);
        numberOfEntries = 0;
    }

    /**
     * 开一个指定大小的阵列。Java不允许直接 new T[capacity](泛型阵列限制),
     * 先建Object[]再强制转型是标准workaround,不是Collections Framework的东西。
     */
    @SuppressWarnings("unchecked")
    private T[] createArray(int capacity) {
        return (T[]) new Object[capacity];
    }

    /**
     * 数组满了就换一个两倍大的新数组,把旧资料搬过去——这就是"自己写扩容逻辑"。
     */
    private void ensureCapacity() {
        if (numberOfEntries == entries.length) {
            T[] biggerArray = createArray(entries.length * 2);
            for (int i = 0; i < numberOfEntries; i++) {
                biggerArray[i] = entries[i];
            }
            entries = biggerArray;
        }
    }

    @Override
    public boolean add(T newEntry) {
        ensureCapacity();
        entries[numberOfEntries] = newEntry;
        numberOfEntries++;
        return true;
    }

    @Override
    public boolean add(int newPosition, T newEntry) {
        // 位置从1开始,合法范围是 1 ~ numberOfEntries+1(+1是允许插在最后面变成新的一个)
        if (newPosition < 1 || newPosition > numberOfEntries + 1) {
            return false;
        }
        ensureCapacity();
        // 从最后面开始,把newPosition(含)之后的条目都往后搬一格,腾出插入的空位
        // 一定要从后面往前搬,不然会把还没搬走的资料覆盖掉
        for (int i = numberOfEntries; i > newPosition - 1; i--) {
            entries[i] = entries[i - 1];
        }
        entries[newPosition - 1] = newEntry;
        numberOfEntries++;
        return true;
    }

    @Override
    public T remove(int givenPosition) {
        if (givenPosition < 1 || givenPosition > numberOfEntries) {
            return null;
        }
        T removed = entries[givenPosition - 1];
        // 把givenPosition之后的条目都往前搬一格,把移除后留下的空隙补起来
        for (int i = givenPosition - 1; i < numberOfEntries - 1; i++) {
            entries[i] = entries[i + 1];
        }
        entries[numberOfEntries - 1] = null;   // 最后一格现在是重复的,清掉避免留着旧引用
        numberOfEntries--;
        return removed;
    }

    @Override
    public void clear() {
        entries = createArray(DEFAULT_CAPACITY);
        numberOfEntries = 0;
    }

    @Override
    public boolean replace(int givenPosition, T newEntry) {
        if (givenPosition < 1 || givenPosition > numberOfEntries) {
            return false;
        }
        entries[givenPosition - 1] = newEntry;
        return true;
    }

    @Override
    public T getEntry(int givenPosition) {
        if (givenPosition < 1 || givenPosition > numberOfEntries) {
            return null;
        }
        return entries[givenPosition - 1];
    }

    @Override
    public int indexOf(T anEntry) {
        // 线性扫描,靠anEntry自己的equals()判断是不是同一笔
        for (int i = 0; i < numberOfEntries; i++) {
            if (entries[i].equals(anEntry)) {
                return i + 1;   // 转成"从1开始"的位置
            }
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
        // 会自动扩容(ensureCapacity),永远不会真的卡在"满了加不进去"的状态
        return false;
    }

    @Override
    public Iterator<T> getIterator() {
        return new ArrayListIterator();
    }

    /**
     * ArrayListIterator —— 直接照着底层数组的顺序走,不用像Tree/Queue那样
     * 另外先复制一份,因为数组本身的顺序就是List该有的顺序
     */
    private class ArrayListIterator implements Iterator<T> {
        private int currentIndex;

        private ArrayListIterator() {
            currentIndex = 0;
        }

        @Override
        public boolean hasNext() {
            return currentIndex < numberOfEntries;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return entries[currentIndex++];
        }
    }
}

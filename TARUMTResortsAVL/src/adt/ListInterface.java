package adt;

import java.util.Iterator;

/**
 * ListInterface.java
 * ADT List — A list is a linear collection of entries where each entry can be
 * accessed by its position. Entries are not required to be unique or sorted.
 *
 * @author 某某
 *
 * 来源: [若这份规格是参考课程 sample code / 教材而来,请在此注明出处;
 *        若是自己独立设计的,可以删除这行]
 *
 * @param <T> 存放的元素类型
 */
public interface ListInterface<T> {

    /**
     * 把一个新条目加到这个 list 的最后面。
     * @param newEntry 要加入的条目
     * @return 加入成功返回 true
     */
    public boolean add(T newEntry);

    /**
     * 把一个新条目插入到指定位置(原本在该位置及之后的条目往后移一位)。
     * @param newPosition 要插入的位置(第一个位置是 1)
     * @param newEntry 要加入的条目
     * @return 加入成功返回 true,位置不合法则返回 false
     */
    public boolean add(int newPosition, T newEntry);

    /**
     * 移除指定位置的条目,该位置之后的条目往前移一位。
     * @param givenPosition 要移除的位置
     * @return 被移除的条目,若位置不合法则返回 null
     */
    public T remove(int givenPosition);

    /**
     * 清空这个 list,移除所有条目。
     */
    public void clear();

    /**
     * 把指定位置的条目换成新的条目。
     * @param givenPosition 要替换的位置
     * @param newEntry 新的条目
     * @return 替换成功返回 true
     */
    public boolean replace(int givenPosition, T newEntry);

    /**
     * 取得指定位置的条目,list 本身不变。
     * @param givenPosition 要取得的位置
     * @return 该位置的条目,若位置不合法则返回 null
     */
    public T getEntry(int givenPosition);

    /**
     * 找出某个条目在 list 里第一次出现的位置。
     * @param anEntry 要寻找的条目
     * @return 该条目的位置,找不到则返回 -1
     */
    public int indexOf(T anEntry);

    /**
     * 检查某个条目是否存在于这个 list 里。
     * @param anEntry 要检查的条目
     * @return 存在则返回 true
     */
    public boolean contains(T anEntry);

    /**
     * 取得这个 list 目前的条目总数。
     * @return 条目总数
     */
    public int getNumberOfEntries();

    /**
     * 检查这个 list 是否为空。
     * @return 是空的则返回 true
     */
    public boolean isEmpty();

    /**
     * 检查这个 list 是否已满(固定容量的实现才有意义,动态扩充的实现永远是 false)。
     * @return 已满则返回 true
     */
    public boolean isFull();

    /**
     * 取得一个能从头到尾遍历这个 list 所有条目的 iterator。
     * @return 这个 list 的 iterator
     */
    public Iterator<T> getIterator();
}

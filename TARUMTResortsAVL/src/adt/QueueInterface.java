package adt;

import java.util.Iterator;

/**
 * QueueInterface.java
 * ADT Queue — A queue is a linear collection where entries are added at the
 * back and removed from the front, following FIFO(First-In-First-Out)。
 *
 * @author 某某
 *
 * 来源: [若这份规格是参考课程 sample code / 教材而来,请在此注明出处;
 *        若是自己独立设计的,可以删除这行]
 *
 * @param <T> 存放的元素类型
 */
public interface QueueInterface<T> {

    /**
     * 把一个新条目加到队伍的最后面。
     * @param newEntry 要加入的条目
     */
    public void enqueue(T newEntry);

    /**
     * 移除并回传队伍最前面的条目。
     * @return 原本排在最前面的条目,若队伍是空的则返回 null
     */
    public T dequeue();

    /**
     * 只看队伍最前面的条目,不移除它。
     * @return 排在最前面的条目,若队伍是空的则返回 null
     */
    public T getFront();

    /**
     * 检查这个队伍是否为空。
     * @return 是空的则返回 true
     */
    public boolean isEmpty();

    /**
     * 检查这个队伍是否已满(固定容量的实现才有意义,动态扩充的实现永远是 false)。
     * @return 已满则返回 true
     */
    public boolean isFull();

    /**
     * 清空这个队伍,移除所有条目。
     */
    public void clear();

    /**
     * 取得这个队伍目前的条目总数。
     * @return 条目总数
     */
    public int size();

    /**
     * 取得一个从队头到队尾遍历所有条目的 iterator。
     * @return 这个队伍的 iterator
     */
    public Iterator<T> getIterator();
}

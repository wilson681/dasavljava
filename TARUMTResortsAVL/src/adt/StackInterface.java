package adt;

import java.util.Iterator;

/**
 * StackInterface.java
 * ADT Stack — A stack is a linear collection where entries are added and
 * removed only from one end(the "top"), following LIFO(Last-In-First-Out)。
 *
 * @author 某某
 *
 * 来源: [若这份规格是参考课程 sample code / 教材而来,请在此注明出处;
 *        若是自己独立设计的,可以删除这行]
 *
 * @param <T> 存放的元素类型
 */
public interface StackInterface<T> {

    /**
     * 把一个新条目加到栈顶。
     * @param newEntry 要加入的条目
     */
    public void push(T newEntry);

    /**
     * 移除并回传栈顶的条目。
     * @return 原本栈顶的条目,若栈是空的则返回 null
     */
    public T pop();

    /**
     * 只看栈顶的条目,不移除它。
     * @return 栈顶的条目,若栈是空的则返回 null
     */
    public T peek();

    /**
     * 检查这个栈是否为空。
     * @return 是空的则返回 true
     */
    public boolean isEmpty();

    /**
     * 检查这个栈是否已满(固定容量的实现才有意义,动态扩充的实现永远是 false)。
     * @return 已满则返回 true
     */
    public boolean isFull();

    /**
     * 清空这个栈,移除所有条目。
     */
    public void clear();

    /**
     * 取得这个栈目前的条目总数。
     * @return 条目总数
     */
    public int size();

    /**
     * 取得一个从栈顶到栈底遍历所有条目的 iterator。
     * @return 这个栈的 iterator
     */
    public Iterator<T> getIterator();
}

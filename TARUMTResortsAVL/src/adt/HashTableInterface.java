package adt;

import java.util.Iterator;

/**
 * HashTableInterface.java
 * ADT Hash Table — 靠对条目算 hash 值来决定存放位置的非线性结构,
 * 用来支持接近 O(1) 的新增/查找/移除。
 *
 * @author 某某
 *
 * 来源: [若这份规格是参考课程 sample code / 教材而来,请在此注明出处;
 *        若是自己独立设计的,可以删除这行]
 *
 * 说明:查找/移除用的参数是 T(条目本身,靠它的 hashCode()/equals() 去比对),
 * 不是像 getEntry(String confirmationNumber) 这种写死业务专属参数的写法,
 * 这样规格才能保持通用、不跟"客人""确认号"这类业务概念绑死。
 *
 * @param <T> 存放的元素类型,需要有意义的 hashCode() 和 equals() 实现
 */
public interface HashTableInterface<T> {

    /**
     * 把一个新条目加进这个 hash table。
     * @param newEntry 要加入的条目
     * @return 加入成功返回 true
     */
    public boolean add(T newEntry);

    /**
     * 移除跟给定条目相等(hashCode/equals判断)的条目。
     * @param anEntry 要移除的条目(用来比对)
     * @return 被移除的条目,找不到则返回 null
     */
    public T remove(T anEntry);

    /**
     * 找出跟给定条目相等的那一条完整资料。
     * @param anEntry 要寻找的条目(用来比对)
     * @return 找到的条目,找不到则返回 null
     */
    public T getEntry(T anEntry);

    /**
     * 检查这个 hash table 里是否存在跟给定条目相等的资料。
     * @param anEntry 要检查的条目
     * @return 存在则返回 true
     */
    public boolean contains(T anEntry);

    /**
     * 取得这个 hash table 目前的条目总数。
     * @return 条目总数
     */
    public int getNumberOfEntries();

    /**
     * 检查这个 hash table 是否为空。
     * @return 是空的则返回 true
     */
    public boolean isEmpty();

    /**
     * 清空这个 hash table,移除所有条目。
     */
    public void clear();

    /**
     * 取得一个能遍历这个 hash table 所有条目的 iterator(顺序不保证)。
     * @return 这个 hash table 的 iterator
     */
    public Iterator<T> getIterator();
}

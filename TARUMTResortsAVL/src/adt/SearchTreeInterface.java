package adt;

import java.util.Iterator;

/**
 * SearchTreeInterface.java
 * ADT Search Tree — 一个 search tree 是靠比较大小(Comparable)来组织资料的
 * 非线性结构,插入的条目会自动依照顺序排好,支持依大小快速查找/新增/移除。
 *
 * @author 某某
 *
 * 来源: [若这份规格是参考课程 sample code / 教材而来,请在此注明出处;
 *        若是自己独立设计的,可以删除这行]
 *
 * 命名说明:这里刻意不叫 AVLTreeInterface / BinaryTreeInterface,
 * 是因为 interface 的规格要保持抽象、不暴露实现方式(AVL 是否要自平衡、
 * 用二叉树还是其他方式实现,都是"实现类"该决定的事,不该出现在规格名字里)。
 *
 * @param <T> 存放的元素类型,必须能互相比较大小
 */
public interface SearchTreeInterface<T extends Comparable<? super T>> {

    /**
     * 取得树根的资料,树本身不变。
     * @return 树根的资料,若树是空的则返回 null
     */
    public T getRootData();

    /**
     * 取得这棵树的高度。
     * @return 树的高度,空树高度为 0
     */
    public int getHeight();

    /**
     * 取得这棵树目前的节点总数。
     * @return 节点总数
     */
    public int getNumberOfNodes();

    /**
     * 检查这棵树是否为空。
     * @return 是空的则返回 true
     */
    public boolean isEmpty();

    /**
     * 清空这棵树,移除所有节点。
     */
    public void clear();

    /**
     * 把一个新条目加进这棵树,树会自动依照顺序把它放到合适的位置。
     * @param newEntry 要加入的条目
     * @return 加入成功返回 true
     */
    public boolean add(T newEntry);

    /**
     * 移除跟给定条目相等的那个节点。
     * @param anEntry 要移除的条目(用来比对大小/相等)
     * @return 被移除的条目,找不到则返回 null
     */
    public T remove(T anEntry);

    /**
     * 找出跟给定条目相等的节点资料,树本身不变。
     * @param anEntry 要寻找的条目(用来比对大小/相等)
     * @return 找到的条目,找不到则返回 null
     */
    public T getEntry(T anEntry);

    /**
     * 检查树里是否存在跟给定条目相等的节点。
     * @param anEntry 要检查的条目
     * @return 存在则返回 true
     */
    public boolean contains(T anEntry);

    /**
     * 取得一个按照中序(inorder,由小到大)遍历这棵树的 iterator。
     * @return 中序 iterator
     */
    public Iterator<T> getInorderIterator();

    /**
     * 取得一个按照前序(preorder)遍历这棵树的 iterator。
     * @return 前序 iterator
     */
    public Iterator<T> getPreorderIterator();

    /**
     * 取得一个按照后序(postorder)遍历这棵树的 iterator。
     * @return 后序 iterator
     */
    public Iterator<T> getPostorderIterator();
}

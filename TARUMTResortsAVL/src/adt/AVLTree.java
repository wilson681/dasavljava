package adt;

import java.util.Iterator;

/**
 * AVLTree.java
 * SearchTreeInterface 的实现 —— 自平衡二叉搜索树(Team ADT)。
 *
 * @author 某某
 *
 * 说明:
 * - 给模块2(VIP & Loyalty Tier Priority Room Allocation)用,是整组唯一交上去评分的 Team ADT
 * - 插入/删除后要检查每个节点的平衡因子(左右子树高度差),失衡时做左旋/右旋/双旋调整
 * - 排序依据是 T 的 compareTo(),实际排序键(等级排名 + 到达顺序)由 Control 层组装成可比较的物件传进来
 *
 * @param <T> 存放的元素类型,必须能互相比较大小
 */
public class AVLTree<T extends Comparable<? super T>> implements SearchTreeInterface<T> {

    private Node<T> root;   // 树根节点

    public AVLTree() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public T getRootData() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public int getHeight() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public int getNumberOfNodes() {
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
    public Iterator<T> getInorderIterator() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Iterator<T> getPreorderIterator() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Iterator<T> getPostorderIterator() {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Node —— AVL树的节点,存资料 + 左右子节点 + 这个节点自己的高度(方便算平衡因子)
     */
    private class Node<E> {
        private E data;
        private Node<E> left;
        private Node<E> right;
        private int height;

        private Node(E data) {
            this.data = data;
            this.height = 1;
        }
    }
}

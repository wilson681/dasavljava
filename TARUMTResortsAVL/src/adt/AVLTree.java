package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * AVLTree.java
 * SearchTreeInterface 的实现 —— 自平衡二叉搜索树(Team ADT)。
 *
 * @author 某某
 *
 * 说明:
 * - 给模块2(VIP & Loyalty Tier Priority Room Allocation)用,是整组唯一交上去评分的 Team ADT
 * - 插入/删除后会检查每个节点的平衡因子(左右子树高度差),失衡时做左旋/右旋/双旋调整
 * - 排序依据是 T 的 compareTo(),实际排序键(等级排名 + 到达顺序)由 T 自己(比如 Booking)决定
 * - getInorderIterator() 是这个实现被使用的关键:呼叫方只要让"优先级越高,compareTo值越小",
 *   in-order遍历第一个吐出来的就是优先级最高的那个,不需要额外的 getMin() 之类的方法
 *
 * @param <T> 存放的元素类型,必须能互相比较大小
 */
public class AVLTree<T extends Comparable<? super T>> implements SearchTreeInterface<T> {

    private Node<T> root;   // 树根节点,树是空的时候是 null

    public AVLTree() {
        root = null;
    }

    @Override
    public T getRootData() {
        // 树是空的就没有root,回传null,不然会NullPointerException
        return (root == null) ? null : root.data;
    }

    @Override
    public int getHeight() {
        return heightOf(root);
    }

    @Override
    public int getNumberOfNodes() {
        // 现在的做法是每次呼叫都重新递归数一遍,不是维护一个计数器字段
        // 好处是不用担心add/remove时忘记更新计数器,坏处是O(n),但对这个规模的项目够用
        return countNodes(root);
    }

    @Override
    public boolean isEmpty() {
        return root == null;
    }

    @Override
    public void clear() {
        // 直接把root断开,底下整棵树没人再引用得到,会被GC回收掉
        root = null;
    }

    @Override
    public boolean add(T newEntry) {
        // 从root开始,递归找到该插入的位置;addToSubtree每一层都会重新指派,
        // 是因为旋转可能会换掉某一层的root,一定要把回传值接回去,不然树会断掉
        root = addToSubtree(root, newEntry);
        return true;
    }

    @Override
    public T remove(T anEntry) {
        // 要先确认真的找得到,才能安心回传"被移除的那个值"给呼叫方
        // (如果没这一步,树里根本没有这笔资料,却还是会跑一遍removeFromSubtree白工)
        T found = getEntry(anEntry);
        if (found == null) {
            return null;
        }
        root = removeFromSubtree(root, anEntry);
        return found;
    }

    @Override
    public T getEntry(T anEntry) {
        // 标准BST查找:比较小就往左走,比较大就往右走,直到找到或走到null
        Node<T> current = root;
        while (current != null) {
            int comparison = anEntry.compareTo(current.data);
            if (comparison == 0) {
                return current.data;
            } else if (comparison < 0) {
                current = current.left;
            } else {
                current = current.right;
            }
        }
        return null;
    }

    @Override
    public boolean contains(T anEntry) {
        return getEntry(anEntry) != null;
    }

    @Override
    public Iterator<T> getInorderIterator() {
        // 做法:先递归数出总共有几个节点,开一个刚好大小的阵列,
        // 再递归一次把资料依照"左->自己->右"的顺序填进阵列,最后包成Iterator吐出去
        // (这样比写一个真正"边走边吐"的懒惰式Iterator简单很多,对这个项目规模足够)
        T[] entries = createArray(countNodes(root));
        fillInorder(root, entries, new int[]{0});
        return new ArrayIterator(entries);
    }

    @Override
    public Iterator<T> getPreorderIterator() {
        T[] entries = createArray(countNodes(root));
        fillPreorder(root, entries, new int[]{0});
        return new ArrayIterator(entries);
    }

    @Override
    public Iterator<T> getPostorderIterator() {
        T[] entries = createArray(countNodes(root));
        fillPostorder(root, entries, new int[]{0});
        return new ArrayIterator(entries);
    }

    // ========== 内部递归/辅助方法 ==========

    /**
     * 递归把newEntry插进以node为根的子树,插完自动检查平衡、需要就旋转,
     * 回传"这个位置现在该放哪个节点当根"(可能因为旋转而换人)。
     */
    private Node<T> addToSubtree(Node<T> node, T newEntry) {
        if (node == null) {
            // 走到底了,这里就是新资料该待的位置,建一个新节点
            return new Node<>(newEntry);
        }
        if (newEntry.compareTo(node.data) < 0) {
            node.left = addToSubtree(node.left, newEntry);
        } else {
            // compareTo == 0 理论上不该发生(每笔Booking的arrivalSequence都不同),
            // 这里保守处理成跟"大于"一样,往右塞,不会导致程序坏掉
            node.right = addToSubtree(node.right, newEntry);
        }
        // 插入完,从这一层开始,一路往上每一层都要重新检查平衡、更新高度
        return rebalance(node);
    }

    /**
     * 递归从以node为根的子树移除anEntry,回传"这个位置现在该放哪个节点当根"。
     * 标准BST删除的三种情况:没有小孩、只有一个小孩、有两个小孩。
     */
    private Node<T> removeFromSubtree(Node<T> node, T anEntry) {
        if (node == null) {
            // 理论上remove()已经先用getEntry()确认存在才会走到这里,
            // 但递归过程中还是有可能子树真的没有,保守回传null
            return null;
        }
        int comparison = anEntry.compareTo(node.data);
        if (comparison < 0) {
            node.left = removeFromSubtree(node.left, anEntry);
        } else if (comparison > 0) {
            node.right = removeFromSubtree(node.right, anEntry);
        } else {
            // 找到要删的节点了
            if (node.left == null) {
                // 没有左子树(可能也没有右子树),直接用右子树顶替这个位置
                return node.right;
            }
            if (node.right == null) {
                // 只有左子树,用左子树顶替
                return node.left;
            }
            // 左右子树都有:找右子树里最小的那个节点(比node.data大的里面最小的),
            // 用它的资料覆盖掉node,再把右子树里那个"重复"的successor节点删掉
            Node<T> successor = findMin(node.right);
            node.data = successor.data;
            node.right = removeFromSubtree(node.right, successor.data);
        }
        // 删除完,从这一层开始,一路往上每一层都要重新检查平衡、更新高度
        return rebalance(node);
    }

    /**
     * 从node开始,一路往左走到底,找出这个子树里最小的节点(标准BST操作)。
     */
    private Node<T> findMin(Node<T> node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    /**
     * AVL的核心:检查这个节点的平衡因子(左右子树高度差),失衡就用旋转修正,
     * 每次add/remove的递归回溯路径上,每一层都要经过这个方法。
     *
     * 平衡因子 = 左子树高度 - 右子树高度
     *   > 1  代表左边太重(LL或LR型失衡)
     *   < -1 代表右边太重(RR或RL型失衡)
     */
    private Node<T> rebalance(Node<T> node) {
        updateHeight(node);
        int balance = balanceFactor(node);

        if (balance > 1) {
            // 左边太重。如果是LR型(左子树的右边比较重),要先对左子树做一次左旋,
            // 把它转成LL型,才能再对自己做右旋修正
            if (balanceFactor(node.left) < 0) {
                node.left = rotateLeft(node.left);
            }
            return rotateRight(node);
        }
        if (balance < -1) {
            // 右边太重。如果是RL型(右子树的左边比较重),要先对右子树做一次右旋,
            // 把它转成RR型,才能再对自己做左旋修正
            if (balanceFactor(node.right) > 0) {
                node.right = rotateRight(node.right);
            }
            return rotateLeft(node);
        }
        // 没有失衡,原本的node继续当这个位置的根
        return node;
    }

    /**
     * 右旋:node的左小孩(newRoot)升上来当这个位置的新根,node自己降下去当newRoot的右小孩,
     * newRoot原本的右子树(可能比node.data大、比newRoot.data大)改接到node的左边。
     * 用在"左边太重"的情况。
     */
    private Node<T> rotateRight(Node<T> node) {
        Node<T> newRoot = node.left;
        node.left = newRoot.right;
        newRoot.right = node;
        // 顺序很重要:node现在变成newRoot的小孩了,要先算node的新高度,
        // 再算newRoot的高度(newRoot的高度会依赖node这个小孩算出来的高度)
        updateHeight(node);
        updateHeight(newRoot);
        return newRoot;
    }

    /**
     * 左旋:node的右小孩(newRoot)升上来当这个位置的新根,node自己降下去当newRoot的左小孩,
     * newRoot原本的左子树改接到node的右边。用在"右边太重"的情况,逻辑跟rotateRight是镜像对称。
     */
    private Node<T> rotateLeft(Node<T> node) {
        Node<T> newRoot = node.right;
        node.right = newRoot.left;
        newRoot.left = node;
        updateHeight(node);
        updateHeight(newRoot);
        return newRoot;
    }

    /**
     * 安全地取得一个节点的高度,node是null(代表空子树)就当高度是0。
     */
    private int heightOf(Node<T> node) {
        return (node == null) ? 0 : node.height;
    }

    /**
     * 重新计算并存回这个节点自己的高度:比左右子树高度较大的那个多1层。
     */
    private void updateHeight(Node<T> node) {
        node.height = 1 + Math.max(heightOf(node.left), heightOf(node.right));
    }

    /**
     * 平衡因子 = 左子树高度 - 右子树高度,正常范围应该在 -1 ~ 1 之间。
     */
    private int balanceFactor(Node<T> node) {
        return heightOf(node.left) - heightOf(node.right);
    }

    /**
     * 递归数出以node为根的子树,总共有几个节点。
     */
    private int countNodes(Node<T> node) {
        if (node == null) {
            return 0;
        }
        return 1 + countNodes(node.left) + countNodes(node.right);
    }

    /**
     * 开一个大小为size的阵列,拿来暂存遍历结果给Iterator用。
     * 因为Java不允许直接 new T[size](泛型阵列限制),要先建Comparable[]再强制转型,
     * 这是标准的workaround写法,不是Collections Framework的东西,不违反规则。
     */
    @SuppressWarnings("unchecked")
    private T[] createArray(int size) {
        return (T[]) new Comparable[size];
    }

    /**
     * 中序遍历(左->自己->右),把结果依序填进array。
     * position是一个只有一格的阵列,当成"目前该填第几格"的可变计数器用
     * (Java的方法参数不能直接当输出参数用,借一个长度1的阵列绕过这个限制)。
     */
    private void fillInorder(Node<T> node, T[] array, int[] position) {
        if (node == null) {
            return;
        }
        fillInorder(node.left, array, position);
        array[position[0]] = node.data;
        position[0]++;
        fillInorder(node.right, array, position);
    }

    /**
     * 前序遍历(自己->左->右)。
     */
    private void fillPreorder(Node<T> node, T[] array, int[] position) {
        if (node == null) {
            return;
        }
        array[position[0]] = node.data;
        position[0]++;
        fillPreorder(node.left, array, position);
        fillPreorder(node.right, array, position);
    }

    /**
     * 后序遍历(左->右->自己)。
     */
    private void fillPostorder(Node<T> node, T[] array, int[] position) {
        if (node == null) {
            return;
        }
        fillPostorder(node.left, array, position);
        fillPostorder(node.right, array, position);
        array[position[0]] = node.data;
        position[0]++;
    }

    /**
     * Node —— AVL树的节点,存资料 + 左右子节点 + 这个节点自己的高度(方便算平衡因子)
     */
    private class Node<E> {
        private E data;
        private Node<E> left;
        private Node<E> right;
        private int height;   // 只记自己的高度,不是整棵树的,方便O(1)取得,不用每次重新爬一遍

        private Node(E data) {
            this.data = data;
            this.height = 1;   // 新节点自己就是一层,没有小孩时高度算1
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

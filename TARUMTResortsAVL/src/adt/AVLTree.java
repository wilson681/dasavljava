package adt;

/**
 * 使用 AVL self-balancing 方式实现的 key-value 搜索树。
 *
 * <p>高度定义与课程 Chapter 9 一致：空子树高度为 0，叶节点高度为 1。
 * balance factor = left height - right height。任何节点的绝对 balance factor
 * 大于 1 时，都会经过 LL、RR、LR 或 RL rotation 恢复平衡。</p>
 *
 * <p>重要：本类只处理通用数据结构逻辑。菜单、Scanner、酒店业务规则和报告
 * 都不应写进这里。</p>
 *
 * <p>为什么这样设计（方便以后 modify / 配合其他 ADT）：</p>
 * <ul>
 *   <li>业务层永远声明 {@link SearchTreeInterface} 类型，不写死 AVLTree；
 *       以后要换实现（甚至换成别的树）只改 new 的那一行。</li>
 *   <li>K、V 是 generic：同一份实现可以同时服务
 *       confirmationNo→Booking、roomNo→Room、composite key→Booking 等任何 module。</li>
 *   <li>Node、rotation、rebalance 全部 private：内部怎么改都不会影响调用方。</li>
 *   <li>所有平衡修复集中在 {@code rebalance()}：以后加 add-on operation
 *       （例如 removeLargest()、rangeSearch()）时，新的递归 helper 直接
 *       {@code return rebalance(node)} 就自动保持 AVL 性质。</li>
 *   <li>将来加辅助 ADT（Queue/Stack/List）时照同一模式放进 adt/ package：
 *       一个 interface + 一个 implementation，互相不依赖，也不依赖本类。</li>
 * </ul>
 *
 * @param <K> 可比较且唯一的 key 类型
 * @param <V> 对应的资料类型
 * @author TODO：提交前替换成实际负责组员姓名
 */
public class AVLTree<K extends Comparable<K>, V>
        implements SearchTreeInterface<K, V> {

    // 整棵树只需要记住 root：所有操作都从 root 往下走。
    // private 是刻意的——外界拿到 Node 就能乱改 left/right/height，invariant 会毁掉。
    private Node<K, V> root;

    // 用一个 int 记录 entry 数量，size() 就是 O(1)；
    // 不然每次都要遍历整棵树数节点，变成 O(n)。
    private int size;

    /**
     * 建立一棵空 AVL Tree。
     */
    public AVLTree() {
        // 空树 = 没有 root。所有方法都把 root == null 当「空」处理。
        root = null;
        // 没有任何 entry。
        size = 0;
    }

    @Override
    public boolean insert(K key, V value) {
        // 先挡 null：宁可在门口抛 IllegalArgumentException，
        // 也不要让 null 进树之后在 compareTo() 时才炸，那时很难查是谁传进来的。
        requireKey(key);
        requireValue(value);

        // 递归 helper 的返回值被用来接「新的 subtree root」了，
        // 没办法同时返回「有没有真的插入」，所以用一个小 flag 对象带进去。
        // 为什么不先 contains() 再 insert？那要走两次 O(log n) 搜索，浪费一半时间。
        MutationFlag inserted = new MutationFlag();

        // 关键写法：root 必须接住返回值！
        // rotation 可能把整棵树的 root 换掉，不接住的话树就断了。
        root = insert(root, key, value, inserted);

        // 只有 flag 说「真的新建了 Node」才 size++；
        // duplicate key 被拒绝时什么都没变，size 不能动。
        if (inserted.changed) {
            size++;
        }

        // 把成败告诉 Control：true = 插入成功，false = key 已存在被拒绝。
        return inserted.changed;
    }

    /**
     * 递归走 BST 插入路径；回程时每一层都更新高度并检查是否需要旋转。
     */
    private Node<K, V> insert(Node<K, V> node, K key, V value,
            MutationFlag inserted) {
        // Base case：走到空位，代表 key 不存在，这里就是新 Node 的家。
        if (node == null) {
            // 标记「真的插入了」，public 方法靠它决定 size++。
            inserted.changed = true;
            // 新 Node 是 leaf，constructor 里 height 自动 = 1。
            return new Node<K, V>(key, value);
        }

        // 用 compareTo() 决定往哪边走：这就是 BST 的核心规则
        // （小的在左、大的在右），也是为什么 K 必须 implements Comparable。
        int comparison = key.compareTo(node.key);
        if (comparison < 0) {
            // key 比当前小 -> 去左子树继续找位置。
            // node.left 必须接住返回值，因为下层可能旋转换了 subtree root。
            node.left = insert(node.left, key, value, inserted);
        } else if (comparison > 0) {
            // key 比当前大 -> 去右子树。同样要接住返回值。
            node.right = insert(node.right, key, value, inserted);
        } else {
            // compareTo() == 0 -> 同一个 key。
            // 重复 key 明确拒绝（flag 保持 false，旧 value 不动）；
            // priority 相同的场景应使用复合 key 加 sequence 作 tiebreaker，
            // 而不是允许 duplicate——rotation 后 duplicate 的位置会变得不可预测。
            return node;
        }

        // 回程（递归返回的路上）每一层都做平衡检查：
        // 这就是 AVL 和普通 BST 唯一的差别。插入最多触发一次旋转，
        // 但每层都要更新 height，所以统一交给 rebalance()。
        return rebalance(node);
    }

    @Override
    public boolean replace(K key, V value) {
        // 与 insert 相同的 null 门禁，保证整棵树永远没有 null。
        requireKey(key);
        requireValue(value);

        // 直接复用查找逻辑找到目标 Node，不重写一遍搜索。
        Node<K, V> node = findNode(key);
        if (node == null) {
            // key 不存在就返回 false，让 Control 决定要不要改用 insert。
            // 为什么不自动帮忙 insert？因为「更新」和「新增」是不同业务动作，
            // ADT 不该替业务做决定。
            return false;
        }

        // 只换 value 不换 key：key 没变 -> 排序位置没变 -> 不需要任何 rotation，
        // 这就是 replace() 独立存在的原因（比 delete+insert 便宜得多）。
        node.value = value;
        return true;
    }

    @Override
    public V search(K key) {
        // null key 直接拒绝，那 search 找不到时返回 null 就没有歧义
        // （null 一定代表「不存在」，不可能是「存了个 null」）。
        requireKey(key);
        // 复用同一个查找 helper。
        Node<K, V> node = findNode(key);
        // 找到给 value，找不到给 null——Control 用 null 判断「查无此人」。
        return node == null ? null : node.value;
    }

    @Override
    public boolean contains(K key) {
        requireKey(key);
        // contains 就是「找得到 Node 吗」，逻辑与 search 完全一致，
        // 所以共用 findNode()，避免两份搜索代码以后改一处漏一处。
        return findNode(key) != null;
    }

    /**
     * 使用 iterative search，避免为单纯查询建立额外递归调用。
     */
    private Node<K, V> findNode(K key) {
        // 从 root 开始一路往下走。查询不改树，所以不需要递归
        // （递归的意义是回程时修树；查询没有回程工作，用 while 更省 stack）。
        Node<K, V> current = root;

        // current == null 代表走到底都没找到。
        while (current != null) {
            // 每一步比一次，决定停 / 往左 / 往右。
            int comparison = key.compareTo(current.key);
            if (comparison == 0) {
                // 找到了。
                return current;
            }
            // 比当前小去左边，比当前大去右边——每走一步就淘汰半棵树，
            // 这就是 O(log n) 的来源（树是平衡的，AVL 保证这点）。
            current = comparison < 0 ? current.left : current.right;
        }

        // 走到空位还没找到 -> key 不存在。
        return null;
    }

    @Override
    public boolean delete(K key) {
        requireKey(key);

        // 与 insert 同一套 flag 手法：递归返回值要接 subtree root，
        // 「有没有真的删到」用 flag 带出来。
        MutationFlag deleted = new MutationFlag();

        // 同样必须接住返回值：删除也可能旋转换 root。
        root = delete(root, key, deleted);

        // 只有真的删掉了才 size--；删不存在的 key 时 size 不动。
        if (deleted.changed) {
            size--;
        }
        return deleted.changed;
    }

    /**
     * 递归删除并在回程时 rebalance，所以根节点和所有祖先都能恢复 AVL 平衡。
     */
    private Node<K, V> delete(Node<K, V> node, K key,
            MutationFlag deleted) {
        // Base case：走到空位 = key 不存在，什么都不用做
        // （flag 保持 false，public 方法就会返回 false）。
        if (node == null) {
            return null;
        }

        // 跟 insert/search 一样先定方向。
        int comparison = key.compareTo(node.key);
        if (comparison < 0) {
            // 目标在左子树；接住返回值，下层结构可能变了。
            node.left = delete(node.left, key, deleted);
        } else if (comparison > 0) {
            // 目标在右子树。
            node.right = delete(node.right, key, deleted);
        } else {
            // 找到要删的 Node 了。
            deleted.changed = true;

            // BST 删除的三种 case——
            // Case 1 + Case 2：leaf（两个 child 都 null）或只有一个 child。
            // 写法上可以合并：返回「另一边的 child」。
            // leaf 时返回 null（等于直接移除）；单 child 时让 child 顶上来接位。
            if (node.left == null) {
                return node.right;
            }
            if (node.right == null) {
                return node.left;
            }

            // Case 3：有两个 children，不能直接拔掉（会同时断两棵子树）。
            // 标准做法：找 inorder successor（右子树里最小的 Node）——
            // 它是「比我大的数里最小的那个」，用它顶替我，BST 顺序仍然成立。
            // 本项目统一用 successor（不用 predecessor），全队口径一致。
            Node<K, V> successor = findMinimumNode(node.right);
            // key 和 value 必须一起复制！只复制 key 会造成 key 对到别人的 value。
            node.key = successor.key;
            node.value = successor.value;
            // successor 的内容已经搬上来了，把右子树里那个原本的 successor 删掉。
            // successor 是右子树最小者，它必然没有 left child，所以删它很简单。
            node.right = deleteMinimum(node.right);
        }

        // 回程每层 rebalance：删除和插入不同，可能一路旋转修到 root
        // （每层都可能失衡），所以这行绝对不能省。
        return rebalance(node);
    }

    /**
     * 删除子树中的最小节点；这个 helper 不再改变 size，也不重复标记 deleted。
     */
    private Node<K, V> deleteMinimum(Node<K, V> node) {
        // 最小节点 = 一直往左走到底。left == null 就是最小的那个。
        if (node.left == null) {
            // 用它的 right child 接位（可能是 null，也没关系）。
            return node.right;
        }

        // 还没到最左，继续往左递归；一样要接住返回值。
        node.left = deleteMinimum(node.left);
        // 回程也要 rebalance——为什么单独写这个 helper 而不复用 public delete()？
        // 1. 复用 public delete() 会让 size 被减两次（外层已经算过一次）。
        // 2. 会覆盖「原本有没有找到目标」的 flag 状态。
        // 3. 这里根本不用比较 key（永远往左），单独写更快也更清楚。
        return rebalance(node);
    }

    @Override
    public K getSmallestKey() {
        // 空树没有最小 key；因为 null key 禁止入树，
        // 返回 null 一定代表「树是空的」，没有歧义。
        if (root == null) {
            return null;
        }
        // BST 性质：最小的永远在最左边。复用 findMinimumNode()。
        return findMinimumNode(root).key;
    }

    @Override
    public K getLargestKey() {
        // 与最小 key 对称：空树返回 null。
        if (root == null) {
            return null;
        }

        // 最大 key = 一直往右走到底（注意：不是 root！root 通常接近中位数）。
        // VIP module 之后就靠这个拿最高 priority。
        Node<K, V> current = root;
        while (current.right != null) {
            current = current.right;
        }
        return current.key;
    }

    private Node<K, V> findMinimumNode(Node<K, V> node) {
        // 一直往左走到 left == null 为止，那个就是这棵子树的最小 Node。
        // 用 while 不用递归：只是走路，没有回程工作。
        Node<K, V> current = node;
        while (current.left != null) {
            current = current.left;
        }
        return current;
    }

    @Override
    public void traversePreOrder(Visitor<K, V> visitor) {
        // visitor 为 null 的话遍历到第一个节点就 NullPointerException，
        // 不如在门口就用统一的 IllegalArgumentException 讲清楚。
        requireVisitor(visitor);
        // public 方法只负责检查参数 + 从 root 出发；真正的遍历交给递归 helper。
        traversePreOrder(root, visitor);
    }

    private void traversePreOrder(Node<K, V> node, Visitor<K, V> visitor) {
        // 空子树没东西可访问，递归到此为止。
        if (node == null) {
            return;
        }

        // pre-order 的定义：先访问自己，再左，再右。
        // 「先自己」适合用来展示/复制树的结构（root 最先出现）。
        visitor.visit(node.key, node.value);
        traversePreOrder(node.left, visitor);
        traversePreOrder(node.right, visitor);
    }

    @Override
    public void traverseInOrder(Visitor<K, V> visitor) {
        requireVisitor(visitor);
        traverseInOrder(root, visitor);
    }

    private void traverseInOrder(Node<K, V> node, Visitor<K, V> visitor) {
        if (node == null) {
            return;
        }

        // in-order 的定义：左 -> 自己 -> 右。
        // 因为 BST 左边全比自己小、右边全比自己大，
        // 这个顺序出来的结果天然按 key 升序——report 列表就靠它。
        traverseInOrder(node.left, visitor);
        visitor.visit(node.key, node.value);
        traverseInOrder(node.right, visitor);
    }

    @Override
    public void traversePostOrder(Visitor<K, V> visitor) {
        requireVisitor(visitor);
        traversePostOrder(root, visitor);
    }

    private void traversePostOrder(Node<K, V> node, Visitor<K, V> visitor) {
        if (node == null) {
            return;
        }

        // post-order 的定义：左 -> 右 -> 自己（children 先处理完才轮到自己），
        // 适合「先处理完下面才能处理上面」的场景。
        traversePostOrder(node.left, visitor);
        traversePostOrder(node.right, visitor);
        visitor.visit(node.key, node.value);
    }

    // 为什么用 Visitor 而不是直接在 ADT 里 println？
    // ADT 不准知道业务：打印什么格式、要不要 filter、算什么统计，
    // 全部是 Control 的事。ADT 只决定「访问顺序」，动作由 visitor 注入。
    // 这也是以后配合其他 ADT 的关键：任何 module 不用改 ADT 就能自定义遍历行为。

    @Override
    public int size() {
        // 字段直接返回，O(1)；正确性由 insert/delete 各自维护。
        return size;
    }

    @Override
    public boolean isEmpty() {
        // 用 size 判断而不是 root == null：两者等价，
        // 但统一走 size 让「size 必须永远正确」这个 invariant 更容易被测试抓错。
        return size == 0;
    }

    @Override
    public void clear() {
        // root 必须一起清掉；只把 size 设为 0 会留下仍可搜索的旧节点。
        // root 一断，整棵旧树没有任何引用，交给 GC 回收即可，不用逐个删。
        root = null;
        size = 0;
    }

    /**
     * 所有 insertion/deletion 的平衡修复都集中在这里。
     * 使用 child 的 balance factor 判断双旋，亦适用于 deletion。
     */
    private Node<K, V> rebalance(Node<K, V> node) {
        // 先把自己的 height 更新到最新（下层刚刚变过），
        // 否则接下来算 balance factor 用的是旧数据，判断会错。
        updateHeight(node);
        // bf = 左高 - 右高。合法范围是 -1、0、+1。
        int balanceFactor = getBalanceFactor(node);

        // bf > 1：左边太高（L 系列失衡）。
        if (balanceFactor > 1) {
            // 看 left child 的 bf 决定是 LL 还是 LR——
            // 为什么用 child 的 bf 而不是「新 key 插在哪边」？
            // 因为 deletion 也会走进来，那时根本没有「新 key」；
            // 用 child bf 判断，insert/delete 共用同一套逻辑，
            // 连删除时 child bf == 0 的边界情况也自动正确。
            if (getBalanceFactor(node.left) < 0) {
                // LR：left child 右边高，先把 left child 左旋「拉直」成 LL 形状。
                node.left = rotateLeft(node.left);
            }
            // LL（或刚拉直的 LR）：把失衡 root 右旋一次就平衡了。
            return rotateRight(node);
        }

        // bf < -1：右边太高（R 系列失衡），与上面完全镜像。
        if (balanceFactor < -1) {
            if (getBalanceFactor(node.right) > 0) {
                // RL：right child 左边高，先把 right child 右旋拉直成 RR 形状。
                node.right = rotateRight(node.right);
            }
            // RR（或刚拉直的 RL）：把失衡 root 左旋。
            return rotateLeft(node);
        }

        // 没失衡就原样返回（height 已更新）。
        return node;
    }

    /**
     * 左旋：RR case 的主要修复动作。
     *
     * <pre>
     *     oldRoot                 newRoot
     *        \\                    /    \\
     *       newRoot     ->     oldRoot   C
     *       /    \\                \\
     *      B      C                 B
     * </pre>
     */
    private Node<K, V> rotateLeft(Node<K, V> oldRoot) {
        // 右边太高 -> 让 right child 升上来当新 root。
        Node<K, V> newRoot = oldRoot.right;
        // newRoot 的左子树（图中 B）会被挤掉，先存起来。
        // 它的 key 介于 oldRoot 和 newRoot 之间，所以等下挂到 oldRoot 右边刚好合法。
        Node<K, V> transferredSubtree = newRoot.left;

        // 两个指针动作完成旋转：oldRoot 降级成 newRoot 的 left child…
        newRoot.left = oldRoot;
        // …被挤掉的 B 补进 oldRoot 空出来的 right 位置。
        oldRoot.right = transferredSubtree;

        // oldRoot 已经变成 child，必须先更新它，再更新 newRoot——
        // 顺序反了 newRoot 会用到 oldRoot 的旧 height，结果是错的。
        updateHeight(oldRoot);
        updateHeight(newRoot);
        // 把新 root 交回给上一层接住（这就是所有 helper 都要 return 的原因）。
        return newRoot;
    }

    /**
     * 右旋：LL case 的主要修复动作；是左旋的镜像。
     *
     * <pre>
     *          oldRoot            newRoot
     *          /                 /      \\
     *      newRoot      ->      A      oldRoot
     *      /    \\                     /
     *     A      B                    B
     * </pre>
     */
    private Node<K, V> rotateRight(Node<K, V> oldRoot) {
        // 左边太高 -> 让 left child 升上来。以下每步都是 rotateLeft 的镜像。
        Node<K, V> newRoot = oldRoot.left;
        // B 介于 newRoot 和 oldRoot 之间，等下挂到 oldRoot 左边刚好合法。
        Node<K, V> transferredSubtree = newRoot.right;

        newRoot.right = oldRoot;
        oldRoot.left = transferredSubtree;

        // 一样：先更新降级的 oldRoot，再更新 newRoot。
        updateHeight(oldRoot);
        updateHeight(newRoot);
        return newRoot;
    }

    private void updateHeight(Node<K, V> node) {
        // 高度定义：1 + 较高的那个 child 的高度（课程口径 leaf = 1）。
        // height(null) = 0 由下面的 height() 处理，所以这行不用判 null。
        node.height = 1 + Math.max(height(node.left), height(node.right));
    }

    private int height(Node<K, V> node) {
        // 把「空子树高度 = 0」集中写在一个地方，
        // 其他代码就永远不用担心 node.height 的 NullPointerException。
        return node == null ? 0 : node.height;
    }

    private int getBalanceFactor(Node<K, V> node) {
        // 统一定义：左高 - 右高。正数 = 左重，负数 = 右重。
        // node == null 返回 0：让 rebalance 里对 child 取 bf 时不用判 null。
        return node == null ? 0 : height(node.left) - height(node.right);
    }

    // 三个 requireXxx 集中管理 null 政策：
    // 以后如果团队决定改政策（例如允许 null value），只改这里一处。
    private void requireKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("AVL key cannot be null.");
        }
    }

    private void requireValue(V value) {
        if (value == null) {
            throw new IllegalArgumentException("AVL value cannot be null.");
        }
    }

    private void requireVisitor(Visitor<K, V> visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException("Visitor cannot be null.");
        }
    }

    /**
     * 只供同 package 的测试验证：BST 顺序、高度、balance factor 和 size 必须同时正确。
     * 业务代码不应调用或依赖这个方法。
     */
    boolean isStructurallyValidForTesting() {
        // package-private（没写 public/private）是刻意的：
        // test 在同一个 adt package 里所以能调用，业务层碰不到它。
        // 从 root 开始全树检查，key 范围先不设上下限（null = 无限制）。
        ValidationResult result = validate(root, null, null);
        // 结构合法还不够，节点数还要跟 size 字段一致才算真的没坏。
        return result.valid && result.nodeCount == size;
    }

    private ValidationResult validate(Node<K, V> node, K lowerExclusive,
            K upperExclusive) {
        // 空子树永远合法：高度 0、节点数 0。
        if (node == null) {
            return new ValidationResult(true, 0, 0);
        }

        // 检查 BST ordering 的正确方式是「范围收窄」：
        // 每个 key 必须落在祖先传下来的 (lower, upper) 开区间内。
        // 只比较自己和 child 是不够的——孙节点可能越界但 child 检查不出来。
        boolean keyInRange = (lowerExclusive == null
                || node.key.compareTo(lowerExclusive) > 0)
                && (upperExclusive == null
                || node.key.compareTo(upperExclusive) < 0);

        // 往左走：上限收紧为自己的 key；往右走：下限收紧为自己的 key。
        ValidationResult leftResult = validate(node.left, lowerExclusive, node.key);
        ValidationResult rightResult = validate(node.right, node.key, upperExclusive);

        // 用 children 的「实际」高度重算自己的高度，跟存起来的 height 比对——
        // 抓「rotation 后忘了 updateHeight」这类 bug。
        int expectedHeight = 1 + Math.max(leftResult.height, rightResult.height);
        int balanceFactor = leftResult.height - rightResult.height;
        // 五个条件全部要过：key 在范围内、左右子树都合法、
        // 存的 height 正确、AVL 平衡（|bf| <= 1）。
        boolean valid = keyInRange
                && leftResult.valid
                && rightResult.valid
                && node.height == expectedHeight
                && Math.abs(balanceFactor) <= 1;

        // 把「合法性、实际高度、节点数」一路往上汇总。
        return new ValidationResult(valid, expectedHeight,
                1 + leftResult.nodeCount + rightResult.nodeCount);
    }

    /**
     * Node 完全封装在 AVLTree 内，Boundary/Control/Entity 永远不能直接操作它。
     */
    private static final class Node<K, V> {

        // key/value 没有 final：delete 两个 children 的 case 要把
        // successor 的 key/value 复制进来（改内容比改一堆指针简单安全）。
        private K key;
        private V value;
        // 左右 child 引用——树的骨架就是这两个指针。
        private Node<K, V> left;
        private Node<K, V> right;
        // 每个 Node 缓存自己的高度，rebalance 才能 O(1) 算 balance factor；
        // 不缓存的话每次都要 O(n) 重算子树高度，AVL 就没意义了。
        private int height;

        private Node(K key, V value) {
            this.key = key;
            this.value = value;
            // 新 Node 一定是 leaf，按课程口径 leaf 高度 = 1。
            // left/right 不用写，Java 对象字段默认就是 null。
            this.height = 1;
        }
    }

    /**
     * 让递归 helper 可以把“是否真的改变树”带回 public method。
     */
    private static final class MutationFlag {

        // Java 参数是传值的，boolean 传进递归里改了外面看不到；
        // 包一层对象，递归改 changed，外面就读得到。默认 false = 没改过。
        private boolean changed;
    }

    private static final class ValidationResult {

        // 一次递归要带三个答案回来（合不合法、实际高度、节点数），
        // Java 方法只能有一个返回值，所以打包成小对象。
        // 全部 final：结果算出来就不该被改。
        private final boolean valid;
        private final int height;
        private final int nodeCount;

        private ValidationResult(boolean valid, int height, int nodeCount) {
            this.valid = valid;
            this.height = height;
            this.nodeCount = nodeCount;
        }
    }
}

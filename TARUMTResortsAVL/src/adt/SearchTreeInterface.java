package adt;

/**
 * 通用的有序搜索树接口。
 *
 * <p>树只认识可比较的 key 和对应的 value，不认识 Booking、Member、Room 等业务类别。
 * Control 层应依赖本接口，而不是依赖 {@link AVLTree} 的内部实现。</p>
 *
 * <p>为什么要先写 interface（而不是直接用 AVLTree class）——这就是「以后容易 modify」的保证：</p>
 * <ul>
 *   <li>业务代码全部写 {@code SearchTreeInterface<K, V> tree = ...}，
 *       以后换实现、加功能，业务层一行都不用改。</li>
 *   <li>这份 interface 就是团队的 shared contract：四个人同时开工时，
 *       大家对着同一份方法签名写 Control，不会互相等。</li>
 *   <li>要加 add-on operation（例如 removeLargest()）时流程固定：
 *       ①这里加 generic 签名和 Javadoc（不准出现业务字眼）
 *       ②AVLTree 加实现 ③AVLTreeTest 加测试 ④全队同步。
 *       只在实现类加方法而不加进 interface 的话，声明成 interface 类型的
 *       Control 根本调用不到它。</li>
 *   <li>以后加别的 ADT（Queue/Stack/List）也是同一个模式：
 *       每个 ADT 一份自己的 interface + 实现，放进 adt/ package，互不依赖。</li>
 * </ul>
 *
 * <p>共同约定（全队必须遵守，测试会验证）：</p>
 * <ul>
 *   <li>key 与 value 都不能是 {@code null}。</li>
 *   <li>key 必须唯一；重复 key 不会覆盖旧 value。</li>
 *   <li>{@code compareTo() == 0} 就视为同一个 key。</li>
 *   <li>key 插入后，任何参与 {@code compareTo()} 的字段都不可直接修改。</li>
 *   <li>{@code search()} 在找不到 key 时返回 {@code null}。</li>
 *   <li>visitor 不能为 null，且 traversal 期间不可结构性修改同一棵树。</li>
 * </ul>
 *
 * <p>来源/AI 披露：本教学 reference 由 OpenAI Codex 于 2026-07-10 根据团队提供的
 * TAR UMT Chapter 9 BST 资料与 assignment 文件生成；没有复制其他团队或外部 source code。
 * AVL 是本作业的核心 Team ADT，因此本 reference 不得作为学生独立完成的 graded core
 * 原样提交。团队必须先向 tutor 确认允许的 AI 技术辅助范围、独立完成并保留自己的
 * implementation 过程，并让 source acknowledgement 与 AI Usage Disclosure 完全一致。</p>
 *
 * @param <K> 用来排序和搜索的 key 类型
 * @param <V> 树中保存的资料类型
 * @author TODO：提交前替换成实际负责组员姓名
 */
// K extends Comparable<K>：在编译期就强制「key 必须能比大小」，
// 因为 BST 的每一步都靠 compareTo() 决定往左还是往右。
// V 没有任何限制：value 只是被存放，树永远不比较 value。
public interface SearchTreeInterface<K extends Comparable<K>, V> {

    /**
     * 插入新的 key-value entry。
     *
     * <p>为什么返回 boolean 而不是 void：让 Control 一次调用就知道
     * 是「插入成功」还是「key 已存在被拒绝」，不用先 contains() 再 insert()
     * 搜两次。</p>
     *
     * @param key 新 entry 的唯一排序 key
     * @param value 要保存的非 null value
     * @return 插入成功返回 true；key 已存在则返回 false
     * @throws IllegalArgumentException key 或 value 为 null
     */
    boolean insert(K key, V value);

    /**
     * 只替换现有 key 对应的 value，不改变 key，因此不需要旋转。
     *
     * <p>为什么和 insert 分开：「新增」和「更新」是不同的业务动作。
     * insert 遇到重复 key 拒绝、replace 遇到不存在的 key 拒绝，
     * 两个方法语义都单一明确，Control 想「有则更新无则新增」时自己组合。</p>
     *
     * @param key 要更新的现有 key
     * @param value 新的非 null value
     * @return key 存在并成功替换返回 true，否则返回 false
     * @throws IllegalArgumentException key 或 value 为 null
     */
    boolean replace(K key, V value);

    /**
     * 按 key 搜索 value。
     *
     * <p>找不到返回 null 之所以没有歧义，是因为 null value 从一开始
     * 就被禁止入树——null 一定代表「不存在」。</p>
     *
     * @param key 要搜索的 key
     * @return 找到时返回 value；找不到时返回 null
     * @throws IllegalArgumentException key 为 null
     */
    V search(K key);

    /**
     * 删除指定 key 及其 value。
     *
     * @param key 要删除的 key
     * @return 有删除到 entry 返回 true；key 不存在返回 false
     * @throws IllegalArgumentException key 为 null
     */
    boolean delete(K key);

    /**
     * 检查 key 是否存在。
     *
     * <p>与 search() 的分工：只想知道「在不在」用 contains（读起来意图明确），
     * 要拿资料用 search。</p>
     *
     * @param key 要检查的 key
     * @return key 存在返回 true，否则返回 false
     * @throws IllegalArgumentException key 为 null
     */
    boolean contains(K key);

    /**
     * 返回最小 key；空树返回 null。
     *
     * <p>提供 min/max 是因为「第一个/最后一个」在业务里太常用
     * （例如最高 priority = 最大 key）；空树返回 null 没有歧义，
     * 因为 null key 禁止入树。</p>
     *
     * @return 最小 key，或空树时的 null
     */
    K getSmallestKey();

    /**
     * 返回最大 key；空树返回 null。
     *
     * @return 最大 key，或空树时的 null
     */
    K getLargestKey();

    /**
     * 前序遍历：root -> left -> right。
     *
     * <p>三个 traversal 都接收 {@link Visitor}：ADT 只决定「访问顺序」，
     * 访问到每个 entry 要做什么（打印、筛选、统计）由调用方注入，
     * 这样 ADT 永远不需要 System.out，也永远不用为新需求改代码。</p>
     *
     * @param visitor 每访问一个 entry 时执行的 callback
     * @throws IllegalArgumentException visitor 为 null
     */
    void traversePreOrder(Visitor<K, V> visitor);

    /**
     * 中序遍历：left -> root -> right，结果会按 key 升序输出。
     *
     * <p>这是 report 最常用的一个：搜索树的 in-order 天然就是排好序的。</p>
     *
     * @param visitor 每访问一个 entry 时执行的 callback
     * @throws IllegalArgumentException visitor 为 null
     */
    void traverseInOrder(Visitor<K, V> visitor);

    /**
     * 后序遍历：left -> right -> root。
     *
     * @param visitor 每访问一个 entry 时执行的 callback
     * @throws IllegalArgumentException visitor 为 null
     */
    void traversePostOrder(Visitor<K, V> visitor);

    /**
     * 返回当前 entry 数量。
     *
     * @return 当前 size
     */
    int size();

    /**
     * 检查树是否为空。
     *
     * @return size 为 0 返回 true，否则返回 false
     */
    boolean isEmpty();

    /**
     * 移除所有 entry。
     */
    void clear();
}

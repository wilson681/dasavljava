package adt;

/**
 * 遍历树时的 callback。
 *
 * <p>AVL 只决定访问顺序；Control 决定要打印、筛选、统计或产生报告。
 * 这样 ADT 内不会出现菜单或业务输出。</p>
 *
 * <p>为什么需要这个小 interface：如果 traversal 直接 println，
 * 那「换个输出格式」「只要 Dirty 的房间」「算总积分」每个需求都要改 ADT。
 * 把动作抽成 callback 注入，ADT 一行都不用改就能服务所有 module——
 * 这正是以后配合各 module 需求时最重要的扩展点。</p>
 *
 * @param <K> key 类型
 * @param <V> value 类型
 * @author TODO：提交前替换成实际负责组员姓名
 */
// @FunctionalInterface = 编译器保证这里只有一个抽象方法，
// 所以调用方可以直接写 lambda：tree.traverseInOrder((k, v) -> ...)，
// 也可以照课程风格写匿名 class，两种都合法。
@FunctionalInterface
public interface Visitor<K, V> {

    /**
     * 处理当前访问到的 entry。
     *
     * <p>树按 traversal 顺序对每个 entry 调用一次这个方法；
     * key/value 保证非 null（null 从一开始就进不了树）。</p>
     *
     * @param key 当前 entry 的 key
     * @param value 当前 entry 的 value
     */
    void visit(K key, V value);
}

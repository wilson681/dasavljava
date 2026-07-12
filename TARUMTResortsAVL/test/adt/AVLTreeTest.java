package adt;

/**
 * 不依赖 JUnit、也不使用 Java Collections Framework 的 AVL regression test。
 *
 * <p>为什么不用 JUnit：作业环境只保证有 JDK，自写一个最小 test harness
 * （run/assert 几个 static 方法）就能用 {@code java adt.AVLTreeTest} 直接跑，
 * 谁的电脑都不用装东西。</p>
 *
 * <p>为什么放在 adt package：这样才能调用 package-private 的
 * {@code isStructurallyValidForTesting()}，在每次 mutation 后验证
 * BST ordering、stored height、balance factor 与实际 node count
 * ——只看 in-order 输出「像是排好序」是抓不到高度算错这种 bug 的。</p>
 *
 * <p>执行方式见 assignment/scripts/test.sh。</p>
 *
 * @author TODO：提交前替换成实际负责组员姓名
 */
public final class AVLTreeTest {

    // 统计通过的测试数，最后打印总数；测试失败会直接抛 AssertionError 中断。
    private static int passed;

    // 工具类不该被 new，藏起 constructor。
    private AVLTreeTest() {
    }

    public static void main(String[] args) {
        // 下面每个 run(...) 注册一个测试案例。
        // 写成匿名 class 而不是 lambda 是为了兼容课程常用的旧 Java 风格；
        // 换成 lambda 也完全可以。

        // 案例 1：空树的所有查询都要有明确定义的行为（null/false/0），不能炸。
        run("empty tree", new TestCase() {
            @Override
            public void execute() {
                testEmptyTree();
            }
        });
        // 案例 2-5：四种失衡各测一次。三个 key 用不同顺序插入，
        // 正好各触发一种 rotation；修好后 root 都应该是 20。
        // LL：一路插左边（30,20,10），左左太高，右旋修复。
        run("LL insertion rotation", new TestCase() {
            @Override
            public void execute() {
                assertRotation(30, 20, 10);
            }
        });
        // RR：一路插右边（10,20,30），右右太高，左旋修复。
        run("RR insertion rotation", new TestCase() {
            @Override
            public void execute() {
                assertRotation(10, 20, 30);
            }
        });
        // LR：先左再右（30,10,20），要先左旋 left child 再右旋自己（双旋）。
        run("LR insertion rotation", new TestCase() {
            @Override
            public void execute() {
                assertRotation(30, 10, 20);
            }
        });
        // RL：先右再左（10,30,20），LR 的镜像双旋。
        run("RL insertion rotation", new TestCase() {
            @Override
            public void execute() {
                assertRotation(10, 30, 20);
            }
        });
        // 案例 6：duplicate 必须被拒绝且旧 value 不变；replace 才能改 value。
        run("duplicate and replace policy", new TestCase() {
            @Override
            public void execute() {
                testDuplicateAndReplace();
            }
        });
        // 案例 7：search/contains/min/max 的正反两面（找得到、找不到）。
        run("search, contains, min and max", new TestCase() {
            @Override
            public void execute() {
                testQueries();
            }
        });
        // 案例 8：三种 traversal 的顺序必须严格正确（用固定七节点树比对）。
        run("three traversal orders", new TestCase() {
            @Override
            public void execute() {
                testTraversals();
            }
        });
        // 案例 9-11：BST 删除的三种 case（leaf / 一个 child / 两个 children）。
        run("delete leaf", new TestCase() {
            @Override
            public void execute() {
                testDeleteLeaf();
            }
        });
        run("delete one-child node", new TestCase() {
            @Override
            public void execute() {
                testDeleteOneChild();
            }
        });
        run("delete two-child node and root", new TestCase() {
            @Override
            public void execute() {
                testDeleteTwoChildrenAndRoot();
            }
        });
        // 案例 12-15：删除也会触发四种 rotation（跟插入不同的代码路径），
        // 每种都构造一棵删掉某个 key 后刚好失衡的树来验证。
        run("LL deletion rotation", new TestCase() {
            @Override
            public void execute() {
                // 删 40 后右边变矮 -> 左重 LL -> 右旋后 preorder 应为 20,10,30,25。
                assertDeleteRotation(new int[]{30, 20, 40, 10, 25}, 40,
                        "20,10,30,25");
            }
        });
        run("RR deletion rotation", new TestCase() {
            @Override
            public void execute() {
                // 删 10 后左边变矮 -> 右重 RR -> 左旋。
                assertDeleteRotation(new int[]{20, 10, 30, 25, 40}, 10,
                        "30,20,25,40");
            }
        });
        run("LR deletion rotation", new TestCase() {
            @Override
            public void execute() {
                // 删 40 后失衡且 left child 右重 -> 双旋 LR。
                assertDeleteRotation(new int[]{30, 10, 40, 20}, 40,
                        "20,10,30");
            }
        });
        run("RL deletion rotation", new TestCase() {
            @Override
            public void execute() {
                // 删 0 后失衡且 right child 左重 -> 双旋 RL。
                assertDeleteRotation(new int[]{10, 0, 30, 20}, 0,
                        "20,10,30");
            }
        });
        // 案例 16：clear 之后旧资料必须搜不到，而且树要能继续用。
        run("clear and reuse", new TestCase() {
            @Override
            public void execute() {
                testClearAndReuse();
            }
        });
        // 案例 17：所有接收 null 的入口都必须抛 IllegalArgumentException。
        run("null contract", new TestCase() {
            @Override
            public void execute() {
                testNullContract();
            }
        });
        // 案例 18：200 个连续 key 全插入再全删除，每一步都验证完整 invariant——
        // 连续递增插入是最容易让不平衡树退化成链表的输入，AVL 必须扛住。
        run("sequential stress insertion and deletion", new TestCase() {
            @Override
            public void execute() {
                testStress();
            }
        });
        // 案例 19：固定种子的随机混合操作，与一个「肯定正确」的数组模型对照，
        // 抓单一场景测试想不到的组合 bug。
        run("fixed-seed mixed operation model", new TestCase() {
            @Override
            public void execute() {
                testMixedOperations();
            }
        });

        // 能走到这里代表没有任何测试抛出错误。
        System.out.println("\nAll " + passed + " AVL tests passed.");
    }

    private static void testEmptyTree() {
        // 新建的树就是最简单的边界情况。
        AVLTree<Integer, String> tree = new AVLTree<Integer, String>();
        // 空树的每个查询都要有安全、明确的返回值：
        assertTrue(tree.isEmpty(), "new tree should be empty");
        assertEquals(0, tree.size(), "empty size");
        assertNull(tree.search(100), "search missing key");
        assertFalse(tree.delete(100), "delete missing key");
        assertNull(tree.getSmallestKey(), "empty minimum");
        assertNull(tree.getLargestKey(), "empty maximum");
        // 空树也要通过结构验证（0 个节点、size == 0）。
        assertValid(tree);
    }

    /**
     * 四种 insertion rotation 共用的检查：三个 key 按指定顺序插入后，
     * 不管是 LL/RR/LR/RL，修复完的树形状都一样（root 20、左 10、右 30）。
     */
    private static void assertRotation(int first, int second, int third) {
        AVLTree<Integer, String> tree = new AVLTree<Integer, String>();
        // 按参数顺序插入，第三个 insert 会触发对应的 rotation。
        insert(tree, first);
        insert(tree, second);
        insert(tree, third);

        // pre-order 的第一个元素是 root，所以用它验证「树的形状」；
        // 四种 case 修好后都应该是同一棵树。
        assertEquals("20,10,30", traversal(tree, Order.PRE_ORDER),
                "all four cases should rotate to root 20");
        // in-order 必须仍然有序——rotation 只准改形状，不准改顺序。
        assertEquals("10,20,30", traversal(tree, Order.IN_ORDER),
                "in-order must remain sorted");
        // rotation 只是搬指针，不增减 entry。
        assertEquals(3, tree.size(), "rotation must not change size");
        assertValid(tree);
    }

    private static void testDuplicateAndReplace() {
        AVLTree<Integer, String> tree = new AVLTree<Integer, String>();
        // 第一次插入应成功。
        assertTrue(tree.insert(10, "original"), "first insert");
        // 同 key 再插入必须被拒绝（返回 false）……
        assertFalse(tree.insert(10, "duplicate"), "duplicate should be rejected");
        // ……而且 size 不变、旧 value 完好，证明拒绝是「完全没动树」。
        assertEquals(1, tree.size(), "duplicate must not increase size");
        assertEquals("original", tree.search(10), "old value must remain");

        // 改 value 的正确方式是 replace()。
        assertTrue(tree.replace(10, "updated"), "replace existing key");
        assertEquals("updated", tree.search(10), "replacement value");
        // replace 不存在的 key 要返回 false，不准偷偷帮忙 insert。
        assertFalse(tree.replace(99, "missing"), "replace missing key");
        assertEquals(1, tree.size(), "replace must not change size");
        assertValid(tree);
    }

    private static void testQueries() {
        // 用固定的七节点平衡树，位置全部已知，方便断言。
        AVLTree<Integer, String> tree = balancedSevenNodeTree();
        // 特意挑三个代表位置：最小 leaf、root、最大 leaf。
        assertTrue(tree.contains(5), "contains smallest");
        assertTrue(tree.contains(20), "contains root");
        assertTrue(tree.contains(35), "contains largest");
        // 反面案例：不存在的 key。
        assertFalse(tree.contains(99), "does not contain missing key");
        // search 要拿回正确的 value（helper 存的是 "v" + key）。
        assertEquals("v25", tree.search(25), "search value");
        // min/max 各自是最左、最右的 key。
        assertEquals(Integer.valueOf(5), tree.getSmallestKey(), "minimum key");
        assertEquals(Integer.valueOf(35), tree.getLargestKey(), "maximum key");
        assertValid(tree);
    }

    private static void testTraversals() {
        AVLTree<Integer, String> tree = balancedSevenNodeTree();
        // 七节点树的三种遍历结果是可以手算的，直接精确比对：
        // pre-order：自己 -> 左 -> 右（root 在最前）。
        assertEquals("20,10,5,15,30,25,35", traversal(tree, Order.PRE_ORDER),
                "pre-order");
        // in-order：左 -> 自己 -> 右（一定是升序）。
        assertEquals("5,10,15,20,25,30,35", traversal(tree, Order.IN_ORDER),
                "in-order");
        // post-order：左 -> 右 -> 自己（root 在最后）。
        assertEquals("5,15,10,25,35,30,20", traversal(tree, Order.POST_ORDER),
                "post-order");
        assertValid(tree);
    }

    private static void testDeleteLeaf() {
        AVLTree<Integer, String> tree = balancedSevenNodeTree();
        // 5 在七节点树里是 leaf——删除 case 1。
        assertTrue(tree.delete(5), "delete existing leaf");
        assertFalse(tree.contains(5), "leaf should be gone");
        assertEquals(6, tree.size(), "size after leaf deletion");
        // 删同一个 key 第二次必须返回 false，size 不再变。
        assertFalse(tree.delete(5), "delete same key twice");
        assertEquals(6, tree.size(), "missing delete must not change size");
        assertValid(tree);
    }

    private static void testDeleteOneChild() {
        AVLTree<Integer, String> tree = new AVLTree<Integer, String>();
        // 构造 30 只有一个 left child (25) 的树——删除 case 2。
        int[] values = {20, 10, 30, 25};
        insertAll(tree, values);

        assertTrue(tree.delete(30), "delete node with one left child");
        // 25 必须接上 30 原本的位置，顺序不变。
        assertEquals("10,20,25", traversal(tree, Order.IN_ORDER),
                "only child must reconnect to parent");
        assertEquals(3, tree.size(), "size after one-child deletion");
        assertValid(tree);

        // 特例：被删的是 root 本身，child 要能直接当新 root（只有右 child）。
        AVLTree<Integer, String> rootWithRightChild = new AVLTree<Integer, String>();
        insertAll(rootWithRightChild, new int[]{20, 30});
        assertTrue(rootWithRightChild.delete(20), "delete root with only right child");
        assertEquals("30", traversal(rootWithRightChild, Order.PRE_ORDER),
                "right child should become root");
        assertValid(rootWithRightChild);

        // 镜像：只有左 child。
        AVLTree<Integer, String> rootWithLeftChild = new AVLTree<Integer, String>();
        insertAll(rootWithLeftChild, new int[]{20, 10});
        assertTrue(rootWithLeftChild.delete(20), "delete root with only left child");
        assertEquals("10", traversal(rootWithLeftChild, Order.PRE_ORDER),
                "left child should become root");
        assertValid(rootWithLeftChild);

        // 最极端：整棵树只有 root 一个节点，删掉后要回到空树状态。
        AVLTree<Integer, String> singleNodeTree = new AVLTree<Integer, String>();
        insert(singleNodeTree, 20);
        assertTrue(singleNodeTree.delete(20), "delete only root leaf");
        assertTrue(singleNodeTree.isEmpty(), "single-node tree should become empty");
        assertValid(singleNodeTree);
    }

    private static void testDeleteTwoChildrenAndRoot() {
        AVLTree<Integer, String> tree = balancedSevenNodeTree();
        // 30 有两个 children (25, 35)——删除 case 3，会用 successor 顶替。
        assertTrue(tree.delete(30), "delete internal two-child node");
        assertFalse(tree.contains(30), "deleted internal key");
        assertEquals("5,10,15,20,25,35", traversal(tree, Order.IN_ORDER),
                "successor replacement must keep ordering");
        // 关键检查：successor(35) 顶上来时 key 和 value 必须一起搬，
        // 只搬 key 的话 35 会对到别人的 value。
        assertEquals("v35", tree.search(35), "successor key and value must stay paired");
        assertValid(tree);

        // 再删 root 本身（也是两个 children 的 case），路径不同要单独测。
        assertTrue(tree.delete(20), "delete root with two children");
        assertFalse(tree.contains(20), "deleted root key");
        assertEquals(5, tree.size(), "two successful deletes change size twice");
        assertValid(tree);
    }

    /**
     * 删除触发 rotation 的共用检查：插入固定 key 组成特定形状，
     * 删掉指定 key 让树失衡，再用 pre-order 精确比对修复后的形状。
     */
    private static void assertDeleteRotation(int[] values, int keyToDelete,
            String expectedPreOrder) {
        AVLTree<Integer, String> tree = new AVLTree<Integer, String>();
        insertAll(tree, values);
        // 删之前先确认构造出来的树本身是合法的。
        assertValid(tree);

        assertTrue(tree.delete(keyToDelete), "delete rotation trigger");
        // pre-order 能同时验证 root 和整体形状。
        assertEquals(expectedPreOrder, traversal(tree, Order.PRE_ORDER),
                "unexpected root/shape after deletion rebalance");
        assertValid(tree);
    }

    private static void testClearAndReuse() {
        AVLTree<Integer, String> tree = balancedSevenNodeTree();
        tree.clear();

        // clear 后必须是「真的空」：
        assertTrue(tree.isEmpty(), "clear should empty tree");
        assertEquals(0, tree.size(), "size after clear");
        // 这条抓「只把 size 归零但没断开 root」的偷懒实现——旧资料必须搜不到。
        assertNull(tree.search(20), "old root must no longer be searchable");
        assertEquals("", traversal(tree, Order.IN_ORDER), "traversal after clear");
        assertValid(tree);

        // clear 之后这棵树必须还能正常继续用，不是一次性的。
        assertTrue(tree.insert(7, "seven"), "reuse after clear");
        assertEquals("seven", tree.search(7), "search after reuse");
        assertValid(tree);
    }

    private static void testNullContract() {
        // final 是因为匿名 class 里要引用外面的变量。
        final AVLTree<Integer, String> tree = new AVLTree<Integer, String>();
        // 逐一验证每个入口的 null 检查：任何一个漏了，
        // null 就会溜进树里，之后 compareTo() 在别处炸，很难查。
        assertIllegalArgument(new TestCase() {
            @Override
            public void execute() {
                tree.insert(null, "value");
            }
        }, "null insert key");
        assertIllegalArgument(new TestCase() {
            @Override
            public void execute() {
                tree.insert(1, null);
            }
        }, "null insert value");
        assertIllegalArgument(new TestCase() {
            @Override
            public void execute() {
                tree.search(null);
            }
        }, "null search key");
        assertIllegalArgument(new TestCase() {
            @Override
            public void execute() {
                tree.replace(null, "value");
            }
        }, "null replace key");
        assertIllegalArgument(new TestCase() {
            @Override
            public void execute() {
                tree.replace(1, null);
            }
        }, "null replace value");
        assertIllegalArgument(new TestCase() {
            @Override
            public void execute() {
                tree.delete(null);
            }
        }, "null delete key");
        assertIllegalArgument(new TestCase() {
            @Override
            public void execute() {
                tree.contains(null);
            }
        }, "null contains key");
        assertIllegalArgument(new TestCase() {
            @Override
            public void execute() {
                tree.traversePreOrder(null);
            }
        }, "null preorder visitor");
        assertIllegalArgument(new TestCase() {
            @Override
            public void execute() {
                tree.traverseInOrder(null);
            }
        }, "null visitor");
        assertIllegalArgument(new TestCase() {
            @Override
            public void execute() {
                tree.traversePostOrder(null);
            }
        }, "null postorder visitor");
        // 被拒绝的调用不准留下任何痕迹，树必须还是合法的空树。
        assertValid(tree);
    }

    private static void testStress() {
        AVLTree<Integer, String> tree = new AVLTree<Integer, String>();

        // 1..200 严格递增插入：这是普通 BST 的最坏输入（会退化成链表），
        // AVL 必须靠 rotation 保持平衡——每插一个就全树验证一次。
        for (int key = 1; key <= 200; key++) {
            assertTrue(tree.insert(key, "v" + key), "stress insert " + key);
            assertValid(tree);
        }
        assertEquals(200, tree.size(), "stress size after insertion");
        assertEquals(Integer.valueOf(1), tree.getSmallestKey(), "stress minimum");
        assertEquals(Integer.valueOf(200), tree.getLargestKey(), "stress maximum");

        // 先删所有奇数再删所有偶数：制造大量不同形状的删除场景，
        // 每删一个都验证 invariant（deletion rebalance 的地毯式测试）。
        for (int key = 1; key <= 199; key += 2) {
            assertTrue(tree.delete(key), "stress delete odd " + key);
            assertValid(tree);
        }
        for (int key = 2; key <= 200; key += 2) {
            assertTrue(tree.delete(key), "stress delete even " + key);
            assertValid(tree);
        }

        // 全部删完要回到干净的空树。
        assertTrue(tree.isEmpty(), "stress tree should finish empty");
        assertEquals(0, tree.size(), "stress final size");
    }

    /**
     * 使用普通 arrays 作为独立 reference model，不使用 Java Collections Framework。
     *
     * <p>思路：boolean[] + String[] 这种「笨但绝对正确」的结构当标准答案，
     * 对树做几千步随机操作，每一步都要求树的行为和标准答案完全一致。
     * 单一场景测试想不到的操作组合，这里都会被扫到。</p>
     */
    private static void testMixedOperations() {
        // key 限制在 0..63：范围小，insert/delete 才会频繁撞到同一个 key，
        // duplicate/missing 的分支才测得到。
        final int keyRange = 64;
        final int steps = 3000;
        // reference model：present[k] = key k 在不在；expectedValues[k] = 应有的 value。
        boolean[] present = new boolean[keyRange];
        String[] expectedValues = new String[keyRange];
        int expectedSize = 0;
        // 固定种子 -> 每次跑的操作序列一模一样，失败可以重现（这点比「更随机」重要）。
        long randomState = 0xC0FFEEL;
        AVLTree<Integer, String> tree = new AVLTree<Integer, String>();

        for (int step = 0; step < steps; step++) {
            // 自写线性同余产生器（不用 java.util.Random 也能有稳定伪随机数）。
            randomState = nextRandom(randomState);
            int key = (int) (randomState % keyRange);
            randomState = nextRandom(randomState);
            // 随机挑五种操作之一。
            int operation = (int) (randomState % 5);
            // 每一步用不同的 value，才能发现「value 串位」的 bug。
            String newValue = "mixed-" + step;

            switch (operation) {
                case 0:
                    // insert：树的返回值必须等于模型的预期（不在才 true）。
                    boolean shouldInsert = !present[key];
                    assertEquals(Boolean.valueOf(shouldInsert),
                            Boolean.valueOf(tree.insert(key, newValue)),
                            "mixed insert result at step " + step);
                    if (shouldInsert) {
                        // 树插入成功，模型也同步更新。
                        present[key] = true;
                        expectedValues[key] = newValue;
                        expectedSize++;
                    }
                    break;
                case 1:
                    // delete：在才删得掉。
                    boolean shouldDelete = present[key];
                    assertEquals(Boolean.valueOf(shouldDelete),
                            Boolean.valueOf(tree.delete(key)),
                            "mixed delete result at step " + step);
                    if (shouldDelete) {
                        present[key] = false;
                        expectedValues[key] = null;
                        expectedSize--;
                    }
                    break;
                case 2:
                    // replace：只有存在的 key 能替换成功。
                    assertEquals(Boolean.valueOf(present[key]),
                            Boolean.valueOf(tree.replace(key, newValue)),
                            "mixed replace result at step " + step);
                    if (present[key]) {
                        expectedValues[key] = newValue;
                    }
                    break;
                case 3:
                    // search：拿到的 value 必须和模型记的一样（不在就是 null）。
                    assertEquals(expectedValues[key], tree.search(key),
                            "mixed search result at step " + step);
                    break;
                case 4:
                    // contains：和模型的 present 一致。
                    assertEquals(Boolean.valueOf(present[key]),
                            Boolean.valueOf(tree.contains(key)),
                            "mixed contains result at step " + step);
                    break;
                default:
                    // % 5 不可能到这里；防御性写法，逻辑错了立刻炸出来。
                    throw new IllegalStateException("Unexpected mixed operation.");
            }

            // 每一步之后：size 要对、整树 invariant 要成立。
            assertEquals(expectedSize, tree.size(), "mixed size at step " + step);
            assertValid(tree);

            // 每 47 步做一次全量对账（k=v 全部比对）。
            // 不每步都做是因为太慢；47 是质数，能错开操作的周期性。
            if (step % 47 == 0) {
                assertEquals(expectedPairText(present, expectedValues),
                        pairTraversal(tree),
                        "mixed key-value traversal at step " + step);
            }
        }

        // 结束时再做最后一次全量对账。
        assertEquals(expectedPairText(present, expectedValues), pairTraversal(tree),
                "mixed final key-value traversal");
    }

    private static long nextRandom(long state) {
        // 经典 LCG 参数（Numerical Recipes）；& 0xffffffffL 模拟 32-bit 溢出，
        // 保证在任何平台上序列都一样。
        return (state * 1664525L + 1013904223L) & 0xffffffffL;
    }

    private static String expectedPairText(boolean[] present, String[] values) {
        // 把 reference model 的内容按 key 升序拼成 "k=v,k=v"——
        // 正好和树的 in-order 遍历顺序一致，可以直接比字符串。
        StringBuilder builder = new StringBuilder();
        for (int key = 0; key < present.length; key++) {
            if (present[key]) {
                appendPair(builder, key, values[key]);
            }
        }
        return builder.toString();
    }

    private static String pairTraversal(AVLTree<Integer, String> tree) {
        // 用 in-order + Visitor 把树的实际内容拼成同样格式的字符串。
        final StringBuilder builder = new StringBuilder();
        tree.traverseInOrder(new Visitor<Integer, String>() {
            @Override
            public void visit(Integer key, String value) {
                appendPair(builder, key, value);
            }
        });
        return builder.toString();
    }

    private static void appendPair(StringBuilder builder, int key, String value) {
        // 除了第一个 pair，前面都补逗号——避免结尾多一个逗号的经典问题。
        if (builder.length() > 0) {
            builder.append(',');
        }
        builder.append(key).append('=').append(value);
    }

    private static AVLTree<Integer, String> balancedSevenNodeTree() {
        // 多个测试共用的固定形状：20 当 root，10/30 第二层，5/15/25/35 第三层。
        // 按这个顺序插入不会触发任何 rotation，所以形状是可预测的。
        AVLTree<Integer, String> tree = new AVLTree<Integer, String>();
        insertAll(tree, new int[]{20, 10, 30, 5, 15, 25, 35});
        return tree;
    }

    private static void insertAll(AVLTree<Integer, String> tree, int[] keys) {
        // 批量插入的小工具，让测试案例的构造代码短一点。
        for (int key : keys) {
            insert(tree, key);
        }
    }

    private static void insert(AVLTree<Integer, String> tree, int key) {
        // value 统一用 "v" + key 的格式，之后 search 断言才有已知答案。
        // 顺便断言 insert 成功——测试数据不该有 duplicate。
        assertTrue(tree.insert(key, "v" + key), "insert key " + key);
    }

    private static String traversal(AVLTree<Integer, String> tree, Order order) {
        // 把任一种遍历的 key 序列拼成 "a,b,c" 字符串，方便精确比对。
        final StringBuilder builder = new StringBuilder();
        Visitor<Integer, String> visitor = new Visitor<Integer, String>() {
            @Override
            public void visit(Integer key, String value) {
                if (builder.length() > 0) {
                    builder.append(',');
                }
                builder.append(key);
            }
        };

        // 同一个 visitor 接到不同的遍历方法上——正是 Visitor 模式的用法示范。
        switch (order) {
            case PRE_ORDER:
                tree.traversePreOrder(visitor);
                break;
            case IN_ORDER:
                tree.traverseInOrder(visitor);
                break;
            case POST_ORDER:
                tree.traversePostOrder(visitor);
                break;
            default:
                throw new IllegalStateException("Unsupported order.");
        }
        return builder.toString();
    }

    private static void assertValid(AVLTree<Integer, String> tree) {
        // 一行调用完成全树体检（BST 顺序 + height + balance factor + size）。
        // 几乎每个测试的每次 mutation 后都会调用它——这是整个 suite 的骨干。
        assertTrue(tree.isStructurallyValidForTesting(),
                "BST order, height, balance factor or size invariant failed");
    }

    private static void assertIllegalArgument(TestCase testCase, String message) {
        try {
            // 执行应该抛异常的动作……
            testCase.execute();
            // ……没抛就是失败（这行只有在没抛异常时才会执行到）。
            throw new AssertionError(message + ": expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // 抓到预期的异常 = 通过，什么都不用做。
        }
    }

    // 以下是自写的最小断言工具（代替 JUnit 的 assertTrue/assertEquals 等）：
    // 条件不满足就抛 AssertionError，带上讲得清楚的 message。

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        // 直接复用 assertTrue，少一份重复逻辑。
        assertTrue(!condition, message);
    }

    private static void assertNull(Object actual, String message) {
        if (actual != null) {
            // 把实际值放进 message，失败时不用 debug 就知道差在哪。
            throw new AssertionError(message + ": expected null but was " + actual);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        // null-safe 的相等比较：expected 为 null 时不能调用 equals()。
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected
                    + " but was " + actual);
        }
    }

    private static void run(String name, TestCase testCase) {
        try {
            // 执行测试本体。
            testCase.execute();
            // 没抛异常 = 通过。
            passed++;
            System.out.println("[PASS] " + name);
        } catch (RuntimeException exception) {
            // 分开抓 RuntimeException 和 AssertionError（它们的父类不同），
            // 打印是哪个测试挂了再往外抛，让 test.sh 以非零码结束。
            System.err.println("[FAIL] " + name + ": " + exception.getMessage());
            throw exception;
        } catch (AssertionError error) {
            System.err.println("[FAIL] " + name + ": " + error.getMessage());
            throw error;
        }
    }

    // 自写的最小「测试案例」接口：一个测试 = 一段可以执行的代码。
    // 有了它，run() 才能统一地执行任何测试并统计结果。
    private interface TestCase {

        void execute();
    }

    // 遍历顺序的枚举，给 traversal() 当参数用，比传 int/String 安全。
    private enum Order {
        PRE_ORDER,
        IN_ORDER,
        POST_ORDER
    }
}

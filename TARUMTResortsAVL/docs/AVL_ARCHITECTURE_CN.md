# TAR UMT Resorts AVL 基础架构与协作说明

## 1. 先定资料权威顺序

判断作业要求时按以下顺序：

1. `datastructuresassignment/_202605 BMCS2063 Assignment Specification.pdf`
2. `datastructuresassignment/202605 BMCS063 Assignment Rubrics.pdf`
3. `datastructuresassignment/202605 BMCS2063 Report Template.pdf`
4. `datastructuresassignment/BMCS2063 Assignment Q_A.pdf`
5. Lecture、Practical、Appendix 和官方 `ECBDemo 2`
6. 根目录及 `output/pdf/` 的中文指南只当内部解释

文件名含 `TARUMT` 不代表它就是校方正式文件。现有几份中文指南把 Queue/Stack、报告排序与四人分工说错了，不能覆盖正式 specification。

## 2. 这次架构已经决定与尚未决定的事

已经决定：

- 团队评核的一个 Collection ADT 选择 AVL Search Tree。
- Java console prototype。
- ECB 分层。
- AVL 使用 generic key-value entry：`SearchTreeInterface<K, V>`。
- 重复 key 拒绝；更新 value 必须显式 `replace()`。
- null key/value 禁止。
- height(null) = 0，height(leaf) = 1。
- 删除双子节点统一使用 inorder successor（右子树最小节点）。

还没有决定：

- 四位成员最终获批的四个不重复 module。
- 各 module 的 Entity fields、key 与 report criteria。
- 哪些课程 sample Linear ADT 要复制进项目。
- 各成员贡献的 add-on AVL operation。
- 是否需要 file persistence。

### 必须向 tutor 确认的命名口径

课程 Chapter 9 的普通 BST sample 是单泛型 `T`；当前实现保存 `(key, value)`，在 Chapter 10 的术语中也接近 ordered dictionary。技术上这是常见的 key-value search tree，但 Team Report 必须准确写成类似：

> Our team collection ADT is a generic key-value AVL Search Tree whose data consists of unique (K,V) pairs, implemented using AVL self-balancing.

不要一边交 key-value interface，一边把 ADT specification 写成课程单 entry BST 的原样版本。在任何正式 ModuleControl 依赖这个 API 前，必须立即让 tutor 确认登记名称、ADT specification、interface 与 implementation 的统一口径。若 tutor 只接受课程式单 entry AVL/BST，要先调整 API，再开始业务层。

## 3. 官方硬要求，不能被“我们选 AVL”掩盖

- 整体系统必须体现 Linear ADT、Non-Linear ADT、明确 searching 和明确 sorting。
- 团队只提交及评核一个 Collection ADT，不等于项目只准出现一个 ADT。
- 每名成员做一个不同 module，团队最后整合。
- 每人至少两份 report；报告要结合 searching、自写 sorting、多条件 filter，不能只是 inorder/list all。
- 可以使用课程 sample ADT，但禁止 Java Collections Framework collection class/interface。
- `Comparator`、`Iterator`、`Scanner` 等非 collection 类型允许；`Collections.sort()` 不允许。
- 必须使用 ECB，最终交 NetBeans project、实际数据文件（若有）和 `ReadMe.txt`。
- 所有新 class/method/variable 必须遵守 Java standard naming convention / CamelCase。
- 每个 authored Java class 开头要有作者姓名；改编来源要在 interface/class 开头注明。
- 本作业是 Yellow / Limited AI：AI 不得生成 entire application 或 complete primary module。当前完整 AVL 是 Codex 教学 reference，不能靠“改几行再披露”就变成合规的学生核心成果；团队必须先问 tutor 允许范围，并独立完成、保留和解释自己的核心 implementation 过程，同时如实披露。

## 4. 项目目录与每个文件的责任

```text
assignment/
├── build.xml                         NetBeans/Ant build entry
├── manifest.mf                       JAR manifest template
├── nbproject/                        NetBeans project metadata
├── ReadMe.txt                        最终项目必须有的运行说明
├── scripts/
│   ├── common.sh                     找可用 JDK
│   ├── compile.sh                    编译 src
│   ├── run.sh                        跑 main.Main
│   └── test.sh                       编译并跑 AVL tests
├── docs/
│   └── AVL_ARCHITECTURE_CN.md        本说明
├── data/
│   └── README.txt                    正式 data file 放置规则
├── src/
│   ├── main/
│   │   └── Main.java                 placeholder 入口；整合时改成启动 MainMenuControl
│   ├── adt/
│   │   ├── SearchTreeInterface.java  团队 AVL contract（graded Team ADT）
│   │   ├── AVLTree.java              唯一 AVL implementation（rotation/insert/delete 全在此）
│   │   ├── Visitor.java              traversal callback
│   │   └── package-info.java
│   ├── boundary/  control/  entity/  dao/  utility/
│   │   └── package-info.java         各 package 只留责任说明；等 module 分工与
│   │                                 辅助方案确定后才建 class
└── test/
    └── adt/
        └── AVLTreeTest.java          无 JUnit regression/invariant tests
```

### 为什么 Node 不独立成 `AVLNode.java`

Node 是 `AVLTree` 的 private static nested class。Control、Boundary 或 Entity 若能拿到 Node，就能随意改 `left/right/height`，整棵树的 invariant 会失效。对外只暴露 interface operations。

## 5. 各层怎样配合

```text
Actor
  -> Boundary（问输入、显示结果）
      <-> Control（决定 use-case flow）
            -> SearchTreeInterface（团队共同 contract）
                  -> AVLTree（Node / height / rotation 完全封装）
            -> Entity（POJO）
            -> DAO（只有真的 save/load 才用）
```

关键规则：

- `Main` 不写 menu 或 booking logic，只启动顶层 Control。
- Control 字段写 `SearchTreeInterface<String, Booking>`，不要写死成 `AVLTree<String, Booking>`。
- Boundary 不调用 `insert/delete`，也不 import Node。
- Entity 不持有 AVL，不出现 Scanner/System.out。
- ADT 不 import Boundary/Control/Entity，不 print 错误或 report。
- 真正整合时在一个 composition root 建立共享 tree，再传给有关 Controls；不要每个 Control 自己 `new` 一棵同类 tree，否则模块资料互相看不到。

### 将来整合时的全局共享 state（现在还没建）

全队既然全局都用 AVL，整合时在唯一的 composition root（建议叫 `MainMenuControl`，
也是唯一允许 `new AVLTree` 的业务类）建立共享 tree，constructor 注入相关 Control：

```java
private final SearchTreeInterface<String, Booking> bookingTree; // 例：confirmationNo -> Booking
private final SearchTreeInterface<String, Room> roomTree;       // 例：roomNo -> Room
private final SearchTreeInterface<String, Member> memberTree;   // 例：memberId -> Member
```

规则：

- 不要用 static global；也不要每个 Control 自己 new 同类 tree，否则模块资料互相看不到。
- 共享同一个 value 对象 reference，状态改一次全系统可见，不需要同步逻辑。
- 某个 module 私有的结构（若以后需要）在自己 Control 里建；要跨 module 共享时
  提升到 composition root，多传一个 constructor 参数即可。
- 修改 value 对象的非 key 字段直接 setter 即可，不需 `replace()`；
  key 或参与 `compareTo()` 的字段永远不可原地修改，必须 delete 旧 key 再 insert。

Entity 仍应按 Q&A 建立 constructor、getter/setter，并按需要正确 override `toString()` 与 `equals()`。在当前 K,V 设计中，真正决定树排序的是 `K` 或 composite key，所以它必须正确实现 `Comparable<K>`；`V` Entity 本身不一定需要 Comparable。不要为了模仿单泛型 BST 而给每个 Entity 随意写一个错误的 `compareTo()`。

## 6. 当前 v1 API contract

```java
boolean insert(K key, V value);
boolean replace(K key, V value);
V search(K key);
boolean delete(K key);
boolean contains(K key);

K getSmallestKey();
K getLargestKey();

void traversePreOrder(Visitor<K, V> visitor);
void traverseInOrder(Visitor<K, V> visitor);
void traversePostOrder(Visitor<K, V> visitor);

int size();
boolean isEmpty();
void clear();
```

语义：

- `compareTo() == 0` 就是相同 key。
- duplicate insert 返回 false，旧 value 完全不变，size 不变。
- `replace()` 只换 value，不换 key，不需 rotation，size 不变。
- `search()` 找不到返回 null；因为 null value 禁止，所以没有歧义。
- `delete()` 只有真的删除才返回 true、size--。
- min/max 在空树返回 null；null key 禁止，所以也没有歧义。
- Visitor 让 Control 决定打印、统计或筛选，ADT 只决定 traversal order。

## 7. AVL 内部 invariant

每个 Node 存：

```text
key, value, left, right, height
```

统一定义：

```text
height(null) = 0
height(leaf) = 1
height(node) = 1 + max(height(left), height(right))
balanceFactor(node) = height(left) - height(right)
```

正确 AVL 必须同时满足：

```text
所有 left key < node key < 所有 right key
stored height 与实际高度一致
每个 node 的 abs(balanceFactor) <= 1
实际 node 数量 == size
```

`AVLTreeTest` 在各关键 mutation 后验证这些条件；sequential/mixed stress case 会在每次 mutation 后验证，不只是检查 inorder 看起来有序。

## 8. 两个 primitive rotation 与四种 case

### Left rotation（修 RR）

```text
oldRoot = x
newRoot = x.right
transfer = newRoot.left

newRoot.left = oldRoot
oldRoot.right = transfer

先 updateHeight(oldRoot)
再 updateHeight(newRoot)
return newRoot
```

### Right rotation（修 LL）

完全镜像：

```text
oldRoot = y
newRoot = y.left
transfer = newRoot.right

newRoot.right = oldRoot
oldRoot.left = transfer

先 updateHeight(oldRoot)
再 updateHeight(newRoot)
return newRoot
```

### 四种 case

```text
LL: node bf > 1，left child bf >= 0
    -> rotateRight(node)

RR: node bf < -1，right child bf <= 0
    -> rotateLeft(node)

LR: node bf > 1，left child bf < 0
    -> node.left = rotateLeft(node.left)
    -> rotateRight(node)

RL: node bf < -1，right child bf > 0
    -> node.right = rotateRight(node.right)
    -> rotateLeft(node)
```

实现用 child balance factor 判断，不用“新 key 插在哪边”判断。因此同一套 `rebalance()` 对 insertion 和 deletion 都成立；删除时 child bf 刚好为 0 的情况也不会漏掉。

## 9. Insert 为什么要返回新的 subtree root

递归 insertion：

```text
node == null -> 建 leaf，标记真的 inserted
key < node.key -> node.left  = insert(node.left, ...)
key > node.key -> node.right = insert(node.right, ...)
key == node.key -> reject duplicate，直接返回 node
最后 return rebalance(node)
```

rotation 会更换 subtree root，所以 parent 必须接住 helper 的 return；public method 也必须：

```java
root = insert(root, key, value, flag);
```

只有 flag 显示真的创建 Node 才 `size++`，避免先 `contains()` 再 insert 的双重搜索。

## 10. Delete 三种 BST case 与 AVL 回程修复

找到目标后：

```text
0 child -> return null
1 child -> return 唯一 child
2 children:
    successor = 右子树最小 Node
    当前 Node 同时复制 successor.key 与 successor.value
    node.right = deleteMinimum(node.right)
```

然后每一层 ancestor 都 `return rebalance(node)`，所以删除可能一路修到 root。

为什么 `deleteMinimum()` 是 private helper：

- 不应再次调用 public `delete()`，否则 size 可能减两次。
- 不应覆盖“原本有没有找到目标”的状态。
- 两个 children 时必须 key 和 value 一起复制，不能只复制一边。

## 11. duplicate 与 VIP priority

普通 BST slide 曾提到 equal entry 放右边，但 AVL rotation 后 equal key 可能被转到左边，使严格 ordering 和 search/delete 语义变模糊。本项目明确 reject duplicate。

相同 tier 的 VIP 不等于 duplicate key。应建立 immutable composite key，例如：

```text
VipPriorityKey
├── tierRank
├── arrivalSequence
└── confirmationNumber
```

用 sequence/confirmation number 把 key 变成完全唯一。VIP 的最高 priority 在最右节点，不是 AVL root；root 通常接近中位数。priority 改变时要 delete old key，再 insert new key，不要直接修改已经参与 `compareTo()` 的字段。

8 位 confirmation number 建议用 String，避免开头 `0` 消失。

## 12. 四人怎样并行而不互相踩文件

### 先冻结 shared contract

全队先确认：

- API 名称、返回语义。
- duplicate/null policy。
- height convention。
- successor deletion policy。
- 每个 module 的 primary key 与 add-on method owner。

### 每人一个获批 module

不要采用“一个人做 Walk-In + Front Desk、另一个人做 VIP + Housekeeping”的旧分工。Specification 要每人一个不重复 module。

建议每位成员拥有自己的文件（module 获批后才建）：

```text
src/entity/<ModuleEntity>.java
src/boundary/<Module>UI.java
src/control/<Module>Control.java
```

共享文件（修改前先全队沟通）：

```text
SearchTreeInterface.java
AVLTree.java
Visitor.java
之后的 MainMenuControl.java 与共用 entity
```

共享文件修改前先沟通；一个 add-on method 要一起更新 interface、implementation、test 和 ADT specification，不能只改一处。

### 各 module 与 AVL/Linear ADT 的合理关系

| Module | 题面必须体现 | AVL 合理用途 / add-on 候选 |
|---|---|---|
| Walk-In | Linear chronology，通常 Queue | confirmation number 辅助 index；range traversal |
| VIP | Non-Linear priority | composite key -> Booking；get/remove largest |
| Housekeeping | Linear rollback，通常 Stack | room number -> current Room；replace current value |
| Front Desk | Non-Linear + search | 8-digit confirmation number -> Booking；range/prefix query |
| Loyalty | member/points/tier logic | member ID -> Member；ranking index 或 filtered traversal |

Q&A 还说每位成员应调用团队 ADT，并贡献 modified/add-on operation。不要让一个 ADT Lead 写完所有 graded AVL，其他三人完全没 contribution；但也不要四个人各复制一棵 AVL。正确做法是共同一份实现、每个 add-on 有清楚 owner 和 tests。

## 13. Report 不能只靠 inorder

Inorder 是 AVL traversal，不等于题目要求的 explicit sorting algorithm。每份管理报告应有：

```text
1. AVL search/range/traversal 找候选资料
2. 至少多个 criteria filter
3. 放进普通 array 或课程自写 List ADT
4. 调用自己实现的 Merge Sort / Quick Sort / 其他合适算法
5. Control 组成 structured report string
6. Boundary 显示
```

若做 `SortUtil`，它必须只有 static members，可接受 Q&A 允许的 `Comparator<T>`，但不可调用 `Collections.sort()`、`Arrays.sort()` 或 Java List。

### 正式报告包装也要照 template

- 使用 tutor/Google Classroom 提供的正式 Google Doc template。
- Part A 放团队 ADT specification、完整 interface 与完整 implementation。`Visitor` 是当前 public API dependency，应一起附上或先向 tutor 确认放置方式。
- Part B 每人只放自己 module 的 Control class；黄色 highlight ADT object declaration、creation 与 method invocation，并附清楚标注的核心 input/output screenshots。
- 补齐学生资料、declaration、签名与 Group Contract Appendix。
- 由组长提交 Google Classroom，并遵守 tutor 最新的 file naming 要求。

## 14. 怎样增加一个 AVL operation

例：VIP owner 要加 `removeLargest()`：

1. 先写通用 ADT specification，不得出现 VIP/Booking 字眼。
2. 在 `SearchTreeInterface` 加 generic signature。
3. 在 `AVLTree` 写 public wrapper + private helper，复用 `rebalance()`。
4. 明确 empty/return/size 语义。
5. 增加 normal、empty、连续删除和 deletion rebalance tests。
6. Control 字段仍然保持 interface 类型。
7. 全队同步 contract version，再 merge。

不要为了一个 module 把 `allocateVipRoom()`、`searchMember()` 之类业务 method 塞进 ADT。

## 15. Run / Test

```bash
cd <project-path>/assignment

./scripts/test.sh
./scripts/run.sh
```

`test.sh` 跑 19 个 AVL regression/invariant tests——这是基础架构阶段的
验证方式。`run.sh` 目前只印 placeholder 提示；等 module 整合后才有主菜单。

测试覆盖：

- empty tree。
- insertion LL/RR/LR/RL。
- duplicate + replace。
- search/contains/min/max。
- pre/in/post traversal。
- leaf、one-child、two-child、root deletion。
- deletion LL/RR/LR/RL。
- clear 后旧 Node 不可搜索并能 reuse。
- null contract。
- 1..200 sequential insert，再 odd/even 全删除；每一步验证完整 invariant。
- fixed-seed mixed insert/delete/replace/search/contains，并与 array reference model 对照。

## 16. 当前仍不是 final assignment 的部分

- 现在只有 AVL 基础架构（adt/ 三个文件 + tests）；boundary/control/entity/dao/utility
  都还是空 package，等团队定好 module 分工与各 module 的辅助方案后才建 class。
- 还没有任何 module、report（searching + 自写 sorting + multi-criteria filter）、
  SortUtil 或 seed data；这些是每人 graded 的核心，必须由 module owner 独立完成。
- `TODO author` 必须换成真实姓名。
- key-value AVL 的 ADT 名称/规格必须在正式 module work 前跟 tutor 确认。
- 当前 Codex reference 不可直接成为 graded AVL core；先确认 Yellow AI 许可范围，并由学生独立完成核心过程。
- Team Registration 原 deadline 是 2026-06-26 23:59；若尚未处理，应立即问 tutor。
- Final PDF + NetBeans project + 实际使用的数据文件（若有）+ ReadMe + AI form deadline 是 2026-08-21 23:59。
- Week 11-12 每位成员要独立 demo 并解释自己的代码。

这份基础的目标是把 shared contract、AVL invariant、ECB 边界和运行方式定稳；下一阶段才按 tutor 已批准的 member/module allocation 建正式业务层。

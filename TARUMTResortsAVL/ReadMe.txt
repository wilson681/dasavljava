TAR UMT Resorts - AVL Foundation
=================================

CURRENT PURPOSE
---------------
这是团队选择的 AVL Search Tree（generic key-value 版本）基础架构与测试。
全队全局都用 AVL 当 main ADT；每个 module 的辅助方案还没定，所以
boundary/control/entity/dao/utility 目前只是空 package（保留 ECB 结构）。
本基础是 AI 生成的教学/概念验证 reference，不是可直接提交的 graded AVL core。

BLOCKING DECISION BEFORE MODULE WORK
------------------------------------
任何正式 ModuleControl 依赖这个 K,V API 前，团队必须先让 tutor 确认：
1. Team ADT 名称是 generic key-value AVL Search Tree；资料是 unique (K,V) pairs。
2. 登记名称、ADT specification、interface 和 implementation 使用同一口径。
3. 若 tutor 只接受课程的单 entry AVL/BST，先重构 API，之后才写业务层。
4. 先确认 Yellow AI 允许的辅助范围；graded AVL core 必须由学生独立完成、
   能解释，并如实填写 AI Usage Disclosure。

REQUIREMENTS
------------
- Working JDK 14+ approved by the tutor（NetBeans project 与 scripts 统一产生
  Java 14 bytecode）
- UTF-8 terminal/source encoding
- NetBeans can open this folder directly as TARUMTResortsAVL

MAC / LINUX - QUICK RUN
-----------------------
在这个 assignment 目录执行：

  ./scripts/test.sh
  ./scripts/run.sh

第一个命令跑 19 个 AVL regression/invariant tests，这是基础架构的验证方式。
第二个命令目前只印 placeholder 提示；等 module 整合后才有主菜单。

MANUAL TERMINAL RUN
-------------------
  rm -rf build/classes
  mkdir -p build/classes
  javac --release 14 -encoding UTF-8 -d build/classes $(find src -name "*.java")
  java -cp build/classes main.Main

NETBEANS RUN
------------
1. File > Open Project，选择整个 assignment folder。
2. 确认 Project Properties > Libraries 使用 tutor 认可的 JDK。
3. Main Class 应为 main.Main。
4. 测试用 ./scripts/test.sh 最直接。

FOLDER RESPONSIBILITIES
-----------------------
- src/adt      : SearchTreeInterface + AVLTree + Visitor（团队 graded ADT）
- src/entity   : 之后放纯资料 POJO（Booking/Room/Member...）
- src/boundary : 之后放 Scanner、菜单、输入输出
- src/control  : 之后放各 module 的 use-case flow + composition root
- src/dao      : 真的需要 seed data / file persistence 才建 class
- src/utility  : 之后放共享 static helper（例如自写 sort）
- src/main     : 只启动顶层 Control（现在是 placeholder）
- test/adt     : 不依赖 JUnit 的 AVL regression tests
- data         : 程序实际使用的数据文件（若有，才须随 final project 提交）

IMPORTANT TEAM RULES
--------------------
- Control 字段声明 SearchTreeInterface<K,V>；Boundary/Entity 永远接触不到 Node。
- 整合时在唯一 composition root 建共享 tree 并 constructor 注入；
  不要 static global，也不要每个 Control 自己 new 同类 tree。
- duplicate key 拒绝；更新现有 value 用 replace()；null key/value 禁止。
- key 或参与 compareTo() 的字段入树后不可修改；要变就 delete 再 insert。
- priority 同分用 composite key + sequence/confirmation number 保证唯一。
- 最高 priority = getLargestKey()（最右节点），不是 AVL root。
- 8 位 confirmation number 用 String，防前导 0 消失。
- 题面要求整体系统体现 Linear + Non-Linear ADT 与明确 searching/sorting；
  选 AVL 当 main ADT 不代表全系统只准出现 AVL，辅助方案由团队之后决定。
- Java Collections Framework collection classes 禁用；report sorting 必须自写。

BEFORE FINAL SUBMISSION
-----------------------
- 把每个 Java class 的 TODO author 替换为真正作者姓名。
- 每位成员完成自己获批 module 的核心功能 + 至少两份
  searching + self-written sorting + multi-criteria filter 的 report。
- 使用正式 Google Doc template；Part A 放 ADT specification、完整 interface 与
  implementation（Visitor 是 public dependency，一起附上或先问 tutor）。
- Part B 每人只放自己的 Control class，黄色 highlight ADT 声明/创建/调用，
  附核心 input/output screenshots。
- 补齐学生资料、declaration、签名与 Group Contract Appendix；组长依 tutor
  命名规范提交 Google Classroom，并附 AI Usage Disclosure Form。
- 不得把本 reference 改作者名后提交；学生须独立完成核心、保留 implementation
  过程、能在 Week 11-12 demo 解释自己的代码。

完整设计与协作说明：docs/AVL_ARCHITECTURE_CN.md

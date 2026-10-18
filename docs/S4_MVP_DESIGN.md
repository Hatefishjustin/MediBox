# S4 · OCR 确认 + 药品详情 + 临期 + 盘点（MVP 真机验证）设计

> 本文是 MediBox 的 **S4 编码前设计文档**。只做设计，不做实现。  
> 设计目标：把 S3 的 OCR 候选结果，安全、可编辑、可确认地落到 S1 数据模型里；补齐药品详情、临期、盘点的 MVP 真机闭环。  
> 设计依据：`DESIGN_SYSTEM.md`、`UI_GUIDELINES.md`、`UI_REVIEW.md`、`S1_ENTITY_DESIGN.md`、`S2_UI_DESIGN.md`、`S3_OCR_DESIGN.md`、当前实际代码。

---

## 1. S4 设计目标

S4 不是“功能堆叠”，而是把 MediBox 从“能扫描”推进到“能真正管理药箱”。

S4 的完整闭环：

1. 扫描药品
2. OCR 产出候选
3. 用户确认 / 编辑
4. 保存 Medicine
5. 查看药品详情
6. 执行库存操作
7. 查看临期药品
8. 完成盘点
9. 真机验证

S4 的核心判断：

- OCR 只是候选，不是事实。
- 保存前必须由用户确认。
- 详情页是药品管理页，不是医疗说明书页。
- 临期判断必须沿用 Domain 层。
- 盘点必须通过 Repository 事务完成，不能直接改库存。

---

## 2. 当前代码现状审查

### 2.1 已确认的基础能力

当前 S1/S2/S3 的实际代码已经提供了这些基础：

- `Medicine`、`Location`、`StockRecord` 三表模型已存在。
- `ApproxDate` 已支持 `YEAR_MONTH` / `FULL_DATE` 精度安全存储。
- `StockChangeType` 已定义库存方向语义。
- `MedicineRepository` 已具备库存事务方法：`purchaseStock`、`addStock`、`useStock`、`discardStock`、`decreaseStock`、`adjustStock`。
- `LocationRepository` 已具备默认药箱幂等创建和删除保护。
- `ExpiryChecker` 已集中处理 `EXPIRED`、`EXPIRING_SOON`、`NORMAL`、`UNKNOWN`。
- `HomeScreen`、`MedicineListScreen` 已按非 Dashboard 的工具型方式实现。
- `ScanScreen`、`OcrConfirmScreen` 目前仍是占位态，适合作为 S4 继续接手的入口。
- `DetailScreen`、`ExpiringScreen`、`InventoryScreen` 目前只是空壳页面，S4 正好补齐。

### 2.2 现有实现中的关键边界

1. `ApproxDate` 已经是精度安全的，不应在 UI 或 ViewModel 中把 `YYYY-MM` 偷补成 `YYYY-MM-01`。
2. `ExpiryChecker` 已经是唯一应该负责有效期判断的地方。
3. `Medicine.stock` 是当前库存唯一真值，`StockRecord.balance` 只是历史快照。
4. `MedicineRepository.adjustStock()` 已经适合盘点的最终应用动作。
5. `deleteMedicine()` 已经负责：
   - 删除药品行
   - 依赖外键级联删除 `StockRecord`
   - 删除照片文件
6. `locationId` 允许为空，所以 S4 不需要强制新建位置结构。
7. 当前导航里已经预留了 `DETAIL`、`EXPIRING`、`INVENTORY`、`OCR_CONFIRM` 路由，因此 S4 主要是内容补齐，不是重构导航。

### 2.3 现有占位页结论

- `ScanScreen`：还只是设计态，S4 不接管相机实现。
- `OcrConfirmScreen`：当前只是手工表单壳子，S4 要把它变成 OCR 候选确认页。
- `DetailScreen`：当前只是空态占位，S4 要做成真实详情页。
- `ExpiringScreen`：当前只是空态占位，S4 要做成临期 / 过期列表页。
- `InventoryScreen`：当前只是空态占位，S4 要做成盘点页。

### 2.4 结论

S4 **不需要新增数据库表**，也**不需要新增 DAO**。现有三表模型足够支撑本阶段 MVP。

原因：

- OCR 确认只是把候选值写入 `Medicine`。
- 详情页与库存操作都能由 `Medicine` + `StockRecord` 表达。
- 临期页只需要 `Medicine.expiryDate` 和 `ExpiryChecker`。
- 盘点只需要读取 `Medicine.stock` 并通过 `adjustStock()` 事务写回。

---

## 3. OCR 确认流程

### 3.1 设计原则

- OCR 结果永远是候选值。
- 用户必须逐字段确认。
- OCR 未识别出的字段必须允许手动填写。
- 用户可以返回扫描页重新识别。
- 用户也可以直接走完整手动录入路径。
- 保存成功后才进入详情或返回列表。

### 3.2 字段边界

#### 事实字段，S4 可确认 / 保存

- `name`
- `specification`
- `manufacturer`
- `batchNo`
- `productionDate`
- `expiryDate`
- `locationId`（保存时选择）
- `photoPath`（后续补图 / 现阶段保留接口）

#### 不能由 OCR 自动填充

- `usageNote`
- `contraindicationNote`
- `precautionNote`
- `userNote`

这些字段属于 S6 范畴，S4 不自动生成，也不从 OCR 解析进去。

### 3.3 OCR → 用户确认 → Medicine

流程必须是：

`OCR Candidate -> 用户确认 / 编辑 -> Medicine`

禁止：

`OCR -> Medicine`

任何自动识别字段都需要在确认页接受用户最终决定。

### 3.4 保存前校验

保存前至少校验：

- `name` 非空
- `stock` 默认至少为 1，或由用户在后续页调整
- `productionDate` / `expiryDate` 的格式必须可解析为 `ApproxDate`
- `expiryDate` 不能早于常识边界的非法输入值
- `locationId` 如为空，允许落到默认药箱或保留为空，按产品最终决策执行

S4 不应在确认页上做复杂医疗判断，只做数据完整性校验。

### 3.5 保存成功反馈

- 成功后短暂展示成功状态。
- 随后返回详情页或药箱列表。
- 成功文案只说“已保存到药箱”，不说“识别准确率高”。

### 3.6 保存失败状态

- 网络不存在，不需要网络错误。
- 失败只可能来自本地校验、存储失败、数据异常或事务失败。
- 必须提供可读错误信息和重新编辑入口。

### 3.7 返回扫描重新识别

如果用户发现 OCR 候选不可靠，应能直接返回扫描页。

### 3.8 手动录入完整路径

S4 需要保证：

- OCR 失败
- OCR 结果过少
- 用户主动放弃 OCR

这三种情况都能进入同一套手动补全流程。

### 3.9 OcrConfirmScreen 设计

#### 页面目标

让用户在最短时间内确认一条药品记录，而不是被长表单压住。

#### 信息层级

1. 顶部说明：`确认药品信息`
2. OCR 候选区 / 编辑区
3. 保存按钮
4. 返回扫描 / 手动录入

#### 字段顺序

1. 药品名称
2. 规格
3. 生产厂家
4. 批号
5. 生产日期
6. 有效期
7. 位置
8. 照片入口（后续）

#### UI 规则

- 名称最突出。
- 已识别字段预填。
- 未识别字段留空。
- 每个字段都可直接改。
- 不做多列排版。
- 不在一个屏幕堆过多说明。

#### 状态

- Loading：候选准备中。
- Empty：无候选，直接空白表单。
- Editing：用户手动修改。
- Saving：保存中。
- Error：保存失败。
- Success：保存成功。

#### 操作

- `保存`
- `返回扫描`
- `手动录入`

### 3.10 日期精度要求

OCR 输入与保存必须保留精度：

- `2028-03` -> `ApproxDate.YEAR_MONTH`
- `2028-03-15` -> `ApproxDate.FULL_DATE`

绝不补成 `2028-03-01`。

---

## 4. MedicineDetailScreen UI

### 4.1 页面目标

详情页要回答四件事：

- 这是什么药
- 还有多少
- 放在哪
- 接下来能做什么

它不是说明书页，也不是医疗建议页。

### 4.2 信息层级

按优先级设计为四层：

#### 第一层：核心身份

- 药品名称
- 规格
- 状态（临期 / 过期 / 正常 / 未知）

#### 第二层：当前状态

- 有效期
- 库存
- 药箱位置

#### 第三层：客观来源信息

- 生产厂家
- 批号
- 生产日期

#### 第四层：用户自己保存的信息

- usageNote
- contraindicationNote
- precautionNote
- userNote

### 4.3 空字段展示

空字段不要显示成空表格格子，应使用更轻的方式：

- 没有时隐藏该行，或
- 显示“未填写”但不占太多视觉权重

### 4.4 页面操作

必须明确保留：

- 编辑
- 库存操作
- 删除
- 返回列表
- 查看/修改位置
- 查看照片

### 4.5 不能出现的内容

详情页禁止：

- 药品功效判断
- 疾病判断
- 用药推荐
- 剂量计算
- 药物相互作用判断
- “建议服用”
- “不建议服用”
- “适合某人”

如果展示用户自己保存的说明信息，必须明确标注这是“用户保存的信息”，不是 App 生成的结论。

### 4.6 删除流程

删除入口必须清晰，但不能误触。

要求：

- 进入删除前有确认对话框
- 明确提示：删除药品后，库存流水会一起删除，照片文件也会被清理
- 删除成功后返回列表
- 删除失败时给出可读错误

### 4.7 库存操作入口

详情页应是库存操作的主入口之一，支持：

- 入库
- 使用
- 丢弃
- 减少
- 调整

对应 Repository：

- `purchaseStock`
- `addStock`
- `useStock`
- `discardStock`
- `decreaseStock`
- `adjustStock`

### 4.8 照片展示

- 有照片：显示药品主图。
- 无照片：显示自然占位。
- 删除 / 替换照片不在 UI 里手工处理文件逻辑，仍由 Repository 负责。

### 4.9 详情页状态

- Loading
- Empty
- Error
- Success
- Editing
- Saving

至少 `Editing / Saving / Error` 必须明确。

---

## 5. ExpiringScreen UI

### 5.1 页面目标

临期页是“需要处理的药品列表”，不是提醒中心，也不是统计中心。

### 5.2 依赖的 Domain 逻辑

必须直接沿用 `ExpiryChecker`：

- `EXPIRED`
- `EXPIRING_SOON`
- `NORMAL`
- `UNKNOWN`

UI 不允许自己比较日期。

### 5.3 页面分组

建议按两组展示：

1. 临期
2. 已过期

`UNKNOWN` 不进主列表，可单独归在“有效期未知”或不重点展示，避免打扰。

### 5.4 YEAR_MONTH / FULL_DATE 处理

- `YEAR_MONTH`：按月末边界计算
- `FULL_DATE`：按具体日期计算

展示时使用 `ExpiryChecker.displayString()`。

### 5.5 排序

建议排序顺序：

1. 已过期，按最久未处理优先
2. 临期，按最接近到期优先
3. 同状态下按有效期更早优先
4. 若有效期相同，再按名称排序

### 5.6 点击去向

点击条目进入药品详情页。

### 5.7 空状态

- 没有临期：`暂时没有临期的药`
- 没有过期：`还没有过期药品`
- 不要用焦虑式语气

### 5.8 与 HomeScreen 的一致性

HomeScreen 的“需要关注”必须与 ExpiringScreen 的判断保持一致，避免首页和临期页说法冲突。

### 5.9 状态

- Loading
- Empty
- Error
- Success

---

## 6. StocktakeScreen 盘点设计

### 6.1 盘点目标

盘点不是建仓储系统，而是让用户快速核对“系统库存”和“实际库存”的差异。

### 6.2 基本概念

- 系统库存：`Medicine.stock`
- 实际盘点：用户输入的真实数量
- 差异：`actualStock - systemStock`

### 6.3 页面结构

#### 顶部

- 标题：`盘点`
- 可显示进度：`已调整 X / 总数 Y`

#### 主区

- 药品名称
- 规格
- 当前库存
- 药箱位置
- 实际库存输入框 / 步进器
- 差异提示

#### 底部

- `完成盘点`

### 6.4 盘点操作

用户逐条输入或调整实际库存。

必须区分：

- 记录库存
- 实际盘点库存

示例：

- 系统库存 12
- 实际盘点 9
- 差异 -3

### 6.5 盘点确认

确认后不能只是 `stock = actualStock`。

必须通过 Repository 事务写入：

- `adjustStock(medicineId, actualStock)`
- 生成 `StockChangeType.ADJUST`
- 同步更新 `Medicine.stock`
- 同步写入 `StockRecord.balance`

### 6.6 盘点异常

必须处理：

- 空药品列表
- 用户未修改任何数量
- 输入负数
- 输入非法字符
- 数量过大
- 盘点中退出
- 确认失败
- 部分保存失败

### 6.7 盘点交互建议

- 输入框要支持数字键盘。
- 变更值要即时显示差异。
- 未改动的行不要高亮。
- 如果整个列表没有变动，完成按钮应弱化或提示“没有变更”。

### 6.8 盘点状态

- Loading
- Empty
- Editing
- Saving
- Error
- Success

### 6.9 盘点结果

完成后给一条非常短的确认：

- `本次盘点已完成`

不要做成庆祝页。

---

## 7. 库存操作流程

### 7.1 支持的操作

S4 允许在详情页直接提供：

- 入库
- 使用
- 丢弃
- 减少
- 调整

### 7.2 与 StockChangeType 的映射

| UI 操作   | Repository      | StockChangeType |
| ------- | --------------- | --------------- |
| 入库      | `purchaseStock` | `PURCHASE`      |
| 补货 / 增加 | `addStock`      | `INCREASE`      |
| 使用      | `useStock`      | `USE`           |
| 丢弃      | `discardStock`  | `DISCARD`       |
| 减少      | `decreaseStock` | `DECREASE`      |
| 盘点调整    | `adjustStock`   | `ADJUST`        |

### 7.3 规则

- `amount` 必须为正整数。
- 库存不能为负。
- 所有库存变化必须走事务。
- UI 不直接碰 DAO。

### 7.4 保存失败反馈

库存失败时：

- 保留用户输入
- 明确提示失败原因
- 不悄悄回滚 UI 到初始状态

---

## 8. 删除流程

### 8.1 删除入口

删除入口放在详情页显著但不突兀的位置。

### 8.2 删除确认

确认文案必须说明后果：

- 药品会被删除
- 对应库存流水会级联删除
- 照片文件会被清理
- 操作不可恢复

### 8.3 删除后导航

删除成功后返回列表，避免详情页悬空。

### 8.4 删除失败

- 需要显示错误
- 保留在详情页
- 不丢失上下文

### 8.5 不重复实现文件逻辑

UI 不单独处理照片文件删除，继续交给 `MedicineRepository.deleteMedicine()`。

---

## 9. Location 处理

### 9.1 当前模型结论

现有 `Location` 足以覆盖 S4：

- 默认药箱：`我的药箱`
- 药品可归属某个位置
- `locationId` 可空

### 9.2 S4 的使用方式

- OCR 保存时可选择位置
- 默认值建议为默认药箱
- 详情页显示所属位置
- 允许修改位置

### 9.3 不做的事

S4 不扩展成完整多药箱管理系统，不增加复杂层级，不新增位置分析逻辑。

### 9.4 是否需要新表

不需要。

### 9.5 是否需要 Repository 扩展

如果后续 UI 需要“列出可选位置”，现有 `LocationRepository.observeLocations()` 已够用。

若确认页需要默认位置选择，可在后续编码阶段补一个轻量 ViewModel 读取默认位置，不需要新表。

---

## 10. Repository / DAO 是否需要变化

### 10.1 结论

S4 **不需要新增 Entity**、**不需要新增 DAO**、**不需要数据库 Migration**。

### 10.2 理由

- OCR 确认页只是在写 `Medicine`。
- 详情页只是在读写 `Medicine`、`StockRecord`。
- 临期页只在读 `Medicine.expiryDate`。
- 盘点页只在读 `Medicine.stock` 并调用 `adjustStock()`。
- 位置选择已经可由 `LocationRepository` 支持。

### 10.3 可能需要新增的 Repository / API（如果编码时发现缺口）

当前设计上暂不强制新增，但允许考虑以下轻量补充：

- 获取默认位置
- 列出位置候选
- 读取单药品详情聚合视图
- 盘点时的批量读取辅助方法

这些属于 ViewModel 便利层，**不是新数据模型需求**。

### 10.4 Migration

当前无新增表，因此无 migration 需求。

---

## 11. ViewModel 设计

### 11.1 需要的 ViewModel

- `OcrConfirmViewModel`
- `DetailViewModel`
- `ExpiringViewModel`
- `InventoryViewModel`

### 11.2 OcrConfirmViewModel

职责：

- 接收 S3 候选值
- 合并候选与用户编辑
- 管理表单状态
- 做保存前校验
- 调用保存逻辑

### 11.3 DetailViewModel

职责：

- 读取药品详情
- 读取库存流水
- 读取有效期状态
- 处理编辑 / 删除 / 库存操作入口状态

### 11.4 ExpiringViewModel

职责：

- 从 `Medicine` 列表计算 `ExpiryStatus`
- 生成临期 / 过期分组
- 保持与 HomeScreen 一致

### 11.5 InventoryViewModel

职责：

- 读取待盘点药品列表
- 追踪用户输入的实际库存
- 计算差异
- 批量确认时按条调用 `adjustStock()`

### 11.6 状态模型

每个 ViewModel 都要明确：

- Loading
- Empty
- Editing
- Saving
- Success
- Error

---

## 12. Navigation 设计

### 12.1 当前路由现状

当前导航已挂：

- `HOME`
- `MEDICINE_LIST`
- `SCAN`
- `OCR_CONFIRM`
- `DETAIL/{id}`
- `EXPIRING`
- `INVENTORY`
- `PROFILE`

### 12.2 S4 设计中的导航关系

- Home -> Scan
- Scan -> OCR Confirm
- OCR Confirm -> Detail 或返回列表
- Medicine List -> Detail
- Home 的临期条目 -> Medicine List filter
- Expiring -> Detail
- Detail -> Inventory / Edit / Delete
- Inventory -> 完成后返回列表或详情

### 12.3 当前代码状态判断

当前路由结构已够用，S4 不需要推翻重做导航。

### 12.4 可能的小补充

如果要更优雅地传递 OCR 候选数据，S4 编码阶段可以通过：

- ViewModel 临时态
- SavedStateHandle
- 路由参数（仅用于轻量标识）

但设计上不要求新增复杂导航协议。

---

## 13. 状态设计

### 13.1 OcrConfirmScreen

- Loading
- Empty
- Editing
- Saving
- Success
- Error

### 13.2 MedicineDetailScreen

- Loading
- Empty
- Error
- Success
- Editing
- Saving

### 13.3 ExpiringScreen

- Loading
- Empty
- Error
- Success

### 13.4 StocktakeScreen

- Loading
- Empty
- Editing
- Saving
- Error
- Success

### 13.5 统一原则

- 错误状态必须可恢复。
- 成功状态必须短促。
- 空状态必须给行动入口。
- 不能让任何页面“只剩一个 spinner”。

---

## 14. 真机验收计划

S4 的最终目标是 MVP 真机跑通，所以必须单列验收计划。

### 14.1 首次启动

验证：

- 首页可打开
- 默认药箱存在
- 无药品时的空状态自然

### 14.2 OCR

验证：

- 请求相机权限
- 允许后打开相机
- 对准真实药盒
- 识别中文文字
- 进入 OCR 候选
- 手动确认并保存

### 14.3 手动录入

验证：

- OCR 失败后仍能手动创建药品
- 不依赖识别成功才能落库

### 14.4 药品详情

验证：

- 名称
- 规格
- 厂家
- 批号
- 日期
- 库存
- 位置

### 14.5 库存

至少验证：

- 入库
- 使用
- 丢弃
- 调整
- `Medicine.stock` 与 `StockRecord` 一致

### 14.6 临期

准备至少四种数据：

- 正常
- 临期
- 过期
- UNKNOWN
- YEAR_MONTH

验证：

- 显示
- 排序
- 点击跳转

### 14.7 盘点

验证：

- 系统库存 10
- 实际库存 7
- 提交后库存变 7
- 生成 `ADJUST` 流水

### 14.8 删除

验证：

- 删除药品后列表消失
- 详情页退出
- `StockRecord` 级联删除
- 照片文件清理

---

## 15. 反 AI UI Review

### 15.1 S4 需要复检的页面

1. HomeScreen
2. MedicineListScreen
3. ScanScreen
4. OcrConfirmScreen
5. MedicineDetailScreen
6. ExpiringScreen
7. StocktakeScreen

### 15.2 10 个审查问题

1. 这个详情页是不是像 AI 自动生成的后台？
2. 是否真的需要这个 Card？
3. 是否可以删掉一个按钮？
4. OCR 确认是不是信息过载？
5. 用户能不能一眼知道哪个字段需要确认？
6. 临期页面是不是在制造焦虑？
7. 库存操作是不是足够直接？
8. 盘点流程是不是比纸笔还复杂？
9. 是否存在为了展示功能而增加的 UI？
10. 如果隐藏项目名，这是不是一个真实家庭工具？

### 15.3 设计评分

当前设计自评：**9/10**

理由：

- 没有引入医疗建议。
- 没有新增表。
- 详情、临期、盘点都围绕实际操作展开。
- UI 重点是“能确认、能修改、能处理库存”，不是“展示设计感”。
- 仍保持系统字体、发丝线、克制圆角、低装饰密度。

### 15.4 需要警惕的低分点

若编码时出现以下情况，必须回退：

- Card 过多
- 大面积彩色状态块
- 说明文字过长
- 页面像仪表盘
- 详情页过度表格化
- 盘点页像仓库系统

---

## 16. 无障碍与真实使用场景

S4 设计需默认考虑：

- 中文字体可读性
- 触控区域足够大
- 小屏幕适配
- 大字体系统设置
- 深色模式的未来兼容
- TalkBack 基础语义
- 键盘输入
- 数字输入
- 焦点顺序
- 错误提示

原则：

- 无障碍不应把页面变复杂。
- 必须自然融入现有工具型 UI。

---

## 17. S4 / S5 边界

S4 只做：

- OCR 确认
- 药品落库
- 药品详情
- 基础库存操作
- 临期页面
- 基础盘点
- MVP 真机验证

S5 才做：

- WorkManager
- 后台临期提醒
- 库存提醒
- Notification
- 通知渠道
- 提醒规则

S4 不偷渡通知系统。

---

## 18. S4 / S6 边界

S6 才做完整的药品说明信息管理。

S4 可以展示用户自己录入的备注字段，但不能扩展为医疗说明书系统。

禁止：

- 自动生成用药说明
- AI 总结药品
- AI 推荐
- 医疗知识库
- 药物相互作用
- 疾病判断

---

## 19. 风险清单

### 19.1 技术风险

- 盘点批量提交时事务失败的回滚体验。
- 详情页与库存操作的 ViewModel 聚合复杂度。
- OCR 候选和确认页之间状态传递方式。

### 19.2 产品风险

- 详情页如果信息密度过高，会像后台管理工具。
- 临期页如果颜色过重，会制造焦虑。
- 盘点页如果操作过多，会比纸笔更累。

### 19.3 设计风险

- 卡片用多了会破坏当前的工具感。
- 大字号与长药名可能挤压 trailing 区域。
- 详情页如果给用户备注太多视觉权重，会偏离工具定位。

---

## 20. 编码顺序

S4 编码阶段建议按这个顺序推进：

1. OcrConfirmScreen 状态与字段模型
2. 医药详情聚合 ViewModel
3. MedicineDetailScreen
4. ExpiringViewModel + ExpiringScreen
5. InventoryViewModel + StocktakeScreen
6. 删除流程与库存操作入口收口
7. 真机验证与细节修正

这一顺序先保证“确认能落库”，再补“看详情 / 管库存 / 看临期 / 做盘点”。

---

## 21. 测试计划

### 21.1 单元测试

- `ExpiryChecker` 的状态测试
- `ApproxDate` 精度保持测试
- `adjustStock()` 与盘点差异测试
- 删除药品级联删除与照片清理测试

### 21.2 ViewModel 测试

- OCR 候选进入确认页的状态转换
- 盘点差异计算
- 临期分组与排序
- 错误状态恢复

### 21.3 手动验证

- 真机 OCR
- 真机保存
- 真机库存调整
- 真机删除
- 真机临期筛选

---

## 22. S4 设计完成报告

### 当前代码审查结论

- S1 数据层已足够支撑 S4，不需要新增表。
- `ApproxDate`、`ExpiryChecker`、`MedicineRepository.adjustStock()` 已经提供了 S4 必需的领域能力。
- 当前的 `DetailScreen`、`ExpiringScreen`、`InventoryScreen` 只是占位壳，适合由 S4 继续实现。
- `OcrConfirmScreen` 还停留在手工表单态，需要 S4 把它升级为 OCR 候选确认页。

### docs/S4_MVP_DESIGN.md

- 已完成。

### 是否需要新增 Entity

- 不需要。

### 是否需要数据库 Migration

- 不需要。

### 是否需要新增 Repository API

- 设计上不强制需要。
- 如编码时需要更方便的聚合读取，可轻量补充 ViewModel 辅助方法，但不属于模型扩展。

### 需要修改哪些文件

- 设计阶段：不改业务代码。
- 编码阶段预计会改：
  - `OcrConfirmScreen`
  - `DetailScreen`
  - `ExpiringScreen`
  - `InventoryScreen`
  - 新增 `DetailViewModel`、`ExpiringViewModel`、`InventoryViewModel`、`OcrConfirmViewModel`
  - 可能补充少量导航参数处理

### 新增哪些文件

- 设计阶段已新增：`docs/S4_MVP_DESIGN.md`
- 编码阶段预计新增：若干 ViewModel / 状态模型文件

### Navigation 怎么变化

- 结构基本不变。
- S4 只会让已有路由真正有内容。

### 真机测试计划

- 已在第 14 节明确。

### 反 AI Review 得分

- 9/10

### 最终结论

**S4 设计阶段完成，等待项目负责人确认，不进入编码。**

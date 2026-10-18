# S1 · Entity Design（数据库实体设计 · 待确认）

> 本文是 **S1 数据库与数据层** 的前置设计，尚未写任何 Room/DAO 代码。
> 设计原则：**简单 / 可靠 / 可扩展 / 本地优先**，优先满足 MVP（S0–S4），不为「架构完整」提前建表。
> 医疗红线：MediBox 是「家庭药品管理 / 药箱整理工具」，**不**做医疗判断；所有用法用量/禁忌/注意事项均为用户录入，程序绝不生成。

---

## 1. Entity 列表（MVP 建议）

| Entity | 是否纳入 MVP | 说明 |
|---|---|---|
| **Medicine** | ✅ 必须 | 一条记录 = 一盒/一瓶具体药品（含批次维度） |
| **Location** | ✅ 必须 | 药箱 / 存放位置（用户自定义，如「主卧药箱」） |
| **StockRecord** | ✅ 必须 | 库存变更流水（+/−/使用/丢弃/调整），详情页可查 |
| medicine_photos | ❌ 推迟 | MVP 不建独立表；MediBox 用 Medicine 上一个**可空 `photoPath` 列**存主照片路径即可 |
| instructions | ❌ 推迟 | 说明书（图/PDF）属 S6 范畴，MVP 不做 |
| inventory_checks / inventory_check_items | ❌ 推迟 | 盘点任务与明细属 S7，MVP 不需要 |
| categories | ❌ 推迟 | 分类是筛选辅助，非 MVP 核心；S0–S4 流程不依赖，推迟 |

> 对 `PROJECT_ANALYSIS.md §4.2` 的 8 张表做了**大幅精简**：MVP 只落地 3 张表，其余 5 个推到各自所属阶段（照片/说明书→S6，盘点→S7，分类→后续）。

---

## 2. 每个 Entity 的字段

### 2.1 Medicine（药品 / 一盒药）
| 字段 | 类型 | 可空 | 类别 | 说明 |
|---|---|---|---|---|
| `id` | Long | 否（PK autoGenerate） | 系统 | 内部主键 |
| `name` | String | 否 | 事实 | 药品名称（必填） |
| `specification` | String? | 可空 | 事实 | 规格（如「0.25g*20片」） |
| `manufacturer` | String? | 可空 | 事实 | 生产厂家 |
| `batchNo` | String? | 可空 | 事实 | **批号**（支持同药多批次） |
| `productionDate` | ApproxDate? | 可空 | 事实 | 生产日期（精度安全，见 §9） |
| `expiryDate` | ApproxDate? | 可空 | 事实 | 有效期（精度安全，见 §9） |
| `locationId` | Long? | 可空 | 事实 | 外键 → Location.id（未归位可为空） |
| `stock` | Int | 否（默认 1） | 事实 | 当前库存数量（按本条记录/批次计） |
| `photoPath` | String? | 可空 | 事实 | 主照片路径（app 私有目录，仅存路径） |
| `usageNote` | String? | 可空 | **用户备注** | 用法用量（OCR 不填，仅用户手填） |
| `contraindicationNote` | String? | 可空 | **用户备注** | 禁忌（用户手填） |
| `precautionNote` | String? | 可空 | **用户备注** | 注意事项（用户手填） |
| `userNote` | String? | 可空 | **用户备注** | 用户自由备注 |
| `createdAt` | Long | 否 | 系统 | 录入时间（epoch millis，排序用） |
| `updatedAt` | Long | 否 | 系统 | 最后修改时间（epoch millis） |

### 2.2 Location（药箱 / 位置）
| 字段 | 类型 | 可空 | 说明 |
|---|---|---|---|
| `id` | Long | 否（PK） | 主键 |
| `name` | String | 否 | 位置名（如「客厅抽屉」「主卧药箱」） |
| `isDefault` | Boolean | 否（默认 false） | 是否默认位置 |
| `sortOrder` | Int | 否（默认 0） | 排序权重 |
| `createdAt` | Long | 否 | 创建时间（epoch millis） |

### 2.3 StockRecord（库存变更流水）
| 字段 | 类型 | 可空 | 说明 |
|---|---|---|---|
| `id` | Long | 否（PK） | 主键 |
| `medicineId` | Long | 否 | 外键 → Medicine.id（NOT NULL） |
| `changeType` | String | 否 | 变更类型：`INCREASE`/`DECREASE`/`USE`/`DISCARD`/`ADJUST`/`PURCHASE` |
| `amount` | Int | 否 | 变更量（配合 changeType 理解正负） |
| `balance` | Int | 否 | 变更后该 Medicine 的库存余额 |
| `note` | String? | 可空 | 备注（如「吃了 1 片」） |
| `createdAt` | Long | 否 | 变更时间（epoch millis） |

---

## 3. 主键
- `Medicine.id`、`Location.id`、`StockRecord.id`：均为 `Long`，`autoGenerate = true`。

## 4. 外键
- `Medicine.locationId` → `Location.id`（可空，ON DELETE SET NULL）。
- `StockRecord.medicineId` → `Medicine.id`（NOT NULL，ON DELETE CASCADE）。

## 5. 索引
- `medicines(name)` —— 搜索/筛选/「发现已有药品」匹配。
- `medicines(expiryDate)` —— 临期/过期查询（`expiryDate` 存零填充规范串，见 §9，字典序即时间序，可直接范围查询）。
- `stock_records(medicineId)` —— 按药品查流水。
- 可选：`medicines(locationId)`。

## 6. Entity 关系（ER）
```
Location 1 ──< Medicine (0..1 归属) >── 1 StockRecord
            （locationId 可空）        （medicineId 非空）
```
- 一个 Location 拥有多件 Medicine；一件 Medicine 最多归属一个 Location（也可不归属）。
- 一件 Medicine 拥有多条 StockRecord（库存流水）；StockRecord 必属于某 Medicine。
- 照片/说明书/盘点：MVP 阶段不参与关系图（推迟）。

## 7. 为什么需要这个 Entity（必要性）
- **Medicine**：核心领域对象，没有它就没有「药」。一条记录对应家庭用户心里的一盒药，最直观。
- **Location**：MediBox 的本质是「药箱整理」，药品必须能归属到某个药箱/位置，否则「整理」无意义。
- **StockRecord**：家庭场景虽简单，但「上次少了 2 片」「这盒啥时候开的」需要一条轻量流水来回答；也是详情页「库存记录」的数据源。非会计级台账，只是个人回忆辅助。

## 8. 哪些字段允许为空（及为何）
- Medicine：`specification / manufacturer / batchNo / productionDate / expiryDate / locationId / photoPath / usageNote / contraindicationNote / precautionNote / userNote` 均可空。
  - 理由：OCR 不可靠字段留空由用户补；药品可能无批号、无明确有效期、未归位；医疗备注类默认不存（用户不填即空），**绝不程序生成**。
  - 仅 `name` 必填（没有名字无法识别药品）。
- Location：除系统字段外均必填（`name` 必须有）。
- StockRecord：`note` 可空，其余必填（流水必须知道「哪条药、什么类型、变多少、余额多少、何时」）。

## 9. 日期存储方式（核心：解决 `2028-03` 精度）
**对 `PROJECT_ANALYSIS.md §4.3`「日期存 epoch millis」的明确偏离**：epoch millis 会强制落到一个具体「日」，与「有效期可能只有年-月」冲突，且容易把 `2028-03` 偷偷变成 `2028-03-01` 冒充精确日——**违反用户要求**。

**采用精度安全的 `ApproxDate` 自定义类型**（通过 Room `TypeConverter` 以 TEXT 持久化）：
- 结构（示意，非实现）：`{ precision: YEAR_MONTH | FULL_DATE, year: Int, month: Int?, day: Int? }`
- **持久化规范串**：统一零填充为 `YYYY-MM`（年-月）或 `YYYY-MM-DD`（完整日）。
  - `2028-03` → 存 `"2028-03"`
  - `2028-03-15` → 存 `"2028-03-15"`
- **显示**：直接回显规范串，保持用户录入精度；**绝不**补 `-01` 当日。
- **查询/排序**：因零填充，`"2028-03"` < `"2028-03-15"` < `"2028-04"`，字典序即时间序，可安全做临期范围筛选（S5 再细化年-月精度的边界策略，如按月末计）。
- 转换示意：`ApproxDate` ↔ `String`，Converter 负责解析/格式化；Medicine 中 `productionDate`/`expiryDate` 类型为 `ApproxDate?`。

> `createdAt`/`updatedAt` 仍是 epoch millis——它们是系统元数据（用于排序），不是展示给用户的药品日期，不受此精度约束。

## 10. 删除策略
- **删除 Medicine**：`ON DELETE CASCADE` 同时删除其 StockRecord 流水（家庭个人数据，无审计留存硬性要求）；Repository 层额外删除 `photoPath` 指向的私有目录照片文件（DB 只删行）。
- **删除 Location**：若该 Location 仍关联 Medicine，**阻止删除**（UI 提示先移走/重新归属药品），比级联置空更安全、更符合家庭心智；确认无关联后再删。
- **删除 StockRecord**：流水不向用户开放单条删除，仅随 Medicine 级联清理。
- **软删除**：MVP 不需要；本地优先个人数据用硬删除即可。

---

## 为什么这结构适合「家庭药箱」而不是「企业仓库」
- **一盒药 = 一条 Medicine 记录**（批号/有效期直接挂在这条记录上）。家庭用户心智就是「这盒布洛芬 2028-03 到期、那盒 2029-01 到期」→ 两条记录，直觉清晰。
- **对比方案「Product + Batch 双表」**：企业仓库常用（产品主数据 + 批次库存），但对家庭过度——用户要理解「先选产品再选批次」两层结构，复杂度无收益。MVP 拒绝该方案。
- **无 SKU / 供应商 / 多仓 / 序列号追踪**：家庭不需要。
- **Location = 用户随手命名的药箱/抽屉**，不是仓库分区。
- **stock = 每条记录一个简单的整数**，不是库位级/序列级库存。
- **StockRecord 是轻量个人流水**，不是财务台账。
- 结论：3 张表、扁平批次模型，正好覆盖「看得清、找得到、不会过期浪费」，且为后续阶段（照片/说明书/盘点/分类）预留了清晰的可扩展点（加表即可，不动核心）。

---

## 需要你拍板的开放问题
1. **照片**：MVP 是否在 Medicine 上保留一个可空 `photoPath` 列（扫描时存主照片）？还是完全推迟到 S6？（建议：保留轻量列，扫描体验更完整。）
2. **默认位置**：首次启动是否播种一个默认 Location（如「我的药箱」）？还是从空开始让用户自建？（建议：播种 1 个默认位置，降低首屏门槛。）
3. **同药多批次 UX**：扫描到同名药品时，是否维持「发现已有药品 → +1 盒 / 重新录入」的两条路径？（建议：维持，重新录入即生成新批次记录。）

> 确认以上设计（含开放问题答复）后，再开始 S1 的 Room Entity / DAO / Database / Repository / TypeConverter / 测试编码。

# S2 · 首页 + 药箱列表 UI 设计（设计先行 · 待确认）

> 本文是 **S2 编码前** 的 UI 设计文档，覆盖 HomeScreen、MedicineListScreen、搜索、有效期逻辑、全部状态。
> **在本文通过反 AI 10 题自测（≥8/10）并获得用户确认前，不开始编码。**
> 设计依据：`DESIGN_SYSTEM.md`（Token/组件）、`UI_GUIDELINES.md`（纪律）、`UI_REVIEW.md`（逐页决策）、`S1_ENTITY_DESIGN.md`（数据模型）。

---

## 0. 设计意图（一句话）

> 打开 App 2 秒内知道「有没有需要关注的药」，然后一步进入药箱列表查找。

首页是**药箱入口**，不是数据看板。列表是**查找工具**，不是卡片墙。

---

## 1. 反 AI 审美原则（S2 比 S0 更严格）

| 原则 | S2 具体要求 |
|------|------------|
| 不要 Dashboard | 首页**不出现** `[总药品 23] [临期 4] [缺货 2]` 三宫格统计卡 |
| 不要卡片墙 | 列表用**行 + 发丝分隔线**，不用 Card 包裹每行 |
| 不要为显示而显示 | 首页只显示**需要关注**的信息；"共 12 种药"是一行安静的文字，不是数字宫格 |
| 高级感 = 减法 | 能删的元素就删；每个元素必须通过质询「它帮助理解或操作了吗？」 |
| 状态色只用于状态 | 过期=danger，临期=warning，正常=中性 inkSecondary；不铺满绿色 |
| 系统中文字体 | `FontFamily.Default`，不加载自定义字体；中文走设备默认 |
| 暖纸背景 | 全页 `paper` (#FAF7F2)，局部 `surface`/`surfaceVariant` |
| 薄分隔线 | 行间 `divider` (#E8E2D8) 1dp 发丝线 |
| 克制圆角 | 行/容器 `radiusMd` (12dp)，标签 `radiusSm` (8dp)，筛选 pill 例外 |
| 几乎无动画 | 进入 fadeIn+slideUp(16dp) ≤200ms；状态切换 crossfade；无弹跳/粒子/发光 |

**绝对禁用**：AI 紫、紫蓝渐变、玻璃拟态、发光、霓虹、Emoji 当图标、巨型圆角、万物 Card、处处阴影、Dashboard 模板、Material Demo 感、默认 M3 紫、弹跳/粒子/循环动画、"智能/AI/赋能/平台"等文案。

---

## 2. 有效期逻辑（集中至 Domain 层 · UI 不硬编码）

> 用户明确要求：有效期判断逻辑集中到 Domain/Repository，UI 只消费结果。
> `ApproxDate` 可能只有 `2028-03`（年月精度）——**绝不**显示成 `2028-03-01`。

### 2.1 新增文件：`domain/ExpiryChecker.kt`

```kotlin
package com.medibox.domain

import java.time.LocalDate
import java.time.YearMonth

enum class ExpiryStatus { EXPIRED, EXPIRING_SOON, NORMAL, UNKNOWN }

/**
 * 有效期判断（集中至 Domain，UI 不硬编码）。
 *
 * 精度安全比较规则：
 * - FULL_DATE "2028-03-15" → 与当天直接比较。
 * - YEAR_MONTH "2028-03"   → 当月最后一天为边界（月末才过期）。
 *   即 3 月 1 日时 "2028-03" 尚未过期；4 月 1 日起才算过期。
 *   绝不把 "2028-03" 当作 "2028-03-01" 处理。
 *
 * @param expiringSoonDays 临期阈值天数（默认 30 天）
 */
object ExpiryChecker {

    const val DEFAULT_SOON_DAYS = 30

    fun status(
        expiryDate: ApproxDate?,
        now: LocalDate = LocalDate.now(),
        soonDays: Int = DEFAULT_SOON_DAYS
    ): ExpiryStatus {
        if (expiryDate == null) return ExpiryStatus.UNKNOWN
        val boundary = expiryBoundary(expiryDate) // 月末 or 当天
        return when {
            now.isAfter(boundary) -> ExpiryStatus.EXPIRED
            now.plusDays(soonDays.toLong()).isAfter(boundary) -> ExpiryStatus.EXPIRING_SOON
            else -> ExpiryStatus.NORMAL
        }
    }

    /**
     * 距过期剩余天数（可能为负 = 已过期）。
     * YEAR_MONTH 按月末计算；返回 Long 避免溢出。
     */
    fun daysUntilExpiry(
        expiryDate: ApproxDate?,
        now: LocalDate = LocalDate.now()
    ): Long? {
        if (expiryDate == null) return null
        val boundary = expiryBoundary(expiryDate)
        return java.time.temporal.ChronoUnit.DAYS.between(now, boundary)
    }

    /**
     * 展示串（保持精度，绝不补 -01）。
     * - YEAR_MONTH  → "2028年3月"
     * - FULL_DATE   → "2028年3月15日"
     */
    fun displayString(expiryDate: ApproxDate?): String? {
        if (expiryDate == null) return null
        return when (expiryDate.precision) {
            ApproxDate.Precision.YEAR_MONTH ->
                "${expiryDate.year}年${expiryDate.month}月"
            ApproxDate.Precision.FULL_DATE ->
                "${expiryDate.year}年${expiryDate.month}月${expiryDate.day}日"
        }
    }

    /**
     * 临期展示文案（如 "还有 15 天" / "已过期 3 天" / "本月到期"）。
     */
    fun relativeText(
        expiryDate: ApproxDate?,
        now: LocalDate = LocalDate.now()
    ): String? {
        val days = daysUntilExpiry(expiryDate, now) ?: return null
        return when {
            days < 0 -> "已过期 ${-days} 天"
            days == 0L -> "今天到期"
            days <= 30 -> "还有 $days 天"
            else -> null // 不显示
        }
    }

    // ---- 内部 ----

    private fun expiryBoundary(date: ApproxDate): LocalDate = when (date.precision) {
        ApproxDate.Precision.YEAR_MONTH -> {
            val ym = YearMonth.of(date.year, date.month!!)
            ym.atEndOfMonth() // 月末
        }
        ApproxDate.Precision.FULL_DATE ->
            LocalDate.of(date.year, date.month!!, date.day)
    }
}
```

### 2.2 为什么放 Domain 层

- **UI 不做业务判断**：HomeScreen / MedicineListScreen 只接收 `ExpiryStatus` 枚举，不做日期比较。
- **精度安全**：`YearMonth.atEndOfMonth()` 正确处理 "2028-03" 的月末边界，不偷补日。
- **可测试**：ExpiryChecker 是纯 Kotlin 对象，可写 JVM 单元测试（不依赖 Android）。
- **单一来源**：首页和列表共用同一套判断逻辑，不会出现"首页说临期、列表说正常"的矛盾。

### 2.3 筛选不靠 SQL（MVP 数据量小）

家庭药箱数据量 < 100 条，不做复杂 SQL 过期查询。方案：

1. `MedicineDao.observeAll()` / `search(query)` 返回全量 / 搜索结果。
2. ViewModel 用 `ExpiryChecker.status()` 对每条药品计算状态。
3. `ExpiryFilter`（ALL / EXPIRING / EXPIRED）在 ViewModel 的 Flow 变换里 filter。

这样避免了 SQL 对 `2028-03` vs `2028-03-15` 的字典序精度问题（见 S1_ENTITY_DESIGN §9 分析）。

---

## 3. HomeScreen 设计（非 Dashboard · 药箱入口）

### 3.1 页面目标

打开 2 秒内回答两个问题：
1. **有没有需要关注的药？**（过期、快到期、快用完）
2. **怎么进入药箱列表？**

**不**回答：有多少种药、总库存多少、多少临期——这些是 Dashboard 数据，用户在列表里自然看到。

### 3.2 信息层级（4 层 · 按注意力递减）

```
┌──────────────────────────────────────┐
│  AppTopBar（"药箱"）                   │  ← 无底色、无阴影，底部 divider
├──────────────────────────────────────┤
│                                      │
│  space8 (40dp) 呼吸留白               │
│                                      │
│  ┌─ Layer 1: 需要关注 ──────────────┐│
│  │ ▸ 有 1 个药品已过期          →   ││  ← danger 色，可点击进列表(过期筛选)
│  │ ▸ 有 2 个药品即将到期        →   ││  ← warning 色，可点击进列表(临期筛选)
│  │ ▸ 有 1 个药品快用完了        →   ││  ← inkSecondary，可点击进列表
│  │ (以上均无时不显示任何东西)        ││  ← 不要为了显示而显示
│  └──────────────────────────────────┘│
│                                      │
│  sectionGap (24dp)                   │
│                                      │
│  Layer 2: 药箱概况（一行安静文字）     │
│  我的药箱 · 共 12 种药                │  ← titleSmall + secondary，不是宫格
│                                      │
│  sectionGap (24dp)                   │
│                                      │
│  ┌─ Layer 3: 最近添加 ─────────────┐ │
│  │ 最近添加                  全部 →│ │  ← section 标签 + Ghost 链接
│  ├──────────────────────────────┤  │
│  │ 布洛芬缓释胶囊   2028年3月 [临期]│ │  ← AppListItem 行
│  │ 0.3g*20片 · 3盒               │  │
│  ├──────────────────────────────┤  │
│  │ 感冒灵颗粒      2027年11月     │  │
│  │ 9g*10袋 · 2盒                  │  │
│  └──────────────────────────────┘  │
│  (最多 3 条，最近添加的)              │
│                                      │
│  sectionGap (24dp)                   │
│                                      │
│  Layer 4: 进入全部列表               │
│  [   查看全部药品   ]                 │  ← AppButtonSecondary 全宽
│                                      │
│  sectionGap (24dp)                   │
│                                      │
│  [   扫描药品   ]                     │  ← AppButtonPrimary 全宽(唯一品牌色)
│                                      │
│  space8 (40dp) 底部留白               │
└──────────────────────────────────────┘
```

### 3.3 各层详细设计

#### Layer 1 — 需要关注（可操作 · 不为显示而显示）

**显示条件**：只在该类别有药品时显示对应条目；全都没有则整个 Layer 1 消失（不留空区域）。

| 条目 | 条件 | 颜色 | 文案 | 点击行为 |
|------|------|------|------|---------|
| 已过期 | `expiredList.isNotEmpty()` | danger | "有 N 个药品已过期" | → MedicineList(filter=EXPIRED) |
| 即将到期 | `expiringSoonList.isNotEmpty()` | warning | "有 N 个药品即将到期" | → MedicineList(filter=EXPIRING) |
| 快用完 | `lowStockList.isNotEmpty()` | inkSecondary | "有 N 个药品快用完了" | → MedicineList(filter=ALL) |

- 每条是**一行文字 + 右箭头**，不是卡片。用 `AppListItem` 的简化形态或自定义 Row。
- 文字用 `secondary` (14sp) + 对应状态色。
- 不用图标装饰；不用红底/黄底铺满。

#### Layer 2 — 药箱概况（一行安静文字）

```
我的药箱 · 共 12 种药
```

- 默认药箱名 + 药品总数，一行 `titleSmall`(17sp,600) + `secondary`(14sp)。
- 不是数字宫格，不是统计卡。**一行文字**。
- 若总数为 0，不显示此行（空状态已覆盖）。

#### Layer 3 — 最近添加（列表行 · 非 Card）

```
最近添加                    全部 →
────────────────────────────────
布洛芬缓释胶囊      2028年3月 [临期]
0.3g*20片 · 3盒
────────────────────────────────
感冒灵颗粒          2027年11月
9g*10袋 · 2盒
────────────────────────────────
```

- section 标签："最近添加"（`section` 15sp,600）+ 右侧 Ghost 按钮"全部 →"跳转列表。
- 每行用 `AppListItem`：
  - `title` = 药品名称
  - `subtitle` = 规格 + " · " + 库存（如 "0.3g*20片 · 3盒"）
  - `trailing` = 有效期展示串 + `AppTag`（临期/过期时）
  - `onClick` = → Detail(id)
- 最多显示 **3 条**（最近添加的 `createdAt DESC`）。
- 行间用 `divider` 发丝线，不套 Card。

#### Layer 4 — 进入全部列表

```
[   查看全部药品   ]
```

- `AppButtonSecondary` 全宽 → MedicineListScreen。
- 按钮文字用动作："查看全部药品"，不用"智能管理"。

#### 扫描入口

```
[   扫描药品   ]
```

- `AppButtonPrimary` 全宽（唯一品牌绿按钮）→ ScanScreen。
- 与"查看全部"分开，通过 `sectionGap` 隔开。

### 3.4 HomeScreen 状态

| 状态 | UI | 文案 |
|------|-----|------|
| **Empty** (无药品) | `AppEmptyState` 居中 | 标题"你的药箱还是空的" / 说明"扫描第一件药品，几秒就能记下来。" / 按钮"扫描药品" |
| **Loading** (首次加载) | `AppLoadingState` | 无文字或"正在读取药箱…" |
| **Error** (读取失败) | `AppErrorState` | "药品读取失败了" / "存储出了问题，点这里重试。" |
| **Data** (有药品) | Layer 1-4 完整呈现 | 见 3.3 |
| **Data 但无关注项** | Layer 1 消失，Layer 2-4 正常 | 不显示"0 临期"之类 |

### 3.5 与 S0 占位的差异

| S0 占位 | S2 改动 |
|---------|---------|
| 三宫格 `AppStat` (0 种类/0 库存/0 临期) | **删除**，改为 Layer 1 关注条目 + Layer 2 一行概况 |
| 两个 `AppEmptyState` 硬编码 | 改为根据实际数据动态显示 Empty 或 Data 状态 |
| 无 ViewModel | 新增 `HomeViewModel` 绑定 Repository |
| 无真实数据 | 使用 `observeMedicines()` 真实 Room 数据 |

---

## 4. MedicineListScreen 设计（列表行 · 非卡片 · 搜索为核心）

### 4.1 页面目标

输入"布洛芬"就能找到药。浏览全部药品，按有效期筛选，点击进详情。

### 4.2 信息层级

```
┌──────────────────────────────────────┐
│  AppTopBar（"药箱"）                   │
├──────────────────────────────────────┤
│                                      │
│  ┌──────────────────────────────────┐│
│  │ 🔍 搜索药品名称、厂家、批号       ││  ← AppTextField, surfaceVariant 凹陷底
│  └──────────────────────────────────┘│
│                                      │
│  space3 (12dp)                       │
│                                      │
│  [全部] [临期] [过期]                 │  ← AppFilterChip pill, 水平可滚动
│                                      │
│  space4 (16dp)                       │
│                                      │
│  ┌─ 列表 ───────────────────────────┐│
│  │ 布洛芬缓释胶囊     2028年3月[临期]││
│  │ 0.3g*20片 · 3盒 · 我的药箱        ││  ← AppListItem 行 + divider
│  ├──────────────────────────────────┤│
│  │ 感冒灵颗粒         2027年11月     ││
│  │ 9g*10袋 · 2盒                     ││
│  ├──────────────────────────────────┤│
│  │ 创可贴             2029年6月      ││
│  │ 100片 · 1盒 · 急救箱              ││
│  ├──────────────────────────────────┤│
│  │ ...                              ││
│  └──────────────────────────────────┘│
│                                      │
└──────────────────────────────────────┘
```

### 4.3 搜索设计

| 属性 | 设计 |
|------|------|
| 搜索框 | `AppTextField`，`surfaceVariant` 凹陷底，`radiusMd`，placeholder = "搜索药品名称、厂家、批号" |
| 搜索字段 | 药品名称(`name`) + 规格(`specification`) + 厂家(`manufacturer`) + 批号(`batch_no`) — DAO `search()` 已覆盖 |
| 搜索语法 | **无复杂语法**，纯文本 LIKE 匹配，输入即搜 |
| 实时 | 输入变化时自动触发搜索（debounce 300ms 避免抖动） |
| 空查询 | 显示全部药品（`observeAll()`），不显示"请输入关键词"提示 |
| 结果排序 | 搜索结果按 `name ASC`（DAO 已实现）；无搜索时按 `createdAt DESC`（最近添加优先） |

### 4.4 筛选设计

**只保留基于有效期状态的筛选**（来自 Domain ExpiryChecker），不用不存在的假分类。

| Chip | 文案 | 条件 |
|------|------|------|
| 全部 | "全部" | 所有药品 |
| 临期 | "临期" | `ExpiryStatus.EXPIRING_SOON` 或 `EXPIRED`（过期药也属"需要关注"） |
| 过期 | "过期" | `ExpiryStatus.EXPIRED` |

> 注：S0 占位有 "感冒相关"、"外用" 等假分类——数据模型里没有分类字段，删除。

### 4.5 列表行设计（AppListItem · 非 Card）

每行内容**按相关性选择显示**，不一次铺满所有字段：

| 位置 | 内容 | 样式 | 显示条件 |
|------|------|------|---------|
| `title` | 药品名称 | `bodyStrong` (16sp,500) | 必显示 |
| `subtitle` | 规格 + " · " + 库存 + (位置) | `secondary` (14sp) | 有则显示，用 " · " 连接 |
| `trailing` | 有效期展示串 + `AppTag` | `caption` + Tag | 有有效期才显示 |
| `onClick` | → Detail(id) | — | — |

**subtitle 拼装规则**（按有则拼接）：
```
规格 · N盒 · 位置名
```
- 规格：有则显示，如 "0.3g*20片"
- 库存："N盒"（或 "N片"/"N瓶"——MVP 统一 "N盒" 即可，不做单位换算）
- 位置名：非默认药箱才显示，如 "急救箱"；默认药箱不显示

**trailing 有效期展示**：
- 有效期有值：`ExpiryChecker.displayString()` 展示串（如 "2028年3月"）
- 临期：追加 `AppTag("临期", Warning)`
- 过期：追加 `AppTag("已过期", Danger)`
- 无有效期：不显示 trailing

**示例行**：
```
布洛芬缓释胶囊        2028年3月 [临期]
0.3g*20片 · 3盒

感冒灵颗粒            2027年11月
9g*10袋 · 2盒

创可贴                 2029年6月
100片 · 1盒 · 急救箱
```

### 4.6 MedicineListScreen 状态

| 状态 | 触发条件 | UI | 文案 |
|------|---------|-----|------|
| **Empty (无药品)** | 总数为 0 且无搜索 | `AppEmptyState` 居中 | "你的药箱还是空的" / "扫描第一件药品，几秒就能记下来。" / 按钮"扫描药品" |
| **Search no result** | 有搜索词但结果为空 | `AppEmptyState` 居中 | "没有找到相关药品" / "换个关键词，或扫描新药盒。" |
| **Filter no result** | 筛选临期/过期但无结果 | `AppEmptyState` 居中 | "暂时没有临期的药" / "药品快过期时会在这里提醒你。" 或 "没有过期的药" |
| **Loading** | 首次加载 | `AppLoadingState` | "正在读取…" |
| **Error** | 读取异常 | `AppErrorState` | "药品读取失败了" / "存储出了问题，点这里重试。" |
| **Data** | 正常 | 列表行 + 搜索框 + 筛选 | — |

### 4.7 与 S0 占位的差异

| S0 占位 | S2 改动 |
|---------|---------|
| 假分类 chips ("感冒相关"/"外用") | **删除**，改为有效期筛选（全部/临期/过期）|
| 硬编码空状态 | 改为 Empty/Search-no-result/Filter-no-result/Error 多状态 |
| 无 ViewModel | 新增 `MedicineListViewModel` 绑定 Repository |
| 无真实数据 | 使用 `observeMedicines()` / `searchMedicines()` 真实 Room 数据 |
| placeholder "搜索药品名称、规格、厂家" | 改为 "搜索药品名称、厂家、批号"（匹配 DAO search 字段）|

---

## 5. ViewModel 设计

### 5.1 HomeViewModel

```kotlin
// ui/home/HomeViewModel.kt

data class HomeAttention(
    val expiredCount: Int,
    val expiringSoonCount: Int,
    val lowStockCount: Int,
)

data class HomeRecentItem(
    val id: Long,
    val name: String,
    val specAndStock: String,      // "0.3g*20片 · 3盒"
    val locationName: String?,     // null = 默认药箱，不显示
    val expiryDisplay: String?,   // "2028年3月"
    val expiryStatus: ExpiryStatus,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val totalCount: Int = 0,
    val defaultLocationName: String = "我的药箱",
    val attention: HomeAttention? = null,      // null = 无需关注（不显示 Layer 1）
    val recentItems: List<HomeRecentItem> = emptyList(),
    val error: String? = null,
)
```

**数据流**：
```
MedicineRepository.observeMedicines()  → Flow<List<Medicine>>
  ↓ map { 计算每条的 ExpiryStatus、拼装 specAndStock、取 locationName }
  ↓ 分组：expired / expiringSoon / lowStock / recent(top 3)
HomeUiState
```

- `lowStock`：stock <= 2（可调阈值，MVP 用 2）。
- `recentItems`：按 `createdAt DESC` 取前 3。
- `attention` 为 null 当且仅当三个 count 全为 0。

### 5.2 MedicineListViewModel

```kotlin
// ui/list/MedicineListViewModel.kt

enum class ExpiryFilter { ALL, EXPIRING, EXPIRED }

data class MedicineRowData(
    val id: Long,
    val name: String,
    val specAndStock: String,
    val locationName: String?,
    val expiryDisplay: String?,
    val expiryStatus: ExpiryStatus,
)

data class MedicineListUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val filter: ExpiryFilter = ExpiryFilter.ALL,
    val rows: List<MedicineRowData> = emptyList(),
    val totalCount: Int = 0,       // 全部药品总数（不受搜索/筛选影响，用于判断 Empty）
    val error: String? = null,
)
```

**数据流**：
```
if (searchQuery.isBlank())
    MedicineRepository.observeMedicines()     // 全量
else
    MedicineRepository.searchMedicines(query)  // 搜索结果
  ↓ map { 计算每条 ExpiryStatus、拼装 specAndStock、取 locationName }
  ↓ filter { 满足 ExpiryFilter }
MedicineListUiState
```

- 搜索 debounce 300ms（`debounce(300)` Flow operator）。
- 筛选在 Flow 变换里做，不额外查数据库。
- `totalCount` 始终来自 `observeMedicines().map { it.size }`，不受搜索影响。

### 5.3 ViewModel 依赖注入

MVP 不用 Hilt/Dagger，用 **Application + ViewModelProvider.Factory** 模式：

```kotlin
// MediBoxApp.kt 已有 db + repository 懒加载
// 新增：
fun provideHomeViewModelFactory() = HomeViewModelFactory(medicineRepository, locationRepository)
fun provideListViewModelFactory() = MedicineListViewModelFactory(medicineRepository, locationRepository)
```

- ViewModel 在 `init` 中 `collect` Flow → `StateFlow<UiState>`。
- UI 用 `collectAsStateWithLifecycle()` 订阅。

---

## 6. 需要新增 / 修改的文件清单（设计确认后再编码）

### 6.1 新增

| 文件 | 职责 |
|------|------|
| `domain/ExpiryChecker.kt` | 有效期判断 + 展示串 + 相对文案（集中至 Domain） |
| `ui/home/HomeViewModel.kt` | 首页状态 + 数据流 + Factory |
| `ui/list/MedicineListViewModel.kt` | 列表状态 + 搜索 + 筛选 + Factory |
| `ui/components/MedicineRow.kt`（可选） | 药品行专用组件，封装 title/subtitle/trailing 组合逻辑 |

### 6.2 修改

| 文件 | 改动 |
|------|------|
| `ui/screens/HomeScreen.kt` | 全面重写：删除 AppStat 三宫格，改为 4 层信息架构 + ViewModel 绑定 |
| `ui/screens/MedicineListScreen.kt` | 重写：假分类删除，改为有效期筛选 + 搜索 debounce + ViewModel 绑定 + 多状态 |
| `ui/navigation/NavGraph.kt` | HomeScreen/MedicineListScreen 构造改为传 ViewModel（或 Factory）|
| `ui/navigation/Routes.kt` | 可能新增带参数路由：`detail/{id}`、`medicine_list?filter={filter}` |
| `MediBoxApp.kt` | 暴露 ViewModel Factory 方法 |

### 6.3 不修改

| 文件 | 原因 |
|------|------|
| `domain/ApproxDate.kt` | 已在 S1 完成且测试通过，不改 |
| `data/` 全部 | 数据层在 S1 已完成验证，不改 |
| `ui/theme/*` | Token 在 S0 已落定，不改 |
| `ui/components/AppListItem.kt` | 接口够用（title/subtitle/leading/trailing/onClick）；MedicineRow 若需更复杂布局可新建，不改 AppListItem |
| `ui/components/AppTag.kt` | 够用，不改 |
| `ui/components/AppStateView.kt` | 够用，不改 |
| 其他 S0 组件 | 够用，不改 |

---

## 7. 动画设计

| 场景 | 动画 | 时长 |
|------|------|------|
| 页面进入 | `fadeIn` + `slideUp(16dp)` | 200ms, `Standard` easing |
| 搜索结果切换 | `crossfade` | 150ms |
| 筛选切换 | `crossfade` | 150ms |
| 列表项出现 | 逐个 `fadeIn`（间隔 ≤40ms） | 150ms |
| 按压反馈 | 底色变化 (`brandSoft`/`surfaceSunken`) | 150ms |

**禁止**：弹跳、弹簧过冲、粒子、发光、旋转、循环动画、页面级大位移。

---

## 8. 文案规范（Voice & Tone）

| 场景 | 文案 | 备注 |
|------|------|------|
| 首页空状态标题 | "你的药箱还是空的" | 第二人称，温度但不谄媚 |
| 首页空状态说明 | "扫描第一件药品，几秒就能记下来。" | 动作引导 |
| 首页关注-过期 | "有 N 个药品已过期" | 醒目但不恐慌 |
| 首页关注-临期 | "有 N 个药品即将到期" | 温和提醒 |
| 首页关注-缺货 | "有 N 个药品快用完了" | 中性 |
| 首页概况 | "我的药箱 · 共 N 种药" | 安静一行 |
| 列表搜索框 | "搜索药品名称、厂家、批号" | 匹配 DAO 字段 |
| 列表空状态 | "你的药箱还是空的" / "扫描第一件药品，几秒就能记下来。" | 同首页 |
| 搜索无结果 | "没有找到相关药品" / "换个关键词，或扫描新药盒。" | 引导下一步 |
| 筛选无结果-临期 | "暂时没有临期的药" / "药品快过期时会在这里提醒你。" | 安心 |
| 筛选无结果-过期 | "没有过期的药" | 简洁 |
| 错误状态 | "药品读取失败了" / "存储出了问题，点这里重试。" | 人话，不抛异常 |

**禁用词**：智能、AI、赋能、魔法、全自动、革命性、下一代、重新定义、AI 驱动、平台、解决方案、智能识别。

---

## 9. 颜色使用清单（核对品牌绿占比 < 8%）

| 元素 | 颜色 | 备注 |
|------|------|------|
| 页面背景 | `paper` | 全页 |
| TopBar | `paper` + 底部 `divider` | 无底色无阴影 |
| 搜索框底 | `surfaceVariant` | 凹陷感 |
| 筛选 chip 选中 | `brandSoft` 底 + `brand` 文字 + 1dp `brand` 描边 | pill |
| 筛选 chip 未选 | `surfaceVariant` 底 + `inkSecondary` 文字 | pill |
| 列表行底 | `paper` / `surface` | 无卡片 |
| 列表行分隔 | `divider` 1dp | 发丝线 |
| 药品名称 | `ink` | bodyStrong |
| 规格/库存/位置 | `inkSecondary` | secondary |
| 有效期文字 | `inkSecondary` | caption |
| 临期 Tag | `warningSoft` 底 + `warning` 文字 | 小标签 |
| 过期 Tag | `dangerSoft` 底 + `danger` 文字 | 小标签 |
| 扫描按钮 | `brand` 底 + `onBrand` 文字 | **唯一品牌绿大块** |
| 查看全部按钮 | `surfaceVariant`/描边 + `ink` 文字 | Secondary |
| 空状态图标 | `inkTertiary` | 48dp |
| 空状态标题 | `ink` | titleSmall |
| 空状态说明 | `inkSecondary` | secondary |
| 加载 spinner | `inkSecondary` | 28dp |

**品牌绿面积**：仅扫描按钮（1 个全宽按钮） + 筛选选中态（2-3 个小 pill）。预估 < 5%，满足 < 8% 要求。

---

## 10. 反 AI 10 题自测

| # | 问题 | 回答 | 理由 |
|---|------|------|------|
| 1 | 看起来像 AI 自动生成吗？ | **否** | 暖纸背景 + 系统中文字体 + 发丝线 + 无 AI 紫/渐变/玻璃；首页不是 Dashboard |
| 2 | 看起来像 Material Demo 吗？ | **否** | 完全重主题化，无 M3 紫；组件自研，不用默认 Scaffold 样式 |
| 3 | 看起来像模板 App 吗？ | **否** | 首页 4 层信息架构非通用模板；列表非卡片墙；文案说人话 |
| 4 | 是否存在过度 Card？ | **否** | 列表用 AppListItem + divider，无卡片包裹；首页关注条目是一行文字不是卡片 |
| 5 | 是否存在过度圆角？ | **否** | 行/容器 12dp，标签 8dp，搜索框 12dp；筛选 pill 是合理例外 |
| 6 | 是否存在无意义装饰？ | **否** | 无装饰图标、无渐变、无 emoji、无发光；每个元素都有信息或操作功能 |
| 7 | 是否可以删除 10% 的视觉元素？ | **否** | 当前已极简：TopBar + 关注 + 概况 + 最近列表 + 入口 + 扫描。删任何一项都损失功能 |
| 8 | 是否可以减少颜色？ | **否** | 仅 paper/surface 3 级 + ink 3 级 + brand + danger/warning + divider；每色有明确用途 |
| 9 | 是否可以减少组件？ | **否** | AppListItem/AppTopBar/AppTag/AppEmptyState/AppLoadingState/AppErrorState/AppTextField/AppFilterChip/AppButton——每组件有独占职责 |
| 10 | 隐藏项目名称，是否仍像一个成熟的商业 App？ | **是** | 暖纸调色 + 系统字体 + 克制留白 + 人话文案 + 安静列表 = 成熟工具感 |

**得分：10/10** ✅（目标 ≥8/10）

---

## 11. 编码阶段待办（设计确认后执行）

> 以下为编码计划，**设计确认前不执行**。

### 11.1 编码顺序

1. `domain/ExpiryChecker.kt` + 单元测试（ExpiryCheckerTest）
2. `ui/home/HomeViewModel.kt` + Factory
3. `ui/list/MedicineListViewModel.kt` + Factory
4. `MediBoxApp.kt` 暴露 Factory
5. `ui/screens/HomeScreen.kt` 重写（绑定 ViewModel）
6. `ui/screens/MedicineListScreen.kt` 重写（绑定 ViewModel + 搜索 + 筛选 + 状态）
7. `ui/navigation/NavGraph.kt` + `Routes.kt` 调整路由参数
8. （可选）`ui/components/MedicineRow.kt`

### 11.2 完成检查清单

- [ ] 搜索"布洛芬"能找到对应药品（名称/厂家/批号匹配）
- [ ] 空状态正确显示（无药品时、搜索无结果时、筛选无结果时）
- [ ] 数据状态正确显示（有药品时列表渲染）
- [ ] 有效期正确分类（过期/临期/正常，`ApproxDate` YYYY-MM 不补日）
- [ ] 删除药品后列表自动更新（Flow 响应式）
- [ ] 修改库存后列表自动更新
- [ ] 修改有效期后列表自动更新
- [ ] `assembleDebug` 通过
- [ ] `testDebugUnitTest` 全部通过
- [ ] 更新 `DEVELOPMENT_LOG.md`
- [ ] 暂停，不进入 S3

---

## 12. 设计决策记录（关键选择的理由）

| 决策 | 选择 | 拒绝的方案 | 理由 |
|------|------|-----------|------|
| 首页信息架构 | 4 层注意力递减 | 三宫格 Dashboard | 用户要"不为显示而显示"；关注信息比统计数字更有价值 |
| 列表行样式 | AppListItem + divider | Card 包裹每行 | 用户要求"安静、清晰、高信息密度但不拥挤" |
| 筛选维度 | 有效期状态（全部/临期/过期） | 假分类（感冒/外用） | 数据模型无分类字段；有效期是用户真正关心的 |
| 有效期逻辑位置 | Domain 层 ExpiryChecker | UI 硬编码 / SQL 字典序 | 用户要求"集中到 Domain"；精度安全（YYYY-MM 月末边界） |
| 搜索实现 | DAO search() LIKE 匹配 | 复杂搜索语法 | 用户要求"无复杂搜索语法" |
| ViewModel 架构 | ViewModelProvider.Factory | Hilt/Dagger | MVP 不引入 DI 框架，保持简单 |
| 筛选实现 | Flow 变换 filter | 额外 SQL 查询 | 数据量小（< 100），避免 SQL 精度问题 |
| 搜索 debounce | 300ms | 即时 | 避免输入抖动导致过多查询 |

---

_设计完成。等待用户确认后进入编码阶段。_

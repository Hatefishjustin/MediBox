# 药箱管家 MediBox · 产品与技术分析（S0 前交付物）

> 本文是「从 0 到 1 开发 MediBox」的第一步交付物：**完整分析产品**，不直接写业务代码。
> 阅读对象：开发团队（含产品/设计/工程视角）。分析通过后再进入 **S0 项目初始化**。
> 英文产品名沿用 **MediBox**（也可在后续用更口语的 *PillBox* 作副标识，但不改动中文定位「家庭药品管理工具」）。

---

## 0. 关键环境结论与最大风险（先说最关键的）

| 检查项 | 当前环境 | 结论 |
|---|---|---|
| Java | OpenJDK 17 ✅ | 满足 Gradle/AGP 要求 |
| Android SDK | ❌ 未安装 | **S0 必须先安装 SDK 命令行工具并 accept licenses** |
| adb / 设备 | ❌ 无 | 本沙箱无真机、无模拟器镜像 |
| Gradle | ❌ 无（用 Gradle Wrapper 自下载） | 项目内置 wrapper 即可 |
| Android Studio | ❌ 无 | 全程用命令行 + 编辑器开发 |

**最大风险（R1）**：本环境可以**构建 APK**（安装 SDK 后），但无法在此完成「安装到真机 → 拍真实药盒 → 验证 OCR/相机」的验收步骤。
**缓解**：① S0 在本环境安装 Android SDK + 构建链路；② 用**模拟器**（下载系统镜像）做冒烟测试与 CI 式验证；③ 把**可安装 APK 交付给你**，由你在真机走查验收标准 1–14。所有相机/OCR 逻辑必须保证「无权限/无识别结果时优雅降级」，以便在无相机环境下仍可编译与单元验证。

---

## 1. 产品架构

### 1.1 产品本质
**本地优先（Local-first）的家庭药品「整理 + 查找」工具**。它帮普通家庭把家里的药理清楚，并在需要时快速找到。不是医疗软件，不是 AI 问诊，不是用药推荐。

### 1.2 核心价值（唯一）
> 让家庭药品「看得清、找得到、不会过期浪费」。

### 1.3 明确边界（NOT —— 写进代码注释与文案，永不越界）
- ❌ 不注册 / 不登录 / 无服务器 / 无 API Key / 不联网
- ❌ 不根据症状推荐药品、不诊断、不开药、不算剂量、不判断相互作用
- ❌ 不 AI 总结说明书、不联网搜说明书
- ❌ 不做复杂仓储/企业级库存

允许的「智能化」仅限：**在用户自己保存的说明书内容里做本地搜索**（纯文本检索，非医疗建议）。

### 1.4 信息架构（IA）
```
药箱管家
├─ 录入层：扫描(OCR) / 手动新建
├─ 数据层（本地）：药品、照片、说明书、库存记录、位置、分类、盘点
├─ 视图层：首页概览 / 药箱列表 / 药品详情 / 临期 / 盘点 / 设置
└─ 提醒层：WorkManager 周期检查 → 本地通知
```

---

## 2. 用户使用流程

### 2.1 主流程（首次录入，最高频）
```
打开 App
  → 首页点「扫描药品」
  → 授权相机（拒绝则回到首页并提示）
  → CameraX 预览 → 拍摄药盒
  → ML Kit 中文 OCR
  → 字段提取（名称/有效期/规格/厂家/生产日期）
       ├─ 与库内已有药品高度匹配 → 弹「发现已有药品」：显示当前库存，「+1 盒」/「重新录入」
       └─ 无匹配 → 进入「确认药品信息」
  → 用户确认/修改（OCR 未识别的字段留空，可手填）
  → 保存 → 回到药箱/首页
```

### 2.2 次流程
- **查找**：首页/药箱搜索框 → 实时过滤（名称/规格/厂家/备注）
- **查看**：药箱列表项 → 药品详情（全部字段 + 照片 + 说明书 + 库存记录）
- **库存变更**：详情页「+1 / 使用 -1 / 丢弃 -1 / 手动调整」→ 写 StockRecord
- **临期**：系统每日检查 → 通知 → 点通知进临期列表
- **盘点**：盘点页逐项快速改库存 → 完成 → 存盘点记录
- **备份**：设置 → 导出 zip（数据库+照片+说明书）/ 恢复

### 2.3 有效期状态机
```
无有效期 ──┐
           ├─(有日期)→ 正常(in-date) → 90天内(提醒?) → 30天内(提醒?) → 7天内(提醒?) → 已过期
```
状态仅用于**展示颜色 + 通知触发**，绝不影响任何医疗判断。

---

## 3. 页面地图

### 3.1 底部导航（5 项，扫描为视觉重点但不夸张）
`首页` · `药箱` · `扫描*` · `盘点` · `我的`
- `扫描*`：居中，克制的小号品牌色圆形按钮（约 56dp，非巨型 FAB），带描边相机图标。
- 其余 4 项为标准文字+线性图标。具体形态在 S9 UI 审查时定稿。

### 3.2 主要页面
| 页面 | 关键内容 | 必备状态 |
|---|---|---|
| 首页 Home | 我的药箱（种类/库存/临期统计）、最近添加、临期提醒、扫描/搜索入口 | 空态 / 加载 / 错误 |
| 药箱 MedicineList | 列表 + 搜索 + 筛选(分类/位置/临期/过期) + 排序(名称/有效期/添加时间) | 空态 / 加载 |
| 扫描 Scan | CameraX 预览、快门、权限引导 | 权限拒绝 / 错误 |
| OCR 确认 OcrConfirm | 识别字段可编辑表单，确认/手动补录 | 加载 / 识别失败 |
| 药品详情 Detail | 照片、全部字段、说明书入口、库存记录、编辑 | 加载 / 空 |
| 临期 Expiring | 临期/过期筛选列表（通知入口同页） | 空态 |
| 盘点 Inventory | 全量列表快速改库存 → 完成 | 空态 / 完成 |
| 我的 Profile | 设置入口、关于、备份恢复、分类/位置管理 | — |

### 3.3 子流程/弹层
编辑药品（复用详情编辑态）、分类管理、位置管理、通知设置、备份恢复、说明书查看（图片/PDF）、关于、手动新建药品。

---

## 4. 数据库设计（Room / SQLite）

### 4.1 ER 概览
```
Medicine 1──* MedicinePhoto
Medicine 1──* Instruction
Medicine 1──* StockRecord
Medicine *──1 Category (可空)
Medicine *──1 Location (可空)
InventoryCheck 1──* InventoryCheckItem ──* Medicine
```

### 4.2 实体字段（Kotlin 示意）
```kotlin
@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                 // 必填
    val specification: String? = null,// 规格
    val manufacturer: String? = null, // 生产厂家
    val productionDate: Long? = null, // epoch millis（本地零点）
    val expiryDate: Long? = null,     // 有效期
    val categoryId: Long? = null,
    val locationId: Long? = null,
    val usageFor: String? = null,     // 成人/儿童/全家
    val stock: Int = 1,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "medicine_photos")
data class MedicinePhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val path: String,        // app 私有目录相对/绝对路径
    val isPrimary: Boolean = false,
    val createdAt: Long
)

@Entity(tableName = "instructions")
data class Instruction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val type: String,        // "image" | "pdf"
    val path: String,
    val createdAt: Long
)

@Entity(tableName = "stock_records")
data class StockRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val changeType: String,  // INCREASE/DECREASE/ADJUST/DISCARD/PURCHASE/USE
    val amount: Int,
    val balance: Int,        // 变更后库存
    val note: String? = null,
    val createdAt: Long
)

@Entity(tableName = "locations")
data class Location(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val isDefault: Boolean, val sortOrder: Int, val createdAt: Long)

@Entity(tableName = "categories")
data class Category(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val isDefault: Boolean, val sortOrder: Int, val createdAt: Long)

@Entity(tableName = "inventory_checks")
data class InventoryCheck(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String?, val createdAt: Long, val note: String? = null)

@Entity(tableName = "inventory_check_items")
data class InventoryCheckItem(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val checkId: Long, val medicineId: Long, val oldStock: Int, val newStock: Int, val updatedAt: Long)
```

### 4.3 关键约定
- **日期存储**：`Long` epoch millis（本地时区零点），避免字符串歧义；展示时格式化。
- **图片存储**：拍摄/导入的文件存入 `Context.getExternalFilesDir()` 下 `photos/`、`instructions/`（app 私有，**无需存储权限**），DB 只存路径。
- **设置存储**：用 **DataStore (Preferences)** 存开关（通知总开关、7/30/90 天提醒、是否显示过引导），与领域数据分离；分类/位置属用户数据，走 Room。
- **索引**：`medicines(name)`、`medicines(expiryDate)`、`stock_records(medicineId)`、`photos/instructions(medicineId)`。
- **迁移**：`fallbackToDestructiveMigration` 在 S0–S4 可接受；正式版起启用 `autoMigrations`。
- **默认数据**：首次启动播种 8 个分类 + 5 个位置。

---

## 5. 技术架构

### 5.1 分层
```
UI (Jetpack Compose)
   ↓
ViewModel (lifecycle-viewmodel-compose)
   ↓
Repository (领域数据聚合)
   ↓
Room (DAO)  ── SQLite
      ↘ DataStore (设置)
能力层：CameraX / ML Kit / WorkManager / SAF / Coil(本地图片)
```

### 5.2 模块目录（app 模块内，不过度拆分）
```
app/src/main/java/com/mediBox/...
├─ ui/
│  ├─ theme/      # DesignSystem: Color/Typography/Shape/Spacing/Theme
│  ├─ components/ # EmptyState/ListItem/TopBar/StateViews 等共享件
│  ├─ home/  medicinelist/  scan/  detail/  inventory/  settings/
├─ data/
│  ├─ local/db/        # entities, daos, AppDatabase
│  ├─ local/store/     # DataStore settings
│  ├─ repository/
│  └─ datasource/      # CameraManager, OcrEngine, BackupManager, ExpiryNotifier
├─ domain/             # 轻量模型与纯函数（日期解析、匹配算法）
├─ util/               # DateParser, Permission, FileUtils
└─ MainApplication / MainActivity / NavGraph
```

### 5.3 关键依赖（最小化，均为第一方/Jetpack 或必要SDK）
| 用途 | 依赖（写 S0 时锁定版本） |
|---|---|
| 语言/构建 | Kotlin 2.0+，AGP 8.5+，Compose Compiler 插件 |
| UI | Compose BOM、material3（仅作基础组件，全量重主题化）、navigation-compose、activity-compose |
| 生命周期 | lifecycle-viewmodel-compose / runtime-ktx |
| 本地存储 | room + **KSP**、datastore-preferences |
| 相机 | camera-core / camera-camera2 / camera-lifecycle / camera-view |
| OCR | **com.google.mlkit:text-recognition-chinese**（离线，首次需下载模型，非数据上传） |
| 后台 | workmanager |
| 图片 | coil（加载本地文件，压缩由采样控制） |
| 测试 | junit、compose-ui-test、room-testing、kaspresso(可选) |

> 不引入：任何图表库、任何后端 SDK、任何 AI/LLM SDK、任何重型 DI（用轻量手动/工厂或 `androidx.hilt` 仅当确需；优先构造函数注入）。

### 5.4 权限矩阵（最小化）
| 权限 | 用途 | 备注 |
|---|---|---|
| CAMERA | 扫描药盒 | 运行时请求；拒绝则引导手动录入 |
| POST_NOTIFICATIONS | 临期提醒 | Android 13+ 运行时请求；可关 |
| （无需存储权限） | 备份/恢复/存图 | 全部走 **SAF 文件选择器** + app 私有目录 |

### 5.5 关键流程实现要点
- **CameraX 捕获**：`ImageCapture` 输出到 `getExternalFilesDir("photos")` 临时文件 → 传给 ML Kit `InputImage.fromFilePath` → 识别。
- **OCR 字段提取**：`TextRecognizer`(chinese) 取 `text` 全文 → `DateParser` 用多正则（见 §7 日期兼容）抽取有效期/生产日期；药品名称取置信度最高的「药品名/通用名称」候选行；规格/厂家用关键词定位。
- **已有药品匹配**：归一化名称（去空格/括号/规格）后与库内 `name` 做包含/编辑距离匹配，阈值命中即提示「发现已有药品」。
- **通知**：`ExpiryCheckWorker`（WorkManager `PeriodicWorkRequest` 24h）查询 `expiryDate` 落在启用阈值内的药品，用 `NotificationManager` 发分组通知；点击 `PendingIntent` 深链到临期页。
- **备份**：`ACTION_CREATE_DOCUMENT` 选位置 → 用 `java.util.zip` 打 zip（db 文件 + photos + instructions）→ 写；恢复：`ACTION_OPEN_DOCUMENT` 读 zip → 校验 → 覆盖 db + 解压资源。

---

## 6. Design System（统一 Token，禁止各页自定）

> 所有页面**只**引用下列 Token，不得在 Compose 里写死颜色/字号/圆角。

### 6.1 Color（温暖 / 中性 / 克制；无紫无蓝无渐变）
| Token | 值 | 用途 |
|---|---|---|
| `paper` | `#FAF7F2` | App 背景（暖纸白，强反 AI 信号） |
| `surface` | `#FFFFFF` | 卡片/表单底 |
| `surfaceVariant` | `#F3EFE8` | 轻微背景变化（替代阴影） |
| `ink` | `#2B2722` | 主文字（暖近黑，非纯黑） |
| `inkSecondary` | `#6B635A` | 次级文字 |
| `inkTertiary` | `#9A9088` | 说明/元信息 |
| `divider` | `#E8E2D8` | 发丝分隔线 |
| `brand` | `#3F7A6B` | 唯一品牌色（沉静鼠尾草绿），仅用于主操作/小强调 |
| `brandSoft` | `#E4EFEA` | 选中/激活底色 |
| `brandInk` | `#2C5A4E` | 品牌色上的文字 |
| `danger` | `#B4473B` | 已过期（砖红，非纯红） |
| `warning` | `#B97A2E` | 临期（暖琥珀，非霓虹橙） |

**纪律**：状态色只出现在「过期/临期」与极端错误；正常内容一律中性，绝不用绿色铺满。品牌绿仅作点睛。

### 6.2 Typography（用 Android 系统字体 = 设备中文默认字体，保证原生阅读体验）
| Token | 字号/字重 | 用途 |
|---|---|---|
| Display | 28sp / 600 | 仅首页大数字统计（如「12 种」），不作标题 |
| Title | 20sp / 600 | 页面标题/区块标题 |
| Section | 15sp / 600 | 分组标签 |
| Body | 16sp / 400 | 列表主文字 |
| Secondary | 14sp / 400 | 列表副文字 |
| Caption | 12sp / 400 | 日期/元信息 |

行高：Body 1.45；标题 1.25。中文用 `FontFamily.Default`（即系统字体），不加载任何网络/自定义中文字体。

### 6.3 Spacing（4dp 基准，温暖节奏）
`4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48`。首页纵向留白偏大（≥24）。

### 6.4 Radius（按层级，不统一 24）
`sm 8`（输入/芯片/小按钮）· `md 12`（列表项/标准卡）· `lg 16`（弹层/对话框）· `pill 999`（药丸/扫描按钮）。

### 6.5 Elevation（极轻，暖色阴影；优先分隔线而非卡片）
`none 0` · `low 1dp(低透明度暖阴影)` · `med 2–3dp(弹层)`。普通列表用**留白+发丝线+轻微背景**，不滥用卡片。

### 6.6 Motion（自然/短/克制）
时长 150–200ms，ease-out；类型仅：淡入、位移(8–16dp)、轻微缩放(0.98→1)、状态过渡。**禁**弹跳/弹簧过冲/粒子/发光。

### 6.7 Icon（统一一套，线性、克制）
用 `Icons.Outlined.*`（第一方），统一 24dp、1.5px 描边观感，`tint=inkSecondary` 默认、激活用 `brand`。**不用 Emoji 当图标**，不堆装饰性图标。

---

## 7. UI 视觉方向

### 7.1 方向词
**Premium · Minimal · Calm · Human · Warm · Trustworthy · Native**。
像一款成熟的消费级独立 App，不像 AI Demo / 后台系统 / 医疗 SaaS。

### 7.2 视觉参考（真实产品共性：大量留白、发丝线、中性底、系统字体、极小强调）
- 国内：苹果「健康」、微信（设置/我的）、下厨房、豆瓣、小米运动。
- 海外：Things 3、Streaks、Daily、Bear、Calm。
共性＝**删掉不必要的元素**，靠层级/留白/排版取胜。

### 7.3 日期格式兼容（OCR 重点，正则清单）
有效期/生产日期需兼容（至少）：
```
有效期至 2028-03-15   有效期至 2028.03.15   有效期至 2028/03/15
EXP 2028-03-15        EXP: 2028/03         有效期 2028-03
2028年03月15日         2028.03              2028/3
```
识别不可靠 → **留空**，绝不猜测生成日期；用户必须可手改。

### 7.4 禁止清单（落到代码评审）
紫/蓝/渐变/玻璃拟态/发光/霓虹/大量 Emoji/巨型圆角/万物 Card/处处阴影/默认 M3 紫/巨型 Hero 标题/无意义图表/营销腔文案。

---

## 8. MVP 开发计划（阶段总览）

| 阶段 | 范围 | 交付/验收 |
|---|---|---|
| **S0** | 工程初始化、SDK、导航骨架、主题骨架 | 可编译空壳，底部导航可切换 |
| **S1** | Room 全表 + 仓库 + 默认数据 + 设置 | 数据库单测通过 |
| **S2** | 首页 + 药箱列表 + 搜索/筛选/排序 | 列表/空态走查 |
| **S3** | CameraX + ML Kit OCR + 字段提取 + 已有匹配 | 模拟器拍摄可出识别 |
| **S4** | OCR 确认 + 保存 + 详情 + 库存 + 持久化 | **真实机测试**（验收 1–14） |
| S5 | 有效期状态 + WorkManager 通知 | 通知触发走查 |
| S6 | 说明书（图/PDF 本地） | 增删查看 |
| S7 | 盘点 + 记录 | 完成流程 |
| S8 | 备份/恢复（SAF + zip） | 导出恢复闭环 |
| S9 | UI 全面反 AI 打磨 | 逐页审查通过 |
| S10 | 测试 + 产出 APK | 可安装 APK + 真机验收 |

**MVP 范围 = S0–S4 + 真实机测试**（验收标准 1–14）。其余为后续阶段，不提前做。

---

## 9. 风险清单

| ID | 风险 | 影响 | 缓解 |
|---|---|---|---|
| R1 | 本环境无 Android SDK/真机 | 无法在此完成真机验收 | S0 装 SDK；模拟器冒烟；交付 APK 由你真机验收 |
| R2 | 中文药盒 OCR 准确率参差（包装混乱） | 录入体验差 | 多正则日期解析 + 模糊名匹配 + 全字段可改 + 手动兜底；OCR 仅辅助 |
| R3 | ML Kit 中文模型首次需联网下载 | 首次识别慢/失败 | 引导重试；无模型时直接手动录入；不阻塞主流程 |
| R4 | Android 13+ 通知权限易被拒 | 临期提醒失效 | 优雅降级：App 内仍有临期红点/列表；引导开启 |
| R5 | OEM 省电策略杀 WorkManager | 提醒不准时 | 用 PeriodicWork + 重试；文档说明；不承诺精确时刻 |
| R6 | 备份含大量高清图 → 体积/失败 | 备份恢复慢/崩 | 图片合理压缩/采样；zip 分块；失败友好提示 |
| R7 | 大图加载卡顿 | 列表滚动差 | Coil + 缩略图 + 不预载全尺寸 |
| R8 | SAF 跨机型行为差异 | 恢复路径异常 | 严格校验 zip 结构；文件不存在/损坏友好提示 |
| R9 | 日期解析误判（如把批号当日期） | 错误有效期 | 仅匹配明确日期关键词；不可靠留空 |
| R10 | 范围蔓延（提前做非 MVP） | 拖慢交付 | 严守 MVP；非必需功能先不做 |

---

## 10. 第一阶段（S0–S4）任务拆解

### S0 项目初始化
- [ ] 安装 Android SDK 命令行工具 + accept licenses；配置 `ANDROID_HOME`（写 S0 时执行）
- [ ] 新建 Gradle 工程：Kotlin 2.0+、AGP 8.5+、minSdk 29、target 35/36、Compose BOM
- [ ] 锁定 §5.3 依赖；配置 KSP（Room）
- [ ] 建立 `ui/theme` 骨架（§6 Token 落地为 Compose `ColorScheme`/扩展）
- [ ] `NavGraph` + 底部导航外壳（5 项占位）+ 各页空状态占位
- [ ] 验证：`./gradlew assembleDebug` 通过

### S1 数据库与数据层
- [ ] 实现 §4 全部实体 + DAO（增删改查、筛选、排序、统计）
- [ ] `AppDatabase` + 数据库单测（插入/统计/迁移）
- [ ] `Repository` 聚合（药品/分类/位置/库存/说明书/盘点）
- [ ] 首次启动播种 8 分类 + 5 位置
- [ ] DataStore 设置（通知开关、7/30/90）
- [ ] `DateParser` 多正则日期解析单测

### S2 首页 + 药箱列表
- [ ] 首页：统计卡（种类/库存/临期）、最近添加、临期提醒入口、扫描/搜索入口（空态/加载/错误）
- [ ] 药箱列表：搜索（实时）、分类/位置/临期/过期筛选、名称/有效期/添加时间排序
- [ ] 共享 `ListItem`、`EmptyState`、`TopBar`、`StateView` 组件
- [ ] 反 AI 走查：留白/发丝线/非卡片化

### S3 CameraX + OCR
- [ ] `CameraManager`：CameraX 预览 + `ImageCapture` 到 app 私有目录
- [ ] 相机权限请求 + 拒绝引导
- [ ] `OcrEngine`：ML Kit 中文识别 + 字段提取（名称/有效期/规格/厂家/生产日期）
- [ ] 已有药品模糊匹配 → 「发现已有药品」提示流
- [ ] OCR 失败/无结果 → 转手动录入

### S4 OCR 确认 + 详情 + 库存
- [ ] OCR 确认页：全部字段可编辑表单（**绝不自动保存**），确认/手补
- [ ] 保存到 Room（含照片路径）
- [ ] 药品详情：照片/全部字段/说明书入口/库存记录；全部可编辑
- [ ] 库存：+1 / 使用-1 / 丢弃-1 / 手动调整 → 写 `StockRecord`
- [ ] 搜索生效；重启数据持久化
- [ ] **真机/模拟器走查验收标准 1–14**

---

## 11. 如何让 App 看起来「不像 AI 生成」（重点）

### 11.1 哲学
> 真正高级的设计是**删东西**，不是加东西。每个元素都要回答「没有它行不行」；不行就删。

AI Demo 的「味」来自：模板化 Material 紫、处处卡片+阴影、巨型圆角、Hero 大标题、空状态写「欢迎使用智能平台」、状态色滥用、Emoji 堆砌、为了高级感堆视觉。我们要的恰恰相反。

### 11.2 每屏 10 题自检（S9 及每完成一页必做）
1. 看起来像 AI 生成的吗？
2. 像 Material Demo 吗？
3. 过度用 Card 了吗？
4. 过度用圆角了吗？
5. 视觉拥挤吗？
6. 有无意义装饰？
7. 有廉价感吗？
8. 有模板感吗？
9. 像真正商业 App 吗？
10. 用户首次打开 2 秒内理解吗？
→ 任一不理想：**不解释，直接改**。

### 11.3 具体禁令 ↔ 正例
| 禁 | 改为 |
|---|---|
| 默认 M3 紫色/动态色 | 全量重主题化为 §6 暖纸调色板 |
| 万物 Card + 阴影 | 列表用留白+发丝线+`surfaceVariant` 轻微背景 |
| 统一 24dp 圆角 | 按层级 8/12/16/pill |
| 巨大 Hero 标题「我的药箱」 | 克制 Title(20sp)；大数字仅用于统计 |
| 空态「欢迎使用智能药品管理平台」 | 「你的药箱还是空的」+「扫描第一件药品」 |
| 状态色铺满 | 仅过期/临期用 danger/warning，其余中性 |
| Emoji 当图标 | 统一线性 `Icons.Outlined` |
| 营销腔「AI 智能识别」 | 「扫描药品」「库存」 |
| 弹跳/粒子/发光动画 | 150–200ms 淡入/位移/轻缩放 |

### 11.4 反 AI 的「杀手锏」细节
- **暖纸背景 `#FAF7F2`**：几乎任何 AI Demo 都用纯白或紫，暖纸白立刻显「人手调过」。
- **系统中文默认字体**：不加载花哨字体，阅读体验最原生。
- **发丝分隔线 + 大留白**：比卡片更像「真实笔记/健康类 App」。
- **单一克制品牌绿**：不靠颜色区分功能，靠层级与排版。
- **空状态说人话**：把「空」当作产品与用户第一次对话的机会。
- **文案去 AI 化**：通篇无「智能/赋能/AI/全自动/革命性」。
- **动效几乎无感**：用户感觉「顺」，而不是「炫」。

### 11.5 组件策略（避免 Material demo 感）
- 用 Material3 作**底层工具箱**，但 `ColorScheme`/`Typography`/`Shapes` 全量覆盖；自定义 `TopBar`/`ListItem`/`EmptyState`/`ScanButton`，不暴露默认 M3 脚手架样式。
- 底部导航自定义：5 项等宽，扫描为克制的品牌色圆形按钮（非夸张 FAB）。
- 表单/详情用原生排版节奏，不套「设置页模板」。

---

## 附录
- **英文命名**：MediBox 沿用；如需更口语可在商店副标题用 *PillBox*，不改变中文定位。
- **技术决策记录点**：SDK 版本、Compose BOM 版本、ML Kit 模型首次下载策略、备份 zip 结构，将在 S0/S1 落地时写入 `DEVELOPMENT_LOG.md`。
- **下一步**：本分析经确认后，从 **S0** 开始，逐阶段开发并每阶段更新 `docs/DEVELOPMENT_LOG.md`。

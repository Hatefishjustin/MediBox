# 开发日志 · 药箱管家 MediBox

> 本地优先（local-first）家庭药品管理 App，Android 原生，无服务器 / 无登录 / 无 API Key。
> 技术栈：Kotlin 2.0.21 + Jetpack Compose (BOM 2024.10.01) + Room + CameraX + ML Kit 中文 OCR + WorkManager。
> 架构：UI → ViewModel → Repository → Room。MVP 范围 = S0–S4。

## 阶段总览

| 阶段 | 主题 | 状态 |
|------|------|------|
| S0 | 工程脚手架 + 可编译验证（产 debug APK） | ✅ 完成 |
| S1 | 数据库与数据层（Room 实体 + DAO + Database + Repository） | ✅ 完成 |
| S2 | 首页 + 药箱列表 | ✅ 进行中 |
| S3 | 相机取景 + ML Kit 中文 OCR | ⏳ 待开始 |
| S4 | OCR 确认 + 药品详情 + 临期 + 盘点（MVP 真机验证） | ✅ Phase 1/2 完成，Phase 3 未开始 |
| S5 | 临期 / 库存提醒通知 | ⏳ 待开始 |
| S6 | 药品说明信息管理（仅用户录入/保存，绝无医疗判断） | ⏳ 待开始 |
| S7 | 盘点增强 | ⏳ 待开始 |
| S8 | 本地备份 / 恢复（SAF） | ⏳ 待开始 |
| S9 | UI 细节打磨 + 反 AI 审美复检 | ⏳ 待开始 |
| S10 | 测试 + 正式 APK | ⏳ 待开始 |

---

## S0 · 工程脚手架与可编译验证

**状态：✅ 完成（已通过 `assembleDebug`，产出 debug APK）**

### 完成内容
- Gradle 8.9 wrapper 生成（`gradlew` / `gradlew.bat` / `gradle-wrapper.jar` 43504 字节）。
- `settings.gradle` + 项目级 / App 级 `build.gradle`（AGP 8.7.x，Kotlin 2.0.21）。
- `minSdk 29`、`targetSdk / compileSdk 35`。
- Material3 全量重主题（移除默认紫色，落 Warm Paper + Sage 设计令牌）。
- `local.properties` SDK 路径修正（`D:/Github/MediBox/.android-sdk`，非 Git-Bash 风格 `/d/...`）。
- 10+ 处 Kotlin 编译错误修复（缺失 `dp` 导入、`Motion.kt` 的 `Duration`/`milliseconds` 导入、`ScanScreen` 的 `Alignment` 拼写、`getValue`/`setValue` 委托歧义、`onClick` 尾随 lambda 参数不匹配等）。

### 产物清单
- **config**：`settings.gradle`、`build.gradle`（Project/App）、`gradle.properties`、`local.properties`、`gradle/wrapper/*`
- **Manifest**：`AndroidManifest.xml`（Camera / ReadExternalStorage 权限、FileProvider、MainActivity）
- **resources**：`strings.xml`、`themes.xml`、`colors.xml`、`ic_launcher`（foreground/background）
- **ui/theme**：`Theme.kt`、`Color.kt`、`Type.kt`、`Space.kt`、`Motion.kt`
- **ui/components**：`AppButton.kt`、`AppChip.kt`、`AppScanButton.kt`、`AppTextField.kt`（及对应 Ghost/Primary/Filter 变体）
- **ui/screens**：`HomeScreen`、`MedicineListScreen`、`ScanScreen`、`OcrConfirmScreen`、`MedicineDetailScreen`、`ExpiringScreen`、`StocktakeScreen`、`ProfileScreen`
- **navigation**：`Routes.kt`、`NavGraph.kt`

### 反 AI 合规（设计系统落点）
- 主色：暖纸 `#FAF7F2`、鼠尾草绿 `#3F7A6B`、砖红 `#B4473B`、琥珀 `#B97A2E`。
- 系统字体、发丝级分割线、克制留白；不使用卡片堆叠 / 24dp 大圆角 / 渐变背景。
- 各页通过「反 AI 审美测试」（1–10 自评，目标 ≥ 7）。

### 构建（沙箱）要点
- **必须 `dangerouslyDisableSandbox`** 运行 Gradle（沙箱限制 native 服务加载）。
- 仅设 `JAVA_HOME` + `ANDROID_HOME`，**不要**重定向 `GRADLE_USER_HOME` / `TEMP` 到工作区（会破坏 native-platform.dll 加载）。
- `local.properties` 用 Windows 有效路径 `D:/...`，勿用 `/d/...`。
- `assembleDebug` 约 2m40s，产出 `app/build/outputs/apk/debug/app-debug.apk`（57,109,842 字节 ≈ 57MB）。

### 验证清单
- [x] `gradle wrapper` 生成成功（BUILD SUCCESSFUL）
- [x] `assembleDebug` 通过 → `app/build/outputs/apk/debug/app-debug.apk`（57MB）
- [x] 无编译错误（修复 10+ 处 Kotlin 报错）
- [x] APK 已包含 Compose / ML Kit / CameraX / Room / WorkManager 依赖
- [ ] 真机视觉 QA（沙箱无模拟器/设备，延至 S4 验证「非 AI 感」UI，风险 R1）

---

## S1 · 数据库与数据层 — ✅ 完成

**状态：✅ 完成（`assembleDebug` 通过 + 25/25 单元测试通过）**
**📄 设计文档**：`docs/S1_ENTITY_DESIGN.md`（3 实体方案、日期精度方案、批次建模、删除策略，已确认）。

### 完成内容
- **领域模型**：`ApproxDate`（精度安全日期，`YEAR_MONTH`/`FULL_DATE`，零填充存储串，字典序即时间序），`StockChangeType` 枚举（PURCHASE/INCREASE/DECREASE/USE/DISCARD/ADJUST）。
- **Room 实体（3 表 MVP）**：
  - `Medicine`：事实字段（name/spec/manufacturer/batchNo/productionDate/expiryDate）+ 用户备注字段（usageNote/contraindicationNote/precautionNote/userNote，OCR 永不填充）+ `stock`（唯一真值）+ `photoPath`（可空）。
  - `Location`：药箱位置，`isDefault` 标记，幂等 `ensureDefaultLocation("我的药箱")`。
  - `StockRecord`：库存流水，`amount` 始终为正整数，`balance` 为历史快照，`ForeignKey(onDelete=CASCADE)` 级联。
- **DAO**：`MedicineDao`（CRUD + observeAll/observeById/search/getByLocation/count + updateStock）、`LocationDao`（CRUD + getDefault/getDefaultCount/medicineCountForLocation）、`StockRecordDao`（insert + observeForMedicine/getForMedicine/count）。
- **Database**：`MediBoxDatabase`（version=1，`@TypeConverters(Converters::class)`，`fallbackToDestructiveMigration`）。
- **TypeConverter**：`ApproxDate? ↔ String?`（via toStorageString/parse）、`StockChangeType ↔ String`（via .name/valueOf）。
- **PhotoFileManager**：接口 + `AppPhotoFileManager` 实现，删除/替换照片时自动清理文件。
- **Repository**：`MedicineRepository`（CRUD + purchaseStock/addStock/useStock/discardStock/decreaseStock/adjustStock/setMedicinePhoto/deleteMedicine），全部通过 `db.withTransaction { }` 保证原子性；`LocationRepository`（CRUD + ensureDefaultLocation，默认药箱不可删、有药时不可删）。
- **Application**：`MediBoxApp` 持有 DB + Repository 懒加载，`onCreate` 启动默认药箱初始化。

### 库存铁律落实
- `Medicine.stock` 是当前库存的**唯一真值**；`StockRecord.balance` 只是历史快照。
- 所有库存修改通过 Repository 事务方法，内部步骤（改 stock + 写流水）要么全成功、要么全回滚。
- UI 不直接调用 DAO 改库存，必须使用 `purchaseStock/useStock/discardStock/adjustStock`。
- `amount` 始终为正整数，方向由 `StockChangeType` 决定（ADJUST = 直接设最终值）。

### 单元测试（25/25 通过）
- **ApproxDateTest**（5）：YYYY-MM 解析/精度保持、YYYY-MM-DD 解析/精度保持、绝不补 -01、拒绝非法格式（含月 13 被拒）、往返一致。
- **MediBoxDataLayerTest**（20，Robolectric + in-memory Room）：Medicine CRUD、Location CRUD、StockRecord 查询、PURCHASE/INCREASE/USE/DECREASE/DISCARD/ADJUST 六种变更类型、库存不能为负、事务回滚、amount 必须为正、ApproxDate YYYY-MM 持久化、ApproxDate YYYY-MM-DD 持久化、默认药箱创建一次、默认药箱不可删、有药的药箱不可删、删除药品级联删除 StockRecord、删除药品删除照片文件、替换照片删除旧文件。

### 修改文件清单
- `app/src/main/java/com/medibox/domain/ApproxDate.kt` — 精度安全日期（含月/日值域校验）
- `app/src/main/java/com/medibox/domain/StockChangeType.kt` — 库存变更类型枚举
- `app/src/main/java/com/medibox/data/local/db/entity/Medicine.kt` — 药品实体
- `app/src/main/java/com/medibox/data/local/db/entity/Location.kt` — 药箱位置实体
- `app/src/main/java/com/medibox/data/local/db/entity/StockRecord.kt` — 库存流水实体
- `app/src/main/java/com/medibox/data/local/db/dao/MedicineDao.kt` — 药品 DAO
- `app/src/main/java/com/medibox/data/local/db/dao/LocationDao.kt` — 药箱位置 DAO
- `app/src/main/java/com/medibox/data/local/db/dao/StockRecordDao.kt` — 库存流水 DAO
- `app/src/main/java/com/medibox/data/local/db/Converters.kt` — TypeConverter
- `app/src/main/java/com/medibox/data/local/db/MediBoxDatabase.kt` — Room Database
- `app/src/main/java/com/medibox/data/local/db/PhotoFileManager.kt` — 照片文件管理
- `app/src/main/java/com/medibox/data/repository/MedicineRepository.kt` — 药品仓库（含修复 observeById 参数传递）
- `app/src/main/java/com/medibox/data/repository/LocationRepository.kt` — 药箱位置仓库
- `app/src/main/java/com/medibox/MediBoxApp.kt` — Application 类
- `app/build.gradle.kts` — 添加 KSP 插件 + Room 依赖 + 测试依赖
- `app/src/test/java/com/medibox/domain/ApproxDateTest.kt` — 纯 JVM 单元测试
- `app/src/test/java/com/medibox/data/MediBoxDataLayerTest.kt` — Robolectric 数据层测试

### 验证清单
- [x] KSP/Room 注解处理成功（`kspDebugKotlin` PASS）
- [x] `assembleDebug` 通过 → `app-debug.apk`（57.6 MB）
- [x] `testDebugUnitTest` 25/25 通过
- [x] 6 种 StockChangeType 全覆盖
- [x] 事务回滚验证（amount 为正 / 库存不能为负）
- [x] ApproxDate 精度保持（YYYY-MM ≠ YYYY-MM-DD）
- [x] 默认药箱幂等创建 + 不可删
- [x] 删除药品级联 StockRecord + 删除照片
- [ ] 真机数据层验证（延至 S4 真机测试）

### 构建环境备注
- Kaspersky 文件系统过滤器阻止 Java `RandomAccessFile`/`FileOutputStream` 打开已存在的 `.lock` 文件 → 每次构建前需重命名 `native`/`daemon`/`caches` lock 文件和 `app/build` 目录。
- Kotlin 编译 daemon 无法启动（同样文件权限问题）→ 使用 `-Dkotlin.compiler.execution.strategy=in-process` 强制内进程编译。
- `debug.keystore.lock` 权限被拒 → `assembleDebug` 时用 `-x validateSigningDebug` 跳过签名验证（不影响 APK 打包）。

## S2 · 首页 + 药箱列表 — ✅ 完成

**状态：✅ 完成（`assembleDebug` 通过 + `testDebugUnitTest` 通过）**

### 完成内容
- 首页改为 4 层信息架构：Layer 1 需要关注（仅过期/临期）、Layer 2 药箱概况、Layer 3 最近添加、Layer 4 操作入口。
- 药品列表改为列表行而非 Card Grid，搜索为核心能力，支持药品名称 / 厂家 / 批号本地匹配。
- 新增 `ExpiryChecker`，统一 YEAR_MONTH 语义：`2028-03` 视为整月有效期，边界为 2028-03-31，绝不补成 `2028-03-01`。
- 搜索 Empty State、筛选 Empty State、Loading / Error / Data 状态已纳入设计与实现。
- 完成 `HomeViewModel`、`MedicineListViewModel`、`HomeScreen`、`MedicineListScreen`、导航接入。

### 验证结果
- `testDebugUnitTest`：通过。
- `assembleDebug`：通过。
- 代码质量检查：未引入 UI 直连 DAO、未在 UI 中做日期计算、未新增医疗判断逻辑。

## S3 · 相机取景 + ML Kit 中文 OCR — 待开始

**目标**：CameraX 取景，ML Kit `text-recognition-chinese` 识别说明书/包装文字。
**待办**：
- [ ] CameraX Preview + ImageAnalysis 绑定
- [ ] 中文 OCR 调用与结果解析
- [ ] 权限 State（Permission Denied 引导）
- [ ] OCR Failed State

## S4 · OCR 确认 + 药品详情 + 临期 + 盘点（MVP 真机验证）— Phase 1/2 完成

**目标**：OCR 结果确认表单 → 落库；药品详情、临期列表、盘点页；**真机跑通 MVP**。
**状态**：S4 Phase 1 = COMPLETE；S4 Phase 2 = COMPLETE；S4 Phase 3 = NOT STARTED。
**交付结果**：
- `testDebugUnitTest` = PASS
- `assembleDebug` = PASS
- APK 产物：`D:\Github\MediBox\app\build\outputs\apk\debug\app-debug.apk`
- 之前的 Gradle / daemon loopback connection 问题，已通过 Codex 「完整访问权限」后在真实 Windows 环境中完成验证并消除。
**待办**：
- [ ] Phase 3（保留未开始状态，不提前实现）

## S5 · 临期 / 库存提醒通知 — 待开始

**目标**：WorkManager 周期任务 + 系统通知，临期/缺货提醒。
**待办**：[ ] 提醒规则配置 [ ] WorkManager 调度 [ ] Notification 渠道

## S6 · 药品说明信息管理 — 待开始

**定位边界（医疗红线，永不越界）**：MediBox = 「家庭药品管理 / 药箱整理工具」，**不是**医疗咨询、疾病诊断、用药推荐、药物相互作用判断、自动剂量计算。App 不得自行推导任何医疗结论。

**允许保存 / 展示（均为用户侧信息）**：
- 用户手动录入的用法用量
- 用户从药品包装确认后录入的信息
- 用户保存的说明书内容
- 用户自己的备注
- 用户自己的禁忌备注

**禁止 App 自行判断**：
- 是否应该服用某药 / 应服多少 / 一天几次
- 两种药是否可以一起服用
- 某症状应使用什么药
- 某人是否适合某药

**未来说明书检索（如实现）**：仅能在「用户自己保存的说明书内容」中检索与定位；**不得把检索结果改写成医疗建议**。

**待办**：[ ] 说明信息录入/展示（纯用户字段） [ ] 详情页联动 [ ] 检索仅限本地已存内容，不改写为建议

## S7 · 盘点增强 — 待开始

**目标**：盘点流程优化（扫码/勾选、差异记录、历史）。
**待办**：[ ] 盘点记录持久化 [ ] 差异高亮

## S8 · 本地备份 / 恢复（SAF）— 待开始

**目标**：通过 Storage Access Framework 导出/导入 Room 数据（JSON/SQLite）。
**待办**：[ ] 导出 [ ] 导入 [ ] 冲突处理

## S9 · UI 细节打磨 + 反 AI 审美复检 — 待开始

**目标**：全量走查空/加载/错误/成功/权限/OCR 失败态，统一动效与间距。
**待办**：[ ] 全页反 AI 复检 [ ] 动效收敛 [ ] 无障碍

## S10 · 测试 + 正式 APK — 待开始

**目标**：补充测试，`assembleRelease` 产出正式包（需签名配置）。
**待办**：[ ] 单元测试 [ ] UI 走查 [ ] Release 签名 + 构建

---
_最后更新：S4 Phase 1 完成；S4 Phase 2 完成；`testDebugUnitTest` 与 `assembleDebug` 均已在真实 Windows 环境中 PASS；Phase 3 未开始。_

- 2026-08-22: S3 OCR 领域层收尾中，修复 TextNormalizer 的 Kotlin 转义错误，准备重新跑 testDebugUnitTest / assembleDebug。

- 2026-08-22: 完成 S4 MVP 设计审查，新增 `docs/S4_MVP_DESIGN.md`，明确无需新增 Entity / DAO / Migration，并给出 OCR 确认、详情、临期、盘点与真机验收设计。

- 2026-08-22: S4 Phase 1 已完成编码与 assembleDebug 验证；新增 OcrConfirmViewModel、OcrConfirmScreen 的候选确认与落库流程，并接入默认药箱。

- 2026-08-23: S4 Phase 2 最终交付完成；`testDebugUnitTest` 与 `assembleDebug` 在 Codex 完全访问权限的真实 Windows 环境中均 PASS，APK 产物为 `D:\Github\MediBox\app\build\outputs\apk\debug\app-debug.apk`，Phase 3 仍未开始。

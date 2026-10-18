# Design System · 药箱管家 MediBox

> 本文件是 UI 实现的唯一视觉规范来源。所有页面**只**引用这里的 Token 与组件原语，禁止在业务代码里写死颜色/字号/圆角/间距。
> 设计基调：**Premium · Minimal · Calm · Human · Warm · Trustworthy · Native**。
> 当前只做 Light 主题；Dark 主题在 S9 评估，结构已预留。

---

## 0. 第一性原则

1. **克制优于丰富**：能删的元素就删。
2. **留白优于装饰**：用间距建立层级，不用线框/底色堆砌。
3. **层级优于颜色**：靠字号、字重、位置表达重要度，不靠颜色。
4. **排版优于特效**：字体、行高、字距、对齐决定质感。
5. **每个元素都要通过质询**：「它帮助理解或操作了吗？」否则删除。
6. **绝对禁用**：AI 紫、紫蓝渐变、玻璃拟态、发光、霓虹、AI 星星、Emoji 当图标、巨型圆角、万物 Card、处处阴影、弹跳/粒子动画、默认 Material 3 紫、默认 Android Demo 风格。

---

## 1. Color（暖 · 中性 · 克制）

> 背景一律暖纸白；文字暖近黑；唯一品牌色是沉静鼠尾草绿，只作"点睛"；状态色只在"过期/临期/错误"出现，绝不铺满。

### 1.1 调色板（Light）

| Token | Hex | 用途 |
|---|---|---|
| `paper` | `#FAF7F2` | App 根背景（暖纸白，强反 AI 信号） |
| `surface` | `#FFFFFF` | 卡片/表单/弹层底 |
| `surfaceVariant` | `#F3EFE8` | 轻微背景变化（搜索框、选中底、分段控制） |
| `surfaceSunken` | `#EFEAE2` | 更深的凹陷底（输入框底、压按态） |
| `ink` | `#2B2722` | 主文字（暖近黑，非纯黑） |
| `inkSecondary` | `#6B635A` | 次级文字、图标默认色 |
| `inkTertiary` | `#9A9088` | 说明、元信息、占位符 |
| `inkDisabled` | `#C2B9AE` | 禁用态文字 |
| `divider` | `#E8E2D8` | 发丝分隔线 |
| `borderStrong` | `#D8D0C4` | 输入框/卡片描边（比 divider 略深） |
| `brand` | `#3F7A6B` | 唯一品牌色，仅主操作/选中/强调 |
| `brandSoft` | `#E4EFEA` | 选中/激活底色（如选中筛选项、品牌按钮按压） |
| `brandInk` | `#2C5A4E` | 品牌色之上的文字（个别场景） |
| `onBrand` | `#FFFFFF` | 品牌色按钮上的文字 |
| `danger` | `#B4473B` | 已过期 / 错误（砖红，非纯红） |
| `dangerSoft` | `#F6E7E4` | 危险态底色（如删除确认、过期标签底） |
| `warning` | `#B97A2E` | 临期（暖琥珀，非霓虹橙） |
| `warningSoft` | `#F6ECDB` | 临期态底色 |
| `ok` | `#5E7B5A` | "正常/在保"等极少场景（避免与品牌绿混淆，谨慎使用） |
| `okSoft` | `#E7EEE4` | 正常态底色 |
| `scrim` | `#2B2722` @ 32% | 弹层/对话框遮罩 |

### 1.2 使用纪律
- **品牌绿占比 < 8%** 屏幕面积：只出现在主按钮、选中态、扫描按钮、关键链接。
- **状态色只用于状态**：过期=danger，临期=warning；"正常/在库"用中性 `inkSecondary`，不用绿色铺满。
- **不用纯黑/纯白做文字**：文字用 `ink`/`inkSecondary`/`inkTertiary`。
- **背景层次**：页面=`paper`，局部容器=`surface`/`surfaceVariant`；层次靠底色差 + 发丝线，不靠阴影。
- **禁止**：渐变、半透明彩色叠加、玻璃模糊。

---

## 2. Typography（系统字体 · 原生中文阅读）

> 全部使用 `FontFamily.Default`（即设备系统字体，中文走系统默认中文字体），**不加载任何自定义/网络字体**。这套做法在中文机型上最"原生"，也最不像 AI Demo。

| Token | 字号 | 字重 | 字距 | 行高 | 用途 |
|---|---|---|---|---|---|
| `display` | 28sp | 600 | 0 | 1.2 | **仅**首页统计大数字（如「12」），不作标题 |
| `title` | 20sp | 600 | 0 | 1.25 | 页面标题 / 区块标题 |
| `titleSmall` | 17sp | 600 | 0 | 1.3 | 卡片/区块内小标题、列表主文字强调 |
| `section` | 15sp | 600 | 0 | 1.35 | 分组标签（如「临期提醒」） |
| `body` | 16sp | 400 | 0 | 1.45 | 列表主文字、正文 |
| `bodyStrong` | 16sp | 500 | 0 | 1.45 | 强调正文（如药品名称） |
| `secondary` | 14sp | 400 | 0 | 1.4 | 列表副文字、说明 |
| `caption` | 12sp | 400 | 0.2sp | 1.3 | 日期、元信息、脚注 |
| `label` | 13sp | 500 | 0.2sp | 1.3 | 输入标签、芯片文字 |

- 统计数字（display/含数字的 title）启用 `fontFeatureSettings = "tnum"`（等宽数字），对齐更稳。
- 中文不做负字距；标题可微调至 0。
- 行长：正文最大 ~38 中文字/行（约 600dp 内换行）。

---

## 3. Spacing（4dp 基准 · 温暖节奏）

| Token | dp | 用途 |
|---|---|---|
| `space1` | 4 | 图标与文字最小间隙 |
| `space2` | 8 | 紧密元素间隙 |
| `space3` | 12 | 列表行内元素间隙 |
| `space4` | 16 | 区块内默认间隙 |
| `space5` | 20 | **屏幕左右安全边距（screenPaddingX）** |
| `space6` | 24 | 区块之间垂直间距（sectionGap） |
| `space7` | 32 | 大区块之间 |
| `space8` | 40 | 首页顶部留白、空状态上下留白 |
| `space9` | 48 | 极大留白（首屏 hero 区） |

语义常量（代码里优先用这些）：
- `screenPaddingX = 20`
- `screenPaddingTop = 16`（ topBar 之下）
- `sectionGap = 24`
- `listRowVertical = 14`
- `itemGap = 12`

---

## 4. Radius（按层级，不统一 24）

| Token | dp | 用途 |
|---|---|---|
| `radiusXs` | 6 | 极小元素（标签内文字、细分隔块） |
| `radiusSm` | 8 | 输入框、芯片、小按钮、缩略图 |
| `radiusMd` | 12 | 列表项、标准卡片、按钮、表单组 |
| `radiusLg` | 16 | 底部弹层、对话框、大图容器 |
| `radiusPill` | 999 | 药丸（筛选芯片选中态、扫描按钮、分段控制） |

**纪律**：列表项与卡片用 `md 12`；不要 24；不要把所有东西都 pill。

---

## 5. Elevation（极轻 · 暖色阴影；优先发丝线）

> 默认**不用阴影**。用 `surfaceVariant` 底色差 + `divider` 发丝线表达层级。仅弹层/对话框/底部导航顶边使用极轻阴影。

| Token | 参数 | 用途 |
|---|---|---|
| `elevationNone` | 0 | 默认 |
| `elevationLow` | 1dp，`#2B2722` @ 8% alpha | 弹层、底部导航上沿 |
| `elevationMed` | 3dp，`#2B2722` @ 12% alpha | 对话框、底部抽屉 |

代码实现：Compose 中优先用 `surfaceVariant` + `divider`；确需阴影时用 `Modifier.shadow(elevation, RoundedCornerShape)` 配暖色 shadowColor，而非默认黑影。

---

## 6. Motion（自然 · 短 · 克制）

| Token | 值 | 用途 |
|---|---|---|
| `durationFast` | 150ms | 状态切换、淡入 |
| `durationNormal` | 200ms | 页面/弹层进出 |
| `durationSlow` | 250ms | 极少用（大图载入） |
| `easing` | `FastOutSlowIn` / `Standard`(easeOutCubic) | 全部动画统一缓动 |
| `slideDistance` | 16dp | 弹层/列表项位移 |
| `scaleInFrom` | 0.96 → 1.0 | 轻缩放（弹层/按钮按压反馈） |

允许类型：**淡入、位移动画(≤16dp)、轻缩放(0.96→1)、状态 crossfade、按压态底色变化**。
禁止：**弹跳、弹簧过冲、粒子、发光、旋转炫光、无故循环动画**。
原则：动画服务于"理解变化"，时长 ≤200ms，绝不拖慢操作。

---

## 7. Icon（统一一套 · 线性 · 克制）

- 来源：Material Symbols **Outlined**（`Icons.Outlined.*`，第一方），统一观感。
- 规格：默认 24dp；列表/行内 20dp；强调 28dp。线性、细描边。
- 着色：`tint = inkSecondary` 默认；选中/激活 `tint = brand`；状态相关用 `danger`/`warning`。
- **禁止**：Emoji 当图标；彩色填充图标；装饰性图标堆砌；每个页面图标数 ≤ 3（不含导航）。

| 场景 | 图标 |
|---|---|
| 首页 | `Icons.Outlined.Home` |
| 药箱 | `Icons.Outlined.Medication` / `Inventory2` |
| 扫描 | `Icons.Outlined.DocumentScanner` / `CameraAlt` |
| 盘点 | `Icons.Outlined.Checklist` / `FactCheck` |
| 我的 | `Icons.Outlined.Person` / `Settings` |
| 搜索 | `Icons.Outlined.Search` |
| 添加 | `Icons.Outlined.Add` |
| 编辑 | `Icons.Outlined.Edit` / `ChevronRight` |
| 删除 | `Icons.Outlined.DeleteOutline` |
| 通知 | `Icons.Outlined.Notifications` |
| 备份 | `Icons.Outlined.Backup` |
| 空/错误 | `Icons.Outlined.Medication`(空) / `ErrorOutline`(错) / `CloudOff`(无权限/失败) |

---

## 8. 组件原语（在 `ui/components` 实现，全部引用上述 Token）

| 组件 | 形态要点 | 关键 Token |
|---|---|---|
| `AppScreen` | Scaffold：TopBar + 内容(`screenPaddingX`) + BottomNav；内容可滚动 | paper, screenPaddingX |
| `AppTopBar` | 左标题(`title`)，右侧最多 2 个图标动作；**无背景色、无阴影**，底部 `divider` 发丝线 | divider, title, inkSecondary |
| `AppListItem` | 可选 leading(图标/缩略图) + 主文字(`bodyStrong`) + 副文字(`secondary`) + trailing + 底部 `divider`；纵向 `listRowVertical` | divider, bodyStrong, secondary |
| `AppButton.Primary` | 品牌绿填充，`radiusMd`，文字 `onBrand`；按压 `brandSoft`/`surfaceSunken` | brand, onBrand, radiusMd |
| `AppButton.Secondary` | `surfaceVariant` 底 + `ink` 文字，或品牌描边 | surfaceVariant, borderStrong |
| `AppButton.Ghost` | 纯文字按钮，`brand` 文字，无底色 | brand |
| `AppChip`(筛选) | `radiusPill`，`surfaceVariant` 底 + `ink` 文字；选中=`brandSoft` 底 + `brand` 文字 + 细 `brand` 描边 | radiusPill, brandSoft, brand |
| `AppTextField` | `radiusMd`，`surface` 或 `surfaceSunken` 底，`borderStrong` 描边；label(`label`)、placeholder(`inkTertiary`) | radiusMd, borderStrong, inkTertiary |
| `AppSwitch` | 开启=`brand` 轨道 | brand |
| `AppTag` | 小状态标签，`radiusXs/sm`，`warningSoft`+`warning` 或 `dangerSoft`+`danger` 或中性 `surfaceVariant`+`inkSecondary` | 对应 soft/color |
| `AppStat` | 大数字(`display`) + 标签(`caption`)；用于首页统计 | display, caption |
| `AppEmptyState` | 居中：线性图标(`inkTertiary`) + 标题(`titleSmall`) + 说明(`secondary`) + 可选主操作 | inkTertiary, titleSmall |
| `AppStateView.Loading` | 居中细 spinner（`inkSecondary`），无文字或一行 `secondary` | inkSecondary |
| `AppStateView.Error` | `ErrorOutline`(`danger`) + 消息(`secondary`) + 重试按钮 | danger, secondary |
| `AppStateView.Success` | 对勾(`ok`) + 消息(`secondary`)；短暂展示后消失 | ok, secondary |
| `AppStateView.Permission` | `CloudOff`/`Camera` 图标 + 说明 + 去授权按钮 | brand, secondary |
| `AppStateView.OcrFailed` | 说明 + 「手动录入」按钮（**保留主路径**） | secondary, brand |
| `AppBottomNav` | 5 项等宽；选中=`brand` 图标+文字；**无顶部阴影，仅 1dp `divider` 上沿**；扫描居中为品牌圆按钮 | divider, brand, radiusPill |
| `AppSheet` / `AppDialog` | `radiusLg`，`surface` 底，`scrim` 遮罩，`elevationMed` | radiusLg, scrim, elevationMed |

---

## 9. 反模式对照（实现时逐条对照）

| ❌ 禁止 | ✅ 改为 |
|---|---|
| 默认 M3 紫色/动态取色 | 全量重主题化为 §1 暖纸调色板 |
| 万物 Card + 黑阴影 | 列表用留白 + `divider` + `surfaceVariant` |
| 统一 24dp 圆角 | 按层级 8/12/16/pill |
| 巨型 Hero 标题「我的药箱」 | `title`(20sp) 克制；大数字仅用于统计 |
| 空态「欢迎使用智能药品管理平台」 | 「你的药箱还是空的」+「扫描第一件药品」 |
| 状态色铺满 | 仅过期/临期用 danger/warning，其余中性 |
| Emoji 当图标 | 统一线性 `Icons.Outlined` |
| 「AI 智能识别」「赋能」 | 「扫描药品」「库存」 |
| 弹跳/粒子/发光动画 | 150–200ms 淡入/位移/轻缩放 |
| 渐变/玻璃/霓虹 | 纯色平涂 + 发丝线 |

---

## 10. 主题落地说明（给工程）

- Material 3 仅作**引擎**：我们提供**完全重写的 `ColorScheme`**（用 §1 Token 覆盖所有 M3 颜色角色），自定义 `Typography`/`Shapes`，并优先用 §8 自研组件而非默认 M3 脚手架。
- 目标：编译产物在视觉上**读不出 Material 3 默认紫**，也读不出 Android Demo。
- Token 以 Compose `object`/`val` 暴露（`AppColor`、`AppSpacing`、`AppRadius`、`AppMotion`），组件只引用它们。
- 颜色集中定义在 `ui/theme/Color.kt`；Typography 在 `Type.kt`；其余在 `Theme.kt`/`Spacing.kt`/`Radius.kt`/`Motion.kt`。

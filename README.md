# 生活回响 · LifeTimeRE

将真实生活记录转化为角色成长的 Android 应用。

## 当前进度

里程碑 A“可留下旅程”、B“第一次成长”和 C“营地回应”已经完成：Android 工程、像素主题、四页导航、本地旅志、可配置的真实 AI 回响、固定规则成长与可追溯世界变化形成了第一条完整游戏循环。

- 今日：全天分笔记录、时间线、单笔编辑和删除、按用户设置的晚间时间自动汇总、手动提前汇总、AI 回响与设置入口。

真实 AI 网关的运行与应用配置说明见 [docs/AI_INTEGRATION.md](docs/AI_INTEGRATION.md)。

当前个人版为 0.3.0：记录入口接纳完成、尝试、调整与休息；保存后立即显示营地足迹反馈；AI 汇总失败原因会保存在本机；回顾页按自然周展示带逐字原文引用的本周旅程，并可打开当天全文。安装后在“今日 → 设置”启用 AI，选择“DeepSeek 直连”，输入自己的 API Key，测试连接并保存。构建包没有注入供应商 Key。

从当前源码运行 `.\gradlew.bat :app:assembleDebug` 可生成 `app/build/outputs/apk/debug/app-debug.apk`。世界页另有营地、帐篷、工坊和小径的美术交互演示；工坊循环使用九张完整画帧，演示内容暂不读写旅志。
- 角色：五项属性的真实账本汇总，点击属性可查看来源日期、旅志和依据；等级进度尚未开放。
- 世界：由有效结算日期推导的三态营地，篝火、路标和工坊可回溯来源；支持可选、可修改的冒险方向。
- 回顾：按日期展示已保存旅志及回响状态，支持编辑与确认删除。

旅志使用 Room 保存在本机，包含当地日期、时区、创建与修改时间及修订号；应用重启后仍可恢复。冒险方向同样保存在本机，但不参与属性结算。
已接入真实 AI，支持在设置页自行输入 DeepSeek API Key 直连，也支持自建网关；构建包不注入供应商 Key。白天每笔记录先保存本机，晚间将所选日期的全部正文发送 AI 分析；最终点数由版本化规则计算，单属性每日最多 3 点、全天最多 8 点。补记或修改后原收益保留到新结算成功，再原子替换，避免重复加点；删除整天或当天最后一笔会同步删除该日贡献。记录与结算设计见 [docs/DAILY_CAPTURE_AND_SETTLEMENT.md](docs/DAILY_CAPTURE_AND_SETTLEMENT.md)。

## 开发环境

- Android Studio，使用支持 AGP 8.7 的版本。
- Android SDK Platform 35。
- JDK 17 或 21。本机可选 `D:\JDK`（21）；如果 Android Studio 自带 JDK 25，请在 Settings → Build, Execution, Deployment → Build Tools → Gradle 中将 Gradle JDK 改成 17 或 21。
- Gradle 8.9 all 发行包（通过项目 Wrapper 自动获取，包含 Android Studio Kotlin DSL 所需源码）。
- 最低 Android 8.0（API 26）。

在 Android Studio 选择 Open，打开克隆后的项目目录，等待 Gradle 同步完成，选择模拟器或真机后运行 `app`。
本地 SDK 路径放在被 Git 忽略的 `local.properties`，其他电脑由 Android Studio 自动生成。

```powershell
$env:JAVA_HOME = 'D:\JDK' # 仅本机；其他电脑填写自己的 JDK 17 / 21 路径
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

首次构建需要下载 Gradle 和依赖。Gradle all 分发包使用华为云镜像，并校验官方 SHA-256；Google Maven、Maven Central 优先使用阿里云镜像，并保留官方仓库和 Gradle Plugin Portal 作为后备。使用 all 包可避免 Android Studio 额外从官方站下载 `gradle-8.9-src.zip` 时因网络超时导致同步失败。

## 后续顺序

1. 开展七日体验验证，观察记录成本、成长解释和营地反馈。
2. 根据试玩结果完善次日线索、周回顾和晚间结算反馈。
3. 完善后台失败状态、可配置汇总时间与线上服务运维。

领域属性定义位于 `domain`，Room 与 Repository 位于 `data`，界面位于 `ui`。Compose 页面通过 ViewModel 使用数据，不直接读写数据库。

页面预览入口为 `ui/ScreenPreviews.kt`；开发范围与后续验收要求见 `docs/MVP.md`。

## 已知工具限制

Navigation 2.8.5 的部分附带 lint 规则与 AGP 8.7 的 lint API 不兼容，会产生 `ObsoleteLintCustomCheck` 警告并跳过相应导航专项检查。常规 lint 检查仍运行；当前使用字符串路由，不使用受影响的类型安全路由功能。未屏蔽这些警告。

## 本轮验证（2026-11-01）

- `:app:assembleDebug`：通过，生成 `app/build/outputs/apk/debug/app-debug.apk`。
- `:app:lintDebug`：通过，0 个错误；仍有 3 个导航库附带检查兼容性警告。
- `Pixel_8_API_35`：已验证保存、强制结束后恢复、历史编辑、修订号递增、确认删除、冒险方向保存/恢复/清除和页面布局；设备端集成测试还覆盖重复结算、并发、修订、过期响应、零成长、重试、级联删除、营地三态及回退。世界物件的来源列表和旅志详情已在模拟器完成实际点击检查。
- 本机 IDE 的项目 Gradle JDK 已通过被忽略的 `.idea/gradle.xml`、`.gradle/config.properties` 指向 `D:/JDK`，不影响其他工程。

## 随手记录版本验证（2026-11-03）

- 11 项 JVM 单元测试通过；API 35 模拟器 24 项设备测试通过，包含 Room 4 → 5 成长账本迁移、多笔记录、补记替换、并发结算、过期响应、失败回滚与 5000 字上限。
- Lint 0 个错误、4 个既有警告；其中 3 个是 Navigation 自定义检查与当前 Lint API 的兼容性提示，另 1 个是既有 Compose Modifier 参数顺序提示。
- `daily_review_v5` / `daily_review_eval_v3` 静态检查通过。真实 DeepSeek 批量评测 19/32 得到响应并全部通过，其余 13 条因 502 或超时未评分；新增的 3 条多笔记录去重样本全部通过。不得把本轮结果写成 32/32。
- 内测 APK 已在 API 35 模拟器覆盖安装并启动，无崩溃；手动保存一笔后界面正确显示“已记录 1 笔”和 22:00 待汇总状态。

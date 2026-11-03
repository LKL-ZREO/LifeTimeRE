# 真实 AI 回响接入

日期：2026-11-03  
状态：已实现应用接入层、版本化分析协议、可部署网关、本地评测 harness，以及在设置页输入 Key 的 DeepSeek 直连方式。

## 架构

设置页可选择 DeepSeek 直连或自建网关。直连时使用者输入自己的 DeepSeek API Key，使用 Android Keystore 加密保存在本机应用设置中；APK 不包含构建时注入的 Key。网关模式保存网关地址与访问令牌，供应商 Key 留在网关服务端。生成回响时，应用把当前旅志正文、日期和修订号发送到所选服务；最终点数仍由应用内 `GrowthRules` 计算。

DeepSeek 直连固定使用 `https://api.deepseek.com` 与 `deepseek-flash`。设置页可先测试当前输入的 Key，再保存配置；关闭 AI 后仍可本地记录。

当前每日回顾协议使用 `daily_review_v5` Prompt 与 `daily_review_output_v1` Schema。网关响应同时返回模型、Prompt 版本、Schema 版本和分析时间；应用把这些字段与固定规则版本一起保存在 Room v5 中。v5 Prompt 增加全天多笔记录的去重、进度合并与后文明确更正处理。周回顾不得复用这个 Prompt 或输出 Schema，后续应作为独立任务实现。

客户端与服务端都会验证：属性 ID、强度 1–3、行动 ID 唯一、引用确实存在于原文、字段长度和行动数量。直连与网关在 Android 端共用 `ReviewResultParser`，拒绝字符串或小数强度、额外奖励字段、缺失字段和非文本内容。修改旅志会产生新修订号，旧请求无法覆盖新记录。

## 设置页直连 DeepSeek

正常构建即可，不再需要 `LIFETIMERE_DEEPSEEK_API_KEY` 环境变量：

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest
```

输出为 `app/build/outputs/apk/debug/app-debug.apk`。安装后在“今日 → 设置”选择“DeepSeek 直连”，输入自己的 Key，点击“测试连接”并保存。不要把实际 Key 写进文档、Gradle 源码或版本库。

白天保存每笔足迹只写入本机；默认每天 22:00 后由 WorkManager 汇总全天，也可在 18:00–23:30 间按 30 分钟一档调整，并可手动提前汇总。设定时间后补记或修改会触发当天重算，以新结果替换旧收益，失败时保留上次有效结果。详见 [随手记录与晚间结算](DAILY_CAPTURE_AND_SETTLEMENT.md)。模型使用 `deepseek-flash`，JSON Output、temperature 0、reasoning_effort none、max_tokens 2000；连接超时 10 秒、读取超时 60 秒。手机端 Prompt 与网关 `daily_review_v5` 保持一致。模型只负责识别行动，点数仍由本地固定规则结算，每项每日最多 3 点、每日合计最多 8 点。无完成行动时，允许零点回响。

网络或校验失败时旅志仍在本机保存，玩家可重试；有效回响和属性结算写入本地数据库，重启后保留。内测版会将当前分析所需的正文、日期和修订号发给 DeepSeek，不上传整本历史旅志。

2026-11-01 内测验证：8 项单元测试、15 项 API 35 Android 测试通过，后者包含 7 项解析器测试，覆盖类型错误、伪造引用、额外奖励字段、重复 ID、行动数量上限和网关兼容。最终构建与 Lint 无错误；Lint 仍有 3 条 Navigation 依赖检查器兼容性警告（部分自定义检查被跳过），以及 1 条既有 Modifier 参数顺序警告。

API 35 模拟器实际直连验证通过：设置页连接成功；跑步与阅读识别为力量 +1、学识 +1；重启后结果仍在；修改为仅计划未来行动后返回零行动，角色合计从 17 回到初始 15；本次临时旅志已通过应用内删除入口清理。还修复并验证了世界页返回今日时恢复错误页面的问题。上述测试未运行电脑端 AI 网关。

## 本机运行网关

需要 Node.js 20 或更新版本。在 PowerShell 中进入 `services/review-gateway`，选择一个供应商并只为当前终端设置环境变量。

OpenAI：

```powershell
$env:AI_PROVIDER = "openai"
$env:AI_MODEL = "gpt-5.4-mini"
$env:OPENAI_API_KEY = "你的 OpenAI API Key"
$env:GATEWAY_TOKEN = "一段足够长的随机令牌"
$env:PORT = "8787"
npm start
```

DeepSeek：

```powershell
$env:AI_PROVIDER = "deepseek"
$env:AI_MODEL = "deepseek-flash"
$env:DEEPSEEK_API_KEY = "你的 DeepSeek API Key"
$env:DEEPSEEK_BASE_URL = "https://api.deepseek.com"
$env:GATEWAY_TOKEN = "一段足够长的随机令牌"
$env:PORT = "8787"
npm start
```

网关模式的 API Key 只设置在服务端环境中，不要写入仓库或 Android 设置。OpenAI 分支设置 `store: false`；DeepSeek 分支使用 JSON Output，并由网关执行字段校验。网关自身不记录旅志正文。生产部署还需要在基础设施层关闭请求正文日志，并使用 HTTPS。

## Android 模拟器配置

打开“今日”右上角的设置按钮：

- 启用 AI 回响。
- 服务地址填写 `http://10.0.2.2:8787`。
- 访问令牌填写与 `GATEWAY_TOKEN` 相同的值。
- 点击“测试连接”，看到模型名称后保存。

`10.0.2.2` 只用于 Android 模拟器连接开发电脑。真机或发布版本应填写部署后的 HTTPS 地址。

## 接口约定

- `GET /health`：验证网关令牌、模型名称和供应商密钥是否已配置。
- `POST /v1/reviews/analyze`：输入旅志，返回 `summary` 与 `actions`。
- 每个 action 包含 `id`、`attribute`、`intensity`、逐字 `quote` 和 `explanation`。
- 响应 `metadata` 包含 `model`、`promptVersion`、`schemaVersion` 与 `analyzedAtEpochMillis`。

提示词只允许识别已经发生且有原文依据的行动。计划、愿望、泛泛感受和未完成事项不会直接加点；AI 不输出最终属性点。客户端与网关都拒绝原文中找不到的引用。

## 评测 harness

`services/review-gateway/evals/daily-review-cases.json` 是版本化测试集。当前 `daily_review_eval_v3` 包含 32 个中文样本，覆盖计划、否定、未完成、休息、重复描述、部分完成、多行动、属性边界、强度边界、提示注入，以及多笔记录中的计划转完成、进度时长与明确更正。

不连接模型时，先验证数据集、证据锚点和脚本：

```powershell
cd services/review-gateway
npm run check
npm run eval
```

启动已配置模型密钥的网关后运行完整回归：

```powershell
$env:REVIEW_GATEWAY_URL = "http://127.0.0.1:8787"
$env:GATEWAY_TOKEN = "与网关相同的访问令牌"
npm run eval:live
```

在线报告逐样本统计漏识别、错误增加的行动、属性不匹配、强度不匹配和版本元数据错误。修改 Prompt、Schema 或模型后必须使用同一数据集比较，不允许只凭少数演示文本判断精度。

2026-11-01 使用 `deepseek-flash`、`daily_review_v4` 与 `daily_review_eval_v2` 的一次完整回归为 29/29，通过报告保存在 `services/review-gateway/deepseek-flash-eval-report-final.json`。该结果只表示当前小型回归集通过，不代表真实用户文本已达到同等准确率。

2026-11-03 的 v5 验证使用 `daily_review_eval_v3` 共 32 条：静态结构检查通过；真实 `deepseek-flash` 批量调用中 19 条得到有效响应且全部满足预期，13 条未得到可评分结果（10 条上游 502、3 条 60 秒超时），因此本次在线报告的总通过率是 19/32，不能表述为完整回归通过。新加入的“计划后完成只算一次”“同一次进度不叠加时长”“后文明确更正”3 条均得到有效响应并通过。原始报告保存在 `services/review-gateway/deepseek-flash-eval-v5-report.json`，后续应在服务稳定时重跑未完成样本。

## 玩家纠错

已完成回响提供“判断不准确”入口。玩家可以修改旅志后生成新修订，或选择问题类型并请求按原文重新分析；玩家不能手动指定属性和点数。异议会与原模型及 Prompt/Schema 版本一起保存在本机。重新分析期间旧回响仍有效，新结果只有在结构、引用和旅志修订号全部通过校验后才会原子替换；重新分析失败不会清空原属性。

## 发布前工作

- 将网关部署到 HTTPS 服务，使用密钥管理服务保存供应商 Key 和网关令牌。
- 为每位用户或设备签发可撤销的短期令牌，替换试玩阶段的共享令牌。
- 增加请求频率限制、用量监控与正文脱敏日志策略。
- 将经过用户同意并完成匿名化的纠错样本加入评测集，建立 Prompt 版本的回归基线。

接口格式依据 [OpenAI Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs)、[OpenAI Datasets 评测指南](https://developers.openai.com/api/docs/guides/evaluation-getting-started)、[DeepSeek JSON Output](https://api-docs.deepseek.com/guides/json_mode/) 和 [DeepSeek Chat Completions](https://api-docs.deepseek.com/api/create-chat-completion/)。


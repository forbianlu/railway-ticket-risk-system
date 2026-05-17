# 项目开发日志

## 阶段提交索引

| 阶段 | 提交 | 说明 |
| --- | --- | --- |
| 订单支付状态机与超时关闭 | `7403c58 add order payment state machine` | 创建订单进入待支付、支付成功、关闭、超时关闭、退票和库存释放 |
| 订单分页筛选与运营看板指标增强 | `caf77b0 add order filtering and dashboard metrics` | 订单多条件分页查询，运营看板补充状态统计、退票率、风险率 |
| 支付流水表与支付回调幂等 | `b40b735 add payment records and callback idempotency` | 支付流水、模拟支付回调、`callbackRequestId` 幂等 |
| 风险处置闭环增强 | `0fc4543 enhance risk handling workflow` | 风险状态、处置备注、处理人、处理时间、处置历史和权限控制 |
| 风险事件分页筛选与风险运营报表增强 | `1f77ac7 add risk query pagination and summary` | 风险分页筛选、风险运营报表、前端风险统计展示 |

## 2026-05-15 风险处置闭环增强

### 本轮任务目标

将风控模块从“生成风险 + 简单标记已处理”升级为完整的风险处置闭环，支持风险状态、处置备注、处理人、处理时间、处置历史、权限控制和看板联动。

### 开发前状态

- 订单支付状态机与超时关闭已提交：`7403c58 add order payment state machine`。
- 订单分页筛选与运营看板指标增强已提交：`caf77b0 add order filtering and dashboard metrics`。
- 支付流水表与支付回调幂等已在本轮开始前测试通过并提交：`b40b735 add payment records and callback idempotency`。
- 工作区仅保留未跟踪规划文件 `docs/project-context-for-gpt.md`。
- 原风险事件主要依赖 `handled` 布尔值，缺少处置结论、备注、处理人、处理时间和历史记录。

### 实际修改文件

- 后端领域模型：`RiskStatus`、`RiskEvent`、`RiskEventHandleRecord`
- 后端仓储：`RiskEventRepository`、`RiskEventHandleRecordRepository`
- 后端 DTO：`RiskHandleRequest`、`RiskEventResponse`、`RiskEventHandleRecordResponse`
- 后端服务与接口：`RiskService`、`RiskController`、`DashboardService`
- 前端：`frontend/index.html`、`frontend/app.js`、`frontend/styles.css`
- 测试：`RailwayApiIntegrationTests`
- 文档：`README.md`、`docs/api-design.md`、`docs/database-design.md`、`docs/er-diagram.mmd`、`docs/project-outline.md`、`docs/resume-and-interview.md`、`docs/risk-handling-design.md`

### 新增功能

- 风险事件状态机：`PENDING`、`CONFIRMED`、`FALSE_POSITIVE`、`CLOSED`
- 风险事件列表支持按状态和场景筛选。
- 风险处置支持提交处置状态和备注。
- 风险事件保存最新处理人、处理时间、处理备注和关闭时间。
- 新增风险处置历史表，记录每次状态流转。
- 新增查询风险处置历史接口。
- 看板未处理风险数改为基于 `PENDING` 状态统计。
- 前端支持选择处置结论、填写备注、查看处置历史。

### 风险状态设计

- `PENDING`：系统规则生成后等待人工审核。
- `CONFIRMED`：风控人员确认存在风险。
- `FALSE_POSITIVE`：风控人员判断为误报。
- `CLOSED`：事件已关闭归档。

保留 `handled` 字段兼容旧逻辑，当前语义为 `status != PENDING`。

### 处置历史设计

`risk_event_handle_records` 记录：

- 风险事件 ID
- 处置前状态
- 处置后状态
- 处置备注
- 操作人
- 操作时间

历史表用于审计追踪，风险事件表保存当前最新状态。

### 接口设计

- `GET /api/risks?status=PENDING&scene=ORDER_CREATED`
- `POST /api/risks/{id}/handle`
- `GET /api/risks/{id}/handle-records`

`POST /api/risks/{id}/handle` 请求体：

```json
{
  "status": "CONFIRMED",
  "remark": "短时间多次购票，确认存在异常购票行为"
}
```

### 前端变化

- 风险列表新增状态筛选和场景筛选。
- 风险卡片展示状态、处理备注、处理人和处理时间。
- 可选择 `CONFIRMED`、`FALSE_POSITIVE`、`CLOSED` 作为处置结果。
- 可输入处置备注。
- 可查看某个风险事件的处置历史。

### 测试结果

已执行 Maven 测试，结果通过：

- `Tests run: 20`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`

前端 JS 语法检查已通过：`node --check frontend\app.js`。

### 当前提交状态

上一轮支付流水成果已提交为 `b40b735`。本轮“风险处置闭环增强”尚未提交，建议确认后使用：

```text
enhance risk handling workflow
```

### 后续建议

- 为风险事件增加分页查询。
- 为高风险事件增加分配处理人和处理 SLA。
- 增加风险事件导出和审核报表。
- 后续可把处置流程扩展为“初审、复核、归档”的多级审核。

## 2026-05-15 风险事件分页筛选 + 风险运营报表增强

### 本轮任务目标

将风险事件查询从最近 50 条升级为分页查询，并新增风险运营报表，让风控模块具备按状态、场景、用户、订单号、日期筛选和按状态、场景统计分析的能力。

### 开发前状态

- 订单支付状态机与超时关闭已提交：`7403c58 add order payment state machine`。
- 订单分页筛选与运营看板指标增强已提交：`caf77b0 add order filtering and dashboard metrics`。
- 支付流水表与支付回调幂等已提交：`b40b735 add payment records and callback idempotency`。
- 风险处置闭环增强已在本轮开始前测试通过并提交：`0fc4543 enhance risk handling workflow`。
- 工作区仍保留未跟踪规划文件 `docs/project-context-for-gpt.md`，本轮未纳入功能修改。

### 实际修改文件

- 后端接口：`RiskController`
- 后端服务：`RiskService`
- 后端仓储：`RiskEventRepository`
- 后端 DTO：`RiskEventPageResponse`、`RiskSummaryResponse`
- 测试：`RailwayApiIntegrationTests`
- 前端：`frontend/index.html`、`frontend/app.js`、`frontend/styles.css`
- 文档：`README.md`、`docs/api-design.md`、`docs/project-outline.md`、`docs/resume-and-interview.md`、`docs/risk-handling-design.md`、`docs/project-development-log.md`

### 新增功能

- `GET /api/risks` 返回分页对象。
- 风险事件支持按状态、场景、用户 ID、订单号、创建日期筛选。
- 新增 `GET /api/risks/summary` 风险运营报表。
- 前端风险列表支持筛选、重置、上一页、下一页和分页元信息展示。
- 前端新增风险运营报表卡片和按状态、按场景统计展示。

### 风险分页查询设计

分页参数为 `page` 和 `size`，默认 `page=0`、`size=10`，每页最大 100 条。后端使用 `JpaSpecificationExecutor` 动态组合 `status`、`scene`、`userId`、`orderNo`、`fromDate`、`toDate` 条件，并按 `createdAt`、`id` 倒序排序。

分页响应包括：

- `content`
- `page`
- `size`
- `totalElements`
- `totalPages`
- `first`
- `last`

非法状态、非法页码或非法页大小返回 400。

### 风险运营报表设计

`GET /api/risks/summary` 返回：

- `totalRiskCount`
- `pendingRiskCount`
- `confirmedRiskCount`
- `falsePositiveRiskCount`
- `closedRiskCount`
- `pendingRate`
- `confirmedRate`
- `falsePositiveRate`
- `closedRate`
- `handlingCompletionRate`
- `averageHandleMinutes`
- `riskCountByScene`
- `riskCountByStatus`

比例字段统一使用 `max(1, totalRiskCount)` 防止除以 0。`handlingCompletionRate` 表示非 `PENDING` 风险占比，`averageHandleMinutes` 基于已处置事件的 `handledAt - createdAt` 计算。

### 前端变化

- 风险筛选区新增用户 ID、订单号、开始日期、结束日期。
- 风险列表新增分页控件和总条数展示。
- 风险运营报表展示总风险、待处理、确认风险、误报、关闭归档、确认风险占比、误报率、处置完成率。
- 新增按风险状态和风险场景聚合展示。
- 保留风险处置、处置备注和处置历史交互。

### 测试结果

已执行 Maven 测试，结果通过：

- `Tests run: 21`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`

前端 JS 语法检查已通过：`node --check frontend\app.js`。

### 当前提交状态

上一轮风险处置闭环成果已提交为 `0fc4543`。本轮“风险事件分页筛选 + 风险运营报表增强”尚未提交，建议确认后使用：

```text
add risk query pagination and summary
```

### 后续建议

- 增加风险事件导出 CSV。
- 为高风险事件增加处理 SLA 和超时提醒。
- 支持按风险等级、处理人继续筛选。
- 后续可增加风险趋势图和日报统计。

## 2026-05-17 项目收尾体检与 GitHub 展示优化

### 本轮任务目标

对项目做收尾体检，确认上一轮风险分页报表成果已提交；检查 README、docs、图片资源、API 文档、数据库文档、ER 图、简历面试材料、前端 JS 和 Maven 测试；优化 GitHub 展示效果，并新增最终总结文档。

### 开发前状态

- 订单支付状态机与超时关闭已提交：`7403c58 add order payment state machine`。
- 订单分页筛选与运营看板指标增强已提交：`caf77b0 add order filtering and dashboard metrics`。
- 支付流水表与支付回调幂等已提交：`b40b735 add payment records and callback idempotency`。
- 风险处置闭环增强已提交：`0fc4543 enhance risk handling workflow`。
- 风险事件分页筛选与风险运营报表增强在本轮开始时尚未提交。
- 工作区存在未跟踪规划文件 `docs/project-context-for-gpt.md`，本轮继续不纳入核心功能提交。

### 是否提交上一轮风险分页报表功能

已在 Maven 测试通过后提交上一轮风险分页报表成果：

```text
1f77ac7 add risk query pagination and summary
```

### 本轮检查了哪些内容

- README 中引用的文档和图片是否存在。
- docs 目录中的 Markdown 相对链接是否存在。
- docs/assets 下 README 引用的 SVG 和截图是否存在。
- API 文档是否覆盖订单、支付、风险、看板、缓存、权限和日志接口。
- 数据库设计和 ER 图是否包含当前主要表。
- 简历面试文档是否包含状态机、防超卖、幂等、支付回调、风控、缓存、权限和测试亮点。
- project-development-log 是否包含最近几轮开发记录。
- 前端 `frontend/app.js` 是否通过语法检查。
- 后端 Maven 测试是否通过。

### README 做了哪些优化

- 增加项目简介，明确项目面向铁路局、银行科技岗和央国企软件岗。
- 强调当前是后端主导型交易与风控系统，避免误导为真实支付、Redis、MQ 或 Spring Security 项目。
- 重新组织核心亮点、技术栈、系统架构与流程、功能模块、快速启动、测试方式、接口概览、数据库核心表、简历写法和面试可讲点。
- 补充当前测试数量和前端脚本检查命令。
- 新增最终总结文档入口。

### docs 做了哪些一致性修复

- 修正 `docs/api-design.md` 中订单分页响应说明的位置，使其回到“查询订单”章节。
- 确认 `docs/database-design.md` 和 `docs/er-diagram.mmd` 已包含 `app_users`、`stations`、`trains`、`seat_inventories`、`ticket_orders`、`payment_records`、`risk_events`、`risk_event_handle_records`、`operation_logs`。
- 确认 `docs/resume-and-interview.md` 已包含订单状态机、并发防超卖、订单幂等、支付回调幂等、风控规则引擎、风险处置闭环、查询缓存、权限审计和集成测试。
- 在本文件顶部追加阶段提交索引，串联最近五轮核心开发成果。

### 是否新增 final-project-summary.md

已新增 `docs/final-project-summary.md`，用于项目复习和面试准备。

### 测试结果

Markdown 链接扫描通过，未发现失效相对链接。

前端 JS 语法检查通过：

```text
node --check frontend\app.js
```

Maven 测试通过：

```text
Tests run: 21
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

### 当前提交状态

上一轮风险分页报表成果已提交。本轮“项目收尾体检与 GitHub 展示优化”尚未提交，建议确认后使用：

```text
polish docs and project showcase
```

### 后续建议

- 提交本轮文档收尾改动后再 push 到 GitHub。
- GitHub README 首页可配合截图展示管理台核心页面。
- 简历中保留 3 到 5 个最强亮点，面试时再展开支付幂等、状态机、风控闭环和并发防超卖。

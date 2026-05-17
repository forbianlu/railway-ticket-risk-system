# 项目开发日志

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

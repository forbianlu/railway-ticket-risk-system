# 风险处置闭环设计

## 为什么需要风险处置闭环

风控规则只能完成“发现风险”，后台运营还需要完成“审核风险、记录结论、留痕追溯”。如果风险事件只有一个 `handled` 布尔值，系统只能表达是否处理过，无法区分确认风险、误报和关闭归档，也无法说明处理人、处理时间和处理理由。

因此本轮将风险模块升级为更完整的风控运营闭环：

支付成功或退票 -> 触发风控规则 -> 生成风险事件 -> 风控人员处置 -> 写入处置历史 -> 写入操作日志 -> 看板统计待处理风险。

## 风险事件状态

新增 `RiskStatus`：

| 状态 | 含义 |
| --- | --- |
| PENDING | 系统规则生成后等待人工审核 |
| CONFIRMED | 风控人员确认该事件确实存在风险 |
| FALSE_POSITIVE | 风控人员判断该事件为误报 |
| CLOSED | 事件已完成处置并归档 |

保留 `handled` 字段用于兼容旧前端和旧响应。当前约定：`status == PENDING` 时 `handled=false`，其他状态 `handled=true`。

## 处置信息

`risk_events` 增加：

- `status`：当前风险状态。
- `scene`：触发场景，例如支付成功后或退票后。
- `handleRemark`：最新处置备注。
- `handledBy`：最新处理人，取当前登录用户。
- `handledAt`：最新处理时间。
- `closedAt`：关闭归档时间。

这些字段让列表页能直接展示当前处置结论，也便于后台运营人员跟进风险事件。

## 处置历史表

新增 `risk_event_handle_records`，记录每次处置动作：

- `riskEventId`
- `fromStatus`
- `toStatus`
- `remark`
- `operatorName`
- `operatedAt`

当前状态用于快速查询，历史表用于审计追踪。两者配合后，可以回答“这个风险事件现在是什么状态”和“它是如何一步步变成这个状态的”。

## 状态流转规则

允许流转：

- `PENDING -> CONFIRMED`
- `PENDING -> FALSE_POSITIVE`
- `PENDING -> CLOSED`
- `CONFIRMED -> CLOSED`
- `FALSE_POSITIVE -> CLOSED`

禁止流转：

- `CLOSED` 不允许重复处置。
- 不允许处置回 `PENDING`。
- `CONFIRMED` 不允许改回 `FALSE_POSITIVE`，避免审核结论来回跳转导致审计语义混乱。

如果早期前端仍调用无请求体的 `POST /api/risks/{id}/handle`，后端按 `CLOSED` 兼容处理。

## 权限控制

- `ADMIN`：可以处置风险事件、查看处置历史和审计日志。
- `RISK_OFFICER`：可以处置风险事件、查看处置历史和审计日志。
- `OPERATOR`：可以查看运营数据，但不能处置风险事件，调用处置接口返回 403。

权限仍沿用项目已有的签名令牌、拦截器和 `@RequiredRole` 注解，不引入 Spring Security。

## 操作日志与审计

每次风险处置会写入两类记录：

- `risk_event_handle_records`：记录风控处置业务历史。
- `operation_logs`：记录后台操作审计日志。

业务历史更适合展示事件流转过程，操作日志更适合统一审计所有后台动作。

## 看板统计

看板中的 `unhandledRiskCount` 和兼容字段 `openRiskEvents` 基于 `RiskStatus.PENDING` 统计，而不是仅依赖 `handled=false`。这样统计口径更清晰：只有等待人工审核的事件才算未处理风险。

## 风险查询分页

`GET /api/risks` 已从最近 50 条列表升级为分页查询，支持以下筛选条件：

- `status`：按 `PENDING`、`CONFIRMED`、`FALSE_POSITIVE`、`CLOSED` 筛选。
- `scene`：按 `ORDER_CREATED`、`ORDER_REFUNDED` 等触发场景筛选。
- `userId`：定位某个用户产生的风险。
- `orderNo`：按订单号模糊查询关联风险。
- `fromDate`、`toDate`：按创建日期筛选，结束日期包含当天。
- `page`、`size`：分页参数，页码从 0 开始，每页最大 100 条。

后端使用 Spring Data JPA `Specification` 动态组合条件，并用 `PageRequest` 按 `createdAt`、`id` 倒序返回分页元信息。这样既能保持代码清晰，也能支撑后台运营系统常见的多条件查询能力。

## 风险运营报表

新增 `GET /api/risks/summary` 风险运营报表接口，返回：

- 总风险数、待处理数、确认风险数、误报数、关闭归档数。
- 待处理占比、确认风险占比、误报率、关闭率、处置完成率。
- 平均首次处置耗时，基于 `handledAt - createdAt` 计算已处置事件。
- 按 `RiskScene` 统计风险数量。
- 按 `RiskStatus` 统计风险数量。

所有比例字段都使用 `max(1, totalRiskCount)` 作为保护分母，避免没有风险事件时出现除以 0。报表和分页列表配合后，风控人员可以先通过报表判断整体风险压力，再进入列表按状态、场景和日期处理具体事件。

## 设计价值

这个设计让风险模块具备完整的运营闭环：

- 异常购票、频繁退票等事件可以由后台人员审核和留痕。
- 风险事件具备状态流转、处置结论、操作审计和可追溯能力。
- 权限分层、流程闭环和日志审计共同保证风险处置过程可管理。

相比只做规则命中，风险处置闭环进一步覆盖了人工运营、权限控制和审计追踪。

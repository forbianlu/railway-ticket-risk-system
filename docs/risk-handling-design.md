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

这些字段让列表页能直接展示当前处置结论，也便于面试时说明后台运营人员如何跟进风险事件。

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

## 面试价值

这个设计可以服务于铁路局、银行科技岗和央国企软件岗的面试讲解：

- 铁路局场景：异常购票、频繁退票需要后台人员审核和留痕。
- 银行科技场景：交易风控强调状态流转、处置结论、操作审计和可追溯。
- 央国企软件岗场景：后台管理系统常见要求是权限分层、流程闭环和日志审计。

相比只做规则命中，风险处置闭环更能体现你对“业务系统不是只跑算法，还要支持人工运营和审计”的理解。

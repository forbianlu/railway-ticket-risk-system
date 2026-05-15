# Codex 反馈：风险处置闭环增强

## 1. 本轮任务目标

本轮目标是增强风险处置闭环，让风控模块从“生成风险 + 简单标记已处理”升级为可展示、可审计、可面试讲解的后台风控运营流程。

目标链路：

支付成功或退票 -> 触发风控规则 -> 生成风险事件 -> 风控人员查看事件 -> 提交处置结论和备注 -> 更新风险状态 -> 写入处置历史 -> 写入操作日志 -> 看板展示待处理风险数量。

## 2. 开始前 git status 和 git log 检查结果

开始前执行了：

```powershell
git status --short
git log --oneline -10
```

检查结果：

- 支付流水表与支付回调幂等相关文件尚未提交。
- `docs/project-context-for-gpt.md` 为未跟踪规划上下文文件，没有纳入核心功能提交。
- 最近提交为 `caf77b0 add order filtering and dashboard metrics`。
- 再往前为 `7403c58 add order payment state machine`。

## 3. 是否已经提交上一轮支付流水改动

是。开始本轮风险处置闭环开发前，先在 `backend` 目录运行 Maven 测试确认上一轮支付流水功能通过，然后创建本地保护提交。

## 4. 上一轮支付流水提交信息和 commit hash

- commit hash：`b40b735`
- commit message：`add payment records and callback idempotency`

本轮没有 push 到 GitHub。

## 5. 实际修改了哪些文件

新增文件：

- `backend/src/main/java/com/example/railway/domain/RiskStatus.java`
- `backend/src/main/java/com/example/railway/domain/RiskEventHandleRecord.java`
- `backend/src/main/java/com/example/railway/repository/RiskEventHandleRecordRepository.java`
- `backend/src/main/java/com/example/railway/dto/RiskHandleRequest.java`
- `backend/src/main/java/com/example/railway/dto/RiskEventHandleRecordResponse.java`
- `docs/risk-handling-design.md`
- `docs/project-development-log.md`
- `docs/codex-feedback-20260515-risk-handling.md`

修改文件：

- `README.md`
- `backend/src/main/java/com/example/railway/controller/RiskController.java`
- `backend/src/main/java/com/example/railway/domain/RiskEvent.java`
- `backend/src/main/java/com/example/railway/dto/RiskEventResponse.java`
- `backend/src/main/java/com/example/railway/repository/RiskEventRepository.java`
- `backend/src/main/java/com/example/railway/service/DashboardService.java`
- `backend/src/main/java/com/example/railway/service/RiskService.java`
- `backend/src/test/java/com/example/railway/RailwayApiIntegrationTests.java`
- `docs/api-design.md`
- `docs/database-design.md`
- `docs/er-diagram.mmd`
- `docs/project-outline.md`
- `docs/resume-and-interview.md`
- `frontend/index.html`
- `frontend/app.js`
- `frontend/styles.css`

## 6. 每个文件主要改了什么

- `RiskStatus.java`：新增风险状态枚举。
- `RiskEvent.java`：新增状态、场景、处理备注、处理人、处理时间、关闭时间字段，保留 `handled` 兼容旧逻辑。
- `RiskEventHandleRecord.java`：新增风险处置历史实体。
- `RiskEventHandleRecordRepository.java`：支持按风险事件 ID 查询处置历史。
- `RiskHandleRequest.java`：新增风险处置请求 DTO，包含 `status` 和 `remark`。
- `RiskEventResponse.java`：响应新增状态、场景、备注、处理人、处理时间和关闭时间。
- `RiskEventHandleRecordResponse.java`：新增处置历史响应 DTO。
- `RiskEventRepository.java`：新增按状态、场景筛选和按状态统计能力。
- `RiskService.java`：实现风险状态流转校验、处置历史写入、操作日志写入、查询处置历史。
- `RiskController.java`：增强 `GET /api/risks` 和 `POST /api/risks/{id}/handle`，新增 `GET /api/risks/{id}/handle-records`。
- `DashboardService.java`：未处理风险数改为基于 `RiskStatus.PENDING` 统计。
- `RailwayApiIntegrationTests.java`：新增风险状态、处置历史、备注、处理人、看板联动和权限相关断言。
- `frontend/index.html`：风险模块新增状态/场景筛选和处置交互区域。
- `frontend/app.js`：支持风险筛选、提交处置状态和备注、查看处置历史。
- `frontend/styles.css`：新增风险状态标签、处置表单和历史展示样式。
- 文档文件：同步说明风险处置闭环、数据库表、接口、简历讲法和开发日志。

## 7. 风险状态最终设计

`RiskStatus` 包含：

- `PENDING`：待处理，系统规则生成后等待人工审核。
- `CONFIRMED`：已确认风险。
- `FALSE_POSITIVE`：误报。
- `CLOSED`：已关闭归档。

## 8. 风险事件字段变化

`risk_events` 新增：

- `status`
- `scene`
- `handleRemark`
- `handledBy`
- `handledAt`
- `closedAt`

保留：

- `handled`

兼容关系：`status == PENDING` 时 `handled=false`，其他状态下 `handled=true`。

## 9. 风险处置历史表设计

新增 `risk_event_handle_records`：

- `id`
- `riskEventId`
- `fromStatus`
- `toStatus`
- `remark`
- `operatorName`
- `operatedAt`

历史表用于记录每次人工处置动作，支持审计和面试讲解。

## 10. 新增或增强接口设计

### GET /api/risks

支持：

- `status`
- `scene`

例如：

```http
GET /api/risks?status=PENDING&scene=ORDER_CREATED
```

### POST /api/risks/{id}/handle

请求体：

```json
{
  "status": "CONFIRMED",
  "remark": "短时间多次购票，确认存在异常购票行为"
}
```

兼容旧调用：请求体为空时按 `CLOSED` 处理。

### GET /api/risks/{id}/handle-records

返回指定风险事件的处置历史。

## 11. 风险状态流转规则

允许：

- `PENDING -> CONFIRMED`
- `PENDING -> FALSE_POSITIVE`
- `PENDING -> CLOSED`
- `CONFIRMED -> CLOSED`
- `FALSE_POSITIVE -> CLOSED`

禁止：

- `CLOSED` 重复处置
- 任意状态流转回 `PENDING`
- `CONFIRMED` 和 `FALSE_POSITIVE` 之间互相跳转

## 12. 权限控制规则

- `ADMIN` 可以处置风险事件。
- `RISK_OFFICER` 可以处置风险事件。
- `OPERATOR` 无权处置风险事件，返回 403。

权限仍沿用已有 `@RequiredRole` 注解和签名令牌机制，没有引入 Spring Security。

## 13. 操作日志和审计规则

每次处置风险事件会写两类记录：

- `RiskEventHandleRecord`：业务处置历史。
- `OperationLog`：统一操作审计日志，动作为 `HANDLE_RISK_EVENT`。

重复处置已关闭事件会返回业务错误，不写新的处置历史。

## 14. 看板未处理风险统计规则

看板中的 `unhandledRiskCount` 和兼容字段 `openRiskEvents` 改为基于：

```text
RiskStatus.PENDING
```

只有等待人工审核的风险事件才计入未处理风险。

## 15. 前端管理台新增了哪些风险处置交互

- 风险事件支持按状态筛选。
- 风险事件支持按触发场景筛选。
- 风险卡片展示风险状态、处理备注、处理人和处理时间。
- 风控人员可以选择 `CONFIRMED`、`FALSE_POSITIVE`、`CLOSED`。
- 风控人员可以输入处置备注。
- 支持查看单个风险事件的处置历史。
- `ADMIN` 和 `RISK_OFFICER` 可见处置控件，`OPERATOR` 不显示处置控件。

## 16. 新增或修改了哪些测试

新增或增强集成测试覆盖：

- 新生成风险事件默认 `PENDING`。
- `ADMIN` 可将风险事件处置为 `CONFIRMED`。
- `RISK_OFFICER` 可将风险事件处置为 `FALSE_POSITIVE`。
- `OPERATOR` 无权处置风险事件，返回 403。
- 处置备注可保存。
- 处理人可保存。
- 处理时间可保存。
- 处置后写入 `RiskEventHandleRecord`。
- 处置历史接口可查询记录。
- `CONFIRMED` 可关闭为 `CLOSED`。
- `CLOSED` 不能重复处置。
- 看板未处理风险数基于 `PENDING` 事件统计。
- 原有支付流水、订单状态机、订单幂等、分页筛选、看板指标、并发防超卖、缓存和权限测试继续通过。

## 17. Maven 测试结果

执行命令：

```powershell
& 'D:\简历系统\tools\apache-maven-3.9.15\bin\mvn.cmd' test
```

执行目录：`backend`

结果：

- `Tests run: 20`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`
- `BUILD SUCCESS`

## 18. 前端 JS 检查结果

执行命令：

```powershell
node --check frontend\app.js
```

结果：通过，无语法错误。

## 19. docs/project-development-log.md 追加了哪些内容

本轮创建并记录了 `docs/project-development-log.md`，包含：

- 本轮任务目标
- 开发前状态
- 实际修改文件
- 新增功能
- 风险状态设计
- 处置历史设计
- 接口设计
- 前端变化
- 测试结果
- 当前提交状态
- 后续建议

## 20. 仍未完成的问题

- 风险事件列表本轮仍保持最多 50 条列表返回，没有升级为分页。
- 暂未做风险事件分配处理人、SLA 超时提醒、复核流程。
- 暂未做风险事件导出或统计报表。
- `docs/project-context-for-gpt.md` 仍为未跟踪规划上下文文件，没有纳入核心功能。

## 21. 后续建议

- 下一轮可以做风险事件分页筛选和风险运营报表。
- 可以增加风险事件处理 SLA、分配处理人和复核状态。
- 可以增加退款流水表，让退票和支付形成完整资金链路。
- 可以增加接口签名、回调验签和支付金额一致性校验。

## 22. 建议提交信息

```text
enhance risk handling workflow
```

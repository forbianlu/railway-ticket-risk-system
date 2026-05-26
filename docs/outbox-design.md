# Outbox 事件设计

## 设计目标

Outbox 事件表用于在不引入外部消息队列的前提下，把核心交易事实可靠记录到数据库中。业务事务提交时，订单、支付、退款和风险相关事件与业务数据一起落库；后续由应用内派发器扫描并处理事件。

当前阶段保留原有同步风控、缓存失效和关键日志逻辑，Outbox 主要承担事务事件记录、轻量派发、失败观测、手动重试和运行统计。后续接入消息队列时，可以把 handler 替换为消息生产者。

## 为什么需要 Outbox

支付成功、退票、退款回调和风险处置之后通常会产生多种后置动作，例如审计日志、通知、报表聚合、风险计算和缓存失效。若所有后置动作都直接耦合在核心交易事务中，系统扩展后会增加事务耗时和失败传播范围。

Outbox 模式先把“交易事实”写入数据库，确保事件与业务状态具备同一事务边界。派发器再根据事件状态异步推进处理，从而让交易写入和后置处理之间有清晰边界。

## outbox_events 表结构

核心字段：

- `eventId`：事件唯一 ID。
- `eventType`：事件类型。
- `aggregateType`：聚合类型，如 `ORDER`、`PAYMENT`、`REFUND`、`RISK`。
- `aggregateId`：聚合 ID。
- `payload`：JSON 字符串载荷。
- `status`：事件状态。
- `retryCount`：已经尝试处理的次数。
- `maxRetryCount`：最大自动重试次数。
- `nextRetryAt`：下次可处理时间。
- `lastError`：最后一次处理失败原因。
- `createdAt`、`updatedAt`、`processedAt`：创建、更新和最终处理时间。

## 事件状态流转

```text
PENDING -> PROCESSING -> DONE
PENDING -> PROCESSING -> PENDING
PENDING -> PROCESSING -> FAILED
FAILED  -> PENDING
```

- `PENDING`：待处理，或失败后等待下次重试。
- `PROCESSING`：派发器正在处理。
- `DONE`：处理成功。
- `FAILED`：达到最大自动重试次数后仍失败。

手动重试只允许作用于 `FAILED` 事件。重试接口不会直接执行 handler，只会把事件重新置为 `PENDING` 并设置 `nextRetryAt = now`，由派发器继续处理。历史 `retryCount` 和 `lastError` 会保留，便于观察失败轨迹；`processedAt` 会清空，表示事件重新进入待处理队列。

## 事件发布流程

业务服务通过 `OutboxEventPublisher` 写入事件。事件写入与当前业务写操作处于同一个数据库事务中，如果业务事务回滚，事件也会回滚。

默认事件字段：

- `status = PENDING`
- `retryCount = 0`
- `maxRetryCount = 3`
- `nextRetryAt = 当前时间`

## 事件派发流程

`OutboxEventDispatcher` 定时扫描 `PENDING` 且 `nextRetryAt <= now` 的事件，也支持通过管理接口手动触发一次派发。

处理步骤：

1. 将事件标记为 `PROCESSING`。
2. 根据 `eventType` 查找 `OutboxEventHandler`。
3. handler 处理成功后标记为 `DONE`。
4. handler 处理失败后记录 `lastError` 并增加 `retryCount`。
5. 未达到最大重试次数时回到 `PENDING`，等待下次重试。
6. 达到最大重试次数后标记为 `FAILED`。

派发器还会恢复超过 5 分钟未更新的 `PROCESSING` 事件，将其重新置为 `PENDING` 并记录恢复说明，避免进程异常中断导致事件长期卡住。

## 失败事件重试

单条重试接口：

```http
POST /api/outbox-events/{id}/retry
```

批量重试接口：

```http
POST /api/outbox-events/retry-failed
```

两个接口均仅允许 `ADMIN` 角色访问。批量重试会把当前所有 `FAILED` 事件重新入队，并返回入队数量。非 `FAILED` 事件调用单条重试会返回业务错误。

## Outbox 统计指标

统计接口：

```http
GET /api/outbox-events/summary
```

统计字段：

- `totalCount`：事件总数。
- `pendingCount`、`processingCount`、`doneCount`、`failedCount`：按状态统计数量。
- `retryingCount`：`PENDING` 且 `retryCount > 0` 的数量。
- `maxRetryReachedCount`：`retryCount >= maxRetryCount` 的数量。
- `backlogCount`：`PENDING + PROCESSING`。
- `failureRate`：`failedCount / max(1, totalCount)`。
- `averageProcessSeconds`：已完成最终处理事件的平均耗时。
- `eventCountByType`：按事件类型统计。
- `eventCountByStatus`：按事件状态统计。
- `failedCountByType`：按事件类型统计失败数量。

所有比例计算都使用安全分母，避免空数据时出现除以 0。

## 当前处理器

当前 `OperationLogEventHandler` 会对已接入的业务事件写入一条派发完成日志，便于在不改变原业务结果的情况下验证事件派发链路。

## 当前已接入事件类型

- `ORDER_PAID`
- `ORDER_REFUNDED`
- `ORDER_CLOSED`
- `PAYMENT_SUCCEEDED`
- `PAYMENT_FAILED`
- `REFUND_CREATED`
- `REFUND_SUCCEEDED`
- `REFUND_FAILED`
- `RISK_EVENT_CREATED`
- `RISK_EVENT_HANDLED`

## 事件中心前端展示

前端事件中心展示事件总数、待处理、处理中、已完成、已失败、失败率和积压数量，并按事件类型和状态展示聚合数量。事件列表展示失败原因、重试次数、处理时间；对 `FAILED` 事件提供单条重试按钮，也支持批量重试全部失败事件和手动触发一次派发。

## 与后续消息队列的关系

当前派发器直接在应用内扫描数据库事件。后续可以将 handler 替换为消息队列生产者，在消息发送成功后再标记 `DONE`。这样核心业务仍只依赖数据库事务，不直接依赖外部消息系统可用性。

## 后续扩展方向

- 增加事件幂等消费记录。
- 增加按事件类型的处理耗时趋势。
- 增加处理器级别的失败告警。
- 将 Outbox 派发替换为消息队列生产者。
- 为报表、通知和外部系统同步增加独立事件处理器。

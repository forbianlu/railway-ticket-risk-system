# Outbox 事件设计

## 设计目标

Outbox 事件表用于在不引入消息队列的前提下，将核心交易事实记录为数据库事件。业务事务提交时，订单、支付、退款和风险相关事件与业务数据一起落库；后续由派发器扫描并处理事件。

## 为什么需要 Outbox

同步业务流程能保证结果直接可见，但随着系统扩展，支付成功后的风控、缓存失效、日志审计、通知和报表聚合会越来越多。Outbox 模式把“交易事实”先可靠写入数据库，为后续异步处理和接入消息队列提供稳定边界。

当前阶段仍保留原有同步风控、缓存失效和关键操作日志，Outbox 主要用于事件记录、轻量派发、重试和失败观测。

## outbox_events 表结构

核心字段：

- `eventId`：事件唯一 ID。
- `eventType`：事件类型。
- `aggregateType`：聚合类型，如 `ORDER`、`PAYMENT`、`REFUND`、`RISK`。
- `aggregateId`：聚合 ID。
- `payload`：JSON 字符串载荷。
- `status`：事件状态。
- `retryCount`：已重试次数。
- `maxRetryCount`：最大重试次数。
- `nextRetryAt`：下次可重试时间。
- `lastError`：最后一次处理失败原因。

## 事件状态流转

```text
PENDING -> PROCESSING -> DONE
PENDING -> PROCESSING -> PENDING
PENDING -> PROCESSING -> FAILED
```

- `PENDING`：待处理或等待下次重试。
- `PROCESSING`：派发器正在处理。
- `DONE`：处理成功。
- `FAILED`：达到最大重试次数后仍失败。

## 事件发布流程

业务服务通过 `OutboxEventPublisher` 写入事件。事件写入与当前业务写操作处于同一数据库事务中，如果业务事务回滚，事件也会回滚。

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
5. 未超过最大重试次数时回到 `PENDING`，等待下次重试。
6. 超过最大重试次数后标记为 `FAILED`。

## 当前处理器

当前 `OperationLogEventHandler` 对已接入的业务事件写入一条派发完成日志，便于在不改变原业务结果的情况下验证事件派发链路。

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

## 与未来消息队列的关系

当前派发器直接在应用内扫描数据库事件。后续可以将 `DONE` 前的处理动作替换为“发送消息到 Kafka、RabbitMQ 或其他消息系统”，发送成功后再标记 `DONE`。这样核心业务只依赖数据库事务，不直接依赖外部 MQ 可用性。

## 后续扩展方向

- 增加事件幂等消费表。
- 增加按事件类型的 handler 失败统计。
- 增加后台重试指定失败事件的接口。
- 将 Outbox 派发替换为消息队列生产者。
- 为报表、通知和外部系统同步增加独立事件处理器。

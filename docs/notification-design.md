# 站内通知中心设计

## 设计目标

站内通知中心用于把订单、支付、出票、关闭、退票和退款结果同步给普通乘客，同时让运营管理端可以查看通知总量、未读量、类型分布和状态分布。当前阶段只实现系统内通知，不接入短信、邮件、外部推送或第三方消息渠道。

## notification_records 表

| 字段 | 说明 |
| --- | --- |
| id | 主键 |
| notification_no | 系统内部通知编号，唯一 |
| user_id | 通知归属乘客 ID |
| title | 通知标题 |
| content | 通知正文 |
| type | 通知类型 |
| status | 通知状态 |
| business_type | 业务类型，例如 ORDER、PAYMENT、TICKET、REFUND |
| business_id | 业务幂等 ID |
| order_id | 关联订单 ID |
| order_no | 关联订单号 |
| ticket_no | 关联电子票号 |
| payment_no | 关联支付流水号 |
| refund_no | 关联退款流水号 |
| read_at | 阅读时间 |
| created_at | 创建时间 |
| updated_at | 更新时间 |

## 通知类型和状态

`NotificationType` 覆盖 `ORDER_CREATED`、`PAYMENT_SUCCEEDED`、`TICKET_ISSUED`、`ORDER_CLOSED`、`ORDER_REFUNDED`、`REFUND_SUCCEEDED`、`REFUND_FAILED` 和 `RISK_ALERT`。

`NotificationStatus` 包含 `UNREAD` 和 `READ`。乘客读取通知或执行全部已读后，系统记录 `read_at`。

## 创建触发点

通知创建接入现有业务链路，但不改变核心交易规则：

- 下单成功后创建 `ORDER_CREATED`。
- 支付成功后创建 `PAYMENT_SUCCEEDED`。
- 出票成功后创建 `TICKET_ISSUED`。
- 待支付订单关闭后创建 `ORDER_CLOSED`。
- 退票成功后创建 `ORDER_REFUNDED`。
- 退款成功回调后创建 `REFUND_SUCCEEDED`。
- 退款失败回调后创建 `REFUND_FAILED`。

## 幂等策略

通知使用 `business_type + business_id + type + user_id` 形成业务幂等键，重复触发同一业务动作时不会重复插入相同通知。通知编号使用 `NT` 前缀加时间和随机片段生成，演示数据使用固定编号保证重复启动幂等。

## 乘客侧接口

- `GET /api/passenger/notifications`：分页查看我的通知，支持 `status`、`type`、`page`、`size`。
- `GET /api/passenger/notifications/unread-count`：查看当前乘客通知统计和未读数量。
- `POST /api/passenger/notifications/{id}/read`：标记单条通知已读。
- `POST /api/passenger/notifications/read-all`：标记当前乘客全部通知已读。

乘客只能查看和更新自己的通知。

## 管理侧接口

- `GET /api/notifications`：分页查看全部通知，支持按用户、状态和类型筛选。
- `GET /api/notifications/summary`：查看通知总数、未读数、已读数、类型分布和状态分布。

管理端接口只允许管理角色访问，普通乘客访问会被拒绝。

## Outbox 关系

每次通知创建成功后写入 `NOTIFICATION_CREATED` Outbox 事件。当前阶段该事件用于系统内观测和后续扩展，不引入消息队列，也不替代同步业务结果。

## 前端展示

乘客端新增消息中心，展示未读数量、通知列表、状态筛选、单条已读和全部已读。管理端新增通知中心，展示通知列表、状态分布和类型分布。两端都沿用现有原生 HTML/CSS/JavaScript 结构。

## 后续扩展

后续可以增加通知偏好、按渠道订阅、通知模板、批量生成策略和外部推送网关。本阶段保留系统内通知边界，不接入外部推送。

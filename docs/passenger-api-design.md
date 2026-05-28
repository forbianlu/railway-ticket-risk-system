# 乘客端 API 设计

## 设计目标

系统在运营管理端之外新增普通乘客访问边界。乘客端 API 统一使用 `/api/passenger/**`，复用既有订单、支付、退款、风控和 Outbox 业务链路，避免为用户端创建重复实体或重复状态机。

## USER 角色

`USER` 表示普通乘客。乘客登录后只能访问乘客端接口、车站车次查询和当前登录用户信息，不能访问订单管理、风险管理、审计日志、Outbox、缓存和限流等管理端接口。

演示账号：

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `passenger1` | `123456` | `USER` |
| `passenger2` | `123456` | `USER` |
| `passenger3` | `123456` | `USER` |

## 接口列表

| 接口 | 说明 |
| --- | --- |
| `GET /api/passenger/summary` | 当前乘客订单、支付、退款和近期行程概览 |
| `GET /api/passenger/orders` | 分页查询我的订单，可按订单状态筛选 |
| `POST /api/passenger/orders` | 当前乘客下单，用户 ID 来自 JWT |
| `POST /api/passenger/orders/{id}/pay` | 支付当前乘客自己的待支付订单 |
| `POST /api/passenger/orders/{id}/close` | 取消当前乘客自己的待支付订单 |
| `POST /api/passenger/orders/{id}/refund` | 退当前乘客自己的已支付订单 |
| `GET /api/passenger/payments` | 分页查询我的支付流水 |
| `GET /api/passenger/refunds` | 分页查询我的退款流水 |

## 我的订单权限边界

乘客接口从 `AuthContext` 读取当前 JWT 中的 `userId`，不会信任前端传入的用户 ID。下单时服务端将当前乘客 ID 写入 `ticket_orders.user_id`；查询、支付、关闭和退票都会先校验订单归属。

## 下单、支付和退票流程

下单流程：

```text
USER 登录 -> 查询车次 -> POST /api/passenger/orders -> 复用 OrderService 创建 PENDING_PAYMENT 订单并锁票
```

支付流程：

```text
校验订单归属 -> 创建或复用 PENDING 支付流水 -> 构造模拟成功回调 -> 复用 PaymentService 验签、改流水、改订单、触发风控
```

退票流程：

```text
校验订单归属 -> 复用 OrderService 退票 -> 释放库存 -> 触发退票风控 -> 自动创建退款流水
```

## 与管理端数据互通

乘客端和管理端共用以下表：

- `ticket_orders`
- `payment_records`
- `refund_records`
- `risk_events`
- `outbox_events`
- `operation_logs`

因此乘客端产生的订单、支付、退款和风险事件会自然出现在管理端订单列表、支付流水、退款流水、风险运营和 Outbox 事件中心中。

## 安全限制

- `USER` 访问 `/api/orders`、`/api/payments`、`/api/refunds`、`/api/risks`、`/api/logs`、`/api/outbox-events`、`/api/cache`、`/api/rate-limit` 和 `/api/dashboard` 会返回 403。
- `USER` 不能支付、关闭或退票其他乘客订单。
- `ADMIN`、`RISK_OFFICER`、`OPERATOR` 的原有管理端权限保持不变。

## 后续前端计划

后续可以新增入口选择页和乘客购票前端，使用本轮新增的 passenger API 展示查票、下单、我的订单、我的支付流水和我的退款流水。

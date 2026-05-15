# 数据库设计

## app_users

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| username | varchar | 登录账号，唯一 |
| password_hash | varchar | 演示环境密码哈希 |
| display_name | varchar | 展示名称 |
| role | varchar | 角色：ADMIN、RISK_OFFICER、OPERATOR |
| enabled | boolean | 是否启用 |

## stations

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| code | varchar | 车站编码 |
| name | varchar | 车站名称 |
| city | varchar | 所在城市 |
| enabled | boolean | 是否启用 |

## trains

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| train_no | varchar | 车次号 |
| departure_station_id | bigint | 出发站 |
| arrival_station_id | bigint | 到达站 |
| departure_time | time | 出发时间 |
| arrival_time | time | 到达时间 |
| enabled | boolean | 是否启用 |

## seat_inventories

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| train_id | bigint | 车次 |
| travel_date | date | 乘车日期 |
| seat_type | varchar | 座位类型 |
| total_seats | int | 总座位 |
| remaining_seats | int | 剩余座位 |
| price | decimal | 票价 |
| version | bigint | 乐观锁版本号 |

## ticket_orders

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| order_no | varchar | 订单号 |
| request_id | varchar | 幂等请求号，与 user_id 组成唯一约束 |
| passenger_name | varchar | 乘客姓名 |
| passenger_id_card | varchar | 证件号 |
| user_id | bigint | 用户 ID |
| train_id | bigint | 车次 |
| inventory_id | bigint | 座位库存 |
| travel_date | date | 乘车日期 |
| seat_type | varchar | 座位类型 |
| amount | decimal | 金额 |
| status | varchar | 订单状态：PENDING_PAYMENT、PAID、REFUNDED、CLOSED、CANCELLED |
| created_at | timestamp | 创建时间 |
| payment_deadline_at | timestamp | 支付截止时间 |
| paid_at | timestamp | 支付时间 |
| refunded_at | timestamp | 退票时间 |
| closed_at | timestamp | 关闭时间 |

订单状态说明：

| 状态 | 说明 | 库存影响 |
| --- | --- | --- |
| PENDING_PAYMENT | 待支付，订单已创建并锁定 1 张余票 | 已扣减库存 |
| PAID | 已支付，订单生效并参与风控统计 | 继续占用库存 |
| REFUNDED | 已退票 | 释放库存 |
| CLOSED | 待支付订单关闭或超时关闭 | 释放库存 |
| CANCELLED | 预留状态，供后续取消订单扩展 | 视业务规则处理 |

## risk_events

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| order_id | bigint | 关联订单 |
| user_id | bigint | 用户 ID |
| risk_type | varchar | 风险类型 |
| risk_level | varchar | 风险等级 |
| scene | varchar | 风控触发场景：ORDER_CREATED、ORDER_REFUNDED |
| status | varchar | 风险状态：PENDING、CONFIRMED、FALSE_POSITIVE、CLOSED |
| reason | varchar | 触发原因 |
| handled | boolean | 是否处理，兼容旧前端；当前等价于 status != PENDING |
| handle_remark | varchar | 最新处置备注 |
| handled_by | varchar | 最新处理人 |
| handled_at | timestamp | 最新处理时间 |
| closed_at | timestamp | 关闭归档时间 |
| created_at | timestamp | 创建时间 |

风险状态说明：

| 状态 | 说明 |
| --- | --- |
| PENDING | 系统规则生成后等待人工审核 |
| CONFIRMED | 风控人员确认存在风险 |
| FALSE_POSITIVE | 风控人员判断为误报 |
| CLOSED | 事件已完成处置并归档 |

## risk_event_handle_records

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| risk_event_id | bigint | 风险事件 ID |
| from_status | varchar | 处置前状态 |
| to_status | varchar | 处置后状态 |
| remark | varchar | 处置备注 |
| operator_name | varchar | 操作人 |
| operated_at | timestamp | 操作时间 |

## payment_records

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| payment_no | varchar | 支付流水号，唯一 |
| order_id | bigint | 关联订单 ID |
| order_no | varchar | 关联订单号，便于展示 |
| user_id | bigint | 用户 ID |
| amount | decimal | 支付金额 |
| status | varchar | 支付状态：PENDING、SUCCESS、FAILED |
| channel | varchar | 支付渠道，当前为 MOCK |
| request_id | varchar | 创建支付流水请求号，用于创建幂等 |
| callback_request_id | varchar | 支付回调请求号，用于回调幂等 |
| callback_message | varchar | 回调消息 |
| paid_at | timestamp | 支付成功时间 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

支付流水状态说明：

| 状态 | 说明 | 对订单影响 |
| --- | --- | --- |
| PENDING | 已创建支付流水，等待回调 | 订单仍为 PENDING_PAYMENT |
| SUCCESS | 支付成功 | 订单变为 PAID，触发支付后风控 |
| FAILED | 支付失败 | 订单保持 PENDING_PAYMENT，可重新创建流水或等待关闭 |

## operation_logs

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| operator | varchar | 操作人 |
| action | varchar | 操作类型 |
| target_type | varchar | 操作对象类型 |
| target_id | varchar | 操作对象 ID |
| detail | varchar | 操作详情 |
| created_at | timestamp | 创建时间 |

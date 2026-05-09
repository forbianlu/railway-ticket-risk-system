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
| request_id | varchar | 幂等请求号，同一用户内唯一 |
| passenger_name | varchar | 乘客姓名 |
| passenger_id_card | varchar | 证件号 |
| user_id | bigint | 用户 ID |
| train_id | bigint | 车次 |
| inventory_id | bigint | 座位库存 |
| travel_date | date | 乘车日期 |
| seat_type | varchar | 座位类型 |
| amount | decimal | 金额 |
| status | varchar | 订单状态 |
| created_at | timestamp | 创建时间 |
| refunded_at | timestamp | 退票时间 |

## risk_events

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| order_id | bigint | 关联订单 |
| user_id | bigint | 用户 ID |
| risk_type | varchar | 风险类型 |
| risk_level | varchar | 风险等级 |
| reason | varchar | 触发原因 |
| handled | boolean | 是否处理 |
| created_at | timestamp | 创建时间 |

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

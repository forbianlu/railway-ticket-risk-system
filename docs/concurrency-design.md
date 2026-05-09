# 并发购票防超卖设计

## 问题背景

铁路票务系统的核心风险是余票超卖。多个用户同时购买同一车次、同一日期、同一席别时，如果只做普通查询后扣减，可能出现多个请求都读到“还有 1 张票”，最终生成多个订单。

## 当前方案

项目第一版使用数据库事务 + JPA 乐观锁控制库存扣减。

| 设计点 | 当前实现 |
| --- | --- |
| 库存字段 | `seat_inventories.remaining_seats` |
| 乐观锁字段 | `seat_inventories.version` |
| 事务边界 | `OrderService.createOrder` |
| 扣减逻辑 | 校验余票大于 0 后执行 `deductOne()` |
| 冲突处理 | 版本号冲突时返回 409 `INVENTORY_CONFLICT` |
| 无票处理 | 已无余票时返回 400 `BUSINESS_ERROR` |

## 下单流程

1. 请求进入 `POST /api/orders`。
2. 服务层根据 `inventoryId` 查询座位库存。
3. 校验库存与车次是否匹配、余票是否充足。
4. 扣减 `remainingSeats`。
5. 保存订单和库存。
6. 事务提交时数据库检查 `version`。
7. 如果版本号已被其他事务更新，当前事务失败并返回 409。

## 为什么能防止超卖

乐观锁的关键是更新库存时带上版本号。多个并发事务同时读取到同一条库存时，它们拿到的 `version` 相同；第一个提交成功后版本号增加，后续事务再提交时发现版本号已经变化，更新失败并回滚。因此订单和库存扣减不会同时成功。

## 已验证场景

集成测试 `shouldPreventOversellUnderConcurrentPurchase` 会动态创建一条只有 1 张票的测试车次，然后发起 16 个并发下单请求。

断言结果：

- 成功订单数等于 1
- 失败请求数等于 15
- 库存最终为 0
- 数据库中该库存关联订单数等于 1
- 失败请求只允许返回 400 或 409

运行方式：

```bash
cd backend
mvn test
```

## 手动压测脚本

启动后端后，可以运行：

```bash
node scripts/concurrent-purchase.js 30
```

脚本会并发调用购票接口，并输出请求数、耗时、HTTP 状态分布和压测前后的余票变化。

可通过环境变量指定目标库存：

```text
API_BASE=http://localhost:8080/api
REQUESTS=50
FROM=BJP
TO=SHH
DATE=2026-06-01
TRAIN_ID=1
INVENTORY_ID=1
```

## 后续优化

- 使用 Redis 预扣库存减少数据库写冲突。
- 使用消息队列削峰，把高峰下单请求排队处理。
- 对同一用户、同一库存做短时间限流。
- 在真实压测中记录 P95/P99 响应时间、成功率和冲突率。

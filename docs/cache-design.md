# 车次余票查询缓存设计

## 设计目标

车次余票查询属于高频读接口，用户会反复按“出发站、到达站、乘车日期”查询同一路线。缓存目标是降低数据库查询压力，同时在购票和退票后尽快让余票数据回到最新状态。

## 缓存策略

| 设计点 | 当前实现 |
| --- | --- |
| 缓存对象 | `GET /api/trains/search` 查询结果 |
| 缓存 Key | `departureCode + arrivalCode + travelDate` |
| 缓存 Value | `List<TrainSearchResponse>` |
| 过期时间 | 默认 30 秒，可通过配置调整 |
| 最大容量 | 默认 256 个查询条件 |
| 命中统计 | 记录 hit、miss、evict、entryCount |
| 失效方式 | 下单、退票事务提交后按线路日期删除缓存 |

配置项：

```yaml
railway:
  cache:
    train-search:
      enabled: true
      ttl-seconds: 30
      max-entries: 256
```

## 查询流程

1. 前端调用 `GET /api/trains/search?from=BJP&to=SHH&date=2026-06-01`。
2. `TrainQueryService` 先按线路日期查询 `TrainSearchCacheService`。
3. 缓存命中且未过期时直接返回缓存副本。
4. 缓存未命中或已过期时查询数据库。
5. 数据库结果写入缓存，再返回给前端。

## 失效流程

下单和退票都会改变座位库存，因此 `OrderService` 会在事务提交后失效对应线路日期的缓存：

- 下单：扣减 `remainingSeats`，提交成功后删除该线路日期缓存。
- 退票：释放 `remainingSeats`，提交成功后删除该线路日期缓存。
- 如果事务回滚，不触发缓存失效，避免缓存状态被错误修改。

## 管理接口

```http
GET /api/cache/train-search
Authorization: Bearer {token}
```

查看缓存统计，仅允许 `ADMIN` 和 `RISK_OFFICER`。

```http
DELETE /api/cache/train-search
Authorization: Bearer {token}
```

清空缓存，仅允许 `ADMIN`。

## 后续升级 Redis

当前实现使用本地内存缓存，优点是无需额外中间件，适合演示和本地开发。生产化可以将 `TrainSearchCacheService` 替换为 Redis 实现：

- Key 使用 `railway:train-search:{from}:{to}:{date}`
- Value 使用 JSON 序列化的查询结果
- TTL 使用 Redis 原生过期时间
- 下单和退票后删除对应 Key
- 多后端实例共享同一份缓存

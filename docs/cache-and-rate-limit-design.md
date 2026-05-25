# 缓存与限流设计

## 设计目标

系统通过车次查询缓存降低重复余票查询开销，通过接口限流保护高频查询和敏感写操作。默认环境使用本地内存实现，避免依赖外部 Redis；需要多实例共享状态时，可以通过配置切换到 Redis。

## 车次查询缓存设计

缓存只保存车次余票查询结果，不缓存订单、支付、风险等写模型。缓存入口统一由 `TrainSearchCacheService` 提供，内部根据配置选择 local 或 Redis store。

## 本地缓存和 Redis 缓存切换

配置项：

```yaml
railway:
  cache:
    train-search:
      enabled: true
      mode: local
      ttl-seconds: 30
      max-entries: 256
```

- `mode: local`：使用本地 `ConcurrentHashMap` 和过期时间。
- `mode: redis`：使用 Redis 字符串保存 JSON 结果，并设置 TTL。
- Redis 调用异常时，服务可降级到本地缓存，默认演示环境不强依赖 Redis。

## 缓存 key 设计

```text
railway:cache:train-search:{from}:{to}:{date}
```

其中站点编码会转换为大写并去除首尾空格，日期使用 `yyyy-MM-dd`。

## 缓存失效场景

以下动作会在事务提交后失效对应线路和日期缓存：

- 创建待支付订单。
- 支付成功。
- 手动关闭待支付订单。
- 超时关闭待支付订单。
- 退票成功。

## 缓存统计指标

缓存统计接口返回：

- `cacheMode`：当前实际使用模式。
- `configuredMode`：配置模式。
- `ttlSeconds`：缓存 TTL。
- `entryCount`：当前缓存条目数。
- `hitCount`：命中次数。
- `missCount`：未命中次数。
- `evictCount`：失效次数。
- `redisAvailable`：Redis 模式是否可用。
- `localFallback`：是否发生本地降级。

## 接口限流设计

限流入口由 `RateLimitService` 提供，采用固定窗口计数。默认使用本地内存窗口，可切换为 Redis `INCR + EXPIRE`。

当前保护接口：

- `GET /api/trains/search`：60 秒 120 次，按登录用户或 IP。
- `POST /api/orders`：60 秒 10 次，按用户 ID。
- `POST /api/payments/callback`：60 秒 30 次，按支付流水号和 IP。
- `POST /api/risks/{id}/handle`：60 秒 30 次，按当前操作人。

## 限流 key 设计

```text
rate:train:search:user:{userId}
rate:train:search:ip:{ip}
rate:order:create:user:{userId}
rate:payment:callback:{paymentNo}:ip:{ip}
rate:risk:handle:user:{username}
```

限流 key 不保存 JWT 原文，也不保存敏感业务内容。

## 429 错误处理

超过阈值后统一返回 HTTP 429：

```json
{
  "success": false,
  "code": "TOO_MANY_REQUESTS",
  "message": "请求过于频繁，请稍后再试"
}
```

## 后续扩展方向

- 使用 Lua 脚本提升 Redis 限流原子性。
- 增加不同接口的动态配置。
- 增加限流命中趋势统计。
- 增加按租户、客户端或设备维度的限流策略。

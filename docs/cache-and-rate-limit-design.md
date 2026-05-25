# 缓存与限流设计

## 设计目标

系统通过车次查询缓存降低重复余票查询开销，通过接口限流保护高频查询和敏感写操作。默认环境使用本地内存实现，避免依赖外部 Redis；需要多实例共享状态时，可以通过配置切换到 Redis。

## 车次查询缓存设计

缓存只保存车次余票查询结果，不缓存订单、支付、风险等写模型。缓存入口统一由 `TrainSearchCacheService` 提供，内部根据配置选择 local 或 Redis store。

## 本地缓存和 Redis 缓存切换

配置项：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms

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

切换到 Redis 模式：

```yaml
railway:
  cache:
    train-search:
      mode: redis
      ttl-seconds: 30
```

默认测试环境保持 `mode: local`，因此执行 Maven 测试不需要本机或 CI 环境提供 Redis 服务。

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

配置项：

```yaml
railway:
  rate-limit:
    enabled: true
    mode: local
    local-fallback-enabled: true
    rules:
      train-search:
        limit: 60
        window-seconds: 60
      order-create:
        limit: 10
        window-seconds: 60
      payment-callback:
        limit: 30
        window-seconds: 60
      risk-handle:
        limit: 30
        window-seconds: 60
```

切换到 Redis 限流：

```yaml
railway:
  rate-limit:
    mode: redis
    local-fallback-enabled: true
```

控制器只传入规则名和业务限流 key，阈值统一从 `railway.rate-limit.rules` 读取。

当前保护接口：

- `GET /api/trains/search`：使用 `train-search` 规则，按登录用户或 IP。
- `POST /api/orders`：使用 `order-create` 规则，按用户 ID。
- `POST /api/payments/callback`：使用 `payment-callback` 规则，按支付流水号和 IP。
- `POST /api/risks/{id}/handle`：使用 `risk-handle` 规则，按当前操作人。

## 限流 key 设计

```text
rate:train:search:user:{userId}
rate:train:search:ip:{ip}
rate:order:create:user:{userId}
rate:payment:callback:{paymentNo}:ip:{ip}
rate:risk:handle:user:{username}
```

限流 key 不保存 JWT 原文，也不保存敏感业务内容。

## Redis 联调步骤

当前默认测试环境不依赖 Redis。需要验证 Redis 模式时，可以按以下步骤操作：

1. 启动 Redis，并确认 `localhost:6379` 可连接。
2. 将 `spring.redis.host`、`spring.redis.port` 配置为目标 Redis。
3. 将 `railway.cache.train-search.mode` 改为 `redis`。
4. 将 `railway.rate-limit.mode` 改为 `redis`。
5. 启动后端服务。
6. 调用 `GET /api/trains/search?from=BJP&to=SHH&date=yyyy-MM-dd`。
7. 在 Redis 中检查是否出现 `railway:cache:train-search:BJP:SHH:yyyy-MM-dd`。
8. 重复调用车次查询，检查 `GET /api/cache/train-search` 中的 `hitCount` 是否增加。
9. 创建订单或执行退票，确认对应车次查询缓存 key 被清理。
10. 高频调用受限接口，检查 Redis 中是否出现 `railway:rate-limit:*` key。
11. 超过规则阈值后，确认接口返回 HTTP 429。

本次默认验证环境未检测到可用 Redis 服务，因此没有记录真实 Redis 端到端联调结果；代码和配置已保留 Redis 模式入口，后续可按上述步骤执行。

## Redis 不可用时的 fallback

当配置为 Redis 模式且 `local-fallback-enabled: true` 时：

- Redis 缓存读写异常会回退到本地缓存。
- Redis 限流异常会回退到本地固定窗口。
- 统计接口会通过 `configuredMode`、`cacheMode`、`redisAvailable` 和 `localFallback` 展示当前状态。

如果希望 Redis 不可用时直接暴露错误，可将 `local-fallback-enabled` 关闭，用于严格依赖 Redis 的部署环境。

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
- 将限流规则接入配置中心或数据库，支持运行时调整。
- 增加限流命中趋势统计。
- 增加按租户、客户端或设备维度的限流策略。

## 常见问题排查

- `cacheMode` 为 `local`，`configuredMode` 为 `redis`：说明配置选择了 Redis，但当前请求已回退到本地缓存。
- `redisAvailable` 为 `false`：检查 Redis 是否启动、端口是否正确、网络是否可达。
- 限流没有触发：检查 `railway.rate-limit.enabled` 是否为 `true`，以及对应 `rules` 的阈值是否过大。
- 默认 Maven 测试不访问 Redis：这是有意设计，避免开发和 CI 环境因缺少 Redis 导致基础测试失败。

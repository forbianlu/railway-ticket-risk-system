# 项目开发日志

## 阶段提交索引

| 阶段 | 提交 | 说明 |
| --- | --- | --- |
| 订单支付状态机与超时关闭 | `7403c58 add order payment state machine` | 创建订单进入待支付、支付成功、关闭、超时关闭、退票和库存释放 |
| 订单分页筛选与运营看板指标增强 | `caf77b0 add order filtering and dashboard metrics` | 订单多条件分页查询，运营看板补充状态统计、退票率、风险率 |
| 支付流水表与支付回调幂等 | `b40b735 add payment records and callback idempotency` | 支付流水、模拟支付回调、`callbackRequestId` 幂等 |
| 风险处置闭环增强 | `0fc4543 enhance risk handling workflow` | 风险状态、处置备注、处理人、处理时间、处置历史和权限控制 |
| 风险事件分页筛选与风险运营报表增强 | `1f77ac7 add risk query pagination and summary` | 风险分页筛选、风险运营报表、前端风险统计展示 |
| 项目展示文档整理 | `3b0227e polish docs and project showcase` | README、API 文档、最终总结和开发日志整理 |
| Spring Security + JWT + BCrypt 权限体系升级 | `6e38db3 upgrade auth to spring security jwt` | 标准认证链路、JWT Bearer Token、BCrypt 密码存储和角色授权 |
| Redis 缓存与接口限流增强 | `e38e0de add redis cache and rate limiting` | 车次查询 local/Redis 缓存切换、本地 fallback、关键接口限流和 429 响应 |
| Redis 联调说明与限流规则配置化 | `configure redis rate limit rules` | Redis 模式联调步骤、限流 rules 配置化、前端规则展示 |
| 支付校验与退款流水闭环 | `add refund records and payment verification` | 支付回调签名与金额校验、退款流水、退款回调签名和幂等 |
| Outbox 事件表与交易事件解耦 | `add outbox event processing` | 事务内事件落库、事件派发、重试失败处理和事件中心 |

## 订单支付状态机与超时关闭

### 目标

将订单流程从创建即成功调整为真实交易系统中的待支付状态机，并支持手动关闭、超时关闭和退票。

### 主要内容

- 创建订单后进入 `PENDING_PAYMENT`。
- 创建订单时扣减库存，表示锁定余票。
- 支付成功后订单进入 `PAID`。
- 待支付订单可以关闭为 `CLOSED`，关闭后释放库存。
- 超时待支付订单由批量接口和定时任务关闭。
- 已支付订单可以退票为 `REFUNDED`，退票后释放库存。
- 支付成功和退票成功后触发对应风控规则。

### 验证结果

- 覆盖订单状态流转、非法流转、库存扣减和释放。
- 保持下单幂等、并发防超卖和查询缓存测试通过。

## 订单分页筛选与运营看板指标增强

### 目标

增强后台运营查询能力，让订单列表和运营看板更适合管理端使用。

### 主要内容

- `GET /api/orders` 支持分页响应。
- 支持按用户、订单状态、订单号、创建日期筛选。
- 看板新增待支付、已支付、已关闭、已退票统计。
- 看板新增退票率、风险率和未处理风险数。
- 前端订单列表增加筛选、重置和分页控件。

### 验证结果

- 覆盖订单默认分页、状态筛选、用户筛选、日期筛选、分页元信息和非法参数。
- 原有订单状态机、幂等、防超卖、缓存和权限测试保持通过。

## 支付流水表与支付回调幂等

### 目标

将订单支付从简单状态变更扩展为包含支付流水和回调幂等的交易链路。

### 主要内容

- 新增 `payment_records` 表。
- 新增支付状态：`PENDING`、`SUCCESS`、`FAILED`。
- 新增创建支付流水接口。
- 新增支付回调接口。
- 使用 `requestId` 保证支付流水创建幂等。
- 使用 `callbackRequestId` 保证支付回调幂等。
- 成功回调复用订单支付逻辑，避免重复触发风控。
- 失败回调只更新支付流水，不关闭订单、不释放库存。
- 前端新增支付流水管理区域。

### 验证结果

- 覆盖创建支付流水、成功回调、失败回调、重复回调、支付流水分页和状态筛选。
- 原有订单、风控、看板、缓存和权限测试保持通过。

## 风险处置闭环增强

### 目标

将风险模块从简单标记处理升级为包含状态、备注、处理人、处理时间和处置历史的运营闭环。

### 主要内容

- 新增 `RiskStatus`：`PENDING`、`CONFIRMED`、`FALSE_POSITIVE`、`CLOSED`。
- 风险事件保存最新处置备注、处理人、处理时间和关闭时间。
- 新增 `risk_event_handle_records` 表记录每次处置动作。
- `POST /api/risks/{id}/handle` 支持处置状态和备注。
- 新增 `GET /api/risks/{id}/handle-records`。
- 看板未处理风险数基于 `PENDING` 状态统计。
- 前端支持风险状态筛选、处置备注和历史查看。

### 验证结果

- 覆盖默认风险状态、角色权限、处置备注、处理人、处理时间、处置历史和关闭规则。
- 原有支付流水、订单状态机、订单分页、看板、缓存和权限测试保持通过。

## 风险事件分页筛选与风险运营报表增强

### 目标

增强风险查询和风险运营分析能力。

### 主要内容

- `GET /api/risks` 返回分页对象。
- 支持按状态、场景、用户 ID、订单号、创建日期筛选。
- 新增 `GET /api/risks/summary` 风险运营报表。
- 报表包含总风险数、各状态数量、比例字段、平均首次处置耗时、按场景统计和按状态统计。
- 前端风险列表支持筛选、重置、上一页、下一页和分页元信息。
- 前端新增风险运营统计展示。

### 验证结果

- 覆盖风险默认分页、状态筛选、场景筛选、用户筛选、订单号筛选、日期筛选和非法参数。
- 覆盖风险报表数量、比例、状态聚合和场景聚合。
- 原有风险处置、支付流水、订单状态机、缓存和权限测试保持通过。

## 项目展示文档整理

### 目标

整理公开项目文档，使 README、API 文档、数据库文档、开发日志和总结文档保持一致。

### 主要内容

- README 重新组织项目简介、核心功能、技术栈、流程图、功能模块、启动方式、接口概览、数据库核心表和设计说明。
- API 文档修正订单分页响应说明的位置。
- 新增最终总结文档，集中说明系统定位、功能模块、技术亮点、业务流程、幂等设计、风控设计、权限审计和测试覆盖。
- 开发日志补充阶段提交索引。

### 验证结果

- Markdown 相对链接扫描通过。
- 前端脚本语法检查通过。
- Maven 测试通过：`Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`。

## Spring Security + JWT + BCrypt 权限体系升级

### 目标

将登录认证从轻量签名令牌升级为 Spring Security 无状态认证链路，使用 JWT 承载登录态，使用 BCrypt 存储和校验密码，同时保持现有角色权限语义。

### 主要内容

- 新增 `spring-boot-starter-security` 依赖。
- 新增 `SecurityConfig`，关闭表单登录、HTTP Basic、CSRF 和服务端 Session。
- 新增 `JwtAuthenticationFilter`，解析 `Authorization: Bearer {token}` 并写入 SecurityContext。
- `AuthTokenService` 改为生成 HMAC-SHA256 JWT，载荷包含用户 ID、用户名、角色、签发时间和过期时间。
- `PasswordService` 改为基于 `BCryptPasswordEncoder` 的密码哈希与校验。
- 保留 `@RequiredRole` 注解，敏感接口继续使用 `ADMIN`、`RISK_OFFICER`、`OPERATOR` 角色控制。
- 新增 `docs/security-design.md` 说明认证流程、JWT 设计、BCrypt 存储和 401/403 语义。

### 验证结果

- Maven 测试通过：`Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`。
- 前端脚本语法检查通过。
- 公开文案敏感词检查通过。

## Redis 缓存与接口限流增强

### 目标

将车次查询缓存从单一本地 TTL 缓存升级为支持 local / Redis 切换的缓存层，同时为高频查询、下单、支付回调和风险处置接口增加轻量限流保护。

### 开发前状态

- Spring Security + JWT + BCrypt 权限体系升级已完成并推送到远端。
- 默认环境仍使用 H2 演示数据库和本地运行方式。
- 车次查询已有本地 TTL 缓存，缺少 Redis 模式和统一限流能力。

### 主要内容

- 新增 `spring-boot-starter-data-redis` 依赖。
- 新增 `TrainSearchCacheStore` 抽象，提供 `LocalTrainSearchCacheStore` 和 `RedisTrainSearchCacheStore` 两种实现。
- `railway.cache.train-search.mode` 默认使用 `local`，可配置为 `redis`；Redis 模式异常时回退到本地缓存。
- 缓存统计新增 `cacheMode`、`configuredMode`、`redisAvailable` 和 `localFallback` 等字段。
- 新增 `RateLimitService`，支持本地固定窗口限流和 Redis `INCR + EXPIRE` 限流。
- 对 `GET /api/trains/search`、`POST /api/orders`、`POST /api/payments/callback`、`POST /api/risks/{id}/handle` 增加限流保护。
- 超过限流阈值时返回 HTTP 429，错误码为 `TOO_MANY_REQUESTS`。
- 新增 `GET /api/rate-limit/summary`，供管理员查看限流模式和拦截统计。
- 前端缓存管理区展示缓存模式、TTL、命中、未命中、失效和本地 fallback 状态，并展示限流模式和拦截次数。
- 新增 `docs/cache-and-rate-limit-design.md`，说明缓存切换、缓存 key、失效场景、限流 key 和 429 处理。

### 验证结果

- Maven 测试通过：`Tests run: 23, Failures: 0, Errors: 0, Skipped: 0`。
- 前端脚本语法检查通过。
- 公开文案敏感词检查通过。

### 当前提交状态

- 本轮改动计划提交为 `add redis cache and rate limiting`。
- 本轮提交仅保留在本地，不自动推送远端。

### 后续建议

- 在独立 Redis 环境中补充手工联调和部署说明。
- 后续可增加限流白名单、分接口配置化阈值和更细粒度的缓存监控。

## Redis 联调说明与限流规则配置化

### 目标

完善 Redis 模式配置和联调说明，将限流阈值从 Controller 硬编码迁移到 `application.yml`，并在限流统计接口和前端展示当前规则配置。

### 开发前状态

- `e38e0de add redis cache and rate limiting` 已通过普通 push 更新到远端。
- 默认缓存和限流模式为 `local`，Maven 测试不依赖 Redis。
- 当前环境未检测到可用 Redis 服务，`localhost:6379` 连接失败。

### 主要内容

- `railway.rate-limit.rules` 新增 `train-search`、`order-create`、`payment-callback`、`risk-handle` 四类规则。
- Controller 调用 `RateLimitService` 时只传规则名和业务 key，不再传硬编码阈值。
- `RateLimitSummary` 返回当前 rules 配置，便于管理端查看限流阈值。
- 前端缓存与限流区域展示规则名、次数上限和窗口秒数。
- `docs/cache-and-rate-limit-design.md` 补充 Redis 模式启动、local / Redis 切换、缓存联调、限流联调、fallback 和排查说明。

### 验证结果

- Maven 测试通过：`Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`。
- 前端脚本语法检查通过。
- 公开文案敏感词检查通过。

### 当前提交状态

- 本轮改动计划提交为 `configure redis rate limit rules`。
- 本轮提交仅保留在本地，不自动推送远端。

### 后续建议

- 在真实 Redis 环境中按文档步骤执行缓存 key、限流 key 和 429 行为联调。
- 如后续部署多实例，可将 Redis 模式作为生产 profile 的默认配置。

## 支付校验与退款流水闭环

### 目标

在已有支付流水和回调幂等基础上，增强支付回调安全校验，并补齐退票后的退款流水，使资金进入和资金退回都能被追踪。

### 开发前状态

- `bc20f5b configure redis rate limit rules` 已同步到远端。
- 系统已有支付流水、支付回调幂等、订单退票和退票后风控。
- 支付回调尚未校验签名和金额，退票后尚未生成退款流水。

### 主要内容

- 支付回调新增 `amount`、`timestamp`、`signature` 和 `channelPaymentNo`。
- 支付回调使用 HMAC-SHA256 签名校验、时间戳容忍窗口和金额一致性校验。
- 新增 `refund_records` 表和 `RefundStatus`：`PENDING`、`SUCCESS`、`FAILED`。
- 订单退票成功后自动创建 `PENDING` 退款流水。
- 新增退款流水分页查询和退款回调接口。
- 退款回调支持签名校验、金额一致性校验和 `callbackRequestId` 幂等。
- 前端新增退款流水列表、筛选、分页和模拟退款成功/失败回调。
- 文档补充支付签名、退款流水、退款回调和数据库关系说明。

### 验证结果

- Maven 测试通过：`Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`。
- 前端脚本语法检查通过。
- 公开文案敏感词检查通过。

### 当前提交状态

- 本轮改动计划提交为 `add refund records and payment verification`。
- 本轮提交仅保留在本地，不自动推送远端。

### 后续建议

- 增加退款重试、退款人工补偿和对账报表。
- 接入真实渠道时补充渠道证书、回调来源校验和渠道错误码映射。

## Outbox 事件表与交易事件解耦

### 目标

在不引入消息队列的前提下，为核心交易动作增加 Outbox 事件记录、派发、重试和失败观测能力，为后续异步化改造预留边界。

### 开发前状态

- `b822aca add refund records and payment verification` 已与远端同步。
- 系统已有订单、支付、退款、风控、缓存和限流能力。
- 后置动作主要仍在同步流程中直接执行。

### 主要内容

- 新增 `outbox_events` 表和 `OutboxEventStatus`。
- 新增 `OutboxEventPublisher`，在业务事务内写入事件。
- 新增 `OutboxEventDispatcher`，支持定时扫描和手动派发。
- 新增 `OutboxEventHandler` 和 `OperationLogEventHandler`。
- 接入 `ORDER_PAID`、`ORDER_REFUNDED`、`ORDER_CLOSED`、`PAYMENT_SUCCEEDED`、`PAYMENT_FAILED`、`REFUND_CREATED`、`REFUND_SUCCEEDED`、`REFUND_FAILED`、`RISK_EVENT_CREATED`、`RISK_EVENT_HANDLED`。
- 新增 `GET /api/outbox-events` 和 `POST /api/outbox-events/dispatch`，仅允许管理员访问。
- 前端新增事件中心，支持状态筛选、事件类型筛选、分页和手动派发。
- 文档新增 `docs/outbox-design.md`。

### 设计说明

本轮保留原有同步风控、缓存失效和关键操作日志逻辑，Outbox 当前承担事务事件记录、轻量派发和失败观测。后续接入消息队列时，可以将 handler 替换为消息生产者。

### 验证结果

- Maven 测试通过：`Tests run: 30, Failures: 0, Errors: 0, Skipped: 0`。
- 前端脚本语法检查通过。
- 公开文案敏感词检查通过。

### 当前提交状态

- 本轮改动计划提交为 `add outbox event processing`。
- 本轮提交仅保留在本地，不自动推送远端。

### 后续建议

- 增加失败事件手动重试接口。
- 增加事件处理耗时和失败率统计。
- 将 Outbox 派发器替换为消息队列生产者。

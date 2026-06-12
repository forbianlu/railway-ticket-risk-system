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
| Outbox 失败重试与统计监控增强 | `enhance outbox retry and metrics` | 失败事件单条/批量重试、事件统计、失败率和积压监控 |
| OpenAPI 与 Docker 工程化增强 | `add openapi and docker deployment` | Swagger UI、OpenAPI JSON、Dockerfile、Docker Compose 和部署指南 |

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

## Outbox 失败重试与统计监控增强

### 目标

在现有 Outbox 事件表和派发器基础上，增强事件运行管理能力，支持管理员重新入队失败事件，并通过统计接口和前端事件中心观察事件积压、失败率和状态分布。

### 开发前状态

- `ab12494 add outbox event processing` 已与远端同步。
- 系统已有 `outbox_events` 表、事件发布器、派发器、handler 机制、事件分页查询和手动派发接口。
- 失败事件只能等待自动重试或保留为 `FAILED`，缺少人工重新入队入口和运行统计。

### 主要内容

- 新增 `GET /api/outbox-events/summary`，统计事件总数、状态分布、类型分布、失败类型分布、失败率、积压数量和平均处理耗时。
- 新增 `POST /api/outbox-events/{id}/retry`，仅允许管理员将单条 `FAILED` 事件重新置为 `PENDING`。
- 新增 `POST /api/outbox-events/retry-failed`，支持批量重新入队当前全部失败事件。
- 手动重试保留历史 `retryCount` 和 `lastError`，设置 `nextRetryAt = now`，清空 `processedAt`，事件实际处理仍由派发器完成。
- 派发器增加 `PROCESSING` 超时恢复策略，超过 5 分钟未更新的处理中的事件会重新进入 `PENDING`。
- 前端事件中心新增统计卡片、按类型/状态统计、失败事件单条重试按钮和批量重试按钮。
- 文档补充 Outbox 重试、统计口径、积压数量、失败率和事件中心说明。

### 验证结果

- 覆盖失败事件单条重试、非失败事件拒绝重试、无令牌和角色不足、批量重试、summary 统计和原有 Outbox 派发链路。
- Maven 测试、前端脚本检查和公开文案敏感词检查在本轮提交前执行。

### 当前提交状态

- 本轮改动计划提交为 `enhance outbox retry and metrics`。
- 本轮提交仅保留在本地，不自动推送远端。

### 后续建议

- 增加处理器级别的失败告警。
- 增加事件处理耗时趋势。
- 将 Outbox handler 逐步替换为消息队列生产者。

## OpenAPI 与 Docker 工程化增强

### 目标

增强系统工程化展示能力，提供 OpenAPI / Swagger UI 接口文档，并通过 Docker Compose 支持本地启动后端、MySQL 和 Redis。

### 开发前状态

- `0d8c776 enhance outbox retry and metrics` 已与远端同步。
- 系统默认通过 H2、本地缓存和本地限流运行，Docker Compose 编排尚未形成可执行入口。
- API 文档主要由 Markdown 维护，缺少可交互的 Swagger UI。

### 主要内容

- 引入 `springdoc-openapi-ui`，暴露 `/v3/api-docs` 和 `/swagger-ui/index.html`。
- 新增 `OpenApiConfig`，配置 API 标题、版本、描述和 Bearer Token 安全方案。
- 为主要 Controller 增加 OpenAPI Tag 和接口 summary。
- Spring Security 放行 Swagger UI、OpenAPI JSON、webjars 和 H2 控制台相关路径。
- 新增 `Dockerfile`，使用 Maven 多阶段构建后端镜像。
- 新增 `.dockerignore`，避免复制 `.git`、构建产物和本地反馈文件。
- 新增 `docker-compose.yml`，编排后端、MySQL 8 和 Redis 7。
- `application.yml` 增加 `docker` profile，默认仍使用 H2/local，Docker profile 使用 MySQL/Redis。
- 新增 `docs/deployment-guide.md`，说明 H2 启动、Swagger 使用、Docker Compose、环境变量和排查方式。
- README 和设计文档同步补充 OpenAPI、Swagger、Docker Compose 和 profile 说明。

### 设计说明

默认 Maven test 继续使用 H2、本地缓存和本地限流，不依赖 Docker、MySQL 或 Redis。Docker Compose 使用演示环境变量启动 MySQL 与 Redis，适合本地工程化联调。

### 验证结果

- 新增 OpenAPI 匿名访问测试，确认 `/v3/api-docs` 可访问。
- 保留受保护接口未登录返回 401 的测试。
- Maven 测试通过：`Tests run: 34, Failures: 0, Errors: 0, Skipped: 0`。
- 前端脚本语法检查通过。
- 公开文案敏感词检查通过。
- 当前环境未安装 Docker CLI，未执行 `docker compose config`；Docker Compose 配置已写入文档，后续可在安装 Docker 的环境执行联调。

### 当前提交状态

- 本轮改动计划提交为 `add openapi and docker deployment`。
- 本轮提交仅保留在本地，不自动推送远端。

### 后续建议

- 增加镜像版本标签和容器健康检查看板。
- 增加独立部署环境的启动验证记录。
- 在 CI 中增加可选的 Dockerfile build 校验。

## 演示数据内容增强

### 目标

增强默认启动后的业务数据密度，让运营看板、订单列表、支付流水、退款流水、风险事件、操作日志和 Outbox 事件中心都有可展示内容。

### 开发前状态

- 系统已有完整交易、支付、退款、风控、Outbox、OpenAPI 和 Docker Compose 能力。
- 基础初始化数据主要集中在少量车站、车次和库存，前端部分列表在空库启动后内容较少。
- `6c11557 fix docker mysql host port` 已与远端同步，Docker Compose 已可使用宿主机 `3307` 访问 MySQL。

### 主要内容

- 将 `DemoDataInitializer` 拆分为用户、车站、车次、库存、订单资金流水、风险、操作日志和 Outbox 多个幂等初始化步骤。
- 新增 `railway.demo-data.enabled` 配置，默认启用演示数据；关闭后仍保留演示账号初始化。
- 初始化 17 个车站、16 趟车次和未来 14 天库存，库存数量按车次、日期和座席类型差异化生成。
- 初始化 48 条订单，覆盖 `PENDING_PAYMENT`、`PAID`、`CLOSED`、`REFUNDED`。
- 初始化支付流水，覆盖 `PENDING`、`SUCCESS`、`FAILED`。
- 初始化退款流水，覆盖 `PENDING`、`SUCCESS`、`FAILED`。
- 初始化风险事件和处置历史，覆盖 `PENDING`、`CONFIRMED`、`FALSE_POSITIVE`、`CLOSED`。
- 初始化操作日志和 Outbox 事件，让审计页面和事件中心具备可浏览数据。
- 新增 `docs/demo-data-design.md`，说明初始化开关、数据规模、状态分布和重置方式。

### 测试结果

- 新增演示数据初始化集成测试，覆盖数据规模、状态分布和重复执行不重复插入。
- Maven 测试通过：`Tests run: 35, Failures: 0, Errors: 0, Skipped: 0`。
- 前端脚本检查和公开文案敏感词检查在本轮提交前执行。

### 当前提交状态

- 本轮改动计划提交为 `enrich demo data`。
- 本轮提交仅保留在本地，不自动推送远端。

### 后续建议

- 下一阶段可在不改变接口的前提下优化前端视觉层级和数据密度。
- 可增加更多可控数据集，例如节假日高峰、退款异常批次和风险处置 SLA。

## 前端管理台视觉重构

### 目标

在不修改后端接口、不引入前端框架的前提下，提升原生 HTML/CSS/JavaScript 管理台的信息层级、视觉质量和可读性。

### 开发前状态

- 系统已有完整交易、支付、退款、风险、Outbox、缓存限流和演示数据能力。
- 前端功能完整，但整体呈现更偏基础功能页，登录入口、看板层级、状态标签和事件中心展示仍有提升空间。

### 主要内容

- 登录入口升级为独立登录屏，展示系统能力、演示账号和后端连接状态。
- 主页面改为侧边导航、顶部状态栏和内容工作区结构。
- 运营看板增加更清晰的指标层级、热门车次进度展示、缓存和限流指标区。
- 订单、支付、退款列表强化车次、金额、状态和低库存信息展示。
- 风险模块保留处置和历史查询能力，风险报表保持右侧运营分析区。
- Outbox 事件中心强化统计卡片、状态分布、类型分布、失败重试和手动派发入口。
- 新增 `docs/frontend-design.md` 记录前端管理台设计目标、结构和未改变范围。

### 验证结果

- Maven 测试、前端脚本检查、公开文案敏感词检查和 Docker 验证在本轮提交前执行。

### 当前提交状态

- 本轮改动计划提交为 `redesign frontend dashboard`。
- 本轮提交仅保留在本地，不自动推送远端。

### 后续建议

- 后续可增加只读演示模式提示、局部加载状态和更细粒度的数据图表。
- 如需进一步增强，可补充管理台截图和更多窄屏交互验证。

## 乘客角色和用户端接口

### 目标

在既有运营管理后台基础上增加普通乘客后端边界，为后续乘客购票前端提供接口基础。乘客端与管理端共用订单、支付、退款、风险、Outbox 和操作日志数据，保证双端数据互通。

### 开发前状态

- 系统已有 Spring Security + JWT + BCrypt、订单状态机、支付退款流水、风险处置、Outbox、缓存限流和管理端前端。
- 角色模型包含 `ADMIN`、`RISK_OFFICER`、`OPERATOR`，缺少普通乘客角色。
- 当前管理台前端相关提交已完成并与远端同步，工作区干净。

### 主要内容

- 新增 `USER` 角色，表示普通乘客。
- 演示账号初始化增加 `passenger1`、`passenger2`、`passenger3`，密码均为 `123456`，并继续使用 BCrypt 存储。
- 新增 `/api/passenger/**` 接口：
  - `GET /api/passenger/summary`
  - `GET /api/passenger/orders`
  - `POST /api/passenger/orders`
  - `POST /api/passenger/orders/{id}/pay`
  - `POST /api/passenger/orders/{id}/close`
  - `POST /api/passenger/orders/{id}/refund`
  - `GET /api/passenger/payments`
  - `GET /api/passenger/refunds`
- 新增 `PassengerService`，复用 `OrderService`、`PaymentService`、`RefundService` 完成下单、模拟支付、取消和退票。
- 乘客下单时从 JWT 读取当前 `userId`，不信任前端传入用户 ID。
- 乘客查询和操作订单前校验订单归属，不能支付、关闭或退票其他乘客订单。
- 拦截器增加 `USER` 管理端访问边界，已登录乘客访问订单管理、风险管理、审计日志、Outbox、缓存限流和看板接口时返回 403。
- 演示数据订单用户 ID 改为关联 passenger 演示账号，便于后续用户端页面展示“我的订单”。
- 新增 `docs/passenger-api-design.md`，并同步更新 API、安全、项目大纲、技术笔记、总结和路线文档。

### 测试结果

- 新增 passenger 集成测试，覆盖 USER 登录、乘客概览、我的订单、下单、管理端可见、跨用户操作拒绝、乘客支付、取消、退票、支付流水和退款流水隔离，以及 USER 访问管理端接口返回 403。
- Maven 测试通过：`Tests run: 37, Failures: 0, Errors: 0, Skipped: 0`。

### 当前提交状态

- 本轮改动计划提交为 `add passenger role and APIs`。
- 本轮提交仅保留在本地，不自动推送远端。

### 后续建议

- 下一轮可以开发入口选择页和乘客购票前端。
- 乘客端前端完成后再统一整理 README 的双端平台说明和截图。

## 入口选择页和乘客购票端前端

### 目标

在已完成 `/api/passenger/**` 后端接口的基础上，将前端从单一运营管理台扩展为双端入口结构。入口页负责区分乘客购票服务和运营管理系统，乘客端前端负责接入普通乘客登录、查票、下单、支付、取消、退票、我的订单、我的支付流水和我的退款流水。

### 开发前状态

- 后端已新增 `USER` 角色、乘客演示账号和乘客侧 API。
- 管理端前端已经完成视觉重构和交互优化。
- 当前本地最新后端乘客接口提交为 `a1de926 add passenger role and APIs`。

### 主要内容

- 将原管理端页面保留为 `frontend/admin.html`，原 `app.js` 管理端逻辑不变。
- 将 `frontend/index.html` 改为双端入口选择页，提供乘客购票服务和运营管理系统两个入口。
- 新增 `frontend/passenger.html` 和 `frontend/passenger.js`，实现乘客端登录、概览、查票购票、我的订单、支付流水和退款流水。
- 乘客端使用独立 `railway-passenger-auth` token key，减少和管理端登录状态互相污染。
- 乘客端 401、403、429 和网络错误分别给出不同提示。
- 乘客下单使用 `POST /api/passenger/orders`，支付、取消和退票分别复用乘客侧订单接口。
- 乘客端支付、退票后刷新概览、订单和流水，便于手动验证和管理端数据互通。
- `styles.css` 增加入口页、乘客端布局、乘客订单卡片、乘客端响应式和交互反馈样式。

### 验证结果

- `node --check frontend\app.js` 通过。
- `node --check frontend\passenger.js` 通过。
- 本轮未修改后端 Java 文件，未执行 Maven 测试。

### 当前提交状态

- 本轮计划提交信息为 `add passenger frontend entry`。

## 电子票与订单详情聚合

### 目标

新增电子票记录和订单详情聚合能力，让乘客端可以查看自己的行程单与资金流水，让运营管理端可以从订单维度查看交易、票务、风险、事件和日志链路。

### 主要内容

- 新增 `ticket_records` 表、`TicketRecord` 实体和 `TicketStatus`。
- 支付成功后自动签发电子票，状态为 `ISSUED`。
- 退票成功后将电子票标记为 `REFUNDED` 并记录失效时间。
- 演示数据为已支付和已退票订单补充电子票记录。
- 新增乘客订单详情接口：`GET /api/passenger/orders/{id}/detail`。
- 新增管理端订单详情接口：`GET /api/orders/{id}/detail`。
- 乘客详情只返回本人订单、电子票、支付流水和退款流水。
- 管理端详情额外返回风险事件、Outbox 事件和最近操作日志。
- 乘客端和管理端前端均增加订单详情入口和详情弹窗。

### 验证结果

- Maven 测试通过：`Tests run: 39, Failures: 0, Errors: 0, Skipped: 0`。
- 前端脚本检查通过：`frontend/app.js`、`frontend/passenger.js`。

### 当前提交状态

- 本轮计划提交信息：`add ticket records and order details`。
- 本轮提交仅保留在本地，不自动 push。

### 后续建议

- 后续可实际启动后端和静态前端，手动验证乘客端下单、支付、退票后管理端订单和流水是否同步显示。
- 可以在双端稳定后统一补充 README 截图和平台化说明。

## 乘客常用乘车人与实名信息快照

### 目标

在乘客购票端增加常用乘车人资料能力，让普通乘客可以维护常用旅客、选择乘车人下单，并让订单与电子票保存脱敏实名快照。

### 主要内容

- 新增 `PassengerTraveler`、`PassengerIdType` 和 `passenger_profiles` 表。
- 新增 `PassengerTravelerRepository`、`PassengerTravelerRequest`、`PassengerTravelerResponse`。
- 新增 `/api/passenger/travelers` 查询、新增、更新、删除和设置默认接口。
- 乘客下单接口支持 `travelerId`，后端校验乘车人归属当前 `USER`。
- `TicketOrder` 新增证件类型、脱敏证件号、脱敏手机号快照字段。
- `TicketRecord` 新增证件类型和脱敏手机号快照字段。
- `OrderService` 和 `TicketService` 写入脱敏快照，避免历史订单受乘车人资料后续编辑影响。
- 演示数据为 passenger1、passenger2、passenger3 初始化常用乘车人。
- 乘客端前端新增常用乘车人维护区，购票弹窗支持选择常用乘车人或手动输入。

### 验证结果

- `node --check frontend\app.js` 通过。
- `node --check frontend\passenger.js` 通过。
- Maven 测试通过：`Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`。

### 当前提交状态

- 本轮计划提交信息：`add passenger traveler profiles`。
- 本轮提交仅保留在本地，不自动 push。
## 站内通知中心

### 目标

新增系统内通知能力，让乘客能够在购票端查看订单、支付、出票、关闭、退票和退款结果消息；让运营管理端能够查看通知列表、未读数量、类型分布和状态分布。

### 主要内容

- 新增 `notification_records` 表、`NotificationRecord` 实体、`NotificationType` 和 `NotificationStatus`。
- 新增 `NotificationService`，在下单、支付成功、出票成功、订单关闭、退票、退款成功回调和退款失败回调后创建通知。
- 通知创建使用业务幂等键，避免重复业务动作产生重复通知。
- 新增乘客通知接口：列表、未读统计、单条已读和全部已读。
- 新增管理端通知接口：全量列表和统计概览。
- 通知创建后写入 `NOTIFICATION_CREATED` Outbox 事件，用于后续事件观测和扩展。
- 演示数据为 passenger 账号初始化多类型通知。
- 乘客端新增消息中心和未读角标，管理端新增通知中心。

### 验证结果

- Maven 测试通过：`Tests run: 41, Failures: 0, Errors: 0, Skipped: 0`。
- 前端脚本检查通过：`frontend/app.js`、`frontend/passenger.js`。

### 当前提交状态

- 本轮计划提交信息：`add passenger notification center`。
- 本轮提交仅保留在本地，不自动 push。
## 管理端综合查询中心

- 新增 `/api/search` 管理端综合查询接口。
- 新增 `AdminSearchService`，支持订单、电子票、支付流水、退款流水、乘车人、风险事件、Outbox、通知和操作日志检索。
- 综合查询仅允许 `ADMIN`、`RISK_OFFICER`、`OPERATOR` 访问，`USER` 返回 403。
- 查询结果对证件号和手机号做脱敏展示，不返回密码、JWT、签名密钥或完整个人敏感字段。
- 管理端前端新增综合查询模块，并复用已有订单详情弹窗。
- 集成测试覆盖角色权限、关键词校验、按类型查询、链路字段和敏感字段脱敏。
## 入口页视觉重设计

- 将 `frontend/index.html` 从普通双端入口升级为平台门户式首页。
- 保留进入 `passenger.html` 和 `admin.html` 的入口，不修改乘客端和管理端业务逻辑。
- 使用 CSS 渐变、轨道线、内联 SVG 和克制动效实现临时 Hero 背景。
- 新增 `frontend/assets/home-hero-background-prompt.md`，用于后续生成正式首页背景图。

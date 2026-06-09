# 铁路客运票务与风控运营管理系统项目大纲

## 1. 项目背景

铁路客运系统涉及车次计划、座位库存、订单交易、退改签、异常行为识别、人工处置和运营统计。该系统围绕客运售票后台的核心流程进行建模，重点关注交易状态、库存一致性、风控规则、权限控制、日志审计和运营报表。

## 2. 项目目标

系统目标是提供一个可运行的铁路客运票务与风控运营系统，并逐步形成乘客购票端与运营管理端双端结构：

- 乘客可以查询车次和余票。
- 乘客通过 `USER` 角色访问 `/api/passenger/**`，查看和操作自己的订单、支付流水和退款流水。
- 乘客可以创建待支付订单并锁定余票。
- 乘客可以模拟支付，支付成功后订单生效。
- 待支付订单可以手动关闭或超时自动关闭并释放库存。
- 重复提交同一购票请求不会生成重复订单。
- 乘客可以退票并释放余票。
- 退票后自动创建退款流水，并通过退款回调确认资金处理结果。
- 热门线路查询可以命中 local / Redis 缓存，库存变更后缓存自动失效。
- 高频接口具备限流保护，超过阈值返回 429。
- 核心交易动作写入 Outbox 事件表，为异步处理和后续消息队列接入预留边界。
- 系统提供 OpenAPI / Swagger UI，便于查看和调试接口。
- 系统提供 Docker Compose，用于本地启动后端、MySQL 和 Redis。
- 系统默认初始化一组完整演示数据，启动后即可查看看板、订单、支付、退款、风险和事件中心。
- 并发购票时库存不会超卖。
- 系统能识别异常交易行为。
- 运营后台可以查看订单、支付流水、风险事件和统计指标。
- 风控人员可以处置风险事件，运营人员无权执行敏感操作。
- 关键业务动作可记录审计日志。

## 3. 用户角色

- 乘客：查询车次、购票、支付、取消待支付订单、退票、查看自己的订单和资金流水。
- 运营人员：查看订单、支付流水、缓存和运营统计。
- 风控人员：查看风险事件、处置风险事件、查看审计日志。
- 管理员：查看日志、处理风险事件、管理缓存。

## 4. 核心业务模块

### 4.1 基础数据

- 车站管理：车站编码、车站名称、城市、状态。
- 车次管理：车次号、始发站、终到站、出发时间、到达时间。
- 座位库存：车次、日期、座位类型、总座位、剩余座位和票价。
- 演示数据：默认生成多线路车次、未来 14 天库存、多状态订单、支付退款流水、风险事件、日志和 Outbox 事件。

### 4.2 票务交易

- 车次查询：按出发站、到达站、乘车日期查询。
- 乘客端 API：`/api/passenger/**` 提供我的订单、下单、支付、取消、退票、我的支付流水和我的退款流水。
- 查询缓存：按出发站、到达站、乘车日期缓存余票结果，支持 local / Redis 模式。
- 接口限流：对车次查询、下单、支付回调和风险处置进行固定窗口限流，阈值由配置文件维护。
- 创建订单：校验余票、校验幂等号、扣减库存、生成待支付订单。
- 模拟支付：待支付订单支付成功后进入已支付状态，并触发支付后风控规则。
- 支付流水：为待支付订单创建支付流水，记录支付号、渠道、金额和支付状态。
- 支付回调：模拟支付成功/失败回调，校验签名、金额和 `callbackRequestId` 幂等。
- 退款流水：退票后创建退款流水，记录退款状态、渠道退款号和回调结果。
- 退款回调：模拟退款成功/失败回调，校验签名、金额和回调幂等。
- 超时关闭：待支付订单超过支付截止时间后关闭，释放已锁定库存。
- 订单查询：按用户、订单状态、订单号、创建日期组合筛选，并支持分页返回。
- 退票：更新订单状态，释放库存。
- 并发控制：库存表使用乐观锁版本号，降低并发扣减时的超卖风险。
- 缓存一致性：锁票、支付、关闭和退票事务提交后失效对应线路日期缓存。
- 幂等提交：同一 `userId + requestId` 重复请求直接返回原订单。
- 交易事件：支付、关闭、退票、退款和风险处置写入 Outbox 事件，支持派发、失败重试、统计监控和失败记录。

订单状态机：

- `PENDING_PAYMENT`：订单创建成功，库存已锁定，等待支付。
- `PAID`：支付成功，订单生效，进入风控统计。
- `REFUNDED`：已支付订单退票，库存释放。
- `CLOSED`：待支付订单主动关闭或超时关闭，库存释放。
- `CANCELLED`：预留状态，便于后续扩展取消订单场景。

### 4.3 风控识别

当前使用规则引擎式判断：

- 短时间多次购票：同一用户 10 分钟内支付成功订单超过 3 次。
- 频繁退票：同一用户 7 天内退票超过 3 次。
- 高价值异常：同一用户单日订单金额超过阈值。

风控结果包括：

- 风险类型。
- 风险等级。
- 触发原因。
- 关联订单。
- 处理状态：待处理、已确认风险、误报、已关闭。
- 处置备注、处理人、处理时间。
- 处置历史记录。
- 分页筛选：按状态、场景、用户、订单号和创建日期查询风险事件。

风控实现采用规则引擎结构：

- `RiskRule` 定义统一规则接口。
- `RiskScene` 区分支付成功后、退票后等触发场景。
- `RiskService` 负责调度规则并生成风险事件。
- 每条规则独立实现，便于后续扩展。
- `RiskEventHandleRecord` 记录每次人工处置动作，支持审计追踪。
- 风险运营报表按状态和场景统计风险分布，展示处置完成率和平均首次处置耗时。

### 4.4 运营看板

- 总订单数。
- 待支付订单数。
- 有效订单数。
- 已关闭订单数。
- 退票订单数。
- 退票率。
- 风险率。
- 风险事件数。
- 风险待处理占比、确认风险占比、误报率和处置完成率。
- 按风险状态、风险场景聚合统计。
- 热门车次。

### 4.5 日志审计

- 下单日志。
- 支付日志。
- 订单关闭日志。
- 退票日志。
- 风控触发日志。
- 风险处置日志。
- 缓存管理日志。
- Outbox 事件派发日志。

### 4.6 事件中心

- 事件落库：业务事务内写入 `outbox_events`。
- 事件派发：定时任务或手动接口扫描待处理事件。
- 重试机制：处理失败后记录错误并按重试次数延后处理。
- 失败观测：达到最大重试次数后标记为 `FAILED`。
- 手动运维：管理员可以对单条失败事件或全部失败事件重新入队。
- 统计监控：展示事件状态分布、事件类型分布、失败率和积压数量。
- 当前策略：保留原同步风控、缓存失效和关键日志逻辑，Outbox 用于渐进式解耦。

### 4.7 登录与角色权限

- 演示账号：管理员、风控专员、运营人员、普通乘客。
- 登录接口：使用 BCrypt 校验账号密码后签发带过期时间的 JWT。
- 鉴权过滤：前端携带 `Authorization: Bearer {token}`，后端 Spring Security 过滤器解析用户身份并写入 SecurityContext。
- 角色控制：使用 `@RequiredRole` 标注敏感接口，风控处置和审计日志只允许管理员或风控专员访问。
- 乘客边界：`USER` 只能访问乘客端接口和公开查询接口，不能访问管理端订单、风险、日志、Outbox、缓存限流和运营看板接口。
- 前端联动：登录后通过 Bearer Token 调用受保护接口，并根据当前角色展示风险处理、事件中心和审计日志等操作结果。

### 4.8 前端管理台

- 登录入口：独立登录屏展示系统能力、演示账号和后端连接状态。
- 导航结构：使用侧边导航组织运营看板、车次查询、订单、支付、退款、风险、事件和日志模块。
- 数据展示：通过指标卡、状态标签、表格、事件列表和进度条展示演示数据。
- 响应式：桌面端保持高信息密度，窄屏下切换为单列布局。

### 4.9 工程化运行

- OpenAPI：`/v3/api-docs` 输出接口定义，`/swagger-ui/index.html` 提供交互式接口页面。
- Dockerfile：构建后端运行镜像。
- Docker Compose：编排后端、MySQL 和 Redis。
- Profile：默认 H2/local 模式用于本地开发和 CI，`docker` profile 使用 MySQL 和 Redis。

### 4.10 电子票与订单详情

- 支付成功后生成 `ticket_records` 电子票记录。
- 电子票状态包括 `ISSUED`、`REFUNDED`、`CANCELLED`。
- 退票成功后电子票标记为 `REFUNDED`，并记录失效时间。
- 乘客端订单详情展示本人订单、电子票、支付流水和退款流水。
- 管理端订单详情聚合订单、电子票、支付流水、退款流水、风险事件、Outbox 事件和最近操作日志。

## 5. 数据库设计

核心表：

- `app_users`
- `stations`
- `trains`
- `seat_inventories`
- `ticket_orders`
- `ticket_records`
- `passenger_profiles`
- `payment_records`
- `refund_records`
- `risk_events`
- `risk_event_handle_records`
- `outbox_events`
- `operation_logs`

详见 `docs/database-design.md`。

## 6. 接口设计

核心接口：

- `GET /api/health`
- `GET /api/stations`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/trains/search`
- `GET /api/passenger/summary`
- `GET /api/passenger/orders`
- `GET /api/passenger/orders/{id}/detail`
- `GET /api/passenger/travelers`
- `POST /api/passenger/travelers`
- `PUT /api/passenger/travelers/{id}`
- `DELETE /api/passenger/travelers/{id}`
- `POST /api/passenger/travelers/{id}/default`
- `POST /api/passenger/orders`
- `POST /api/passenger/orders/{id}/pay`
- `POST /api/passenger/orders/{id}/close`
- `POST /api/passenger/orders/{id}/refund`
- `GET /api/passenger/payments`
- `GET /api/passenger/refunds`
- `POST /api/orders`
- `POST /api/orders/{id}/pay`
- `POST /api/orders/{id}/close`
- `POST /api/orders/close-expired`
- `POST /api/orders/{id}/refund`
- `GET /api/orders`
- `GET /api/orders/{id}/detail`
- `POST /api/payments`
- `POST /api/payments/callback`
- `GET /api/payments`
- `GET /api/refunds`
- `POST /api/refunds/callback`
- `GET /api/risks`
- `GET /api/risks/summary`
- `POST /api/risks/{id}/handle`
- `GET /api/risks/{id}/handle-records`
- `GET /api/cache/train-search`
- `GET /api/rate-limit/summary`
- `GET /api/outbox-events`
- `POST /api/outbox-events/dispatch`
- `GET /api/outbox-events/summary`
- `POST /api/outbox-events/{id}/retry`
- `POST /api/outbox-events/retry-failed`
- `GET /api/dashboard/summary`
- `GET /v3/api-docs`

详见 `docs/api-design.md`。

## 7. 开发阶段

### 第一阶段：核心链路

- Spring Boot 项目初始化。
- H2 演示数据库。
- 车次查询。
- 下单与退票。
- 订单支付状态机和超时关闭。
- 支付流水表、支付回调签名校验和回调幂等。
- 退款流水表、退款回调签名校验和回调幂等。
- 订单分页筛选。
- 运营看板状态指标和风险指标。
- 基础风控规则。
- 风险处置闭环和处置历史。
- 风险事件分页筛选和风险运营报表。
- 登录与角色权限。
- 车次查询 local / Redis TTL 缓存。
- 高频接口限流。
- Outbox 事件表、轻量派发器、失败重试和统计监控。
- OpenAPI / Swagger UI。
- 演示数据初始化和幂等校验。
- 并发购票防超卖集成测试。
- 订单幂等提交。
- 操作日志。
- 原生前端管理台。

### 第二阶段：工程增强

- MySQL 持久化配置。
- Docker Compose 编排。
- 演示数据规模和展示内容增强。
- GitHub Actions 自动测试。
- 更丰富的异常场景测试。
- 接口压测和并发验证脚本。

### 第三阶段：可扩展能力

- Redis 运维监控、端到端联调记录和限流指标可视化。
- Spring Security + JWT + BCrypt 认证授权方案。
- 延时队列或任务调度优化订单超时关闭。
- 风险等级、处理人、处理 SLA 和导出报表。
- 退款补偿、退款重试和对账报表。
## 双端前端结构

当前前端已从单一管理端扩展为入口页、乘客端和管理端三部分：

- `frontend/index.html`：平台入口选择页，不要求登录，负责进入乘客购票服务或运营管理系统。
- `frontend/passenger.html`：乘客购票端，使用普通乘客账号登录后可查票、下单、支付、取消、退票，并查看我的订单、我的支付流水和我的退款流水。
- `frontend/admin.html`：运营管理端，保留原有管理台能力，用于订单、支付退款、风险、Outbox、缓存限流和审计日志管理。

乘客端和管理端共用后端核心表和业务链路。乘客端产生的订单、支付流水、退款流水、风险事件和 Outbox 事件仍可在管理端查看和治理。
# 站内通知中心补充

系统新增站内通知能力，乘客端可以查看下单、支付成功、出票、订单关闭、退票、退款成功和退款失败等业务消息；运营管理端可以查看通知列表和统计分布。该能力复用现有订单、支付、退款、电子票和 Outbox 链路，不改变核心交易状态机，也不接入外部推送渠道。

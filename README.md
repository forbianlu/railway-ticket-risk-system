# Railway Ticket Risk System

## 项目简介

铁路客运票务与风控运营管理系统，面向铁路局信息技术岗、银行科技岗等校招场景设计。项目模拟客运交易链路中的车次查询、余票锁定、订单支付、超时关闭、退票退款、风险识别、风险处置、角色权限、日志审计和运营统计。

这是一个后端主导型交易与风控系统，重点展示 Java 后端分层设计、事务一致性、状态机、幂等、防超卖、风控规则、权限审计、运营报表和集成测试能力。当前版本使用 H2 演示数据库，同时提供 MySQL profile；支付、权限和缓存均为项目内模拟实现，没有接入真实支付 SDK、Redis、MQ 或 Spring Security。

## 核心亮点

- 完整票务闭环：查询车次、创建待支付订单、锁定库存、支付确认、退票释放库存、超时关闭释放库存。
- 订单状态机：支持 `PENDING_PAYMENT -> PAID -> REFUNDED`，以及待支付订单关闭为 `CLOSED`。
- 支付流水与回调幂等：支持创建模拟支付流水、处理支付成功/失败回调，并用 `callbackRequestId` 防止重复回调重复触发风控。
- 库存防超卖：座位库存使用 JPA 乐观锁版本号，锁票冲突时返回明确提示。
- 订单幂等提交：下单支持 `requestId`，同一用户重复提交同一请求不会重复扣库存。
- 热门查询缓存：车次余票查询支持本地 TTL 缓存，锁票、支付、关闭和退票后按线路日期失效缓存。
- 并发压测验证：集成测试模拟 16 个请求抢 1 张票，验证只生成 1 个订单且库存不为负。
- 风控规则引擎：将风险规则拆成独立 `RiskRule`，按 `RiskScene` 调度。
- 风险处置闭环：风险事件支持 `PENDING`、`CONFIRMED`、`FALSE_POSITIVE`、`CLOSED` 状态流转，保存处置备注、处理人、处理时间和处置历史。
- 风险运营分析：风险事件支持状态、场景、用户、订单号和日期分页筛选，并提供状态占比、场景分布和处置完成率报表。
- 角色权限控制：支持管理员、风控专员、运营人员演示账号，使用签名令牌和注解式角色校验保护敏感接口。
- 订单运营管理：订单列表支持用户、状态、订单号、创建日期筛选，并返回分页元信息。
- 运营看板：统计总订单、待支付、已支付、已关闭、已退票、退票率、风险率、未处理风险和热门车次。
- 工程化交付：提供接口集成测试、H2 演示库、MySQL profile、Docker Compose、GitHub Actions。

## 技术栈

- 后端：Java 8, Spring Boot 2.7, Spring Web, Spring Data JPA, Bean Validation
- 权限：自定义签名令牌、HandlerInterceptor、`@RequiredRole` 注解式角色校验
- 缓存：本地 TTL 缓存、缓存命中统计、事务提交后缓存失效
- 数据库：H2 本地演示，MySQL profile
- 前端：原生 HTML, CSS, JavaScript 管理台
- 工程化：Maven, Docker Compose, GitHub Actions

## 系统架构与流程

![系统架构](docs/assets/system-architecture.svg)

### 购票交易流程

![票务交易流程](docs/assets/ticket-flow.svg)

### 权限流程

![登录鉴权与角色权限](docs/assets/auth-access-control.svg)

### 缓存流程

![车次余票查询缓存](docs/assets/cache-flow.svg)

### 防超卖流程

![并发购票防超卖](docs/assets/oversell-control.svg)

### 幂等提交流程

![订单幂等提交](docs/assets/idempotency-flow.svg)

### 订单状态流转

![订单支付状态流转](docs/assets/order-state-flow.svg)

## 界面截图

### 运营看板与车次查询

![运营看板与车次查询](docs/assets/screenshots/dashboard.png)

### 订单管理

![订单管理](docs/assets/screenshots/orders.png)

### 风险事件处理

![风险事件处理](docs/assets/screenshots/risk-events.png)

## 风控设计

![风控规则引擎](docs/assets/risk-engine.svg)

当前规则：

- `RapidPurchaseRiskRule`：同一用户 10 分钟内支付成功订单达到 3 次。
- `HighAmountRiskRule`：同一用户当日有效支付金额超过 1000 元。
- `FrequentRefundRiskRule`：同一用户 7 天内退票达到 3 次。

## 功能模块说明

| 模块 | 说明 |
| --- | --- |
| 车次查询 | 按出发站、到达站和日期查询车次、票价和余票，支持本地 TTL 缓存 |
| 订单管理 | 创建待支付订单、支付、关闭、超时关闭、退票和分页筛选 |
| 库存控制 | 下单锁定库存，关闭或退票释放库存，使用 JPA 乐观锁防止超卖 |
| 支付流水 | 创建模拟支付流水，处理成功/失败回调，支持回调幂等和流水分页 |
| 风险管理 | 支付成功和退票后触发规则，生成风险事件并支持人工处置闭环 |
| 风险运营 | 风险事件分页筛选，按状态和场景统计风险分布、误报率和处置完成率 |
| 运营看板 | 展示订单状态分布、退票率、风险率、未处理风险和热门车次 |
| 缓存管理 | 查看车次查询缓存统计，管理员可清空缓存 |
| 权限审计 | 登录令牌、角色权限、操作日志、风险处置历史 |

## 主要接口

```text
GET  /api/health
POST /api/auth/login
GET  /api/auth/me
GET  /api/stations
GET  /api/trains/search?from=BJP&to=SHH&date=2026-06-01
POST /api/orders
POST /api/orders/{id}/pay
POST /api/orders/{id}/close
POST /api/orders/close-expired
POST /api/orders/{id}/refund
GET  /api/orders?userId=1001&status=PAID&page=0&size=10
POST /api/payments
POST /api/payments/callback
GET  /api/payments?status=SUCCESS&page=0&size=10
GET  /api/risks?status=PENDING&scene=ORDER_CREATED&userId=1001&page=0&size=10
GET  /api/risks/summary
POST /api/risks/{id}/handle
GET  /api/risks/{id}/handle-records
GET  /api/cache/train-search
DELETE /api/cache/train-search
GET  /api/dashboard/summary
GET  /api/logs
```

## 数据库核心表

| 表 | 说明 |
| --- | --- |
| `app_users` | 演示用户、角色和启用状态 |
| `stations` | 车站基础数据 |
| `trains` | 车次基础数据 |
| `seat_inventories` | 乘车日期、座位类型、余票、票价和乐观锁版本 |
| `ticket_orders` | 订单号、乘客、金额、状态、支付/退票/关闭时间 |
| `payment_records` | 支付流水号、支付状态、渠道、回调请求号和支付时间 |
| `risk_events` | 风险类型、等级、场景、状态和最新处置信息 |
| `risk_event_handle_records` | 风险事件处置前后状态、备注、操作人和操作时间 |
| `operation_logs` | 下单、支付、关闭、退票、风控触发和处置等审计日志 |

## 目录结构

```text
railway-ticket-risk-system
├── backend              # Spring Boot 后端
├── frontend             # 管理台原型
├── docs                 # 项目文档、ER 图、架构图
├── docker-compose.yml   # MySQL + 后端编排
└── README.md
```

## 快速启动

### 后端启动

```bash
cd backend
mvn spring-boot:run
```

启动后访问：

- API 健康检查：`http://localhost:8080/api/health`
- H2 控制台：`http://localhost:8080/h2-console`

H2 JDBC URL：

```text
jdbc:h2:mem:railway
```

使用 MySQL profile：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

### 前端启动

```bash
cd frontend
node static-server.js
```

访问：

```text
http://127.0.0.1:5173
```

## 演示账号

| 账号 | 密码 | 角色 | 可演示能力 |
| --- | --- | --- | --- |
| `admin` | `admin123` | 系统管理员 | 查看日志、处理风险事件 |
| `risk` | `risk123` | 风控专员 | 查看日志、处理风险事件 |
| `ops` | `ops123` | 运营人员 | 查看运营数据，不能处理风险事件 |

## 测试方式

```bash
cd backend
mvn test
```

当前测试结果：

```text
Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
```

前端脚本语法检查：

```bash
node --check frontend\app.js
```

并发购票压测脚本：

```bash
node scripts/concurrent-purchase.js 30
```

可选环境变量：

```text
API_BASE=http://localhost:8080/api
REQUESTS=50
FROM=BJP
TO=SHH
DATE=2026-06-01
TRAIN_ID=1
INVENTORY_ID=1
```

已验证链路：

- 创建待支付订单后余票锁定，支付成功后触发短时多次购票、高金额订单风险。
- 支付流水创建后为 `PENDING`，成功回调后变为 `SUCCESS` 并确认订单支付，重复回调不重复触发风控。
- 支付失败回调后流水变为 `FAILED`，订单仍保持待支付，可重新创建流水或等待超时关闭。
- 连续退票后库存释放，并触发频繁退票风险。
- 待支付订单可手动关闭或超时批量关闭，关闭后库存自动释放。
- 风险事件可确认为风险、标记误报或关闭归档，处置备注进入历史表和操作日志。
- 风险事件支持分页查询和多条件筛选，风险运营报表可按状态、场景统计风险分布和处置完成率。
- 未登录访问受保护接口返回 401，运营人员处理风险事件返回 403。
- 车次查询重复请求可命中缓存，锁票、支付、关闭和退票后按线路日期失效缓存。
- 16 个并发请求抢 1 张票时，只成功生成 1 个待支付订单，库存最终为 0。
- 使用同一 `requestId` 重复下单时返回原订单，库存只扣减一次。
- 集成测试覆盖车次查询、下单、支付流水、支付回调幂等、超时关闭、订单幂等、退票、订单分页筛选、看板增强指标、风控生成、风险状态流转、处置历史、权限保护、缓存失效和并发防超卖。

## 简历写法示例

铁路客运票务与风控运营管理系统  
基于 Spring Boot 开发铁路客运票务与风控运营管理系统，实现车次查询、待支付锁票、模拟支付、超时关闭、退票释放库存、异常订单识别、风险事件处置、风险运营报表、角色权限、运营数据看板和操作日志审计。项目使用 JPA 建模核心业务表，通过订单状态机和事务保证 `PENDING_PAYMENT`、`PAID`、`REFUNDED`、`CLOSED` 状态与库存一致；使用乐观锁和并发集成测试验证 16 个请求抢 1 张票时只生成 1 个待支付订单；下单接口支持 `requestId` 幂等提交，重复请求返回原订单且不重复扣库存；为车次余票查询设计本地 TTL 缓存，并在锁票、支付、关闭、退票事务提交后失效线路日期缓存；将风控逻辑抽象为 `RiskRule` 规则引擎，支付成功后触发短时间多次购票、高金额订单规则，退票后触发频繁退票规则；风险事件支持分页筛选、状态分布、场景分布和处置完成率统计；使用签名令牌和 `@RequiredRole` 注解保护风控处置与审计接口，并通过集成测试覆盖核心接口链路。

面试展开：订单创建后先进入待支付并锁定库存，支付成功后才进入有效交易和风控统计；待支付订单支持手动关闭和定时超时关闭，关闭后释放库存。重复 `requestId` 下单返回原订单，重复支付已支付订单不会重复触发风控。

运营管理补充：订单查询接口支持按用户、状态、订单号和创建日期组合筛选，并使用 Spring Data JPA `Specification` + `PageRequest` 返回分页结果；运营看板补充待支付、已关闭、退票率、风险率等指标，便于从后台管理视角分析订单状态分布和交易风险。

支付链路补充：新增支付流水表和模拟支付回调接口，支付流水使用 `PENDING`、`SUCCESS`、`FAILED` 状态独立记录支付侧处理结果；成功回调复用订单支付状态机，将订单从待支付确认到已支付，并通过 `callbackRequestId` 保证重复回调不会重复改订单、重复写成功业务日志或重复触发风控。

风控闭环补充：风险事件不再只用 `handled` 布尔值表示处理结果，而是升级为 `PENDING`、`CONFIRMED`、`FALSE_POSITIVE`、`CLOSED` 状态机；处置时保存备注、处理人和处理时间，并写入 `risk_event_handle_records` 历史表，便于展示风控审核、权限控制和审计追踪能力。

风险运营补充：风险事件查询接口使用 `Specification` + `PageRequest` 支持状态、场景、用户、订单号和创建日期组合筛选；风险运营报表统计总风险、待处理、确认风险、误报、关闭归档、处置完成率，以及按状态和场景聚合的风险分布，适合在面试中说明后台风控运营分析能力。

## 面试可讲点

- 交易状态机：为什么创建订单先锁票并进入待支付，而不是直接购票成功。
- 数据一致性：下单扣库存、关闭释放库存、退票释放库存如何放在事务边界内。
- 并发防超卖：JPA 乐观锁如何让 16 个请求抢 1 张票时只成功 1 单。
- 幂等设计：下单 `requestId`、支付流水 `requestId`、支付回调 `callbackRequestId` 分别解决什么问题。
- 支付建模：为什么订单状态和支付流水状态需要分开。
- 风控规则引擎：`RiskRule` 和 `RiskScene` 如何让规则扩展更清晰。
- 风险处置闭环：为什么不能只用 `handled` 布尔值，为什么需要状态、备注、处理人和历史表。
- 后台运营能力：订单分页、风险分页、看板指标和风险报表如何体现管理系统能力。
- 权限审计：轻量签名令牌、角色注解、操作日志和处置历史如何配合。

## 文档

- 项目最终总结：`docs/final-project-summary.md`
- 项目大纲：`docs/project-outline.md`
- API 设计：`docs/api-design.md`
- 订单状态机设计：`docs/order-state-design.md`
- 支付流水设计：`docs/payment-design.md`
- 风险处置闭环设计：`docs/risk-handling-design.md`
- 订单幂等设计：`docs/idempotency-design.md`
- 缓存设计：`docs/cache-design.md`
- 并发防超卖设计：`docs/concurrency-design.md`
- 数据库设计：`docs/database-design.md`
- ER 图：`docs/er-diagram.mmd`
- 简历与面试材料：`docs/resume-and-interview.md`
- 项目开发日志：`docs/project-development-log.md`
- GitHub 上传步骤：`docs/github-upload.md`

## 后续计划

- 将本地 TTL 缓存替换为 Redis，支持多实例共享缓存。
- 将演示版签名令牌升级为 Spring Security + JWT + BCrypt。
- 接入真实支付回调、延时队列关闭超时订单和接口限流。
- 使用 Vue3 重构前端管理台。
- 增加接口测试、异常场景测试和压力测试说明。

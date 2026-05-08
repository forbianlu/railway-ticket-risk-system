# Railway Ticket Risk System

铁路客运票务与风控运营管理系统，面向铁路局信息技术岗、银行科技岗等校招场景设计。项目模拟客运交易链路中的车次查询、余票扣减、购票退票、风险识别、风险处置、角色权限、日志审计和运营统计。

## 项目亮点

- 完整票务闭环：查询车次、创建订单、扣减库存、退票释放库存。
- 库存防超卖：座位库存使用 JPA 乐观锁版本号，冲突时返回明确提示。
- 风控规则引擎：将风险规则拆成独立 `RiskRule`，按 `RiskScene` 调度。
- 风险处置闭环：风险事件支持待处理、已处理状态流转，并写入审计日志。
- 角色权限控制：支持管理员、风控专员、运营人员演示账号，使用签名令牌和注解式角色校验保护敏感接口。
- 运营看板：统计总订单、有效订单、退票订单、未处理风险和热门车次。
- 工程化交付：提供接口集成测试、H2 演示库、MySQL profile、Docker Compose、GitHub Actions。

## 系统架构

![系统架构](docs/assets/system-architecture.svg)

## 业务流程

![票务交易流程](docs/assets/ticket-flow.svg)

## 权限流程

![登录鉴权与角色权限](docs/assets/auth-access-control.svg)

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

- `RapidPurchaseRiskRule`：同一用户 10 分钟内下单达到 3 次。
- `HighAmountRiskRule`：同一用户当日有效订单金额超过 1000 元。
- `FrequentRefundRiskRule`：同一用户 7 天内退票达到 3 次。

## 技术栈

- 后端：Java 8, Spring Boot 2.7, Spring Web, Spring Data JPA, Validation
- 权限：自定义签名令牌、HandlerInterceptor、`@RequiredRole` 注解式角色校验
- 数据库：H2 本地演示，MySQL 生产化配置
- 前端：HTML, CSS, JavaScript 管理台原型
- 工程化：Maven, Docker Compose, GitHub Actions

## 主要接口

```text
GET  /api/health
POST /api/auth/login
GET  /api/auth/me
GET  /api/stations
GET  /api/trains/search?from=BJP&to=SHH&date=2026-06-01
POST /api/orders
POST /api/orders/{id}/refund
GET  /api/orders
GET  /api/risks
POST /api/risks/{id}/handle
GET  /api/dashboard/summary
GET  /api/logs
```

## 目录结构

```text
railway-ticket-risk-system
├── backend              # Spring Boot 后端
├── frontend             # 管理台原型
├── docs                 # 项目文档、ER 图、架构图
├── docker-compose.yml   # MySQL + 后端编排
└── README.md
```

## 后端启动

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

## 前端启动

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

## 本地验证

```bash
cd backend
mvn test
```

已验证链路：

- 连续购票后余票扣减，并触发短时多次购票、高金额订单风险。
- 连续退票后库存释放，并触发频繁退票风险。
- 风险事件可标记已处理，处置动作进入操作日志。
- 未登录访问受保护接口返回 401，运营人员处理风险事件返回 403。
- 集成测试覆盖车次查询、下单、退票、风控生成、风险处置、权限保护和看板指标。

## 简历写法示例

铁路客运票务与风控运营管理系统  
基于 Spring Boot 开发铁路客运票务与风控运营管理系统，实现车次查询、余票扣减、购票退票、异常订单识别、风险事件处置、角色权限、运营数据看板和操作日志审计。项目使用 JPA 建模核心业务表，通过事务和乐观锁保证订单状态流转与库存一致性；将风控逻辑抽象为 `RiskRule` 规则引擎，支持短时间多次购票、高金额订单、频繁退票等规则扩展；使用签名令牌和 `@RequiredRole` 注解保护风控处置与审计接口，并通过集成测试覆盖核心接口链路。

## 文档

- 项目大纲：`docs/project-outline.md`
- API 设计：`docs/api-design.md`
- 数据库设计：`docs/database-design.md`
- ER 图：`docs/er-diagram.mmd`
- 简历与面试材料：`docs/resume-and-interview.md`
- GitHub 上传步骤：`docs/github-upload.md`

## 后续计划

- 引入 Redis 缓存热门车次余票。
- 将演示版签名令牌升级为 Spring Security + JWT + BCrypt。
- 增加并发扣库存压测和接口限流说明。
- 使用 Vue3 重构前端管理台。
- 增加接口测试、异常场景测试和压力测试说明。

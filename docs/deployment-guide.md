# 部署指南

## 本地 H2 启动

默认启动方式使用 H2 内存数据库、本地车次查询缓存和本地限流，不依赖 MySQL、Redis 或 Docker。

```bash
cd backend
mvn spring-boot:run
```

启动后访问：

- 健康检查：`http://localhost:8080/api/health`
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`
- H2 控制台：`http://localhost:8080/h2-console`

H2 JDBC URL：

```text
jdbc:h2:mem:railway
```

## Swagger UI 使用方式

1. 打开 `http://localhost:8080/swagger-ui/index.html`。
2. 调用 `POST /api/auth/login`，使用演示账号获取 JWT。
3. 点击 Swagger UI 右上角 Authorize。
4. 输入 `Bearer {token}`。
5. 调用需要权限的接口。

## Docker Compose 启动

项目提供 `Dockerfile` 和 `docker-compose.yml`，用于本地启动后端、MySQL 和 Redis。

```bash
docker compose up --build
```

服务端口：

| 服务 | 容器 | 端口 |
| --- | --- | --- |
| 后端 | `railway-backend` | `8080:8080` |
| MySQL | `railway-mysql` | `3307:3306`，宿主机端口为 `3307`，容器内部端口仍为 `3306` |
| Redis | `railway-redis` | `6379:6379` |

停止服务：

```bash
docker compose down
```

清理本地数据卷：

```bash
docker compose down -v
```

## Docker Profile

Docker Compose 使用：

```text
SPRING_PROFILES_ACTIVE=docker
```

`docker` profile 会切换为：

- MySQL 数据源。
- Redis 车次查询缓存。
- Redis 接口限流。
- H2 控制台关闭。

默认 Maven 测试仍使用 H2 和本地缓存/限流，不依赖 Docker、MySQL 或 Redis。

## MySQL 配置

`docker-compose.yml` 默认创建数据库：

```text
railway_ticket_risk
```

演示账号：

```text
username: railway
password: railway_demo
```

这些配置仅用于本地演示。其他环境应通过环境变量覆盖。

## Redis 配置

Docker Compose 中 Redis 服务名为 `redis`，后端通过以下环境变量连接：

```text
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
```

车次查询缓存和接口限流都会使用 Redis 模式，同时保留本地 fallback，避免 Redis 临时不可用时直接影响默认演示链路。

## 环境变量

常用环境变量：

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Spring profile | `default` |
| `SERVER_PORT` | 后端端口 | `8080` |
| `SPRING_DATASOURCE_URL` | 数据库连接 URL | H2 或 Docker MySQL URL |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 | `sa` / `railway` |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | 空 / `railway_demo` |
| `SPRING_REDIS_HOST` | Redis 主机 | `localhost` / `redis` |
| `SPRING_REDIS_PORT` | Redis 端口 | `6379` |
| `JWT_SECRET` | JWT 签名密钥 | 本地演示密钥 |
| `PAYMENT_CALLBACK_SECRET` | 支付回调签名密钥 | 本地演示密钥 |
| `REFUND_CALLBACK_SECRET` | 退款回调签名密钥 | 本地演示密钥 |

## 验证服务

启动完成后可以执行：

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/v3/api-docs
```

也可以打开前端静态管理台：

```bash
cd frontend
node static-server.js
```

访问 `http://127.0.0.1:5173`。

## 常见问题

### 后端启动时无法连接 MySQL

先确认 `mysql` 容器健康状态：

```bash
docker compose ps
```

如果 MySQL 首次初始化较慢，等待 healthcheck 通过后后端会继续启动。

### 端口被占用

修改 `docker-compose.yml` 中左侧宿主机端口，例如将 `8080:8080` 改为 `18080:8080`。

### Swagger UI 能打开但业务接口返回 401

说明接口需要认证。先调用登录接口获取 JWT，并在 Swagger UI 的 Authorize 中填写 `Bearer {token}`。

### 默认测试是否需要 Docker

不需要。默认 `mvn test` 使用 H2、本地缓存和本地限流，CI 也按该模式执行。

# 安全认证设计

## 权限体系目标

系统使用 Spring Security、JWT 和 BCrypt 统一处理登录认证、令牌校验和角色授权。认证链路保持无状态，适合前后端分离的管理台调用方式。

## Spring Security 接入方式

后端通过 `SecurityFilterChain` 组件配置安全链路：

- 关闭 CSRF、表单登录、HTTP Basic 和服务端 Session。
- 使用 `SessionCreationPolicy.STATELESS` 保持无状态认证。
- 放行 `/api/auth/login`、`/api/health` 和 H2 控制台。
- 通过自定义 JWT 过滤器解析 Bearer Token。
- 保留 `@RequiredRole` 注解作为接口级角色校验入口。

## JWT 认证流程

1. 客户端调用 `POST /api/auth/login` 提交用户名和密码。
2. 服务端使用 BCrypt 校验密码。
3. 校验通过后签发 HMAC-SHA256 JWT。
4. JWT 载荷包含 `userId`、`username`、`displayName`、`role`、`iat` 和 `exp`。
5. 客户端后续请求携带 `Authorization: Bearer {token}`。
6. `JwtAuthenticationFilter` 校验签名和过期时间，成功后写入 Spring SecurityContext。

## BCrypt 密码存储

演示用户初始化时不再保存明文或固定摘要密码，而是通过 `BCryptPasswordEncoder` 生成带盐哈希。登录时使用 `PasswordEncoder.matches` 校验原始密码和数据库中的哈希值。

## 角色权限模型

系统保留三类角色：

- `ADMIN`：可查看日志、处理风险事件、查看和清空缓存。
- `RISK_OFFICER`：可查看日志、处理风险事件、查看缓存统计。
- `OPERATOR`：可查看运营数据，不能处理风险事件或执行敏感管理动作。

敏感接口通过 `@RequiredRole` 标注允许角色。JWT 过滤器只负责认证，接口注解负责授权。

## 401 和 403

- 401：未携带 Token、Token 格式错误、签名无效或已过期。
- 403：Token 有效，但当前角色不在接口允许角色范围内。

## 前端调用方式

前端登录成功后保存登录响应中的 `token`，后续请求统一设置：

```http
Authorization: Bearer {token}
```

角色信息仍由登录响应返回，前端可据此控制风险处置按钮和审计日志展示。

## 与审计链路的关系

登录成功会写入操作日志。风险处置、缓存清理等敏感动作通过当前认证用户确定操作人，业务日志和风险处置历史共同构成审计记录。

## 后续可扩展方向

- 增加 refresh token。
- 增加 token blacklist 或登出失效机制。
- 增加登录失败次数限制。
- 增加登录审计维度，例如客户端 IP 和 User-Agent。

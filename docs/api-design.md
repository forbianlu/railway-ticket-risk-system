# API 设计

## 健康检查

```http
GET /api/health
```

响应：

```json
{
  "status": "UP",
  "service": "railway-ticket-risk-system"
}
```

## 查询车站

```http
GET /api/stations
```

## 登录

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "risk",
  "password": "risk123"
}
```

响应：

```json
{
  "token": "base64Payload.signature",
  "username": "risk",
  "displayName": "风控专员",
  "role": "RISK_OFFICER",
  "expiresAt": 1770000000
}
```

## 当前登录用户

```http
GET /api/auth/me
Authorization: Bearer {token}
```

受保护接口统一使用 `Authorization: Bearer {token}` 传递登录令牌。缺少令牌返回 401，角色不足返回 403。

## 查询车次

```http
GET /api/trains/search?from=BJP&to=SHH&date=2026-06-01
```

查询结果会按 `from + to + date` 写入本地 TTL 缓存，下单或退票成功后失效对应线路日期缓存。

## 创建订单

```http
POST /api/orders
Content-Type: application/json

{
  "userId": 1001,
  "requestId": "8f7f5c41-b6fd-48ab-8ec5-96b08d3c26d1",
  "trainId": 1,
  "inventoryId": 1,
  "passengerName": "张三",
  "passengerIdCard": "110101200001010011"
}
```

`requestId` 为可选幂等号。客户端重复提交相同 `userId + requestId` 时，系统返回第一次创建的订单，不重复扣减库存。

## 退票

```http
POST /api/orders/1/refund
```

## 查询订单

```http
GET /api/orders?userId=1001
```

## 查询风险事件

```http
GET /api/risks
```

## 处理风险事件

```http
POST /api/risks/1/handle
Authorization: Bearer {token}
```

该接口仅允许 `RISK_OFFICER` 和 `ADMIN` 角色访问，操作人默认取当前登录用户。

## 运营看板

```http
GET /api/dashboard/summary
```

## 审计日志

```http
GET /api/logs
Authorization: Bearer {token}
```

该接口仅允许 `RISK_OFFICER` 和 `ADMIN` 角色访问。

## 查询车次缓存统计

```http
GET /api/cache/train-search
Authorization: Bearer {token}
```

该接口仅允许 `RISK_OFFICER` 和 `ADMIN` 角色访问。

响应：

```json
{
  "enabled": true,
  "ttlSeconds": 30,
  "maxEntries": 256,
  "entryCount": 1,
  "hitCount": 3,
  "missCount": 2,
  "evictCount": 1
}
```

## 清空车次缓存

```http
DELETE /api/cache/train-search
Authorization: Bearer {token}
```

该接口仅允许 `ADMIN` 角色访问。

## 演示账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin123` | `ADMIN` |
| `risk` | `risk123` | `RISK_OFFICER` |
| `ops` | `ops123` | `OPERATOR` |

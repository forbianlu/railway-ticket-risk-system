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

查询结果会按 `from + to + date` 写入本地 TTL 缓存，锁票、支付、关闭待支付订单或退票成功后失效对应线路日期缓存。

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

创建订单会扣减余票并生成 `PENDING_PAYMENT` 待支付订单，默认支付截止时间为创建后 15 分钟。此时库存被锁定，但不会触发支付后风控规则。

响应中的主要订单字段：

```json
{
  "id": 1,
  "orderNo": "RT202605091230001234",
  "requestId": "8f7f5c41-b6fd-48ab-8ec5-96b08d3c26d1",
  "status": "PENDING_PAYMENT",
  "paymentDeadlineAt": "2026-05-09T12:45:00",
  "paidAt": null,
  "closedAt": null
}
```

## 支付订单

```http
POST /api/orders/1/pay
```

待支付订单支付成功后状态变为 `PAID`，写入支付时间，失效对应车次查询缓存，并触发下单后风控规则。已支付订单重复支付会直接返回原订单，不重复触发风控。

如果支付时订单已超过 `paymentDeadlineAt`，系统会关闭订单、释放库存，并返回 `CLOSED` 状态。

## 关闭待支付订单

```http
POST /api/orders/1/close
```

该接口用于主动关闭 `PENDING_PAYMENT` 订单。关闭后订单状态变为 `CLOSED`，库存释放，车次查询缓存失效。`PAID`、`REFUNDED`、`CLOSED` 订单不能关闭。

## 批量关闭超时订单

```http
POST /api/orders/close-expired
```

该接口会扫描超过支付截止时间的待支付订单并关闭。系统也内置定时任务，默认每 60 秒自动执行一次。

## 退票

```http
POST /api/orders/1/refund
```

仅 `PAID` 订单允许退票。退票成功后状态变为 `REFUNDED`，库存释放，并触发退票后风控规则。

## 查询订单

```http
GET /api/orders?userId=1001&status=PAID&fromDate=2026-05-01&toDate=2026-05-31&orderNo=RT2026&page=0&size=10
```

查询参数：

| 参数 | 是否必填 | 说明 |
| --- | --- | --- |
| userId | 否 | 按用户 ID 筛选 |
| status | 否 | 订单状态：`PENDING_PAYMENT`、`PAID`、`CLOSED`、`REFUNDED`、`CANCELLED` |
| fromDate | 否 | 创建日期起始，格式 `yyyy-MM-dd`，包含当天 |
| toDate | 否 | 创建日期结束，格式 `yyyy-MM-dd`，包含当天 |
| orderNo | 否 | 按订单号模糊查询 |
| page | 否 | 页码，从 0 开始，默认 0 |
| size | 否 | 每页大小，默认 10，最大 100 |

非法 `status`、负数页码或非正数页大小会返回 400。

分页响应：

```json
{
  "content": [
    {
      "id": 1,
      "orderNo": "RT202605150001234",
      "userId": 1001,
      "trainNo": "G101",
      "status": "PAID",
      "createdAt": "2026-05-15T09:30:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
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

新增指标字段：

```json
{
  "totalOrderCount": 20,
  "pendingPaymentOrderCount": 3,
  "paidOrderCount": 10,
  "closedOrderCount": 4,
  "refundedOrderCount": 3,
  "unhandledRiskCount": 2,
  "refundRate": 0.23,
  "riskRate": 0.15
}
```

其中 `refundRate = refundedOrderCount / max(1, paidOrderCount + refundedOrderCount)`，`riskRate = totalRiskEvents / max(1, paidOrderCount + refundedOrderCount)`。接口继续保留 `totalOrders`、`paidOrders`、`refundedOrders`、`openRiskEvents` 等旧字段，方便前端兼容。

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

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

## 查询车次

```http
GET /api/trains/search?from=BJP&to=SHH&date=2026-06-01
```

## 创建订单

```http
POST /api/orders
Content-Type: application/json

{
  "userId": 1001,
  "trainId": 1,
  "inventoryId": 1,
  "passengerName": "张三",
  "passengerIdCard": "110101200001010011"
}
```

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
POST /api/risks/1/handle?operator=risk-admin
```

## 运营看板

```http
GET /api/dashboard/summary
```

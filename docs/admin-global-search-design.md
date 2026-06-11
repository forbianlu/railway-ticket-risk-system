# 管理端综合查询设计

## 设计目标

管理端综合查询用于在一个入口中检索订单、电子票、支付流水、退款流水、常用乘车人、风险事件、Outbox 事件、站内通知和操作日志。它面向运营、风控和管理员使用，帮助快速定位交易链路中的关键对象，并跳转到已有订单详情视图查看完整上下文。

## 接口

```http
GET /api/search?keyword=RT202606120001&types=ORDER,TICKET&limitPerType=5&includeTrace=true
Authorization: Bearer {ADMIN/RISK_OFFICER/OPERATOR token}
```

参数说明：

- `keyword`：必填，至少 2 个字符。
- `types`：可选，逗号分隔，支持 `ORDER`、`TICKET`、`PAYMENT`、`REFUND`、`TRAVELER`、`RISK`、`OUTBOX`、`NOTIFICATION`、`OPERATION_LOG`。
- `limitPerType`：可选，默认 5，最大 20。
- `includeTrace`：可选，返回轻量链路提示。

## 权限边界

`/api/search` 仅允许 `ADMIN`、`RISK_OFFICER`、`OPERATOR` 访问。普通乘客 `USER` 不能访问综合查询，也不能通过该接口查看其他乘客或管理端数据。

## 搜索类型

| 类型 | 检索字段示例 |
| --- | --- |
| ORDER | 订单号、乘客姓名、车次、线路、用户 ID |
| TICKET | 电子票号、订单号、乘客姓名、车次 |
| PAYMENT | 支付流水号、渠道支付流水号、订单号 |
| REFUND | 退款流水号、渠道退款流水号、支付流水号、订单号 |
| TRAVELER | 乘车人姓名、脱敏证件号、脱敏手机号、用户 ID |
| RISK | 风险原因、订单号、用户 ID |
| OUTBOX | 事件 ID、事件类型、聚合类型、聚合 ID、payload |
| NOTIFICATION | 通知号、标题、订单号、票号、支付号、退款号 |
| OPERATION_LOG | 操作人、动作、目标类型、目标 ID、详情 |

## 脱敏策略

综合查询结果不返回完整证件号、手机号、密码、JWT、签名密钥或回调签名。常用乘车人结果只展示脱敏后的证件号和手机号。

## 订单详情复用

搜索结果中与订单关联的记录会返回 `orderId` 和 `detailAction=ORDER_DETAIL`。前端点击“查看订单详情”时复用管理端已有订单详情弹窗，不新增重复的详情页或重复弹窗。

## 当前阶段

当前实现使用数据库查询和分页限制，不引入 Elasticsearch、外部搜索服务或消息队列。后续如数据量增长，可在保持权限和脱敏规则不变的前提下扩展索引服务。

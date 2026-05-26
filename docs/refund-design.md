# 退款流水设计

## 设计目标

退款流水用于补齐退票后的资金处理过程。订单状态负责描述票务业务是否已经退票，退款流水负责描述资金退款是否已经被渠道受理并确认成功。

## 订单状态与退款状态分离

当前系统保持订单状态机不增加 `REFUNDING`。用户退票成功后，订单立即从 `PAID` 变为 `REFUNDED`，库存释放并触发退票后风控；同时系统创建一条 `PENDING` 退款流水，等待模拟退款回调。

这样可以避免大规模改动订单状态机，同时清晰区分：

- 票务状态：是否允许乘客继续持票出行。
- 资金状态：退款渠道是否完成资金退回。

## refund_records 表

`refund_records` 保存系统内部退款流水号、关联订单、关联支付流水、退款金额、退款状态、渠道退款号、创建幂等号和回调幂等号。

核心字段：

- `refundNo`：系统内部退款流水号。
- `paymentNo`：关联支付流水号。
- `orderId` / `orderNo`：关联票务订单。
- `amount`：退款金额。
- `status`：`PENDING`、`SUCCESS`、`FAILED`。
- `channelRefundNo`：外部退款渠道流水号。
- `requestId`：创建退款流水幂等号。
- `callbackRequestId`：退款回调幂等号。

## 退款流水创建时机

调用 `POST /api/orders/{id}/refund` 成功后，系统自动创建退款流水。默认创建幂等号为 `refund-order-{orderId}`。

如果同一订单已经存在 `PENDING` 或 `SUCCESS` 退款流水，系统不会重复创建。重复退票请求也不会产生多条退款流水。

## 退款回调流程

```text
退票成功 -> 创建 PENDING 退款流水 -> 退款回调 -> SUCCESS / FAILED
```

成功回调：

- 校验签名。
- 校验金额一致。
- 保存必填的 `channelRefundNo`。
- 退款流水变为 `SUCCESS`。
- 写入 `refundedAt`。

失败回调：

- 校验签名。
- 校验金额一致。
- 退款流水变为 `FAILED`。
- 订单保持 `REFUNDED`。

## 退款回调幂等

退款回调使用 `callbackRequestId` 保证幂等：

- 相同 `callbackRequestId` 重复回调直接返回首次处理结果。
- 已经 `SUCCESS` 的退款流水重复成功回调直接返回原结果。
- 已经 `FAILED` 的退款流水不允许再改为 `SUCCESS`，后续可通过人工处理或重新发起退款扩展处理。

退款回调不会重复释放库存，也不会重复触发退票风控。库存释放和风控触发发生在订单退票成功时。

## 退款金额一致性校验

退款回调必须携带 `amount`。系统使用 `BigDecimal.compareTo` 比较回调金额与退款流水金额，金额不一致时拒绝回调，退款流水状态不变。

## 退款签名校验

退款回调使用 HMAC-SHA256 签名，签名原文固定为：

```text
refundNo={refundNo}&callbackRequestId={callbackRequestId}&amount={amount}&success={success}&timestamp={timestamp}
```

后端使用 `railway.refund.callback-secret` 计算签名，并校验签名和时间戳容忍窗口。

## 支付流水和退款流水关系

支付流水记录资金进入系统的过程，退款流水记录资金退回渠道的过程。二者通过 `paymentNo` 关联，订单通过 `orderId` 同时关联票务状态和资金状态。

## 后续扩展方向

- 增加重新发起退款接口。
- 增加退款人工补偿状态。
- 增加退款对账报表。
- 增加退款渠道错误码和错误原因分类。

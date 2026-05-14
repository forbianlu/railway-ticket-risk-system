# Codex 反馈：订单支付状态机与超时关闭收尾

## 1. 本轮任务目标

收尾“订单支付状态机与超时关闭功能”，使订单流程从创建后直接成功升级为：

```text
创建订单 -> PENDING_PAYMENT -> PAID / CLOSED -> REFUNDED
```

同时保证订单幂等、库存一致性、风控触发、查询缓存、前端管理台、文档和集成测试都保持一致。

## 2. 实际修改了哪些文件

本轮涉及的核心文件：

- `backend/src/main/java/com/example/railway/service/OrderService.java`
- `backend/src/test/java/com/example/railway/RailwayApiIntegrationTests.java`
- `docs/order-state-design.md`
- `docs/assets/order-state-flow.svg`
- `docs/assets/ticket-flow.svg`
- `README.md`
- `docs/api-design.md`
- `docs/idempotency-design.md`
- `docs/project-outline.md`
- `docs/resume-and-interview.md`
- `docs/codex-feedback-20260513-order-state-machine.md`

当前工作区中还存在状态机开发前已改动或已新增的文件，包括：

- `backend/src/main/java/com/example/railway/RailwayTicketRiskApplication.java`
- `backend/src/main/java/com/example/railway/controller/OrderController.java`
- `backend/src/main/java/com/example/railway/domain/OrderStatus.java`
- `backend/src/main/java/com/example/railway/domain/TicketOrder.java`
- `backend/src/main/java/com/example/railway/dto/OrderResponse.java`
- `backend/src/main/java/com/example/railway/repository/TicketOrderRepository.java`
- `backend/src/main/java/com/example/railway/service/OrderTimeoutScheduler.java`
- `backend/src/main/java/com/example/railway/service/risk/RapidPurchaseRiskRule.java`
- `backend/src/main/resources/application.yml`
- `docs/database-design.md`
- `docs/er-diagram.mmd`
- `frontend/app.js`
- `frontend/styles.css`

另外，`docs/project-context-for-gpt.md` 是上一轮为规划用途生成的上下文文档，当前仍处于未跟踪状态。

## 3. 每个文件主要改了什么

- `OrderService.java`
  - 支付成功后增加车次查询缓存失效。
  - 手动关闭已关闭订单改为返回业务错误，避免 `CLOSED` 被重复关闭。

- `RailwayApiIntegrationTests.java`
  - 补充订单状态机核心断言。
  - 增加非法状态流转测试。
  - 增加重复支付不重复触发风控测试。
  - 增加重复退票测试。
  - 增强超时关闭测试，验证只关闭超时的待支付订单。
  - 增强查询缓存测试，覆盖创建、支付、退票、关闭后的缓存失效。

- `docs/order-state-design.md`
  - 新增订单状态机设计说明，解释状态含义、流转规则、库存一致性、幂等、风控和缓存关系。

- `docs/assets/order-state-flow.svg`
  - 新增干净的 SVG 状态流转图，展示 `PENDING_PAYMENT -> PAID`、`PENDING_PAYMENT -> CLOSED`、`PAID -> REFUNDED`。

- `docs/assets/ticket-flow.svg`
  - 更新业务流程图文案，从“创建订单直接风控”调整为“创建待支付、锁定余票、支付风控”。

- `README.md`
  - 同步状态机、支付缓存失效、简历表述和文档入口。

- `docs/api-design.md`
  - 补充支付、关闭、批量关闭超时订单接口语义。
  - 明确支付成功后触发风控并失效缓存，关闭只允许待支付订单。

- `docs/idempotency-design.md`
  - 补充重复请求不会触发额外风控。

- `docs/project-outline.md`
  - 同步缓存失效点为锁票、支付、关闭、退票。

- `docs/resume-and-interview.md`
  - 同步简历表述、面试讲法和缓存设计说明。

## 4. 新增或修复了哪些接口

已确认并稳定以下接口：

```text
POST /api/orders
POST /api/orders/{id}/pay
POST /api/orders/{id}/close
POST /api/orders/close-expired
POST /api/orders/{id}/refund
GET  /api/orders
```

核心语义：

- `POST /api/orders` 创建 `PENDING_PAYMENT` 订单并锁定库存。
- `POST /api/orders/{id}/pay` 支付待支付订单，成功后变为 `PAID`。
- `POST /api/orders/{id}/close` 手动关闭待支付订单，成功后变为 `CLOSED`。
- `POST /api/orders/close-expired` 批量关闭超时待支付订单。
- `POST /api/orders/{id}/refund` 只允许已支付订单退票，成功后变为 `REFUNDED`。

## 5. 订单状态机最终规则

允许流转：

```text
创建订单 -> PENDING_PAYMENT
PENDING_PAYMENT -> PAID
PENDING_PAYMENT -> CLOSED
PAID -> REFUNDED
```

禁止流转：

- `PENDING_PAYMENT` 不能退票。
- `PAID` 不能关闭。
- `CLOSED` 不能支付、不能重复关闭、不能退票。
- `REFUNDED` 不能重复退票。

重复支付已支付订单会直接返回原订单，不重复触发风控。

## 6. 库存扣减和释放规则

- 创建待支付订单：扣减 1 张余票，表示锁票。
- 支付订单：不改变库存，只确认交易生效。
- 手动关闭待支付订单：释放 1 张余票。
- 超时关闭待支付订单：释放 1 张余票。
- 已支付订单退票：释放 1 张余票。

库存扣减和释放仍在事务中执行，并继续依赖 `SeatInventory` 的 JPA 乐观锁防止并发超卖。

## 7. 风控触发点

- 支付成功后触发下单后风控规则。
- 退票成功后触发退票后风控规则。
- 创建待支付订单不触发风控。
- 关闭待支付订单不触发退票风控。
- 重复支付已支付订单不重复触发风控。

## 8. 缓存失效点

车次查询缓存会在事务提交后按线路日期失效：

- 创建待支付订单后失效缓存。
- 支付成功后失效缓存。
- 手动关闭待支付订单后失效缓存。
- 超时关闭待支付订单后失效缓存。
- 退票成功后失效缓存。

## 9. 新增或修改了哪些测试

集成测试新增或增强了：

- 创建订单后状态为 `PENDING_PAYMENT`。
- 创建订单后库存减少。
- 支付待支付订单后状态变为 `PAID`。
- 支付成功后可触发风控规则。
- 重复支付同一订单不重复触发风控。
- 关闭待支付订单后状态变为 `CLOSED`。
- 关闭订单后库存恢复。
- `PAID` 订单不能关闭。
- `CLOSED` 订单不能支付。
- `PENDING_PAYMENT` 订单不能退票。
- `PAID` 订单可以退票并释放库存。
- `REFUNDED` 订单不能重复退票。
- 超时关闭只处理超时的 `PENDING_PAYMENT` 订单。
- `requestId` 幂等逻辑仍然有效。
- 并发防超卖测试仍然通过。
- 查询缓存测试覆盖创建、支付、退票、关闭后的缓存失效。

## 10. Maven 测试结果

第一次在仓库根目录执行 Maven 时失败，原因是根目录没有 `pom.xml`，实际 Maven 项目位于 `backend` 目录。

随后在 `backend` 目录执行：

```powershell
& 'D:\简历系统\tools\apache-maven-3.9.15\bin\mvn.cmd' test
```

结果：

```text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 11. 前端 JS 检查结果

执行命令：

```powershell
node --check frontend\app.js
```

结果：通过，无语法错误。

## 12. 仍未完成的问题

- 本轮没有自动提交 Git commit。
- 本轮没有 push 到 GitHub，符合“不自动 push”的要求。
- 当前本地存在已提交但未推送的 `add order idempotency`，以及本轮状态机相关未提交改动。
- `docs/project-context-for-gpt.md` 是上一轮生成的规划上下文文档，是否纳入提交可由后续决定。

## 13. 后续建议

建议下一步先提交当前状态机改动，然后再进入新功能：

1. 提交订单状态机和文档。
2. 网络稳定后推送到 GitHub。
3. 下一阶段优先做订单列表分页、状态筛选、运营看板指标增强。
4. 再后续可考虑支付流水表、支付回调幂等、风控处置备注和处置历史。

## 14. 建议提交信息

```text
add order payment state machine
```

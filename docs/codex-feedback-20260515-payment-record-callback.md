# Codex 反馈：支付流水表与支付回调幂等

## 1. 本轮任务目标

本轮目标是在既有订单状态机、订单分页筛选和运营看板能力之上，新增模拟支付流水表和支付回调幂等能力，使交易链路从“直接支付订单”扩展为：

创建订单 -> 待支付锁票 -> 创建支付流水 -> 模拟第三方支付回调 -> 支付成功后订单变为已支付 -> 支付流水变为成功 -> 触发风控和审计日志。

同时要求重复回调不重复修改订单、不重复触发风控、不重复释放或扣减库存。

## 2. 开始前 git status 和 git log 检查结果

开始前执行了 `git status --short` 和 `git log --oneline -10`。

检查结果显示：

- 上一轮“订单分页筛选与运营看板指标增强”相关代码和文档仍处于未提交状态。
- `docs/project-context-for-gpt.md` 为未跟踪规划上下文文件，未被 README 或正式 docs 引用，因此没有纳入功能提交。
- 当时最新提交为 `7403c58 add order payment state machine`。

## 3. 是否已经提交上一轮订单筛选和看板改动

是。已先运行 Maven 测试确认上一轮功能通过，然后在本地创建保护提交。

## 4. 上一轮保护提交信息和 commit hash

- commit hash：`caf77b0`
- commit message：`add order filtering and dashboard metrics`

本轮没有 push 到 GitHub。

## 5. 实际修改了哪些文件

新增文件：

- `backend/src/main/java/com/example/railway/domain/PaymentRecord.java`
- `backend/src/main/java/com/example/railway/domain/PaymentStatus.java`
- `backend/src/main/java/com/example/railway/repository/PaymentRecordRepository.java`
- `backend/src/main/java/com/example/railway/dto/CreatePaymentRequest.java`
- `backend/src/main/java/com/example/railway/dto/PaymentCallbackRequest.java`
- `backend/src/main/java/com/example/railway/dto/PaymentResponse.java`
- `backend/src/main/java/com/example/railway/dto/PaymentPageResponse.java`
- `backend/src/main/java/com/example/railway/service/PaymentService.java`
- `backend/src/main/java/com/example/railway/controller/PaymentController.java`
- `docs/payment-design.md`
- `docs/codex-feedback-20260515-payment-record-callback.md`

修改文件：

- `README.md`
- `backend/src/test/java/com/example/railway/RailwayApiIntegrationTests.java`
- `docs/api-design.md`
- `docs/database-design.md`
- `docs/er-diagram.mmd`
- `docs/project-outline.md`
- `docs/resume-and-interview.md`
- `frontend/index.html`
- `frontend/app.js`
- `frontend/styles.css`

## 6. 每个文件主要改了什么

- `PaymentRecord.java`：新增支付流水实体，记录支付号、订单号、用户、金额、支付状态、渠道、创建幂等号、回调幂等号、回调消息和支付时间。
- `PaymentStatus.java`：新增 `PENDING`、`SUCCESS`、`FAILED` 三种支付状态。
- `PaymentRecordRepository.java`：新增支付流水查询仓储，支持支付号、订单 ID、创建幂等号、回调幂等号查询，并支持 `Specification` 分页筛选。
- `CreatePaymentRequest.java`：新增创建支付流水请求 DTO。
- `PaymentCallbackRequest.java`：新增支付回调请求 DTO。
- `PaymentResponse.java`：新增支付流水响应 DTO。
- `PaymentPageResponse.java`：新增支付流水分页响应 DTO。
- `PaymentService.java`：实现创建支付流水、生成支付号、支付成功/失败回调、回调幂等、支付流水分页筛选。
- `PaymentController.java`：新增支付流水 REST 接口。
- `RailwayApiIntegrationTests.java`：新增支付流水创建、成功回调幂等、失败回调、非法状态创建、支付流水分页筛选等集成测试。
- `frontend/index.html`：新增“支付流水”管理区域。
- `frontend/app.js`：新增创建支付流水、模拟成功/失败回调、支付流水查询和分页交互。
- `frontend/styles.css`：新增支付流水筛选区样式和移动端适配。
- `README.md`：补充支付流水、模拟支付回调、回调幂等和测试说明。
- `docs/api-design.md`：补充 `POST /api/payments`、`POST /api/payments/callback`、`GET /api/payments`。
- `docs/database-design.md`：补充 `payment_records` 表结构和支付状态说明。
- `docs/er-diagram.mmd`：补充 `PAYMENT_RECORDS` 实体以及订单到支付流水的一对多关系。
- `docs/project-outline.md`：补充支付流水和支付回调幂等在项目能力中的位置。
- `docs/resume-and-interview.md`：补充简历表述和面试讲法，说明支付流水、支付状态和回调幂等。
- `docs/payment-design.md`：新增支付设计文档。

## 7. 支付流水表最终设计

表名：`payment_records`

核心字段：

- `id`：主键。
- `payment_no`：支付流水号，唯一。
- `order_id`：关联订单 ID。
- `order_no`：订单号，便于展示和对账。
- `user_id`：用户 ID。
- `amount`：支付金额。
- `status`：支付状态。
- `channel`：支付渠道，本项目使用 `MOCK`。
- `request_id`：创建支付流水的幂等号。
- `callback_request_id`：支付回调幂等号。
- `callback_message`：回调消息。
- `paid_at`：支付成功时间。
- `created_at`：创建时间。
- `updated_at`：更新时间。

## 8. 支付状态设计

- `PENDING`：支付流水已创建，等待第三方支付结果。
- `SUCCESS`：支付成功，订单应处于 `PAID`。
- `FAILED`：支付失败，订单仍保持 `PENDING_PAYMENT`。

支付状态描述的是一次支付请求的处理结果；订单状态描述的是票务订单业务阶段，两者分开建模，便于对账、审计和面试讲解真实交易链路。

## 9. 新增接口设计

### POST /api/payments

创建支付流水。

请求字段：

- `orderId`：订单 ID。
- `requestId`：创建支付流水幂等号，可选。

规则：

- 只能为 `PENDING_PAYMENT` 订单创建支付流水。
- 同一订单已有 `PENDING` 支付流水时直接返回已有流水。
- 相同 `requestId` 重复请求直接返回原支付流水。
- `CLOSED`、`REFUNDED`、`PAID` 订单不能创建新的待支付流水。

### POST /api/payments/callback

处理模拟第三方支付回调。

请求字段：

- `paymentNo`：支付流水号。
- `callbackRequestId`：回调请求号，用于幂等。
- `success`：是否支付成功。
- `message`：回调消息。

### GET /api/payments

分页查询支付流水。

支持参数：

- `orderId`
- `status`
- `paymentNo`
- `page`
- `size`

返回 `PaymentPageResponse`，包含 `content`、`page`、`size`、`totalElements`、`totalPages`、`first`、`last`。

## 10. 支付流水和订单状态机的关系

订单创建后进入 `PENDING_PAYMENT` 并扣减库存，表示已锁票。支付流水在此基础上记录一次支付尝试：

- 支付流水 `PENDING`：订单仍是 `PENDING_PAYMENT`。
- 支付流水 `SUCCESS`：调用既有 `OrderService.pay`，订单从 `PENDING_PAYMENT` 变为 `PAID`。
- 支付流水 `FAILED`：订单仍是 `PENDING_PAYMENT`，库存不释放。

本轮保留了既有 `POST /api/orders/{id}/pay` 快捷模拟支付接口，用于兼容原有前端和测试；新增 `/api/payments` 和 `/api/payments/callback` 用于展示更真实的支付流水和回调幂等链路。

## 11. 支付成功回调处理流程

1. 根据 `callbackRequestId` 判断是否为重复回调。
2. 根据 `paymentNo` 查询支付流水。
3. 如果流水已是 `SUCCESS`，直接返回已有结果。
4. 如果流水是 `PENDING`，更新为 `SUCCESS`，记录回调请求号、回调消息和支付成功时间。
5. 调用 `OrderService.pay(orderId)` 完成订单支付状态流转。
6. 复用既有订单支付逻辑触发风控、写订单支付日志、失效车次查询缓存。
7. 记录支付流水成功操作日志。

## 12. 支付失败回调处理流程

1. 根据 `callbackRequestId` 判断是否为重复回调。
2. 根据 `paymentNo` 查询支付流水。
3. 如果流水是 `PENDING`，更新为 `FAILED`，记录回调请求号和失败消息。
4. 订单继续保持 `PENDING_PAYMENT`。
5. 不释放库存。
6. 不触发支付成功风控。
7. 记录支付失败操作日志。

## 13. 回调幂等规则

- 相同 `callbackRequestId` 重复请求时，直接返回第一次处理后的支付流水。
- 支付流水已是 `SUCCESS` 后，再收到成功回调直接返回已有成功结果。
- 支付流水已是 `FAILED` 后，不允许同一支付流水再从失败改为成功，需要重新创建支付流水。
- 回调幂等只保证支付回调不会重复处理，不改变订单下单 `requestId` 幂等规则。

## 14. 重复回调如何避免重复风控

支付成功风控触发仍集中在既有 `OrderService.pay` 中。`PaymentService` 在处理回调时：

- 重复 `callbackRequestId` 会在进入支付逻辑前直接返回。
- 已经 `SUCCESS` 的支付流水不会再次调用 `OrderService.pay`。
- `OrderService.pay` 本身也保留 `PAID` 订单重复支付不重复触发风控的保护。

因此重复成功回调不会重复生成风险事件。

## 15. 前端管理台新增了哪些支付流水交互

新增“支付流水”区域：

- 输入订单 ID 创建支付流水。
- 展示支付流水号、订单号、用户、金额、支付状态、渠道、创建时间和支付时间。
- 支持按支付状态和支付流水号筛选。
- 支持支付流水分页查询。
- 支持对 `PENDING` 支付流水模拟成功回调和失败回调。
- 在订单列表的待支付订单操作区增加创建支付流水按钮。

## 16. 新增或修改了哪些测试

在 `RailwayApiIntegrationTests` 中新增或增强：

- 为 `PENDING_PAYMENT` 订单创建支付流水成功。
- 支付流水初始状态为 `PENDING`。
- 相同 `requestId` 创建支付流水返回同一流水。
- 成功回调后支付流水变为 `SUCCESS`。
- 成功回调后订单变为 `PAID`。
- 成功回调触发风控。
- 相同 `callbackRequestId` 重复回调不重复触发风控。
- 支付失败回调后支付流水变为 `FAILED`。
- 支付失败回调不触发风控。
- 支付失败回调后订单仍保持 `PENDING_PAYMENT`。
- `FAILED` 支付流水不能再改为成功。
- `CLOSED` 订单不能创建支付流水。
- `REFUNDED` 订单不能创建支付流水。
- 支付流水分页查询正常。
- 支付流水按 `status` 筛选正常。
- 支付流水按 `paymentNo` 筛选正常。
- 非法支付状态参数返回 400。

原有订单状态机、订单幂等、分页筛选、看板指标、并发防超卖、缓存和权限相关测试继续保留。

## 17. Maven 测试结果

执行命令：

```powershell
& 'D:\简历系统\tools\apache-maven-3.9.15\bin\mvn.cmd' test
```

执行目录：`backend`

结果：

- `Tests run: 18`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`
- `BUILD SUCCESS`

## 18. 前端 JS 检查结果

执行命令：

```powershell
node --check frontend\app.js
```

结果：通过，无语法错误。

## 19. 仍未完成的问题

- 本轮没有接入真实第三方支付 SDK，这是按项目约束保留为模拟支付。
- 当前支付失败后的策略是同一 `paymentNo` 不能从 `FAILED` 改为 `SUCCESS`，需要重新创建支付流水；该规则已写入文档。
- 支付回调没有做签名验签，后续如果要更贴近生产支付系统，可增加回调签名校验、回调来源校验和支付金额一致性校验。
- `docs/project-context-for-gpt.md` 仍为未跟踪规划上下文文件，未纳入本轮核心功能。

## 20. 后续建议

- 增加支付回调签名校验和金额校验，进一步贴近银行交易系统。
- 增加支付流水对账状态，例如 `RECONCILED` 或单独对账表。
- 增加退款流水，使退票和支付流水形成完整资金链路。
- 增加运营看板支付转化率、支付失败率、待支付超时率。
- 后续提交本轮功能后再进入下一阶段，避免支付链路成果和后续需求混在一个提交里。

## 21. 建议提交信息

```text
add payment records and callback idempotency
```

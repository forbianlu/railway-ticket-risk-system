# GitHub 上传步骤

## 1. 初始化本地仓库

```bash
cd railway-ticket-risk-system
git init
git add .
git commit -m "init railway ticket risk system"
```

## 2. 在 GitHub 创建空仓库

仓库名建议：

```text
railway-ticket-risk-system
```

仓库描述建议：

```text
Railway passenger ticketing and risk control management system built with Spring Boot.
```

## 3. 关联远程仓库

把下面的地址替换成你自己的 GitHub 仓库地址：

```bash
git remote add origin https://github.com/your-name/railway-ticket-risk-system.git
git branch -M main
git push -u origin main
```

## 4. README 截图建议

后端跑起来后，建议补充这些截图：

- 运营看板
- 车次查询结果
- 下单成功后的订单列表
- 风险事件列表
- H2 表数据或数据库 ER 图

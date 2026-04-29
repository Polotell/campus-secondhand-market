# 系统架构设计

> 对应实验报告 **4.1 系统架构设计** 章节

## 4.1.1 物理架构图

```
+--------------------+       +---------------------+       +--------------------+
|  frontend-user     |       |  frontend-admin     |       |  浏览器用户         |
|  Vue3+ElementPlus  |       |  Vue3+ElementPlus   |       |  （学生 / 商家 /    |
|  http://localhost  |       |  http://localhost   |       |    管理员）         |
|        :5173       |       |        :5174        |       +--------------------+
+---------+----------+       +----------+----------+
          |                             |
          |   Axios (JSON, Bearer Token)|
          +-------------+---------------+
                        |
                        v
             +----------------------+
             | Spring Boot 后端     |
             | localhost:8080       |
             |  - Controller        |
             |  - Service           |
             |  - Mapper            |
             |  - AOP / 拦截器     |
             +----------+-----------+
                        |
        +---------------+-----------------+
        |                                 |
        v                                 v
+----------------+          +----------------------------+
|  MySQL 8.x     |          |  本地文件存储              |
|  db=2023011308 |          |  D:/campus-market-uploads/ |
|  (产品/订单/   |          |  （商品图、营业执照、身份证、|
|   用户/流水)   |          |   头像、退货凭证）         |
+----------------+          +----------------------------+
```

### 部署关系说明

| 组件 | 地址 / 端口 | 说明 |
|---|---|---|
| 用户端前端 | `http://localhost:5173` | 注册/登录、商城浏览、下单、钱包、积分、买家中心、商家中心 |
| 管理后台前端 | `http://localhost:5174` | 审核用户、审核商品、调整商家等级、充值、管理轮播 |
| 后端 API | `http://localhost:8080/api` | `context-path=/api`，与 Controller 中 `/auth` 等拼接为完整路径；未接 Swagger |
| 数据库 | `localhost:3306/2023011308` | 唯一数据库（数据库名=学号） |
| 文件存储 | `D:/campus-market-uploads/` | Spring Boot 通过 `/uploads/**` 静态资源映射对外访问 |

### 程序资源清单（GitHub 仓库建议：`campus-market-2023011308`）

| 资源类型 | 路径 | 作用说明 |
|---|---|---|
| 后端工程 | `backend/` | Spring Boot 主工程，打包产物 `campus-market-0.0.1.jar` |
| 后端文档 | `backend/doc/` | 架构图、E-R、接口清单、时序图、模块表 |
| 建表脚本 | `backend/sql/schema.sql` | 建库 + 建表 DDL |
| 初始数据 | `backend/sql/data.sql` | 管理员、分类、等级费率种子数据 |
| Mapper XML | `backend/src/main/resources/mapper/` | MyBatis Plus 多表关联复杂 SQL |
| 用户端前端 | `frontend-user/` | 买家 + 商家共用界面 |
| 管理后台前端 | `frontend-admin/` | 管理员专用 |
| 课程报告 | （自行维护 Word/PDF） | 与 `backend/doc` 中章节互链 |

## 4.1.2 逻辑分工（分层职责划分）

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 Vue 组件层                         │
│              View <── Pinia（状态）── Router（路由）         │
└─────────────────────────────────────────────────────────────┘
                            │
                    Axios (Bearer JWT)
                            │
┌─────────────────────────────────────────────────────────────┐
│  ① Controller 层   —— 接口入参校验、结果 Result<T> 封装     │
│      @RestController  /  @Valid  /  @RequiresRole(AOP)      │
│  职责：路由分发；不含任何业务                                │
├─────────────────────────────────────────────────────────────┤
│  ② Service 层      —— 业务编排、事务控制、状态机流转         │
│      @Service / @Transactional / 状态校验 / 规则计算         │
│  职责：所有业务规则（资金流转、积分抵扣、24h 退货、等级费率）│
├─────────────────────────────────────────────────────────────┤
│  ③ Mapper(DAO) 层  —— 持久化操作                             │
│      继承 BaseMapper<T>；复杂 SQL 写 XML                     │
│  职责：CRUD + 多表联查，不含业务                             │
├─────────────────────────────────────────────────────────────┤
│  ④ Entity / DTO / VO                                         │
│      entity 一表一对象；dto 接收入参；vo 返回视图            │
└─────────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────────┐
│  横切层（AOP）                                               │
│   - LogAspect      ：请求日志、执行耗时                      │
│   - AuthAspect     ：@RequiresRole 权限校验                  │
│   - 操作留痕       ：如 `operation_log` 表在业务层（如 Admin）写入
│  全局异常 GlobalExceptionHandler 统一拦截异常→Result         │
└─────────────────────────────────────────────────────────────┘
```

### 各层职责明细

**Controller 层**：
- 只做 HTTP 协议适配：路径映射、参数接收（`@RequestBody`/`@RequestParam`/`@PathVariable`）、`@Valid` 校验。
- 返回统一 `Result<T>`。**禁止写任何业务 if/else**，一旦超过 5 行逻辑就应下沉到 Service。

**Service 层**：
- 承载所有业务规则。
- **事务边界**就是 Service 方法边界，`@Transactional(rollbackFor = Exception.class)`。
- 跨表操作、状态机校验、资金计算、积分计算全部在这里。
- 例如 `OrderService.pay()` 内部完成：扣买家钱包 → 计算平台手续费 → 入中间账户 → 扣库存 → 生成订单 → 记钱包流水 → 清购物车，全过程一个事务。

**Mapper 层**：
- 简单 CRUD 直接用 MyBatis Plus `BaseMapper<T>` 提供的方法。
- 涉及分页 + 多表 JOIN（如商品列表带商家信息、评分聚合）写在 XML 里。
- **禁止在 Mapper 里写业务 if**。

**Entity / DTO / VO 拆分原因**：
- `Entity` 和数据库一对一，字段不会直接暴露给前端（如密码、软删除标记）。
- `DTO` 用于接收前端请求，字段可做 `@NotBlank/@Size` 校验。
- `VO` 用于封装返回给前端的数据，可聚合多表信息（如商品 VO 含商家名、评分均值）。
- 这样做避免前端 payload 和数据库模型直接耦合，修改表结构时前端不感知。

## 技术栈选型理由（对应报告 3.3）

| 技术 | 选型 | 对比备选 | 选择理由 |
|---|---|---|---|
| 后端框架 | Spring Boot 2.7 | Spring MVC、SpringCloud、Solon | 单体项目 + 团队熟悉度高 + 约定大于配置，快速起步 |
| 持久层 | MyBatis Plus 3.5 | Hibernate/JPA、JdbcTemplate | 课程要求；BaseMapper 减少样板 SQL，XML 支持复杂 SQL |
| 数据库 | MySQL 8.x | PostgreSQL、H2 | 校园环境普及度高，课程标配 |
| 前端框架 | Vue 3 + Vite | Vue 2、React | 组合式 API + 响应式性能更好，Vite 启动快 |
| UI 库 | Element Plus | Ant Design Vue、Vant | PC 后台/商城首选，组件全面 |
| 状态管理 | Pinia | Vuex | Vue 3 官方推荐，TypeScript 友好 |
| 鉴权 | JWT | Session、OAuth2 | 无状态、前后端分离天然契合；便于 AOP 切权限 |
| 文件存储 | 本地磁盘 + 静态映射 | OSS、MinIO | 实验环境无需外部依赖，课程重点不在存储 |
| AI 辅助 | Cursor + Claude | Copilot、Codex | 支持 `.cursorrules` 项目级规则约束，规范代码风格 |

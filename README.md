# 校园二手交易平台

> 《软件开发实践》课程实验项目 · 基于 Spring Boot 的综合 Web 应用实践

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5-42b883)](https://vuejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-Course%20Project-lightgrey)](#)

面向校园场景的二手交易系统，采用**前后端分离**架构：统一 Spring Boot 后端 + 用户端 / 管理端两个 Vue 前端。覆盖注册审核、商品发布、购物车下单、资金托管、发货收货、退货结算、评价、钱包积分，以及轮播图、黑名单等扩展功能。

**数据库名：** `2023011308`（组员学号）

**代码仓库：** https://github.com/Polotell/campus-secondhand-market

---

## 团队成员

| 姓名 | 学号 | 角色 |
|---|---|---|
| 王昊宇 | 2023011308 | 后端 / 项目负责人 |
| 杨吝时 | 2023011306 | 前端 |

---

## 功能概览

### 普通用户
- 图形验证码注册，管理员审核通过后登录
- 商品搜索、排序（价格 / 销量 / 好评率）
- 购物车、一键下单、钱包扣款、积分抵扣
- 确认收货、24 小时内申请退货
- 商品评价、商家服务评价

### 商家
- 提交营业执照 / 身份证注册，审核通过后开店
- 多图发布 / 下架商品，处理订单发货与退货审核
- 评价买家；管理店铺级买家黑名单

### 管理员
- 审核用户注册、审核商品上架
- 设置商家等级（1～5 级）与用户充值
- 管理首页轮播图、平台级黑名单、用户封禁

### 核心业务链路

```
发布商品 → 管理员审核 → 买家下单（扣余额 + 托管 ESCROW）
  → 商家发货 → 确认收货（或 7 天自动确认）
  → 结算（商家到账 + 平台手续费）→ 双向评价
```

---

## 技术栈

| 层次 | 技术 |
|---|---|
| 后端 | Spring Boot 2.7 · MyBatis-Plus 3.5 · MySQL 8 · JWT · AOP · Hutool · Lombok |
| 前端 | Vue 3 · Vite 5 · Element Plus · Pinia · Vue Router · Axios |
| 构建 | Maven · pnpm |
| 文档 | Markdown · PlantUML |

---

## 系统架构

```
┌─────────────────┐     ┌─────────────────┐
│ frontend-user   │     │ frontend-admin  │
│  :5173          │     │  :5174          │
└────────┬────────┘     └────────┬────────┘
         │    Axios + JWT        │
         └──────────┬────────────┘
                    ▼
         ┌──────────────────────┐
         │  Spring Boot :8080   │
         │  context-path: /api  │
         └──────────┬───────────┘
                    │
        ┌───────────┴───────────┐
        ▼                       ▼
┌───────────────┐     ┌─────────────────────────┐
│ MySQL 8       │     │ 本地文件存储             │
│ 2023011308    │     │ D:/campus-market-uploads│
│ （17 张表）    │     │ 映射 /uploads/**        │
└───────────────┘     └─────────────────────────┘
```

**分层结构：** `Controller → Service → Mapper → Entity`，配合 DTO 入参、VO 出参、`Result<T>` 统一响应。

---

## 工程结构

```
校园二手交易平台/
├── README.md
├── .cursorrules                 # 项目编码规范
│
├── backend/                     # Spring Boot 后端
│   ├── pom.xml
│   ├── sql/
│   │   ├── schema.sql           # 建库 + 17 张表
│   │   └── data.sql             # 初始数据（管理员、测试账号）
│   ├── doc/
│   │   ├── architecture.md      # 架构设计
│   │   ├── ER.md                # E-R 与表清单
│   │   ├── API.md               # REST 接口清单
│   │   ├── flow.md              # 核心业务时序图
│   │   ├── modules.md           # 模块拆分与工时
│   │   ├── demo-core-flow.md    # 演示链路与答辩 Q&A
│   │   └── uml/                 # PlantUML 源文件
│   └── src/main/
│       ├── java/com/campus/market/
│       │   ├── controller/      # REST 接口
│       │   ├── service/         # 业务逻辑
│       │   ├── mapper/          # 数据访问
│       │   ├── entity/dto/vo/   # 实体与传输对象
│       │   ├── config/          # 跨域、JWT、MyBatis 等配置
│       │   └── aspect/          # AOP 权限、日志
│       └── resources/
│           ├── application.yml
│           └── mapper/          # MyBatis XML
│
├── frontend-user/               # 用户端（买家 + 商家）→ :5173
│   └── src/
│       ├── views/               # 页面
│       ├── api/                 # Axios 封装
│       └── components/          # 图片上传等组件
│
└── frontend-admin/              # 管理后台 → :5174
    └── src/
        └── views/               # 审核、轮播、黑名单等
```

---

## 环境要求

| 工具 | 版本建议 |
|---|---|
| JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Node.js | 18+ |
| pnpm | 8+ |

---

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/Polotell/campus-secondhand-market.git
cd campus-secondhand-market/校园二手交易平台
```

### 2. 初始化数据库

**务必使用 UTF-8（utf8mb4）客户端导入**，否则中文可能乱码：

```bash
mysql --default-character-set=utf8mb4 -u root -p < backend/sql/schema.sql
mysql --default-character-set=utf8mb4 -u root -p 2023011308 < backend/sql/data.sql
```

如需补充中文演示商品：

```bash
mysql --default-character-set=utf8mb4 -u root -p 2023011308 < backend/sql/seed-products-demo-cn.sql
```

> 若已有库且中文变成问号，见 [字符集修复](#字符集乱码修复)。

### 3. 配置后端

编辑 `backend/src/main/resources/application.yml`，或通过环境变量覆盖数据库密码：

```bash
# Windows PowerShell
$env:DB_PASSWORD="你的MySQL密码"

# Linux / macOS
export DB_PASSWORD=你的MySQL密码
```

确认本机存在文件上传目录（默认 `D:/campus-market-uploads/`，可在 `application.yml` 的 `file.upload-dir` 修改）。

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

启动成功后 API 基址：`http://localhost:8080/api`

健康检查：`GET http://localhost:8080/api/hello`

### 5. 启动前端

```bash
# 用户端
cd frontend-user
pnpm install
pnpm dev          # http://localhost:5173

# 管理后台（新开终端）
cd frontend-admin
pnpm install
pnpm dev          # http://localhost:5174
```

---

## 测试账号

密码均为 **`admin123`**（来自 `data.sql`）：

| 角色 | 用户名 | 说明 |
|---|---|---|
| 管理员 | `admin` | 管理后台登录 |
| 普通用户 | `student01` / `student02` | 买家测试 |
| 商家 | `merchant01` | 商家中心测试 |

---

## 核心业务规则

### 商家等级费率

| 等级 | 费率 |
|:---:|---|
| 1 | 0.1% |
| 2 | 0.2% |
| 3 | 0.5% |
| 4 | 0.75% |
| 5 | 1% |

下单时按商家当前等级**快照**写入订单，结算时从 ESCROW 拆分至商家余额与平台 FEE 账户。

### 积分规则

- 消费 1 元 → 获得 1 积分（订单 `COMPLETED` 后发放）
- 100 积分 → 抵扣 1 元（下单时使用）

### 时限规则

| 规则 | 说明 |
|---|---|
| 7 天自动确认 | 发货后 7 天买家未确认，系统自动确认收货 |
| 24 小时退货 | 确认收货后 24 小时内可申请退货，超时禁止 |
| 资金托管 | 下单后货款进入平台 ESCROW，结算或退货后再分账 |

### 订单状态主路径

```
PAID → SHIPPED → RECEIVED → COMPLETED
                    ↓
              RETURN_APPLYING → RETURNED（同意退货）
```

---

## 主要 API 模块

完整接口清单见 [`backend/doc/API.md`](backend/doc/API.md)。

| 模块 | 路径前缀 | 说明 |
|---|---|---|
| 认证 | `/auth` | 注册、登录、当前用户 |
| 商品 | `/products` | 搜索、详情（公开） |
| 购物车 | `/cart` | 加购、修改、删除 |
| 订单 | `/orders` | 预览、下单、确认收货、退货 |
| 商家 | `/merchant` | 商品发布、发货、黑名单 |
| 管理 | `/admin` | 审核、充值、等级、轮播 |
| 文件 | `/file/upload` | 图片上传（multipart） |
| 首页 | `/home/carousels` | 轮播图（游客可见） |
| 钱包 | `/wallet` | 用户自助充值 |

统一响应格式：

```json
{ "code": 0, "message": "success", "data": { } }
```

`code = 0` 表示成功；业务错误码见 `ResultCode` 枚举。

---

## 设计文档

| 文档 | 说明 |
|---|---|
| [architecture.md](backend/doc/architecture.md) | 物理架构、分层职责、技术选型 |
| [ER.md](backend/doc/ER.md) | E-R 关系、17 张表清单、状态机 |
| [API.md](backend/doc/API.md) | REST 接口与错误码 |
| [flow.md](backend/doc/flow.md) | 下单→结算时序图 |
| [demo-core-flow.md](backend/doc/demo-core-flow.md) | 演示步骤与答辩要点 |
| [UML.md](backend/doc/UML.md) | PlantUML 图索引 |

---

## 开发规范

项目根目录 [`.cursorrules`](.cursorrules) 约定：

1. 数据库名必须为学号 `2023011308`
2. 持久层使用 MyBatis-Plus，不使用 JPA / Hibernate
3. Controller 统一返回 `Result<T>`，不写业务逻辑
4. 涉及资金与状态流转的方法使用 `@Transactional` 并加中文注释
5. 前端联调前先确认 `API.md` 与后端实现一致

---

## 字符集乱码修复

若界面或 API 返回中文为问号：

1. 执行 `backend/sql/fix-charset-utf8mb4.sql`
2. 再执行 `backend/sql/repatch-chinese-data.sql`

Windows 下推荐用 CMD 导入（避免 PowerShell 破坏 UTF-8 字节）：

```cmd
chcp 65001
mysql --default-character-set=utf8mb4 -u root -p 2023011308 < backend\sql\repatch-chinese-data.sql
```

JDBC 连接串使用 `characterEncoding=UTF-8`（不要写成 `utf8mb4`）。

---

## 已实现与待扩展

### 已实现
- [x] 用户 / 商家 / 管理员三角色与 JWT 鉴权
- [x] 图形验证码注册与管理员审核
- [x] 商品多图发布、搜索排序、购物车、下单
- [x] ESCROW 托管、商家等级费率、定时自动确认与结算
- [x] 24 小时退货、三类评价（商品 / 商家 / 买家）
- [x] 钱包充值（管理员 + 用户自助）、积分抵扣
- [x] 轮播图、黑名单、商家封禁（可选功能）

### 待扩展（以 API.md 第四节为准）
- [ ] 独立钱包 / 积分流水查询 API
- [ ] 店铺聚合接口 `GET /shops/{id}`
- [ ] 首页商品推荐接口

---

## 常见问题

**Q：后端启动报 `Access denied for user 'root'`？**  
A：检查 MySQL 密码，设置环境变量 `DB_PASSWORD` 或修改 `application.yml`。

**Q：8080 端口被占用？**  
A：`netstat -ano | findstr :8080` 查 PID 后结束旧进程，再重启后端。

**Q：图片上传成功但页面不显示？**  
A：确认 `file.upload-dir` 目录存在，且 `WebMvcConfig` 已映射 `/uploads/**`。

**Q：管理端审核用户报「用户不存在」？**  
A：雪花 ID 超出 JS 精度，前端需用字符串处理 id；后端 VO 已加 `ToStringSerializer`。

---

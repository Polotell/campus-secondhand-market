校园二手交易平台
《软件开发实践》课程实验项目 · 基于 Spring Boot 综合应用实践 数据库名：2023011308（组员学号）

一、项目简介
面向校园场景的二手交易市场，包含用户端商城和管理员后台审核系统两部分：

普通用户：注册、浏览、购买、评价、退货、查看钱包/积分
商家用户：注册（提交营业执照/身份证）、发布商品、发货、审核退货、评价买家
管理员：审核用户注册、审核商品、调整商家等级（1~5）、给用户充值、管理轮播图与黑名单
完整业务流程覆盖："发布 → 审核 → 下单 → 资金托管 → 发货 → 确认收货 → 结算（扣手续费）→ 评价"，并支持7 天自动确认 和 24 小时退货。

二、技术栈
层	技术
后端	Spring Boot 2.7 · MyBatis Plus 3.5 · MySQL 8 · JWT · AOP · Hutool · Lombok
前端	Vue 3 · Vite · Element Plus · Pinia · Vue Router · Axios
构建	Maven · pnpm
文档	Markdown · PlantUML
AI 辅助	Cursor + Claude（基于 .cursorrules 约束代码风格）
三、工程结构
校园二手交易平台/
├─ .cursorrules              Cursor 项目级编码规则（强制约束）
├─ README.md                 本文件
│
├─ backend/                  后端 Spring Boot 工程
│   ├─ pom.xml              Maven 工程描述与依赖
│   ├─ sql/
│   │   ├─ schema.sql       ★ 建库 + 17 张表 DDL（数据库名 2023011308）
│   │   └─ data.sql         ★ 初始数据（管理员、分类、测试用户商家）
│   ├─ doc/
│   │   ├─ architecture.md  ★ 系统架构设计（物理+逻辑分工+技术选型）
│   │   ├─ ER.md            ★ E-R 关系 + 17 张表清单
│   │   ├─ API.md           ★ REST 接口清单（与当前 Controller/ResultCode 一致）
│   │   ├─ flow.md          ★ 下单-扣费-收货-结算时序图
│   │   └─ modules.md       ★ 模块拆分 + 工时表 + 测试点
│   └─ src/
│       └─ main/
│           ├─ java/com/campus/market/...
│           └─ resources/
│               ├─ application.yml
│               └─ mapper/     （MyBatis Plus XML）
│
├─ frontend-user/             用户端（买家+商家）  http://localhost:5173
│   └─ src/
│
├─ frontend-admin/            管理后台           http://localhost:5174
│   └─ src/
│
└─ test-auto-confirm.ps1     （可选）本机联调/回归：自动确认与结算等接口脚本
四、设计文档索引
文档	位置	对应报告章节
系统架构设计	backend/doc/architecture.md	4.1
E-R 与表清单	backend/doc/ER.md	4.2
建表 SQL	backend/sql/schema.sql	4.2.2
初始数据 SQL	backend/sql/data.sql	4.2.2
RESTful 接口清单	backend/doc/API.md	4.3
核心业务时序图	backend/doc/flow.md	4.6
UML（PlantUML）	uml/order-business-swimlanes.puml（泳道）、backend-package-modules.puml、order-domain-classes.puml、UML.md、flow.md（时序）	4.6 / 4.x
模块拆分与工时表	backend/doc/modules.md	3.1 / 3.4
五、快速开始
5.1 初始化数据库
请务必使用 UTF-8（utf8mb4）客户端导入，否则中文会变成问号乱码：

mysql --default-character-set=utf8mb4 -u root -p < backend/sql/schema.sql
mysql --default-character-set=utf8mb4 -u root -p 2023011308 < backend/sql/data.sql
若数据库早已初始化，仅需 中文分类 + 演示商品：

mysql --default-character-set=utf8mb4 -u root -p 2023011308 < backend/sql/seed-products-demo-cn.sql
若界面/API 仍乱码：先在库里执行 backend/sql/fix-charset-utf8mb4.sql，再执行 backend/sql/repatch-chinese-data.sql（用 CMD：cmd /c "chcp 65001>nul && mysql --default-character-set=utf8mb4 -u root -p 2023011308 < backend\sql\repatch-chinese-data.sql"），可修正已变成问号的历史中文。不要使用 PowerShell 管道导入 UTF-8 中文 SQL，易被破坏字节。

后端 application.yml 里 JDBC 使用 characterEncoding=UTF-8（与数据库 utf8mb4 并存）；不可用 characterEncoding=utf8mb4，否则驱动无法识别 Charset。

5.2 启动后端
cd backend
mvn spring-boot:run
# 默认 http://localhost:8080/api （context-path 为 /api）
# 本仓库未集成 SpringDoc，无内置 Swagger UI；联调见 backend/doc/API.md 或使用 Postman
5.3 启动前端
# 用户端
cd frontend-user
pnpm install
pnpm dev       # http://localhost:5173

# 管理后台
cd frontend-admin
pnpm install
pnpm dev       # http://localhost:5174
5.4 测试账号（来自 data.sql）
角色	用户名	密码
管理员	admin	admin123
普通用户	student01 / student02	admin123
商家	merchant01	admin123
六、核心业务规则摘要
6.1 商家等级费率表
等级	费率
1	0.1%
2	0.2%
3	0.5%
4	0.75%
5	1%
6.2 积分规则
消费 1 元 = 获得 1 积分（结算成功后发放）
100 积分 = 抵扣 1 元现金（下单时使用）
6.3 时限规则
7 天自动确认：商家发货后 7 天买家未确认，系统自动确认并结算
24 小时退货：买家确认收货后 24 小时内可申请退货，超时禁止退货
中间账户托管：买家下单后货款进平台 ESCROW 账户，直到结算或退货
七、开发规范
严格遵守工程根目录的 .cursorrules，核心要点：

数据库名必须是学号（2023011308）
强制 MyBatis Plus，禁止 Hibernate/JPA
Controller 统一返回 Result<T>
涉及资金/状态流转的方法必须写中文注释 + 说明事务/AOP 原理
前端联调前必须先确认后端接口已实现
八、项目进度（与当前仓库一致）
 设计文档（backend/doc/* 与 README 持续按代码维护）
 后端：Spring Boot + MyBatis Plus + JWT + 多 Controller，订单/退货/资金与定时任务见 OrderServiceImpl 等
 双前端 Vite 工程与核心页面联调
 注册/登录、验证码、商品、购物车、订单、管理端用户/商品/轮播/黑名单、商家端订单与商品与黑名单
 可选：独立钱包/积分流水查询 API、店铺聚合接口 GET /shops/{id}、首页推荐等未实现能力（以 API.md「第四节」为准）
接口与错误码以 backend/doc/API.md 与 ResultCode 为准；业务流程与定时间隔以 flow.md 与 OrderServiceImpl 为准。

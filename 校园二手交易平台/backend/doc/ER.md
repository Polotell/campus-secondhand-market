# 数据库设计（E-R 关系 + 数据表清单）

> 对应实验报告 **4.2 数据库设计** 章节
> 数据库名：`2023011308`（组员学号）

## 一、E-R 关系总览图（文字版）

```
                          ┌───────────────────┐
                          │      user          │
                          │ （普通用户/商家/   │
                          │   管理员 三种角色）│
                          └─┬──────┬──────┬───┘
                            │ 1    │ 1    │ 1
               ┌────────────┘      │      └─────────────┐
               │ N                 │ N                  │ N
     ┌─────────▼────────┐    ┌─────▼───────────┐  ┌────▼──────────────┐
     │   cart_item       │    │   product        │  │ wallet_transaction│
     │ （购物车 一对多） │    │ （商家发布商品） │  │ （钱包流水）       │
     └────────┬──────────┘    └─────┬─────┬─────┘  └───────────────────┘
              │  N               1  │     │ 1
              │ ┌──────────────────┘     └────┐ N
              │ │                             ▼
              │ │N                  ┌──────────────────┐
              │ │                   │  product_image    │
              │ │                   │ （商品图片 1:N）  │
              │ │                   └──────────────────┘
              │ │
              │ │ 结算一键下单
              ▼ ▼
     ┌──────────────────┐    1:N  ┌────────────────────┐
     │      order        │◄───────│    order_item      │
     │ （订单主表）      │        │ （订单明细 商品快照）│
     └───┬──┬──┬─────────┘        └────────────────────┘
       1 │  │1 │ 1
         │  │  │
         │  │  └────► return_record       （退货申请 1:1 或 1:N）
         │  │
         │  └───────► product_review      （商品评价）
         │           merchant_review      （商家服务评价）
         │           buyer_review         （商家对买家评价）
         │
         └──────────► points_transaction  （积分流水）
                      wallet_transaction  （钱包流水）

     ┌──────────────────┐
     │ platform_account  │  平台中间账户（托管买家货款 + 手续费收入）
     └──────────────────┘

     ┌──────────────────┐
     │ user_blacklist    │  买家黑名单（商家级 / 平台级）
     │ carousel          │  首页轮播图（可选）
     │ operation_log     │  操作日志（AOP 记录，答辩展示用）
     └──────────────────┘
```

## 二、核心实体关系说明

| 关系 | 基数 | 说明 |
|---|---|---|
| user ─── product | 1 : N | 一个商家可发布多个商品（`product.merchant_id`）|
| product ─── product_image | 1 : N | 一个商品多张图 |
| user ─── cart_item | 1 : N | 一个用户购物车有多条商品 |
| user ─── order | 1 : N | 作为买家；同时商家也作为 `order.merchant_id` 关联 |
| order ─── order_item | 1 : N | 一个订单含多件（同一商家的）商品，便于购物车跨商家拆单 |
| order ─── return_record | 1 : 1 | 单次退货（按整单退） |
| order_item ─── product_review | 1 : 1 | 每个商品买后可评价一次 |
| order ─── merchant_review | 1 : 1 | 对商家整体服务评价一次 |
| order ─── buyer_review | 1 : 1 | 商家评价买家一次，影响买家好评率 |
| user ─── wallet_transaction | 1 : N | 钱包全部进出流水可追溯 |
| user ─── points_transaction | 1 : N | 积分全部进出流水可追溯 |

## 三、数据表清单（17 张表）

| # | 表名 | 中文名 | 说明 |
|---|---|---|---|
| 1 | `user` | 用户表 | 三种角色共用（普通用户/商家/管理员） |
| 2 | `product_category` | 商品分类表 | 树形分类（支持一级/二级） |
| 3 | `product` | 商品表 | 商家发布的商品 |
| 4 | `product_image` | 商品图片表 | 商品多图 |
| 5 | `cart_item` | 购物车表 | 用户购物车明细 |
| 6 | `order` | 订单主表 | **注意 order 为 SQL 关键字，需反引号** |
| 7 | `order_item` | 订单明细表 | 包含商品快照字段 |
| 8 | `return_record` | 退货申请表 | 24 小时时限、商家审核 |
| 9 | `product_review` | 商品评价表 | 五星+文字+图片 |
| 10 | `merchant_review` | 商家服务评价 | 对商家服务态度评分 |
| 11 | `buyer_review` | 买家评价表 | 商家评价买家，影响买家好评率 |
| 12 | `wallet_transaction` | 钱包流水表 | 充值/支付/退款/入账/手续费/托管进出 |
| 13 | `points_transaction` | 积分流水表 | 获得/抵扣/退货退回 |
| 14 | `platform_account` | 平台中间账户 | 托管货款 + 手续费收入 |
| 15 | `user_blacklist` | 买家黑名单表 | 商家级 / 平台级 |
| 16 | `carousel` | 首页轮播图 | 可选功能 |
| 17 | `operation_log` | 操作日志表 | AOP 切面记录，答辩展示 AOP |

## 四、关键字段设计说明

### 4.1 用户表 `user` 角色与状态

| 字段 | 类型 | 枚举值 | 说明 |
|---|---|---|---|
| `role` | VARCHAR(20) | USER / MERCHANT / ADMIN | 用户角色 |
| `status` | VARCHAR(20) | PENDING / APPROVED / REJECTED / BANNED | 审核状态；普通用户/商家注册后 PENDING，管理员审核后 APPROVED |
| `merchant_level` | TINYINT | 1~5 | 商家等级；非商家为 NULL |
| `ban_until` | DATETIME | - | 限时封禁截止；为空表示未封禁 |

### 4.2 商品状态机 `product.status`

```
DRAFT ──商家提交审核──> PENDING
PENDING ──管理员审核通过──> ON_SALE
PENDING ──管理员拒绝──> REJECTED
ON_SALE ──买家下单锁定──> LOCKED
LOCKED ──订单完成──> SOLD
ON_SALE ──商家下架──> OFF_SHELF
OFF_SHELF ──商家重新上架──> ON_SALE
```

### 4.3 订单状态机 `order.status`（与当前 `OrderServiceImpl` / `ReturnServiceImpl` 一致）

```
PAID（已付款，货款在 ESCROW）
  └──> SHIPPED（商家发货；auto_confirm_at = 发货 + 7 天）
        ├──> RECEIVED（买家主动确认 或 定时任务 7 天自动确认；return_deadline = 确认时刻 + 24h）
        │      ├──> RETURN_APPLYING（24h 内申请退货）
        │      │     ├──> 商家同意 → RETURNED（退款、回库）
        │      │     └──> 商家拒绝 → 当前实现为**立即**打款/结算，订单直接 COMPLETED
        │      └──> COMPLETED（无退货申请，RECEIVED 且超过 return_deadline 后由定时任务结算）
        └──> CANCELLED 等（异常/取消，见实现）
```

**两个关键时间字段**：
- `auto_confirm_at` = 预计自动确认时间（发货 + 7 天）→ `OrderServiceImpl` 定时任务将 SHIPPED 置为 RECEIVED。
- `return_deadline` = 可退货截止时间 → 超期不可申请；未退货且过期的 RECEIVED 由定时任务结算为 COMPLETED。

> 注：枚举中若仍包含 `RETURN_REJECTED`，可与 DB/扩展用法预留一致；**主业务路径**下拒绝退货在代码中**直接**进入 `COMPLETED`，详见 `backend/doc/flow.md`。

### 4.4 金额与积分字段规范

| 字段类别 | 数据库类型 | Java 类型 | 样例 |
|---|---|---|---|
| 金额 | `DECIMAL(12,2)` | `BigDecimal` | `balance`, `amount`, `platform_fee` |
| 积分 | `INT UNSIGNED` | `Integer` | `points`, `points_used` |
| 费率 | `DECIMAL(5,4)` | `BigDecimal` | `platform_fee_rate`（0.0010 = 0.1%）|

### 4.5 通用字段（所有业务表必带）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT UNSIGNED` | 雪花 ID（`@TableId(type = ASSIGN_ID)`）|
| `created_at` | `DATETIME` | 创建时间，`fill = INSERT` 自动填充 |
| `updated_at` | `DATETIME` | 更新时间，`fill = INSERT_UPDATE` 自动填充 |
| `deleted` | `TINYINT(1)` | 软删除标记，`@TableLogic`；0=未删除，1=已删除 |

## 五、索引设计建议

| 表 | 索引 | 用途 |
|---|---|---|
| `user` | `uk_username`、`idx_role_status` | 登录、审核列表 |
| `product` | `idx_merchant_status`、`idx_category`、`idx_name`（全文/前缀） | 店铺商品、分类筛选、搜索 |
| `order` | `idx_buyer_status`、`idx_merchant_status`、`idx_auto_confirm_at` | 买家订单、商家订单、定时扫描 |
| `cart_item` | `uk_user_product` | 防止重复加购 |
| `wallet_transaction` | `idx_user_created` | 用户钱包流水分页 |

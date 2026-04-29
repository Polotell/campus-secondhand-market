# RESTful 接口清单（与当前代码一致）

> 对应实验报告 **4.3**；本文档以仓库内 `backend` 的 Controller 与 `com.campus.market.common.ResultCode` 为准。  
> **全局路径前缀**：`application.yml` 中 `server.servlet.context-path: /api`，下表在「路径」列中写**去掉 `/api` 之后**的片段，实际访问为 `http://<host>:8080/api` + 路径。  
> **鉴权**：`LoginInterceptor` 对非白名单路径要求请求头 `Authorization: Bearer <JWT>`；类或方法上 `@RequiresRole` 时由 AOP 校验角色。  
> 图例：**🔒** 需登录；**🏪** 需商家角色；**👑** 需管理员。

## 一、设计原则（仍适用）

1. **HTTP 与业务码**：`Result<T>` 中 `code` 取 `ResultCode`；与 HTTP 状态码可并存，前端以 `data.code === 0` 判成功。  
2. **分页**：`PageResult` 字段为 `records、total、pageNum、pageSize`（页码从 1 起）。各接口默认值以方法参数 `defaultValue` 为准。  
3. **文件上传白名单**：`POST /file/upload` 允许匿名，便于注册时上传资质图（见 `FileController` 注释与 `WebMvcConfig` 白名单）。

## 二、全局错误码（`ResultCode` 枚举，与代码一致）

| 码 | 含义 |
|---:|---|
| 0 | 成功（`SUCCESS`） |
| 400 | 请求参数错误（`BAD_REQUEST`） |
| 401 | 未登录或登录已过期（`UNAUTHORIZED`） |
| 403 | 无权限（`FORBIDDEN`） |
| 404 | 资源不存在（`NOT_FOUND`） |
| 500 | 服务器内部错误（`INTERNAL_ERROR`） |
| 10001 | 账号尚未通过审核（`ACCOUNT_PENDING_AUDIT`） |
| 10002 | 账号已被封禁（`ACCOUNT_BANNED`） |
| 10003 | 验证码错误或已过期（`CAPTCHA_INVALID`） |
| 10004 | 用户名已存在（`USERNAME_EXIST`） |
| 10005 | 用户名或密码错误（`USERNAME_OR_PASSWORD_WRONG`） |
| 10006 | 账号审核未通过（`ACCOUNT_REJECTED`） |
| 10007 | 当前状态不允许此审核操作（`AUDIT_STATUS_ILLEGAL`） |
| 20001 | 商品库存不足（`PRODUCT_STOCK_NOT_ENOUGH`） |
| 20002 | 商品已下架或未上架（`PRODUCT_NOT_ON_SALE`） |
| 20003 | 商品不存在（`PRODUCT_NOT_EXIST`） |
| 20004 | 当前不可评价（`REVIEW_NOT_ALLOWED`） |
| 20005 | 已评价过，不能重复提交（`REVIEW_DUPLICATE`） |
| 30001 | 钱包余额不足（`BALANCE_NOT_ENOUGH`） |
| 30002 | 积分余额不足（`POINTS_NOT_ENOUGH`） |
| 40001 | 订单状态不允许此操作（`ORDER_STATUS_ILLEGAL`） |
| 40002 | 已超过 24 小时退货时限（`RETURN_DEADLINE_EXCEEDED`） |
| 40003 | 订单未完成，不能评价（`ORDER_NOT_COMPLETED`） |
| 40004 | 订单不存在（`ORDER_NOT_EXIST`） |
| 40005 | 购物车没有勾选/提交任何商品（`CART_EMPTY`） |
| 40006 | 一次下单仅支持同一家商家（`CART_MULTI_MERCHANT`） |
| 40007 | 购物车条目不存在或不属于当前用户（`CART_ITEM_NOT_EXIST`） |
| 40008 | 不能购买自己店铺的商品（`CANNOT_BUY_OWN_PRODUCT`） |
| 40009 | 退货记录不存在（`RETURN_RECORD_NOT_EXIST`） |
| 40010 | 该订单已申请退货，不可重复提交（`RETURN_RECORD_DUPLICATE`） |
| 40011 | 退货当前状态不允许此操作（`RETURN_STATUS_ILLEGAL`） |
| 50001 | 已被商家或平台拉黑，无法购买（`BLACKLISTED`） |

## 三、按 Controller 的接口清单

### 1. 验证码 `CaptchaController`（匿名）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/captcha` | 返回 `CaptchaUtil.Captcha`（含 `captchaKey`、图片等） |

### 2. 认证 `AuthController`（`/auth`）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/register/user` | 普通用户注册 |
| POST | `/auth/register/merchant` | 商家注册 |
| POST | `/auth/login` | 登录，成功返回 `LoginVO`（`token、tokenPrefix、user`） |
| GET | `/auth/me` | 🔒 当前用户 `UserVO`（含 `balance、points` 等） |
| POST | `/auth/logout` | 🔒 无状态登出（预留） |

### 3. 文件 `FileController`（`/file`）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/file/upload` | `multipart` 字段名 `file`；成功返回 `{ "url": "/uploads/..." }`（相对路径，需加 `/api` 前缀访问见 `FileController`） |

### 4. 探活/演示 `HelloController`（`/hello`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/hello/ping` | 匿名健康检查 |
| GET | `/hello/me` | 🔒 当前 `UserContext` |
| GET | `/hello/admin` | 👑 需管理员 |

### 5. 分类 `CategoryController`（`/categories`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/categories` | 分类树 `List<CategoryVO>`（匿名可访问） |

### 6. 公开商品 `ProductController`（`/products`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/products` | 在售列表；`categoryId, keyword, sort, pageNum, pageSize`；`sort` 支持 `latest`（默认）、`price_asc`、`price_desc`、`sales_desc`、`rating_desc` |
| GET | `/products/{id}/reviews` | 商品评价分页 |
| GET | `/products/{id}` | 商品详情（内部按登录态/角色处理展示） |

### 7. 购物车 `CartController`（`/cart`）🔒

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/cart` | 购物车聚合视图 `CartViewVO` |
| POST | `/cart` | 加购，body 含 `productId、quantity`；返回 `{ "id": cartItemId }` |
| PUT | `/cart/{id}` | 改数量/勾选，body `CartUpdateDTO` |
| DELETE | `/cart/{id}` | 删除一条 |
| POST | `/cart/select-all?selected=` | 全选/全不选 |
| POST | `/cart/clear-selected` | 清空已勾选 |

### 8. 买家订单 `OrderController`（`/orders`）🔒

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/orders/preview` | body `{"cartItemIds":[...]}`，返回结算预览 `Preview`（金额、可用积分等） |
| POST | `/orders` | 下单，body `CheckoutDTO`（`cartItemIds、pointsUsed、remark、meetPlace、meetTime`）；**仅允许同一商家**；成功返回 `{ "id": orderId }` |
| GET | `/orders` | 买家订单分页，可选 `status` |
| GET | `/orders/{id}` | 订单详情 |
| POST | `/orders/{id}/cancel` | 买家取消 |
| POST | `/orders/{id}/confirm-receive` | 确认收货 |
| POST | `/orders/{id}/return-apply` | 申请退货，body `ReturnApplyDTO`；成功 `{ "returnRecordId": ... }` |
| POST | `/orders/{id}/reviews/products` | 商品评价，body `SubmitProductReviewsDTO` |
| POST | `/orders/{id}/reviews/merchant-service` | 商家服务评价，body `MerchantServiceReviewDTO` |

### 9. 商家订单 `MerchantOrderController`（`/merchant/orders`）🔒 🏪

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/merchant/orders` | 本店订单列表 |
| GET | `/merchant/orders/{id}` | 本店订单详情 |
| POST | `/merchant/orders/{id}/ship` | 发货 |
| POST | `/merchant/orders/{id}/return-approve` | 同意退货 |
| POST | `/merchant/orders/{id}/return-reject` | 拒绝退货，body `ReturnRejectDTO` |
| POST | `/merchant/orders/{id}/reviews/buyer` | 评价买家，body `BuyerBehaviorReviewDTO` |

### 10. 商家商品 `MerchantProductController`（`/merchant/products`）🔒 🏪

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/merchant/products` | 本店商品列表，可选 `status` |
| POST | `/merchant/products` | 发布商品，body `ProductCreateDTO`；返回 `{ "id" }` |
| POST | `/merchant/products/{id}/off-shelf` | 下架 |
| POST | `/merchant/products/{id}/on-shelf` | 重新上架 |

### 11. 商家黑名单 `MerchantBlacklistController`（`/merchant/blacklist`）🔒 🏪

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/merchant/blacklist` | 分页列表 |
| POST | `/merchant/blacklist` | 添加，body `BlacklistAddDTO` |
| DELETE | `/merchant/blacklist/{id}` | 删除 |

### 12. 首页 `HomeController`（`/home`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/home/carousels` | 轮播图 `List<CarouselVO>`（匿名可访问；仅 `/home/**` 中当前实现有此项） |

### 13. 管理端 `AdminController`（`/admin`）👑

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/users` | 用户分页；`role、status`（默认 `PENDING`）、`keyword、pageNum、pageSize` |
| POST | `/admin/users/{id}/approve` | 审核通过 |
| POST | `/admin/users/{id}/reject` | 驳回，body 含 `reason`（`RejectUserDTO`） |
| GET | `/admin/users/{id}` | 用户详情 |
| PUT | `/admin/users/{id}` | 更新，body `AdminUserUpdateDTO` |
| DELETE | `/admin/users/{id}` | 软删除用户 |
| POST | `/admin/users/{id}/recharge` | 充值，body `AdminRechargeDTO` |
| POST | `/admin/users/{id}/merchant-level` | 调商家等级，body `AdminMerchantLevelDTO` |
| GET | `/admin/blacklist` | 全平台黑名单分页 |
| POST | `/admin/blacklist` | 添加全平台黑名单，body `BlacklistAddDTO` |
| DELETE | `/admin/blacklist/{id}` | 删除 |
| GET | `/admin/products` | 商品审核列表，默认 `status=PENDING` |
| POST | `/admin/products/{id}/approve` | 商品过审 |
| POST | `/admin/products/{id}/reject` | 驳回，body `RejectUserDTO`（复用 `reason`） |

### 14. 管理端轮播 `AdminCarouselController`（`/admin/carousels`）👑

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/carousels` | 分页 |
| POST | `/admin/carousels` | 新增，body `CarouselSaveDTO`；返回 `{ "id" }` |
| PUT | `/admin/carousels/{id}` | 更新 |
| DELETE | `/admin/carousels/{id}` | 删除 |

## 四、当前代码中**未**提供独立接口的能力（与早期设计/报告草案差异）

| 能力 | 说明 |
|------|------|
| `GET /home/recommend` | 无对应 Controller 方法。 |
| `GET /shops/{merchantId}` 等 | 无 `ShopController`；`WebMvcConfig` 白名单含 `/shops/*` 为预留。店铺信息可从前端用商品/订单数据组合展示。 |
| `GET /wallet/**`、`GET /points/**` 独立流水 | 无专门 WalletController；余额与积分在 `UserVO` 中通过 `/auth/me` 等返回，流水查询未单独暴露。 |
| 管理端 `ban/unban`、`/admin/platform-accounts`、`/admin/transactions`、`/admin/operation-logs` 等 | 无对应 `AdminController` 路由。封禁/解封如存在则见用户状态字段与 `User` 表及业务层。 |
| **Swagger / OpenAPI** | `pom.xml` 未引入 SpringDoc，无内置 Swagger UI。联调请用 Postman 或本文档。 |

## 五、核心接口速查（与代码一致）

| 方法 | 路径 | 作用 |
|------|------|------|
| GET | `/products` | 条件搜索在售商品 |
| POST | `/orders` | 单商家购物车结算下单 |
| POST | `/orders/{id}/confirm-receive` | 买家确认收货 |
| POST | `/orders/{id}/return-apply` | 申请退货 |
| POST | `/admin/users/{id}/approve` | 审核用户/商家通过 |
| POST | `/admin/users/{id}/recharge` | 管理员充值 |
| POST | `/admin/users/{id}/merchant-level` | 调整商家等级 |

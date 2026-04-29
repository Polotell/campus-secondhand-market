-- ============================================================
-- 校园二手交易平台  建表脚本
-- 数据库名：2023011308（组员学号）
-- 作者：项目组   日期：2026-03
-- MySQL 8.x   存储引擎 InnoDB   字符集 utf8mb4
-- ============================================================

DROP DATABASE IF EXISTS `2023011308`;
CREATE DATABASE `2023011308`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `2023011308`;

-- ------------------------------------------------------------
-- 1. 用户表 user  （普通用户 / 商家 / 管理员 共用）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`                BIGINT UNSIGNED  NOT NULL                COMMENT '雪花ID',
  `username`          VARCHAR(50)      NOT NULL                COMMENT '登录账号',
  `password`          VARCHAR(100)     NOT NULL                COMMENT '密码（BCrypt 加密）',
  `real_name`         VARCHAR(50)      NOT NULL                COMMENT '真实姓名',
  `phone`             VARCHAR(20)      NOT NULL                COMMENT '手机号',
  `email`             VARCHAR(100)     DEFAULT NULL            COMMENT '邮箱',
  `city`              VARCHAR(50)      DEFAULT NULL            COMMENT '所在城市',
  `gender`            VARCHAR(10)      DEFAULT NULL            COMMENT '性别 MALE/FEMALE/OTHER',
  `bank_account`      VARCHAR(16)      DEFAULT NULL            COMMENT '银行账号（16位数字）',
  `avatar`            VARCHAR(255)     DEFAULT NULL            COMMENT '头像路径',
  `role`              VARCHAR(20)      NOT NULL DEFAULT 'USER' COMMENT '角色 USER/MERCHANT/ADMIN',
  `status`            VARCHAR(20)      NOT NULL DEFAULT 'PENDING' COMMENT '审核状态 PENDING/APPROVED/REJECTED/BANNED',
  `reject_reason`     VARCHAR(255)     DEFAULT NULL            COMMENT '审核拒绝原因',
  `ban_until`         DATETIME         DEFAULT NULL            COMMENT '限时封禁截止时间；NULL=未封禁',
  -- 商家专属字段
  `shop_name`         VARCHAR(100)     DEFAULT NULL            COMMENT '店铺名（商家）',
  `business_license`  VARCHAR(255)     DEFAULT NULL            COMMENT '营业执照图片路径（商家）',
  `id_card_front`     VARCHAR(255)     DEFAULT NULL            COMMENT '身份证正面图片路径（商家）',
  `id_card_back`      VARCHAR(255)     DEFAULT NULL            COMMENT '身份证反面图片路径（商家）',
  `merchant_level`    TINYINT UNSIGNED DEFAULT NULL            COMMENT '商家等级 1~5；NULL=非商家',
  `good_rate`         DECIMAL(5,4)     DEFAULT NULL            COMMENT '商家好评率 0~1（从评价聚合）',
  `buyer_good_rate`   DECIMAL(5,4)     DEFAULT NULL            COMMENT '作为买家的好评率（商家评买家聚合）',
  -- 钱包与积分
  `balance`           DECIMAL(12,2)    NOT NULL DEFAULT 0.00   COMMENT '钱包余额',
  `points`            INT UNSIGNED     NOT NULL DEFAULT 0      COMMENT '积分余额',
  -- 通用字段
  `created_at`        DATETIME         NOT NULL                COMMENT '创建时间',
  `updated_at`        DATETIME         NOT NULL                COMMENT '更新时间',
  `deleted`           TINYINT(1)       NOT NULL DEFAULT 0      COMMENT '软删除 0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role_status` (`role`, `status`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表（普通用户/商家/管理员）';

-- ------------------------------------------------------------
-- 2. 商品分类表 product_category
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `product_category`;
CREATE TABLE `product_category` (
  `id`          BIGINT UNSIGNED  NOT NULL                COMMENT '雪花ID',
  `name`        VARCHAR(50)      NOT NULL                COMMENT '分类名',
  `parent_id`   BIGINT UNSIGNED  NOT NULL DEFAULT 0      COMMENT '父分类ID；0=一级',
  `sort`        INT              NOT NULL DEFAULT 0      COMMENT '排序',
  `icon`        VARCHAR(255)     DEFAULT NULL            COMMENT '分类图标',
  `created_at`  DATETIME         NOT NULL,
  `updated_at`  DATETIME         NOT NULL,
  `deleted`     TINYINT(1)       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ------------------------------------------------------------
-- 3. 商品表 product
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `id`              BIGINT UNSIGNED  NOT NULL                COMMENT '雪花ID',
  `merchant_id`     BIGINT UNSIGNED  NOT NULL                COMMENT '商家用户ID',
  `category_id`     BIGINT UNSIGNED  NOT NULL                COMMENT '分类ID',
  `name`            VARCHAR(100)     NOT NULL                COMMENT '商品名称',
  `description`     TEXT             DEFAULT NULL            COMMENT '使用说明/描述',
  `original_price`  DECIMAL(12,2)    NOT NULL                COMMENT '原价',
  `discount_price`  DECIMAL(12,2)    NOT NULL                COMMENT '折扣价（实际售价）',
  `size_info`       VARCHAR(100)     DEFAULT NULL            COMMENT '尺寸规格',
  `condition_level` VARCHAR(20)      NOT NULL                COMMENT '新旧程度 NEW/NINETY/EIGHTY/SEVENTY/OTHER',
  `stock`           INT UNSIGNED     NOT NULL DEFAULT 0      COMMENT '库存',
  `sales_count`     INT UNSIGNED     NOT NULL DEFAULT 0      COMMENT '历史销量（累计已完成订单数量）',
  `negotiable`      TINYINT(1)       NOT NULL DEFAULT 0      COMMENT '是否允许议价 0否 1是',
  `good_rate`       DECIMAL(5,4)     DEFAULT NULL            COMMENT '好评率（评价聚合）',
  `avg_rating`      DECIMAL(3,2)     DEFAULT NULL            COMMENT '平均星级 0~5',
  `status`          VARCHAR(20)      NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/ON_SALE/LOCKED/SOLD/OFF_SHELF/REJECTED',
  `reject_reason`   VARCHAR(255)     DEFAULT NULL            COMMENT '审核拒绝原因',
  `created_at`      DATETIME         NOT NULL,
  `updated_at`      DATETIME         NOT NULL,
  `deleted`         TINYINT(1)       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_merchant_status` (`merchant_id`, `status`),
  KEY `idx_category`        (`category_id`),
  KEY `idx_name`            (`name`),
  KEY `idx_status_sales`    (`status`, `sales_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ------------------------------------------------------------
-- 4. 商品图片表 product_image
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `product_image`;
CREATE TABLE `product_image` (
  `id`          BIGINT UNSIGNED  NOT NULL                COMMENT '雪花ID',
  `product_id`  BIGINT UNSIGNED  NOT NULL                COMMENT '商品ID',
  `url`         VARCHAR(255)     NOT NULL                COMMENT '图片相对路径',
  `sort`        INT              NOT NULL DEFAULT 0      COMMENT '排序（0为主图）',
  `created_at`  DATETIME         NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片表';

-- ------------------------------------------------------------
-- 5. 购物车表 cart_item
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `cart_item`;
CREATE TABLE `cart_item` (
  `id`          BIGINT UNSIGNED  NOT NULL                COMMENT '雪花ID',
  `user_id`     BIGINT UNSIGNED  NOT NULL                COMMENT '用户ID',
  `product_id`  BIGINT UNSIGNED  NOT NULL                COMMENT '商品ID',
  `quantity`    INT UNSIGNED     NOT NULL DEFAULT 1      COMMENT '数量',
  `selected`    TINYINT(1)       NOT NULL DEFAULT 1      COMMENT '前端是否勾选',
  `created_at`  DATETIME         NOT NULL,
  `updated_at`  DATETIME         NOT NULL,
  `deleted`     TINYINT(1)       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- ------------------------------------------------------------
-- 6. 订单主表 `order` （关键字，加反引号）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
  `id`                  BIGINT UNSIGNED  NOT NULL                COMMENT '雪花ID',
  `order_no`            VARCHAR(32)      NOT NULL                COMMENT '业务订单号（展示用）',
  `buyer_id`            BIGINT UNSIGNED  NOT NULL                COMMENT '买家ID',
  `merchant_id`         BIGINT UNSIGNED  NOT NULL                COMMENT '商家ID（一订单限一商家）',
  `total_amount`        DECIMAL(12,2)    NOT NULL                COMMENT '商品总金额（未抵扣前）',
  `points_used`         INT UNSIGNED     NOT NULL DEFAULT 0      COMMENT '使用的积分',
  `points_deduction`    DECIMAL(12,2)    NOT NULL DEFAULT 0.00   COMMENT '积分抵扣金额（100积分=1元）',
  `actual_amount`       DECIMAL(12,2)    NOT NULL                COMMENT '实际支付金额 = total - points_deduction',
  `platform_fee_rate`   DECIMAL(5,4)     NOT NULL                COMMENT '下单时商家等级对应费率快照',
  `platform_fee`        DECIMAL(12,2)    NOT NULL DEFAULT 0.00   COMMENT '平台手续费 = actual_amount * rate',
  `merchant_income`     DECIMAL(12,2)    NOT NULL DEFAULT 0.00   COMMENT '商家应得 = actual_amount - platform_fee',
  `status`              VARCHAR(30)      NOT NULL                COMMENT 'PAID/SHIPPED/RECEIVED/RETURN_APPLYING/RETURN_APPROVED/RETURN_REJECTED/RETURNED/COMPLETED/CANCELLED',
  `paid_at`             DATETIME         DEFAULT NULL            COMMENT '付款时间',
  `shipped_at`          DATETIME         DEFAULT NULL            COMMENT '发货时间',
  `received_at`         DATETIME         DEFAULT NULL            COMMENT '确认收货时间',
  `auto_confirm_at`     DATETIME         DEFAULT NULL            COMMENT '自动确认时间 = shipped_at + 7天',
  `return_deadline`     DATETIME         DEFAULT NULL            COMMENT '退货截止 = received_at + 24小时',
  `completed_at`        DATETIME         DEFAULT NULL            COMMENT '交易完成时间（结算给商家）',
  `cancelled_at`        DATETIME         DEFAULT NULL,
  `remark`              VARCHAR(255)     DEFAULT NULL,
  `meet_place`          VARCHAR(200)     DEFAULT NULL            COMMENT '线下交易约定地点',
  `meet_time`           VARCHAR(100)     DEFAULT NULL            COMMENT '线下交易约定时间（自由文本）',
  `created_at`          DATETIME         NOT NULL,
  `updated_at`          DATETIME         NOT NULL,
  `deleted`             TINYINT(1)       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no`             (`order_no`),
  KEY `idx_buyer_status`               (`buyer_id`, `status`),
  KEY `idx_merchant_status`            (`merchant_id`, `status`),
  KEY `idx_auto_confirm_at`            (`auto_confirm_at`),
  KEY `idx_status_created`             (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- ------------------------------------------------------------
-- 7. 订单明细表 order_item （含商品快照，防止商品后续改价/下架影响订单）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
  `id`              BIGINT UNSIGNED  NOT NULL                COMMENT '雪花ID',
  `order_id`        BIGINT UNSIGNED  NOT NULL                COMMENT '订单ID',
  `product_id`      BIGINT UNSIGNED  NOT NULL                COMMENT '商品ID',
  `product_name`    VARCHAR(100)     NOT NULL                COMMENT '快照：商品名',
  `product_image`   VARCHAR(255)     DEFAULT NULL            COMMENT '快照：主图',
  `unit_price`      DECIMAL(12,2)    NOT NULL                COMMENT '快照：单价（折扣价）',
  `quantity`        INT UNSIGNED     NOT NULL                COMMENT '数量',
  `subtotal`        DECIMAL(12,2)    NOT NULL                COMMENT '小计 = unit_price * quantity',
  `reviewed`        TINYINT(1)       NOT NULL DEFAULT 0      COMMENT '是否已评价',
  `created_at`      DATETIME         NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order`   (`order_id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品明细表';

-- ------------------------------------------------------------
-- 8. 退货申请表 return_record
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `return_record`;
CREATE TABLE `return_record` (
  `id`              BIGINT UNSIGNED  NOT NULL                COMMENT '雪花ID',
  `order_id`        BIGINT UNSIGNED  NOT NULL                COMMENT '订单ID',
  `buyer_id`        BIGINT UNSIGNED  NOT NULL                COMMENT '买家ID',
  `merchant_id`     BIGINT UNSIGNED  NOT NULL                COMMENT '商家ID',
  `reason`          VARCHAR(500)     NOT NULL                COMMENT '退货原因',
  `images`          TEXT             DEFAULT NULL            COMMENT '凭证图（JSON数组）',
  `apply_time`      DATETIME         NOT NULL                COMMENT '申请时间（必须 <= order.return_deadline）',
  `audit_status`    VARCHAR(20)      NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  `audit_remark`    VARCHAR(500)     DEFAULT NULL,
  `audit_time`      DATETIME         DEFAULT NULL,
  `created_at`      DATETIME         NOT NULL,
  `updated_at`      DATETIME         NOT NULL,
  `deleted`         TINYINT(1)       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order` (`order_id`, `deleted`),
  KEY `idx_merchant_status` (`merchant_id`, `audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退货申请表';

-- ------------------------------------------------------------
-- 9. 商品评价表 product_review
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `product_review`;
CREATE TABLE `product_review` (
  `id`              BIGINT UNSIGNED  NOT NULL                COMMENT '雪花ID',
  `order_id`        BIGINT UNSIGNED  NOT NULL,
  `order_item_id`   BIGINT UNSIGNED  NOT NULL,
  `product_id`      BIGINT UNSIGNED  NOT NULL,
  `buyer_id`        BIGINT UNSIGNED  NOT NULL,
  `merchant_id`     BIGINT UNSIGNED  NOT NULL,
  `rating`          TINYINT UNSIGNED NOT NULL                COMMENT '1~5 星',
  `content`         VARCHAR(1000)    DEFAULT NULL,
  `images`          TEXT             DEFAULT NULL            COMMENT '评价图（JSON数组）',
  `created_at`      DATETIME         NOT NULL,
  `deleted`         TINYINT(1)       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_item` (`order_item_id`, `deleted`),
  KEY `idx_product` (`product_id`),
  KEY `idx_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';

-- ------------------------------------------------------------
-- 10. 商家服务态度评价 merchant_review
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `merchant_review`;
CREATE TABLE `merchant_review` (
  `id`          BIGINT UNSIGNED  NOT NULL,
  `order_id`    BIGINT UNSIGNED  NOT NULL,
  `buyer_id`    BIGINT UNSIGNED  NOT NULL,
  `merchant_id` BIGINT UNSIGNED  NOT NULL,
  `rating`      TINYINT UNSIGNED NOT NULL,
  `content`     VARCHAR(500)     DEFAULT NULL,
  `created_at`  DATETIME         NOT NULL,
  `deleted`     TINYINT(1)       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order` (`order_id`, `deleted`),
  KEY `idx_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家服务态度评价';

-- ------------------------------------------------------------
-- 11. 买家评价表（商家评买家）buyer_review
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `buyer_review`;
CREATE TABLE `buyer_review` (
  `id`          BIGINT UNSIGNED  NOT NULL,
  `order_id`    BIGINT UNSIGNED  NOT NULL,
  `merchant_id` BIGINT UNSIGNED  NOT NULL,
  `buyer_id`    BIGINT UNSIGNED  NOT NULL,
  `rating`      TINYINT UNSIGNED NOT NULL                COMMENT '1~5 星',
  `content`     VARCHAR(500)     DEFAULT NULL,
  `created_at`  DATETIME         NOT NULL,
  `deleted`     TINYINT(1)       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order` (`order_id`, `deleted`),
  KEY `idx_buyer` (`buyer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家对买家的评价（影响买家好评率）';

-- ------------------------------------------------------------
-- 12. 钱包流水表 wallet_transaction
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `wallet_transaction`;
CREATE TABLE `wallet_transaction` (
  `id`                BIGINT UNSIGNED  NOT NULL                COMMENT '雪花ID',
  `user_id`           BIGINT UNSIGNED  NOT NULL                COMMENT '用户ID',
  `type`              VARCHAR(30)      NOT NULL                COMMENT 'RECHARGE/PAY/REFUND/INCOME/PLATFORM_FEE/ESCROW_IN/ESCROW_OUT',
  `amount`            DECIMAL(12,2)    NOT NULL                COMMENT '金额（正数表示增加，负数表示减少）',
  `balance_after`     DECIMAL(12,2)    NOT NULL                COMMENT '变动后余额（对账用）',
  `related_order_id`  BIGINT UNSIGNED  DEFAULT NULL            COMMENT '关联订单ID',
  `remark`            VARCHAR(255)     DEFAULT NULL,
  `operator_id`       BIGINT UNSIGNED  DEFAULT NULL            COMMENT '操作人（管理员充值时记录）',
  `created_at`        DATETIME         NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`, `created_at`),
  KEY `idx_related_order` (`related_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包流水表';

-- ------------------------------------------------------------
-- 13. 积分流水表 points_transaction
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `points_transaction`;
CREATE TABLE `points_transaction` (
  `id`                BIGINT UNSIGNED  NOT NULL,
  `user_id`           BIGINT UNSIGNED  NOT NULL,
  `type`              VARCHAR(20)      NOT NULL                COMMENT 'EARN/USE/REFUND',
  `points`            INT              NOT NULL                COMMENT '积分变动（正/负）',
  `balance_after`     INT UNSIGNED     NOT NULL,
  `related_order_id`  BIGINT UNSIGNED  DEFAULT NULL,
  `remark`            VARCHAR(255)     DEFAULT NULL,
  `created_at`        DATETIME         NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水表';

-- ------------------------------------------------------------
-- 14. 平台中间账户 platform_account （只两行：ESCROW 托管 + FEE 手续费收入）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `platform_account`;
CREATE TABLE `platform_account` (
  `id`          BIGINT UNSIGNED  NOT NULL,
  `type`        VARCHAR(20)      NOT NULL                COMMENT 'ESCROW 中间托管 / FEE 手续费收入',
  `balance`     DECIMAL(14,2)    NOT NULL DEFAULT 0.00   COMMENT '当前余额',
  `updated_at`  DATETIME         NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台中间账户（托管 + 手续费）';

-- ------------------------------------------------------------
-- 15. 买家黑名单 user_blacklist
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user_blacklist`;
CREATE TABLE `user_blacklist` (
  `id`          BIGINT UNSIGNED  NOT NULL,
  `user_id`     BIGINT UNSIGNED  NOT NULL                COMMENT '被拉黑买家ID',
  `merchant_id` BIGINT UNSIGNED  DEFAULT NULL            COMMENT '拉黑商家ID；NULL=平台级拉黑',
  `reason`      VARCHAR(500)     DEFAULT NULL,
  `operator_id` BIGINT UNSIGNED  NOT NULL                COMMENT '操作人（商家或管理员）',
  `created_at`  DATETIME         NOT NULL,
  `deleted`     TINYINT(1)       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user`       (`user_id`),
  KEY `idx_merchant`   (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='买家黑名单（商家级/平台级）';

-- ------------------------------------------------------------
-- 16. 首页轮播图 carousel （可选功能）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `carousel`;
CREATE TABLE `carousel` (
  `id`          BIGINT UNSIGNED  NOT NULL,
  `image_url`   VARCHAR(255)     NOT NULL,
  `link_url`    VARCHAR(255)     DEFAULT NULL            COMMENT '跳转链接',
  `sort`        INT              NOT NULL DEFAULT 0,
  `status`      VARCHAR(20)      NOT NULL DEFAULT 'ON'   COMMENT 'ON/OFF',
  `created_at`  DATETIME         NOT NULL,
  `updated_at`  DATETIME         NOT NULL,
  `deleted`     TINYINT(1)       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页轮播图';

-- ------------------------------------------------------------
-- 17. 操作日志表 operation_log  （AOP 切面记录，答辩展示用）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `id`            BIGINT UNSIGNED  NOT NULL,
  `user_id`       BIGINT UNSIGNED  DEFAULT NULL,
  `username`      VARCHAR(50)      DEFAULT NULL,
  `module`        VARCHAR(50)      DEFAULT NULL          COMMENT '业务模块',
  `operation`     VARCHAR(100)     DEFAULT NULL          COMMENT '操作描述',
  `request_uri`   VARCHAR(255)     DEFAULT NULL,
  `request_method` VARCHAR(10)     DEFAULT NULL,
  `params`        TEXT             DEFAULT NULL,
  `ip`            VARCHAR(50)      DEFAULT NULL,
  `duration_ms`   INT              DEFAULT NULL          COMMENT '耗时毫秒（AOP 环绕通知记录）',
  `status`        VARCHAR(10)      DEFAULT NULL          COMMENT 'SUCCESS/FAIL',
  `error_msg`     TEXT             DEFAULT NULL,
  `created_at`    DATETIME         NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志（AOP 切面写入）';

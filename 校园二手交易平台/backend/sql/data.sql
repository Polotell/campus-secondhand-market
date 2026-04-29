-- ============================================================
-- 校园二手交易平台  初始化数据脚本
-- 运行前提：已执行 schema.sql
-- 依赖数据库：2023011308
-- ============================================================

USE `2023011308`;

-- ------------------------------------------------------------
-- 1. 超级管理员账号
--   用户名：admin   密码：admin123
--   注意：以下密码密文是占位符（不是 admin123 的真实哈希）。
--   项目启动时 DataInitializer (CommandLineRunner) 会用 BCrypt.hashpw("admin123")
--   自动将 admin / student01 / student02 / merchant01 四个账号的密码重置为 admin123，
--   确保无论 seed 里写的哈希值是什么都能正确登录。
-- ------------------------------------------------------------
INSERT INTO `user`
(`id`, `username`, `password`, `real_name`, `phone`, `email`, `city`, `gender`,
 `role`, `status`, `balance`, `points`, `created_at`, `updated_at`, `deleted`)
VALUES
(1, 'admin',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
 '系统管理员', '13800000000', 'admin@campus.edu', '北京', 'OTHER',
 'ADMIN', 'APPROVED', 0.00, 0, NOW(), NOW(), 0);

-- ------------------------------------------------------------
-- 2. 商品分类（一级 + 二级）
-- ------------------------------------------------------------
INSERT INTO `product_category` (`id`, `name`, `parent_id`, `sort`, `created_at`, `updated_at`, `deleted`) VALUES
-- 一级分类
(100, '教材书籍',  0, 1, NOW(), NOW(), 0),
(200, '数码电子',  0, 2, NOW(), NOW(), 0),
(300, '生活用品',  0, 3, NOW(), NOW(), 0),
(400, '服装鞋帽',  0, 4, NOW(), NOW(), 0),
(500, '运动户外',  0, 5, NOW(), NOW(), 0),
(600, '乐器文玩',  0, 6, NOW(), NOW(), 0),
-- 二级分类
(101, '专业教材',  100, 1, NOW(), NOW(), 0),
(102, '考研资料',  100, 2, NOW(), NOW(), 0),
(103, '课外读物',  100, 3, NOW(), NOW(), 0),
(201, '笔记本电脑', 200, 1, NOW(), NOW(), 0),
(202, '手机',      200, 2, NOW(), NOW(), 0),
(203, '耳机音箱',  200, 3, NOW(), NOW(), 0),
(204, '摄影设备',  200, 4, NOW(), NOW(), 0),
(301, '宿舍家电',  300, 1, NOW(), NOW(), 0),
(302, '日用杂货',  300, 2, NOW(), NOW(), 0),
(401, '男装',      400, 1, NOW(), NOW(), 0),
(402, '女装',      400, 2, NOW(), NOW(), 0),
(403, '鞋子',      400, 3, NOW(), NOW(), 0),
(501, '健身器材',  500, 1, NOW(), NOW(), 0),
(502, '球类',      500, 2, NOW(), NOW(), 0);

-- ------------------------------------------------------------
-- 3. 平台中间账户（初始化两条记录）
-- ------------------------------------------------------------
INSERT INTO `platform_account` (`id`, `type`, `balance`, `updated_at`) VALUES
(1, 'ESCROW', 0.00, NOW()),   -- 买家货款托管账户
(2, 'FEE',    0.00, NOW());   -- 平台手续费收入账户

-- ------------------------------------------------------------
-- 4. 测试用普通用户（可选，用于联调）
-- 密码同为 admin123（BCrypt）
-- ------------------------------------------------------------
INSERT INTO `user`
(`id`, `username`, `password`, `real_name`, `phone`, `email`, `city`, `gender`,
 `bank_account`, `role`, `status`, `balance`, `points`, `created_at`, `updated_at`, `deleted`)
VALUES
(10, 'student01',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
 '张三', '13811110001', 'zhang3@student.edu', '南京', 'MALE',
 '6222000000000001', 'USER', 'APPROVED', 500.00, 0, NOW(), NOW(), 0),

(11, 'student02',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
 '李四', '13811110002', 'li4@student.edu', '南京', 'FEMALE',
 '6222000000000002', 'USER', 'APPROVED', 500.00, 0, NOW(), NOW(), 0);

-- ------------------------------------------------------------
-- 5. 测试用商家
-- ------------------------------------------------------------
INSERT INTO `user`
(`id`, `username`, `password`, `real_name`, `phone`, `email`, `city`, `gender`,
 `bank_account`, `role`, `status`, `shop_name`, `business_license`,
 `id_card_front`, `id_card_back`, `merchant_level`,
 `balance`, `points`, `created_at`, `updated_at`, `deleted`)
VALUES
(20, 'merchant01',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
 '王老板', '13822220001', 'wang@shop.com', '南京', 'MALE',
 '6222000000000010', 'MERCHANT', 'APPROVED', '小王二手数码店',
 '/uploads/license/sample.jpg', '/uploads/idcard/sample_front.jpg', '/uploads/idcard/sample_back.jpg',
 2, 0.00, 0, NOW(), NOW(), 0);

-- ------------------------------------------------------------
-- 首页轮播（可选演示数据，图片为外网占位图）
-- ------------------------------------------------------------
INSERT INTO `carousel`
(`id`, `image_url`, `link_url`, `sort`, `status`, `created_at`, `updated_at`, `deleted`)
VALUES
(1, 'https://picsum.photos/seed/campus1/1200/360', '/home', 1, 'ON', NOW(), NOW(), 0),
(2, 'https://picsum.photos/seed/campus2/1200/360', NULL, 2, 'ON', NOW(), NOW(), 0);

-- ============================================================
-- 商家等级费率对照表（无需建表，固化在后端常量 MerchantFeeConfig）
-- 等级 1 -> 0.1%   (0.0010)
-- 等级 2 -> 0.2%   (0.0020)
-- 等级 3 -> 0.5%   (0.0050)
-- 等级 4 -> 0.75%  (0.0075)
-- 等级 5 -> 1%     (0.0100)
-- ============================================================

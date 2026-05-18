-- ============================================================
-- 校园二手交易平台  初始化数据脚本
-- 运行前提：已执行 schema.sql
-- 依赖数据库：2023011308
-- 导入时请指定客户端字符集（否则会 GBK/系统编码误判导致中文乱码）：
--   mysql --default-character-set=utf8mb4 -u root -p 2023011308 < data.sql
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
 'admin', '13800000000', 'admin@campus.edu', 'Beijing', 'OTHER',
 'ADMIN', 'APPROVED', 0.00, 0, NOW(), NOW(), 0);

-- ------------------------------------------------------------
-- 2. 商品分类（一级 + 二级）
-- ------------------------------------------------------------
INSERT INTO `product_category` (`id`, `name`, `parent_id`, `sort`, `created_at`, `updated_at`, `deleted`) VALUES
-- 一级分类（首页侧边栏展示为中文）
(100, '图书文娱',  0, 1, NOW(), NOW(), 0),
(200, '数码电子',  0, 2, NOW(), NOW(), 0),
(300, '日用百货',  0, 3, NOW(), NOW(), 0),
(400, '服饰鞋包',  0, 4, NOW(), NOW(), 0),
(500, '运动户外',  0, 5, NOW(), NOW(), 0),
(600, '乐器音像',  0, 6, NOW(), NOW(), 0),
-- 二级分类
(101, '专业课教材',  100, 1, NOW(), NOW(), 0),
(102, '考研考公',  100, 2, NOW(), NOW(), 0),
(103, '课外读物',  100, 3, NOW(), NOW(), 0),
(201, '笔记本电脑', 200, 1, NOW(), NOW(), 0),
(202, '手机平板',      200, 2, NOW(), NOW(), 0),
(203, '耳机音响',  200, 3, NOW(), NOW(), 0),
(204, '相机摄影',  200, 4, NOW(), NOW(), 0),
(301, '宿舍电器',  300, 1, NOW(), NOW(), 0),
(302, '洗漱日用',  300, 2, NOW(), NOW(), 0),
(401, '男装',      400, 1, NOW(), NOW(), 0),
(402, '女装',      400, 2, NOW(), NOW(), 0),
(403, '鞋靴',      400, 3, NOW(), NOW(), 0),
(501, '健身器材',  500, 1, NOW(), NOW(), 0),
(502, '球类运动',      500, 2, NOW(), NOW(), 0);

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
 'ZhangSan', '13811110001', 'zhang3@student.edu', 'Nanjing', 'MALE',
 '6222000000000001', 'USER', 'APPROVED', 500.00, 0, NOW(), NOW(), 0),

(11, 'student02',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
 'LiSi', '13811110002', 'li4@student.edu', 'Nanjing', 'FEMALE',
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
 'WangBoss', '13822220001', 'wang@shop.com', 'Nanjing', 'MALE',
 '6222000000000010', 'MERCHANT', 'APPROVED', '老王数码杂货铺',
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

-- ------------------------------------------------------------
-- 6. 演示商品（商家 merchant01 id=20，已在售 ON_SALE，主图为外网占位图）
--    重新导入本脚本前若已有订单引用请先清空订单相关表或跳过本节。
-- ------------------------------------------------------------
INSERT INTO `product`
(`id`, `merchant_id`, `category_id`, `name`, `description`, `original_price`, `discount_price`,
 `size_info`, `condition_level`, `stock`, `sales_count`, `negotiable`, `good_rate`, `avg_rating`,
 `status`, `reject_reason`, `created_at`, `updated_at`, `deleted`)
VALUES
(5001, 20, 101, '高等数学同济第七版（上下册）', '期末考完佛系出，少许笔记划线，无缺页。', 56.00, 28.00, NULL, 'EIGHTY', 8, 42, 0, 0.9600, 4.80, 'ON_SALE', NULL, NOW(), NOW(), 0),
(5002, 20, 102, '考研英语黄皮书历年真题', '2024版，真题只做了一年，解析全新。', 128.00, 65.00, NULL, 'NINETY', 5, 18, 0, 0.9200, 4.60, 'ON_SALE', NULL, NOW(), NOW(), 0),
(5003, 20, 103, '《人类简史》精装', '当当购入，读过一遍，书脊轻微磨损。', 68.00, 22.00, NULL, 'NINETY', 12, 7, 1, 0.9800, 4.90, 'ON_SALE', NULL, NOW(), NOW(), 0),
(5004, 20, 201, 'MacBook Air M1 8+256', '自用一年半，办公码字为主，电池健康92%，箱说齐全。', 7999.00, 4299.00, NULL, 'NINETY', 1, 9, 0, 0.9900, 4.90, 'ON_SALE', NULL, NOW(), NOW(), 0),
(5005, 20, 202, 'iPhone 13 128GB 午夜色', '贴膜戴壳使用，边框两处细小划痕，功能正常。', 4799.00, 2699.00, NULL, 'EIGHTY', 2, 33, 0, 0.9400, 4.70, 'ON_SALE', NULL, NOW(), NOW(), 0),
(5006, 20, 203, 'AirPods Pro 二代（USB-C）', '降噪通勤够用，右耳续航略弱于左耳，已消毒清洁。', 1899.00, 999.00, NULL, 'SEVENTY', 3, 11, 0, 0.8800, 4.40, 'ON_SALE', NULL, NOW(), NOW(), 0),
(5007, 20, 204, '佳能单反入门练手机身（单机）', '练手成色，快门次数适中，CMOS无尘，附背带充电器。', 3200.00, 1380.00, NULL, 'OTHER', 1, 4, 0, NULL, NULL, 'ON_SALE', NULL, NOW(), NOW(), 0),
(5008, 20, 301, '小米电饭煲 3L', '宿舍限电不适用转出，功能完好内胆无痕。', 299.00, 129.00, NULL, 'NINETY', 4, 21, 0, 0.9100, 4.55, 'ON_SALE', NULL, NOW(), NOW(), 0),
(5009, 20, 302, '宜家台灯 + 公牛插排套装', '毕业离校整套出，正常使用痕迹。', 159.00, 49.00, NULL, 'OTHER', 6, 5, 1, 0.8500, 4.30, 'ON_SALE', NULL, NOW(), NOW(), 0),
(5010, 20, 401, '优衣库羽绒服男 L', '干洗过一次，保暖够用，冬季离校清仓。', 599.00, 220.00, '175/L', 'EIGHTY', 3, 6, 0, NULL, NULL, 'ON_SALE', NULL, NOW(), NOW(), 0),
(5011, 20, 402, '针织开衫外套女 M', '浅灰色百搭款，九成新无起球。', 259.00, 79.00, '160/M', 'NINETY', 5, 14, 0, 0.9300, 4.65, 'ON_SALE', NULL, NOW(), NOW(), 0),
(5012, 20, 403, '耐克跑步鞋 42 码', '夜跑款，鞋底磨损正常，鞋面干净。', 699.00, 199.00, '42', 'SEVENTY', 2, 27, 0, 0.9000, 4.50, 'ON_SALE', NULL, NOW(), NOW(), 0),
(5013, 20, 501, '可调节哑铃一对（共20kg）', '宿舍健身用，毕业转让，送瑜伽垫。', 399.00, 168.00, NULL, 'EIGHTY', 2, 8, 0, NULL, NULL, 'ON_SALE', NULL, NOW(), NOW(), 0),
(5014, 20, 502, '斯伯丁篮球（室内外通用）', '院队训练剩一个，手感正常略磨。', 219.00, 85.00, '7号', 'OTHER', 4, 16, 0, 0.8700, 4.35, 'ON_SALE', NULL, NOW(), NOW(), 0),
(5015, 20, 201, '联想小新轻薄本 i5+16G', '日常上网课写论文，换固态后开机飞快。', 4599.00, 1899.00, NULL, 'SEVENTY', 1, 31, 0, 0.9500, 4.75, 'ON_SALE', NULL, NOW(), NOW(), 0);

INSERT INTO `product_image` (`id`, `product_id`, `url`, `sort`, `created_at`) VALUES
(60001, 5001, 'https://picsum.photos/seed/cm5001/800/800', 0, NOW()),
(60002, 5002, 'https://picsum.photos/seed/cm5002/800/800', 0, NOW()),
(60003, 5003, 'https://picsum.photos/seed/cm5003/800/800', 0, NOW()),
(60004, 5004, 'https://picsum.photos/seed/cm5004/800/800', 0, NOW()),
(60005, 5005, 'https://picsum.photos/seed/cm5005/800/800', 0, NOW()),
(60006, 5006, 'https://picsum.photos/seed/cm5006/800/800', 0, NOW()),
(60007, 5007, 'https://picsum.photos/seed/cm5007/800/800', 0, NOW()),
(60008, 5008, 'https://picsum.photos/seed/cm5008/800/800', 0, NOW()),
(60009, 5009, 'https://picsum.photos/seed/cm5009/800/800', 0, NOW()),
(60010, 5010, 'https://picsum.photos/seed/cm5010/800/800', 0, NOW()),
(60011, 5011, 'https://picsum.photos/seed/cm5011/800/800', 0, NOW()),
(60012, 5012, 'https://picsum.photos/seed/cm5012/800/800', 0, NOW()),
(60013, 5013, 'https://picsum.photos/seed/cm5013/800/800', 0, NOW()),
(60014, 5014, 'https://picsum.photos/seed/cm5014/800/800', 0, NOW()),
(60015, 5015, 'https://picsum.photos/seed/cm5015/800/800', 0, NOW());

-- ============================================================
-- 商家等级费率对照表（无需建表，固化在后端常量 MerchantFeeConfig）
-- 等级 1 -> 0.1%   (0.0010)
-- 等级 2 -> 0.2%   (0.0020)
-- 等级 3 -> 0.5%   (0.0050)
-- 等级 4 -> 0.75%  (0.0075)
-- 等级 5 -> 1%     (0.0100)
-- ============================================================

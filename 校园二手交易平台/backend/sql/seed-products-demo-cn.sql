-- ============================================================
-- 增量脚本：中文分类名 + 演示商品（已有库可直接执行）
-- 导入：mysql --default-character-set=utf8mb4 -u root -p 2023011308 < seed-products-demo-cn.sql
-- 数据库：2023011308
-- 说明：会删除 id 5001~5015 的旧演示商品及其图片后重建；
--       并把所有预设分类名更新为中文（与新版 data.sql 一致）。
-- ============================================================

USE `2023011308`;

DELETE FROM `product_image` WHERE `product_id` BETWEEN 5001 AND 5015;
DELETE FROM `product` WHERE `id` BETWEEN 5001 AND 5015;

UPDATE `product_category` SET `name` = '图书文娱' WHERE `id` = 100;
UPDATE `product_category` SET `name` = '数码电子' WHERE `id` = 200;
UPDATE `product_category` SET `name` = '日用百货' WHERE `id` = 300;
UPDATE `product_category` SET `name` = '服饰鞋包' WHERE `id` = 400;
UPDATE `product_category` SET `name` = '运动户外' WHERE `id` = 500;
UPDATE `product_category` SET `name` = '乐器音像' WHERE `id` = 600;
UPDATE `product_category` SET `name` = '专业课教材' WHERE `id` = 101;
UPDATE `product_category` SET `name` = '考研考公' WHERE `id` = 102;
UPDATE `product_category` SET `name` = '课外读物' WHERE `id` = 103;
UPDATE `product_category` SET `name` = '笔记本电脑' WHERE `id` = 201;
UPDATE `product_category` SET `name` = '手机平板' WHERE `id` = 202;
UPDATE `product_category` SET `name` = '耳机音响' WHERE `id` = 203;
UPDATE `product_category` SET `name` = '相机摄影' WHERE `id` = 204;
UPDATE `product_category` SET `name` = '宿舍电器' WHERE `id` = 301;
UPDATE `product_category` SET `name` = '洗漱日用' WHERE `id` = 302;
UPDATE `product_category` SET `name` = '男装' WHERE `id` = 401;
UPDATE `product_category` SET `name` = '女装' WHERE `id` = 402;
UPDATE `product_category` SET `name` = '鞋靴' WHERE `id` = 403;
UPDATE `product_category` SET `name` = '健身器材' WHERE `id` = 501;
UPDATE `product_category` SET `name` = '球类运动' WHERE `id` = 502;

UPDATE `user` SET `shop_name` = '老王数码杂货铺' WHERE `id` = 20;

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

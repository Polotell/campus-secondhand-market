-- 已有库升级：放宽轮播图 URL 长度（外链 / 带参数地址可能超过 255）
-- 用法：mysql -u用户名 -p 你的数据库名 < migrate-carousel-url-length.sql
-- 若库名不是 2023011308，请先修改下一行或删掉 USE 后在命令行指定数据库。

USE `2023011308`;

ALTER TABLE `carousel`
  MODIFY COLUMN `image_url` VARCHAR(2000) NOT NULL COMMENT '图片地址',
  MODIFY COLUMN `link_url`  VARCHAR(2000) DEFAULT NULL COMMENT '跳转链接';

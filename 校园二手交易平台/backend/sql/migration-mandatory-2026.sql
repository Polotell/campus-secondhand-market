-- 在必选功能补全脚本：若库已存在，请手工执行一次本文件（或整库重建）。
USE `2023011308`;

ALTER TABLE `order`
  ADD COLUMN `meet_place` VARCHAR(200) DEFAULT NULL COMMENT '线下交易约定地点' AFTER `remark`,
  ADD COLUMN `meet_time`   VARCHAR(100) DEFAULT NULL COMMENT '线下交易约定时间（自由文本）' AFTER `meet_place`;

-- ============================================
-- 数字健康助手系统 - 数据库初始化脚本
-- 版本: 1.0.0
-- 日期: 2026-04-25
-- 说明: 完整的数据库表结构，包含用户认证、手机使用记录、目标管理、日常活动等功能模块
-- ============================================

-- 设置字符集和排序规则
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 第1部分: 创建数据库
-- ============================================
CREATE DATABASE IF NOT EXISTS `iterate`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  COMMENT '数字健康助手数据库';

USE `iterate`;

-- ============================================
-- 第2部分: 用户认证模块
-- ============================================

-- ----------------------------
-- Table structure: user (用户表)
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(255) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码(加密存储)',
  `role` VARCHAR(50) NOT NULL DEFAULT 'USER' COMMENT '角色: ADMIN-管理员, USER-普通用户',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 插入默认管理员账户 (用户名: admin, 密码: admin123)
INSERT INTO `user` (`username`, `password`, `role`) VALUES
('admin', 'admin123', 'ADMIN');

-- 插入普通测试用户 (用户名: test, 密码: test123)
INSERT INTO `user` (`username`, `password`, `role`) VALUES
('test', 'test123', 'USER');

-- ============================================
-- 第3部分: 手机使用情况模块
-- ----------------------------

-- ----------------------------
-- Table structure: app_preset (常用App预设表)
-- ----------------------------
DROP TABLE IF EXISTS `app_preset`;
CREATE TABLE `app_preset` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '预设ID',
  `app_name` VARCHAR(255) NOT NULL COMMENT 'App名称',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序(数字越小越靠前)',
  `icon` VARCHAR(255) COMMENT 'App图标URL或图标名称',
  `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_name` (`app_name`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='常用App预设表(用于快速录入手机使用情况)';

-- 初始化常用App预设数据
INSERT INTO `app_preset` (`app_name`, `sort_order`, `is_active`) VALUES
('微信', 1, 1),
('抖音', 2, 1),
('快手', 3, 1),
('QQ', 4, 1),
('微博', 5, 1),
('哔哩哔哩', 6, 1),
('小红书', 7, 1),
('淘宝', 8, 1),
('京东', 9, 1),
('支付宝', 10, 1),
('王者荣耀', 11, 1),
('和平精英', 12, 1),
('原神', 13, 1),
('网易云音乐', 14, 1),
('QQ音乐', 15, 1),
('今日头条', 16, 1),
('知乎', 17, 1),
('豆瓣', 18, 1),
('拼多多', 19, 1),
('美团', 20, 1),
('饿了么', 21, 1),
('其他', 99, 1);

-- ----------------------------
-- Table structure: phone_usage (手机使用记录表)
-- ----------------------------
DROP TABLE IF EXISTS `phone_usage`;
CREATE TABLE `phone_usage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID(关联user表)',
  `date` DATE NOT NULL COMMENT '日期',
  `usage_time` BIGINT NOT NULL DEFAULT 0 COMMENT '使用总时长(单位:分钟)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `date`),
  KEY `idx_date` (`date`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='手机使用记录表(每日汇总)';

-- ----------------------------
-- Table structure: app_usage_detail (App使用明细表)
-- ----------------------------
DROP TABLE IF EXISTS `app_usage_detail`;
CREATE TABLE `app_usage_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `phone_usage_id` BIGINT NOT NULL COMMENT '关联的手机使用记录ID',
  `app_name` VARCHAR(255) NOT NULL COMMENT 'App名称',
  `app_preset_id` BIGINT DEFAULT NULL COMMENT '关联的预设App ID(可选)',
  `usage_time` BIGINT NOT NULL DEFAULT 0 COMMENT '使用时长(单位:分钟)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_phone_usage_id` (`phone_usage_id`),
  KEY `idx_app_preset_id` (`app_preset_id`),
  KEY `idx_app_name` (`app_name`),
  CONSTRAINT `fk_app_usage_detail_phone_usage` FOREIGN KEY (`phone_usage_id`)
    REFERENCES `phone_usage` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_app_usage_detail_app_preset` FOREIGN KEY (`app_preset_id`)
    REFERENCES `app_preset` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='App使用明细表(记录每个App的详细使用时长)';

-- ============================================
-- 第4部分: 目标管理模块
-- ----------------------------

-- ----------------------------
-- Table structure: goal (目标表)
-- ----------------------------
DROP TABLE IF EXISTS `goal`;
CREATE TABLE `goal` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '目标ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID(关联user表)',
  `goal` VARCHAR(255) NOT NULL COMMENT '目标内容',
  `description` TEXT COMMENT '目标详细描述(可选)',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间/截止时间',
  `status` VARCHAR(20) DEFAULT 'IN_PROGRESS' COMMENT '状态: IN_PROGRESS-进行中, COMPLETED-已完成, CANCELLED-已取消',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_end_time` (`end_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='目标表';

-- ----------------------------
-- Table structure: daily_subtask (每日子任务表)
-- ----------------------------
DROP TABLE IF EXISTS `daily_subtask`;
CREATE TABLE `daily_subtask` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '子任务ID',
  `goal_id` BIGINT NOT NULL COMMENT '关联的目标ID',
  `task_content` VARCHAR(255) NOT NULL COMMENT '任务内容(例如: 背50个单词)',
  `target_date` DATE NOT NULL COMMENT '计划完成日期',
  `is_completed` TINYINT(1) DEFAULT 0 COMMENT '是否完成打卡: 0-未完成, 1-已完成',
  `completion_date` DATE COMMENT '实际完成日期',
  `completion_note` VARCHAR(500) COMMENT '完成备注/心得(可选)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_goal_id` (`goal_id`),
  KEY `idx_target_date` (`target_date`),
  KEY `idx_is_completed` (`is_completed`),
  KEY `idx_completion_date` (`completion_date`),
  CONSTRAINT `fk_daily_subtask_goal` FOREIGN KEY (`goal_id`)
    REFERENCES `goal` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日子任务表(目标下的每日打卡任务)';

-- ============================================
-- 第5部分: 日常活动与成果记录模块
-- ----------------------------

-- ----------------------------
-- Table structure: daily_activity (日常活动表)
-- ----------------------------
DROP TABLE IF EXISTS `daily_activity`;
CREATE TABLE `daily_activity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID(关联user表)',
  `date` DATE NOT NULL COMMENT '活动日期',
  `activity` VARCHAR(255) NOT NULL COMMENT '活动内容',
  `duration` INT DEFAULT 0 COMMENT '持续时间(单位:分钟,可选)',
  `location` VARCHAR(255) COMMENT '活动地点(可选)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日常活动表';

-- ----------------------------
-- Table structure: achievement (成果/成就表)
-- ----------------------------
DROP TABLE IF EXISTS `achievement`;
CREATE TABLE `achievement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '成果ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID(关联user表)',
  `achievement` VARCHAR(255) NOT NULL COMMENT '成果内容',
  `description` TEXT COMMENT '成果详细描述(可选)',
  `category` VARCHAR(50) COMMENT '成果分类: LEARNING-学习, WORK-工作, HEALTH-健康, LIFE-生活等',
  `time` DATETIME NOT NULL COMMENT '获得时间/达成时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_time` (`time`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成果/成就表';

-- ============================================
-- 第6部分: 周期总结模块
-- ----------------------------

-- ----------------------------
-- Table structure: period_summary (周期总结表)
-- ----------------------------
DROP TABLE IF EXISTS `period_summary`;
CREATE TABLE `period_summary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '总结ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID(关联user表)',
  `period` VARCHAR(50) NOT NULL COMMENT '周期名称(例如: 2024年第1周, 2024年1月)',
  `period_type` VARCHAR(20) NOT NULL COMMENT '周期类型: WEEKLY-周总结, MONTHLY-月总结',
  `start_date` DATE NOT NULL COMMENT '周期开始日期',
  `end_date` DATE NOT NULL COMMENT '周期结束日期',
  `summary` TEXT NOT NULL COMMENT '总结内容',
  `highlights` TEXT COMMENT '亮点/成就(可选)',
  `improvements` TEXT COMMENT '改进计划(可选)',
  `next_plan` TEXT COMMENT '下一步计划(可选)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_period` (`user_id`, `period_type`, `start_date`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_period_type` (`period_type`),
  KEY `idx_start_date` (`start_date`),
  KEY `idx_end_date` (`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='周期总结表(周/月复盘)';

-- ============================================
-- 第7部分: 索引优化(可选,用于提升查询性能)
-- ----------------------------

-- 为user表添加邮箱唯一索引(如果需要邮箱登录)
-- ALTER TABLE `user` ADD UNIQUE KEY `uk_email` (`email`);

-- 为phone_usage表添加年度索引(用于年度统计)
-- ALTER TABLE `phone_usage` ADD KEY `idx_year` (`year`);

-- 为goal表添加年度索引(用于年度统计)
-- ALTER TABLE `goal` ADD KEY `idx_year` (`year`);

-- ============================================
-- 第8部分: 数据库初始化完成
-- ============================================

SET FOREIGN_KEY_CHECKS = 1;

-- 执行完成提示
SELECT '数据库初始化完成! 共创建9张数据表.' AS '执行状态';

-- ================================================================
-- SmartSchedule 初始化脚本
-- 一键创建数据库 + 表结构 + 种子数据
--
-- 使用方式:
--   mysql -u root -p < init.sql
--
-- 或登录 MySQL 后执行:
--   source init.sql;
-- ================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库（如已存在则跳过）
CREATE DATABASE IF NOT EXISTS `smartschedule`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `smartschedule`;

-- ================================================================
-- 用户表
-- ================================================================
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(100),
    `email` VARCHAR(100),
    `wake_up_time` TIME DEFAULT '08:00',
    `sleep_time` TIME DEFAULT '23:00',
    `energy_peak` VARCHAR(20) DEFAULT 'EVENING',
    `total_points` INT DEFAULT 0,
    `level` INT DEFAULT 1,
    `enabled` TINYINT(1) DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================================
-- 目标表
-- ================================================================
CREATE TABLE IF NOT EXISTS `goals` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `name` VARCHAR(200) NOT NULL,
    `description` TEXT,
    `category` VARCHAR(50) DEFAULT 'OTHER',
    `energy_level` VARCHAR(20) DEFAULT 'MEDIUM',
    `priority` INT DEFAULT 5,
    `default_duration` INT DEFAULT 60,
    `weekly_frequency` INT DEFAULT 3,
    `is_active` TINYINT(1) DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_active` (`is_active`),
    CONSTRAINT `fk_goals_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================================
-- 课表表
-- ================================================================
CREATE TABLE IF NOT EXISTS `courses` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `name` VARCHAR(200) NOT NULL,
    `day_of_week` INT NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    `location` VARCHAR(200),
    `is_active` TINYINT(1) DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_courses_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================================
-- 日计划表
-- ================================================================
CREATE TABLE IF NOT EXISTS `daily_plans` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `plan_date` DATE NOT NULL,
    `status` VARCHAR(20) DEFAULT 'DRAFT',
    `weather_summary` VARCHAR(200),
    `ai_suggestion` TEXT,
    `estimated_points` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date` (`user_id`, `plan_date`),
    CONSTRAINT `fk_daily_plans_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================================
-- 日任务表
-- ================================================================
CREATE TABLE IF NOT EXISTS `daily_tasks` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `daily_plan_id` BIGINT NOT NULL,
    `goal_id` BIGINT,
    `course_id` BIGINT,
    `name` VARCHAR(200) NOT NULL,
    `task_type` VARCHAR(20) DEFAULT 'GOAL',
    `scheduled_start` DATETIME,
    `scheduled_end` DATETIME,
    `duration_minutes` INT DEFAULT 0,
    `energy_level` VARCHAR(20) DEFAULT 'MEDIUM',
    `priority` INT DEFAULT 5,
    `status` VARCHAR(20) DEFAULT 'PENDING',
    `sort_order` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_daily_plan_id` (`daily_plan_id`),
    KEY `idx_goal_id` (`goal_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_tasks_plan` FOREIGN KEY (`daily_plan_id`) REFERENCES `daily_plans` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_tasks_goal` FOREIGN KEY (`goal_id`) REFERENCES `goals` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_tasks_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================================
-- 积分记录表
-- ================================================================
CREATE TABLE IF NOT EXISTS `points_records` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `task_id` BIGINT,
    `points` INT NOT NULL,
    `reason` VARCHAR(200),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_points_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================================
-- AI 对话记录表
-- ================================================================
CREATE TABLE IF NOT EXISTS `ai_conversations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `session_id` VARCHAR(36) NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `content` TEXT NOT NULL,
    `intent` VARCHAR(50),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_conversations_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================================
-- 种子数据：演示用户（密码由 DataInitializer 在首次启动时设置）
-- 首次启动后可用 demo / demo123 登录
-- ================================================================
INSERT INTO `users` (`username`, `password`, `nickname`, `wake_up_time`, `sleep_time`, `energy_peak`, `total_points`, `level`, `enabled`)
VALUES ('demo', '$2a$10$PLACEHOLDER_WILL_BE_UPDATED_BY_DATA_INITIALIZER', '演示用户', '08:00', '23:00', 'EVENING', 0, 1, 1)
ON DUPLICATE KEY UPDATE `username` = `username`;

-- ================================================================
-- 种子数据：示例目标（演示用户 id=1）
-- ================================================================
INSERT INTO `goals` (`user_id`, `name`, `description`, `category`, `energy_level`, `priority`, `default_duration`, `weekly_frequency`, `is_active`) VALUES
(1, '英语四级学习', '背单词、做阅读、练听力', 'STUDY', 'HIGH', 8, 60, 5, 1),
(1, '驾照理论学习', '科目一/科目四题库刷题', 'STUDY', 'MEDIUM', 7, 60, 4, 1),
(1, '健身锻炼', '力量训练或有氧运动', 'EXERCISE', 'HIGH', 6, 60, 3, 1),
(1, '课外阅读', '每天读书充电', 'HOBBY', 'LOW', 4, 30, 3, 1)
ON DUPLICATE KEY UPDATE `name` = `name`;

-- ================================================================
-- 种子数据：示例课程（演示用户 id=1）
-- ================================================================
INSERT INTO `courses` (`user_id`, `name`, `day_of_week`, `start_time`, `end_time`, `location`, `is_active`) VALUES
(1, '数据分析与应用-林燕茹', 1, '14:00', '14:30', '正心305', 1),
(1, '高等数学', 2, '08:00', '09:40', '教学楼A201', 1),
(1, '大学英语', 3, '10:00', '11:40', '教学楼B103', 1)
ON DUPLICATE KEY UPDATE `name` = `name`;

SET FOREIGN_KEY_CHECKS = 1;

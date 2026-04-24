-- 数字健康系统数据库表结构

-- 创建用户表
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(255) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码',
  `role` VARCHAR(50) NOT NULL COMMENT '角色',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 创建手机使用记录表
CREATE TABLE `phone_usage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `date` DATE NOT NULL COMMENT '日期',
  `usage_time` BIGINT NOT NULL COMMENT '使用时长（分钟）',
  PRIMARY KEY (`id`),
  KEY `idx_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='手机使用记录表';

-- 创建目标表
CREATE TABLE `goal` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '目标ID',
  `goal` VARCHAR(255) NOT NULL COMMENT '目标内容',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_end_time` (`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='目标表';

-- 创建日常活动表
CREATE TABLE `daily_activity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `date` DATE NOT NULL COMMENT '日期',
  `activity` VARCHAR(255) NOT NULL COMMENT '活动内容',
  PRIMARY KEY (`id`),
  KEY `idx_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日常活动表';

-- 创建成就表
CREATE TABLE `achievement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '成就ID',
  `achievement` VARCHAR(255) NOT NULL COMMENT '成就内容',
  `time` DATETIME NOT NULL COMMENT '获得时间',
  PRIMARY KEY (`id`),
  KEY `idx_time` (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成就表';

-- 创建周期总结表
CREATE TABLE `period_summary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '总结ID',
  `period` VARCHAR(50) NOT NULL COMMENT '周期（如：2024-01-01 至 2024-01-31）',
  `summary` TEXT NOT NULL COMMENT '总结内容',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_period` (`period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='周期总结表';
-- 创建数据库
CREATE DATABASE IF NOT EXISTS iterate CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE iterate;

-- 创建用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

-- 创建手机使用记录表
CREATE TABLE IF NOT EXISTS phone_usage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL,
    usage_time BIGINT NOT NULL -- 存储Duration的毫秒数
);

-- 创建目标表
CREATE TABLE IF NOT EXISTS goal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal VARCHAR(255) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL
);

-- 创建成就表
CREATE TABLE IF NOT EXISTS achievement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    achievement VARCHAR(255) NOT NULL,
    time DATETIME NOT NULL
);

-- 创建周期总结表
CREATE TABLE IF NOT EXISTS period_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(50) NOT NULL,
    summary TEXT NOT NULL
);

-- 插入默认数据（可选）
INSERT INTO user (username, password, role) VALUES ('admin', 'admin123', 'ADMIN');

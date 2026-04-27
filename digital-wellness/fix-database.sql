-- ============================================
-- 修复数据库 - 添加缺失的 updated_at 字段
-- ============================================

USE `iterate`;

-- 检查并为 app_preset 表添加 updated_at 字段
SET @dbname = DATABASE();
SET @tablename = "app_preset";
SET @columnname = "updated_at";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_schema = @dbname)
      AND (table_name = @tablename)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER created_at")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 检查是否添加成功
SELECT '数据库修复完成' AS status;

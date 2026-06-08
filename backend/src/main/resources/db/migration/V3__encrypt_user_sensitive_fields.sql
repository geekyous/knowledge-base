-- 企业知识库问答系统 - 敏感字段加密迁移
-- 版本: V3
-- 创建时间: 2026-06-08
-- 说明: 为 email/phone 字段启用加密存储，新增 email_hash 列用于查询匹配
-- 迁移管理: Flyway（本文件只增不删，不可包含 DROP TABLE 语句）

-- 新增 email_hash 列（SHA-256 哈希，用于代替明文 email 进行查询和唯一性校验）
ALTER TABLE kb_users ADD COLUMN email_hash CHAR(64) NULL COMMENT '邮箱SHA-256哈希（用于查询匹配）' AFTER email;

-- 拓宽 email/phone 列以容纳 AES-256 密文（Base64 编码后约为明文的 2-3 倍）
ALTER TABLE kb_users MODIFY COLUMN email VARCHAR(255) NULL COMMENT '邮箱（AES加密存储）';
ALTER TABLE kb_users MODIFY COLUMN phone VARCHAR(255) NULL COMMENT '手机号（AES加密存储）';

-- 唯一约束迁移：从 email 列移到 email_hash 列
-- email 列存的是加密后的密文，不适合做唯一性校验
ALTER TABLE kb_users DROP INDEX email;
CREATE UNIQUE INDEX uk_email_hash ON kb_users(email_hash);

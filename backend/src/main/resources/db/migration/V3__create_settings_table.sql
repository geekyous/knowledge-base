-- V3: 创建系统设置表（键值对结构，按 category 分组）
CREATE TABLE kb_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    setting_key VARCHAR(100) NOT NULL UNIQUE COMMENT '设置键',
    setting_value TEXT NOT NULL COMMENT '设置值（布尔用 true/false，数值用数字）',
    category VARCHAR(50) NOT NULL COMMENT '分组：ai / permission / storage',
    description VARCHAR(255) COMMENT '人类可读描述',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    UNIQUE INDEX uk_setting_key (setting_key),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统设置';

-- 种子数据：匹配前端 Settings.vue 的默认值
INSERT INTO kb_settings (setting_key, setting_value, category, description) VALUES
('ai.enable_qa', 'true', 'ai', '启用智能问答'),
('ai.auto_classify', 'true', 'ai', '自动文档分类'),
('ai.smart_suggestion', 'false', 'ai', '智能搜索建议'),
('permission.public_search', 'true', 'permission', '允许未登录用户搜索公开文档'),
('permission.require_review', 'true', 'permission', '新文档发布需要审核'),
('storage.used_gb', '85', 'storage', '已使用存储（GB）'),
('storage.total_gb', '100', 'storage', '总存储容量（GB）');

-- 企业知识库问答系统 - 引用数据种子
-- 版本: V2
-- 说明: 仅包含分类和标签等无用户依赖的引用数据
--       文档、对话等业务种子数据由 DataInitializer 在用户创建后生成
-- 迁移管理: Flyway（本文件只增不删，不可包含 DROP 语句）

SET NAMES utf8mb4;

-- =====================================================
-- 1. 分类数据 (4个一级分类 + 6个二级分类)
-- =====================================================
INSERT INTO kb_categories (id, name, slug, description, icon, sort_order, parent_id) VALUES
(1,  '人事制度', 'hr',          '人力资源管理相关制度文档',   'Users',       1, NULL),
(2,  '技术文档', 'tech',        '技术开发相关文档',           'Code',        2, NULL),
(3,  '销售支持', 'sales',       '销售培训和支持材料',         'TrendCharts', 3, NULL),
(4,  '合规法务', 'legal',       '法律法规和合规管理',         'Document',    4, NULL);

INSERT INTO kb_categories (id, name, slug, description, icon, sort_order, parent_id) VALUES
(11, '招聘流程',   'hr-recruit',    '招聘管理流程',     'UserAdd',   1, 1),
(12, '薪酬福利',   'hr-salary',     '薪酬和福利制度',   'Money',     2, 1),
(13, '考勤制度',   'hr-attendance', '考勤和请假制度',   'Clock',     3, 1),
(21, '架构设计',   'tech-arch',     '系统架构设计文档', 'Grid',      1, 2),
(22, '接口文档',   'tech-api',      'API接口规范',     'Connection', 2, 2),
(23, '部署指南',   'tech-deploy',   '部署运维指南',     'Upload',    3, 2);

-- =====================================================
-- 2. 标签数据 (8个常用标签)
-- =====================================================
INSERT INTO kb_tags (id, name, color, usage_count) VALUES
(1,  '流程',   '#409EFF', 3),
(2,  '规范',   '#67C23A', 2),
(3,  '指南',   '#E6A23C', 2),
(4,  '制度',   '#F56C6C', 2),
(5,  'API',    '#909399', 1),
(6,  '架构',   '#00B5AD', 1),
(7,  '销售',   '#F56C6C', 1),
(8,  '法律',   '#303133', 1);

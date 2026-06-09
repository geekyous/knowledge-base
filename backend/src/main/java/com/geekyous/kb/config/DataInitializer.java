package com.geekyous.kb.config;

import com.geekyous.kb.entity.Category;
import com.geekyous.kb.entity.Document;
import com.geekyous.kb.entity.Document.DocumentStatus;
import com.geekyous.kb.entity.User;
import com.geekyous.kb.repository.CategoryRepository;
import com.geekyous.kb.repository.DocumentRepository;
import com.geekyous.kb.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 初始数据初始化器 — 在 Flyway 迁移完成后执行
 * <p>
 * 当用户表为空时，根据配置创建初始用户和示例文档。
 * 密码通过环境变量注入，BCrypt 加密后存储。
 *
 * @author Geekyous
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.admin-password:admin123}")
    private String adminPassword;

    @Value("${app.init.editor-password:admin123}")
    private String editorPassword;

    @Value("${app.init.user-password:admin123}")
    private String userPassword;

    public DataInitializer(UserRepository userRepository,
                           DocumentRepository documentRepository,
                           CategoryRepository categoryRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            logger.info("用户数据已存在，跳过初始化");
            return;
        }

        logger.info("开始初始化数据...");

        User admin = createUser("admin", adminPassword, "admin@company.com",
                "13800000001", User.Role.ADMIN);
        User editor = createUser("editor", editorPassword, "editor@company.com",
                "13800000002", User.Role.EDITOR);
        createUser("user1", userPassword, "user1@company.com",
                "13800000003", User.Role.USER);

        createSampleDocuments(admin, editor);

        logger.info("数据初始化完成: 3 个用户, 示例文档");
    }

    private User createUser(String username, String password, String email,
                            String phone, User.Role role) {
        User user = userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .phone(phone)
                .role(role)
                .status(User.UserStatus.ACTIVE)
                .build());
        logger.info("创建用户: {} ({})", username, role);
        return user;
    }

    private void createSampleDocuments(User admin, User editor) {
        if (documentRepository.count() > 0) {
            return;
        }

        List<Category> categories = categoryRepository.findAll();

        // 员工手册
        createDocument(
                "员工手册（2026版）",
                "公司员工行为规范和基本制度汇编",
                """
                # 员工手册（2026版）

                ## 第一章 总则

                本手册适用于公司全体正式员工，旨在明确员工的基本权利和义务。

                ## 第二章 考勤管理

                ### 2.1 工作时间
                - 标准工时：周一至周五 9:00-18:00
                - 午休时间：12:00-13:00
                - 弹性工时：8:30-10:00之间打卡即可

                ### 2.2 请假制度
                - 年假：工作满1年享有5天年假，每增加1年增加1天，上限15天
                - 病假：凭医院证明，每年不超过10天
                - 事假：需提前申请，每年不超过5天

                ### 2.3 加班制度
                - 工作日加班：按1.5倍工资计算
                - 周末加班：按2倍工资计算或安排调休
                - 法定假日加班：按3倍工资计算

                ## 第三章 薪酬福利

                ### 3.1 薪资结构
                - 基本工资 + 绩效奖金 + 各项补贴
                - 每月15日发放上月工资

                ### 3.2 五险一金
                - 养老保险、医疗保险、失业保险、工伤保险、生育保险
                - 住房公积金：个人和公司各缴纳12%

                ## 第四章 行为规范

                - 遵守公司规章制度
                - 保守公司商业秘密
                - 维护公司形象和利益
                """,
                findCategoryBySlug(categories, "hr-recruit"),
                admin, DocumentStatus.PUBLISHED, 256, 18, true);

        // 年假申请流程
        createDocument(
                "年假申请流程",
                "详细说明年假天数计算方法和申请审批流程",
                """
                # 年假申请流程

                ## 一、年假天数计算

                | 工龄      | 年假天数 |
                |-----------|----------|
                | 1-5年     | 5天      |
                | 5-10年    | 7天      |
                | 10-15年   | 10天     |
                | 15年以上   | 15天     |

                > 注：年假天数按照累计工龄计算，非本司工龄。

                ## 二、申请流程

                ### 步骤1：提前申请
                - 至少提前 **15个工作日** 提交年假申请
                - 登录OA系统 → 请假管理 → 新建年假申请

                ### 步骤2：填写信息
                - 选择请假类型：年假
                - 填写起止日期和请假事由

                ### 步骤3：审批流程
                1. 直属领导审批（1-3个工作日）
                2. 部门经理审批（1-2个工作日）
                3. HR备案确认（1个工作日）

                ## 三、注意事项

                1. 年假不可跨年累积，每年12月31日前需休完
                2. 如遇法定假日，年假天数顺延
                3. 年假可以分多次使用，每次最少半天
                4. 离职时未休年假按日工资的300%折算发放
                """,
                findCategoryBySlug(categories, "hr-attendance"),
                editor, DocumentStatus.PUBLISHED, 189, 12, true);

        // 系统架构设计
        createDocument(
                "企业知识库系统架构设计",
                "基于微服务架构的企业知识库系统整体技术架构",
                """
                # 企业知识库系统架构设计

                ## 1. 系统概述

                企业知识库问答系统采用前后端分离 + AI微服务的混合架构：
                - **前端**: Vue 3 + TypeScript + Element Plus
                - **后端**: Java 17 + Spring Boot 3.2
                - **AI服务**: Python 3.11 + FastAPI + LangChain
                - **数据库**: MySQL 8.0 + Redis 7 + Elasticsearch 8 + Qdrant

                ## 2. 核心模块

                ### 2.1 用户认证模块
                - JWT Token认证机制
                - RBAC角色权限控制（USER/EDITOR/ADMIN）

                ### 2.2 文档管理模块
                - 文档CRUD、版本控制、文件上传与解析

                ### 2.3 AI问答模块（RAG）
                - 向量化文档存储（Qdrant）
                - 语义检索和召回
                - 大语言模型生成答案

                ## 3. RAG流程

                用户提问 → 意图识别 → 向量检索 → 文档召回 → Prompt构建 → LLM生成 → 答案返回
                """,
                findCategoryBySlug(categories, "tech-arch"),
                editor, DocumentStatus.PUBLISHED, 145, 9, true);

        // 合同管理办法（草稿）
        createDocument(
                "合同管理暂行办法",
                "公司合同签订、审批、归档的管理规定",
                """
                # 合同管理暂行办法

                ## 第一章 总则

                第一条 为规范公司合同管理，防范法律风险，特制定本办法。

                第二条 本办法适用于公司及各部门签订的各类合同。

                ## 第二章 合同审批

                ### 审批权限
                - 10万元以下：部门经理审批
                - 10-50万元：分管副总审批
                - 50万元以上：总经理审批

                ### 审批流程
                1. 业务部门起草合同
                2. 法务部门审核
                3. 按权限报领导审批
                4. 用印并归档

                ## 第三章 合同归档

                所有合同原件须在签订后5个工作日内交行政部归档。
                """,
                findCategoryBySlug(categories, "legal"),
                admin, DocumentStatus.DRAFT, 0, 0, false);

        logger.info("创建示例文档: 4 篇");
    }

    private void createDocument(String title, String summary, String content,
                                Long categoryId, User author,
                                DocumentStatus status, int viewCount,
                                int likeCount, boolean featured) {
        documentRepository.save(Document.builder()
                .title(title)
                .summary(summary)
                .content(content)
                .categoryId(categoryId)
                .author(author)
                .status(status)
                .viewCount(viewCount)
                .likeCount(likeCount)
                .isFeatured(featured)
                .publishedAt(status == DocumentStatus.PUBLISHED ? LocalDateTime.now() : null)
                .build());
    }

    private Long findCategoryBySlug(List<Category> categories, String slug) {
        return categories.stream()
                .filter(c -> slug.equals(c.getSlug()))
                .map(Category::getId)
                .findFirst()
                .orElse(null);
    }
}

package com.geekyous.kb.config;

import com.geekyous.kb.entity.User;
import com.geekyous.kb.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 初始用户数据初始化器 — 在 Flyway 迁移完成后执行
 * <p>
 * 当 kb_users 表为空时，根据配置创建初始管理员、编辑和普通用户。
 * 密码通过环境变量注入，BCrypt 加密后存储，避免在 SQL 中硬编码。
 *
 * @author Geekyous
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.admin-password:admin123}")
    private String adminPassword;

    @Value("${app.init.editor-password:admin123}")
    private String editorPassword;

    @Value("${app.init.user-password:admin123}")
    private String userPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            logger.info("用户数据已存在，跳过初始化");
            return;
        }

        logger.info("开始初始化用户数据...");

        userRepository.save(User.builder()
                .username("admin")
                .password(passwordEncoder.encode(adminPassword))
                .email("admin@company.com")
                .phone("13800000001")
                .role(User.Role.ADMIN)
                .status(User.UserStatus.ACTIVE)
                .build());

        userRepository.save(User.builder()
                .username("editor")
                .password(passwordEncoder.encode(editorPassword))
                .email("editor@company.com")
                .phone("13800000002")
                .role(User.Role.EDITOR)
                .status(User.UserStatus.ACTIVE)
                .build());

        userRepository.save(User.builder()
                .username("user1")
                .password(passwordEncoder.encode(userPassword))
                .email("user1@company.com")
                .phone("13800000003")
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .build());

        logger.info("用户数据初始化完成: admin, editor, user1");
    }
}

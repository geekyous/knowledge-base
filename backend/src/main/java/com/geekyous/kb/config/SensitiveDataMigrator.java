package com.geekyous.kb.config;

import com.geekyous.kb.utils.FieldEncryptor;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 敏感数据迁移器 — 将 V3 迁移前的明文 email/phone 加密并计算 email_hash。
 *
 * <p>使用原生 SQL 读写，绕过 JPA {@code AttributeConverter}，
 * 避免 Converter 尝试解密明文值导致失败。
 * 仅在检测到 email_hash 为空且 email 非空的行时执行，执行后自动跳过。
 *
 * @author Geekyous
 */
@Component
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class SensitiveDataMigrator implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveDataMigrator.class);

    private final EntityManager entityManager;
    private final FieldEncryptor fieldEncryptor;

    public SensitiveDataMigrator(EntityManager entityManager, FieldEncryptor fieldEncryptor) {
        this.entityManager = entityManager;
        this.fieldEncryptor = fieldEncryptor;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 检查是否存在未迁移的行（email_hash 为空但 email 非空）
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                "SELECT id, email, phone FROM kb_users WHERE email_hash IS NULL AND email IS NOT NULL"
        ).getResultList();

        if (rows.isEmpty()) {
            return;
        }

        logger.info("检测到 {} 条未加密的敏感数据，开始迁移...", rows.size());

        for (Object[] row : rows) {
            Long id = ((Number) row[0]).longValue();
            String plainEmail = (String) row[1];
            String plainPhone = (String) row[2];

            // 加密 email/phone
            String encryptedEmail = plainEmail != null ? fieldEncryptor.encrypt(plainEmail) : null;
            String encryptedPhone = plainPhone != null ? fieldEncryptor.encrypt(plainPhone) : null;

            // 计算 email_hash（用原始明文）
            String emailHash = plainEmail != null ? fieldEncryptor.hash(plainEmail) : null;

            // 原生 SQL 更新，绕过 AttributeConverter
            entityManager.createNativeQuery(
                    "UPDATE kb_users SET email = ?, phone = ?, email_hash = ? WHERE id = ?"
            ).setParameter(1, encryptedEmail)
             .setParameter(2, encryptedPhone)
             .setParameter(3, emailHash)
             .setParameter(4, id)
             .executeUpdate();

            logger.debug("已迁移用户 id={}", id);
        }

        logger.info("敏感数据加密迁移完成，共处理 {} 条记录", rows.size());
    }
}

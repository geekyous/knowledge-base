package com.company.kb.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * RSA 密钥对配置 — 应用启动时自动生成 RSA 2048 密钥对
 *
 * <p>用于前后端敏感字段（如密码）的加密传输：
 * <ul>
 *   <li>前端通过公钥接口获取 RSA 公钥</li>
 *   <li>前端使用公钥加密密码等敏感字段后传输</li>
 *   <li>后端使用私钥解密，还原为明文后进行业务处理</li>
 * </ul>
 * </p>
 *
 * <h3>密钥生命周期</h3>
 * <p>密钥对在应用启动时生成，存储在 JVM 内存中。应用重启后会生成新的密钥对，
 * 前端需要重新获取公钥。对于生产环境，可改为从配置文件或密钥管理服务加载固定密钥对。</p>
 *
 * @author Geekyous
 * @since 1.0.0
 */
@Configuration
public class RsaKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyConfig.class);
    private static final int KEY_SIZE = 2048;

    /**
     * 生成 RSA 密钥对 Bean
     *
     * @return RSA 2048 密钥对
     */
    @Bean
    public KeyPair rsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_SIZE);
            KeyPair keyPair = generator.generateKeyPair();
            log.info("RSA {} 密钥对已生成，公钥指纹: {}...",
                    KEY_SIZE,
                    Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()).substring(0, 20));
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA 算法不可用", e);
        }
    }
}

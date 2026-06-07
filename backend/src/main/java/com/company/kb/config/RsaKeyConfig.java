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
 * @author Geekyous
 */
@Configuration
public class RsaKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyConfig.class);
    private static final int KEY_SIZE = 2048;

    /** 生成 RSA 密钥对 Bean */
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

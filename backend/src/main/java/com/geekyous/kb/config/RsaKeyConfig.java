package com.geekyous.kb.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * RSA 密钥对配置 — 支持从外部配置加载持久化密钥对，或回退到启动时随机生成。
 *
 * <p>双模式加载：
 * <ul>
 *   <li>配置了 {@code rsa.private-key}（PKCS8 DER Base64）→ 从私钥重建 KeyPair（公钥派生）。
 *       生产环境持久化，重启/多实例共享同一对。</li>
 *   <li>未配置私钥：
 *     <ul>
 *       <li>{@code rsa.strict=false}（默认，开发）：回退启动时随机生成 + WARN。</li>
 *       <li>{@code rsa.strict=true}（生产）：抛 IllegalStateException 阻断启动。</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>只配置私钥：公钥从 {@link RSAPrivateCrtKey} 的 modulus/publicExponent 派生，
 * 天然保证公私钥配对一致，避免双配置项不匹配。注入风格对齐 {@link JwtConfig} 的 {@code @Value}。
 *
 * @author Geekyous
 * @since 1.0.0
 * @see com.geekyous.kb.utils.RsaUtil
 */
@Slf4j
@Configuration
public class RsaKeyConfig {

    private static final int KEY_SIZE = 2048;
    private static final String RSA_ALGORITHM = "RSA";

    /** PKCS8 DER Base64 私钥；留空走随机生成回退。Spring relaxed binding 对应环境变量 RSA_PRIVATE_KEY。 */
    @Value("${rsa.private-key:}")
    private String privateKeyBase64;

    /** 严格模式：true 时私钥缺失直接抛异常阻断启动。环境变量 RSA_STRICT。 */
    @Value("${rsa.strict:false}")
    private boolean strict;

    /**
     * RSA 密钥对 Bean。
     *
     * <p>优先从 {@code rsa.private-key} 加载（生产持久化）；未配置时按 {@code rsa.strict} 决定
     * 回退随机生成（开发）或抛异常阻断启动（生产）。
     *
     * @return RSA 2048 KeyPair
     * @throws IllegalStateException strict=true 且私钥未配置，或私钥格式非法时
     */
    @Bean
    public KeyPair rsaKeyPair() {
        String normalized = normalizeKey(privateKeyBase64);
        if (normalized.isEmpty()) {
            if (strict) {
                throw new IllegalStateException(
                        "RSA 私钥未配置（rsa.private-key 为空）且 rsa.strict=true，拒绝启动。"
                        + "请在 .env 设置 RSA_PRIVATE_KEY（PKCS8 DER Base64），或将 RSA_STRICT=false 走随机生成回退。");
            }
            log.warn("rsa.private-key 未配置，回退到启动时随机生成 RSA {} 密钥对（仅适用于开发）。"
                    + "生产请设置 RSA_PRIVATE_KEY 并 RSA_STRICT=true。", KEY_SIZE);
            return generateRandomKeyPair();
        }
        return loadFromPrivateKey(normalized);
    }

    /**
     * Base64 字符串容错：去 PEM header/footer、压平所有换行空白（含 \r\n）、trim。
     *
     * <p>兼容三种粘贴形态：单行纯 Base64、多行 PEM、带 {@code -----BEGIN/END-----} 的完整 PEM。
     */
    private static String normalizeKey(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return "";
        }
        s = s.replaceAll("(?m)^-----BEGIN [A-Z ]+-----$", "")
                .replaceAll("(?m)^-----END [A-Z ]+-----$", "")
                .replaceAll("\\s+", "");
        return s;
    }

    /**
     * 从 PKCS8 Base64 私钥派生完整 KeyPair（含公钥）。
     *
     * <p>派生路径：{@link PKCS8EncodedKeySpec} → {@link KeyFactory#generatePrivate} →
     * 强转 {@link RSAPrivateCrtKey} → 用 modulus + publicExponent 构造 {@link RSAPublicKeySpec}
     * → {@link KeyFactory#generatePublic}。公钥与私钥共享 modulus，数学上必然配对。
     *
     * @throws IllegalStateException Base64 非法、私钥非 CRT 形式、或规格解析失败
     */
    private KeyPair loadFromPrivateKey(String base64) {
        try {
            byte[] der = Base64.getDecoder().decode(base64);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(der));

            // 从 CRT 私钥的 modulus + publicExponent 派生公钥（数学上必然配对）
            PublicKey publicKey = keyFactory.generatePublic(
                    new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));

            KeyPair keyPair = new KeyPair(publicKey, privateKey);
            log.info("RSA 私钥已从配置加载（PKCS8），公钥派生成功，公钥指纹: {}...",
                    Base64.getEncoder().encodeToString(publicKey.getEncoded()).substring(0, 20));
            return keyPair;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("RSA 私钥 Base64 解码失败，请确认是 PKCS8 DER 的 Base64 编码", e);
        } catch (ClassCastException e) {
            throw new IllegalStateException("RSA 私钥派生公钥失败：私钥不是 RSAPrivateCrtKey（标准 OpenSSL RSA 私钥应为 CRT 形式）", e);
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException(
                    "RSA 私钥规格解析失败：请确认使用 PKCS8 格式（openssl genpkey / pkcs8 -topk8），而非 PKCS1（BEGIN RSA PRIVATE KEY）", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA 算法不可用", e);
        }
    }

    /**
     * 启动时随机生成 RSA 密钥对（开发回退路径）。
     *
     * @throws IllegalStateException RSA 算法不可用
     */
    private KeyPair generateRandomKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            generator.initialize(KEY_SIZE);
            KeyPair keyPair = generator.generateKeyPair();
            log.info("RSA {} 密钥对已（随机）生成，公钥指纹: {}...",
                    KEY_SIZE,
                    Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()).substring(0, 20));
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA 算法不可用", e);
        }
    }
}

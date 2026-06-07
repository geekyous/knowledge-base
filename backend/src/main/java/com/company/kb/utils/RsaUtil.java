package com.company.kb.utils;

import javax.crypto.Cipher;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

/**
 * RSA 加密/解密工具 — 用于前后端敏感字段的加密传输
 *
 * <p>配合 {@link com.company.kb.config.RsaKeyConfig} 使用：
 * <ul>
 *   <li>{@link #encrypt(String, PublicKey)} — 前端公钥加密对应的 Java 实现（供测试用）</li>
 *   <li>{@link #decrypt(String, PrivateKey)} — 后端私钥解密前端传来的密文</li>
 *   <li>{@link #getPublicKeyBase64(KeyPair)} — 获取 Base64 编码的公钥，供前端获取</li>
 * </ul>
 * </p>
 *
 * <h3>加密流程</h3>
 * <pre>
 * 前端：password → RSA公钥加密 → Base64密文 → HTTP 传输
 * 后端：Base64密文 → Base64解码 → RSA私钥解密 → 明文password → BCrypt比对
 * </pre>
 *
 * @author Geekyous
 * @since 1.0.0
 */
public class RsaUtil {

    private static final String RSA_ALGORITHM = "RSA";
    /** RSA 2048 最大加密明文长度（字节） */
    private static final int MAX_ENCRYPT_BLOCK = 245;

    private RsaUtil() {
        // 工具类禁止实例化
    }

    /**
     * RSA 公钥加密
     *
     * @param plainText 明文
     * @param publicKey RSA 公钥
     * @return Base64 编码的密文
     */
    public static String encrypt(String plainText, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("RSA 加密失败", e);
        }
    }

    /**
     * RSA 私钥解密
     *
     * @param cipherText Base64 编码的密文
     * @param privateKey RSA 私钥
     * @return 解密后的明文
     */
    public static String decrypt(String cipherText, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("RSA 解密失败", e);
        }
    }

    /**
     * 尝试解密，失败时返回原始值（兼容明文模式）
     *
     * <p>当传入的值不是 RSA 密文时（如开发调试直接传明文），解密会失败，
     * 此时 fallback 返回原始值，方便开发调试。</p>
     *
     * @param cipherText 可能是密文或明文
     * @param privateKey RSA 私钥
     * @return 解密后的明文，或原始值（解密失败时）
     */
    public static String tryDecrypt(String cipherText, PrivateKey privateKey) {
        try {
            return decrypt(cipherText, privateKey);
        } catch (Exception e) {
            // 解密失败，当作明文返回（开发调试兼容）
            return cipherText;
        }
    }

    /**
     * 获取 Base64 编码的公钥字符串
     *
     * @param keyPair RSA 密钥对
     * @return Base64 编码的公钥
     */
    public static String getPublicKeyBase64(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }
}

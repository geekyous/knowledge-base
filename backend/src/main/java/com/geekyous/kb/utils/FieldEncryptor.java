package com.geekyous.kb.utils;

import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 字段加密器 — 封装 Jasypt 的 AES 加密和 SHA-256 哈希能力。
 *
 * <p>供 JPA {@link com.geekyous.kb.converter.EncryptedStringConverter} 使用，
 * 在写入数据库前加密、读取后解密，以及计算 email_hash 用于查询匹配。
 *
 * @author Geekyous
 */
@Component
public class FieldEncryptor {

    private final StringEncryptor encryptor;

    public FieldEncryptor(StringEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    /**
     * AES 加密明文。
     *
     * @param plaintext 明文
     * @return Base64 编码的密文
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        return encryptor.encrypt(plaintext);
    }

    /**
     * AES 解密密文。
     *
     * @param ciphertext Base64 编码的密文
     * @return 明文
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return ciphertext;
        }
        return encryptor.decrypt(ciphertext);
    }

    /**
     * SHA-256 哈希 — 用于 email_hash 列，支持加密后的查询匹配。
     * 哈希是确定性的（同输入同输出），适合数据库索引和唯一约束。
     *
     * @param plaintext 明文
     * @return 64 字符的十六进制哈希值
     */
    public String hash(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plaintext.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

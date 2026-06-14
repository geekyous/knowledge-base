package com.geekyous.kb.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * {@link RsaUtil} 单测 — 覆盖加密往返、{@link RsaUtil#tryDecrypt} 明文回退、{@link RsaUtil#getPublicKeyBase64} 合法性。
 *
 * <p>纯静态工具类，测试内自建 KeyPair 隔离，不依赖 RsaKeyConfig Bean。
 *
 * @author Geekyous Guo
 */
class RsaUtilTest {

    private static final String PLAIN = "P@ssw0rd!中文-é";

    /** 现场生成 RSA 2048 密钥对。 */
    private static KeyPair freshKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Test
    @DisplayName("encrypt ↔ decrypt 往返一致（含中文/特殊字符）")
    void encryptDecrypt_roundTrip() throws Exception {
        KeyPair keyPair = freshKeyPair();
        String cipher = RsaUtil.encrypt(PLAIN, keyPair.getPublic());
        assertEquals(PLAIN, RsaUtil.decrypt(cipher, keyPair.getPrivate()));
    }

    @Test
    @DisplayName("tryDecrypt 传明文（合法 Base64 但非密文）：回退原值，不抛异常")
    void tryDecrypt_plainText_fallsBack() throws Exception {
        KeyPair keyPair = freshKeyPair();
        // "plaintext123" 恰好是合法 Base64，但解出的字节不是有效 RSA 密文 → 回退
        assertEquals("plaintext123", RsaUtil.tryDecrypt("plaintext123", keyPair.getPrivate()));
    }

    @Test
    @DisplayName("tryDecrypt 传正确密文：解出明文")
    void tryDecrypt_cipher_decrypts() throws Exception {
        KeyPair keyPair = freshKeyPair();
        String cipher = RsaUtil.encrypt("secret", keyPair.getPublic());
        assertEquals("secret", RsaUtil.tryDecrypt(cipher, keyPair.getPrivate()));
    }

    @Test
    @DisplayName("tryDecrypt 传非法 Base64：回退原值，不抛异常")
    void tryDecrypt_invalidBase64_fallsBack() throws Exception {
        KeyPair keyPair = freshKeyPair();
        assertEquals("!!!notbase64!!!", RsaUtil.tryDecrypt("!!!notbase64!!!", keyPair.getPrivate()));
    }

    @Test
    @DisplayName("getPublicKeyBase64 返回合法 Base64（可解码）")
    void getPublicKeyBase64_isValidBase64() throws Exception {
        KeyPair keyPair = freshKeyPair();
        String base64 = RsaUtil.getPublicKeyBase64(keyPair);
        assertDoesNotThrow(() -> Base64.getDecoder().decode(base64));
    }

    @Test
    @DisplayName("encrypt 同一明文两次返回值不同（RSA 带随机 padding）")
    void encrypt_isRandomized() throws Exception {
        KeyPair keyPair = freshKeyPair();
        assertNotEquals(
                RsaUtil.encrypt("same", keyPair.getPublic()),
                RsaUtil.encrypt("same", keyPair.getPublic()));
    }
}

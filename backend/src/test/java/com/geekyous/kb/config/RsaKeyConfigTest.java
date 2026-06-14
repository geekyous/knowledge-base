package com.geekyous.kb.config;

import com.geekyous.kb.utils.RsaUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RsaKeyConfig} 单测 — 覆盖私钥加载、公钥派生、双模式（strict/回退）与 Base64 容错。
 *
 * <p>纯 POJO 单测，不启动 Spring 容器；{@code @Value} 字段通过反射注入
 * （参考 {@code ClientIpResolverTest} 直接构造的风格）。测试密钥用 {@link KeyPairGenerator} 现场生成，不硬编码。
 *
 * @author Geekyous Guo
 */
class RsaKeyConfigTest {

    /** 现场生成 RSA 2048 密钥对，返回其私钥的 PKCS8 Base64 单行字符串。 */
    private static String freshPkcs8PrivateKeyBase64() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        return Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    }

    /** 把单行 Base64 包装成带 PEM header/footer、每 64 字符断行的完整 PEM 文本。 */
    private static String wrapAsPem(String singleLineBase64) {
        StringBuilder sb = new StringBuilder("-----BEGIN PRIVATE KEY-----\n");
        for (int i = 0; i < singleLineBase64.length(); i += 64) {
            sb.append(singleLineBase64, i, Math.min(i + 64, singleLineBase64.length())).append('\n');
        }
        sb.append("-----END PRIVATE KEY-----");
        return sb.toString();
    }

    /** 反射注入 @Value 字段后构造 RsaKeyConfig 实例。 */
    private static RsaKeyConfig configWith(String privateKey, boolean strict) throws Exception {
        RsaKeyConfig config = new RsaKeyConfig();
        Field pk = RsaKeyConfig.class.getDeclaredField("privateKeyBase64");
        pk.setAccessible(true);
        pk.set(config, privateKey);
        Field st = RsaKeyConfig.class.getDeclaredField("strict");
        st.setAccessible(true);
        st.set(config, strict);
        return config;
    }

    @Test
    @DisplayName("配置真实 PKCS8 私钥：派生 KeyPair 能自洽加解密（证明公私钥配对一致）")
    void loadFromPrivateKey_derivesWorkingKeyPair() throws Exception {
        RsaKeyConfig config = configWith(freshPkcs8PrivateKeyBase64(), false);
        KeyPair keyPair = config.rsaKeyPair();

        String plain = "hello-RSA-配对验证";
        String cipher = RsaUtil.encrypt(plain, keyPair.getPublic());
        assertEquals(plain, RsaUtil.decrypt(cipher, keyPair.getPrivate()));
    }

    @Test
    @DisplayName("strict=false + 私钥为空：回退随机生成，返回非 null KeyPair")
    void emptyKey_nonStrict_fallsBackToRandom() throws Exception {
        RsaKeyConfig config = configWith("", false);
        KeyPair keyPair = config.rsaKeyPair();
        assertNotNull(keyPair);
        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate());
    }

    @Test
    @DisplayName("strict=true + 私钥为空：抛 IllegalStateException，消息含「RSA 私钥未配置」")
    void emptyKey_strict_throws() throws Exception {
        RsaKeyConfig config = configWith("", true);
        IllegalStateException ex = assertThrows(IllegalStateException.class, config::rsaKeyPair);
        assertTrue(ex.getMessage().contains("RSA 私钥未配置"), "异常消息应指引配置 RSA_PRIVATE_KEY");
    }

    @Test
    @DisplayName("私钥含 PEM header/footer + 多行换行：容错解析成功，加解密往返")
    void pemWithHeaders_isNormalizedAndParsed() throws Exception {
        String pem = wrapAsPem(freshPkcs8PrivateKeyBase64());
        RsaKeyConfig config = configWith(pem, false);
        KeyPair keyPair = config.rsaKeyPair();

        String plain = "PEM-容-oké";
        assertEquals(plain, RsaUtil.decrypt(RsaUtil.encrypt(plain, keyPair.getPublic()), keyPair.getPrivate()));
    }

    @Test
    @DisplayName("非法私钥（含非 Base64 字符）：抛 IllegalStateException")
    void invalidBase64_throws() throws Exception {
        RsaKeyConfig config = configWith("not!a-valid*base64!!", false);
        assertThrows(IllegalStateException.class, config::rsaKeyPair);
    }

    @Test
    @DisplayName("strict=false + 空：连续两次调用得到不同 KeyPair（验证真随机非缓存）")
    void randomFallback_isNonDeterministic() throws Exception {
        RsaKeyConfig config = configWith("", false);
        KeyPair first = config.rsaKeyPair();
        KeyPair second = config.rsaKeyPair();
        assertNotEquals(first.getPublic(), second.getPublic(), "回退路径每次都应重新随机生成");
    }

    @Test
    @DisplayName("加载持久化私钥：连续两次返回同一 KeyPair（验证缓存稳定，非每次重建）")
    void loadedKey_isStable() throws Exception {
        RsaKeyConfig config = configWith(freshPkcs8PrivateKeyBase64(), false);
        KeyPair first = config.rsaKeyPair();
        KeyPair second = config.rsaKeyPair();
        assertEquals(first.getPrivate(), second.getPrivate(), "持久化私钥应确定性重建，非随机");
    }
}

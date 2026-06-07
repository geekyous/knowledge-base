package com.company.kb.utils;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;

/**
 * Jasypt 密码加密/解密工具
 *
 * <p>用于在配置文件（application.yml）中使用 ENC() 包裹的密文替代明文密码。</p>
 *
 * <h3>用法</h3>
 * <pre>
 * # 加密
 * mvn exec:java -Pencrypt -Dexec.args="encrypt root123" -Djasypt.encryptor.password=mykey
 *
 * # 解密（验证）
 * mvn exec:java -Pencrypt -Dexec.args="decrypt 密文" -Djasypt.encryptor.password=mykey
 * </pre>
 *
 * <p>生成密文后，在 application.yml 中使用 {@code ENC(密文)} 格式替换明文默认值。</p>
 *
 * @author Geekyous
 */
public class JasyptEncryptUtil {

    private static final String ALGORITHM = "PBEWITHHMACSHA512ANDAES_256";

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("用法: JasyptEncryptUtil <encrypt|decrypt> <值>");
            System.err.println();
            System.err.println("示例:");
            System.err.println("  encrypt root123          → 输出密文");
            System.err.println("  decrypt xxxxxxxxxxxxx    → 解密并输出明文");
            System.exit(1);
        }

        String action = args[0];
        String value = args[1];

        // 主密钥通过系统属性 -Djasypt.encryptor.password 传入
        String masterKey = System.getProperty("jasypt.encryptor.password");
        if (masterKey == null || masterKey.isBlank()) {
            System.err.println("错误：请通过 -Djasypt.encryptor.password=<主密钥> 传入主密钥");
            System.exit(1);
        }

        StandardPBEStringEncryptor encryptor = createEncryptor(masterKey);

        switch (action.toLowerCase()) {
            case "encrypt" -> {
                String encrypted = encryptor.encrypt(value);
                System.out.println("加密结果（复制到配置文件中使用）:");
                System.out.println("ENC(" + encrypted + ")");
            }
            case "decrypt" -> {
                // 去除可能存在的 ENC() 包裹
                String ciphertext = value;
                if (ciphertext.startsWith("ENC(") && ciphertext.endsWith(")")) {
                    ciphertext = ciphertext.substring(4, ciphertext.length() - 1);
                }
                String decrypted = encryptor.decrypt(ciphertext);
                System.out.println("解密结果: " + decrypted);
            }
            default -> {
                System.err.println("错误：未知操作 '" + action + "'，请使用 encrypt 或 decrypt");
                System.exit(1);
            }
        }
    }

    /**
     * 创建与 Spring Boot 自动配置一致的加密器实例
     */
    private static StandardPBEStringEncryptor createEncryptor(String password) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(password);
        encryptor.setAlgorithm(ALGORITHM);
        encryptor.setIvGenerator(new RandomIvGenerator());
        return encryptor;
    }
}

package com.geekyous.kb.converter;

import com.geekyous.kb.config.SpringContextHolder;
import com.geekyous.kb.utils.FieldEncryptor;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA 字段加密转换器 — 在 Entity 字段与数据库列之间自动进行 AES 加密/解密。
 *
 * <p>使用方式：在 Entity 字段上标注 {@code @Convert(converter = EncryptedStringConverter.class)}。
 * <p>注意：{@code AttributeConverter} 由 Hibernate 创建，不在 Spring 容器中，
 * 因此通过 {@link SpringContextHolder} 获取 {@link FieldEncryptor} Bean。
 *
 * @author Geekyous
 * @see com.geekyous.kb.utils.FieldEncryptor
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        return getEncryptor().encrypt(plaintext);
    }

    @Override
    public String convertToEntityAttribute(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return ciphertext;
        }
        return getEncryptor().decrypt(ciphertext);
    }

    private FieldEncryptor getEncryptor() {
        return SpringContextHolder.getBean(FieldEncryptor.class);
    }
}

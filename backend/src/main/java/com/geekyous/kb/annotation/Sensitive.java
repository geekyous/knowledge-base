package com.geekyous.kb.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.geekyous.kb.serializer.SensitiveSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感字段自动脱敏注解 — 标注在 DTO 字段上，Jackson 序列化时自动掩码。
 *
 * <p>使用方式：
 * <pre>
 * &#64;Sensitive(Sensitive.Type.EMAIL)
 * private String email;
 * </pre>
 *
 * @author Geekyous
 * @see com.geekyous.kb.serializer.SensitiveSerializer
 * @see com.geekyous.kb.utils.SensitiveFieldUtil
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveSerializer.class)
public @interface Sensitive {

    /** 脱敏类型，决定掩码规则 */
    SensitiveType value();

    /** 支持的脱敏类型 */
    enum SensitiveType {
        /** 邮箱：u***@example.com */
        EMAIL,
        /** 手机号：138****5678 */
        PHONE,
        /** 身份证：1101****1234 */
        ID_CARD
    }
}

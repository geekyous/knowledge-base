package com.geekyous.kb.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.geekyous.kb.annotation.Sensitive;
import com.geekyous.kb.utils.SensitiveFieldUtil;

import java.io.IOException;

/**
 * 敏感字段 Jackson 序列化器 — 根据 {@link Sensitive} 注解类型自动脱敏。
 *
 * <p>通过 {@link ContextualSerializer} 在创建时读取字段上的 {@code @Sensitive} 注解，
 * 获取脱敏类型后缓存，序列化时直接调用 {@link SensitiveFieldUtil} 对应方法。
 * <p>
 * 使用方式：在 DTO 字段上标注 {@code @Sensitive(SensitiveType.EMAIL)} 即可，
 * 无需在 Service 层手动调用脱敏工具。
 *
 * @author Geekyous
 * @see Sensitive
 * @see SensitiveFieldUtil
 */
public class SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer {

    /** 脱敏类型，由 createContextual 从注解读取后缓存 */
    private final Sensitive.SensitiveType type;

    /** 无参构造 — Jackson 初始创建时使用 */
    public SensitiveSerializer() {
        this.type = null;
    }

    /** 有参构造 — createContextual 创建已配置的实例 */
    public SensitiveSerializer(Sensitive.SensitiveType type) {
        this.type = type;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null || value.isBlank() || type == null) {
            gen.writeString(value);
            return;
        }

        String masked = switch (type) {
            case EMAIL -> SensitiveFieldUtil.maskEmail(value);
            case PHONE -> SensitiveFieldUtil.maskPhone(value);
            case ID_CARD -> SensitiveFieldUtil.maskIdCard(value);
        };
        gen.writeString(masked);
    }

    /**
     * Jackson 上下文回调 — 首次序列化字段时调用，从注解读取脱敏类型并返回已配置的序列化器实例。
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        Sensitive annotation = property.getAnnotation(Sensitive.class);
        if (annotation != null) {
            return new SensitiveSerializer(annotation.value());
        }
        return this;
    }
}

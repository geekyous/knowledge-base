package com.geekyous.kb.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 上下文持有器 — 为非 Spring 管理的组件（如 JPA AttributeConverter）提供 Bean 获取能力。
 *
 * <p>JPA 的 {@code AttributeConverter} 由 Hibernate 通过反射创建，不在 Spring 容器中，
 * 因此无法使用 {@code @Autowired}。此类通过静态方法暴露 {@link ApplicationContext}，
 * 使 Converter 能获取 Spring 管理的 Bean。
 *
 * @author Geekyous
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextHolder.context = applicationContext;
    }

    /**
     * 按 Class 获取 Spring Bean。
     *
     * @param clazz Bean 的类型
     * @param <T>   Bean 类型
     * @return Bean 实例
     */
    public static <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }
}

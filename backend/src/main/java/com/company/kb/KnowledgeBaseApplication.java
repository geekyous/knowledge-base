package com.company.kb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 企业知识库问答系统 - 应用程序主启动类（Application Entry Point）
 *
 * <h2>架构角色</h2>
 * <p>这是整个 Spring Boot 应用的入口点。Spring Boot 采用"约定优于配置"的设计理念，
 * 通过这一个类就能启动一个完整的 Web 应用，无需传统的 web.xml 配置文件。</p>
 *
 * <h2>核心注解解析</h2>
 * <ul>
 *   <li>{@code @SpringBootApplication} — 这是一个组合注解（元注解），等价于同时使用：
 *     <ul>
 *       <li>{@code @SpringBootConfiguration} — 声明当前类是配置类（本质是 {@code @Configuration}）</li>
 *       <li>{@code @EnableAutoConfiguration} — 告诉 Spring Boot 根据类路径上的依赖自动配置 Bean。
 *           例如：检测到 spring-boot-starter-web 就自动配置 Tomcat、DispatcherServlet 等</li>
 *       <li>{@code @ComponentScan} — 自动扫描当前包及子包下的 {@code @Component}、{@code @Service}、
 *           {@code @Repository}、{@code @Controller} 等注解，将它们注册为 Spring Bean</li>
 *     </ul>
 *   </li>
 *   <li>{@code @EnableJpaAuditing} — 启用 JPA 审计功能，使实体类中的 {@code @CreatedDate}、
 *       {@code @LastModifiedDate} 等注解生效。注意：此注解必须放在配置类上，
 *       本项目放在启动类上是常见做法。它会在后台自动填充时间戳字段。</li>
 * </ul>
 *
 * <h2>启动流程</h2>
 * <ol>
 *   <li>JVM 调用 {@code main} 方法</li>
 *   <li>{@code SpringApplication.run()} 创建 Spring 应用上下文（ApplicationContext）</li>
 *   <li>执行组件扫描，发现并注册所有带注解的 Bean</li>
 *   <li>执行自动配置，根据 classpath 依赖自动设置数据源、Web 服务器等</li>
 *   <li>启动内嵌 Tomcat 服务器（默认端口 8080）</li>
 *   <li>应用就绪，开始接收 HTTP 请求</li>
 * </ol>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>约定优于配置</b>: Spring Boot 的核心设计哲学。只要把类放在主启动类的同包或子包下，
 *       就会被自动扫描到，无需手动配置</li>
 *   <li><b>起步依赖（Starter）</b>: spring-boot-starter-web、spring-boot-starter-data-jpa 等
 *       起步依赖是一组相关库的集合，简化了 Maven/Gradle 依赖管理</li>
 *   <li><b>内嵌服务器</b>: Spring Boot 无需外部部署 WAR 包到 Tomcat，而是内嵌了 Tomcat/Jetty，
 *       直接以 JAR 方式运行（java -jar app.jar）</li>
 *   <li><b>自动配置的条件化</b>: 自动配置基于 {@code @ConditionalOnClass} 等条件注解，
 *       只有当某个类存在于 classpath 时才生效</li>
 * </ul>
 *
 * @author Geekyous Guo
 * @since 1.0.0
 * @see SpringBootApplication
 * @see EnableJpaAuditing
 */
@SpringBootApplication
@EnableJpaAuditing
public class KnowledgeBaseApplication {

    /**
     * 应用程序主入口方法。
     *
     * <p>{@code SpringApplication.run()} 是一个静态辅助方法，它完成以下工作：
     * 1. 创建 ApplicationContext（Spring 容器）
     * 2. 注册命令行参数为 Spring 属性
     * 3. 刷新容器（触发所有 Bean 的创建和初始化）
     * 4. 启动内嵌 Web 服务器</p>
     *
     * @param args 命令行参数，可以覆盖 application.properties/yml 中的配置。
     *             例如：{@code --server.port=9090} 可以修改启动端口
     */
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeBaseApplication.class, args);
    }
}

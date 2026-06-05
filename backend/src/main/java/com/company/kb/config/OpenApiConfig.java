package com.company.kb.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger 配置类 — 定义 API 文档的元信息和安全方案
 *
 * <p>springdoc-openapi 自动扫描 Controller 生成 OpenAPI 规范文档，
 * 本类提供全局级别的定制：API 标题/描述、JWT 安全方案、服务器地址等。</p>
 *
 * <h3>访问地址</h3>
 * <ul>
 *   <li>Swagger UI: {@code http://localhost:8080/api/swagger-ui.html}</li>
 *   <li>OpenAPI JSON: {@code http://localhost:8080/api/v3/api-docs}</li>
 * </ul>
 *
 * @author Geekyous Guo
 * @since 1.0.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * 自定义 OpenAPI 元信息 Bean。
     *
     * <p>SpringDoc 自动收集此 Bean，用于填充 Swagger UI 页面的标题、描述等信息，
     * 以及定义全局安全方案（JWT Bearer Token）。</p>
     *
     * @return 定制后的 OpenAPI 对象
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            // API 基本信息
            .info(new Info()
                .title("企业知识库问答系统 API")
                .description("提供文档管理、智能问答、分类管理、用户认证等 RESTful API 接口")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Geekyous Guo")
                    .email("geekyous@example.com")))
            // 服务器地址（与 application.yml 中的 context-path 保持一致）
            .servers(List.of(
                new Server().url("/api").description("当前服务器")
            ))
            // 定义 JWT Bearer 安全方案
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .name("bearerAuth")
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("请输入 JWT Token（登录接口返回的 token 字段）")));
    }
}

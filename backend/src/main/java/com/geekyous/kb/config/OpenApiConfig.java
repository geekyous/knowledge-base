package com.geekyous.kb.config;

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
 * @author Geekyous Guo
 */
@Configuration
public class OpenApiConfig {

    /** 自定义 OpenAPI 元信息 Bean */
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

package com.geekyous.kb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 企业知识库问答系统 - 应用程序主启动类
 *
 * @author Geekyous Guo
 */
@SpringBootApplication
@EnableJpaAuditing
public class KnowledgeBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeBaseApplication.class, args);
    }
}

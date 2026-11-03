package com.smartschedule.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 配置。
 * 访问地址: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartScheduleOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmartSchedule API")
                        .description("AI 驱动的智能日程规划助手 - RESTful API 文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SmartSchedule Team")
                                .email("team@smartschedule.dev")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("输入 JWT Token（登录接口返回的 token 值）")));
    }
}

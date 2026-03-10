package com.ksa.financing.identity.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.kong.base-url:http://localhost:8000}")
    private String kongBaseUrl;

    @Bean
    public OpenAPI identityServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Identity Service API")
                        .description("KSA Islamic Financing Platform - Identity & Authentication Service. "
                                + "Handles user registration, PIN-based login, token management, Keycloak integration, "
                                + "RBAC role/permission management, Casbin policy management, and authorization checks.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("KSA Financing Platform Team")))
                .servers(List.of(
                        new Server()
                                .url(kongBaseUrl + "/identity-service")
                                .description("Kong API Gateway")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer JWT"))
                .schemaRequirement("Bearer JWT", new SecurityScheme()
                        .name("Bearer JWT")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter your JWT token from /api/v1/auth/login or /api/v1/auth/login-with-pin"));
    }
}

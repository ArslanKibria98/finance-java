package com.ksa.financing.onboarding.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Customer Onboarding API")
                        .description("Signal-driven customer onboarding workflow — KSA Islamic Financing Platform\n\n"
                                + "**Public endpoints** (no token): Initiate, Verify OTP, Resend OTP, Nafath Callback\n\n"
                                + "**Protected endpoints** (Bearer JWT): Accept Terms, Initiate Nafath, Submit Info, Submit EDD, Set PIN, Status")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("KSA Islamic Financing Platform")
                                .email("support@company.com")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token from Verify OTP or Login endpoint")))
                .servers(List.of(
                        new Server().url("http://46.62.226.94:8000/onboarding-service").description("Kong Gateway (port 8000)"),
                        new Server().url("http://46.62.226.94:8089").description("Direct Access (port 8089)")))
                .tags(List.of(
                        new Tag().name("1. Initiate Onboarding")
                                .description("PUBLIC — Start onboarding, run Tahakuk mobile check, send OTP"),
                        new Tag().name("2. OTP Verification")
                                .description("PUBLIC — Verify OTP code, create Keycloak user, return JWT tokens"),
                        new Tag().name("3. Accept Terms")
                                .description("JWT REQUIRED — Accept or decline terms and conditions"),
                        new Tag().name("4. Nafath Verification")
                                .description("Initiate Nafath (JWT) + Callback webhook (PUBLIC)"),
                        new Tag().name("5. Additional Info")
                                .description("JWT REQUIRED — Submit employment, banking details and isPep flag"),
                        new Tag().name("6. EDD Form")
                                .description("JWT REQUIRED — Enhanced Due Diligence form (only when isPep=true)"),
                        new Tag().name("7. Set PIN")
                                .description("JWT REQUIRED — Set 6-digit app PIN to complete onboarding"),
                        new Tag().name("8. Status")
                                .description("PUBLIC — Query full onboarding state at any time"),
                        new Tag().name("Health")
                                .description("Service health check")));
    }
}

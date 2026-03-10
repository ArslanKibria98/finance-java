package com.ksa.financing.product.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
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
                        .title("Product Service API")
                        .description("Islamic financing product catalog management — KSA Islamic Financing Platform\n\n"
                                + "Manages the 5-step product creation wizard:\n"
                                + "- **Step 1**: Basic product information (category, type, financial terms)\n"
                                + "- **Step 2**: Commodity configuration (Tawarruq commodity details)\n"
                                + "- **Step 3**: Settings (8 tabs: Application Steps, T&C, Fees, Admin Slabs, Environment, Duration, Approval, Credit Scoring)\n"
                                + "- **Step 4**: Partner affiliations\n"
                                + "- **Step 5**: Required documents\n\n"
                                + "All endpoints require a valid JWT Bearer token from Keycloak (CompanyRealm).\n"
                                + "Tenant isolation is enforced via `tenant_id` JWT claim.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("KSA Islamic Financing Platform")
                                .email("support@kfs.com")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token from Keycloak CompanyRealm. Include `tenant_id` claim for multi-tenant isolation.")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .servers(List.of(
                        new Server().url("http://46.62.226.94:8000/product-service").description("Kong Gateway"),
                        new Server().url("http://46.62.226.94:8091").description("Direct Access"),
                        new Server().url("http://localhost:8091").description("Local Development")))
                .tags(List.of(
                        new Tag().name("Product Categories")
                                .description("Master and sub-category reference data (Murabaha, Ijarah, Tawarruq, BNPL, Crowd Funding)"),
                        new Tag().name("Products")
                                .description("Product CRUD, activation/deactivation, and wizard step 1 (basic info)"),
                        new Tag().name("Product Settings")
                                .description("Wizard step 3 — All 8 settings tabs:\n"
                                        + "- Tab 1: Application Steps\n"
                                        + "- Tab 2: Terms & Conditions\n"
                                        + "- Tab 3: Fee Settings (min/max financing, VAT, DBR)\n"
                                        + "- Tab 4: Admin Fee Slabs (tiered fees by amount range)\n"
                                        + "- Tab 5: Environment Configs (third-party integrations: SIMAH, Nafath, Yakeen)\n"
                                        + "- Tab 6: Duration Settings (request/approval/disbursement/repayment durations)\n"
                                        + "- Tab 7: Approval Workflows (auto-approve, manual review, rejection rules)\n"
                                        + "- Tab 8: Credit Scoring (criteria + rules with weights and percentages)"),
                        new Tag().name("Product Partners")
                                .description("Wizard step 4 — Partner affiliations (PRIMARY, SECONDARY, REFERRAL) with commission rates"),
                        new Tag().name("Product Documents")
                                .description("Wizard step 5 — Required documents (National ID, Salary Certificate, Bank Statement, etc.)")));
    }
}

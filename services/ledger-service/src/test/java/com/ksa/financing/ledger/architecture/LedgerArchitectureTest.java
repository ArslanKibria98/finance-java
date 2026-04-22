package com.ksa.financing.ledger.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * ArchUnit test enforcing hexagonal architecture rules for ledger-service.
 * MANDATORY per TESTING_STANDARDS.md — must pass on every build.
 */
@AnalyzeClasses(
        packages = "com.ksa.financing.ledger",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class LedgerArchitectureTest {

    // -----------------------------------------------------------------------
    // Rule 1: Domain must not depend on infrastructure
    // -----------------------------------------------------------------------
    @ArchTest
    static final ArchRule domain_must_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAPackage("..ledger.domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..ledger.infrastructure..")
                    .because("Domain layer must be pure — zero infrastructure dependencies");

    // -----------------------------------------------------------------------
    // Rule 2: Domain must not depend on adapter layer
    // -----------------------------------------------------------------------
    @ArchTest
    static final ArchRule domain_must_not_depend_on_adapter =
            noClasses()
                    .that().resideInAPackage("..ledger.domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..ledger.adapter..")
                    .because("Domain layer must not depend on adapter layer");

    // -----------------------------------------------------------------------
    // Rule 3: Domain must not use Spring framework
    // -----------------------------------------------------------------------
    @ArchTest
    static final ArchRule domain_must_not_use_spring =
            noClasses()
                    .that().resideInAPackage("..ledger.domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.springframework..")
                    .because("Domain layer must be a pure Java domain — no Spring annotations");

    // -----------------------------------------------------------------------
    // Rule 4: Domain must not use JPA annotations
    // -----------------------------------------------------------------------
    @ArchTest
    static final ArchRule domain_must_not_use_jpa =
            noClasses()
                    .that().resideInAPackage("..ledger.domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("jakarta.persistence..")
                    .because("Domain layer must not contain JPA/persistence annotations");

    // -----------------------------------------------------------------------
    // Rule 5: Application layer must not depend on adapter layer
    // -----------------------------------------------------------------------
    @ArchTest
    static final ArchRule application_must_not_depend_on_adapter =
            noClasses()
                    .that().resideInAPackage("..ledger.application..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..ledger.adapter..")
                    .because("Application layer must not depend on adapter layer — only inward dependencies");

    // -----------------------------------------------------------------------
    // Rule 6: Infrastructure must not depend on adapter layer
    // -----------------------------------------------------------------------
    @ArchTest
    static final ArchRule infrastructure_must_not_depend_on_adapter =
            noClasses()
                    .that().resideInAPackage("..ledger.infrastructure..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..ledger.adapter..")
                    .because("Infrastructure layer must not depend on adapter layer");

    // -----------------------------------------------------------------------
    // Rule 7: Controllers must not directly access repositories
    // -----------------------------------------------------------------------
    @ArchTest
    static final ArchRule controllers_must_not_access_jpa_repositories =
            noClasses()
                    .that().resideInAPackage("..ledger.adapter.rest..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..ledger.infrastructure.persistence.repository.Jpa..")
                    .because("Controllers must use use cases — never JPA repositories directly");

    // -----------------------------------------------------------------------
    // Rule 8: Domain must not use Kafka
    // -----------------------------------------------------------------------
    @ArchTest
    static final ArchRule domain_must_not_use_kafka =
            noClasses()
                    .that().resideInAPackage("..ledger.domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.apache.kafka..")
                    .because("Domain layer must not depend on Kafka — use EventPublisher port instead");
}

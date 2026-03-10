package com.ksa.financing.identity.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit tests enforcing hexagonal architecture rules.
 * MANDATORY for every service per TESTING_STANDARDS.md.
 */
@AnalyzeClasses(
        packages = "com.ksa.financing.identity",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .because("Domain layer must not depend on infrastructure (hexagonal architecture)");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_adapter =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter..")
                    .because("Domain layer must not depend on adapter layer (hexagonal architecture)");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_application =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..application..")
                    .because("Domain layer must not depend on application layer (hexagonal architecture)");

    @ArchTest
    static final ArchRule domain_should_not_use_spring =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .because("Domain layer must be pure Java with zero Spring dependencies");

    @ArchTest
    static final ArchRule domain_should_not_use_jakarta_persistence =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                    .because("Domain layer must not use JPA annotations");

    @ArchTest
    static final ArchRule application_should_not_depend_on_adapter =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter..")
                    .because("Application layer must not depend on adapter layer");

    @ArchTest
    static final ArchRule application_should_not_depend_on_infrastructure_persistence =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure.persistence..")
                    .because("Application layer must not depend on persistence implementation details");

    @ArchTest
    static final ArchRule controllers_should_not_access_repositories =
            noClasses().that().resideInAPackage("..adapter.rest..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure.persistence..")
                    .because("REST controllers must not bypass application layer to access persistence directly");
}

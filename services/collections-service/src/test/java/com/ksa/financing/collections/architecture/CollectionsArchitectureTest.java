package com.ksa.financing.collections.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(
        packages = "com.ksa.financing.collections",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class CollectionsArchitectureTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .as("Domain layer must not depend on infrastructure layer");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_adapter =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter..")
                    .as("Domain layer must not depend on adapter layer");

    @ArchTest
    static final ArchRule domain_must_not_use_spring =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .as("Domain layer must not use Spring framework");

    @ArchTest
    static final ArchRule domain_must_not_use_jpa =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                    .as("Domain layer must not use JPA annotations");

    @ArchTest
    static final ArchRule application_must_not_depend_on_adapter =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter..")
                    .as("Application layer must not depend on adapter layer");

    @ArchTest
    static final ArchRule infrastructure_must_not_depend_on_adapter =
            noClasses()
                    .that().resideInAPackage("..infrastructure..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter.rest..")
                    .as("Infrastructure layer must not depend on REST adapter layer");

    @ArchTest
    static final ArchRule controllers_must_not_access_repositories_directly =
            noClasses()
                    .that().resideInAPackage("..adapter.rest.controller..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure.persistence..")
                    .as("REST controllers must not directly access persistence repositories");

    @ArchTest
    static final ArchRule layered_architecture_is_respected =
            layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .layer("Domain").definedBy("..domain..")
                    .layer("Application").definedBy("..application..")
                    .layer("Infrastructure").definedBy("..infrastructure..")
                    .layer("Adapter").definedBy("..adapter..")
                    .whereLayer("Domain").mayNotAccessAnyLayer()
                    .whereLayer("Application").mayOnlyAccessLayers("Domain")
                    .whereLayer("Infrastructure").mayOnlyAccessLayers("Domain", "Application")
                    .whereLayer("Adapter").mayOnlyAccessLayers("Domain", "Application")
                    .as("Hexagonal architecture layers must respect dependency direction");
}

package com.treasury.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class CanonicalHexagonalArchitectureTest {
    private static JavaClasses classes;

    @BeforeAll
    static void load() {
        classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.treasury");
    }

    @Test
    void domainIsFrameworkIndependent() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..application..", "..infrastructure..",
                        "org.springframework..", "jakarta.persistence..", "org.hibernate..",
                        "com.fasterxml.jackson..", "com.rabbitmq..", "org.springframework.amqp..",
                        "io.jsonwebtoken..", "org.springframework.security..",
                        "org.springframework.security.oauth2..")
                .check(classes);
    }

    @Test
    void applicationIsFrameworkAndInfrastructureIndependent() {
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..infrastructure..", "org.springframework..", "jakarta.persistence..",
                        "org.hibernate..", "com.fasterxml.jackson..", "com.rabbitmq..",
                        "org.springframework.amqp..", "io.jsonwebtoken..",
                        "org.springframework.security..", "org.springframework.security.oauth2..")
                .check(classes);
    }

    @Test
    void applicationPortsAreInterfaces() {
        classes().that().resideInAnyPackage("..application.input..", "..application.output..")
                .should().beInterfaces()
                .check(classes);
    }

    @Test
    void dependenciesPointInward() {
        layeredArchitecture().consideringOnlyDependenciesInLayers()
                .layer("Domain").definedBy("..domain..")
                .layer("Application").definedBy("..application..")
                .layer("Infrastructure").definedBy("..infrastructure..")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
                .check(classes);
    }
}

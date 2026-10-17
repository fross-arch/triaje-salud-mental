package com.triaje.arquitectura;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Pruebas de reglas de arquitectura hexagonal.
 * La capa de dominio debe ser Java puro y no depender de infraestructura ni de Spring.
 */
@AnalyzeClasses(packages = "com.triaje", importOptions = {ImportOption.DoNotIncludeTests.class})
public class ArquitecturaTest {

    @ArchTest
    static final ArchRule elDominioNoDebeDependerDeInfraestructura = noClasses()
            .that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat().resideInAPackage("..infraestructura..");

    @ArchTest
    static final ArchRule elDominioNoDebeDependerDeAplicacion = noClasses()
            .that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat().resideInAPackage("..aplicacion..");

    @ArchTest
    static final ArchRule elDominioNoDebeDependerDeSpring = noClasses()
            .that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule elDominioNoDebeDependerDeJpa = noClasses()
            .that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule laCapaDeAplicacionNoDebeDependerDeInfraestructura = noClasses()
            .that().resideInAPackage("..aplicacion..")
            .should().dependOnClassesThat().resideInAPackage("..infraestructura..");

    @ArchTest
    static final ArchRule laCapaDeAplicacionNoDebeDependerDeSpring = noClasses()
            .that().resideInAPackage("..aplicacion..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..");
}

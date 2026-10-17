package com.bigfito.elastiflix.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Comprueba la matriz de dependencias de {@code PLAN.md}, sección 8.
 *
 * <p>Cada regla es una lista de prohibición. Un módulo puede usar cualquier otra librería
 * gestionada por el POM padre mientras no toque lo que aquí se prohíbe. Solo se importan
 * las clases de producción de los cuatro módulos.</p>
 */
@AnalyzeClasses(
        packages = "com.bigfito.elastiflix",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String CORE = "com.bigfito.elastiflix.core..";
    private static final String SEARCH = "com.bigfito.elastiflix.search..";
    private static final String INGEST = "com.bigfito.elastiflix.ingest..";
    private static final String WEB = "com.bigfito.elastiflix.web..";
    private static final String SPRING_WEB = "org.springframework.web..";
    private static final String THYMELEAF = "org.thymeleaf..";
    private static final String ELASTIC_CLIENT = "co.elastic.clients..";

    /** El núcleo no conoce la búsqueda, la carga, la web, Spring MVC ni Thymeleaf. */
    @ArchTest
    static final ArchRule coreDoesNotDependOnUpperLayers = noClasses()
            .that().resideInAPackage(CORE)
            .should().dependOnClassesThat().resideInAnyPackage(
                    SEARCH, INGEST, WEB, SPRING_WEB, THYMELEAF);

    /** La búsqueda no conoce la carga, la web, Spring MVC ni Thymeleaf. */
    @ArchTest
    static final ArchRule searchDoesNotDependOnIngestOrWeb = noClasses()
            .that().resideInAPackage(SEARCH)
            .should().dependOnClassesThat().resideInAnyPackage(INGEST, WEB, SPRING_WEB, THYMELEAF);

    /** La carga no conoce la búsqueda, la web, Spring MVC ni Thymeleaf. */
    @ArchTest
    static final ArchRule ingestDoesNotDependOnSearchOrWeb = noClasses()
            .that().resideInAPackage(INGEST)
            .should().dependOnClassesThat().resideInAnyPackage(SEARCH, WEB, SPRING_WEB, THYMELEAF);

    /** La web no usa el cliente de Elasticsearch; llama a las fachadas de los otros módulos. */
    @ArchTest
    static final ArchRule webDoesNotUseElasticsearchClient = noClasses()
            .that().resideInAPackage(WEB)
            .should().dependOnClassesThat().resideInAPackage(ELASTIC_CLIENT);

    /**
     * No hay ciclos entre core, search, ingest y web.
     * El paréntesis de la expresión captura el nombre del módulo.
     */
    @ArchTest
    static final ArchRule noCyclesBetweenModules = slices()
            .matching("com.bigfito.elastiflix.(*)..")
            .should().beFreeOfCycles();

    /**
     * Un controlador vive en el módulo web y su nombre termina en {@code Controller}.
     * En la Fase 0 aún no hay controladores: {@code archunit.properties} permite que la regla
     * no falle por estar vacía. La tarea 4.7 quita esa tolerancia.
     */
    @ArchTest
    static final ArchRule controllersLiveOnlyInWeb = classes()
            .that().haveSimpleNameEndingWith("Controller")
            .or().areAnnotatedWith(Controller.class)
            .or().areAnnotatedWith(RestController.class)
            .should().resideInAPackage(WEB)
            .andShould().haveSimpleNameEndingWith("Controller");

    /** Nadie escribe en {@code System.out} ni en {@code System.err}. El log va por SLF4J. */
    @ArchTest
    static final ArchRule noStandardStreams = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
}

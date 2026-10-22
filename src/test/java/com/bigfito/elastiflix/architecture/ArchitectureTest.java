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
 * <p>El proyecto es un solo módulo de Maven (Requerimiento 8): {@code core}, {@code search}, {@code ingest} y
 * {@code web} son paquetes que comparten classpath, así que estas reglas son las que los mantienen separados. Cada
 * regla es una lista de prohibición: un paquete puede usar cualquier otra librería del POM mientras no toque lo que
 * aquí se prohíbe. Solo se importan las clases de producción.</p>
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
    private static final String SERVLET = "jakarta.servlet..";
    private static final String SPRING_UI = "org.springframework.ui..";
    private static final String SPRING_HTTP = "org.springframework.http..";
    private static final String BOOT_WEBMVC = "org.springframework.boot.webmvc..";

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

    /**
     * Servlet, HTTP y los modelos de Spring MVC son de la web. Con cuatro módulos lo impedía Maven, porque no estaban
     * en el classpath de core, search e ingest; con uno solo lo impide esta regla.
     */
    @ArchTest
    static final ArchRule onlyWebUsesTheServletStack = noClasses()
            .that().resideInAnyPackage(CORE, SEARCH, INGEST)
            .should().dependOnClassesThat().resideInAnyPackage(SERVLET, SPRING_UI, SPRING_HTTP, BOOT_WEBMVC);

    /** La web no usa el cliente de Elasticsearch; llama a las fachadas de los otros paquetes. */
    @ArchTest
    static final ArchRule webDoesNotUseElasticsearchClient = noClasses()
            .that().resideInAPackage(WEB)
            .should().dependOnClassesThat().resideInAPackage(ELASTIC_CLIENT);

    /**
     * No hay ciclos entre core, search, ingest y web.
     * El paréntesis de la expresión captura el nombre del paquete.
     * {@code ElastiflixApplication}, en la raíz, no pertenece a ninguno.
     */
    @ArchTest
    static final ArchRule noCyclesBetweenPackages = slices()
            .matching("com.bigfito.elastiflix.(*)..")
            .should().beFreeOfCycles();

    /**
     * Un controlador vive en el paquete web y su nombre termina en {@code Controller}.
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

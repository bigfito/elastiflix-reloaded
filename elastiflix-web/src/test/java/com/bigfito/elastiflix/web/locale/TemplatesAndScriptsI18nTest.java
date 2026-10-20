package com.bigfito.elastiflix.web.locale;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que los scripts JavaScript y las plantillas Thymeleaf no contengan frases en español cableadas.
 */
class TemplatesAndScriptsI18nTest {

    private static final Path TEMPLATES_DIR = Path.of("src/main/resources/templates");
    private static final Path JS_DIR = Path.of("src/main/resources/static/js");

    private static final List<String> FORBIDDEN_JS_LITERALS = List.of(
            "No se puede cargar este archivo",
            "El archivo se rechazó",
            "Cambiar archivo",
            "Comprobando archivo",
            "Comprobaciones superadas",
            "Advertencias ·",
            "Correspondencia de columnas",
            "Importación bloqueada",
            "Cancelar importación",
            "errores en",
            "Mostrando",
            "importadas",
            "películas",
            "índice temporal",
            "Copiar enlace",
            "¡Copiado!",
            "Escribe para buscar",
            "Sin resultados",
            "Volver arriba");

    @Test
    @DisplayName("los scripts JavaScript no contienen literales de interfaz en español")
    void scriptsShouldNotContainHardcodedSpanishLiterals() throws IOException {
        assertThat(Files.isDirectory(JS_DIR)).isTrue();
        try (Stream<Path> stream = Files.walk(JS_DIR)) {
            List<Path> jsFiles = stream.filter(p -> p.toString().endsWith(".js")).toList();
            assertThat(jsFiles).isNotEmpty();

            for (Path jsFile : jsFiles) {
                String content = Files.readString(jsFile, StandardCharsets.UTF_8);
                for (String forbidden : FORBIDDEN_JS_LITERALS) {
                    assertThat(content)
                            .as("El archivo %s no debe contener el literal '%s'", jsFile.getFileName(), forbidden)
                            .doesNotContain(forbidden);
                }
            }
        }
    }

    @Test
    @DisplayName("todas las plantillas HTML existen y usan atributos Thymeleaf para textos visibles")
    void templatesShouldExistAndUseThymeleafAttributes() throws IOException {
        assertThat(Files.isDirectory(TEMPLATES_DIR)).isTrue();
        try (Stream<Path> stream = Files.walk(TEMPLATES_DIR)) {
            List<Path> htmlFiles = stream.filter(p -> p.toString().endsWith(".html")).toList();
            assertThat(htmlFiles).isNotEmpty();

            for (Path htmlFile : htmlFiles) {
                String content = Files.readString(htmlFile, StandardCharsets.UTF_8);
                assertThat(content)
                        .as("La plantilla %s debe contener atributos Thymeleaf (th:)", htmlFile.getFileName())
                        .contains("th:");
            }
        }
    }
}

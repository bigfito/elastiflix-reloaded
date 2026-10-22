package com.bigfito.elastiflix.web.locale;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ninguna plantilla ni script conserva textos de la interfaz escritos en español.
 *
 * <p>En una plantilla, el texto de un elemento con {@code th:text}, {@code th:utext}, {@code th:replace} o
 * {@code th:insert} es solo el ejemplo de la plantilla natural: Thymeleaf lo reemplaza. Todo lo demás (texto
 * suelto y los atributos {@code aria-label}, {@code title}, {@code alt} y {@code placeholder} sin su versión
 * {@code th:}) llega tal cual a la persona y no puede estar en español. En un script, ningún literal puede tener
 * forma de texto en español; los comentarios no cuentan.</p>
 */
class TemplatesAndScriptsI18nTest {

    private static final Path TEMPLATES_DIR = Path.of("src/main/resources/templates");
    private static final Path JS_DIR = Path.of("src/main/resources/static/js");

    /** Letras propias del español o palabras frecuentes que no existen en inglés. */
    static final Pattern SPANISH = Pattern.compile(
            "[áéíóúñ¿¡]|\\b(de|del|la|las|los|una|para|por|con|sin|que|y)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern REPLACED_ELEMENT = Pattern.compile(
            "<([\\w:]+)(\\s[^>]*?\\bth:(?:u?text|replace|insert)=[^>]*)>(?:(?!<\\1[\\s>]).)*?</\\1>",
            Pattern.DOTALL);
    private static final Pattern SKIPPED_BLOCK = Pattern.compile(
            "<!--.*?-->|<script\\b.*?</script>|<style\\b.*?</style>", Pattern.DOTALL);
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern ATTRIBUTE = Pattern.compile(
            "(?<![\\w:-])(aria-label|title|alt|placeholder)=\"([^\"]*)\"");
    private static final Pattern JS_LITERAL = Pattern.compile("\"((?:[^\"\\\\\\n]|\\\\.)*)\"|'((?:[^'\\\\\\n]|\\\\.)*)'");
    private static final Pattern JS_COMMENT = Pattern.compile("/\\*.*?\\*/|(?m)^\\s*//.*$", Pattern.DOTALL);

    @Test
    @DisplayName("las plantillas no muestran textos fijos en español, ni en el texto ni en atributos accesibles")
    void templatesShouldNotShowFixedSpanishText() throws IOException {
        List<String> found = new ArrayList<>();
        for (Path template : files(TEMPLATES_DIR, ".html")) {
            found.addAll(fixedSpanish(template.getFileName().toString(), read(template)));
        }

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("los scripts no llevan literales de interfaz en español, ni como respaldo de un data-*")
    void scriptsShouldNotContainSpanishLiterals() throws IOException {
        List<String> found = new ArrayList<>();
        for (Path script : files(JS_DIR, ".js")) {
            String code = JS_COMMENT.matcher(read(script)).replaceAll("");
            Matcher literal = JS_LITERAL.matcher(code);
            while (literal.find()) {
                String text = literal.group(1) != null ? literal.group(1) : literal.group(2);
                if (SPANISH.matcher(text).find()) {
                    found.add(script.getFileName() + ": " + text);
                }
            }
        }

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("la revisión de plantillas detecta un texto suelto y un aria-label en español")
    void shouldDetectFixedSpanishText() {
        String html = "<p th:text=\"#{a}\">Texto <span th:text=\"${b}\">de</span> ejemplo</p>"
                + "<p>Mostrando 0 de 0 errores.</p>"
                + "<div aria-label=\"Progreso de la importación\"></div>"
                + "<div th:attr=\"aria-label=#{b}\" aria-label=\"Progreso de la carga\"></div>";

        assertThat(fixedSpanish("prueba.html", html)).containsExactlyInAnyOrder(
                "prueba.html: Mostrando 0 de 0 errores.",
                "prueba.html: aria-label=Progreso de la importación");
    }

    static List<String> fixedSpanish(String name, String html) {
        String visible = SKIPPED_BLOCK.matcher(html).replaceAll("");
        List<String> found = new ArrayList<>();
        Matcher tag = TAG.matcher(visible);
        while (tag.find()) {
            found.addAll(attributes(name, tag.group()));
        }
        String previous;
        do {
            previous = visible;
            visible = REPLACED_ELEMENT.matcher(visible).replaceAll(" ");
        } while (!visible.equals(previous));
        for (String text : TAG.matcher(visible).replaceAll("\n").split("\n")) {
            if (!text.isBlank() && SPANISH.matcher(text).find()) {
                found.add(name + ": " + text.strip());
            }
        }
        return found;
    }

    private static List<String> attributes(String name, String tag) {
        List<String> found = new ArrayList<>();
        Matcher attribute = ATTRIBUTE.matcher(tag);
        while (attribute.find()) {
            String attr = attribute.group(1);
            boolean dynamic = tag.contains("th:" + attr + "=") || tag.matches("(?s).*th:attr=\"[^\"]*" + attr + "=.*");
            if (!dynamic && SPANISH.matcher(attribute.group(2)).find()) {
                found.add(name + ": " + attr + "=" + attribute.group(2));
            }
        }
        return found;
    }

    private static List<Path> files(Path dir, String extension) throws IOException {
        assertThat(Files.isDirectory(dir)).isTrue();
        try (Stream<Path> stream = Files.walk(dir)) {
            List<Path> files = stream.filter(path -> path.toString().endsWith(extension)).toList();
            assertThat(files).isNotEmpty();
            return files;
        }
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}

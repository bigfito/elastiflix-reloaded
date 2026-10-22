package com.bigfito.elastiflix.web.about;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.file.FileKind;
import com.bigfito.elastiflix.ingest.mapping.MappingValidator;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;
import com.bigfito.elastiflix.ingest.mapping.VectorFields;
import com.bigfito.elastiflix.ingest.service.MovieReaderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las columnas «Obligatorio en .json» y «Obligatorio en .csv» dicen lo mismo que el validador real (Requerimiento 6).
 */
class CatalogRequirementsTest {

    private static final Set<String> REQUIRED = Set.of("id", "title", "plot");
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @TempDir
    private Path folder;

    @Test
    @DisplayName("las filas «Sí» son id, title y plot, en las dos columnas")
    void shouldMarkOnlyTheRequiredFields() {
        Set<String> json = new TreeSet<>();
        Set<String> csv = new TreeSet<>();
        for (CatalogField field : fields()) {
            if (field.jsonKey().equals(CatalogField.REQUIRED_KEY)) {
                json.add(field.name());
            }
            if (field.csvKey().equals(CatalogField.REQUIRED_KEY)) {
                csv.add(field.name());
            }
        }

        assertThat(json).isEqualTo(REQUIRED);
        assertThat(csv).isEqualTo(REQUIRED);
    }

    @Test
    @DisplayName("las filas «se ignora» son los vectores que el validador ignora")
    void shouldMarkTheVectorsAsIgnored() {
        Set<String> ignored = new TreeSet<>();
        for (CatalogField field : fields()) {
            if (field.jsonKey().endsWith(".ignored")) {
                ignored.add(field.name());
                assertThat(field.csvKey()).endsWith(".ignored");
            }
        }

        assertThat(ignored).isEqualTo(VectorFields.NAMES);
    }

    @Test
    @DisplayName("un .json con solo id, title y plot se acepta: los demás campos son opcionales")
    void shouldAcceptAJsonWithOnlyTheRequiredFields() {
        ValidationReport report = validate(movie());

        assertThat(report.accepted()).isTrue();
    }

    @ParameterizedTest(name = "un .json sin {0} da EFX-ING-011")
    @ValueSource(strings = {"id", "title", "plot"})
    @DisplayName("sin uno de los obligatorios, el .json se rechaza con EFX-ING-011")
    void shouldRejectAJsonWithoutARequiredField(String missing) {
        ObjectNode movie = movie();
        movie.remove(missing);

        ValidationReport report = validate(movie);

        assertThat(report.errors()).extracting(ValidationIssue::code).contains("EFX-ING-011");
    }

    @ParameterizedTest(name = "un .csv sin la columna {0} da EFX-ING-006")
    @ValueSource(strings = {"id", "title", "plot"})
    @DisplayName("sin una de las columnas obligatorias, el .csv se rechaza con EFX-ING-006")
    void shouldRejectACsvWithoutARequiredColumn(String missing) throws IOException {
        List<String> columns = new ArrayList<>(List.of("id", "title", "plot"));
        columns.remove(missing);
        Path file = Files.writeString(folder.resolve("peliculas.csv"),
                String.join(",", columns) + "\n" + String.join(",", List.of("1", "A", "B").subList(0, columns.size()))
                        + "\n", StandardCharsets.UTF_8);

        ValidationReport report = new MovieReaderFactory(new IndexMapping())
                .load(FileKind.CSV, file, CsvSettings.withHeader()).report();

        assertThat(report.errors()).extracting(ValidationIssue::code).contains("EFX-ING-006");
    }

    @Test
    @DisplayName("un .csv con solo las columnas id, title y plot se lee sin errores")
    void shouldReadACsvWithOnlyTheRequiredColumns() throws IOException {
        Path file = Files.writeString(folder.resolve("peliculas.csv"), "id,title,plot\n1,A,B\n", StandardCharsets.UTF_8);

        ValidationReport report = new MovieReaderFactory(new IndexMapping())
                .load(FileKind.CSV, file, CsvSettings.withHeader()).report();

        assertThat(report.errors()).isEmpty();
    }

    private static ValidationReport validate(ObjectNode movie) {
        List<JsonNode> documents = new ArrayList<>();
        documents.add(movie);
        return new MappingValidator().validate(documents, Set.of()).report();
    }

    private static ObjectNode movie() {
        return MAPPER.createObjectNode().put("id", "1").put("title", "Título").put("plot", "Una sinopsis");
    }

    private static List<CatalogField> fields() {
        List<CatalogField> fields = new ArrayList<>();
        for (CatalogGroup group : CatalogFields.GROUPS) {
            fields.addAll(group.fields());
        }
        return fields;
    }
}

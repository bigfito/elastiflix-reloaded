package com.bigfito.elastiflix.web.ingest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.ingest.csv.CsvColumnRow;
import com.bigfito.elastiflix.ingest.csv.CsvColumnRows;
import com.bigfito.elastiflix.ingest.csv.CsvMovieReader;
import com.bigfito.elastiflix.ingest.csv.CsvRead;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.mapping.IngestPhrase;
import com.bigfito.elastiflix.ingest.mapping.MappingValidator;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.web.locale.SupportedLocale;
import com.bigfito.elastiflix.web.locale.UiMessages;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lo que escribe la carga sale en inglés completo con {@code lang=en}, y en español sin cambios.
 *
 * <p>Usa la salida real de {@link MappingValidator}, {@link CsvMovieReader} y {@link CsvColumnRows}, no textos
 * escritos a mano.</p>
 */
class IngestTranslatorTest {

    private static final Pattern SPANISH = Pattern.compile(
            "[áéíóúñ¿¡]|\\b(un|una|el|la|los|las|de|en|sin|se|y|o|vino|fila|filas|celdas|columna|campo|"
                    + "llamad[oa]|vacía|texto|número|lista|objeto|archivo|encabezado|registros?|leídos|ningún)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern MARKER = Pattern.compile("\\{\\d}");

    private final UiMessages messages = UiMessages.defaultCatalog();
    private final IngestTranslator translator = new IngestTranslator(messages);

    @TempDir
    private Path folder;

    @ParameterizedTest(name = "{0}")
    @EnumSource(IngestPhrase.class)
    @DisplayName("cada frase del vocabulario tiene clave en los dos catálogos, con los mismos marcadores")
    void shouldTranslateEveryPhrase(IngestPhrase phrase) {
        String key = "ingest.phrase." + phrase.key();
        String spanish = messages.getFor(SupportedLocale.ES, key);
        String english = messages.getFor(SupportedLocale.EN, key);

        assertThat(spanish).as("el español del catálogo es la frase de la carga").isEqualTo(phrase.template());
        assertThat(markers(english)).isEqualTo(markers(spanish));
        assertThat(SPANISH.matcher(english).find()).as("«%s» no debe tener español", english).isFalse();
    }

    @Test
    @DisplayName("los errores y advertencias reales de un JSON inválido salen en inglés, y en español no cambian")
    void shouldTranslateRealJsonIssues() {
        String json = "[{\"id\":\"1\",\"title\":\"T\",\"plot\":\"P\",\"runtime\":\"x\",\"budget\":\"z\","
                + "\"genres\":\"Drama\",\"release_date\":\"19-12-1997\",\"adult\":\"si\",\"keywords\":[{\"id\":\"a\"},\"\"],"
                + "\"belongs_to_collection\":5,\"cast\":[\"\"],\"extra\":1,\"tagline\":\"\",\"_id\":2},"
                + "{\"id\":\"1\",\"title\":\"\",\"plot\":\"p\",\"production_companies\":7},"
                + "5, {\"title\":\"x\",\"spoken_languages\":[1]}]";
        List<JsonNode> documents = new ArrayList<>();
        MovieJsonMapper.create().readTree(json).forEach(documents::add);
        List<ValidationIssue> issues = new MappingValidator().validate(documents, Set.of()).report().issues();

        assertThat(issues).hasSizeGreaterThan(10);
        assertNoSpanish(translator.issues(issues, SupportedLocale.EN));
        assertThat(translator.issues(issues, SupportedLocale.ES)).isEqualTo(issues);
    }

    @Test
    @DisplayName("el encabezado, las filas y las columnas reales de un CSV inválido salen en inglés")
    void shouldTranslateRealCsvIssuesAndColumns() throws IOException {
        Path file = folder.resolve("peliculas.csv");
        Files.writeString(file, "id,title,plot,plot_256,foo,foo,,runtime,genres\n"
                + "1,T,P,,a,b,c,90,\"[\"\"Drama\"\"]\"\n2,T\n3,T,P,,a,b,c,,\n", StandardCharsets.UTF_8);
        CsvRead read = new CsvMovieReader(new IndexMapping()).read(file, new CsvSettings(CsvSettings.COMMA, true));
        Path valid = folder.resolve("columnas.csv");
        Files.writeString(valid, "id,title,plot,plot_256,runtime,genres,_id,foo\n1,T,P,,,,1,\n2,T,P,,90,,2,\n",
                StandardCharsets.UTF_8);
        CsvRead columns = new CsvMovieReader(new IndexMapping()).read(valid, new CsvSettings(CsvSettings.COMMA, true));
        List<CsvColumnRow> rows = new CsvColumnRows(new IndexMapping()).of(columns.columns(), columns.rowCount());

        assertThat(read.report().issues()).isNotEmpty();
        assertNoSpanish(translator.issues(read.report().issues(), SupportedLocale.EN));
        assertNoSpanish(translator.issues(columns.report().issues(), SupportedLocale.EN));
        for (CsvColumnRow row : rows) {
            assertNoSpanish(List.of(translator.text(row.field(), SupportedLocale.EN),
                    translator.text(row.content(), SupportedLocale.EN),
                    translator.text(row.status(), SupportedLocale.EN)));
        }
    }

    @Test
    @DisplayName("los números de una frase se agrupan por miles según el idioma")
    void shouldGroupThousandsByLanguage() {
        String read = IngestPhrase.CHECK_READ.text(1234);
        String empty = IngestPhrase.EMPTY_IN_ROWS.text(IngestPhrase.KIND_TEXT.text(), 4960);

        assertThat(translator.text(read, SupportedLocale.EN)).isEqualTo("1,234 records read, with id, title and plot.");
        assertThat(translator.text(read, SupportedLocale.ES)).isEqualTo(read);
        assertThat(translator.text(empty, SupportedLocale.EN)).isEqualTo("Text · empty in 4,960 rows");
    }

    @Test
    @DisplayName("un dato del archivo no se traduce")
    void shouldKeepData() {
        assertThat(translator.text("Titanic", SupportedLocale.EN)).isEqualTo("Titanic");
        assertThat(translator.text(null, Locale.ENGLISH)).isEmpty();
    }

    private static void assertNoSpanish(List<?> values) {
        for (Object value : values) {
            String text = value.toString();
            Matcher spanish = SPANISH.matcher(text);
            assertThat(spanish.find()).as("«%s» no debe tener español", text).isFalse();
        }
    }

    private static Set<String> markers(String text) {
        Set<String> found = new TreeSet<>();
        Matcher marker = MARKER.matcher(text);
        while (marker.find()) {
            found.add(marker.group());
        }
        return found;
    }
}

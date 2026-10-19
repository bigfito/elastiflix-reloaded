package com.bigfito.elastiflix.search.query;

import java.time.LocalDate;
import java.util.List;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.SearchValidationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Una prueba por cada regla de {@code EFX-SRCH-*}.
 */
class SearchQueryValidatorTest {

    private final SearchQueryValidator validator = new SearchQueryValidator();

    @Test
    @DisplayName("una consulta completa no se rechaza")
    void shouldAcceptWhenTheQueryFollowsTheRules() {
        assertThatCode(() -> validator.validate(fullText("titanic"))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(vector("un barco", 100, 200))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(hybrid("toy story"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("el texto vacío usa la frase del mockup")
    void shouldRejectWhenTheTextIsBlank() {
        assertCode(fullText("   "), ErrorCode.EMPTY_SEARCH_TEXT,
                "Escribe qué película buscas antes de pulsar Buscar.");
    }

    @Test
    @DisplayName("k menor que 1 usa la frase del mockup")
    void shouldRejectWhenKIsBelowOne() {
        assertCode(vector("tigre", 0, 200), ErrorCode.K_TOO_SMALL,
                "k debe ser un entero mayor o igual que 1.");
    }

    @Test
    @DisplayName("num_candidates menor que k usa la frase del mockup")
    void shouldRejectWhenCandidatesAreBelowK() {
        assertCode(vector("tigre", 100, 50), ErrorCode.CANDIDATES_BELOW_K,
                "num_candidates debe ser mayor o igual que k.");
    }

    @Test
    @DisplayName("num_candidates por encima de 10000 usa la frase del mockup")
    void shouldRejectWhenCandidatesExceedTheMaximum() {
        assertCode(vector("tigre", 100, 10_001), ErrorCode.CANDIDATES_TOO_LARGE,
                "num_candidates debe ser un entero de máximo 10 000.");
    }

    @Test
    @DisplayName("un campo junto con multi-match no se acepta")
    void shouldRejectWhenMultiMatchCarriesAField() {
        SearchQuery query = new SearchQuery(
                "cielo",
                SearchMode.FULL_TEXT,
                new FullTextSettings(MatchType.MULTI_MATCH, TextField.TITLE, TextOperator.OR),
                null,
                SearchFilters.none(),
                1,
                SearchQuery.PAGE_25);
        assertCode(query, ErrorCode.FIELD_WITH_MULTI_MATCH, "El campo no se usa con multi-match.");
    }

    @Test
    @DisplayName("25, 50 y 100 pasan la validación")
    void shouldAcceptTheThreePageSizes() {
        assertThatCode(() -> validator.validate(sized(SearchQuery.PAGE_25))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(sized(SearchQuery.PAGE_50))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(sized(SearchQuery.PAGE_100))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("el tamaño de página solo puede ser 25, 50 o 100")
    void shouldRejectWhenThePageSizeIsNotAllowed() {
        SearchQuery query = new SearchQuery(
                "titanic", SearchMode.FULL_TEXT, matchTitle(), null, SearchFilters.none(), 1, 30);
        assertCode(query, ErrorCode.PAGE_SIZE_NOT_ALLOWED, "El tamaño de página debe ser 25, 50 o 100.");
    }

    @Test
    @DisplayName("Desde posterior a Hasta usa la frase del mockup")
    void shouldRejectWhenTheStartDateIsAfterTheEndDate() {
        SearchFilters filters = new SearchFilters(
                List.of(), List.of(), List.of(), List.of(),
                LocalDate.parse("2020-01-02"), LocalDate.parse("2020-01-01"));
        SearchQuery query = new SearchQuery(
                "titanic", SearchMode.FULL_TEXT, matchTitle(), null, filters, 1, SearchQuery.PAGE_25);
        assertCode(query, ErrorCode.DATE_RANGE_REVERSED,
                "La fecha «Desde» no puede ser posterior a «Hasta».");
    }

    @Test
    @DisplayName("full-text no pasa de 10000 resultados recorridos")
    void shouldRejectWhenFullTextLeavesTheResultWindow() {
        SearchQuery query = new SearchQuery(
                "titanic", SearchMode.FULL_TEXT, matchTitle(), null,
                SearchFilters.none(), 401, SearchQuery.PAGE_25);
        assertCode(query, ErrorCode.FULL_TEXT_WINDOW_EXCEEDED,
                "La página se sale de las 10 000 coincidencias que se pueden recorrer.");
    }

    @Test
    @DisplayName("la última página de full-text, antes de 10000, se acepta")
    void shouldAcceptTheLastFullTextPageInsideTheWindow() {
        SearchQuery query = new SearchQuery(
                "titanic", SearchMode.FULL_TEXT, matchTitle(), null,
                SearchFilters.none(), 400, SearchQuery.PAGE_25);
        assertThatCode(() -> validator.validate(query)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("una página parcial dentro de k se acepta")
    void shouldAcceptAPartialVectorPageInsideK() {
        assertThatCode(() -> validator.validate(vectorPage(10, 1, SearchQuery.PAGE_25)))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(vectorPage(150, 2, SearchQuery.PAGE_100)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("vectorial rechaza la página que empieza en k o después")
    void shouldRejectWhenTheVectorPageStartsAtK() {
        assertCode(vectorPage(10, 2, SearchQuery.PAGE_25), ErrorCode.KNN_WINDOW_EXCEEDED,
                "La página se sale de los k resultados de este modo.");
    }

    @Test
    @DisplayName("un modo sin sus ajustes se rechaza")
    void shouldRejectWhenTheModeHasNoSettings() {
        SearchQuery query = new SearchQuery(
                "titanic", SearchMode.VECTOR, null, null, SearchFilters.none(), 1, SearchQuery.PAGE_25);
        assertCode(query, ErrorCode.MISSING_MODE_SETTINGS, "Faltan los ajustes de este modo.");
    }

    @Test
    @DisplayName("un vector de consulta sin campo se rechaza como ajustes faltantes")
    void shouldRejectWhenTheKnnSettingsHaveNoField() {
        // given
        SearchQuery query = new SearchQuery("titanic", SearchMode.VECTOR, null,
                new KnnSettings(null, 100, 200), SearchFilters.none(), 1, SearchQuery.PAGE_25);

        // then
        assertCode(query, ErrorCode.MISSING_MODE_SETTINGS, "Faltan los ajustes de este modo.");
    }

    @Test
    @DisplayName("la página empieza en 1")
    void shouldRejectWhenThePageIsBelowOne() {
        SearchQuery query = new SearchQuery(
                "titanic", SearchMode.FULL_TEXT, matchTitle(), null, SearchFilters.none(), 0,
                SearchQuery.PAGE_25);
        assertCode(query, ErrorCode.PAGE_TOO_SMALL, "La página debe ser un entero mayor o igual que 1.");
    }

    private void assertCode(SearchQuery query, ErrorCode code, String whatHappened) {
        assertThatThrownBy(() -> validator.validate(query))
                .isInstanceOf(SearchValidationException.class)
                .satisfies(thrown -> {
                    SearchValidationException error = (SearchValidationException) thrown;
                    assertThat(error.errorCode()).isEqualTo(code);
                    assertThat(error).hasMessageContaining(whatHappened);
                });
    }

    private static SearchQuery sized(int pageSize) {
        return new SearchQuery("titanic", SearchMode.FULL_TEXT, matchTitle(), null,
                SearchFilters.none(), 1, pageSize);
    }

    private static SearchQuery fullText(String text) {
        return new SearchQuery(text, SearchMode.FULL_TEXT, matchTitle(), null,
                SearchFilters.none(), 1, SearchQuery.PAGE_25);
    }

    private static SearchQuery vector(String text, int k, int candidates) {
        return new SearchQuery(text, SearchMode.VECTOR, null, knn(k, candidates),
                SearchFilters.none(), 1, SearchQuery.PAGE_25);
    }

    private static SearchQuery hybrid(String text) {
        return new SearchQuery(text, SearchMode.HYBRID, matchTitle(), knn(100, 200),
                SearchFilters.none(), 1, SearchQuery.PAGE_25);
    }

    private static FullTextSettings matchTitle() {
        return new FullTextSettings(MatchType.MATCH, TextField.TITLE, TextOperator.OR);
    }

    private static KnnSettings knn(int k, int candidates) {
        return new KnnSettings(VectorField.PLOT_1024, k, candidates);
    }

    private static SearchQuery vectorPage(int k, int page, int pageSize) {
        return new SearchQuery("tigre", SearchMode.VECTOR, null, knn(k, Math.max(k, 200)),
                SearchFilters.none(), page, pageSize);
    }
}

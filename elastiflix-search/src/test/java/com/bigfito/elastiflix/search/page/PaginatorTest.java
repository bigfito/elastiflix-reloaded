package com.bigfito.elastiflix.search.page;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Última página parcial y una página que se pasa del total sin salirse de la ventana.
 */
class PaginatorTest {

    private static final String FIRST_PAGE = "1–25 de 100";
    private static final String PARTIAL_PAGE = "26–30 de 30";
    private static final String PAST_THE_TOTAL = "0–0 de 10";

    private final Paginator paginator = new Paginator();

    @Test
    @DisplayName("la primera página usa el guion largo")
    void shouldDescribeTheFirstPageWhenTheTotalFillsIt() {
        Pagination page = paginator.describe(100, 1, 25);

        assertThat(page.range()).isEqualTo(FIRST_PAGE);
        assertThat(page.pageCount()).isEqualTo(4);
        assertThat(page.range()).contains("–");
    }

    @Test
    @DisplayName("k = 30 y tamaño 25 dejan cinco resultados en la segunda página")
    void shouldEndTheRangeAtTheTotalWhenTheLastPageIsPartial() {
        List<Integer> hits = IntStream.rangeClosed(1, 30).boxed().toList();
        Pagination page = paginator.describe(hits.size(), 2, 25);

        assertThat(page.pageCount()).isEqualTo(2);
        assertThat(page.range()).isEqualTo(PARTIAL_PAGE);
        assertThat(paginator.slice(hits, 2, 25)).containsExactly(26, 27, 28, 29, 30);
    }

    @Test
    @DisplayName("una página más allá del total, pero dentro de la ventana, no es un error")
    void shouldReturnAnEmptySliceWhenThePageIsPastTheTotal() {
        Pagination page = paginator.describe(10, 2, 25);

        assertThat(page.pageCount()).isEqualTo(1);
        assertThat(page.total()).isEqualTo(10);
        assertThat(page.range()).isEqualTo(PAST_THE_TOTAL);
        assertThat(paginator.slice(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 2, 25)).isEmpty();
    }

    @Test
    @DisplayName("50 y 100 también parten la lista")
    void shouldSliceWhenThePageHasFiftyOrOneHundredRows() {
        List<Integer> hits = IntStream.rangeClosed(1, 100).boxed().toList();

        assertThat(paginator.slice(hits, 1, 50)).hasSize(50);
        assertThat(paginator.describe(hits.size(), 1, 50).pageCount()).isEqualTo(2);
        assertThat(paginator.slice(hits, 1, 100)).hasSize(100);
        assertThat(paginator.describe(hits.size(), 1, 100).pageCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("las páginas no pasan de la ventana aunque el total sea mayor")
    void shouldCountPagesInsideTheWindowWhenTheTotalIsLarger() {
        Pagination page = paginator.describe(12_000, 1, 25, 10_000);

        assertThat(page.pageCount()).isEqualTo(400);
        assertThat(page.total()).isEqualTo(12_000);
    }

    @Test
    @DisplayName("sin resultados hay cero páginas")
    void shouldReportZeroPagesWhenTheTotalIsZero() {
        Pagination page = paginator.describe(0, 1, 25);

        assertThat(page.pageCount()).isZero();
        assertThat(page.range()).isEqualTo("0–0 de 0");
    }
}

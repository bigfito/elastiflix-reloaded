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

    private final Paginator paginator = new Paginator();

    @Test
    @DisplayName("la primera página va de la película 1 a la 25")
    void shouldDescribeTheFirstPageWhenTheTotalFillsIt() {
        Pagination page = paginator.describe(100, 1, 25, 100);

        assertThat(page.first()).isEqualTo(1);
        assertThat(page.last()).isEqualTo(25);
        assertThat(page.pageCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("la página 40 de 25 va de la película 976 a la 1 000")
    void shouldDescribeAPageDeepInsideALargeTotal() {
        Pagination page = paginator.describe(1234, 40, 25, 10_000);

        assertThat(page.first()).isEqualTo(976);
        assertThat(page.last()).isEqualTo(1000);
        assertThat(page.total()).isEqualTo(1234);
    }

    @Test
    @DisplayName("k = 30 y tamaño 25 dejan cinco resultados en la segunda página")
    void shouldEndTheRangeAtTheTotalWhenTheLastPageIsPartial() {
        List<Integer> hits = IntStream.rangeClosed(1, 30).boxed().toList();
        Pagination page = paginator.describe(hits.size(), 2, 25, hits.size());

        assertThat(page.pageCount()).isEqualTo(2);
        assertThat(page.first()).isEqualTo(26);
        assertThat(page.last()).isEqualTo(30);
        assertThat(paginator.slice(hits, 2, 25)).containsExactly(26, 27, 28, 29, 30);
    }

    @Test
    @DisplayName("una página más allá del total, pero dentro de la ventana, no es un error")
    void shouldReturnAnEmptySliceWhenThePageIsPastTheTotal() {
        Pagination page = paginator.describe(10, 2, 25, 10);

        assertThat(page.pageCount()).isEqualTo(1);
        assertThat(page.total()).isEqualTo(10);
        assertThat(page.first()).isZero();
        assertThat(page.last()).isZero();
        assertThat(paginator.slice(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 2, 25)).isEmpty();
    }

    @Test
    @DisplayName("50 y 100 también parten la lista")
    void shouldSliceWhenThePageHasFiftyOrOneHundredRows() {
        List<Integer> hits = IntStream.rangeClosed(1, 100).boxed().toList();

        assertThat(paginator.slice(hits, 1, 50)).hasSize(50);
        assertThat(paginator.describe(hits.size(), 1, 50, hits.size()).pageCount()).isEqualTo(2);
        assertThat(paginator.slice(hits, 1, 100)).hasSize(100);
        assertThat(paginator.describe(hits.size(), 1, 100, hits.size()).pageCount()).isEqualTo(1);
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
        Pagination page = paginator.describe(0, 1, 25, 0);

        assertThat(page.pageCount()).isZero();
        assertThat(page.first()).isZero();
        assertThat(page.last()).isZero();
    }
}

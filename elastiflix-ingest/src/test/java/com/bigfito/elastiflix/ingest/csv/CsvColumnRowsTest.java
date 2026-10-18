package com.bigfito.elastiflix.ingest.csv;

import java.util.List;

import com.bigfito.elastiflix.core.mapping.IndexMapping;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La tabla «Correspondencia de columnas» tiene los cuatro estados del mockup.
 */
class CsvColumnRowsTest {

    private final CsvColumnRows rows = new CsvColumnRows(new IndexMapping());

    @Test
    @DisplayName("asignada, se ignora, sin datos y no existe en el mapping")
    void shouldGiveEachColumnItsStatus() {
        List<CsvColumnRow> table = rows.of(List.of(
                new CsvColumn("_id", 4322),
                new CsvColumn("belongs_to_collection", 1231),
                new CsvColumn("plot_1024", 4322),
                new CsvColumn("user_score", 0),
                new CsvColumn("director", 512),
                new CsvColumn("genres", 4322),
                new CsvColumn("rating", 3905)), 4322);

        assertThat(table).containsExactly(
                new CsvColumnRow("_id", "(id del documento)", "Metadato; tiene que ser igual a id", "Asignada"),
                new CsvColumnRow("belongs_to_collection", "belongs_to_collection",
                        "Objeto JSON anidado · vacía en 3 091 filas", "Asignada"),
                new CsvColumnRow("plot_1024", "plot_1024", "Se ignora · se generará desde plot", "Se ignora"),
                new CsvColumnRow("user_score", "user_score", "Vacía en todas las filas", "Sin datos"),
                new CsvColumnRow("director", "— Sin campo en el índice —", "Texto", "No existe en el mapping"),
                new CsvColumnRow("genres", "genres", "Lista JSON de textos", "Asignada"),
                new CsvColumnRow("rating", "rating", "Texto · vacía en 417 filas", "Asignada"));
    }
}

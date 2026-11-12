package com.bigfito.elastiflix.core.mapping;

import com.bigfito.elastiflix.core.error.ConfigurationException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El mapping empaquetado describe el catálogo de películas.
 */
class IndexMappingTest {

    private static final int FIELD_COUNT = 34;
    private static final int PLOT_512_DIMENSIONS = 512;

    @Test
    @DisplayName("expone 34 campos, la fecha yyyy-MM-dd y plot_512 de 512 dimensiones")
    void shouldDescribeTheCatalogWhenMappingIsPackaged() {
        IndexMapping mapping = new IndexMapping();

        assertThat(mapping.fields()).hasSize(FIELD_COUNT);
        assertThat(field(mapping, "release_date").format()).isEqualTo("yyyy-MM-dd");
        assertThat(field(mapping, "plot_512").dimensions()).isEqualTo(PLOT_512_DIMENSIONS);
        assertThat(field(mapping, "plot_256").dimensions()).isEqualTo(256);
        assertThat(field(mapping, "plot_1024").dimensions()).isEqualTo(1024);
        assertThat(field(mapping, "belongs_to_collection").nestedFields())
                .containsExactly("backdrop_path", "id", "name", "poster_path");
        assertThat(field(mapping, "keywords").nestedFields()).containsExactly("id", "name");
        assertThat(field(mapping, "poster_path").type()).isEqualTo("keyword");
        assertThat(field(mapping, "enrichment").type()).isEqualTo("object");
        assertThat(field(mapping, "enrichment").nestedFields()).containsExactly(
                "backdrop_path", "belongs_to_collection", "budget", "cast", "genres", "homepage", "imdb_id",
                "keywords", "overview", "plot", "poster_path", "production_companies", "production_countries",
                "rating", "revenue", "runtime", "spoken_languages", "tagline", "user_score", "vote_average",
                "vote_count");
        assertThat(mapping.mappings()).contains("\"plot_512\"");
        assertThat(IndexMapping.class.getResource("/META-INF/spring.factories")).isNotNull();
    }

    @Test
    @DisplayName("un recurso ausente se reporta con EFX-CFG-003")
    void shouldReportTheCodeWhenMappingIsMissing() {
        assertThatThrownBy(() -> IndexMapping.read(null))
                .isInstanceOf(ConfigurationException.class)
                .extracting(thrown -> ((ConfigurationException) thrown).errorCode())
                .isEqualTo(ErrorCode.MAPPING_NOT_PACKAGED);
    }

    /** Busca el campo con la misma firma que usa ingest: la lista de {@code fields()}. */
    private static FieldMapping field(IndexMapping mapping, String name) {
        return mapping.fields().stream()
                .filter(field -> field.name().equals(name))
                .findFirst()
                .orElseThrow();
    }
}

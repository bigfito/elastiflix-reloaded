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

    private static final int FIELD_COUNT = 33;
    private static final int PLOT_512_DIMENSIONS = 512;

    @Test
    @DisplayName("expone 33 campos, la fecha yyyy-MM-dd y plot_512 de 512 dimensiones")
    void shouldDescribeTheCatalogWhenMappingIsPackaged() {
        IndexMapping mapping = new IndexMapping();

        assertThat(mapping.fields()).hasSize(FIELD_COUNT);
        assertThat(mapping.field("release_date").format()).isEqualTo("yyyy-MM-dd");
        assertThat(mapping.field("plot_512").dimensions()).isEqualTo(PLOT_512_DIMENSIONS);
        assertThat(mapping.field("plot_256").dimensions()).isEqualTo(256);
        assertThat(mapping.field("plot_1024").dimensions()).isEqualTo(1024);
        assertThat(mapping.field("belongs_to_collection").nestedFields())
                .containsExactly("backdrop_path", "id", "name", "poster_path");
        assertThat(mapping.field("keywords").nestedFields()).containsExactly("id", "name");
        assertThat(mapping.document()).contains("\"plot_512\"");
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
}

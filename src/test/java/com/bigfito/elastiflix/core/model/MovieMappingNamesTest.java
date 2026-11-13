package com.bigfito.elastiflix.core.model;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.mapping.FieldMapping;
import com.bigfito.elastiflix.core.mapping.IndexMapping;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Con el mapping estricto, Elasticsearch rechaza un campo que no declara: el JSON de una película tiene que usar
 * exactamente los nombres del mapping.
 */
class MovieMappingNamesTest {

    private final JsonMapper mapper = MovieJsonMapper.create();
    private final Map<String, FieldMapping> fields = new IndexMapping().fields().stream()
            .collect(Collectors.toMap(FieldMapping::name, Function.identity()));

    @Test
    @DisplayName("una película se escribe solo con los campos de primer nivel que el mapping declara")
    void shouldWriteOnlyTopLevelFieldsOfTheMapping() {
        assertThat(mapper.valueToTree(Movie.identified("1")).propertyNames())
                .containsExactlyInAnyOrderElementsOf(fields.keySet());
    }

    @Test
    @DisplayName("la colección y la palabra clave se escriben con los subcampos que el mapping declara")
    void shouldWriteOnlyNestedFieldsOfTheMapping() {
        assertThat(mapper.valueToTree(new MovieCollection(null, null, null, null)).propertyNames())
                .containsExactlyInAnyOrderElementsOf(fields.get("belongs_to_collection").nestedFields());
        assertThat(mapper.valueToTree(new MovieKeyword(null, null)).propertyNames())
                .containsExactlyInAnyOrderElementsOf(fields.get("keywords").nestedFields());
    }
}

package com.bigfito.elastiflix.ingest.mapping;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bigfito.elastiflix.core.mapping.FieldMapping;
import com.bigfito.elastiflix.core.mapping.IndexMapping;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las reglas del validador siguen al mapping empaquetado: si el mapping cambia un campo, esta prueba falla
 * antes de que una carga acepte algo que Elasticsearch rechaza.
 */
class FieldRulesMappingTest {

    /** Formas que admite cada tipo del mapping; {@code keyword} es un texto o una lista de textos. */
    private static final Map<String, Set<FieldKind>> KINDS_BY_TYPE = Map.of(
            "text", Set.of(FieldKind.TEXT),
            "keyword", Set.of(FieldKind.KEYWORD, FieldKind.TEXT_LIST),
            "boolean", Set.of(FieldKind.BOOLEAN),
            "integer", Set.of(FieldKind.INTEGER),
            "float", Set.of(FieldKind.DECIMAL),
            "date", Set.of(FieldKind.DATE),
            "dense_vector", Set.of(FieldKind.VECTOR),
            "nested", Set.of(FieldKind.COLLECTION, FieldKind.KEYWORDS),
            "object", Set.of(FieldKind.TAGS));

    private final List<FieldMapping> fields = new IndexMapping().fields();

    @Test
    @DisplayName("el validador conoce exactamente los campos del mapping")
    void shouldKnowTheSameFieldsAsTheMapping() {
        Set<String> mapped = new HashSet<>();
        for (FieldMapping field : fields) {
            mapped.add(field.name());
        }

        assertThat(FieldRules.names()).hasSize(34).containsExactlyInAnyOrderElementsOf(mapped);
    }

    @Test
    @DisplayName("la forma de cada campo corresponde a su tipo en el mapping")
    void shouldGiveEachFieldAKindThatMatchesItsType() {
        assertThat(fields).allSatisfy(field -> assertThat(KINDS_BY_TYPE.get(field.type()))
                .as("%s (%s)", field.name(), field.type())
                .contains(FieldRules.kind(field.name())));
    }

    @Test
    @DisplayName("los subcampos de la colección, las palabras clave y la procedencia son los del mapping")
    void shouldAllowTheNestedFieldsOfTheMapping() {
        assertThat(fields).filteredOn(field -> "nested".equals(field.type()) || "object".equals(field.type()))
                .allSatisfy(field -> assertThat(allowed(field.name()))
                        .as(field.name())
                        .containsExactlyInAnyOrderElementsOf(field.nestedFields()));
        assertThat(FieldRules.kind("belongs_to_collection")).isEqualTo(FieldKind.COLLECTION);
        assertThat(FieldRules.kind("keywords")).isEqualTo(FieldKind.KEYWORDS);
        assertThat(FieldRules.kind("enrichment")).isEqualTo(FieldKind.TAGS);
    }

    private static Set<String> allowed(String name) {
        return switch (FieldRules.kind(name)) {
            case COLLECTION -> FieldRules.COLLECTION;
            case KEYWORDS -> FieldRules.KEYWORD;
            case TAGS -> FieldRules.ENRICHMENT;
            default -> Set.of();
        };
    }
}

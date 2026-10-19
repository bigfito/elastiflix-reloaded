package com.bigfito.elastiflix.core.config;

import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.metadata.BeanDescriptor;
import jakarta.validation.metadata.PropertyDescriptor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los grupos que no vienen en el entorno usan los valores del plan.
 */
class ElastiflixPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesOnly.class)
            .withPropertyValues(
                    "elastiflix.elasticsearch.url=http://127.0.0.1:9",
                    "elastiflix.elasticsearch.api-key=test-key");

    @Test
    @DisplayName("sin el resto de propiedades quedan los valores por defecto, embeddings incluidos")
    void shouldUsePlanDefaultsWhenOptionalGroupsAreMissing() {
        runner.run(context -> {
            ElastiflixProperties properties = context.getBean(ElastiflixProperties.class);
            assertThat(properties.index().alias()).isEqualTo("elastiflix-reloaded");
            assertThat(properties.inference().id()).isEqualTo(".jina-embeddings-v5-omni-small");
            assertThat(properties.ingest().batchSize()).isEqualTo(100);
            assertThat(properties.ingest().maxFileSize().toMegabytes()).isEqualTo(150);
            assertThat(properties.poster().baseUrl()).isEqualTo("https://image.tmdb.org/t/p/w342");
            assertThat(properties.bootstrap().enabled()).isFalse();
            assertThat(properties.embedding().documentInputType()).isEqualTo("ingest");
            assertThat(properties.embedding().documentPrefix()).isEmpty();
            assertThat(properties.elasticsearch().toString()).contains("apiKey=****").doesNotContain("test-key");
            assertThat(properties.toString()).doesNotContain("test-key");
        });
    }

    @Test
    @DisplayName("con solo document-prefix vacío, el input_type de documento sale del @DefaultValue")
    void shouldTakeTheDocumentInputTypeFromTheDefaultValue() {
        runner.withPropertyValues("elastiflix.embedding.document-prefix=").run(context -> {
            ElastiflixProperties.Embedding embedding = context.getBean(ElastiflixProperties.class).embedding();
            assertThat(embedding.documentInputType())
                    .isEqualTo(ElastiflixProperties.Embedding.DEFAULT_DOCUMENT_INPUT_TYPE)
                    .isEqualTo("ingest");
            assertThat(embedding.documentPrefix()).isEmpty();
        });
    }

    @Test
    @DisplayName("@Validated tiene qué validar: alias e id de inferencia son @NotBlank, en cascada desde el record")
    void shouldDeclareTheConstraintsThatValidatedChecks() {
        // given
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            // when
            BeanDescriptor root = validator.getConstraintsForClass(ElastiflixProperties.class);
            PropertyDescriptor alias = validator.getConstraintsForClass(ElastiflixProperties.Index.class)
                    .getConstraintsForProperty("alias");
            PropertyDescriptor inferenceId = validator.getConstraintsForClass(ElastiflixProperties.Inference.class)
                    .getConstraintsForProperty("id");

            // then
            assertThat(root.getConstraintsForProperty("index").isCascaded()).isTrue();
            assertThat(root.getConstraintsForProperty("inference").isCascaded()).isTrue();
            assertThat(annotationsOf(alias)).contains(NotBlank.class);
            assertThat(annotationsOf(inferenceId)).contains(NotBlank.class);
            assertThat(validator.validate(new ElastiflixProperties(null, null, null, null, null, null, null)))
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("un alias o un id de inferencia en blanco no fallan: el constructor ya los rellenó")
    void shouldFillTheBlankAliasAndInferenceIdBeforeValidating() {
        runner.withPropertyValues("elastiflix.index.alias= ", "elastiflix.inference.id= ").run(context -> {
            assertThat(context).hasNotFailed();
            ElastiflixProperties properties = context.getBean(ElastiflixProperties.class);
            assertThat(properties.index().alias()).isEqualTo("elastiflix-reloaded");
            assertThat(properties.inference().id()).isEqualTo(".jina-embeddings-v5-omni-small");
        });
    }

    private static List<Class<?>> annotationsOf(PropertyDescriptor property) {
        assertThat(property).as("la propiedad no declara restricciones").isNotNull();
        return property.getConstraintDescriptors().stream()
                .<Class<?>>map(constraint -> constraint.getAnnotation().annotationType())
                .toList();
    }

    /**
     * Solo enlaza las propiedades. No arranca el cliente de Elasticsearch.
     */
    @Configuration
    @EnableConfigurationProperties(ElastiflixProperties.class)
    static class PropertiesOnly {
    }
}

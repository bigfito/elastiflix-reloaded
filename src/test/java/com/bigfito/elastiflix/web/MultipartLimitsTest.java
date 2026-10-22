package com.bigfito.elastiflix.web;

import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.servlet.MultipartConfigElement;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.web.ingest.UploadMultipartConfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.servlet.autoconfigure.MultipartAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los límites de subida salen de {@code FileChecker.MAX_FILE_SIZE} (25 MB fijos), no del YAML ni del entorno.
 */
class MultipartLimitsTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MultipartAutoConfiguration.class))
            .withUserConfiguration(Limits.class);

    @Test
    @DisplayName("Tomcat deja responder con EFX-ING en lugar de cortar, y el YAML no repite los topes")
    void shouldKeepOnlyTheSwallowSizeWhenTheYamlIsRead() throws Exception {
        String yaml = Files.readString(Path.of("src", "main", "resources", "application.yml"));

        assertThat(yaml).contains("max-swallow-size: -1");
        assertThat(yaml).doesNotContain("max-request-size", "max-file-size", "ELASTIFLIX_INGEST_MAX_FILE_SIZE");
    }

    @Test
    @DisplayName("el archivo admite 25 MB y la petición 26 MB")
    void shouldDeriveBothLimitsFromTheFixedConstant() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(MultipartConfigElement.class);
            MultipartConfigElement element = context.getBean(MultipartConfigElement.class);
            assertThat(element.getMaxFileSize()).isEqualTo(DataSize.ofMegabytes(25).toBytes());
            assertThat(element.getMaxRequestSize()).isEqualTo(DataSize.ofMegabytes(26).toBytes());
        });
    }

    @Test
    @DisplayName("una ELASTIFLIX_INGEST_MAX_FILE_SIZE vieja en el entorno no cambia los 25 MB")
    void shouldIgnoreTheRemovedPropertyWhenAnOldEnvironmentSetsIt() {
        runner.withPropertyValues("elastiflix.ingest.max-file-size=200MB", "ELASTIFLIX_INGEST_MAX_FILE_SIZE=200MB")
                .run(context -> {
                    MultipartConfigElement element = context.getBean(MultipartConfigElement.class);
                    assertThat(element.getMaxFileSize()).isEqualTo(DataSize.ofMegabytes(25).toBytes());
                    assertThat(element.getMaxRequestSize()).isEqualTo(DataSize.ofMegabytes(26).toBytes());
                });
    }

    /** La configuración de la web que interviene: propiedades y el multipart derivado. */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ElastiflixProperties.class)
    @Import(UploadMultipartConfig.class)
    static class Limits {
    }
}

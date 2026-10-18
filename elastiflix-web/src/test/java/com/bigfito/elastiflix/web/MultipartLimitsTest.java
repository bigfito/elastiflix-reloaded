package com.bigfito.elastiflix.web;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los límites de subida de {@code PLAN.md} §7 siguen en el YAML de la web.
 */
class MultipartLimitsTest {

    @Test
    @DisplayName("el archivo, la petición y Tomcat permiten responder con EFX-ING en lugar de cortar")
    void shouldKeepTheUploadLimitsWhenTheYamlIsRead() throws Exception {
        String yaml = Files.readString(Path.of("src", "main", "resources", "application.yml"));

        assertThat(yaml).contains("max-file-size: ${elastiflix.ingest.max-file-size}");
        assertThat(yaml).contains("max-request-size: 151MB");
        assertThat(yaml).contains("max-swallow-size: -1");
    }
}

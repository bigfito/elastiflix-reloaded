package com.bigfito.elastiflix.core.config;

import java.nio.charset.StandardCharsets;

import com.bigfito.elastiflix.core.TestApplication;
import com.bigfito.elastiflix.core.error.ConfigurationException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * El arranque se detiene antes de crear el cliente cuando falta una credencial.
 */
class ElasticsearchCredentialsEnvironmentPostProcessorTest {

    @Test
    @DisplayName("con la URL vacía no arranca y el análisis pide revisar ELASTICSEARCH_URL")
    void shouldReportMissingUrlWhenUrlIsBlank() throws Exception {
        Throwable thrown = startWith("--elastiflix.elasticsearch.url=",
                "--elastiflix.elasticsearch.api-key=test-key");

        assertAnalysis(thrown, ErrorCode.MISSING_ELASTICSEARCH_URL, "Revisa ELASTICSEARCH_URL");
    }

    @Test
    @DisplayName("con la API key vacía no arranca y el análisis pide revisar ELASTICSEARCH_API_KEY")
    void shouldReportMissingApiKeyWhenApiKeyIsBlank() throws Exception {
        Throwable thrown = startWith("--elastiflix.elasticsearch.url=http://127.0.0.1:9",
                "--elastiflix.elasticsearch.api-key=");

        assertAnalysis(thrown, ErrorCode.MISSING_API_KEY, "Revisa ELASTICSEARCH_API_KEY");
    }

    private static Throwable startWith(String... args) {
        SpringApplication application = new SpringApplication(TestApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        return catchThrowable(() -> application.run(args));
    }

    private static void assertAnalysis(Throwable thrown, ErrorCode code, String actionPhrase) throws Exception {
        assertThat(thrown).isNotNull();
        assertThat(findConfiguration(thrown).errorCode()).isEqualTo(code);
        FailureAnalysis analysis = new ElasticsearchCredentialsFailureAnalyzer().analyze(thrown);
        assertThat(analysis.getDescription()).contains(code.code()).contains(actionPhrase);
        assertThat(analysis.getAction()).contains(actionPhrase);
        String factories = new ClassPathResource("META-INF/spring.factories")
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(factories).contains(ElasticsearchCredentialsEnvironmentPostProcessor.class.getName());
        assertThat(factories).contains(ElasticsearchCredentialsFailureAnalyzer.class.getName());
    }

    private static ConfigurationException findConfiguration(Throwable thrown) {
        Throwable current = thrown;
        while (current != null) {
            if (current instanceof ConfigurationException configuration) {
                return configuration;
            }
            current = current.getCause();
        }
        throw new AssertionError("no hubo ConfigurationException");
    }
}

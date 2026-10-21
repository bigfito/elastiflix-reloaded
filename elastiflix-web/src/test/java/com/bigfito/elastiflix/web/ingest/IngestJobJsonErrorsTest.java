package com.bigfito.elastiflix.web.ingest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.ingest.file.CheckedFile;
import com.bigfito.elastiflix.ingest.file.FileKind;
import com.bigfito.elastiflix.ingest.service.IngestService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Un IOException o cualquier falla con código en un endpoint de trabajos responde JSON, no una página.
 */
@WebMvcTest(controllers = IngestJobController.class, properties = {
        "elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test"})
@EnableConfigurationProperties(ElastiflixProperties.class)
@Import({IngestJobErrors.class, UploadSizeErrors.class, IngestLimits.class, IngestTranslator.class})
class IngestJobJsonErrorsTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IngestService ingest;

    @MockitoBean
    private UploadCheck uploads;

    @MockitoBean
    private CatalogStatusService catalog;

    @Test
    @DisplayName("un IOException al leer la subida responde 500 JSON con EFX-IMP-007")
    void shouldAnswerJsonWhenTheUploadCannotBeRead() throws Exception {
        when(uploads.requireAccepted(any())).thenThrow(new IOException("disco"));
        MockMultipartFile file = new MockMultipartFile(
                "archivo", "peliculas.json", MediaType.APPLICATION_JSON_VALUE, "[]".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/cargar/archivo").file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("EFX-IMP-007"));
    }

    @Test
    @DisplayName("una falla de Elasticsearch al cancelar responde JSON con su código")
    void shouldAnswerJsonWhenElasticsearchFails() throws Exception {
        doThrow(new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE)).when(ingest).cancel("j1");

        mvc.perform(post("/cargar/trabajos/j1/cancelar"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("EFX-ES-001"));
    }

    @Test
    @DisplayName("un IllegalStateException sin código responde 500 JSON con EFX-IMP-007, no una página HTML")
    void shouldAnswerJsonWhenAnUncodedFailureHappens() throws Exception {
        when(ingest.job("j1")).thenThrow(new IllegalStateException("falla no prevista"));

        mvc.perform(get("/cargar/trabajos/j1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("EFX-IMP-007"))
                .andExpect(jsonPath("$.message").value("El trabajo de carga se interrumpió"));
    }

    @Test
    @DisplayName("si acceptFile lanza, el temporal de la subida se borra y la respuesta trae EFX-IMP-007")
    void shouldDeleteTheUploadCopyWhenTheJobCannotStart() throws Exception {
        // given
        AtomicReference<Path> temp = new AtomicReference<>();
        when(uploads.requireAccepted(any())).thenReturn(new CheckedFile("peliculas.json", FileKind.JSON));
        when(ingest.acceptFile(any(), any(), any())).thenAnswer(call -> {
            temp.set(call.getArgument(1));
            throw new IllegalStateException("sin hilos");
        });
        MockMultipartFile file = new MockMultipartFile(
                "archivo", "peliculas.json", MediaType.APPLICATION_JSON_VALUE, "[]".getBytes(StandardCharsets.UTF_8));

        // when
        mvc.perform(multipart("/cargar/archivo").file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("EFX-IMP-007"));

        // then
        assertThat(temp.get()).isNotNull().doesNotExist();
    }

    @Test
    @DisplayName("una subida sin la parte archivo la rechaza Spring con 400, sin JSON, como dice IngestJobErrors")
    void shouldLeaveTheMissingFilePartToSpring() throws Exception {
        // when
        var result = mvc.perform(multipart("/cargar/archivo").param("separador", ","))
                .andExpect(status().isBadRequest())
                .andReturn();

        // then
        assertThat(result.getResolvedException())
                .isInstanceOf(org.springframework.web.multipart.support.MissingServletRequestPartException.class);
        assertThat(result.getResponse().getContentType()).isNull();
    }
}

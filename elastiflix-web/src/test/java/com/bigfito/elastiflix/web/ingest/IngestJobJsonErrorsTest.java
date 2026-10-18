package com.bigfito.elastiflix.web.ingest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.ingest.service.IngestService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
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
@Import({IngestJobErrors.class, UploadSizeErrors.class, IngestLimits.class})
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
}

package com.bigfito.elastiflix.web.ingest;

import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.ingest.service.IngestService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Un archivo mayor que el tope y un trabajo desconocido responden JSON, no una página HTML.
 */
@WebMvcTest(IngestJobController.class)
@Import({IngestJobErrors.class, UploadCheck.class})
@TestPropertySource(properties = "spring.servlet.multipart.max-file-size=32B")
class IngestJobErrorTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IngestService ingest;

    @MockitoBean
    private CatalogStatusService catalog;

    @Test
    @DisplayName("un archivo mayor que el tope responde 413 con EFX-ING-003")
    void shouldRejectAnOversizedUploadAsJson() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "archivo", "grande.json", MediaType.APPLICATION_JSON_VALUE, new byte[64]);

        mvc.perform(multipart("/cargar/archivo").file(file))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.code").value("EFX-ING-003"))
                .andExpect(jsonPath("$.message").value("El archivo supera el tamaño máximo"));
    }

    @Test
    @DisplayName("un trabajo desconocido responde 404 JSON")
    void shouldReturnNotFoundJsonForAnUnknownJob() throws Exception {
        when(ingest.job("no-esta")).thenThrow(new ImportException(ErrorCode.UNKNOWN_JOB));

        mvc.perform(get("/cargar/trabajos/no-esta"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EFX-IMP-005"))
                .andExpect(jsonPath("$.message").value("No está ese trabajo de importación"));
    }
}

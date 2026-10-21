package com.bigfito.elastiflix.web.ingest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.ingest.service.IngestService;
import com.bigfito.elastiflix.web.layout.LayoutAdvice;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.util.unit.DataSize;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El tope de la subida son 25 MB fijos (Requerimiento 3): una {@code ELASTIFLIX_INGEST_MAX_FILE_SIZE} vieja en el
 * entorno no lo cambia, y el lote sigue saliendo de {@code ELASTIFLIX_INGEST_BATCH_SIZE} (T-13).
 */
@WebMvcTest(controllers = {IngestController.class, IngestJobController.class}, properties = {
        "elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test",
        "ELASTIFLIX_INGEST_MAX_FILE_SIZE=200MB", "ELASTIFLIX_INGEST_BATCH_SIZE=50"})
@EnableConfigurationProperties(ElastiflixProperties.class)
@Import({LayoutAdvice.class, MovieForms.class, UploadCheck.class, IngestLimits.class, UploadSizeErrors.class,
        IngestTranslator.class})
class IngestUploadLimitTest {

    private static final long LIMIT = DataSize.ofMegabytes(25).toBytes();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IngestService ingest;

    @MockitoBean
    private CatalogStatusService catalog;

    @BeforeEach
    void catalogAnswers() {
        when(catalog.status()).thenReturn(new CatalogStatus("elastiflix-reloaded", "idx", 4322L, true, null));
    }

    @Test
    @DisplayName("la página muestra «25 MB» y lotes de 50, y el navegador recibe el tope en bytes")
    void shouldShowTheFixedLimitWhenAnOldEnvironmentSetsAnotherOne() throws Exception {
        mvc.perform(get("/cargar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Máximo 25 MB")))
                .andExpect(content().string(containsString("tamaño (máximo 25 MB) antes de subir nada.")))
                .andExpect(content().string(containsString("en lotes de 50.")))
                .andExpect(content().string(containsString("data-max-bytes=\"" + LIMIT + "\"")))
                .andExpect(content().string(containsString("data-max-label=\"25 MB\"")))
                .andExpect(content().string(not(containsString("200 MB"))))
                .andExpect(content().string(not(containsString("150 MB"))));
    }

    @Test
    @DisplayName("en inglés, la página dice «25 MB max»")
    void shouldShowTheFixedLimitInEnglish() throws Exception {
        mvc.perform(get("/cargar").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("25 MB max")))
                .andExpect(content().string(containsString("size (25 MB max) before anything is uploaded.")))
                .andExpect(content().string(not(containsString("200 MB"))));
    }

    @Test
    @DisplayName("un archivo de 25 MB pasa el tope del servidor; uno de 25 MB + 1 byte responde 413")
    void shouldAcceptTheExactLimitAndRejectOneByteMore() throws Exception {
        // La fachada simulada no se queda con el temporal: lo borra como lo haría el trabajo real.
        when(ingest.acceptFile(eq("peliculas.json"), any(), any(), any())).thenAnswer(call -> {
            Files.deleteIfExists(call.getArgument(1, Path.class));
            return "job-1";
        });

        mvc.perform(multipart("/cargar/archivo").file(sized(LIMIT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-1"));
        mvc.perform(multipart("/cargar/archivo").file(sized(LIMIT + 1)))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.code").value("EFX-ING-003"));
    }

    /** Un archivo pequeño que dice pesar {@code size} bytes: el tope solo mira el tamaño declarado. */
    private static MockMultipartFile sized(long size) {
        return new MockMultipartFile("archivo", "peliculas.json", MediaType.APPLICATION_JSON_VALUE,
                "[]".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public long getSize() {
                return size;
            }
        };
    }
}

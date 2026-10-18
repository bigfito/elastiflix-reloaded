package com.bigfito.elastiflix.web.ingest;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.job.JobView;
import com.bigfito.elastiflix.ingest.job.SaveResult;
import com.bigfito.elastiflix.ingest.service.IngestService;
import com.bigfito.elastiflix.web.layout.LayoutAdvice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La carga valida, importa, consulta y cancela con la fachada simulada.
 */
@WebMvcTest({IngestController.class, IngestJobController.class})
@Import({LayoutAdvice.class, MovieForms.class, UploadCheck.class})
class IngestPageTest {

    private static final String JOB = "job-1";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IngestService ingest;

    @MockitoBean
    private CatalogStatusService catalog;

    @BeforeEach
    void catalogAnswers() {
        when(catalog.status()).thenReturn(new CatalogStatus("elastiflix-reloaded", "idx", 4322L, true, null));
        when(ingest.job(JOB)).thenReturn(view("valido"));
    }

    @Test
    @DisplayName("la página dice que los vectores se generan y que el índice anterior se borra")
    void shouldShowTheAuthorizedCopy() throws Exception {
        mvc.perform(get("/cargar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Registro individual")))
                .andExpect(content().string(containsString(
                        "Los vectores de la sinopsis se generan para cada película "
                                + "con el servicio de inferencia de Elasticsearch.")))
                .andExpect(content().string(containsString("el índice anterior se borra")));
    }

    @Test
    @DisplayName("validar, consultar, importar y cancelar usan el trabajo simulado")
    void shouldRunTheJobFlow() throws Exception {
        when(ingest.acceptFile(eq("peliculas.json"), any(), any())).thenReturn(JOB);
        when(ingest.errorReport(JOB)).thenReturn("codigo,campo\n");
        MockMultipartFile file = new MockMultipartFile(
                "archivo", "peliculas.json", MediaType.APPLICATION_JSON_VALUE, "{}".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/cargar/archivo").file(file).param("encabezado", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(JOB));
        mvc.perform(get("/cargar/trabajos/" + JOB))
                .andExpect(jsonPath("$.status").value("valido"));
        mvc.perform(post("/cargar/trabajos/" + JOB + "/importar"))
                .andExpect(jsonPath("$.status").value("valido"));
        verify(ingest).importFile(JOB);
        mvc.perform(post("/cargar/trabajos/" + JOB + "/cancelar"))
                .andExpect(status().isOk());
        verify(ingest).cancel(JOB);
        mvc.perform(get("/cargar/trabajos/" + JOB + "/informe.csv"))
                .andExpect(content().string(containsString("codigo")));
    }

    @Test
    @DisplayName("un archivo .txt se rechaza con EFX-ING-001")
    void shouldRejectATextFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "archivo", "notas.txt", MediaType.TEXT_PLAIN_VALUE, "hola".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/cargar/archivo").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EFX-ING-001"));
    }

    @Test
    @DisplayName("el registro individual vuelve a la página")
    void shouldRedirectAfterSavingOneMovie() throws Exception {
        when(ingest.saveOne(any(Movie.class))).thenReturn(new SaveResult.Saved(saved()));

        mvc.perform(post("/cargar/registro").param("id", "1").param("title", "A").param("plot", "B"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/cargar"));
    }

    private static JobView view(String status) {
        return new JobView(JOB, status, 4, 0, 0, 0, 2, 0, List.of(), List.of(), "");
    }

    private static Movie saved() {
        return new Movie(
                "1", null, "A", null, null, null, null, null, null, null, null, null, "B",
                List.of(), List.of(), List.of(), null, null, null, null, null, null, null,
                null, null, null, List.of(), List.of(), List.of(), null, null, null, null);
    }
}

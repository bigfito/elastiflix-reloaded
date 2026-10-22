package com.bigfito.elastiflix.web.ingest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.ingest.job.ImportMode;
import com.bigfito.elastiflix.ingest.service.IngestService;
import com.bigfito.elastiflix.web.layout.LayoutAdvice;
import com.bigfito.elastiflix.web.locale.LocaleConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El modo de carga: la tarjeta de la página y el parámetro {@code modo} de la subida. Sin catálogo,
 * append viene deshabilitado y bulk marcado.
 */
@WebMvcTest(controllers = {IngestController.class, IngestJobController.class}, properties = {
        "elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test"})
@EnableConfigurationProperties(ElastiflixProperties.class)
@Import({LayoutAdvice.class, LocaleConfig.class, MovieForms.class, UploadCheck.class, IngestLimits.class,
        UploadSizeErrors.class, IngestTranslator.class})
class IngestModeTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IngestService ingest;

    @MockitoBean
    private CatalogStatusService catalog;

    @BeforeEach
    void catalogAnswers() {
        when(catalog.status()).thenReturn(new CatalogStatus("elastiflix-reloaded", "idx", 6899L, true, null));
        // La fachada simulada no se queda con el temporal: lo borra como lo haría el trabajo real.
        when(ingest.acceptFile(eq("peliculas.json"), any(), any(), any())).thenAnswer(call -> {
            Files.deleteIfExists(call.getArgument(1, Path.class));
            return "job-1";
        });
    }

    @ParameterizedTest(name = "modo «{0}» llega como {1}")
    @CsvSource(value = {"bulk, BULK", "BULK, BULK", "append, APPEND", "otro, APPEND", "'', APPEND"})
    @DisplayName("bulk elige bulk; append, vacío u otro valor eligen append")
    void shouldPassTheModeToTheService(String mode, ImportMode expected) throws Exception {
        mvc.perform(upload().param("modo", mode)).andExpect(status().isOk());

        verify(ingest).acceptFile(eq("peliculas.json"), any(), any(), eq(expected));
    }

    @Test
    @DisplayName("sin modo, la subida es append, el modo que no borra nada")
    void shouldAppendWhenTheModeIsMissing() throws Exception {
        mvc.perform(upload()).andExpect(status().isOk());

        verify(ingest).acceptFile(eq("peliculas.json"), any(), any(), eq(ImportMode.APPEND));
    }

    @Test
    @DisplayName("la página trae la tarjeta «Modo de carga» con append marcado y el aviso de bulk con el conteo")
    void shouldShowTheModeCardInSpanish() throws Exception {
        mvc.perform(get("/cargar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Modo de carga")))
                .andExpect(content().string(containsString("Agregar al catálogo")))
                .andExpect(content().string(containsString("Reemplazar el catálogo")))
                .andExpect(content().string(containsString("name=\"modo\" value=\"append\" checked=\"checked\">")))
                .andExpect(content().string(containsString("name=\"modo\" value=\"bulk\">")))
                .andExpect(content().string(containsString("Con bulk, el catálogo actual (6 899 películas) se borra "
                        + "al publicar y queda solo con las películas de este archivo.")))
                .andExpect(content().string(containsString("id=\"bulk-warning\" class=\"mode-warning\" hidden")))
                .andExpect(content().string(containsString("<span id=\"step4-title\">Escritura en el catálogo</span>")))
                .andExpect(content().string(containsString("data-title-bulk=\"Importación en un índice temporal\"")))
                .andExpect(content().string(containsString("<li data-step=\"5\" hidden>")));
    }

    @Test
    @DisplayName("en inglés, la tarjeta dice «Load mode» y el aviso lleva el conteo con coma")
    void shouldShowTheModeCardInEnglish() throws Exception {
        mvc.perform(get("/cargar").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Load mode")))
                .andExpect(content().string(containsString("Add to the catalog")))
                .andExpect(content().string(containsString("Replace the catalog")))
                .andExpect(content().string(containsString("With bulk, the current catalog (6,899 movies) is deleted "
                        + "at publication and only the movies in this file remain.")))
                .andExpect(content().string(containsString("<span id=\"step4-title\">Write to the catalog</span>")));
    }

    @Test
    @DisplayName("sin catálogo: el aviso, append deshabilitado, bulk marcado y sin aviso de borrado")
    void shouldBlockAppendWhenTheCatalogIsMissing() throws Exception {
        when(catalog.status()).thenReturn(missingCatalog());

        mvc.perform(get("/cargar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El catálogo <code>elastiflix-reloaded</code> todavía no "
                        + "existe. Se crea con la primera carga: un archivo en modo bulk o un registro individual. "
                        + "La carga en modo append queda bloqueada hasta entonces.")))
                .andExpect(content().string(containsString("data-no-catalog=\"true\"")))
                .andExpect(content().string(containsString(
                        "name=\"modo\" value=\"append\" disabled=\"disabled\">")))
                .andExpect(content().string(containsString("name=\"modo\" value=\"bulk\" checked=\"checked\">")))
                .andExpect(content().string(containsString("Conectado · el catálogo todavía no existe")))
                .andExpect(content().string(containsString("Índice: elastiflix-reloaded · todavía no existe")));
    }

    @Test
    @DisplayName("sin catálogo, en inglés, el aviso y la cabecera dicen que todavía no existe")
    void shouldBlockAppendWhenTheCatalogIsMissingInEnglish() throws Exception {
        when(catalog.status()).thenReturn(missingCatalog());

        mvc.perform(get("/cargar").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The catalog <code>elastiflix-reloaded</code> doesn't "
                        + "exist yet. The first load creates it: a file in bulk mode or a single record. "
                        + "Append loads are blocked until then.")))
                .andExpect(content().string(containsString("Connected · the catalog doesn&#39;t exist yet")))
                .andExpect(content().string(containsString("Index: elastiflix-reloaded · doesn&#39;t exist yet")));
    }

    @Test
    @DisplayName("con catálogo, la página no trae el aviso de catálogo inexistente")
    void shouldNotWarnWhenTheCatalogExists() throws Exception {
        mvc.perform(get("/cargar"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("id=\"catalog-missing\""))))
                .andExpect(content().string(containsString("data-no-catalog=\"false\"")));
    }

    private static CatalogStatus missingCatalog() {
        return new CatalogStatus("elastiflix-reloaded", null, null, false, "EFX-ES-002");
    }

    private static MockMultipartHttpServletRequestBuilder upload() {
        return multipart("/cargar/archivo").file(new MockMultipartFile("archivo", "peliculas.json",
                MediaType.APPLICATION_JSON_VALUE, "[]".getBytes(StandardCharsets.UTF_8)));
    }
}

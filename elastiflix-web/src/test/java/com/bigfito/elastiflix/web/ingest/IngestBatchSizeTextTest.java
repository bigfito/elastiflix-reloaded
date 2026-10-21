package com.bigfito.elastiflix.web.ingest;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El lote del paso 4 se agrupa por miles como en el resto de la interfaz: «1 000» en español y «1,000» en
 * inglés, nunca el «1.000» que pondría {@code MessageFormat} con un entero (U-12).
 */
@WebMvcTest(controllers = IngestController.class, properties = {
        "elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test",
        "ELASTIFLIX_INGEST_BATCH_SIZE=1000"})
@EnableConfigurationProperties(ElastiflixProperties.class)
@Import({LayoutAdvice.class, MovieForms.class, IngestLimits.class, IngestTranslator.class})
class IngestBatchSizeTextTest {

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
    @DisplayName("en español, el paso 4 dice «en lotes de 1 000.»")
    void shouldGroupTheBatchSizeWithASpaceInSpanish() throws Exception {
        mvc.perform(get("/cargar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("en lotes de 1 000.")))
                .andExpect(content().string(not(containsString("1.000"))));
    }

    @Test
    @DisplayName("en inglés, el paso 4 dice «in batches of 1,000.»")
    void shouldGroupTheBatchSizeWithACommaInEnglish() throws Exception {
        mvc.perform(get("/cargar").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("in batches of 1,000.")));
    }
}

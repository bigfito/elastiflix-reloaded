package com.bigfito.elastiflix.web.about;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.web.ingest.IngestLimits;
import com.bigfito.elastiflix.web.layout.LayoutAdvice;

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
 * Acerca de toma el tamaño máximo y el lote de las variables de entorno, no de cifras escritas a mano.
 */
@WebMvcTest(controllers = AboutController.class, properties = {
        "elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test",
        "ELASTIFLIX_INGEST_MAX_FILE_SIZE=200MB", "ELASTIFLIX_INGEST_BATCH_SIZE=50"})
@Import({LayoutAdvice.class, IngestLimits.class})
@EnableConfigurationProperties(ElastiflixProperties.class)
class AboutLimitsTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogStatusService catalog;

    @Test
    @DisplayName("con 200MB y lotes de 50 en el entorno, Acerca de muestra «200 MB» y «50»")
    void shouldShowTheConfiguredLimits() throws Exception {
        // given
        when(catalog.status()).thenReturn(new CatalogStatus("elastiflix-reloaded", "idx", 6899L, true, null));

        // when / then
        mvc.perform(get("/acerca"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("de máximo 200 MB y con contenido")))
                .andExpect(content().string(containsString("en lotes de 50 en un índice temporal")))
                .andExpect(content().string(not(containsString("150 MB"))))
                .andExpect(content().string(not(containsString("lotes de 100"))));
    }
}

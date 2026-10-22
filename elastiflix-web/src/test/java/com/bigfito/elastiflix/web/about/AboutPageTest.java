package com.bigfito.elastiflix.web.about;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.web.ingest.IngestLimits;
import com.bigfito.elastiflix.web.layout.CatalogTexts;
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
 * Acerca de muestra las secciones del mockup, la tabla de precisión y los créditos de TMDB.
 */
@WebMvcTest(controllers = AboutController.class, properties = {
        "elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test"})
@Import({LayoutAdvice.class, IngestLimits.class})
@EnableConfigurationProperties(ElastiflixProperties.class)
class AboutPageTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogStatusService catalog;

    @BeforeEach
    void catalogOfFourThousand() {
        when(catalog.status()).thenReturn(new CatalogStatus(
                "elastiflix-reloaded", "elastiflix-reloaded-20261018-000000", 4322L, true, null));
    }

    @Test
    @DisplayName("las secciones, la precisión y Créditos usan el alias y el conteo reales")
    void shouldShowSectionsPrecisionAndCredits() throws Exception {
        mvc.perform(get("/acerca"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Full-text (léxica)")))
                .andExpect(content().string(containsString("Vectorial")))
                .andExpect(content().string(containsString("Híbrida")))
                .andExpect(content().string(containsString("Resultados y filtros")))
                .andExpect(content().string(containsString("Carga de datos")))
                .andExpect(content().string(containsString("El catálogo")))
                .andExpect(content().string(containsString(
                        "Medido en el catálogo original de 4 322 películas")))
                .andExpect(content().string(containsString("ninguna película del catálogo lo es")))
                .andExpect(content().string(containsString("de 1902 a 2023")))
                .andExpect(content().string(containsString("22 de 32 (69 %)")))
                .andExpect(content().string(containsString("0.774")))
                .andExpect(content().string(containsString("0.853")))
                .andExpect(content().string(containsString("0.880")))
                .andExpect(content().string(containsString("Créditos")))
                .andExpect(content().string(containsString(CatalogTexts.TMDB)))
                .andExpect(content().string(containsString(CatalogTexts.TMDB_ES)))
                .andExpect(content().string(containsString("blue_short-")))
                .andExpect(content().string(containsString("elastiflix-reloaded")))
                .andExpect(content().string(containsString("Tiene 4 322 películas.")))
                .andExpect(content().string(containsString("las búsquedas siempre leen el catálogo publicado. Una "
                        + "carga en bulk se publica de una sola vez o se descarta sin afectar a las búsquedas; una carga "
                        + "en append se suma al índice vigente lote por lote.")))
                .andExpect(content().string(containsString("se generan siempre")))
                .andExpect(content().string(containsString("Un CSV sin encabezado se rechaza")))
                .andExpect(content().string(containsString("El índice anterior se borra")))
                .andExpect(content().string(containsString("de máximo 25 MB y con contenido")))
                .andExpect(content().string(containsString("en lotes de 100 en un índice temporal nuevo")))
                .andExpect(content().string(containsString("Un archivo se puede cargar en dos modos: append, que "
                        + "agrega las películas al catálogo, o bulk, que lo reemplaza.")))
                .andExpect(content().string(containsString("<dt>Modo append (por defecto)</dt>")))
                .andExpect(content().string(containsString("en lotes de 100 y sin índice temporal. Si un <code>id</code> "
                        + "ya existe, la película se reemplaza.")))
                .andExpect(content().string(containsString("<dt>Modo bulk</dt>")))
                .andExpect(content().string(containsString("el catálogo actual no se copia.")))
                .andExpect(content().string(containsString("<dt>Publicación (bulk)</dt>")))
                .andExpect(content().string(containsString("<dt>Si falla (bulk)</dt>")))
                .andExpect(content().string(not(containsString("empieza como copia del catálogo actual"))))
                .andExpect(content().string(containsString("Similarity (BM25)")))
                .andExpect(content().string(containsString(
                        "<div class=\"note sky\">\n          <h3>Cómo se calcula la relevancia: BM25</h3>")))
                .andExpect(content().string(containsString("<section class=\"mode-cards\"")))
                .andExpect(content().string(containsString("<span class=\"mode-icon lilac\" aria-hidden=\"true\">")))
                .andExpect(content().string(containsString("<div class=\"note rose\">\n          <h3>Cuando no hay resultados</h3>")));
    }

    @Test
    @DisplayName("el índice enlaza a las ocho partes de la página con sus títulos, en español y en inglés")
    void shouldLinkEverySectionFromTheIndex() throws Exception {
        mvc.perform(get("/acerca"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<nav class=\"toc\" aria-label=\"En esta página\">")))
                .andExpect(content().string(containsString("<summary>En esta página</summary>")))
                .andExpect(content().string(containsString("<a href=\"#intro\">Tres formas de buscar una película</a>")))
                .andExpect(content().string(containsString("<a href=\"#fulltext\">Full-text (léxica)</a>")))
                .andExpect(content().string(containsString("<a href=\"#vectorial\">Vectorial</a>")))
                .andExpect(content().string(containsString("<a href=\"#hibrida\">Híbrida</a>")))
                .andExpect(content().string(containsString("<a href=\"#resultados\">Resultados y filtros</a>")))
                .andExpect(content().string(containsString("<a href=\"#carga\">Carga de datos</a>")))
                .andExpect(content().string(containsString("<a href=\"#catalogo\">El catálogo</a>")))
                .andExpect(content().string(containsString("<a href=\"#creditos\">Créditos</a>")))
                .andExpect(content().string(containsString("<section id=\"catalogo\"")))
                .andExpect(content().string(containsString("/js/about.js")));
        mvc.perform(get("/acerca").param("lang", "en"))
                .andExpect(content().string(containsString("<summary>On this page</summary>")))
                .andExpect(content().string(containsString("<a href=\"#resultados\">Results and filters</a>")))
                .andExpect(content().string(containsString("<a href=\"#creditos\">Credits</a>")));
    }

    @Test
    @DisplayName("el catálogo trae los cinco grupos y todas las propiedades, con los subcampos con sangría")
    void shouldDescribeTheWholeCatalog() throws Exception {
        mvc.perform(get("/acerca"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h3>Identificación y títulos</h3>")))
                .andExpect(content().string(containsString("<h3>Sinopsis y textos</h3>")))
                .andExpect(content().string(containsString("<h3>Clasificación, fecha y estado</h3>")))
                .andExpect(content().string(containsString("<h3>Métricas</h3>")))
                .andExpect(content().string(containsString("<h3>Reparto, producción y medios</h3>")))
                .andExpect(content().string(containsString("<td>imdb_id</td><td>keyword</td><td>Identificador en IMDb, "
                        + "por ejemplo tt0120338; el detalle enlaza a su ficha</td><td>Detalle</td>")))
                .andExpect(content().string(containsString("<td>title</td><td>text</td><td>Título de la película</td>"
                        + "<td>Full-text, Híbrida, tarjeta y detalle</td>")))
                .andExpect(content().string(containsString("<td>plot_256</td><td>dense_vector</td>")))
                .andExpect(content().string(containsString("<tr class=\"subfield\">")))
                .andExpect(content().string(containsString("<td>keywords.<wbr>name</td><td>text</td>")))
                .andExpect(content().string(containsString("<td>belongs_to_collection.<wbr>backdrop_path</td><td>text</td>")))
                .andExpect(content().string(containsString("<td>user_score</td><td>float</td>")))
                .andExpect(content().string(containsString("agrupadas como en el formulario de «Cargar datos»")))
                .andExpect(content().string(containsString("<th>Obligatorio en .json</th><th>Obligatorio en .csv</th>")))
                .andExpect(content().string(containsString("<td>Detalle</td><td class=\"req-yes\">Sí</td>"
                        + "<td class=\"req-yes\">Sí</td>")))
                .andExpect(content().string(containsString("<td>Vectorial, Híbrida</td><td>No; si viene, se ignora y "
                        + "se genera</td><td>No; si viene, se ignora y se genera</td>")))
                .andExpect(content().string(containsString("<td>Detalle</td><td>No</td><td>No; va en el JSON de la "
                        + "celda de su objeto</td>")))
                .andExpect(content().string(containsString("en modo append o bulk (las reglas son las mismas)")));
        mvc.perform(get("/acerca").param("lang", "en"))
                .andExpect(content().string(containsString("<h3>Cast, production and media</h3>")))
                .andExpect(content().string(containsString("<td>runtime</td><td>integer</td><td>Runtime in minutes</td>"
                        + "<td>Card and detail</td>")))
                .andExpect(content().string(containsString("grouped as in the «Load data» form")))
                .andExpect(content().string(containsString("<th>Required in .json</th><th>Required in .csv</th>")))
                .andExpect(content().string(containsString("in append or bulk mode (the rules are the same)")));
    }

    @Test
    @DisplayName("con lang=en muestra Acerca de en inglés y formatea 4,322 con coma")
    void shouldShowEnglishSectionsWhenLangIsEn() throws Exception {
        mvc.perform(get("/acerca").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"en\">")))
                .andExpect(content().string(containsString("Three ways to search for a movie")))
                .andExpect(content().string(containsString("Full-text (lexical)")))
                .andExpect(content().string(containsString("How relevance is scored: BM25")))
                .andExpect(content().string(containsString("Measured precision by dimension")))
                .andExpect(content().string(containsString("22 of 32 (69%)")))
                .andExpect(content().string(containsString("Results and filters")))
                .andExpect(content().string(containsString("When there are no results")))
                .andExpect(content().string(containsString("Loading data")))
                .andExpect(content().string(containsString("up to 25 MB, with UTF-8 text content")))
                .andExpect(content().string(containsString("in batches of 100 to a new temporary index")))
                .andExpect(content().string(containsString("<dt>Append mode (default)</dt>")))
                .andExpect(content().string(containsString("in batches of 100 and with no temporary index.")))
                .andExpect(content().string(containsString("<dt>Bulk mode</dt>")))
                .andExpect(content().string(containsString("<dt>If it fails (bulk)</dt>")))
                .andExpect(content().string(containsString("It has 4,322 movies.")))
                .andExpect(content().string(containsString("searches always read the published catalog. A bulk load "
                        + "is published all at once or discarded without affecting searches; an append load is added "
                        + "to the current index batch by batch.")))
                .andExpect(content().string(containsString("Index: elastiflix-reloaded · 4,322 movies")))
                .andExpect(content().string(containsString("Credits")));
    }

    @Test
    @DisplayName("Créditos abre con la frase del mockup y marca el idioma de cada aviso de TMDB")
    void shouldIntroduceTheCreditsAndMarkTheLanguageOfEachNotice() throws Exception {
        String notices = "<p lang=\"en\">" + CatalogTexts.TMDB + "</p>\n        <p lang=\"es\">"
                + CatalogTexts.TMDB_ES + "</p>";

        mvc.perform(get("/acerca"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<p>Los datos de las películas y las imágenes de "
                        + "pósters vienen de TMDB (The Movie Database).</p>")))
                .andExpect(content().string(containsString(notices)));

        mvc.perform(get("/acerca").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "<p>Movie data and poster images come from TMDB (The Movie Database).</p>")))
                .andExpect(content().string(containsString(notices)));
    }
}

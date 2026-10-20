package com.bigfito.elastiflix.web.ingest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.csv.CsvColumnRow;
import com.bigfito.elastiflix.ingest.job.JobView;
import com.bigfito.elastiflix.ingest.job.SaveResult;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;
import com.bigfito.elastiflix.ingest.mapping.WarningCount;
import com.bigfito.elastiflix.ingest.service.IngestService;
import com.bigfito.elastiflix.web.locale.LocaleConfig;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La carga valida, importa, consulta y cancela con la fachada simulada, con los textos del mockup.
 */
@WebMvcTest(controllers = {IngestController.class, IngestJobController.class}, properties = {
        "elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test"})
@EnableConfigurationProperties(ElastiflixProperties.class)
@Import({LayoutAdvice.class, LocaleConfig.class, MovieForms.class, UploadCheck.class, IngestLimits.class,
        IngestTranslator.class})
class IngestPageTest {

    private static final String JOB = "job-1";
    private static final long MEGABYTE = 1024L * 1024L;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IngestService ingest;

    @MockitoBean
    private CatalogStatusService catalog;

    @BeforeEach
    void catalogAnswers() {
        when(catalog.status()).thenReturn(new CatalogStatus("elastiflix-reloaded", "idx", 4322L, true, null));
        when(ingest.job(JOB)).thenReturn(validJson());
    }

    @Test
    @DisplayName("la página dice que los vectores se generan y que el índice anterior se borra")
    void shouldShowTheAuthorizedCopy() throws Exception {
        mvc.perform(get("/cargar"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("nav", "cargar"))
                .andExpect(model().attribute("maxFileLabel", "150 MB"))
                .andExpect(model().attribute("maxFileBytes", 150L * MEGABYTE))
                .andExpect(model().attribute("batchSize", 100))
                .andExpect(content().string(containsString("Índice elastiflix-reloaded")))
                .andExpect(content().string(containsString("data-alias=\"elastiflix-reloaded\"")))
                .andExpect(content().string(containsString("id=\"tab-json\" data-tab=\"json\" aria-selected=\"false\" "
                        + "tabindex=\"-1\"")))
                .andExpect(content().string(containsString("Registro individual")))
                .andExpect(content().string(containsString("Archivo JSON")))
                .andExpect(content().string(containsString("Campos obligatorios.")))
                .andExpect(content().string(containsString("Registros leídos")))
                .andExpect(content().string(containsString("Reemplazarán existentes")))
                .andExpect(content().string(containsString("Se recibió")))
                .andExpect(content().string(containsString("El mapping espera")))
                .andExpect(content().string(containsString("Comprobaciones superadas")))
                .andExpect(content().string(containsString("Advertencias · no detienen la carga")))
                .andExpect(content().string(containsString("Correspondencia de columnas")))
                .andExpect(content().string(containsString("Cancelar importación")))
                .andExpect(content().string(containsString("Codificación")))
                .andExpect(content().string(containsString("type=\"url\"")))
                .andExpect(content().string(containsString("role=\"tab\"")))
                .andExpect(content().string(containsString("el índice anterior se borra")));
    }

    @Test
    @DisplayName("escenario «Archivo rechazado»: el bloque del navegador trae los textos del mockup")
    void shouldShowTheRejectedFileCopy() throws Exception {
        mvc.perform(get("/cargar"))
                .andExpect(model().attribute("maxFileLabel", "150 MB"))
                .andExpect(model().attribute("batchSize", 100))
                .andExpect(content().string(containsString("No se puede cargar este archivo")))
                .andExpect(content().string(containsString("El archivo se rechazó en el navegador, antes de subirlo, "
                        + "así que no salió de tu equipo ni se importó nada.")))
                .andExpect(content().string(containsString("Antes de subir se comprueba la extensión y el tipo "
                        + "(.json o .csv, sin dobles extensiones), el tamaño (máximo 150 MB) y que el contenido sea "
                        + "texto UTF-8. El servidor repite estas comprobaciones al recibirlo.")))
                .andExpect(content().string(containsString("Máximo 150 MB")))
                .andExpect(content().string(containsString("Cambiar archivo")))
                .andExpect(content().string(containsString("en lotes de 100.")))
                .andExpect(content().string(containsString("data-max-bytes=\"157286400\"")))
                .andExpect(content().string(not(containsString("· detectad"))))
                .andExpect(content().string(not(containsString("Si se desmarca"))))
                .andExpect(content().string(not(containsString("Elige un archivo"))));
    }

    @Test
    @DisplayName("escenario «Archivo válido»: cifras, texto de importación y progreso del mockup")
    void shouldGiveTheValidFileTexts() throws Exception {
        mvc.perform(get("/cargar/trabajos/" + JOB))
                .andExpect(jsonPath("$.job.status").value("valido"))
                .andExpect(jsonPath("$.texts.read").value("4 322"))
                .andExpect(jsonPath("$.texts.valid").value("4 322"))
                .andExpect(jsonPath("$.texts.errors").value("0"))
                .andExpect(jsonPath("$.texts.locationLabel").value("Registro"))
                .andExpect(jsonPath("$.texts.importLabel").value("Importar 4 322 registros"))
                .andExpect(jsonPath("$.texts.importText").value("Se importarán 4 322 películas en 44 lotes de 100 "
                        + "dentro de un índice temporal que empieza como copia del catálogo actual. Al terminar se "
                        + "comprueba el conteo y el alias elastiflix-reloaded pasa al índice nuevo y el índice "
                        + "anterior se borra, en una sola operación. Si Elasticsearch rechaza un lote o la "
                        + "comprobación falla, el índice temporal se borra y el catálogo queda exactamente como "
                        + "estaba. Los vectores de la sinopsis se generan para cada película con el servicio de "
                        + "inferencia de Elasticsearch."))
                .andExpect(jsonPath("$.texts.progressText").value(
                        "0 de 4 322 importadas · lote 0 de 44 · índice temporal elastiflix-reloaded-AAAAMMDD-HHMMSS"))
                .andExpect(jsonPath("$.texts.fileMeta").value("107.3 MB · 4 322 documentos"));
    }

    @Test
    @DisplayName("escenario «No coincide con el mapping»: 23 errores en 17 registros e importación bloqueada")
    void shouldGiveTheMismatchTexts() throws Exception {
        when(ingest.job(JOB)).thenReturn(mismatchCsv());

        mvc.perform(get("/cargar/trabajos/" + JOB))
                .andExpect(jsonPath("$.texts.read").value("512"))
                .andExpect(jsonPath("$.texts.valid").value("495"))
                .andExpect(jsonPath("$.texts.errors").value("23"))
                .andExpect(jsonPath("$.texts.errorSummary").value("23 errores en 17 registros"))
                .andExpect(jsonPath("$.texts.errorRange").value("Mostrando 6 de 23 errores."))
                .andExpect(jsonPath("$.texts.locationLabel").value("Fila"))
                .andExpect(jsonPath("$.texts.importLabel").value("Importación bloqueada"))
                .andExpect(jsonPath("$.texts.importText").value("La importación está bloqueada: el archivo no "
                        + "coincide con el mapping del índice y no se escribió ningún registro."))
                .andExpect(jsonPath("$.texts.progressText").value("0 de 512 importadas"))
                .andExpect(jsonPath("$.texts.mappingNote").value("33 de 34 columnas asignadas. La columna director "
                        + "no existe en el mapping del índice, así que la carga no puede continuar."))
                .andExpect(jsonPath("$.texts.fileMeta").value("12.4 MB · CSV · 34 columnas · 512 filas"))
                .andExpect(jsonPath("$.job.columns[0].status").value("No existe en el mapping"));
        mvc.perform(get("/cargar"))
                .andExpect(content().string(containsString("Se revisó el archivo completo y se encontraron "
                        + "<strong id=\"error-summary\">0 errores</strong>. <strong>No se importó ningún registro."
                        + "</strong> Corrige el archivo y vuelve a cargarlo; la carga solo continúa cuando todo el "
                        + "archivo coincide con el mapping.")));
    }

    @Test
    @DisplayName("las advertencias reales terminan en punto, con su conteo, y la de vectores va primero")
    void shouldEndEachWarningWithAPeriod() throws Exception {
        when(ingest.job(JOB)).thenReturn(withWarnings(List.of(
                new WarningCount("EFX-ING-021", "spoken_languages", 62, "se omite"),
                new WarningCount("EFX-ING-022", "tagline", 1, "sin valor"),
                new WarningCount("EFX-ING-020", "plot_256", 4322, "se generará desde plot"))));

        mvc.perform(get("/cargar/trabajos/" + JOB))
                .andExpect(jsonPath("$.texts.warnings[0]").value("El archivo trae plot_256, plot_512 y plot_1024: "
                        + "se ignorarán y se generarán con el servicio de inferencia."))
                .andExpect(jsonPath("$.texts.warnings[1]").value("spoken_languages: se omite (62)."))
                .andExpect(jsonPath("$.texts.warnings[2]").value("tagline: sin valor."));
    }

    @Test
    @DisplayName("un registro individual rechazado muestra «código · campo · esperado.» con punto final")
    void shouldEndEachRejectionWithAPeriod() throws Exception {
        List<ValidationIssue> rejected = List.of(new ValidationIssue("EFX-ING-012", "formulario", "1",
                "vote_count", "1e3", "un número entero", false));

        mvc.perform(get("/cargar").flashAttr("rejected", rejected))
                .andExpect(content().string(containsString("<li>EFX-ING-012 · vote_count · un número entero.</li>")));
    }

    @Test
    @DisplayName("un trabajo fallido muestra el código una sola vez")
    void shouldShowTheFailureCodeOnce() throws Exception {
        when(ingest.job(JOB)).thenReturn(job("fallido", "json", 4, 0, 0, List.of(), List.of(),
                "EFX-IMP-007", "La importación se interrumpió"));

        mvc.perform(get("/cargar/trabajos/" + JOB))
                .andExpect(jsonPath("$.texts.failure").value("EFX-IMP-007 · La importación se interrumpió."));
    }

    @Test
    @DisplayName("validar, consultar, importar y cancelar usan el trabajo simulado")
    void shouldRunTheJobFlow() throws Exception {
        // La fachada simulada no se queda con el temporal: lo borra como lo haría el trabajo real.
        when(ingest.acceptFile(eq("peliculas.json"), any(), any())).thenAnswer(call -> {
            Files.deleteIfExists(call.getArgument(1, Path.class));
            return JOB;
        });
        when(ingest.errorReport(JOB)).thenReturn("codigo,campo\n");
        when(ingest.job(JOB)).thenReturn(validJson(), inState("importando"), inState("publicado"));
        MockMultipartFile file = new MockMultipartFile(
                "archivo", "peliculas.json", MediaType.APPLICATION_JSON_VALUE, "{}".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/cargar/archivo").file(file).param("encabezado", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(JOB));
        mvc.perform(get("/cargar/trabajos/" + JOB))
                .andExpect(jsonPath("$.job.status").value("valido"));
        mvc.perform(post("/cargar/trabajos/" + JOB + "/importar"))
                .andExpect(jsonPath("$.job.status").value("importando"));
        verify(ingest).importFile(JOB);
        mvc.perform(get("/cargar/trabajos/" + JOB))
                .andExpect(jsonPath("$.job.status").value("publicado"));
        mvc.perform(post("/cargar/trabajos/" + JOB + "/cancelar"))
                .andExpect(status().isOk());
        verify(ingest).cancel(JOB);
        mvc.perform(get("/cargar/trabajos/" + JOB + "/informe.csv"))
                .andExpect(content().string(containsString("codigo")));
    }

    @Test
    @DisplayName("un CSV con encabezado=false se rechaza en la subida con EFX-ING-005")
    void shouldRejectACsvWithoutHeader() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "archivo", "peliculas.csv", "text/csv", "1,a,b\n".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/cargar/archivo").file(file).param("encabezado", "false"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EFX-ING-005"));
        verify(ingest, never()).acceptFile(anyString(), any(), any());
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
    @DisplayName("vote_count=1e3 o un runtime de 11 dígitos vuelven con EFX-ING-012 y el campo, sin 500")
    void shouldRejectAnOutOfRangeFormNumber() throws Exception {
        mvc.perform(post("/cargar/registro").param("id", "1").param("title", "A").param("plot", "B")
                        .param("vote_count", "1e3"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/cargar"))
                .andExpect(flash().attribute("rejected", List.of(new ValidationIssue("EFX-ING-012", "formulario",
                        "1", "vote_count", "1e3", "un número entero", false))));
        mvc.perform(post("/cargar/registro").param("id", "1").param("title", "A").param("plot", "B")
                        .param("runtime", "12345678901"))
                .andExpect(status().isFound())
                .andExpect(flash().attribute("rejected", List.of(new ValidationIssue("EFX-ING-012", "formulario",
                        "1", "runtime", "12345678901", "un número entero", false))));
        verify(ingest, never()).saveOne(any(Movie.class));
    }

    @Test
    @DisplayName("el registro individual vuelve a la página")
    void shouldRedirectAfterSavingOneMovie() throws Exception {
        when(ingest.saveOne(any(Movie.class))).thenReturn(new SaveResult.Saved(saved()));

        mvc.perform(post("/cargar/registro").param("id", "1").param("title", "A").param("plot", "B"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/cargar"))
                .andExpect(flash().attribute("savedTitle", "A"))
                .andExpect(flash().attribute("rejected", (Object) null));
        mvc.perform(get("/cargar").flashAttr("savedTitle", "A"))
                .andExpect(content().string(containsString("Se guardó <span>A</span>.")));
    }

    @Test
    @DisplayName("un registro que Elasticsearch o el mapping rechaza vuelve con sus errores y sin título guardado")
    void shouldRedirectWithTheIssuesWhenTheMovieIsRejected() throws Exception {
        ValidationIssue issue = new ValidationIssue("EFX-ING-010", "formulario", "1", "release_date",
                "\"1999\"", "date con formato yyyy-MM-dd", false);
        when(ingest.saveOne(any(Movie.class))).thenReturn(new SaveResult.Rejected(
                new ValidationReport(List.of(issue))));

        mvc.perform(post("/cargar/registro").param("id", "1").param("title", "A").param("plot", "B"))
                .andExpect(redirectedUrl("/cargar"))
                .andExpect(flash().attribute("rejected", List.of(issue)))
                .andExpect(flash().attribute("savedTitle", (Object) null));
    }

    @Test
    @DisplayName("la página de carga responde en inglés con lang=en")
    void shouldRenderIngestPageInEnglish() throws Exception {
        mvc.perform(get("/cargar?lang=en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"en\"")))
                .andExpect(content().string(containsString("Index elastiflix-reloaded")))
                .andExpect(content().string(containsString("Load data")))
                .andExpect(content().string(containsString("Single record")))
                .andExpect(content().string(containsString("Import .json")))
                .andExpect(content().string(containsString("Import .csv")))
                .andExpect(content().string(containsString("Identification and titles")))
                .andExpect(content().string(containsString("Save movie")))
                .andExpect(content().string(containsString("Upload process")))
                .andExpect(content().string(containsString("Browser check")));
    }

    @Test
    @DisplayName("el estado de un trabajo válido responde textos en inglés con lang=en")
    void shouldGiveTheValidFileTextsInEnglish() throws Exception {
        mvc.perform(get("/cargar/trabajos/" + JOB + "?lang=en"))
                .andExpect(jsonPath("$.job.status").value("valido"))
                .andExpect(jsonPath("$.texts.read").value("4,322"))
                .andExpect(jsonPath("$.texts.valid").value("4,322"))
                .andExpect(jsonPath("$.texts.errors").value("0"))
                .andExpect(jsonPath("$.texts.locationLabel").value("Record"))
                .andExpect(jsonPath("$.texts.importLabel").value("Import 4,322 records"))
                .andExpect(jsonPath("$.texts.fileMeta").value("107.3 MB · 4,322 documents"))
                .andExpect(jsonPath("$.texts.progressText").value(
                        "0 of 4,322 imported · batch 0 of 44 · temporary index elastiflix-reloaded-AAAAMMDD-HHMMSS"));
    }

    @Test
    @DisplayName("el informe CSV de errores se traduce al inglés con lang=en")
    void shouldGiveErrorReportInEnglish() throws Exception {
        when(ingest.errorReport(JOB)).thenReturn(
                "\uFEFFregistro,id,campo,recibido,esperado\nRegistro 1,1,title,,un texto\n");

        mvc.perform(get("/cargar/trabajos/" + JOB + "/informe.csv?lang=en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("record,id,field,received,expected\nRecord 1,1,title,,un texto\n")));
    }

    private static JobView validJson() {
        return job("valido", "json", 4322, 0, 0, List.of(), List.of(), "", "");
    }

    private static JobView withWarnings(List<WarningCount> warnings) {
        return new JobView(JOB, "valido", "json", MEGABYTE, 4, 0, 0, 0, 4322, 4322, 0, 0, 0,
                List.of(), warnings, List.of(), List.of(), "", "", 100, 44, "");
    }

    private static JobView inState(String status) {
        return job(status, "json", 4322, 0, 0, List.of(), List.of(), "", "");
    }

    private static JobView mismatchCsv() {
        List<ValidationIssue> errors = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            errors.add(new ValidationIssue("EFX-ING-010", Integer.toString(38 + index), "27205", "release_date",
                    "\"16/07/1999\"", "date con formato yyyy-MM-dd", false));
        }
        List<CsvColumnRow> columns = new ArrayList<>();
        columns.add(new CsvColumnRow("director", "— Sin campo en el índice —", "Texto", "No existe en el mapping"));
        for (int index = 1; index < 34; index++) {
            columns.add(new CsvColumnRow("c" + index, "c" + index, "Texto", "Asignada"));
        }
        return job("invalido", "csv", 512, 17, 23, errors, columns, "", "");
    }

    private static JobView job(String status, String kind, int read, int invalidRecords, int errorTotal,
            List<ValidationIssue> errors, List<CsvColumnRow> columns, String failureCode, String failureMessage) {
        long size = "csv".equals(kind) ? Math.round(12.4 * MEGABYTE) : Math.round(107.3 * MEGABYTE);
        int batches = (read + 99) / 100;
        return new JobView(JOB, status, kind, size, "valido".equals(status) ? 4 : 3, 0, 0, 0,
                read, read - invalidRecords, invalidRecords, 0, errorTotal, errors, List.of(), List.of(), columns,
                failureCode, failureMessage, 100, batches, "");
    }

    private static Movie saved() {
        return new Movie(
                "1", null, "A", null, null, null, null, null, null, null, null, null, "B",
                List.of(), List.of(), List.of(), null, null, null, null, null, null, null,
                null, null, null, List.of(), List.of(), List.of(), null, null, null, null);
    }
}

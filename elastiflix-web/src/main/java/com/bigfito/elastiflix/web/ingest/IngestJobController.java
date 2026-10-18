package com.bigfito.elastiflix.web.ingest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.bigfito.elastiflix.core.error.IngestValidationException;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.job.JobView;
import com.bigfito.elastiflix.ingest.service.IngestService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Subida, estado, importación, cancelación e informe de un archivo.
 */
@RestController
public class IngestJobController {

    private final IngestService ingest;
    private final UploadCheck uploads;

    /**
     * Crea los endpoints con la fachada y la comprobación previa.
     *
     * @param ingest  fachada
     * @param uploads extensión, tamaño y UTF-8
     */
    public IngestJobController(IngestService ingest, UploadCheck uploads) {
        this.ingest = ingest;
        this.uploads = uploads;
    }

    /**
     * Comprueba el archivo y arranca la validación.
     *
     * @param file      multipart {@code archivo}
     * @param separator coma, punto y coma o tabulador
     * @param header    {@code false} si la casilla de encabezado está desmarcada
     * @return {@code jobId}, o 400 con el código si el archivo no sirve
     * @throws IOException si no se puede copiar el temporal
     */
    @PostMapping("/cargar/archivo")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("archivo") MultipartFile file,
            @RequestParam(name = "separador", defaultValue = ",") String separator,
            @RequestParam(name = "encabezado", defaultValue = "false") String header) throws IOException {
        try {
            uploads.requireAccepted(file);
        } catch (IngestValidationException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "code", exception.errorCode().code(),
                    "message", exception.getMessage()));
        }
        String jobId = ingest.acceptFile(file.getOriginalFilename(), copy(file), settings(separator, header));
        return ResponseEntity.ok(Map.of("jobId", jobId));
    }

    /**
     * Estado del trabajo.
     *
     * @param jobId identificador
     * @return la copia que ve la página
     */
    @GetMapping("/cargar/trabajos/{jobId}")
    public JobView job(@PathVariable String jobId) {
        return ingest.job(jobId);
    }

    /**
     * Empieza la importación de un archivo ya válido.
     *
     * @param jobId identificador
     * @return el estado después de pedirla
     */
    @PostMapping("/cargar/trabajos/{jobId}/importar")
    public JobView start(@PathVariable String jobId) {
        ingest.importFile(jobId);
        return ingest.job(jobId);
    }

    /**
     * Pide cancelar y borrar el índice temporal.
     *
     * @param jobId identificador
     * @return el estado después de pedirla
     */
    @PostMapping("/cargar/trabajos/{jobId}/cancelar")
    public JobView cancel(@PathVariable String jobId) {
        ingest.cancel(jobId);
        return ingest.job(jobId);
    }

    /**
     * Informe completo de errores.
     *
     * @param jobId identificador
     * @return CSV UTF-8
     */
    @GetMapping(value = "/cargar/trabajos/{jobId}/informe.csv", produces = "text/csv")
    public String report(@PathVariable String jobId) {
        return ingest.errorReport(jobId);
    }

    private static Path copy(MultipartFile file) throws IOException {
        Path path = Files.createTempFile("elastiflix-upload-", ".bin");
        file.transferTo(path);
        return path;
    }

    private static CsvSettings settings(String separator, String header) {
        return new CsvSettings(separatorOf(separator), !"false".equals(header));
    }

    private static char separatorOf(String separator) {
        if (";".equals(separator)) {
            return CsvSettings.SEMICOLON;
        }
        if ("tab".equals(separator)) {
            return CsvSettings.TAB;
        }
        return CsvSettings.COMMA;
    }
}

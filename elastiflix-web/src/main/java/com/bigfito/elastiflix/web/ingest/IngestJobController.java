package com.bigfito.elastiflix.web.ingest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.IngestValidationException;
import com.bigfito.elastiflix.ingest.file.CheckedFile;
import com.bigfito.elastiflix.ingest.file.FileKind;
import com.bigfito.elastiflix.ingest.job.JobView;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.service.IngestService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Subida, estado, importación, cancelación e informe de un archivo.
 */
@RestController
public class IngestJobController {

    private static final Logger LOG = LoggerFactory.getLogger(IngestJobController.class);

    private final IngestService ingest;
    private final UploadCheck uploads;
    private final IngestLimits limits;

    /**
     * Crea el controlador.
     *
     * @param ingest  fachada
     * @param uploads extensión, tamaño y UTF-8
     * @param limits  tamaño máximo del archivo y alias del catálogo para los textos
     */
    public IngestJobController(IngestService ingest, UploadCheck uploads, IngestLimits limits) {
        this.ingest = ingest;
        this.uploads = uploads;
        this.limits = limits;
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
        rejectIfTooLarge(file);
        try {
            requireHeader(uploads.requireAccepted(file), header);
        } catch (IngestValidationException exception) {
            return rejected(file, exception);
        }
        String jobId = start(file, copy(file), settings(separator, header));
        return ResponseEntity.ok(Map.of("jobId", jobId));
    }

    /**
     * Estado del trabajo.
     *
     * @param jobId identificador
     * @return la copia que ve la página, con sus textos
     */
    @GetMapping("/cargar/trabajos/{jobId}")
    public JobScreen job(@PathVariable String jobId) {
        return screen(jobId);
    }

    /**
     * Empieza la importación de un archivo ya válido.
     *
     * @param jobId identificador
     * @return el estado después de pedirla
     */
    @PostMapping("/cargar/trabajos/{jobId}/importar")
    public JobScreen start(@PathVariable String jobId) {
        ingest.importFile(jobId);
        return screen(jobId);
    }

    /**
     * Pide cancelar y borrar el índice temporal.
     *
     * @param jobId identificador
     * @return el estado después de pedirla
     */
    @PostMapping("/cargar/trabajos/{jobId}/cancelar")
    public JobScreen cancel(@PathVariable String jobId) {
        ingest.cancel(jobId);
        return screen(jobId);
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

    private static ResponseEntity<Map<String, String>> rejected(
            MultipartFile file, IngestValidationException exception) {
        String code = exception.errorCode().code();
        LOG.warn("{}: se rechazó {}", code, fileName(file));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "code", code,
                "message", exception.errorCode().summary()));
    }

    private static String fileName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return "sin-nombre";
        }
        return name;
    }

    private JobScreen screen(String jobId) {
        JobView view = ingest.job(jobId);
        return new JobScreen(view, JobTexts.of(view, limits.alias()));
    }

    /** Un CSV sin encabezado se rechaza en la subida, antes de crear el trabajo (PLAN §5). */
    private static void requireHeader(CheckedFile checked, String header) {
        if (checked.kind() == FileKind.CSV && "false".equals(header)) {
            throw new IngestValidationException(ErrorCode.HEADER_NOT_CONFIRMED);
        }
    }

    private void rejectIfTooLarge(MultipartFile file) {
        long maxFileBytes = limits.maxFileSize().toBytes();
        if (file.getSize() > maxFileBytes) {
            throw new MaxUploadSizeExceededException(maxFileBytes);
        }
    }

    /**
     * Entrega el temporal al trabajo. Si {@code acceptFile} lanza, nadie más lo borraría: se borra aquí y la
     * excepción sigue a {@link IngestJobErrors}. Un {@code finally} con bandera evita atrapar
     * {@code RuntimeException} (regla {@code JM-ERR-01}).
     */
    private String start(MultipartFile file, Path temp, CsvSettings settings) {
        boolean handedOver = false;
        try {
            String jobId = ingest.acceptFile(file.getOriginalFilename(), temp, settings);
            handedOver = true;
            return jobId;
        } finally {
            if (!handedOver) {
                deleteTemp(temp);
            }
        }
    }

    private static Path copy(MultipartFile file) throws IOException {
        Path path = Files.createTempFile("elastiflix-upload-", ".bin");
        boolean copied = false;
        try {
            file.transferTo(path);
            copied = true;
            return path;
        } finally {
            if (!copied) {
                deleteTemp(path);
            }
        }
    }

    private static void deleteTemp(Path temp) {
        try {
            Files.deleteIfExists(temp);
        } catch (IOException exception) {
            LOG.warn("No se pudo borrar el temporal de una subida rechazada {} tipo={}",
                    temp.getFileName(), exception.getClass().getSimpleName());
        }
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

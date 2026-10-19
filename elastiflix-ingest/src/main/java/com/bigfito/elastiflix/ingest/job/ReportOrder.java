package com.bigfito.elastiflix.ingest.job;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;

/**
 * Junta el informe del lector con el de los campos, en el orden en que una persona lee el archivo.
 *
 * <p>Primero van el encabezado y los errores del archivo entero, que no tienen número de fila. Después,
 * cada fila en orden. Así la vista previa de seis errores no muestra la fila 900 antes que la 2 solo
 * porque su error de ancho lo encontró el lector.</p>
 */
final class ReportOrder {

    /** Más cifras que esto no caben en un {@code long}; una fila así no existe. */
    private static final int MAX_DIGITS = 18;

    private ReportOrder() {
    }

    /**
     * Une los dos informes y ordena por fila. El orden es estable: dentro de una fila se conserva el de
     * llegada.
     *
     * @param reader informe del lector: encabezado, ancho de fila o {@code document_count}
     * @param fields informe de la validación contra el mapping
     * @return un solo informe ordenado
     */
    static ValidationReport join(ValidationReport reader, ValidationReport fields) {
        List<ValidationIssue> issues = new ArrayList<>(reader.issues());
        issues.addAll(fields.issues());
        issues.sort(Comparator.comparingLong(ReportOrder::position));
        return new ValidationReport(issues);
    }

    private static long position(ValidationIssue issue) {
        String label = issue.recordLabel();
        if (!JobFigures.isRecordNumber(label)) {
            return -1;
        }
        return label.length() > MAX_DIGITS ? Long.MAX_VALUE : Long.parseLong(label);
    }
}

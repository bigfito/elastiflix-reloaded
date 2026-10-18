package com.bigfito.elastiflix.ingest.mapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Errores y advertencias de un archivo. Si hay un error, el archivo no se importa.
 *
 * @param issues en el orden en que se encontraron
 */
public record ValidationReport(List<ValidationIssue> issues) {

    private static final char BOM = '\uFEFF';
    private static final String FORMULA = "=+-@";

    /**
     * Copia la lista para que nadie la cambie después.
     */
    public ValidationReport {
        issues = List.copyOf(issues);
    }

    /**
     * Informe vacío: el archivo puede importarse.
     *
     * @return informe sin incidencias
     */
    public static ValidationReport empty() {
        return new ValidationReport(List.of());
    }

    /**
     * Dice si no hay errores. Las advertencias no cuentan.
     *
     * @return {@code true} cuando se puede importar
     */
    public boolean accepted() {
        return errors().isEmpty();
    }

    /**
     * Incidencias que impiden la importación.
     *
     * @return solo los errores
     */
    public List<ValidationIssue> errors() {
        return filter(false);
    }

    /**
     * Incidencias que se informan y no detienen.
     *
     * @return solo las advertencias
     */
    public List<ValidationIssue> warnings() {
        return filter(true);
    }

    /**
     * Advertencias juntas por código y campo. Cada consulta de estado recibe esta lista.
     *
     * @return una fila por código y campo, con cuántas veces apareció
     */
    public List<WarningCount> groupedWarnings() {
        Map<String, WarningCount> groups = new LinkedHashMap<>();
        for (ValidationIssue issue : warnings()) {
            addGroup(groups, issue);
        }
        return List.copyOf(groups.values());
    }

    /**
     * CSV de los errores, sin las advertencias. La primera fila es el encabezado.
     *
     * @return texto con columnas registro, id, campo, recibido y esperado
     */
    public String toErrorCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append(BOM);
        csv.append("registro,id,campo,recibido,esperado\n");
        for (ValidationIssue issue : errors()) {
            appendRow(csv, issue);
        }
        return csv.toString();
    }

    private static void appendRow(StringBuilder csv, ValidationIssue issue) {
        csv.append(cell(issue.recordLabel())).append(',')
                .append(cell(issue.id())).append(',')
                .append(cell(issue.field())).append(',')
                .append(cell(issue.received())).append(',')
                .append(cell(issue.expected())).append('\n');
    }

    private static void addGroup(Map<String, WarningCount> groups, ValidationIssue issue) {
        String key = issue.code() + '\u0000' + issue.field();
        WarningCount current = groups.get(key);
        int count = current == null ? 1 : current.count() + 1;
        String message = current == null ? issue.expected() : current.message();
        groups.put(key, new WarningCount(issue.code(), issue.field(), count, message));
    }

    private static String cell(String value) {
        String text = neutralize(value == null ? "" : value);
        if (needsQuotes(text)) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private static String neutralize(String text) {
        if (text.isEmpty() || FORMULA.indexOf(text.charAt(0)) < 0) {
            return text;
        }
        return "'" + text;
    }

    private static boolean needsQuotes(String text) {
        return text.indexOf(',') >= 0
                || text.indexOf('"') >= 0
                || text.indexOf('\n') >= 0
                || text.indexOf('\r') >= 0;
    }

    private List<ValidationIssue> filter(boolean warning) {
        List<ValidationIssue> selected = new ArrayList<>();
        for (ValidationIssue issue : issues) {
            if (issue.warning() == warning) {
                selected.add(issue);
            }
        }
        return List.copyOf(selected);
    }
}

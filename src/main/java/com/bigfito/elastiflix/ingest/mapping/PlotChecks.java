package com.bigfito.elastiflix.ingest.mapping;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bigfito.elastiflix.core.error.ErrorCode;

import tools.jackson.databind.JsonNode;

/**
 * Revisa {@code plot}: rechaza un marcador o un texto de menos de {@value #MIN_PLOT_LENGTH} caracteres,
 * y avisa (sin rechazar) de restos de un modelo o de una sinopsis repetida en el archivo.
 *
 * <p>Un error rechaza el archivo completo. El catálogo que se publica todavía tiene sinopsis compartidas
 * y frases de un modelo, así que esas dos revisiones son advertencias.</p>
 */
final class PlotChecks {

    /** Por debajo de este largo, tras quitar espacios de los extremos, la sinopsis no sirve. */
    static final int MIN_PLOT_LENGTH = 40;

    private static final Pattern PLACEHOLDER = Pattern.compile(
            "(?i)^\\[?\\s*no information found\\s*\\]?$");
    private static final Pattern ASSISTANT = Pattern.compile(
            "(?i)\\b(?:i )?(?:couldn'?t|could not|was unable to|unable to) find\\b"
                    + "|knowledge cutoff|as of my (?:last )?(?:knowledge|update|training)"
                    + "|\\bi'?m sorry\\b|\\bi apologi[sz]e\\b");
    private static final Pattern MARKDOWN_BOLD = Pattern.compile("\\*\\*");
    private static final Pattern MARKDOWN_HEADING = Pattern.compile("(?m)^#+ ");
    private static final Pattern NOT_CLARIFICATION = Pattern.compile(", not [A-Z][A-Za-z0-9.'&-]{0,40}");
    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final String SHA_256 = "SHA-256";

    private PlotChecks() {
    }

    /**
     * Deja pasar la sinopsis, o anota el rechazo.
     *
     * @param plot  texto ya comprobado como no vacío
     * @param scope registro que se revisa
     * @return {@code false} si el texto no se copia
     */
    static boolean keep(String plot, CheckScope scope) {
        if (rejected(plot.strip(), scope)) {
            return false;
        }
        String mark = artifact(plot);
        if (mark != null) {
            scope.warning(ErrorCode.SUSPECT_PLOT, FieldNames.PLOT, mark, IngestPhrase.CLEAN_PLOT.text());
        }
        return true;
    }

    /**
     * Avisa si otra película del mismo archivo ya traía esta sinopsis. No rechaza el archivo.
     *
     * @param checked registro ya revisado; si el plot no se copió, no hay nada que comparar
     * @param label   fila o número de registro
     * @param run     pasada en curso, con las sinopsis ya vistas
     */
    static void noteDuplicate(CheckedDocument checked, String label, ValidationRun run) {
        JsonNode plot = checked.body().get(FieldNames.PLOT);
        if (plot == null || !plot.isString()) {
            return;
        }
        String previous = run.plots().putIfAbsent(fingerprint(plot.asString()), checked.id());
        if (previous != null && !previous.equals(checked.id())) {
            run.add(duplicate(label, checked.id(), previous));
        }
    }

    private static boolean rejected(String stripped, CheckScope scope) {
        if (!placeholder(stripped) && stripped.length() >= MIN_PLOT_LENGTH) {
            return false;
        }
        scope.error(ErrorCode.PLACEHOLDER_PLOT, FieldNames.PLOT, IssueTexts.clip(stripped),
                IngestPhrase.REAL_PLOT.text(MIN_PLOT_LENGTH));
        return true;
    }

    private static boolean placeholder(String stripped) {
        return PLACEHOLDER.matcher(stripped).matches();
    }

    private static String artifact(String plot) {
        String found = first(ASSISTANT, plot);
        if (found == null) {
            found = markdown(plot);
        }
        if (found == null) {
            found = first(NOT_CLARIFICATION, plot);
        }
        return found;
    }

    private static String markdown(String plot) {
        String bold = first(MARKDOWN_BOLD, plot);
        if (bold != null) {
            return bold;
        }
        return first(MARKDOWN_HEADING, plot);
    }

    private static String first(Pattern pattern, String plot) {
        Matcher matcher = pattern.matcher(plot);
        if (!matcher.find()) {
            return null;
        }
        return IssueTexts.clip(matcher.group());
    }

    private static ValidationIssue duplicate(String label, String id, String previousId) {
        return new ValidationIssue(
                ErrorCode.DUPLICATE_PLOT.code(),
                label,
                id,
                FieldNames.PLOT,
                previousId,
                IngestPhrase.DISTINCT_PLOT.text(),
                true);
    }

    private static String fingerprint(String plot) {
        String normalized = SPACES.matcher(plot.strip()).replaceAll(" ").toLowerCase(Locale.ROOT);
        return HexFormat.of().formatHex(digest(normalized));
    }

    private static byte[] digest(String text) {
        try {
            return MessageDigest.getInstance(SHA_256).digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(SHA_256, exception);
        }
    }
}

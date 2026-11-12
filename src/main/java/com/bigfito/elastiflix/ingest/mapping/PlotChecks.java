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
 * y avisa (sin rechazar) de restos de un modelo, de un ensayo de temas o de una sinopsis repetida.
 *
 * <p>Un error rechaza el archivo completo. Una frase de asistente, markdown, una aclaración de ficha
 * o un ensayo de temas se avisan: rechazarlos tiraría el archivo entero. La aclaración «, not Nombre»
 * solo cuenta justo después de «directed by» o «produced by»; «unable to find work» y un «, not» de la
 * historia no. El ensayo es «main themes», «the main theme of» o una lista de temas cerrada con dos puntos.</p>
 */
final class PlotChecks {

    /** Por debajo de este largo, tras quitar espacios de los extremos, la sinopsis no sirve. */
    static final int MIN_PLOT_LENGTH = 40;

    private static final Pattern PLACEHOLDER = Pattern.compile(
            "(?i)^\\[?\\s*no information found\\s*\\]?$");
    private static final Pattern ASSISTANT = Pattern.compile(
            "(?i)\\bI (?:couldn't|could not|was unable to) find\\b"
                    + "|find any information"
                    + "|knowledge cutoff"
                    + "|as of my (?:last )?(?:knowledge|update|training)"
                    + "|\\bi'?m sorry\\b"
                    + "|\\bi apologi[sz]e\\b");
    private static final Pattern MARKDOWN_BOLD = Pattern.compile("\\*\\*");
    private static final Pattern MARKDOWN_HEADING = Pattern.compile("(?m)^#+ ");
    private static final Pattern CREDIT = Pattern.compile("(?i)\\b(?:directed|produced) by\\b");
    private static final Pattern NOT_NAME = Pattern.compile(", not [A-Z][A-Za-z0-9.'&-]{0,40}");
    /** «main themes» en cualquier parte. */
    private static final Pattern MAIN_THEMES = Pattern.compile("(?i)\\bmain themes\\b");
    /** Una línea con theme o themes que termina en dos puntos. */
    private static final Pattern THEME_LIST = Pattern.compile("(?im)\\bthemes?\\b[^.\\n:]{0,80}:[ \\t]*$");
    /** «the main/central/primary/core/overarching/key theme of». */
    private static final Pattern SINGLE_THEME = Pattern.compile(
            "(?i)\\bthe (?:main|central|primary|core|overarching|key) theme of\\b");
    /** Encabezado «Themes:», «Key themes:» o «Major themes:». */
    private static final Pattern THEMES_HEADING = Pattern.compile(
            "(?im)^\\s*(?:themes|key themes|major themes)\\s*:");

    /**
     * Caracteres entre «directed by» o «produced by» y «, not Nombre».
     * 160 cubre la aclaración más larga que sí era un resto (id 249070, a 153).
     */
    private static final int NOT_WINDOW = 160;
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
        String essay = essay(plot);
        if (essay != null) {
            scope.warning(ErrorCode.ESSAY_PLOT, FieldNames.PLOT, essay, IngestPhrase.NARRATIVE_PLOT.text());
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
            found = notClarification(plot);
        }
        return found;
    }

    /**
     * Devuelve el primer fragmento que delata un ensayo de temas.
     * No rechaza: la carga sigue.
     */
    private static String essay(String plot) {
        String found = first(MAIN_THEMES, plot);
        if (found == null) {
            found = first(THEME_LIST, plot);
        }
        if (found == null) {
            found = first(SINGLE_THEME, plot);
        }
        if (found == null) {
            found = first(THEMES_HEADING, plot);
        }
        return found;
    }

    /**
     * «directed by …, not Estudio» o «produced by …, not Estudio».
     * Un «, not» suelto, como el de una historia, no cuenta.
     */
    private static String notClarification(String plot) {
        Matcher credit = CREDIT.matcher(plot);
        while (credit.find()) {
            int from = credit.end();
            Matcher correction = NOT_NAME.matcher(plot);
            if (correction.find(from) && correction.start() <= from + NOT_WINDOW) {
                return IssueTexts.clip(correction.group());
            }
        }
        return null;
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

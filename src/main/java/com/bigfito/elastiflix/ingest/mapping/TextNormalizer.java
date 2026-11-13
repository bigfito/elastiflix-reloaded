package com.bigfito.elastiflix.ingest.mapping;

import java.text.Normalizer;

import com.bigfito.elastiflix.core.error.ErrorCode;

import tools.jackson.databind.JsonNode;

/**
 * Deja un texto sin espacios en los extremos, sin espacio duro y en forma NFC.
 */
final class TextNormalizer {

    private static final char HARD_SPACE = '\u00a0';

    private TextNormalizer() {
    }

    /**
     * Normaliza y, si el texto que se va a guardar cambió, avisa {@code EFX-ING-029}.
     *
     * <p>Un resultado vacío no avisa aquí: quien guarda el campo ya avisa de un texto en blanco.</p>
     *
     * @param field campo o ruta del informe
     * @param text  texto que llegó
     * @param scope registro que se revisa
     * @return el texto ya normalizado
     */
    static String keep(String field, String text, CheckScope scope) {
        String normalized = normalize(text);
        if (!normalized.equals(text) && !normalized.isBlank()) {
            scope.warning(ErrorCode.TEXT_NORMALIZED, field, IssueTexts.clip(text),
                    IngestPhrase.NORMALIZED_TEXT.text());
        }
        return normalized;
    }

    /** Nodo de texto para copiar el valor ya normalizado al documento limpio. */
    static JsonNode node(String text) {
        return MovieNodes.MAPPER.valueToTree(text);
    }

    /**
     * Copia el texto al documento. El {@code id} se deja igual: un espacio en él es parte del identificador.
     *
     * @param name  campo del documento
     * @param value texto que llegó
     * @param scope registro que se revisa
     * @return {@link Outcome#BLANK} si no quedó texto; el campo en blanco lo avisa quien llama
     */
    static Outcome apply(String name, JsonNode value, CheckScope scope) {
        if (FieldNames.ID.equals(name)) {
            return copyId(name, value, scope);
        }
        return copyText(name, value, scope);
    }

    private static Outcome copyId(String name, JsonNode value, CheckScope scope) {
        if (value.asString().isBlank()) {
            return Outcome.BLANK;
        }
        scope.target().set(name, value);
        return Outcome.STORED;
    }

    private static Outcome copyText(String name, JsonNode value, CheckScope scope) {
        String normalized = keep(name, value.asString(), scope);
        if (normalized.isBlank()) {
            return Outcome.BLANK;
        }
        return store(name, normalized, scope);
    }

    private static Outcome store(String name, String normalized, CheckScope scope) {
        if (FieldNames.PLOT.equals(name) && !PlotChecks.keep(normalized, scope)) {
            return Outcome.DROPPED;
        }
        scope.target().set(name, node(normalized));
        return Outcome.STORED;
    }

    private static String normalize(String text) {
        String spaced = text.replace(HARD_SPACE, ' ');
        return Normalizer.normalize(spaced.strip(), Normalizer.Form.NFC);
    }

    /** Qué pasó con el texto: se guardó, quedó vacío o la sinopsis no sirvió. */
    enum Outcome {
        /** El texto quedó en el documento. */
        STORED,
        /** No quedó texto; quien llama avisa del vacío. */
        BLANK,
        /** La sinopsis no sirvió y no se copia. */
        DROPPED
    }
}

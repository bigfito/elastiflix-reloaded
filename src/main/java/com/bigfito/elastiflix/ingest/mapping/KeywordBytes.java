package com.bigfito.elastiflix.ingest.mapping;

import java.nio.charset.StandardCharsets;

import com.bigfito.elastiflix.core.error.ErrorCode;

import tools.jackson.databind.JsonNode;

/**
 * Largo máximo, en bytes UTF-8, de los valores {@code keyword} que el mapping guarda sin {@code ignore_above}.
 *
 * <p>Sin esta revisión, un valor demasiado largo pasa como válido y Elasticsearch lo rechaza al escribir el
 * lote, después de copiar todo el catálogo ({@code EFX-IMP-001}). Los subcampos {@code .keyword} de
 * {@code keywords} y {@code belongs_to_collection} tienen {@code ignore_above: 256} y no necesitan tope.</p>
 */
final class KeywordBytes {

    /** Elasticsearch no acepta un {@code _id} más largo, y el {@code id} es el {@code _id} del documento. */
    static final int MAX_ID = 512;

    /** Lucene no indexa un término más largo. */
    static final int MAX_TERM = 32_766;

    /** Un carácter de Java ocupa como mucho 3 bytes en UTF-8 (un par sustituto, 4 bytes por 2 caracteres). */
    private static final int MAX_BYTES_PER_CHAR = 3;

    private KeywordBytes() {
    }

    /**
     * Dice si el texto del valor pasa del tope de ese campo. Un valor que no es texto ni entero no cuenta:
     * su error de tipo lo da quien lo revisa.
     *
     * @param name  campo, o lista de textos, del documento
     * @param value valor que llegó
     * @return {@code true} si hay que rechazarlo
     */
    static boolean exceeds(String name, JsonNode value) {
        String text = IdTexts.of(value);
        if (text == null || text.length() * MAX_BYTES_PER_CHAR <= limit(name)) {
            return false;
        }
        return text.getBytes(StandardCharsets.UTF_8).length > limit(name);
    }

    /**
     * Anota el error {@code EFX-ING-012} con el tope del campo.
     *
     * @param name  campo
     * @param value valor que llegó; el informe guarda solo su comienzo
     * @param scope registro que se revisa
     */
    static void reject(String name, JsonNode value, CheckScope scope) {
        scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(value),
                IngestPhrase.TEXT_UP_TO_BYTES.text(limit(name)));
    }

    private static int limit(String name) {
        return FieldNames.ID.equals(name) ? MAX_ID : MAX_TERM;
    }
}

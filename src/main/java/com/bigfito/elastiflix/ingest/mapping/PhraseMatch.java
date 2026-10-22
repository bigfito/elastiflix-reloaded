package com.bigfito.elastiflix.ingest.mapping;

import java.util.List;

/**
 * Una frase del vocabulario de la carga reconocida en un texto, con sus argumentos.
 *
 * @param phrase la frase
 * @param args   un {@link Long} por número y un {@link String} por texto, en orden
 */
public record PhraseMatch(IngestPhrase phrase, List<Object> args) {

    /**
     * Copia los argumentos.
     */
    public PhraseMatch {
        args = List.copyOf(args);
    }
}

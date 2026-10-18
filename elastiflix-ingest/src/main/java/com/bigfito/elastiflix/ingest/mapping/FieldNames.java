package com.bigfito.elastiflix.ingest.mapping;

import java.util.List;

/**
 * Nombres que se repiten al validar un documento.
 */
final class FieldNames {

    static final String ID = "id";
    static final String TITLE = "title";
    static final String PLOT = "plot";
    static final String DOCUMENT_ID = "_id";
    static final String FILE = "archivo";
    static final String MISSING = "—";
    static final String ABSENT = "no vino";
    static final String BLANK = "en blanco";
    static final List<String> REQUIRED = List.of(ID, TITLE, PLOT);

    private FieldNames() {
    }
}

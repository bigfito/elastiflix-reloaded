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
    static final String FILE = IngestPhrase.FILE.text();
    static final String MISSING = "—";
    static final String ABSENT = IngestPhrase.ABSENT.text();
    static final String BLANK = IngestPhrase.BLANK.text();
    static final List<String> REQUIRED = List.of(ID, TITLE, PLOT);

    private FieldNames() {
    }
}

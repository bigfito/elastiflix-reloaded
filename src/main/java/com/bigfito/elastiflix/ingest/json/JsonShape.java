package com.bigfito.elastiflix.ingest.json;

/**
 * Forma de un archivo JSON de carga.
 */
enum JsonShape {

    /** Un arreglo de películas. */
    ARRAY,

    /** Objeto de exportación, con el campo {@code documents}. */
    EXPORT,

    /** Una película, o varias una detrás de otra. */
    NDJSON
}

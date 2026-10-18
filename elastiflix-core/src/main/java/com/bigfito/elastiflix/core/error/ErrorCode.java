package com.bigfito.elastiflix.core.error;

/**
 * Catálogo de códigos de error de Elastiflix.
 *
 * <p>Cada código explica qué pasó, la causa probable y la acción sugerida, para que quien use la
 * demo sepa cómo resolver o reportar la falla. Las fases posteriores agregan sus constantes aquí.</p>
 */
public enum ErrorCode {

    /** Falta {@code ELASTICSEARCH_URL} al arrancar. */
    MISSING_ELASTICSEARCH_URL("EFX-CFG-001",
            "Falta la URL de Elasticsearch",
            "ELASTICSEARCH_URL no está definida o está vacía",
            "Revisa ELASTICSEARCH_URL en .env y vuelve a arrancar"),

    /** Falta {@code ELASTICSEARCH_API_KEY} al arrancar. */
    MISSING_API_KEY("EFX-CFG-002",
            "Falta la API key de Elasticsearch",
            "ELASTICSEARCH_API_KEY no está definida o está vacía",
            "Revisa ELASTICSEARCH_API_KEY en .env y vuelve a arrancar"),

    /** El jar de core no incluye el mapping del catálogo. */
    MAPPING_NOT_PACKAGED("EFX-CFG-003",
            "No está el mapping del catálogo",
            "No se empaquetó elasticsearch/elastiflix-reloaded-mapping.json",
            "Reconstruye elastiflix-core con el recurso del mapping"),

    /** No se pudo hablar con Elasticsearch. */
    ELASTICSEARCH_UNAVAILABLE("EFX-ES-001",
            "No se pudo conectar con Elasticsearch",
            "La URL o la API key no son válidas, o el proyecto Serverless no responde",
            "Revisa ELASTICSEARCH_URL y ELASTICSEARCH_API_KEY en .env y vuelve a intentarlo"),

    /** El alias configurado no existe ni como alias ni como índice. */
    CATALOG_NOT_FOUND("EFX-ES-002",
            "El catálogo no existe",
            "No hay alias ni índice concreto con ese nombre",
            "Revisa el alias configurado y que el proyecto tenga el catálogo"),

    /** El reindex no copió todos los documentos. */
    REINDEX_COUNT_MISMATCH("EFX-ES-003",
            "El índice nuevo no tiene los mismos documentos que el original",
            "El reindex no copió todos los documentos",
            "No se publicó el alias. El índice nuevo se borró y el original sigue igual. "
                    + "Vuelve a ejecutar la migración"),

    /** La llamada única a {@code _aliases} no movió el alias: sigue en el índice anterior. */
    ALIAS_PUBLISH_FAILED("EFX-ES-004",
            "No se pudo publicar el alias",
            "Elasticsearch rechazó la publicación y el alias sigue en el índice anterior",
            "Se borró el índice temporal. El catálogo anterior sigue igual. "
                    + "Revisa el catálogo y vuelve a intentarlo"),

    /** El alias apunta a más de un índice. */
    ALIAS_AMBIGUOUS("EFX-ES-005",
            "El alias apunta a más de un índice",
            "El alias no se publicó con la operación única de esta demo",
            "Deja el alias apuntando a un solo índice antes de continuar"),

    /** El endpoint de inferencia no respondió. */
    INFERENCE_UNAVAILABLE("EFX-EMB-001",
            "No se pudo generar el vector",
            "El id no existe o el endpoint falla",
            "Revisa ELASTIFLIX_INFERENCE_ID y que el proyecto tenga ese endpoint"),

    /** El vector no tiene 1024 dimensiones o su norma es cero. */
    UNEXPECTED_VECTOR("EFX-EMB-002",
            "El vector no tiene la dimensión esperada",
            "El modelo no devolvió el vector esperado",
            "Revisa el endpoint: la demo espera 1024 dimensiones"),

    /** No hay texto que vectorizar. */
    EMPTY_EMBEDDING_TEXT("EFX-EMB-003",
            "No hay texto para vectorizar",
            "La consulta o la sinopsis no trae texto",
            "Escribe un texto antes de pedir el vector"),

    /** La caja de búsqueda llegó vacía. */
    EMPTY_SEARCH_TEXT("EFX-SRCH-001",
            "Escribe qué película buscas antes de pulsar Buscar",
            "La caja de búsqueda llegó vacía",
            "Escribe una consulta"),

    /** {@code k} es menor que 1. */
    K_TOO_SMALL("EFX-SRCH-002",
            "k debe ser un entero mayor o igual que 1",
            "k es menor que 1",
            "Elige un entero a partir de 1"),

    /** Hay menos candidatos que resultados pedidos. */
    CANDIDATES_BELOW_K("EFX-SRCH-003",
            "num_candidates debe ser mayor o igual que k",
            "Hay menos candidatos que resultados pedidos",
            "Sube num_candidates o baja k"),

    /** {@code num_candidates} pasa de 10 000. */
    CANDIDATES_TOO_LARGE("EFX-SRCH-004",
            "num_candidates debe ser un entero de máximo 10 000",
            "num_candidates pasa de 10 000",
            "Elige un entero entre k y 10 000"),

    /** Llegó un campo junto con {@code multi_match}. */
    FIELD_WITH_MULTI_MATCH("EFX-SRCH-005",
            "El campo no se usa con multi-match",
            "Llegó un campo junto con multi_match",
            "Quita el campo: multi_match ya busca en los dos títulos"),

    /** El tamaño de página no es 25, 50 ni 100. */
    PAGE_SIZE_NOT_ALLOWED("EFX-SRCH-006",
            "El tamaño de página debe ser 25, 50 o 100",
            "El tamaño de página no es 25, 50 ni 100",
            "Elige uno de esos tres"),

    /** «Desde» es posterior a «Hasta». */
    DATE_RANGE_REVERSED("EFX-SRCH-007",
            "La fecha «Desde» no puede ser posterior a «Hasta»",
            "Desde es posterior a Hasta",
            "Invierte las fechas o deja una sola"),

    /** {@code from} llega a 10 000. */
    FULL_TEXT_WINDOW_EXCEEDED("EFX-SRCH-008",
            "La página se sale de las 10 000 coincidencias que se pueden recorrer",
            "from es mayor o igual que 10 000",
            "Vuelve a una página anterior"),

    /** {@code from} llega a {@code k}. */
    KNN_WINDOW_EXCEEDED("EFX-SRCH-009",
            "La página se sale de los k resultados de este modo",
            "from es mayor o igual que k",
            "Vuelve a una página anterior o sube k"),

    /** El modo no trae sus ajustes. */
    MISSING_MODE_SETTINGS("EFX-SRCH-010",
            "Faltan los ajustes de este modo",
            "El modo no trae sus ajustes",
            "Completa el tipo de consulta o el vector, según el modo"),

    /** La página es menor que 1. */
    PAGE_TOO_SMALL("EFX-SRCH-011",
            "La página debe ser un entero mayor o igual que 1",
            "La página es menor que 1",
            "Empieza en la página 1"),

    /** El nombre no termina en {@code .json} ni en {@code .csv}. */
    BAD_EXTENSION("EFX-ING-001",
            "La extensión no es .json ni .csv",
            "El nombre no tiene punto, o el sufijo es otro",
            "Elige un archivo .json o .csv"),

    /** El nombre tiene más de un punto. */
    DOUBLE_EXTENSION("EFX-ING-002",
            "El archivo tiene más de una extensión",
            "El nombre tiene más de un punto",
            "Renombra el archivo para que solo termine en .json o .csv"),

    /** El archivo pesa más que el límite configurado. */
    FILE_TOO_LARGE("EFX-ING-003",
            "El archivo supera el tamaño máximo",
            "Pesa más que el límite configurado",
            "Parte el archivo o súbelo por debajo del límite"),

    /** Hay bytes que no son UTF-8. */
    INVALID_UTF8("EFX-ING-004",
            "El archivo no es texto UTF-8",
            "Hay bytes que no decodifican como UTF-8",
            "Guarda el archivo como UTF-8 y vuelve a subirlo"),

    /** La casilla de encabezado del CSV llegó desmarcada. */
    HEADER_NOT_CONFIRMED("EFX-ING-005",
            "El CSV no confirma que la primera fila sea el encabezado",
            "La casilla de encabezado llegó desmarcada",
            "Marca que la primera fila trae los nombres de columna"),

    /** La primera fila no sirve como encabezado. */
    INVALID_HEADER("EFX-ING-006",
            "La primera fila no tiene nombres de columna válidos",
            "Faltan id, title o plot, o hay un nombre vacío o repetido",
            "Pon en la primera fila los nombres del mapping, con id, title y plot"),

    /** Una columna o un campo no está en el mapping. */
    UNKNOWN_FIELD("EFX-ING-007",
            "Hay un campo que no existe en el mapping",
            "La columna o el campo no está en el índice",
            "Quita el campo o renómbralo a uno del mapping"),

    /** El mismo {@code id} aparece dos veces. */
    DUPLICATE_ID("EFX-ING-009",
            "Hay un id repetido",
            "El mismo id aparece dos veces",
            "Deja una sola fila por id"),

    /** El metadato {@code _id} no coincide con {@code id}. */
    ID_MISMATCH("EFX-ING-010",
            "_id no es igual a id",
            "Vino el metadato y, ya como texto, no coincide",
            "Haz que _id y id sean el mismo texto"),

    /** Falta {@code id}, {@code title} o {@code plot}. */
    REQUIRED_FIELD("EFX-ING-011",
            "Faltan id, title o plot",
            "Uno de los tres no viene, viene null o viene en blanco",
            "Completa id, title y plot"),

    /** El valor no encaja con el tipo del campo. */
    TYPE_MISMATCH("EFX-ING-012",
            "El valor no tiene el tipo del mapping",
            "La fecha, el booleano, el número, la lista o el objeto no encajan",
            "Corrige el valor para que coincida con el tipo del campo"),

    /** El archivo trae vectores y se ignoran. */
    VECTORS_IGNORED("EFX-ING-020",
            "Los vectores del archivo se ignoran: se generarán con el servicio de inferencia",
            "El archivo trae plot_256, plot_512 o plot_1024",
            "No hace falta traer esos campos; la demo los calcula"),

    /** Un hueco dentro de una lista se omitió. */
    EMPTY_LIST_VALUE("EFX-ING-021",
            "Un valor vacío dentro de una lista se omitió",
            "Había un texto vacío o un hueco en una lista",
            "Quita el hueco o escribe el texto"),

    /** Un texto opcional quedó en blanco y se guardó sin valor. */
    EMPTY_TEXT("EFX-ING-022",
            "Un texto vacío se guardó sin valor",
            "Un texto opcional solo tiene espacios",
            "No hace falta traer el campo si no tiene texto"),

    /** {@code document_count} no coincide con la lista {@code documents}. */
    DOCUMENT_COUNT_MISMATCH("EFX-ING-008",
            "document_count no coincide con los documentos",
            "El objeto de exportación trae un conteo distinto del largo de documents",
            "Corrige document_count o la lista de documentos y vuelve a subir el archivo"),

    /** El JSON del archivo o de una celda no se puede leer. */
    UNREADABLE_JSON("EFX-ING-013",
            "El JSON no se puede leer",
            "El archivo, o el JSON de una celda, está mal formado",
            "Revisa el JSON y vuelve a subir el archivo"),

    /** Un ítem del lote fue rechazado. */
    BULK_REJECTED("EFX-IMP-001",
            "Elasticsearch rechazó un documento del lote",
            "Un ítem del lote trae error",
            "Se borró el índice temporal. El catálogo anterior sigue igual. Revisa el archivo y vuelve a importar"),

    /** El conteo después de los lotes no es el esperado. */
    IMPORT_COUNT_MISMATCH("EFX-IMP-002",
            "El conteo del índice nuevo no es el esperado",
            "Después de los lotes, el conteo no cuadra",
            "Se borró el índice temporal y no se publicó. Vuelve a importar"),

    /** Alguien pidió cancelar la importación. */
    IMPORT_CANCELLED("EFX-IMP-003",
            "La importación se canceló",
            "Alguien pidió cancelar",
            "Se borró el índice temporal si existía. El catálogo anterior sigue igual"),

    /** El nombre configurado es un índice concreto, no un alias. */
    CATALOG_NOT_ALIAS("EFX-IMP-004",
            "El catálogo configurado no es un alias",
            "El nombre es un índice concreto",
            "Publica el catálogo como alias antes de importar"),

    /** El identificador del trabajo no está en memoria. */
    UNKNOWN_JOB("EFX-IMP-005",
            "No está ese trabajo de importación",
            "El identificador no está en memoria",
            "Vuelve a subir el archivo"),

    /**
     * La publicación no confirmó el resultado y no se pudo leer el alias.
     *
     * <p>El índice temporal se deja: borrarlo podría vaciar el catálogo si el alias ya apunta a él.</p>
     */
    PUBLISH_UNCERTAIN("EFX-IMP-006",
            "No se sabe si el alias se publicó",
            "La respuesta no llegó y no se pudo leer el alias",
            "Estado incierto: revisa el alias manualmente. El índice temporal se dejó sin borrar");

    private final String code;
    private final String whatHappened;
    private final String probableCause;
    private final String suggestedAction;

    ErrorCode(String code, String whatHappened, String probableCause, String suggestedAction) {
        this.code = code;
        this.whatHappened = whatHappened;
        this.probableCause = probableCause;
        this.suggestedAction = suggestedAction;
    }

    /**
     * Devuelve el código visible, por ejemplo {@code EFX-ES-001}.
     *
     * @return el código del error
     */
    public String code() {
        return code;
    }

    /**
     * Devuelve solo qué pasó, sin el código ni la causa.
     *
     * <p>La página de resultados lo muestra junto al campo. La página de error lo antepone al código.</p>
     *
     * @return el texto corto del error
     */
    public String summary() {
        return whatHappened;
    }

    /**
     * Compone el mensaje en el formato del proyecto.
     *
     * @return mensaje con qué pasó, causa probable y acción sugerida
     */
    public String userMessage() {
        return "%s · %s. Causa probable: %s. Acción sugerida: %s."
                .formatted(code, whatHappened, probableCause, suggestedAction);
    }
}

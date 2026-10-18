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

    /** La llamada única a {@code _aliases} fue rechazada. */
    ALIAS_PUBLISH_FAILED("EFX-ES-004",
            "No se pudo publicar el alias",
            "Elasticsearch rechazó la publicación",
            "El índice original no se tocó. Si quedó un índice nuevo, se intenta borrar. "
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

    /** {@code from + size} pasa de 10 000. */
    FULL_TEXT_WINDOW_EXCEEDED("EFX-SRCH-008",
            "La página se sale de las 10 000 coincidencias que se pueden recorrer",
            "from + size pasa de 10 000",
            "Vuelve a una página anterior"),

    /** {@code from + size} pasa de {@code k}. */
    KNN_WINDOW_EXCEEDED("EFX-SRCH-009",
            "La página se sale de los k resultados de este modo",
            "from + size pasa de k",
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
            "Guarda el archivo como UTF-8 y vuelve a subirlo");

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
     * Compone el mensaje en el formato del proyecto.
     *
     * @return mensaje con qué pasó, causa probable y acción sugerida
     */
    public String userMessage() {
        return "%s · %s. Causa probable: %s. Acción sugerida: %s."
                .formatted(code, whatHappened, probableCause, suggestedAction);
    }
}

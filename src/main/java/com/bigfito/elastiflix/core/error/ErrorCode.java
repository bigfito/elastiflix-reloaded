package com.bigfito.elastiflix.core.error;

/** Catálogo completo de códigos. Cada uno dice qué pasó, la causa probable y qué hacer. */
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
            "Reconstruye la aplicación con el recurso del mapping"),

    /** La migración se lanzó con el servidor web encendido. */
    BOOTSTRAP_WITH_WEB("EFX-CFG-004",
            "La migración no puede convivir con el servidor web",
            "Falta --spring.main.web-application-type=none",
            "Vuelve a lanzar la migración con ese argumento"),

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
            "No se publicó el alias y el catálogo original sigue igual. El índice nuevo se borra si el original "
                    + "sigue en su lugar; si quedó, bórralo a mano. Vuelve a ejecutar la importación o la "
                    + "migración"),

    /** La tarea de {@code _reindex} terminó con error o no llegó a completarse. */
    REINDEX_FAILED("EFX-ES-006",
            "No se pudo copiar el catálogo al índice nuevo",
            "La tarea de reindex terminó con error o no se completó",
            "No se publicó el alias y el catálogo original sigue igual. Se pide cancelar la tarea y el índice nuevo "
                    + "se borra si el original sigue en su lugar; si quedó, bórralo a mano. Vuelve a ejecutar la "
                    + "importación o la migración"),

    /** La llamada única a {@code _aliases} no movió el alias: sigue en el índice anterior. */
    ALIAS_PUBLISH_FAILED("EFX-ES-004",
            "No se pudo publicar el alias",
            "Elasticsearch rechazó la publicación y el alias sigue en el índice anterior",
            "El catálogo anterior sigue igual. El índice nuevo se borra; si quedó, bórralo a mano. "
                    + "Revisa el catálogo y vuelve a intentarlo"),

    /** Elasticsearch respondió 4xx. No es un fallo de red. */
    ELASTICSEARCH_REJECTED("EFX-ES-007",
            "Elasticsearch rechazó la operación",
            "El proyecto respondió un error 4xx",
            "Revisa el tipo de error en el log y corrige la petición"),

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

    /** El lote no trajo un vector por cada texto. */
    INCOMPLETE_EMBEDDING_BATCH("EFX-EMB-004",
            "La inferencia no devolvió un vector por cada texto",
            "El número de vectores del lote no es el de textos enviados",
            "Revisa el endpoint de inferencia y vuelve a intentar"),

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

    /** Enlace directo a detalle sin consulta previa o con consulta inválida. */
    DIRECT_DETAIL_WITHOUT_QUERY("EFX-SRCH-012",
            "Tu búsqueda falló y la ficha de la película no se puede abrir",
            "El enlace no trae una búsqueda válida: falta la consulta o algún ajuste no es válido",
            "Vuelve a Inicio, busca la película y ábrela desde los resultados"),

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

    /** El archivo pesa más que el límite de la carga (25 MB). */
    FILE_TOO_LARGE("EFX-ING-003",
            "El archivo supera el tamaño máximo",
            "Pesa más que el límite de la carga",
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
    /** Un cero en budget, revenue, runtime, vote_average o vote_count se guardó sin valor. */
    ZERO_AS_NULL("EFX-ING-023", "Un cero se guardó sin valor",
            "budget, revenue, runtime, vote_average o vote_count llegó en 0, que aquí significa desconocido",
            "Quita el campo o déjalo vacío si no conoces el número"),
    /** Un texto o una palabra clave repetidos se omitieron. Se conserva el primero. */
    DUPLICATE_LIST_VALUE("EFX-ING-024", "Un valor repetido dentro de una lista se omitió",
            "El mismo texto, o la misma palabra clave, ya estaba en la lista",
            "Deja cada valor una sola vez; se conserva el primero"),
    /** La sinopsis es un marcador, abre con una negativa del asistente o no llega a 40 caracteres. Rechaza. */
    PLACEHOLDER_PLOT("EFX-ING-025", "La sinopsis no sirve: es un marcador, abre con una negativa o es demasiado corta",
            "Empieza con «no match found», «I don't have information» o «please provide», o tiene menos de 40",
            "Escribe una sinopsis real de al menos 40 caracteres"),
    /** La sinopsis parece traer restos de un modelo. No rechaza el archivo. */
    SUSPECT_PLOT("EFX-ING-026", "La sinopsis parece traer restos de un modelo de lenguaje",
            "Asistente, «is a 2004 film, not 2005», «here's a summary», markdown o «, not» tras un crédito",
            "Revisa la sinopsis; la carga sigue porque esto no impide importar"),
    /** La misma sinopsis está en otra película del archivo. No rechaza el archivo. */
    DUPLICATE_PLOT("EFX-ING-027", "La sinopsis coincide con la de otra película del archivo",
            "Otro id del mismo archivo tiene la misma sinopsis, ya sin espacios ni mayúsculas de más",
            "Si es un error, escribe una sinopsis propia; si no hay otra fuente, se puede cargar igual"),
    /** La sinopsis parece un ensayo de temas. No rechaza el archivo. */
    ESSAY_PLOT("EFX-ING-028", "La sinopsis parece un ensayo de temas de un modelo",
            "«main themes», «at its core», «explores the theme of», «delves into the complexities» o una lista",
            "Revisa la sinopsis; la carga sigue porque esto no impide importar"),
    /** El texto traía espacios de más, un espacio duro o no estaba en NFC. No rechaza el archivo. */
    TEXT_NORMALIZED("EFX-ING-029", "El texto se normalizó al validar",
            "Tenía espacios en los extremos, un espacio duro (U+00A0) o no estaba en NFC",
            "No hay que hacer nada: la carga guarda el texto ya normalizado"),
    /** {@code document_count} no coincide con la lista {@code documents}. */
    DOCUMENT_COUNT_MISMATCH("EFX-ING-008", "document_count no coincide con los documentos",
            "El objeto de exportación trae un conteo distinto del largo de documents",
            "Corrige document_count o la lista de documentos y vuelve a subir el archivo"),

    /** El JSON del archivo no se puede leer. Una celda CSV mala no llega aquí: da {@code EFX-ING-012}. */
    UNREADABLE_JSON("EFX-ING-013", "El JSON no se puede leer",
            "El archivo JSON está mal formado",
            "Revisa el JSON y vuelve a subir el archivo"),

    /** El archivo CSV no se puede leer. */
    UNREADABLE_CSV("EFX-ING-015", "El CSV no se puede leer",
            "El archivo CSV está mal formado",
            "Revisa el CSV y vuelve a subir el archivo"),

    /** Una fila del CSV no trae una celda por cada columna del encabezado. */
    COLUMN_COUNT("EFX-ING-014", "Una fila no tiene las mismas celdas que el encabezado",
            "Faltan o sobran columnas en esa fila",
            "Corrige la fila para que tenga una celda por cada columna del encabezado"),

    /** Un ítem del lote fue rechazado. */
    BULK_REJECTED("EFX-IMP-001",
            "Elasticsearch rechazó un documento del lote",
            "Un ítem del lote trae error",
            "El catálogo anterior sigue igual. El índice temporal se borra; si quedó, bórralo a mano. "
                    + "Revisa el archivo y vuelve a importar"),

    /** El conteo después de los lotes no es el esperado. */
    IMPORT_COUNT_MISMATCH("EFX-IMP-002",
            "El conteo del índice nuevo no es el esperado",
            "Después de los lotes, el conteo no cuadra",
            "No se publicó. El índice temporal se borra; si quedó, bórralo a mano. Vuelve a importar"),

    /** Alguien pidió cancelar la importación. */
    IMPORT_CANCELLED("EFX-IMP-003",
            "La importación se canceló",
            "Alguien pidió cancelar",
            "El catálogo anterior sigue igual. El índice temporal, si existía, se borra; si quedó, "
                    + "bórralo a mano"),

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

    /** La publicación no se confirmó. El índice nuevo se deja: borrarlo podría vaciar el catálogo. */
    PUBLISH_UNCERTAIN("EFX-IMP-006",
            "No se sabe si el alias se publicó",
            "_aliases no confirmó el cambio y el alias no se pudo leer o no quedó en un estado que permita "
                    + "borrar el índice nuevo",
            "Estado incierto: revisa el alias manualmente. El índice nuevo se dejó sin borrar"),

    /** La validación o la importación salió por un error que no es de la demo. */
    JOB_FAILED("EFX-IMP-007",
            "El trabajo de carga se interrumpió",
            "Ocurrió un error no previsto durante la validación o la importación",
            "Vuelve a subir el archivo. El catálogo anterior no se publicó por este error"),

    /** Modo append: Elasticsearch rechazó al menos un ítem de un lote. */
    APPEND_BATCH_REJECTED("EFX-IMP-008",
            "Elasticsearch rechazó un lote de la carga",
            "Un documento del lote trae error",
            "Lo que ya se escribió se queda en el catálogo; la página dice cuántas películas se agregaron. "
                    + "Revisa el archivo y vuelve a cargarlo"),

    /** Modo append: alguien pidió cancelar entre lotes. */
    APPEND_CANCELLED("EFX-IMP-009",
            "La carga se canceló",
            "Alguien pidió cancelar",
            "Lo que ya se escribió se queda en el catálogo; la página dice cuántas películas se agregaron"),

    /** Modo append sin catálogo: el alias no existe todavía. */
    APPEND_WITHOUT_CATALOG("EFX-IMP-010",
            "El catálogo todavía no existe",
            "La carga en modo append agrega al catálogo y el alias no apunta a ningún índice",
            "Carga primero un archivo en modo bulk o guarda un registro individual: los dos crean el catálogo");

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
     * @return el código del error
     */
    public String code() {
        return code;
    }

    /**
     * Devuelve solo qué pasó, sin el código ni la causa.
     * @return el texto corto del error
     */
    public String summary() {
        return whatHappened;
    }

    /**
     * Compone el mensaje en el formato del proyecto.
     * @return mensaje con qué pasó, causa probable y acción sugerida
     */
    public String userMessage() {
        return "%s · %s. Causa probable: %s. Acción sugerida: %s."
                .formatted(code, whatHappened, probableCause, suggestedAction);
    }
}

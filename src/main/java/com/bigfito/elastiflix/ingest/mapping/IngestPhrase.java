package com.bigfito.elastiflix.ingest.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bigfito.elastiflix.core.text.Thousands;

/**
 * Vocabulario de la carga: cada frase fija que escriben la lectura, la validación y el estado de un trabajo.
 *
 * <p>Es la única fuente de esas frases en español, el idioma de los logs y del informe CSV en español. La
 * web reconoce una frase con {@link #match(String)} y la traduce por {@link #key()}. Lo que no es una frase
 * de este vocabulario (valores del archivo, ids, nombres de campos y columnas) es dato y no se traduce.</p>
 *
 * <p>{@code {0}} y {@code {1}} reciben un número o un texto, que puede ser otra frase.</p>
 */
public enum IngestPhrase {

    /** Se esperaba un texto. */
    TEXT("un texto"),
    /** Se esperaba un texto que quepa en un keyword o en un {@code _id}. */
    TEXT_UP_TO_BYTES("un texto de máximo {0} bytes en UTF-8"),
    /** Se esperaba un entero. */
    INTEGER("un número entero"),
    /** Se esperaba un número (formulario). */
    NUMBER("un número"),
    /** Se esperaba un float finito. */
    FINITE_FLOAT("un número float finito"),
    /** Se esperaba un booleano; también es el contenido de una columna booleana. */
    BOOLEAN("true o false"),
    /** Se esperaba una fecha. */
    DATE("una fecha yyyy-MM-dd"),
    /** Se esperaba una lista de textos. */
    TEXT_LIST("una lista de textos"),
    /** Se esperaba un objeto o {@code null}. */
    OBJECT_OR_NULL("un objeto o null"),
    /** Se esperaba una lista de objetos con id y name. */
    ID_NAME_LIST("una lista de {id, name}"),
    /** Se esperaba un objeto con id y name. */
    ID_NAME_OBJECT("un objeto {id, name}"),
    /** Se esperaba un documento de película. */
    MOVIE_OBJECT("un objeto de película"),
    /** Se esperaba un id que no se repita. */
    UNIQUE_ID("un id único"),
    /** {@code _id} debe repetir {@code id}. */
    SAME_AS_ID("el mismo texto que id"),
    /** Los tres campos obligatorios. */
    REQUIRED_FIELDS("id, title y plot"),
    /** Se esperaba un nombre de columna del mapping. */
    MAPPING_NAME("un nombre del mapping"),
    /** Se esperaba un nombre de columna sin repetir. */
    UNIQUE_NAME("un nombre único"),
    /** El encabezado debe traer los tres obligatorios. */
    HEADER_WITH_REQUIRED("la primera fila incluye id, title y plot"),
    /** Un campo o una columna que el mapping no tiene. */
    NOT_IN_MAPPING("Ese campo no existe en el mapping del índice"),
    /** Los vectores del archivo se generan con inferencia. */
    WILL_BE_GENERATED("se generarán con el servicio de inferencia"),
    /** Cuántas celdas esperaba la fila. */
    CELLS("{0} celdas"),
    /** Advertencia: un valor vacío de una lista no se guarda. */
    SKIPPED("se omite"),
    /** Advertencia: un texto vacío se guarda sin valor. */
    NO_VALUE("sin valor"),
    /** El campo no vino. */
    ABSENT("no vino"),
    /** El campo vino en blanco. */
    BLANK("en blanco"),
    /** El valor no es texto. */
    NOT_TEXT("no es texto"),
    /** El documento no es un objeto JSON. */
    NOT_OBJECT("no es un objeto"),
    /** Una columna sin nombre. */
    EMPTY_NAME("un nombre vacío"),
    /** Una columna con un nombre que ya estaba. */
    REPEATED_NAME("nombre repetido"),
    /** Al encabezado le falta un obligatorio. */
    MISSING_REQUIRED("faltan id, title o plot"),
    /** Un campo que el mapping no tiene. */
    FIELD_NAMED("Un campo llamado {0}"),
    /** Una columna que el mapping no tiene. */
    COLUMN_NAMED("Una columna llamada {0}"),
    /** Una columna de vector. */
    COLUMN("columna {0}"),
    /** Etiqueta del encabezado de un CSV. */
    HEADER("encabezado"),
    /** Etiqueta del archivo entero. */
    FILE("archivo"),
    /** Etiqueta de un documento JSON. */
    DOCUMENT("documento"),
    /** Etiqueta del registro individual. */
    FORM("formulario"),
    /** Estado de una columna que tiene campo. */
    MAPPED("Asignada"),
    /** Estado de una columna de vector. */
    IGNORED("Se ignora"),
    /** Estado de una columna vacía. */
    NO_DATA("Sin datos"),
    /** Estado de una columna sin campo. */
    UNKNOWN_COLUMN("No existe en el mapping"),
    /** Campo de la columna {@code _id}. */
    DOCUMENT_ID("(id del documento)"),
    /** Campo de una columna sin campo. */
    NO_INDEX_FIELD("— Sin campo en el índice —"),
    /** Contenido de la columna {@code _id}. */
    SAME_AS_ID_META("Metadato; tiene que ser igual a id"),
    /** Contenido de una columna de texto. */
    KIND_TEXT("Texto"),
    /** Contenido de una columna entera. */
    KIND_INTEGER("Número entero"),
    /** Contenido de una columna float. */
    KIND_NUMBER("Número"),
    /** Contenido de una columna de fecha. */
    KIND_DATE("Fecha yyyy-MM-dd"),
    /** Contenido de la columna {@code id}. */
    KIND_ID("Texto o número, se guarda como texto (keyword)"),
    /** Contenido de una columna con una lista de textos. */
    KIND_TEXT_LIST("Lista JSON de textos"),
    /** Contenido de una columna con una lista anidada. */
    KIND_NESTED_LIST("Lista JSON de objetos anidados"),
    /** Contenido de una columna con un objeto anidado. */
    KIND_NESTED_OBJECT("Objeto JSON anidado"),
    /** Contenido de una columna de vector. */
    VECTOR_IGNORED("Se ignora · se generará desde plot"),
    /** Contenido de una columna sin ningún valor. */
    EMPTY_IN_ALL_ROWS("Vacía en todas las filas"),
    /** Tipo de la columna y la única fila vacía. */
    EMPTY_IN_ONE_ROW("{0} · vacía en {1} fila"),
    /** Tipo de la columna y cuántas filas vacías. */
    EMPTY_IN_ROWS("{0} · vacía en {1} filas"),
    /** Comprobación superada: registros leídos. */
    CHECK_READ("{0} registros leídos, con id, title y plot."),
    /** Comprobación superada: ids únicos. */
    CHECK_UNIQUE_IDS("Ningún id repetido."),
    /** Se esperaba una sinopsis real y con largo suficiente. */
    REAL_PLOT("una sinopsis real de al menos {0} caracteres"),
    /** Advertencia: la sinopsis parece de un modelo. */
    CLEAN_PLOT("una sinopsis sin restos de un modelo"),
    /** Advertencia: la sinopsis ya está en otra película del archivo. */
    DISTINCT_PLOT("una sinopsis distinta"),
    /** Advertencia: la sinopsis parece un ensayo de temas. */
    NARRATIVE_PLOT("una sinopsis sin un ensayo de temas"),
    /** Advertencia: el texto se guardó sin espacios de más, sin espacio duro y en NFC. */
    NORMALIZED_TEXT("el texto ya normalizado");

    private final String spanish;
    private final Pattern pattern;

    IngestPhrase(String spanish) {
        this.spanish = spanish;
        this.pattern = Patterns.PLACEHOLDER.matcher(spanish).find() ? compile(spanish) : null;
    }

    /**
     * Clave estable de la frase, por ejemplo {@code empty.in.rows}.
     *
     * @return el nombre en minúsculas, con puntos
     */
    public String key() {
        return name().toLowerCase(Locale.ROOT).replace('_', '.');
    }

    /**
     * Plantilla en español, con sus marcadores.
     *
     * @return la frase tal como la escribe la carga
     */
    public String template() {
        return spanish;
    }

    /**
     * La frase en español. Un número se agrupa por miles como en el resto de la interfaz.
     *
     * @param args un valor por marcador
     * @return la frase con los valores puestos
     */
    public String text(Object... args) {
        String out = spanish;
        for (int index = 0; index < args.length; index++) {
            out = out.replace("{" + index + "}", shown(args[index]));
        }
        return out;
    }

    /**
     * Reconoce una frase de este vocabulario. Los argumentos numéricos vuelven como {@link Long}.
     *
     * @param text texto que escribió la carga
     * @return la frase y sus argumentos, o vacío si el texto es un dato
     */
    public static Optional<PhraseMatch> match(String text) {
        if (text == null || text.isEmpty()) {
            return Optional.empty();
        }
        return exact(text).or(() -> withArguments(text));
    }

    private static Optional<PhraseMatch> exact(String text) {
        for (IngestPhrase phrase : values()) {
            if (phrase.pattern == null && phrase.spanish.equals(text)) {
                return Optional.of(new PhraseMatch(phrase, List.of()));
            }
        }
        return Optional.empty();
    }

    private static Optional<PhraseMatch> withArguments(String text) {
        for (IngestPhrase phrase : values()) {
            Matcher matcher = phrase.pattern == null ? null : phrase.pattern.matcher(text);
            if (matcher != null && matcher.matches()) {
                return Optional.of(new PhraseMatch(phrase, arguments(matcher)));
            }
        }
        return Optional.empty();
    }

    private static String shown(Object arg) {
        if (arg instanceof Number number) {
            return Thousands.of(number.longValue());
        }
        return String.valueOf(arg);
    }

    private static List<Object> arguments(Matcher matcher) {
        List<Object> args = new ArrayList<>();
        for (int group = 1; group <= matcher.groupCount(); group++) {
            String value = matcher.group(group);
            if (Patterns.NUMBER_TEXT.matcher(value).matches()) {
                args.add(Long.parseLong(value.replace(" ", "")));
            } else {
                args.add(value);
            }
        }
        return List.copyOf(args);
    }

    private static Pattern compile(String template) {
        StringBuilder regex = new StringBuilder();
        Matcher marker = Patterns.PLACEHOLDER.matcher(template);
        int from = 0;
        while (marker.find()) {
            regex.append(Pattern.quote(template.substring(from, marker.start()))).append("(.+?)");
            from = marker.end();
        }
        regex.append(Pattern.quote(template.substring(from)));
        return Pattern.compile(regex.toString());
    }

    /** Patrones compartidos; una clase aparte porque el constructor del enum no puede leer sus estáticos. */
    private static final class Patterns {

        private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d)}");
        private static final Pattern NUMBER_TEXT = Pattern.compile("\\d+|\\d{1,3}(?: \\d{3})+");

        private Patterns() {
        }
    }
}

package com.bigfito.elastiflix.web.locale;

import java.util.Locale;
import java.util.Optional;

import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Idiomas admitidos por la interfaz web: español ({@code es}, por defecto) e inglés ({@code en}).
 */
public final class SupportedLocale {

    /** Idioma por defecto: español. */
    public static final Locale ES = Locale.of("es");

    /** Segundo idioma admitido: inglés. */
    public static final Locale EN = Locale.of("en");

    /** Nombre del parámetro de consulta que cambia el idioma. */
    public static final String PARAM_NAME = "lang";

    /** Nombre de la cookie donde se recuerda el idioma elegido. */
    public static final String COOKIE_NAME = "elastiflix-lang";

    private SupportedLocale() {
    }

    /**
     * Devuelve el idioma activo normalizado a {@code es} o {@code en}.
     *
     * @return {@link #EN} si el contexto actual es inglés; {@link #ES} en cualquier otro caso
     */
    public static Locale current() {
        return normalize(LocaleContextHolder.getLocale());
    }

    /**
     * Normaliza un {@link Locale} cualquiera a {@link #ES} o {@link #EN}.
     *
     * @param locale idioma recibido (puede ser {@code null})
     * @return {@link #EN} si su código de idioma es {@code en}; {@link #ES} en caso contrario
     */
    public static Locale normalize(Locale locale) {
        if (isEnglish(locale)) {
            return EN;
        }
        return ES;
    }

    /**
     * Indica si el idioma corresponde a inglés ({@code en}).
     *
     * @param locale idioma a comprobar
     * @return {@code true} cuando el idioma es {@code en}
     */
    public static boolean isEnglish(Locale locale) {
        return locale != null && "en".equalsIgnoreCase(locale.getLanguage());
    }

    /**
     * Interpreta el valor del parámetro {@code lang}. Solo acepta {@code es} o {@code en}.
     *
     * @param value valor recibido en la URL
     * @return el {@link Locale} correspondiente, o vacío si el valor no es válido
     */
    public static Optional<Locale> fromParam(String value) {
        if ("es".equals(value)) {
            return Optional.of(ES);
        }
        if ("en".equals(value)) {
            return Optional.of(EN);
        }
        return Optional.empty();
    }
}

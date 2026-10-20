package com.bigfito.elastiflix.web.locale;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;

import com.bigfito.elastiflix.core.error.ErrorCode;

import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Acceso centralizado a los catálogos {@code messages_es.properties} y {@code messages_en.properties},
 * incluyendo la traducción de códigos {@link ErrorCode} según el idioma activo.
 */
public final class UiMessages {

    private static final UiMessages DEFAULT_INSTANCE = new UiMessages(createMessageSource());

    private final MessageSource source;

    /**
     * Crea el componente sobre el {@link MessageSource} de la aplicación.
     *
     * @param source fuente de mensajes configurada
     */
    public UiMessages(MessageSource source) {
        this.source = source;
    }

    /**
     * Devuelve una instancia respaldada directamente por los catálogos UTF-8 del classpath,
     * útil en pruebas unitarias sin contexto de Spring.
     *
     * @return instancia por defecto de {@link UiMessages}
     */
    public static UiMessages defaultCatalog() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Construye el {@link MessageSource} UTF-8 con español como idioma base y sin caer al idioma del sistema.
     *
     * @return fuente de mensajes configurada
     */
    public static MessageSource createMessageSource() {
        ResourceBundleMessageSource bundle = new ApostropheSafeMessageSource();
        bundle.setBasename("messages");
        bundle.setDefaultEncoding(StandardCharsets.UTF_8.name());
        bundle.setDefaultLocale(SupportedLocale.ES);
        bundle.setFallbackToSystemLocale(false);
        return bundle;
    }

    /**
     * Obtiene un mensaje en el idioma activo de la petición actual.
     *
     * @param key  clave del catálogo
     * @param args argumentos posicionales opcionales
     * @return texto traducido
     */
    public String get(String key, Object... args) {
        return getFor(SupportedLocale.current(), key, args);
    }

    /**
     * Obtiene un mensaje en el idioma indicado.
     *
     * @param locale idioma destino
     * @param key    clave del catálogo
     * @param args   argumentos posicionales opcionales
     * @return texto traducido
     */
    public String getFor(Locale locale, String key, Object... args) {
        Locale target = SupportedLocale.normalize(locale);
        Object[] resolvedArgs = args != null && args.length == 0 ? null : args;
        return source.getMessage(key, resolvedArgs, target);
    }

    /**
     * Devuelve el resumen («qué pasó») de un {@link ErrorCode} en el idioma activo.
     *
     * @param code código de error
     * @return resumen traducido
     */
    public String errorSummary(ErrorCode code) {
        return errorSummary(code, SupportedLocale.current());
    }

    /**
     * Devuelve el resumen («qué pasó») de un {@link ErrorCode} en el idioma indicado.
     *
     * @param code   código de error
     * @param locale idioma destino
     * @return resumen traducido
     */
    public String errorSummary(ErrorCode code, Locale locale) {
        return getFor(locale, "error." + code.code() + ".summary");
    }

    /**
     * Devuelve la causa probable de un {@link ErrorCode} en el idioma indicado.
     *
     * @param code   código de error
     * @param locale idioma destino
     * @return causa probable traducida
     */
    public String errorCause(ErrorCode code, Locale locale) {
        return getFor(locale, "error." + code.code() + ".cause");
    }

    /**
     * Devuelve la acción sugerida de un {@link ErrorCode} en el idioma indicado.
     *
     * @param code   código de error
     * @param locale idioma destino
     * @return acción sugerida traducida
     */
    public String errorAction(ErrorCode code, Locale locale) {
        return getFor(locale, "error." + code.code() + ".action");
    }

    /**
     * Compone el mensaje completo de un {@link ErrorCode} («código · qué pasó. Causa probable / Likely cause...»)
     * en el idioma activo.
     *
     * @param code código de error
     * @return mensaje completo traducido
     */
    public String errorUserMessage(ErrorCode code) {
        return errorUserMessage(code, SupportedLocale.current());
    }

    /**
     * Compone el mensaje completo de un {@link ErrorCode} en el idioma indicado.
     *
     * @param code   código de error
     * @param locale idioma destino
     * @return mensaje completo traducido
     */
    public String errorUserMessage(ErrorCode code, Locale locale) {
        return getFor(
                locale,
                "error.userMessage.format",
                code.code(),
                errorSummary(code, locale),
                errorCause(code, locale),
                errorAction(code, locale));
    }

    private static final class ApostropheSafeMessageSource extends ResourceBundleMessageSource {

        @Override
        protected MessageFormat createMessageFormat(String msg, Locale locale) {
            String safe = msg == null ? "" : msg.replace("'", "''");
            return new MessageFormat(safe, locale);
        }
    }
}

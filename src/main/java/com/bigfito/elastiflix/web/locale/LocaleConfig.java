package com.bigfito.elastiflix.web.locale;

import java.time.Duration;
import java.util.Locale;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Configura el idioma de la aplicación con {@link CookieLocaleResolver} (español por defecto, sin usar
 * {@code Accept-Language}) y {@link LocaleChangeInterceptor} en el parámetro {@code lang}.
 */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    private static final Duration COOKIE_MAX_AGE = Duration.ofDays(365);

    /**
     * Fuente de mensajes UTF-8 respaldada por {@code messages_es.properties} y {@code messages_en.properties}.
     *
     * @return fuente de mensajes configurada
     */
    @Bean
    public MessageSource messageSource() {
        return UiMessages.createMessageSource();
    }

    /**
     * Componente de acceso al catálogo de mensajes de la interfaz.
     *
     * @param messageSource fuente de mensajes
     * @return instancia de {@link UiMessages}
     */
    @Bean
    public UiMessages uiMessages(MessageSource messageSource) {
        return new UiMessages(messageSource);
    }

    /**
     * Resuelve el idioma desde la cookie {@code elastiflix-lang}, usando siempre {@code es} por defecto.
     *
     * <p>Una cookie ilegible (por ejemplo {@code elastiflix-lang=e$}) se ignora y la página sale en español: con el
     * valor por defecto de Spring, cada página respondería 500 hasta que la persona borrara la cookie. La cookie es
     * {@code HttpOnly} porque ningún script la lee.</p>
     *
     * @return resolvedor de idioma basado en cookie
     */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new StrictCookieLocaleResolver();
        resolver.setDefaultLocale(SupportedLocale.ES);
        resolver.setCookieMaxAge(COOKIE_MAX_AGE);
        resolver.setCookiePath("/");
        resolver.setCookieHttpOnly(true);
        resolver.setRejectInvalidCookies(false);
        return resolver;
    }

    /**
     * Interceptor que cambia el idioma con el parámetro {@code lang}, aceptando únicamente {@code es} o {@code en}.
     *
     * @return interceptor de cambio de idioma
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new StrictLocaleChangeInterceptor();
        interceptor.setParamName(SupportedLocale.PARAM_NAME);
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    private static final class StrictCookieLocaleResolver extends CookieLocaleResolver {

        private StrictCookieLocaleResolver() {
            super(SupportedLocale.COOKIE_NAME);
        }

        @Override
        public Locale resolveLocale(HttpServletRequest request) {
            return SupportedLocale.normalize(super.resolveLocale(request));
        }
    }

    private static final class StrictLocaleChangeInterceptor extends LocaleChangeInterceptor {

        @Override
        public boolean preHandle(
                HttpServletRequest request, HttpServletResponse response, Object handler) throws ServletException {
            String param = request.getParameter(getParamName());
            return param != null && SupportedLocale.fromParam(param).isEmpty()
                    || super.preHandle(request, response, handler);
        }

        @Override
        protected Locale parseLocaleValue(String localeValue) {
            return SupportedLocale.fromParam(localeValue).orElse(null);
        }
    }
}

package com.bigfito.elastiflix.core.client;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.MutableCoercionConfig;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;

/**
 * Construye el {@link JsonMapper} con el que se leen y escriben películas.
 */
public final class MovieJsonMapper {

    private MovieJsonMapper() {
    }

    /**
     * Crea el mapper: snake_case, fechas {@code yyyy-MM-dd} y un {@code id} numérico guardado como texto.
     *
     * <p>{@code plot256} se escribe {@code plot_256}. El snake_case de Jackson no pone el guion
     * bajo antes de un número.</p>
     *
     * @return mapper de películas
     */
    public static JsonMapper create() {
        return JsonMapper.builder()
                .findAndAddModules()
                .propertyNamingStrategy(new MoviePropertyNames())
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .withCoercionConfig(LogicalType.Textual, MovieJsonMapper::acceptNumbersAsText)
                .build();
    }

    private static void acceptNumbersAsText(MutableCoercionConfig config) {
        config.setCoercion(CoercionInputShape.Integer, CoercionAction.TryConvert);
        config.setCoercion(CoercionInputShape.Float, CoercionAction.TryConvert);
    }

    /**
     * snake_case que también separa una letra de un número: {@code plot256} queda {@code plot_256}.
     */
    private static final class MoviePropertyNames extends SnakeCaseStrategy {

        /**
         * Traduce el nombre Java al nombre del JSON.
         *
         * @param input nombre del componente
         * @return nombre en el índice
         */
        @Override
        public String translate(String input) {
            return separateDigits(super.translate(input));
        }

        private static String separateDigits(String snake) {
            StringBuilder separated = new StringBuilder(snake.length() + 2);
            for (int index = 0; index < snake.length(); index++) {
                char current = snake.charAt(index);
                boolean digitAfterLetter = index > 0
                        && Character.isDigit(current)
                        && Character.isLetter(snake.charAt(index - 1));
                if (digitAfterLetter) {
                    separated.append('_');
                }
                separated.append(current);
            }
            return separated.toString();
        }
    }
}

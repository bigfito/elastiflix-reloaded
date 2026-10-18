package com.bigfito.elastiflix.core.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj UTC para los nombres de índice.
 */
@Configuration
public class UtcClockConfig {

    /**
     * Reloj del sistema en UTC.
     *
     * @return reloj UTC
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

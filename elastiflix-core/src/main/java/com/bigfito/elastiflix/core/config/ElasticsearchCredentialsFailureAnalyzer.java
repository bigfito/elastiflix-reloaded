package com.bigfito.elastiflix.core.config;

import com.bigfito.elastiflix.core.error.ConfigurationException;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Explica una {@link ConfigurationException} sin mostrar la URL ni la API key.
 */
public final class ElasticsearchCredentialsFailureAnalyzer
        extends AbstractFailureAnalyzer<ConfigurationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, ConfigurationException cause) {
        String message = cause.errorCode().userMessage();
        return new FailureAnalysis(message, message, cause);
    }
}

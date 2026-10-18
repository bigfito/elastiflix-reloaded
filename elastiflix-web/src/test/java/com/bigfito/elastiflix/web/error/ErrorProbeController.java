package com.bigfito.elastiflix.web.error;

import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Dispara una excepción real para que el consejo de errores tenga algo que mostrar.
 */
@Controller
public class ErrorProbeController {

    /**
     * Simula una falla de Elasticsearch que ninguna página captura.
     *
     * @return no llega a una vista
     */
    @GetMapping("/probe/elastiflix")
    public String fail() {
        throw new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE);
    }
}

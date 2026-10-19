package com.bigfito.elastiflix.ingest.job;

import java.util.List;
import java.util.Set;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.mapping.MappingValidator;
import com.bigfito.elastiflix.ingest.mapping.ValidatedMovies;
import com.bigfito.elastiflix.ingest.mapping.VectorFields;
import com.bigfito.elastiflix.ingest.vector.PlotVectorizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Valida un registro, le genera los vectores y lo escribe directo en el alias.
 */
@Component
public final class IndividualRecord {

    private static final Logger LOG = LoggerFactory.getLogger(IndividualRecord.class);
    private static final JsonMapper MAPPER = MovieJsonMapper.create();

    private final MappingValidator validator;
    private final PlotVectorizer vectorizer;
    private final CatalogIndex catalog;

    /**
     * Crea el guardado de un solo registro.
     *
     * @param validator  las mismas reglas que un archivo
     * @param vectorizer vectores de esa sinopsis
     * @param catalog    escribe en el alias
     */
    public IndividualRecord(MappingValidator validator, PlotVectorizer vectorizer, CatalogIndex catalog) {
        this.validator = validator;
        this.vectorizer = vectorizer;
        this.catalog = catalog;
    }

    /**
     * Guarda la película si no hay errores. Un fallo de campos no llama al catálogo.
     *
     * @param alias alias del catálogo
     * @param movie campos del formulario; los vectores no se usan
     * @return la película guardada, o el informe
     */
    public SaveResult save(String alias, Movie movie) {
        ValidatedMovies checked = validator.validate(List.of(withoutVectors(movie)), Set.of());
        if (!checked.report().accepted()) {
            return new SaveResult.Rejected(checked.report());
        }
        Movie ready = vectorizer.apply(checked.movies()).getFirst();
        catalog.indexOne(alias, ready);
        LOG.info("Se guardó el registro {}", ready.id());
        return new SaveResult.Saved(ready);
    }

    private static ObjectNode withoutVectors(Movie movie) {
        ObjectNode node = (ObjectNode) MAPPER.valueToTree(movie);
        node.remove(VectorFields.NAMES);
        return node;
    }
}

package com.bigfito.elastiflix.ingest.job;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.mapping.VectorFields;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Arma el JSON de la prueba: las 4 películas sin vectores, el título de 597 cambiado y una quinta.
 */
final class ImportSample {

    static final String REPLACED_TITLE = "Titanic (edición de prueba)";
    static final String EXTRA_ID = "990000001";
    static final String TITANIC = "597";
    static final String SOURCE_OF_EXTRA = "10681";
    private static final List<String> DROPPED = dropped();

    private final JsonMapper mapper = MovieJsonMapper.create();
    private final List<Movie> fixture;
    private final ArrayNode documents;

    private ImportSample(List<Movie> fixture, ArrayNode documents) {
        this.fixture = fixture;
        this.documents = documents;
    }

    static ImportSample read(Path fixture) {
        JsonMapper mapper = MovieJsonMapper.create();
        ArrayNode documents = (ArrayNode) mapper.readTree(fixture.toFile()).get("documents");
        return new ImportSample(movies(mapper, documents), documents.deepCopy());
    }

    List<Movie> fixture() {
        return fixture;
    }

    List<String> originalIds() {
        return fixture.stream().map(Movie::id).toList();
    }

    Path writeImportFile() throws Exception {
        ArrayNode clean = mapper.createArrayNode();
        ObjectNode extra = null;
        for (JsonNode document : documents) {
            ObjectNode copy = ((ObjectNode) document).deepCopy();
            if (SOURCE_OF_EXTRA.equals(text(copy.get("id")))) {
                extra = copy.deepCopy();
            }
            rewrite(copy, text(copy.get("id")));
            clean.add(copy);
        }
        clean.add(extraMovie(extra));
        Path path = Files.createTempFile("elastiflix-it-", ".json");
        mapper.writeValue(path.toFile(), clean);
        return path;
    }

    private ObjectNode extraMovie(ObjectNode source) {
        ObjectNode extra = source.deepCopy();
        extra.put("id", EXTRA_ID);
        extra.remove(DROPPED);
        return extra;
    }

    private static void rewrite(ObjectNode document, String id) {
        document.remove(DROPPED);
        if (TITANIC.equals(id)) {
            document.put("title", REPLACED_TITLE);
        }
    }

    private static List<String> dropped() {
        List<String> names = new ArrayList<>(VectorFields.NAMES);
        names.add("_id");
        return List.copyOf(names);
    }

    private static List<Movie> movies(JsonMapper mapper, ArrayNode documents) {
        List<Movie> movies = new ArrayList<>();
        for (JsonNode document : documents) {
            movies.add(mapper.treeToValue(document, Movie.class));
        }
        return movies;
    }

    private static String text(JsonNode id) {
        return id == null ? "" : id.asString();
    }
}

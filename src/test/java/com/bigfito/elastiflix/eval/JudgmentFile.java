package com.bigfito.elastiflix.eval;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lee {@code eval/judgments.jsonl}: una consulta por línea, en UTF-8.
 */
public final class JudgmentFile {

    /** Ruta del archivo, relativa a la raíz del repositorio. */
    public static final Path DEFAULT = Path.of("eval", "judgments.jsonl");

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final int MAX_RATING = 3;

    private JudgmentFile() {
    }

    /**
     * Lee y valida el archivo. Las líneas vacías se saltan.
     *
     * @param path archivo JSON Lines
     * @return las consultas en el orden del archivo
     * @throws IllegalStateException si un id se repite, si la película buscada no tiene 3 o si una calificación
     *                               sale de 0 a 3
     */
    public static List<Judgment> read(Path path) {
        List<Judgment> judgments = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (String line : lines(path)) {
            if (line.isBlank()) {
                continue;
            }
            Judgment judgment = JSON.readValue(line, Judgment.class);
            check(judgment, ids);
            judgments.add(judgment);
        }
        return judgments;
    }

    private static List<String> lines(Path path) {
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + path, e);
        }
    }

    private static void check(Judgment judgment, Set<String> ids) {
        if (!ids.add(judgment.id())) {
            throw new IllegalStateException("La consulta " + judgment.id() + " está repetida");
        }
        Integer target = judgment.ratings().get(judgment.target());
        if (target == null || target != Judgment.TARGET_RATING) {
            throw new IllegalStateException(judgment.id() + ": la película buscada debe tener calificación 3");
        }
        judgment.ratings().forEach((movie, rating) -> {
            if (rating < 0 || rating > MAX_RATING) {
                throw new IllegalStateException(judgment.id() + ": la película " + movie
                        + " tiene calificación " + rating + "; va de 0 a 3");
            }
        });
    }
}

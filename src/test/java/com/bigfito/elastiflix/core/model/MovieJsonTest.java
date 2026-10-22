package com.bigfito.elastiflix.core.model;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El fixture de cuatro películas sobrevive a una ida y vuelta por JSON.
 */
class MovieJsonTest {

    private static final Path FIXTURE = Path.of("elasticsearch", "fixtures", "movies-sample.json");
    private static final String EQUALIZER_ID = "345887";

    private final JsonMapper mapper = MovieJsonMapper.create();

    @Test
    @DisplayName("lee, reescribe y conserva las cuatro películas, incluida la colección de 345887")
    void shouldKeepValuesWhenMoviesAreWrittenAgain() {
        List<Movie> movies = readMovies();

        assertThat(movies).hasSize(4);
        assertThat(movies).extracting(Movie::id).containsExactly("597", "27205", "10681", EQUALIZER_ID);
        Movie titanic = movie(movies, "597");
        assertThat(titanic.releaseDate()).isEqualTo(LocalDate.parse("1997-11-18"));
        assertThat(titanic.plot256()).hasSize(256);
        assertThat(titanic.plot512()).hasSize(512);
        assertThat(titanic.plot1024()).hasSize(1024);
        assertThat(titanic.belongsToCollection()).isNull();

        Movie equalizer = movie(movies, EQUALIZER_ID);
        assertThat(equalizer.belongsToCollection().id()).isEqualTo(523855L);
        assertThat(equalizer.belongsToCollection().name()).isEqualTo("The Equalizer Collection");

        for (Movie movie : movies) {
            Movie again = mapper.readValue(mapper.writeValueAsString(movie), Movie.class);
            assertThat(again)
                    .usingRecursiveComparison()
                    .withEqualsForType(Arrays::equals, float[].class)
                    .isEqualTo(movie);
        }
    }

    private List<Movie> readMovies() {
        JsonNode documents = mapper.readTree(FIXTURE.toFile()).get("documents");
        List<Movie> movies = new ArrayList<>();
        for (JsonNode document : documents) {
            movies.add(mapper.treeToValue(document, Movie.class));
        }
        return movies;
    }

    private static Movie movie(List<Movie> movies, String id) {
        return movies.stream()
                .filter(movie -> id.equals(movie.id()))
                .findFirst()
                .orElseThrow();
    }
}

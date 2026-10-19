package com.bigfito.elastiflix.web.detail;

import java.util.List;

import com.bigfito.elastiflix.core.model.MovieKeyword;

/**
 * Modelo completo para renderizar la plantilla de detalle de película.
 *
 * @param id            identificador de la película
 * @param title         título principal
 * @param originalTitle título original
 * @param tagline       lema publicitario
 * @param overview      resumen corto
 * @param plot          sinopsis completa
 * @param posterUrl     URL completa del póster en CDN o vacía
 * @param backdropUrl   URL completa de la imagen de fondo en CDN o vacía
 * @param voteAverage   promedio de votos formateado
 * @param voteCount     número de votos formateado con miles
 * @param popularity    índice de popularidad formateado
 * @param imdbHref      enlace al título en IMDb o vacío
 * @param homepage      sitio web oficial o vacío
 * @param hasHomepage   si tiene sitio oficial
 * @param pills         píldoras informativas rápidas
 * @param genres        géneros con sus colores
 * @param facts         filas de la ficha técnica
 * @param production    filas de producción e idiomas
 * @param numbers       filas de cifras financieras y estadísticas
 * @param hasCollection si pertenece a alguna colección
 * @param collName      nombre de la colección o vacío
 * @param collection    filas de la colección
 * @param images        filas de imágenes
 * @param cast          lista de reparto completa
 * @param castCount     total de personas en el reparto
 * @param keywords      palabras clave
 * @param keywordCount  total de palabras clave
 * @param searchState   elementos del estado de búsqueda previo
 * @param returnHref    enlace de retorno a /buscar?...#r-{id}
 */
public record MovieDetailView(
        String id,
        String title,
        String originalTitle,
        String tagline,
        String overview,
        String plot,
        String posterUrl,
        String backdropUrl,
        String voteAverage,
        String voteCount,
        String popularity,
        String imdbHref,
        String homepage,
        boolean hasHomepage,
        List<DetailPill> pills,
        List<GenreBadge> genres,
        List<FactRow> facts,
        List<FactRow> production,
        List<FactRow> numbers,
        boolean hasCollection,
        String collName,
        List<FactRow> collection,
        List<FactRow> images,
        List<String> cast,
        int castCount,
        List<MovieKeyword> keywords,
        int keywordCount,
        List<SearchStateChip> searchState,
        String returnHref) {

    /**
     * Copia inmutable de las listas.
     */
    public MovieDetailView {
        pills = List.copyOf(pills);
        genres = List.copyOf(genres);
        facts = List.copyOf(facts);
        production = List.copyOf(production);
        numbers = List.copyOf(numbers);
        collection = List.copyOf(collection);
        images = List.copyOf(images);
        cast = List.copyOf(cast);
        keywords = List.copyOf(keywords);
        searchState = List.copyOf(searchState);
    }
}

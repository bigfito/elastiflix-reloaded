# 🎬 Datos del catálogo · Catalog data

**Español** · [English](#english)

Exportación completa del catálogo `elastiflix-reloaded`: **6 899 películas** repartidas en **7 partes**, cada una en
`.json` y en `.csv` con el mismo contenido. Las partes están ordenadas por `id` y no se solapan.

| Parte | Películas | `id` desde – hasta | `.json` | `.csv` |
|---|---|---|---|---|
| `elastiflix-reloaded_01` | 986 | 11 – 9965 | 4,3 MB | 3,4 MB |
| `elastiflix-reloaded_02` | 986 | 9966 – 24238 | 3,9 MB | 3,1 MB |
| `elastiflix-reloaded_03` | 986 | 24271 – 103332 | 4,0 MB | 3,2 MB |
| `elastiflix-reloaded_04` | 986 | 103370 – 324849 | 4,0 MB | 3,2 MB |
| `elastiflix-reloaded_05` | 985 | 324852 – 470035 | 4,0 MB | 3,2 MB |
| `elastiflix-reloaded_06` | 985 | 470044 – 649520 | 4,0 MB | 3,2 MB |
| `elastiflix-reloaded_07` | 985 | 649918 – 1059999 | 3,8 MB | 3,0 MB |

## Cómo usarlos

Cada parte es un **catálogo completo por sí sola** y está por debajo del tope de 25 MB de «Cargar datos». Se carga
desde la pestaña «Importar .json» o «Importar .csv», en cualquiera de los dos modos:

- **Bulk:** reemplaza el catálogo con las películas de esa parte (~985). Sirve para tener un catálogo más chico, por
  ejemplo para una demo.
- **Append:** agrega las películas de esa parte al catálogo actual; un `id` que ya existe se reemplaza.

Para tener el **catálogo completo de 6 899 películas**, carga `_01` en **bulk** y después `_02` a `_07` en
**append**, en el formato que prefieras.

## Formato

- **Sin vectores:** `plot_256`, `plot_512` y `plot_1024` no vienen en los archivos. La carga siempre los genera a
  partir de `plot` con el servicio de inferencia de Elasticsearch.
- **`.json`:** el mismo formato que `elasticsearch/fixtures/movies-sample.json`: un objeto con `documents` (las
  películas), `document_count` (cuántas trae esa parte), `mapping` y notas. Cada película lleva `_id` igual a `id`.
- **`.csv`:** UTF-8 con BOM, separador coma, encabezado en la primera fila y `\r\n` entre filas. Las listas y los
  objetos anidados van como JSON dentro de la celda; un valor vacío es una celda vacía. Al cargarlo, deja el separador
  en «Coma» y marca «La primera fila contiene los nombres de columna».
- **Procedencia:** `enrichment` es un objeto al final de cada película con la fuente de cada campo; toda película
  lo trae. En `.csv` es la última columna: JSON compacto, sin espacios, con las claves en el orden del mapping
  (`{"plot":"tmdb_overview"}`). Es la misma convención que `belongs_to_collection` y `keywords`.
  La etiqueta `enrichment.plot` dice de dónde salió la sinopsis:

  | Valor | Significa |
  |---|---|
  | `wikipedia_plot_AAAA-MM-DD` | Sección de trama del artículo de Wikipedia, descargada esa fecha |
  | `tmdb_overview` | Overview de TMDB |
  | `tmdb_overview_translated:<idioma>` | Overview de TMDB traducido (una película, `zh-CN`) |
  | `tmdb_metadata_summary` | Resumen armado con metadatos de TMDB (una película) |
  | `unverified` | Origen desconocido; el texto se mantiene mientras no haya fuente verificable |
  En los demás campos la etiqueta suele ser `tmdb_api_AAAA-MM-DD` (la fecha de la descarga de TMDB).
  `wikipedia_infobox` marca un monto (`budget` o `revenue`) tomado de la ficha del artículo de Wikipedia (WP-09).
  `revenue` 618 de *Money Plane* no es un error: la ficha y Box Office Mojo dan $618.
- **Contenido para adultos:** ninguna de las 6 899 películas tiene `adult = true`. La búsqueda mantiene el filtro
  `adult` para los catálogos que cargue un usuario (D-04).
- Los campos son los del mapping (`elasticsearch/elastiflix-reloaded-mapping.json`); «Acerca de» › «El catálogo»
  describe cada uno.

## Palabras clave con dos ids

«horror», «thriller» y «comedy» tienen dos ids en el catálogo. No es un id que pertenezca a otra palabra: los dos
ids nombraban la misma palabra en TMDB, así que se dejan los dos.

| Nombre | Ids en el catálogo | Ficha pública el 2026-11-13 |
|---|---|---|
| horror | `8087` (69 películas) y `315058` (1) | [`315058`](https://www.themoviedb.org/keyword/315058); `8087` ya no tiene página |
| thriller | `9950` (26) y `316362` (2) | [`316362`](https://www.themoviedb.org/keyword/316362); `9950` ya no tiene página |
| comedy | `289153` (1) y `322268` (3) | [`322268`](https://www.themoviedb.org/keyword/322268); `289153` ya no tiene página |

## Créditos y aviso de uso

Estos datos se publican con fines exclusivamente académicos y demostrativos, sin ningún lucro comercial. Los créditos
de la información corresponden a [TMDB](https://www.themoviedb.org/) (datos de las películas y pósters) y a
[Wikipedia](https://www.wikipedia.org/) (las sinopsis con `enrichment.plot = wikipedia_plot_…`, bajo la licencia
[CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/deed.es)). *This product uses the TMDB API but is not endorsed or certified by TMDB.*

---

## English

A full export of the `elastiflix-reloaded` catalog: **6,899 movies** split into **7 parts**, each one in `.json` and
in `.csv` with the same content. The parts are sorted by `id` and don't overlap (see the table above).

### How to use them

Each part is a **complete catalog on its own** and stays under the 25 MB limit of «Load data». Load it from the
«Import .json» or «Import .csv» tab, in either mode:

- **Bulk:** replaces the catalog with the movies in that part (~985). Useful for a smaller catalog, for example for a
  demo.
- **Append:** adds the movies in that part to the current catalog; an `id` that already exists is replaced.

To get the **full 6,899-movie catalog**, load `_01` in **bulk** and then `_02` to `_07` in **append**, in the format
you prefer.

### Format

- **No vectors:** `plot_256`, `plot_512`, and `plot_1024` are not in the files. The import always generates them from
  `plot` with the Elasticsearch inference service.
- **`.json`:** the same format as `elasticsearch/fixtures/movies-sample.json`: an object with `documents` (the movies),
  `document_count` (how many the part holds), `mapping`, and notes. Every movie has `_id` equal to `id`.
- **`.csv`:** UTF-8 with BOM, comma separator, a header in the first row, and `\r\n` between rows. Lists and nested
  objects are JSON inside the cell; an empty value is an empty cell. When loading it, keep the separator on «Comma»
  and check «The first row contains the column names».
- **Provenance:** `enrichment` is an object at the end of each movie with the source of each field; every movie
  carries one. In `.csv` it is the last column: compact JSON, no spaces, keys in mapping order
  (`{"plot":"tmdb_overview"}`). Same convention as `belongs_to_collection` and `keywords`.
  The `enrichment.plot` tag says where the synopsis came from:

  | Value | Meaning |
  |---|---|
  | `wikipedia_plot_YYYY-MM-DD` | Plot section of the Wikipedia article, downloaded on that date |
  | `tmdb_overview` | TMDB overview |
  | `tmdb_overview_translated:<lang>` | Translated TMDB overview (one movie, `zh-CN`) |
  | `tmdb_metadata_summary` | Summary built from TMDB metadata (one movie) |
  | `unverified` | Unknown origin; the text stays until a verifiable source exists |
  In the other fields the tag is usually `tmdb_api_YYYY-MM-DD` (the date of the TMDB download).
  `wikipedia_infobox` marks an amount (`budget` or `revenue`) taken from the Wikipedia article's infobox (WP-09).
  The `revenue` of 618 for *Money Plane* is not an error: the infobox and Box Office Mojo give $618.
- **Adult content:** none of the 6,899 movies has `adult = true`. The search keeps the `adult` filter for catalogs a
  user loads (D-04).
- The fields are those of the mapping (`elasticsearch/elastiflix-reloaded-mapping.json`); «About the demo» › «The
  catalog» describes each one.

### Keywords with two ids

`horror`, `thriller`, and `comedy` each have two ids in the catalog. This is not an id that belongs to a different
word: both ids named the same word on TMDB, so both stay.

| Name | Ids in the catalog | Public page on 2026-11-13 |
|---|---|---|
| horror | `8087` (69 movies) and `315058` (1) | [`315058`](https://www.themoviedb.org/keyword/315058); `8087` no longer has a page |
| thriller | `9950` (26) and `316362` (2) | [`316362`](https://www.themoviedb.org/keyword/316362); `9950` no longer has a page |
| comedy | `289153` (1) and `322268` (3) | [`322268`](https://www.themoviedb.org/keyword/322268); `289153` no longer has a page |

### Credits and terms of use

This data is published for academic and demonstration purposes only, with no commercial profit. Credit for the
information goes to [TMDB](https://www.themoviedb.org/) (movie data and posters) and to
[Wikipedia](https://www.wikipedia.org/) (the synopses tagged `enrichment.plot = wikipedia_plot_…`, under the [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) license). *This product uses
the TMDB API but is not endorsed or certified by TMDB.*

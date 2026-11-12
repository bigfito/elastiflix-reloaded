# 🎬 Datos del catálogo · Catalog data

**Español** · [English](#english)

Exportación completa del catálogo `elastiflix-reloaded`: **6 899 películas** repartidas en **7 partes**, cada una en
`.json` y en `.csv` con el mismo contenido. Las partes están ordenadas por `id` y no se solapan.

| Parte | Películas | `id` desde – hasta | `.json` | `.csv` |
|---|---|---|---|---|
| `elastiflix-reloaded_01` | 986 | 11 – 9965 | 4,7 MB | 3,8 MB |
| `elastiflix-reloaded_02` | 986 | 9966 – 24238 | 4,2 MB | 3,4 MB |
| `elastiflix-reloaded_03` | 986 | 24271 – 103332 | 4,3 MB | 3,4 MB |
| `elastiflix-reloaded_04` | 986 | 103370 – 324849 | 4,3 MB | 3,5 MB |
| `elastiflix-reloaded_05` | 985 | 324852 – 470035 | 4,3 MB | 3,5 MB |
| `elastiflix-reloaded_06` | 985 | 470044 – 649520 | 4,2 MB | 3,4 MB |
| `elastiflix-reloaded_07` | 985 | 649918 – 1059999 | 3,9 MB | 3,1 MB |

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
- **Procedencia:** `enrichment` es un objeto opcional al final de cada película. En `.json`, `null` significa que no
  hay etiquetas (solo la 751432, *Merlin's Apprentice*). En `.csv` es la última columna: JSON compacto, sin espacios,
  con las claves en el orden del mapping (`{"plot":"tmdb_overview"}`); una celda vacía es null. Es la misma convención
  que `belongs_to_collection` y `keywords`.
- Los campos son los del mapping (`elasticsearch/elastiflix-reloaded-mapping.json`); «Acerca de» › «El catálogo»
  describe cada uno.

## Créditos y aviso de uso

Estos datos se publican con fines exclusivamente académicos y demostrativos, sin ningún lucro comercial. Los créditos
de la información corresponden a [TMDB](https://www.themoviedb.org/) (datos de las películas y pósters) y a
[Wikipedia](https://www.wikipedia.org/) (textos de las películas, bajo la licencia
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
- **Provenance:** `enrichment` is an optional object at the end of each movie. In `.json`, `null` means there are no
  tags (only 751432, *Merlin's Apprentice*). In `.csv` it is the last column: compact JSON, no spaces, keys in mapping
  order (`{"plot":"tmdb_overview"}`); an empty cell is null. Same convention as `belongs_to_collection` and `keywords`.
- The fields are those of the mapping (`elasticsearch/elastiflix-reloaded-mapping.json`); «About the demo» › «The
  catalog» describes each one.

### Credits and terms of use

This data is published for academic and demonstration purposes only, with no commercial profit. Credit for the
information goes to [TMDB](https://www.themoviedb.org/) (movie data and posters) and to
[Wikipedia](https://www.wikipedia.org/) (movie texts, under the [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) license). *This product uses
the TMDB API but is not endorsed or certified by TMDB.*

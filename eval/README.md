# 🎯 Harness de relevancia

**Español** · [English](#-relevance-harness)

Mide con [`_rank_eval`](https://www.elastic.co/docs/reference/elasticsearch/rest-apis/search-rank-eval) qué tan bien
ordena cada modo de búsqueda un conjunto fijo de consultas con juicios graduados, y falla si alguna métrica baja de su
mínimo.

## Qué compara

| Configuración | Qué es |
|---|---|
| `bm25_title` | Full-text, `match` sobre `title` (lo que hace la aplicación por defecto) |
| `bm25_multi_match` | Full-text, `multi_match` sobre `title` y `original_title` |
| `knn_plot_256` · `knn_plot_512` · `knn_plot_1024` | Vectorial con cada número de dimensiones |
| `rrf_plot_256` · `rrf_plot_1024` | Híbrida RRF: `match` sobre `title` más k-NN |

Todas usan los valores por defecto de la página de Inicio (`OR`, `k = 100`, `num_candidates = 200`). Las peticiones
salen de los **mismos armadores que usa la aplicación** y el vector de cada consulta del mismo servicio de inferencia,
así que el harness mide lo que la aplicación envía.

## Métricas

Todas sobre los 10 primeros resultados. Para MRR, precision y recall, una película es relevante con calificación 2 o 3;
nDCG usa la calificación completa. Una película sin calificar cuenta como no relevante.

| Métrica | Pregunta que responde |
|---|---|
| `mrr@10` | ¿En qué posición aparece la primera película relevante? |
| `p@10` | ¿Qué fracción de los 10 resultados es relevante? |
| `recall@10` | ¿Qué fracción de las películas relevantes aparece en los 10 primeros? |
| `ndcg@10` | ¿Están las mejores calificaciones arriba? |

## Archivos

| Archivo | Contenido |
|---|---|
| [`judgments.jsonl`](judgments.jsonl) | 40 consultas (20 en español, 20 en inglés; 30 describen la trama y 10 buscan por título) con sus juicios |
| [`thresholds.json`](thresholds.json) | Mínimo de cada métrica por configuración: el gate |

Cada línea de `judgments.jsonl`:

```json
{"id": "es-plot-01", "lang": "es", "kind": "plot", "text": "un transatlántico de lujo choca con un iceberg…",
 "target": "597", "ratings": {"597": 3, "44918": 2, "1701": 0}}
```

Los juicios van por `id` de película, no por índice: cada carga bulk crea un índice nuevo y el harness resuelve el
alias en cada corrida.

### Escala

| Calificación | Consulta de trama (`plot`) | Consulta de título (`title`) |
|---|---|---|
| **3** | La película buscada, o una nueva versión de la misma historia | La película buscada, o una nueva versión |
| **2** | Cumple lo esencial de la descripción | Otra película de la saga, si la consulta nombra la saga en general («star wars») |
| **1** | Comparte un elemento central, pero cuenta otra historia | Secuela, precuela o derivada, si la consulta nombra una película concreta |
| **0** | No relevante: solo coinciden palabras sueltas o el género | No relevante |

## Cómo correrlo

Necesita `.env` y el catálogo de referencia de 6 899 películas ([`data/`](../data/README.md)); con otro catálogo, la
prueba se omite. Solo lee: no crea ni borra índices.

```bash
./mvnw verify -Pit -Dit.test=RelevanceIT
```

También corre con el resto de las pruebas de integración en `./mvnw verify -Pit`. Deja dos archivos en `target/eval/`:

- `report.md`: promedios por configuración, desglose por idioma y tipo de consulta, y la posición de la película
  buscada en cada consulta.
- `pending.jsonl`: películas que aparecen en algún top 10 sin juicio, con su título, año y resumen. Para calificarlas,
  agrega cada `movie` con su calificación a `ratings` de su consulta en `judgments.jsonl` y vuelve a correr.

Si cambias el modelo, el mapping o una configuración y las métricas mejoran, sube los mínimos de `thresholds.json`.
Si bajan a propósito, explica el cambio en el commit. Los mínimos dejan margen porque el k-NN es aproximado: al
reconstruir el índice, una película al borde puede moverse un lugar.

---

# 🎯 Relevance harness

[Español](#-harness-de-relevancia) · **English**

It uses [`_rank_eval`](https://www.elastic.co/docs/reference/elasticsearch/rest-apis/search-rank-eval) to measure how
well each search mode ranks a fixed set of queries with graded judgments, and it fails if any metric drops below its
minimum.

## What it compares

| Configuration | What it is |
|---|---|
| `bm25_title` | Full-text, `match` on `title` (the app's default) |
| `bm25_multi_match` | Full-text, `multi_match` on `title` and `original_title` |
| `knn_plot_256` · `knn_plot_512` · `knn_plot_1024` | Vector search with each number of dimensions |
| `rrf_plot_256` · `rrf_plot_1024` | Hybrid RRF: `match` on `title` plus k-NN |

They all use the Home page defaults (`OR`, `k = 100`, `num_candidates = 200`). The requests come from **the same
builders the app uses**, and each query vector from the same inference service, so the harness measures what the app
sends.

## Metrics

All over the top 10 results. For MRR, precision, and recall, a movie is relevant with a rating of 2 or 3; nDCG uses the
full rating. An unrated movie counts as not relevant.

| Metric | Question it answers |
|---|---|
| `mrr@10` | At what position does the first relevant movie appear? |
| `p@10` | What fraction of the 10 results is relevant? |
| `recall@10` | What fraction of the relevant movies appears in the top 10? |
| `ndcg@10` | Are the best ratings at the top? |

## Files

| File | Contents |
|---|---|
| [`judgments.jsonl`](judgments.jsonl) | 40 queries (20 in Spanish, 20 in English; 30 describe the plot and 10 search by title) with their judgments |
| [`thresholds.json`](thresholds.json) | The minimum of each metric per configuration: the gate |

Judgments are keyed by movie `id`, not by index: every bulk load creates a new index, and the harness resolves the
alias on each run. The rating scale is the table in the Spanish section above: **3** the movie sought (or a remake),
**2** matches the essentials of the description (or, for a franchise query, another film of the franchise), **1**
shares one central element (or is a sequel when the query names a specific film), **0** not relevant.

## How to run it

It needs `.env` and the 6,899-movie reference catalog ([`data/`](../data/README.md)); with any other catalog the test
is skipped. It only reads: it never creates or deletes indices.

```bash
./mvnw verify -Pit -Dit.test=RelevanceIT
```

It also runs with the rest of the integration tests in `./mvnw verify -Pit`. It leaves two files in `target/eval/`:

- `report.md`: averages per configuration, a breakdown by language and query kind, and the position of the movie sought
  for each query.
- `pending.jsonl`: movies that appear in some top 10 without a judgment, with their title, year, and overview. To rate
  them, add each `movie` with its rating to its query's `ratings` in `judgments.jsonl` and run it again.

If you change the model, the mapping, or a configuration and the metrics improve, raise the minimums in
`thresholds.json`. If they drop on purpose, explain the change in the commit. The minimums leave a margin because k-NN
is approximate: when the index is rebuilt, a movie at the edge can move one place.

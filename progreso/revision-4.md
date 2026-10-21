# Estado de trabajo — Revisión 4

> Archivo de estado para retomar el trabajo en otra sesión sin perder avance. Especificación:
> [`docs/revisiones/revision-4.md`](../docs/revisiones/revision-4.md). Reglas en `PLAN.md` §9.

## Identificación

- **Unidad:** Revisión 4
- **Nombre:** Correcciones de la cuarta revisión (tras las fases 7 y 8)
- **Rama:** `revision-4`
- **Carpeta:** `E:\02 - programming\03 - elastiflix-reloaded`
- **Herramienta de la última sesión:** Claude Code (Opus 5.5)

## Estado general

en curso

## Punto de reanudación

- **Tarea actual:** U-17 (textos de la cabecera y etiquetas de las sugerencias, al catálogo)
- **Subpaso en curso:** pasar `CatalogTexts` y las etiquetas de `*Suggestions` a `messages_*.properties`
- **Próximo paso:** claves `layout.catalog.*` y `home.suggest.*`, con sus pruebas
- **Última actualización:** 2026-10-21 19:40 UTC

## Tareas

> El hash de una tarea se completa en el commit de la tarea siguiente.

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| U-11 | terminada | `a1b8fa8` | `./mvnw verify` en verde (386 unitarias); `AppliedFiltersTest` en español e inglés |
| U-03 | terminada | `ec6ca83` | `./mvnw verify` en verde; `ExternalLinksTest` (16 casos) y `MovieDetailPageTest` |
| U-01 | terminada | `59d2662` | Medición del 2026-10-21 (k=100, 200 candidatos, 6 899 películas): Life of Pi 31.º · 7.º · 5.º (antes 30 · 7 · 4); Titanic y WALL·E sin cambios. `VectorSearchIT`, `FullTextSearchIT` e `HybridSearchIT` en verde (14 de 14) |
| U-24 | terminada | `59d2662` | Sugerencias en español: Titanic 36/7/4, Inception 94/13/8, Life of Pi 31/7/5, Spirited Away 13/4/1 (E.T. y WALL·E sin cambios). Conteos léxicos de los dos idiomas: dark knight 57/5, love story 107/3, el secreto 35/2, le fabuleux 26/1, heaven 10, next 15; Kolejne en title 3 (OR) y 0 (AND); híbrida Titanic 1.º vectorial, 1.º léxico. Las de inglés coinciden con la fase 8. `./mvnw -pl elastiflix-web verify` en verde |
| U-07 | terminada | `f70be66` | Texto nuevo en `ErrorCode`, `messages_es` y `messages_en`, y en `docs/diseno/7.md`. `MovieDetailControllerTest` (7), `MessageCatalogParityTest` (3) y `ErrorPageTest` (4) en verde |
| U-16 | terminada | `3d1933f` | `EmptyState` enum, `ComponentScore(score, rank, k)` y `Pagination(first, last)`; la web arma los textos con `results.pager.span` y `results.card.score.outsideTop`. `./mvnw verify` en verde (386 unitarias, Checkstyle 0, PMD 0) |
| U-13, U-17 (carga) | terminada | se anota en el commit siguiente | `IngestPhrase` (57 frases) en ingest; `IngestTranslator` y `JobTexts` traducen por clave en los dos idiomas; informe CSV, rechazos del registro individual y 413 traducidos. `./mvnw verify` en verde: 513 unitarias (core 68, search 71, ingest 189, web 185), Checkstyle 0, PMD 0. `IngestTranslatorTest` pasa la salida real de `MappingValidator`, `CsvMovieReader` y `CsvColumnRows` en inglés sin una palabra en español |
| U-17 (cabecera y sugerencias) | pendiente | | |
| U-14, U-15 | pendiente | | |
| U-02 | pendiente | | |
| U-33 (cabeceras) | descartada | — | Adolfo decidió no agregar cabeceras de seguridad (2026-10-21) |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw verify`
- **Resultado:** en verde: 386 unitarias (core 68, search 71, ingest 123, web 124), Checkstyle 0, PMD 0
- **Fecha:** 2026-10-21 17:40 UTC

## Decisiones

- 2026-10-21: U-11 y U-03 se aplicaron directo en `main` a pedido de Adolfo («Sí, corrige U-11 … y U-03 …»
  y «Sí, haz commit y push en main …»).
- 2026-10-21: Adolfo respondió las decisiones de la lista:
  1. U-01: volver a medir la tabla vectorial de §6 («Si, hazlo»);
  2. U-07: el agente redacta el texto de `EFX-SRCH-012` según su objetivo («Genera uno acorde al objetivo»);
  3. U-13, U-16 y U-17: corrección de fondo («Corrígela de fondo»);
  4. U-02: instalar Playwright y Chromium y correr las pruebas de navegador («te doy vía libre»);
  5. U-24: volver a medir las sugerencias en español («Sí, vuelve a medirlas»);
  6. U-33: sin cabeceras de seguridad («No, no hace falta»).
- 2026-10-21: el resto de la revisión 4 va en la rama `revision-4`, con commit y push por tarea, y se integra
  con un PR. U-14 y U-15 entran con la decisión 3, porque son la misma traducción de «Cargar datos» y la
  prueba que la vigila. Los demás puntos esperan instrucciones de Adolfo.
- 2026-10-21 (U-01, U-24): la medición usó una IT temporal (`MeasureTmpIT`, no versionada) con las mismas
  estrategias de búsqueda que la aplicación. El código de búsqueda no cambió desde la revisión 3, así que la
  deriva de Life of Pi viene del servicio (inferencia o k-NN aproximado de Serverless). Los conteos léxicos de
  las sugerencias también eran del catálogo de 4 322 películas, en los dos idiomas: se corrigieron con la
  misma medición. PLAN §1 registra que las etiquetas ya no siguen los valores históricos de los mockups.
- 2026-10-21 (U-07): texto de `EFX-SRCH-012`, según el objetivo que fijó Adolfo el 2026-10-19 («un error
  controlado que le dice que su consulta falló»): «Tu búsqueda falló y la ficha de la película no se puede
  abrir · El enlace no trae una búsqueda válida: falta la consulta o algún ajuste no es válido · Vuelve a
  Inicio, busca la película y ábrela desde los resultados», y su versión en inglés. El estado de error **no**
  se agregó a los mockups `Detail*.dc.html`: la página usa la plantilla de error común (T-20), que no tiene
  mockup. Queda como pregunta para Adolfo si quiere ese estado en el canvas.
- 2026-10-21 (U-13, U-17): la corrección de fondo de la traducción de la carga no cambia `ValidationIssue`
  (sus textos siguen en español, para los logs, el informe en español y el JSON que lee `ingest.js`). En su
  lugar, cada frase fija vive en un vocabulario de ingest, `IngestPhrase`, que sabe escribirla y reconocerla, y
  la web traduce por su clave. Así no hay copias de las frases en la web, el contrato JSON no cambia y dos
  pruebas lo vigilan: `IngestPhraseTest` (cada frase se reconoce a sí misma, sin plantillas repetidas) e
  `IngestTranslatorTest` (cada frase tiene clave en los dos catálogos y la salida real sale en inglés). Se
  borraron `ValidationIssueTranslator`, `CsvColumnTranslator` y `EnglishJobTexts`. `IngestJobController`,
  `IngestJobErrors` y `UploadSizeErrors` ya no aceptan `UiMessages` opcional (parte de U-18).

## Resumen de diseño

Se completa al cerrar.

## Revisión de convenciones

Se completa al cerrar.

## Bloqueos y solicitudes

Vacío.

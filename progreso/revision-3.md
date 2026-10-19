# Estado de trabajo — Revisión 3 (tablero)

> Tablero de la revisión 3 de `main` en `27fcdef`. El trabajo va en paralelo por **carriles**: cada uno
> tiene su rama y su archivo de estado. La especificación y las reglas de los carriles están en
> [`docs/revisiones/revision-3.md`](../docs/revisiones/revision-3.md) › «Cómo trabajar en paralelo».
> **Este tablero solo lo escribe el carril Z (cierre)**: los carriles A–F escriben únicamente su archivo,
> para que nunca choquen en git.

## Identificación

- **Unidad:** Revisión 3 (tablero)
- **Nombre:** Correcciones de código de la tercera revisión exhaustiva, en carriles paralelos
- **Rama:** una por carril, `revision-3-<carril>`; el cierre usa `revision-3-cierre`
- **Carpeta:** un clon o worktree por carril
- **Herramienta de la última sesión:** Claude Code (Opus 5.5); los carriles A–F, seis agentes de Claude Code en paralelo

## Estado general

terminado (falta que Adolfo fusione el PR del cierre)

## Punto de reanudación

- **Tarea actual:** ninguna; T-01 a T-27 terminadas
- **Subpaso en curso:** ninguno
- **Próximo paso:** Adolfo revisa y fusiona el PR de `revision-3-cierre`.
- **Última actualización:** 2026-10-19 03:19 UTC

## Carriles

> Z actualiza esta tabla al cerrar. Mientras tanto, el estado vivo está en la rama remota y en el
> archivo de cada carril.

| Carril | Rama | Archivo de estado | Estado | PR |
|---|---|---|---|---|
| A · Núcleo | `revision-3-core` | [`progreso/revision-3-core.md`](revision-3-core.md) | fusionado | #18 |
| B · Búsqueda | `revision-3-busqueda` | [`progreso/revision-3-busqueda.md`](revision-3-busqueda.md) | fusionado | #16 |
| C · Carga: trabajos | `revision-3-trabajos` | [`progreso/revision-3-trabajos.md`](revision-3-trabajos.md) | fusionado | #19 |
| D · Carga: lectura | `revision-3-lectura` | [`progreso/revision-3-lectura.md`](revision-3-lectura.md) | fusionado | #15 |
| E · Web: páginas | `revision-3-paginas` | [`progreso/revision-3-paginas.md`](revision-3-paginas.md) | fusionado | #20 |
| F · Web: Cargar datos | `revision-3-cargar` | [`progreso/revision-3-cargar.md`](revision-3-cargar.md) | fusionado | #17 |
| Z · Cierre | `revision-3-cierre` | este tablero | terminado | PR del cierre |

## Tareas

> Una `T-NN` está terminada cuando todas sus partes (columna «Carriles») lo están. La evidencia de cada
> parte (commit, prueba que falla si se rompe el código y conteos) está en el archivo de su carril. Al cerrar,
> los seis archivos dicen `terminado` y todas sus filas `terminada`; ninguna quedó `bloqueada`.

| Id | Carriles | Estado | Corrección |
|---|---|---|---|
| T-01 | A | terminada | La copia del índice no se cancela ante una respuesta ilegible de `_tasks` |
| T-02 | A, B | terminada | Búsqueda e inferencia no traducen una respuesta ilegible a un código EFX |
| T-03 | B | terminada | Validación k-NN: un vector de consulta sin campo da NPE |
| T-04 | F | terminada | «Cargar datos»: el aviso de un 409 o 404 al importar o cancelar se borra al instante |
| T-05 | F | terminada | «Cargar datos»: no se puede volver a validar el mismo archivo con otro separador |
| T-06 | E | terminada | Resultados: el filtro `adult=true` se pierde sin aviso |
| T-07 | E | terminada | Cada página pide `/favicon.ico`, y ese 404 consulta Elasticsearch |
| T-08 | C | terminada | «Archivo válido» aparece antes de terminar la validación y en trabajos fallidos |
| T-09 | C | terminada | Un rechazo de archivo detiene el paso equivocado |
| T-10 | A, B, C | terminada | Faltan pruebas unitarias de ramas importantes |
| T-11 | F | terminada | El manejo de estados de `ingest.js` no tiene prueba automática |
| T-12 | A, B, C, D, E | terminada | Código muerto y campos que nadie lee |
| T-13 | E, F | terminada | Límites de carga con dos orígenes y valores escritos a mano |
| T-14 | E | terminada | Una página de error EFX lee el catálogo dos veces |
| T-15 | F | terminada | La API de carga: un error sin código se muestra como «sin conexión», y la subida deja un temporal |
| T-16 | C | terminada | Hilos de la carga: estado final, excepciones y nombres |
| T-17 | C, D | terminada | Detalles de la validación de archivos |
| T-18 | B | terminada | Códigos de error equivocados en búsqueda y mapping |
| T-19 | A, C, E | terminada | Textos de error y Javadoc que no corresponden al comportamiento |
| T-20 | E, F | terminada | Textos visibles sin punto final o sin fila en PLAN §1 |
| T-21 | E | terminada | «Ver todos» aparece aunque no oculte nada |
| T-22 | E | terminada | CSS duplicado y muerto |
| T-23 | E | terminada | Tres maneras de agrupar miles, y cifras sin agrupar |
| T-24 | C, D | terminada | Duplicados y costo por sondeo en la carga |
| T-25 | A, E, F | terminada | Configuración y beans armados para las pruebas |
| T-26 | E, F | terminada | Accesibilidad |
| T-27 | A, B, C, D, E, F | terminada | Pruebas que no pueden fallar, débiles o con higiene pendiente |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw verify -Pit` y `BASE_URL=http://localhost:8089 npm test` (pruebas de navegador, con el
  jar del cierre y sin Elasticsearch) sobre `be485a3`
- **Resultado:** BUILD SUCCESS, 326 unitarias (core 63, search 69, ingest 123, web 71) y 29 de integración
  (core 9, search 14, ingest 2, web 4), 0 fallos y 0 omitidas; navegador 11 de 11
- **Fecha:** 2026-10-19 03:13 UTC (después solo cambiaron documentación y comentarios de pruebas)

## Decisiones

- 2026-10-19: Adolfo pidió una revisión exhaustiva del código y de la documentación tras la revisión 2.
  Decidió que los problemas de código **solo se documentan** (esta lista) y que la documentación se alinea
  con el código actual en un commit directo a `main`. La revisión no pudo compilar ni correr pruebas: el
  entorno no tenía Maven Central ni JDK 25. Checkstyle y PMD, corridos con sus jars, siguen en 0.
- 2026-10-19: Adolfo pidió que el trabajo de la revisión 3 se planifique para varios agentes en paralelo,
  sin dependencias que obliguen a detenerse. Se repartió en seis carriles con archivos disjuntos (A–F) y un
  cierre (Z). Las partes que cruzaban módulos se resolvieron así: firmas públicas congeladas entre módulos,
  `ErrorContextTest` y los textos de `ErrorCode` en A, todo el CSS en E, y la documentación compartida en Z.
- 2026-10-19: las decisiones abiertas de T-07, T-15, T-19 y T-20 las toma el carril que hace cada parte y
  las registra en su archivo.

- 2026-10-19, Z: los seis carriles los hicieron seis agentes de Claude Code en paralelo, cada uno en su worktree
  (`/workspaces/elastiflix-<carril>`), sin `./mvnw install` para no compartir `~/.m2`. Antes de fusionar, Z
  fusionó las seis ramas en un worktree temporal: sin conflictos y `./mvnw verify -Pit` en verde. Adolfo fusionó
  los PR #15 a #20 («Fusiona los PR del 15 al 20»); `./mvnw verify` sobre `main` en `7e360b4`: 323 unitarias.
- 2026-10-19, Z (pedido de Adolfo, «Inclúyelas en el cierre»): dos puntos que los carriles dejaron fuera de la
  lista entran en el cierre:
  - el rango de la paginación agrupa los miles («976–1 000 de 1 234»). `Thousands` pasa de
    `ingest.mapping` a `core.text` para que lo use también la búsqueda (un solo helper en los cuatro módulos,
    como pedía T-23). Prueba: `PaginatorTest.shouldGroupThousandsWhenTheTotalIsLarge`; falla si el total se
    escribe sin agrupar. `ThousandsTest` nueva en core;
  - `HybridRequestBuilder.build` ya no recibe `preference` (siempre llegaba `null`): genera el valor compartido.
    `HybridRequestBuilderTest` afirma que las tres cabeceras del `_msearch` llevan el mismo `elastiflix-<uuid>`;
    falla si una cabecera usa otro valor.
- 2026-10-19, Z (regla 1): se quitó `RankedHit.rrf()`. `SearchService` pone la puntuación RRF solo en la híbrida
  (`SearchServiceTest`: `rrf` es la puntuación en híbrida y `null` en vectorial; falla si se copia siempre).
  Se conservan, por decisión de Z:
  - el relleno de `null` de `ElastiflixProperties` y los `fallback()` de cada grupo (incluido
    `Poster.fallback()`): los usa el propio relleno, el contexto de prueba de core y 17 construcciones en pruebas
    de los cuatro módulos. Ya no lo usa ningún bean de la web;
  - `CatalogIndex.readWithVectors` y `readAllWithVectors`: los usan las IT de core e ingest.

- **Evidencia de T-22 (copiada del carril E, como pide su criterio):** `elastiflix.css` pasó de 645 a 625 líneas
  (`f2b7b4a`): las parejas repetidas de la lista quedaron en una regla y se quitaron las muertas
  (`.score-panel .eyebrow`, `.sizes`, `.page-size select`, `#job-ok[hidden]`, `.file-chip[hidden]`). Cómo se
  comparó: un script de Playwright fuera del repositorio sirvió el CSS viejo o el nuevo con `page.route` contra
  la aplicación del carril, bloqueó los pósters de TMDB, esperó las fuentes y guardó, por pantalla y ancho, el
  `getComputedStyle` de cada elemento (con `::before` y `::after`) y una captura PNG de página completa. 15
  pantallas (Inicio, Resultados en varios estados, Cargar datos, Acerca de y 404) a 1440 y 390 px. Dos corridas
  con el mismo CSS salieron idénticas; el CSS nuevo contra el viejo dio 60 de 60 archivos idénticos byte a byte.
- **Conteo de web en la revisión 2:** `progreso/revision-2.md` registró 55 pruebas unitarias de web; Maven
  contó 54. El 55 salió de sumar a mano los informes de Surefire. No faltaba ninguna prueba.
- **Supresiones nuevas:** cuatro `@SuppressWarnings("unchecked")` en pruebas de core (T-01 y T-10), por la
  función genérica que entrega Mockito. Z agregó el comentario que faltaba a tres y las registró en
  «Supresiones» de `docs/diseno/1.md`.
- **Limpieza local:** se quitaron los seis worktrees de los carriles (sin cambios ni commits pendientes; con
  ellos sus copias de `.env`) y los temporales de prueba en `/tmp` (30 `elastiflix-upload-*.bin` y 48
  `elastiflix-job-*`). Las ramas remotas `revision-3-*` siguen en GitHub.

## Resumen de diseño

La revisión no abre un diseño nuevo: cada `T-NN` lleva el código a lo que ya decían `PLAN.md`, los mockups y
los diseños, y Z llevó a `docs/diseno/1.md` a `4.md` y a PLAN §1 y §7 lo que cambió (entradas «Revisión 3» en
cada «Cambios respecto al diseño aprobado»). Lo más relevante:

- **Copia con `_reindex` (T-01):** también se cancela ante una respuesta ilegible de `_tasks`; la cancelación
  no espera en la llamada y se confirma con hasta 3 lecturas de 20 s.
- **Errores (T-02, T-15, T-18, T-19):** respuestas ilegibles de búsqueda e inferencia con código EFX; un
  `_msearch` con partes de más o de menos da `EFX-ES-007`; la API de carga responde `EFX-IMP-007` a una falla
  sin código; las acciones sugeridas ya no prometen un borrado que puede fallar.
- **Carga (T-08, T-09, T-16, T-17, T-24):** «Archivo válido» y el paso detenido corresponden al estado real;
  hilos con nombre y estado final correcto; ids existentes solo con el archivo válido; cifras del informe
  calculadas una vez.
- **Topes de subida (T-13):** un solo origen, `elastiflix.ingest.max-file-size`; `UploadMultipartConfig` deriva
  el tope de la petición.
- **Web (T-04 a T-07, T-11, T-14, T-20 a T-23, T-26):** avisos que no se borran solos, revalidación del mismo
  archivo, «Solo adultos», ícono, error que lee el catálogo una vez, textos con punto, CSS sin duplicados, miles
  agrupados con un solo helper y accesibilidad.
- **Z:** `Thousands` pasa a `core.text` y lo usan los cuatro módulos (también el rango de paginación);
  `HybridRequestBuilder` genera su `preference`; se quitó `RankedHit.rrf()`.

## Revisión de convenciones

- [x] Diseño previo: no hay fase nueva; la especificación es `docs/revisiones/revision-3.md`, aprobada por
  Adolfo con su reparto en carriles.
- [x] Código y diseño coinciden: Z aplicó las «Notas para el cierre» de los seis carriles a `PLAN.md` y
  `docs/diseno/1.md` a `4.md`, comprobando cada afirmación contra el código; los diagramas Mermaid se generan
  sin error con `mmdc` 11.4.2 (12 diagramas).
- [x] `./mvnw verify` y `./mvnw verify -Pit` en verde: 326 unitarias (core 63, search 69, ingest 123, web 71),
  29 de integración, Checkstyle y PMD en 0, ArchUnit en verde. Pruebas de navegador: 11 de 11.
- [x] Una responsabilidad por clase: las clases nuevas (`JobThreads`, `JobSteps`, `CauseType`, `ReportFigures`,
  `ReportOrder`, `UploadMultipartConfig`, `core.text.Thousands`…) se describen en una frase en su Javadoc.
- [x] Patrones: solo los de la lista cerrada; nada nuevo.
- [x] Errores: sin códigos nuevos; se corrigieron acciones sugeridas y el código de casos mal clasificados.
- [x] Logs: sin API key, vectores ni documentos; el log de un hilo que falla solo lleva su nombre y el tipo de
  la excepción.
- [x] Supresiones: cuatro `@SuppressWarnings("unchecked")` nuevas, solo en pruebas, justificadas en el código y
  registradas en `docs/diseno/1.md`. Ninguna de Checkstyle ni de PMD.
- [x] Pruebas de escritura: solo alias propios `elastiflix-it-*`, que se borran; el alias real siguió con 6 899.
- [x] Junior: clases cortas, nombres en inglés y Javadoc en español. `AGENTS.md` en 11 647 caracteres.

## Bloqueos y solicitudes

Vacío.

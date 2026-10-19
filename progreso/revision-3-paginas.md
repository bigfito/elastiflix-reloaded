# Estado de trabajo — Revisión 3 · Carril E (Web: páginas)

> Carril E de la revisión 3. La especificación de cada corrección está en
> [`docs/revisiones/revision-3.md`](../docs/revisiones/revision-3.md); las reglas de los carriles, en su
> sección «Cómo trabajar en paralelo». Este archivo solo lo escribe el agente de este carril.

## Identificación

- **Unidad:** Revisión 3 · carril E
- **Nombre:** Web: páginas
- **Rama:** `revision-3-paginas` (creada desde `origin/main` en `6c403f4`)
- **Carpeta:** worktree `/workspaces/elastiflix-paginas`
- **Archivos del carril:** web salvo el paquete `ingest/`, `MultipartLimitsTest.java` y las aserciones de «Cargar datos» de `SmokeIT.java` (regla 8); plantillas salvo `ingest.html`; `static/css/**`, `static/img/**`, `home.js` y `results.js`
- **Herramienta de la última sesión:** Claude Code (Opus 5.5)

## Estado general

en curso

## Punto de reanudación

- **Tarea actual:** T-20 (página de error)
- **Subpaso en curso:** por empezar
- **Próximo paso:** agrega el punto final al «qué pasó» en `ErrorPages.filled` y afírmalo en `ErrorPageTest`; anota la fila de PLAN §1 en «Notas para el cierre».
- **Última actualización:** 2026-10-19 02:33 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-06 | terminada | `4a21bfd` | Con `adult=true`, `ResultsPage.adultChoice` agrega la opción «Solo adultos» marcada (y «Todos» deja de estarlo), como `mk()` de S-11. Pruebas nuevas `ResultsPageTest.shouldKeepTheAdultOnlyFilterChecked` y `shouldNotOfferAdultOnlyByDefault`. `./mvnw -q -pl elastiflix-web -am verify -Dtest=ResultsPageTest -Dsurefire.failIfNoSpecifiedTests=false`: 13 pruebas, 0 fallos, Checkstyle y PMD en 0. Rotura a mano (`adultOnly` siempre `false`): falla `shouldKeepTheAdultOnlyFilterChecked`; revertido. |
| T-07 | terminada | `6db5cfd` | Ícono `static/img/favicon.svg` con `<link rel="icon">` en `fragments/head.html` (ver «Decisiones»). `LayoutPageTest` (ahora importa `NotFoundAdvice`): `shouldShowConnectionIndexAndTmdbOnTheHomePage` afirma el `<link>`; `shouldServeTheIconWithoutReadingTheCatalog` pide el ícono, espera 200 `image/svg+xml` y `verify(catalog, never()).status()`. `./mvnw -q -pl elastiflix-web -am verify -Dtest=LayoutPageTest -Dsurefire.failIfNoSpecifiedTests=false`: 3 pruebas, 0 fallos, Checkstyle y PMD en 0. Rotura a mano (quitar el ícono): falla con 404, y sin la aserción de estado falla `never()` (`NeverWantedButInvoked`, el 404 lee el catálogo); revertido. |
| T-12 (`changeHref`) | terminada | `e160ec0` | Se quitó `model.addAttribute("changeHref", "/")` de `ResultsPage.heading`; «Cambiar ajustes» sigue con `@{/}`. `git grep -n changeHref -- '*/src/main/*'` no devuelve nada (código 1). `./mvnw -q -pl elastiflix-web -am verify -Dtest=ResultsPageTest,ServedPagesTest -Dsurefire.failIfNoSpecifiedTests=false`: 20 pruebas, 0 fallos, Checkstyle y PMD en 0. Es una eliminación de código muerto: no hay comportamiento que romper a mano. El `./mvnw verify` completo va al cierre del carril. |
| T-13 («Acerca de») | terminada | `5e30db1` | Parte de E: `AboutController` recibe `IngestLimits` y pone `maxFileLabel` y `batchSize` en el modelo; `about.html` los usa en «Antes de subir» y «Índice temporal». Prueba nueva `AboutLimitsTest` (`ELASTIFLIX_INGEST_MAX_FILE_SIZE=200MB`, `ELASTIFLIX_INGEST_BATCH_SIZE=50`): muestra «de máximo 200 MB» y «lotes de 50» y no «150 MB» ni «lotes de 100»; `AboutPageTest` afirma los valores por defecto. Las dos declaran `@EnableConfigurationProperties(ElastiflixProperties.class)` e importan `IngestLimits` (regla 7). `./mvnw -q -pl elastiflix-web -am verify -Dtest='About*Test' -Dsurefire.failIfNoSpecifiedTests=false`: 2 pruebas, 0 fallos, Checkstyle y PMD en 0. Rotura a mano (cifras otra vez fijas en la plantilla): falla `AboutLimitsTest`; revertido. La parte de «Cargar datos» es del carril F. |
| T-14 | terminada | `36f85bc` | `LayoutAdvice.shell(HttpServletRequest)` guarda el cromo en `PageShells.REQUEST_ATTRIBUTE`; `WebExceptionHandler.elastiflix` lo reutiliza y solo llama a `catalog.status()` si falta. Prueba nueva `ErrorPageTest.shouldReadTheCatalogOnceForAnElastiflixError` (`/probe/elastiflix` → 500 con el pie y `verify(catalog, times(1)).status()`). `./mvnw -q -pl elastiflix-web -am verify -Dtest=ErrorPageTest,LayoutPageTest,IngestPageTest,ServedPagesTest -Dsurefire.failIfNoSpecifiedTests=false`: 23 pruebas, 0 fallos, Checkstyle y PMD en 0. Rotura a mano (ignorar el atributo y volver a leer): falla con `TooManyActualInvocations` (2 lecturas); revertido. |
| T-19 (`SearchController`) | terminada | (hash en el commit siguiente) | Parte de E: Javadoc de clase y de `search` en `SearchController` fiel al comportamiento (solo `summary()` con punto junto al campo; el código al log en `WARN`). `git grep -n "con su código" elastiflix-web/src/main/java/com/bigfito/elastiflix/web/search` no devuelve nada. `./mvnw -q -pl elastiflix-web -am verify -Dtest=ResultsPageTest -Dsurefire.failIfNoSpecifiedTests=false`: 13 pruebas, 0 fallos, Checkstyle y PMD en 0. Solo cambia Javadoc: no hay comportamiento que romper. Las partes de A y C son de sus carriles. |
| T-20 (página de error) | pendiente | — | Textos visibles sin punto final o sin fila en PLAN §1 |
| T-21 | pendiente | — | «Ver todos» aparece aunque no oculte nada |
| T-22 | pendiente | — | CSS duplicado y muerto |
| T-23 | pendiente | — | Tres maneras de agrupar miles, y cifras sin agrupar |
| T-25 (`PosterUrls`) | pendiente | — | Configuración y beans armados para las pruebas |
| T-26 (Resultados y CSS) | pendiente | — | Accesibilidad |
| T-27 (`ErrorPageTest`) | pendiente | — | Pruebas que no pueden fallar, débiles o con higiene pendiente |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw -pl elastiflix-web -am verify` (y al final `./mvnw verify` completo)
- **Resultado:** —
- **Fecha:** —

## Decisiones

- 2026-10-19 02:10 UTC: Claude Code toma el carril E en el worktree `/workspaces/elastiflix-paginas`. El
  primer `./mvnw -q -pl elastiflix-web -am verify` sobre `6c403f4` terminó en verde (3 min 48 s, CPU compartida).
- **T-07 (decisión abierta):** se elige el ícono, no excluir recursos del advice. `static/img/favicon.svg` (el
  logo del header: cuadro durazno `#FFCDB2` de radio 10 con el triángulo `#2E2A3B`) y
  `<link rel="icon" type="image/svg+xml">` en `fragments/head.html`. Con el ícono declarado el navegador ya
  no pide `/favicon.ico`, y el ícono lo sirve el manejador de recursos estáticos, que no pasa por
  `NotFoundAdvice`. Va en `static/img/` y no en la raíz de `static/` porque esa carpeta es la que la tabla
  de carriles asigna a E.
- **T-19 (decisión del texto, parte de E):** el Javadoc de `SearchController` dice ahora que el error de
  validación se muestra junto al campo solo con su «qué pasó» (`summary()`) y punto final, y que el código
  `EFX-*` va al log en `WARN`; el de `search` dice «muestra qué pasó junto al campo y registra el código».

## Notas para el cierre

- **T-06 · PLAN §1, fila nueva:** `Results.dc.html` › filtro «Contenido para adultos» | El mockup solo
  ofrece «Todos» y «Solo no adultos» | Si la URL trae `adult=true`, se agrega la opción «Solo adultos»,
  marcada, con su conteo (como `mk()` de las facetas, S-11), para que APLICAR no quite el filtro sin avisar.
- **T-13 (parte de E):** «Acerca de» ya no escribe 150 MB ni 100 a mano: `AboutController` pasa
  `maxFileLabel` y `batchSize` de `IngestLimits`. Si Z actualiza `docs/diseno/4.md` (modelo de cada vista),
  `about` suma esos dos atributos. La parte de F (`@Value`, `max-request-size`) y su nota de PLAN §7 son de F.
- **T-14:** `docs/diseno/4.md` (cromo y errores): `LayoutAdvice.shell` deja el `PageShell` en el atributo de
  petición `PageShells.REQUEST_ATTRIBUTE`, y `WebExceptionHandler` lo reutiliza; solo lee el catálogo si la
  petición no lo trae.

## Bloqueos y solicitudes

Vacío.

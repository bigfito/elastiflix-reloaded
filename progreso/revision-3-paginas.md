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

- **Tarea actual:** T-07
- **Subpaso en curso:** por empezar
- **Próximo paso:** agrega `static/img/favicon.svg` (logo del mockup) y su `<link rel="icon">` en `fragments/head.html`; prueba MockMvc de que pedir el ícono no llama a `catalog.status()` y aserción del `<link>` en `LayoutPageTest`.
- **Última actualización:** 2026-10-19 02:18 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-06 | terminada | (hash en el commit siguiente) | Con `adult=true`, `ResultsPage.adultChoice` agrega la opción «Solo adultos» marcada (y «Todos» deja de estarlo), como `mk()` de S-11. Pruebas nuevas `ResultsPageTest.shouldKeepTheAdultOnlyFilterChecked` y `shouldNotOfferAdultOnlyByDefault`. `./mvnw -q -pl elastiflix-web -am verify -Dtest=ResultsPageTest -Dsurefire.failIfNoSpecifiedTests=false`: 13 pruebas, 0 fallos, Checkstyle y PMD en 0. Rotura a mano (`adultOnly` siempre `false`): falla `shouldKeepTheAdultOnlyFilterChecked`; revertido. |
| T-07 | pendiente | — | Cada página pide `/favicon.ico`, y ese 404 consulta Elasticsearch |
| T-12 (`changeHref`) | pendiente | — | Código muerto y campos que nadie lee |
| T-13 («Acerca de») | pendiente | — | Límites de carga con dos orígenes y valores escritos a mano |
| T-14 | pendiente | — | Una página de error EFX lee el catálogo dos veces |
| T-19 (`SearchController`) | pendiente | — | Textos de error y Javadoc que no corresponden al comportamiento |
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

- 2026-10-19 02:10 UTC: Claude Code toma el carril E en el worktree `/workspaces/elastiflix-paginas`.

## Notas para el cierre

- **T-06 · PLAN §1, fila nueva:** `Results.dc.html` › filtro «Contenido para adultos» | El mockup solo
  ofrece «Todos» y «Solo no adultos» | Si la URL trae `adult=true`, se agrega la opción «Solo adultos»,
  marcada, con su conteo (como `mk()` de las facetas, S-11), para que APLICAR no quite el filtro sin avisar.

## Bloqueos y solicitudes

Vacío.

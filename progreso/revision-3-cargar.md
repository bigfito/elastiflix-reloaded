# Estado de trabajo — Revisión 3 · Carril F (Web: Cargar datos)

> Carril F de la revisión 3. La especificación de cada corrección está en
> [`docs/revisiones/revision-3.md`](../docs/revisiones/revision-3.md); las reglas de los carriles, en su
> sección «Cómo trabajar en paralelo». Este archivo solo lo escribe el agente de este carril.

## Identificación

- **Unidad:** Revisión 3 · carril F
- **Nombre:** Web: Cargar datos
- **Rama:** `revision-3-cargar` (creada desde `origin/main` en `6c403f4`)
- **Carpeta:** worktree `/workspaces/elastiflix-cargar`
- **Archivos del carril:** paquete `ingest/` de web y sus pruebas, `MultipartLimitsTest.java`, las aserciones de «Cargar datos» de `SmokeIT.java` (regla 8), `ingest.html`, `ingest.js`, `application.yml` de web y `src/test/browser/**`
- **Herramienta de la última sesión:** Claude Code (Opus 5.5)

## Estado general

en curso

## Punto de reanudación

- **Tarea actual:** T-05
- **Subpaso en curso:** T-04 terminada
- **Próximo paso:** T-05 en `ingest.js`: `input.value = ""` en `choose()` y rehabilitar «Validar archivo» al cambiar separador o encabezado.
- **Última actualización:** 2026-10-19 02:16 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-04 | terminada | se anota en el commit siguiente | `ingest.js`: bandera `actionError` que pone `act()` ante un 4xx o una caída de red; `paint()` ya no oculta `#server-error` mientras esté puesta y el siguiente clic la limpia. Prueba: `BASE_URL=http://localhost:8096 node --test ingest.test.js` → «T-04: un 409 al importar deja el aviso visible después del siguiente sondeo» ✔. Con `ingest.js` de `main` falla (`actual: false, expected: true`); se restauró la corrección. |
| T-05 | pendiente | — | «Cargar datos»: no se puede volver a validar el mismo archivo con otro separador |
| T-11 | pendiente | — | El manejo de estados de `ingest.js` no tiene prueba automática |
| T-13 («Cargar datos» y `application.yml`) | pendiente | — | Límites de carga con dos orígenes y valores escritos a mano |
| T-15 | pendiente | — | La API de carga: un error sin código se muestra como «sin conexión», y la subida deja un temporal |
| T-20 («Cargar datos» y `JobTexts`) | pendiente | — | Textos visibles sin punto final o sin fila en PLAN §1 |
| T-25 (`IngestLimits`) | pendiente | — | Configuración y beans armados para las pruebas |
| T-26 (teclado de las pestañas) | pendiente | — | Accesibilidad |
| T-27 (`IngestPageTest`) | pendiente | — | Pruebas que no pueden fallar, débiles o con higiene pendiente |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw -pl elastiflix-web -am verify` (y al final `./mvnw verify` completo)
- **Resultado:** `main` de partida (`6c403f4`) en verde (EXIT 0)
- **Fecha:** 2026-10-19 02:13 UTC

## Decisiones

- 2026-10-19 02:10 UTC: carril tomado con Claude Code (Opus 5.5), worktree `/workspaces/elastiflix-cargar`.

## Notas para el cierre

<Cambios de documentación que aplicará el carril Z: filas de PLAN §1 (texto exacto), propiedades de §7,
«Cambios respecto al diseño aprobado» de `docs/diseno/<fase>.md`, tablas de errores y README. También las
firmas viejas que la regla 1 obligó a conservar y que Z puede quitar.>

## Bloqueos y solicitudes

Vacío.

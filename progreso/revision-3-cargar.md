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

- **Tarea actual:** T-04
- **Subpaso en curso:** carril reservado
- **Próximo paso:** corre `./mvnw -q -pl elastiflix-web -am verify` sobre `main` y corrige T-04 en `ingest.js`
  (bandera `actionError` que `paint()` respeta).
- **Última actualización:** 2026-10-19 02:10 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-04 | pendiente | — | «Cargar datos»: el aviso de un 409 o 404 al importar o cancelar se borra al instante |
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
- **Resultado:** —
- **Fecha:** —

## Decisiones

- 2026-10-19 02:10 UTC: carril tomado con Claude Code (Opus 5.5), worktree `/workspaces/elastiflix-cargar`.

## Notas para el cierre

<Cambios de documentación que aplicará el carril Z: filas de PLAN §1 (texto exacto), propiedades de §7,
«Cambios respecto al diseño aprobado» de `docs/diseno/<fase>.md`, tablas de errores y README. También las
firmas viejas que la regla 1 obligó a conservar y que Z puede quitar.>

## Bloqueos y solicitudes

Vacío.

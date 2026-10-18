# Estado de trabajo — Revisión 2

> Correcciones de la segunda revisión de `main` en `41c1188`. Una tarea por cada `S-NN`. La especificación
> de cada una (dónde, problema, corrección y criterio) está en
> [`docs/revisiones/revision-2.md`](../docs/revisiones/revision-2.md); léela antes de empezar cada tarea.

## Identificación

- **Unidad:** Revisión 2
- **Nombre:** Correcciones de la segunda revisión exhaustiva
- **Rama:** `revision-2` (por crear desde `main`)
- **Carpeta:** raíz del repositorio
- **Herramienta de la última sesión:** ninguna todavía (puede ser Claude Code, Google Antigravity, JetBrains Junie o Cursor)

## Estado general

no iniciado

## Punto de reanudación

- **Tarea actual:** ninguna
- **Subpaso en curso:** ninguno
- **Próximo paso:** aplica el protocolo de reanudación de `AGENTS.md`, crea la rama `revision-2` desde
  `main`, marca esta unidad `en curso` y empieza S-01 según `docs/revisiones/revision-2.md`.
- **Última actualización:** 2026-10-18 19:42 UTC

## Tareas

> El hash de una tarea se completa en el commit de la tarea siguiente. La fila que se cierra puede decir «se anota en …» hasta ese commit.
> Al cerrar una tarea, sustituye su título por la evidencia: comando, resultado y la prueba que falla si se rompe el comportamiento.

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| S-01 | pendiente | — | La copia del índice se corta a los 30 s, y el reindex sigue escribiendo después de borrar el temporal |
| S-02 | pendiente | — | «Cargar datos» sigue sin reproducir `Ingest.dc.html`: contenedor, botones y estilos |
| S-03 | pendiente | — | «Cargar datos»: faltan datos y textos del trabajo de carga |
| S-04 | pendiente | — | Publicación: proteger el temporal ya publicado y no borrar ante un fallo que no es 4xx |
| S-05 | pendiente | — | El `input_type` y el prefijo de documento siguen con dos fuentes |
| S-06 | pendiente | — | Memoria de la ingesta: los vectores se quitan tarde y los trabajos nunca se expulsan |
| S-07 | pendiente | — | Números fuera de rango y conversiones que tumban el trabajo o la página |
| S-08 | pendiente | — | CSV: el número de fila de los errores no corresponde a la fila del archivo |
| S-09 | pendiente | — | La comprobación UTF-8 del navegador rechaza archivos válidos |
| S-10 | pendiente | — | Las sugerencias de Inicio siguen perdiendo los ajustes |
| S-11 | pendiente | — | Resultados: un filtro marcado desaparece si no está en las facetas |
| S-12 | pendiente | — | «Acerca de»: las tarjetas de modo salen apiladas |
| S-13 | pendiente | — | Resultados: estilos del mockup pendientes |
| S-14 | pendiente | — | Las pruebas que pedía R-18 no se hicieron |
| S-15 | pendiente | — | El registro de entrega no refleja las decisiones de la revisión 1 |
| S-16 | pendiente | — | Tabla de ajustes autorizados de PLAN §1: faltan R-10, R-30 y el cambio del mockup de B-05 |
| S-17 | pendiente | — | Seis documentos dicen que las pruebas solo exigen «conteo mayor que cero» |
| S-18 | pendiente | — | Las capturas y la comparación de R-21 no reflejan la interfaz real |
| S-19 | pendiente | — | Con solo el README todavía no se puede preparar el catálogo |
| S-20 | pendiente | — | Una API key inválida aparece como «corrige la petición» |
| S-21 | pendiente | — | Códigos y textos de error que no corresponden al contexto |
| S-22 | pendiente | — | Mensajes y página de error genérica |
| S-23 | pendiente | — | Errores JSON de la carga con alcance incompleto |
| S-24 | pendiente | — | Detalles de la ingesta |
| S-25 | pendiente | — | Código muerto |
| S-26 | pendiente | — | Documentación técnica desactualizada |
| S-27 | pendiente | — | Evidencia histórica que quedó sin nota |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** ninguno en esta unidad todavía. La última corrida registrada es la de `progreso/INDICE.md`.
- **Resultado:** —
- **Fecha:** —

## Decisiones

- 2026-10-18: Adolfo pidió que esta lista la tome un agente de Claude Code, Google Antigravity o
  JetBrains Junie. Ya no se le encarga al agente de grokbot (Cursor) que hizo las fases, la revisión 1 y
  los bugs.
- 2026-10-18: las decisiones abiertas de S-03 (textos fuera del mockup), S-05 (consulta configurable o
  fija) y S-06 (cuántos trabajos conservar) las toma el agente. Elige la opción más simple que cumpla
  el criterio y la registra aquí.

## Resumen de diseño

<Al cerrar la unidad: estructura final, decisiones relevantes y diferencias con los diseños `docs/diseno/<fase>.md` que se tocaron.>

## Revisión de convenciones

<Al cerrar la unidad, responde cada punto de la lista de revisión de `docs/convenciones-java.md`.>

## Bloqueos y solicitudes

Vacío.

# Estado de trabajo — Revisión 3 · Carril B (Búsqueda)

> Carril B de la revisión 3. La especificación de cada corrección está en
> [`docs/revisiones/revision-3.md`](../docs/revisiones/revision-3.md); las reglas de los carriles, en su
> sección «Cómo trabajar en paralelo». Este archivo solo lo escribe el agente de este carril.

## Identificación

- **Unidad:** Revisión 3 · carril B
- **Nombre:** Búsqueda
- **Rama:** `revision-3-busqueda` (creada desde `origin/main` en `6c403f4`)
- **Carpeta:** worktree `/workspaces/elastiflix-busqueda`
- **Archivos del carril:** `elastiflix-search/**`
- **Herramienta de la última sesión:** Claude Code (Opus 5.5)

## Estado general

en curso

## Punto de reanudación

- **Tarea actual:** T-02 (transporte)
- **Subpaso en curso:** carril reservado; falta el primer `./mvnw -pl elastiflix-search -am verify`
- **Próximo paso:** correr `./mvnw -q -pl elastiflix-search -am verify` en el worktree y empezar T-02:
  agregar `JsonpMappingException` al multi-catch de `ElasticsearchSearchTransport` con su prueba.
- **Última actualización:** 2026-10-19 02:10 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-02 (transporte) | pendiente | — | Búsqueda e inferencia no traducen una respuesta ilegible a un código EFX |
| T-03 | pendiente | — | Validación k-NN: un vector de consulta sin campo da NPE |
| T-10 (`KnnStrategiesTest`) | pendiente | — | Faltan pruebas unitarias de ramas importantes |
| T-12 (search) | pendiente | — | Código muerto y campos que nadie lee |
| T-18 | pendiente | — | Códigos de error equivocados en búsqueda y mapping |
| T-27 (search) | pendiente | — | Pruebas que no pueden fallar, débiles o con higiene pendiente |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw -pl elastiflix-search -am verify` (y al final `./mvnw verify` completo)
- **Resultado:** —
- **Fecha:** —

## Decisiones

- Sesión iniciada 2026-10-19 02:10 UTC con Claude Code (Opus 5.5): carril libre (`git ls-remote` sin ramas
  `revision-3-*`); rama creada desde `origin/main` (`6c403f4`) en el worktree `/workspaces/elastiflix-busqueda`.

## Notas para el cierre

<Cambios de documentación que aplicará el carril Z: filas de PLAN §1 (texto exacto), propiedades de §7,
«Cambios respecto al diseño aprobado» de `docs/diseno/<fase>.md`, tablas de errores y README. También las
firmas viejas que la regla 1 obligó a conservar y que Z puede quitar.>

## Bloqueos y solicitudes

Vacío.

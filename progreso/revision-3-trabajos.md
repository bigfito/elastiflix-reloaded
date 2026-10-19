# Estado de trabajo — Revisión 3 · Carril C (Carga: trabajos)

> Carril C de la revisión 3. La especificación de cada corrección está en
> [`docs/revisiones/revision-3.md`](../docs/revisiones/revision-3.md); las reglas de los carriles, en su
> sección «Cómo trabajar en paralelo». Este archivo solo lo escribe el agente de este carril.

## Identificación

- **Unidad:** Revisión 3 · carril C
- **Nombre:** Carga: trabajos
- **Rama:** `revision-3-trabajos` (por crear desde `main`; si ya existe en `origin`, el carril está tomado)
- **Carpeta:** clon o worktree propio (por ejemplo `../elastiflix-trabajos`)
- **Archivos del carril:** paquetes `job/`, `service/` y `vector/` de ingest y `TestApplication.java`, salvo `ErrorContextTest.java`
- **Herramienta de la última sesión:** ninguna todavía

## Estado general

no iniciado

## Punto de reanudación

- **Tarea actual:** ninguna
- **Subpaso en curso:** ninguno
- **Próximo paso:** crea la rama `revision-3-trabajos` desde `origin/main` en un clon o worktree propio, marca
  este archivo `en curso`, haz commit y push (eso reserva el carril) y empieza T-08.
- **Última actualización:** 2026-10-19 00:34 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-08 | pendiente | — | «Archivo válido» aparece antes de terminar la validación y en trabajos fallidos |
| T-09 | pendiente | — | Un rechazo de archivo detiene el paso equivocado |
| T-10 (`ImportJob`, `ImportRunner`, `JobStore`) | pendiente | — | Faltan pruebas unitarias de ramas importantes |
| T-12 (`ImportCounts`, `ImportProgress`) | pendiente | — | Código muerto y campos que nadie lee |
| T-16 | pendiente | — | Hilos de la carga: estado final, excepciones y nombres |
| T-17 (`FileIntake` y `JobStore`) | pendiente | — | Detalles de la validación de archivos |
| T-19 (Javadoc y `JobStatus`) | pendiente | — | Textos de error y Javadoc que no corresponden al comportamiento |
| T-24 (`job/`) | pendiente | — | Duplicados y costo por sondeo en la carga |
| T-27 (`job/`) | pendiente | — | Pruebas que no pueden fallar, débiles o con higiene pendiente |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw -pl elastiflix-ingest -am verify` (y al final `./mvnw verify` completo)
- **Resultado:** —
- **Fecha:** —

## Decisiones

<Decisiones abiertas de este carril, con su razón, y cada reanudación.>

## Notas para el cierre

<Cambios de documentación que aplicará el carril Z: filas de PLAN §1 (texto exacto), propiedades de §7,
«Cambios respecto al diseño aprobado» de `docs/diseno/<fase>.md`, tablas de errores y README. También las
firmas viejas que la regla 1 obligó a conservar y que Z puede quitar.>

## Bloqueos y solicitudes

Vacío.

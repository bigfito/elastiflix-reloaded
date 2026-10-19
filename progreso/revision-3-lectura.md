# Estado de trabajo — Revisión 3 · Carril D (Carga: lectura)

> Carril D de la revisión 3. La especificación de cada corrección está en
> [`docs/revisiones/revision-3.md`](../docs/revisiones/revision-3.md); las reglas de los carriles, en su
> sección «Cómo trabajar en paralelo». Este archivo solo lo escribe el agente de este carril.

## Identificación

- **Unidad:** Revisión 3 · carril D
- **Nombre:** Carga: lectura
- **Rama:** `revision-3-lectura` (por crear desde `main`; si ya existe en `origin`, el carril está tomado)
- **Carpeta:** clon o worktree propio (por ejemplo `../elastiflix-lectura`)
- **Archivos del carril:** paquetes `csv/`, `json/`, `mapping/`, `movie/` y `file/` de ingest y `src/test/resources/**` de ingest
- **Herramienta de la última sesión:** ninguna todavía

## Estado general

no iniciado

## Punto de reanudación

- **Tarea actual:** ninguna
- **Subpaso en curso:** ninguno
- **Próximo paso:** crea la rama `revision-3-lectura` desde `origin/main` en un clon o worktree propio, marca
  este archivo `en curso`, haz commit y push (eso reserva el carril) y empieza T-12.
- **Última actualización:** 2026-10-19 00:34 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-12 (lectores y validador) | pendiente | — | Código muerto y campos que nadie lee |
| T-17 (`document_count`) | pendiente | — | Detalles de la validación de archivos |
| T-24 (BOM y nombres de vectores) | pendiente | — | Duplicados y costo por sondeo en la carga |
| T-27 (`mapping/`) | pendiente | — | Pruebas que no pueden fallar, débiles o con higiene pendiente |

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

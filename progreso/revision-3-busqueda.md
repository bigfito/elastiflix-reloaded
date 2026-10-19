# Estado de trabajo — Revisión 3 · Carril B (Búsqueda)

> Carril B de la revisión 3. La especificación de cada corrección está en
> [`docs/revisiones/revision-3.md`](../docs/revisiones/revision-3.md); las reglas de los carriles, en su
> sección «Cómo trabajar en paralelo». Este archivo solo lo escribe el agente de este carril.

## Identificación

- **Unidad:** Revisión 3 · carril B
- **Nombre:** Búsqueda
- **Rama:** `revision-3-busqueda` (por crear desde `main`; si ya existe en `origin`, el carril está tomado)
- **Carpeta:** clon o worktree propio (por ejemplo `../elastiflix-busqueda`)
- **Archivos del carril:** `elastiflix-search/**`
- **Herramienta de la última sesión:** ninguna todavía

## Estado general

no iniciado

## Punto de reanudación

- **Tarea actual:** ninguna
- **Subpaso en curso:** ninguno
- **Próximo paso:** crea la rama `revision-3-busqueda` desde `origin/main` en un clon o worktree propio, marca
  este archivo `en curso`, haz commit y push (eso reserva el carril) y empieza T-02.
- **Última actualización:** 2026-10-19 00:34 UTC

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

<Decisiones abiertas de este carril, con su razón, y cada reanudación.>

## Notas para el cierre

<Cambios de documentación que aplicará el carril Z: filas de PLAN §1 (texto exacto), propiedades de §7,
«Cambios respecto al diseño aprobado» de `docs/diseno/<fase>.md`, tablas de errores y README. También las
firmas viejas que la regla 1 obligó a conservar y que Z puede quitar.>

## Bloqueos y solicitudes

Vacío.

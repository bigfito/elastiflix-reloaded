# Estado de trabajo — Revisión 3 · Carril E (Web: páginas)

> Carril E de la revisión 3. La especificación de cada corrección está en
> [`docs/revisiones/revision-3.md`](../docs/revisiones/revision-3.md); las reglas de los carriles, en su
> sección «Cómo trabajar en paralelo». Este archivo solo lo escribe el agente de este carril.

## Identificación

- **Unidad:** Revisión 3 · carril E
- **Nombre:** Web: páginas
- **Rama:** `revision-3-paginas` (por crear desde `main`; si ya existe en `origin`, el carril está tomado)
- **Carpeta:** clon o worktree propio (por ejemplo `../elastiflix-paginas`)
- **Archivos del carril:** web salvo el paquete `ingest/`, `MultipartLimitsTest.java` y las aserciones de «Cargar datos» de `SmokeIT.java` (regla 8); plantillas salvo `ingest.html`; `static/css/**`, `static/img/**`, `home.js` y `results.js`
- **Herramienta de la última sesión:** ninguna todavía

## Estado general

no iniciado

## Punto de reanudación

- **Tarea actual:** ninguna
- **Subpaso en curso:** ninguno
- **Próximo paso:** crea la rama `revision-3-paginas` desde `origin/main` en un clon o worktree propio, marca
  este archivo `en curso`, haz commit y push (eso reserva el carril) y empieza T-06.
- **Última actualización:** 2026-10-19 00:34 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-06 | pendiente | — | Resultados: el filtro `adult=true` se pierde sin aviso |
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

<Decisiones abiertas de este carril, con su razón, y cada reanudación.>

## Notas para el cierre

<Cambios de documentación que aplicará el carril Z: filas de PLAN §1 (texto exacto), propiedades de §7,
«Cambios respecto al diseño aprobado» de `docs/diseno/<fase>.md`, tablas de errores y README. También las
firmas viejas que la regla 1 obligó a conservar y que Z puede quitar.>

## Bloqueos y solicitudes

Vacío.

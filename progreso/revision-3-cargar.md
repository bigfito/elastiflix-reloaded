# Estado de trabajo — Revisión 3 · Carril F (Web: Cargar datos)

> Carril F de la revisión 3. La especificación de cada corrección está en
> [`docs/revisiones/revision-3.md`](../docs/revisiones/revision-3.md); las reglas de los carriles, en su
> sección «Cómo trabajar en paralelo». Este archivo solo lo escribe el agente de este carril.

## Identificación

- **Unidad:** Revisión 3 · carril F
- **Nombre:** Web: Cargar datos
- **Rama:** `revision-3-cargar` (por crear desde `main`; si ya existe en `origin`, el carril está tomado)
- **Carpeta:** clon o worktree propio (por ejemplo `../elastiflix-cargar`)
- **Archivos del carril:** paquete `ingest/` de web y sus pruebas, `MultipartLimitsTest.java`, las aserciones de «Cargar datos» de `SmokeIT.java` (regla 8), `ingest.html`, `ingest.js`, `application.yml` de web y `src/test/browser/**`
- **Herramienta de la última sesión:** ninguna todavía

## Estado general

no iniciado

## Punto de reanudación

- **Tarea actual:** ninguna
- **Subpaso en curso:** ninguno
- **Próximo paso:** crea la rama `revision-3-cargar` desde `origin/main` en un clon o worktree propio, marca
  este archivo `en curso`, haz commit y push (eso reserva el carril) y empieza T-04.
- **Última actualización:** 2026-10-19 00:34 UTC

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

<Decisiones abiertas de este carril, con su razón, y cada reanudación.>

## Notas para el cierre

<Cambios de documentación que aplicará el carril Z: filas de PLAN §1 (texto exacto), propiedades de §7,
«Cambios respecto al diseño aprobado» de `docs/diseno/<fase>.md`, tablas de errores y README. También las
firmas viejas que la regla 1 obligó a conservar y que Z puede quitar.>

## Bloqueos y solicitudes

Vacío.

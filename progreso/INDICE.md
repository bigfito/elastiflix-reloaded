# 📍 Índice de estado del plan

> Punto de entrada para retomar el trabajo. Lo mantiene el agente que ejecuta `PLAN.md`.
> Actualízalo **al terminar cada tarea** (y al empezar y cerrar cada fase), en el mismo commit que el
> archivo de la fase, y haz push.
> Reglas completas: `PLAN.md` §9 "Estado del trabajo y reanudación".

## Punto de reanudación

- **Fase actual:** 0
- **Estado de la fase:** en curso
- **Herramienta prevista:** Cursor (rama `cursor/fase-0-andamiaje-29a6`)
- **Próximo paso:** Tarea 0.9: comprobar `.env.example` con `grep -E '=.+' .env.example`. Solo deben aparecer valores por defecto no secretos.
- **Última actualización:** 2026-10-17 23:49 UTC

## Fases

| Fase | Nombre | Compuerta | Estado | Archivo de estado | Verificación de fase |
|---|---|---|---|---|---|
| 0 | Andamiaje y calidad | — | en curso | `progreso/0.md` | — |
| 1 | Núcleo: conexión, catálogo y embeddings | 🔒 | no iniciado | `progreso/1.md` | — |
| 2 | Búsqueda | 🔒 | no iniciado | `progreso/2.md` | — |
| 3 | Carga de datos | 🔒 | no iniciado | `progreso/3.md` | — |
| 4 | Interfaz web | — | no iniciado | `progreso/4.md` | — |
| 5 | Pruebas integrales y smoke test | — | no iniciado | `progreso/5.md` | — |
| 6 | Documentación y cierre | — | no iniciado | `progreso/6.md` | — |

## Aprobaciones del usuario

| Fecha UTC | Qué se aprobó | Comentarios |
|---|---|---|
| 2026-10-17 | Decisiones sobre el plan (no es aún el visto bueno para programar) | Paquete `com.bigfito.elastiflix`; migración a alias sin aprobación previa; rutas en español; híbrida con `_msearch`; pruebas de integración contra Serverless; sin autenticación; borrar el índice anterior al publicar; registro individual directo al catálogo; CSV sin encabezado se rechaza; vectores siempre generados con inferencia; pósters de TMDB; commit + push por tarea; documentación al cerrar cada fase; estado actualizado en cada tarea; un solo usuario a la vez, sin controles de concurrencia; programa un agente de Cursor; vectores de documento con la combinación calibrada en 1.9 (los archivos no traen vectores y el formulario no los captura); atribución de TMDB en el pie y en «Acerca de»; sin restricción por costo de inferencia |
| 2026-10-17 | Visto bueno del plan e inicio de la Fase 0 | Adolfo (bigfito) pidió explícitamente ejecutar la Fase 0. El agente de Cursor arranca por la tarea 0.1. |

## Última verificación

- **Comando:** `./mvnw -q verify` tras revertir la prueba negativa
- **Resultado:** en verde (código 0) y `git status` limpio.
- **Fecha:** 2026-10-17 23:49 UTC

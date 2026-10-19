# Estado de trabajo — Revisión 3

> Correcciones de código de la tercera revisión de `main` en `27fcdef`. Una tarea por cada `T-NN`. La
> especificación de cada una (dónde, problema, corrección y criterio) está en
> [`docs/revisiones/revision-3.md`](../docs/revisiones/revision-3.md); léela antes de empezar cada tarea.

## Identificación

- **Unidad:** Revisión 3
- **Nombre:** Correcciones de código de la tercera revisión exhaustiva
- **Rama:** `revision-3` (por crear desde `main`)
- **Carpeta:** raíz del repositorio
- **Herramienta de la última sesión:** ninguna todavía (Claude Code, Google Antigravity o JetBrains Junie)

## Estado general

no iniciado

## Punto de reanudación

- **Tarea actual:** ninguna
- **Subpaso en curso:** ninguno
- **Próximo paso:** aplica el protocolo de reanudación de `AGENTS.md`, crea la rama `revision-3` desde
  `main`, marca esta unidad `en curso` y empieza T-01 según `docs/revisiones/revision-3.md`.
- **Última actualización:** 2026-10-19 00:18 UTC

## Tareas

> El hash de una tarea se completa en el commit de la tarea siguiente; nunca dejes «este commit».
> Al cerrar una tarea, sustituye su título por la evidencia: comando, resultado y la prueba que falla si se rompe el comportamiento.

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-01 | pendiente | — | La copia del índice no se cancela ante una respuesta ilegible de `_tasks` |
| T-02 | pendiente | — | Búsqueda e inferencia no traducen una respuesta ilegible a un código EFX |
| T-03 | pendiente | — | Validación k-NN: un vector de consulta sin campo da NPE |
| T-04 | pendiente | — | «Cargar datos»: el aviso de un 409 o 404 al importar o cancelar se borra al instante |
| T-05 | pendiente | — | «Cargar datos»: no se puede volver a validar el mismo archivo con otro separador |
| T-06 | pendiente | — | Resultados: el filtro `adult=true` se pierde sin aviso |
| T-07 | pendiente | — | Cada página pide `/favicon.ico`, y ese 404 consulta Elasticsearch |
| T-08 | pendiente | — | «Archivo válido» aparece antes de terminar la validación y en trabajos fallidos |
| T-09 | pendiente | — | Un rechazo de archivo detiene el paso equivocado |
| T-10 | pendiente | — | Faltan pruebas unitarias de ramas importantes |
| T-11 | pendiente | — | El manejo de estados de `ingest.js` no tiene prueba automática |
| T-12 | pendiente | — | Código muerto y campos que nadie lee |
| T-13 | pendiente | — | Límites de carga con dos orígenes y valores escritos a mano |
| T-14 | pendiente | — | Una página de error EFX lee el catálogo dos veces |
| T-15 | pendiente | — | La API de carga: un error sin código se muestra como «sin conexión», y la subida deja un temporal |
| T-16 | pendiente | — | Hilos de la carga: estado final, excepciones y nombres |
| T-17 | pendiente | — | Detalles de la validación de archivos |
| T-18 | pendiente | — | Códigos de error equivocados en búsqueda y mapping |
| T-19 | pendiente | — | Textos de error y Javadoc que no corresponden al comportamiento |
| T-20 | pendiente | — | Textos visibles sin punto final o sin fila en PLAN §1 |
| T-21 | pendiente | — | «Ver todos» aparece aunque no oculte nada |
| T-22 | pendiente | — | CSS duplicado y muerto |
| T-23 | pendiente | — | Tres maneras de agrupar miles, y cifras sin agrupar |
| T-24 | pendiente | — | Duplicados y costo por sondeo en la carga |
| T-25 | pendiente | — | Configuración y beans armados para las pruebas |
| T-26 | pendiente | — | Accesibilidad |
| T-27 | pendiente | — | Pruebas que no pueden fallar, débiles o con higiene pendiente |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** ninguno en esta unidad todavía. La última corrida registrada está en `progreso/INDICE.md`.
- **Resultado:** —
- **Fecha:** —

## Decisiones

- 2026-10-19: Adolfo pidió una revisión exhaustiva del código y de la documentación tras la revisión 2.
  Decidió que los problemas de código **solo se documentan** (esta lista) y que la documentación se alinea
  con el código actual en un commit directo a `main`. La revisión no pudo compilar ni correr pruebas: el
  entorno no tenía Maven Central ni JDK 25. Checkstyle y PMD, corridos con sus jars, siguen en 0.
- 2026-10-19: las decisiones abiertas de T-07, T-15, T-19 y T-20 las toma el agente. Elige la opción más
  simple que cumpla el criterio y la registra aquí.

## Resumen de diseño

<Al cerrar la unidad: estructura final, decisiones relevantes y diferencias con los diseños `docs/diseno/<fase>.md` que se tocaron.>

## Revisión de convenciones

<Al cerrar la unidad, responde cada punto de la lista de revisión de `docs/convenciones-java.md`.>

## Bloqueos y solicitudes

Vacío.

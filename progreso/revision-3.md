# Estado de trabajo — Revisión 3 (tablero)

> Tablero de la revisión 3 de `main` en `27fcdef`. El trabajo va en paralelo por **carriles**: cada uno
> tiene su rama y su archivo de estado. La especificación y las reglas de los carriles están en
> [`docs/revisiones/revision-3.md`](../docs/revisiones/revision-3.md) › «Cómo trabajar en paralelo».
> **Este tablero solo lo escribe el carril Z (cierre)**: los carriles A–F escriben únicamente su archivo,
> para que nunca choquen en git.

## Identificación

- **Unidad:** Revisión 3 (tablero)
- **Nombre:** Correcciones de código de la tercera revisión exhaustiva, en carriles paralelos
- **Rama:** una por carril, `revision-3-<carril>`; el cierre usa `revision-3-cierre`
- **Carpeta:** un clon o worktree por carril
- **Herramienta de la última sesión:** ninguna todavía (Claude Code, Google Antigravity o JetBrains Junie)

## Estado general

no iniciado

## Punto de reanudación

- **Tarea actual:** ninguna
- **Subpaso en curso:** ninguno
- **Próximo paso:** cada agente toma un carril libre de A–F (paso 2 de «Pasos de un carril»). Para saber
  cuáles están tomados: `git ls-remote --heads origin 'revision-3-*'` y el estado de cada archivo de carril.
  El carril Z empieza cuando los PR de A–F están fusionados.
- **Última actualización:** 2026-10-19 00:34 UTC

## Carriles

> Z actualiza esta tabla al cerrar. Mientras tanto, el estado vivo está en la rama remota y en el
> archivo de cada carril.

| Carril | Rama | Archivo de estado | Estado | PR |
|---|---|---|---|---|
| A · Núcleo | `revision-3-core` | [`progreso/revision-3-core.md`](revision-3-core.md) | libre | — |
| B · Búsqueda | `revision-3-busqueda` | [`progreso/revision-3-busqueda.md`](revision-3-busqueda.md) | libre | — |
| C · Carga: trabajos | `revision-3-trabajos` | [`progreso/revision-3-trabajos.md`](revision-3-trabajos.md) | libre | — |
| D · Carga: lectura | `revision-3-lectura` | [`progreso/revision-3-lectura.md`](revision-3-lectura.md) | libre | — |
| E · Web: páginas | `revision-3-paginas` | [`progreso/revision-3-paginas.md`](revision-3-paginas.md) | libre | — |
| F · Web: Cargar datos | `revision-3-cargar` | [`progreso/revision-3-cargar.md`](revision-3-cargar.md) | libre | — |
| Z · Cierre | `revision-3-cierre` | este tablero | espera a A–F | — |

## Tareas

> Una `T-NN` está terminada cuando todas sus partes (columna «Carriles») lo están. La evidencia de cada
> parte está en el archivo de su carril; Z la resume aquí al cerrar.

| Id | Carriles | Estado | Corrección |
|---|---|---|---|
| T-01 | A | pendiente | La copia del índice no se cancela ante una respuesta ilegible de `_tasks` |
| T-02 | A, B | pendiente | Búsqueda e inferencia no traducen una respuesta ilegible a un código EFX |
| T-03 | B | pendiente | Validación k-NN: un vector de consulta sin campo da NPE |
| T-04 | F | pendiente | «Cargar datos»: el aviso de un 409 o 404 al importar o cancelar se borra al instante |
| T-05 | F | pendiente | «Cargar datos»: no se puede volver a validar el mismo archivo con otro separador |
| T-06 | E | pendiente | Resultados: el filtro `adult=true` se pierde sin aviso |
| T-07 | E | pendiente | Cada página pide `/favicon.ico`, y ese 404 consulta Elasticsearch |
| T-08 | C | pendiente | «Archivo válido» aparece antes de terminar la validación y en trabajos fallidos |
| T-09 | C | pendiente | Un rechazo de archivo detiene el paso equivocado |
| T-10 | A, B, C | pendiente | Faltan pruebas unitarias de ramas importantes |
| T-11 | F | pendiente | El manejo de estados de `ingest.js` no tiene prueba automática |
| T-12 | A, B, C, D, E | pendiente | Código muerto y campos que nadie lee |
| T-13 | E, F | pendiente | Límites de carga con dos orígenes y valores escritos a mano |
| T-14 | E | pendiente | Una página de error EFX lee el catálogo dos veces |
| T-15 | F | pendiente | La API de carga: un error sin código se muestra como «sin conexión», y la subida deja un temporal |
| T-16 | C | pendiente | Hilos de la carga: estado final, excepciones y nombres |
| T-17 | C, D | pendiente | Detalles de la validación de archivos |
| T-18 | B | pendiente | Códigos de error equivocados en búsqueda y mapping |
| T-19 | A, C, E | pendiente | Textos de error y Javadoc que no corresponden al comportamiento |
| T-20 | E, F | pendiente | Textos visibles sin punto final o sin fila en PLAN §1 |
| T-21 | E | pendiente | «Ver todos» aparece aunque no oculte nada |
| T-22 | E | pendiente | CSS duplicado y muerto |
| T-23 | E | pendiente | Tres maneras de agrupar miles, y cifras sin agrupar |
| T-24 | C, D | pendiente | Duplicados y costo por sondeo en la carga |
| T-25 | A, E, F | pendiente | Configuración y beans armados para las pruebas |
| T-26 | E, F | pendiente | Accesibilidad |
| T-27 | A, B, C, D, E, F | pendiente | Pruebas que no pueden fallar, débiles o con higiene pendiente |

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
- 2026-10-19: Adolfo pidió que el trabajo de la revisión 3 se planifique para varios agentes en paralelo,
  sin dependencias que obliguen a detenerse. Se repartió en seis carriles con archivos disjuntos (A–F) y un
  cierre (Z). Las partes que cruzaban módulos se resolvieron así: firmas públicas congeladas entre módulos,
  `ErrorContextTest` y los textos de `ErrorCode` en A, todo el CSS en E, y la documentación compartida en Z.
- 2026-10-19: las decisiones abiertas de T-07, T-15, T-19 y T-20 las toma el carril que hace cada parte y
  las registra en su archivo.

## Resumen de diseño

<Al cerrar la unidad (Z): estructura final, decisiones relevantes y diferencias con los diseños que se tocaron.>

## Revisión de convenciones

<Al cerrar la unidad (Z), responde cada punto de la lista de revisión de `docs/convenciones-java.md`.>

## Bloqueos y solicitudes

Vacío.

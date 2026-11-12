# 🤝 Cómo trabaja el equipo de calidad del catálogo

Instrucciones para los agentes que ejecutan [`PLAN.md`](PLAN.md). Son obligatorias. Si tu herramienta no carga este
archivo sola, léelo completo antes de la primera acción.

El trabajo es de un **equipo de cuatro agentes y una persona**. Tres agentes ejecutan, uno revisa, y avanzan por
**turnos** en un orden fijo. Nadie depende de la memoria de otro: lo que el siguiente necesita saber queda escrito en
la bitácora y en git, en la rama local `dq/ronda-4`.

## 1. El equipo

| Integrante | Papel | Qué hace en su turno |
|---|---|---|
| **Google Antigravity** | Ejecutor | Ejecuta las tareas de su turno (roles DATOS, JAVA, EVAL o DOCS, según la tarea) |
| **Cursor** | Ejecutor | Igual que Antigravity |
| **GitHub Copilot** (modo agente, con el modelo **Kimi K3**) | Ejecutor | Igual que Antigravity |
| **Claude Code** | Revisor y coordinador | Después de **cada** turno de ejecución: revisa todo lo que hizo el ejecutor, **corrige** lo que quedó mal, deja el tablero listo y da paso al siguiente. No toma tareas del plan |
| **Dueña del proyecto** (humana) | Decide y aprueba | Contesta P-0, H-1, H-2 y D-xx; integra los hitos en `main`; da la orden de arrancar cada turno |

El orden de los turnos es siempre el mismo y se repite hasta terminar:

```text
Antigravity → Claude Code → Cursor → Claude Code → GitHub Copilot → Claude Code → Antigravity → …
```

El reparto exacto (21 turnos de ejecución, 7 por ejecutor y 2 grandes cada uno) está en
[`PLAN.md` · Reparto y turnos](PLAN.md#reparto-y-turnos). Ningún ejecutor toma un turno que no es suyo, aunque esté
libre: si el que sigue no está disponible, la dueña del proyecto decide quién lo cubre y se anota como decisión.

## 2. Archivos que mandan

| Archivo | Para qué | Quién lo cambia |
|---|---|---|
| [`PLAN.md`](PLAN.md) | Qué hay que hacer: WP, tareas, criterios y reparto de turnos | Nadie durante la ejecución. Un cambio de plan se anota como decisión |
| [`BITACORA.md`](BITACORA.md) | **Tablero**: turnos, WP, revisiones, próximo paso, staging, aprobaciones, métricas | El ejecutor, su fila de turno y las de sus WP; Claude Code, el resto |
| `bitacora/WP-XX.md` | Diario del WP: tareas, lotes, desviaciones, entradas de sesión, punto de reanudación | Quien trabaje en el WP |
| `bitacora/R-NN.md` | Informe de la revisión del turno NN | Claude Code |
| `cambios/WP-XX/lote-NNN.jsonl` · `estado.tsv` | Cambios a `data/` con su fuente, y el resultado por película | El ejecutor; Claude Code agrega lotes de corrección |
| `hallazgos/listas/*.tsv` · `hallazgos/salidas/m0/` | Alcance congelado y línea base | Nadie |

## 3. Reglas de seguridad (no negociables)

1. **Nunca escribas una sinopsis.** Ni con un modelo ni a mano: solo código que copia de la fuente (norma NS-2 del
   plan). Es exactamente el defecto que este plan corrige. Vale también para las correcciones de Claude Code.
2. **Producción es el alias `elastiflix-reloaded`.** Solo `publicar.py` lo cambia, y solo con H-2 aprobada.
   `cargar.py` se niega a cargar producción. Toda instancia de la aplicación que levantes lleva
   `ELASTIFLIX_INDEX_ALIAS=elastiflix-dq-staging` y `SERVER_PORT=8081` **explícitos**: el `.env` apunta a producción.
   Una aprobación solo pasa a `APROBADA` con la respuesta literal de la dueña del proyecto; nadie se aprueba solo.
3. **`scripts/es.py` es de solo lectura** salvo en índices `elastiflix-dq-staging*`, y ahí solo con
   `DQ_PERMITIR_ESCRITURA=1` y H-1 aprobada. No lo modifiques para saltar esa regla.
4. **Secretos:** nunca imprimas, copies a la conversación ni comitees `.env`, la URL ni la API key.
5. **Trabaja solo en el worktree** (§4). El checkout principal es de la dueña del proyecto y su aplicación lee
   plantillas de su `target/classes`. Nada de `./mvnw clean` ni de borrar `target/`, en ningún checkout.
6. **`data/` se cambia solo con `aplicar_cambios.py`.** Nunca a mano ni con buscar y reemplazar.
7. **Git, en local (P-04):** nada de `push` ni de `pull` a `origin`; nunca commit a `main`, nunca `--force`, nunca
   `--no-verify`, nunca reescribir commits de otro turno (`rebase`, `amend`, `reset`). Un push a `main` despliega la
   demo.
8. **Ante la duda, pregunta:** abre una decisión D-xx o un bloqueo en el tablero y sigue con otra tarea de tu turno.
   No inventes lo que el plan no define.

## 4. El worktree

Todo el trabajo ocurre en la copia local **`E:\02 - programming\03 - elastiflix-reloaded-dq`**, en la rama
`dq/ronda-4`, con su propio `.env` y su propio `target/`. El checkout principal
(`E:\02 - programming\03 - elastiflix-reloaded`, en `main`) no se toca. Abre tu herramienta con el worktree como
carpeta del proyecto.

La única excepción es WP-03, que vive en la rama `dq/wp-03-reglas-plot` porque no puede integrarse hasta después de
WP-05. Su worktree es `E:\02 - programming\03 - elastiflix-reloaded-dq-wp03`, lo crea el ejecutor del turno 8 con
`git worktree add "../03 - elastiflix-reloaded-dq-wp03" -b dq/wp-03-reglas-plot dq/ronda-4`, y el tablero de ese
turno se sigue escribiendo en el worktree principal.

Si hubiera que recrear el worktree principal, desde el checkout principal:
`git worktree add "../03 - elastiflix-reloaded-dq" dq/ronda-4`, y copiar `.env` sin mostrar su contenido.

## 5. Turno de un ejecutor

### Al empezar

1. `git status` y `git log --oneline -5`. El último commit debe ser el cierre de la revisión anterior
   (`Revisión R-NN …`) y debe existir la etiqueta `dq-r<NN>` (`git tag --list "dq-*"`); en el turno 1, la
   etiqueta `dq-r00`, que marca el plan listo para empezar. Si hay cambios sin commitear, **no los toques**: anota un bloqueo y avisa a la
   dueña del proyecto.
2. Lee `BITACORA.md` completo. «Próximo paso» debe nombrarte a ti y a tu turno. Si nombra a otro, detente y avisa.
3. Lee `bitacora/R-NN.md`, la revisión del turno anterior: **qué corrigió Claude Code y por qué**. Revisa sus commits
   de corrección (`git show --stat` de cada uno). Si no estás de acuerdo con una corrección, anótalo en tu diario y
   como bloqueo para la dueña del proyecto, pero no la deshagas. Así las correcciones del revisor también tienen
   cuatro ojos.
4. **Reclama** el turno: en «Turnos» pon `EN CURSO`, tu nombre y la fecha, y en «Tablero» pon tus WP en `EN CURSO`.
   Commit `Bitácora: toma el turno NN`.
5. Abre la tarjeta de tus WP en `PLAN.md`, `bitacora/WP-XX.md` (créalo con la plantilla del §10 si no existe) y, en
   un WP de datos, `cambios/WP-XX/estado.tsv`.

### Durante

- Haz **solo las tareas de tu turno**, en el orden del plan. Si terminas antes, no adelantes trabajo del turno
  siguiente: ciérralo bien.
- Al terminar cada tarea, marca su casilla en el diario con la evidencia: comando, resultado y archivo.
- Un commit por lote o por tarea, con el formato del §7.
- En un WP de datos, ejecuta el flujo completo de cada lote: proponer, aplicar, validar y registrar. La revisión del
  lote la hace Claude Code en su turno.
- Si algo del plan no funciona como dice, anótalo como desviación en el diario; si cambia el alcance, ábrelo como
  decisión en el tablero.

### Al terminar (aunque no hayas acabado)

1. Corre las comprobaciones de tu turno (`./mvnw verify` si tocaste código o `data/`; las que diga la tarjeta).
2. Actualiza el **punto de reanudación** y agrega tu entrada de sesión en cada diario de WP que tocaste.
3. En «Turnos»: `EN REVISIÓN` si cumpliste todo, o `INCOMPLETO` con lo que faltó. En «Próximo paso»:
   «Claude Code: revisión R-NN».
4. Commit `Turno NN: <resumen>` y crea la etiqueta del fin de turno: `git tag dq-t<NN>` (por ejemplo `dq-t07`).
5. Si levantaste staging, apágala o anota en la línea «Staging» que sigue arriba. **Nunca termines con cambios sin
   commitear.**

## 6. Turno de revisión (Claude Code)

Se hace después de cada turno de ejecución, con el contexto limpio: no se apoya en lo que el ejecutor dijo en su
conversación, solo en lo que dejó en git y en la bitácora.

1. **Alcance.** `git diff dq-r<NN-1>..dq-t<NN> --stat` y la tarjeta de los WP del turno. Cada criterio
   «Hecho cuando» se comprueba con su comando; no se da por cierto.
2. **Comprobaciones.** `./mvnw verify`; `./mvnw -q test "-Dtest=ShippedCatalogValidationTest"` si cambió `data/`;
   `python data-quality/scripts/analizar.py --etiqueta r<NN> --desde-data` y comparación con el indicador del WP;
   lectura del código nuevo contra `docs/convenciones-java.md` o contra la especificación de WP-00.
3. **Lotes.** Muestreo de cada lote aplicado en el turno: 10 líneas al azar (20 en un lote mecánico) contra la fuente
   (NS-1 a NS-7). Si una línea está mal, se corrige con un **lote de corrección** nuevo (`lote-NNN.jsonl` con el
   número siguiente y `motivo: "corrección R-NN"`), nunca editando el lote original.
4. **Corregir.** Todo lo que quedó mal se corrige en este turno, con commits separados
   `Revisión R-NN: corrige <qué>`, para distinguir lo del ejecutor de lo del revisor. Si la corrección necesita una
   decisión de la dueña del proyecto, se abre como D-xx y no se adivina.
5. **Informe.** `bitacora/R-NN.md` con: veredicto (`APROBADO`, `APROBADO CON CORRECCIONES` o
   `PENDIENTE DE DECISIÓN`), lo que se comprobó y con qué comando, cada hallazgo con su corrección y su commit, y lo
   que el siguiente ejecutor debe saber. Una fila en «Revisiones» de la bitácora.
6. **Tablero.** WP cumplidos a `HECHA`; WP de varios turnos siguen `EN CURSO`, con el avance; el turno pasa a
   `REVISADO`; «Próximo paso» con el turno siguiente, su ejecutor y su primera tarea. Si el turno quedó
   `INCOMPLETO`, lo que faltó lo termina Claude Code en esta revisión, si es una corrección, o pasa como primera tarea
   del siguiente turno del **mismo** ejecutor, anotado como decisión.
7. **Cierre.** Commit `Revisión R-NN: <veredicto>` y `git tag dq-r<NN>`.

En los hitos (después de los turnos 7, 14 y 21) Claude Code escribe además el resumen del hito en el diario del plan
y avisa a la dueña del proyecto que puede integrar `dq/ronda-4` en `main`.

## 7. Git

| Elemento | Convención |
|---|---|
| Rama | `dq/ronda-4` para todo; `dq/wp-03-reglas-plot` solo para WP-03 hasta su integración (turno 13) |
| Etiquetas | `dq-t<NN>` al cerrar el turno de ejecución NN; `dq-r<NN>` al cerrar su revisión. Locales; no se publican |
| Commit | En español, primera línea en indicativo, como el resto del repo («Reemplaza 100 ensayos de plot con la trama de Wikipedia»); cuerpo con viñetas; al final:<br>`Plan: turno 05 · WP-02 · T-01 · lote-004`<br>`Rol: DATOS`<br>`Agente: Cursor (<modelo>)`<br>más las líneas de atribución que exija la herramienta |
| Integración a `main` | Solo la dueña del proyecto, en su checkout: `git merge --no-ff dq/ronda-4`, después de un hito. Ella decide si y cuándo hace `push` |

## 8. Definición de hecho

**De un turno de ejecución:** las tareas del turno cumplen su «Hecho cuando», con evidencia en el diario;
`./mvnw verify` en verde si se tocó código o `data/`; documentación en español **y** en inglés si cambió algo
visible; tablero y diarios al día; todo commiteado y la etiqueta `dq-t<NN>` creada.

**De un WP:** todas sus tareas hechas, en uno o varios turnos; si cambió `data/`, `ShippedCatalogValidationTest` en
verde, `estado.tsv` sin filas en `propuesto` y cada lote muestreado por Claude Code. Lo pasa a `HECHA` Claude Code.

## 9. Cómo arrancar cada herramienta

La dueña del proyecto abre la herramienta que toca, con el worktree `E:\02 - programming\03 - elastiflix-reloaded-dq`
como carpeta, y envía el mensaje de la tabla.

| Herramienta | Notas | Mensaje |
|---|---|---|
| **Google Antigravity** | Agent Manager, con el worktree como workspace. Si planea con sus propios artefactos, el plan oficial sigue siendo `PLAN.md` y el estado, `BITACORA.md` | `Eres Google Antigravity, ejecutor del equipo de calidad. Lee data-quality/AGENTS.md y ejecuta tu turno según «Próximo paso» de data-quality/BITACORA.md.` |
| **Cursor** | Chat en modo Agent. Agrega `data-quality/` al contexto | `Eres Cursor, ejecutor del equipo de calidad. Lee data-quality/AGENTS.md y ejecuta tu turno según «Próximo paso» de data-quality/BITACORA.md.` |
| **GitHub Copilot** | Copilot Chat de VS Code en modo Agent, con **Kimi K3** como modelo. Firma sus commits como `Agente: GitHub Copilot (Kimi K3)`. Agrega `data-quality/AGENTS.md` y `data-quality/BITACORA.md` como contexto (`#file`) | `Eres GitHub Copilot, ejecutor del equipo de calidad. Lee data-quality/AGENTS.md y ejecuta tu turno según «Próximo paso» de data-quality/BITACORA.md.` |
| **Claude Code** | `data-quality/CLAUDE.md` importa este archivo | `Eres Claude Code, revisor del equipo de calidad. Lee data-quality/AGENTS.md y haz la revisión del último turno según data-quality/BITACORA.md.` |

Comandos con variables de entorno, según el shell:

| Shell | Harness sobre staging |
|---|---|
| PowerShell | `$env:ELASTIFLIX_INDEX_ALIAS='elastiflix-dq-staging'; ./mvnw verify -Pit "-Dit.test=RelevanceIT"` |
| Git Bash | `ELASTIFLIX_INDEX_ALIAS=elastiflix-dq-staging ./mvnw verify -Pit -Dit.test=RelevanceIT` |

En Git Bash, antepón `MSYS_NO_PATHCONV=1` cuando pases rutas de API como `/_count` a `es.py`. Después de cada
harness, comprueba que `target/eval/report.md` tenga la hora de esa corrida: si el alias no tenía 6 899 películas, la
prueba se omitió y no hubo medición.

## 10. Plantillas

### `bitacora/WP-XX.md`

```markdown
# WP-XX · <título del plan>

## Punto de reanudación
- Última tarea completa: T-NN (commit abc1234, turno NN)
- Siguiente acción exacta: `python data-quality/scripts/... `
- A medio hacer: (nada | archivo y estado)

## Tareas
- [ ] T-01 · <texto del plan> — turno: · evidencia:
- [ ] T-02 · …

## Lotes
| Lote | Películas | Turno y ejecutor | Muestreo de la revisión | Aplicado (commit) |
|---|---|---|---|---|

## Desviaciones y hallazgos

## Diario
### AAAA-MM-DD HH:MM UTC · <agente> · turno NN · <rol>
- Hice:
- Evidencia:
- Quedó pendiente:
- Para el siguiente:
```

### `bitacora/R-NN.md`

```markdown
# Revisión R-NN · turno NN de <ejecutor>

**Veredicto:** APROBADO | APROBADO CON CORRECCIONES | PENDIENTE DE DECISIÓN
**Rango revisado:** dq-r<NN-1>..dq-t<NN> · **Fecha:** AAAA-MM-DD HH:MM UTC

## Comprobaciones
| Criterio del plan | Comando | Resultado |
|---|---|---|

## Hallazgos y correcciones
| # | Qué estaba mal | Corrección | Commit |
|---|---|---|---|

## Para el siguiente ejecutor
```

### Respuesta a una aprobación o decisión (en `BITACORA.md` §3)

La columna «Estado» pasa de `ABIERTA` a `APROBADA AAAA-MM-DD · «<texto literal>»` o a
`RECHAZADA AAAA-MM-DD · «<texto literal>»`. `es.aprobada()` lee exactamente ese formato.

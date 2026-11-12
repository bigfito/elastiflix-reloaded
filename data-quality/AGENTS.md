# 🤝 Cómo trabaja el equipo de calidad del catálogo

Instrucciones para cualquier agente (Claude Code, Cursor, Google Antigravity, Qwen 2.5 Coder u otro) que tome
trabajo de [`PLAN.md`](PLAN.md). Son obligatorias. Si tu herramienta no carga este archivo sola, léelo completo antes
de la primera acción.

El trabajo es de un **equipo**: varios agentes, en sesiones distintas y a veces con herramientas distintas, avanzan
un mismo plan. Nadie depende de la memoria de otro. Lo que el siguiente necesita saber queda escrito en la bitácora
y en git, en la rama de integración `dq/ronda-4`.

## 1. Archivos que mandan

| Archivo | Para qué | Quién lo cambia |
|---|---|---|
| [`PLAN.md`](PLAN.md) | Qué hay que hacer: WP, tareas y criterios | Nadie durante la ejecución; un cambio de plan se anota como decisión |
| [`BITACORA.md`](BITACORA.md) | **Tablero**: estado de cada WP, llave de `data/`, staging, próximo paso, aprobaciones, métricas | Cada agente, su fila al tomar y al cerrar; COORD el resto |
| `bitacora/WP-XX.md` | Diario del WP: tareas, lotes, desviaciones, entradas de sesión y punto de reanudación | El agente que tiene el WP |
| `cambios/WP-XX/lote-NNN.jsonl` · `cambios/WP-XX/estado.tsv` | Cambios propuestos a `data/` con su fuente, y el resultado por película | DATOS; REV marca rechazos |
| `hallazgos/listas/*.tsv` · `hallazgos/salidas/m0/` | Alcance congelado y línea base del informe | Nadie |

## 2. Roles

Un agente puede tener varios roles en una sesión y firma cada entrada con el rol que ejercía. La regla de **cuatro
ojos** no se salta: quien escribió un cambio no lo aprueba como REV. Si solo hay un agente, REV se hace en una
sesión nueva, con el contexto limpio, y lo dice en su entrada.

| Rol | Responde por | Entrega |
|---|---|---|
| **COORD** · coordinación | Elegir el siguiente WP, mantener el tablero, abrir decisiones y bloqueos, pedir aprobaciones, abrir los PR de hito | Tablero al día; «Próximo paso» siempre accionable |
| **DATOS** · curaduría | Scripts que proponen cambios a `data/`, lotes, aplicación | Lotes aplicados y `data/` válido |
| **JAVA** · validación | Reglas de carga, pruebas y textos de error, según `docs/convenciones-java.md` | `./mvnw verify` en verde |
| **EVAL** · relevancia | Staging, harness, mediciones M0–M3, umbrales | Tablas de métricas en la bitácora |
| **DOCS** · documentación | README es/en, `data/README.md`, «Acerca de», `eval/README.md` | Textos bilingües coherentes con los datos |
| **REV** · revisión | Muestreo de lotes contra la fuente, revisión de diffs, paso de un WP a HECHA | Veredicto escrito en `bitacora/WP-XX.md` |
| **Dueña del proyecto** (humana) | P-0, H-1, H-2 y decisiones D-xx; merge de los PR de hito a `main` | Respuesta anotada literal en la bitácora |

## 3. Reglas de seguridad (no negociables)

1. **Nunca escribas una sinopsis.** Ni con un modelo ni a mano: solo código que copia de la fuente (norma NS-2 del
   plan). Es exactamente el defecto que este plan corrige.
2. **Producción es el alias `elastiflix-reloaded`.** Solo `publicar.py` lo cambia, y solo con H-2 aprobada.
   `cargar.py` se niega a cargar producción. Toda instancia de la aplicación que levantes lleva
   `ELASTIFLIX_INDEX_ALIAS=elastiflix-dq-staging` y `SERVER_PORT=8081` **explícitos**: el `.env` apunta a producción.
   Una fila de aprobación solo pasa a `APROBADA` con la respuesta literal de la dueña del proyecto, escrita por ella o
   copiada de su mensaje; ningún agente se aprueba a sí mismo.
3. **`scripts/es.py` es de solo lectura** salvo en índices `elastiflix-dq-staging*`, y ahí solo con
   `DQ_PERMITIR_ESCRITURA=1` y H-1 aprobada. No lo modifiques para saltar esa regla.
4. **Secretos:** nunca imprimas, copies a la conversación ni comitees `.env`, la URL ni la API key.
5. **Trabaja en el worktree** (§4), nunca en el checkout principal: la aplicación de la dueña del proyecto lee
   plantillas de su `target/classes`. Nada de `./mvnw clean` ni de borrar `target/`, en ningún checkout.
6. **`data/` se cambia solo con `aplicar_cambios.py`** y con la llave de `data/` (§5). Nunca a mano ni con buscar y
   reemplazar.
7. **Git, en local (decisión P-04):** se trabaja solo en el repositorio local. Nada de `push` ni de `pull` a
   `origin`; nunca commit a `main`, nunca `--force`, nunca `--no-verify`. Un push a `main` despliega la demo.
8. **Ante la duda, pregunta:** abre una decisión D-xx o un bloqueo en el tablero y pasa a otra tarea. No inventes lo
   que el plan no define.

## 4. Protocolo de sesión

### El worktree

El trabajo ocurre en una copia local del repositorio: el worktree **`E:\02 - programming\03 - elastiflix-reloaded-dq`**,
en la rama `dq/ronda-4`, con su propio `.env` y su propio `target/`. El checkout principal
(`E:\02 - programming\03 - elastiflix-reloaded`, en `main`) es de la dueña del proyecto y no se toca. Todos los
agentes trabajan en esta misma máquina y comparten la rama: no hay remoto de por medio.

Si hubiera que recrearlo, desde el checkout principal:

```bash
git worktree add "../03 - elastiflix-reloaded-dq" dq/ronda-4
```

y copiar `.env` al worktree con el explorador de archivos o `cp`, sin mostrar su contenido.

### Al empezar (siempre)

1. En el worktree: `git status` y `git log --oneline -5`. Si hay cambios sin commitear que no son tuyos,
   **no los toques**: anótalo como bloqueo y avisa.
2. Lee `BITACORA.md` completo: tablero, llave de `data/`, staging, «Próximo paso», aprobaciones y bloqueos.
3. Elige trabajo:
   - Si «Próximo paso» nombra un WP **EN CURSO** cuyo responsable no tiene entradas desde hace más de 24 h, puedes
     retomarlo: anota «retomado de <agente>» en su diario.
   - Si no, toma el primer WP **PENDIENTE** cuyas dependencias del plan estén en **HECHA**.
4. **Reclama** el WP en el tablero: `EN CURSO`, tu nombre de agente y la fecha; si es de datos, toma también la llave
   de `data/`. Commit `Bitácora: toma WP-XX` en `dq/ronda-4`. Desde ese momento es tuyo.
5. Abre `bitacora/WP-XX.md` (desde la plantilla del §9 si no existe), lee su **punto de reanudación** y, en un WP
   de datos, `cambios/WP-XX/estado.tsv`.

### Durante

- Trabaja por tareas (`T-NN`) del plan, de una en una. Al terminar cada una, marca su casilla en el diario con la
  evidencia: comando, resultado y archivo.
- **Commit pequeño y frecuente**, uno por lote o por tarea. Nada se queda solo en tu sesión.
- Si algo del plan no funciona como dice, no lo rodees en silencio: anótalo como desviación y, si cambia el
  alcance, como decisión en el tablero.

### Al terminar (aunque no hayas acabado)

1. Actualiza el **punto de reanudación** del diario: última tarea completa, siguiente comando exacto, archivos a
   medio hacer.
2. Agrega tu entrada de sesión al diario.
3. Actualiza tu fila del tablero y reescribe «Próximo paso» con **una** instrucción concreta para el siguiente.
4. Commit en `dq/ronda-4`. Si quedó a medias, el commit empieza con `WIP:`. **Nunca termines con cambios sin
   commitear.** Si levantaste una instancia de staging, apágala o anota en la línea «Staging» que sigue arriba.
5. Si el WP cumple sus criterios: ponlo en `EN REVISIÓN`, libera la llave de `data/` y deja en «Próximo paso»
   «REV: revisar WP-XX».

### Estados de un WP

`PENDIENTE` → `EN CURSO` → `EN REVISIÓN` → `HECHA`, o `BLOQUEADA` en cualquier momento, con el bloqueo escrito.
Solo REV pasa un WP a `HECHA`, en `dq/ronda-4`, cuando la definición de hecho (§7) se cumple.

## 5. Trabajo en paralelo

- **Llave de `data/`.** Solo un WP a la vez cambia `data/`, y el tablero dice quién la tiene. Los WP de datos van en
  el orden del plan.
- **En paralelo sí:** WP-03 y WP-10 (código), cada uno en su rama desde `dq/ronda-4` y en su propio worktree
  (`git worktree add "../03 - elastiflix-reloaded-dq-wp03" -b dq/wp-03-reglas-plot dq/ronda-4`). Sus cambios de tablero se hacen
  en `dq/ronda-4`, no en su rama. Se integran con `git merge` en `dq/ronda-4` cuando REV lo aprueba y su dependencia
  lo permite.
- **REV en paralelo:** puede revisar un lote mientras DATOS prepara el siguiente.
- **Conflicto en `BITACORA.md`:** conserva cada fila con su estado más avanzado y todas las entradas. Las entradas
  nunca se borran ni se reescriben.

## 6. Git y PR

| Elemento | Convención |
|---|---|
| Rama de integración | `dq/ronda-4`: todo el trabajo y la bitácora. Ahí avanzan los WP de datos |
| Ramas de código | `dq/wp-03-reglas-plot` y `dq/wp-10-mapping`, desde `dq/ronda-4`, que vuelven a ella con merge |
| Commit | En español, primera línea en indicativo, como el resto del repo («Reemplaza 100 ensayos de plot con la trama de Wikipedia»); cuerpo con viñetas; al final:<br>`Plan: WP-02 · T-01 · lote-003`<br>`Rol: DATOS`<br>`Agente: <herramienta> (<modelo>)`<br>más las líneas de atribución que exija tu herramienta |
| Hito | **A** tras M1 (WP-01 y 02), **B** tras M2 (WP-03 a 06) y **C** en C-01. COORD escribe en el diario del plan el resumen del hito: WP incluidos, cifras antes y después y lo que queda fuera. Si la dueña del proyecto pide un PR, el texto es el mismo |
| Integración a `main` | Solo la dueña del proyecto, en su checkout: `git merge --no-ff dq/ronda-4`. Ella decide si y cuándo hace `push`: un push a `main` despliega la demo con Cloud Build. Los datos nuevos no llegan al índice hasta C-01 |

## 7. Definición de hecho (todo WP)

- [ ] Todos los criterios «Hecho cuando» del WP se cumplen, con evidencia en el diario.
- [ ] `./mvnw verify` en verde (y `-Pit` donde el WP lo pida, leyendo `target/eval/report.md` de esa corrida).
- [ ] Si el WP cambió `data/`: `ShippedCatalogValidationTest` en verde, `estado.tsv` sin filas en `propuesto` y cada
      lote con veredicto de REV.
- [ ] Documentación en español **y** en inglés, si el WP cambia algo visible o documentado.
- [ ] Tablero y diario al día, todo commiteado en `dq/ronda-4`.

## 8. Notas por herramienta

| Herramienta | Cómo arrancar |
|---|---|
| **Claude Code** | Abre el worktree y escribe: *«Lee `data-quality/AGENTS.md` y continúa el plan desde `data-quality/BITACORA.md`»*. `data-quality/CLAUDE.md` importa este archivo |
| **Cursor** | Abre el worktree como proyecto y envía el mismo mensaje al agente. Agrega `data-quality/` al contexto |
| **Google Antigravity** | Worktree como workspace, mismo mensaje en el Agent Manager. Si planea con sus propios artefactos, el plan oficial sigue siendo `PLAN.md` y el estado, `BITACORA.md` |
| **Qwen 2.5 Coder** (Qwen Code, Continue, Aider u otro) | Contexto corto: carga solo este archivo, la tarjeta de tu WP en `PLAN.md` y `bitacora/WP-XX.md`. **Una tarea `T-NN` o un lote por sesión.** Copia los comandos tal cual. Toma WP de perfil «Cualquiera» |

Comandos con variables de entorno, según el shell:

| Shell | Harness sobre staging |
|---|---|
| PowerShell | `$env:ELASTIFLIX_INDEX_ALIAS='elastiflix-dq-staging'; ./mvnw verify -Pit "-Dit.test=RelevanceIT"` |
| Git Bash | `ELASTIFLIX_INDEX_ALIAS=elastiflix-dq-staging ./mvnw verify -Pit -Dit.test=RelevanceIT` |

En Git Bash, antepón `MSYS_NO_PATHCONV=1` cuando pases rutas de API como `/_count` a `es.py`. Después de cada
harness, comprueba que `target/eval/report.md` tenga la hora de esa corrida: si el alias no tenía 6 899 películas, la
prueba se omitió y no hubo medición.

## 9. Plantillas

### `bitacora/WP-XX.md`

```markdown
# WP-XX · <título del plan>

## Punto de reanudación
- Última tarea completa: T-NN (commit abc1234)
- Siguiente acción exacta: `python data-quality/scripts/... --lote 004`
- A medio hacer: (nada | archivo y estado)

## Tareas
- [ ] T-01 · <texto del plan> — evidencia:
- [ ] T-02 · …

## Lotes
| Lote | Películas | Propuesto por | Revisión (muestra y veredicto) | Aplicado (commit) |
|---|---|---|---|---|

## Desviaciones y hallazgos

## Diario
### AAAA-MM-DD HH:MM UTC · <agente> · <rol>
- Hice:
- Evidencia:
- Quedó pendiente:
- Para el siguiente:
```

### Respuesta a una aprobación o decisión (en `BITACORA.md` §3)

La columna «Estado» pasa de `ABIERTA` a `APROBADA AAAA-MM-DD · «<texto literal>»` o a
`RECHAZADA AAAA-MM-DD · «<texto literal>»`. `es.aprobada()` lee exactamente ese formato.

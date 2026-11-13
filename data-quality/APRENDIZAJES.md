# 🧠 Aprendizajes · calidad del catálogo `elastiflix-reloaded` (ronda 4)

Memoria de la ronda 4 de calidad del índice `elastiflix-reloaded`, que vive en el proyecto Elasticsearch Serverless de
la demo. El trabajo fue del 12 al 13 de noviembre de 2026; la última tarea es del 20 de noviembre.

Este documento sirve a una persona o a un agente que necesite:
- entender qué se hizo para mejorar el índice y por qué;
- repetir el trabajo en una ronda 5, o aprender de los errores de esta;
- ajustar otra documentación del proyecto (ver [§11](#11-impacto-en-otra-documentación-del-proyecto)).

No reemplaza a los documentos de trabajo. El detalle de cada paso está en [`BITACORA.md`](BITACORA.md) (tablero,
decisiones y métricas), [`PLAN.md`](PLAN.md) (la especificación de cada paquete de trabajo), [`AGENTS.md`](AGENTS.md)
(el protocolo del equipo) y [`bitacora/`](bitacora/) (diarios por WP e informes de revisión R-NN). Aquí está lo que
conviene recordar de todo eso.

---

## 1. En una página

**El problema.** Unas 1 400 sinopsis (`plot`) del catálogo eran **texto generado por un modelo**: ensayos con frases
del tipo «At its core…», respuestas de un asistente que no encontró la película y datos inventados. Además:
- unas 3 700 sinopsis abrían con la ficha enciclopédica («X is a 2015 American film directed by…»);
- unas 1 500 hablaban de taquilla o crítica en lugar de la trama;
- casi ninguna decía de dónde venía.

**La regla que lo ordenó todo.** Como el defecto era texto escrito por un modelo, **ningún agente escribió una sola
sinopsis** (norma NS-2). Todo cambio copió texto de una fuente verificable con código:
- primero, la sección de trama del artículo de Wikipedia en inglés;
- si no había, el `overview` de TMDB que ya traía el documento;
- si no había ninguna de las dos, la sinopsis se quedó como estaba, con su origen marcado.

**Cómo se trabajó.** Se armó un equipo de agentes por turnos, con Claude Code revisando cada turno, y la dueña del
proyecto decidió lo importante:
- 22 turnos (el 13 partido en 13a y 13b), 24 revisiones (R-00 a R-22, con R-13a y R-13b) y 85 lotes de cambios a
  `data/`;
- 222 commits en la rama `dq/ronda-4`, integrados en `main` por hitos.

Producción solo cambió por un cambio atómico de alias, después de medir en staging y con aprobación explícita (H-2 y
H-3).

**El resultado** (antes = análisis del 12-nov sobre producción; después = M3/M4):

| Indicador | Antes | Después |
|---|---|---|
| Sinopsis con 2 o más firmas de texto generado | 1 408 | 0 |
| Respuestas de asistente y correcciones de año inventadas | 2 + 6 | 0 |
| Sinopsis que abren con la ficha | 3 734 | 9 |
| Sinopsis con taquilla o crítica | 1 549 | 5 |
| Sinopsis sin origen declarado (`enrichment.plot`) | 5 275 | 0 |
| Textos con espacios sobrantes, NBSP, sin NFC o con `&amp;` | 124 | 0 (y 238 campos con espacios dobles en WP-11) |
| Búsqueda por defecto de la app (`rrf_title_knn256`, nDCG@10) | 0.348 | 0.425 |
| Búsqueda vectorial `knn_plot_512` (nDCG@10) | 0.651 | 0.708 |

**Origen de las 6 899 sinopsis al final:**

| Origen | Películas |
|---|---|
| Wikipedia (`wikipedia_plot_2026-11-12` y `wikipedia_plot_2026-11-13`) | 5 403 (3 635 + 1 768) |
| Overview de TMDB (`tmdb_overview`) | 1 065 |
| Sin verificar (`unverified`) | 429 |
| Otros (`tmdb_metadata_summary`, `tmdb_overview_translated:zh-CN`) | 2 |

La atribución de cada sinopsis de Wikipedia (artículo, `revid` y fecha), que pide la licencia CC BY-SA, está en
[`fuentes/plot-fuentes.tsv`](fuentes/plot-fuentes.tsv).

---

## 2. Contexto que hay que entender antes de tocar el índice

| Hecho | Por qué importa |
|---|---|
| El índice vivo es idéntico, campo por campo, a `data/elastiflix-reloaded_0{1..7}.json` y `.csv` (6 899 películas) | **Toda corrección se hace en `data/`** y llega al índice al cargar con la app. Nunca se escribe directo en Elasticsearch |
| Cada parte existe en `.json` y `.csv` con el mismo contenido | Se escriben juntas con `catalogo.py`; nunca a mano |
| `ShippedCatalogValidationTest` exige que las 14 partes carguen sin errores ni advertencias | Si una regla nueva de ingesta avisa sobre `data/`, primero se limpia `data/` y después se integra la regla |
| Los vectores `plot_256/512/1024` se generan desde `plot` al cargar, con el servicio de inferencia del proyecto | Cambiar `plot` cambia la búsqueda vectorial: hay que volver a medir (M1 a M4) |
| La demo pública y el `.env` local apuntan al alias de producción `elastiflix-reloaded` | Toda instancia de prueba lleva `ELASTIFLIX_INDEX_ALIAS=elastiflix-dq-staging` y `SERVER_PORT=8081` explícitos |
| Una carga bulk de la app borra los índices `<alias>-<AAAAMMDD-HHMMSS>` que no son el vigente (`OrphanCleaner`) | Producción no se recarga por partes: se publica cambiando el alias, y la vuelta atrás depende de no hacer una carga bulk durante 7 días |
| Un push a `main` dispara Cloud Build y despliega la demo | Se trabajó solo en local (P-04) y hubo un único push al final (D-12) |
| La app que corre la dueña del proyecto lee plantillas de `target/classes` del checkout principal | Los agentes trabajan en un worktree aparte (`E:\02 - programming\03 - elastiflix-reloaded-dq`). Nunca `./mvnw clean` ni borrar `target/` |
| La ronda 3 había cambiado 1 641 sinopsis por el overview de TMDB, y `plot_1024` bajó de nDCG@10 0.817 a 0.742 | El overview es corto: volver a la trama real recupera precisión vectorial |

---

## 3. Qué se hizo, paquete por paquete

El plan tradujo las 10 recomendaciones del [informe inicial](informe/informe-calidad.html) en paquetes de trabajo
(WP). El [informe v2](informe/informe-calidad-v2.html) muestra el antes y el después de cada hallazgo.

| WP | Qué resolvía | Qué se hizo | Resultado |
|---|---|---|---|
| WP-00 | Preparación | Herramientas (`catalogo.py`, `aplicar_cambios.py`, `wiki_plot.py`, `proponer_trama.py`, `cargar.py`, `publicar.py`, `es.py` con escritura bloqueada fuera de staging), línea base M0 y alias de staging | Base del resto |
| WP-01 | 2 respuestas de asistente y 11 casos de datos inventados | Trama de Wikipedia o, si no había, overview de TMDB | 0 casos |
| WP-02 | 1 402 ensayos generados | Trama de Wikipedia o overview de TMDB, en lotes de 100 | 0 sinopsis con dos firmas; **M1** |
| WP-03 | Reglas de ingesta que no avisaban de estos textos | `EFX-ING-025`, `026` y `028` ampliadas en Java, en su propia rama; se integraron cuando `data/` ya estaba limpio | Cero avisos sobre `data/` |
| WP-04 | No se sabía el origen de cada sinopsis | `enrichment.plot` en las 6 899: `wikipedia_plot_<fecha>`, `tmdb_overview` o `unverified` | 0 sin origen |
| WP-05 | 3 862 sinopsis enciclopédicas | 3 425 con trama de Wikipedia, 372 limpiadas quitando párrafos completos (`limpiar_enciclopedico.py`), 13 corregidas, 7 con overview de TMDB (D-06), 45 sin fuente | Al cerrar WP-05: ficha inicial 3 734 → 8 (9 en M3); taquilla o crítica 1 549 → 5 |
| WP-06 | 686 sinopsis de menos de 200 caracteres | 616 con una trama más larga; 82 sin fuente; 5 no eran más largas | **M2** |
| WP-07 | Espacios, NBSP, NFC, `&amp;` y vocabularios | Lote mecánico de 309 películas y la regla `EFX-ING-029`, que normaliza al cargar | 124 → 0 |
| WP-08 | Palabras clave con varios nombres y colecciones | 3 ids unificados; *Death Note Relight 2* en su colección; nota de los ids que TMDB duplica | 14 → 11, los legítimos de TMDB |
| WP-09 | Dos puntuaciones y montos imposibles | `user_score` rotulado (D-02, opción A); `budget` de *Inuyasha* tomado de la ficha de Wikipedia | Montos comprobados contra la ficha |
| WP-10 | Mapping permisivo y filtro `adult` | `"dynamic": "strict"` (D-03); el filtro `adult` se queda y se documenta (D-04) | Mapping estricto |
| C-01 | Cierre | **M3**, informe v2, umbrales nuevos, documentación, publicación con H-2 | Producción = M3 el 2026-11-13 |
| WP-11 | Pendientes después de publicar | Arreglo de seguridad en `OrphanCleaner`, presupuesto de *Money Plane*, espacios dobles, analizador `english` en `plot` y `overview`, nueva búsqueda en Wikipedia (0 fuentes nuevas), **M4** y publicación con H-3 | Producción = M4 |

### El ciclo de un cambio de datos

Todos los WP de datos siguieron el mismo ciclo. Es lo más reutilizable de la ronda:

1. **Proponer.** Un script escribe `cambios/WP-XX/lote-NNN.jsonl` (100 películas como máximo) y una fila por película
   en `estado.tsv`: `propuesto`, `sin fuente` u `omitido`, con su motivo. No toca `data/`.
2. **Aplicar.** `aplicar_cambios.py` escribe JSON y CSV juntos. Es idempotente y **rechaza el lote entero si un valor
   «antes» ya no coincide**.
3. **Validar.** `ShippedCatalogValidationTest` en verde y el indicador del WP con `analizar.py --desde-data`.
4. **Registrar.** Un commit por lote, con su `estado.tsv`, `data/` y el diario.
5. **Revisar.** El revisor toma 10 líneas al azar por lote y las compara con la fuente. Un error **no se edita en su
   lote**: se corrige con un lote de corrección nuevo, y la película queda `corregido`.

Reanudar es leer `estado.tsv`: se sigue con las películas que todavía no tienen fila.

---

## 4. Aprendizajes sobre los datos

### 4.1 Wikipedia es la mejor fuente, pero hay que defenderse de ella

Encontrar el artículo por `imdb_id` → Wikidata (P345) → `enwiki` es mucho más seguro que buscar por título. Aun así, las
revisiones encontraron **tramas de otra obra** una y otra vez. Cada vez se agregó una guarda a `wiki_plot.py` y un
lote de corrección:

| Trampa | Ejemplo real | Guarda |
|---|---|---|
| El artículo es de la serie, el videojuego o el manga | *Gundam I* recibió la trama de la serie de TV (R-05) | Se rechaza un artículo que no es de una película |
| El sitelink apunta a otra película de la saga | *Jackass 3.5* → *Jackass 3D* (R-05) | Se compara el título y el año |
| El artículo cubre una obra en dos partes | *Red Cliff* y *Red Cliff Part II* con la misma trama (B-03, R-10) | Se descartan las obras en dos partes, aunque «two-part» lleve adjetivos antes (R-12) |
| El artículo cubre una trilogía o saga | *Berserk: The Golden Age Arc I* recibió la de la trilogía completa (R-11) | Se descarta «is a … trilogy / duology / film series» |
| Wikidata apunta a una **redirección a una sección** de otro artículo | *Jarhead 2* → «Jarhead (film)#Sequels»; *Sundays at Tiffany's* → la novela (R-11) | Se rechaza la redirección con `tofragment`: la película no tiene artículo propio |
| La búsqueda por título acepta un artículo parecido | Títulos que no se contienen uno al otro (R-11, R-12) | Todas las palabras del título de la película (sin artículos ni paréntesis) deben estar en el título del artículo |
| La redirección lleva a una película de otro año | *Prancer* (R-13a) | Se rechaza si el año del destino no cuadra |
| El mismo artículo sirve para dos películas | Detectado en la revisión | `proponer_trama.py` no repite un artículo ya usado |

**Lección:** cada guarda salió de un error real que encontró el muestreo de la revisión. Sin la revisión por
muestreo, esas tramas equivocadas estarían hoy en producción.

### 4.2 Otras lecciones de datos

- **Las secciones de trama tienen muchos nombres.** Además de `Plot`, también `Synopsis`, `Plot summary`, `Story`,
  `Premise` y `Summary`, y a veces con paréntesis («Synopsis (extended version)»). Los encabezados se comparan sin
  paréntesis (P-08).
- **El largo importa.** Se fijó un tope de 3 000 caracteres, con corte en el último fin de oración (D-01).
- **Limpiar es quitar párrafos completos, nunca reescribir.** `limpiar_enciclopedico.py` quita párrafos de ficha,
  producción o recepción. Las 372 sinopsis que no tenían trama en Wikipedia quedaron limpias sin que nadie redactara.
- **El overview de TMDB es el último recurso.** Es de la película, pero corto. Solo se usó donde el texto actual no
  servía (WP-01, WP-02 y D-06).
- **Normalizar al cargar, no solo en los datos.** WP-07 limpió `data/` y además agregó `EFX-ING-029` para que la
  carga normalice espacios, NBSP y NFC. En WP-11 se amplió para juntar espacios repetidos sin tocar los saltos de
  línea entre párrafos.
- **Cuidado con los signos combinantes.** Un paso de normalización dejó «ಕನ್ನಡ» (kannada) sin su virama en 2
  películas (R-15). Ahora hay una prueba de signos combinantes de otras escrituras.
- **Los montos se comprueban contra la ficha de Wikipedia citada por revisión** (`oldid`), no contra la memoria de
  un modelo:
  - *Money Plane* tenía `revenue` 618, y es correcto: la ficha dice «$618».
  - Su `budget` estaba mal: 568 443 en lugar de 586 443 (WP-11).
- **Los ids de palabra clave duplicados pueden ser legítimos.** TMDB tiene ids distintos con el mismo nombre. Solo se
  unificaron los que eran errores; el resto quedó documentado en `data/README.md`.
- **El mapping estricto tiene consecuencias de migración.** Con `dynamic: strict`, copiar un índice viejo que trae un
  campo no declarado falla con `EFX-ES-006` (R-18). Está documentado en el paso 9 del README.
- **El sobre de `data/*.json` también lleva un mapping.** Estaba desactualizado. Ahora lo escribe `catalogo.py` desde
  el archivo del repo (R-20).

---

## 5. Aprendizajes sobre la medición

- **El harness de relevancia puede dar un verde falso.** `RelevanceIT` se **omite** si el alias no tiene exactamente
  6 899 películas, y el build sale en verde. Una medición vale solo si `target/eval/report.md` se reescribió en esa
  corrida y su segunda línea nombra el índice esperado.
- **Medir en staging antes de publicar.** Cada recarga completa creó un índice `elastiflix-dq-staging-<fecha>` bajo el
  alias de staging (H-1), con los vectores regenerados por inferencia. Hubo cinco mediciones: M0 (producción
  original), M1, M2, M3 y M4 (staging). Después de cada publicación se volvió a medir producción para confirmar que
  quedó igual a la medición de staging (±0.02).
- **Una caída de métricas no siempre es un daño (B-06, D-08).** En M2, `bm25_plot` cayó de 0.408 a 0.296 y
  `rrf_plot_knn1024` de 0.675 a 0.558. La causa (R-14): **las consultas de evaluación se escribieron con el vocabulario
  de las sinopsis generadas** que el plan quitó, así que la búsqueda léxica sobre `plot` premiaba el texto falso. En
  la misma medición, los vectores (`knn_*`, +0.019 a +0.082) y la búsqueda por defecto de la app
  (`rrf_title_knn256`, +0.019) mejoraron. La dueña del proyecto decidió seguir. Desde entonces, el criterio de parada
  vigila `knn_*` y `rrf_title_knn*`, y `bm25_plot` y `rrf_plot_*` solo se informan.
- **Las raíces ayudan poco a lo léxico.** El analizador `english` en `plot` (D-11) subió `bm25_plot` de 0.296 a 0.305 y
  `rrf_plot_knn1024` de 0.557 a 0.564. No cambió los vectores ni la búsqueda por defecto.
- **Regla de los umbrales.** El mínimo de `eval/thresholds.json` es la medición menos 0.05, redondeado hacia abajo a 2
  decimales. Hay que recalcularlo después de cada medición aceptada.
- **Los conteos de la interfaz son parte de la medición.** El cambio de analizador cambió los conteos de las búsquedas
  OR sugeridas en la página de Inicio («ogre donkey princess» 197, «bathhouse spirits» 363, «clownfish ocean son»
  1 528). Están fijados en `LexicalSuggestions`, `FullTextSearchIT` y `HomePageTest`.
- **`analizar.py` es la vara de medir de los datos.** Corre igual sobre `data/` (`--desde-data`, sin red) que sobre un
  índice. Cada medición queda en `hallazgos/salidas/<etiqueta>/`, y `m0` está congelada.

**Métricas finales (nDCG@10).** El detalle con MRR está en `BITACORA.md` §6.

| Configuración | M0 | M4 = producción |
|---|---|---|
| `knn_plot_256` | 0.549 | 0.622 |
| `knn_plot_512` | 0.651 | 0.708 |
| `knn_plot_1024` | 0.742 | 0.745 |
| `rrf_title_knn256` (por defecto) | 0.348 | 0.425 |
| `rrf_title_knn1024` | 0.464 | 0.486 |
| `bm25_plot` | 0.423 | 0.305 |
| `rrf_plot_knn1024` | 0.662 | 0.564 |
| `bm25_title` · `bm25_multi_match` | 0.113 · 0.115 | sin cambio |

---

## 6. Aprendizajes sobre Elasticsearch Serverless y la publicación

- **Publicar es cambiar un alias, no cargar.** `publicar.py`:
  - comprueba el índice de staging: 6 899 películas, mapping fiel y los tres vectores en todas;
  - exige H-2 aprobada;
  - mueve `elastiflix-reloaded` al índice de staging en una sola operación atómica.
- **La vuelta atrás es otro cambio de alias:**
  `publicar.py --revertir <índice anterior>`.
- **`publicar.py` quita el alias de staging del índice que publica.** Así, staging y producción nunca comparten
  índice. R-21 dijo lo contrario por error, y R-22 lo corrigió.
- **El riesgo más serio lo encontró una relectura del código, no una prueba (WP-11 T-00).**
  - Qué podía pasar: `listOwnedIndexes` listaba como huérfano cualquier índice cuyo nombre siguiera el patrón del
    alias, aunque otro alias lo usara. Una recarga de staging habría borrado el índice que servía producción.
  - El arreglo: deja fuera todo índice que tenga un alias distinto.
  - La comprobación: una prueba de integración nueva, y una prueba en vivo en la que el índice publicado siguió en pie
    durante la recarga.
- **El índice vigente tiene nombre de staging** (`elastiflix-dq-staging-20261113-220031`). No sigue el patrón
  `elastiflix-reloaded-<fecha>`, así que `OrphanCleaner` nunca lo limpia solo: hay que borrarlo a mano cuando deje de
  usarse.
- **Siete días sin cargas bulk.** Mientras se guarda el índice anterior para volver atrás, una carga bulk lo borraría.
  Está escrito en `data/README.md` y en «Próximo paso».
- **La inferencia puede fallar a medias.** En la recarga de M4, la parte 05 falló con `EFX-EMB-001` (error del servicio
  de inferencia). Se volvió a intentar en modo append y funcionó. Una recarga completa debe comprobar al final las
  6 899 películas y los vectores, no solo el código de salida.
- **`es.py` bloquea la escritura fuera de `elastiflix-dq-staging*`.** Ningún script de análisis puede dañar
  producción por error.

---

## 7. Aprendizajes sobre el equipo de agentes

### 7.1 El protocolo que funcionó

- **Turnos en serie y una revisión después de cada uno.** Nunca hubo dos agentes cambiando `data/` a la vez. La
  revisión se hace «con el contexto limpio»: solo con lo que el turno dejó en git y en la bitácora, nunca con lo que el
  ejecutor dijo.
- **Los archivos son el estado, no la conversación.**
  - `BITACORA.md` es el tablero, con su sección «Próximo paso».
  - `PLAN.md` es la especificación y no se marca.
  - Cada WP tiene un diario con su «punto de reanudación».
  - Cada turno cierra con la etiqueta `dq-t<NN>` y cada revisión con `dq-r<NN>`.
- **«Próximo paso» con comandos literales.** El siguiente agente, cualquiera que sea, sabe exactamente qué hacer.
- **Las decisiones son de la persona, con su texto literal.** Una aprobación pasa a `APROBADA` solo con la respuesta
  literal de la dueña del proyecto (por ejemplo, «Apruebo H-2»). Ningún agente se aprueba solo.
- **Los cuatro ojos se mantienen aunque el revisor ejecute.** Cuando Claude Code tomó un turno, la revisión se apoyó
  en otra cosa (P-21):
  - en el turno 18, en un agente nuevo que leyó el diff contra la tarjeta sin el contexto del turno;
  - en los turnos 21 y 22, en comprobaciones objetivas: pruebas, mediciones y el estado del clúster.
- **Integración por hitos.** `main` solo recibió hitos revisados (A, B, C y las publicaciones), siempre con
  `git merge --no-ff` y sin push hasta el final (D-12).

### 7.2 Lo que aprendimos de cada herramienta

| Ejecutor | Turnos | Lo que hay que saber |
|---|---|---|
| Google Antigravity | 1, 4, 7, 10 y 17 (con Gemini 4 Argon) | Cumplió. En el turno 10 se detuvo bien ante un bloqueo (B-03) |
| Cursor | 2, 5, 8, 11, 13b, 14 a 16 y 19 a 21 | El más constante. Se detiene donde el plan lo manda (B-06 en el turno 14; H-2 en el 21) |
| GitHub Copilot (Kimi K3) | 3, 6 y 9 | Cumplió con correcciones menores |
| Gemma 4 E4B (OpenCode) | 12 y 13a | **No commiteó ninguno de sus dos turnos**; los cerraron las revisiones. Ver §7.3 |
| Claude Code | Revisiones R-00 a R-22; ejecutó los turnos 18, 21 (parte) y 22 | Revisa, corrige con lotes de corrección y escribe el siguiente «Próximo paso» |

Qwen 2.5 Coder quedó fuera del equipo desde el principio (P-06). Antigravity y Copilot salieron después del turno 11
(P-13).

### 7.3 Modelos pequeños (Gemma 4 E4B, unos 4 000 millones de parámetros efectivos)

Lo que pasó:
- **B-04:** OpenCode corta los comandos a los 2 minutos por defecto, y `proponer_trama.py` tarda más. El lote nunca
  se escribió, y el paso siguiente falló.
- **B-05:** informó un fallo de `./mvnw verify` que no existía. Leyó como fallos las líneas `ERROR` que escriben a
  propósito las pruebas de manejo de errores.
- Dejó lotes propuestos sin aplicar, sin diario y sin commit.

Lo que se le agregó en `AGENTS.md` §5.1 (útil para cualquier modelo pequeño):
- leer poco y en un orden fijo;
- copiar los comandos tal cual;
- un lote a la vez, con commit después de cada uno;
- timeout de 600 000 ms en todo comando;
- cómo leer la salida de Maven: pasó si el código de salida es 0;
- no editar la bitácora desde la terminal (en PowerShell, `>>` y `Out-File` escriben UTF-16);
- no delegar en subagentes;
- cerrar limpio si se le llena el contexto.

**La decisión que lo cerró (D-07):** los turnos que quedaban incluían integrar Java, recargar staging y publicar en
producción. Pasaron a Cursor, y Claude Code terminó WP-05.

**Lección:** un modelo pequeño puede ejecutar tareas mecánicas con comandos literales, pero no conviene darle turnos
que terminan en un cambio irreversible ni turnos sin un punto de control por lote.

### 7.4 Trampas de la terminal en Windows

- **PowerShell expande `$`.** Al pasar «$618» como argumento entre comillas dobles se pierde el texto (R-17). Con `$`
  se usan comillas simples.
- **PowerShell escribe UTF-16** con `>`, `>>`, `Out-File` y `Add-Content`, y rompe archivos UTF-8. Para editar texto
  se usa la herramienta de edición, no la terminal.
- **Un proceso de Java puede sobrevivir a la orden de detener la tarea** que lo lanzó. Hay que comprobar el puerto y
  cerrar el proceso por su PID.
- **Escapes en heredocs de Python.** Un `\u00a0` dentro de un heredoc de bash no llega como se escribió. Para cambios
  exactos se usa la herramienta de edición.

---

## 8. Errores propios del revisor (y cómo se evitan)

Claude Code también se equivocó. Quedan anotados para que nadie los repita:

| Error | Consecuencia | Cómo se evita |
|---|---|---|
| Un recorte de texto en `BITACORA.md` borró filas | Se restauró con `git checkout` y se rehízo | Cambios con reemplazos exactos y `assert` de una sola coincidencia; revisar `git diff --stat` antes del commit |
| Un reemplazo global (Qwen → Gemma) cambió una línea histórica sobre «Qwen 2.5 Coder» | Se restauró la línea | Nunca un reemplazo global sobre un registro histórico |
| El commit de T-01 de WP-11 llevaba también los datos de otro lote | `git reset --soft` y los lotes se aplicaron por separado | `git status` antes de cada commit: solo los archivos de ese lote |
| R-21 dijo que el alias de staging apuntaba a producción | Advertencia falsa en la documentación; se tachó en R-21 y se corrigió en R-22 | Comprobar el estado del clúster (`GET /_alias/...`) antes de escribir un riesgo |
| Finales de línea mezclados (CRLF y LF) en la bitácora | Diffs ruidosos | Escribir con `newline=''` y conservar el estilo del archivo |
| Dos líneas de más de 120 caracteres rompieron Checkstyle | Build en rojo hasta corregirlas | `./mvnw -q verify` antes del commit, no después |

---

## 9. Estado final (2026-11-13)

- **Producción:** `elastiflix-reloaded` → `elastiflix-dq-staging-20261113-220031` (M4, 6 899 películas, `plot` y
  `overview` con analizador `english`, mapping `dynamic: strict`).
- **Código y datos:** `dq/ronda-4` integrada en `main`, y `main` publicado en origin (`354e16d`).
- **Vuelta atrás disponible hasta el 2026-11-20:** `elastiflix-dq-staging-20261113-195801` (M3) y
  `elastiflix-reloaded-20261112-144129` (el original).
- **Pendiente (C-01 T-08), desde el 2026-11-20:**
  - Comprobar que no haya incidentes.
  - Borrar los dos índices anteriores con `publicar.py --borrar-anterior`, solo con la confirmación de la dueña del
    proyecto.
  - Revisión final R-21b, con la retrospectiva del equipo.

  Hay un recordatorio programado (`elastiflix-t08-borrar-indice-anterior`) para ese día a las 09:00 (hora de la dueña
  del proyecto, UTC−6).

---

## 10. Lo que queda abierto para una ronda 5

| Tema | Situación | Sugerencia |
|---|---|---|
| 429 sinopsis `unverified` | No tienen trama en Wikipedia; la nueva búsqueda de WP-11 no encontró fuentes nuevas | Probar otras fuentes con licencia compatible (Wikipedia en otros idiomas solo si se acepta citar el original, sin traducir: NS-2) o dejarlas así, marcadas |
| 82 + 45 sinopsis «sin fuente» (WP-06 y WP-05) | Siguen con su texto anterior | Igual que el punto anterior |
| Conjunto de evaluación sesgado | Las consultas usan el vocabulario de las sinopsis generadas (B-06); 592 películas no tienen juicio | Reescribir las consultas desde la trama real y calificar las 592 antes de volver a usar `bm25_plot` como criterio |
| Nombre del índice vigente | `elastiflix-dq-staging-…` sirve producción y `OrphanCleaner` no lo limpia | En la próxima carga bulk de producción, volver al patrón `elastiflix-reloaded-<fecha>` y borrar el de staging a mano |
| Puntuaciones | Se mantienen las dos, rotuladas (D-02, opción A) | Revisar si la demo debe mostrar solo una |
| Origen de 2 sinopsis atípicas | `tmdb_metadata_summary` y `tmdb_overview_translated:zh-CN` | Decidir si se aceptan como etiquetas o se reemplazan |

---

## 11. Impacto en otra documentación del proyecto

Lugares que este trabajo toca o que conviene ajustar:

| Documento | Qué cambió o qué hay que hacer |
|---|---|
| `README.md` y `README.en.md` | Ya al día: tabla de relevancia en M4, `EFX-ING-029` («espacios en los extremos o repetidos»), migración del paso 9 con mapping estricto, créditos de Wikipedia (línea de `wikipedia_plot_AAAA-MM-DD`) |
| `data/README.md` (es/en) | La sección «Índice vigente en producción» habla de la vuelta atrás **hasta el 2026-11-20**. Después de T-08 hay que quitar el párrafo de los índices anteriores y la advertencia de cargas bulk |
| `eval/README.md` y `eval/thresholds.json` | Al día con M4. Si cambia el conjunto de evaluación (§10), hay que volver a medir y recalcular los umbrales |
| `docs/convenciones-java.md` | Al día con `EFX-ING-029` |
| `elasticsearch/elastiflix-reloaded-mapping.json` | `dynamic: strict` y analizador `english` en `plot` y `overview`. Un campo nuevo exige actualizar el mapping, `IndexMappingTest` y el sobre de `data/*.json` (`catalogo.py`) |
| `docs/arquitectura.mermaid` · `docs/despliegue.mermaid` | No muestran el flujo staging → publicación por alias ni la limpieza de huérfanos. Conviene agregarlo si se documenta la operación |
| `data-quality/README.md` | Todavía describe un equipo activo («Abre la herramienta que indica Próximo paso…»). Después de R-21b conviene marcarlo como ronda cerrada y apuntar a este documento |
| `data-quality/BITACORA.md` | «Próximo paso» es T-08. Después de R-21b, turno 21 y C-01 a `HECHA` |
| Memoria de Claude Code del proyecto | La nota «data-quality solo en local» ya no aplica: `dq/ronda-4` está en `main` y en origin |

---

## 12. Dónde buscar cada cosa

| Necesito… | Archivo |
|---|---|
| El diagnóstico original | `informe/informe-calidad.html` y `hallazgos/hallazgos.csv` (2 514 hallazgos en 2 401 películas) |
| El antes y el después por hallazgo | `informe/informe-calidad-v2.html` |
| Qué se decidió, quién y con qué palabras | `BITACORA.md` §3 y §4 |
| Las métricas M0 a M4 | `BITACORA.md` §6 y `hallazgos/salidas/<m0…m4>/` |
| Por qué cambió una sinopsis concreta | `cambios/WP-XX/estado.tsv` (buscar el `id`) y el lote que la cambió |
| De qué artículo de Wikipedia salió una sinopsis | `fuentes/plot-fuentes.tsv` |
| Qué encontró cada revisión | `bitacora/R-NN.md` |
| Cómo correr el análisis otra vez | `README.md` de esta carpeta, «Volver a correr el análisis» |
| Cómo publicar o volver atrás | `scripts/publicar.py --help` y `BITACORA.md` §2 |

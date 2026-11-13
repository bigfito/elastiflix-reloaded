# 📒 Bitácora · calidad del catálogo, ronda 4

Tablero del plan. Vive en la rama `dq/ronda-4`; reglas de uso en [`AGENTS.md` §5 y §6](AGENTS.md#5-turno-de-un-ejecutor).
Fechas en UTC, ISO 8601. Los diarios de cada WP y los informes de revisión están en [`bitacora/`](bitacora/).

**Última actualización:** 2026-11-13 04:55 UTC · Cursor (Grok 4.7) · ejecutor (turno 13b)
**Equipo (desde el turno 13b, D-07):** ejecuta Cursor; revisa y corrige Claude Code; decide la dueña del proyecto. Antes ejecutaron Google Antigravity y GitHub Copilot (turnos 1–11) y Gemma 4 E4B (12 y 13a)
**Orden de turnos:** Cursor 13b → Claude Code → Cursor 14 → Claude Code → … → Cursor 21 → Claude Code (`PLAN.md` · Reparto y turnos)
**Dónde se trabaja:** worktree `E:\02 - programming\03 - elastiflix-reloaded-dq`, rama `dq/ronda-4`, solo en local (P-04)
**Staging:** alias `elastiflix-dq-staging` · índice `elastiflix-dq-staging-20261112-201036` (6 899 películas) · `data/` cargado en el commit `220a8ea` · app compilada en el commit `220a8ea` · instancia apagada (puerto 8081)

## 1. Tablero

### 1.1 Turnos

Estados: `PENDIENTE` → `EN CURSO` → `EN REVISIÓN` (o `INCOMPLETO`) → `REVISADO`, o `EN ESPERA` en el turno 21.
Etiquetas: `dq-t<NN>` al cerrar el turno y `dq-r<NN>` al cerrar su revisión. `dq-r00` marca el punto de partida.

| Turno | Ejecutor | Tareas (detalle en el plan) | Estado | Inicio | Etiqueta `dq-t` | Revisión |
|---|---|---|---|---|---|---|
| 01 | Google Antigravity | WP-00 T-01 a T-04 | REVISADO | 2026-11-12 | `dq-t01` | [R-01](bitacora/R-01.md) |
| 02 | Cursor | WP-00 T-05 y T-09 | REVISADO | 2026-11-12 | `dq-t02` | [R-02](bitacora/R-02.md) |
| 03 | GitHub Copilot | WP-00 T-06, T-07, T-08 y T-10 | REVISADO | 2026-11-12 | `dq-t03` | [R-03](bitacora/R-03.md) |
| 04 | Google Antigravity | WP-01 y WP-02 `lote-001` | REVISADO | 2026-11-12 | `dq-t04` | [R-04](bitacora/R-04.md) |
| 05 | Cursor | WP-02 lotes `002`–`006` | REVISADO | 2026-11-12 | `dq-t05` | [R-05](bitacora/R-05.md) |
| 06 | GitHub Copilot | WP-02 lotes `009`–`013` (500 desde `68727`; P-07) | REVISADO | 2026-11-12 | `dq-t06` | [R-06](bitacora/R-06.md) |
| 07 | Google Antigravity | WP-02, las 302 restantes (lotes `015`–`018`; P-07), T-04 a T-06 (M1) · hito A | REVISADO | 2026-11-12 | `dq-t07` | [R-07](bitacora/R-07.md) |
| 08 | Cursor | WP-03 T-01, T-02 y T-04 | REVISADO | 2026-11-12 | `dq-t08` | [R-08](bitacora/R-08.md) |
| 09 | GitHub Copilot | WP-04 | REVISADO | 2026-11-12 | `dq-t09` | [R-09](bitacora/R-09.md) |
| 10 | Google Antigravity | WP-05 T-01 y lotes `001`–`009` (se detuvo por B-03) | REVISADO | 2026-11-12 | `dq-t10` | [R-10](bitacora/R-10.md) |
| 11 | Cursor | WP-05: lotes `011`–`025` y limpieza de todas las omitidas (`026`–`027`; P-11) | REVISADO | 2026-11-12 | `dq-t11` | [R-11](bitacora/R-11.md) |
| 12 | Gemma 4 E4B | WP-05: lotes `029`–`040` (10 de Wikipedia y 2 de limpieza) y T-03; sin cerrar, lo cerró R-12 | REVISADO | 2026-11-13 | `dq-t12` | [R-12](bitacora/R-12.md) |
| 13a | Gemma 4 E4B | Cierre de WP-05: evaluó las 462 restantes (lotes `042`–`047`) sin commits; R-13a terminó WP-05 (D-07) | REVISADO | 2026-11-13 | `dq-t13a` | [R-13a](bitacora/R-13a.md) |
| 13b | Cursor | WP-03 T-03 (integración) y WP-06 T-01 y T-02 (D-07) | EN REVISIÓN | 2026-11-13 | `dq-t13b` | — |
| 14 | Cursor | WP-06 T-03 (M2) y WP-07 parte A · hito B | PENDIENTE | — | — | — |
| 15 | Cursor | WP-07 parte B (P-14) | PENDIENTE | — | — | — |
| 16 | Cursor | WP-08 (D-07) | PENDIENTE | — | — | — |
| 17 | Cursor | WP-09 | PENDIENTE | — | — | — |
| 18 | Cursor | WP-10 (P-14) | PENDIENTE | — | — | — |
| 19 | Cursor | C-01 T-01 a T-03 (M3, informe v2, pruebas y umbrales; D-07) | PENDIENTE | — | — | — |
| 20 | Cursor | C-01 T-04 y T-05 · hito C | PENDIENTE | — | — | — |
| 21 | Cursor | C-01 T-06 a T-08 (H-2; D-07) | PENDIENTE | — | — | — |

### 1.2 Paquetes de trabajo

| WP | Título | Estado | Turnos | Actualizado |
|---|---|---|---|---|
| WP-00 | Preparación del equipo | HECHA | 1, 2, 3 | 2026-11-12 |
| WP-01 | Respuestas de asistente y datos inventados | HECHA | 4 | 2026-11-12 |
| WP-02 | Reescribir los ensayos generados | HECHA | 4, 5, 6, 7 | 2026-11-12 |
| WP-03 | Ampliar las reglas de advertencia (rama `dq/wp-03-reglas-plot`) | EN CURSO · T-03 integrada en el turno 13b (`4f14c65`, `lote-051`); pendiente de R-13b | 8, 13b | 2026-11-13 |
| WP-04 | Etiquetar el origen de todas las sinopsis | HECHA | 9 | 2026-11-12 |
| WP-05 | Limpiar el plot enciclopédico | HECHA · 3 862/3 862: 3 425 Wikipedia, 7 overview de TMDB (D-06), 13 corregidas, 372 limpias, 45 sin fuente; ficha inicial 8 y taquilla o crítica 5 | 10, 11, 12, 13a | 2026-11-13 |
| WP-06 | Completar las sinopsis cortas | EN CURSO · T-01 y T-02 hechas (616 Wikipedia, 65 sin fuente, 5 no más largas); T-03 en el turno 14 | 13b, 14 | 2026-11-13 |
| WP-07 | Normalizar al cargar | PENDIENTE | 14, 15 | — |
| WP-08 | Corregir palabras clave y colecciones | PENDIENTE | 16 | — |
| WP-09 | Una sola puntuación y montos imposibles | PENDIENTE | 17 | — |
| WP-10 | Opcional: `dynamic: strict` y filtro `adult` | PENDIENTE | 18 | — |
| C-01 | Cierre: medición, publicación y documentación | PENDIENTE | 19, 20, 21 | — |

### 1.3 Revisiones de Claude Code

| Revisión | Turno revisado | Veredicto | Hallazgos corregidos | Commits de corrección | Informe |
|---|---|---|---|---|---|
| R-00 | (plan inicial) | APROBADO | 23 de la revisión independiente del plan | `ebab666` y siguiente | Diario del plan, §8 |
| R-01 | 01 · Google Antigravity | APROBADO CON CORRECCIONES | 3 defectos de validación en `catalogo.py` y `aplicar_cambios.py`; trazabilidad de correcciones en `estado.tsv` | `fb06ad8` | [R-01](bitacora/R-01.md) |
| R-02 | 02 · Cursor | APROBADO CON CORRECCIONES | Límite de Wikimedia (429), subtítulos dentro de la trama, comilla final, `--agente` por defecto | `83af348` | [R-02](bitacora/R-02.md) |
| R-03 | 03 · GitHub Copilot | APROBADO CON CORRECCIONES | Guardas de borrado de `publicar.py` (alias y comodines); comando del harness sobre staging | `0ab67e4` | [R-03](bitacora/R-03.md) |
| R-04 | 04 · Google Antigravity | APROBADO | Ninguno que corregir: 113 tramas correctas; T-02 de WP-01 cerrada | — | [R-04](bitacora/R-04.md) |
| R-05 | 05 · Cursor | APROBADO CON CORRECCIONES | 2 tramas de otra obra (*Gundam I* → serie de TV; *Jackass 3.5* → *Jackass 3D*), guarda nueva en `wiki_plot.py` | `4ea1d8d` `9c0f2ff` `70c638b` | [R-05](bitacora/R-05.md) |
| R-06 | 06 · GitHub Copilot | APROBADO CON CORRECCIONES | B-01 (regla `EFX-ING-026` sobre citas), segundo aviso oculto, 4 tramas recuperadas con encabezados «Summary» y con paréntesis | `73cd5d0` `a6369d2` `e9dac1a` | [R-06](bitacora/R-06.md) |
| R-07 | 07 · Google Antigravity | APROBADO | Guarda de artículos de varias películas hecha explícita; cierre de WP-02 y del hito A | `be0f349` | [R-07](bitacora/R-07.md) |
| R-08 | 08 · Cursor | APROBADO CON CORRECCIONES | B-02 era del plan (P-09); lista `wp05-avisos-wp03.tsv` y WP-05 T-03b; apóstrofo tipográfico en tres patrones | `df935f1` (rama WP-03) y `dq/ronda-4` | [R-08](bitacora/R-08.md) |
| R-09 | 09 · GitHub Copilot | APROBADO CON CORRECCIONES | Créditos de Wikipedia coherentes en los README; la prueba vigila `enrichment.plot` | `738511a` | [R-09](bitacora/R-09.md) |
| R-10 | 10 · Google Antigravity | APROBADO CON CORRECCIONES | B-03 (artículos de obras en dos partes) resuelto; prueba de `limpiar_enciclopedico.py`; guardas nuevas | `df0ce61` `7f0ff43` `0a67893` | [R-10](bitacora/R-10.md) |
| R-11 | 11 · Cursor | APROBADO CON CORRECCIONES | 7 tramas de otra obra (trilogías, redirecciones a secciones, búsqueda por título) y tres guardas nuevas en `wiki_plot.py`; D-06 abierta | `9a28321` `be73b85` `fdae033` | [R-11](bitacora/R-11.md) |
| R-12 | 12 · Gemma 4 E4B | APROBADO CON CORRECCIONES | Turno sin cerrar: commit de recuperación y 8 lotes aplicados; 3 tramas de otra obra («two-part», búsqueda por título); `--d06` para T-06; falso bloqueo B-05 | `1042632` `3f4c422` `afb20ad`…`e16b441` `a6d3a23` `62a1e39` | [R-12](bitacora/R-12.md) |
| R-13a | 13a · Gemma 4 E4B | APROBADO CON CORRECCIONES | Turno sin un commit: recuperación y 5 lotes aplicados; *Prancer* corregida; guarda nueva; con D-07, cierre de WP-05 (`lote-049` y `050`, T-05 y T-03) |  `3595bdd` `501a192` `4a06a94`…`17c4b44` `10f735d` | [R-13a](bitacora/R-13a.md) |

## 2. Próximo paso

> **Claude Code: revisión R-13b** del turno 13b (Cursor). Alcance: `git diff dq-r13a..dq-t13b`. WP-03 quedó integrada
> (`4f14c65`); el único aviso fue `616803` *Breaking Surface*, corregido con `lote-051`, y
> `ShippedCatalogValidationTest` quedó en verde. WP-06 T-01 y T-02: lotes `001`–`007` (616 tramas más largas, 65 sin
> fuente, 5 que no se reemplazan porque la trama de Wikipedia no es más larga). T-03 (M2) es el turno 14.

## 3. Aprobaciones y decisiones de la dueña del proyecto

Formato del estado: `ABIERTA`, `APROBADA AAAA-MM-DD · «texto literal»` o `RECHAZADA AAAA-MM-DD · «texto literal»`.
Solo la dueña del proyecto, o un agente copiando su respuesta literal, cambia esta columna.

| ID | Pregunta | Opciones | Recomendación | Estado |
|---|---|---|---|---|
| P-0 | Precondición: dónde vive el trabajo. Se planteó commitear `data-quality/` en `main` y publicar `dq/ronda-4` | En `main` y remoto · Solo en local | Lo que decida la dueña | APROBADA 2026-11-12 · «Vamos a trabajar con la copia de nuestro repositorio local.» · se aplicó como P-04 |
| H-1 | ¿Se puede crear y recargar el alias `elastiflix-dq-staging` en el proyecto Serverless (índices nuevos con vectores generados por inferencia; unas 4 recargas completas en todo el plan)? | Sí · No | Sí: permite medir sin tocar producción | APROBADA 2026-11-12 · «Autorizo H-1 y acepto las recomendaciones de D-01 a D-04» |
| D-01 | Largo máximo de `plot` tomado de Wikipedia | 3 000 caracteres · otro valor | 3 000, con corte en fin de oración (hoy p95 = 2 670) | APROBADA 2026-11-12 · «Autorizo H-1 y acepto las recomendaciones de D-01 a D-04» · rige 3 000 |
| D-02 | ¿Qué puntuación muestra la demo? (WP-09) | A · B · C del plan | A: mantener las dos y rotularlas | APROBADA 2026-11-12 · «Autorizo H-1 y acepto las recomendaciones de D-01 a D-04» · opción A; WP-09 T-01 no necesita memo |
| D-03 | ¿`"dynamic": "strict"` en el mapping? (WP-10) | Sí · No | Sí | APROBADA 2026-11-12 · «Autorizo H-1 y acepto las recomendaciones de D-01 a D-04» · sí, `dynamic: strict` |
| D-04 | ¿Quitar el filtro `adult`? (WP-10) | Sí · No | No; documentar | APROBADA 2026-11-12 · «Autorizo H-1 y acepto las recomendaciones de D-01 a D-04» · no se quita; se documenta |
| D-05 | ¿Qué contacto va en el `User-Agent` para Wikipedia y Wikidata (NS-7)? Sin contacto, Wikimedia limita a 10 peticiones por minuto y las unas 5 950 descargas de los turnos 4 a 13 tardan 11 h o más en total; con contacto, menos de 2 h. El valor va en `.env` como `DQ_WIKI_CONTACTO=` y no se commitea | URL pública del repo · un correo · sin contacto | Lo que prefiera la dueña del proyecto: es un dato suyo que se envía a Wikimedia | APROBADA 2026-11-12 · «La URL del repo» · `DQ_WIKI_CONTACTO=https://github.com/bigfito/elastiflix-reloaded` en el `.env` del worktree |
| D-06 | En WP-05, una sinopsis sin trama de Wikipedia que es **texto generado**, y que la limpieza no arregla, ¿toma el overview de TMDB que ya trae el documento (NS-1 b, etiqueta `tmdb_overview`)? Hoy son 5: `213927` *Born to Race: Fast Track* (la respuesta de un asistente que no encontró la película), `298016` *It's a SpongeBob Christmas!* (corrige el año e inventa la trama) y las 3 de T-03b (`150015`, `198436`, `364708`, ensayos «At its core»). Sin esto, esas 3 bloquean la integración de WP-03 (turno 13) | Sí, en el turno 13 · No, quedan `unverified` | Sí: el overview es corto pero es de la película, y nadie redacta (NS-2) | APROBADA 2026-11-13 · «Apruebo D-06» · WP-05 T-06 en el turno 13a |
| D-07 | Gemma 4 E4B dejó los turnos 12 y 13a sin commitear (lotes propuestos sin aplicar, sin diario ni cierre); los cerró la revisión. ¿Quién termina lo que falta de WP-05 (la limpieza de 70 omitidas, T-06, T-05 y T-03: comandos deterministas ya escritos) y quién toma los turnos 13b, 16, 19a, 19b y 21? | A · Claude Code termina WP-05 en R-13a y Cursor toma los turnos de Gemma · B · Gemma repite un turno 13a corto con lo que falta y conserva sus turnos · C · Claude Code termina WP-05 y Gemma conserva sus turnos | A: los turnos que quedan incluyen integrar Java (13b), recargar staging (19a) y publicar en producción (21), y Gemma no cerró ninguno de sus dos turnos | APROBADA 2026-11-13 · «WP-05 tú, resto a Cursor» · opción A |
| H-2 | ¿Se publica el catálogo nuevo cambiando `elastiflix-reloaded` al índice de staging (cambio atómico, con vuelta atrás), y se borra el índice anterior a los 7 días? (C-01) | Sí · No | Se decide con M3 | ABIERTA · se pregunta en C-01 T-06 |

## 4. Decisiones tomadas

| Fecha | ID | Decisión | Quién |
|---|---|---|---|
| 2026-11-12 | P-01 | El alcance de cada WP es el de `hallazgos/listas/` (congelado sobre el índice `elastiflix-reloaded-20261112-144129`) | COORD, al crear el plan |
| 2026-11-12 | P-02 | Las correcciones se hacen en `data/` y llegan al índice por la aplicación; producción solo cambia con `publicar.py` y H-2 | COORD, al crear el plan |
| 2026-11-12 | P-03 | Todo el trabajo y esta bitácora viven en `dq/ronda-4`, trabajada desde un worktree; a `main` solo llegan los hitos A, B y C | COORD, tras la revisión independiente del plan |
| 2026-11-12 | P-04 | Solo en local: worktree `E:\02 - programming\03 - elastiflix-reloaded-dq` en `dq/ronda-4`, creado desde `main` (`9a1b864`). Sin `push` ni `pull`; `main` y `origin` no se tocan. La dueña integra los hitos con `git merge --no-ff` y decide si publica | Dueña del proyecto (P-0) |
| 2026-11-12 | P-05 | Equipo: ejecutan Google Antigravity, Cursor y GitHub Copilot, 7 turnos cada uno en orden fijo; Claude Code revisa y corrige después de cada turno y no toma tareas del plan. Reparto en `PLAN.md` · Reparto y turnos | Dueña del proyecto |
| 2026-11-12 | P-06 | Qwen 2.5 Coder queda fuera del equipo de ejecutores; GitHub Copilot trabaja con el modelo Kimi K3 | Dueña del proyecto |
| 2026-11-12 | D-01 | Largo máximo de `plot` de Wikipedia: 3 000 caracteres, con corte en fin de oración | Dueña del proyecto |
| 2026-11-12 | D-02 | Opción A: se mantienen `vote_average` y `user_score` y se rotulan; WP-09 implementa sin memo | Dueña del proyecto |
| 2026-11-12 | D-03 | `"dynamic": "strict"` en `elasticsearch/elastiflix-reloaded-mapping.json` (WP-10) | Dueña del proyecto |
| 2026-11-12 | D-04 | El filtro `adult` se queda; se documenta que el catálogo de `data/` no tiene contenido adulto (WP-10) | Dueña del proyecto |
| 2026-11-12 | D-05 | Contacto del `User-Agent` para Wikimedia: la URL pública del repo. Va en `.env` como `DQ_WIKI_CONTACTO` (no se commitea); `wiki_plot.py` pasa a 1 petición por segundo | Dueña del proyecto |
| 2026-11-12 | P-07 | Los lotes de corrección de R-05 tomaron los números `007` y `008`. Los números de lote los asigna `proponer_trama.py`; las películas de cada turno no cambian (turno 06: 500 desde `68727`, lotes `009`–`013`; turno 07: las 302 restantes, `014`–`017`) | Claude Code (R-05) |
| 2026-11-12 | P-08 | NS-1 acepta `Summary` como sección de trama y compara los encabezados sin paréntesis («Synopsis (extended version)»). Con la prueba en rojo no se aplican más lotes (`AGENTS.md` §5) | Claude Code (R-06) |
| 2026-11-12 | P-09 | En la rama de WP-03, el criterio es `verify` en verde salvo `ShippedCatalogValidationTest`, que solo puede avisar sobre películas de `hallazgos/listas/wp05-avisos-wp03.tsv` (81). WP-05 suma T-03b: esas 81 quedan con trama de Wikipedia o limpias de la frase. Todo en verde se exige en WP-03 T-03 (turno 13) | Claude Code (R-08) |
| 2026-11-12 | P-10 | El hito A (`dq-r07`) se integró en `main` local con el merge `739a118`, sin push | Dueña del proyecto: «Sí, integra en local» |
| 2026-11-12 | P-11 | WP-05 se reparte por cantidad de lotes, no por número: turno 11, 15 lotes de 100 y la limpieza de todas las omitidas pendientes; turno 12, 14 lotes, su limpieza y T-03; turno 13, lo que quede (unas 60 películas), T-03b y T-05 antes de WP-03 T-03 | Claude Code (R-10) |
| 2026-11-13 | D-06 | En WP-05, la sinopsis sin trama de Wikipedia que es texto generado y que la limpieza no arregla toma el overview de TMDB del documento (NS-1 b, `tmdb_overview`). Lista en `hallazgos/listas/wp05-d06-generadas.tsv` (5 hoy; el turno 12 suma las suyas); la aplica WP-05 T-06 en el turno 13a | Dueña del proyecto: «Apruebo D-06» |
| 2026-11-13 | P-14 | Los turnos de Java, 15 (WP-07 parte B) y 18 (WP-10), pasan de Gemma 4 E4B a Cursor. Gemma queda con 12, 13a, 13b, 16, 19a, 19b y 21; Cursor, con 14, 15, 17, 18 y 20 | Dueña del proyecto: «Sí, pásalos a Cursor» |
| 2026-11-13 | P-17 | D-07, opción A: Claude Code terminó WP-05 en R-13a (limpieza `lote-049`, D-06 `lote-050`, T-05 y T-03) y Cursor hace todos los turnos que quedan (13b a 21). El turno 19 vuelve a ser uno solo. WP-05 pasa a HECHA | Dueña del proyecto: «WP-05 tú, resto a Cursor» |
| 2026-11-13 | P-16 | R-12 cerró el turno 12, que Gemma dejó sin commitear: commit de lo que dejó tal cual (`dq-t12`) y los 8 lotes propuestos aplicados por la revisión, uno por uno, tras simularlos y muestrearlos. Para el turno 13a, `proponer_trama.py --d06` hace WP-05 T-06 sin programar. WP-05 T-03 queda juzgado en R-12 salvo 10 películas que el turno 13a todavía evalúa | Claude Code (R-12) |
| 2026-11-13 | P-15 | Corrección de P-13 y P-14: el ejecutor que toma los turnos de Google Antigravity y GitHub Copilot es **Gemma 4 E4B** en OpenCode (versión de escritorio), no Qwen 3.5 9B. Todo lo de P-13 y P-14 vale igual para Gemma; firma `Gemma (Gemma 4 E4B)`. Como es aún más pequeño, `AGENTS.md` §5.1 le permite cerrar el turno después de cualquier lote commiteado | Dueña del proyecto: «Cometí un error, te dije que era un agente de Qwen 3.5 9B corriendo desde el harness de OpenCode quien iba a realizar algunas tareas cuando en realidad es un agente de Gemma-4-e4b también corriendo desde OpenCode.» |
| 2026-11-13 | P-13 | Google Antigravity y GitHub Copilot salen del equipo; sus turnos (12, 13, 15, 16, 18, 19 y 21) los toma Gemma 4 E4B. Para un modelo pequeño, el 13 y el 19 se parten en dos (13a/13b, 19a/19b), «Próximo paso» trae los comandos literales, `AGENTS.md` §5.1 suma reglas extra, WP-05 T-03 pasa a ser mecánico (`revisar_reparto.py`) y la revisión cuenta T-03b y decide D-06. Si un turno de Gemma queda `INCOMPLETO` dos veces por la misma tarea, Claude Code la propone para el siguiente turno de Cursor | Dueña del proyecto: «A partir de este punto salen del equipo el agente de Google Antigravity y el agente de Github Copilot. El trabajo de estos agentes lo tomará un agente de Qwen 3.5 9B.» · ajuste del reparto: Claude Code · el modelo lo corrige P-15 |
| 2026-11-13 | P-12 | `wiki_plot.py` rechaza tres fuentes más: artículos de trilogías o sagas, redirecciones a una sección de otro artículo y, en la búsqueda por título, un artículo cuyo título no contiene el de la película ni al revés. La corrección `lote-028` ocupa ese número: el turno 12 empieza en el `029` | Claude Code (R-11) |

## 5. Bloqueos abiertos

Ninguno.

Resueltos: **B-05** (turno 12: Gemma informó que `./mvnw verify` fallaba; no fallaba, leyó como fallos los `ERROR` que escriben a propósito las pruebas de errores. Código de salida 0; R-12). **B-04** (turno 12: `lote-029` no existía porque OpenCode cortó `proponer_trama.py` a los 2 minutos, su timeout por defecto, antes de que escribiera el lote; `aplicar_cambios.py` falló después por eso). No se aplicó nada ni cambió `data/`. «Próximo paso» y `AGENTS.md` §5.1 piden ahora 600000 ms para todo comando del turno. **B-01** (turno 06, `EFX-ING-026` sobre una cita de Wikipedia), cerrado en R-06 con `73cd5d0`. **B-02**
(turno 08, `ShippedCatalogValidationTest` en rojo en la rama de WP-03), cerrado en R-08 con la decisión P-09. **B-03**
(turno 10, `EFX-ING-027`: *Red Cliff* y *Red Cliff Part II* con la misma trama), cerrado en R-10 con `7f0ff43` y
`0a67893`.

## 6. Métricas de relevancia

Valores de `target/eval/report.md` de `RelevanceIT`, en formato `nDCG@10 · MRR@10`. M0 es producción; M1, M2 y M3 son
staging. Criterio de parada: [WP-02](PLAN.md#wp-02--reescribir-los-ensayos-generados).

| Configuración | Mínimo nDCG actual | M0 | M1 | M2 | M3 | Producción final |
|---|---|---|---|---|---|---|
| `bm25_title` | 0.06 | 0.113 · 0.125 | 0.113 · 0.125 | | | |
| `bm25_multi_match` | 0.06 | 0.115 · 0.125 | 0.115 · 0.125 | | | |
| `bm25_plot` | 0.37 | 0.423 · 0.444 | 0.408 · 0.443 | | | |
| `knn_plot_256` | 0.49 | 0.549 · 0.536 | 0.573 · 0.580 | | | |
| `knn_plot_512` | 0.60 | 0.651 · 0.653 | 0.626 · 0.636 | | | |
| `knn_plot_1024` | 0.69 | 0.742 · 0.755 | 0.726 · 0.755 | | | |
| `rrf_title_knn256` | 0.29 | 0.348 · 0.316 | 0.407 · 0.385 | | | |
| `rrf_title_knn1024` | 0.41 | 0.464 · 0.422 | 0.475 · 0.439 | | | |
| `rrf_plot_knn256` | 0.53 | 0.581 · 0.612 | 0.574 · 0.589 | | | |
| `rrf_plot_knn1024` | 0.61 | 0.662 · 0.677 | 0.675 · 0.691 | | | |

M0 medida el 2026-11-12 17:48 UTC (turno 03) sobre el índice de producción `elastiflix-reloaded-20261112-144129`;
M1 medida el 2026-11-12 20:20 UTC (turno 07) sobre el índice de staging `elastiflix-dq-staging-20261112-201036` (6 899 películas, commit `220a8ea`);
`target/eval/report.md` lleva la hora de la última corrida (`RelevanceIT`, 1 prueba, 0 fallos, 0 omitidas).

## 7. Indicadores de calidad del catálogo

Cifras de `python data-quality/scripts/analizar.py --etiqueta <X>` (con `--desde-data` mientras no haya recarga).
La columna «Inicial» sale de `hallazgos/salidas/m0/`.

| Indicador | Salida | Inicial | Meta | M3 |
|---|---|---|---|---|
| Plot con 2 o más firmas de texto generado | `llm.txt` («con >=2») | 1 408 | ≤ 50 | |
| Respuestas de asistente y correcciones de año | `llm.txt` | 2 + 6 | 0 | |
| Plot con taquilla o crítica | `plots.txt` («producción/recepción») | 1 549 | < 300 | |
| Plot que abre con la ficha | `plots.txt` («ficha inicial») | 3 734 | < 500 | |
| Plot de menos de 200 caracteres | `plots.txt` («< 200 chars») | 686 | solo las «sin fuente» | |
| Plot sin `enrichment.plot` (6 899 menos la cuenta de `plot` en «enrichment: claves») | `deep.txt` | 5 275 | 0 | |
| Textos con espacios, NBSP, sin NFC o con `&amp;` | `deep.txt` | 124 | 0 | |
| Ids de palabra clave con varios nombres | `deep.txt` | 14 | solo los legítimos de TMDB | |

## 8. Diario del plan

Las entradas de cada WP van en su diario. Aquí solo se anotan hitos del plan completo.

### 2026-11-12 15:55 UTC · Claude Code (Claude Opus 5.5) · COORD
- Hice: análisis completo del índice vivo en solo lectura; informe en `informe/informe-calidad.html`; detalle por
  película en `hallazgos/hallazgos.csv`; alcance congelado en `hallazgos/listas/`; plan, protocolo y bitácora.
- Evidencia: `hallazgos/salidas/m0/*.txt`. `analizar.py --desde-data` reproduce `hallazgos.csv` byte a byte, así que
  `data/` y el índice coinciden.
- Quedó pendiente: todo el plan.

### 2026-11-12 16:10 UTC · Claude Code (Claude Opus 5.5) · COORD
- Hice: revisión independiente del plan contra el código (23 observaciones) y correcciones. Publicación por cambio
  atómico de alias con vuelta atrás; bitácora única en `dq/ronda-4` con worktree; `es.py` solo escribe en staging;
  mediciones por etiqueta con `m0` congelada; `--desde-data`; `estado.tsv` por WP; WP-03 se integra tras WP-05;
  regla de umbrales corregida (medición − 0.05).
- Quedó pendiente: P-0 y H-1 de la dueña del proyecto.
- Para el siguiente: ver «Próximo paso».

### 2026-11-12 16:16 UTC · Claude Code (Claude Opus 5.5) · COORD
- Hice: P-0 según la respuesta de la dueña (P-04). Worktree `E:\02 - programming\03 - elastiflix-reloaded-dq` en la
  rama local `dq/ronda-4` desde `main` (`9a1b864`); `data-quality/` movido ahí y commiteado; `.env` copiado sin
  mostrarlo. `AGENTS.md`, `PLAN.md` y `README.md` adaptados al modo local (sin push ni PR; hitos por merge local).
- Evidencia: `git worktree list`; `analizar.py --etiqueta p0 --desde-data` reproduce `hallazgos.csv` desde el worktree.
- Quedó pendiente: H-1.
- Para el siguiente: ver «Próximo paso».

### 2026-11-12 16:27 UTC · Claude Code (Claude Opus 5.5) · revisor
- Hice: P-05 a pedido de la dueña del proyecto. Reparto del plan en 21 turnos (7 por ejecutor, 2 grandes cada uno);
  protocolo de turno del ejecutor y de revisión de Claude Code (`AGENTS.md` §5 y §6, plantilla `R-NN.md`);
  tablero de turnos y de revisiones; etiqueta de partida `dq-r00`. GitHub Copilot entra en lugar de Qwen 2.5 Coder.
- Quedó pendiente: H-1 y D-01 a D-04.
- Para el siguiente: turno 01, Google Antigravity.

### 2026-11-12 16:30 UTC · Claude Code (Claude Opus 5.5) · revisor
- Hice: anoté la respuesta de la dueña del proyecto, «Autorizo H-1 y acepto las recomendaciones de D-01 a D-04»: H-1, D-01, D-02 (opción A), D-03 (sí) y D-04 (no se
  quita) en APROBADA, con su fila en «Decisiones tomadas».
- Evidencia: `es.aprobada("H-1")` devuelve `True`; `es.aprobada("H-2")`, `False`.
- Quedó pendiente: H-2, que se pide en el turno 21.
- Para el siguiente: turno 01, Google Antigravity.

### 2026-11-12 16:55 UTC · Claude Code (Claude Opus 5.5) · revisor (R-01)
- Hice: revisión del turno 01 de Google Antigravity. APROBADO CON CORRECCIONES: tres defectos de validación
  corregidos (`fb06ad8`), dos líneas sin commitear descartadas, y las horas de mis entradas anteriores corregidas
  según los commits. Detalle en `bitacora/R-01.md`.
- Evidencia: `./mvnw -q verify` (798 pruebas, 0 fallos); `catalogo.py --verificar` 14/14; `aplicar_cambios.py
  --verificar` con los tres rechazos nuevos.
- Para el siguiente: turno 02, Cursor.

### 2026-11-12 17:45 UTC · Claude Code (Claude Opus 5.5) · revisor (R-02)
- Hice: P-06 registrada antes de la revisión (`4e25443`). Revisión del turno 02 de Cursor: APROBADO CON
  CORRECCIONES (`83af348`). Wikimedia limita a 10 peticiones por minuto a un `User-Agent` sin contacto; se abre
  D-05 y se ajusta NS-7. Detalle en `bitacora/R-02.md`.
- Evidencia: `wiki_plot.py --verificar` y `proponer_trama.py --comprobar` en verde; lote real de las 13 de WP-01
  propuesto en `work/r02-wp01/` (13 de 13 con trama) y simulado sin escribir `data/`.
- Para el siguiente: turno 03, GitHub Copilot.

### 2026-11-12 18:00 UTC · Claude Code (Claude Opus 5.5) · revisor (R-02)
- Hice: D-05 aprobada por la dueña del proyecto («La URL del repo»). Agregué
  `DQ_WIKI_CONTACTO=https://github.com/bigfito/elastiflix-reloaded` al `.env` del worktree, que no se commitea.
- Evidencia: el `User-Agent` es `elastiflix-data-quality/1.0 (https://github.com/bigfito/elastiflix-reloaded)
  python-urllib`, con pausa de 1 s; 8 de 8 tramas de `wp02-prioridad.tsv` descargadas sin ningún 429.
- Para el siguiente: turno 03, GitHub Copilot.

### 2026-11-12 18:15 UTC · Claude Code (Claude Opus 5.5) · revisor (R-03)
- Hice: revisión del turno 03 de GitHub Copilot. APROBADO CON CORRECCIONES (`0ab67e4`): guardas de borrado en
  `publicar.py` y comando del harness sobre staging en `AGENTS.md` §9. WP-00 queda HECHA. Detalle en
  `bitacora/R-03.md`.
- Evidencia: producción intacta (`elastiflix-reloaded-20261112-144129`, 6 899); staging con 6 899 y comprobaciones de
  `publicar.py` en verde; harness sobre staging repetido con el comando corregido, dentro de ±0.02 de M0.
- Para el siguiente: turno 04, Google Antigravity.

### 2026-11-12 18:45 UTC · Claude Code (Claude Opus 5.5) · revisor (R-04)
- Hice: revisión del turno 04 de Google Antigravity. APROBADO sin correcciones. Cerré T-02 de WP-01 (lectura de las
  13) y WP-01 queda HECHA; WP-02 `lote-001` aceptado. Detalle en `bitacora/R-04.md`.
- Evidencia: `data/` cambió solo en las 113 películas de los lotes; `./mvnw -q verify` en verde (798);
  `analizar.py --etiqueta r04 --desde-data` en `hallazgos/salidas/r04/`.
- Para el siguiente: turno 05, Cursor.

### 2026-11-12 19:35 UTC · Claude Code (Claude Opus 5.5) · revisor (R-05)
- Hice: revisión del turno 05 de Cursor. APROBADO CON CORRECCIONES: dos tramas venían del artículo de otra obra
  (*Mobile Suit Gundam I* y *Jackass 3.5*) y se corrigieron con los lotes `007` y `008` (overview de TMDB);
  `wiki_plot.py` descarta artículos de series, videojuegos y manga. Decisión P-07 sobre los números de lote.
  Detalle en `bitacora/R-05.md`.
- Evidencia: `data/` cambió solo en las 500 películas de los lotes y en las 2 corregidas; `./mvnw -q verify` en
  verde; `hallazgos/salidas/r05/`.
- Para el siguiente: turno 06, GitHub Copilot.

### 2026-11-12 20:05 UTC · Claude Code (Claude Opus 5.5) · revisor (R-06)
- Hice: revisión del turno 06 de GitHub Copilot. APROBADO CON CORRECCIONES: B-01 cerrado ajustando `EFX-ING-026`
  para que no avise una disculpa entre comillas (`73cd5d0`); `wiki_plot.py` reconoce «Summary» y encabezados con
  paréntesis (`a6369d2`) y 4 películas recuperan su trama de Wikipedia (`lote-014`, `e9dac1a`); regla nueva en
  `AGENTS.md` §5. Detalle en `bitacora/R-06.md`.
- Evidencia: `./mvnw -q verify` con 801 pruebas y 0 fallos; `hallazgos/salidas/r06/`.
- Para el siguiente: turno 07, Google Antigravity (cierre de WP-02, M1 y hito A).

### 2026-11-12 20:45 UTC · Claude Code (Claude Opus 5.5) · revisor (R-07) · cierre del hito A
- Hice: revisión del turno 07 de Google Antigravity. APROBADO. WP-02 queda HECHA y se cierra el **hito A** (WP-00,
  WP-01 y WP-02). Detalle y resumen del hito en `bitacora/R-07.md`.
- Hito A: las 1 415 sinopsis de WP-01 y WP-02 son la trama de Wikipedia de su película (1 363) o el overview de TMDB
  (52), con fuente y lote. Sinopsis con dos o más firmas de texto generado: 1 408 → 0; respuestas de asistente y
  correcciones de año: 8 → 0. Relevancia M0 → M1 dentro de ±0.03 en nDCG@10 (`knn_plot_1024` 0.742 → 0.726); ninguna
  baja llega al criterio de parada.
- Evidencia: staging (`elastiflix-dq-staging-20261112-201036`) idéntico a `data/` en las 6 899 sinopsis y con vectores
  al día; producción intacta; `./mvnw -q verify` en verde (801).
- Para el siguiente: turno 08, Cursor (WP-03 en su rama). La dueña del proyecto puede integrar `dq/ronda-4` en `main`.

### 2026-11-12 22:30 UTC · Claude Code (Claude Opus 5.5) · revisor (R-08)
- Hice: revisión del turno 08 de Cursor (WP-03 en su rama). APROBADO CON CORRECCIONES. B-02 venía de mi plan y se
  cierra con P-09; lista `wp05-avisos-wp03.tsv` y WP-05 T-03b para que el turno 13 pueda integrar; las reglas aceptan
  el apóstrofo tipográfico. Registro de la integración del hito A en `main` (P-10). Detalle en `bitacora/R-08.md`.
- Para el siguiente: turno 09, GitHub Copilot (WP-04).

### 2026-11-12 22:50 UTC · Claude Code (Claude Opus 5.5) · revisor (R-09)
- Hice: revisión del turno 09 de GitHub Copilot. APROBADO CON CORRECCIONES (`738511a`): créditos de Wikipedia coherentes
  en `README.md`, `README.en.md` y `data/README.md`, y `ShippedCatalogValidationTest` exige `enrichment.plot` en las
  6 899. WP-04 queda HECHA. Detalle en `bitacora/R-09.md`.
- Evidencia: `data/` cambió solo en las 3 860 películas del lote; 0 sinopsis sin etiqueta; `./mvnw -q verify` en verde.
- Para el siguiente: turno 10, Google Antigravity (WP-05).

### 2026-11-12 23:40 UTC · Claude Code (Claude Opus 5.5) · revisor (R-10)
- Hice: revisión del turno 10 de Google Antigravity. APROBADO CON CORRECCIONES: B-03 resuelto (guardas contra
  artículos repetidos y de obras en dos partes; *Red Cliff*, *Red Cliff Part II* y *Che: Part Two* vuelven a su
  sinopsis propia, limpiada, en `lote-010`); la prueba de `limpiar_enciclopedico.py` ya no depende de `data/`.
  Decisión P-11 sobre el reparto de WP-05. Detalle en `bitacora/R-10.md`.
- Evidencia: `./mvnw -q verify` en verde; `hallazgos/salidas/r10/`.
- Para el siguiente: turno 11, Cursor.

### 2026-11-13 00:05 UTC · Claude Code (Claude Opus 5.5) · revisor (R-11)
- Hice: revisión del turno 11 de Cursor. APROBADO CON CORRECCIONES. 7 de las 1 390 tramas eran de otra obra: dos de
  trilogías, cuatro por redirecciones a una sección de otro artículo y una por la búsqueda por título. Tres guardas
  nuevas en `wiki_plot.py` (P-12), y las 7 vuelven a su sinopsis propia, limpiada, en `lote-028`. Abrí D-06 para las
  5 sinopsis generadas que no tienen fuente. Detalle en `bitacora/R-11.md`.
- Evidencia: `./mvnw -q verify` en verde; `hallazgos/salidas/r11/`.
- Para el siguiente: turno 12, GitHub Copilot (empieza en el `lote-029`). D-06 aprobada por la dueña del proyecto («Apruebo D-06»), la aplica el turno 13a (WP-05 T-06).

### 2026-11-13 00:15 UTC · Claude Code (Claude Opus 5.5) · COORD (P-13)
- Hice: ajusté el plan al equipo nuevo. Salen Google Antigravity y GitHub Copilot, y Gemma 4 E4B toma sus turnos.
  Partí los turnos 13 y 19 en dos y escribí «Próximo paso» del turno 12 con comandos literales. Sumé `AGENTS.md`
  §5.1 (reglas para un modelo pequeño) y `scripts/revisar_reparto.py` (WP-05 T-03, solo lectura). Actualicé
  `PLAN.md`, `AGENTS.md`, `README.md` y este tablero.
- Evidencia: decisión P-13; `python data-quality/scripts/revisar_reparto.py --etiqueta r11` probado (193 películas,
  129 cerradas y 64 pendientes de juicio, sobre los datos de `dq-r11`).
- Para el siguiente: turno 12, Gemma 4 E4B.

### 2026-11-13 00:25 UTC · Claude Code (Claude Opus 5.5) · COORD (P-14)
- Hice: pasé a Cursor los turnos de Java, 15 (WP-07 parte B) y 18 (WP-10), por decisión de la dueña del proyecto.
  Actualicé el reparto en `PLAN.md`, el orden en `AGENTS.md` y §1.1 de este tablero.
- Para el siguiente: turno 12, Gemma 4 E4B, sin cambios.

### 2026-11-13 00:40 UTC · Claude Code (Claude Opus 5.5) · COORD (P-15)
- Hice: corregí el ejecutor de P-13 y P-14: es Gemma 4 E4B en OpenCode, no Qwen 3.5 9B. Cambié el nombre y la firma
  en `PLAN.md`, `AGENTS.md`, `README.md`, este tablero, R-11, el diario de WP-05 y `revisar_reparto.py`. El reparto
  no cambia. `AGENTS.md` §5.1 permite cerrar el turno después de cualquier lote commiteado.
- Para el siguiente: turno 12, Gemma 4 E4B.

### 2026-11-13 00:45 UTC · Claude Code (Claude Opus 5.5) · COORD (B-04)
- Hice: diagnostiqué el bloqueo que Gemma reportó en el turno 12. `proponer_trama.py` funciona (lo probé fuera de
  `data-quality/cambios/`), pero OpenCode lo cortó a los 2 minutos, su timeout por defecto: la corrida empezó a las
  00:31:52 UTC y la última descarga de Wikipedia es de las 00:33:50. Sin `lote-029.jsonl`, `aplicar_cambios.py`
  falló. Descarté el texto que Gemma había agregado sin commitear (una línea en UTF-16 al final de este archivo,
  escrita por redirección de la terminal) y registré B-04 como resuelto. Ajusté «Próximo paso» y `AGENTS.md` §5.1:
  timeout de 600000 ms para todo comando, comprobar la línea final del script antes de aplicar, y editar la bitácora
  solo con la herramienta de edición.
- Evidencia: `git status` sin cambios en `data/` ni en `cambios/`; `estado.tsv` termina en `lote-025` y `lote-028`.
- Para el siguiente: Gemma retoma el turno 12 en el paso B (el paso A ya está hecho, commit `114de11`).

### 2026-11-13 01:20 UTC · Claude Code (Claude Opus 5.5) · revisor (R-12)
- Hice: revisión del turno 12 de Gemma 4 E4B. APROBADO CON CORRECCIONES. Gemma dejó el turno sin cerrar: commiteé lo
  que dejó tal cual (`dq-t12`) y apliqué, uno por uno, los 8 lotes que había propuesto (P-16). Corregí 3 tramas de
  otra obra con dos guardas más en `wiki_plot.py` y `lote-041`. Sumé `proponer_trama.py --d06` y 4 películas a D-06.
  B-05 fue un falso bloqueo. Detalle en `bitacora/R-12.md`.
- Evidencia: `./mvnw -q verify` con código 0; `hallazgos/salidas/r12/`.
- Para el siguiente: turno 13a, Gemma 4 E4B (cierre de WP-05).

### 2026-11-13 03:50 UTC · Claude Code (Claude Opus 5.5) · revisor (R-13a)
- Hice: revisión del turno 13a de Gemma 4 E4B, que no hizo ningún commit. Commit de recuperación (`dq-t13a`), los 5
  lotes que dejó propuestos aplicados uno por uno, y *Prancer: A Christmas Tale* corregida con `lote-048` y una
  guarda nueva para redirecciones a otra película. Abrí D-07. Detalle en `bitacora/R-13a.md`.
- Evidencia: `./mvnw -q verify` con código 0.
- Para el siguiente: en espera de D-07.

### 2026-11-13 04:10 UTC · Claude Code (Claude Opus 5.5) · revisor (R-13a, D-07)
- Hice: con D-07 aprobada (opción A), terminé WP-05: `lote-049` limpió las 70 omitidas (61 limpias y 9 sin fuente),
  `lote-050` puso el overview de TMDB en las 7 sinopsis generadas de D-06, y medí T-05 y T-03. **WP-05 queda HECHA.**
  Pasé a Cursor los turnos 13b a 21 y volví a juntar el 19 en uno solo.
- Evidencia: `ShippedCatalogValidationTest` y `./mvnw -q verify` con código 0; `hallazgos/salidas/wp-05/` (ficha
  inicial 8, taquilla o crítica 5); T-03b, 0 de 81 con la frase; `plot-fuentes.tsv` cuadra con las 4 788 etiquetas
  `wikipedia_plot_…`.
- Para el siguiente: turno 13b, Cursor (WP-03 T-03 y WP-06 T-01 y T-02).

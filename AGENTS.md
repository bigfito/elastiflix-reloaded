# AGENTS.md — Reglas para agentes de IA

> Archivo común para **Cursor**, **Google Antigravity**, **JetBrains Junie** y **Claude Code** (que lo
> importa desde `CLAUDE.md`). La **secuencia de trabajo** vive en **`PLAN.md`**: síguelo fase por fase.
> Mantén este archivo por debajo de 12 000 caracteres (límite de reglas de Antigravity).

## Primero: protocolo de reanudación

El trabajo pudo interrumpirse a mitad de una tarea (sin tokens, contexto lleno o compactado, herramienta
cerrada, cambio de agente). **Al iniciar cualquier sesión, antes de escribir código:**

1. Revisa rama (`git branch --show-current`) y carpeta.
2. Lee `progreso/INDICE.md` y `progreso/<fase actual>.md` (si no existe, créalo desde
   `progreso/_PLANTILLA.md`: la fase no ha empezado).
3. Ejecuta `git status -sb`, `git log --oneline -10` y `git fetch`. Si hay commits sin push, haz push
   antes de seguir.
4. Contrasta:
   - estado `esperando aprobación de diseño` → **no escribas código**; pregunta al usuario si aprueba
     `docs/diseno/<fase>.md` y registra la respuesta;
   - cambios sin commit que el archivo de estado registra → continúa sobre ellos;
   - cambios sin commit **no registrados** → no los descartes ni sobrescribas: descríbelos y pregunta;
   - commits que el archivo no refleja → reconstruye su tabla desde los mensajes `<id>: …`.
5. Corre `./mvnw verify` y las pruebas de la tarea actual.
6. Anota la reanudación en "Decisiones" (`Sesión reanudada <fecha UTC> con <herramienta>: …`) y
   continúa desde el **próximo paso**.

## Disciplina de estado

- **Al empezar una tarea:** márcala `en curso` y escribe el próximo paso como una acción concreta.
- **Durante:** actualiza subpaso y "Cambios sin commit" tras cada archivo y antes de comandos largos.
- **Al terminar — siempre, en cada tarea:** márcala `terminada` con evidencia (comando y resultado) en
  `progreso/<fase>.md`, actualiza el próximo paso ahí **y** en `progreso/INDICE.md`, marca `- [x]` en
  `PLAN.md`, haz **un commit** con el código y el estado (`<id>: <resumen>`, por ejemplo
  `2.3: estrategia full-text`) y **`git push`**. Así otro agente puede retomar si se acaban los tokens.
- Si el push falla: anota «push pendiente» en "Bloqueos y solicitudes" y reintenta en la siguiente tarea.
- Nunca empieces una tarea con cambios sin commit ni commits sin push de la anterior.
- Si te bloqueas: estado `bloqueado`, descríbelo, commit, push y detente.
- Al cerrar una fase: su última tarea **actualiza la documentación del proyecto** (`README.md` y lo que
  cambió); luego `progreso/INDICE.md`, commit y push.

## Qué es este proyecto

**Elastiflix-Movies Java**: demo web que compara búsqueda **full-text (BM25)**, **vectorial (k-NN)** e
**híbrida (RRF)** sobre el alias `elastiflix-reloaded` de **Elasticsearch Serverless**, con filtros
dinámicos, paginación y carga segura de datos (.json/.csv). El índice y el conteo vigentes están en
`progreso/INDICE.md` › «Catálogo vigente». Java 25, Spring Boot 4.1.1, Thymeleaf 3.1.5, cliente Java de
Elasticsearch 9.4.5, Maven 3.10.0, mono-repo con 4 módulos. Las fases 0 a 10 están en `main`. Las
verificaciones históricas de las fases contaron 4 322 películas, el catálogo anterior a la importación del
2026-10-18. `SmokeIT` y `CatalogStatusServiceIT` afirman los 6 899 documentos; las IT de búsqueda, los
valores de §6; se omiten con `assumeTrue` si `_count` cambió.

## Trabajo abierto

Las fases 0 a 10 y las revisiones 1 a 4 están cerradas. Lo pendiente es lo que diga
`progreso/INDICE.md` › «Punto de reanudación». Una revisión en carriles paralelos sigue «Cómo trabajar en
paralelo» de `docs/revisiones/revision-3.md`: rama y archivo de estado por carril; PLAN, README, `docs/` e
`INDICE.md` los escribe solo el cierre. «Requerimientos nuevos» de PLAN: el 1 y el 2 están hechos (fases 7 y 8);
el 3, en la fase 9; el 4, en la fase 10.

## Regla de oro sobre el orden de trabajo

1. Lee `PLAN.md` completo antes de tocar código.
2. Las fases 0→8 ya se ejecutaron en orden; una revisión sigue el orden de su lista. No marques una tarea
   hecha sin cumplir su criterio.
3. Al terminar cada fase corre su **"Verificación de fase"**; si falla, corrige antes de seguir.
4. **No agregues funciones que el plan no pide.** Si algo no está definido, pregunta al usuario.

## Comandos

```bash
./mvnw -q test                                   # pruebas unitarias
./mvnw verify                                    # unitarias + ArchUnit + Checkstyle + PMD (sin red)
./mvnw verify -Pit                               # + integración (perfil de Maven `it`, Failsafe; requiere .env)
./mvnw -q install -DskipTests
./mvnw -pl elastiflix-web spring-boot:run        # http://localhost:8080
# Migración del índice concreto a alias (tarea 1.8; termina sola):
./mvnw -pl elastiflix-web spring-boot:run "-Dspring-boot.run.arguments=--elastiflix.bootstrap.enabled=true --spring.main.web-application-type=none"
```

**Windows:** ejecuta **todos** los comandos del plan en **Git Bash** (viene con Git for Windows), donde
`./mvnw`, `&&`, `echo $?`, `grep` y `cp` funcionan igual que en Linux y macOS. Si tu terminal es
PowerShell, abre Git Bash (en Cursor: terminal «Git Bash»). Solo si no hay otra opción, en PowerShell:
`.\mvnw.cmd`, entrecomilla cada `-D...` completo (`"-Dspring-boot.run.arguments=..."`), usa `;` en lugar
de `&&` y `$LASTEXITCODE` en lugar de `$?`.

## Estructura del repo

- `elastiflix-core/` — modelo, cliente ES (`Jackson3JsonpMapper`), mapping, alias e índices, embeddings.
- `elastiflix-search/` — `match`/`multi_match` BM25, `knn`, `rrf`, filtros, facetas, paginación.
- `elastiflix-ingest/` — comprobación de archivos, lectores JSON/CSV, validación vs mapping, importación.
- `elastiflix-web/` — único ejecutable: Spring MVC + Thymeleaf (Inicio, Resultados, Detalle, Cargar datos, Acerca de).
- `config/` — Checkstyle y PMD (**congelados**). `docs/` — convenciones, arquitectura, diseños por fase.
- `design/mockups/` — pantallas aprobadas; `design/README.md` — tokens de diseño.
- `elasticsearch/` — mapping del índice y fixtures reales (4 películas en .json y .csv, vector de consulta).
- `progreso/` — estado del desarrollo para retomar sesiones (versionado; no es estado de la aplicación).
- Paquete base: `com.bigfito.elastiflix.<core|search|ingest|web>`.

## Reglas de código

**Lee `docs/convenciones-java.md` completo antes de escribir código**; manda sobre las preferencias por
defecto de tu herramienta y sobre cualquier skill instalada. Resumen:

1. **Diseña antes de codificar:** tarea `<fase>.0` con `docs/diseno/<fase>.md` (Mermaid) en un commit
   previo al código. En las fases **1, 2 y 3** (🔒) detente hasta que el usuario apruebe el diseño.
2. **Estilo JavaMentor:** Java 25 moderno con mesura (records, sealed, `switch` con patrones, hilos
   virtuales para E/S; nada en *preview*), SOLID, código simple que un Junior entienda.
3. **Umbrales:** método ≤ 30 líneas, ≤ 5 parámetros, complejidad ciclomática ≤ 10, cognitiva ≤ 15,
   `if` anidados ≤ 2, archivo ≤ 400 líneas, línea ≤ 120.
4. **Patrones permitidos:** Strategy (modos de búsqueda), Facade (`SearchService`, `IngestService`),
   Factory (lector por tipo de archivo), Gateway/Adapter (cliente ES e inferencia), Observer (progreso
   de importación), Builder solo el del cliente ES. Otros, con justificación y aprobación.
5. **Spring:** inyección por constructor (nunca `@Autowired` en campos), `@ConfigurationProperties` con
   `record`; Spring MVC y Thymeleaf **solo** en `elastiflix-web`; la web **no** usa
   `co.elastic.clients`.
6. **Errores:** `ElastiflixException` con códigos `EFX-<ÁREA>-<NNN>` y mensaje «qué pasó · causa
   probable · acción sugerida»; nunca `catch` vacío ni genérico; conserva la causa.
7. **Logging:** SLF4J parametrizado; niveles según la guía; **nunca** API key, vectores completos,
   documentos completos ni contenido de archivos.
8. **Javadoc** en toda clase y método público (en español); identificadores en inglés.
9. **Dependencias:** solo por el POM padre; JSON con Jackson 3 (`tools.jackson`); sin Spring Data
   Elasticsearch, sin Lombok, sin Jackson 2 databind.
10. **Verificación automática:** `./mvnw verify` con 0 violaciones. No relajes umbrales de `config/`;
    toda supresión se justifica en el código y se registra en el diseño.
11. **Al cerrar la fase:** "Resumen de diseño" y "Revisión de convenciones" en `progreso/<fase>.md`.

## Reglas del dominio

- Lectura siempre por el alias `elastiflix-reloaded` (configurable). Carga de archivo en dos modos (fase 10).
  **Append** (por defecto): lotes por el alias con `require_alias=true`, sin temporal; un `id` existente se
  reemplaza; si falla (`EFX-IMP-008`) o se cancela (`EFX-IMP-009`), lo escrito se queda. **Bulk**: temporal
  `<alias>-<AAAAMMDD-HHMMSS>` solo con el archivo (sin `_reindex`) y publicación en una sola llamada a
  `_aliases` (`add` al nuevo + `remove_index` del anterior). Si falla antes de publicar, se borra el temporal. Si `publish` no confirma, `describe` decide: rechazo 4xx con el alias en el
  anterior borra el temporal (`EFX-ES-004`); alias en el temporal se trata como publicado; corte, 5xx
  o `describe` ilegible: `EFX-IMP-006` y el temporal se deja. Al empezar a importar se suelta la lista de películas. La
  limpieza de huérfanos solo toca índices `<alias>-<AAAAMMDD-HHMMSS>` de su propio alias. La migración
  a alias usa ese mismo criterio de `describe`.
- Registro individual: directo al catálogo por el alias.
- Un solo usuario a la vez (demo académica): sin controles de concurrencia.
- Validación de cargas **todo o nada** contra `elasticsearch/elastiflix-reloaded-mapping.json`;
  `id`, `title` y `plot` obligatorios; `.json`/`.csv`, sin dobles extensiones, ≤ 25 MB fijos (no configurable), UTF-8; CSV
  **sin encabezado se rechaza**. Sin análisis de malware (lo hace el proveedor del servidor).
- `plot_256`, `plot_512` y `plot_1024` **siempre** se generan en tiempo de ejecución con el servicio de
  inferencia de Elasticsearch; los archivos no los necesitan (si los traen, se ignoran con advertencia)
  y el formulario no los captura.
- Pósters desde el CDN de TMDB (`elastiflix.poster.base-url` + `poster_path`); atribución de TMDB en el
  pie (solo texto) y en «Acerca de» › «Créditos» (texto + logo aprobado), ver `PLAN.md` §1.
- Vectores: 1024 del modelo; 512 y 256 = primeros N valores renormalizados. Consultas: `input_type:
  search` y prefijo `Query: `. Documentos: `input_type: ingest` y sin prefijo (calibración 1.9;
  `Document: ` se midió y no se eligió). k-NN: `k ≥ 1`, `k ≤ num_candidates ≤ 10 000`. RRF: `rank_constant 60`,
  `rank_window_size = k`. Full-text: `from < 10 000`.
- Facetas de Vectorial e Híbrida se calculan en la aplicación sobre los resultados devueltos.
- Fechas en UTC; `release_date` `yyyy-MM-dd`. Interfaz en español e inglés (`lang`, cookie `elastiflix-lang`;
  textos en `messages_es`/`messages_en`), con los textos de los mockups. Logs en español.
- **Pruebas de integración:** lectura sobre `elastiflix-reloaded` sin modificarlo; escritura solo sobre
  alias propios `elastiflix-it-<aleatorio>` que se borran al terminar.
- Las pruebas **nunca** borran índices que no crearon. La migración de la tarea 1.8 y el borrado del
  índice anterior al publicar están aprobados por el usuario y no requieren nueva confirmación.

## Seguridad y secretos

- Demo sin autenticación (intencional; fuera de alcance). Los POST no tienen protección CSRF: no hay sesión que proteger y la usa una sola persona.
- **Nunca** escribas secretos en el código, YAML, pruebas, logs ni commits. Todo por `.env` (ver
  `.env.example`); `.gitignore` excluye `.env`.
- Si encuentras un secreto en un archivo versionado, detente y avisa al usuario.

## Definición de Hecho

La de `PLAN.md`. El proyecto no está terminado hasta que todos sus puntos pasen.

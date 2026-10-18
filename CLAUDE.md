@AGENTS.md

# CLAUDE.md — Notas para Claude Code

> Las reglas comunes están en `AGENTS.md` (importado arriba). La secuencia de trabajo está en
> **`PLAN.md`**. Este archivo solo agrega lo específico de Claude Code.
>
> Las fases 0 a 6, la revisión 1 y los bugs B-01 a B-05 ya están en `main`. La **Revisión 2**
> (`S-01`…`S-27`, lista en `docs/revisiones/revision-2.md`, estado en `progreso/revision-2.md`) está
> terminada en la rama `revision-2` y se integra por PR. Antes de empezar, lee `progreso/INDICE.md`: ahí
> están el catálogo vigente y el próximo paso. Para pedirlo en Claude Code basta con «Retoma el trabajo»
> (ver `README.md`).

## Antes de empezar

0. Aplica el **protocolo de reanudación** de `AGENTS.md`: `progreso/INDICE.md`, el archivo de la fase
   actual, `git status` y `git log --oneline -10`.
1. Lee **`PLAN.md`** completo (arquitectura, stack, contrato, modelo de datos, fases).
2. Lee **`docs/convenciones-java.md`** completo, **aunque tengas instalada la skill `java-developer`**:
   el documento tiene umbrales y desviaciones del proyecto y manda sobre la skill.
3. Trabaja **fase por fase, en orden**; corre la "Verificación de fase" antes de avanzar.
4. En una revisión, cada `S-NN` es una tarea: atiéndelas en orden, con su criterio completo, en la rama
   `revision-2`.

## Sugerencias de uso con Claude Code

- Aplica el protocolo de reanudación también tras `/clear`, tras una **compactación automática** o al
  retomar una conversación: tu memoria de la sesión puede estar incompleta; `progreso/` y git son la
  fuente de verdad.
- Aborda **una fase por sesión** cuando sea posible; **un commit y un push por tarea** con su archivo de
  `progreso/` actualizado.
- Si la conversación ya es muy larga, detente en un **punto seguro** (tarea con commit o estado
  actualizado) antes de empezar trabajo nuevo.
- En las fases 🔒 (1, 2 y 3), presenta el diseño **en la conversación** (resumen y diagramas Mermaid),
  deja la fase en `esperando aprobación de diseño` y espera la respuesta del usuario.
- Usa la lista de tareas de `PLAN.md`; marca `- [x]` cuando pase el criterio.
- Si una verificación falla, **detente y corrige** antes de continuar.
- Al terminar **cada** tarea: estado actualizado (`progreso/<fase>.md` e `INDICE.md`), commit y `git push`.
- Al cerrar cada fase: actualiza la documentación del proyecto (`README.md` y lo que cambió).

## Definición de Hecho

La de `PLAN.md`.

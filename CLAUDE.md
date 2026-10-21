@AGENTS.md

# CLAUDE.md — Notas para Claude Code

> Las reglas comunes están en `AGENTS.md` (importado arriba). La secuencia de trabajo está en
> **`PLAN.md`**. Este archivo solo agrega lo específico de Claude Code.
>
> Las fases 0 a 8, la revisión 1, los bugs B-01 a B-05 y las revisiones 2 (PR #13) y 3 (carriles en los PR
> #15 a #20 y cierre Z) y 4 (PR #25 y #26) ya están en `main`, igual que la fase 9 (Requerimiento 3, PR #27); la fase 10 (Requerimiento 4) está en curso. Antes de empezar, lee
> `progreso/INDICE.md`: ahí están el catálogo vigente y el próximo paso. De los «Requerimientos nuevos» de
> `PLAN.md`, el 1 (fase 7, detalle) y el 2 (fase 8, español e inglés) están hechos; el 3 (fase 9) también; el 4 es la
> fase 10.

## Antes de empezar

0. Aplica el **protocolo de reanudación** de `AGENTS.md`: `progreso/INDICE.md`, el archivo de la fase
   actual, `git status` y `git log --oneline -10`.
1. Lee **`PLAN.md`** completo (arquitectura, stack, contrato, modelo de datos, fases).
2. Lee **`docs/convenciones-java.md`** completo, **aunque tengas instalada la skill `java-developer`**:
   el documento tiene umbrales y desviaciones del proyecto y manda sobre la skill.
3. Trabaja **fase por fase, en orden**; corre la "Verificación de fase" antes de avanzar.
4. En una revisión, cada corrección es una tarea: atiéndelas en orden, con su criterio completo. En la
   revisión en carriles, toma un carril libre y trabaja en un worktree propio (`git worktree add`) con su
   rama; varias sesiones de Claude Code pueden llevar carriles distintos a la vez.

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

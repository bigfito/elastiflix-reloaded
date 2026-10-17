# Estado de trabajo — <unidad>

> Archivo de estado para retomar el trabajo en otra sesión sin perder avance. Cópialo como `progreso/<unidad>.md` al empezar la unidad. Actualízalo al empezar y al terminar **cada** tarea, súbelo en el mismo commit que el código y haz `git push` al terminar la tarea (también actualiza `progreso/INDICE.md`). Reglas completas en la sección "Estado del trabajo y reanudación" de `PLAN.md`.

## Identificación

- **Unidad:** <id de la fase o carril>
- **Nombre:** <nombre de la fase o carril>
- **Rama:** <main | nombre de la rama>
- **Carpeta:** <carpeta principal o ruta del worktree>
- **Herramienta de la última sesión:** <Claude Code | Cursor | Google Antigravity | Junie | …>

## Estado general

<no iniciado | en curso | esperando aprobación de diseño | bloqueado | en verificación | terminado>

## Punto de reanudación

- **Tarea actual:** <id y título>
- **Subpaso en curso:** <qué se estaba haciendo exactamente>
- **Próximo paso:** <acción concreta, ejecutable por otro agente sin contexto previo>
- **Última actualización:** <AAAA-MM-DD HH:MM UTC>

## Tareas

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| <id> | <pendiente \| en curso \| terminada \| bloqueada> | <hash corto> | <comando ejecutado y resultado> |

## Cambios sin commit

<Archivos modificados en la tarea actual y qué falta en cada uno. Vacío cuando todo está en commit.>

## Última verificación

- **Comando:** <…>
- **Resultado:** <en verde | falló: resumen>
- **Fecha:** <AAAA-MM-DD HH:MM UTC>

## Decisiones

<Decisiones con tarea y razón. Registra cada reanudación: "Sesión reanudada <fecha>: <punto encontrado>".>

## Resumen de diseño

<Al cerrar la unidad: estructura final, decisiones relevantes, alternativas descartadas y diferencias con `docs/diseno/<unidad>.md`.>

## Revisión de convenciones

<Al cerrar la unidad, responde cada punto de la lista de revisión de `docs/convenciones-java.md`; quien integra la confirma.>

- [ ] <pregunta de revisión> <respuesta>

## Bloqueos y solicitudes

<Vacío si no hay.>

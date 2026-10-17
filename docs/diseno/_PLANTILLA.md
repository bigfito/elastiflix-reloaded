# Diseño — <unidad>: <nombre>

> Resultado de la tarea `<unidad>.0`. Se hace commit **antes** de escribir código de producción. Sigue `docs/convenciones-java.md`. Cópialo como `docs/diseno/<unidad>.md`.

## Estado de aprobación

- **Requiere aprobación del usuario:** <sí | no> (ver la sección "Calidad de código" de `PLAN.md`)
- **Estado:** <borrador | esperando aprobación | aprobado | no requiere aprobación>
- **Aprobación:** <quién, fecha UTC y comentarios del usuario>

## Propósito

<Qué resuelve esta unidad, en dos o tres frases.>

## Requerimientos cubiertos

| Tarea de `PLAN.md` | Referencia en modelo de datos o contratos | Cómo la cubre este diseño |
|---|---|---|

## Dudas y supuestos

<Preguntas abiertas (si hay, la unidad está bloqueada hasta resolverlas) y supuestos explícitos con su justificación.>

## Diagrama de clases

```mermaid
classDiagram
  %% Contratos que implementa o consume, clases nuevas y sus relaciones.
```

## Secuencia o flujo

<Obligatorio si hay concurrencia, reintentos, estados o varios pasos coordinados. Si no aplica, dilo en una línea.>

```mermaid
sequenceDiagram
  %% o flowchart
```

## Responsabilidades

| Clase | Responsabilidad en una frase | Contrato que implementa o consume |
|---|---|---|

## Patrones usados

| Patrón (de la lista cerrada del documento de convenciones) | Dónde | Por qué aporta claridad aquí |
|---|---|---|

## Errores

| Código | Cuándo ocurre | Causa probable | Acción sugerida |
|---|---|---|---|

## Logging

| Nivel | Evento | Contexto incluido |
|---|---|---|

## Pruebas previstas

| Prueba | Comportamiento que verifica | Tipo (unitaria o de integración) |
|---|---|---|

## Alternativas descartadas

<Opción, por qué se consideró y por qué se descartó.>

## Supresiones de la verificación automática

<Idealmente vacío. Si hay alguna: regla, clase, motivo.>

## Cambios respecto al diseño aprobado

<Se completa al cerrar la unidad: qué cambió durante la implementación y por qué.>

# 🎨 Diseño aprobado — Elastiflix-Movies Java

La interfaz de la aplicación **reproduce fielmente** estos mockups: misma paleta, tipografía, textos,
componentes y comportamiento. No se rediseñan.

- **Canvas de diseño (vista navegable):** https://claude.ai/artifact/EioeoK2qmzQEZUTKuqgwtW
- **Fuentes de los mockups:** `mockups/` (formato `.dc.html` del canvas; necesitan el runtime del
  canvas para verse interactivos, pero el HTML, los estilos en línea, los textos y la lógica de cada
  pantalla se leen directamente).

| Archivo | Página | Ruta en la aplicación | Variantes (tweaks del canvas) |
|---|---|---|---|
| `mockups/Main.dc.html` | Inicio · búsqueda | `GET /` | modo, ajustes, sugerencias dinámicas |
| `mockups/Results.dc.html` | Resultados | `GET /buscar` | modo: Full-text · Vectorial · Híbrida; estado: Con resultados · Sin resultados · Sin resultados con filtros |
| `mockups/Ingest.dc.html` | Cargar datos | `GET /cargar` | escenario: Archivo válido · No coincide con el mapping · Archivo rechazado |
| `mockups/About.dc.html` | Acerca de la demo | `GET /acerca` | — |

> Los valores entre corchetes de los mockups (`[release_date]`, `[vote_count]`…) y las variables
> `{{…}}` se sustituyen por datos reales. Los números de ejemplo de Resultados e Ingesta son reales
> (se verificaron contra el catálogo), pero en la aplicación salen de Elasticsearch.

## Tokens de diseño

**Tipografías** (Google Fonts)

| Rol | Familia | Pesos |
|---|---|---|
| Títulos | Bricolage Grotesque | 500, 700 |
| Texto | IBM Plex Sans | 400, 500, 600 |
| Código, etiquetas y cifras | IBM Plex Mono | 400, 500 |

**Colores base** (cuando hay dos valores, el primero es el token principal y el segundo su variante, con
el nombre indicado; úsalos donde los usa el mockup)

| Token | Hex | Uso |
|---|---|---|
| `--bg` | `#FBF7F1` | Fondo de página |
| `--surface` | `#FFFFFF` | Tarjetas y paneles |
| `--surface-muted` / `--surface-muted-2` | `#F3ECE2` / `#F6F0E8` | Bloques secundarios |
| `--border` / `--border-soft` | `#E2D8CA` / `#EFE7DC` | Bordes / separadores y fondos deshabilitados |
| `--ink` | `#2E2A3B` | Texto principal |
| `--ink-2` | `#3A3548` | Texto y enlaces |
| `--ink-3` / `--ink-4` | `#4F4A5E` / `#585266` | Texto secundario |
| `--muted` / `--muted-2` | `#6B6578` / `#8C8597` | Ayudas y marcadores |
| `--accent-ink` | `#A9502A` | Hover, foco y elemento activo |
| `--error` | `#B9472A` | Mensajes de error |

**Pastel multitono** (franja superior, modos, pestañas y estados)

| Token | Hex | Tinta asociada |
|---|---|---|
| `--peach` | `#FFCDB2` (acento por defecto) | `#A9502A` |
| `--butter` | `#FBE7A1` | — |
| `--mint` | `#BDE8D7` | `--mint-ink` `#1E7A5C` / `--mint-ink-2` `#2E8B6E` |
| `--sky` | `#BFE0F6` | `#1F6FA8` |
| `--lavender` | `#CDBDF6` | `#6A50C8` |
| `--rose` | `#F7C1D9` | `#A8336B` |

**Estados del proceso de carga** (pasos 1–5)

| Estado | Fondo | Borde | Tinta |
|---|---|---|---|
| completado | `#ECF8F3` | `#BDE8D7` | `#1E7A5C` |
| en curso | `#EEF6FD` | `#BFE0F6` | `#1F6FA8` |
| detenido | `#FDEEF5` | `#F7C1D9` | `#B9472A` |
| listo para empezar | `#FFF1E8` | `#FFCDB2` | `#A9502A` |
| pendiente | `#FBF7F1` | `#E2D8CA` | `#6B6578` |

**Elemento distintivo:** la franja superior de bloques pastel (`repeating-linear-gradient` de 10 px con
los seis tonos) presente en todas las páginas, y el logo cuadrado con el triángulo de "play".

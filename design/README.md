# 🎨 Diseño — Elastiflix-Movies Java

Mockups de las cinco páginas y tokens de diseño (tipografías, paleta y estados) que sigue la interfaz.

## Mockups

Los archivos de [`mockups/`](mockups/) son prototipos en HTML con estilos en línea. Se abren en el navegador y
muestran la estructura, los colores y los textos de cada pantalla; sus variantes interactivas (cambiar de modo, de
escenario o de película) solo funcionan en la herramienta de diseño donde se hicieron. Cada página tiene su versión
en inglés (`-en`).

| Archivo | Página | Ruta en la aplicación | Variantes |
|---|---|---|---|
| `mockups/Main.dc.html` | Inicio · búsqueda | `GET /` | modo, ajustes, sugerencias |
| `mockups/Results.dc.html` | Resultados | `GET /buscar` | modo: Full-text · Vectorial · Híbrida; estado: con resultados · sin resultados · sin resultados con filtros |
| `mockups/Detail.dc.html` | Detalle de película | `GET /pelicula/{id}` | película: Inception · The Equalizer 2; estado: ficha · error `EFX-SRCH-012` (enlace directo sin búsqueda) |
| `mockups/Ingest.dc.html` | Cargar datos | `GET /cargar` | escenario: archivo válido · no coincide con el mapping · archivo rechazado; modo de carga append · bulk |
| `mockups/About.dc.html` | Acerca de la demo | `GET /acerca` | — |
| `mockups/<Página>-en.dc.html` | Versión en inglés de cada página | la misma ruta con `?lang=en` | las mismas |

- Los valores entre corchetes (`[release_date]`, `[vote_count]`…) y las variables `{{…}}` son marcadores: en la
  aplicación salen de Elasticsearch, igual que los conteos del pie y de «Acerca de».
- El logo de TMDB de «Créditos» es `src/main/resources/static/img/tmdb/…svg`.
- En el error `EFX-SRCH-012`, la aplicación usa su página de error común: muestra el código, el «qué pasó» y el
  enlace «Volver a buscar», y el menú no marca ninguna página.

## Tokens de diseño

**Tipografías** (Google Fonts)

| Rol | Familia | Pesos |
|---|---|---|
| Títulos | Bricolage Grotesque | 500, 700 |
| Texto | IBM Plex Sans | 400, 500, 600 |
| Código, etiquetas y cifras | IBM Plex Mono | 400, 500 |

**Colores base** (cuando hay dos valores, el primero es el token principal y el segundo su variante, con
el nombre indicado)

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

"""Localiza la sección de trama en Wikipedia en inglés (WP-00 T-05).

Normas que implementa:
- NS-1(a): sección `Plot`, `Synopsis`, `Plot summary`, `Story` o `Premise`.
- NS-3: de 40 a 3 000 caracteres; si sobra, corta en el último fin de oración.
- NS-4: `imdb_id` → Wikidata (P345) → artículo de `en.wikipedia.org`. Sin id o sin
  resultado, busca por título y solo acepta si el año coincide ±1 y el artículo
  nombra al menos 2 integrantes de `cast`.
- NS-7: `User-Agent` con el contacto de `DQ_WIKI_CONTACTO` (variable de entorno o `.env`, nunca en git) y
  respuestas en `data-quality/work/cache/`. Wikimedia limita a 10 peticiones por minuto a quien no se identifica
  y a 200 a quien da un contacto; sin contacto, el ritmo baja solo a una petición cada 6,5 s. Un 429 o un 503
  respeta `Retry-After` (revisión R-02).

`buscar(ids)` y `trama(artículo)` no escriben sinopsis: copian el texto de la
fuente y solo quitan referencias, notas y espacios de más. No tocan `data/`.
"""
import hashlib
import json
import os
import pathlib
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from collections.abc import Iterable
from typing import Any, NamedTuple

sys.stdout.reconfigure(encoding="utf-8", line_buffering=True)
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import catalogo
from paths import WORK

def _contacto() -> str:
    """Contacto para el `User-Agent` (correo o URL completa), de la variable `DQ_WIKI_CONTACTO` o del `.env`."""
    valor = os.environ.get("DQ_WIKI_CONTACTO", "").strip()
    env = catalogo.REPO / ".env"
    if not valor and env.is_file():
        for linea in env.read_text(encoding="utf-8").splitlines():
            if linea.strip().startswith("DQ_WIKI_CONTACTO="):
                valor = linea.split("=", 1)[1].strip().strip('"').strip("'")
    return valor


CONTACTO = _contacto()
USER_AGENT = (
    f"elastiflix-data-quality/1.0 ({CONTACTO}) python-urllib"
    if CONTACTO
    else "elastiflix-data-quality/1.0 (academic demo) python-urllib"
)
WIKIDATA = "https://query.wikidata.org/sparql"
WIKIPEDIA = "https://en.wikipedia.org/w/api.php"
# Con contacto: 1 petición por segundo (60/min, por debajo de 200/min). Sin contacto: 6,5 s (por debajo de 10/min).
PAUSA_SEGUNDOS = 1.0 if CONTACTO else 6.5
ESPERA_429_MINIMA = 60
LOTE_SPARQL = 200
LARGO_MIN = 40
LARGO_MAX = 3000
# «Summary» es el encabezado de la trama en documentales y en algunas películas (R-06, decisión P-08).
SECCIONES = frozenset({"plot", "synopsis", "plot summary", "story", "premise", "summary"})
NAMESPACES = ("Wikipedia:", "Category:", "Template:", "File:", "Help:", "Portal:", "Draft:", "Talk:")
ID_TITANIC = "597"
ID_SIN_ARTICULO = "1031353"  # Why We Vote (2022): sin imdb_id y sin artículo que cumpla NS-4

_ENCABEZADO = re.compile(r"(?m)^(=+)[ \t]*(.*?)[ \t]*\1[ \t]*$")
_CITA = re.compile(
    r"[ \t]*\[(?:"
    r"\d+"
    r"|citation needed"
    r"|clarification needed"
    r"|verification needed"
    r"|failed verification"
    r"|additional citation(?:\(s\))? needed"
    r"|better source needed"
    r"|page needed"
    r"|unreliable source\??"
    r"|dead link"
    r"|(?:note|nb)\s*\d+"
    r"|n\s*\d+"
    r"|lower-(?:alpha|roman)\s*\d+"
    r"|upper-(?:alpha|roman)\s*\d+"
    r"|who\??|when\??|where\??|why\??|how\??|which\??|by whom\??"
    r"|[A-Za-z]"
    r")\]",
    re.IGNORECASE,
)
_CITA_VISIBLE = re.compile(r"\[(?:\d+|[A-Za-z]|citation needed|note \d+)\]", re.IGNORECASE)
_ANIO_TITULO = re.compile(r"\(((?:19|20)\d{2})\)")
_ANIO_TEXTO = re.compile(r"\b((?:19|20)\d{2})\b")
_ABREVIATURA = re.compile(
    r"(?:^|[\s\n])(?:Mr|Mrs|Ms|Dr|St|Jr|Sr|vs|etc|Prof|Gen|Col|Sgt|Lt|No)\.$",
    re.IGNORECASE,
)

_ultimo_inicio = 0.0


class Trama(NamedTuple):
    """Texto ya limpio y recortado, revisión de la página y título canónico."""

    texto: str
    revid: int
    titulo: str


class Pagina(NamedTuple):
    titulo: str
    extracto: str
    revid: int
    # Si el título pedido es una redirección a una sección de otro artículo («Jarhead 2: Field of Fire» →
    # «Jarhead (film)#Sequels»), el nombre de esa sección: la película no tiene artículo propio (R-11).
    seccion: str | None = None
    # True si el título pedido redirige a otro artículo (R-13a).
    redirigida: bool = False


def _exigir(condicion: bool, mensaje: str) -> None:
    if not condicion:
        raise AssertionError(mensaje)


def _cache(grupo: str, clave: str) -> pathlib.Path:
    digest = hashlib.sha256(clave.encode("utf-8")).hexdigest()[:24]
    return WORK / "cache" / grupo / f"{digest}.json"


def _leer_cache(ruta: pathlib.Path) -> Any | None:
    if not ruta.is_file():
        return None
    try:
        return json.loads(ruta.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return None


def _guardar_cache(ruta: pathlib.Path, cuerpo: Any) -> None:
    ruta.parent.mkdir(parents=True, exist_ok=True)
    temporal = ruta.with_suffix(".json.tmp")
    temporal.write_text(json.dumps(cuerpo, ensure_ascii=False), encoding="utf-8", newline="\n")
    temporal.replace(ruta)


def _esperar() -> None:
    """Separa el inicio de cada petición de red al menos un segundo (NS-7)."""
    global _ultimo_inicio
    ahora = time.monotonic()
    falta = PAUSA_SEGUNDOS - (ahora - _ultimo_inicio)
    if _ultimo_inicio and falta > 0:
        time.sleep(falta)
    _ultimo_inicio = time.monotonic()


def _leer_url(url: str, datos: bytes | None = None, cabeceras: dict[str, str] | None = None) -> bytes:
    headers = {"User-Agent": USER_AGENT, "Accept": "application/json"}
    if cabeceras:
        headers.update(cabeceras)
    req = urllib.request.Request(url, data=datos, headers=headers)
    print(f"Red: {urllib.parse.urlparse(url).netloc}", file=sys.stderr)
    ultimo: Exception | None = None
    intentos = 6
    for intento in range(intentos):
        _esperar()
        try:
            with urllib.request.urlopen(req, timeout=90) as resp:
                return resp.read()
        except urllib.error.HTTPError as exc:
            detalle = exc.read().decode("utf-8", "replace")[:300]
            ultimo = RuntimeError(f"HTTP {exc.code} al consultar Wikipedia o Wikidata: {detalle}")
            if exc.code not in {429, 500, 502, 503, 504} or intento == intentos - 1:
                raise ultimo from exc
            time.sleep(_espera_reintento(exc, intento))
        except urllib.error.URLError as exc:
            ultimo = RuntimeError(f"No se pudo conectar con Wikipedia o Wikidata: {exc.reason}")
            if intento == intentos - 1:
                raise ultimo from exc
            time.sleep(5 * 2**intento)
    raise ultimo or RuntimeError("No se pudo completar la petición")


def _espera_reintento(exc: urllib.error.HTTPError, intento: int) -> float:
    """Segundos antes de reintentar: `Retry-After` si viene; si no, espera exponencial. Un 429 espera al menos 60 s."""
    cabecera = (exc.headers or {}).get("Retry-After", "") if hasattr(exc, "headers") else ""
    espera = float(cabecera) if str(cabecera).strip().isdigit() else 5.0 * 2**intento
    if exc.code == 429:
        espera = max(espera, ESPERA_429_MINIMA)
    print(f"HTTP {exc.code}: espero {espera:.0f} s antes de reintentar ({intento + 1})", file=sys.stderr)
    return espera


def _json_en_cache(grupo: str, clave: str, url: str, datos: bytes | None = None, cabeceras: dict[str, str] | None = None) -> Any:
    ruta = _cache(grupo, clave)
    guardado = _leer_cache(ruta)
    if guardado is not None:
        return guardado["cuerpo"]
    crudo = _leer_url(url, datos, cabeceras)
    cuerpo = json.loads(crudo.decode("utf-8"))
    if isinstance(cuerpo, dict) and "error" in cuerpo and "query" not in cuerpo and "results" not in cuerpo:
        raise RuntimeError(f"La API rechazó la consulta: {str(cuerpo['error'])[:240]}")
    _guardar_cache(ruta, {"clave": clave, "cuerpo": cuerpo})
    return cuerpo


def _anio(doc: dict[str, Any]) -> int:
    return int(str(doc["release_date"])[:4])


def _nombres(cast: Any) -> list[str]:
    if not isinstance(cast, list):
        return []
    nombres: list[str] = []
    for item in cast:
        if isinstance(item, str):
            nombre = item.strip()
        elif isinstance(item, dict) and isinstance(item.get("name"), str):
            nombre = item["name"].strip()
        else:
            continue
        if len(nombre) >= 3:
            nombres.append(nombre)
    return nombres


def _titulo_de_url(url: str) -> str | None:
    marca = "en.wikipedia.org/wiki/"
    if marca not in url:
        return None
    nombre = urllib.parse.unquote(url.split(marca, 1)[1]).split("#", 1)[0].replace("_", " ").strip()
    if not nombre or nombre.startswith(NAMESPACES):
        return None
    return nombre


def _elegir_titulo(titulos: list[str], doc: dict[str, Any]) -> str:
    if len(titulos) == 1:
        return titulos[0]
    anio = _anio(doc)
    pelicula = str(doc.get("title") or "").casefold()

    def puntaje(titulo: str) -> tuple[int, int, int]:
        hallado = _ANIO_TITULO.search(titulo)
        anio_ok = int(hallado is not None and abs(int(hallado.group(1)) - anio) <= 1)
        contiene = int(bool(pelicula) and pelicula in titulo.casefold())
        return (anio_ok, contiene, -len(titulo))

    return max(titulos, key=puntaje)


def _titulos_wikidata(imdbs: list[str]) -> dict[str, list[str]]:
    pendientes = [imdb for imdb in imdbs if _leer_cache(_cache("wikidata", imdb)) is None]
    for inicio in range(0, len(pendientes), LOTE_SPARQL):
        lote = pendientes[inicio : inicio + LOTE_SPARQL]
        valores = " ".join(f'"{imdb}"' for imdb in lote)
        consulta = (
            "SELECT ?imdb ?article WHERE {\n"
            f"  VALUES ?imdb {{ {valores} }}\n"
            "  ?item wdt:P345 ?imdb .\n"
            "  ?article schema:about ?item ;\n"
            "           schema:isPartOf <https://en.wikipedia.org/> .\n"
            "}\n"
        )
        if any(not re.fullmatch(r"tt\d+", imdb) for imdb in lote):
            raise ValueError(f"imdb_id inesperado en la consulta SPARQL: {lote[:3]}")
        print(f"SPARQL: {len(lote)} imdb_id", file=sys.stderr)
        cuerpo = _json_en_cache(
            "sparql",
            " ".join(lote),
            WIKIDATA,
            consulta.encode("utf-8"),
            {"Content-Type": "application/sparql-query", "Accept": "application/sparql-results+json"},
        )
        if "results" not in cuerpo:
            raise RuntimeError(f"Wikidata no devolvió resultados SPARQL: {str(cuerpo)[:240]}")
        por_imdb: dict[str, list[str]] = {imdb: [] for imdb in lote}
        for fila in cuerpo["results"]["bindings"]:
            imdb = fila["imdb"]["value"]
            titulo = _titulo_de_url(fila["article"]["value"])
            if imdb in por_imdb and titulo and titulo not in por_imdb[imdb]:
                por_imdb[imdb].append(titulo)
        for imdb, titulos in por_imdb.items():
            _guardar_cache(_cache("wikidata", imdb), {"imdb": imdb, "titulos": titulos})

    salida: dict[str, list[str]] = {}
    for imdb in imdbs:
        guardado = _leer_cache(_cache("wikidata", imdb))
        salida[imdb] = list(guardado["titulos"]) if guardado else []
    return salida


def _pagina_desde_respuesta(cuerpo: dict[str, Any]) -> Pagina | None:
    pages = cuerpo.get("query", {}).get("pages", {})
    valores = list(pages.values()) if isinstance(pages, dict) else list(pages)
    utiles = [p for p in valores if "missing" not in p and p.get("pageid", 0) > 0]
    if not utiles:
        return None
    pagina = utiles[0]
    revisiones = pagina.get("revisions") or []
    if not revisiones or "revid" not in revisiones[0]:
        raise RuntimeError(f"El artículo {pagina.get('title')!r} no trajo revid; no se puede atribuir (NS-5).")
    redirecciones = cuerpo.get("query", {}).get("redirects") or []
    seccion = next((str(r["tofragment"]) for r in redirecciones if r.get("tofragment")), None)
    return Pagina(str(pagina["title"]), str(pagina.get("extract") or ""), int(revisiones[0]["revid"]), seccion, bool(redirecciones))


def pagina(titulo: str) -> Pagina | None:
    """Descarga (o relee de la caché) el extracto en texto plano y el `revid`."""
    params = {
        "action": "query",
        "format": "json",
        "redirects": "1",
        "prop": "extracts|revisions",
        "explaintext": "1",
        "rvprop": "ids",
        "titles": titulo,
    }
    url = WIKIPEDIA + "?" + urllib.parse.urlencode(params)
    cuerpo = _json_en_cache("enwiki", titulo, url)
    return _pagina_desde_respuesta(cuerpo)


def _anio_compatible(titulo: str, extracto: str, anio: int) -> bool:
    explicito = _ANIO_TITULO.search(titulo)
    if explicito:
        return abs(int(explicito.group(1)) - anio) <= 1
    entrada = extracto.split("\n", 1)[0]
    hallado = _ANIO_TEXTO.search(entrada)
    return hallado is not None and abs(int(hallado.group(1)) - anio) <= 1


def _nombres_presentes(nombres: list[str], extracto: str) -> int:
    texto = extracto.casefold()
    vistos = 0
    ya: set[str] = set()
    for nombre in nombres:
        clave = nombre.casefold()
        if clave in ya:
            continue
        if re.search(r"(?<!\w)" + re.escape(clave) + r"(?!\w)", texto):
            ya.add(clave)
            vistos += 1
            if vistos >= 2:
                break
    return vistos


_VACIAS = frozenset({"the", "and", "a", "an", "of", "in", "on", "to", "for", "with", "her", "his", "film", "movie"})


def _palabras_de_titulo(titulo: str) -> set[str]:
    sin_parentesis = re.sub(r"\s*\([^)]*\)\s*$", "", titulo).casefold().replace("&", " and ")
    return {p for p in re.findall(r"\w+", sin_parentesis) if p not in _VACIAS}


def titulo_compatible(articulo: str, pelicula: str) -> bool:
    """True si el título del artículo contiene todas las palabras del de la película (sin artículos ni paréntesis).

    Revisión R-11: la búsqueda aceptó «Barbie Dolphin Magic» (2017) para *Barbie & Her Sisters in a Puppy Chase*
    (2016), porque el año cabe en ±1 y el reparto de voces es el mismo, y «Joker (The Dark Knight)», un artículo de
    personaje, para *Batman Unmasked*. Revisión R-12: tampoco vale al revés, un artículo cuyo título es solo una parte
    del de la película, como «X-Men (film)» (2000) para *X-Men: The Mutant Watch* (2001), que es otra obra de la saga.
    Solo se exige en la búsqueda por título: P345 ya identifica la película.
    """
    a, p = _palabras_de_titulo(articulo), _palabras_de_titulo(pelicula)
    return bool(a) and bool(p) and p <= a


def _buscar_por_titulo(doc: dict[str, Any], detalle: list[str]) -> str | None:
    nombres = _nombres(doc.get("cast"))
    if len(nombres) < 2:
        detalle.append("sin imdb_id en Wikidata y con menos de 2 nombres de reparto: NS-4 no permite la búsqueda")
        return None
    anio = _anio(doc)
    consulta = f"{doc.get('title')} {anio} film"
    params = {
        "action": "query",
        "format": "json",
        "list": "search",
        "srnamespace": "0",
        "srlimit": "5",
        "srsearch": consulta,
    }
    url = WIKIPEDIA + "?" + urllib.parse.urlencode(params)
    cuerpo = _json_en_cache("busqueda", consulta, url)
    candidatos = [item["title"] for item in cuerpo.get("query", {}).get("search", [])]
    motivos: list[str] = []
    for titulo in candidatos:
        pag = pagina(titulo)
        if pag is None:
            motivos.append(f"{titulo}: no existe")
            continue
        if not _anio_compatible(pag.titulo, pag.extracto, anio):
            motivos.append(f"{pag.titulo}: el año no coincide ±1")
            continue
        if _nombres_presentes(nombres, pag.extracto) < 2:
            motivos.append(f"{pag.titulo}: no nombra 2 integrantes del reparto")
            continue
        if pag.seccion:
            motivos.append(f"{pag.titulo}: redirige a la sección «{pag.seccion}» de otro artículo")
            continue
        if not titulo_compatible(pag.titulo, str(doc.get("title") or "")):
            motivos.append(f"{pag.titulo}: el título no es el de la película")
            continue
        detalle.append(f"búsqueda por título aceptada: {pag.titulo}")
        return pag.titulo
    detalle.append(f"búsqueda «{consulta}» sin artículo válido ({'; '.join(motivos) or 'sin candidatos'})")
    return None


def buscar(ids: Iterable[Any], docs: dict[str, dict[str, Any]] | None = None, detalles: dict[str, str] | None = None) -> dict[str, str]:
    """Devuelve `{id: título del artículo}` solo para las películas que NS-4 acepta."""
    if docs is None:
        docs = catalogo.cargar()
    pedidos = [str(i) for i in ids]
    faltan = [i for i in pedidos if i not in docs]
    if faltan:
        raise KeyError(f"No están en el catálogo: {', '.join(faltan[:8])}")

    con_imdb = [i for i in pedidos if re.fullmatch(r"tt\d+", str(docs[i].get("imdb_id") or ""))]
    por_imdb = _titulos_wikidata([str(docs[i]["imdb_id"]) for i in con_imdb]) if con_imdb else {}
    articulos: dict[str, str] = {}
    for doc_id in pedidos:
        doc = docs[doc_id]
        notas: list[str] = []
        imdb = str(doc.get("imdb_id") or "")
        titulos = por_imdb.get(imdb, []) if re.fullmatch(r"tt\d+", imdb) else []
        if titulos:
            elegido = _elegir_titulo(titulos, doc)
            articulos[doc_id] = elegido
            notas.append(f"P345 {imdb} → {elegido}")
        else:
            if imdb:
                notas.append(f"{imdb} no tiene artículo en en.wikipedia.org vía P345")
            else:
                notas.append("sin imdb_id")
            titulo = _buscar_por_titulo(doc, notas)
            if titulo:
                articulos[doc_id] = titulo
        if detalles is not None:
            detalles[doc_id] = "; ".join(notas)
    return articulos


def _es_fin_oracion(texto: str, indice: int) -> bool:
    signo = texto[indice]
    if signo not in ".!?":
        return False
    if signo == ".":
        anterior = texto[indice - 1] if indice else ""
        siguiente = texto[indice + 1] if indice + 1 < len(texto) else ""
        if anterior.isdigit() and siguiente.isdigit():
            return False
        # Inicial o sigla: «A.», «U.S.» y «U.S.A.». La letra anterior al punto va sola.
        if anterior.isupper():
            previo = texto[indice - 2] if indice >= 2 else ""
            if not previo.isalpha():
                return False
        if _ABREVIATURA.search(texto[max(0, indice - 8) : indice + 1]):
            return False
    if indice + 1 < len(texto) and texto[indice + 1] not in " \t\n\"'”’":
        return False
    return True


def _limpiar(texto: str) -> str:
    texto = texto.replace("\u00a0", " ").replace("\u2009", " ").replace("\u202f", " ")
    texto = texto.replace("'''", "").replace("''", "")
    texto = _CITA.sub("", texto)
    texto = re.sub(r"<[^>\n]{1,80}>", "", texto)
    lineas: list[str] = []
    en_blanco = False
    for linea in texto.splitlines():
        linea = re.sub(r"[ \t]+", " ", linea).strip()
        if not linea:
            if lineas and not en_blanco:
                lineas.append("")
            en_blanco = True
            continue
        lineas.append(linea)
        en_blanco = False
    while lineas and lineas[-1] == "":
        lineas.pop()
    return "\n".join(lineas).strip()


def _recortar(texto: str) -> str | None:
    if len(texto) < LARGO_MIN:
        return None
    if len(texto) <= LARGO_MAX:
        return texto
    ultimo: int | None = None
    for indice in range(min(LARGO_MAX, len(texto))):
        if _es_fin_oracion(texto, indice):
            ultimo = indice
    if ultimo is None or ultimo + 1 < LARGO_MIN:
        return None
    fin = ultimo + 1
    # Una oración que termina con cita cierra la comilla: «…said "Go home."»
    while fin < len(texto) and fin < LARGO_MAX and texto[fin] in "\"'”’":
        fin += 1
    return texto[:fin].rstrip()


def preparar_texto(texto: str | None) -> str | None:
    """Quita referencias y espacios, y aplica el largo de NS-3. `None` si no sirve."""
    if not isinstance(texto, str):
        return None
    return _recortar(_limpiar(texto))


def cita_visible(texto: str) -> bool:
    """True si quedó una referencia de las que NS-2 manda quitar."""
    return _CITA_VISIBLE.search(texto) is not None


def nombre_de_seccion(encabezado: str) -> str:
    """Encabezado sin paréntesis ni puntuación final: «Synopsis (extended version)» → «synopsis» (R-06)."""
    sin_parentesis = re.sub(r"\s*\([^)]*\)", "", encabezado)
    return sin_parentesis.strip().rstrip(":.").strip().casefold()


def secciones_de_trama(extracto: str) -> list[str]:
    """Textos de las secciones de trama de nivel 2, en el orden del artículo."""
    marcas = list(_ENCABEZADO.finditer(extracto))
    secciones: list[str] = []
    for indice, marca in enumerate(marcas):
        if len(marca.group(1)) != 2:
            continue
        if nombre_de_seccion(marca.group(2)) not in SECCIONES:
            continue
        fin = len(extracto)
        for siguiente in marcas[indice + 1 :]:
            if len(siguiente.group(1)) == 2:
                fin = siguiente.start()
                break
        # Los subtítulos de nivel 3 o más («=== Setting ===») se quitan; su texto sigue siendo parte de la trama.
        secciones.append(_ENCABEZADO.sub("", extracto[marca.end() : fin]))
    return secciones


def texto_de_extracto(extracto: str) -> str | None:
    """Primera sección de trama que, ya limpia, cumple NS-3."""
    for seccion in secciones_de_trama(extracto):
        texto = preparar_texto(seccion)
        if texto:
            return texto
    return None


_NO_PELICULA = re.compile(
    r"(?i)\b(?:television series|TV series|web series|video game|manga series|anime series|light novel series|miniseries)\b"
)
# Solo el singular: un artículo que se presenta como «trilogy of anime films» o «group of films and television series»
# trata de varias películas y su trama no es la de una sola (R-07: Psycho-Pass: Sinners of the System, Case.2).
_PELICULA = re.compile(r"(?i)\b(?:film|movie)\b")


# El artículo se presenta como obra en dos partes: «Che is a two-part 2008…» o «Red Cliff … is a 2008–09 … film» (R-10),
# o como trilogía o saga: «Berserk: The Golden Age Arc … is a Japanese anime film trilogy» (R-11). Los adjetivos entre
# «is a» y «trilogy» no pueden ser «of», «to», «in»…: «is a reboot of the Spider-Man film series» es una sola película.
_VARIAS_PARTES = re.compile(
    r"(?i)\bis an? (?:(?:[\w'’-]+\s){0,4}?two-part\b|(?:19|20)\d{2}\s*[–-]\s*\d{2,4}\b"
    r"|(?:(?!(?:of|to|in|from|the|and|for|with)\b)[\w'’-]+\s){0,5}(?:trilogy|duology|tetralogy|film series)\b)"
)


def es_articulo_de_varias_partes(extracto: str) -> bool:
    """True si la primera línea presenta el artículo como obra en dos partes, trilogía o saga, cuya trama abarca varias."""
    return bool(_VARIAS_PARTES.search(extracto.split("\n", 1)[0][:400]))


def es_articulo_de_obra_no_filmica(extracto: str) -> bool:
    """True si la primera línea presenta el artículo como serie, videojuego o manga y no habla de una película.

    Revisión R-05: el `imdb_id` de *Mobile Suit Gundam I* (1981) llevó al artículo de la serie de televisión de
    1979, cuya trama abarca las tres películas. Una novela no cuenta: su adaptación suele contar la misma historia.
    """
    primera = extracto.split("\n", 1)[0][:400]
    return bool(_NO_PELICULA.search(primera)) and not _PELICULA.search(primera)


def trama(articulo: str, anio: int | None = None) -> Trama | None:
    """`(texto, revid, titulo)` de la sección de trama, o `None` si no hay una usable.

    Con `anio`, una redirección a otro artículo solo se acepta si el año de ese artículo cabe en ±1 (R-13a: *Prancer: A
    Christmas Tale*, de 2022, redirige a «Prancer (film)», la película de 1989).
    """
    pag = pagina(articulo)
    if pag is None:
        return None
    if pag.seccion:
        print(f"{articulo}: redirige a la sección «{pag.seccion}» de {pag.titulo}; la película no tiene artículo propio", file=sys.stderr)
        return None
    if anio is not None and pag.redirigida and not _anio_compatible(pag.titulo, pag.extracto, anio):
        print(f"{articulo}: redirige a {pag.titulo}, cuyo año no coincide ±1 con {anio}; es otra obra", file=sys.stderr)
        return None
    if es_articulo_de_obra_no_filmica(pag.extracto):
        print(f"{pag.titulo}: el artículo es de una serie, un videojuego o un manga, no de la película", file=sys.stderr)
        return None
    if es_articulo_de_varias_partes(pag.extracto):
        print(f"{pag.titulo}: el artículo es de una obra en varias partes; su trama abarca más de una", file=sys.stderr)
        return None
    texto = texto_de_extracto(pag.extracto)
    if texto is None:
        return None
    return Trama(texto, pag.revid, pag.titulo)


def comprobar_texto() -> None:
    """Comprueba el corte, las citas y los encabezados sin usar la red."""
    _exigir(preparar_texto("A" * 39) is None, "Un texto de 39 caracteres no debe servir")
    _exigir(preparar_texto("A" * 40) == "A" * 40, "Un texto de 40 caracteres debe conservarse")

    citado = "In 1996, a treasure hunter [1] finds the wreck [citation needed]. He keeps the map [a]."
    limpio = preparar_texto(citado + (" The voyage goes on across the ocean." * 3))
    _exigir(limpio is not None and limpio.startswith("In 1996, a treasure hunter finds the wreck."), "Las citas deben desaparecer sin pegar palabras")
    _exigir(limpio is not None and not cita_visible(limpio), "No debe quedar [n], [a] ni [citation needed]")

    base_marina = "The crew left the harbor before dawn and never came back."
    marina = preparar_texto(base_marina + (" The U.S. " * 400))
    _exigir(marina == base_marina, f"El corte cayó en la abreviatura U.S.: {marina[:80] if marina else None!r}")

    base_doctor = "The doctor waited by the dock until the ship was ready."
    doctor = preparar_texto(base_doctor + (" Dr. " * 800))
    _exigir(doctor == base_doctor, f"El corte cayó en Dr.: {doctor[:80] if doctor else None!r}")

    base_decimal = "They agreed on the plan and left the harbor together."
    decimal = preparar_texto(base_decimal + (" 3.14" * 700))
    _exigir(decimal == base_decimal, f"El corte cayó en un decimal: {decimal[:80] if decimal else None!r}")

    largo = ("A" * 100) + ". " + ("B" * 4000)
    _exigir(preparar_texto(largo) == ("A" * 100) + ".", "Hay que cortar en el último punto dentro de 3 000")
    _exigir(len(preparar_texto(("Sentence number one stays here. " * 200)) or "") <= LARGO_MAX, "El resultado no puede pasar de 3 000")

    premise = "== Premise ==\nToo short.\n\n== Plot ==\n" + ("In 1996, the treasure hunter sails west. " * 4) + "\n\n== Cast ==\nNobody."
    _exigir(texto_de_extracto(premise).startswith("In 1996, the treasure hunter sails west."), "Si Premise no llega a 40, se usa Plot")
    desarrollo = "== Story development ==\n" + ("This heading is not a plot section at all. " * 5)
    _exigir(texto_de_extracto(desarrollo) is None, "Story development no es sinónimo de Story")
    extendida = "== Synopsis (extended version) ==\n" + ("The crew of the Pegasus fights the Cylons. " * 3)
    _exigir(texto_de_extracto(extendida) is not None, "«Synopsis (extended version)» es una sección de trama")
    resumen = "== Summary ==\n" + ("The documentary follows the impostor across Spain. " * 3)
    _exigir(texto_de_extracto(resumen) is not None, "«Summary» es una sección de trama")
    sub = "=== Plot ===\n" + ("A level-three heading is not the plot section. " * 4)
    _exigir(texto_de_extracto(sub) is None, "Solo valen los encabezados de nivel 2")
    anidada = "== Plot ==\n=== Setting ===\n" + ("The region is full of creatures. " * 3) + "\n=== Story ===\n" + ("The hero sets out on a long journey. " * 3)
    limpia = texto_de_extracto(anidada)
    _exigir(limpia is not None and "===" not in limpia and "The hero sets out" in limpia, f"Los subtítulos de nivel 3 no deben quedar en la trama: {limpia!r}")
    serie = "Mobile Suit Gundam (Japanese: ...) is a Japanese anime television series produced by Nippon Sunrise.\n\n== Plot =="
    _exigir(es_articulo_de_obra_no_filmica(serie), "El artículo de una serie de TV no es el de la película")
    pelicula = "Titanic is a 1997 American epic romantic disaster film directed by James Cameron.\n\n== Plot =="
    _exigir(not es_articulo_de_obra_no_filmica(pelicula), "El artículo de una película debe aceptarse")
    trilogia = ("Psycho-Pass: Sinners of the System is a trilogy of Japanese anime films based on characters who "
                "appear in the Psycho-Pass television series.\n\n== Plot ==")
    _exigir(es_articulo_de_obra_no_filmica(trilogia), "El artículo de una trilogía no es el de una de sus películas")
    for dos_partes in ("Che is a two-part 2008 epic biographical film about Ernesto Guevara.",
                       "Red Cliff, also known as Chibi, is a 2008–09 internationally co-produced epic war film."):
        _exigir(es_articulo_de_varias_partes(dos_partes), f"Obra en dos partes no detectada: {dos_partes}")
    for saga in ("Berserk: The Golden Age Arc (Japanese: ...) is a Japanese anime film trilogy, which adapts the manga.",
                 "Kizumonogatari is a Japanese anime film trilogy based on the novel by Nisio Isin.",
                 "Digimon Adventure tri. is a Japanese adventure anime film series.",
                 "Batman: The Long Halloween is a 2021 American two-part animated direct-to-video superhero film.",
                 "Sailor Moon Eternal is a 2021 Japanese two-part animated action fantasy film."):
        _exigir(es_articulo_de_varias_partes(saga), f"Trilogía o saga no detectada: {saga}")
    for una in ("The Amazing Spider-Man is a 2012 superhero film. It is a reboot of the Spider-Man film series.",
                "Deadpool is a 2016 superhero film. It is a spin-off of the X-Men film series.",
                "Bionicle: The Legend Reborn is a 2009 animated film. It is a stand-alone sequel to the trilogy."):
        _exigir(not es_articulo_de_varias_partes(una), f"Una película de una saga es una sola película: {una}")
    for una in ("The Wind That Shakes the Barley is a 2006 war drama film set during the Irish War (1919–1921).",
                "Dragonball Evolution is a 2009 superhero film based on the 1984–95 manga series Dragon Ball."):
        _exigir(not es_articulo_de_varias_partes(una), f"Un período histórico no es una obra en dos partes: {una}")
    redireccion = {"query": {"redirects": [{"from": "Jarhead 2: Field of Fire", "to": "Jarhead (film)", "tofragment": "Sequels"}],
                             "pages": {"1": {"pageid": 1, "title": "Jarhead (film)", "extract": "Jarhead is a 2005 film.",
                                             "revisions": [{"revid": 7}]}}}}
    _exigir(_pagina_desde_respuesta(redireccion).seccion == "Sequels", "Una redirección a una sección debe notarse")
    directa = {"query": {"pages": {"1": {"pageid": 1, "title": "Jarhead (film)", "extract": "", "revisions": [{"revid": 7}]}}}}
    _exigir(_pagina_desde_respuesta(directa).seccion is None, "Un artículo pedido por su nombre no es una redirección")
    _exigir(not _pagina_desde_respuesta(directa).redirigida and _pagina_desde_respuesta(redireccion).redirigida, "redirigida marca las redirecciones")
    for articulo, pelicula, esperado in (
        ("Barbie Dolphin Magic", "Barbie & Her Sisters in a Puppy Chase", False),
        ("Joker (The Dark Knight)", "Batman Unmasked: The Psychology of the Dark Knight", False),
        ("Malibu Rescue", "Malibu Rescue", True),
        ("Why We Vote (2022 film)", "Why We Vote", True),
        ("Hit and Run (2012 film)", "Hit & Run", True),
        ("X-Men (film)", "X-Men: The Mutant Watch", False),
        ("Monsters vs. Aliens: Mutant Pumpkins from Outer Space", "Mutant Pumpkins from Outer Space", True),
    ):
        _exigir(titulo_compatible(articulo, pelicula) is esperado, f"titulo_compatible({articulo!r}, {pelicula!r}) debe ser {esperado}")
    adaptacion = "Kamikaze Girls is a 2002 Japanese novel written by Novala Takemoto.\n\n== Plot =="
    _exigir(not es_articulo_de_obra_no_filmica(adaptacion), "Una novela adaptada se acepta")
    cita = ('She tells him "Go home." ' * 2) + ("X" * 3100)
    _exigir(preparar_texto(cita) == 'She tells him "Go home." She tells him "Go home."', f"El corte debe cerrar la comilla: {preparar_texto(cita)!r}")


def verificar() -> int:
    """Aceptación de T-05: Titanic (597) y una película sin artículo."""
    comprobar_texto()
    print("Parser NS-2/NS-3: OK (citas, abreviaturas, límite de 3 000 y encabezados).")
    docs = catalogo.cargar()
    detalles: dict[str, str] = {}
    articulos = buscar([ID_TITANIC, ID_SIN_ARTICULO], docs, detalles)
    _exigir(articulos.get(ID_TITANIC) == "Titanic (1997 film)", f"597 resolvió {articulos.get(ID_TITANIC)!r}. {detalles.get(ID_TITANIC)}")
    resultado = trama(articulos[ID_TITANIC])
    _exigir(resultado is not None, "597 no devolvió sección de trama")
    assert resultado is not None
    _exigir(resultado.texto.startswith("In 1996,"), f"La trama no empieza en 1996: {resultado.texto[:160]!r}")
    _exigir("treasure" in resultado.texto[:200].casefold(), f"La trama no abre con la búsqueda del tesoro: {resultado.texto[:200]!r}")
    _exigir(LARGO_MIN <= len(resultado.texto) <= LARGO_MAX, f"Largo {len(resultado.texto)} fuera de NS-3")
    _exigir(not cita_visible(resultado.texto), "La trama de 597 todavía tiene referencias")
    _exigir(resultado.revid > 0 and resultado.titulo == "Titanic (1997 film)", f"Revisión inesperada: {resultado}")
    print(
        f"597 {resultado.titulo} revid={resultado.revid} caracteres={len(resultado.texto)} "
        f"fin={resultado.texto[-1]!r}"
    )
    print(f"    inicio: {resultado.texto[:180]}")
    print(f"    {detalles.get(ID_TITANIC)}")

    _exigir(ID_SIN_ARTICULO not in articulos, f"{ID_SIN_ARTICULO} no debía tener artículo: {articulos.get(ID_SIN_ARTICULO)}")
    doc = docs[ID_SIN_ARTICULO]
    print(f"{ID_SIN_ARTICULO} {doc.get('title')}: sin fuente")
    print(f"    {detalles.get(ID_SIN_ARTICULO)}")
    print("Verificación T-05: OK (597 con la búsqueda del tesoro de 1996, sin [n], ≤ 3 000; 1031353 sin fuente).")
    return 0


def main() -> None:
    import argparse

    parser = argparse.ArgumentParser(description="Busca la trama de Wikipedia de una película del catálogo (WP-00 T-05).")
    parser.add_argument("--verificar", action="store_true", help="Aceptación de T-05: 597 y una película sin artículo.")
    parser.add_argument("--id", action="append", dest="ids", help="Id de película. Puede repetirse.")
    args = parser.parse_args()
    if args.verificar:
        try:
            sys.exit(verificar())
        except (AssertionError, RuntimeError, KeyError) as exc:
            print(f"ERROR: {exc}", file=sys.stderr)
            sys.exit(1)
    if not args.ids:
        parser.print_help()
        sys.exit(1)
    docs = catalogo.cargar()
    detalles: dict[str, str] = {}
    articulos = buscar(args.ids, docs, detalles)
    for doc_id in [str(i) for i in args.ids]:
        print(f"{doc_id} {docs[doc_id].get('title')}: {detalles.get(doc_id)}")
        titulo = articulos.get(doc_id)
        if not titulo:
            print("    sin fuente")
            continue
        resultado = trama(titulo)
        if resultado is None:
            print(f"    artículo {titulo}: sin sección de trama usable")
            continue
        print(f"    {resultado.titulo} revid={resultado.revid} caracteres={len(resultado.texto)}")
        print(f"    {resultado.texto[:180]}")


if __name__ == "__main__":
    main()

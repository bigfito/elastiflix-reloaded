"""Cliente mínimo de Elasticsearch para data-quality, de solo lectura salvo staging.

Lee ELASTICSEARCH_URL, ELASTICSEARCH_API_KEY y ELASTIFLIX_INDEX_ALIAS del .env de la raíz del checkout (o del
worktree). Las variables de entorno tienen prioridad, igual que en la aplicación, así que
`ELASTIFLIX_INDEX_ALIAS=elastiflix-dq-staging python dump.py` analiza staging.

Reglas de seguridad (AGENTS.md §3):
- Lectura, siempre: GET y HEAD; POST a _search, _count, _pit, _inference y _rank_eval; DELETE /_pit.
- Escritura, solo con DQ_PERMITIR_ESCRITURA=1 **y** sobre índices cuyo nombre empieza con `elastiflix-dq-staging`.
- `/_aliases` y cualquier otra cosa: rechazado aquí. La publicación en producción la hace publicar.py, que antes
  exige `aprobada("H-2")`.
"""
import json, os, re, sys, urllib.request, urllib.error, ssl, pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
ENV = ROOT / ".env"
BITACORA = ROOT / "data-quality" / "BITACORA.md"
STAGING_PREFIX = "elastiflix-dq-staging"
PRODUCCION = "elastiflix-reloaded"

cfg = {}
if ENV.exists():
    for line in ENV.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            k, v = line.split("=", 1)
            cfg[k.strip()] = v.strip().strip('"').strip("'")
for k in ("ELASTICSEARCH_URL", "ELASTICSEARCH_API_KEY", "ELASTIFLIX_INDEX_ALIAS", "ELASTIFLIX_INFERENCE_ID"):
    if os.environ.get(k):
        cfg[k] = os.environ[k]
if not cfg.get("ELASTICSEARCH_URL") or not cfg.get("ELASTICSEARCH_API_KEY"):
    sys.exit("Faltan ELASTICSEARCH_URL o ELASTICSEARCH_API_KEY en .env (ver .env.example). No se imprime ningún valor.")
URL = cfg["ELASTICSEARCH_URL"].rstrip("/")
KEY = cfg["ELASTICSEARCH_API_KEY"]
ALIAS = cfg.get("ELASTIFLIX_INDEX_ALIAS", PRODUCCION)
CTX = ssl.create_default_context()
POST_READ = re.compile(r"/(_search|_count|_pit|_inference|_rank_eval)(/|\?|$)")


def es_lectura(method, path):
    if method in ("GET", "HEAD"):
        return True
    if method == "POST" and POST_READ.search(path):
        return True
    return method == "DELETE" and path.split("?")[0].rstrip("/") == "/_pit"


def es_staging(path):
    first = path.lstrip("/").split("/")[0].split("?")[0]
    return first.startswith(STAGING_PREFIX)


def es(method, path, body=None):
    method = method.upper()
    if not es_lectura(method, path):
        if "/_alias" in path or PRODUCCION in path or not es_staging(path):
            raise PermissionError(f"{method} {path}: solo se escribe en índices {STAGING_PREFIX}*. Producción: publicar.py con H-2.")
        if os.environ.get("DQ_PERMITIR_ESCRITURA") != "1":
            raise PermissionError(f"{method} {path}: escritura en staging bloqueada. Requiere H-1 y DQ_PERMITIR_ESCRITURA=1.")
    return _call(method, path, body)


def _call(method, path, body=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(URL + path, data=data, method=method,
                                 headers={"Authorization": "ApiKey " + KEY,
                                          "Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, context=CTX, timeout=120) as r:
            return json.loads(r.read() or b"null")
    except urllib.error.HTTPError as e:
        return {"__error__": e.code, "body": e.read().decode()[:2000]}


def aprobada(ident):
    """True si la fila `ident` (H-1, H-2, D-xx) de BITACORA.md §3 tiene un estado que empieza con APROBADA y una fecha."""
    for line in BITACORA.read_text(encoding="utf-8").splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if cells and cells[0] == ident:
            estado = cells[-1]
            return estado.startswith("APROBADA") and bool(re.search(r"\d{4}-\d{2}-\d{2}", estado))
    return False


if __name__ == "__main__":
    # Uso: python es.py GET "/elastiflix-reloaded/_count"   (en Git Bash: MSYS_NO_PATHCONV=1)
    m = sys.argv[1]; p = sys.argv[2]
    b = json.loads(sys.argv[3]) if len(sys.argv) > 3 else None
    print(json.dumps(es(m, p, b), indent=1, ensure_ascii=False)[:20000])

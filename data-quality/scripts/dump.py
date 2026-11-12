"""Deja en work/docs.json las películas que se van a analizar.

- Por defecto las descarga del alias ELASTIFLIX_INDEX_ALIAS (PIT y search_after) y guarda el mapping vivo en OUT.
- Con DQ_FUENTE=data (analizar.py --desde-data) las lee de data/elastiflix-reloaded_0*.json, sin red: sirve para
  medir un cambio a data/ antes de cargarlo en staging. En ese modo no hay mapping vivo ni vectores.
"""
import sys; sys.stdout.reconfigure(encoding="utf-8")
import os, pathlib, json
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from paths import WORK, OUT, REPO
os.chdir(WORK)

if os.environ.get("DQ_FUENTE") == "data":
    docs = []
    for part in sorted((REPO / "data").glob("elastiflix-reloaded_0*.json")):
        docs += json.load(open(part, encoding="utf-8"))["documents"]
    (OUT / "live_mapping.json").unlink(missing_ok=True)
    print("fuente: data/ ·", len(docs), "películas")
else:
    from es import es, ALIAS
    pit = es('POST', f'/{ALIAS}/_pit?keep_alive=5m')['id']
    docs, after = [], None
    while True:
        body = {"size": 1000, "pit": {"id": pit, "keep_alive": "5m"},
                "sort": [{"_shard_doc": "asc"}], "_source": True}
        if after:
            body["search_after"] = after
        r = es('POST', '/_search', body)
        hits = r['hits']['hits']
        if not hits:
            break
        for h in hits:
            docs.append({"_id": h["_id"], **h["_source"]})
        after = hits[-1]["sort"]
        pit = r.get("pit_id", pit)
    es('DELETE', '/_pit', {"id": pit})
    live = es('GET', f'/{ALIAS}/_mapping')
    json.dump(live[next(iter(live))]['mappings'], open(OUT / 'live_mapping.json', 'w', encoding='utf-8'), indent=1)
    print('fuente: índice', next(iter(live)), '· alias', ALIAS, '·', len(docs), 'películas')
json.dump(docs, open('docs.json', 'w', encoding='utf-8'), ensure_ascii=False)
print('claves:', sorted({k for d in docs for k in d}))

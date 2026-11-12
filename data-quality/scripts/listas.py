"""Congela las listas de trabajo de cada WP del plan en data-quality/hallazgos/listas/.

Se corrió una vez, al crear el plan (2026-11-12), sobre el índice medido. Las listas son el alcance de cada WP:
no se regeneran durante la ejecución, para que el alcance no cambie a mitad del trabajo. Para ver el estado
actual del catálogo usa analizar.py, que escribe en salidas/ y no toca listas/.

Uso: python data-quality/scripts/listas.py [--forzar]
"""
import sys; sys.stdout.reconfigure(encoding="utf-8")
import csv, json, pathlib, re, collections
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from paths import WORK, M0 as SALIDAS, HALLAZGOS

OUT = HALLAZGOS / "listas"
if OUT.exists() and any(OUT.iterdir()) and "--forzar" not in sys.argv:
    sys.exit("listas/ ya existe y es el alcance congelado del plan. Usa --forzar solo con una decisión anotada en BITACORA.md.")
OUT.mkdir(exist_ok=True)

docs = json.load(open(WORK / "docs.json", encoding="utf-8"))
by = {d["id"]: d for d in docs}
rows = list(csv.DictReader(open(HALLAZGOS / "hallazgos.csv", encoding="utf-8-sig")))
cat = collections.defaultdict(list)
for r in rows:
    cat[r["categoria"]].append(r)
llm = json.load(open(SALIDAS / "llm_ids.json"))


def ids_of(*cats):
    return sorted({r["id"] for c in cats for r in cat[c]}, key=int)


def write(name, ids, header):
    lines = [f"# {header}", "# id\ttítulo (año)"] + [f"{i}\t{by[i]['title']} ({by[i]['release_date'][:4]})" for i in ids]
    (OUT / name).write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"{name:34} {len(ids):5}")


wp01 = ids_of("plot_respuesta_de_asistente", "plot_dato_inventado_verificado")
write("wp01-plots-criticos.tsv", wp01, "WP-01 · respuestas de asistente y datos inventados verificados")

year = ids_of("plot_correccion_del_modelo")
essay = sorted((set(ids_of("plot_ensayo_generado")) | set(year)) - set(wp01), key=int)
write("wp02-ensayos.tsv", essay, "WP-02 · ensayos generados (2+ firmas) y correcciones de año; primero las de wp02-prioridad")
prio = [i for i in essay if i in set(year)]
judged = set()
for line in open(HALLAZGOS.parent.parent / "eval" / "judgments.jsonl", encoding="utf-8"):
    judged.add(json.loads(line)["target"])
prio += [i for i in essay if i in judged and i not in prio]
write("wp02-prioridad.tsv", prio, "WP-02 · primero: correcciones de año y objetivos del harness")
write("wp02-revisar-reparto.tsv", ids_of("plot_reparto_no_coincide_revisar"), "WP-02/WP-05 · el plot nombra protagonistas que no están en cast (heurística, revisar a mano)")

done = set(wp01) | set(essay)
tmdb = {d["id"] for d in docs if (d.get("enrichment") or {}).get("plot") == "tmdb_overview"}
enc = sorted((set(by) - done - tmdb), key=int)
write("wp05-enciclopedicos.tsv", enc, "WP-05 · plot de tipo enciclopédico sin etiqueta (ficha, taquilla y crítica mezcladas)")
one_sig = sorted(set(llm["any"]) - set(llm["strong"]) - set(wp01), key=int)
write("wp05-una-firma.tsv", one_sig, "WP-05 · subconjunto con 1 firma de texto generado: revisar con más cuidado")

short = sorted([d["id"] for d in docs if len(d["plot"]) < 200], key=lambda i: (len(by[i]["plot"]), int(i)))
write("wp06-cortos.tsv", short, "WP-06 · plot de menos de 200 caracteres, del más corto al más largo")

hyg = ids_of("texto_espacios", "texto_no_NFC", "entidad_html", "idioma_corrupto", "pais_no_canonico", "idioma_contradictorio")
ru = sorted([d["id"] for d in docs if any(x.startswith("P") and x[1:] == "усский" for x in d.get("spoken_languages") or [])], key=int)
write("wp07-higiene.tsv", hyg, "WP-07 · espacios, NBSP, NFC, &amp;, idioma ilegible, país no canónico")
write("wp07-ruso-p-latina.tsv", ru, "WP-07 · «Pусский» con P latina (U+0050)")

write("wp08-palabras-clave.tsv", ids_of("keyword_id_con_otro_nombre"), "WP-08 · películas con una palabra clave cuyo id es de otra palabra")
write("wp08-colecciones.tsv", ids_of("coleccion_id_partido"), "WP-08 · colección con el mismo nombre y otro id: verificar antes de cambiar")

write("wp09-puntuaciones.tsv", ids_of("puntuaciones_divergentes"), "WP-09 · |user_score - vote_average| >= 1")
write("wp09-montos.tsv", ids_of("numero_sospechoso"), "WP-09 · montos imposibles")

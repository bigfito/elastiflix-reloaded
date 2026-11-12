import sys; sys.stdout.reconfigure(encoding="utf-8")
import os, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from paths import WORK, OUT, HALLAZGOS, REPO, M0
os.chdir(WORK)
import json, re, csv, collections, unicodedata
docs = json.load(open('docs.json', encoding='utf-8'))
by = {d['id']: d for d in docs}
L = json.load(open(OUT / 'llm_ids.json'))
rows = []
def add(d, sev, cat, field, detail):
    rows.append({'id': d['id'], 'title': d['title'], 'year': d['release_date'][:4], 'severidad': sev,
                 'categoria': cat, 'campo': field, 'detalle': detail[:300]})
for i in ['671', '799546']:
    add(by[i], 'crítica', 'plot_respuesta_de_asistente', 'plot', by[i]['plot'][:160].replace('\n', ' '))
for d in docs:
    m = re.search(r'\b(?:is|was) a \d{4} film, not \d{4}', d['plot'])
    if m: add(d, 'alta', 'plot_correccion_del_modelo', 'plot', d['plot'][:140].replace('\n', ' '))
confirmed = {'13971': 'Atribuye el reparto a Emma Watson; es Emma Roberts', '9381': 'Gerard Butler y Moon Bloodgood; es Vin Diesel', '10066': 'Ewan McGregor y Hayden Christensen; no actúan', '38408': 'Jennifer Aniston y Paul Rudd, directores Ficarra/Requa; es Heigl/Duhamel, dir. Greg Berlanti', '48340': 'Rhys Ifans y Forest Whitaker; no actúan', '9896': 'Will Ferrell, Jon Heder, Steve Carell; no actúan', '11208': 'Director Phedon Papamichael (fue el fotógrafo); dir. Paul McGuigan', '621707': 'Trama inventada (policía retirado); es horror sobre un shomer en Brooklyn', '245698': '«directed by Gambit»; dir. Edward Zwick', '1450': 'La versión live-action de 2009 tiene el ensayo del anime de 2000', '242828': 'Marnie «not a real person… alter ego»: interpretación errónea'}
for i, t in confirmed.items(): add(by[i], 'alta', 'plot_dato_inventado_verificado', 'plot', t)
for i in L['cast_miss']:
    if i not in confirmed and i != '8398': add(by[i], 'media', 'plot_reparto_no_coincide_revisar', 'plot', 'El plot nombra protagonistas que no están en cast (heurística)')
strong = set(L['strong'])
for i in L['strong']: add(by[i], 'alta', 'plot_ensayo_generado', 'plot', by[i]['plot'][:140].replace('\n', ' '))
for d in docs:
    if len(d['plot']) < 200: add(d, 'media', 'plot_corto', 'plot', f"{len(d['plot'])} caracteres; origen {(d.get('enrichment') or {}).get('plot')}")
# keywords conflicts
kname = collections.defaultdict(collections.Counter)
for d in docs:
    for k in d.get('keywords') or []: kname[k['id']][k['name']] += 1
for d in docs:
    for k in d.get('keywords') or []:
        c = kname[k['id']]
        if len(c) > 1 and c[k['name']] < c.most_common(1)[0][1]:
            add(d, 'media', 'keyword_id_con_otro_nombre', 'keywords', f"id {k['id']} «{k['name']}»; el id es «{c.most_common(1)[0][0]}» en {c.most_common(1)[0][1]} películas")
# text hygiene
for d in docs:
    for f in ('cast',):
        for x in d.get(f) or []:
            if x != x.strip() or '\xa0' in x: add(d, 'baja', 'texto_espacios', f, repr(x))
            if unicodedata.normalize('NFC', x) != x: add(d, 'baja', 'texto_no_NFC', f, x)
    for k in d.get('keywords') or []:
        if k['name'] != k['name'].strip() or '\xa0' in k['name']: add(d, 'baja', 'texto_espacios', 'keywords.name', repr(k['name']))
    for f in ('plot', 'overview', 'original_title'):
        v = d.get(f) or ''
        if '&amp;' in v: add(d, 'baja', 'entidad_html', f, '&amp;')
        if unicodedata.normalize('NFC', v) != v: add(d, 'baja', 'texto_no_NFC', f, v[:60])
    if any(re.fullmatch(r'\?+', x) for x in d.get('spoken_languages') or []): add(d, 'media', 'idioma_corrupto', 'spoken_languages', str(d['spoken_languages']))
    if 'United States' in (d.get('production_countries') or []): add(d, 'baja', 'pais_no_canonico', 'production_countries', 'United States (vs United States of America)')
    if 'No Language' in (d.get('spoken_languages') or []) and len(d['spoken_languages']) > 1: add(d, 'baja', 'idioma_contradictorio', 'spoken_languages', str(d['spoken_languages']))
    if d.get('user_score') is not None and d.get('vote_average') is not None and abs(d['user_score'] - d['vote_average']) >= 1:
        add(d, 'baja', 'puntuaciones_divergentes', 'user_score/vote_average', f"vote_average {d['vote_average']} · user_score {d['user_score']} · vote_count {d['vote_count']}")
for i in ['68555', '543583']: add(by[i], 'baja', 'coleccion_id_partido', 'belongs_to_collection', f"{by[i]['belongs_to_collection']['name']} con id {by[i]['belongs_to_collection']['id']}, distinto al resto de la saga")
add(by['694919'], 'media', 'numero_sospechoso', 'revenue', 'revenue 618 con budget 568443')
add(by['42360'], 'media', 'numero_sospechoso', 'budget', 'budget 10540 con revenue 10539779 (×1000)')
sev = {'crítica': 0, 'alta': 1, 'media': 2, 'baja': 3}
rows.sort(key=lambda r: (sev[r['severidad']], r['categoria'], int(r['id'])))
with open(OUT / 'hallazgos.csv', 'w', encoding='utf-8-sig', newline='') as f:
    w = csv.DictWriter(f, fieldnames=list(rows[0])); w.writeheader(); w.writerows(rows)
c = collections.Counter((r['severidad'], r['categoria']) for r in rows)
for k, v in sorted(c.items(), key=lambda x: sev[x[0][0]]): print(v, k)
print('filas', len(rows), 'películas', len({r['id'] for r in rows}))
print('películas con algún hallazgo alto/crítico:', len({r['id'] for r in rows if sev[r['severidad']] <= 1}))

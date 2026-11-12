import sys; sys.stdout.reconfigure(encoding="utf-8")
import os, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from paths import WORK, OUT, HALLAZGOS, REPO, M0
os.chdir(WORK)
import json, collections, re, statistics as st, datetime as dt, unicodedata
docs = json.load(open('docs.json', encoding='utf-8'))
N = len(docs)
C = collections.Counter
def show(title, items, k=15):
    items = list(items)
    print(f'\n## {title}: {len(items)}')
    for x in items[:k]: print('   ', x)
def lbl(d): return f"{d['id']} «{d['title']}» ({(d.get('release_date') or '')[:4]})"

# ---------- ids
show('_id != id', [lbl(d) for d in docs if d['_id'] != d['id']])
show('id no numérico', [lbl(d) for d in docs if not re.fullmatch(r'\d+', d['id'])])
show('id duplicado', [k for k, v in C(d['id'] for d in docs).items() if v > 1])
imdb = [d for d in docs if d.get('imdb_id')]
show('imdb_id con formato raro', [(lbl(d), d['imdb_id']) for d in imdb if not re.fullmatch(r'tt\d{7,8}', d['imdb_id'])])
dup = {k: v for k, v in C(d['imdb_id'] for d in imdb).items() if v > 1}
show('imdb_id compartido', [(k, [lbl(d) for d in imdb if d['imdb_id'] == k]) for k in dup])

# ---------- revenue vs collection
col = [d for d in docs if d.get('belongs_to_collection')]
print('\n## revenue vs colección')
print('   con colección y revenue null:', sum(1 for d in col if d.get('revenue') is None), '/', len(col))
print('   sin colección y revenue null:', sum(1 for d in docs if not d.get('belongs_to_collection') and d.get('revenue') is None))
print('   con colección y revenue:', sum(1 for d in col if d.get('revenue') is not None))
print('   enrichment.revenue values:', C((d.get('enrichment') or {}).get('revenue') for d in docs).most_common(8))

# ---------- titles
tk = C((d['title'].strip().lower(), (d.get('release_date') or '')[:4]) for d in docs)
show('título+año duplicado', [(k, [d['id'] for d in docs if (d['title'].strip().lower(), (d.get('release_date') or '')[:4]) == k]) for k, v in tk.items() if v > 1])
tt = C(d['title'].strip().lower() for d in docs)
print('   títulos repetidos (cualquier año):', sum(1 for v in tt.values() if v > 1))
en_diff = [(lbl(d), d['original_title']) for d in docs if d['original_language'] == 'en' and d['title'] != d['original_title']]
show('en: title != original_title', en_diff, 25)

# ---------- whitespace / encoding in all strings
def strings(d):
    for k, v in d.items():
        if k in ('_id',): continue
        if isinstance(v, str): yield k, v
        elif isinstance(v, list):
            for x in v:
                if isinstance(x, str): yield k, x
                elif isinstance(x, dict):
                    for kk, vv in x.items():
                        if isinstance(vv, str): yield f'{k}.{kk}', vv
        elif isinstance(v, dict):
            for kk, vv in v.items():
                if isinstance(vv, str): yield f'{k}.{kk}', vv
issues = collections.defaultdict(list)
MOJI = re.compile(r'Ã[\x80-\xbf¡-¿]|â€|Â[\xa0-\xbf ]|ï¿½|�')
for d in docs:
    for k, v in strings(d):
        if v != v.strip(): issues['espacio en extremos'].append((d['id'], k, repr(v[:60])))
        if '  ' in v and k not in ('plot',): issues['doble espacio'].append((d['id'], k, v[:60]))
        if MOJI.search(v): issues['mojibake'].append((d['id'], k, MOJI.search(v).group(), v[:80]))
        if re.search(r'&(amp|quot|#\d+|lt|gt|nbsp);', v): issues['entidad HTML'].append((d['id'], k, v[:80]))
        if re.search(r'<[a-z/][^>]*>', v): issues['etiqueta HTML'].append((d['id'], k, v[:80]))
        if re.search(r'[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]', v): issues['caracter de control'].append((d['id'], k))
        if ' ' in v or '​' in v: issues['nbsp/zwsp'].append((d['id'], k, v[:60]))
        if unicodedata.normalize('NFC', v) != v: issues['no NFC'].append((d['id'], k, v[:60]))
for k, v in issues.items():
    byf = C(x[1] for x in v)
    show(f'{k} (por campo {dict(byf)})', v, 8)

# ---------- dates & status
today = dt.date(2026, 11, 12)
dates = []
for d in docs:
    try: dates.append((dt.date.fromisoformat(d['release_date']), d))
    except Exception: print('   fecha inválida', lbl(d), d['release_date'])
ys = C(x[0].year for x in dates)
print('\n## release_date: min', min(dates, key=lambda x: x[0])[0], 'max', max(dates, key=lambda x: x[0])[0])
print('   por década:', sorted(C(x[0].year // 10 * 10 for x in dates).items()))
print('   status:', C(d['status'] for d in docs))
show('futuras', [(lbl(d), d['release_date'], d['status']) for x, d in dates if x > today])
show('Released con fecha futura', [(lbl(d), d['release_date']) for x, d in dates if x > today and d['status'] == 'Released'])
show('no Released con fecha pasada', [(lbl(d), d['release_date'], d['status']) for x, d in dates if x <= today and d['status'] != 'Released'])
show('fecha 01-01 (posible placeholder)', [lbl(d) for x, d in dates if x.month == 1 and x.day == 1], 5)

# ---------- numerics
def num(f):
    return [(d[f], d) for d in docs if isinstance(d.get(f), (int, float)) and not isinstance(d.get(f), bool)]
for f in ['budget', 'revenue', 'runtime', 'popularity', 'vote_average', 'vote_count', 'user_score']:
    v = num(f); xs = sorted(x for x, _ in v)
    q = lambda p: xs[int(p * (len(xs) - 1))]
    print(f'\n## {f}: n={len(xs)} min={xs[0]} p1={q(.01)} p25={q(.25)} med={q(.5)} p75={q(.75)} p99={q(.99)} max={xs[-1]}')
    print('   ceros:', sum(1 for x in xs if x == 0), ' negativos:', sum(1 for x in xs if x < 0), ' no enteros:', sum(1 for x in xs if x != int(x)))
show('budget < 1000 (¿en millones?)', [(lbl(d), x) for x, d in num('budget') if x < 1000], 30)
show('revenue < 1000', [(lbl(d), x) for x, d in num('revenue') if x < 1000], 30)
show('budget >= 400M', [(lbl(d), x) for x, d in num('budget') if x >= 4e8], 30)
show('revenue >= 2.5B', [(lbl(d), x) for x, d in num('revenue') if x >= 2.5e9], 30)
show('revenue/budget > 500', [(lbl(d), d['budget'], d['revenue']) for d in docs if d.get('budget') and d.get('revenue') and d['revenue'] / d['budget'] > 500], 30)
show('runtime < 40', [(lbl(d), x) for x, d in num('runtime') if x < 40], 30)
show('runtime > 240', [(lbl(d), x) for x, d in num('runtime') if x > 240], 30)
show('vote_average fuera de 0-10', [(lbl(d), x) for x, d in num('vote_average') if not 0 <= x <= 10])
show('user_score fuera de 0-100', [(lbl(d), x) for x, d in num('user_score') if not 0 <= x <= 100])
rel = C()
for d in docs:
    if d.get('user_score') is not None and d.get('vote_average') is not None:
        rel[round(d['user_score'] - d['vote_average'] * 10)] += 1
print('\n## user_score - 10*vote_average (redondeado):', rel.most_common(10))
show('user_score vs vote_average incoherentes (>1 pt)', [(lbl(d), d['vote_average'], d['user_score']) for d in docs if d.get('user_score') is not None and d.get('vote_average') is not None and abs(d['user_score'] - d['vote_average'] * 10) > 1], 20)
show('vote_count bajo (<10) con vote_average', [(lbl(d), d['vote_count'], d['vote_average']) for d in docs if d.get('vote_count') is not None and d['vote_count'] < 10], 10)
show('nulls de votos', [(lbl(d), d.get('vote_average'), d.get('vote_count'), d.get('user_score')) for d in docs if d.get('vote_count') is None or d.get('vote_average') is None or d.get('user_score') is None])

# ---------- booleans / categoricals
print('\n## adult:', C(d['adult'] for d in docs), ' video:', C(d['video'] for d in docs))
show('adult=true', [lbl(d) for d in docs if d['adult']])
show('video=true', [lbl(d) for d in docs if d['video']])
print('\n## rating:', C(d.get('rating') for d in docs).most_common())
print('## original_language:', len(C(d['original_language'] for d in docs)), C(d['original_language'] for d in docs).most_common(40))
show('original_language no ISO-639-1', [(lbl(d), d['original_language']) for d in docs if not re.fullmatch(r'[a-z]{2}', d['original_language'])])
for f in ['genres', 'spoken_languages', 'production_countries']:
    c = C(x for d in docs for x in (d.get(f) or []))
    print(f'\n## {f}: {len(c)} valores'); print('   ', c.most_common(200 if f == 'genres' else 40))
    print('    raros (<=2):', [k for k, v in c.items() if v <= 2][:60])
pc = C(x for d in docs for x in (d.get('production_companies') or []))
print('\n## production_companies:', len(pc), 'top', pc.most_common(10))
low = collections.defaultdict(set)
for k in pc: low[re.sub(r'[^a-z0-9]', '', k.lower())].add(k)
show('compañías casi iguales', [v for v in low.values() if len(v) > 1], 20)
cc = C(x for d in docs for x in (d.get('cast') or []))
print('\n## cast: personas', len(cc), 'top', cc.most_common(8))
print('   largo de cast:', sorted(C(len(d.get('cast') or []) for d in docs).items()))
lowc = collections.defaultdict(set)
for k in cc: lowc[unicodedata.normalize('NFKD', k).encode('ascii', 'ignore').decode().lower().replace('.', '').replace(' ', '')].add(k)
show('nombres de reparto casi iguales', [v for v in lowc.values() if len(v) > 1], 20)
for f in ['genres', 'cast', 'production_companies', 'production_countries', 'spoken_languages']:
    dd = [(lbl(d), f) for d in docs if len(d.get(f) or []) != len(set(d.get(f) or []))]
    if dd: show(f'repetidos dentro de {f}', dd)

# ---------- paths / urls
P = re.compile(r'/[A-Za-z0-9]{20,40}\.(jpg|png|svg)')
for f in ['poster_path', 'backdrop_path']:
    v = [d for d in docs if d.get(f)]
    show(f'{f} formato raro', [(lbl(d), d[f]) for d in v if not P.fullmatch(d[f])])
    dp = {k: c for k, c in C(d[f] for d in v).items() if c > 1}
    show(f'{f} compartido', [(k, [lbl(d) for d in v if d[f] == k]) for k in dp])
show('poster == backdrop', [lbl(d) for d in docs if d.get('poster_path') and d['poster_path'] == d.get('backdrop_path')])
hp = [d for d in docs if d.get('homepage')]
show('homepage no http(s)', [(lbl(d), d['homepage']) for d in hp if not re.match(r'https?://[^\s/]+\.[^\s]+$', d['homepage'])])
print('   homepage http:// (no https):', sum(1 for d in hp if d['homepage'].startswith('http://')))
hd = {k: c for k, c in C(d['homepage'] for d in hp).items() if c > 1}
show('homepage compartido', sorted(hd.items(), key=lambda x: -x[1]), 25)

# ---------- tagline
tg = [d for d in docs if d.get('tagline')]
show('tagline == title', [lbl(d) for d in tg if d['tagline'].strip().lower() == d['title'].lower()])
show('tagline larga (>150)', [(lbl(d), len(d['tagline'])) for d in tg if len(d['tagline']) > 150])
td = {k: c for k, c in C(d['tagline'] for d in tg).items() if c > 1}
show('tagline compartida', sorted(td.items(), key=lambda x: -x[1]), 25)

# ---------- collections
cid = collections.defaultdict(set); cnames = collections.defaultdict(set)
for d in col:
    b = d['belongs_to_collection']
    cid[b.get('id')].add(b.get('name')); cnames[b.get('name')].add(b.get('id'))
print('\n## colecciones:', len(cid), ' películas con colección:', len(col))
show('colección id con varios nombres', [(k, v) for k, v in cid.items() if len(v) > 1])
show('nombre de colección con varios id', [(k, v) for k, v in cnames.items() if len(v) > 1])
print('   claves de colección:', C(tuple(sorted(d['belongs_to_collection'])) for d in col))
print('   colecciones de 1 película:', sum(1 for k in cid if sum(1 for d in col if d['belongs_to_collection']['id'] == k) == 1))
show('colección sin "Collection" en el nombre', sorted({d['belongs_to_collection']['name'] for d in col if 'ollection' not in (d['belongs_to_collection']['name'] or '')}), 20)

# ---------- keywords
kc = C(); kname = collections.defaultdict(set); kid = collections.defaultdict(set)
for d in docs:
    for k in d.get('keywords') or []:
        kc[k.get('id')] += 1; kname[k.get('id')].add(k.get('name')); kid[(k.get('name') or '').lower()].add(k.get('id'))
print('\n## keywords: distintas', len(kc), ' asignaciones', sum(kc.values()), ' por película:', sorted(C(len(d.get('keywords') or []) for d in docs).items())[:5], '...max', max(len(d.get('keywords') or []) for d in docs))
show('keyword id con varios nombres', [(k, v) for k, v in kname.items() if len(v) > 1])
show('keyword nombre con varios id', [(k, v) for k, v in kid.items() if len(v) > 1])
print('   claves keyword:', C(tuple(sorted(k)) for d in docs for k in d.get('keywords') or []))

# ---------- enrichment
en = [d['enrichment'] for d in docs if d.get('enrichment')]
print('\n## enrichment: claves', C(k for e in en for k in e).most_common())
for k in sorted({k for e in en for k in e}):
    print(f'   {k}:', C(e.get(k) for e in en if k in e).most_common(8))
# enrichment key present but field null, or vice versa
inc = C()
for d in docs:
    e = d.get('enrichment') or {}
    for k, v in e.items():
        val = d.get(k)
        if val is None or val == []: inc[(k, 'etiqueta sin valor')] += 1
print('   etiqueta de procedencia sobre un campo vacío:', inc.most_common())

import sys; sys.stdout.reconfigure(encoding="utf-8")
import os, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from paths import WORK, OUT, HALLAZGOS, REPO, M0
os.chdir(WORK)
import json, collections, re
docs = json.load(open('docs.json', encoding='utf-8'))
by = {d['id']: d for d in docs}
C = collections.Counter
def lbl(d): return f"{d['id']} «{d['title']}» ({d['release_date'][:4]})"
def show(t, xs, k=15):
    xs = list(xs); print(f'\n## {t}: {len(xs)}')
    for x in xs[:k]: print('   ', x)

# user_score vs vote_average
diffs = [(round(d['user_score'] - d['vote_average'], 3), d) for d in docs if d.get('user_score') is not None and d.get('vote_average') is not None]
print('## user_score - vote_average: buckets', sorted(C(round(x, 1) for x, _ in diffs).items()))
show('|user_score - vote_average| >= 0.5', sorted([(x, lbl(d), d['vote_average'], d['user_score'], d['vote_count'], (d.get('enrichment') or {}).get('user_score')) for x, d in diffs if abs(x) >= .5], key=lambda t: -abs(t[0])), 40)
show('user_score fallback', [(lbl(d), d['vote_average'], d['user_score']) for d in docs if (d.get('enrichment') or {}).get('user_score') == 'tmdb_api_fallback_doc_vote_average'])

# keyword conflicts
for kid in [12377, 303361, 208349, 10789, 2299, 5968, 12377]:
    rows = [(lbl(d), k['name']) for d in docs for k in d.get('keywords') or [] if k['id'] == kid]
    print(f'\n## keyword {kid}:', C(n for _, n in rows), [r for r in rows if r[1] != C(n for _, n in rows).most_common(1)[0][0]][:6])
    print('    enrichment.keywords de esos:', C((by[r[0].split()[0]].get('enrichment') or {}).get('keywords') for r in rows))
kw_enr = [d for d in docs if (d.get('enrichment') or {}).get('keywords')]
print('\n## keywords enriquecidos: ids > 300000 (recientes en TMDB):', sum(1 for d in kw_enr for k in d['keywords'] if k['id'] > 300000), '/', sum(len(d['keywords']) for d in kw_enr))

# languages / countries
show('spoken_languages raro', [(lbl(d), d['spoken_languages']) for d in docs if any(re.fullmatch(r'\?+', x) for x in d.get('spoken_languages') or [])])
show('No Language junto a otros', [(lbl(d), d['spoken_languages']) for d in docs if 'No Language' in (d.get('spoken_languages') or []) and len(d['spoken_languages']) > 1])
show('"United States" (no "of America")', [(lbl(d), d['production_countries']) for d in docs if 'United States' in (d.get('production_countries') or [])])
show('países históricos', [(lbl(d), d['production_countries']) for d in docs if set(d.get('production_countries') or []) & {'Soviet Union', 'Czechoslovakia', 'Yugoslavia', 'Serbia and Montenegro', 'East Germany'}])
show("original_language 'cn'", [lbl(d) for d in docs if d['original_language'] == 'cn'], 5)
show('rating TV-PG', [lbl(d) for d in docs if d.get('rating') == 'TV-PG'])
lang_map = {'en': 'English', 'ja': '日本語', 'fr': 'Français', 'es': 'Español', 'ko': '한국어/조선말', 'de': 'Deutsch', 'it': 'Italiano', 'zh': '普通话', 'cn': '广州话 / 廣州話', 'ru': 'Pусский', 'hi': 'हिन्दी'}
show('original_language no está en spoken_languages', [(lbl(d), d['original_language'], d['spoken_languages']) for d in docs if d['original_language'] in lang_map and d.get('spoken_languages') and lang_map[d['original_language']] not in d['spoken_languages']], 25)

# collection naming duplicates
for n in ['Death Note Collection', 'Cinderella Collection']:
    print(f'\n## {n}:', [(lbl(d), d['belongs_to_collection']['id']) for d in docs if (d.get('belongs_to_collection') or {}).get('name') == n])
# collection whose members are only this movie but other movies of same franchise exist? skip
# Inuyasha / money plane
for i in ['42360', '694919', '222724', '13648']:
    d = by[i]; print('\n##', lbl(d), {k: d.get(k) for k in ['budget', 'revenue', 'runtime']}, d.get('enrichment'))
# date 01-01 provenance
print('\n## 01-01 dates all:', [lbl(d) for d in docs if d['release_date'][5:] == '01-01'])

import sys; sys.stdout.reconfigure(encoding="utf-8")
import os, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from paths import WORK, OUT, HALLAZGOS, REPO, M0
os.chdir(WORK)
import json, collections, re, unicodedata
docs = json.load(open('docs.json', encoding='utf-8'))
by = {d['id']: d for d in docs}
C = collections.Counter
def lbl(d): return f"{d['id']} «{d['title']}» ({d['release_date'][:4]})"
def show(t, xs, k=15):
    xs = list(xs); print(f'\n## {t}: {len(xs)}')
    for x in xs[:k]: print('   ', x)

SIG = {
 'delves into': r'\bdelves? into\b', 'thought-provoking': r'\bthought-provoking\b',
 'complexities of': r'\bcomplexit(?:y|ies) of\b', 'explores the (themes|complexities|world)': r'\bexplores? the (?:themes?|complexit|world|idea|concept|dark)',
 'revolves around': r'\brevolves? around\b', 'poignant': r'\bpoignant\b', 'heartwarming': r'\bheartwarming\b',
 'Overall, the': r'\bOverall, (?:the|it|this)\b', 'In conclusion': r'\bIn conclusion\b',
 'serves as a': r'\bserves as a (?:reminder|testament|powerful|commentary)', 'testament to': r'\btestament to\b',
 'human nature': r'\bhuman nature\b', 'tapestry': r'\btapestry\b', 'navigate(s) the': r'\bnavigat(?:e|es|ing) the (?:complex|challeng|treacher)',
 'not YEAR / correction': r'\b(?:is|was) a \d{4} film, not \d{4}', 'no match/please provide': r'(?i)\[?no match found\]?|please provide|i don\'t have|i do not have (?:any )?information|not (?:able|unable) to (?:find|locate)|there is no (?:information|record)|could not be found|unable to (?:find|locate|verify)|may be (?:confusing|thinking of)|did you mean|is not a (?:real|known)|i\'m not aware|no (?:such|known) film|fictional film that does not',
 'film is known for': r'\bthe (?:film|movie) is known for\b',
}
hits = collections.defaultdict(set)
for d in docs:
    for k, p in SIG.items():
        if re.search(p, d['plot'], re.I if k != 'no match/please provide' else 0): hits[k].add(d['id'])
print('## firmas de texto de modelo (docs por firma):')
for k in SIG: print(f'   {k:45} {len(hits[k])}')
score = C()
for k, ids in hits.items():
    if k in ('human nature', 'poignant', 'heartwarming', 'testament to'): w = .5
    else: w = 1
    for i in ids: score[i] += w
strong = {i for i, s in score.items() if s >= 2}
anyh = set(score)
print('\n## docs con >=1 firma:', len(anyh), ' con >=2 (alta probabilidad de texto generado):', len(strong))
print('   por origen de plot (enrichment.plot):', C((by[i].get('enrichment') or {}).get('plot') or 'wikipedia/sin etiqueta' for i in strong))
show('no match / please provide / negaciones', [(lbl(by[i]), re.search(SIG['no match/please provide'], by[i]['plot']).group(), by[i]['plot'][:160].replace('\n', ' ')) for i in sorted(hits['no match/please provide'], key=int)], 60)
show('correcciones «not YEAR»', [(lbl(by[i]), by[i]['plot'][:140]) for i in hits['not YEAR / correction']], 30)

# "starring X" names not in cast
def norm(s): return unicodedata.normalize('NFKD', s).encode('ascii', 'ignore').decode().lower()
NAME = r"([A-Z][a-zA-Z'\-\.]+(?: [A-Z][a-zA-Z'\-\.]+){1,3})"
bad = []
for d in docs:
    cast = {norm(c) for c in d.get('cast') or []}
    if not cast: continue
    castlast = {c.split()[-1] for c in cast if c.split()}
    m = re.findall(r'\b(?:starring|stars|starred|featuring|co-starring)\s+((?:' + NAME[1:-1] + r'(?:,\s*|\s+and\s+)?){1,6})', d['plot'])
    names = []
    for grp in m:
        names += re.findall(NAME, grp)
    miss = [n for n in names if norm(n) not in cast and norm(n).split()[-1] not in castlast and len(n.split()) >= 2]
    if names and len(miss) >= 1 and len(miss) / len(names) >= .5:
        bad.append((lbl(d), miss[:4], f'{len(miss)}/{len(names)}'))
show('plot nombra protagonistas que no están en cast (posible alucinación)', bad, 40)
json.dump({'strong': sorted(strong, key=int), 'any': sorted(anyh, key=int), 'nomatch': sorted(hits['no match/please provide'], key=int), 'cast_miss': [b[0].split()[0] for b in bad]}, open(OUT / 'llm_ids.json', 'w'))
# sample strong plots for reading
for i in sorted(strong, key=lambda i: -score[i])[:6]:
    print('\n---', lbl(by[i]), score[i]); print(by[i]['plot'][:900])

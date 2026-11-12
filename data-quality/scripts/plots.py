import sys; sys.stdout.reconfigure(encoding="utf-8")
import os, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from paths import WORK, OUT, HALLAZGOS, REPO, M0
os.chdir(WORK)
import json, collections, re, hashlib
docs = json.load(open('docs.json', encoding='utf-8'))
by = {d['id']: d for d in docs}
C = collections.Counter
def lbl(d): return f"{d['id']} «{d['title']}» ({d['release_date'][:4]})"
def show(t, xs, k=15):
    xs = list(xs); print(f'\n## {t}: {len(xs)}')
    for x in xs[:k]: print('   ', x)

for i in ['603', '105', '196', '165']:
    print(lbl(by[i]), [(k['id'], k['name']) for k in by[i]['keywords']])

L = sorted(len(d['plot']) for d in docs)
q = lambda p: L[int(p * (len(L) - 1))]
print('\n## plot largo: min', L[0], 'p5', q(.05), 'p25', q(.25), 'med', q(.5), 'p75', q(.75), 'p95', q(.95), 'max', L[-1])
W = sorted(len(d['plot'].split()) for d in docs)
print('   palabras: min', W[0], 'med', W[len(W)//2], 'max', W[-1])
print('   < 200 chars:', sum(1 for x in L if x < 200), ' < 100:', sum(1 for x in L if x < 100), ' > 8000:', sum(1 for x in L if x > 8000))
show('más cortos', sorted([(len(d['plot']), lbl(d), d['plot'][:120], (d.get('enrichment') or {}).get('plot')) for d in docs])[:20], 20)
show('más largos', sorted([(len(d['plot']), lbl(d)) for d in docs], reverse=True)[:8])

# plot == overview
same = [d for d in docs if d.get('overview') and d['plot'].strip() == d['overview'].strip()]
print('\n## plot idéntico a overview:', len(same), ' etiquetados tmdb_overview:', sum(1 for d in same if (d.get('enrichment') or {}).get('plot') == 'tmdb_overview'))
tagged = [d for d in docs if (d.get('enrichment') or {}).get('plot') == 'tmdb_overview']
show('etiquetado tmdb_overview pero plot != overview', [(lbl(d), d['plot'][:80], '|', (d.get('overview') or '')[:80]) for d in tagged if d['plot'].strip() != (d.get('overview') or '').strip()], 10)
show('plot == overview sin etiqueta', [lbl(d) for d in same if (d.get('enrichment') or {}).get('plot') != 'tmdb_overview'], 10)
print('   plot desde overview (largo mediano):', sorted(len(d['plot']) for d in tagged)[len(tagged)//2])

# duplicates
fp = C(re.sub(r'\s+', ' ', d['plot'].strip().lower()) for d in docs)
show('plot duplicado exacto', [[lbl(d) for d in docs if re.sub(r'\s+', ' ', d['plot'].strip().lower()) == k] for k, v in fp.items() if v > 1])
fo = C(d['overview'].strip().lower() for d in docs if d.get('overview'))
show('overview duplicado', [[lbl(d) for d in docs if (d.get('overview') or '').strip().lower() == k] for k, v in fo.items() if v > 1])
# shared first sentence / 200-char prefix
pre = collections.defaultdict(list)
for d in docs: pre[d['plot'][:150].lower()].append(lbl(d))
show('mismo inicio (150 chars)', [v for v in pre.values() if len(v) > 1])

# PlotChecks regexes + extras
pat = {
 'ASSISTANT': r"(?i)\bI (?:couldn't|could not|was unable to) find\b|find any information|knowledge cutoff|as of my (?:last )?(?:knowledge|update|training)|\bi'?m sorry\b|\bi apologi[sz]e\b",
 'MARKDOWN': r"(?m)\*\*|^#+ ",
 'THEMES': r"(?im)\bmain themes\b|\bthe (?:main|central|primary|core|overarching|key) theme of\b|^\s*(?:themes|key themes|major themes)\s*:",
 'wiki cita [n]': r"\[\d+\]|\[citation needed\]|\[note \d+\]",
 'wiki encabezado': r"(?im)^=+ ?[A-Z][^=]+=+|^(plot|synopsis|plot summary)\s*$",
 'ficha inicial («X is a 2004 ... film»)': r"^[^\n]{0,200}\bis an? (?:\d{4} )?(?:[A-Z][a-z]+ )*(?:[a-z\-]+ ){0,6}(?:film|movie|documentary|anime)\b",
 'meta (the film/movie ...)': r"(?i)\bthe (?:film|movie) (?:stars|was released|received|grossed|premiered|was directed|is directed|is a \d{4})\b",
 'producción/recepción': r"(?i)\b(box office|critical reception|received (?:mixed|positive|negative) reviews|rotten tomatoes|grossed \$)",
 'dirección (directed by)': r"(?i)\bdirected by\b",
 'en español/otro idioma': r"(?i)\b(?:el|la|los|las|una|del|que|con|para)\b.*\b(?:el|la|los|las|una|del|que|con|para)\b.*\b(?:el|la|los|las|del|que)\b",
 'termina sin puntuación': r"[^.!?\"'”’)\]…]\s*$",
 'URL': r"https?://|www\.",
 'spoiler/notice': r"(?i)spoiler|this article|this section|wikipedia",
 'segunda persona IA': r"(?i)\b(?:here is|here's) (?:a|the) (?:summary|plot|synopsis)\b|\blet me\b|\bin summary\b|\boverall, the (?:film|movie)\b|\bin conclusion\b",
 'lista viñetas': r"(?m)^\s*(?:[-•*]|\d+\.)\s+\S",
}
for k, p in pat.items():
    hits = [(lbl(d), re.search(p, d['plot']).group()[:60]) for d in docs if re.search(p, d['plot'])]
    show(f'plot · {k}', hits, 12)

# non-ascii heavy plots (likely non-English)
na = [(round(sum(1 for c in d['plot'] if ord(c) > 127) / len(d['plot']), 2), lbl(d), d['plot'][:80]) for d in docs]
show('plot con >10% no-ASCII', sorted([x for x in na if x[0] > .1], reverse=True), 15)
# stopword heuristic for Spanish/French/German
SW = {'es': {' el ', ' los ', ' una ', ' del ', ' que ', ' con ', ' por '}, 'fr': {' le ', ' les ', ' une ', ' des ', ' est ', ' dans ', ' pour '}, 'de': {' der ', ' die ', ' und ', ' ist ', ' nicht ', ' mit ', ' ein '}}
for lang, sw in SW.items():
    hits = [(lbl(d), d['plot'][:90]) for d in docs if sum(f' {d["plot"].lower()} '.count(w) for w in sw) > len(d['plot'].split()) * .08]
    show(f'plot que parece {lang}', hits, 8)

# plot mentions title?  plot vs overview proper-noun overlap
def names(t): return {w for w in re.findall(r"\b[A-Z][a-z]{2,}\b", t or '')} - {'The', 'When', 'After', 'His', 'Her', 'But', 'They', 'She', 'And', 'With', 'Meanwhile', 'Later', 'While', 'However', 'Then', 'One', 'This', 'That', 'There', 'Their', 'Now', 'During', 'Back', 'Two', 'Years', 'Soon', 'New', 'Before', 'Once', 'Some', 'What', 'From', 'Its', 'Despite', 'Together', 'Eventually', 'Meanwhile', 'Christmas', 'American', 'World', 'Earth', 'Day', 'Year', 'Life', 'Man', 'King', 'City', 'War'}
low = []
for d in docs:
    if (d.get('enrichment') or {}).get('plot') == 'tmdb_overview' or not d.get('overview'): continue
    o = names(d['overview']); p = names(d['plot'])
    if len(o) >= 3:
        r = len(o & p) / len(o)
        if r == 0: low.append((lbl(d), sorted(o)[:6], d['plot'][:100]))
show('overview con nombres propios que el plot no menciona (¿plot de otra película?)', low, 40)
json.dump([x[0] for x in low], open(OUT / 'suspect_plot_ids.json', 'w'))

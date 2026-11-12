import sys; sys.stdout.reconfigure(encoding="utf-8")
import os, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from paths import WORK, OUT, HALLAZGOS, REPO, M0
os.chdir(WORK)
import sys; sys.stdout.reconfigure(encoding="utf-8"); import json, random, numpy as np
from es import es, ALIAS, cfg
docs = json.load(open('docs.json', encoding='utf-8'))
by = {d['id']: d for d in docs}
ids = [d['id'] for d in docs]
V = {f: {} for f in ('plot_256', 'plot_512', 'plot_1024')}
for s in range(0, len(ids), 250):
    chunk = ids[s:s + 250]
    r = es('POST', f'/{ALIAS}/_search', {"size": 250, "query": {"ids": {"values": chunk}},
                                          "_source": {"exclude_vectors": False, "includes": list(V)}})
    for h in r['hits']['hits']:
        for f in V:
            V[f][h['_id']] = h['_source'].get(f)
print({f: sum(1 for v in V[f].values() if v) for f in V}); json.dump(V, open("vectors_raw.json","w"))
M = {}
for f, n in (('plot_256', 256), ('plot_512', 512), ('plot_1024', 1024)):
    bad = [i for i in ids if not V[f].get(i) or len(V[f][i]) != n]
    print(f, 'dims incorrectas/faltantes:', len(bad), bad[:5])
    M[f] = np.array([V[f][i] for i in ids], dtype=np.float64)
    norms = np.linalg.norm(M[f], axis=1)
    print(f'   norma: min {norms.min():.4f} max {norms.max():.4f}  fuera de [0.99,1.01]: {int(((norms < .99) | (norms > 1.01)).sum())}  NaN: {int(np.isnan(M[f]).sum())}  vectores cero: {int((norms == 0).sum())}')
    M[f] /= norms[:, None]
# projection consistency
for f, n in (('plot_256', 256), ('plot_512', 512)):
    pref = M["plot_1024"][:, :n].copy(); pref /= np.linalg.norm(pref, axis=1)[:, None]
    cos = (pref * M[f]).sum(1)
    print(f'{f} vs prefijo normalizado de plot_1024: cos min {cos.min():.5f} p1 {np.percentile(cos, 1):.5f} med {np.median(cos):.5f}  <0.999: {int((cos < .999).sum())}', [ids[i] for i in np.argsort(cos)[:5]])
# near duplicates in plot_1024
X = M['plot_1024'].astype(np.float32)
S = X @ X.T; np.fill_diagonal(S, -1)
iu = np.argwhere(np.triu(S > 0.95, 1))
pairs = sorted(((float(S[a, b]), ids[a], ids[b]) for a, b in iu), reverse=True)
print('\n## pares plot_1024 con coseno > 0.95:', len(pairs))
for s, a, b in pairs[:40]:
    print(f'   {s:.4f}  {a} «{by[a]["title"]}» ({by[a]["release_date"][:4]})  ~  {b} «{by[b]["title"]}» ({by[b]["release_date"][:4]})')
print('   distribución del vecino más cercano: p50 %.3f p90 %.3f p99 %.3f max %.3f' % tuple(np.percentile(S.max(1), [50, 90, 99, 100])))
# hubness: how often each doc is someone's nearest neighbour
nn = S.argmax(1); hub = np.bincount(nn, minlength=len(ids))
top = np.argsort(-hub)[:12]
print('\n## hubs (veces que son el vecino más cercano de otra película):')
for t in top: print(f'   {hub[t]:4d}  {ids[t]} «{by[ids[t]]["title"]}» plot_len={len(by[ids[t]]["plot"])}')
np.save('X1024.npy', X); json.dump(ids, open('ids.json', 'w'))

# freshness: re-embed sample
random.seed(7)
sample = random.sample(ids, 48) + ['597', '671', '799546', '621707']
inf_id = cfg.get('ELASTIFLIX_INFERENCE_ID', '.jina-embeddings-v5-omni-small')
got = {}
for s in range(0, len(sample), 16):
    chunk = sample[s:s + 16]
    r = es('POST', f'/_inference/embedding/{inf_id}', {"input": [by[i]['plot'] for i in chunk], "input_type": "ingest"})
    if '__error__' in r: print('inference error', r); break
    key = [k for k in r if isinstance(r[k], list)][0]
    for i, e in zip(chunk, r[key]):
        got[i] = np.array(e.get('embedding') or e.get('values') or e, dtype=np.float64)
if got:
    idx = {i: k for k, i in enumerate(ids)}
    cs = sorted(((float(np.dot(v / np.linalg.norm(v), M['plot_1024'][idx[i]])), i) for i, v in got.items()))
    print('\n## re-embedding de', len(cs), 'sinopsis actuales vs plot_1024 guardado: min %.4f med %.4f' % (cs[0][0], cs[len(cs)//2][0]))
    for c, i in cs[:8]: print(f'   {c:.4f} {i} «{by[i]["title"]}»')
    print('   < 0.99:', sum(1 for c, _ in cs if c < .99))

import sys; sys.stdout.reconfigure(encoding="utf-8")
import os, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from paths import WORK, OUT, HALLAZGOS, REPO, M0
os.chdir(WORK)
import json, collections, re, statistics as st
docs = json.load(open('docs.json', encoding='utf-8'))
mapping = json.load(open(OUT / 'live_mapping.json' if (OUT / 'live_mapping.json').exists() else REPO / 'elasticsearch' / 'elastiflix-reloaded-mapping.json', encoding='utf-8'))
mapping = mapping.get('mappings', mapping)['properties']
N = len(docs)
out = {}

def tname(v):
    if v is None: return 'null'
    if isinstance(v, bool): return 'bool'
    if isinstance(v, int): return 'int'
    if isinstance(v, float): return 'float'
    if isinstance(v, str): return 'str-empty' if not v.strip() else 'str'
    if isinstance(v, list): return 'list-empty' if not v else 'list'
    if isinstance(v, dict): return 'dict'
    return type(v).__name__

fields = [f for f in mapping if not f.startswith('plot_')]
print(f'N={N}')
print(f"{'field':24}{'present':>8}{'missing':>8}  types")
for f in fields:
    c = collections.Counter(tname(d.get(f, '__absent__')) if f in d else 'absent' for d in docs)
    print(f"{f:24}{N-c['absent']:>8}{c['absent']:>8}  {dict(c)}")

# unmapped keys
extra = collections.Counter(k for d in docs for k in d if k not in mapping and k != '_id')
print('unmapped keys:', dict(extra))

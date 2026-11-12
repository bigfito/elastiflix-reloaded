"""Corre el análisis completo de calidad y guarda cada salida en data-quality/hallazgos/salidas/<etiqueta>/.

Uso (desde cualquier carpeta):
    python data-quality/scripts/analizar.py --etiqueta wp05 --desde-data   # mide data/ sin red ni Elasticsearch
    python data-quality/scripts/analizar.py --etiqueta m1                  # descarga el alias y mide todo
Opciones:
    --etiqueta X     carpeta de salida (obligatoria). `m0` es la línea base del informe y está congelada.
    --desde-data     lee data/ en lugar de Elasticsearch (implica --sin-vectores)
    --sin-descarga   reutiliza work/docs.json tal como está
    --sin-vectores   omite vectors.py (descarga unos 165 MB y llama a inferencia)
Para medir staging, antepón ELASTIFLIX_INDEX_ALIAS=elastiflix-dq-staging.
Ningún paso escribe en Elasticsearch.
"""
import os, subprocess, sys, pathlib, time
sys.stdout.reconfigure(encoding="utf-8")
SCRIPTS = pathlib.Path(__file__).resolve().parent

PASOS = [
    ("dump.py", "dump.txt", "Películas a work/docs.json (de Elasticsearch o de data/)"),
    ("profile.py", "profile.txt", "Presencia y tipos por campo"),
    ("deep.py", "deep.txt", "Validez, vocabularios, duplicados y coherencia entre campos"),
    ("deep2.py", "deep2.txt", "Puntuaciones, palabras clave, idiomas y colecciones"),
    ("plots.py", "plots.txt", "Calidad del texto de plot"),
    ("llm.py", "llm.txt", "Firmas de texto generado y reparto que no coincide"),
    ("vectors.py", "vectors.txt", "Vectores: normas, proyecciones, duplicados y frescura"),
    ("findings_csv.py", "findings_csv.txt", "Escribe hallazgos.csv en la carpeta de salida"),
]


def arg(name):
    return name in sys.argv


def main():
    if "--etiqueta" not in sys.argv:
        sys.exit("Falta --etiqueta (por ejemplo wp05, m1, m3).")
    etiqueta = sys.argv[sys.argv.index("--etiqueta") + 1]
    if etiqueta == "m0" and not arg("--forzar"):
        sys.exit("m0 es la línea base congelada del informe. Usa otra etiqueta.")
    env = dict(os.environ, DQ_ETIQUETA=etiqueta)
    skip = set()
    if arg("--desde-data"):
        env["DQ_FUENTE"] = "data"
        skip.add("vectors.py")
    if arg("--sin-descarga"):
        skip.add("dump.py")
    if arg("--sin-vectores"):
        skip.add("vectors.py")
    out = SCRIPTS.parent / "hallazgos" / "salidas" / etiqueta
    out.mkdir(parents=True, exist_ok=True)
    for script, salida, que in PASOS:
        if script in skip:
            print(f"· {script:16} omitido")
            continue
        t = time.time()
        r = subprocess.run([sys.executable, str(SCRIPTS / script)], capture_output=True, text=True,
                           encoding="utf-8", env=env)
        (out / salida).write_text(r.stdout + (("\n[stderr]\n" + r.stderr) if r.stderr else ""), encoding="utf-8")
        estado = "ok" if r.returncode == 0 else f"FALLÓ ({r.returncode})"
        print(f"· {script:16} {estado:12} {time.time() - t:6.1f}s  {que}")
        if r.returncode != 0:
            print(r.stderr[-2000:])
            sys.exit(1)
    print(f"Salidas en {out}")


if __name__ == "__main__":
    main()

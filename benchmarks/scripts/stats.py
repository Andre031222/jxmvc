import csv
import itertools
import statistics
import sys
from collections import defaultdict

ORDER = ["jxmvc", "spring", "quarkus", "micronaut", "javalin", "quarkus-native"]
ENDPOINTS = ["plaintext", "json", "db"]

def load(path):
    cells = defaultdict(list)
    meta = {}
    for r in csv.DictReader(open(path)):
        ep = r["endpoint"].rsplit("/", 1)[-1]
        cells[(r["framework"], ep)].append(r)
        meta.setdefault(r["framework"], r)
    return cells, meta

def col(rows, name):
    return [float(r[name]) for r in rows]

def mann_whitney(a, b):
    pooled = sorted(a + b)
    rank = {}
    i = 0
    while i < len(pooled):
        j = i
        while j < len(pooled) and pooled[j] == pooled[i]:
            j += 1
        rank[pooled[i]] = (i + j + 1) / 2
        i = j
    values = a + b
    observed = sum(rank[v] for v in a)
    mean = len(a) * (len(values) + 1) / 2
    extreme = 0
    total = 0
    for combo in itertools.combinations(range(len(values)), len(a)):
        total += 1
        if abs(sum(rank[values[k]] for k in combo) - mean) >= abs(observed - mean) - 1e-9:
            extreme += 1
    return extreme / total

def cliff(a, b):
    gt = sum(1 for x in a for y in b if x > y)
    lt = sum(1 for x in a for y in b if x < y)
    return (gt - lt) / (len(a) * len(b))

def magnitude(d):
    d = abs(d)
    return "negligible" if d < 0.147 else "small" if d < 0.33 else "medium" if d < 0.474 else "large"

def main(path, out):
    cells, meta = load(path)
    fws = [f for f in ORDER if f in meta]
    lines = ["# Estadística del benchmark", "",
             f"Fuente: `{path.split('/')[-1]}`. reps={len(cells[(fws[0], 'plaintext')])} por celda; rps como mediana [mín–máx].",
             "Latencia: mediana de las repeticiones de la media y del p99 (ms). Arranque, RSS e imagen: mediana por framework.", "",
             "## Resumen", "",
             "| Framework | Imagen MB | Arranque ms | RSS MB | /plaintext rps | /json rps | /db rps |",
             "|---|---|---|---|---|---|---|"]
    for f in fws:
        m = meta[f]
        allrows = [r for ep in ENDPOINTS for r in cells.get((f, ep), [])]
        row = [f, m["image_mb"], f"{statistics.median(col(allrows, 'startup_ms')):.0f}", f"{statistics.median(col(allrows, 'rss_mb')):.1f}"]
        for ep in ENDPOINTS:
            rows = cells.get((f, ep))
            if rows:
                rps = col(rows, "rps")
                row.append(f"{statistics.median(rps):.0f} [{min(rps):.0f}–{max(rps):.0f}]")
            else:
                row.append("—")
        lines.append("| " + " | ".join(row) + " |")
    lines += ["", "## Latencia (ms)", "", "| Framework | " + " | ".join(f"/{ep} media | /{ep} p99" for ep in ENDPOINTS) + " |",
              "|---|" + "---|---|" * len(ENDPOINTS)]
    for f in fws:
        row = [f]
        for ep in ENDPOINTS:
            rows = cells.get((f, ep))
            row += [f"{statistics.median(col(rows, 'meanMs')):.3f}", f"{statistics.median(col(rows, 'p99')):.3f}"] if rows else ["—", "—"]
        lines.append("| " + " | ".join(row) + " |")
    lines += ["", "## Significancia frente a JxMVC (rps)", "",
              "Mann–Whitney U exacto a dos colas (n=5 por lado) y δ de Cliff (positivo: el rival rinde más que JxMVC).", "",
              "| Rival | Endpoint | Diferencia de medianas | p | δ | Magnitud |", "|---|---|---|---|---|---|"]
    for f in fws:
        if f == "jxmvc":
            continue
        for ep in ENDPOINTS:
            a, b = cells.get((f, ep)), cells.get(("jxmvc", ep))
            if not a or not b:
                continue
            ra, rb = col(a, "rps"), col(b, "rps")
            diff = (statistics.median(ra) - statistics.median(rb)) / statistics.median(rb) * 100
            d = cliff(ra, rb)
            lines.append(f"| {f} | /{ep} | {diff:+.1f} % | {mann_whitney(ra, rb):.3f} | {d:+.2f} | {magnitude(d)} |")
    lines += ["", "## Coste de /db frente a /json en cada framework", "",
              "| Framework | /json rps | /db rps | Coste | p | δ |", "|---|---|---|---|---|---|"]
    for f in fws:
        a, b = cells.get((f, "db")), cells.get((f, "json"))
        if not a or not b:
            continue
        ra, rb = col(a, "rps"), col(b, "rps")
        cost = (statistics.median(rb) - statistics.median(ra)) / statistics.median(rb) * 100
        lines.append(f"| {f} | {statistics.median(rb):.0f} | {statistics.median(ra):.0f} | {cost:+.1f} % | {mann_whitney(ra, rb):.3f} | {cliff(ra, rb):+.2f} |")
    errors = sum(int(r["errors"]) + int(r["non2xx"]) for rows in cells.values() for r in rows)
    requests = sum(int(r["requests"]) for rows in cells.values() for r in rows)
    lines += ["", f"Peticiones totales: {requests:,}. Errores o respuestas no-2xx: {errors}."]
    open(out, "w").write("\n".join(lines) + "\n")
    print("\n".join(lines))

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])

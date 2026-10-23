#!/usr/bin/env bash
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
APPS_DIR="$REPO/benchmarks/docker/apps"
OUT="$REPO/benchmarks/results/SECURITY.md"
RAW="$REPO/benchmarks/results/raw-security.tsv"
PORT=8080
APPS=(${SEC_APPS:-jxmvc spring quarkus micronaut javalin})

command -v java >/dev/null || { echo "Falta java en el host"; exit 1; }
docker info >/dev/null 2>&1 || { echo "Docker no está corriendo"; exit 1; }
(cd "$HERE" && javac -d "$HERE/build" SecProbe.java)

wait_up() {
  for _ in $(seq 1 600); do
    [ "$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$PORT/plaintext" || true)" = "200" ] && return 0
    sleep 0.2
  done
  return 1
}

printf 'framework\tid\tverdict\tdetail\n' > "$RAW"
for fw in "${APPS[@]}"; do
  echo "── $fw"
  img="bench-$fw"
  if [ "$fw" = "jxmvc" ]; then
    docker build -q -t "$img" -f "$APPS_DIR/jxmvc/Dockerfile" "$REPO" >/dev/null
  else
    docker build -q -t "$img" "$APPS_DIR/$fw" >/dev/null
  fi
  docker rm -f "sec_$fw" >/dev/null 2>&1 || true
  docker run -d --name "sec_$fw" -p "$PORT:8080" "$img" >/dev/null
  if wait_up; then
    skip=""
    case "$fw" in quarkus|javalin) skip="B2,B3" ;; esac
    java -cp "$HERE/build" SecProbe "$PORT" "$skip" | while IFS=$'\t' read -r id verdict detail; do
      printf '%s\t%s\t%s\t%s\n' "$fw" "$id" "$verdict" "$detail" >> "$RAW"
      printf '   %-3s %-5s %s\n' "$id" "$verdict" "$detail"
    done
  else
    echo "   no arrancó"; docker logs --tail 20 "sec_$fw" || true
  fi
  docker rm -f "sec_$fw" >/dev/null 2>&1 || true
done

python3 - "$RAW" "$OUT" <<'EOF'
import csv, sys, collections
rows = list(csv.DictReader(open(sys.argv[1]), delimiter="\t"))
names = {
 "H1": "X-Content-Type-Options: nosniff", "H2": "Protección contra clickjacking", "H3": "Referrer-Policy",
 "H4": "Content-Security-Policy", "H5": "Sin versión del servidor en cabeceras", "B1": "Cuerpo de 20 MiB rechazado",
 "B2": "JSON con 100 000 niveles rechazado (4xx)", "B3": "JSON truncado responde 4xx, no 5xx",
 "E1": "Excepción sin filtrar detalles internos", "C1": "POST entre sitios rechazado (CSRF)",
 "R1": "Sin redirección abierta", "X1": "Parámetro reflejado sin <script> ejecutable", "T1": "Sin path traversal",
 "M1": "TRACE no refleja la petición", "I1": "Sin endpoints internos expuestos", "L1": "Fuerza bruta en /login limitada (429)",
 "L2": "Cabecera de 200 KB rechazada", "S1": "Transfer-Encoding + Content-Length sin ambigüedad"}
fws = list(dict.fromkeys(r["framework"] for r in rows))
cell = {(r["framework"], r["id"]): r for r in rows}
mark = {"PASS": "✅", "FAIL": "❌", "N/A": "—"}
lines = ["# Seguridad por defecto", "",
         "Cada framework con su configuración por defecto y la misma app mínima del benchmark (`benchmarks/docker/apps`).",
         "Generado por `benchmarks/security/run.sh`; las sondas están en `SecProbe.java`. ✅ protege · ❌ no protege · — no aplica.", "",
         "| # | Comprobación | " + " | ".join(fws) + " |", "|---|---|" + "---|" * len(fws)]
for pid, name in names.items():
    lines.append(f"| {pid} | {name} | " + " | ".join(mark.get(cell.get((f, pid), {}).get("verdict", ""), "?") for f in fws) + " |")
totals = collections.Counter(r["framework"] for r in rows if r["verdict"] == "PASS")
applicable = collections.Counter(r["framework"] for r in rows if r["verdict"] != "N/A")
lines += ["| | **Total** | " + " | ".join(f"**{totals[f]}/{applicable[f]}**" for f in fws) + " |", "", "## Detalle", ""]
for f in fws:
    lines.append(f"**{f}**"); lines.append("")
    for pid in names:
        r = cell.get((f, pid))
        if r: lines.append(f"- {pid} {r['verdict']}: {r['detail']}")
    lines.append("")
open(sys.argv[2], "w").write("\n".join(lines))
print("\n".join(lines[:len(names) + 8]))
EOF

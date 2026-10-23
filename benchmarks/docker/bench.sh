#!/usr/bin/env bash
set -euo pipefail

CONNS="${1:-64}"
DUR="${2:-20}"
REPS="${3:-3}"
CPUS="${BENCH_CPUS:-2}"
MEM="${BENCH_MEM:-1g}"
PORT=8080
CPUSET="${BENCH_CPUSET:-}"
CLIENT_CPUS="${BENCH_CLIENT_CPUS:-}"
CLIENT_PREFIX=()
[ -n "$CLIENT_CPUS" ] && CLIENT_PREFIX=(taskset -c "$CLIENT_CPUS")

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
OUT="$HERE/../results/RESULTS-docker.md"
CSV="$HERE/../results/raw-docker.csv"

APPS=(${BENCH_APPS:-jxmvc spring quarkus micronaut javalin})
[ "${BENCH_NATIVE:-0}" = "1" ] && APPS+=(quarkus-native)

command -v java >/dev/null || { echo "Falta java en el host (para LoadClient)"; exit 1; }
docker info >/dev/null 2>&1 || { echo "Docker no está corriendo. Inicia Docker Desktop."; exit 1; }
[ -f "$HERE/../load/LoadClient.class" ] || (cd "$HERE/../load" && javac LoadClient.java)

[ "${BENCH_APPEND:-0}" = "1" ] && [ -f "$CSV" ] || echo "framework,image_mb,startup_ms,rss_mb,endpoint,conns,dur,requests,errors,non2xx,rps,meanMs,p50,p90,p95,p99" > "$CSV"

wait_up() {  # espera 200 en /plaintext, imprime ms de arranque
  local start now code
  start=$(date +%s%3N)
  for _ in $(seq 1 600); do
    code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$PORT/plaintext" 2>/dev/null || echo 000)
    [ "$code" = "200" ] && { now=$(date +%s%3N); echo $((now-start)); return 0; }
    sleep 0.1
  done
  echo "-1"; return 1
}

for fw in "${APPS[@]}"; do
  echo "──────────── $fw ────────────"
  img="bench-$fw"
  blog="/tmp/bench-build-$fw.log"
  bok=1
  if [ "$fw" = "jxmvc" ]; then
    docker build -t "$img" -f "$HERE/apps/jxmvc/Dockerfile" "$REPO" >"$blog" 2>&1 || bok=0
  elif [ "$fw" = "quarkus-native" ]; then
    docker build -t "$img" -f "$HERE/apps/quarkus/Dockerfile.native" "$HERE/apps/quarkus" >"$blog" 2>&1 || bok=0
  else
    docker build -t "$img" "$HERE/apps/$fw" >"$blog" 2>&1 || bok=0
  fi
  if [ "$bok" = 0 ]; then
    echo "  $fw: BUILD FALLÓ — últimas líneas:"; tail -25 "$blog"; echo "  (log completo: $blog)"; continue
  fi
  image_mb=$(docker image inspect "$img" --format '{{.Size}}' | awk '{printf "%.1f", $1/1048576}')

  docker rm -f "bench_$fw" >/dev/null 2>&1 || true
  runargs=(--cpus="$CPUS" --memory="$MEM")
  [ -n "${BENCH_HEAP:-}" ] && [ "$fw" != "quarkus-native" ] && runargs+=(-e "JAVA_TOOL_OPTIONS=-Xms$BENCH_HEAP -Xmx$BENCH_HEAP")
  [ -n "$CPUSET" ] && runargs+=(--cpuset-cpus="$CPUSET")
  docker run -d --name "bench_$fw" "${runargs[@]}" -p "$PORT:8080" "$img" >/dev/null

  startup_ms=$(wait_up) || startup_ms=-1
  if [ "$startup_ms" = "-1" ]; then
    echo "  $fw NO arrancó — logs:"; docker logs --tail 20 "bench_$fw" || true
    docker rm -f "bench_$fw" >/dev/null 2>&1 || true
    continue
  fi
  echo "  arranque=${startup_ms}ms  imagen=${image_mb}MB"

  dbep=""; [ -n "${BENCH_DB:-}" ] && [ "$fw" != "quarkus-native" ] && dbep=db
  for ep in plaintext json $dbep; do
    for r in $(seq 1 "$REPS"); do
      line=$( (cd "$HERE/../load" && ${CLIENT_PREFIX[@]+"${CLIENT_PREFIX[@]}"} java LoadClient "http://localhost:$PORT/$ep" "$CONNS" "$DUR" 5) 2>/dev/null )
      rss_mb=$(docker stats --no-stream --format '{{.MemUsage}}' "bench_$fw" | awk -F'/' '{gsub(/[^0-9.]/,"",$1); print $1}')
      echo "$fw,$image_mb,$startup_ms,$rss_mb,$line" >> "$CSV"
      e=$(echo "$line" | awk -F, '{print ($5+$6)+0}')
      [ "${e:-0}" -gt 0 ] && echo "  ⚠ $fw /$ep rep $r: errores/no-2xx=$e — medición SOSPECHOSA (revisa saturación/CPU del cliente)"
    done
  done

  docker rm -f "bench_$fw" >/dev/null 2>&1 || true
done

echo "CSV crudo -> $CSV"

med() {
  awk -F, -v f="$1" -v c="$2" -v ep="${3:-}" \
    '$1==f && (ep=="" || $5==ep){print $c}' "$CSV" \
    | sort -n | awk '{a[NR]=$1} END{print (NR? a[int((NR+1)/2)] : "-")}'
}

{
  echo "# Resultados dockerizados"
  echo
  echo "Entorno: \`docker --cpus=$CPUS --memory=$MEM\`, misma base JRE. conns=$CONNS, dur=${DUR}s, reps=$REPS."
  echo "Generado por \`bench.sh\`. Números relativos comparables; ver README §8 (validez)."
  echo "Arranque/RSS/rps son **mediana** de las $REPS repeticiones. \`⚠\` = el framework tuvo errores/no-2xx: cifra NO válida."
  echo
  echo "| Framework | Imagen (MB) | Arranque (ms) | RSS (MB) | rps /plaintext (mediana) | rps /json (mediana) | rps /db (mediana) |"
  echo "|---|---|---|---|---|---|---|"
  TABLE_APPS=($(awk -F, 'NR>1 && !s[$1]++{print $1}' "$CSV"))
  for fw in "${APPS[@]}"; do [[ " ${TABLE_APPS[*]} " == *" $fw "* ]] || TABLE_APPS+=("$fw"); done
  for fw in "${TABLE_APPS[@]}"; do
    im=$(awk -F, -v f="$fw" '$1==f{print $2; exit}' "$CSV")   # imagen: valor constante por framework
    su=$(med "$fw" 3)                                          # arranque: mediana
    rs=$(med "$fw" 4)                                          # RSS: mediana
    pt=$(med "$fw" 11 "http://localhost:8080/plaintext")
    js=$(med "$fw" 11 "http://localhost:8080/json")
    db=$(med "$fw" 11 "http://localhost:8080/db")             # "-" si no se corrió con BENCH_DB=1
    err=$(awk -F, -v f="$fw" '$1==f{e+=$9+$10} END{print e+0}' "$CSV")
    flag=""; [ "${err:-0}" -gt 0 ] && flag=" ⚠"
    [ -n "$im" ] && echo "| ${fw}${flag} | $im | $su | $rs | $pt | $js | $db |" || echo "| $fw | (no arrancó) | | | | | |"
  done
} > "$OUT"

echo "Tabla -> $OUT"
cat "$OUT"

total_err=$(awk -F, 'NR>1{e+=$9+$10} END{print e+0}' "$CSV")
if [ "${total_err:-0}" -gt 0 ]; then
  echo ""
  echo "⚠⚠ ATENCIÓN: $total_err errores/no-2xx en total. Las filas marcadas con ⚠ NO son válidas para el paper."
  echo "   Causas típicas: cliente saturado (usa BENCH_CLIENT_CPUS), poca CPU/RAM al contenedor, o el server falla bajo carga."
  exit 3
fi

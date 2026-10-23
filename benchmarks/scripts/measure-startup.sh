#!/usr/bin/env bash
set -euo pipefail
CMD="${1:?cmd de arranque requerido}"
URL="${2:?url de salud requerida}"

start=$(date +%s%3N)
bash -c "$CMD" >/tmp/app-startup.log 2>&1 &
PID=$!
trap 'kill "$PID" 2>/dev/null || true' EXIT

for _ in $(seq 1 600); do   # hasta 60 s
  code=$(curl -s -o /dev/null -w "%{http_code}" "$URL" 2>/dev/null || echo 000)
  if [ "$code" = "200" ]; then
    end=$(date +%s%3N)
    echo "startup_ms=$((end - start))  pid=$PID"
    exit 0
  fi
  sleep 0.1
done
echo "TIMEOUT: no respondió 200 en 60s" >&2
exit 1

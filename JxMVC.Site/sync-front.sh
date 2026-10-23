#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

SRC="src/main/webapp"
DEP="target/cargo/configurations/tomcat10x/webapps/jxmvc"

if [ ! -d "$DEP" ]; then
  echo "✗ No existe el webapp desplegado: $DEP"
  echo "  Levanta el sitio primero:  mvn -o package cargo:run -DskipTests=true"
  exit 1
fi

sync_once() {
  cp -r "$SRC/WEB-INF/views" "$DEP/WEB-INF/"
  cp    "$SRC/WEB-INF/jx.tld" "$DEP/WEB-INF/jx.tld"
  cp -r "$SRC/assets"         "$DEP/"
  echo "✓ front sincronizado → refresca el navegador (Ctrl+F5)"
}

if [ "${1:-}" = "watch" ]; then
  echo "▶ watch: sincronizando front cada 2 s (Ctrl+C para salir)"
  while true; do sync_once; sleep 2; done
else
  sync_once
fi

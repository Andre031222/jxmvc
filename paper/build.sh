#!/usr/bin/env bash
# Compila el paper con la plantilla OFICIAL de Wiley (clase USG).
# Los .cls/.sty/.bst/fonts viven en wiley-template/Optimal-Design-layout/.
set -euo pipefail
cd "$(dirname "$0")"
export TEXINPUTS=".//:wiley-template/Optimal-Design-layout//:${TEXINPUTS:-}"
export BIBINPUTS=".//:wiley-template/Optimal-Design-layout//:${BIBINPUTS:-}"
pdflatex -interaction=nonstopmode -halt-on-error main.tex
pdflatex -interaction=nonstopmode -halt-on-error main.tex   # 2ª pasada: refs/cross-refs
echo "OK -> paper/main.pdf"

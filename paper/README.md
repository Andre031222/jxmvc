# Paper — JxMVC (Software: Practice and Experience)

Fuente LaTeX del artículo, con la **plantilla oficial de Wiley** (clase `USG`,
New Journal Design).

## Estructura
- `main.tex` — documento principal (metadata, estructura, bibliografía).
- `sections/06-evaluation.tex` — sección de Evaluación (cifras oficiales del benchmark).
- `tables/results.tex`, `tables/latency.tex` — tablas de resultados y latencia.
- `tables/qualitative.tex` — tabla comparativa cualitativa (§3).
- `references.bib` — bibliografía (en construcción, 15–25 refs objetivo).
- `OUTLINE.md` — outline de trabajo y checklist.
- `build.sh` — compila con la plantilla de Wiley.

## Plantilla oficial de Wiley (no versionada)

Los archivos de clase de Wiley **no** se guardan en el repo (copyright + ~7 MB).
Descárgalos una vez desde la fuente oficial:

```bash
cd paper
mkdir -p wiley-template && cd wiley-template
curl -fsSL -o WileyDesign.zip https://authors.wiley.com/asset/WileyDesign.zip
unzip -q WileyDesign.zip
# la clase queda en wiley-template/Optimal-Design-layout/USG.cls
```

Fuente oficial: <https://authors.wiley.com/author-resources/Journal-Authors/Prepare/latex-template.html>

## Compilar

```bash
./build.sh        # usa TEXINPUTS para encontrar la clase USG; genera main.pdf
```

Requiere una instalación de TeX Live con `ulem`, `booktabs`, `siunitx`
(en Arch: `texlive-plaingeneric texlive-latexextra`).

## Notas
- `\documentclass[AMA]{USG}` → referencias numeradas, el estilo de SPE.
- El logo de revista en la cabecera es un *placeholder* de la plantilla; Wiley
  lo sustituye en producción.
- Para pasar la bibliografía a BibTeX con el estilo de Wiley:
  `\bibliographystyle{wileyNJD-AMA}\bibliography{references}` (el `.bst` viene
  en el paquete descargado).

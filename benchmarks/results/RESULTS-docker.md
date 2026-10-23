# Resultados dockerizados

Entorno: `docker --cpus=4 --memory=2g`, misma base JRE. conns=64, dur=30s, reps=5.
Generado por `bench.sh`. Números relativos comparables; ver README §8 (validez).
Arranque/RSS/rps son **mediana** de las 5 repeticiones. `⚠` = el framework tuvo errores/no-2xx: cifra NO válida.

| Framework | Imagen (MB) | Arranque (ms) | RSS (MB) | rps /plaintext (mediana) | rps /json (mediana) | rps /db (mediana) |
|---|---|---|---|---|---|---|
| jxmvc | 274.2 | 1321 | 461.1 | 44342.8 | 44509.4 | 43919.8 |
| spring | 302.4 | 2574 | 504.5 | 46450.3 | 47082.4 | 45925.2 |
| quarkus | 298.4 | 862 | 432.6 | 50879.3 | 50438.8 | 49867.5 |
| micronaut | 295.0 | 1232 | 456.9 | 47370.3 | 47371.2 | 42175.6 |
| javalin | 288.9 | 451 | 471.7 | 49636.1 | 49187.3 | 50325.8 |
| quarkus-native | 72.9 | 13 | 23.88 | 49027.5 | 48831.0 | - |

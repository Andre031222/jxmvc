# Resultados dockerizados

Entorno: `docker --cpus=4 --memory=2g --cpuset-cpus=0-3`, misma base JRE. conns=64, dur=30s, reps=5.
Cliente aislado (taskset 4-7). Generado por `bench.sh`; ver README §8 (validez).
Arranque/RSS/rps son **mediana** de las 5 repeticiones. `⚠` = errores/no-2xx: cifra NO válida.

| Framework | Imagen (MB) | Arranque (ms) | RSS (MB) | rps /plaintext (mediana) | rps /json (mediana) |
|---|---|---|---|---|---|
| jxmvc | 271.7 | 822 | 448.5 | 49061.8 | 48720.4 |
| spring | 299.9 | 1945 | 375.6 | 49462.0 | 49896.2 |
| quarkus | 295.9 | 606 | 431.2 | 55087.3 | 54208.6 |
| micronaut | 292.5 | 1154 | 331.4 | 50973.9 | 50652.3 |
| javalin | 286.4 | 369 | 424.6 | 54168.8 | 53613.9 |
| quarkus-native | 72.8 | 12 | 25.12 | 52699.1 | 50072.9 |

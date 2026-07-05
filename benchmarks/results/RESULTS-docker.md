# Resultados dockerizados

Entorno: `docker --cpus=2 --memory=1g`, misma base JRE. conns=64, dur=15s, reps=3.
Generado por `bench.sh`. Números relativos comparables; ver README §8 (validez).

| Framework | Imagen (MB) | Arranque (ms) | RSS (MB) | rps /plaintext (mediana) | rps /json (mediana) |
|---|---|---|---|---|---|
| jxmvc | 104.0 | 1225 | 157.5 | 12216.5 | 12917.3 |
| spring | 121.4 | 2375 | 172.6 | 11078.6 | 11641.9 |
| quarkus | 117.3 | 1515 | 94.7 | 15957.2 | 17195.3 |
| micronaut | 114.7 | 1382 | 95.77 | 10871.4 | 11627.6 |
| javalin | 109.3 | 499 | 89.65 | 13867.6 | 13424.5 |

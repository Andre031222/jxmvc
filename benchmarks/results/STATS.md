# Estadística del benchmark

Fuente: `raw-docker.csv`. reps=5 por celda; rps como mediana [mín–máx].
Latencia: mediana de las repeticiones de la media y del p99 (ms). Arranque, RSS e imagen: mediana por framework.

## Resumen

| Framework | Imagen MB | Arranque ms | RSS MB | /plaintext rps | /json rps | /db rps |
|---|---|---|---|---|---|---|
| jxmvc | 274.2 | 1321 | 461.1 | 44343 [43870–44481] | 44509 [43900–44847] | 43920 [43222–44480] |
| spring | 302.4 | 2574 | 504.5 | 46450 [45398–46772] | 47082 [46248–47132] | 45925 [44960–46358] |
| quarkus | 298.4 | 862 | 432.6 | 50879 [50037–51515] | 50439 [48864–50940] | 49868 [48382–50319] |
| micronaut | 295.0 | 1232 | 456.9 | 47370 [46470–48311] | 47371 [46217–47593] | 42176 [41664–43200] |
| javalin | 288.9 | 451 | 471.7 | 49636 [49245–50138] | 49187 [48588–50215] | 50326 [48664–50577] |
| quarkus-native | 72.9 | 13 | 24.0 | 49028 [48318–49582] | 48831 [48247–49006] | — |

## Latencia (ms)

| Framework | /plaintext media | /plaintext p99 | /json media | /json p99 | /db media | /db p99 |
|---|---|---|---|---|---|---|
| jxmvc | 1.442 | 3.743 | 1.437 | 3.673 | 1.456 | 3.577 |
| spring | 1.377 | 3.332 | 1.358 | 3.437 | 1.392 | 3.369 |
| quarkus | 1.256 | 3.309 | 1.268 | 3.445 | 1.282 | 3.363 |
| micronaut | 1.349 | 4.082 | 1.349 | 4.059 | 1.516 | 5.676 |
| javalin | 1.288 | 3.404 | 1.300 | 3.530 | 1.270 | 3.278 |
| quarkus-native | 1.304 | 3.768 | 1.309 | 3.886 | — | — |

## Significancia frente a JxMVC (rps)

Mann–Whitney U exacto a dos colas (n=5 por lado) y δ de Cliff (positivo: el rival rinde más que JxMVC).

| Rival | Endpoint | Diferencia de medianas | p | δ | Magnitud |
|---|---|---|---|---|---|
| spring | /plaintext | +4.8 % | 0.008 | +1.00 | large |
| spring | /json | +5.8 % | 0.008 | +1.00 | large |
| spring | /db | +4.6 % | 0.008 | +1.00 | large |
| quarkus | /plaintext | +14.7 % | 0.008 | +1.00 | large |
| quarkus | /json | +13.3 % | 0.008 | +1.00 | large |
| quarkus | /db | +13.5 % | 0.008 | +1.00 | large |
| micronaut | /plaintext | +6.8 % | 0.008 | +1.00 | large |
| micronaut | /json | +6.4 % | 0.008 | +1.00 | large |
| micronaut | /db | -4.0 % | 0.008 | -1.00 | large |
| javalin | /plaintext | +11.9 % | 0.008 | +1.00 | large |
| javalin | /json | +10.5 % | 0.008 | +1.00 | large |
| javalin | /db | +14.6 % | 0.008 | +1.00 | large |
| quarkus-native | /plaintext | +10.6 % | 0.008 | +1.00 | large |
| quarkus-native | /json | +9.7 % | 0.008 | +1.00 | large |

## Coste de /db frente a /json en cada framework

| Framework | /json rps | /db rps | Coste | p | δ |
|---|---|---|---|---|---|
| jxmvc | 44509 | 43920 | +1.3 % | 0.095 | -0.68 |
| spring | 47082 | 45925 | +2.5 % | 0.032 | -0.84 |
| quarkus | 50439 | 49868 | +1.1 % | 0.151 | -0.60 |
| micronaut | 47371 | 42176 | +11.0 % | 0.008 | -1.00 |
| javalin | 49187 | 50326 | -2.3 % | 0.222 | +0.52 |

Peticiones totales: 120,892,746. Errores o respuestas no-2xx: 0.

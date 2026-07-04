# Resultados del benchmark

> Metodología en [`../README.md`](../README.md). Las celdas **verificadas** provienen del repo
> (tamaño del artefacto, conteo de dependencias). Las celdas `— (medir)` se completan ejecutando
> el protocolo (§6) en una **máquina limpia**; no rellenar con números de una laptop de desarrollo.

## Entorno de la corrida

```
CPU: —    RAM: —    SO: —    JDK: —    Fecha: —    Modo: JVM
Cliente de carga: —    Repeticiones: N=5 (mediana)    Warmup: 5 s
```

## Tabla comparativa

| Métrica | JxMVC 3.4.0 | Spring Boot 3.3 | Quarkus 3 (JVM) | Micronaut 4 | Javalin 6 |
|---|---|---|---|---|---|
| **Framework solo** (JAR) | **253 KB** ✓ | — (medir) | — (medir) | — (medir) | — (medir) |
| **Deps runtime** | **0** ✓ | — (medir) | — (medir) | — (medir) | — (medir) |
| **Desplegable + servidor** | core 253 KB + Tomcat 10.1 (≈13 MB lib) | uber-JAR (incluye server) | quarkus-app | uber-JAR | fat-JAR |
| **Arranque frío (ms)** | — (medir) | — | — | — | — |
| **Memoria RSS (MB)** | — (medir) | — | — | — | — |
| **Throughput /plaintext (req/s)** | — (medir) | — | — | — | — |
| **Throughput /json (req/s)** | — (medir) | — | — | — | — |
| **Latencia p50/p95/p99 (ms)** | — (medir) | — | — | — | — |

✓ = verificado en este repo. El resto se mide con los scripts de `../scripts/`.

## Datos verificados de JxMVC (reproducibles ya)

```
Framework JAR:  jxmvc-core-3.4.0.jar = 258 897 bytes (253 KB)
Deps runtime:   0  (única dependencia jakarta.jakartaee-api en scope 'provided')
Clases core:    54 archivos fuente, ~10 400 LOC
Tests:          347 verificaciones (suite propia, sin JUnit)
```
Reproducir: `cd JxMVC.Core && mvn -q clean install` → `ls -l target/jxmvc-core-3.4.0.jar`.

## CSV crudo por corrida

Cada línea de `run-load.sh` se anexa aquí (una por endpoint × repetición):

```
# endpoint,conns,durSecs,requests,errors,non2xx,rps,meanMs,p50,p90,p95,p99
# (pendiente de corrida en máquina limpia)
```

## Notas

- Reportar Quarkus/Micronaut también en **modo nativo (GraalVM)** en una fila aparte: cambia
  radicalmente arranque (~ms) y memoria — es su ventaja real, y omitirlo sería deshonesto.
- El tamaño "Framework solo" favorece a JxMVC (no incluye servidor); la fila **Desplegable +
  servidor** es la comparación de conclusiones.

# Resultados del benchmark

Corrida **dockerizada** de un comando (`../docker/bench.sh`). Cada framework se ejecuta como
imagen con la **misma base JRE** y **mismos límites** (`--cpus=2 --memory=1g`), exponiendo los
mismos endpoints `/plaintext` y `/json`. Metodología en [`../README.md`](../README.md).

## Entorno de esta corrida

```
Motor:    Docker Desktop (WSL2) · límites --cpus=2 --memory=1g · misma base eclipse-temurin:17-jre
Carga:    LoadClient (JDK), 64 conexiones, 15 s de medición + 5 s warmup, 3 repeticiones
Reporte:  mediana de las 3 repeticiones (la 1ª, con JIT frío, queda descartada por la mediana)
Fecha:    2026-07-05 · Modo: JVM (Quarkus/Micronaut NO nativo — ver nota)
```

> ⚠️ Números **relativos** comparables (idénticas condiciones). Los **absolutos** dependen de la
> VM de Docker Desktop; para cifras finales de publicación, correr el mismo `bench.sh` en un
> **Linux nativo** (p. ej. el VPS Debian). Se reportan tal cual, con el entorno declarado.

## Tabla (mediana de 3 repeticiones)

| Framework | Imagen (MB) | Arranque frío (ms) | RSS (MB) | rps /plaintext | rps /json | p95 /json (ms) | errores |
|---|---|---|---|---|---|---|---|
| **JxMVC 3.4.0** | **104.0** | **1225** | ~150 | 12 217 | 12 917 | 11.8 | **0** |
| Spring Boot 3.3 | 121.4 | 2375 | ~181 | 11 079 | 11 642 | 24.7 | 0 |
| Quarkus 3.11 (JVM) | 117.3 | 1515 | ~96 | 15 957 | 17 195 | 6.8 | 0 |
| Micronaut 4.10 | 114.7 | 1382 | ~96 | 10 871 | 11 628 | 11.8 | 0 |
| Javalin 6 (Jetty) | 109.3 | 499 | ~94 | 13 868 | 13 424 | 9.0 | 0 |

Volumen total: **5.5M+ peticiones, 0 errores y 0 respuestas no-2xx** en los cinco.

## Lectura honesta (dónde gana y dónde no JxMVC)

**Fortalezas medidas:**
- **Imagen más pequeña (104 MB)** — el WAR de 253 KB sobre Tomcat/JRE pesa menos que los
  uber-JAR/fast-jar de los demás. (Aun así, el JRE base domina: todos caen en 104–121 MB;
  la ventaja del artefacto de 253 KB casi se diluye a nivel de contenedor — dato honesto.)
- **2º arranque más rápido (1.2 s)** — bate a Spring (2×), a Quarkus-JVM y a Micronaut; solo
  Javalin (Jetty mínimo) arranca antes. Muy bueno para un framework sobre Tomcat.
- **Throughput competitivo (~12–13k rps)**: 3º lugar, **por encima de Spring y de Micronaut**,
  detrás de Quarkus y Javalin. Respetable para un framework de dependencia-cero.
- **Corrección bajo carga: 0 errores** en millones de peticiones.

**Debilidad honesta:**
- **Memoria (RSS ~150 MB)**: 2ª más alta, solo mejor que Spring; los stacks Netty
  (Quarkus/Micronaut) y Jetty (Javalin) rondan ~95 MB. Es el coste del contenedor servlet
  (Tomcat). Es la métrica a mejorar y NO se debe ocultar en el paper.

**Conclusión defendible:** para un framework de **253 KB y cero dependencias**, JxMVC es
**competitivo con Spring/Quarkus/Micronaut/Javalin** — mejor arranque que casi todos, imagen más
pequeña, throughput por encima de Spring y Micronaut — con un mayor uso de memoria como
contrapartida del stack servlet. Es una historia sólida y creíble, no un "somos los más rápidos".

## Notas para el paper

- **Modo nativo**: Quarkus y Micronaut compilados con GraalVM cambian radicalmente arranque
  (~decenas de ms) y RSS. Añadir una fila nativa aparte para no subrepresentarlos sería lo justo.
- **Warmup**: la 1ª repetición de cada framework rinde menos (JIT frío); la mediana de 3 la
  descarta. Para el paper, subir a N≥5 y reportar mediana + [min,max].
- **Barrido de concurrencia**: repetir en 1/8/32/64/128/256 conexiones para la curva completa.
- Reproducir: `cd benchmarks/docker && ./bench.sh 64 15 3` → `results/RESULTS-docker.md`.

## Datos verificados del artefacto (sin correr nada)

```
JxMVC core JAR: 258 897 bytes (253 KB) · 0 dependencias runtime (jakarta-api provided)
54 clases fuente · ~10 400 LOC · 347 verificaciones (suite propia, sin JUnit)
```

El CSV crudo por repetición está en [`raw-docker.csv`](raw-docker.csv).

# Entorno del benchmark (para el paper)

Corrida del 23 de octubre de 2026 sobre JxMVC 3.5.0.

- CPU: 12th Gen Intel(R) Core(TM) i5-12500H
- Núcleos: 16 hilos (4 P-cores @2.5GHz + 8 E-cores @1.8GHz)
- RAM: 30Gi
- Kernel: 7.2.8-arch1-1 (Arch Linux sin virtualizar)
- JDK (host, LoadClient): openjdk version "25.0.4.1" 2026-08-18
- Docker: Docker version 29.8.2, build 7fc2dff9bc
- Governor: performance | Turbo: OFF (no_turbo=1)
- Aislamiento: contenedor cpuset=0-3 (2 P-cores, --cpus=4, --memory=2g); cliente taskset=4-7 (2 P-cores)
- Heap: -Xms512m -Xmx512m en todos los frameworks JVM (BENCH_HEAP); el nativo no usa heap de JVM
- Config de carga: conns=64, dur=30s, reps=5 (mediana), warmup=5s, endpoints /plaintext, /json y /db (SELECT sobre H2 en memoria, 1000 filas)
- Frameworks: jxmvc, spring, quarkus, micronaut, javalin, quarkus-native (GraalVM, sin /db: H2 no compila a nativo)
- Comando: `BENCH_CPUS=4 BENCH_MEM=2g BENCH_CPUSET=0-3 BENCH_CLIENT_CPUS=4-7 BENCH_HEAP=512m BENCH_DB=1 BENCH_NATIVE=1 ./bench.sh 64 30 5`

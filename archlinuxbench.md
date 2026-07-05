# Benchmark de JxMVC en Arch Linux — instrucciones paso a paso

Objetivo: correr el benchmark en **Linux bare-metal** (tu Arch dual-boot) para obtener las cifras
**de calidad de publicación** de JxMVC vs Spring Boot, Quarkus, Micronaut y Javalin. Es
prácticamente **un comando**; Docker se encarga de construir cada framework en igualdad de
condiciones y de escribir la tabla de resultados.

> Tiempo estimado: ~15–25 min la 1ª vez (descarga imágenes y dependencias Maven). Las siguientes
> corridas son más rápidas (todo queda cacheado).

---

## 1. Instalar lo necesario (una sola vez)

```bash
sudo pacman -Syu --needed docker jdk-openjdk git
```
- `docker` → construye y corre cada framework aislado.
- `jdk-openjdk` → un JDK en el host (lo usa el generador de carga `LoadClient`).
- `git` → para clonar el repo.

## 2. Arrancar Docker y darte permiso (una sola vez)

```bash
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
```
**Importante:** cierra sesión y vuelve a entrar (o reinicia) para que el grupo `docker` tome
efecto. Verifica que funciona sin sudo:
```bash
docker run --rm hello-world
```
Si imprime "Hello from Docker!", listo.

## 3. Clonar el repositorio

```bash
git clone https://github.com/Andre031222/jxmvc.git
cd jxmvc/benchmarks/docker
```

## 4. Correr el benchmark

**Opción A — completo con la fila GraalVM nativo (recomendado para el paper):**
```bash
BENCH_NATIVE=1 ./bench.sh 64 30 5
```

**Opción B — sin el nativo (más rápido, las 5 en JVM):**
```bash
./bench.sh 64 30 5
```

Los números son: `64` conexiones concurrentes, `30` segundos de medición por prueba, `5`
repeticiones (se reporta la mediana). Puedes ajustar CPU/RAM fijas para reproducibilidad:
```bash
BENCH_CPUS=4 BENCH_MEM=2g BENCH_NATIVE=1 ./bench.sh 64 30 5
```

> El script es **resiliente**: si algún framework no construye o no arranca, lo registra con sus
> logs y sigue con los demás. No se detiene todo por uno.

## 5. Resultado

Cuando termine, se generan **dos archivos**:

- `../results/RESULTS-docker.md` → la **tabla** (imagen, arranque, RSS, throughput por framework).
- `../results/raw-docker.csv` → cada repetición en crudo (para estadística: mediana, min, max).

Míralos así:
```bash
cat ../results/RESULTS-docker.md
```

## 6. Qué me tienes que pasar

Pégame (o súbeme) el contenido de **`RESULTS-docker.md`** y **`raw-docker.csv`**. Con eso
consolido los números oficiales del paper. Y dime, para el "Entorno" del paper:
```bash
# ejecuta esto y pásame la salida:
echo "CPU:  $(grep -m1 'model name' /proc/cpuinfo | cut -d: -f2 | xargs)"
echo "Nucleos: $(nproc)"
echo "RAM:  $(free -h | awk '/Mem:/{print $2}')"
echo "Kernel: $(uname -r)"
echo "JDK:  $(java -version 2>&1 | head -1)"
echo "Docker: $(docker --version)"
```

---

## 7. (Opcional) Barrido de concurrencia — para la curva del paper

Si quieres la curva completa throughput/latencia a distintas cargas:
```bash
for c in 1 8 32 64 128 256; do ./bench.sh "$c" 20 3; mv ../results/raw-docker.csv ../results/raw-$c.csv; done
```
Pásame todos los `raw-*.csv`.

## 8. Buenas prácticas para cifras de publicación

- Cierra navegador/IDE y apps pesadas antes de correr.
- Si tienes `cpupower`: `sudo cpupower frequency-set -g performance`.
- Corre 2–3 veces y quédate con la corrida más estable (la 1ª repetición de cada framework, con
  el JIT frío, ya la descarta la mediana).

## 9. Problemas comunes

| Síntoma | Solución |
|---|---|
| `permission denied` al usar docker | No cerraste sesión tras el `usermod -aG docker`. Re-login o `newgrp docker`. |
| `port is already allocated` (8080) | Algo usa el 8080. Ciérralo: `sudo ss -ltnp | grep :8080` y para ese proceso. |
| La fila `quarkus-native` tarda mucho | Normal: compilar a binario nativo con GraalVM toma 5–10 min. Ten paciencia o usa la Opción B. |
| Un framework dice "no arrancó" | El script sigue con los demás; pásame igual el `RESULTS-docker.md` y reviso ese caso. |
| Poco espacio en disco | Las imágenes ocupan ~2–3 GB en total. Libera con `docker system prune -af` al terminar. |

---

Cuando tengas el `RESULTS-docker.md`, me lo pasas y **armamos el paper con las cifras definitivas**.

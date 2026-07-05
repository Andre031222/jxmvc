# Paper — outline de trabajo

**Título (borrador):** *JxMVC: a zero-dependency, security-hardened MVC framework for Jakarta EE
in 253 KB — design, implementation and empirical evaluation.*

**Revista objetivo:** _Software: Practice and Experience_ (Wiley) — tipo "experience/systems paper".
**Alternativa hito rápido:** ICWE 2026 (Springer/Scopus).

**Autoría (acordar):** Dr. Ramiro Pedro Laura Murillo (autor principal, diseño y arquitectura) ·
R. Andre Vilca Solórzano (contribuciones v3.x, evaluación).

---

## Tesis de contribución (el "por qué importa")

Un framework MVC full-stack para Jakarta EE que cabe en **253 KB con cero dependencias en
runtime** y aplica **seguridad por defecto a nivel de pipeline**, siendo a la vez **competitivo
en rendimiento** con Spring Boot, Quarkus, Micronaut y Javalin. No se reclaman "técnicas nuevas":
la contribución es el **punto de diseño** (minimalismo + hardening por defecto + legibilidad
pedagógica) y su **evaluación empírica honesta**.

---

## Estructura

### 1. Abstract
Problema (peso/dependencias/complejidad de los frameworks actuales) → propuesta (JxMVC, 253 KB,
0 deps, secure-by-default) → método (evaluación dockerizada vs 4 frameworks + fila GraalVM nativo)
→ resultado clave (competitivo: **imagen JVM más pequeña** de las cinco, **arranque más rápido que
Spring y Micronaut**, throughput **a la par de Spring y ~1–11% por debajo del resto**, latencia p99
indistinguible, **0 errores en 92.8M requests**; contrapartida: **mayor RSS** por Tomcat) →
disponibilidad (open source + DOI).

> ⚠️ **Corrección basada en datos (corrida nativa en Linux, jul-2026):** el claim previo de
> "throughput *sobre* Spring/Micronaut" **no se sostiene** en bare-metal: JxMVC queda ~1% bajo Spring
> y ~4% bajo Micronaut. La fortaleza real es **imagen mínima + arranque + latencia parejas**, no el pico
> de rps. Redactado honestamente en `sections/06-evaluation.tex`.

### 2. Introduction
- El coste oculto de los frameworks "de baterías incluidas": tamaño, deps transitivas, superficie
  de ataque, curva de aprendizaje, arranque.
- Nichos donde importa: entornos con recursos limitados, edge, docencia, auditabilidad.
- Vacío: casi ningún framework MVC serio es de dependencia-cero **y** secure-by-default.
- **Contribuciones** (lista numerada, ver abajo).

### 3. Background & Related Work
- Panorama: Spring Boot, Quarkus, Micronaut, Javalin, Jakarta EE "crudo" — modelo de deps,
  tamaño, filosofía (runtime vs compile-time/AOT).
- Tabla comparativa cualitativa (deps, JSON propio, pool propio, DI, seguridad por defecto).
- Trabajo previo sobre "microframeworks" y sobre seguridad-por-defecto.

### 4. Design & Architecture
- Visión general y principios (cero deps, secure-by-default, API legible/bilingüe).
- **Pipeline de 15 etapas por request** (figura) — describir el orden real.
- Subsistemas construidos desde cero sobre el JDK+Jakarta: routing (convención+anotaciones+
  plantillas), DI por reflexión con detección de ciclos, pool de conexiones (CAS+keepalive),
  cache (LRU por muestreo), scheduler (cron propio), JSON (parser con límites), validación,
  métricas, OpenAPI, WebSocket, event bus.
- **Seguridad por defecto**: cabeceras en toda respuesta, auth fail-closed, rate-limit por acción
  (sliding window), CSRF token de sesión, caps de body/JSON, anti open-redirect, bloqueo de
  extensiones en uploads, PBKDF2 para passwords.
- **Autenticación OIDC**: OAuth 2.0 Authorization Code + PKCE (S256) y **verificación de la firma
  RS256 del id_token contra JWKS** — todo con criptografía del JDK, sin librerías.

### 5. Implementation (técnicas destacables)
- Router determinista (plantillas ordenadas por especificidad; 405 correcto; HEAD→GET).
- Rate limiter **sliding-window counter** (fórmula prev·(1−t/W)+actual).
- Verificación de id_token con solo `java.security` (JWKS→RSAPublicKey, `SHA256withRSA`,
  validación alg/iss/aud/exp; rechazo de `alg:none`).
- Optimización de hot-path: cache de métodos anotados por clase (evita reflexión por request).
- **Suite de tests propia sin JUnit** (347 verificaciones) — argumento de dependencia-cero
  incluso en pruebas; utilidad de test de controladores sin contenedor.

### 6. Evaluation
- **Preguntas de investigación**: RQ1 tamaño/arranque/memoria; RQ2 throughput/latencia; RQ3
  corrección bajo carga; RQ4 coste de la seguridad-por-defecto.
- **Metodología** (reproducible, dockerizada): misma base JRE, `--cpus`/`--memory` fijos,
  endpoints idénticos, warmup, N repeticiones, mediana. Entorno declarado. (Harness público.)
- **Resultados** (tabla — datos ya obtenidos, WSL2; re-correr en Linux nativo para la versión
  final): imagen, arranque, RSS, rps /plaintext y /json, percentiles, 0 errores en 5.5M+ req.
- **Análisis honesto**: dónde gana JxMVC (imagen, arranque, throughput vs Spring/Micronaut) y
  dónde no (RSS por Tomcat). Comparar modo JVM; añadir fila **nativa GraalVM** para Quarkus/Micronaut.

### 7. Discussion / Threats to Validity
- Validez: VM de Docker (números relativos), warmup/JIT, paridad de endpoints, tamaño core-vs-stack.
- **Limitaciones honestas**: mayor RSS (Tomcat); reflexión en binding/JSON; `@JxQuery` vía
  stack-trace; SQL por concatenación (con mitigaciones); scanners de classpath (file:/jar:);
  cron 5-campos; event bus síncrono; madurez/adopción; un autor principal.
- Amenaza "¿otro framework más?": respuesta = nicho + cifras + valor pedagógico.

### 8. Conclusion & Future Work
- Recap de la contribución y de la evidencia.
- Futuro: reducir RSS, cachear metadata de binding, soporte GraalVM native-image, más proveedores
  OIDC, HTTP/2, SSE.

### 9. Availability
- Repositorio: https://github.com/Andre031222/jxmvc (MIT).
- Release v3.4.0 archivado en **Zenodo** con DOI (pendiente de crear).
- Harness de benchmarks reproducible incluido (`benchmarks/`).

---

## Contribuciones (para la lista numerada del paper)

1. **JxMVC**, framework MVC full-stack para Jakarta EE de **dependencia-cero (253 KB)** que cubre
   routing, DI, pool, cache, scheduler, WebSocket, OpenAPI, validación, métricas y OIDC.
2. **Diseño secure-by-default a nivel de pipeline** (no opt-in) y su implementación sin librerías.
3. **Cliente OIDC completo** (OAuth 2.0 + PKCE + verificación de firma del id_token) con solo el JDK.
4. **Evaluación empírica reproducible y honesta** frente a Spring/Quarkus/Micronaut/Javalin,
   incluyendo un harness dockerizado de un comando (artefacto reutilizable).
5. **Artefacto open source** verificable con suite de pruebas propia (347) y DOI.

---

## Checklist antes de enviar
- [x] Re-correr `bench.sh` en Linux nativo (i5-12500H bare-metal) + fila GraalVM nativo. **Hecho jul-2026.**
- [ ] Figura del pipeline; tabla comparativa cualitativa; ~~tabla de resultados con [min,max]~~ **hecha** (`tables/results.tex`, `tables/latency.tex`).
- [ ] Related work con 15–25 referencias (frameworks, microframeworks, seguridad-por-defecto, OSP).
- [ ] Subir v3.4.0 a Zenodo → DOI; enlazar en el paper.
- [ ] Plantilla LaTeX de Wiley/SPE; carta de presentación (cover letter).
- [ ] Acordar autoría y afiliación.

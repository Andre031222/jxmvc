# Seguridad por defecto

Cada framework con su configuración por defecto y la misma app mínima del benchmark (`benchmarks/docker/apps`).
Generado por `benchmarks/security/run.sh`; las sondas están en `SecProbe.java`. ✅ protege · ❌ no protege · — no aplica.

| # | Comprobación | jxmvc | spring | quarkus | micronaut | javalin |
|---|---|---|---|---|---|---|
| H1 | X-Content-Type-Options: nosniff | ✅ | ❌ | ❌ | ❌ | ❌ |
| H2 | Protección contra clickjacking | ✅ | ❌ | ❌ | ❌ | ❌ |
| H3 | Referrer-Policy | ✅ | ❌ | ❌ | ❌ | ❌ |
| H4 | Content-Security-Policy | ✅ | ❌ | ❌ | ❌ | ❌ |
| H5 | Sin versión del servidor en cabeceras | ✅ | ✅ | ✅ | ✅ | ✅ |
| B1 | Cuerpo de 20 MiB rechazado | ✅ | ❌ | ✅ | ✅ | ✅ |
| B2 | JSON con 100 000 niveles rechazado (4xx) | ✅ | ✅ | — | ✅ | — |
| B3 | JSON truncado responde 4xx, no 5xx | ✅ | ✅ | — | ✅ | — |
| E1 | Excepción sin filtrar detalles internos | ✅ | ✅ | ✅ | ❌ | ✅ |
| C1 | POST entre sitios rechazado (CSRF) | ✅ | ❌ | ❌ | ❌ | ❌ |
| R1 | Sin redirección abierta | ✅ | ❌ | ❌ | ❌ | ❌ |
| X1 | Parámetro reflejado sin <script> ejecutable | ✅ | ❌ | ❌ | ❌ | ❌ |
| T1 | Sin path traversal | ✅ | ✅ | ✅ | ✅ | ✅ |
| M1 | TRACE no refleja la petición | ✅ | ✅ | ✅ | ✅ | ✅ |
| I1 | Sin endpoints internos expuestos | ✅ | ✅ | ✅ | ✅ | ✅ |
| L1 | Fuerza bruta en /login limitada (429) | ❌ | ❌ | ❌ | ❌ | ❌ |
| L2 | Cabecera de 200 KB rechazada | ✅ | ✅ | ✅ | ✅ | ✅ |
| S1 | Transfer-Encoding + Content-Length sin ambigüedad | ✅ | ✅ | ✅ | ✅ | ✅ |
| | **Total** | **17/18** | **9/18** | **8/16** | **9/18** | **8/16** |

## Detalle

**jxmvc**

- H1 PASS: X-Content-Type-Options=nosniff
- H2 PASS: X-Frame-Options=SAMEORIGIN
- H3 PASS: Referrer-Policy=strict-origin-when-cross-origin
- H4 PASS: CSP=base-uri 'self'; object-src 'none'; frame-ancestors 'self'
- H5 PASS: Server= X-Powered-By=
- B1 PASS: 20 MiB -> 413
- B2 PASS: anidamiento 100000 -> 400
- B3 PASS: JSON truncado -> 400
- E1 PASS: excepción -> 500
- C1 PASS: POST entre sitios -> 403
- R1 PASS: redirect -> 302 /https://evil.example/
- X1 PASS: parámetro reflejado -> 200
- T1 PASS: rutas con .. -> 400 400 404 400 400
- M1 PASS: TRACE -> 405
- I1 PASS: expuestos: ninguno
- L1 FAIL: 300 intentos de login -> 0 respuestas 429
- L2 PASS: cabecera de 200 KB -> 400
- S1 PASS: TE+CL ambiguos -> 200

**spring**

- H1 FAIL: X-Content-Type-Options=
- H2 FAIL: X-Frame-Options=
- H3 FAIL: Referrer-Policy=
- H4 FAIL: CSP=
- H5 PASS: Server= X-Powered-By=
- B1 FAIL: 20 MiB -> 200
- B2 PASS: anidamiento 100000 -> 400
- B3 PASS: JSON truncado -> 400
- E1 PASS: excepción -> 500
- C1 FAIL: POST entre sitios -> 200
- R1 FAIL: redirect -> 302 https://evil.example/
- X1 FAIL: parámetro reflejado -> 200
- T1 PASS: rutas con .. -> 400 400 404 400 400
- M1 PASS: TRACE -> 405
- I1 PASS: expuestos: ninguno
- L1 FAIL: 300 intentos de login -> 0 respuestas 429
- L2 PASS: cabecera de 200 KB -> 400
- S1 PASS: TE+CL ambiguos -> 200

**quarkus**

- H1 FAIL: X-Content-Type-Options=
- H2 FAIL: X-Frame-Options=
- H3 FAIL: Referrer-Policy=
- H4 FAIL: CSP=
- H5 PASS: Server= X-Powered-By=
- B1 PASS: 20 MiB -> 413
- B2 N/A: sin binding JSON en la app mínima
- B3 N/A: sin binding JSON en la app mínima
- E1 PASS: excepción -> 500
- C1 FAIL: POST entre sitios -> 200
- R1 FAIL: redirect -> 302 https://evil.example/
- X1 FAIL: parámetro reflejado -> 200
- T1 PASS: rutas con .. -> 404 404 404 404 404
- M1 PASS: TRACE -> 405
- I1 PASS: expuestos: ninguno
- L1 FAIL: 300 intentos de login -> 0 respuestas 429
- L2 PASS: cabecera de 200 KB -> 431
- S1 PASS: TE+CL ambiguos -> 200

**micronaut**

- H1 FAIL: X-Content-Type-Options=
- H2 FAIL: X-Frame-Options=
- H3 FAIL: Referrer-Policy=
- H4 FAIL: CSP=
- H5 PASS: Server= X-Powered-By=
- B1 PASS: 20 MiB -> 413
- B2 PASS: anidamiento 100000 -> 400
- B3 PASS: JSON truncado -> 400
- E1 FAIL: excepción -> 500 (filtra detalle interno)
- C1 FAIL: POST entre sitios -> 200
- R1 FAIL: redirect -> 301 https://evil.example/
- X1 FAIL: parámetro reflejado -> 200
- T1 PASS: rutas con .. -> 404 404 404 404 404
- M1 PASS: TRACE -> 405
- I1 PASS: expuestos: ninguno
- L1 FAIL: 300 intentos de login -> 0 respuestas 429
- L2 PASS: cabecera de 200 KB -> 413
- S1 PASS: TE+CL ambiguos -> 400

**javalin**

- H1 FAIL: X-Content-Type-Options=
- H2 FAIL: X-Frame-Options=
- H3 FAIL: Referrer-Policy=
- H4 FAIL: CSP=
- H5 PASS: Server= X-Powered-By=
- B1 PASS: 20 MiB -> 413
- B2 N/A: sin binding JSON en la app mínima
- B3 N/A: sin binding JSON en la app mínima
- E1 PASS: excepción -> 500
- C1 FAIL: POST entre sitios -> 200
- R1 FAIL: redirect -> 302 https://evil.example/
- X1 FAIL: parámetro reflejado -> 200
- T1 PASS: rutas con .. -> 400 400 404 400 400
- M1 PASS: TRACE -> 404
- I1 PASS: expuestos: ninguno
- L1 FAIL: 300 intentos de login -> 0 respuestas 429
- L2 PASS: cabecera de 200 KB -> 431
- S1 PASS: TE+CL ambiguos -> 400

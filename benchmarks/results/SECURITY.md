# Seguridad por defecto

Cada framework con su configuración por defecto y la misma app mínima del benchmark (`benchmarks/docker/apps`).
Generado por `benchmarks/security/run.sh`; las sondas están en `SecProbe.java`. sí: protege · no: no protege · —: no aplica.

| # | Comprobación | jxmvc | spring | quarkus | micronaut | javalin |
|---|---|---|---|---|---|---|
| H1 | X-Content-Type-Options: nosniff | sí | no | no | no | no |
| H2 | Protección contra clickjacking | sí | no | no | no | no |
| H3 | Referrer-Policy | sí | no | no | no | no |
| H4 | Content-Security-Policy | sí | no | no | no | no |
| H5 | Sin versión del servidor en cabeceras | sí | sí | sí | sí | sí |
| B1 | Cuerpo de 20 MiB rechazado | sí | no | sí | sí | sí |
| B2 | JSON con 100 000 niveles rechazado (4xx) | sí | sí | — | sí | — |
| B3 | JSON truncado responde 4xx, no 5xx | sí | sí | — | sí | — |
| E1 | Excepción sin filtrar detalles internos | sí | sí | sí | no | sí |
| C1 | POST entre sitios rechazado (CSRF) | sí | no | no | no | no |
| R1 | Sin redirección abierta | sí | no | no | no | no |
| X1 | Parámetro reflejado sin <script> ejecutable | sí | no | no | no | no |
| T1 | Sin path traversal | sí | sí | sí | sí | sí |
| M1 | TRACE no refleja la petición | sí | sí | sí | sí | sí |
| I1 | Sin endpoints internos expuestos | sí | sí | sí | sí | sí |
| L1 | Fuerza bruta en /login limitada (429) | no | no | no | no | no |
| L2 | Cabecera de 200 KB rechazada | sí | sí | sí | sí | sí |
| S1 | Transfer-Encoding + Content-Length sin ambigüedad | sí | sí | sí | sí | sí |
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

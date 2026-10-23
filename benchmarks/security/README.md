# Evaluación de seguridad por defecto

Mide qué protecciones trae cada framework **sin configurar nada**, sobre la misma app mínima
que usa el benchmark de rendimiento (`benchmarks/docker/apps`). No mide lo que cada framework
puede llegar a hacer: Spring Boot con Spring Security o Quarkus con sus extensiones de
seguridad ganarían varias casillas. Lo que compara es el punto de partida que recibe una app
recién creada.

## Cómo se corre

```bash
cd benchmarks/security
./run.sh                       # los cinco frameworks
SEC_APPS="jxmvc spring" ./run.sh
```

Requiere Docker en marcha y un JDK en el host. Escribe `benchmarks/results/SECURITY.md` y
`benchmarks/results/raw-security.tsv`.

## Reglas de la comparación

- Cada app expone los mismos endpoints, escritos con el estilo propio de cada framework:
  `POST /echo`, `POST /json-echo`, `GET /boom`, `GET /go?to=`, `GET /hello?name=` y
  `POST /login`.
- Ningún framework recibe módulos ni propiedades de seguridad. JxMVC usa el perfil `prod`, el
  mismo del benchmark de rendimiento.
- Quarkus y Javalin no traen binding JSON en su app mínima. B2 y B3 no aplican para ellos, en
  vez de agregarles una dependencia que cambiaría sus imágenes.
- Las sondas viajan con `Host: bench.example`. Así no cuenta la protección que Micronaut aplica
  solo cuando el destino es `localhost`.
- Cada prueba exige el código esperado (403, 413, 400…). Un 404 casual no cuenta como protección.

## Las sondas

| # | Qué envía | Aprueba si |
|---|---|---|
| H1–H4 | `GET /plaintext` | trae `nosniff`, protección contra clickjacking, `Referrer-Policy` y CSP |
| H5 | `GET /plaintext` | no publica la versión del servidor |
| B1 | 20 MiB a `/echo` | 413 o 400, o la conexión se corta |
| B2 | JSON con 100 000 niveles | 400, 413 o 422 |
| B3 | JSON truncado `{"a":` | 400 o 422 |
| E1 | excepción con un dato secreto | la respuesta no contiene el dato, la clase ni la traza |
| C1 | POST con `Origin` y `Sec-Fetch-Site` de otro sitio | 403, 401 o 400 |
| R1 | `/go?to=https://evil.example/` | no redirige al dominio externo |
| X1 | `/hello?name=<script>…` | la respuesta no contiene el `<script>` ejecutable |
| T1 | rutas con `..`, `%2e%2e` y `..;` | ninguna devuelve archivos del sistema ni `web.xml` |
| M1 | `TRACE` | no refleja la petición |
| I1 | `/actuator/*`, `/q/dev`, `/jx/*` | ninguno responde 200 |
| L1 | 300 POST a `/login` | aparece algún 429 |
| L2 | cabecera de 200 KB | se rechaza |
| S1 | `Transfer-Encoding` y `Content-Length` a la vez | se rechaza o gana `Transfer-Encoding` |

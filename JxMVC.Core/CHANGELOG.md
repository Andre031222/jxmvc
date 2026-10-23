# Changelog — JxMVC Core

## 3.5.0 — 2026-10-23

Lectura y generación de PDF sin dependencias, endurecimientos encontrados por una evaluación de seguridad automatizada y un instalador de un comando.

### ⚠ Cambios de comportamiento

1. **JSON estricto**: `JxJson.fromJson` (y por tanto `@JxBody`) rechaza con **400** el JSON mal formado: llaves o corchetes sin cerrar, comas sobrantes, contenido después del valor, literales y números inválidos, escapes inválidos y caracteres de control sin escapar. Antes los aceptaba y devolvía datos incompletos.
2. **Código de error real sin `error.jsp`**: si la app no define `/WEB-INF/views/shared/error.jsp`, los errores (403, 413, 500…) se responden con su propio código en texto plano. Antes el reenvío a la vista inexistente los convertía en 404.
3. **CSP mínima por defecto**: toda respuesta lleva `Content-Security-Policy: base-uri 'self'; object-src 'none'; frame-ancestors 'self'` (`'none'` si `jxmvc.security.frame-options=DENY`). No restringe scripts ni estilos. `jxmvc.security.csp` la reemplaza y `jxmvc.security.csp=false` la quita.

### Nuevo

- **JxPdf**: lectura y generación de PDF sin dependencias externas, en reemplazo de iText/PDFBox para los casos comunes.
  - `JxPdf.read(path)` devuelve el texto por página y el diccionario `/Info`; `JxPdf.read(path, contraseña)` abre PDF cifrados con contraseña de usuario o de propietario.
  - Cifrado estándar completo: RC4 40/128 bits (R2–R4), AES-128 (AESV2) y AES-256 (R5 y R6), solo con `javax.crypto` del JDK.
  - Filtros FlateDecode, LZWDecode, ASCII85Decode, ASCIIHexDecode y RunLengthDecode, encadenados y con predictores PNG/TIFF; object streams y xref rotas toleradas.
  - Texto con espaciado por geometría real (anchos `/Widths` y `/W`, `Tc`, `Tw`, `TJ`, `cm`), fuentes con `ToUnicode`, `/Differences` y Type3, y composición de tildes de TeX (`´o` → `ó`).
  - `doc.isScanned(i)` marca las páginas que son solo imagen; no hay OCR, porque requeriría una dependencia.
  - `JxPdf.create(título, texto)` genera un PDF de texto plano en A4 con paginado automático.
  - Entrada no confiable: tope de 64 MB por stream descomprimido, anidamiento máximo de 64 y errores siempre como `IllegalArgumentException`.

- **Instalador** `install.sh` (macOS/Linux) e `install.ps1` (Windows): descarga el último release, compila e instala `jxmvc-core`, y con `new <nombre>` crea un proyecto listo para `mvn package cargo:run`.
- **Evaluación de seguridad por defecto** (`benchmarks/security`): 18 sondas sobre la misma app mínima en los cinco frameworks. JxMVC 17/18, Spring Boot 9/18, Micronaut 9/18, Quarkus 8/16, Javalin 8/16.
- **`BENCH_HEAP`** en `benchmarks/docker/bench.sh` fija el mismo heap para todos los frameworks JVM.

### Tests

- **JsonTest**: 17 verificaciones nuevas del parser estricto. **SecurityTest**: CSP por defecto. Total: **425** en verde.
- **PdfTest** (58 verificaciones): cifrados generados con una herramienta externa, los cinco filtros, páginas escaneadas, tildes de TeX, espaciado, 400 PDF corruptos y una *zip bomb*. Contra `pdftotext` sobre 80 PDF reales recupera el 98,7 % de las palabras (mediana 99,5 %).

## 3.4.0 — 2026-07-04

Autenticación de fábrica (OAuth 2.0 + PKCE con verificación OIDC y hashing PBKDF2), endurecimientos y una mejora de rendimiento en el hot-path — todo sin dependencias externas. API pública intacta; lo nuevo es opt-in.

### Nuevo

- **JxOAuth**: flujo *Authorization Code* con PKCE (S256) para OpenID Connect, con preset de **Google**. Construye la URL de consentimiento, intercambia el código y obtiene la identidad apoyándose en `JxHttp`/`JxJson` — cero dependencias. **Verifica la firma RS256 del `id_token`** contra el JWKS del proveedor (con caché) y valida `alg`/`iss`/`aud`/`exp` usando solo criptografía del JDK; cae a userinfo si el proveedor no publica JWKS.
- **JxPasswords**: hashing PBKDF2-SHA256 con salt e iteraciones embebidas, verificación en tiempo constante y `needsRehash`.

### Cambiado / corregido

- **JxRateLimiter**: ahora es un **sliding window counter** real (pondera la ventana previa) — antes el javadoc decía "ventana deslizante" pero el algoritmo era de ventana fija y permitía una ráfaga del doble del límite en el borde.
- **Rendimiento (hot-path)**: los métodos anotados `@JxBeforeAction`/`@JxAfterAction`/`@JxModelAttr` se **cachean por clase de controlador** en lugar de escanear todos los métodos por reflexión en cada petición.
- **Documentación del pipeline** alineada con el orden real del código (rate-limit tras resolver la ruta, CSRF explícito, sin "perfil" como etapa).

### Tests

- **Nuevas verificaciones** en `OAuthTest` (verificación de firma del id_token con par RSA generado en el test: válido, firma manipulada, `aud`/`iss`/`exp` incorrectos, `alg:none` rechazado, reconstrucción de clave desde JWK) y `RateLimiterTest` (estimación de ventana deslizante, arrastre y reseteo). Total: **347** en verde.

## 3.3.0 — 2026-07-04

Routing endurecido, capas de seguridad nuevas (CSRF, límites, anti open-redirect) y métricas más ricas. API pública intacta; todo lo nuevo es opt-in o corrección de comportamiento incorrecto.

### ⚠ Cambios de comportamiento

1. **Rate limiting por acción**: la clave del bucket es la acción resuelta (`VERBO:controlador#accion`), no la URI cruda — rotar `/api/{id}` ya no evade el límite.
2. **405 correcto**: pedir una ruta de plantilla (`{id}`) con el verbo equivocado responde 405 (antes 404). `HEAD` se resuelve como `GET` (antes 405).
3. **Auth fail-closed**: una ruta protegida sin `JxAuthProvider` configurado deniega el acceso (antes lo permitía).
4. **`@JxBody` exige `application/json`** (o `*+json`); otros `Content-Type` responden 415.
5. **Redirecciones externas bloqueadas** por defecto (anti open-redirect); se habilitan con `jxmvc.redirect.external=true`.
6. **Body no-multipart con tope** (10 MB por defecto); al excederlo responde 413.
7. **Parámetros `String` de método** pasan por el sanitizador, consistente con `model.param()`.

### Nuevo

- **JxCsrf**: protección CSRF por token de sesión (`jxmvc.security.csrf=true`), campo `_csrf` o cabecera `X-CSRF-Token`, comparación en tiempo constante, `@JxCsrfExempt` para APIs. Funciones EL `jx:csrf` / `jx:csrfToken`.
- **JxHtml** (`jx:esc`): codificación de salida HTML — la defensa XSS en el punto correcto.
- **JxMetrics**: mín/máx por ruta y contadores por clase de estado (1xx–5xx) en `/jx/metrics`.
- **Router determinista**: rutas de plantilla ordenadas por especificidad (antes dependían del orden del `HashSet` del escaneo); las colisiones de rutas se registran con `log.warn`.

### Corregido

- **extraArgs**: argumentos posicionales calculados por segmentos — `//` dobles o mayúsculas en el path ya no desalinean los args; además se URL-decodifican (consistente con `{vars}`).
- **JxRateLimiter**: la limpieza respeta ventanas largas activas (antes un bucket de 1 h se reseteaba a los 10 min); daemon único de limpieza.
- **JxCache**: desalojo LRU aproximado por muestreo (estilo Redis) — sin escaneo O(n) por `put` a tope; el daemon de limpieza se detiene en el shutdown (fuga de classloader).
- **JxDB**: ciclo de vida de conexiones centralizado en `withConn` (sin fugas en caminos de error).
- **@JxAsync**: los argumentos se resuelven con el request vivo (Tomcat recicla el objeto al retornar) y la clave de métrica se normaliza.
- **Métricas**: los segmentos variables (números, UUIDs, hex) se colapsan a `{n}` — cardinalidad acotada.
- **render()**: si la vista falla con la respuesta ya comprometida, no se anexa el fallback JSON (respuesta mixta).
- **CORS**: `Vary: Origin` también en el camino same-host.
- **Mensajes de error** sin nombres de clases/paquetes internos hacia el cliente.

### Nuevas propiedades

```properties
jxmvc.security.csrf=false      # protección CSRF por token de sesión
jxmvc.body.maxBytes=10485760   # tope del body no-multipart (413 al exceder)
jxmvc.redirect.external=false  # permitir redirecciones a otros dominios
```

### Tests

305 verificaciones (30 nuevas): CSRF (token, header, campo, exención), open redirect, body cap 413, min/máx y clases de estado en métricas, ventanas largas del rate limiter, extraArgs con dobles slashes/mayúsculas/URL-encoding, colapso de cardinalidad, escape HTML.

---

## 3.2.0 — 2026-06-12

Auditoría completa de producción: concurrencia, recursos y seguridad. Cero dependencias, API pública intacta.

### ⚠ Cambios de comportamiento

1. **Errores en producción**: con perfil `prod` (y devMode inactivo) los mensajes de excepciones no controladas ya no llegan al cliente; se responde un mensaje genérico y el detalle completo va al log. Las `JxException` lanzadas por el desarrollador conservan su mensaje siempre.
2. **Endpoints internos**: `/jx/info`, `/jx/metrics` y `/jx/openapi` responden 404 en `prod` salvo `jxmvc.internal.expose=true`. `/jx/health` sigue disponible pero omite `version`/`profile`/`devMode` en prod.
3. **Rutas con `..`**: cualquier path con `..` o `\` se rechaza (404), tanto en el dispatcher como al resolver vistas JSP.

### Corregido

- **JxPool**: contabilidad exacta del pool bajo concurrencia (las conexiones muertas se cierran de verdad y el contador no se corrompe en ningún camino, incluido fallo de `open()`); el pool ya no puede exceder `maxSize` ni filtrar sockets.
- **JxGzip**: las respuestas ya no se retienen completas en memoria; al superar `maxBytes` pasan a streaming directo sin comprimir (elimina riesgo de OOM).
- **JxCache**: tope de entradas con desalojo de la menos accedida; `computeIfAbsent` sin ventana de stampede.
- **JxWebSocket**: envío serializado por sesión (el Basic remote no es thread-safe); las sesiones también se limpian en `onError`.
- **Errores JSON**: escape correcto con el serializador propio (antes un `replace` de comillas que rompía con backslash/saltos de línea).
- **Content-Disposition**: nombre de archivo saneado contra inyección de cabeceras (CRLF/comillas).
- **JxHttp**: solo esquemas `http`/`https`.
- **JxJson**: el binding JSON→POJO ignora campos `static`/`final`/`transient` (mass assignment) y registra fallos de mapeo en nivel debug.
- **Jakarta EE** alineado a 10.0.0 (baseline Tomcat 10.1).

### Nuevas propiedades

```properties
jxmvc.pool.validationInterval=30   # seg sin revalidar conexión contra la BD (0 = siempre)
jxmvc.cache.maxEntries=10000       # tope por caché nominada (0 = sin límite)
jxmvc.gzip.maxBytes=1048576        # umbral de passthrough sin compresión
jxmvc.internal.expose=false        # forzar /jx/* internos en prod
jxmvc.security.csp=                # Content-Security-Policy (vacío = sin header)
jxmvc.ws.maxConnections=0          # tope global de conexiones WebSocket (0 = sin límite)
```

### Tests

275 verificaciones (54 nuevas): pool bajo concurrencia (50 threads), gzip con passthrough, stampede de caché, broadcast WebSocket concurrente, escape de errores, path traversal, mass assignment.

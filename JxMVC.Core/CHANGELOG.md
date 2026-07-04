# Changelog — JxMVC Core

## 3.4.0 — 2026-07-04

Autenticación de fábrica: inicio de sesión con **Google** (OAuth 2.0 + PKCE) y hashing de contraseñas, ambos sin dependencias externas. API pública intacta; todo lo nuevo es opt-in.

### Nuevo

- **JxOAuth**: flujo *Authorization Code* con PKCE (S256) para OpenID Connect, con preset de **Google** listo para usar. Construye la URL de consentimiento, intercambia el código y recupera los claims del usuario apoyándose en `JxHttp` y `JxJson` — cero dependencias. Se configura con `jxmvc.oauth.google.client-id` / `client-secret` / `redirect-uri` (o variables de entorno equivalentes).
- **JxPasswords**: hashing de contraseñas PBKDF2-SHA256 con salt aleatorio, número de iteraciones embebido en el propio hash, verificación en tiempo constante y `needsRehash` para reforzar hashes antiguos.

### Tests

- **28 verificaciones nuevas** (`OAuthTest`): vector PKCE del RFC 7636, construcción de la URL de autorización, tokens URL-safe y roundtrip de PBKDF2. Total: **333** en verde.

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

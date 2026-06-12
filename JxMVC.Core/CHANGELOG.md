# Changelog — JxMVC Core

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

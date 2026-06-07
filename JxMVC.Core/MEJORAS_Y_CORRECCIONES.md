# JxMVC — Mejoras y Correcciones Identificadas

> Documento generado a partir del uso real del framework en el proyecto **Soft_Matriculas_Java**.
> Versión evaluada: **3.1.1** · Fecha: 2026-05-19

---

## 1. Bugs críticos corregidos en esta versión

### 1.1 `normalize()` baja en minúsculas los valores de path variables

**Problema:**  
El método `normalize()` de `MainLxServlet` aplica `.toLowerCase()` a toda la URI incluyendo los segmentos que luego se extraen como `@JxPathVar`. Esto hace imposible leer valores que requieren mayúsculas (por ejemplo, códigos de matrícula como `MAT-2026-000001`).

**Síntoma:**
```
GET /api/matriculas/buscar/MAT-2026-000001
→ path variable recibida: "mat-2026-000001"
→ la consulta SQL no encuentra nada → 404
```

**Solución aplicada en controlador (workaround):**
```java
@JxGetMapping("buscar/{codigo}")
public ActionResult buscarPorCodigo(@JxPathVar("codigo") String codigo) {
    Map<String, Object> m = MatriculaModel.buscarPorCodigo(
        codigo != null ? codigo.toUpperCase() : "");
    ...
}
```

**Solución correcta en el framework:**  
`normalize()` solo debería normalizar la *ruta estructural* (slashes, prefijos), nunca los valores concretos de los segmentos variables. Los segmentos deben extraerse **antes** de normalizar, o la normalización debe excluir segmentos después de reconocer que corresponden a un `{placeholder}`.

---

### 1.2 Directorio temporal de multipart no creado en Tomcat Cargo (modo embebido)

**Problema:**  
En modo `cargo:run` (Tomcat embebido), el atributo de contexto `javax.servlet.context.tempdir` es `null`. Tomcat 11 espera que el directorio `{catalina.base}/work/Catalina/localhost/{context}/tmp` exista **antes** de procesar el primer request multipart. Si no existe, lanza:

```
IllegalArgumentException: No es válida la localización ...\matriculas\tmp
```

**Solución aplicada en `MainLxServlet.init()`:**
```java
@Override
public void init() throws ServletException {
    String catalinaBase = System.getProperty("catalina.base");
    String contextName  = getServletContext().getContextPath().replaceAll("^/+", "");
    if (contextName.isEmpty()) contextName = "ROOT";
    File multipartTmp;
    if (catalinaBase != null) {
        multipartTmp = new File(catalinaBase,
                "work/Catalina/localhost/" + contextName + "/tmp");
    } else {
        File tomcatHome = new File(System.getProperty("java.io.tmpdir")).getParentFile();
        multipartTmp = new File(tomcatHome,
                "work/Catalina/localhost/" + contextName + "/tmp");
    }
    multipartTmp.mkdirs();
    if (getServletContext().getAttribute("javax.servlet.context.tempdir") == null) {
        File up = multipartTmp.getParentFile();
        up.mkdirs();
        getServletContext().setAttribute("javax.servlet.context.tempdir", up);
    }
    // ... resto del init
}
```

**Recomendación para el framework:**  
Incluir esta lógica directamente en `MainLxServlet` como comportamiento estándar, o documentar que la aplicación debe configurar `multipartConfig` con una `location` absoluta explícita.

---

## 2. Mejoras de diseño recomendadas

### 2.1 Soporte para rutas raíz en controladores

**Situación actual:**  
Para listar un recurso se debe usar una sub-ruta explícita:
```java
@JxControllerMapping("api/sedes")
@JxGetMapping("list")      // → GET /api/sedes/list  ✓
@JxGetMapping("")          // → GET /api/sedes        ✗ (no funciona)
```

**Mejora propuesta:**  
Permitir que `@JxGetMapping("")` o `@JxGetMapping("/")` capture `GET /api/sedes` (ruta raíz del controlador), compatible con convenciones REST estándar.

---

### 2.2 `@JxPathVar` debe permitir valores mixtos (mayúsculas/minúsculas)

Como extensión de la corrección 1.1: agregar una opción para deshabilitar la normalización por ruta o por variable:

```java
// Propuesta de anotación
@JxGetMapping(value = "buscar/{codigo}", normalize = false)
// o en la variable:
@JxPathVar(value = "codigo", raw = true)
```

---

### 2.3 Respuesta de error genérica cuando no existe JSP de error

**Situación actual:**  
Cuando un endpoint lanza una excepción no controlada, el framework intenta renderizar `/WEB-INF/views/shared/error.jsp`. Si ese archivo no existe (proyectos API-only), Tomcat devuelve un segundo 404 con HTML, lo que confunde al cliente JSON.

**Mejora propuesta:**  
En `MainLxServlet`, si la aplicación es de tipo API (no usa vistas JSP), capturar excepciones no controladas y responder siempre con JSON:
```json
{"ok": false, "error": "Error interno del servidor", "status": 500}
```

Alternativa: agregar un manejador global de excepciones configurable:
```java
@JxExceptionHandler
public ActionResult handleException(Exception e) {
    return respondError("Error interno", 500);
}
```

---

### 2.4 `cargo:redeploy` corrompe el ClassLoader — documentar y prevenir

**Problema observado:**  
Usar `mvn cargo:redeploy` mientras Tomcat está corriendo causa `IllegalStateException: NoSuchFileException: checker-qual-3.42.0.jar` porque el nuevo JAR reemplaza al que el ClassLoader tiene abierto.

**Workaround documentado:**  
Siempre matar el proceso Tomcat primero y luego hacer `cargo:run`. Nunca usar `cargo:redeploy`.

**Mejora propuesta:**  
- Documentar esto en el README del framework.
- Considerar usar `cargo:run` con `<daemon>false</daemon>` para forzar que el proceso esté en primer plano y sea fácil de detener con Ctrl+C.

---

### 2.5 Nombres de `@JxPathVar` deben coincidir en minúsculas con el placeholder

**Situación actual (consecuencia de 1.1):**  
Dado que `normalize()` baja en minúsculas los nombres de los segmentos, si el placeholder en la anotación tiene mayúsculas (`{idSede}`), el matching falla silenciosamente y la variable llega como `null`.

```java
// ✗ Falla si normalize() actúa sobre la clave
@JxGetMapping("niveles/{idSede}")
public ActionResult listar(@JxPathVar("idSede") String id) { ... }

// ✓ Correcto — usar solo minúsculas
@JxGetMapping("niveles/{idsede}")
public ActionResult listar(@JxPathVar("idsede") String id) { ... }
```

**Mejora propuesta:**  
Normalizar solo las *claves* del matching (nombres de placeholders) pero nunca los *valores* extraídos.

---

## 3. Mejoras de funcionalidad futura

| Prioridad | Mejora |
|-----------|--------|
| Alta | Soporte para `@JxGetMapping("")` en ruta raíz del controlador |
| Alta | Handler global de excepciones no controladas → siempre JSON |
| Media | Opción para deshabilitar normalización por ruta o por variable |
| Media | `@JxBody` para deserialización automática de JSON a POJO |
| Media | Interceptores pre/post por controlador (`@JxBefore`, `@JxAfter`) |
| Baja | Validación de parámetros con anotaciones (`@JxNotBlank`, `@JxMin`) |
| Baja | Soporte para respuestas paginadas (`@JxPage`) |
| Baja | Generación automática de documentación de endpoints en `/api/docs` |

---

## 4. Notas de compatibilidad

- **Java 17+** requerido (uso de `instanceof` con pattern matching, records, etc.)
- **Tomcat 10+** requerido (namespace `jakarta.*` en lugar de `javax.*`)
- **`@MultipartConfig`** en `MainLxServlet` con `location = ""` hace que Tomcat use el directorio de trabajo del contexto — puede ser `null` en Cargo embebido (ver corrección 1.2)
- **Maven Cargo 1.10.x** con Tomcat 11.0.2: no usar `cargo:redeploy` (ver 2.4)

---

## 5. Convenciones de rutas validadas en producción

```
GET    /api/{recurso}/list          → listar todos
GET    /api/{recurso}/{id}          → obtener uno por ID
POST   /api/{recurso}/create        → crear
PUT    /api/{recurso}/{id}          → actualizar
PUT    /api/{recurso}/{id}/toggle   → activar/desactivar
DELETE /api/{recurso}/{id}          → eliminar

# Rutas especiales validadas:
GET    /api/matriculas/buscar/{codigo}    → búsqueda pública por código
GET    /api/matriculas/dashboard         → resumen estadístico
GET    /api/academico/niveles/{idsede}   → anidado por ID (minúsculas obligatorio)
```

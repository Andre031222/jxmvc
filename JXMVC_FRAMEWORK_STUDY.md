# JxMVC / Lux — Documentacion Tecnica Completa

Autor principal: Dr. Ramiro Pedro Laura Murillo  
Contribuciones: R. Andre Vilca Solorzano  
Version: 3.0.0  
Periodo de desarrollo: 2024 – 2026  
Licencia: MIT  
Repositorio demo: https://jxmvc.andre.net.pe

---

## Indice

1. [Filosofia y objetivos](#1-filosofia-y-objetivos)
2. [Arquitectura general](#2-arquitectura-general)
3. [Pipeline de 14 etapas](#3-pipeline-de-14-etapas)
4. [Modulo de routing](#4-modulo-de-routing)
5. [Controladores y vistas](#5-controladores-y-vistas)
6. [Inyeccion de dependencias](#6-inyeccion-de-dependencias)
7. [Base de datos — JxPool y JxDB](#7-base-de-datos--jxpool-y-jxdb)
8. [JxRepository — CRUD generico](#8-jxrepository--crud-generico)
9. [Validacion](#9-validacion)
10. [Seguridad — Auth y Roles](#10-seguridad--auth-y-roles)
11. [CORS](#11-cors)
12. [Rate Limiting](#12-rate-limiting)
13. [Filtros](#13-filtros)
14. [Cache](#14-cache)
15. [Scheduler](#15-scheduler)
16. [Eventos](#16-eventos)
17. [Transacciones](#17-transacciones)
18. [Async y Retry](#18-async-y-retry)
19. [Multi-datasource](#19-multi-datasource)
20. [JSON propio](#20-json-propio)
21. [Metricas](#21-metricas)
22. [OpenAPI](#22-openapi)
23. [Endpoints internos del sistema](#23-endpoints-internos-del-sistema)
24. [Catalogo completo de anotaciones](#24-catalogo-completo-de-anotaciones)
25. [Catalogo de clases del core](#25-catalogo-de-clases-del-core)
26. [Configuracion — application.properties](#26-configuracion--applicationproperties)
27. [Testing con JxTest](#27-testing-con-jxtest)
28. [Sitio demo — JxMVC2x](#28-sitio-demo--jxmvc2x)
29. [Comparativa de rendimiento](#29-comparativa-de-rendimiento)
30. [Bugs encontrados y corregidos](#30-bugs-encontrados-y-corregidos)
31. [Despliegue en produccion](#31-despliegue-en-produccion)

---

## 1. Filosofia y objetivos

JxMVC nacio con una premisa especifica: construir un framework MVC para Jakarta EE que no dependa de ninguna libreria externa en runtime. Ni Spring, ni Hibernate, ni Jackson, ni ninguna otra. Todo lo que la aplicacion necesita para funcionar esta dentro del propio JAR del framework.

Los objetivos de diseno son tres:

**Tamano minimo.** El JAR completo pesa aproximadamente 177 KB. Esto es posible porque se implementaron desde cero todas las capas: JSON, validacion, pool de conexiones, planificador de tareas, bus de eventos, metricas y documentacion OpenAPI. Al no incluir librerias de terceros, el WAR resultante de una aplicacion tipica pesa menos de 2 MB.

**Arranque rapido.** El ciclo de vida completo de inicio (escaneo de controladores, registro de servicios, inicio del pool, arranque del scheduler) tarda menos de 500 ms en hardware convencional. Comparado con Spring Boot, que puede tardar entre 3 y 8 segundos dependiendo de la cantidad de beans y configuracion automatica, la diferencia es significativa en entornos con reinicios frecuentes o desplegados en contenedores.

**Pipeline predecible.** Cada peticion HTTP pasa por exactamente las mismas 14 etapas en el mismo orden, siempre. No hay magia de auto-configuracion ni beans condicionales. El desarrollador puede leer el codigo de `MainLxServlet` de principio a fin y entender exactamente que hace el framework con cada request.

---

## 2. Arquitectura general

```
JxMVC.Core (JAR ~177 KB)
    jxmvc.core
        MainLxServlet         Punto de entrada unico (Servlet 6.0)
        BaseDispatcher        Resolucion de rutas
        BaseCorsResolver      Politica CORS por anotacion
        BaseDispatchPlan      Plan de despacho resuelto por cada request
        BaseSanitizer         Limpieza de entradas
        BaseDbResolver        Resolucion de drivers JDBC
        JxMapping             Todas las anotaciones del framework (44 en total)
        JxController          Clase base de todos los controladores
        JxRequest             Abstraccion de HttpServletRequest
        JxResponse            Abstraccion de HttpServletResponse
        JxHttp                Constantes HTTP
        ActionResult          Resultado de una accion (view, json, redirect, text, raw)
        JxPageResult          Resultado paginado para listados
        JxDB                  Cliente JDBC de alto nivel
        DBRow                 Fila de resultado como Map tipado
        DBRowSet              Coleccion de DBRow
        JxPool                Pool de conexiones propio
        JxDataSourceRegistry  Registro de datasources multiples
        JxRepository          CRUD generico con reflection
        JxValidation          Motor de validacion de campos
        JxJson                Serializador/deserializador JSON sin dependencias
        JxLogger              Logger interno basado en SLF4J-simple
        JxMetrics             Metricas por ruta en memoria
        JxOpenApi             Generador de spec OpenAPI 3.0
        JxSecurity            Registro del proveedor de autenticacion
        JxAuthProvider        Interfaz de autenticacion personalizable
        JxFilter              Interfaz de filtros before/after
        JxFilterContext       Contexto pasado a los filtros
        JxFilters             Registro global de filtros programaticos
        JxRateLimiter         Limitador de tasa por IP con ventana deslizante
        JxCache               API de cache con backend intercambiable
        JxCacheBackend        Interfaz del backend de cache (in-memory o externo)
        JxScheduler           Planificador de tareas anotado con @JxScheduled
        JxEventBus            Bus de eventos publish/subscribe
        JxTransaction         Gestor de transacciones JDBC
        JxServiceRegistry     Contenedor de inyeccion de dependencias
        JxAdviceRegistry      Manejador global de excepciones (@JxControllerAdvice)
        JxEnvironment         Acceso a propiedades del sistema y profiles
        JxConfigBinder        Vinculacion de properties a POJOs (@JxConfigProperties)
        JxProfile             Evaluacion de perfiles activos
        JxException           Excepcion HTTP con codigo de estado
        JxTest                Utilidades para pruebas de controladores
        JxTagFor              Tag JSP personalizado: bucle
        JxTagIf               Tag JSP personalizado: condicional

Aplicacion (WAR)
    WEB-INF/web.xml           Mapeo del servlet y configuracion Jakarta EE
    WEB-INF/views/            Vistas JSP
    assets/                   Archivos estaticos (CSS, JS, imagenes)
    application.properties    Configuracion del framework
```

El WAR no tiene ninguna dependencia en runtime mas alla de la API de Jakarta EE (que provee el servidor de aplicaciones) y el driver JDBC de la base de datos elegida.

---

## 3. Pipeline de 14 etapas

Cada peticion HTTP que llega a `MainLxServlet` pasa por las siguientes etapas en orden estricto. Si alguna etapa falla o interrumpe el flujo, las etapas posteriores no se ejecutan.

```
Peticion HTTP entrante
        |
        v
Etapa 01 — Endpoints internos
        Verifica si la ruta empieza por /jx/.
        Si es /jx/health, /jx/info, /jx/metrics o /jx/openapi,
        la respuesta se genera directamente y el pipeline termina aqui.
        |
        v
Etapa 02 — Metricas inicio
        Se registra el timestamp de inicio para calcular la latencia
        al final del pipeline. El contador de peticiones por ruta
        se incrementa aqui.
        |
        v
Etapa 03 — Rate limiting
        Si el metodo o controlador tiene @JxRateLimit, se verifica
        si la IP del cliente ha superado el limite de peticiones
        en la ventana de tiempo configurada.
        Si supera el limite → 429 Too Many Requests.
        Implementacion: sliding window con ConcurrentHashMap en memoria.
        |
        v
Etapa 04 — Resolucion de ruta
        BaseDispatcher busca el controlador y metodo que corresponden
        a la ruta y verbo HTTP de la peticion.
        Estrategias de busqueda (en orden de prioridad):
          1. Ruta exacta
          2. Plantilla con variables ({id}, {slug}, etc.)
          3. Convencion: /controlador/accion
          4. Controlador principal (@JxControllerMain) para la raiz
        Si no se encuentra ninguna coincidencia → 404 Not Found.
        |
        v
Etapa 05 — Perfil de ejecucion
        Si el metodo o controlador tiene @JxProfile, se verifica
        si el perfil activo (configurado en application.properties)
        coincide con los perfiles permitidos.
        Si no coincide → 404 (el recurso no existe para ese entorno).
        |
        v
Etapa 06 — Autenticacion y roles
        Si el metodo o controlador tiene @JxAuth, se llama al
        JxAuthProvider registrado para verificar si el usuario
        esta autenticado y tiene los roles requeridos.
        Sin autenticacion → 401 Unauthorized.
        Sin rol requerido → 403 Forbidden.
        |
        v
Etapa 07 — CORS
        BaseCorsResolver evalua la politica CORS.
        Si hay cabecera Origin:
          - Mismo host que el servidor → permitido sin restriccion.
          - Origin distinto + anotacion @JxCors → valida origins, methods, hosts.
          - Origin distinto + sin @JxCors → 403 Forbidden.
        En preflights (OPTIONS) se generan las cabeceras y termina aqui.
        |
        v
Etapa 08 — Filtros before
        Se ejecutan los JxFilter registrados en JxFilters.before()
        y los @JxFilter anotados en el controlador.
        Orden de ejecucion: filtros globales primero, luego por controlador.
        Un filtro puede interrumpir el pipeline llamando context.stop().
        |
        v
Etapa 09 — Instanciacion + DI
        Se crea una nueva instancia del controlador por reflection.
        Se inyectan los campos anotados con @JxInject desde
        JxServiceRegistry (singletons o prototypes segun @JxScope).
        |
        v
Etapa 10 — @JxBeforeAction
        Se ejecutan los metodos del controlador anotados con
        @JxBeforeAction. Pueden modificar el modelo de la vista
        o lanzar excepciones para interrumpir.
        |
        v
Etapa 11 — @JxModelAttr
        Se ejecutan los metodos anotados con @JxModelAttr.
        Su valor de retorno se agrega al modelo de la vista como
        atributo de request con el nombre indicado.
        |
        v
Etapa 12 — Invocacion de la accion
        Se invoca el metodo del controlador con los parametros
        resueltos desde la ruta, query string, body JSON o form.
        Si el metodo tiene @JxAsync → se despacha a un hilo separado
          y se devuelve 202 Accepted inmediatamente.
        Si el metodo tiene @JxRetry → la invocacion se reintenta
          en caso de excepcion segun la configuracion.
        |
        v
Etapa 13 — @JxAfterAction + filtros after
        Se ejecutan los metodos anotados con @JxAfterAction.
        Luego se ejecutan los filtros registrados en JxFilters.after().
        |
        v
Etapa 14 — Render + metricas finales
        Negociacion de contenido segun el ActionResult devuelto:
          view(name)     → forward al JSP correspondiente
          json(obj)      → Content-Type: application/json
          text(str)      → Content-Type: text/plain
          redirect(url)  → 302 Location
          raw(bytes, ct) → bytes crudos (ZIP, PDF, imagen, etc.)
        Se registra la latencia y el codigo de respuesta en JxMetrics.
```

---

## 4. Modulo de routing

### 4.1 Convencion sobre configuracion

JxMVC resuelve las rutas por convencion antes de buscar anotaciones. La convencion es:

```
GET /controlador/accion  →  ControladorController.accion()
GET /controlador         →  ControladorController.index()
GET /                    →  Controlador anotado con @JxControllerMain → index()
```

Ejemplo:

```java
@JxControllerMapping("productos")
public class ProductosController extends JxController {

    // GET /productos         (por convencion: accion omitida = index)
    // GET /productos/index   (explicito)
    @JxGetMapping("index")
    public ActionResult index() {
        return view("productos/lista");
    }

    // GET /productos/detalle/42
    @JxGetMapping("detalle/{id}")
    public ActionResult detalle(long id) {
        return view("productos/detalle");
    }
}
```

### 4.2 Plantillas de ruta con variables

Los segmentos entre llaves son variables que se extraen de la URL y se pasan como parametros al metodo:

```java
@JxGetMapping("articulos/{categoria}/{slug}")
public ActionResult articulo(String categoria, String slug) {
    // GET /articulos/tecnologia/mi-primer-post
    // categoria = "tecnologia", slug = "mi-primer-post"
}
```

La anotacion `@JxPathVar` permite nombrar explicitamente la variable cuando el parametro Java tiene un nombre diferente:

```java
@JxGetMapping("usuarios/{user_id}/pedidos/{order_id}")
public ActionResult pedido(
        @JxPathVar("user_id")  long userId,
        @JxPathVar("order_id") long orderId) { ... }
```

### 4.3 Anotaciones de metodo HTTP

```java
@JxGetMapping("ruta")     // GET
@JxPostMapping("ruta")    // POST
@JxPutMapping("ruta")     // PUT
@JxDeleteMapping("ruta")  // DELETE
@JxPatchMapping("ruta")   // PATCH
@JxAnyMapping("ruta")     // cualquier verbo
```

### 4.4 Parametros de la peticion

```java
// Desde query string o formulario
public ActionResult buscar(@JxParam("q") String query,
                           @JxParam("page") int pagina) { ... }

// Desde el cuerpo JSON (deserializado automaticamente)
public ActionResult crear(@JxBody @JxValid ProductoDto dto) { ... }

// Cabecera HTTP
public ActionResult info(@JxRequestHeader("X-Api-Key") String apiKey) { ... }

// Cookie
public ActionResult perfil(@JxCookieValue("session_id") String sessionId) { ... }
```

### 4.5 Algoritmo de resolucion en BaseDispatcher

La clase `BaseDispatcher` escanea todos los controladores al inicio y construye un indice de rutas. Cuando llega una peticion, el algoritmo de resolucion opera en cuatro pasos:

1. Busqueda exacta en el indice.
2. Busqueda con plantillas: itera las rutas con variables y verifica que la cantidad de segmentos coincida.
3. Busqueda por prefijo de controlador: si la ruta tiene al menos dos segmentos (`/ctrl/action`), busca el controlador que coincida con el primer segmento.
4. Ruta raiz: si la ruta es `/`, busca el controlador anotado con `@JxControllerMain`.

Un bug corregido durante el desarrollo fue que el paso 3 podia hacer coincidir una ruta como `/home` con cualquier ruta que empezara por `/home/`, porque no se verificaba si la ruta del indice tenia al menos un segmento adicional. Se corrigio comprobando que la ruta candidata contuviera al menos un `/` despues del primero.

---

## 5. Controladores y vistas

### 5.1 JxController — clase base

Todos los controladores extienden `JxController`, que expone:

```java
// Acceso a la peticion y respuesta
JxRequest  model   // parametros, sesion, atributos de request
JxResponse view    // metodos de respuesta y cabeceras

// Helpers de resultado
ActionResult view(String name)          // forward a WEB-INF/views/{name}.jsp
ActionResult json(Object obj)           // serializa a JSON
ActionResult json(String rawJson)       // JSON ya serializado
ActionResult text(String content)       // texto plano
ActionResult redirect(String url)       // redireccion HTTP 302
ActionResult notFound()                 // 404
ActionResult forbidden()                // 403
ActionResult badRequest(String msg)     // 400

// Acceso a sesion y parametros
String  param(String name)
String  session(String name)
void    setSession(String name, Object value)
void    removeSession(String name)
```

### 5.2 JxRequest

Abstrae `HttpServletRequest` y proporciona metodos tipados:

```java
model.param("nombre")                  // String o null
model.paramInt("cantidad", 1)          // int con fallback
model.paramLong("id", 0L)             // long con fallback
model.paramBool("activo", false)       // boolean con fallback
model.setVar("clave", valor)           // atributo de request para la vista
model.getVar("clave")                  // recuperar atributo
model.file("adjunto")                  // Part de multipart/form-data
```

### 5.3 ActionResult

`ActionResult` es un objeto de resultado que el framework interpreta en la etapa 14. Los tipos posibles son:

| Tipo | Metodo | Comportamiento |
|---|---|---|
| Vista JSP | `view("path/vista")` | Forward a `/WEB-INF/views/path/vista.jsp` |
| JSON | `json(objeto)` | Serializa con JxJson, Content-Type application/json |
| Texto | `text("contenido")` | Content-Type text/plain |
| Redireccion | `redirect("/ruta")` | HTTP 302 con cabecera Location |
| Binario | `view.raw(bytes, ct, filename)` | Content-Disposition attachment |
| Nulo | `return null` | El controlador ya escribio directamente a la respuesta |

### 5.4 Vistas JSP

Las vistas se ubican en `WEB-INF/views/` y se incluyen con la convencion de carpetas:

```
WEB-INF/views/
    shared/
        header.jspf    Cabecera comun (incluida con <%@ include %>)
        footer.jspf    Pie comun
        error.jsp      Vista de error generica
    home/
        index.jsp
        about.jsp
    productos/
        lista.jsp
        detalle.jsp
```

Las vistas acceden al modelo mediante Expression Language:

```jsp
<h1>${producto.nombre}</h1>
<p>${jx_error_message}</p>   <!-- atributo especial de error -->
```

---

## 6. Inyeccion de dependencias

### 6.1 Definir un servicio

```java
@JxService
public class EmailService {

    public void enviar(String destinatario, String asunto, String cuerpo) {
        // implementacion
    }
}
```

`@JxService` marca la clase para que `JxServiceRegistry` la instancie y gestione durante el arranque del framework.

### 6.2 Inyectar en un controlador

```java
@JxControllerMapping("usuarios")
public class UsuarioController extends JxController {

    @JxInject
    private EmailService emailService;

    @JxPostMapping("registro")
    public ActionResult registrar(@JxBody @JxValid UsuarioDto dto) {
        // ... persistir usuario
        emailService.enviar(dto.email, "Bienvenido", "...");
        return redirect("/usuarios/inicio");
    }
}
```

### 6.3 Inyeccion entre servicios

Los servicios pueden inyectar otros servicios con `@JxInject`:

```java
@JxService
public class PedidoService {

    @JxInject
    private EmailService emailService;

    @JxInject
    private InventarioService inventarioService;
}
```

### 6.4 Scope PROTOTYPE

Por defecto los servicios son singletons. Para crear una nueva instancia en cada inyeccion:

```java
@JxService
@JxScope(ScopeType.PROTOTYPE)
public class CarritoService {
    // nueva instancia por cada inyeccion
}
```

### 6.5 Vinculacion de configuracion a POJO

```java
@JxService
@JxConfigProperties(prefix = "mail")
public class MailConfig {
    public String host;
    public int port;
    public String user;
}
```

Con `application.properties`:
```properties
mail.host = smtp.example.com
mail.port = 587
mail.user = no-reply@example.com
```

---

## 7. Base de datos — JxPool y JxDB

### 7.1 JxPool

JxPool es el pool de conexiones propio de JxMVC. No depende de HikariCP, DBCP ni ninguna otra libreria.

Configuracion minima:

```properties
jxmvc.db.url              = jdbc:postgresql://localhost:5432/mi_base
jxmvc.db.user             = postgres
jxmvc.db.pass             = secreto
jxmvc.pool.enabled        = true
jxmvc.pool.size           = 10
jxmvc.pool.timeout        = 5
```

Caracteristicas:
- Tamanio configurable de pool.
- Keepalive automatico para evitar que el servidor cierre conexiones ociosas.
- Timeout de adquisicion configurable.
- Reporte de estado disponible en `/jx/health`.

### 7.2 JxDB — cliente JDBC de alto nivel

`JxDB` es el cliente de acceso a base de datos. Abstrae `PreparedStatement` y mapea resultados a objetos Java sin reflection compleja.

```java
JxDB db = new JxDB();

// Consulta que devuelve multiples filas
List<DBRow> filas = db.query("SELECT * FROM productos WHERE activo = ?", true);
for (DBRow fila : filas) {
    String nombre = fila.str("nombre");
    int stock     = fila.num("stock").intValue();
}

// Consulta que devuelve una fila
DBRow fila = db.queryOne("SELECT * FROM productos WHERE id = ?", 42);

// Insercion / actualizacion / eliminacion
int afectadas = db.execute("UPDATE productos SET stock = ? WHERE id = ?", 0, 42);

// Escalar unico (COUNT, SUM, etc.)
long total = db.scalar("SELECT COUNT(*) FROM pedidos WHERE usuario_id = ?", userId);
```

`DBRow` proporciona metodos tipados: `str(col)`, `num(col)`, `bool(col)`, `date(col)`, `ts(col)`.

---

## 8. JxRepository — CRUD generico

`JxRepository<T, ID>` proporciona operaciones CRUD completas sobre una entidad sin escribir SQL manual para las operaciones basicas.

### 8.1 Definir la entidad

```java
@JxTable("productos")
public class Producto {

    @JxId
    public long id;

    @JxColumn("nombre")
    public String nombre;

    public double precio;
    public boolean activo;
}
```

`@JxTable` indica la tabla. `@JxId` marca la clave primaria. `@JxColumn` permite usar un nombre de columna diferente al del campo Java. Si no hay `@JxColumn`, se usa el nombre del campo directamente.

### 8.2 Definir el repositorio

```java
@JxService
public class ProductoRepository extends JxRepository<Producto, Long> {

    public ProductoRepository() {
        super("productos", Producto.class);
    }

    // Consulta personalizada
    @JxQuery("SELECT * FROM productos WHERE precio < ? AND activo = true")
    public List<Producto> findBaratos(double precioMaximo) {
        return executeQuery(precioMaximo);
    }

    @JxQuery("SELECT COUNT(*) AS _cnt FROM productos WHERE activo = ?")
    public long countActivos(boolean activo) {
        return executeCount(activo);
    }
}
```

### 8.3 Operaciones disponibles en JxRepository

```java
// Busqueda
Optional<T>  findById(ID id)
List<T>      findAll()
List<T>      findAll(int page, int size)   // paginado
long         count()

// Persistencia
T            save(T entity)               // INSERT o UPDATE segun si id es 0
void         saveAll(List<T> entities)    // batch insert
T            update(T entity)
void         deleteById(ID id)            // DELETE o soft-delete si @JxSoftDelete

// Existencia
boolean      existsById(ID id)
```

### 8.4 Borrado logico

```java
@JxTable("usuarios")
@JxSoftDelete(column = "eliminado", deletedValue = "1", activeValue = "0")
public class Usuario {
    @JxId public long id;
    public String email;
    public int eliminado;
}
```

Con `@JxSoftDelete`, `deleteById()` hace `UPDATE ... SET eliminado = 1` en lugar de `DELETE`. Todos los `SELECT` del repositorio incluyen automaticamente `WHERE eliminado = 0`.

### 8.5 Paginacion

```java
JxPageResult<Producto> pagina = repo.findAll(page, size);
pagina.getItems()       // List<Producto> de la pagina actual
pagina.getTotal()       // total de registros
pagina.getTotalPages()  // total de paginas
pagina.getPage()        // pagina actual (1-based)
pagina.getSize()        // tamano de pagina
```

---

## 9. Validacion

`JxValidation` valida automaticamente objetos deserializados con `@JxBody @JxValid`.

### 9.1 Anotaciones de campo

```java
public class ProductoDto {

    @JxRequired
    @JxMinLength(2)
    @JxMaxLength(100)
    public String nombre;

    @JxMin(0)
    @JxMax(999999)
    public double precio;

    @JxPattern("[A-Z]{3}-\\d{6}")
    public String codigo;

    @JxEmail
    public String contacto;
}
```

### 9.2 Comportamiento ante error

Si la validacion falla, el framework responde automaticamente con `400 Bad Request` y un cuerpo JSON que describe el error:

```json
{
  "ok": false,
  "error": "nombre: no puede estar vacio; precio: debe ser >= 0"
}
```

No es necesario escribir codigo de verificacion en el controlador.

---

## 10. Seguridad — Auth y Roles

### 10.1 Implementar JxAuthProvider

```java
@JxService
public class MiAuthProvider implements JxAuthProvider {

    @Override
    public boolean isAuthenticated(HttpServletRequest req) {
        return req.getSession(false) != null
            && req.getSession().getAttribute("userId") != null;
    }

    @Override
    public boolean hasRole(HttpServletRequest req, String role) {
        String userRole = (String) req.getSession().getAttribute("role");
        return role.equalsIgnoreCase(userRole);
    }
}
```

Registrarlo en `JxSecurity` al inicio:

```java
JxSecurity.register(new MiAuthProvider());
```

O simplemente anotarlo con `@JxService` — el framework lo detecta y registra automaticamente si implementa `JxAuthProvider`.

### 10.2 Proteger rutas

```java
// Requiere autenticacion en todo el controlador
@JxAuth
@JxControllerMapping("admin")
public class AdminController extends JxController { ... }

// Requiere rol especifico en un metodo
@JxAuth(roles = {"ADMIN", "SUPERUSER"})
@JxGetMapping("usuarios/eliminar/{id}")
public ActionResult eliminar(long id) { ... }

// Sin autenticacion requerida (overrides el del controlador)
@JxAuth(required = false)
@JxGetMapping("login")
public ActionResult loginForm() { ... }
```

---

## 11. CORS

### 11.1 Politica por defecto

Sin anotacion `@JxCors`, las peticiones cross-origin son bloqueadas con 403. Las peticiones same-origin (sin cabecera `Origin`, o con `Origin` igual al host del servidor) siempre pasan.

### 11.2 Habilitar CORS

```java
// Orígenes especificos en el controlador
@JxCors(origins = {"https://miapp.com", "https://www.miapp.com"})
@JxControllerMapping("api")
public class ApiController extends JxController { ... }

// Wildcard — cualquier origen
@JxCors(origins = {"*"})
@JxGetMapping("datos/publicos")
public ActionResult datosPublicos() { ... }

// Con credenciales (no compatible con wildcard de origen)
@JxCors(
    origins = {"https://miapp.com"},
    methods = {"GET", "POST"},
    allowCredentials = true,
    maxAge = 7200
)
```

### 11.3 Logica interna de BaseCorsResolver

La resolucion de politica sigue este orden:

1. Si no hay cabecera `Origin` → same-origin, permitido.
2. Si el hostname del `Origin` coincide con `req.getServerName()` → same-origin, permitido. Esta comparacion se hace solo por hostname para que funcione correctamente detras de un reverse proxy (Apache, Nginx) donde el esquema interno puede ser `http:8080` pero el origen real es `https:443`.
3. Si hay `@JxCors` en el metodo → se usa esa politica.
4. Si hay `@JxCors` en el controlador → se usa esa politica.
5. Sin anotacion → 403.

La politica se resuelve verificando: origen en la lista de `origins`, verbo en la lista de `methods`, IP del cliente en la lista de `hosts`.

---

## 12. Rate Limiting

```java
// 10 peticiones por minuto por IP en este metodo
@JxRateLimit(requests = 10, window = 60)
@JxPostMapping("login")
public ActionResult login(@JxBody LoginDto dto) { ... }

// 1000 peticiones por hora en todo el controlador
@JxRateLimit(requests = 1000, window = 3600)
@JxControllerMapping("api/v1")
public class ApiV1Controller extends JxController { ... }
```

La implementacion usa una ventana deslizante almacenada en un `ConcurrentHashMap`. La clave es la combinacion de IP del cliente y ruta. Cuando se supera el limite, el framework responde con `429 Too Many Requests` y la cabecera `Retry-After` con los segundos restantes hasta que se libere la ventana.

---

## 13. Filtros

Los filtros son codigo que se ejecuta antes y/o despues de la accion del controlador.

### 13.1 Implementar un filtro

```java
@JxService
public class LogFilter implements JxFilter {

    @Override
    public void before(JxFilterContext ctx) throws Exception {
        System.out.println("Antes: " + ctx.getRequest().getRequestURI());
    }

    @Override
    public void after(JxFilterContext ctx) throws Exception {
        System.out.println("Despues: " + ctx.getResponse().getStatus());
    }
}
```

### 13.2 Registro programatico

```java
JxFilters.before(new AuthFilter());
JxFilters.after(new AuditFilter());
```

### 13.3 Anotacion por controlador o metodo

```java
@JxFilter(LogFilter.class)
@JxControllerMapping("sensible")
public class SensibleController extends JxController { ... }
```

### 13.4 Interrumpir el pipeline desde un filtro

```java
@Override
public void before(JxFilterContext ctx) throws Exception {
    if (!validarToken(ctx)) {
        ctx.stop();  // Las etapas posteriores no se ejecutan
        ctx.getResponse().sendError(401, "Token invalido");
    }
}
```

---

## 14. Cache

### 14.1 Marcar un metodo como cacheable

```java
@JxService
public class ProductoService {

    @JxCacheable(key = "productos:todos", ttl = 300)
    public List<Producto> findAll() {
        return repo.findAll();  // solo se ejecuta si no hay cache
    }

    @JxCacheEvict(key = "productos:todos")
    public void guardar(Producto p) {
        repo.save(p);  // invalida el cache al guardar
    }
}
```

### 14.2 Backend de cache personalizado

La implementacion predeterminada es en memoria (ConcurrentHashMap con TTL). Para usar Redis u otro backend:

```java
@JxService
@JxCacheManager
public class RedisCacheBackend implements JxCacheBackend {

    @Override
    public void put(String key, Object value, long ttlSeconds) { ... }

    @Override
    public Optional<Object> fetch(String key) { ... }

    @Override
    public void evict(String key) { ... }

    @Override
    public void clear() { ... }

    @Override
    public boolean has(String key) { ... }
}
```

Al detectar una clase `@JxCacheManager`, `JxCache` usa ese backend en lugar del predeterminado.

---

## 15. Scheduler

```java
@JxService
public class TareasProgramadas {

    // Se ejecuta cada 60 segundos
    @JxScheduled(fixedRate = 60_000)
    public void limpiarSesionesExpiradas() {
        // ...
    }

    // Expresion cron: todos los dias a las 2:00 AM
    @JxScheduled(cron = "0 0 2 * * *")
    public void reporteDiario() {
        // ...
    }
}
```

`JxScheduler` arranca en un thread dedicado cuando se inicializa el framework. Soporta `fixedRate` (intervalo fijo en milisegundos) y expresiones cron de 6 campos.

---

## 16. Eventos

El bus de eventos permite comunicacion desacoplada entre servicios.

```java
// Publicar
@JxService
public class PedidoService {

    public void confirmar(long pedidoId) {
        // ...
        JxEventBus.publish(new PedidoConfirmadoEvent(pedidoId));
    }
}

// Suscribirse
@JxService
public class NotificacionService {

    @JxEventListener
    public void onPedidoConfirmado(PedidoConfirmadoEvent evento) {
        emailService.enviar("...", "Pedido confirmado", "...");
    }
}
```

`JxEventBus.publish()` ejecuta los listeners de forma sincrona en el mismo thread. Para ejecucion asincrona, el metodo listener puede anotarse con `@JxAsync`.

---

## 17. Transacciones

```java
@JxService
public class TransferenciaService {

    @JxInject
    private JxDB db;

    @JxTransactional
    public void transferir(long origen, long destino, double monto) {
        db.execute("UPDATE cuentas SET saldo = saldo - ? WHERE id = ?", monto, origen);
        db.execute("UPDATE cuentas SET saldo = saldo + ? WHERE id = ?", monto, destino);
        // Si cualquier linea lanza excepcion → rollback automatico
    }
}
```

`@JxTransactional` abre una transaccion al inicio del metodo y hace commit si termina normalmente, o rollback si se lanza cualquier excepcion no controlada.

---

## 18. Async y Retry

### 18.1 @JxAsync

```java
@JxAsync
@JxPostMapping("exportar")
public ActionResult exportar(@JxBody ExportRequest req) {
    // Se ejecuta en un hilo separado del pool de JxScheduler
    // La respuesta HTTP 202 se devuelve inmediatamente al cliente
    generarExcel(req);
    return null;
}
```

### 18.2 @JxRetry

```java
@JxRetry(attempts = 3, backoff = 1000, on = {IOException.class})
@JxPostMapping("enviar-webhook")
public ActionResult enviarWebhook(@JxBody WebhookPayload payload) {
    // Si falla con IOException, reintenta hasta 3 veces con 1 segundo entre intentos
    httpClient.post(webhookUrl, payload);
    return json("{\"ok\":true}");
}
```

---

## 19. Multi-datasource

```java
// Registrar datasources adicionales al inicio
JxDataSourceRegistry.register("reportes",
    "jdbc:postgresql://reporting-server:5432/reportes",
    "user", "pass");

// Usarlo en un servicio
@JxService
@JxDS("reportes")
public class ReporteService extends JxRepository<Reporte, Long> {
    public ReporteService() { super("reportes_mensuales", Reporte.class); }
}
```

---

## 20. JSON propio

`JxJson` serializa y deserializa JSON sin Jackson, Gson ni ninguna otra libreria.

```java
// Serializar objeto a String JSON
String json = JxJson.toJson(miObjeto);

// Deserializar JSON a clase
ProductoDto dto = JxJson.fromJson(jsonString, ProductoDto.class);

// Deserializar a lista
List<ProductoDto> lista = JxJson.fromJsonList(jsonString, ProductoDto.class);
```

Soporta: primitivos, String, List, Map, objetos anidados, null, arrays, LocalDate, LocalDateTime.

---

## 21. Metricas

`JxMetrics` registra automaticamente, por cada ruta:

- Total de peticiones atendidas.
- Total de errores (respuestas 4xx y 5xx).
- Latencia media en milisegundos.
- Latencia maxima.
- Ultima peticion (timestamp).

Accesibles en `/jx/metrics` como JSON:

```json
{
  "routes": {
    "GET /home/index": {
      "requests": 1423,
      "errors": 2,
      "avgLatencyMs": 4.7,
      "maxLatencyMs": 38,
      "lastRequest": "2026-05-12T09:20:01"
    }
  }
}
```

---

## 22. OpenAPI

`JxOpenApi` genera automaticamente una especificacion OpenAPI 3.0 en formato JSON a partir de las anotaciones de los controladores. No requiere ninguna configuracion adicional.

Accesible en `/jx/openapi`.

Incluye:
- Lista de paths y operaciones.
- Parametros de ruta, query y cuerpo.
- Codigos de respuesta inferidos de las anotaciones.
- Informacion del servidor y version del framework.

---

## 23. Endpoints internos del sistema

Todos los endpoints bajo `/jx/` son internos del framework. No pasan por el pipeline de controladores de usuario. Se procesan en la etapa 01.

| Endpoint | Metodo | Descripcion |
|---|---|---|
| `/jx/health` | GET | Estado del pool, uptime, threads activos, scheduler |
| `/jx/info` | GET | Version del framework, perfil activo, version Java, servidor |
| `/jx/metrics` | GET | Metricas por ruta: total, errores, latencia media y maxima |
| `/jx/openapi` | GET | Especificacion OpenAPI 3.0 generada de las anotaciones |

Respuesta de `/jx/health` cuando el pool esta habilitado:

```json
{
  "status": "UP",
  "profile": "prod",
  "pool": {
    "enabled": true,
    "active": 2,
    "idle": 8,
    "total": 10
  },
  "scheduler": { "running": true },
  "async": { "active": true }
}
```

---

## 24. Catalogo completo de anotaciones

JxMVC define 44 anotaciones agrupadas por funcion.

### Enrutamiento de controladores

| Anotacion | Nivel | Descripcion |
|---|---|---|
| `@JxControllerMapping("ruta")` | Clase | Prefijo base de todas las rutas del controlador |
| `@JxControllerMain` | Clase | Controlador que responde a la raiz `/` |
| `@JxRestController` | Clase | Controlador REST — respuestas siempre en JSON |
| `@JxGetMapping("ruta")` | Metodo | Manejador de GET |
| `@JxPostMapping("ruta")` | Metodo | Manejador de POST |
| `@JxPutMapping("ruta")` | Metodo | Manejador de PUT |
| `@JxDeleteMapping("ruta")` | Metodo | Manejador de DELETE |
| `@JxPatchMapping("ruta")` | Metodo | Manejador de PATCH |
| `@JxAnyMapping("ruta")` | Metodo | Manejador de cualquier verbo |

### Parametros de metodo

| Anotacion | Nivel | Descripcion |
|---|---|---|
| `@JxPathVar("nombre")` | Parametro | Variable de plantilla de ruta `{nombre}` |
| `@JxParam("nombre")` | Parametro | Parametro de query string o formulario |
| `@JxBody` | Parametro | Cuerpo de la peticion deserializado desde JSON |
| `@JxValid` | Parametro | Activa validacion automatica en el parametro |
| `@JxRequestHeader("nombre")` | Parametro | Cabecera HTTP |
| `@JxCookieValue("nombre")` | Parametro | Valor de cookie |

### Seguridad

| Anotacion | Nivel | Descripcion |
|---|---|---|
| `@JxAuth(roles={})` | Clase / Metodo | Requiere autenticacion y/o roles |
| `@JxCors(origins={})` | Clase / Metodo | Politica CORS |
| `@JxRateLimit(requests, window)` | Clase / Metodo | Limite de tasa por IP |

### Interceptores y modelo

| Anotacion | Nivel | Descripcion |
|---|---|---|
| `@JxBeforeAction` | Metodo | Se ejecuta antes de la accion en el mismo controlador |
| `@JxAfterAction` | Metodo | Se ejecuta despues de la accion |
| `@JxModelAttr("nombre")` | Metodo | Agrega retorno del metodo al modelo de la vista |
| `@JxFilter(clase)` | Clase / Metodo | Aplica un filtro al controlador o accion |

### Inyeccion de dependencias

| Anotacion | Nivel | Descripcion |
|---|---|---|
| `@JxService` | Clase | Registra la clase como servicio singleton |
| `@JxInject` | Campo | Inyecta un servicio registrado |
| `@JxScope(tipo)` | Clase | `SINGLETON` (por defecto) o `PROTOTYPE` |
| `@JxValue("prop.key")` | Campo | Inyecta un valor de application.properties |
| `@JxConfigProperties(prefix)` | Clase | Vincula un prefijo de properties a campos del POJO |
| `@JxDS("nombre")` | Clase / Campo | Indica el datasource a usar |

### Base de datos

| Anotacion | Nivel | Descripcion |
|---|---|---|
| `@JxTable("tabla")` | Clase | Nombre de la tabla SQL |
| `@JxId` | Campo | Clave primaria |
| `@JxColumn("col")` | Campo | Nombre de columna diferente al campo Java |
| `@JxQuery("SQL")` | Metodo | SQL personalizado en repositorio |
| `@JxSoftDelete(column)` | Clase | Borrado logico en lugar de DELETE |

### Ejecucion

| Anotacion | Nivel | Descripcion |
|---|---|---|
| `@JxAsync` | Metodo | Ejecucion en thread separado, responde 202 inmediatamente |
| `@JxRetry(attempts, backoff)` | Metodo | Reintento automatico en caso de excepcion |
| `@JxTransactional` | Metodo | Transaccion JDBC con commit/rollback automatico |
| `@JxScheduled(fixedRate / cron)` | Metodo | Tarea programada |
| `@JxEventListener` | Metodo | Listener del bus de eventos |

### Cache

| Anotacion | Nivel | Descripcion |
|---|---|---|
| `@JxCacheable(key, ttl)` | Metodo | Cachea el resultado del metodo |
| `@JxCacheEvict(key)` | Metodo | Invalida una entrada del cache al ejecutar |
| `@JxCacheManager` | Clase | Marca el servicio como backend de cache personalizado |

### Respuesta y excepciones

| Anotacion | Nivel | Descripcion |
|---|---|---|
| `@JxResponseStatus(code)` | Metodo | Codigo HTTP de la respuesta |
| `@JxExceptionHandler(tipo)` | Metodo | Manejador de una excepcion especifica |
| `@JxControllerAdvice` | Clase | Clase con manejadores globales de excepcion |
| `@JxProfile("nombre")` | Clase / Metodo | Activa el controlador/metodo solo en el perfil indicado |

---

## 25. Catalogo de clases del core

Las 44 clases que componen el JAR del framework:

| Clase | Rol |
|---|---|
| `MainLxServlet` | Servlet principal, punto de entrada, orquesta el pipeline |
| `BaseDispatcher` | Resolucion de rutas y construccion del plan de despacho |
| `BaseDispatchPlan` | Record inmutable que contiene el controlador, metodo y variables resueltas |
| `BaseCorsResolver` | Evaluacion y aplicacion de la politica CORS |
| `BaseSanitizer` | Limpieza de entradas (HTML, SQL) |
| `BaseDbResolver` | Deteccion automatica del driver JDBC segun la URL |
| `JxMapping` | Definicion de todas las anotaciones del framework |
| `JxController` | Clase base de todos los controladores de usuario |
| `JxRequest` | Abstraccion de HttpServletRequest con metodos tipados |
| `JxResponse` | Abstraccion de HttpServletResponse con metodos de alto nivel |
| `JxHttp` | Constantes de codigos y tipos de contenido HTTP |
| `ActionResult` | Resultado de una accion del controlador |
| `JxPageResult` | Resultado paginado con metadatos de paginacion |
| `JxDB` | Cliente JDBC de alto nivel con mapeo a DBRow |
| `DBRow` | Fila de resultado tipada |
| `DBRowSet` | Coleccion de DBRow con iteracion tipada |
| `JxPool` | Pool de conexiones JDBC propio |
| `JxDataSourceRegistry` | Registro de datasources adicionales para multi-DB |
| `JxRepository` | CRUD generico por reflection con soporte paginacion y soft delete |
| `JxValidation` | Motor de validacion de campos con anotaciones |
| `JxJson` | Serializador/deserializador JSON sin dependencias |
| `JxLogger` | Logger interno con formato estructurado |
| `JxMetrics` | Almacenamiento y consulta de metricas por ruta |
| `JxOpenApi` | Generador de especificacion OpenAPI 3.0 |
| `JxSecurity` | Registro del JxAuthProvider activo |
| `JxAuthProvider` | Interfaz de autenticacion personalizable por el usuario |
| `JxFilter` | Interfaz de filtros before/after |
| `JxFilterContext` | Contexto de ejecucion pasado a los filtros |
| `JxFilters` | Registro programatico de filtros globales |
| `JxRateLimiter` | Ventana deslizante de rate limiting por IP+ruta |
| `JxCache` | Fachada de la capa de cache |
| `JxCacheBackend` | Interfaz del backend de cache (in-memory o externo) |
| `JxScheduler` | Planificador de tareas con fixedRate y cron |
| `JxEventBus` | Bus de eventos publish/subscribe desacoplado |
| `JxTransaction` | Gestor de transacciones JDBC |
| `JxServiceRegistry` | Contenedor de DI: descubrimiento, instanciacion e inyeccion |
| `JxAdviceRegistry` | Registro y despacho de manejadores globales de excepcion |
| `JxEnvironment` | Acceso a propiedades, variables de entorno y perfiles |
| `JxConfigBinder` | Vinculacion de prefijos de properties a POJOs |
| `JxProfile` | Evaluacion del perfil activo en tiempo de ejecucion |
| `JxException` | Excepcion HTTP con codigo de estado y mensaje |
| `JxTest` | Utilidades para pruebas de controladores en modo embebido |
| `JxTagFor` | Tag JSP personalizado para bucles |
| `JxTagIf` | Tag JSP personalizado para condicionales |

---

## 26. Configuracion — application.properties

Ubicacion: `src/main/resources/application.properties`

```properties
# ── Framework core ───────────────────────────────────────────────────────
jxmvc.controllers.package = com.ejemplo.miapp.controllers
jxmvc.services.package    = com.ejemplo.miapp.services
jxmvc.profile             = dev          # dev | prod | test | custom
jxmvc.log.level           = INFO         # TRACE | DEBUG | INFO | WARN | ERROR

# ── Pool de conexiones ───────────────────────────────────────────────────
jxmvc.db.driver           = org.postgresql.Driver    # opcional: autodeteccion por URL
jxmvc.db.url              = jdbc:postgresql://localhost:5432/miapp
jxmvc.db.user             = postgres
jxmvc.db.pass             = secreto
jxmvc.pool.enabled        = true
jxmvc.pool.size           = 10           # conexiones en el pool
jxmvc.pool.timeout        = 5            # segundos para obtener conexion

# ── Ejecucion asincrona ──────────────────────────────────────────────────
jxmvc.async.threads       = 8            # tamanio del thread pool para @JxAsync

# ── Cabeceras de seguridad ───────────────────────────────────────────────
jxmvc.security.frame-options = SAMEORIGIN   # DENY | SAMEORIGIN | ALLOW-FROM url
jxmvc.security.hsts          = false        # true activa Strict-Transport-Security
jxmvc.security.hsts.maxage   = 31536000     # segundos (1 anio)

# ── Datasource secundario (multi-DB) ────────────────────────────────────
jxmvc.ds.reportes.url  = jdbc:postgresql://reporting:5432/reportes
jxmvc.ds.reportes.user = reporte_user
jxmvc.ds.reportes.pass = secreto2

# ── Propiedades de aplicacion (accesibles con @JxValue) ─────────────────
mail.host   = smtp.gmail.com
mail.port   = 587
app.version = 1.0.0
```

---

## 27. Testing con JxTest

`JxTest` permite probar controladores sin levantar un servidor real.

```java
public class ProductoControllerTest {

    @Test
    public void testListar() throws Exception {
        JxTest test = new JxTest(ProductoController.class);
        JxTest.Response resp = test.get("/productos/index");

        assertEquals(200, resp.getStatus());
        assertNotNull(resp.getModel().get("items"));
    }

    @Test
    public void testCrear() throws Exception {
        JxTest test = new JxTest(ProductoController.class);
        JxTest.Response resp = test.post("/productos/save",
            Map.of("nombre", "Laptop", "precio", "999.99"));

        assertEquals(302, resp.getStatus());
    }
}
```

`JxTest` crea instancias reales del controlador con DI inyectada, usando mocks de `HttpServletRequest` y `HttpServletResponse`. No requiere Mockito ni ninguna libreria de mocking.

---

## 28. Sitio demo — JxMVC2x

El proyecto `JxMVC2x` es el sitio web de presentacion del framework. Fue construido enteramente con JxMVC y sirve como demostracion de las capacidades.

### Estructura del sitio

```
Controladores:
  HomeController      /                      index, about, docs, downloads, errors, error403
  GeneratorController /generate/download     POST — genera proyecto ZIP personalizado
  DemoController      /demo/*                demostraciones de funcionalidades

Vistas:
  home/index.jsp      Pagina principal con metricas, features y endpoints
  home/about.jsp      Informacion del framework, pipeline y comparativa
  home/docs.jsp       Documentacion completa con 10 secciones y syntax highlighting
  home/downloads.jsp  Generador de proyecto starter con formulario
  home/errors.jsp     Demostracion de paginas de error
  shared/header.jspf  Cabecera global con nav, i18n, tema y bottom bar movil
  shared/footer.jspf  Pie de pagina con links a endpoints internos
  shared/error.jsp    Pagina de error generico (independiente, sin CDN)
```

### Sistema i18n

Implementado en JavaScript puro sin librerias. Cinco idiomas: espanol, ingles, portugues, ruso, frances.

Mecanismo:
1. Diccionario `JX_T` con todas las traducciones embebido en `header.jspf`.
2. Los elementos traducibles tienen el atributo `data-i18n="clave"`.
3. `jxApply(lang)` itera todos los elementos con ese atributo y reemplaza su `textContent`.
4. Dos pasadas: una inmediata (para el nav que ya esta en el DOM) y otra en `DOMContentLoaded` (para el contenido del body).
5. El idioma se persiste en `localStorage`. Al cargar, se detecta el idioma del navegador como fallback.

### Generador de proyectos

`GeneratorController.download()` recibe `groupId`, `artifactId`, `appName` y `db` via POST, construye en memoria un ZIP con la estructura completa de un proyecto JxMVC listo para usar, y lo devuelve como descarga binaria.

El ZIP generado incluye: `pom.xml`, `web.xml`, `application.properties`, layout JSP completo, controlador base, controlador de ejemplo, entidad y repositorio (si se eligio una base de datos), y el README tecnico completo.

### Despliegue en produccion

El sitio esta desplegado en `https://jxmvc.andre.net.pe` bajo:

- Servidor: Debian 12, VPS 4GB RAM (Elastika.pe)
- Tomcat 11 en puerto 8080 (interno)
- Apache 2 como reverse proxy con SSL (Let's Encrypt)
- Auto-deploy: el script `war-autodeploy.sh` detecta nuevos `.war` en `/var/tomcat11/webapps/`, crea el VirtualHost Apache y solicita certificado automaticamente

La configuracion del VirtualHost HTTPS usa `ProxyPassMatch` para absorber el prefijo de contexto `/jxmvc/` que genera el JSP, de modo que `GET /jxmvc/home/about` se mapea correctamente a `http://127.0.0.1:8080/jxmvc/home/about`.

---

## 29. Comparativa de rendimiento

### 29.1 Tamano del artefacto

| Framework | JAR / WAR minimo | Runtime deps |
|---|---|---|
| JxMVC 3.0 | ~1.5 MB WAR (incluye framework) | 0 |
| Spring Boot 3 | ~18 MB JAR embebido | ~150+ |
| Quarkus 3 (JVM) | ~12 MB | ~80+ |
| Micronaut 4 | ~14 MB | ~60+ |
| Jakarta EE puro | ~50 KB WAR (sin framework) | 0 |

El JAR del framework solo (sin la aplicacion) pesa ~177 KB. El WAR del sitio demo completo pesa 1.5 MB porque incluye el framework embebido y las dependencias de compilacion.

### 29.2 Tiempo de arranque

| Framework | Arranque tipico | Arranque en contenedor Docker |
|---|---|---|
| JxMVC 3.0 | < 500 ms | < 800 ms |
| Spring Boot 3 | 3 – 8 s | 5 – 12 s |
| Quarkus 3 (JVM) | 1 – 2 s | 1.5 – 3 s |
| Quarkus 3 (Native) | < 100 ms | < 200 ms |
| Micronaut 4 | 1 – 3 s | 2 – 4 s |
| Jakarta EE puro | < 300 ms | < 500 ms |

El arranque de JxMVC incluye: escaneo de controladores por reflection, instanciacion de servicios, inicializacion del pool, arranque del scheduler y compilacion de la primera JSP (que ocurre en la primera peticion, no en el arranque).

### 29.3 Uso de memoria en estado idle

| Framework | Heap tipico en reposo |
|---|---|
| JxMVC 3.0 | 40 – 80 MB |
| Spring Boot 3 | 200 – 400 MB |
| Quarkus 3 (JVM) | 120 – 200 MB |
| Micronaut 4 | 100 – 180 MB |
| Jakarta EE puro (Tomcat) | 30 – 60 MB |

La medicion de JxMVC se realizo en el servidor de produccion (Tomcat 11, -Xms256M -Xmx768M, sin carga) con la aplicacion demo desplegada.

### 29.4 Caracteristicas por framework

| Aspecto | JxMVC 3.0 | Spring Boot 3 | Quarkus 3 | Micronaut 4 | Jakarta EE |
|---|---|---|---|---|---|
| Routing | Convencion + anotaciones | Anotaciones | Anotaciones | Anotaciones | Manual / JAX-RS |
| DI | Propio | Spring DI | CDI / ArC | Propio | CDI |
| Validacion | Propia | Bean Validation | Bean Validation | Bean Validation | Bean Validation |
| JSON | Propio | Jackson | Jackson / Jsonb | Jackson / Serde | Manual |
| ORM / DB | JxDB + JxRepository | Spring Data / JPA | Panache / JPA | JDBC / JPA | JPA / JDBC |
| Pool de conexiones | Propio | HikariCP | Agroal | HikariCP | Ninguno |
| Metricas | Built-in `/jx/metrics` | Micrometer | Micrometer | Micrometer | Manual |
| OpenAPI | Built-in `/jx/openapi` | SpringDoc | SmallRye OpenAPI | Swagger | Manual |
| Scheduler | Built-in `@JxScheduled` | `@Scheduled` | Quartz / `@Scheduled` | `@Scheduled` | EJB Timer / Manual |
| Eventos | Built-in JxEventBus | ApplicationEvent | CDI Events | ApplicationEvent | CDI Events |
| Cache | Built-in + backend | Spring Cache | Cache API | Cache | JCache |
| Compilacion AOT | No | Si (GraalVM) | Si (Native) | Si (Native) | No |
| Curva de aprendizaje | Baja | Alta | Media | Media | Alta |

### 29.5 Throughput (estimado para aplicacion CRUD simple)

Las siguientes cifras son estimaciones basadas en benchmarks publicados para cada framework con una aplicacion REST simple (una ruta que lee un registro de la base de datos y devuelve JSON), usando JMH o wrk con 100 usuarios concurrentes:

| Framework | Req/s aproximado | Latencia media (ms) |
|---|---|---|
| JxMVC 3.0 (Tomcat 11) | 8.000 – 15.000 | 5 – 15 |
| Spring Boot 3 (Tomcat) | 10.000 – 20.000 | 4 – 12 |
| Quarkus 3 (JVM) | 15.000 – 25.000 | 3 – 8 |
| Quarkus 3 (Native) | 25.000 – 50.000 | 1 – 4 |
| Micronaut 4 | 12.000 – 22.000 | 3 – 10 |

JxMVC no ha sido optimizado para throughput maximo. El overhead del pipeline de 14 etapas y el uso de reflection para la resolucion de parametros introducen latencia adicional comparado con frameworks compilados en nativo. Para aplicaciones de contenido web (sitios, APIs internas, sistemas de gestion), el rendimiento es mas que suficiente.

### 29.6 Donde JxMVC es competitivo

JxMVC es la opcion apropiada cuando:
- El entorno ya tiene Tomcat instalado y el equipo lo conoce.
- Se requiere un artefacto deploayable de tamano minimo.
- El arranque rapido es critico (CD/CI con reinicio frecuente, serverless warm, entornos de testing).
- Se quiere eliminar el overhead de classpath scanning de Spring y su meta-framework de auto-configuracion.
- El proyecto no necesita compilacion AOT ni imagenes nativas.
- Se prefiere entender cada linea del framework antes de usarlo.

---

## 30. Bugs encontrados y corregidos

Durante el desarrollo y las pruebas del framework se identificaron y corrigieron los siguientes defectos criticos.

### Bug 1 — @WebServlet interceptaba los forwards JSP

**Sintoma:** Las rutas que devolvian texto plano (`text()`) funcionaban correctamente. Las rutas que devolvian vistas JSP (`view()`) fallaban con 404 o producian una respuesta vacia.

**Causa:** El servlet estaba registrado con `@WebServlet(urlPatterns = {"/*"}, loadOnStartup = 1)`. El patron `/*` intercepta absolutamente todas las peticiones, incluyendo los forwards internos que Tomcat hace cuando el framework llama a `RequestDispatcher.forward()` para renderizar el JSP. El forward terminaba siendo procesado de nuevo por el mismo servlet, que no encontraba ninguna ruta que correspondiera a la ruta interna de la vista, produciendo un bucle o un 404.

**Correccion:** Se elimino la anotacion `@WebServlet` por completo. El servlet se registra en `web.xml` con `<url-pattern>/</url-pattern>` (sin el asterisco), que solo intercepta peticiones reales del cliente, no los forwards internos del contenedor.

### Bug 2 — Prefijo de ruta: /home matcheaba /home/cualquier-cosa

**Sintoma:** Solicitar `/home/paginaquenoexiste` devolveria 200 en lugar de 404, cargando la accion `index()` del controlador `HomeController`.

**Causa:** El algoritmo de busqueda por prefijo en `BaseDispatcher` usaba la ruta base del controlador (`/home`) como prefijo para verificar si cualquier ruta que empezara por `/home/` podia pertenecer a ese controlador. Una ruta inexistente como `/home/nope` pasaba el test de prefijo y era despachada al metodo `index()` por convencion.

**Correccion:** En el paso de busqueda por prefijo, se agrego una verificacion: la ruta candidata del indice debe contener al menos un `/` despues del primero (es decir, tener la forma `/ctrl/action`). Las rutas de un solo segmento como `/home` se saltan en ese paso.

```java
// Antes del fix, /home era un candidato valido para /home/nope
// Despues del fix, solo /home/algo se considera candidato para /home/algo
if (rp.indexOf('/', 1) < 0) continue;
```

### Bug 3 — Codigo HTTP se reseteaba a 200 despues del forward a error.jsp

**Sintoma:** Las respuestas de error (404, 403, 500) llegaban al navegador con codigo HTTP 200, aunque el contenido de la pagina mostraba el error correctamente.

**Causa:** `response.setStatus(code)` establecia el codigo antes de `RequestDispatcher.forward()`. Tomcat restablece el codigo de estado a 200 cuando el forward completa exitosamente. El codigo original quedaba descartado.

**Correccion:** En `error.jsp` se agrego un scriptlet JSP al principio del archivo que reaplica el codigo de estado:

```jsp
<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
%><% Object code = request.getAttribute("jx_error_code");
    if (code instanceof Integer) response.setStatus((Integer) code); %>
```

### Bug 4 — Bucle infinito en sendError al fallar el forward

**Sintoma:** Un error 404 en la vista de error misma (error.jsp no encontrado o con error de compilacion) causaba un bucle infinito de peticiones hasta que el hilo se bloqueaba o la JVM lanzaba StackOverflowError.

**Causa:** `sendError()` intentaba hacer forward a `error.jsp`. Si ese forward fallaba (porque error.jsp tenia un error), lanzaba una excepcion que era capturada por el manejador de excepciones del servlet, que llamaba de nuevo a `sendError()`, que intentaba hacer forward a `error.jsp`, y asi indefinidamente.

**Correccion:** Se agrego un atributo de request como guard (`jx_in_error`). Si `sendError()` detecta que ese atributo ya esta en `true`, escribe directamente un texto plano en la respuesta sin intentar hacer forward:

```java
if (Boolean.TRUE.equals(req.getAttribute("jx_in_error"))) {
    resp.setStatus(code);
    write(resp, "text/plain", code + " " + message);
    return;
}
req.setAttribute("jx_in_error", Boolean.TRUE);
// ... forward a error.jsp
```

### Bug 5 — CORS bloqueaba formularios same-origin en produccion

**Sintoma:** El formulario de descarga del generador de proyectos devolvia 403 en produccion. Localmente funcionaba sin problemas.

**Causa raiz:** Los navegadores modernos envian la cabecera `Origin` en los POST de formularios, incluso cuando la peticion es same-origin. Localmente, `BaseCorsResolver.isSameHost()` calculaba `http://localhost:8080` y comparaba con `Origin: http://localhost:8080` — coincidian. En produccion, el request llega a Tomcat via Apache (http:8080 internamente), pero el `Origin` del navegador es `https://jxmvc.andre.net.pe`. La comparacion fallaba porque el esquema y el puerto eran diferentes.

**Correccion:** `isSameHost()` fue reescrito para comparar solo el hostname, ignorando esquema y puerto:

```java
String originHost = origin
    .replaceFirst("^https?://", "")
    .replaceFirst(":\\d+$", "")
    .trim();
String serverName = safe(req.getServerName()); // Host header preservado por ProxyPreserveHost
return !originHost.isEmpty() && originHost.equalsIgnoreCase(serverName);
```

Apache se configura con `ProxyPreserveHost On` para que `req.getServerName()` devuelva el hostname publico y no `127.0.0.1`.

### Bug 6 — Double context path en produccion detras de proxy

**Sintoma:** Al navegar al sitio en produccion, la pagina de inicio cargaba pero todos los enlaces devolvian 404. La URL en el navegador mostraba `https://jxmvc.andre.net.pe/jxmvc/home/about`.

**Causa:** Apache estaba configurado con `ProxyPass / http://127.0.0.1:8080/jxmvc/`. Esto significa que `GET /` se mapeaba a Tomcat `/jxmvc/`. El JSP generaba links con `${pageContext.request.contextPath}` que valia `/jxmvc`. El browser construia la URL `https://jxmvc.andre.net.pe/jxmvc/home/about`. Apache la mapeaba a `http://127.0.0.1:8080/jxmvc/jxmvc/home/about`, que no existe en Tomcat.

**Correccion:** El VirtualHost HTTPS de Apache se cambio a usar `ProxyPassMatch` con una expresion regular que absorbe el prefijo `/jxmvc/` cuando el cliente lo incluye en la URL:

```apache
ProxyPassMatch ^/(jxmvc/)?(.*) http://127.0.0.1:8080/jxmvc/$2
ProxyPassReverse / http://127.0.0.1:8080/jxmvc/
```

Con esta configuracion, `GET /jxmvc/home/about` y `GET /home/about` ambos mapean correctamente a `http://127.0.0.1:8080/jxmvc/home/about`.

### Bug 7 — ProjectZipBuilder lanzaba UnknownFormatConversionException

**Sintoma:** El endpoint `POST /generate/download` devolveria 500 en ciertos casos.

**Causa:** El metodo `headerJspf()` de `ProjectZipBuilder` construia el contenido JSP usando `String.formatted()`. El template contenia `<%@page ...%>`. Java's `Formatter` interpretaba `%@` como un especificador de formato invalido (conversion `@` no existe) y lanzaba `UnknownFormatConversionException`.

**Correccion:** Se escaparon los `%` de las directivas JSP en el template:

```java
// Antes:  <%@page pageEncoding="UTF-8"%>
// Despues: <%%@page pageEncoding="UTF-8"%%>
// Resultado al formatear: <%@page pageEncoding="UTF-8"%>
```

---

## 31. Despliegue en produccion

### 31.1 Requisitos del servidor

- Java 17 o superior
- Apache Tomcat 10.1+ (Jakarta EE 11, Servlet 6.0)
- Maven 3.8+ para compilar
- Apache 2 como reverse proxy (recomendado para HTTPS y subdominios)

### 31.2 Compilar y empaquetar

```bash
# Una sola vez: instalar el JAR del framework en el repositorio local Maven
cd JxMVC.Core && mvn install

# Empaquetar la aplicacion
cd MiApp && mvn package
# Genera: target/MiApp-1.0.war
```

### 31.3 Desplegar en Tomcat

```bash
# Copiar el WAR (el nombre define el context path)
cp target/MiApp-1.0.war /var/tomcat11/webapps/miapp.war

# Tomcat lo despliega automaticamente sin reinicio
# El contexto queda disponible en http://servidor:8080/miapp/
```

### 31.4 Configuracion de Apache como reverse proxy

```apache
<VirtualHost *:443>
    ServerName miapp.dominio.com
    ProxyPreserveHost On
    RequestHeader set X-Forwarded-Proto "https"

    # Absorbe el context path en los links generados por JSP
    ProxyPassMatch ^/(miapp/)?(.*) http://127.0.0.1:8080/miapp/$2
    ProxyPassReverse / http://127.0.0.1:8080/miapp/

    SSLCertificateFile    /etc/letsencrypt/live/miapp.dominio.com/fullchain.pem
    SSLCertificateKeyFile /etc/letsencrypt/live/miapp.dominio.com/privkey.pem
    Include /etc/letsencrypt/options-ssl-apache.conf
</VirtualHost>
```

### 31.5 Despliegue con Docker

```dockerfile
FROM tomcat:10.1-jdk17
COPY target/MiApp-1.0.war /usr/local/tomcat/webapps/ROOT.war
ENV CATALINA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8080
CMD ["catalina.sh", "run"]
```

```bash
docker build -t miapp .
docker run -p 8080:8080 miapp
```

Con context path en ROOT (`ROOT.war`), los links JSP generan URLs sin prefijo y no se necesita el `ProxyPassMatch`. La aplicacion responde directamente en `/`.

---

---

## 32. Novedades v3.0.0

**Fecha:** 2026-05-14

### Nuevas clases
- `GenApi` — Builder JSON variádico `JsonStr("k",v,...)`, `JsonArray`, `JsonPaged`, `nested`, `JsonList`, `toJson`
- `JxWebSocket` — Clase base para endpoints WebSocket con salas y broadcast
- `JxWsRegistrar` — `@WebListener` que registra `@JxWsEndpoint` automáticamente
- `JxDevMode` — Watcher de cambios de archivos en perfil `dev`

### Mejoras de API
- `DBRow` — `GetFloat()`, `GetBigDecimal()`, `GetDate()`, `GetDateTime()`, `toJson()`
- `DBRowSet` — `Add()`, `Get()`, `First()`, `Size()`, `IsEmpty()`, `Result()` como primera clase
- `JxDB` — Named params `:nombre`, `insertBatch()`, `upsertBatch()`, `queryNamed()`, `execNamed()`
- `JxDB` — Fix `LIMIT 1` en MSSQL (ahora `SELECT TOP 1`), fix paginación MSSQL
- `JxDB` — `safeId()` acepta `schema.tabla` con punto
- `JxController` — `Post()`, `Param()`, `Arg()`, `View()`, `Text()`, `Json()`, `Redirect()` etc. como primera clase
- `JxRequest` — `UploadFile()`, `UploadFiles()` como primera clase
- `JxMapping` — `@JxWsEndpoint`, nuevos validators en `JxValidation`

### JxValidation — nuevas anotaciones
`@JxNotNull`, `@JxRange(min,max)`, `@JxIn({"a","b"})`, `@JxPhone`, `@JxDigits(n)`, `@JxSafe`, `@JxLength(n)`

`validate()` lanza `JxException` 422 con cuerpo JSON: `{"errors":{"campo":"mensaje"}}`

Nuevo método `checkMap()` retorna `Map<String,String>` de campo → primer error.

### JxScheduler
- `runOnce(task, delayMs)` — ejecutar una vez en N milisegundos
- `runAsync(task)` — ejecutar inmediatamente en el pool

### JxPool
- `stats()` — retorna `DBRow` con `{total, idle, active, max, engine}`
- `active()` — conexiones actualmente en uso

### BaseDbResolver
- Prioridad: override > `-D` prop > variable de entorno > `application.properties`
- Variables soportadas: `DB_URL`, `DB_USER`, `DB_PASS`, `DB_DRIVER`
- Carga automática de `application-{perfil}.properties`

### BaseSanitizer
- Patterns pre-compilados (mayor rendimiento)
- Nuevos vectores: `<iframe>`, `<object>`, `<embed>`, `vbscript:`, `data:text/html`
- Nuevo método `cleanBasic()` para campos no-HTML

### MainLxServlet
- Detección automática de Virtual Threads (Java 21+) para `@JxAsync`
- En Java 17–20 usa pool de plataforma como antes

### Tamaño del JAR
- v2.7.0: 177 KB
- v3.0.0: 205 KB (+28 KB por nuevas clases)
- Dependencias externas de runtime: **0**

---

*Documento actualizado 2026-05-14. Referencia tecnica interna del proyecto JxMVC / Lux.*

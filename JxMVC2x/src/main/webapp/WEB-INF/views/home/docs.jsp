<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/shared/header.jspf" %>

<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github-dark-dimmed.min.css">
<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/java.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/properties.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/xml.min.js"></script>

<style>
  .hljs { background: transparent !important; font-size: 0.72rem; line-height: 1.6; }
  .code-block { background: #1e2432; border: 1px solid #2d3448; border-radius: .625rem; overflow: hidden; }
  .code-label { font-size: .6rem; font-family: monospace; letter-spacing: .12em; padding: .35rem .75rem;
                border-bottom: 1px solid #2d3448; color: #8892aa; text-transform: uppercase; }
  .code-block pre { padding: .85rem 1rem; margin: 0; overflow-x: auto; }
</style>

<%-- ── Encabezado ───────────────────────────────────────────────────── --%>
<div class="flex items-start justify-between mb-10 jx-reveal jx-delay-1">
  <div>
    <p class="text-xs font-mono uppercase tracking-[0.25em] text-slate-400 mb-2">Reference</p>
    <h1 class="text-4xl font-bold tracking-tight">JxMVC <span class="text-accent">3.0</span> Docs</h1>
    <p class="text-sm text-slate-500 dark:text-slate-400 mt-2 max-w-xl">
      Jakarta EE 11 · Java 17+ · Cero dependencias en runtime · WAR ~177 KB
    </p>
  </div>
  <a href="${pageContext.request.contextPath}/home/downloads"
     class="hidden sm:inline-flex px-4 py-2 bg-slate-900 dark:bg-white text-white dark:text-slate-900 text-xs font-medium rounded-lg hover:opacity-90 transition-opacity shrink-0">
    Descargar →
  </a>
</div>

<%-- ── Nav de secciones ────────────────────────────────────────────── --%>
<div class="flex flex-wrap gap-2 mb-10 pb-6 border-b border-slate-200 dark:border-slate-800 jx-reveal jx-delay-2">
  <% String[] sections = {"Routing","Controladores","Base de datos","Validacion","Seguridad","Filtros","Async & Retry","DI","Config","Sistema"}; %>
  <% for (String s : sections) { %>
    <a href="#<%= s.toLowerCase().replace(" ", "-").replace("&", "").replace("  ","-") %>"
       class="px-3 py-1 text-xs font-mono border border-slate-200 dark:border-slate-700 rounded-md
              text-slate-600 dark:text-slate-400 hover:bg-slate-900 hover:text-white dark:hover:bg-white
              dark:hover:text-slate-900 transition-all">
      <%= s %>
    </a>
  <% } %>
</div>

<div class="space-y-16">

<%-- ══════════════════════════════════════════════════════════════════
     1. ROUTING
════════════════════════════════════════════════════════════════════ --%>
<section id="routing">
  <h2 class="text-lg font-semibold mb-6 flex items-center gap-3">
    <span class="text-xs font-mono text-slate-400">01</span> Routing
  </h2>
  <div class="grid md:grid-cols-2 gap-4">

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">Convención URL</p>
      <p class="text-xs text-slate-500 dark:text-slate-400 mb-3">
        <code class="font-mono">/controlador/accion/arg0/arg1</code> — cero configuración.
      </p>
      <div class="code-block">
        <div class="code-label">HomeController.java</div>
        <pre><code class="language-java">@JxControllerMain              // ruta raíz "/"
@JxControllerMapping("home")
public class HomeController extends JxController {

    public ActionResult index() {      // GET /home/index
        return view("home/index");
    }

    public ActionResult list() {
        String page = model.arg(0);    // GET /home/list/2
        return text("page=" + page);
    }
}</code></pre>
      </div>
    </div>

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">Anotaciones</p>
      <p class="text-xs text-slate-500 dark:text-slate-400 mb-3">
        GET, POST, PUT, DELETE, PATCH, ANY. Plantillas <code class="font-mono">{var}</code>.
      </p>
      <div class="code-block">
        <div class="code-label">ApiController.java</div>
        <pre><code class="language-java">@JxControllerMapping("api")
public class ApiController extends JxController {

    @JxGetMapping("/users/{id}")       // GET /users/42
    public ActionResult getUser() {
        String id = model.pathVar("id");
        return json("{\"id\":" + id + "}");
    }

    @JxPostMapping("save")             // POST /api/save
    public ActionResult save() { ... }

    @JxAnyMapping("ping")              // cualquier verbo
    public ActionResult ping() {
        return text("pong");
    }
}</code></pre>
      </div>
    </div>

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">REST Controller</p>
      <p class="text-xs text-slate-500 dark:text-slate-400 mb-3">
        Alias rápido para APIs — Content-Type JSON por defecto.
      </p>
      <div class="code-block">
        <div class="code-label">ProductApi.java</div>
        <pre><code class="language-java">@JxRestController("/products")
public class ProductApi extends JxController {

    @JxGetMapping("")          // GET /products
    public ActionResult list() {
        return json(repo.findAll());
    }

    @JxGetMapping("{id}")      // GET /products/5
    public ActionResult get() {
        long id = Long.parseLong(model.pathVar("id"));
        return json(repo.findById(id));
    }

    @JxDeleteMapping("{id}")   // DELETE /products/5
    public ActionResult delete() {
        repo.deleteById(Long.parseLong(model.pathVar("id")));
        return json("{\"ok\":true}");
    }
}</code></pre>
      </div>
    </div>

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">Profile por ruta</p>
      <p class="text-xs text-slate-500 dark:text-slate-400 mb-3">
        Activa endpoints solo en ciertos perfiles de ejecución.
      </p>
      <div class="code-block">
        <div class="code-label">AdminController.java</div>
        <pre><code class="language-java">@JxProfile("dev")              // solo perfil dev
@JxGetMapping("debug")
public ActionResult debug() {
    return json(JxMetrics.snapshot());
}

@JxProfile({"prod","staging"}) // varios perfiles
@JxGetMapping("status")
public ActionResult status() {
    return json("{\"env\":\"" + JxProfile.active() + "\"}");
}</code></pre>
      </div>
    </div>

  </div>
</section>

<%-- ══════════════════════════════════════════════════════════════════
     2. CONTROLADORES
════════════════════════════════════════════════════════════════════ --%>
<section id="controladores">
  <h2 class="text-lg font-semibold mb-6 flex items-center gap-3">
    <span class="text-xs font-mono text-slate-400">02</span> Controladores
  </h2>
  <div class="grid md:grid-cols-2 gap-4">

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">ActionResult — todos los tipos</p>
      <div class="code-block">
        <div class="code-label">ejemplos de respuesta</div>
        <pre><code class="language-java">// Vista JSP
return view("home/dashboard");

// Texto plano
return text("pong!");

// JSON raw
return json("{\"ok\":true,\"id\":42}");

// JSON de objeto (usa JxJson)
return json(miObjeto);

// Redirect
return redirect("/home/index");

// Status personalizado
return view("home/created").status(201);

// Archivo binario
byte[] pdf = generarPdf();
view.raw(pdf, "application/pdf", "reporte.pdf");
return null;</code></pre>
      </div>
    </div>

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">model (JxRequest) · view (JxResponse)</p>
      <div class="code-block">
        <div class="code-label">acceso a request y response</div>
        <pre><code class="language-java">// Parámetros de formulario / query string
String nombre = model.param("nombre");

// Argumentos posicionales en la URL
String id   = model.arg(0);    // /ctrl/action/42  → "42"
String slug = model.argRaw(1); // sin sanitizar

// Variables de plantilla
String uid = model.pathVar("id");  // /users/{id}

// Session
model.session().setAttribute("user", dto);

// Cabeceras y cookies
String token = model.header("Authorization");

// Pasar variables a la vista
model.setVar("title", "Dashboard");
model.setVar("items", lista);

// Cabecera / status en respuesta
view.status(201);
view.header("X-Token", jwt);
view.contentType("application/json");</code></pre>
      </div>
    </div>

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">@JxBeforeAction · @JxAfterAction</p>
      <div class="code-block">
        <div class="code-label">interceptores de ciclo de vida</div>
        <pre><code class="language-java">@JxControllerMapping("admin")
public class AdminController extends JxController {

    // Se ejecuta antes de cada acción (o solo las indicadas)
    @JxBeforeAction(only = {"create","update"})
    public void requireAdmin() {
        String role = (String) model.session().getAttribute("role");
        if (!"ADMIN".equals(role))
            throw JxException.forbidden("Solo administradores");
    }

    // Inyecta variables en el modelo antes de renderizar
    @JxModelAttr
    public void commonAttrs() {
        model.setVar("appVersion", "3.0.0");
        model.setVar("usuario", model.session().getAttribute("user"));
    }

    @JxAfterAction
    public void audit() {
        log.info("Admin action: {}", model.path());
    }

    @JxGetMapping("create")
    public ActionResult create() { return view("admin/create"); }
}</code></pre>
      </div>
    </div>

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">@JxAdvice — manejador global de excepciones</p>
      <div class="code-block">
        <div class="code-label">GlobalAdvice.java</div>
        <pre><code class="language-java">@JxAdvice
public class GlobalAdvice {

    // Captura cualquier JxException no manejada en el controlador
    @JxExceptionHandler(JxException.class)
    public ActionResult onJxError(JxException ex) {
        return ActionResult.json(
            "{\"ok\":false,\"code\":" + ex.getStatus() +
            ",\"error\":\"" + ex.getMessage() + "\"}"
        ).status(ex.getStatus());
    }

    // Captura excepciones de runtime genéricas
    @JxExceptionHandler(RuntimeException.class)
    public ActionResult onRuntime(RuntimeException ex) {
        return ActionResult.json("{\"ok\":false,\"error\":\"Error interno\"}")
                           .status(500);
    }
}</code></pre>
      </div>
    </div>

  </div>
</section>

<%-- ══════════════════════════════════════════════════════════════════
     3. BASE DE DATOS
════════════════════════════════════════════════════════════════════ --%>
<section id="base-de-datos">
  <h2 class="text-lg font-semibold mb-6 flex items-center gap-3">
    <span class="text-xs font-mono text-slate-400">03</span> Base de datos
  </h2>
  <div class="grid md:grid-cols-2 gap-4">

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">JxRepository — CRUD genérico</p>
      <div class="code-block">
        <div class="code-label">ProductRepository.java</div>
        <pre><code class="language-java">@JxTable("productos")
public class Producto {
    @JxId public long id;
    @JxRequired @JxMinLength(2) public String nombre;
    public double precio;
    public boolean activo;          // soft delete column
}

@JxService
public class ProductoRepo extends JxRepository&lt;Producto, Long&gt; {

    public ProductoRepo() {
        super("productos", Producto.class);
        enableSoftDelete("activo", "true");  // soft delete
    }

    // Query personalizado — ? = parámetros posicionales
    @JxQuery("SELECT * FROM productos WHERE precio &lt; ?")
    public List&lt;Producto&gt; baratos(double max) {
        return executeQuery(max);
    }

    // Paginación
    public DBRowSet pagina(int page) {
        return findAllPaged(page, 20);  // 20 por página
    }
}</code></pre>
      </div>
    </div>

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">JxDB — acceso JDBC directo</p>
      <div class="code-block">
        <div class="code-label">uso en controlador</div>
        <pre><code class="language-java">public ActionResult reportes() {
    try (JxDB db = db()) {           // AutoCloseable — devuelve al pool

        // Query → DBRowSet (iterable)
        DBRowSet rows = db.query(
            "SELECT id, nombre, total FROM pedidos WHERE fecha = ?",
            LocalDate.now()
        );

        // Fila individual
        DBRow row = db.queryOne(
            "SELECT * FROM config WHERE clave = ?", "max_items"
        );

        // Mutación
        int affected = db.execute(
            "UPDATE productos SET stock = stock - ? WHERE id = ?",
            1, productId
        );

        model.setVar("pedidos", rows.asList());
        model.setVar("config",  row);
    }
    return view("home/reportes");
}</code></pre>
      </div>
    </div>

    <div class="md:col-span-2">
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">application.properties — configuración de BD</p>
      <div class="code-block">
        <div class="code-label">src/main/resources/application.properties</div>
        <pre><code class="language-properties">jxmvc.controllers.package = com.miapp.controllers

# PostgreSQL
jxmvc.db.url     = jdbc:postgresql://localhost:5432/mi_db
jxmvc.db.user    = postgres
jxmvc.db.pass    = secret

# Pool de conexiones
jxmvc.pool.enabled  = true
jxmvc.pool.size     = 10
jxmvc.pool.timeout  = 5       # segundos para obtener conexión del pool</code></pre>
      </div>
    </div>

  </div>
</section>

<%-- ══════════════════════════════════════════════════════════════════
     4. VALIDACIÓN
════════════════════════════════════════════════════════════════════ --%>
<section id="validacion">
  <h2 class="text-lg font-semibold mb-6 flex items-center gap-3">
    <span class="text-xs font-mono text-slate-400">04</span> Validación
  </h2>
  <div class="grid md:grid-cols-2 gap-4">

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">Anotaciones sobre entidades</p>
      <div class="code-block">
        <div class="code-label">Usuario.java</div>
        <pre><code class="language-java">public class Usuario {

    @JxRequired
    @JxMinLength(3)
    @JxMaxLength(50)
    public String nombre;

    @JxRequired
    @JxEmail                       // valida formato email
    public String email;

    @JxPattern("[0-9]{8}")         // regex personalizado
    public String dni;

    @JxMin(0)
    @JxMax(120)
    public int edad;
}</code></pre>
      </div>
    </div>

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">Validar en el controlador</p>
      <div class="code-block">
        <div class="code-label">uso en acción</div>
        <pre><code class="language-java">@JxPostMapping("registro")
public ActionResult registro() {
    Usuario u = new Usuario();
    u.nombre = model.param("nombre");
    u.email  = model.param("email");
    u.dni    = model.param("dni");

    // JxValidation lanza JxException(400) si falla
    JxValidation.validate(u);

    repo.save(u);
    return redirect("/home/index");
}

// O capturar los errores manualmente
JxValidation.Result result = JxValidation.check(u);
if (!result.isValid()) {
    model.setVar("errors", result.getErrors());
    return view("home/registro");
}</code></pre>
      </div>
    </div>

  </div>
</section>

<%-- ══════════════════════════════════════════════════════════════════
     5. SEGURIDAD
════════════════════════════════════════════════════════════════════ --%>
<section id="seguridad">
  <h2 class="text-lg font-semibold mb-6 flex items-center gap-3">
    <span class="text-xs font-mono text-slate-400">05</span> Seguridad
  </h2>
  <div class="grid md:grid-cols-2 gap-4">

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">Autenticación y roles</p>
      <div class="code-block">
        <div class="code-label">control de acceso</div>
        <pre><code class="language-java">// Requiere sesión activa (atributo "user" en sesión)
@JxRequireAuth
@JxGetMapping("perfil")
public ActionResult perfil() { ... }

// Requiere rol específico
@JxRequireRole("ADMIN")
@JxGetMapping("panel")
public ActionResult panel() { ... }

// Por controlador completo
@JxRequireAuth
@JxControllerMapping("admin")
public class AdminController extends JxController { ... }</code></pre>
      </div>
    </div>

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">Rate limiting</p>
      <div class="code-block">
        <div class="code-label">@JxRateLimit</div>
        <pre><code class="language-java">// Máximo 5 requests por IP en 60 segundos
@JxRateLimit(requests = 5, window = 60)
@JxPostMapping("login")
public ActionResult login() {
    String user = model.param("username");
    String pass = model.param("password");
    // ... autenticar
    return redirect("/home/index");
}

// Rate limit a nivel de controlador
@JxRateLimit(requests = 100, window = 3600)
@JxControllerMapping("api")
public class ApiController extends JxController { ... }</code></pre>
      </div>
    </div>

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">CORS</p>
      <div class="code-block">
        <div class="code-label">@JxCors</div>
        <pre><code class="language-java">// Por controlador
@JxCors(origins = {"https://app.midominio.com"})
@JxControllerMapping("api")
public class ApiController extends JxController { ... }

// Por acción (sobreescribe al controlador)
@JxCors(origins = {"*"}, methods = {"GET","POST"})
@JxGetMapping("public")
public ActionResult publicData() { ... }</code></pre>
      </div>
    </div>

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">Headers de seguridad — automáticos</p>
      <div class="code-block">
        <div class="code-label">application.properties</div>
        <pre><code class="language-properties"># Siempre activos en todas las respuestas:
# X-Content-Type-Options: nosniff
# Referrer-Policy: strict-origin-when-cross-origin

# Configurables:
jxmvc.security.frame-options = SAMEORIGIN   # o DENY / false
jxmvc.security.hsts          = false        # true solo con HTTPS
jxmvc.security.hsts.maxage   = 31536000</code></pre>
      </div>
    </div>

  </div>
</section>

<%-- ══════════════════════════════════════════════════════════════════
     6. FILTROS
════════════════════════════════════════════════════════════════════ --%>
<section id="filtros">
  <h2 class="text-lg font-semibold mb-6 flex items-center gap-3">
    <span class="text-xs font-mono text-slate-400">06</span> Filtros globales
  </h2>
  <div class="grid md:grid-cols-2 gap-4">

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">Registrar filtros</p>
      <div class="code-block">
        <div class="code-label">AppFilters.java · @JxFilter</div>
        <pre><code class="language-java">@JxFilter
public class AuthFilter implements JxFilterChain {

    @Override
    public boolean before(JxFilterContext ctx) {
        String path = ctx.path();
        // Permitir rutas públicas
        if (path.startsWith("/home") || path.startsWith("/assets"))
            return true;

        Object user = ctx.request().getSession()
                         .getAttribute("user");
        if (user == null) {
            ctx.response().redirect("/login");
            return false;   // corta la cadena
        }
        return true;
    }

    @Override
    public void after(JxFilterContext ctx) {
        ctx.response().header("X-Frame-Options", "DENY");
    }
}</code></pre>
      </div>
    </div>

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">Registro programático</p>
      <div class="code-block">
        <div class="code-label">alternativa sin anotación</div>
        <pre><code class="language-java">// En el servlet de inicio o AppConfig:
JxFilters.before(ctx -> {
    ctx.response().header("X-App", "JxMVC/3.0");
    return true;
});

JxFilters.after(ctx -> {
    long ms = ctx.elapsed();
    ctx.response().header("X-Response-Time", ms + "ms");
});</code></pre>
      </div>
    </div>

  </div>
</section>

<%-- ══════════════════════════════════════════════════════════════════
     7. ASYNC & RETRY
════════════════════════════════════════════════════════════════════ --%>
<section id="async-retry">
  <h2 class="text-lg font-semibold mb-6 flex items-center gap-3">
    <span class="text-xs font-mono text-slate-400">07</span> Async &amp; Retry
  </h2>
  <div class="grid md:grid-cols-2 gap-4">

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">@JxAsync — ejecución en background</p>
      <div class="code-block">
        <div class="code-label">respuesta inmediata, proceso en background</div>
        <pre><code class="language-java">// Responde 202 Accepted al instante.
// La acción corre en el pool de threads async.
@JxAsync
@JxPostMapping("exportar")
public ActionResult exportar() {
    // Este bloque se ejecuta en background
    byte[] csv = reportService.generarCSV();
    emailService.enviar(csv, destinatario);
    return null;   // respuesta ya fue enviada (202)
}

// Configurar el pool en application.properties:
// jxmvc.async.threads = 8</code></pre>
      </div>
    </div>

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">@JxRetry — reintentos automáticos</p>
      <div class="code-block">
        <div class="code-label">resiliencia ante fallos transitorios</div>
        <pre><code class="language-java">// Hasta 3 intentos con 500ms entre ellos.
// Si todos fallan, propaga la última excepción.
@JxRetry(times = 3, delay = 500)
@JxGetMapping("externo")
public ActionResult externo() {
    // Llama a una API externa que puede fallar
    String data = httpClient.get("https://api.externa.com/data");
    return json(data);
}

// Combinable con @JxAsync
@JxAsync
@JxRetry(times = 2, delay = 1000)
@JxPostMapping("sync")
public ActionResult sync() { ... }</code></pre>
      </div>
    </div>

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">JxScheduler — tareas programadas</p>
      <div class="code-block">
        <div class="code-label">background jobs</div>
        <pre><code class="language-java">// En el init() del servlet, o en un @JxService:
JxScheduler.scheduleAtFixedRate(() -> {
    log.info("Limpiando sesiones expiradas...");
    sesionRepo.eliminarExpiradas();
}, 0, 3_600_000);          // cada hora (ms)

// Keepalive del pool (ya incluido por defecto):
JxScheduler.scheduleAtFixedRate(() -> {
    JxPool pool = JxPool.global();
    if (pool != null) pool.keepAlive();
}, 180_000, 180_000);      // cada 3 minutos</code></pre>
      </div>
    </div>

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">JxJson — serialización propia</p>
      <div class="code-block">
        <div class="code-label">sin Jackson ni Gson</div>
        <pre><code class="language-java">// Objeto → JSON string
String json = JxJson.toJson(miObjeto);
// Serializa campos públicos, listas, mapas y primitivos.

// En un controlador:
public ActionResult datos() {
    List&lt;Producto&gt; lista = repo.findAll();
    return json(lista);   // usa JxJson internamente
}

// En BaseController (convención recomendada):
protected ActionResult jsonOk(Object data) {
    return json("{\"ok\":true,\"data\":" + JxJson.toJson(data) + "}");
}
protected ActionResult jsonError(int status, String msg) {
    view.status(status);
    return json("{\"ok\":false,\"error\":" + JxJson.toJson(msg) + "}");
}</code></pre>
      </div>
    </div>

  </div>
</section>

<%-- ══════════════════════════════════════════════════════════════════
     8. INYECCIÓN DE DEPENDENCIAS
════════════════════════════════════════════════════════════════════ --%>
<section id="di">
  <h2 class="text-lg font-semibold mb-6 flex items-center gap-3">
    <span class="text-xs font-mono text-slate-400">08</span> Inyección de dependencias
  </h2>
  <div class="grid md:grid-cols-2 gap-4">

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">@JxService · @JxInject</p>
      <div class="code-block">
        <div class="code-label">registro automático + inyección</div>
        <pre><code class="language-java">// Marcar como servicio singleton
@JxService
public class EmailService {
    public void enviar(String to, String body) { ... }
}

@JxService
public class UserService {

    @JxInject
    private EmailService emailService;   // inyectado automáticamente

    public void registrar(Usuario u) {
        userRepo.save(u);
        emailService.enviar(u.email, "Bienvenido");
    }
}

// En el controlador:
@JxControllerMapping("user")
public class UserController extends JxController {

    @JxInject
    private UserService userService;     // inyectado

    @JxPostMapping("registro")
    public ActionResult registro() {
        userService.registrar(buildUsuario());
        return redirect("/home/index");
    }
}</code></pre>
      </div>
    </div>

    <div>
      <p class="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">Acceso al registro</p>
      <div class="code-block">
        <div class="code-label">JxServiceRegistry</div>
        <pre><code class="language-java">// Obtener instancia singleton por clase
EmailService svc = JxServiceRegistry.get(EmailService.class);

// Registrar manualmente (útil en tests)
JxServiceRegistry.register(new MockEmailService());

// Todos los @JxService se escanean y registran
// automáticamente al arrancar el framework.</code></pre>
      </div>
    </div>

  </div>
</section>

<%-- ══════════════════════════════════════════════════════════════════
     9. CONFIGURACIÓN
════════════════════════════════════════════════════════════════════ --%>
<section id="config">
  <h2 class="text-lg font-semibold mb-6 flex items-center gap-3">
    <span class="text-xs font-mono text-slate-400">09</span> Configuración completa
  </h2>
  <div class="code-block">
    <div class="code-label">src/main/resources/application.properties</div>
    <pre><code class="language-properties"># ── Núcleo ─────────────────────────────────────────────────────────
jxmvc.controllers.package = com.miapp.controllers
jxmvc.profile             = dev           # dev | prod | staging
jxmvc.log.level           = INFO          # DEBUG | INFO | WARN | ERROR

# ── Base de datos ───────────────────────────────────────────────────
jxmvc.db.url              = jdbc:postgresql://localhost:5432/mi_db
jxmvc.db.user             = postgres
jxmvc.db.pass             = secret

# ── Pool de conexiones ──────────────────────────────────────────────
jxmvc.pool.enabled        = true
jxmvc.pool.size           = 10            # máximo de conexiones
jxmvc.pool.timeout        = 5             # segundos para obtener del pool

# ── Async ───────────────────────────────────────────────────────────
jxmvc.async.threads       = 8

# ── Seguridad ───────────────────────────────────────────────────────
jxmvc.security.frame-options = SAMEORIGIN # DENY | false
jxmvc.security.hsts          = false      # true solo con HTTPS
jxmvc.security.hsts.maxage   = 31536000   # 1 año en segundos</code></pre>
  </div>
</section>

<%-- ══════════════════════════════════════════════════════════════════
     10. ENDPOINTS DEL SISTEMA
════════════════════════════════════════════════════════════════════ --%>
<section id="sistema">
  <h2 class="text-lg font-semibold mb-6 flex items-center gap-3">
    <span class="text-xs font-mono text-slate-400">10</span> Endpoints del sistema
  </h2>
  <div class="divide-y divide-slate-100 dark:divide-slate-800 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden">

    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between px-5 py-4 gap-2 hover:bg-slate-50 dark:hover:bg-slate-900 transition-colors">
      <div class="flex items-center gap-4">
        <span class="text-xs font-mono text-emerald-600 dark:text-emerald-400 w-8 shrink-0">GET</span>
        <div>
          <code class="text-sm font-mono">/jx/health</code>
          <p class="text-xs text-slate-400 mt-0.5">Estado del pool, uptime, threads activos</p>
        </div>
      </div>
      <a href="${pageContext.request.contextPath}/jx/health" target="_blank"
         class="text-xs font-mono text-accent hover:underline shrink-0">probar →</a>
    </div>

    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between px-5 py-4 gap-2 hover:bg-slate-50 dark:hover:bg-slate-900 transition-colors">
      <div class="flex items-center gap-4">
        <span class="text-xs font-mono text-emerald-600 dark:text-emerald-400 w-8 shrink-0">GET</span>
        <div>
          <code class="text-sm font-mono">/jx/info</code>
          <p class="text-xs text-slate-400 mt-0.5">Versión, perfil activo, Java, servidor</p>
        </div>
      </div>
      <a href="${pageContext.request.contextPath}/jx/info" target="_blank"
         class="text-xs font-mono text-accent hover:underline shrink-0">probar →</a>
    </div>

    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between px-5 py-4 gap-2 hover:bg-slate-50 dark:hover:bg-slate-900 transition-colors">
      <div class="flex items-center gap-4">
        <span class="text-xs font-mono text-emerald-600 dark:text-emerald-400 w-8 shrink-0">GET</span>
        <div>
          <code class="text-sm font-mono">/jx/metrics</code>
          <p class="text-xs text-slate-400 mt-0.5">Peticiones por ruta: total, errores, latencia media</p>
        </div>
      </div>
      <a href="${pageContext.request.contextPath}/jx/metrics" target="_blank"
         class="text-xs font-mono text-accent hover:underline shrink-0">probar →</a>
    </div>

    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between px-5 py-4 gap-2 hover:bg-slate-50 dark:hover:bg-slate-900 transition-colors">
      <div class="flex items-center gap-4">
        <span class="text-xs font-mono text-emerald-600 dark:text-emerald-400 w-8 shrink-0">GET</span>
        <div>
          <code class="text-sm font-mono">/jx/openapi</code>
          <p class="text-xs text-slate-400 mt-0.5">Spec OpenAPI 3.0 generada automáticamente de las anotaciones</p>
        </div>
      </div>
      <a href="${pageContext.request.contextPath}/jx/openapi" target="_blank"
         class="text-xs font-mono text-accent hover:underline shrink-0">probar →</a>
    </div>

  </div>
</section>

</div><%-- /space-y-16 --%>

<script>hljs.highlightAll();</script>

<%@ include file="/WEB-INF/views/shared/footer.jspf" %>

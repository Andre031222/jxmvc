<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/shared/header.jspf" %>

<link id="hljs-theme" rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hljs/dark.min.css">
<script src="${pageContext.request.contextPath}/assets/js/highlight.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/hljs-java.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/hljs-properties.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/hljs-xml.min.js"></script>

<style>
  /* ── Code blocks — tema claro y oscuro ─────────────────────────── */
  :root {
    --cb-bg:     #f6f8fa;
    --cb-border: rgba(0,0,0,0.08);
    --cb-label:  rgba(0,0,0,0.45);
    --cb-lborder:rgba(0,0,0,0.06);
  }
  .dark {
    --cb-bg:     #1e2432;
    --cb-border: #2d3448;
    --cb-label:  #8892aa;
    --cb-lborder:#2d3448;
  }

  .hljs { background: transparent !important; font-size: 0.72rem; line-height: 1.65; }

  /* overflow:clip = recorta visualmente (border-radius OK) pero NO suprime
     el contexto de scroll de los hijos — pre puede hacer scroll horizontal */
  .code-block {
    background: var(--cb-bg);
    border: 1px solid var(--cb-border);
    border-radius: .625rem;
    overflow: clip;
    max-width: 100%;
  }
  .code-label {
    font-size: .6rem; font-family: monospace; letter-spacing: .12em;
    padding: .32rem .75rem;
    border-bottom: 1px solid var(--cb-lborder);
    color: var(--cb-label);
    text-transform: uppercase;
    background: var(--cb-bg);
  }
  .code-block pre {
    padding: .75rem 1rem;
    margin: 0;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  .code-block pre code { white-space: pre; display: block; min-width: 0; }

  /* Grid items: min-width:0 evita que min-width:auto expanda el track */
  .grid > div, .grid > [class*="col-span"] { min-width: 0; }

  /* ── Sidebar (escritorio) ───────────────────────────────────────── */
  .jx-slink {
    display: flex; align-items: center; gap: 8px;
    padding: 5px 10px; border-radius: 9px;
    font-size: .8125rem; color: #6e6e73;
    text-decoration: none;
    transition: background 0.13s ease, color 0.13s ease;
  }
  .dark .jx-slink { color: #636366; }
  .jx-slink:hover { background: rgba(0,0,0,0.04); color: #1d1d1f; }
  .dark .jx-slink:hover { background: rgba(255,255,255,0.06); color: #e5e5ea; }
  .jx-dot {
    width: 3px; height: 14px; border-radius: 2px; flex-shrink: 0;
    background: #d1d1d6; transition: background 0.15s ease, transform 0.15s ease;
  }
  .dark .jx-dot { background: #3a3a3c; }
  .jx-slink:hover .jx-dot { transform: scaleY(1.15); }

  /* ── Mobile nav — scroll horizontal ─────────────────────────────── */
  .jx-section-nav {
    display: flex; overflow-x: auto; -webkit-overflow-scrolling: touch;
    scrollbar-width: none; gap: .5rem;
    padding-bottom: 1.25rem; margin-bottom: 2rem;
    border-bottom: 1px solid rgba(0,0,0,0.06);
  }
  .dark .jx-section-nav { border-bottom-color: rgba(255,255,255,0.06); }
  .jx-section-nav::-webkit-scrollbar { display: none; }
  .jx-section-nav a { flex-shrink: 0; }
</style>

<div class="max-w-6xl mx-auto px-4 sm:px-6 py-10">

<%-- ── Encabezado ───────────────────────────────────────────────────── --%>
<div class="flex items-start justify-between mb-10 jx-reveal jx-delay-1">
  <div>
    <p class="text-xs font-mono uppercase tracking-[0.25em] text-muted dark:text-[#86868b] mb-2">Reference</p>
    <h1 class="text-4xl font-bold tracking-tight text-ink dark:text-[#f5f5f7]">JxMVC <span class="text-apple">3.1</span> Docs</h1>
    <p class="text-sm text-muted dark:text-[#86868b] mt-2 max-w-xl">
      Jakarta EE 11 · Java 17+ · Cero dependencias en runtime · WAR ~205 KB
    </p>
  </div>
  <a href="${pageContext.request.contextPath}/home/downloads"
     class="hidden sm:inline-flex px-4 py-2 bg-apple text-white text-xs font-medium rounded-full hover:bg-[#0077ed] transition-colors shrink-0">
    Descargar →
  </a>
</div>

<%-- ── Layout: sidebar (escritorio) + contenido ─────────────────── --%>
<%-- Sin items-start: el aside se estira al alto del contenido, sticky funciona en todo el scroll --%>
<div class="flex gap-10 jx-reveal jx-delay-2">

  <%-- Sidebar — escritorio y laptop --%>
  <aside class="hidden md:block w-[200px] shrink-0 self-start sticky top-20">
    <div class="pt-1 pb-8">
      <p class="text-[10px] font-mono uppercase tracking-[0.22em] text-muted dark:text-[#4a4a52] px-2.5 mb-3">Referencia</p>
      <nav id="jxDocNav" class="space-y-0.5">
      <%
      String[][] sideNav = {
          {"routing",       "Routing",          "#087CFA", "01"},
          {"controladores", "Controladores",    "#087CFA", "02"},
          {"base-de-datos", "Base de datos",    "#5A63D6", "03"},
          {"validacion",    "Validación · 21",  "#FC801D", "04"},
          {"seguridad",     "Seguridad",        "#FE2857", "05"},
          {"filtros",       "Filtros",          "#5A63D6", "06"},
          {"cron-async",    "Cron & Async",     "#FC801D", "07"},
          {"di",            "Inyección DI",     "#087CFA", "08"},
          {"config",        "Configuración",    "#5A63D6", "09"},
          {"sistema",       "Endpoints",        "#087CFA", "10"},
      };
      for (String[] n : sideNav) {
      %>
      <a href="#<%= n[0] %>" class="jx-slink" data-section="<%= n[0] %>" data-color="<%= n[2] %>">
        <span class="jx-dot"></span>
        <span class="text-[10px] font-mono w-5 shrink-0 opacity-50"><%= n[3] %></span>
        <span><%= n[1] %></span>
      </a>
      <% } %>
      </nav>
      <div class="mt-5 px-2.5 pt-4 border-t border-black/[0.06] dark:border-white/[0.06]">
        <a href="${pageContext.request.contextPath}/home/downloads"
           class="flex items-center gap-1.5 text-xs text-apple hover:underline font-mono">
          <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
          </svg>
          Descargar
        </a>
      </div>
    </div>
  </aside>

  <%-- Área de contenido --%>
  <div class="flex-1 min-w-0">

    <%-- Nav móvil — drawer deslizable (solo < md) --%>
    <div class="md:hidden mb-6">
        <button id="jxDocsMobileBtn" onclick="jxDocsDrawerToggle()"
                class="flex items-center gap-2 w-full px-4 py-2.5 rounded-2xl
                       bg-white dark:bg-white/[0.05]
                       border border-black/[0.06] dark:border-white/[0.06]
                       text-muted dark:text-[#86868b] hover:text-ink dark:hover:text-[#f5f5f7]
                       hover:border-apple/30 transition-all text-sm">
            <svg class="w-4 h-4 shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path d="M4 6h16M4 12h10M4 18h7" stroke-linecap="round"/>
            </svg>
            <span class="text-xs font-mono">Secciones</span>
            <svg id="jxDocsMobileChevron" class="w-3.5 h-3.5 ml-auto transition-transform duration-200"
                 fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                <path d="M6 9l6 6 6-6"/>
            </svg>
        </button>
        <div id="jxDocsDrawer"
             class="hidden mt-2 bg-white dark:bg-white/[0.05]
                    border border-black/[0.06] dark:border-white/[0.06]
                    rounded-2xl overflow-hidden">
            <%
            String[][] mobNav = {
                {"routing",       "Routing",          "#087CFA", "01"},
                {"controladores", "Controladores",    "#087CFA", "02"},
                {"base-de-datos", "Base de datos",    "#5A63D6", "03"},
                {"validacion",    "Validación · 21",  "#FC801D", "04"},
                {"seguridad",     "Seguridad",        "#FE2857", "05"},
                {"filtros",       "Filtros",          "#5A63D6", "06"},
                {"cron-async",    "Cron & Async",     "#FC801D", "07"},
                {"di",            "Inyección DI",     "#087CFA", "08"},
                {"config",        "Configuración",    "#5A63D6", "09"},
                {"sistema",       "Endpoints",        "#087CFA", "10"},
            };
            for (String[] mn : mobNav) {
            %>
            <a href="#<%= mn[0] %>" onclick="jxDocsDrawerClose()"
               class="flex items-center gap-3 px-4 py-2.5
                      border-b border-black/[0.04] dark:border-white/[0.04] last:border-0
                      text-sm text-muted dark:text-[#86868b]
                      hover:text-ink dark:hover:text-[#f5f5f7]
                      hover:bg-black/[0.02] dark:hover:bg-white/[0.03] transition-colors">
                <span class="w-1.5 h-1.5 rounded-full shrink-0" style="background:<%= mn[2] %>"></span>
                <span class="text-[10px] font-mono opacity-50 w-5 shrink-0"><%= mn[3] %></span>
                <span><%= mn[1] %></span>
            </a>
            <% } %>
        </div>
    </div>

    <div class="space-y-16">

<%-- ══════════════════════════════════════════════════════════════════
     1. ROUTING
════════════════════════════════════════════════════════════════════ --%>
<section id="routing">
  <h2 class="text-lg font-semibold mb-6 flex items-center gap-3">
    <span class="text-xs font-mono text-muted dark:text-[#86868b]">01</span> Routing
  </h2>
  <div class="grid md:grid-cols-2 gap-4">

    <div>
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">Convención URL</p>
      <p class="text-xs text-muted dark:text-[#86868b] mb-3">
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
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">Anotaciones</p>
      <p class="text-xs text-muted dark:text-[#86868b] mb-3">
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
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">REST Controller</p>
      <p class="text-xs text-muted dark:text-[#86868b] mb-3">
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
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">Profile por ruta</p>
      <p class="text-xs text-muted dark:text-[#86868b] mb-3">
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
    <span class="text-xs font-mono text-muted dark:text-[#86868b]">02</span> Controladores
  </h2>
  <div class="grid md:grid-cols-2 gap-4">

    <div>
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">ActionResult — todos los tipos</p>
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
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">model (JxRequest) · view (JxResponse)</p>
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
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">@JxBeforeAction · @JxAfterAction</p>
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
        model.setVar("appVersion", "3.1.0");
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
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">@JxAdvice — manejador global de excepciones</p>
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
    <span class="text-xs font-mono text-muted dark:text-[#86868b]">03</span> Base de datos
  </h2>
  <div class="grid md:grid-cols-2 gap-4">

    <div>
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">JxRepository — CRUD genérico</p>
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
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">JxDB — acceso JDBC directo</p>
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
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">application.properties — configuración de BD</p>
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
    <span class="text-xs font-mono text-muted dark:text-[#86868b]">04</span> Validación
  </h2>
  <div class="grid md:grid-cols-2 gap-4">

    <div>
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">Anotaciones sobre entidades</p>
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
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">Validar en el controlador</p>
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

    <div>
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">Fechas, URLs y validadores custom <span class="text-apple font-mono normal-case">v3.1</span></p>
      <div class="code-block">
        <div class="code-label">nuevas anotaciones</div>
        <pre><code class="language-java">import java.time.LocalDate;

public class ReservaDto {

    @JxRequired @JxFuture          // debe ser fecha futura
    public LocalDate fechaReserva;

    @JxPast                        // debe ser fecha pasada
    public LocalDate fechaNacimiento;

    @JxUrl                         // http:// o https:// valido
    public String website;

    @JxCheck(RucPeruano.class)     // validador personalizado
    public String ruc;
}

// Implementar JxConstraint&lt;T&gt; para validadores propios:
public class RucPeruano implements JxValidation.JxConstraint&lt;String&gt; {
    public boolean isValid(String v) {
        return v != null &amp;&amp; v.matches("\\d{11}");
    }
    public String message() { return "RUC debe tener 11 digitos"; }
}
// Las instancias se cachean — se crean una sola vez.</code></pre>
      </div>
    </div>

    <div>
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">Todas las anotaciones disponibles</p>
      <div class="code-block">
        <div class="code-label">catalogo — 21 anotaciones</div>
        <pre><code class="language-java">// Strings
@JxRequired  @JxNotNull  @JxNotEmpty
@JxMinLength(n)  @JxMaxLength(n)  @JxLength(n)
@JxEmail  @JxPhone  @JxUrl  @JxPattern(regex)
@JxSafe  @JxDigits(n)  @JxIn({"a","b","c"})

// Numeros
@JxMin(n)  @JxMax(n)  @JxRange(min,max)  @JxPositive

// Fechas (java.time)
@JxFuture  @JxPast

// Custom
@JxCheck(MiValidador.class)</code></pre>
      </div>
    </div>

  </div>
</section>

<%-- ══════════════════════════════════════════════════════════════════
     5. SEGURIDAD
════════════════════════════════════════════════════════════════════ --%>
<section id="seguridad">
  <h2 class="text-lg font-semibold mb-6 flex items-center gap-3">
    <span class="text-xs font-mono text-muted dark:text-[#86868b]">05</span> Seguridad
  </h2>
  <div class="grid md:grid-cols-2 gap-4">

    <div>
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">Autenticación y roles</p>
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
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">Rate limiting</p>
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
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">CORS</p>
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
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">Headers de seguridad — automáticos</p>
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
    <span class="text-xs font-mono text-muted dark:text-[#86868b]">06</span> Filtros globales
  </h2>
  <div class="grid md:grid-cols-2 gap-4">

    <div>
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">Registrar filtros</p>
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
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">Registro programático</p>
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
<section id="cron-async">
  <h2 class="text-lg font-semibold mb-6 flex items-center gap-3">
    <span class="text-xs font-mono text-muted dark:text-[#86868b]">07</span> Cron &amp; Async
  </h2>
  <div class="grid md:grid-cols-2 gap-4">

    <div>
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">@JxAsync — ejecución en background</p>
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
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">@JxRetry — reintentos automáticos</p>
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
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">@JxScheduled — cron y fixed rate <span class="text-apple font-mono normal-case">v3.1</span></p>
      <div class="code-block">
        <div class="code-label">tareas programadas con cron</div>
        <pre><code class="language-java">@JxService
public class TareasService {

    // Cron 5 campos: min hora dom mes dow (0=domingo)
    @JxScheduled(cron = "0 3 * * *")    // cada dia a las 3 AM
    public void backupDiario() { ... }

    @JxScheduled(cron = "0 9 * * 1")    // cada lunes 9 AM
    public void reporteSemanal() { ... }

    @JxScheduled(cron = "0 0 1 * *")    // primer dia del mes
    public void facturacion() { ... }

    // Fixed rate/delay en milisegundos (sigue disponible)
    @JxScheduled(fixedDelay = 60_000, initialDelay = 5_000)
    public void purgeTokens() { ... }

    @JxScheduled(fixedRate = 30_000)
    public void refreshCache() { ... }
}

// Programático — sin @JxService:
JxScheduler.scheduleAtFixedRate(() -> tarea(), 0, 3_600_000);
JxScheduler.scheduleCron(() -> backup(), "0 2 * * *");
JxScheduler.runOnce(() -> notificar(), 5_000);</code></pre>
      </div>
    </div>

    <div>
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">JxJson — serialización propia</p>
      <div class="code-block">
        <div class="code-label">sin Jackson ni Gson</div>
        <pre><code class="language-java">// Soporta: String, Number, Boolean, List, Map,
// DBRow, DBRowSet, POJO con getters, java.time.*

// Tipos java.time — serializacion automatica (v3.1):
LocalDate.of(2026, 5, 17)     // → "2026-05-17"
LocalDateTime.now()            // → "2026-05-17T10:30:00"
LocalTime.of(10, 30)           // → "10:30"
java.sql.Date / Timestamp      // → formato ISO tambien

// POJO con campos de fecha — funciona directo:
public class PedidoDto {
    public String nombre;
    public LocalDate fecha;        // serializado como "2026-05-17"
    public LocalDateTime creado;   // serializado como "2026-05-17T..."
}
return json(pedidoDto);  // {"nombre":"...","fecha":"2026-05-17",...}

// Deserializacion — tambien automatica:
PedidoDto dto = JxJson.fromJson(body, PedidoDto.class);
// dto.fecha es LocalDate.of(2026, 5, 17)</code></pre>
      </div>
    </div>

  </div>
</section>

<%-- ══════════════════════════════════════════════════════════════════
     8. INYECCIÓN DE DEPENDENCIAS
════════════════════════════════════════════════════════════════════ --%>
<section id="di">
  <h2 class="text-lg font-semibold mb-6 flex items-center gap-3">
    <span class="text-xs font-mono text-muted dark:text-[#86868b]">08</span> Inyección de dependencias
  </h2>
  <div class="grid md:grid-cols-2 gap-4">

    <div>
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">@JxService · @JxInject</p>
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
      <p class="text-xs font-semibold text-muted dark:text-[#86868b] uppercase tracking-wider mb-2">Acceso al registro</p>
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
    <span class="text-xs font-mono text-muted dark:text-[#86868b]">09</span> Configuración completa
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
    <span class="text-xs font-mono text-muted dark:text-[#86868b]">10</span> Endpoints del sistema
  </h2>
  <div class="divide-y divide-black/[0.05] dark:divide-white/[0.05] border border-black/[0.06] dark:border-white/[0.06] rounded-2xl overflow-hidden">

    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between px-5 py-4 gap-2 hover:bg-black/[0.02] dark:hover:bg-white/[0.02] transition-colors">
      <div class="flex items-center gap-4">
        <span class="text-xs font-mono text-emerald-600 dark:text-emerald-400 w-8 shrink-0">GET</span>
        <div>
          <code class="text-sm font-mono">/jx/health</code>
          <p class="text-xs text-muted dark:text-[#86868b] mt-0.5">Estado del pool, uptime, threads activos</p>
        </div>
      </div>
      <a href="${pageContext.request.contextPath}/jx/health" target="_blank"
         class="text-xs font-mono text-apple hover:underline shrink-0">probar →</a>
    </div>

    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between px-5 py-4 gap-2 hover:bg-black/[0.02] dark:hover:bg-white/[0.02] transition-colors">
      <div class="flex items-center gap-4">
        <span class="text-xs font-mono text-emerald-600 dark:text-emerald-400 w-8 shrink-0">GET</span>
        <div>
          <code class="text-sm font-mono">/jx/info</code>
          <p class="text-xs text-muted dark:text-[#86868b] mt-0.5">Versión, perfil activo, Java, servidor</p>
        </div>
      </div>
      <a href="${pageContext.request.contextPath}/jx/info" target="_blank"
         class="text-xs font-mono text-apple hover:underline shrink-0">probar →</a>
    </div>

    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between px-5 py-4 gap-2 hover:bg-black/[0.02] dark:hover:bg-white/[0.02] transition-colors">
      <div class="flex items-center gap-4">
        <span class="text-xs font-mono text-emerald-600 dark:text-emerald-400 w-8 shrink-0">GET</span>
        <div>
          <code class="text-sm font-mono">/jx/metrics</code>
          <p class="text-xs text-muted dark:text-[#86868b] mt-0.5">Peticiones por ruta: total, errores, latencia media</p>
        </div>
      </div>
      <a href="${pageContext.request.contextPath}/jx/metrics" target="_blank"
         class="text-xs font-mono text-apple hover:underline shrink-0">probar →</a>
    </div>

    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between px-5 py-4 gap-2 hover:bg-black/[0.02] dark:hover:bg-white/[0.02] transition-colors">
      <div class="flex items-center gap-4">
        <span class="text-xs font-mono text-emerald-600 dark:text-emerald-400 w-8 shrink-0">GET</span>
        <div>
          <code class="text-sm font-mono">/jx/openapi</code>
          <p class="text-xs text-muted dark:text-[#86868b] mt-0.5">Spec OpenAPI 3.0 generada automáticamente de las anotaciones</p>
        </div>
      </div>
      <a href="${pageContext.request.contextPath}/jx/openapi" target="_blank"
         class="text-xs font-mono text-apple hover:underline shrink-0">probar →</a>
    </div>

  </div>
</section>

    </div><%-- /space-y-16 --%>
  </div><%-- /content --%>
</div><%-- /docs-layout --%>

<script>
// Drawer de secciones (móvil)
function jxDocsDrawerToggle() {
    var d = document.getElementById('jxDocsDrawer');
    var c = document.getElementById('jxDocsMobileChevron');
    if (!d) return;
    var opening = d.classList.toggle('hidden');
    if (c) c.style.transform = d.classList.contains('hidden') ? '' : 'rotate(180deg)';
}
function jxDocsDrawerClose() {
    var d = document.getElementById('jxDocsDrawer');
    var c = document.getElementById('jxDocsMobileChevron');
    if (d) d.classList.add('hidden');
    if (c) c.style.transform = '';
}

hljs.highlightAll();

// Sidebar — resalta sección activa
(function() {
    var links = document.querySelectorAll('#jxDocNav .jx-slink');
    if (!links.length) return;

    function activate(id) {
        links.forEach(function(l) {
            var isActive = l.dataset.section === id;
            l.classList.toggle('active', isActive);
            var dot = l.querySelector('.jx-dot');
            if (isActive) {
                var c = l.dataset.color || '#087CFA';
                l.style.color = c;
                var r = parseInt(c.slice(1,3),16), g = parseInt(c.slice(3,5),16), b = parseInt(c.slice(5,7),16);
                l.style.background = 'rgba('+r+','+g+','+b+',0.08)';
                if (dot) { dot.style.background = c; dot.style.opacity = '1'; }
            } else {
                l.style.color = ''; l.style.background = '';
                if (dot) { dot.style.background = ''; dot.style.opacity = ''; }
            }
        });
    }

    if (links[0]) activate(links[0].dataset.section);

    var io = new IntersectionObserver(function(entries) {
        entries.forEach(function(e) { if (e.isIntersecting) activate(e.target.id); });
    }, { rootMargin: '-10% 0px -72% 0px', threshold: 0 });

    document.querySelectorAll('section[id]').forEach(function(s) { io.observe(s); });

    links.forEach(function(link) {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            var t = document.getElementById(link.dataset.section);
            if (t) { t.scrollIntoView({ behavior: 'smooth', block: 'start' }); activate(link.dataset.section); }
        });
    });
})();

// Sincroniza tema de highlight.js con el tema global
(function() {
    var ctx = '${pageContext.request.contextPath}';
    function syncHljs() {
        var dark = document.documentElement.classList.contains('dark');
        document.getElementById('hljs-theme').href =
            ctx + '/assets/css/hljs/' + (dark ? 'dark' : 'light') + '.min.css';
    }
    syncHljs();
    // Observa cambios de tema en el html root
    new MutationObserver(syncHljs).observe(
        document.documentElement, { attributes: true, attributeFilter: ['class'] }
    );
})();
</script>

</div><%-- /container --%>
<%@ include file="/WEB-INF/views/shared/footer.jspf" %>

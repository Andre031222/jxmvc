<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/shared/header.jspf" %>

<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hljs/dark.min.css">
<script src="${pageContext.request.contextPath}/assets/js/highlight.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/hljs-java.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/hljs-properties.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/hljs-xml.min.js"></script>

<style>
  .jx-win { background:#1C1C1E; border:1px solid rgba(255,255,255,0.09); border-radius:16px; overflow:hidden; }
  .jx-win-bar { display:flex; align-items:center; gap:6px; padding:11px 16px; border-bottom:1px solid rgba(255,255,255,0.06); }
  .jxd-r { width:11px; height:11px; border-radius:50%; background:#FF5F57; flex-shrink:0; }
  .jxd-y { width:11px; height:11px; border-radius:50%; background:#FEBC2E; flex-shrink:0; }
  .jxd-g { width:11px; height:11px; border-radius:50%; background:#28C840; flex-shrink:0; }
  .jx-win-tab { padding:3px 10px; border-radius:6px; font-size:11px; font-family:monospace; color:#636366; cursor:pointer; transition:background .13s,color .13s; user-select:none; }
  .jx-win-tab.active { background:rgba(255,255,255,0.09); color:#F5F5F7; }
  .jx-code-pane { padding:20px; font-family:'SF Mono','Menlo','Consolas',monospace; overflow-x:auto; min-height:270px; }
  .hljs { background:transparent !important; font-size:.715rem; line-height:1.72; }

  .jx-play-tab, .jx-chart-tab { padding:7px 16px; border-radius:9px; font-size:12px; font-weight:500; cursor:pointer; color:#6E6E73; transition:background .13s,color .13s; }
  .jx-play-tab.active, .jx-chart-tab.active { background:#0071E3; color:#fff; }
  .dark .jx-play-tab, .dark .jx-chart-tab { color:#8E8E93; }

  .jx-feat-card { position:relative; overflow:hidden; }
  .jx-feat-card::before {
    content:''; position:absolute; inset:0; pointer-events:none;
    background:radial-gradient(circle at var(--mx,50%) var(--my,50%), rgba(0,113,227,.08) 0%, transparent 55%);
    opacity:0; transition:opacity .25s;
  }
  .jx-feat-card:hover::before { opacity:1; }

  .jx-bar { height:6px; border-radius:3px; transition:width 1.3s cubic-bezier(.16,1,.3,1); width:0; }

  .jx-pipe-node { display:flex; flex-direction:column; align-items:center; gap:5px; flex-shrink:0;
    opacity:0; transform:translateY(8px); transition:opacity .35s ease,transform .35s ease; }
  .jx-pipe-node.jx-in { opacity:1; transform:none; }
  .jx-pipe-line { flex:1; height:2px; border-radius:1px; opacity:0; min-width:8px;
    transition:opacity .25s ease; }
  .jx-pipe-node.jx-in + .jx-pipe-line { opacity:1; }

  .jx-counter { font-variant-numeric:tabular-nums; }
</style>

<%-- ── Hero split ─────────────────────────────────────────────────────── --%>
<section class="relative overflow-hidden bg-[#F5F5F7] dark:bg-[#000000] border-b border-black/[0.06] dark:border-white/[0.04]">
  <%-- Glow ambiental: SOLO en tema oscuro. En claro el hero queda limpio, sin difuminado. --%>
  <div class="absolute inset-0 pointer-events-none hidden dark:block" aria-hidden="true">
    <div class="absolute -top-48 left-[28%] w-[760px] h-[600px] rounded-full opacity-[0.16] blur-[100px]"
         style="background:radial-gradient(ellipse,#087CFA 0%,transparent 66%)"></div>
    <div class="absolute top-24 right-[4%] w-[440px] h-[440px] rounded-full opacity-[0.10] blur-[90px]"
         style="background:radial-gradient(ellipse,#5A63D6 0%,transparent 65%)"></div>
  </div>

  <div class="relative max-w-6xl mx-auto px-4 sm:px-6 py-14 sm:py-20">
    <div class="flex flex-col lg:flex-row items-center gap-10 lg:gap-14">

      <%-- Left: headline --%>
      <div class="lg:w-[44%] text-center lg:text-left jx-reveal jx-delay-1">
        <h1 class="text-[clamp(2.6rem,6.5vw,4.2rem)] font-extrabold tracking-tight leading-[1.05] mb-5">
          <span class="jx-grad">JxMVC</span><br>
          <span class="text-ink dark:text-white text-[clamp(1.4rem,3.5vw,2.2rem)] font-bold tracking-tight">Lightning-X Framework</span>
        </h1>

        <p class="text-[14.5px] text-muted dark:text-[#8E8E93] leading-relaxed mb-8 max-w-[400px] mx-auto lg:mx-0" data-i18n="hero.desc">
          Framework MVC para Jakarta EE — 224 KB, cero dependencias, arranque en 1.2 s.
          Pool, JSON, validación, WebSocket y OpenAPI incluidos.
        </p>

        <div class="flex flex-wrap justify-center lg:justify-start gap-2.5 mb-8">
          <% String[][] metrics = {{"224","KB","JAR"},{"0","deps","Runtime"},{"49","cls","Core"},{"1.2","s","Arranque"}}; %>
          <% for (String[] m : metrics) { %>
          <div class="bg-white dark:bg-[#1C1C1E] border border-black/[0.07] dark:border-white/[0.08] rounded-2xl px-4 py-3 shadow-card text-center min-w-[72px]">
            <p class="text-lg font-bold text-ink dark:text-white leading-none">
              <span class="jx-counter" data-target="<%= m[0].replace(".","") %>"><%= m[0] %></span><span class="text-apple text-[10px] font-semibold"> <%= m[1] %></span>
            </p>
            <p class="text-[9px] font-mono uppercase tracking-wider text-muted dark:text-[#8E8E93] mt-1"><%= m[2] %></p>
          </div>
          <% } %>
        </div>

        <div class="flex flex-wrap justify-center lg:justify-start gap-3">
          <a href="${pageContext.request.contextPath}/downloads"
             class="px-6 py-2.5 bg-apple text-white text-sm font-semibold rounded-full hover:bg-[#0077ED] active:scale-[0.97] transition-all shadow-sm"
             data-i18n="hero.dl">Descargar</a>
          <a href="${pageContext.request.contextPath}/docs"
             class="px-6 py-2.5 bg-black/[0.06] dark:bg-white/[0.09] text-ink dark:text-white border border-black/[0.09] dark:border-white/[0.09] text-sm font-semibold rounded-full hover:bg-black/[0.10] dark:hover:bg-white/[0.13] transition-colors"
             data-i18n="hero.docs">Documentación →</a>
        </div>
      </div>

      <%-- Right: code window --%>
      <div class="lg:w-[56%] w-full jx-reveal jx-delay-2">
        <div class="jx-win">
          <div class="jx-win-bar">
            <span class="jxd-r"></span><span class="jxd-y"></span><span class="jxd-g"></span>
            <div class="ml-3 flex gap-1">
              <span class="jx-win-tab active" onclick="jxCodeTab('ctrl',this)">Controller</span>
              <span class="jx-win-tab" onclick="jxCodeTab('model',this)">Model</span>
              <span class="jx-win-tab" onclick="jxCodeTab('cfg',this)">Config</span>
            </div>
            <button onclick="jxCopyPane('jxHeroPane')" class="ml-auto text-[#636366] hover:text-[#8E8E93] transition-colors" title="Copiar">
              <svg id="jxHeroCopyIco" class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                <rect x="9" y="9" width="13" height="13" rx="2"></rect><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"></path>
              </svg>
            </button>
          </div>
          <div id="jxHeroPane">
            <div id="jxH-ctrl" class="jx-code-pane">
<pre><code class="language-java">@JxControllerMapping("api/persona")
public class PersonaController extends JxController {

    @JxGetMapping("{id}")
    public ActionResult get(@JxPathVar String id) {
        DBRow per = new PersonaModel().GetRow(
                "tblPersonas", "id = ?", id);
        if (per == null)
            return Json(GenApi.Error(404, "No encontrado"));

        return Json(GenApi.JsonStr(
            "id",     per.Get("id"),
            "nombre", per.GetString("Nombres"),
            "correo", per.GetString("Correo")
        ));
    }

    @JxPostMapping("save")
    @JxRequireRole("admin")
    @JxRateLimit(requests = 20, windowMs = 60_000)
    public ActionResult save() {
        String nombre = model.param("nombre");
        // validar + guardar
        return Json(GenApi.Ok("Guardado")).status(201);
    }
}</code></pre>
            </div>
            <div id="jxH-model" class="jx-code-pane hidden">
<pre><code class="language-java">public class PersonaModel extends JxDB {

    public PersonaModel() { super(); }

    public DBRowSet buscarPorApellido(String apellido) {
        return GetTable(
            "tblPersonas",
            "Apellidos LIKE ?",
            apellido + "%"
        );
    }

    public DBRow porCorreo(String correo) {
        return GetRow(
            "tblPersonas",
            "Correo = ? AND activo = 1",
            correo
        );
    }
}</code></pre>
            </div>
            <div id="jxH-cfg" class="jx-code-pane hidden">
<pre><code class="language-properties"># application.properties
jxmvc.db.url     = jdbc:postgresql://localhost:5432/miapp
jxmvc.db.user    = usuario
jxmvc.db.pass    = secreto
jxmvc.profiles.active = dev

# Pool de conexiones
jxmvc.pool.min     = 2
jxmvc.pool.max     = 20
jxmvc.pool.timeout = 30000

# Nivel de log
jxmvc.log.level  = INFO</code></pre>
            </div>
          </div>
        </div>
        <div class="mt-3 flex items-center gap-3 bg-white/70 dark:bg-white/[0.05] backdrop-blur border border-black/[0.07] dark:border-white/[0.07] rounded-full px-4 py-2.5 shadow-card">
          <span class="text-apple font-mono text-sm font-bold shrink-0">$</span>
          <code id="jxHeroCmd" class="font-mono text-[13px] text-ink dark:text-[#C8C8D0] flex-1">mvn install -f JxMVC.Core/pom.xml</code>
          <button onclick="jxCopyCmd()" class="text-muted dark:text-[#8E8E93] hover:text-apple transition-colors shrink-0">
            <svg id="jxCmdIco" class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
              <rect x="9" y="9" width="13" height="13" rx="2"></rect><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"></path>
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</section>

<%-- ── Features bento ─────────────────────────────────────────────────── --%>
<section class="py-20 bg-white dark:bg-[#000000]">
  <div class="max-w-6xl mx-auto px-4 sm:px-6">

    <div class="mb-12 jx-reveal jx-delay-1">
      <p class="text-[10px] font-mono uppercase tracking-[0.28em] text-muted dark:text-[#8E8E93] mb-3">Características</p>
      <h2 class="text-3xl sm:text-4xl font-bold text-ink dark:text-white leading-tight">Todo incluido.<br><span class="text-apple">Cero dependencias.</span></h2>
      <p class="text-[14px] text-muted dark:text-[#8E8E93] mt-3 max-w-xl">Un único JAR de 224 KB con todo lo que necesita una aplicación Jakarta EE real. Sin Spring, sin Hibernate, sin Jackson.</p>
    </div>

    <%-- Bento grid --%>
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 jx-reveal jx-delay-2">

      <%-- Grande: Routing --%>
      <div class="jx-feat-card col-span-2 bg-[#F5F5F7] dark:bg-[#1C1C1E] border border-black/[0.06] dark:border-white/[0.07] rounded-2xl p-6 shadow-card hover:shadow-cardh transition-all" onmousemove="jxSpot(event,this)">
        <div class="w-10 h-10 rounded-xl flex items-center justify-center mb-4" style="background:#0071E31A">
          <svg class="w-5 h-5" fill="none" stroke="#0071E3" stroke-width="1.8" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/></svg>
        </div>
        <h3 class="text-[15px] font-semibold text-ink dark:text-white mb-2">Routing inteligente</h3>
        <p class="text-[12px] text-muted dark:text-[#8E8E93] leading-relaxed mb-4">Convención de nombres + anotaciones. Plantillas <code class="font-mono text-apple">{id}</code>, args posicionales, múltiples verbos HTTP.</p>
        <div class="flex flex-wrap gap-1.5">
          <% for (String t : new String[]{"@JxGetMapping","@JxPostMapping","@JxPutMapping","@JxDeleteMapping","{pathVar}"}) { %>
          <span class="text-[10px] font-mono px-2 py-0.5 rounded-md bg-apple/10 text-apple"><%= t %></span>
          <% } %>
        </div>
      </div>

      <%-- Pool --%>
      <div class="jx-feat-card bg-[#F5F5F7] dark:bg-[#1C1C1E] border border-black/[0.06] dark:border-white/[0.07] rounded-2xl p-5 shadow-card hover:shadow-cardh transition-all" onmousemove="jxSpot(event,this)">
        <div class="w-9 h-9 rounded-xl flex items-center justify-center mb-4" style="background:#0071E31A">
          <svg class="w-[18px] h-[18px]" fill="none" stroke="#0071E3" stroke-width="1.8" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4"/></svg>
        </div>
        <h3 class="text-[13px] font-semibold text-ink dark:text-white mb-1.5">Pool de conexiones</h3>
        <p class="text-[11.5px] text-muted dark:text-[#8E8E93] leading-relaxed">JxPool propio — keepalive, timeout, prewarm. PostgreSQL, MySQL, SQL Server.</p>
      </div>

      <%-- Virtual Threads --%>
      <div class="jx-feat-card bg-[#F5F5F7] dark:bg-[#1C1C1E] border border-black/[0.06] dark:border-white/[0.07] rounded-2xl p-5 shadow-card hover:shadow-cardh transition-all" onmousemove="jxSpot(event,this)">
        <div class="w-9 h-9 rounded-xl flex items-center justify-center mb-4" style="background:#5E5CE61A">
          <svg class="w-[18px] h-[18px]" fill="none" stroke="#5E5CE6" stroke-width="1.8" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>
        </div>
        <h3 class="text-[13px] font-semibold text-ink dark:text-white mb-1.5">Virtual Threads</h3>
        <p class="text-[11.5px] text-muted dark:text-[#8E8E93] leading-relaxed">Detectados automáticamente en Java 21+. Sin config. Máxima concurrencia sin bloqueos.</p>
      </div>

      <%-- Validación --%>
      <div class="jx-feat-card bg-[#F5F5F7] dark:bg-[#1C1C1E] border border-black/[0.06] dark:border-white/[0.07] rounded-2xl p-5 shadow-card hover:shadow-cardh transition-all" onmousemove="jxSpot(event,this)">
        <div class="w-9 h-9 rounded-xl flex items-center justify-center mb-4" style="background:#FF950018">
          <svg class="w-[18px] h-[18px]" fill="none" stroke="#FF9500" stroke-width="1.8" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
        </div>
        <h3 class="text-[13px] font-semibold text-ink dark:text-white mb-1.5">Validación declarativa</h3>
        <p class="text-[11.5px] text-muted dark:text-[#8E8E93] leading-relaxed">@JxRequired, @JxEmail, @JxPattern, @JxLength, @JxRange, @JxUrl, @JxFuture.</p>
      </div>

      <%-- Auth + Seguridad --%>
      <div class="jx-feat-card bg-[#F5F5F7] dark:bg-[#1C1C1E] border border-black/[0.06] dark:border-white/[0.07] rounded-2xl p-5 shadow-card hover:shadow-cardh transition-all" onmousemove="jxSpot(event,this)">
        <div class="w-9 h-9 rounded-xl flex items-center justify-center mb-4" style="background:#FF2D5518">
          <svg class="w-[18px] h-[18px]" fill="none" stroke="#FF2D55" stroke-width="1.8" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/></svg>
        </div>
        <h3 class="text-[13px] font-semibold text-ink dark:text-white mb-1.5">Auth + Rate Limit</h3>
        <p class="text-[11.5px] text-muted dark:text-[#8E8E93] leading-relaxed">@JxRequireAuth, @JxRequireRole, @JxRateLimit por IP+ruta. Ventana deslizante.</p>
      </div>

      <%-- Cache --%>
      <div class="jx-feat-card bg-[#F5F5F7] dark:bg-[#1C1C1E] border border-black/[0.06] dark:border-white/[0.07] rounded-2xl p-5 shadow-card hover:shadow-cardh transition-all" onmousemove="jxSpot(event,this)">
        <div class="w-9 h-9 rounded-xl flex items-center justify-center mb-4" style="background:#FF950018">
          <svg class="w-[18px] h-[18px]" fill="none" stroke="#FF9500" stroke-width="1.8" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01"/></svg>
        </div>
        <h3 class="text-[13px] font-semibold text-ink dark:text-white mb-1.5">Caché en memoria</h3>
        <p class="text-[11.5px] text-muted dark:text-[#8E8E93] leading-relaxed">@JxCacheable y @JxCacheEvict con TTL por namespace. Sin Redis, sin Caffeine.</p>
      </div>

      <%-- Grande: Zero deps --%>
      <div class="jx-feat-card col-span-2 bg-gradient-to-br from-[#0071E3]/[0.07] to-transparent dark:from-[#0071E3]/[0.12] dark:to-transparent border border-apple/20 rounded-2xl p-6 shadow-card hover:shadow-cardh transition-all" onmousemove="jxSpot(event,this)">
        <div class="w-10 h-10 rounded-xl flex items-center justify-center mb-4" style="background:#FF2D5520">
          <svg class="w-5 h-5" fill="none" stroke="#FF2D55" stroke-width="1.8" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01"/></svg>
        </div>
        <h3 class="text-[15px] font-semibold text-ink dark:text-white mb-2">224 KB. Cero dependencias en runtime.</h3>
        <p class="text-[12px] text-muted dark:text-[#8E8E93] leading-relaxed mb-4">21 anotaciones propias, scheduler cron, pool JDBC, JSON nativo, WebSocket, métricas y OpenAPI — todo en <strong class="text-ink dark:text-white">java.*</strong> puro.</p>
        <div class="flex gap-4 text-[11px]">
          <span class="flex items-center gap-1.5 text-muted dark:text-[#8E8E93]"><span class="w-1.5 h-1.5 rounded-full bg-[#30D158]"></span>Sin Jackson</span>
          <span class="flex items-center gap-1.5 text-muted dark:text-[#8E8E93]"><span class="w-1.5 h-1.5 rounded-full bg-[#30D158]"></span>Sin Hibernate</span>
          <span class="flex items-center gap-1.5 text-muted dark:text-[#8E8E93]"><span class="w-1.5 h-1.5 rounded-full bg-[#30D158]"></span>Sin Spring</span>
          <span class="flex items-center gap-1.5 text-muted dark:text-[#8E8E93]"><span class="w-1.5 h-1.5 rounded-full bg-[#30D158]"></span>Sin Guice</span>
        </div>
      </div>

      <%-- GenApi + JxDB --%>
      <div class="jx-feat-card bg-[#F5F5F7] dark:bg-[#1C1C1E] border border-black/[0.06] dark:border-white/[0.07] rounded-2xl p-5 shadow-card hover:shadow-cardh transition-all" onmousemove="jxSpot(event,this)">
        <div class="w-9 h-9 rounded-xl flex items-center justify-center mb-4" style="background:#FF950018">
          <svg class="w-[18px] h-[18px]" fill="none" stroke="#FF9500" stroke-width="1.8" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"/></svg>
        </div>
        <h3 class="text-[13px] font-semibold text-ink dark:text-white mb-1.5">GenApi + JxDB</h3>
        <p class="text-[11.5px] text-muted dark:text-[#8E8E93] leading-relaxed">DBRow sin POJOs. JsonStr, JsonArray, JsonPaged, nested. Respuestas JSON en 1 línea.</p>
      </div>

      <%-- Métricas + OpenAPI --%>
      <div class="jx-feat-card bg-[#F5F5F7] dark:bg-[#1C1C1E] border border-black/[0.06] dark:border-white/[0.07] rounded-2xl p-5 shadow-card hover:shadow-cardh transition-all" onmousemove="jxSpot(event,this)">
        <div class="w-9 h-9 rounded-xl flex items-center justify-center mb-4" style="background:#30D15818">
          <svg class="w-[18px] h-[18px]" fill="none" stroke="#30D158" stroke-width="1.8" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/></svg>
        </div>
        <h3 class="text-[13px] font-semibold text-ink dark:text-white mb-1.5">Métricas + OpenAPI</h3>
        <p class="text-[11.5px] text-muted dark:text-[#8E8E93] leading-relaxed">/jx/health, /jx/metrics, /jx/openapi listos sin configuración. Latencia por ruta.</p>
      </div>

    </div>
  </div>
</section>

<%-- ── Code playground ────────────────────────────────────────────────── --%>
<section class="py-20 bg-[#F5F5F7] dark:bg-[#0A0A0A] border-y border-black/[0.06] dark:border-white/[0.04]">
  <div class="max-w-6xl mx-auto px-4 sm:px-6">
    <div class="mb-10 jx-reveal jx-delay-1">
      <p class="text-[10px] font-mono uppercase tracking-[0.28em] text-muted dark:text-[#8E8E93] mb-3">Casos de uso</p>
      <h2 class="text-3xl sm:text-4xl font-bold text-ink dark:text-white leading-tight">Todo lo que necesitas<br><span class="text-apple">desde el día uno.</span></h2>
    </div>
    <div class="jx-reveal jx-delay-2">
      <div class="flex flex-wrap gap-2 mb-4">
        <button class="jx-play-tab active" onclick="jxPlay('rest',this)">REST API</button>
        <button class="jx-play-tab" onclick="jxPlay('ws',this)">WebSocket</button>
        <button class="jx-play-tab" onclick="jxPlay('sched',this)">Scheduler</button>
        <button class="jx-play-tab" onclick="jxPlay('val',this)">Validación</button>
        <button class="jx-play-tab" onclick="jxPlay('cache',this)">Caché</button>
        <button class="jx-play-tab" onclick="jxPlay('auth',this)">Auth</button>
      </div>
      <div class="jx-win">
        <div class="jx-win-bar">
          <span class="jxd-r"></span><span class="jxd-y"></span><span class="jxd-g"></span>
          <span id="jxPlayLbl" class="ml-4 text-[11px] font-mono text-[#636366]">ApiController.java</span>
          <button onclick="jxCopyPane('jxPlayPane')" class="ml-auto text-[#636366] hover:text-[#8E8E93] transition-colors">
            <svg id="jxPlayIco" class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
              <rect x="9" y="9" width="13" height="13" rx="2"></rect><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"></path>
            </svg>
          </button>
        </div>
        <div id="jxPlayPane">
          <div id="jxP-rest" class="jx-code-pane">
<pre><code class="language-java">@JxControllerMapping("api/users")
public class UserController extends JxController {

    @JxGetMapping("{id}")
    @JxRequireAuth
    public ActionResult get(@JxPathVar String id) {
        DBRow user = new UserModel().GetRow("users", "id=?", id);
        if (user == null) return Json(GenApi.Error(404, "No encontrado"));
        return Json(GenApi.JsonStr(
            "id",    id,
            "email", user.GetString("email"),
            "role",  user.GetString("role")
        ));
    }

    @JxPostMapping("")
    @JxRateLimit(requests = 10, windowMs = 60_000)
    public ActionResult create() {
        String email = model.param("email");
        String pass  = model.param("password");
        new UserModel().insert(email, pass);
        return Json(GenApi.Ok("Creado")).status(201);
    }

    @JxDeleteMapping("{id}")
    @JxRequireRole("admin")
    public ActionResult delete(@JxPathVar String id) {
        new UserModel().delete("id=?", id);
        return Json(GenApi.Ok("Eliminado"));
    }
}</code></pre>
          </div>
          <div id="jxP-ws" class="jx-code-pane hidden">
<pre><code class="language-java">@JxWsEndpoint("/ws/chat")
public class ChatEndpoint extends JxWebSocket {

    @Override
    public void onConnect(Session session) {
        broadcast("sistema",
            session.getId() + " entró al chat");
    }

    @Override
    public void onMessage(Session session, String msg) {
        String user = (String)
            session.getUserProperties().get("user");
        broadcast("msg",
            "{\"user\":\"" + user + "\",\"text\":\"" + msg + "\"}");
    }

    @Override
    public void onClose(Session session) {
        broadcast("sistema",
            session.getId() + " salió");
    }

    @Override
    public void onError(Session session, Throwable err) {
        log.warn("WS error: {}", err.getMessage());
    }
}</code></pre>
          </div>
          <div id="jxP-sched" class="jx-code-pane hidden">
<pre><code class="language-java">@JxComponent
public class TareasProgramadas {

    // Cada día a las 2:00 AM
    @JxScheduled(cron = "0 0 2 * * *")
    public void limpiarSesiones() {
        int n = new SesionModel().deleteExpired();
        log.info("Sesiones expiradas eliminadas: {}", n);
    }

    // Cada 5 minutos
    @JxScheduled(fixedRate = 300_000)
    public void sincronizarCache() {
        JxCache.get("catalogo").evictAll();
    }

    // Una sola vez, 10 s después del arranque
    @JxScheduled(runOnce = true, initialDelay = 10_000)
    public void precalentarPool() {
        new ProductoModel().GetRow("productos", "1=1");
        log.info("Pool precalentado");
    }
}</code></pre>
          </div>
          <div id="jxP-val" class="jx-code-pane hidden">
<pre><code class="language-java">public class RegistroForm {

    @JxRequired @JxEmail
    private String email;

    @JxRequired
    @JxLength(min = 8, max = 64)
    @JxPattern(regex = "(?=.*[A-Z])(?=.*\\d).+",
               msg = "Requiere mayúscula y número")
    private String password;

    @JxRequired @JxLength(min = 2, max = 80)
    private String nombre;

    @JxRange(min = 18, max = 120)
    private int edad;

    @JxFuture
    private LocalDate vencimiento;

    @JxUrl
    private String sitioWeb;
}

@JxPostMapping("registro")
public ActionResult registrar(@JxValid RegistroForm form) {
    if (!model.isValid()) return Json(model.errors());
    return Json(GenApi.Ok("Registrado")).status(201);
}</code></pre>
          </div>

          <div id="jxP-cache" class="jx-code-pane hidden">
<pre><code class="language-java">@JxControllerMapping("api/catalogo")
public class CatalogoController extends JxController {

    @JxGetMapping("productos")
    @JxCacheable(value = "catalogo", ttl = 300_000)
    public ActionResult productos() {
        DBRowSet rows = new ProductoModel()
            .GetTable("productos", "activo = 1");
        return Json(GenApi.JsonArray(rows, "productos"));
    }

    @JxPostMapping("producto/save")
    @JxCacheEvict(value = "catalogo")
    public ActionResult guardar() {
        String nombre = model.param("nombre");
        new ProductoModel().insert(nombre);
        return Json(GenApi.Ok("Guardado")).status(201);
    }
}

// Sin Redis. Sin Caffeine.
// JxCache usa ConcurrentHashMap + TTL nativo.</code></pre>
          </div>

          <div id="jxP-auth" class="jx-code-pane hidden">
<pre><code class="language-java">@JxControllerMapping("api/admin")
@JxRequireAuth
public class AdminController extends JxController {

    @JxGetMapping("usuarios")
    @JxRequireRole("admin")
    public ActionResult listar() {
        DBRowSet rows = new UsuarioModel()
            .GetTable("usuarios", "activo = 1");
        return Json(GenApi.JsonArray(rows, "usuarios"));
    }

    @JxDeleteMapping("usuario/{id}")
    @JxRequireRole("superadmin")
    @JxRateLimit(requests = 5, windowMs = 60_000)
    public ActionResult eliminar(@JxPathVar String id) {
        new UsuarioModel().softDelete("id = ?", id);
        return Json(GenApi.Ok("Eliminado"));
    }
}

// Sesión: HttpSession estándar del servlet container.
// Sin JWT externo, sin OAuth lib. Cero dependencias.</code></pre>
          </div>

        </div>
      </div>
    </div>
  </div>
</section>

<%-- ── Pipeline visual ────────────────────────────────────────────────── --%>
<style>
  .jx-pipe-step { cursor:pointer; }
  .jx-pipe-step:hover .jx-ps-box { transform:translateY(-3px); box-shadow:0 8px 24px rgba(0,0,0,0.12); }
  .dark .jx-pipe-step:hover .jx-ps-box { box-shadow:0 8px 24px rgba(0,0,0,0.4); }
  .jx-ps-box { transition:transform .2s ease, box-shadow .2s ease; }
  .jx-pipe-detail { max-height:0; overflow:hidden; transition:max-height .35s ease; }
  .jx-pipe-detail.open { max-height:120px; }
</style>
<section class="py-20 bg-white dark:bg-[#000000]" id="jxPipeSec">
  <div class="max-w-6xl mx-auto px-4 sm:px-6">

    <div class="mb-12">
      <p class="text-[10px] font-mono uppercase tracking-[0.28em] text-muted dark:text-[#8E8E93] mb-3">Arquitectura</p>
      <h2 class="text-3xl sm:text-4xl font-bold text-ink dark:text-white leading-tight">14 etapas por cada request</h2>
      <p class="text-[14px] text-muted dark:text-[#8E8E93] mt-3 max-w-xl">Cada petición HTTP pasa por un pipeline controlado — sin magia, predecible y trazable. Haz clic en cualquier etapa para ver el detalle.</p>
    </div>

    <%
    String[][] pipe = {
      {"01","Endpoints",   "#0071E3","Rutas internas /jx/health, /jx/info, /jx/metrics, /jx/openapi. Resueltas antes de cualquier lógica de negocio."},
      {"02","Métricas",    "#30D158","Timer de latencia iniciado por ruta. Registra total de peticiones, errores 4xx/5xx y P95."},
      {"03","Rate limit",  "#FF2D55","@JxRateLimit — ventana deslizante por IP + ruta. Retorna 429 con Retry-After si se excede."},
      {"04","Routing",     "#0071E3","Convención / anotaciones / plantillas {var}. Selecciona controller + acción. 405 si verbo no coincide."},
      {"05","Perfil",      "#FF9500","@JxProfile — activa o desactiva el endpoint según entorno (dev/prod/test). 404 si no aplica."},
      {"06","Auth",        "#FF2D55","@JxRequireAuth y @JxRequireRole. Valida sesión y rol antes de instanciar el controller."},
      {"07","CORS",        "#5E5CE6","@JxCors global o por controller. Inyecta Access-Control-* y gestiona preflight OPTIONS."},
      {"08","Filtros",     "#FF9500","@JxFilter con fase BEFORE. Permite interceptar, modificar request o abortar con respuesta propia."},
      {"09","DI + Ctrl",   "#0071E3","Controller instanciado. @JxInject y @JxValue resueltos del registro de servicios."},
      {"10","Before",      "#5E5CE6","@JxBeforeAction — interceptores declarados en el controller, ejecutados antes de la acción."},
      {"11","ModelAttr",   "#FF9500","@JxModelAttr — atributos comunes inyectados al modelo antes de llamar la acción."},
      {"12","Invocación",  "#30D158","Acción ejecutada. @JxAsync (background thread), @JxRetry (reintentos con backoff), @JxCacheable."},
      {"13","After",       "#5E5CE6","@JxAfterAction + filtros AFTER. Post-procesamiento, auditoría, transformación de respuesta."},
      {"14","Render",      "#0071E3","Negociación de contenido: JSP, JSON, raw, redirect. Métricas finales registradas."},
    };
    %>

    <%-- Desktop: 2 filas horizontales con nodos --%>
    <div class="hidden md:block space-y-8">
      <div class="flex items-center gap-0">
        <% for (int i=0;i<7;i++) { String[] p=pipe[i]; %>
        <div class="jx-pipe-node jx-pipe-step flex-1 min-w-0 flex flex-col items-center gap-2" style="transition-delay:<%= i*60 %>ms" onclick="jxPipeToggle(<%= i %>)">
          <div class="jx-ps-box w-11 h-11 rounded-xl flex items-center justify-center text-[10px] font-mono font-bold text-white shrink-0 border-2 border-white/20" style="background:<%= p[2] %>"><%= p[0] %></div>
          <span class="text-[9px] font-mono text-muted dark:text-[#8E8E93] text-center leading-tight px-1"><%= p[1] %></span>
        </div>
        <% if(i<6) { %><div class="jx-pipe-line" style="background:linear-gradient(90deg,<%= pipe[i][2] %>,<%= pipe[i+1][2] %>)"></div><% } %>
        <% } %>
      </div>
      <div class="flex items-center gap-0">
        <% for (int i=7;i<14;i++) { String[] p=pipe[i]; %>
        <div class="jx-pipe-node jx-pipe-step flex-1 min-w-0 flex flex-col items-center gap-2" style="transition-delay:<%= i*60 %>ms" onclick="jxPipeToggle(<%= i %>)">
          <div class="jx-ps-box w-11 h-11 rounded-xl flex items-center justify-center text-[10px] font-mono font-bold text-white shrink-0 border-2 border-white/20" style="background:<%= p[2] %>"><%= p[0] %></div>
          <span class="text-[9px] font-mono text-muted dark:text-[#8E8E93] text-center leading-tight px-1"><%= p[1] %></span>
        </div>
        <% if(i<13) { %><div class="jx-pipe-line" style="background:linear-gradient(90deg,<%= pipe[i][2] %>,<%= pipe[i+1][2] %>)"></div><% } %>
        <% } %>
      </div>
    </div>

    <%-- Detail panel --%>
    <div id="jxPipePanel" class="hidden md:block mt-6 bg-[#F5F5F7] dark:bg-[#1C1C1E] border border-black/[0.06] dark:border-white/[0.07] rounded-2xl px-6 py-4 shadow-card jx-pipe-detail">
      <div class="flex items-center gap-3 mb-1">
        <span id="jxPipeBadge" class="text-[10px] font-mono font-bold text-white px-2 py-0.5 rounded-md"></span>
        <span id="jxPipeTitle" class="text-[13px] font-semibold text-ink dark:text-white"></span>
      </div>
      <p id="jxPipeDesc" class="text-[12px] text-muted dark:text-[#8E8E93] leading-relaxed"></p>
    </div>

    <%-- Mobile: lista compacta --%>
    <div class="md:hidden space-y-2">
      <% for (String[] p : pipe) { %>
      <div class="jx-pipe-node flex items-center gap-3 rounded-xl bg-[#F5F5F7] dark:bg-[#1C1C1E] border border-black/[0.06] dark:border-white/[0.07] px-4 py-3">
        <span class="w-7 h-7 rounded-lg flex items-center justify-center text-[9px] font-mono font-bold text-white shrink-0" style="background:<%= p[2] %>"><%= p[0] %></span>
        <div class="min-w-0">
          <span class="text-[12px] font-medium text-ink dark:text-white"><%= p[1] %></span>
          <p class="text-[10.5px] text-muted dark:text-[#8E8E93] leading-tight mt-0.5 truncate"><%= p[3] %></p>
        </div>
      </div>
      <% } %>
    </div>

  </div>
</section>

<script>
var jxPipeData = [
  <% for (int i=0;i<pipe.length;i++) { %>{num:"<%= pipe[i][0] %>",name:"<%= pipe[i][1] %>",color:"<%= pipe[i][2] %>",desc:"<%= pipe[i][3].replace("\"","&quot;") %>"}<%= i<pipe.length-1?",":"" %>
  <% } %>
];
var jxPipeOpen = -1;
function jxPipeToggle(idx) {
  var panel = document.getElementById('jxPipePanel');
  if (jxPipeOpen === idx) {
    panel.classList.remove('open');
    jxPipeOpen = -1;
    return;
  }
  var d = jxPipeData[idx];
  document.getElementById('jxPipeBadge').textContent = d.num;
  document.getElementById('jxPipeBadge').style.background = d.color;
  document.getElementById('jxPipeTitle').textContent = d.name;
  document.getElementById('jxPipeDesc').textContent = d.desc;
  panel.classList.add('open');
  jxPipeOpen = idx;
}
</script>

<%-- ── Comparativa columnas verticales ─────────────────────────────────── --%>
<section class="py-20 bg-[#F5F5F7] dark:bg-[#0A0A0A] border-y border-black/[0.06] dark:border-white/[0.04]" id="jxChartSec">
  <div class="max-w-5xl mx-auto px-4 sm:px-6">

    <div class="mb-4">
      <p class="text-[10px] font-mono uppercase tracking-[0.28em] text-muted dark:text-[#8E8E93] mb-3">Comparativa real · 5 frameworks</p>
      <h2 class="text-3xl sm:text-4xl font-bold text-ink dark:text-white leading-tight">El más ligero.<br><span class="text-apple">El único con cero dependencias.</span></h2>
    </div>

    <div class="flex flex-wrap gap-2.5 mt-5 mb-10">
      <span class="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-apple/10 border border-apple/20 text-apple text-[11px] font-semibold">89× más pequeño que Spring Boot</span>
      <span class="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-[#FF2D55]/10 border border-[#FF2D55]/20 text-[#FF2D55] text-[11px] font-semibold">0 deps en runtime</span>
      <span class="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-[#30D158]/10 border border-[#30D158]/20 text-[#30D158] text-[11px] font-semibold">5× más rápido que Spring Boot</span>
    </div>

    <div class="flex gap-2 mb-8">
      <button class="jx-chart-tab active" onclick="jxChart('jar',this)">Tamaño JAR</button>
      <button class="jx-chart-tab" onclick="jxChart('dep',this)">Dependencias</button>
      <button class="jx-chart-tab" onclick="jxChart('start',this)">Arranque JVM</button>
    </div>

    <div class="bg-white dark:bg-[#1C1C1E] border border-black/[0.06] dark:border-white/[0.07] rounded-2xl p-8 shadow-card">
      <p class="text-[10px] font-mono uppercase tracking-widest text-muted dark:text-[#636366] mb-6" id="jxChartUnit"></p>

      <%-- Canvas del gráfico --%>
      <canvas id="jxCanvas" height="240" style="width:100%;display:block;"></canvas>

      <p class="text-[10px] text-muted dark:text-[#636366] mt-5 leading-relaxed" id="jxChartNote"></p>
    </div>
  </div>
</section>

<script>
var jxChartData = {
  jar: {
    unit: 'Tamaño del artefacto de producción — menor es mejor',
    note: 'JAR/WAR sin dependencias transitivas del servidor. JxMVC: 224 KB. Spring Boot uber-JAR incluye Tomcat embebido.',
    rows: [
      {name:'JxMVC 3.1',   val:'224 KB', raw:0.224, jx:true},
      {name:'Javalin 6',   val:'1.5 MB', raw:1.5,   jx:false},
      {name:'Quarkus 3',   val:'18 MB',  raw:18,    jx:false},
      {name:'Spring Boot', val:'20 MB',  raw:20,    jx:false},
      {name:'Micronaut 4', val:'25 MB',  raw:25,    jx:false},
    ]
  },
  dep: {
    unit: 'Dependencias en runtime — menor es mejor',
    note: 'JxMVC solo necesita el servlet container. Sin JAR externo en classpath de producción.',
    rows: [
      {name:'JxMVC 3.1',   val:'0',    raw:0,   jx:true},
      {name:'Javalin 6',   val:'~10',  raw:10,  jx:false},
      {name:'Quarkus 3',   val:'~80',  raw:80,  jx:false},
      {name:'Spring Boot', val:'~210', raw:210, jx:false},
      {name:'Micronaut 4', val:'~150', raw:150, jx:false},
    ]
  },
  start: {
    unit: 'Tiempo de arranque en JVM — segundos — menor es mejor',
    note: 'JVM estándar sin AOT. Quarkus y Micronaut requieren compilación nativa para sus cifras AOT más bajas.',
    rows: [
      {name:'Quarkus 3',   val:'0.3 s', raw:0.3, jx:false},
      {name:'Micronaut 4', val:'0.4 s', raw:0.4, jx:false},
      {name:'Javalin 6',   val:'0.5 s', raw:0.5, jx:false},
      {name:'JxMVC 3.1',   val:'1.2 s', raw:1.2, jx:true},
      {name:'Spring Boot', val:'6.0 s', raw:6.0, jx:false},
    ]
  }
};

var jxAnimFrame = null;

function jxChart(key, btn) {
  document.querySelectorAll('.jx-chart-tab').forEach(function(b){ b.classList.remove('active'); });
  btn.classList.add('active');
  jxRenderChart(key);
}

function jxRenderChart(key) {
  var data = jxChartData[key];
  document.getElementById('jxChartUnit').textContent = data.unit;
  document.getElementById('jxChartNote').textContent = data.note;

  var canvas = document.getElementById('jxCanvas');
  var dpr    = window.devicePixelRatio || 1;
  var W      = canvas.parentElement.clientWidth - 64;
  var H      = 240;
  canvas.width  = W * dpr;
  canvas.height = H * dpr;
  canvas.style.width  = W + 'px';
  canvas.style.height = H + 'px';
  var ctx = canvas.getContext('2d');
  ctx.scale(dpr, dpr);

  var isDark = document.documentElement.classList.contains('dark');
  var n      = data.rows.length;
  var PAD_L  = 8;
  var PAD_R  = 8;
  var PAD_B  = 48;
  var PAD_T  = 28;
  var chartH = H - PAD_B - PAD_T;
  var colW   = (W - PAD_L - PAD_R) / n;
  var barW   = Math.min(colW * 0.55, 72);

  var maxRaw = 0;
  data.rows.forEach(function(r){ if (r.raw > maxRaw) maxRaw = r.raw; });
  if (maxRaw === 0) maxRaw = 1;

  if (jxAnimFrame) cancelAnimationFrame(jxAnimFrame);

  var startTs = null;
  var DURATION = 1100;

  function ease(t) { return 1 - Math.pow(1 - t, 3); }

  function draw(ts) {
    if (!startTs) startTs = ts;
    var progress = Math.min((ts - startTs) / DURATION, 1);
    var ep = ease(progress);

    ctx.clearRect(0, 0, W, H);

    var gridColor = isDark ? 'rgba(255,255,255,0.07)' : 'rgba(0,0,0,0.07)';
    for (var g = 0; g <= 4; g++) {
      var gy = PAD_T + (chartH / 4) * g;
      ctx.beginPath();
      ctx.strokeStyle = gridColor;
      ctx.lineWidth = 1;
      ctx.setLineDash([4, 4]);
      ctx.moveTo(PAD_L, gy);
      ctx.lineTo(W - PAD_R, gy);
      ctx.stroke();
    }
    ctx.setLineDash([]);

    data.rows.forEach(function(row, i) {
      var cx   = PAD_L + colW * i + colW / 2;
      var x    = cx - barW / 2;
      var pct  = row.raw / maxRaw;
      var fullH = Math.max(pct * chartH, 2);
      var barH = fullH * ep;
      var y    = PAD_T + chartH - barH;

      var color = row.jx ? '#0071E3' : (isDark ? '#3A3A3C' : '#C7C7CC');

      if (row.jx) {
        var grd = ctx.createLinearGradient(0, y, 0, y + barH);
        grd.addColorStop(0, '#34AADC');
        grd.addColorStop(1, '#0071E3');
        ctx.fillStyle = grd;
        ctx.shadowColor = 'rgba(0,113,227,0.45)';
        ctx.shadowBlur  = 18;
      } else {
        ctx.fillStyle = color;
        ctx.shadowBlur = 0;
      }

      var r = Math.min(6, barW / 2);
      ctx.beginPath();
      ctx.moveTo(x + r, y);
      ctx.lineTo(x + barW - r, y);
      ctx.quadraticCurveTo(x + barW, y, x + barW, y + r);
      ctx.lineTo(x + barW, y + barH);
      ctx.lineTo(x, y + barH);
      ctx.lineTo(x, y + r);
      ctx.quadraticCurveTo(x, y, x + r, y);
      ctx.closePath();
      ctx.fill();
      ctx.shadowBlur = 0;

      if (progress > 0.6) {
        var valAlpha = Math.min((progress - 0.6) / 0.4, 1);
        ctx.globalAlpha = valAlpha;
        ctx.fillStyle = row.jx ? '#0071E3' : (isDark ? '#8E8E93' : '#6E6E73');
        ctx.font = 'bold 11px "Space Grotesk", monospace';
        ctx.textAlign = 'center';
        ctx.fillText(row.val, cx, y - 7);
        ctx.globalAlpha = 1;
      }

      ctx.fillStyle = row.jx ? '#0071E3' : (isDark ? '#EBEBF599' : '#1D1D1F99');
      ctx.font = (row.jx ? 'bold' : 'normal') + ' 11px "Space Grotesk", sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(row.name, cx, H - 10);
    });

    if (progress < 1) jxAnimFrame = requestAnimationFrame(draw);
  }

  jxAnimFrame = requestAnimationFrame(draw);
}

document.addEventListener('DOMContentLoaded', function(){
  var fired = false;
  var io2 = new IntersectionObserver(function(entries){
    if (entries[0].isIntersecting && !fired) {
      fired = true;
      jxRenderChart('jar');
    }
  }, {threshold: 0.2});
  var sec = document.getElementById('jxChartSec');
  if (sec) io2.observe(sec);
});

window.addEventListener('resize', function(){
  var active = document.querySelector('.jx-chart-tab.active');
  if (active && document.getElementById('jxChartSec')) {
    var key = active.getAttribute('onclick').match(/'(\w+)'/)[1];
    jxRenderChart(key);
  }
});
</script>

<%-- ── Endpoints ───────────────────────────────────────────────────────── --%>
<section class="py-20 bg-white dark:bg-[#000000]">
  <div class="max-w-6xl mx-auto px-4 sm:px-6">

    <div class="mb-12 jx-reveal jx-delay-1">
      <p class="text-[10px] font-mono uppercase tracking-[0.28em] text-muted dark:text-[#8E8E93] mb-3">Endpoints del sistema</p>
      <h2 class="text-3xl sm:text-4xl font-bold text-ink dark:text-white leading-tight">Observabilidad<br><span class="text-apple">incluida.</span></h2>
      <p class="text-[14px] text-muted dark:text-[#8E8E93] mt-3 max-w-xl">Cuatro endpoints listos desde el arranque — sin configuración, sin dependencias externas.</p>
    </div>

    <div class="grid lg:grid-cols-2 gap-4 jx-reveal jx-delay-2">

      <%-- Lista de endpoints --%>
      <div class="bg-[#F5F5F7] dark:bg-[#1C1C1E] border border-black/[0.06] dark:border-white/[0.07] rounded-2xl overflow-hidden divide-y divide-black/[0.05] dark:divide-white/[0.05] shadow-card">
        <%
        String[][] eps = {
          {"/jx/health",  "Estado del pool, uptime y threads activos","#30D158","health"},
          {"/jx/info",    "Versión, perfil activo, Java y servidor","#0071E3","info"},
          {"/jx/metrics", "Latencia, peticiones totales y errores por ruta","#FF9500","metrics"},
          {"/jx/openapi", "Especificación OpenAPI 3.0 generada automáticamente","#5E5CE6","openapi"},
        };
        for (String[] ep : eps) {
        %>
        <button onclick="jxEpShow('<%= ep[3] %>', this)"
                class="jx-ep-row w-full flex items-center justify-between px-5 py-4 hover:bg-white/70 dark:hover:bg-white/[0.04] transition-colors text-left">
          <div class="flex items-center gap-4">
            <span class="w-2 h-2 rounded-full shrink-0" style="background:<%= ep[2] %>"></span>
            <span class="text-[10px] font-mono font-semibold text-apple bg-apple/10 px-2.5 py-1 rounded-lg">GET</span>
            <div>
              <code class="text-[13px] font-mono font-semibold text-ink dark:text-white"><%= ep[0] %></code>
              <p class="text-[11px] text-muted dark:text-[#8E8E93] mt-0.5"><%= ep[1] %></p>
            </div>
          </div>
          <a href="${pageContext.request.contextPath}<%= ep[0] %>" target="_blank"
             onclick="event.stopPropagation()"
             class="text-[11px] text-apple hover:underline font-mono shrink-0 ml-4">abrir →</a>
        </button>
        <% } %>
      </div>

      <%-- Preview JSON --%>
      <div class="jx-win h-full min-h-[280px]">
        <div class="jx-win-bar">
          <span class="jxd-r"></span><span class="jxd-y"></span><span class="jxd-g"></span>
          <span id="jxEpLbl" class="ml-4 text-[11px] font-mono text-[#636366]">GET /jx/health</span>
        </div>
        <div id="jxEpPane" class="jx-code-pane overflow-auto" style="min-height:240px">
<pre><code id="jxEpCode" class="language-json">{
  "status": "UP",
  "version": "3.2.0",
  "uptime": "00:04:12",
  "pool": {
    "active": 2,
    "idle": 8,
    "total": 10
  },
  "threads": {
    "active": 4,
    "virtual": true
  },
  "memory": {
    "usedMB": 48,
    "maxMB": 512
  }
}</code></pre>
        </div>
      </div>

    </div>
  </div>
</section>

<script>
var jxEpSamples = {
  health: {
    label: 'GET /jx/health',
    code: '{\n  "status": "UP",\n  "version": "3.2.0",\n  "uptime": "00:04:12",\n  "pool": {\n    "active": 2,\n    "idle": 8,\n    "total": 10\n  },\n  "threads": {\n    "active": 4,\n    "virtual": true\n  },\n  "memory": {\n    "usedMB": 48,\n    "maxMB": 512\n  }\n}'
  },
  info: {
    label: 'GET /jx/info',
    code: '{\n  "framework": "JxMVC",\n  "version": "3.2.0",\n  "profile": "prod",\n  "java": "21.0.3",\n  "server": "Apache Tomcat/10.1.20",\n  "startedAt": "2026-06-07T08:00:00Z",\n  "controllers": 6,\n  "routes": 24\n}'
  },
  metrics: {
    label: 'GET /jx/metrics',
    code: '{\n  "routes": [\n    {\n      "path": "GET /api/persona/{id}",\n      "requests": 14820,\n      "errors": 12,\n      "avgMs": 3.4,\n      "p95Ms": 8.1\n    },\n    {\n      "path": "POST /api/persona/save",\n      "requests": 3201,\n      "errors": 0,\n      "avgMs": 6.2,\n      "p95Ms": 14.5\n    }\n  ]\n}'
  },
  openapi: {
    label: 'GET /jx/openapi',
    code: '{\n  "openapi": "3.0.1",\n  "info": {\n    "title": "JxMVC API",\n    "version": "3.2.0"\n  },\n  "paths": {\n    "/api/persona/{id}": {\n      "get": {\n        "summary": "Obtener persona",\n        "parameters": [{\n          "name": "id",\n          "in": "path",\n          "required": true\n        }]\n      }\n    }\n  }\n}'
  }
};

function jxEpShow(key, btn) {
  var s = jxEpSamples[key];
  document.getElementById('jxEpLbl').textContent = s.label;
  var codeEl = document.getElementById('jxEpCode');
  codeEl.textContent = s.code;
  hljs.highlightElement(codeEl);
  document.querySelectorAll('.jx-ep-row').forEach(function(r){ r.classList.remove('bg-apple/5','dark:bg-apple/10'); });
  if (btn) btn.classList.add('bg-apple/5','dark:bg-apple/10');
}
</script>

<script>
/* ── Highlight.js ─────────────────────────────────────────────── */
document.querySelectorAll('pre code').forEach(function(el) {
  hljs.highlightElement(el);
});

/* ── Hero code tabs ───────────────────────────────────────────── */
var jxHeroPanes = {ctrl:'jxH-ctrl', model:'jxH-model', cfg:'jxH-cfg'};
function jxCodeTab(key, btn) {
  Object.keys(jxHeroPanes).forEach(function(k) {
    document.getElementById(jxHeroPanes[k]).classList.toggle('hidden', k !== key);
  });
  document.querySelectorAll('.jx-win-tab').forEach(function(b) { b.classList.remove('active'); });
  btn.classList.add('active');
}

/* ── Playground tabs ──────────────────────────────────────────── */
var jxPlayLabels = {rest:'ApiController.java', ws:'ChatEndpoint.java', sched:'TareasProgramadas.java', val:'RegistroForm.java', cache:'CatalogoController.java', auth:'AdminController.java'};
var jxPlayPanes  = {rest:'jxP-rest', ws:'jxP-ws', sched:'jxP-sched', val:'jxP-val', cache:'jxP-cache', auth:'jxP-auth'};
function jxPlay(key, btn) {
  Object.keys(jxPlayPanes).forEach(function(k) {
    document.getElementById(jxPlayPanes[k]).classList.toggle('hidden', k !== key);
  });
  document.querySelectorAll('.jx-play-tab').forEach(function(b) { b.classList.remove('active'); });
  btn.classList.add('active');
  document.getElementById('jxPlayLbl').textContent = jxPlayLabels[key] || '';
}

/* ── Copy helpers ─────────────────────────────────────────────── */
function jxCopyPane(paneId) {
  var pane = document.getElementById(paneId);
  if (!pane) return;
  var active = pane.querySelector('.jx-code-pane:not(.hidden), .jx-code-pane');
  if (!active) return;
  var text = (active.innerText || active.textContent).trim();
  navigator.clipboard.writeText(text);
}
function jxCopyCmd() {
  var el = document.getElementById('jxHeroCmd');
  if (!el) return;
  navigator.clipboard.writeText((el.innerText || el.textContent).trim()).then(function() {
    var ico = document.getElementById('jxCmdIco');
    if (!ico) return;
    ico.innerHTML = '<polyline points="20 6 9 17 4 12"></polyline>';
    ico.style.stroke = '#0071E3';
    setTimeout(function() {
      ico.innerHTML = '<rect x="9" y="9" width="13" height="13" rx="2"></rect><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"></path>';
      ico.style.stroke = '';
    }, 2000);
  });
}

/* ── Spotlight hover ──────────────────────────────────────────── */
function jxSpot(e, el) {
  var r = el.getBoundingClientRect();
  el.style.setProperty('--mx', ((e.clientX - r.left) / r.width * 100) + '%');
  el.style.setProperty('--my', ((e.clientY - r.top)  / r.height * 100) + '%');
}

/* ── IntersectionObserver: pipeline, barras, contadores ──────── */
var io = new IntersectionObserver(function(entries) {
  entries.forEach(function(entry) {
    if (!entry.isIntersecting) return;
    var el = entry.target;

    if (el.classList.contains('jx-pipe-node')) {
      el.classList.add('jx-in');
      io.unobserve(el);
    }
    if (el.classList.contains('jx-bar')) {
      el.style.width = el.dataset.w + '%';
      io.unobserve(el);
    }
    if (el.classList.contains('jx-counter')) {
      var target = parseInt(el.dataset.target, 10);
      if (isNaN(target) || target === 0) { io.unobserve(el); return; }
      var start = 0, dur = 900, step = 16;
      var timer = setInterval(function() {
        start += step;
        var progress = Math.min(start / dur, 1);
        var eased = 1 - Math.pow(1 - progress, 3);
        el.textContent = Math.round(eased * target);
        if (progress >= 1) { el.textContent = target; clearInterval(timer); }
      }, step);
      io.unobserve(el);
    }
  });
}, {threshold: 0.15});

document.querySelectorAll('.jx-pipe-node, .jx-bar, .jx-counter').forEach(function(el) {
  io.observe(el);
});
</script>

<%@ include file="/WEB-INF/views/shared/footer.jspf" %>

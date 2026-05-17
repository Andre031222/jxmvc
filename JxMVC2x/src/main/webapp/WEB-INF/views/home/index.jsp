<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/shared/header.jspf" %>

<%-- ── Hero ─────────────────────────────────────────────────────────── --%>
<section class="relative overflow-hidden
    bg-white dark:bg-[#060D1A]
    border-b border-black/[0.06] dark:border-white/[0.04]">

    <%-- Glow background (solo dark) --%>
    <div class="absolute inset-0 pointer-events-none select-none" aria-hidden="true">
        <div class="absolute -top-32 left-1/4 w-[480px] h-[480px] rounded-full
                    opacity-0 dark:opacity-[0.07] blur-3xl transition-opacity duration-700"
             style="background:#FC801D"></div>
        <div class="absolute top-0 right-[15%] w-[420px] h-[420px] rounded-full
                    opacity-0 dark:opacity-[0.07] blur-3xl transition-opacity duration-700"
             style="background:#087CFA"></div>
        <div class="absolute bottom-0 left-1/2 -translate-x-1/2 w-[480px] h-[360px] rounded-full
                    opacity-0 dark:opacity-[0.05] blur-3xl transition-opacity duration-700"
             style="background:#5A63D6"></div>
    </div>

    <div class="relative max-w-3xl mx-auto px-6 py-24 sm:py-32 text-center jx-reveal jx-delay-1">

        <%-- Tag --%>
        <p class="inline-block text-[10px] font-mono tracking-[0.28em] uppercase
                  text-muted dark:text-[#86868b] mb-6"
           data-i18n="hero.tag">Lightning-X Web Framework</p>

        <%-- Title --%>
        <h1 class="text-[clamp(3rem,10vw,5.5rem)] font-extrabold tracking-tight leading-[1.02] mb-5">
            <span class="jx-grad">JxMVC</span><span class="text-ink dark:text-white"> 3.0</span>
        </h1>

        <%-- Subtitle --%>
        <p class="text-base sm:text-lg text-muted dark:text-[#8a8a8e] leading-relaxed mb-10 max-w-xl mx-auto"
           data-i18n="hero.desc">
            Framework MVC para Jakarta EE. Cero dependencias en runtime,
            205 KB, arranque en 1.2 s. Pool, JSON, validación, WebSocket,
            métricas y OpenAPI — todo incluido sin librerías externas.
        </p>

        <%-- Metrics strip --%>
        <div class="flex justify-center gap-8 sm:gap-12 mb-10">
            <div>
                <p class="text-2xl font-bold text-ink dark:text-white leading-none">
                    205<span class="text-apple text-base font-semibold"> KB</span>
                </p>
                <p class="text-[10px] font-mono uppercase tracking-widest text-muted dark:text-[#86868b] mt-1">JAR</p>
            </div>
            <div class="w-px bg-black/[0.08] dark:bg-white/[0.08] self-stretch"></div>
            <div>
                <p class="text-2xl font-bold text-ink dark:text-white leading-none">
                    0<span class="text-apple text-base font-semibold"> deps</span>
                </p>
                <p class="text-[10px] font-mono uppercase tracking-widest text-muted dark:text-[#86868b] mt-1">Runtime</p>
            </div>
            <div class="w-px bg-black/[0.08] dark:bg-white/[0.08] self-stretch"></div>
            <div>
                <p class="text-2xl font-bold text-ink dark:text-white leading-none">
                    48<span class="text-apple text-base font-semibold"> cls</span>
                </p>
                <p class="text-[10px] font-mono uppercase tracking-widest text-muted dark:text-[#86868b] mt-1">Core</p>
            </div>
            <div class="w-px bg-black/[0.08] dark:bg-white/[0.08] self-stretch"></div>
            <div>
                <p class="text-2xl font-bold text-ink dark:text-white leading-none">
                    1.2<span class="text-apple text-base font-semibold"> s</span>
                </p>
                <p class="text-[10px] font-mono uppercase tracking-widest text-muted dark:text-[#86868b] mt-1">Arranque</p>
            </div>
        </div>

        <%-- CTAs --%>
        <div class="flex justify-center flex-wrap gap-3 mb-10">
            <a href="${pageContext.request.contextPath}/downloads"
               class="px-5 py-2.5 bg-apple text-white text-sm font-medium rounded-full
                      hover:bg-[#0040CC] transition-colors shadow-sm"
               data-i18n="hero.dl">Descargar</a>
            <a href="${pageContext.request.contextPath}/docs"
               class="px-5 py-2.5 bg-black/[0.05] dark:bg-white/10
                      text-ink dark:text-[#f5f5f7]
                      border border-black/10 dark:border-white/10
                      text-sm font-medium rounded-full
                      hover:bg-black/10 dark:hover:bg-white/15 transition-colors"
               data-i18n="hero.docs">Documentación</a>
            <a href="${pageContext.request.contextPath}/errors"
               class="px-5 py-2.5 bg-black/[0.05] dark:bg-white/10
                      text-ink dark:text-[#f5f5f7]
                      border border-black/10 dark:border-white/10
                      text-sm font-medium rounded-full
                      hover:bg-black/10 dark:hover:bg-white/15 transition-colors"
               data-i18n="hero.errors">Probar errores</a>
        </div>

        <%-- Quick start --%>
        <div class="w-full max-w-lg mx-auto">
            <%-- Tabs --%>
            <div class="flex items-center gap-1 mb-0 justify-center">
                <button id="jxTab-install" onclick="jxSwitchTab('install')"
                        class="px-3 py-1.5 text-[10px] font-mono rounded-t-lg transition-colors
                               bg-black/[0.05] dark:bg-white/[0.08]
                               text-ink dark:text-[#f5f5f7] border border-b-0
                               border-black/[0.07] dark:border-white/[0.08]">
                    install
                </button>
                <button id="jxTab-dep" onclick="jxSwitchTab('dep')"
                        class="px-3 py-1.5 text-[10px] font-mono rounded-t-lg transition-colors
                               text-muted dark:text-[#636366]
                               hover:text-ink dark:hover:text-[#f5f5f7]">
                    pom.xml
                </button>
            </div>
            <%-- Code block --%>
            <div class="bg-black/[0.04] dark:bg-white/[0.06]
                        border border-black/[0.07] dark:border-white/[0.08]
                        rounded-b-2xl rounded-tr-2xl overflow-hidden">
                <%-- Install panel --%>
                <div id="jxPanel-install" class="flex items-center gap-3 px-4 py-3">
                    <span class="text-apple font-semibold font-mono text-sm select-none shrink-0">$</span>
                    <code id="jxInstallCmd" class="font-mono text-sm text-ink dark:text-[#c8c8d0] flex-1 text-left">mvn install -f JxMVC.Core/pom.xml</code>
                    <button onclick="jxCopyCode('jxInstallCmd')" title="Copiar"
                            class="text-muted dark:text-[#86868b] hover:text-apple transition-colors shrink-0">
                        <svg id="jxCopyIcon-install" class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                            <path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"></path>
                        </svg>
                    </button>
                </div>
                <%-- Dependency panel --%>
                <div id="jxPanel-dep" class="hidden">
                    <div class="flex items-center justify-between px-4 pt-3 pb-1">
                        <span class="text-[10px] font-mono text-muted dark:text-[#636366] uppercase tracking-wider">pom.xml</span>
                        <button onclick="jxCopyCode('jxDepCode')" title="Copiar"
                                class="text-muted dark:text-[#86868b] hover:text-apple transition-colors">
                            <svg id="jxCopyIcon-dep" class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                                <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                                <path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"></path>
                            </svg>
                        </button>
                    </div>
                    <pre class="px-4 pb-3 font-mono text-xs text-ink dark:text-[#c8c8d0] leading-relaxed"><code id="jxDepCode">&lt;dependency&gt;
  &lt;groupId&gt;jxmvc&lt;/groupId&gt;
  &lt;artifactId&gt;jxmvc-core&lt;/artifactId&gt;
  &lt;version&gt;3.0.0&lt;/version&gt;
&lt;/dependency&gt;</code></pre>
                </div>
            </div>
        </div>

    </div>
</section>

<%-- ── Features ──────────────────────────────────────────────────────── --%>
<section class="py-12">
    <div class="max-w-6xl mx-auto px-4 sm:px-6">
        <p class="text-xs font-mono uppercase tracking-[0.25em] text-muted dark:text-[#86868b] mb-8 jx-reveal jx-delay-1"
           data-i18n="feat.label">Características</p>

        <div class="grid sm:grid-cols-2 lg:grid-cols-3 gap-4 jx-reveal jx-delay-2">

            <div class="bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06]
                        rounded-2xl p-6 hover:border-apple/30 transition-colors">
                <div class="w-8 h-8 rounded-lg bg-apple/10 flex items-center justify-center mb-4">
                    <svg class="w-4 h-4 text-apple" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/>
                    </svg>
                </div>
                <h3 class="text-sm font-semibold text-ink dark:text-[#f5f5f7] mb-1.5" data-i18n="f1.t">Routing inteligente</h3>
                <p class="text-xs text-muted dark:text-[#86868b] leading-relaxed" data-i18n="f1.d">
                    Convención + anotaciones. @JxGetMapping, plantillas {id} y args posicionales.
                </p>
            </div>

            <div class="bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06]
                        rounded-2xl p-6 hover:border-apple/30 transition-colors">
                <div class="w-8 h-8 rounded-lg bg-apple/10 flex items-center justify-center mb-4">
                    <svg class="w-4 h-4 text-apple" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4"/>
                    </svg>
                </div>
                <h3 class="text-sm font-semibold text-ink dark:text-[#f5f5f7] mb-1.5" data-i18n="f2.t">Pool de conexiones</h3>
                <p class="text-xs text-muted dark:text-[#86868b] leading-relaxed" data-i18n="f2.d">
                    JxPool con keepalive y timeout. PostgreSQL, MySQL, SQL Server.
                </p>
            </div>

            <div class="bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06]
                        rounded-2xl p-6 hover:border-apple/30 transition-colors">
                <div class="w-8 h-8 rounded-lg bg-apple/10 flex items-center justify-center mb-4">
                    <svg class="w-4 h-4 text-apple" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path d="M13 10V3L4 14h7v7l9-11h-7z"/>
                    </svg>
                </div>
                <h3 class="text-sm font-semibold text-ink dark:text-[#f5f5f7] mb-1.5" data-i18n="f3.t">Pipeline 14 etapas</h3>
                <p class="text-xs text-muted dark:text-[#86868b] leading-relaxed" data-i18n="f3.d">
                    Rate limit → Auth → CORS → Filtros → DI → Async/Retry → Render.
                </p>
            </div>

            <div class="bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06]
                        rounded-2xl p-6 hover:border-apple/30 transition-colors">
                <div class="w-8 h-8 rounded-lg bg-apple/10 flex items-center justify-center mb-4">
                    <svg class="w-4 h-4 text-apple" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"/>
                    </svg>
                </div>
                <h3 class="text-sm font-semibold text-ink dark:text-[#f5f5f7] mb-1.5" data-i18n="f4.t">GenApi + Acceso directo</h3>
                <p class="text-xs text-muted dark:text-[#86868b] leading-relaxed" data-i18n="f4.d">
                    DBRow.Get() sin POJOs. GenApi.JsonStr(), JsonArray(), JsonPaged().
                </p>
            </div>

            <div class="bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06]
                        rounded-2xl p-6 hover:border-apple/30 transition-colors">
                <div class="w-8 h-8 rounded-lg bg-apple/10 flex items-center justify-center mb-4">
                    <svg class="w-4 h-4 text-apple" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01"/>
                    </svg>
                </div>
                <h3 class="text-sm font-semibold text-ink dark:text-[#f5f5f7] mb-1.5" data-i18n="f5.t">Zero dependencias</h3>
                <p class="text-xs text-muted dark:text-[#86868b] leading-relaxed" data-i18n="f5.d">
                    205 KB. 21 anotaciones, cron expressions, fechas java.time, pool, JSON y WebSocket propios.
                </p>
            </div>

            <div class="bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06]
                        rounded-2xl p-6 hover:border-apple/30 transition-colors">
                <div class="w-8 h-8 rounded-lg bg-apple/10 flex items-center justify-center mb-4">
                    <svg class="w-4 h-4 text-apple" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
                    </svg>
                </div>
                <h3 class="text-sm font-semibold text-ink dark:text-[#f5f5f7] mb-1.5" data-i18n="f6.t">Métricas + OpenAPI</h3>
                <p class="text-xs text-muted dark:text-[#86868b] leading-relaxed" data-i18n="f6.d">
                    /jx/health, /jx/metrics, /jx/openapi sin configuración.
                </p>
            </div>

        </div>
    </div>
</section>

<%-- ── Endpoints del sistema ───────────────────────────────────────── --%>
<section class="pb-10">
    <div class="max-w-6xl mx-auto px-4 sm:px-6">
        <p class="text-xs font-mono uppercase tracking-[0.25em] text-muted dark:text-[#86868b] mb-4 jx-reveal jx-delay-1"
           data-i18n="ep.label">Endpoints del sistema</p>

        <div class="bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06]
                    rounded-2xl overflow-hidden divide-y divide-black/[0.05] dark:divide-white/[0.05] jx-reveal jx-delay-2">

            <% for (String[] ep : new String[][]{
                {"/jx/health",  "Estado del pool, uptime y threads activos"},
                {"/jx/info",    "Versión, perfil activo, Java y servidor"},
                {"/jx/metrics", "Peticiones por ruta: total, errores y latencia media"},
                {"/jx/openapi", "Spec OpenAPI 3.0 generada de las anotaciones"}
            }) { %>
            <div class="flex items-center justify-between px-5 py-3.5
                        hover:bg-black/[0.02] dark:hover:bg-white/[0.02] transition-colors">
                <div class="flex items-center gap-4">
                    <span class="text-[10px] font-mono font-medium text-apple bg-apple/10 px-2 py-0.5 rounded">GET</span>
                    <div>
                        <code class="text-sm font-mono text-ink dark:text-[#f5f5f7]"><%= ep[0] %></code>
                        <p class="text-xs text-muted dark:text-[#86868b] mt-0.5"><%= ep[1] %></p>
                    </div>
                </div>
                <a href="${pageContext.request.contextPath}<%= ep[0] %>" target="_blank"
                   class="text-xs text-apple hover:underline font-mono shrink-0"
                   data-i18n="ep.try">probar →</a>
            </div>
            <% } %>

        </div>
    </div>
</section>

<script>
var jxActiveTab = 'install';

function jxSwitchTab(tab) {
    jxActiveTab = tab;
    var tabs = ['install', 'dep'];
    tabs.forEach(function(t) {
        var btn = document.getElementById('jxTab-' + t);
        var panel = document.getElementById('jxPanel-' + t);
        var isActive = t === tab;
        panel.classList.toggle('hidden', !isActive);
        if (isActive) {
            btn.classList.add('bg-black/[0.05]', 'dark:bg-white/[0.08]',
                              'text-ink', 'dark:text-[#f5f5f7]',
                              'border', 'border-b-0',
                              'border-black/[0.07]', 'dark:border-white/[0.08]');
            btn.classList.remove('text-muted', 'dark:text-[#636366]',
                                 'hover:text-ink', 'dark:hover:text-[#f5f5f7]');
        } else {
            btn.classList.remove('bg-black/[0.05]', 'dark:bg-white/[0.08]',
                                 'text-ink', 'dark:text-[#f5f5f7]',
                                 'border', 'border-b-0',
                                 'border-black/[0.07]', 'dark:border-white/[0.08]');
            btn.classList.add('text-muted', 'dark:text-[#636366]',
                              'hover:text-ink', 'dark:hover:text-[#f5f5f7]');
        }
    });
}

function jxCopyCode(id) {
    var el = document.getElementById(id);
    if (!el) return;
    var text = el.innerText || el.textContent;
    text = text.replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&amp;/g,'&').trim();
    navigator.clipboard.writeText(text).then(function() {
        var suffix = id === 'jxInstallCmd' ? 'install' : 'dep';
        var icon = document.getElementById('jxCopyIcon-' + suffix);
        if (!icon) return;
        icon.innerHTML = '<polyline points="20 6 9 17 4 12"></polyline>';
        icon.style.stroke = '#087CFA';
        setTimeout(function() {
            icon.innerHTML = '<rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"></path>';
            icon.style.stroke = '';
        }, 2000);
    });
}
</script>

<%@ include file="/WEB-INF/views/shared/footer.jspf" %>

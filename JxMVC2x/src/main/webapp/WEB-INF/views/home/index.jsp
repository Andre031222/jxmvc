<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/shared/header.jspf" %>

<%-- ── Hero ─────────────────────────────────────────────────────────── --%>
<section class="pt-10 pb-16 border-b border-black/[0.06] dark:border-white/[0.06]">
    <div class="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6 lg:gap-12">

        <div class="max-w-2xl">
            <p class="text-xs font-mono tracking-[0.25em] text-muted dark:text-[#86868b] mb-6 uppercase jx-reveal jx-delay-1"
               data-i18n="hero.tag">Lightning-X Web Framework</p>

            <h1 class="text-5xl sm:text-6xl lg:text-[80px] font-extrabold tracking-tight leading-[1.02] mb-6 jx-reveal jx-delay-2">
                JxMVC<span class="text-apple"> 3.0</span>
            </h1>

            <p class="text-base sm:text-lg text-muted dark:text-[#86868b] leading-relaxed mb-8 max-w-lg jx-reveal jx-delay-3"
               data-i18n="hero.desc">
                Framework MVC para Jakarta EE. Cero dependencias en runtime,
                205 KB, arranque en 1.2 s. Pool, JSON, validacion, WebSocket,
                metricas y OpenAPI — todo incluido sin librerias externas.
            </p>

            <div class="flex flex-wrap gap-3 jx-reveal jx-delay-4">
                <a href="${pageContext.request.contextPath}/home/downloads"
                   class="px-5 py-2.5 bg-apple text-white text-sm font-medium rounded-full hover:bg-[#0077ed] transition-colors shadow-sm"
                   data-i18n="hero.dl">Descargar</a>
                <a href="${pageContext.request.contextPath}/home/docs"
                   class="px-5 py-2.5 bg-white dark:bg-white/10 text-ink dark:text-[#f5f5f7] border border-black/10 dark:border-white/10 text-sm font-medium rounded-full hover:bg-black/5 dark:hover:bg-white/15 transition-colors"
                   data-i18n="hero.docs">Documentación</a>
                <a href="${pageContext.request.contextPath}/home/errors"
                   class="px-5 py-2.5 bg-white dark:bg-white/10 text-ink dark:text-[#f5f5f7] border border-black/10 dark:border-white/10 text-sm font-medium rounded-full hover:bg-black/5 dark:hover:bg-white/15 transition-colors"
                   data-i18n="hero.errors">Probar errores</a>
            </div>
        </div>

        <%-- Logo SVG --%>
        <div class="hidden lg:flex shrink-0">
            <div class="w-44 h-44 flex items-center justify-center rounded-[2.5rem] bg-white dark:bg-white/5 border border-black/[0.06] dark:border-white/[0.06] shadow-xl">
                <svg width="96" height="96" viewBox="0 0 256 256" xmlns="http://www.w3.org/2000/svg">
                    <defs>
                        <linearGradient x1="37%" y1="51%" x2="178%" y2="42%" id="ga2">
                            <stop stop-color="#FC801D" offset="9%"/>
                            <stop stop-color="#577DB3" offset="41%"/>
                            <stop stop-color="#087CFA" offset="59%"/>
                        </linearGradient>
                        <linearGradient x1="74%" y1="115%" x2="36%" y2="1%" id="gb2">
                            <stop stop-color="#FE2857" offset="0%"/>
                            <stop stop-color="#386CDA" offset="44%"/>
                            <stop stop-color="#087CFA" offset="100%"/>
                        </linearGradient>
                        <linearGradient x1="29%" y1="24%" x2="82%" y2="130%" id="gc2">
                            <stop stop-color="#FE2857" offset="0%"/>
                            <stop stop-color="#5A63D6" offset="79%"/>
                            <stop stop-color="#087CFA" offset="100%"/>
                        </linearGradient>
                    </defs>
                    <path fill="url(#ga2)" d="M40.5 180.6 2.9 150.8l22.1-41 33.3 11.1z"/>
                    <path fill="#087CFA" d="m256 68.2-4.6 148.3-98.6 39.5-53.7-34.7z"/>
                    <path fill="url(#gb2)" d="m256 68.2-48.8 47.6L144.5 39l31-34.8z"/>
                    <path fill="url(#gc2)" d="m99.1 221.3-78.5 28.4 16.5-57.5 21.2-71.3L0 101.4 37.1 0l83.8 9.9 86.3 105.9z"/>
                    <text x="128" y="125" text-anchor="middle" font-family="Segoe UI,Arial,sans-serif"
                          font-size="75" font-weight="900" fill="white">Jx</text>
                    <text x="128" y="165" text-anchor="middle" font-family="Segoe UI,Arial,sans-serif"
                          font-size="22" font-weight="bold" fill="white" letter-spacing="2">MVC</text>
                </svg>
            </div>
        </div>
    </div>
</section>

<%-- ── Métricas rápidas ─────────────────────────────────────────────── --%>
<section class="py-10 border-b border-black/[0.06] dark:border-white/[0.06]">
    <div class="grid grid-cols-2 sm:grid-cols-4 gap-6">
        <div class="jx-reveal jx-delay-1">
            <p class="text-3xl font-bold tracking-tight text-ink dark:text-[#f5f5f7]">205<span class="text-apple">KB</span></p>
            <p class="text-xs text-muted dark:text-[#86868b] mt-1">Tamaño JAR</p>
        </div>
        <div class="jx-reveal jx-delay-2">
            <p class="text-3xl font-bold tracking-tight text-ink dark:text-[#f5f5f7]">0<span class="text-apple">deps</span></p>
            <p class="text-xs text-muted dark:text-[#86868b] mt-1">Runtime</p>
        </div>
        <div class="jx-reveal jx-delay-3">
            <p class="text-3xl font-bold tracking-tight text-ink dark:text-[#f5f5f7]">48<span class="text-apple">cls</span></p>
            <p class="text-xs text-muted dark:text-[#86868b] mt-1">Clases core</p>
        </div>
        <div class="jx-reveal jx-delay-4">
            <p class="text-3xl font-bold tracking-tight text-ink dark:text-[#f5f5f7]">1.2<span class="text-apple">s</span></p>
            <p class="text-xs text-muted dark:text-[#86868b] mt-1">Arranque</p>
        </div>
    </div>
</section>

<%-- ── Features ──────────────────────────────────────────────────────── --%>
<section class="py-12">
    <p class="text-xs font-mono uppercase tracking-[0.25em] text-muted dark:text-[#86868b] mb-8 jx-reveal jx-delay-1"
       data-i18n="feat.label">Características</p>

    <div class="grid sm:grid-cols-2 lg:grid-cols-3 gap-4 jx-reveal jx-delay-2">

        <div class="bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06] rounded-2xl p-6 hover:border-apple/30 transition-colors">
            <div class="w-8 h-8 rounded-lg bg-apple/10 flex items-center justify-center mb-4">
                <svg class="w-4 h-4 text-apple" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/>
                </svg>
            </div>
            <h3 class="text-sm font-semibold mb-1.5" data-i18n="f1.t">Routing inteligente</h3>
            <p class="text-xs text-muted dark:text-[#86868b] leading-relaxed" data-i18n="f1.d">
                Convención + anotaciones. @JxGetMapping, plantillas {id} y args posicionales.
            </p>
        </div>

        <div class="bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06] rounded-2xl p-6 hover:border-apple/30 transition-colors">
            <div class="w-8 h-8 rounded-lg bg-apple/10 flex items-center justify-center mb-4">
                <svg class="w-4 h-4 text-apple" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4"/>
                </svg>
            </div>
            <h3 class="text-sm font-semibold mb-1.5" data-i18n="f2.t">Pool de conexiones</h3>
            <p class="text-xs text-muted dark:text-[#86868b] leading-relaxed" data-i18n="f2.d">
                JxPool con keepalive y timeout. PostgreSQL, MySQL, SQL Server.
            </p>
        </div>

        <div class="bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06] rounded-2xl p-6 hover:border-apple/30 transition-colors">
            <div class="w-8 h-8 rounded-lg bg-apple/10 flex items-center justify-center mb-4">
                <svg class="w-4 h-4 text-apple" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path d="M13 10V3L4 14h7v7l9-11h-7z"/>
                </svg>
            </div>
            <h3 class="text-sm font-semibold mb-1.5" data-i18n="f3.t">Pipeline 14 etapas</h3>
            <p class="text-xs text-muted dark:text-[#86868b] leading-relaxed" data-i18n="f3.d">
                Rate limit → Auth → CORS → Filtros → DI → Async/Retry → Render.
            </p>
        </div>

        <div class="bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06] rounded-2xl p-6 hover:border-apple/30 transition-colors">
            <div class="w-8 h-8 rounded-lg bg-apple/10 flex items-center justify-center mb-4">
                <svg class="w-4 h-4 text-apple" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"/>
                </svg>
            </div>
            <h3 class="text-sm font-semibold mb-1.5" data-i18n="f4.t">GenApi + Acceso directo</h3>
            <p class="text-xs text-muted dark:text-[#86868b] leading-relaxed" data-i18n="f4.d">
                DBRow.Get() sin POJOs. GenApi.JsonStr(), JsonArray(), JsonPaged().
            </p>
        </div>

        <div class="bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06] rounded-2xl p-6 hover:border-apple/30 transition-colors">
            <div class="w-8 h-8 rounded-lg bg-apple/10 flex items-center justify-center mb-4">
                <svg class="w-4 h-4 text-apple" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01"/>
                </svg>
            </div>
            <h3 class="text-sm font-semibold mb-1.5" data-i18n="f5.t">Zero dependencias</h3>
            <p class="text-xs text-muted dark:text-[#86868b] leading-relaxed" data-i18n="f5.d">
                205 KB. JSON, validacion, pool, scheduler y WebSocket propios.
            </p>
        </div>

        <div class="bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06] rounded-2xl p-6 hover:border-apple/30 transition-colors">
            <div class="w-8 h-8 rounded-lg bg-apple/10 flex items-center justify-center mb-4">
                <svg class="w-4 h-4 text-apple" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
                </svg>
            </div>
            <h3 class="text-sm font-semibold mb-1.5" data-i18n="f6.t">Métricas + OpenAPI</h3>
            <p class="text-xs text-muted dark:text-[#86868b] leading-relaxed" data-i18n="f6.d">
                /jx/health, /jx/metrics, /jx/openapi sin configuración.
            </p>
        </div>

    </div>
</section>

<%-- ── Endpoints del sistema ───────────────────────────────────────── --%>
<section class="pb-10">
    <p class="text-xs font-mono uppercase tracking-[0.25em] text-muted dark:text-[#86868b] mb-4 jx-reveal jx-delay-1"
       data-i18n="ep.label">Endpoints del sistema</p>

    <div class="bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06] rounded-2xl overflow-hidden divide-y divide-black/[0.05] dark:divide-white/[0.05] jx-reveal jx-delay-2">

        <% for (String[] ep : new String[][]{
            {"/jx/health",   "Estado del pool, uptime y threads activos"},
            {"/jx/info",     "Versión, perfil activo, Java y servidor"},
            {"/jx/metrics",  "Peticiones por ruta: total, errores y latencia media"},
            {"/jx/openapi",  "Spec OpenAPI 3.0 generada de las anotaciones"}
        }) { %>
        <div class="flex items-center justify-between px-5 py-3.5 hover:bg-black/[0.02] dark:hover:bg-white/[0.02] transition-colors">
            <div class="flex items-center gap-4">
                <span class="text-[10px] font-mono font-medium text-apple bg-apple/10 px-2 py-0.5 rounded">GET</span>
                <div>
                    <code class="text-sm font-mono text-ink dark:text-[#f5f5f7]"><%= ep[0] %></code>
                    <p class="text-xs text-muted dark:text-[#86868b] mt-0.5"><%= ep[1] %></p>
                </div>
            </div>
            <a href="${pageContext.request.contextPath}<%= ep[0] %>" target="_blank"
               class="text-xs text-apple hover:underline font-mono shrink-0" data-i18n="ep.try">probar →</a>
        </div>
        <% } %>

    </div>
</section>

<%@ include file="/WEB-INF/views/shared/footer.jspf" %>

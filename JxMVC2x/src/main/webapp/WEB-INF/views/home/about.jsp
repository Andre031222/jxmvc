<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/shared/header.jspf" %>

<%-- ── Encabezado ───────────────────────────────────────────────────── --%>
<div class="mb-12 jx-reveal jx-delay-1">
    <p class="text-xs font-mono uppercase tracking-[0.25em] text-slate-400 mb-3">About</p>
    <h1 class="text-4xl font-bold tracking-tight mb-4">
        Lightning-X MVC <span class="text-accent">3.0</span>
    </h1>
    <p class="text-base text-slate-500 dark:text-slate-400 max-w-2xl leading-relaxed">
        Framework MVC para Jakarta EE construido desde cero — sin Spring, sin Hibernate,
        sin dependencias en runtime. Un JAR de 205 KB que incluye routing, pool de conexiones,
        validacion, JSON, WebSocket, metricas y OpenAPI. Virtual Threads detectados automaticamente en Java 21+.
    </p>
</div>

<%-- ── Métricas clave ──────────────────────────────────────────────── --%>
<div class="grid grid-cols-2 sm:grid-cols-4 gap-px bg-slate-200 dark:bg-slate-800 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden mb-12 jx-reveal jx-delay-2">
    <div class="bg-white dark:bg-[#0a0a0a] px-6 py-5">
        <p class="text-2xl font-bold font-mono">205<span class="text-sm font-normal text-slate-400"> KB</span></p>
        <p class="text-xs text-slate-400 mt-1">Tamaño del JAR</p>
    </div>
    <div class="bg-white dark:bg-[#0a0a0a] px-6 py-5">
        <p class="text-2xl font-bold font-mono">0</p>
        <p class="text-xs text-slate-400 mt-1">Dependencias runtime</p>
    </div>
    <div class="bg-white dark:bg-[#0a0a0a] px-6 py-5">
        <p class="text-2xl font-bold font-mono">14</p>
        <p class="text-xs text-slate-400 mt-1">Etapas del pipeline</p>
    </div>
    <div class="bg-white dark:bg-[#0a0a0a] px-6 py-5">
        <p class="text-2xl font-bold font-mono">48</p>
        <p class="text-xs text-slate-400 mt-1">Clases en el core</p>
    </div>
</div>

<%-- ── Pipeline 14 etapas ────────────────────────────────────────── --%>
<div class="mb-12 jx-reveal jx-delay-3">
    <p class="text-xs font-mono uppercase tracking-[0.25em] text-slate-400 mb-5">Pipeline de cada request</p>
    <div class="divide-y divide-slate-100 dark:divide-slate-800 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden">
        <%
        String[][] steps = {
            {"01","Endpoints internos",   "/jx/health · /jx/info · /jx/metrics · /jx/openapi"},
            {"02","Métricas — inicio",    "Timer de latencia por ruta"},
            {"03","Rate limiting",         "@JxRateLimit — ventana deslizante por IP + ruta"},
            {"04","Resolución de ruta",    "Convención / anotaciones / plantillas {var}"},
            {"05","Perfil de ejecución",   "@JxProfile — activa/desactiva por entorno"},
            {"06","Autenticación",         "@JxRequireAuth / @JxRequireRole"},
            {"07","CORS",                  "@JxCors global o por controlador/acción"},
            {"08","Filtros before",        "@JxFilter · JxFilters.before()"},
            {"09","Instancia + DI",        "Controlador instanciado, @JxInject resuelto"},
            {"10","@JxBeforeAction",       "Interceptores pre-acción por método"},
            {"11","@JxModelAttr",          "Atributos comunes inyectados al modelo"},
            {"12","Invocación",            "@JxAsync (background) o @JxRetry (reintentos)"},
            {"13","@JxAfterAction + after","Interceptores post-acción y filtros after"},
            {"14","Render + métricas",     "Negociación de contenido · JSP / JSON / raw · registro final"},
        };
        for (String[] step : steps) {
        %>
        <div class="flex items-start gap-5 px-5 py-3.5 hover:bg-slate-50 dark:hover:bg-[#111111] transition-colors">
            <span class="text-xs font-mono text-slate-300 dark:text-slate-600 w-6 shrink-0 pt-px"><%= step[0] %></span>
            <div class="min-w-0">
                <p class="text-sm font-medium"><%= step[1] %></p>
                <p class="text-xs text-slate-400 dark:text-slate-500 mt-0.5"><%= step[2] %></p>
            </div>
        </div>
        <% } %>
    </div>
</div>

<%-- ── Comparativa ────────────────────────────────────────────────── --%>
<div class="mb-12 jx-reveal jx-delay-4">
    <p class="text-xs font-mono uppercase tracking-[0.25em] text-slate-400 mb-5">Comparativa</p>
    <div class="overflow-x-auto border border-slate-200 dark:border-slate-800 rounded-xl">
        <table class="w-full text-xs">
            <thead>
                <tr class="border-b border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-[#111111]">
                    <th class="text-left px-5 py-3 font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Aspecto</th>
                    <th class="text-center px-4 py-3 font-semibold text-accent">JxMVC 3.0</th>
                    <th class="text-center px-4 py-3 font-semibold text-slate-500">Spring Boot 3</th>
                    <th class="text-center px-4 py-3 font-semibold text-slate-500">Jakarta EE raw</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                <%
                String[][] rows = {
                    {"WAR / JAR mínimo",   "205 KB",   "~18 MB",   "~50 KB"},
                    {"Dependencias runtime","0",        "~150+",    "0"},
                    {"Routing",            "Conv+Ann",  "Ann",      "Manual"},
                    {"Pool de conexiones", "propio",   "HikariCP", "ninguno"},
                    {"DI",                 "propio",   "Spring DI","CDI"},
                    {"Validación",         "propia",   "Bean Val", "Bean Val"},
                    {"JSON",               "propio",   "Jackson",  "manual"},
                    {"WebSocket",          "built-in", "ext",      "manual"},
                    {"Métricas",           "built-in", "Micrometer","manual"},
                    {"OpenAPI",            "built-in", "SpringDoc","manual"},
                    {"Arranque (aprox.)",  "1.2 s",    "~3-8 s",   "< 300 ms"},
                };
                for (String[] row : rows) {
                %>
                <tr class="hover:bg-slate-50 dark:hover:bg-[#111111] transition-colors">
                    <td class="px-5 py-3 text-slate-600 dark:text-slate-300 font-medium"><%= row[0] %></td>
                    <td class="px-4 py-3 text-center font-mono text-accent font-medium"><%= row[1] %></td>
                    <td class="px-4 py-3 text-center text-slate-400 font-mono"><%= row[2] %></td>
                    <td class="px-4 py-3 text-center text-slate-400 font-mono"><%= row[3] %></td>
                </tr>
                <% } %>
            </tbody>
        </table>
    </div>
</div>

<%-- ── Stack técnico ───────────────────────────────────────────────── --%>
<div class="mb-12 jx-reveal jx-delay-4">
    <p class="text-xs font-mono uppercase tracking-[0.25em] text-slate-400 mb-5">Stack</p>
    <div class="grid sm:grid-cols-2 lg:grid-cols-3 gap-px bg-slate-200 dark:bg-slate-800 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden">
        <%
        String[][] stack = {
            {"Lenguaje",   "Java 17+ / 21+",         "Virtual Threads detectados automaticamente en Java 21+"},
            {"Servidor",   "Apache Tomcat 10+",     "Jakarta EE 11, Servlet 6.0"},
            {"Build",      "Maven 3.8+",            "WAR packaging, cargo-maven3-plugin"},
            {"DB",         "PostgreSQL / MySQL / SQL Server", "Driver JDBC estándar"},
            {"Vistas",     "JSP + Tailwind CSS",   "Sin JSTL requerido — EL puro"},
            {"Testing",    "cargo:run embebido",    "Deploy local en segundos"},
        };
        for (String[] s : stack) {
        %>
        <div class="bg-white dark:bg-[#0a0a0a] px-5 py-4 hover:bg-slate-50 dark:hover:bg-[#111111] transition-colors">
            <p class="text-xs text-slate-400 mb-1"><%= s[0] %></p>
            <p class="text-sm font-medium mb-0.5"><%= s[1] %></p>
            <p class="text-xs text-slate-400"><%= s[2] %></p>
        </div>
        <% } %>
    </div>
</div>

<%-- ── CTA ───────────────────────────────────────────────────────── --%>
<div class="flex flex-wrap gap-3 jx-reveal jx-delay-4">
    <a href="${pageContext.request.contextPath}/home/docs"
       class="px-5 py-2.5 bg-slate-900 dark:bg-white text-white dark:text-slate-900 text-sm font-medium rounded-lg hover:opacity-90 transition-opacity">
        Ver documentación
    </a>
    <a href="${pageContext.request.contextPath}/home/downloads"
       class="px-5 py-2.5 border border-slate-300 dark:border-slate-700 text-sm font-medium rounded-lg hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors">
        Descargar
    </a>
    <a href="${pageContext.request.contextPath}/jx/info" target="_blank"
       class="px-5 py-2.5 border border-slate-300 dark:border-slate-700 text-sm font-mono rounded-lg hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors">
        /jx/info →
    </a>
</div>

<%@ include file="/WEB-INF/views/shared/footer.jspf" %>

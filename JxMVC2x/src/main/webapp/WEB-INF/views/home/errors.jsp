<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/shared/header.jspf" %>

<section class="py-10 max-w-2xl">

    <%-- Encabezado --%>
    <p class="text-xs uppercase tracking-[0.35em] text-slate-400 dark:text-slate-500 jx-reveal jx-delay-1">
        Framework · HTTP
    </p>
    <h1 class="text-3xl font-semibold mt-3 jx-reveal jx-delay-2">Manejo de errores</h1>
    <p class="text-sm text-slate-500 dark:text-slate-400 mt-2 jx-reveal jx-delay-3">
        Respuestas controladas para verificar que el framework distingue correctamente cada codigo HTTP.
    </p>

    <%-- Tabla de pruebas --%>
    <div class="mt-10 divide-y divide-slate-100 dark:divide-slate-800 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden jx-reveal jx-delay-4">

        <%-- 403 --%>
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between px-4 sm:px-6 py-4 gap-3 sm:gap-0 hover:bg-slate-50 dark:hover:bg-slate-900/50 transition-colors">
            <div class="flex items-center gap-4 sm:gap-5">
                <span class="text-xs font-mono font-medium text-slate-400 w-8">403</span>
                <div>
                    <p class="text-sm font-medium">Acceso denegado</p>
                    <p class="text-xs text-slate-400 dark:text-slate-500 mt-0.5">
                        Lanza <code class="font-mono">JxException(403, ...)</code> de forma intencional
                    </p>
                </div>
            </div>
            <a href="${pageContext.request.contextPath}/home/error403"
               class="shrink-0 text-xs font-medium px-4 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700
                      hover:bg-slate-900 hover:text-white dark:hover:bg-white dark:hover:text-slate-900
                      transition-all">
                Probar
            </a>
        </div>

        <%-- 404 --%>
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between px-4 sm:px-6 py-4 gap-3 sm:gap-0 hover:bg-slate-50 dark:hover:bg-slate-900/50 transition-colors">
            <div class="flex items-center gap-4 sm:gap-5">
                <span class="text-xs font-mono font-medium text-slate-400 w-8">404</span>
                <div>
                    <p class="text-sm font-medium">Ruta inexistente</p>
                    <p class="text-xs text-slate-400 dark:text-slate-500 mt-0.5">
                        <code class="font-mono">/home/no-existe-ahora</code> — no esta registrada
                    </p>
                </div>
            </div>
            <a href="${pageContext.request.contextPath}/home/no-existe-ahora"
               class="shrink-0 text-xs font-medium px-4 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700
                      hover:bg-slate-900 hover:text-white dark:hover:bg-white dark:hover:text-slate-900
                      transition-all">
                Probar
            </a>
        </div>

        <%-- 405 --%>
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between px-4 sm:px-6 py-4 gap-3 sm:gap-0 hover:bg-slate-50 dark:hover:bg-slate-900/50 transition-colors">
            <div class="flex items-center gap-4 sm:gap-5">
                <span class="text-xs font-mono font-medium text-slate-400 w-8">405</span>
                <div>
                    <p class="text-sm font-medium">Metodo no permitido</p>
                    <p class="text-xs text-slate-400 dark:text-slate-500 mt-0.5">
                        <code class="font-mono">/home/ping</code> acepta GET — este formulario envia POST
                    </p>
                </div>
            </div>
            <form method="post" action="${pageContext.request.contextPath}/home/ping">
                <button type="submit"
                        class="shrink-0 text-xs font-medium px-4 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700
                               hover:bg-slate-900 hover:text-white dark:hover:bg-white dark:hover:text-slate-900
                               transition-all">
                    Probar
                </button>
            </form>
        </div>

        <%-- 500 --%>
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between px-4 sm:px-6 py-4 gap-3 sm:gap-0 hover:bg-slate-50 dark:hover:bg-slate-900/50 transition-colors">
            <div class="flex items-center gap-4 sm:gap-5">
                <span class="text-xs font-mono font-medium text-slate-400 w-8">500</span>
                <div>
                    <p class="text-sm font-medium">Excepcion interna</p>
                    <p class="text-xs text-slate-400 dark:text-slate-500 mt-0.5">
                        Lanza <code class="font-mono">JxException.serverError()</code> de forma intencional
                    </p>
                </div>
            </div>
            <a href="${pageContext.request.contextPath}/home/error500"
               class="shrink-0 text-xs font-medium px-4 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700
                      hover:bg-slate-900 hover:text-white dark:hover:bg-white dark:hover:text-slate-900
                      transition-all">
                Probar
            </a>
        </div>

    </div>

    <%-- Nota --%>
    <p class="mt-6 text-xs text-slate-400 dark:text-slate-500 jx-reveal jx-delay-4">
        Las respuestas de error renderizan <code class="font-mono">WEB-INF/views/shared/error.jsp</code>
        con los atributos <code class="font-mono">jx_error_code</code> y <code class="font-mono">jx_error_message</code>.
    </p>

</section>

<%@ include file="/WEB-INF/views/shared/footer.jspf" %>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/shared/header.jspf" %>

<section class="py-10">
    <div class="max-w-2xl">
        <p class="text-sm uppercase tracking-[0.35em] text-slate-500">Demo</p>
        <h1 class="text-4xl md:text-5xl font-semibold mt-3">Captura de argumentos</h1>
        <p class="text-lg text-slate-600 mt-4">
            Probando rutas dinámicas y sanitización básica.
        </p>
    </div>

    <div class="bg-white/90 dark:bg-[#111111]/70 border border-slate-200 rounded-3xl p-8 shadow-sm mt-10">
        <div class="grid gap-6">
            <div>
                <p class="text-xs uppercase tracking-[0.3em] text-slate-400">Ruta invocada</p>
                <p class="text-lg font-semibold mt-2">${route}</p>
            </div>
            <div class="grid md:grid-cols-2 gap-6">
                <div>
                    <p class="text-xs uppercase tracking-[0.3em] text-slate-400">Valor capturado</p>
                    <p class="text-lg font-semibold mt-2">${value}</p>
                </div>
                <div>
                    <p class="text-xs uppercase tracking-[0.3em] text-slate-400">Cantidad de argumentos</p>
                    <p class="text-lg font-semibold mt-2">${argsCount}</p>
                </div>
            </div>
        </div>
    </div>
                
    <div class="mt-10 bg-white/80 dark:bg-[#111111]/70 border border-slate-200 dark:border-slate-800 rounded-3xl p-8 shadow-xl">            
        <div class="flex items-center justify-between mb-6">
            <div>
                <p class="text-sm uppercase tracking-[0.3em] text-slate-400 dark:text-slate-500">Quick links</p>
                <p class="text-xl font-semibold">Rutas principales</p>
            </div>
            <span class="px-3 py-1 rounded-full bg-emerald-100 dark:bg-emerald-500/20 text-emerald-700 dark:text-emerald-300 text-xs font-semibold">Live</span>
        </div>
        <ul class="space-y-4 text-sm">
            <li class="flex items-center justify-between border border-slate-200 dark:border-slate-800 rounded-2xl px-4 py-3">
                <span>/home/about</span>
                <a class="text-accent font-semibold" href="${pageContext.request.contextPath}/home/about">Abrir</a>
            </li>
            <li class="flex items-center justify-between border border-slate-200 dark:border-slate-800 rounded-2xl px-4 py-3">
                <span>/home/ping</span>
                <a class="text-accent font-semibold" href="${pageContext.request.contextPath}/home/ping">Abrir</a>
            </li>
            <li class="flex items-center justify-between border border-slate-200 dark:border-slate-800 rounded-2xl px-4 py-3">
                <span>/home/grabar/datos</span>
                <a class="text-accent font-semibold" href="${pageContext.request.contextPath}/home/grabar/datos">Abrir</a>
            </li>
            <li class="flex items-center justify-between border border-slate-200 dark:border-slate-800 rounded-2xl px-4 py-3">
                <span>/home/bd</span>
                <a class="text-accent font-semibold" href="${pageContext.request.contextPath}/home/bd">Abrir</a>
            </li>
        </ul>
    </div>

    <div class="mt-8">
        <a class="inline-flex items-center gap-2 text-sm font-semibold text-accent" href="${pageContext.request.contextPath}/home/index">
            Volver a inicio
        </a>
    </div>
            
            
</section>

<%@ include file="/WEB-INF/views/shared/footer.jspf" %>

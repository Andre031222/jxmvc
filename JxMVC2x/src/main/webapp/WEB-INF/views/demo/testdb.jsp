<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/shared/header.jspf" %>

<section class="py-10">
    <div class="max-w-4xl">
        <p class="text-sm uppercase tracking-[0.35em] text-slate-500 dark:text-slate-400">DB Test</p>
        <h1 class="text-4xl md:text-5xl font-semibold mt-3">CRUD de prueba sobre tabla <code>dicTest</code></h1>
        <p class="text-lg text-slate-600 dark:text-slate-300 mt-4">
            Accesos rapidos para evaluar lectura, insercion, actualizacion y borrado con <code>JxDB</code>.
        </p>
    </div>

    <div class="grid gap-4 md:gap-6 mt-10">
        <div class="bg-white/90 dark:bg-[#111111]/70 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-lg">
            <h2 class="text-xl font-semibold">Conectividad</h2>
            <div class="mt-4">
                <a class="inline-flex px-5 py-3 rounded-full bg-ink text-white text-sm font-semibold"
                   href="${pageContext.request.contextPath}/demo/dbremote">
                    Probar conexion remota
                </a>
            </div>
        </div>

        <div class="bg-white/90 dark:bg-[#111111]/70 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-lg">
            <h2 class="text-xl font-semibold">Read</h2>
            <div class="flex flex-wrap gap-4 mt-4">
                <a class="inline-flex px-5 py-3 rounded-full bg-emerald-600 text-white text-sm font-semibold"
                   href="${pageContext.request.contextPath}/demo/test-list">
                    Ver todos
                </a>
                <a class="inline-flex px-5 py-3 rounded-full border border-slate-300 text-sm font-semibold"
                   href="${pageContext.request.contextPath}/demo/test-get/1">
                    Ver id=1
                </a>
                <a class="inline-flex px-5 py-3 rounded-full border border-slate-300 text-sm font-semibold"
                   href="${pageContext.request.contextPath}/demo/test-debug">
                    Debug tabla
                </a>
            </div>
        </div>

        <div class="bg-white/90 dark:bg-[#111111]/70 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-lg">
            <h2 class="text-xl font-semibold">Create</h2>
            <p class="text-sm text-slate-600 dark:text-slate-300 mt-2">
                Inserta un registro usando los campos reales <code>DNI</code> y <code>Nombres</code>.
            </p>
            <div class="mt-4">
                <a class="inline-flex px-5 py-3 rounded-full bg-blue-600 text-white text-sm font-semibold"
                   href="${pageContext.request.contextPath}/demo/test-add/12345678/Juan%20Perez">
                    Insertar ejemplo
                </a>
            </div>
        </div>

        <div class="bg-white/90 dark:bg-[#111111]/70 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-lg">
            <h2 class="text-xl font-semibold">Update</h2>
            <p class="text-sm text-slate-600 dark:text-slate-300 mt-2">
                Actualiza el registro con <code>DNI=42708896</code> y cambia <code>Nombres</code> segun el clic.
            </p>
            <div class="flex flex-wrap gap-4 mt-4">
                <a class="inline-flex px-5 py-3 rounded-full bg-amber-500 text-white text-sm font-semibold"
                   href="${pageContext.request.contextPath}/demo/test-update-dni/42708896/actualizacion%201">
                    Actualizacion 1
                </a>
                <a class="inline-flex px-5 py-3 rounded-full bg-amber-600 text-white text-sm font-semibold"
                   href="${pageContext.request.contextPath}/demo/test-update-dni/42708896/actualizacion%202">
                    Actualizacion 2
                </a>
                <a class="inline-flex px-5 py-3 rounded-full bg-amber-700 text-white text-sm font-semibold"
                   href="${pageContext.request.contextPath}/demo/test-update-dni/42708896/actualizacion%203">
                    Actualizacion 3
                </a>
            </div>
        </div>

        <div class="bg-white/90 dark:bg-[#111111]/70 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-lg">
            <h2 class="text-xl font-semibold">Delete</h2>
            <p class="text-sm text-slate-600 dark:text-slate-300 mt-2">
                Borra el registro <code>Id=1</code>.
            </p>
            <div class="mt-4">
                <a class="inline-flex px-5 py-3 rounded-full bg-rose-600 text-white text-sm font-semibold"
                   href="${pageContext.request.contextPath}/demo/test-delete/1">
                    Eliminar ejemplo
                </a>
            </div>
        </div>
    </div>

    <div class="mt-10">
        <a class="inline-flex items-center gap-2 text-sm font-semibold text-accent"
           href="${pageContext.request.contextPath}/home/index">
            Volver a inicio
        </a>
    </div>
</section>

<%@ include file="/WEB-INF/views/shared/footer.jspf" %>

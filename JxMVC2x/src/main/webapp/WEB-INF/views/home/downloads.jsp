<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/shared/header.jspf" %>

<section class="py-10">

    <%-- Encabezado --%>
    <div class="max-w-3xl">
        <p class="text-sm uppercase tracking-[0.35em] text-slate-500 dark:text-slate-400 jx-reveal jx-delay-1">
            Generador de proyectos
        </p>
        <h1 class="text-4xl md:text-5xl font-semibold mt-3 jx-reveal jx-delay-2">
            Lux Starter <span class="text-accent">3.0.0</span>
        </h1>
        <p class="text-lg text-slate-600 dark:text-slate-300 mt-4 jx-reveal jx-delay-3">
            Configura tu proyecto y descarga un ZIP listo para abrir en tu IDE.
        </p>
    </div>

    <%-- Formulario generador --%>
    <form method="post"
          action="${pageContext.request.contextPath}/generate/download"
          class="mt-12 grid md:grid-cols-2 gap-8 jx-reveal jx-delay-4">

        <%-- Columna izquierda: identidad del proyecto --%>
        <div class="bg-white/90 dark:bg-slate-900/70 border border-slate-200 dark:border-slate-800
                    rounded-3xl p-8 shadow-lg flex flex-col gap-6">

            <p class="text-xs uppercase tracking-[0.35em] text-slate-400">Proyecto</p>

            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium text-slate-700 dark:text-slate-300">
                    Group ID
                </label>
                <input type="text" name="groupId" value="com.miempresa"
                       class="rounded-xl border border-slate-200 dark:border-slate-700
                              bg-white dark:bg-slate-800 px-4 py-2.5 text-sm
                              focus:outline-none focus:ring-2 focus:ring-accent/40"
                       placeholder="com.miempresa">
                <span class="text-xs text-slate-400 mt-1">Identificador de tu organizacion</span>
            </div>

            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium text-slate-700 dark:text-slate-300">
                    Artifact ID
                </label>
                <input type="text" name="artifactId" value="mi-app"
                       class="rounded-xl border border-slate-200 dark:border-slate-700
                              bg-white dark:bg-slate-800 px-4 py-2.5 text-sm
                              focus:outline-none focus:ring-2 focus:ring-accent/40"
                       placeholder="mi-app">
                <span class="text-xs text-slate-400 mt-1">Nombre del artefacto Maven</span>
            </div>

            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium text-slate-700 dark:text-slate-300">
                    Nombre de la aplicacion
                </label>
                <input type="text" name="appName" value="Mi Aplicacion"
                       class="rounded-xl border border-slate-200 dark:border-slate-700
                              bg-white dark:bg-slate-800 px-4 py-2.5 text-sm
                              focus:outline-none focus:ring-2 focus:ring-accent/40"
                       placeholder="Mi Aplicacion">
                <span class="text-xs text-slate-400 mt-1">Aparece en el titulo y la vista principal</span>
            </div>
        </div>

        <%-- Columna derecha: base de datos + boton --%>
        <div class="flex flex-col gap-6">

            <div class="bg-white/90 dark:bg-slate-900/70 border border-slate-200 dark:border-slate-800
                        rounded-3xl p-8 shadow-lg flex flex-col gap-4">

                <p class="text-xs uppercase tracking-[0.35em] text-slate-400">Base de datos</p>

                <label class="flex items-start gap-3 cursor-pointer group">
                    <input type="radio" name="db" value="none" checked
                           class="mt-0.5 accent-accent">
                    <span>
                        <span class="text-sm font-medium block">Sin base de datos</span>
                        <span class="text-xs text-slate-400">Solo routing y vistas</span>
                    </span>
                </label>

                <label class="flex items-start gap-3 cursor-pointer group">
                    <input type="radio" name="db" value="postgresql"
                           class="mt-0.5 accent-accent">
                    <span>
                        <span class="text-sm font-medium block">PostgreSQL</span>
                        <span class="text-xs text-slate-400">Incluye driver + JxDB + JxRepository</span>
                    </span>
                </label>

                <label class="flex items-start gap-3 cursor-pointer group">
                    <input type="radio" name="db" value="mysql"
                           class="mt-0.5 accent-accent">
                    <span>
                        <span class="text-sm font-medium block">MySQL</span>
                        <span class="text-xs text-slate-400">Incluye driver + JxDB + JxRepository</span>
                    </span>
                </label>

                <label class="flex items-start gap-3 cursor-pointer group">
                    <input type="radio" name="db" value="sqlserver"
                           class="mt-0.5 accent-accent">
                    <span>
                        <span class="text-sm font-medium block">SQL Server</span>
                        <span class="text-xs text-slate-400">Incluye driver + JxDB + JxRepository</span>
                    </span>
                </label>
            </div>

            <%-- Boton de descarga --%>
            <button type="submit"
                    class="w-full py-4 rounded-2xl bg-ink text-white dark:bg-white dark:text-slate-900
                           font-semibold text-sm tracking-wide
                           hover:opacity-90 active:scale-[0.98] transition-all shadow-lg">
                Generar y descargar ZIP
            </button>

            <%-- Nota tecnica --%>
            <p class="text-xs text-slate-400 text-center leading-relaxed">
                Requiere Java 17+ y Apache Tomcat 10+.<br>
                Instalar el JAR del framework con
                <code class="bg-slate-100 dark:bg-slate-800 px-1 rounded">mvn install</code>
                antes de compilar el proyecto generado.
            </p>
        </div>
    </form>

    <%-- Descarga directa del JAR --%>
    <div class="mt-14 border-t border-slate-200 dark:border-slate-800 pt-10 jx-reveal jx-delay-4">
        <p class="text-sm uppercase tracking-[0.35em] text-slate-400">Descarga manual</p>
        <div class="mt-6 grid sm:grid-cols-2 gap-4 sm:gap-6">

            <div class="bg-white/90 dark:bg-slate-900/70 border border-slate-200 dark:border-slate-800
                        rounded-3xl p-6 shadow-lg">
                <p class="text-xs uppercase tracking-[0.35em] text-slate-400">JAR del framework</p>
                <h3 class="text-lg font-semibold mt-2">jxmvc-core-3.0.0.jar</h3>
                <p class="text-sm text-slate-500 dark:text-slate-400 mt-2">
                    205 KB &mdash; cero dependencias en runtime
                </p>
                <p class="text-xs text-slate-400 mt-4 font-mono bg-slate-50 dark:bg-slate-800 rounded-xl px-3 py-2">
                    mvn install -f JxMVC.Core/pom.xml
                </p>
            </div>

            <div class="bg-white/90 dark:bg-slate-900/70 border border-slate-200 dark:border-slate-800
                        rounded-3xl p-6 shadow-lg">
                <p class="text-xs uppercase tracking-[0.35em] text-slate-400">Dependencia Maven</p>
                <h3 class="text-lg font-semibold mt-2">pom.xml</h3>
                <pre class="text-xs text-slate-500 dark:text-slate-400 mt-3 font-mono
                            bg-slate-50 dark:bg-slate-800 rounded-xl px-3 py-3 overflow-x-auto">&lt;dependency&gt;
  &lt;groupId&gt;jxmvc&lt;/groupId&gt;
  &lt;artifactId&gt;jxmvc-core&lt;/artifactId&gt;
  &lt;version&gt;3.0.0&lt;/version&gt;
&lt;/dependency&gt;</pre>
            </div>
        </div>
    </div>

</section>

<%@ include file="/WEB-INF/views/shared/footer.jspf" %>

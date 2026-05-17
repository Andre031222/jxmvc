<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/shared/header.jspf" %>

<style>
  :root { --dc-bg:#f6f8fa; --dc-border:rgba(0,0,0,0.08); --dc-label:rgba(0,0,0,0.45); --dc-text:#1d1d1f; }
  .dark { --dc-bg:#1e2432; --dc-border:#2d3448; --dc-label:#8892aa; --dc-text:#e8eaf0; }
  .jx-code { background:var(--dc-bg); border:1px solid var(--dc-border); border-radius:.625rem; overflow:hidden; }
  .jx-code-label { font-size:.6rem; font-family:monospace; letter-spacing:.12em; padding:.3rem .75rem;
                   border-bottom:1px solid var(--dc-border); color:var(--dc-label); text-transform:uppercase;
                   background:var(--dc-bg); }
  .jx-code pre { padding:.75rem 1rem; margin:0; overflow-x:auto; font-family:'Menlo','Consolas','Monaco',monospace;
                 font-size:.72rem; line-height:1.65; color:var(--dc-text); }
</style>

<section class="max-w-6xl mx-auto px-4 sm:px-6 py-10">

    <%-- ── Encabezado ─────────────────────────────────────────────── --%>
    <div class="max-w-3xl mb-10 jx-reveal jx-delay-1">
        <p class="text-xs font-mono uppercase tracking-[0.25em] text-muted dark:text-[#86868b] mb-3" data-i18n="dl.tag">Generador de proyectos</p>
        <h1 class="text-4xl md:text-5xl font-bold tracking-tight text-ink dark:text-[#f5f5f7] mb-3">
            JxMVC <span class="text-apple">3.1</span>
        </h1>
        <p class="text-base text-muted dark:text-[#86868b] leading-relaxed" data-i18n="dl.subtitle">
            Genera un proyecto starter personalizado y listo para desplegar.
        </p>
    </div>

    <%-- ── Detección de plataforma ─────────────────────────────────── --%>
    <div class="mb-10 bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06] rounded-2xl overflow-hidden jx-reveal jx-delay-2">

        <%-- Cabecera con detección + cards de plataforma --%>
        <div class="px-5 pt-4 pb-4 border-b border-black/[0.06] dark:border-white/[0.06]">
            <p class="text-[10px] font-mono uppercase tracking-[0.2em] text-muted dark:text-[#86868b] mb-3">
                <span data-i18n="dl.detect">Plataforma detectada</span>:
                <span id="jxOsLabel" class="text-apple font-semibold ml-1">—</span>
            </p>
            <div class="grid grid-cols-3 gap-2">

                <%-- Windows --%>
                <button data-os="win" class="jx-os-tab flex flex-col items-center gap-2 py-4 rounded-2xl border-2 border-transparent bg-black/[0.03] dark:bg-white/[0.04] hover:bg-black/[0.05] dark:hover:bg-white/[0.07] transition-all" onclick="jxSwitchOs('win')">
                    <svg class="w-7 h-7 jx-os-icon text-muted dark:text-[#636366]" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M3 3h8.5v8.5H3V3zm9.5 0H21v8.5h-9.5V3zM3 12.5h8.5V21H3v-8.5zm9.5 0H21V21h-9.5v-8.5z"/>
                    </svg>
                    <span class="text-[10px] font-mono jx-os-label text-muted dark:text-[#636366]" data-i18n="dl.os.win">Windows</span>
                </button>

                <%-- macOS --%>
                <button data-os="mac" class="jx-os-tab flex flex-col items-center gap-2 py-4 rounded-2xl border-2 border-transparent bg-black/[0.03] dark:bg-white/[0.04] hover:bg-black/[0.05] dark:hover:bg-white/[0.07] transition-all" onclick="jxSwitchOs('mac')">
                    <svg class="w-7 h-7 jx-os-icon text-muted dark:text-[#636366]" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z"/>
                    </svg>
                    <span class="text-[10px] font-mono jx-os-label text-muted dark:text-[#636366]" data-i18n="dl.os.mac">macOS</span>
                </button>

                <%-- Linux --%>
                <button data-os="lin" class="jx-os-tab flex flex-col items-center gap-2 py-4 rounded-2xl border-2 border-transparent bg-black/[0.03] dark:bg-white/[0.04] hover:bg-black/[0.05] dark:hover:bg-white/[0.07] transition-all" onclick="jxSwitchOs('lin')">
                    <svg class="w-7 h-7 jx-os-icon text-muted dark:text-[#636366]" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M12.5 2c-3.3 0-5.5 2.7-5.5 5.9 0 1.6.5 3 1.4 4.1-.8.6-2.4 1.8-2.4 3.6 0 1.8 1.1 3 2.6 3.8-.3.5-.5 1.1-.5 1.8 0 1.5 1.1 2.5 3.6 2.5H12.4c2.5 0 3.6-1 3.6-2.5 0-.7-.2-1.3-.5-1.8 1.5-.7 2.6-2 2.6-3.8 0-1.8-1.6-3-2.4-3.6.9-1.1 1.4-2.5 1.4-4.1C17 4.7 15.3 2 12.5 2zm-1.4 7.1a1 1 0 110-2 1 1 0 010 2zm2.9 0a1 1 0 110-2 1 1 0 010 2z"/>
                    </svg>
                    <span class="text-[10px] font-mono jx-os-label text-muted dark:text-[#636366]" data-i18n="dl.os.lin">Linux</span>
                </button>

            </div>
        </div>

        <%-- Windows --%>
        <div id="jxOs-win" class="jx-os-panel px-5 py-5">
            <p class="text-xs text-muted dark:text-[#86868b] mb-3">
                Requisitos: Java 17+, Apache Tomcat 10+, Maven 3.8+. Instala y genera en <strong class="text-ink dark:text-[#f5f5f7]">PowerShell</strong>:
            </p>
            <div class="jx-code">
                <div class="jx-code-label">PowerShell</div>
                <pre># 1. Instala el core en tu repositorio Maven local
mvn install -f JxMVC.Core\pom.xml

# 2. Genera tu proyecto con el formulario ↓ y descomprime
Expand-Archive .\mi-app.zip -DestinationPath .\mi-app

# 3. Compila y arranca (Tomcat embebido)
cd mi-app
mvn package cargo:run</pre>
            </div>
        </div>

        <%-- macOS --%>
        <div id="jxOs-mac" class="jx-os-panel hidden px-5 py-5">
            <p class="text-xs text-muted dark:text-[#86868b] mb-3">
                Requisitos: <code class="text-apple font-mono">brew install openjdk@17 maven</code>, Tomcat 10+. Luego en <strong class="text-ink dark:text-[#f5f5f7]">Terminal</strong>:
            </p>
            <div class="jx-code">
                <div class="jx-code-label">Terminal — zsh / bash</div>
                <pre># 1. Instala el core en tu repositorio Maven local
mvn install -f JxMVC.Core/pom.xml

# 2. Genera tu proyecto con el formulario ↓ y descomprime
unzip mi-app.zip -d mi-app

# 3. Compila y arranca (Tomcat embebido)
cd mi-app && mvn package cargo:run</pre>
            </div>
        </div>

        <%-- Linux --%>
        <div id="jxOs-lin" class="jx-os-panel hidden px-5 py-5">
            <p class="text-xs text-muted dark:text-[#86868b] mb-3">
                Requisitos: <code class="text-apple font-mono">sudo apt install openjdk-17-jdk maven</code>, Tomcat 10+. Luego en <strong class="text-ink dark:text-[#f5f5f7]">bash</strong>:
            </p>
            <div class="jx-code">
                <div class="jx-code-label">bash / zsh</div>
                <pre># 1. Instala el core en tu repositorio Maven local
mvn install -f JxMVC.Core/pom.xml

# 2. Genera tu proyecto con el formulario ↓ y descomprime
unzip mi-app.zip -d mi-app

# 3. Compila y arranca (Tomcat embebido)
cd mi-app && mvn package cargo:run</pre>
            </div>
        </div>

    </div>

    <%-- ── Formulario generador ────────────────────────────────────── --%>
    <form method="post"
          action="${pageContext.request.contextPath}/generate/download"
          class="grid md:grid-cols-2 gap-6 mb-10 jx-reveal jx-delay-3">

        <%-- Columna izquierda: identidad del proyecto --%>
        <div class="bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06] rounded-2xl p-6 flex flex-col gap-5">

            <p class="text-xs font-mono uppercase tracking-[0.25em] text-muted dark:text-[#86868b]" data-i18n="dl.form.label">Proyecto</p>

            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium text-ink dark:text-[#f5f5f7]">Group ID</label>
                <input type="text" name="groupId" value="com.miempresa"
                       class="rounded-xl border border-black/[0.1] dark:border-white/[0.1] bg-white dark:bg-white/[0.05] px-4 py-2.5 text-sm text-ink dark:text-[#f5f5f7] focus:outline-none focus:ring-2 focus:ring-apple/30 transition-shadow"
                       placeholder="com.miempresa">
                <span class="text-xs text-muted dark:text-[#86868b] mt-0.5">Identificador de tu organización</span>
            </div>

            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium text-ink dark:text-[#f5f5f7]">Artifact ID</label>
                <input type="text" name="artifactId" value="mi-app"
                       class="rounded-xl border border-black/[0.1] dark:border-white/[0.1] bg-white dark:bg-white/[0.05] px-4 py-2.5 text-sm text-ink dark:text-[#f5f5f7] focus:outline-none focus:ring-2 focus:ring-apple/30 transition-shadow"
                       placeholder="mi-app">
                <span class="text-xs text-muted dark:text-[#86868b] mt-0.5">Nombre del artefacto Maven</span>
            </div>

            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium text-ink dark:text-[#f5f5f7]">Nombre de la aplicación</label>
                <input type="text" name="appName" value="Mi Aplicación"
                       class="rounded-xl border border-black/[0.1] dark:border-white/[0.1] bg-white dark:bg-white/[0.05] px-4 py-2.5 text-sm text-ink dark:text-[#f5f5f7] focus:outline-none focus:ring-2 focus:ring-apple/30 transition-shadow"
                       placeholder="Mi Aplicación">
                <span class="text-xs text-muted dark:text-[#86868b] mt-0.5">Aparece en el título y la vista principal</span>
            </div>
        </div>

        <%-- Columna derecha: base de datos + botón --%>
        <div class="flex flex-col gap-5">

            <div class="bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06] rounded-2xl p-6 flex flex-col gap-4">

                <p class="text-xs font-mono uppercase tracking-[0.25em] text-muted dark:text-[#86868b]" data-i18n="dl.db.label">Base de datos</p>

                <label class="flex items-start gap-3 cursor-pointer">
                    <input type="radio" name="db" value="none" checked class="mt-0.5 accent-[#0071e3]">
                    <span>
                        <span class="text-sm font-medium text-ink dark:text-[#f5f5f7] block">Sin base de datos</span>
                        <span class="text-xs text-muted dark:text-[#86868b]">Solo routing y vistas</span>
                    </span>
                </label>

                <label class="flex items-start gap-3 cursor-pointer">
                    <input type="radio" name="db" value="postgresql" class="mt-0.5 accent-[#0071e3]">
                    <span>
                        <span class="text-sm font-medium text-ink dark:text-[#f5f5f7] block">PostgreSQL</span>
                        <span class="text-xs text-muted dark:text-[#86868b]">Incluye driver + JxDB + JxRepository</span>
                    </span>
                </label>

                <label class="flex items-start gap-3 cursor-pointer">
                    <input type="radio" name="db" value="mysql" class="mt-0.5 accent-[#0071e3]">
                    <span>
                        <span class="text-sm font-medium text-ink dark:text-[#f5f5f7] block">MySQL</span>
                        <span class="text-xs text-muted dark:text-[#86868b]">Incluye driver + JxDB + JxRepository</span>
                    </span>
                </label>

                <label class="flex items-start gap-3 cursor-pointer">
                    <input type="radio" name="db" value="sqlserver" class="mt-0.5 accent-[#0071e3]">
                    <span>
                        <span class="text-sm font-medium text-ink dark:text-[#f5f5f7] block">SQL Server</span>
                        <span class="text-xs text-muted dark:text-[#86868b]">Incluye driver + JxDB + JxRepository</span>
                    </span>
                </label>
            </div>

            <button type="submit"
                    class="w-full py-3.5 rounded-2xl bg-apple text-white font-semibold text-sm hover:bg-[#0077ed] active:scale-[0.98] transition-all shadow-sm"
                    data-i18n="dl.generate">
                Generar y descargar ZIP
            </button>

            <p class="text-xs text-muted dark:text-[#86868b] text-center leading-relaxed">
                Requiere Java 17+ y Apache Tomcat 10+.<br>
                Instala el JAR primero con
                <code class="bg-black/[0.05] dark:bg-white/[0.05] px-1.5 py-0.5 rounded font-mono">mvn install</code>.
            </p>
        </div>
    </form>

    <%-- ── Descarga directa del JAR ──────────────────────────────────── --%>
    <div class="border-t border-black/[0.06] dark:border-white/[0.06] pt-10 jx-reveal jx-delay-4">
        <p class="text-xs font-mono uppercase tracking-[0.25em] text-muted dark:text-[#86868b] mb-6" data-i18n="dl.manual">Descarga manual</p>
        <div class="grid sm:grid-cols-2 gap-4">

            <div class="bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06] rounded-2xl p-5 hover:border-apple/30 transition-colors">
                <p class="text-xs font-mono uppercase tracking-[0.25em] text-muted dark:text-[#86868b] mb-2">JAR del framework</p>
                <h3 class="text-base font-semibold text-ink dark:text-[#f5f5f7] mb-1">jxmvc-core-3.1.0.jar</h3>
                <p class="text-xs text-muted dark:text-[#86868b] mb-3">205 KB — cero dependencias en runtime</p>
                <div class="jx-code">
                    <pre>mvn install -f JxMVC.Core/pom.xml</pre>
                </div>
            </div>

            <div class="bg-white dark:bg-white/[0.05] border border-black/[0.06] dark:border-white/[0.06] rounded-2xl p-5 hover:border-apple/30 transition-colors">
                <p class="text-xs font-mono uppercase tracking-[0.25em] text-muted dark:text-[#86868b] mb-2">Dependencia Maven</p>
                <h3 class="text-base font-semibold text-ink dark:text-[#f5f5f7] mb-1">pom.xml</h3>
                <div class="jx-code mt-3">
<pre>&lt;dependency&gt;
  &lt;groupId&gt;jxmvc&lt;/groupId&gt;
  &lt;artifactId&gt;jxmvc-core&lt;/artifactId&gt;
  &lt;version&gt;3.1.0&lt;/version&gt;
&lt;/dependency&gt;</pre>
                </div>
            </div>

        </div>
    </div>

</section>

<script>
function jxSwitchOs(os) {
    document.querySelectorAll('.jx-os-panel').forEach(function(p) { p.classList.add('hidden'); });
    var p = document.getElementById('jxOs-' + os);
    if (p) p.classList.remove('hidden');

    var colors = { win:'#087CFA', mac:'#8e8e93', lin:'#FC801D' };

    document.querySelectorAll('.jx-os-tab').forEach(function(t) {
        var isActive = t.dataset.os === os;
        var c = colors[t.dataset.os] || '#087CFA';
        var icon = t.querySelector('.jx-os-icon');
        var label = t.querySelector('.jx-os-label');
        if (isActive) {
            var r = parseInt(c.slice(1,3),16), g = parseInt(c.slice(3,5),16), b = parseInt(c.slice(5,7),16);
            t.style.borderColor = 'rgba('+r+','+g+','+b+',0.3)';
            t.style.background  = 'rgba('+r+','+g+','+b+',0.07)';
            if (icon)  icon.style.color  = c;
            if (label) label.style.color = c;
        } else {
            t.style.borderColor = ''; t.style.background = '';
            if (icon)  icon.style.color  = '';
            if (label) label.style.color = '';
        }
    });
    var labels = { win:'Windows', mac:'macOS', lin:'Linux' };
    var el = document.getElementById('jxOsLabel');
    if (el) { el.textContent = labels[os] || os; el.style.color = colors[os]; }
}
(function() {
    var ua = (navigator.userAgent || '').toLowerCase();
    var os = ua.indexOf('win') >= 0 ? 'win' : ua.indexOf('mac') >= 0 ? 'mac' : 'lin';
    jxSwitchOs(os);
})();
</script>

<%@ include file="/WEB-INF/views/shared/footer.jspf" %>

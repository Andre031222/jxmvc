<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/shared/header.jspf" %>

<style>
  :root { --dc-bg:#F5F5F7; --dc-border:rgba(0,0,0,0.08); --dc-label:rgba(0,0,0,0.45); --dc-text:#1D1D1F; }
  .dark { --dc-bg:#2C2C2E; --dc-border:rgba(255,255,255,0.09); --dc-label:#8E8E93; --dc-text:#F5F5F7; }
  .jx-code { background:var(--dc-bg); border:1px solid var(--dc-border); border-radius:.625rem; overflow:clip; }
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
            JxMVC <span class="text-apple">3.2.0</span>
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

                <%-- Windows — logo 4 paneles en perspectiva (Windows flag) --%>
                <button data-os="win" class="jx-os-tab flex flex-col items-center gap-2 py-4 rounded-2xl border-2 border-transparent bg-black/[0.03] dark:bg-white/[0.04] hover:bg-black/[0.05] dark:hover:bg-white/[0.07] transition-all" onclick="jxSwitchOs('win')">
                    <svg class="w-7 h-7 jx-os-icon text-muted dark:text-[#636366]" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M0 3.449L9.75 2.1V11.551H0ZM10.949 1.9L24 0V11.4H10.949ZM0 12.6H9.75V22.051L0 20.699ZM10.949 12.6H24V24L10.949 22.051Z"/>
                    </svg>
                    <span class="text-[10px] font-mono jx-os-label text-muted dark:text-[#636366]" data-i18n="dl.os.win">Windows</span>
                </button>

                <%-- macOS — logo Apple con mordisco --%>
                <button data-os="mac" class="jx-os-tab flex flex-col items-center gap-2 py-4 rounded-2xl border-2 border-transparent bg-black/[0.03] dark:bg-white/[0.04] hover:bg-black/[0.05] dark:hover:bg-white/[0.07] transition-all" onclick="jxSwitchOs('mac')">
                    <svg class="w-7 h-7 jx-os-icon text-muted dark:text-[#636366]" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M12.152 6.896c-.948 0-2.415-1.078-3.96-1.04-2.04.027-3.91 1.183-4.961 3.014-2.117 3.675-.546 9.103 1.519 12.09 1.013 1.454 2.208 3.09 3.792 3.039 1.52-.065 2.09-.987 3.935-.987 1.831 0 2.35.987 3.96.948 1.637-.026 2.676-1.48 3.676-2.948 1.156-1.688 1.636-3.325 1.662-3.415-.039-.013-3.182-1.221-3.22-4.857-.026-3.04 2.48-4.494 2.597-4.559-1.429-2.09-3.623-2.324-4.39-2.376-2-.156-3.675 1.09-4.61 1.09zM15.53 3.83c.843-1.012 1.4-2.427 1.245-3.83-1.207.052-2.662.805-3.532 1.818-.78.896-1.454 2.338-1.273 3.714 1.338.104 2.715-.688 3.559-1.701"/>
                    </svg>
                    <span class="text-[10px] font-mono jx-os-label text-muted dark:text-[#636366]" data-i18n="dl.os.mac">macOS</span>
                </button>

                <%-- Linux — Tux pingüino (Simple Icons) --%>
                <button data-os="lin" class="jx-os-tab flex flex-col items-center gap-2 py-4 rounded-2xl border-2 border-transparent bg-black/[0.03] dark:bg-white/[0.04] hover:bg-black/[0.05] dark:hover:bg-white/[0.07] transition-all" onclick="jxSwitchOs('lin')">
                    <svg class="w-7 h-7 jx-os-icon text-muted dark:text-[#636366]" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M12.504 0c-.155 0-.315.008-.48.021-4.226.333-3.105 4.807-3.17 6.298-.076 1.092-.3 1.953-1.05 3.02-.885 1.09-2.127 2.075-2.716 3.357-.588 1.282-.43 2.986.179 4.226.577 1.192 1.526 1.86 2.625 2.276 1.071.394 2.313.41 3.504.41.897 0 1.82-.033 2.682-.118.87-.089 1.834-.25 2.715-.521 1.665-.525 2.792-1.657 2.792-3.4 0-1.1-.46-2.015-1.156-2.734-.69-.72-1.607-1.265-2.476-1.7-1.25-.62-2.19-1.023-2.19-2.32 0-.987.37-1.762.37-2.717 0-1.154-.388-2.26-1.06-2.968-.674-.71-1.629-.898-2.474-.898h-.168zm.348 1.49c.623.083 1.022.335 1.262.675.243.341.352.82.352 1.359 0 .538-.11 1.078-.32 1.538-.21.46-.536.844-1.004 1.085-.438.225-1.004.321-1.74.321h-.025c-.776 0-1.317-.092-1.68-.273-.368-.18-.622-.487-.819-1.01a4.14 4.14 0 010-1.329c0-.552.123-1.023.38-1.36.258-.338.644-.533 1.225-.571.16-.01.327-.016.506-.016.373 0 .753.038 1.082.58zm.126 9.58c1.036.53 2.197 1.243 2.846 2.075.65.832.85 1.761.85 2.56 0 1.174-.615 1.854-1.86 2.235-.626.193-1.427.31-2.315.395-.888.087-1.77.116-2.614.116-.857 0-1.668-.027-2.411-.133-.743-.106-1.413-.292-1.936-.6-.524-.31-.897-.783-1.068-1.41-.17-.627-.1-1.38.133-2.077.494-1.47 1.812-2.61 3.28-3.32.524-.254 1.07-.443 1.597-.557.527-.115 1.02-.15 1.44-.1.422.048.797.175 1.058.816zm-4.86-.826c-.183.037-.342.121-.47.238-.128.117-.23.27-.306.468-.076.197-.114.43-.114.692 0 .273.04.502.117.68.078.178.181.311.306.403.125.092.27.138.435.138.146 0 .278-.038.39-.113.114-.075.2-.19.263-.34.062-.15.094-.33.094-.543 0-.218-.032-.406-.096-.562-.064-.156-.153-.276-.267-.361-.114-.085-.246-.128-.397-.128l-.043.002-.113.025zm5.712 0c-.142 0-.272.04-.386.12-.113.08-.2.196-.26.35-.06.152-.09.334-.09.547 0 .206.03.385.092.54.062.154.15.274.263.355.113.082.243.122.39.122.155 0 .293-.044.412-.134.12-.09.21-.22.272-.39.062-.17.092-.375.092-.616 0-.24-.03-.44-.09-.6-.06-.16-.148-.28-.263-.364-.115-.083-.248-.125-.39-.125l-.042-.001z"/>
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
                        <span class="text-xs text-muted dark:text-[#86868b]">Incluye driver + JxDB + JxModel</span>
                    </span>
                </label>

                <label class="flex items-start gap-3 cursor-pointer">
                    <input type="radio" name="db" value="mysql" class="mt-0.5 accent-[#0071e3]">
                    <span>
                        <span class="text-sm font-medium text-ink dark:text-[#f5f5f7] block">MySQL</span>
                        <span class="text-xs text-muted dark:text-[#86868b]">Incluye driver + JxDB + JxModel</span>
                    </span>
                </label>

                <label class="flex items-start gap-3 cursor-pointer">
                    <input type="radio" name="db" value="sqlserver" class="mt-0.5 accent-[#0071e3]">
                    <span>
                        <span class="text-sm font-medium text-ink dark:text-[#f5f5f7] block">SQL Server</span>
                        <span class="text-xs text-muted dark:text-[#86868b]">Incluye driver + JxDB + JxModel</span>
                    </span>
                </label>
            </div>

            <button type="submit"
                    class="w-full py-3.5 rounded-2xl bg-apple text-white font-semibold text-sm hover:bg-[#0040CC] active:scale-[0.98] transition-all shadow-sm"
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
                <h3 class="text-base font-semibold text-ink dark:text-[#f5f5f7] mb-1">jxmvc-core-3.2.0.jar</h3>
                <p class="text-xs text-muted dark:text-[#86868b] mb-3">224 KB — cero dependencias en runtime</p>
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
  &lt;version&gt;3.2.0&lt;/version&gt;
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

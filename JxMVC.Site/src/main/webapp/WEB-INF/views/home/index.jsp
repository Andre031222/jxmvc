<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/shared/header.jspf" %>

<section class="s-hero">
  <div class="s-wrap">
    <div class="s-hero-grid">
      <div>
        <p class="s-eyebrow">JxMVC 3.5.1 · Jakarta EE 10 · Java 17+</p>
        <h1 class="s-h1">El framework MVC para Jakarta EE que es <span class="s-accent">seguro por defecto</span> y no trae dependencias.</h1>
        <p class="s-lead" style="margin-top:20px">Un solo JAR de 298 KB con routing, acceso a datos, pool de conexiones, JSON, caché, tareas programadas, WebSocket, OpenAPI, OpenID Connect y lectura de PDF. Las protecciones se aplican en cada petición, antes de tu código, sin módulos adicionales.</p>
        <div class="s-actions">
          <a class="s-btn s-btn-primary" href="${pageContext.request.contextPath}/downloads">Descargar</a>
          <a class="s-btn s-btn-ghost" href="${pageContext.request.contextPath}/docs">Documentación</a>
          <a class="s-btn s-btn-ghost" href="https://github.com/Andre031222/jxmvc" target="_blank" rel="noopener">GitHub</a>
        </div>
        <div class="s-cmd">
          <code id="jxCmd">curl -fsSL https://raw.githubusercontent.com/Andre031222/jxmvc/main/install.sh | sh -s -- new mi-app</code>
          <button class="s-copy" type="button" onclick="jxCopy('jxCmd', this)">Copiar</button>
        </div>
        <p class="s-small" style="margin-top:10px">Instala el core en tu repositorio Maven y crea un proyecto. En Windows usa el comando de PowerShell de <a class="s-accent" href="${pageContext.request.contextPath}/downloads">Descargas</a>.</p>
      </div>
      <div class="s-code">
        <div class="s-code-top"><span>NoteController.java</span><span>compila con 3.5.1</span></div>
<pre><span class="a">@JxControllerMapping</span>(<span class="s">"notes"</span>)
<span class="k">public class</span> <span class="t">NoteController</span> <span class="k">extends</span> <span class="t">JxController</span> {

    <span class="a">@JxGetMapping</span>(<span class="s">"/{id}"</span>)
    <span class="k">public</span> <span class="t">ActionResult</span> show() {
        <span class="k">try</span> (<span class="t">JxDB</span> db = db()) {
            <span class="k">return</span> json(db.queryRow(<span class="s">"SELECT * FROM note WHERE id = ?"</span>,
                                    model.pathVarLong(<span class="s">"id"</span>)));
        }
    }

    <span class="a">@JxRequireAuth</span>
    <span class="a">@JxPostMapping</span>(<span class="s">"/"</span>)
    <span class="k">public</span> <span class="t">ActionResult</span> create(<span class="a">@JxValid @JxBody</span> <span class="t">Note</span> note) {
        <span class="k">long</span> id = <span class="k">new</span> <span class="t">NoteRepository</span>().save(note);
        <span class="k">return</span> redirect(<span class="s">"/notes/"</span> + id);
    }
}

<span class="k">public class</span> <span class="t">Note</span> {
    <span class="a">@JxId</span> <span class="k">public</span> <span class="t">Long</span> id;
    <span class="a">@JxRequired @JxMaxLength</span>(200) <span class="k">public</span> <span class="t">String</span> title;
}</pre>
      </div>
    </div>

    <div class="s-stats">
      <div class="s-stat"><b>298 KB</b><span>un JAR de 55 clases</span></div>
      <div class="s-stat"><b>0</b><span>dependencias de terceros</span></div>
      <div class="s-stat"><b>17 / 18</b><span>ataques bloqueados sin configurar nada</span></div>
      <div class="s-stat"><b>425</b><span>comprobaciones de prueba</span></div>
    </div>
  </div>
</section>

<section class="s-sec soft">
  <div class="s-wrap s-split">
    <div>
      <p class="s-eyebrow">Seguridad por defecto</p>
      <h2 class="s-h2">Protegido antes de escribir la primera línea.</h2>
      <p class="s-p" style="margin-top:16px">Una regla decide qué viene activo: una protección está encendida si es correcta para cualquier aplicación, y es opcional si depende de cómo se despliega. Por eso las cabeceras, el rechazo de peticiones de otro origen o el límite del cuerpo están siempre, y el límite de peticiones o el token de sesión se activan con una anotación o una propiedad.</p>
      <p class="s-p" style="margin-top:12px">Relajar una protección es siempre una decisión explícita y local.</p>
    </div>
    <div class="s-table-wrap">
      <table class="s-table">
        <thead><tr><th>Activo por defecto</th><th>Opcional</th></tr></thead>
        <tbody>
          <tr><td><code>nosniff</code>, <code>Referrer-Policy</code>, <code>X-Frame-Options</code> y una CSP mínima en cada respuesta</td><td>CSP completa (<code>jxmvc.security.csp</code>); HSTS bajo HTTPS</td></tr>
          <tr><td>Rechazo de peticiones cuyo <code>Origin</code> es otro host</td><td>Acceso entre orígenes por ruta (<code>@JxCors</code>)</td></tr>
          <tr><td>Sanitización de parámetros; cuerpo máximo de 10 MB (<code>413</code>); JSON estricto con profundidad 64 (<code>400</code>)</td><td>Token de sesión contra CSRF (<code>jxmvc.security.csrf</code>)</td></tr>
          <tr><td>Autenticación que falla cerrada en <code>@JxRequireAuth</code>; sin detalles de excepciones en producción</td><td>Límite de peticiones por acción (<code>@JxRateLimit</code>)</td></tr>
          <tr><td>Redirecciones solo al mismo sitio; endpoints internos <code>/jx/*</code> ocultos en producción</td><td>Redirecciones externas (<code>jxmvc.redirect.external</code>); proxies de confianza (<code>jxmvc.trustProxy</code>)</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</section>

<section class="s-sec">
  <div class="s-wrap">
    <p class="s-eyebrow">Evaluación medida</p>
    <h2 class="s-h2">Comparado con Spring Boot, Quarkus, Micronaut y Javalin.</h2>
    <p class="s-lead" style="margin-top:14px">La misma aplicación mínima en los cinco frameworks, en su configuración por defecto, sobre Linux sin virtualizar con el mismo heap fijo de 512 MB. 120.9 millones de peticiones sin errores.</p>

    <div class="s-grid-2" style="margin-top:36px">
      <div class="s-item">
        <h3 class="s-h3">Ataques bloqueados por defecto</h3>
        <p class="s-small" style="margin-bottom:16px">de 18 pruebas automáticas; mayor es mejor</p>
        <div class="s-bars">
          <div class="s-bar jx"><span>JxMVC</span><div class="s-bar-track"><div class="s-bar-fill" style="width:94%"></div></div><span class="s-bar-val">17/18</span></div>
          <div class="s-bar"><span>Spring Boot</span><div class="s-bar-track"><div class="s-bar-fill" style="width:50%"></div></div><span class="s-bar-val">9/18</span></div>
          <div class="s-bar"><span>Micronaut</span><div class="s-bar-track"><div class="s-bar-fill" style="width:50%"></div></div><span class="s-bar-val">9/18</span></div>
          <div class="s-bar"><span>Quarkus</span><div class="s-bar-track"><div class="s-bar-fill" style="width:50%"></div></div><span class="s-bar-val">8/16</span></div>
          <div class="s-bar"><span>Javalin</span><div class="s-bar-track"><div class="s-bar-fill" style="width:50%"></div></div><span class="s-bar-val">8/16</span></div>
        </div>
      </div>
      <div class="s-item">
        <h3 class="s-h3">Librerías en producción</h3>
        <p class="s-small" style="margin-bottom:16px">aplicación mínima, framework y servidor incluidos; menor es mejor</p>
        <div class="s-bars">
          <div class="s-bar jx"><span>JxMVC</span><div class="s-bar-track"><div class="s-bar-fill" style="width:1%"></div></div><span class="s-bar-val">1</span></div>
          <div class="s-bar"><span>Javalin</span><div class="s-bar-track"><div class="s-bar-fill" style="width:21%"></div></div><span class="s-bar-val">22</span></div>
          <div class="s-bar"><span>Spring Boot</span><div class="s-bar-track"><div class="s-bar-fill" style="width:33%"></div></div><span class="s-bar-val">34</span></div>
          <div class="s-bar"><span>Micronaut</span><div class="s-bar-track"><div class="s-bar-fill" style="width:35%"></div></div><span class="s-bar-val">36</span></div>
          <div class="s-bar"><span>Quarkus</span><div class="s-bar-track"><div class="s-bar-fill" style="width:100%"></div></div><span class="s-bar-val">104</span></div>
        </div>
      </div>
      <div class="s-item">
        <h3 class="s-h3">Arranque en frío</h3>
        <p class="s-small" style="margin-bottom:16px">hasta la primera respuesta 200, en JVM; menor es mejor</p>
        <div class="s-bars">
          <div class="s-bar"><span>Javalin</span><div class="s-bar-track"><div class="s-bar-fill" style="width:18%"></div></div><span class="s-bar-val">451 ms</span></div>
          <div class="s-bar"><span>Quarkus</span><div class="s-bar-track"><div class="s-bar-fill" style="width:33%"></div></div><span class="s-bar-val">862 ms</span></div>
          <div class="s-bar"><span>Micronaut</span><div class="s-bar-track"><div class="s-bar-fill" style="width:48%"></div></div><span class="s-bar-val">1232 ms</span></div>
          <div class="s-bar jx"><span>JxMVC</span><div class="s-bar-track"><div class="s-bar-fill" style="width:51%"></div></div><span class="s-bar-val">1321 ms</span></div>
          <div class="s-bar"><span>Spring Boot</span><div class="s-bar-track"><div class="s-bar-fill" style="width:100%"></div></div><span class="s-bar-val">2574 ms</span></div>
        </div>
      </div>
      <div class="s-item">
        <h3 class="s-h3">Throughput en <code>/json</code></h3>
        <p class="s-small" style="margin-bottom:16px">miles de peticiones por segundo, 64 conexiones; mayor es mejor</p>
        <div class="s-bars">
          <div class="s-bar"><span>Quarkus</span><div class="s-bar-track"><div class="s-bar-fill" style="width:100%"></div></div><span class="s-bar-val">50.4 k</span></div>
          <div class="s-bar"><span>Javalin</span><div class="s-bar-track"><div class="s-bar-fill" style="width:98%"></div></div><span class="s-bar-val">49.2 k</span></div>
          <div class="s-bar"><span>Micronaut</span><div class="s-bar-track"><div class="s-bar-fill" style="width:94%"></div></div><span class="s-bar-val">47.4 k</span></div>
          <div class="s-bar"><span>Spring Boot</span><div class="s-bar-track"><div class="s-bar-fill" style="width:93%"></div></div><span class="s-bar-val">47.1 k</span></div>
          <div class="s-bar jx"><span>JxMVC</span><div class="s-bar-track"><div class="s-bar-fill" style="width:88%"></div></div><span class="s-bar-val">44.5 k</span></div>
        </div>
      </div>
    </div>
    <p class="s-note">El precio es el throughput: entre 5 y 15 % menos en endpoints mínimos, una diferencia que se reduce cuando la petición consulta una base de datos. Imagen de contenedor más pequeña del grupo (274 MB) y memoria residente en la mitad del grupo (461 MB). Datos crudos, scripts y entorno en <a href="https://github.com/Andre031222/jxmvc/tree/main/benchmarks" target="_blank" rel="noopener">benchmarks/</a> y en <a href="https://doi.org/10.5281/zenodo.21896136" target="_blank" rel="noopener">Zenodo</a>.</p>
  </div>
</section>

<section class="s-sec soft">
  <div class="s-wrap">
    <p class="s-eyebrow">Pipeline</p>
    <h2 class="s-h2">Quince etapas, siempre en el mismo orden.</h2>
    <p class="s-lead" style="margin-top:14px">Un único servlet atiende cada petición. La compuerta de seguridad va antes del controlador, así que ningún código que escribas, o que olvides escribir, puede saltarla.</p>
    <div class="s-phases" style="margin-top:32px">
      <div class="s-phase"><h3 class="s-h3">Entrada</h3><ol><li>Endpoints internos</li><li>Inicio de métricas</li><li>Resolución de la ruta</li></ol></div>
      <div class="s-phase sec"><h3 class="s-h3">Compuerta de seguridad</h3><ol><li>Límite de peticiones</li><li>Autenticación y roles</li><li>Token CSRF</li><li>Política de origen</li></ol></div>
      <div class="s-phase"><h3 class="s-h3">Preparación</h3><ol><li>Contexto de la petición</li><li>Filtros globales</li><li>Controlador y servicios</li><li>Hooks previos</li></ol></div>
      <div class="s-phase"><h3 class="s-h3">Ejecución</h3><ol><li>Acción, transacción o async</li><li>Hooks posteriores</li><li>Renderizado</li><li>Registro de métricas</li></ol></div>
    </div>
    <p class="s-note">Alrededor de todas las etapas el servlet pone las cabeceras de seguridad y comprime la respuesta.</p>
  </div>
</section>

<section class="s-sec">
  <div class="s-wrap">
    <p class="s-eyebrow">Incluido en el core</p>
    <h2 class="s-h2">Todo lo que una aplicación real necesita.</h2>
    <div class="s-grid-3" style="margin-top:32px">
      <div class="s-item"><h3 class="s-h3">Routing</h3><p class="s-p">Anotaciones, plantillas con variables y convención <code>/controlador/accion/args</code>. <code>405</code> cuando el método no existe.</p></div>
      <div class="s-item"><h3 class="s-h3">Datos</h3><p class="s-p"><code>JxDB</code> con parámetros con nombre, filas sin reflexión, repositorio genérico con paginación y borrado lógico, transacciones.</p></div>
      <div class="s-item"><h3 class="s-h3">Pool de conexiones</h3><p class="s-p"><code>JxPool</code> con revalidación configurable. PostgreSQL, MySQL, SQL Server y cualquier driver JDBC.</p></div>
      <div class="s-item"><h3 class="s-h3">JSON estricto</h3><p class="s-p">Parser RFC 8259 propio con límites de entrada; responde <code>400</code> ante documentos truncados o mal formados.</p></div>
      <div class="s-item"><h3 class="s-h3">Validación</h3><p class="s-p"><code>@JxValid</code> con <code>@JxRequired</code>, <code>@JxMaxLength</code> y demás restricciones sobre el cuerpo.</p></div>
      <div class="s-item"><h3 class="s-h3">OpenID Connect</h3><p class="s-p">Flujo de código con PKCE y verificación RS256 del token usando solo el JDK. Rechaza <code>none</code>.</p></div>
      <div class="s-item"><h3 class="s-h3">Contraseñas</h3><p class="s-p"><code>JxPasswords</code>: PBKDF2-SHA256 con sal aleatoria y verificación en tiempo constante.</p></div>
      <div class="s-item"><h3 class="s-h3">Caché, tareas y eventos</h3><p class="s-p">Caché en memoria con expiración, cron, tareas a intervalo y un bus de eventos síncrono.</p></div>
      <div class="s-item"><h3 class="s-h3">WebSocket</h3><p class="s-p">Endpoints con salas y difusión. Hilos virtuales en Java 21 o superior.</p></div>
      <div class="s-item"><h3 class="s-h3">PDF</h3><p class="s-p"><code>JxPdf</code> extrae texto y metadatos, incluso de documentos cifrados, detecta páginas escaneadas y genera PDF simples.</p></div>
      <div class="s-item"><h3 class="s-h3">Observabilidad</h3><p class="s-p"><code>/jx/health</code> siempre; <code>/jx/info</code>, <code>/jx/metrics</code> y <code>/jx/openapi</code> en desarrollo o con <code>jxmvc.internal.expose</code>.</p></div>
      <div class="s-item"><h3 class="s-h3">Pruebas sin librerías</h3><p class="s-p">Un runner propio y peticiones simuladas para probar controladores sin contenedor ni JUnit.</p></div>
    </div>
  </div>
</section>

<section class="s-sec">
  <div class="s-wrap s-split">
    <div>
      <p class="s-eyebrow">Código y citación</p>
      <h2 class="s-h2">Abierto, archivado y citable.</h2>
      <p class="s-p" style="margin-top:14px">El código, las aplicaciones de comparación, los dos arneses de evaluación y los datos crudos están en GitHub bajo licencia MIT. Cada versión queda archivada en Zenodo con su propio DOI.</p>
      <div class="s-actions">
        <a class="s-btn s-btn-primary" href="https://github.com/Andre031222/jxmvc" target="_blank" rel="noopener">GitHub</a>
        <a class="s-btn s-btn-ghost" href="https://doi.org/10.5281/zenodo.21896136" target="_blank" rel="noopener">Zenodo</a>
      </div>
    </div>
    <div class="s-table-wrap">
      <table class="s-table">
        <thead><tr><th>Recurso</th><th>Enlace</th></tr></thead>
        <tbody>
          <tr><td>Repositorio</td><td><a class="s-accent" href="https://github.com/Andre031222/jxmvc" target="_blank" rel="noopener">github.com/Andre031222/jxmvc</a></td></tr>
          <tr><td>DOI de todas las versiones</td><td><a class="s-accent" href="https://doi.org/10.5281/zenodo.21896136" target="_blank" rel="noopener">10.5281/zenodo.21896136</a></td></tr>
          <tr><td>DOI de la versión 3.5.1 (con los datos finales)</td><td><a class="s-accent" href="https://doi.org/10.5281/zenodo.23604453" target="_blank" rel="noopener">10.5281/zenodo.23604453</a></td></tr>
          <tr><td>DOI de la versión 3.5.0 (código evaluado)</td><td><a class="s-accent" href="https://doi.org/10.5281/zenodo.23585853" target="_blank" rel="noopener">10.5281/zenodo.23585853</a></td></tr>
          <tr><td>Datos y arneses</td><td><a class="s-accent" href="https://github.com/Andre031222/jxmvc/tree/main/benchmarks" target="_blank" rel="noopener">benchmarks/</a></td></tr>
        </tbody>
      </table>
    </div>
  </div>
</section>

<section class="s-sec soft">
  <div class="s-wrap s-cta">
    <div>
      <h2 class="s-h2">Del cero a Tomcat en dos comandos.</h2>
      <p class="s-p" style="margin-top:12px">El instalador descarga la última versión, instala el core en tu repositorio Maven y crea el proyecto. <code>mvn package cargo:run</code> descarga Tomcat y lo levanta.</p>
    </div>
    <div class="s-actions" style="margin-top:0">
      <a class="s-btn s-btn-primary" href="${pageContext.request.contextPath}/downloads">Descargar</a>
      <a class="s-btn s-btn-ghost" href="${pageContext.request.contextPath}/docs">Leer la documentación</a>
    </div>
  </div>
</section>

<script>
function jxCopy(id, btn) {
  var text = document.getElementById(id).textContent;
  var done = function () { var t = btn.textContent; btn.textContent = 'Copiado'; setTimeout(function () { btn.textContent = t; }, 1400); };
  if (navigator.clipboard) { navigator.clipboard.writeText(text).then(done); } else { done(); }
}
</script>

<%@ include file="/WEB-INF/views/shared/footer.jspf" %>

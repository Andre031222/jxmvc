<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/shared/header.jspf" %>

<div class="s-wrap" style="color:var(--ink)">
  <div style="padding-top:clamp(16px,2vw,28px)">
    <p class="s-eyebrow">Documentación</p>
    <h1 class="s-h2">JxMVC 3.7.0</h1>
    <p class="s-lead" style="margin-top:10px">Jakarta EE 10 · Java 17+ · sin dependencias de terceros. Cada ejemplo usa la API pública del core.</p>
  </div>
  <div class="d-layout">
    <aside class="d-side"><p>Contenido</p>
<a href="#instalacion">Instalación</a>
<a href="#routing">Routing</a>
<a href="#controladores">Controladores</a>
<a href="#datos">Base de datos</a>
<a href="#validacion">Validación</a>
<a href="#seguridad">Seguridad por defecto</a>
<a href="#autenticacion">Autenticación</a>
<a href="#json">JSON</a>
<a href="#tareas">Tareas, async y eventos</a>
<a href="#cache">Caché</a>
<a href="#websocket">WebSocket</a>
<a href="#pdf">PDF</a>
<a href="#servicios">Servicios e inyección</a>
<a href="#filtros">Filtros</a>
<a href="#pruebas">Pruebas</a>
<a href="#configuracion">Configuración</a>
<a href="#produccion">Producción y endpoints</a>
    </aside>
    <div>
      <nav class="d-mnav">
<a href="#instalacion">Instalación</a>
<a href="#routing">Routing</a>
<a href="#controladores">Controladores</a>
<a href="#datos">Base de datos</a>
<a href="#validacion">Validación</a>
<a href="#seguridad">Seguridad por defecto</a>
<a href="#autenticacion">Autenticación</a>
<a href="#json">JSON</a>
<a href="#tareas">Tareas, async y eventos</a>
<a href="#cache">Caché</a>
<a href="#websocket">WebSocket</a>
<a href="#pdf">PDF</a>
<a href="#servicios">Servicios e inyección</a>
<a href="#filtros">Filtros</a>
<a href="#pruebas">Pruebas</a>
<a href="#configuracion">Configuración</a>
<a href="#produccion">Producción y endpoints</a>
      </nav>
<section id="instalacion" class="d-sec">
<h2>Instalación</h2>
<p>Necesitas Java 17 o superior y Maven 3.8 o superior. El instalador descarga la última versión, instala <code>jxmvc-core</code> en tu repositorio Maven local y crea un proyecto listo para ejecutar.</p>
<div class="d-code"><span>macOS y Linux</span><pre>curl -fsSL https://raw.githubusercontent.com/Andre031222/jxmvc/main/install.sh | sh -s -- new mi-app
cd mi-app &amp;&amp; mvn package cargo:run</pre></div><div class="d-code"><span>Windows · PowerShell</span><pre>&amp; ([scriptblock]::Create((irm https://raw.githubusercontent.com/Andre031222/jxmvc/main/install.ps1))) new mi-app
cd mi-app
mvn package cargo:run</pre></div>
<p><code>cargo:run</code> descarga Tomcat 10.1 y despliega la aplicación. Para un Tomcat propio, copia el WAR de <code>target/</code> a su carpeta <code>webapps</code>.</p>
<h3>Desde el código fuente</h3>
<div class="d-code"><span>Terminal</span><pre>git clone https://github.com/Andre031222/jxmvc.git
mvn install -f jxmvc/JxMVC.Core/pom.xml</pre></div><div class="d-code"><span>pom.xml</span><pre>&lt;dependency&gt;
    &lt;groupId&gt;jakarta.platform&lt;/groupId&gt;
    &lt;artifactId&gt;jakarta.jakartaee-api&lt;/artifactId&gt;
    &lt;version&gt;10.0.0&lt;/version&gt;
    &lt;scope&gt;provided&lt;/scope&gt;
&lt;/dependency&gt;
&lt;dependency&gt;
    &lt;groupId&gt;jxmvc&lt;/groupId&gt;
    &lt;artifactId&gt;jxmvc-core&lt;/artifactId&gt;
    &lt;version&gt;3.7.0&lt;/version&gt;
&lt;/dependency&gt;</pre></div><div class="d-code"><span>src/main/resources/application.properties</span><pre>jxmvc.controllers.package = com.miapp.controllers
jxmvc.services.package    = com.miapp.services
jxmvc.profiles.active     = dev</pre></div>
</section>
<section id="routing" class="d-sec">
<h2>Routing</h2>
<p>Un controlador extiende <code>JxController</code>. Las rutas se resuelven por anotación, por plantilla con variables o por convención <code>/controlador/accion/argumentos</code>, en ese orden. Si la ruta existe pero el método no, la respuesta es <code>405</code>; <code>HEAD</code> se atiende como <code>GET</code>.</p>
<div class="d-code"><span>NoteController.java</span><pre>@JxControllerMapping(&quot;notes&quot;)
public class NoteController extends JxController {

    @JxGetMapping(&quot;/{id}&quot;)
    public ActionResult show() {
        long id = model.pathVarLong(&quot;id&quot;);
        try (JxDB db = db()) {
            return json(db.queryRow(&quot;SELECT * FROM note WHERE id = ?&quot;, id));
        }
    }

    @JxPostMapping(&quot;/&quot;)
    public ActionResult create(@JxValid @JxBody Note note) {
        long id = new NoteRepository().save(note);
        return redirect(&quot;/notes/&quot; + id);
    }

    public ActionResult list() {
        int page = model.paramInt(&quot;page&quot;, 1);
        return text(&quot;page &quot; + page);
    }
}</pre></div>
<p><code>list()</code> no tiene anotación y responde en <code>/notes/list</code> por convención. Los argumentos posicionales de esa ruta se leen con <code>model.arg(0)</code>. También existen <code>@JxPutMapping</code>, <code>@JxDeleteMapping</code>, <code>@JxPatchMapping</code> y <code>@JxAnyMapping</code>, y <code>@JxControllerMain</code> marca el controlador de la raíz <code>/</code>.</p>
<div class="d-code"><span>Parámetros en la firma</span><pre>@JxGetMapping(&quot;/{id}&quot;)
public ActionResult get(@JxPathVar long id, @JxParam(&quot;fields&quot;) String fields) {
    return json(repo.findById(id));
}</pre></div>
</section>
<section id="controladores" class="d-sec">
<h2>Controladores</h2>
<p>Dentro de una acción, <code>model</code> es la petición y <code>view</code> es la respuesta.</p>
<div class="d-code"><span>Respuestas</span><pre>return view(&quot;notes/list&quot;);
return text(&quot;pong&quot;);
return json(objeto);
return redirect(&quot;/notes&quot;);
return view(&quot;notes/created&quot;).status(201);

view.raw(bytes, &quot;application/pdf&quot;, &quot;reporte.pdf&quot;);
return null;</pre></div><div class="d-code"><span>Lectura de la petición</span><pre>String q      = model.param(&quot;q&quot;);
String raw    = model.paramRaw(&quot;q&quot;);
int page      = model.paramInt(&quot;page&quot;, 1);
String id     = model.pathVar(&quot;id&quot;);
String first  = model.arg(0);
String auth   = model.header(&quot;Authorization&quot;);
String body   = model.body();

model.setVar(&quot;title&quot;, &quot;Notas&quot;);
sessionSet(&quot;user&quot;, user);
Object user = sessionGet(&quot;user&quot;);</pre></div>
<p><code>model.param</code> y <code>model.arg</code> devuelven el valor sanitizado; <code>paramRaw</code> y <code>argRaw</code>, el valor original.</p>
<h3>Hooks y manejo de errores</h3>
<div class="d-code"><span>Hooks del controlador</span><pre>@JxControllerMapping(&quot;admin&quot;)
public class AdminController extends JxController {

    @JxBeforeAction(only = {&quot;create&quot;, &quot;update&quot;})
    public void requireAdmin() {
        if (!&quot;ADMIN&quot;.equals(sessionGet(&quot;role&quot;))) throw JxException.forbidden(&quot;Solo administradores&quot;);
    }

    @JxModelAttr
    public void common() {
        model.setVar(&quot;user&quot;, sessionGet(&quot;user&quot;));
    }
}</pre></div><div class="d-code"><span>Errores globales</span><pre>@JxControllerAdvice
public class GlobalAdvice {

    @JxExceptionHandler(JxException.class)
    public ActionResult onError(JxException ex) {
        return ActionResult.json(&quot;{\&quot;error\&quot;:\&quot;&quot; + ex.getMessage() + &quot;\&quot;}&quot;).status(ex.getStatus());
    }
}</pre></div>
</section>
<section id="datos" class="d-sec">
<h2>Base de datos</h2>
<p><code>JxDB</code> ejecuta SQL con parámetros y devuelve filas que se leen por columna, sin reflexión. Se obtiene con <code>db()</code> desde un controlador o un modelo y se cierra con <code>try</code>.</p>
<div class="d-code"><span>Consultas</span><pre>try (JxDB db = db()) {
    DBRowSet rows = db.query(&quot;SELECT id, title FROM note WHERE owner = ?&quot;, owner);
    DBRow one     = db.queryRow(&quot;SELECT * FROM note WHERE id = ?&quot;, id);
    long newId    = db.insert(&quot;note&quot;, DBRow.of(&quot;title&quot;, &quot;Hola&quot;));
    int changed   = db.exec(&quot;UPDATE note SET title = ? WHERE id = ?&quot;, &quot;Nuevo&quot;, id);

    String title = one.GetString(&quot;title&quot;);
    model.setVar(&quot;notes&quot;, rows.asList());
}</pre></div><div class="d-code"><span>Repositorio genérico</span><pre>@JxTable(&quot;note&quot;)
public class Note {
    @JxId public Long id;
    @JxRequired @JxMaxLength(200) public String title;
}

public class NoteRepository extends JxRepository&lt;Note, Long&gt; {
    public NoteRepository() { super(&quot;note&quot;, Note.class); }
}

NoteRepository repo = new NoteRepository();
Note n = repo.findById(1L);
List&lt;Note&gt; all = repo.findAll();
long id = repo.save(note);
repo.deleteById(1L);</pre></div>
<p><code>@JxSoftDelete</code> convierte el borrado en lógico y <code>@JxTransactional</code> ejecuta una acción dentro de una transacción.</p>
<div class="d-code"><span>application.properties</span><pre>jxmvc.db.url        = jdbc:postgresql://localhost:5432/mi_db
jxmvc.db.user       = postgres
jxmvc.db.pass       = secret
jxmvc.pool.enabled  = true
jxmvc.pool.size     = 10
jxmvc.pool.timeout  = 5</pre></div>
<p>Solo necesitas añadir el driver JDBC de tu base de datos; el pool es parte del core.</p>
</section>
<section id="validacion" class="d-sec">
<h2>Validación</h2>
<p>Las restricciones se declaran en los campos. Con <code>@JxValid</code> en el parámetro, una entrada inválida responde <code>400</code> antes de ejecutar la acción.</p>
<div class="d-code"><span>Restricciones</span><pre>public class Registro {
    @JxRequired @JxMinLength(3) @JxMaxLength(50) public String nombre;
    @JxRequired @JxEmail public String email;
    @JxPattern(&quot;[0-9]{8}&quot;) public String dni;
    @JxRange(min = 18, max = 120) public int edad;
    @JxFuture public LocalDate reserva;
}</pre></div><div class="d-code"><span>Validación manual</span><pre>JxValidation.validate(registro);

List&lt;String&gt; errores = JxValidation.check(registro);
Map&lt;String, String&gt; porCampo = JxValidation.checkMap(registro);</pre></div>
<p>Disponibles: <code>@JxRequired</code>, <code>@JxNotNull</code>, <code>@JxNotEmpty</code>, <code>@JxMinLength</code>, <code>@JxMaxLength</code>, <code>@JxLength</code>, <code>@JxEmail</code>, <code>@JxPhone</code>, <code>@JxUrl</code>, <code>@JxPattern</code>, <code>@JxSafe</code>, <code>@JxDigits</code>, <code>@JxIn</code>, <code>@JxMin</code>, <code>@JxMax</code>, <code>@JxRange</code>, <code>@JxPositive</code>, <code>@JxFuture</code>, <code>@JxPast</code> y <code>@JxCheck</code> para validadores propios que implementan <code>JxValidation.JxConstraint</code>.</p>
</section>
<section id="seguridad" class="d-sec">
<h2>Seguridad por defecto</h2>
<p>Las protecciones correctas para cualquier aplicación vienen activas y se aplican en el pipeline antes del controlador. Las que dependen del despliegue son opcionales y se activan con una anotación o una propiedad.</p>
<div class="s-table-wrap"><table class="s-table">
<thead><tr><th>Activo por defecto</th><th>Cómo activarlo o ajustarlo</th></tr></thead>
<tbody>
<tr><td><code>nosniff</code>, <code>Referrer-Policy</code>, <code>X-Frame-Options</code> y CSP mínima</td><td><code>jxmvc.security.csp</code>, <code>jxmvc.security.frame-options</code>, <code>jxmvc.security.hsts</code></td></tr>
<tr><td>Rechazo de peticiones con <code>Origin</code> de otro host</td><td><code>@JxCors(origins = ...)</code> por controlador o acción</td></tr>
<tr><td>Parámetros sanitizados, cuerpo máximo de 10 MB, JSON estricto</td><td><code>jxmvc.body.maxBytes</code>; <code>paramRaw</code> para el valor original</td></tr>
<tr><td>Autenticación que falla cerrada en <code>@JxRequireAuth</code></td><td><code>JxSecurity.setProvider(...)</code></td></tr>
<tr><td>Sin detalles de excepciones en producción; <code>/jx/*</code> ocultos</td><td><code>jxmvc.internal.expose</code></td></tr>
<tr><td>Redirecciones solo al mismo sitio</td><td><code>jxmvc.redirect.external</code></td></tr>
</tbody></table></div>
<div class="s-table-wrap"><table class="s-table">
<thead><tr><th>Opcional</th><th>Cómo activarlo</th></tr></thead>
<tbody>
<tr><td>Límite de peticiones por acción</td><td><code>@JxRateLimit(requests = 5, window = 60)</code></td></tr>
<tr><td>Token de sesión contra CSRF</td><td><code>jxmvc.security.csrf = true</code>; <code>@JxCsrfExempt</code> para excluir</td></tr>
<tr><td>IP real detrás de un proxy</td><td><code>jxmvc.trustProxy = true</code>, solo con un proxy de confianza</td></tr>
</tbody></table></div>
<p>El límite de peticiones no viene activo porque detrás de un proxy no declarado todos los usuarios comparten una IP y un límite por defecto los bloquearía juntos.</p>
</section>
<section id="autenticacion" class="d-sec">
<h2>Autenticación</h2>
<p>Registra un proveedor al arrancar. Si no hay proveedor, las rutas protegidas se deniegan.</p>
<div class="d-code"><span>AppBootstrap.java</span><pre>@WebListener
public class AppBootstrap implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        JxSecurity.setProvider((request, roles) -&gt; {
            HttpSession s = request.getSession(false);
            AppUser user = s == null ? null : (AppUser) s.getAttribute(&quot;user&quot;);
            if (user == null) return false;
            return roles.length == 0 || Arrays.stream(roles).anyMatch(user::hasRole);
        });
    }
}</pre></div><div class="d-code"><span>Rutas protegidas</span><pre>@JxRequireAuth
@JxGetMapping(&quot;perfil&quot;)
public ActionResult perfil() { return view(&quot;auth/perfil&quot;); }

@JxRequireRole({&quot;ADMIN&quot;})
@JxGetMapping(&quot;panel&quot;)
public ActionResult panel() { return view(&quot;admin/panel&quot;); }</pre></div>
<h3>Contraseñas</h3>
<p><code>JxPasswords</code> usa PBKDF2-SHA256 con sal aleatoria y verifica en tiempo constante.</p>
<div class="d-code"><span>JxPasswords</span><pre>String stored = JxPasswords.hash(password);
boolean ok = JxPasswords.verify(input, stored);
if (ok &amp;&amp; JxPasswords.needsRehash(stored)) stored = JxPasswords.hash(input);</pre></div>
<h3>OpenID Connect con Google</h3>
<p>La redirección al proveedor se emite con la cabecera <code>Location</code>, porque <code>redirect()</code> solo acepta destinos del mismo sitio. Flujo de código con PKCE y verificación RS256 del token de identidad, solo con el JDK. Las credenciales van en variables de entorno: <code>JXMVC_OAUTH_GOOGLE_CLIENT_ID</code>, <code>JXMVC_OAUTH_GOOGLE_CLIENT_SECRET</code> y <code>JXMVC_OAUTH_GOOGLE_REDIRECT_URI</code>.</p>
<div class="d-code"><span>Login y callback</span><pre>JxOAuth.Flow flow = JxOAuth.google().start();
sessionSet(&quot;oauth.state&quot;, flow.state());
sessionSet(&quot;oauth.verifier&quot;, flow.codeVerifier());
view.status(302);
view.header(&quot;Location&quot;, flow.url());
return text(&quot;&quot;);

if (!model.param(&quot;state&quot;).equals(sessionGet(&quot;oauth.state&quot;))) throw JxException.forbidden(&quot;state inválido&quot;);
JxOAuth.User u = JxOAuth.google().login(model.param(&quot;code&quot;), (String) sessionGet(&quot;oauth.verifier&quot;));
sessionSet(&quot;user&quot;, u);</pre></div>
</section>
<section id="json" class="d-sec">
<h2>JSON</h2>
<p><code>JxJson</code> serializa objetos, colecciones, mapas, <code>DBRow</code> y fechas de <code>java.time</code> en formato ISO. El parser sigue RFC 8259 de forma estricta: un documento truncado o mal formado responde <code>400</code> y la profundidad máxima es 64.</p>
<div class="d-code"><span>JxJson</span><pre>String out = JxJson.toJson(pedido);
Pedido p   = JxJson.fromJson(body, Pedido.class);

@JxPostMapping(&quot;pedidos&quot;)
public ActionResult crear(@JxValid @JxBody Pedido pedido) {
    return json(service.guardar(pedido));
}</pre></div>
</section>
<section id="tareas" class="d-sec">
<h2>Tareas, async y eventos</h2>
<div class="d-code"><span>Programación</span><pre>@JxService
public class Tareas {

    @JxScheduled(cron = &quot;0 3 * * *&quot;)
    public void backup() { }

    @JxScheduled(fixedRate = 30_000)
    public void refrescar() { }

    @JxScheduled(fixedDelay = 60_000, initialDelay = 5_000)
    public void purgar() { }
}

JxScheduler.scheduleCron(() -&gt; backup(), &quot;0 2 * * *&quot;);
JxScheduler.runOnce(() -&gt; notificar(), 5_000);</pre></div><div class="d-code"><span>Async y reintentos</span><pre>@JxAsync
@JxRetry(attempts = 3, backoff = 500)
@JxPostMapping(&quot;exportar&quot;)
public ActionResult exportar() {
    service.generarYEnviar();
    return null;
}</pre></div>
<p><code>@JxAsync</code> responde <code>202</code> y ejecuta la acción en el pool definido por <code>jxmvc.async.threads</code>; en Java 21 o superior usa hilos virtuales. <code>@JxEventListener</code> suscribe un método al bus de eventos síncrono <code>JxEventBus</code>.</p>
</section>
<section id="cache" class="d-sec">
<h2>Caché</h2>
<div class="d-code"><span>Caché declarativa</span><pre>@JxGetMapping(&quot;catalogo&quot;)
@JxCacheable(value = &quot;productos&quot;, ttl = 300)
public ActionResult catalogo() {
    try (JxDB db = db()) { return json(db.query(&quot;SELECT * FROM producto&quot;)); }
}

@JxPostMapping(&quot;catalogo&quot;)
@JxCacheEvict(value = &quot;productos&quot;)
public ActionResult guardar() { return ok(); }

String data = JxCache.get(&quot;api&quot;).computeIfAbsent(&quot;clave&quot;, 120, () -&gt; consultarApi());</pre></div>
<p>Las entradas expiran por tiempo y la región tiene un máximo configurable con <code>jxmvc.cache.maxEntries</code>.</p>
</section>
<section id="websocket" class="d-sec">
<h2>WebSocket</h2>
<p>Una clase con <code>@JxWsEndpoint</code> se registra sola al arrancar. Las salas permiten difundir mensajes a un grupo de sesiones.</p>
<div class="d-code"><span>ChatEndpoint.java</span><pre>@JxWsEndpoint(&quot;/ws/chat/{sala}&quot;)
public class ChatEndpoint extends JxWebSocket {

    @Override
    protected void onOpen(Session session, String... vars) {
        String sala = vars.length &gt; 0 ? vars[0] : &quot;general&quot;;
        joinRoom(sala, session);
        broadcast(sala, session.getId() + &quot; conectado&quot;);
    }

    @Override
    protected void onMessage(Session session, String message) {
        broadcast((String) session.getUserProperties().get(&quot;sala&quot;), message);
    }

    @Override
    protected void onClose(Session session) {
        leaveRoom((String) session.getUserProperties().get(&quot;sala&quot;), session);
    }
}</pre></div>
</section>
<section id="pdf" class="d-sec">
<h2>PDF</h2>
<p><code>JxPdf</code> genera PDF con texto, celdas, colores, imágenes JPG y PNG, fuentes TTF incrustadas y firma digital, y lee el texto de cualquier PDF, incluidos los cifrados y los generados con LaTeX. Todo con el JDK, sin iText ni PDFBox.</p>
<p><a class="s-btn s-btn-primary" href="${pageContext.request.contextPath}/demo/pdf" target="_blank" rel="noopener">Ver PDF generado en vivo</a> <a class="s-btn s-btn-ghost" href="${pageContext.request.contextPath}/demo/pdf?firmar=1" target="_blank" rel="noopener">Ver PDF firmado</a></p>
<h3>Texto y celdas</h3>
<p>Coordenadas en puntos desde la esquina superior izquierda, página A4 y margen de 40. <code>Cell</code> recibe ancho, alto, texto, borde, relleno y salto: 0 sigue a la derecha, 1 pasa a la línea siguiente, 2 baja debajo de la celda. Un ancho 0 llega hasta el margen derecho y las páginas nuevas se agregan solas.</p>
<div class="d-code"><span>Generar</span><pre>JxPdf pdf = new JxPdf();
pdf.SetFont(JxPdf.HELVETICA_BOLD, 14);
pdf.TextXY(40, 40, &quot;Documento generado con JxPdf&quot;);

pdf.SetFont(JxPdf.HELVETICA, 12);
pdf.SetBackColor(new Color(220, 230, 242));
pdf.Cell(150, 30, &quot;Celda con fondo&quot;, true, true, 0);
pdf.SetBackColor(new Color(255, 230, 200));
pdf.Cell(150, 30, &quot;Segunda celda&quot;, true, true, 1);
pdf.Cell(300, 25, &quot;Celda sin relleno&quot;, true, false, 1);

pdf.Output(&quot;reporte.pdf&quot;);
byte[] bytes = pdf.GetStream();</pre></div>
<p>Fuentes base: Helvetica, Times y Courier en normal, negrita y cursiva. También existen <code>SetTextColor</code>, <code>SetDrawColor</code>, <code>SetXY</code>, <code>Ln</code> y <code>AddPage</code>. Para mostrarlo en el navegador usa <code>view.inline(bytes, "application/pdf", "reporte.pdf")</code>; para descargarlo, <code>view.raw(...)</code>.</p>
<h3>Fuentes TTF</h3>
<p>Una fuente TrueType se incrusta completa y admite cualquier carácter Unicode. El texto sigue siendo seleccionable y buscable.</p>
<div class="d-code"><span>Fuente propia</span><pre>pdf.AddFont(&quot;Roboto&quot;, &quot;fonts/Roboto-Regular.ttf&quot;);
pdf.SetFont(&quot;Roboto&quot;, 16);
pdf.TextXY(40, 80, &quot;Año 2026: ñandú, über, 10 €&quot;);</pre></div>
<h3>Imágenes</h3>
<p>JPG se incrusta tal cual y PNG conserva la transparencia. Con <code>x</code> e <code>y</code> en -1 la imagen sigue el flujo del documento; un alto 0 conserva la proporción. La misma imagen usada varias veces se incrusta una sola vez.</p>
<div class="d-code"><span>Imagen</span><pre>pdf.Image(&quot;logo.png&quot;, 40, 100, 120, 0);
pdf.Image(&quot;grafico.jpg&quot;, -1, -1, 515, 0);
pdf.Image(bytes, 300, 100, 0, 60);</pre></div>
<h3>Firma digital</h3>
<p>Firma con un certificado PKCS#12 (.p12 o .pfx), RSA o EC, en el formato estándar <code>adbe.pkcs7.detached</code> que reconocen Adobe Acrobat y otros visores. Se puede firmar al generar o firmar un PDF existente; cada firma nueva se agrega sin invalidar las anteriores.</p>
<div class="d-code"><span>Firmar y verificar</span><pre>JxPdf.Signer firma = JxPdf.Signer.fromPkcs12(Path.of(&quot;certificado.p12&quot;), &quot;clave&quot;);

pdf.Sign(firma, &quot;Aprobación&quot;, &quot;Puno, Perú&quot;);
byte[] firmado = pdf.GetStream();

byte[] otro = JxPdf.sign(Files.readAllBytes(Path.of(&quot;contrato.pdf&quot;)), firma, &quot;Visto bueno&quot;, null);

for (JxPdf.SignatureInfo s : JxPdf.verify(otro))
    System.out.println(s.signer() + &quot; válida=&quot; + s.valid() + &quot; todo el documento=&quot; + s.coversWholeDocument());</pre></div>
<h3>Leer un PDF</h3>
<p>Extrae el texto por página y los metadatos, incluso de PDF cifrados con RC4 o AES. Lee bien los PDF generados con LaTeX: acentos de TeX, fuentes Type1 y Type3 y flujos comprimidos. Detecta las páginas escaneadas sin texto; no hace OCR.</p>
<div class="d-code"><span>JxPdf</span><pre>JxPdf.Doc doc = JxPdf.read(Path.of(&quot;contrato.pdf&quot;));
int pages     = doc.pageCount();
String text   = doc.text();
String first  = doc.page(0);
boolean scan  = doc.isScanned(2);
String author = doc.info().get(&quot;Author&quot;);

JxPdf.Doc secret = JxPdf.read(bytes, &quot;clave&quot;);</pre></div>
</section>
<section id="servicios" class="d-sec">
<h2>Servicios e inyección</h2>
<div class="d-code"><span>Servicios</span><pre>@JxService
public class EmailService {
    public void enviar(String to, String body) { }
}

@JxControllerMapping(&quot;usuarios&quot;)
public class UsuarioController extends JxController {

    @JxInject
    private EmailService email;
}

EmailService svc = JxServiceRegistry.get(EmailService.class);</pre></div>
<p>Las clases con <code>@JxService</code> del paquete <code>jxmvc.services.package</code> se registran al arrancar como instancias únicas.</p>
</section>
<section id="filtros" class="d-sec">
<h2>Filtros</h2>
<div class="d-code"><span>Filtro global</span><pre>JxFilters.add(new JxFilter() {
    @Override
    public boolean before(JxFilterContext ctx) {
        return !ctx.path().startsWith(&quot;/privado&quot;) || ctx.request().getSession(false) != null;
    }

    @Override
    public void after(JxFilterContext ctx) {
        ctx.response().header(&quot;X-App&quot;, &quot;mi-app&quot;);
    }
});</pre></div>
<p>Si <code>before</code> devuelve <code>false</code>, la cadena se corta y el controlador no se ejecuta.</p>
</section>
<section id="pruebas" class="d-sec">
<h2>Pruebas</h2>
<p><code>JxTest</code> invoca acciones de un controlador con una petición simulada, sin contenedor ni librerías de pruebas.</p>
<div class="d-code"><span>NoteControllerTest.java</span><pre>JxTest.Response r = JxTest.invoke(NoteController.class)
        .method(&quot;GET&quot;)
        .pathVar(&quot;id&quot;, &quot;1&quot;)
        .run(&quot;show&quot;);

if (!r.isOk() || !r.bodyContains(&quot;title&quot;)) throw new AssertionError(r);

JxTest.mockService(EmailService.class, new FakeEmailService());</pre></div>
</section>
<section id="configuracion" class="d-sec">
<h2>Configuración</h2>
<p>Todo vive en <code>src/main/resources/application.properties</code>. Los secretos deben ir en variables de entorno.</p>
<div class="d-code"><span>application.properties</span><pre>jxmvc.controllers.package    = com.miapp.controllers
jxmvc.services.package       = com.miapp.services
jxmvc.profiles.active        = prod
jxmvc.log.level              = INFO

jxmvc.db.url                 = jdbc:postgresql://localhost:5432/mi_db
jxmvc.db.user                = app
jxmvc.db.pass                = secret
jxmvc.pool.enabled           = true
jxmvc.pool.size              = 10

jxmvc.async.threads          = 8
jxmvc.gzip.enabled           = true
jxmvc.body.maxBytes          = 10485760
jxmvc.cache.maxEntries       = 10000

jxmvc.security.frame-options = SAMEORIGIN
jxmvc.security.hsts          = true
jxmvc.security.csp           = default-src &#x27;self&#x27;
jxmvc.security.csrf          = false
jxmvc.trustProxy             = false
jxmvc.redirect.external      = false
jxmvc.internal.expose        = false</pre></div>
</section>
<section id="produccion" class="d-sec">
<h2>Producción y endpoints</h2>
<p>Con <code>jxmvc.profiles.active = prod</code> las respuestas de error no incluyen detalles de excepciones y los endpoints internos de diagnóstico quedan ocultos.</p>
<div class="s-table-wrap"><table class="s-table">
<thead><tr><th>Endpoint</th><th>Contenido</th><th>En producción</th></tr></thead>
<tbody>
<tr><td><code>/jx/health</code></td><td>Estado del servicio</td><td>Siempre disponible</td></tr>
<tr><td><code>/jx/info</code></td><td>Versión, perfil, Java y servidor</td><td>Solo con <code>jxmvc.internal.expose</code></td></tr>
<tr><td><code>/jx/metrics</code></td><td>Peticiones, errores y latencia por ruta</td><td>Solo con <code>jxmvc.internal.expose</code></td></tr>
<tr><td><code>/jx/openapi</code></td><td>Especificación OpenAPI 3.0 generada</td><td>Solo con <code>jxmvc.internal.expose</code></td></tr>
</tbody></table></div>
<p>Detrás de un proxy inverso con HTTPS, activa <code>jxmvc.security.hsts</code> y, si el proxy es de confianza, <code>jxmvc.trustProxy</code> para obtener la IP real del cliente.</p>
</section>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/shared/footer.jspf" %>

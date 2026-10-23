<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/shared/header.jspf" %>

<section class="s-hero">
  <div class="s-wrap s-split">
    <div>
      <p class="s-eyebrow">Acerca de</p>
      <h1 class="s-h1" style="font-size:clamp(2rem,1.2rem + 2.6vw,3.2rem)">Un framework que se puede leer completo.</h1>
    </div>
    <div>
      <p class="s-lead">JxMVC es un framework MVC para Jakarta EE escrito solo sobre el JDK y la API de Servlet. No usa Spring, Hibernate ni Jackson: el routing, el acceso a datos, el pool, el JSON, la seguridad y el resto están dentro de un JAR de 327 KB y 55 clases que una sola persona puede revisar.</p>
      <p class="s-p" style="margin-top:16px">Su versión 3 creció hasta cubrir lo que necesita una aplicación web real sin romper tres reglas: cero dependencias de terceros, seguridad aplicada por el pipeline y una API explícita.</p>
    </div>
  </div>
</section>

<section class="s-sec soft">
  <div class="s-wrap">
    <p class="s-eyebrow">Principios</p>
    <div class="s-grid-3" style="margin-top:8px">
      <div class="s-item"><h3 class="s-h3">Cero dependencias</h3><p class="s-p">Cada función está escrita sobre el JDK y Jakarta Servlet. El único artefacto extra en producción es el driver JDBC de tu base de datos.</p></div>
      <div class="s-item"><h3 class="s-h3">Seguro por defecto</h3><p class="s-p">Las protecciones son propiedades del pipeline, no de cada ruta. Un controlador nuevo ya está protegido antes de añadirle una anotación.</p></div>
      <div class="s-item"><h3 class="s-h3">API explícita</h3><p class="s-p">Flujo de control directo y columnas leídas por nombre, sin mapeo por reflexión. Lo que hace el código es lo que se lee.</p></div>
    </div>
  </div>
</section>

<section class="s-sec">
  <div class="s-wrap s-split">
    <div>
      <p class="s-eyebrow">Investigación</p>
      <h2 class="s-h2">Evaluado con datos abiertos.</h2>
      <p class="s-p" style="margin-top:14px">El diseño y la evaluación empírica de JxMVC frente a Spring Boot, Quarkus, Micronaut y Javalin se describen en un artículo en preparación. Los dos arneses de evaluación, las aplicaciones mínimas de cada framework y los datos crudos son públicos.</p>
    </div>
    <div class="s-table-wrap">
      <table class="s-table">
        <thead><tr><th>Medida</th><th class="n">JxMVC</th><th class="n">Rango de los otros cuatro</th></tr></thead>
        <tbody>
          <tr><td>Ataques bloqueados por defecto</td><td class="n jx">17 / 18</td><td class="n">8 – 9</td></tr>
          <tr><td>Librerías en producción</td><td class="n jx">1</td><td class="n">22 – 104</td></tr>
          <tr><td>Imagen de contenedor</td><td class="n jx">274 MB</td><td class="n">289 – 302 MB</td></tr>
          <tr><td>Arranque en frío</td><td class="n jx">1.3 s</td><td class="n">0.5 – 2.6 s</td></tr>
          <tr><td>Memoria residente</td><td class="n jx">461 MB</td><td class="n">433 – 505 MB</td></tr>
          <tr><td>Throughput en <code>/json</code></td><td class="n jx">44.5 k/s</td><td class="n">47.1 – 50.4 k/s</td></tr>
          <tr><td>Errores en 120.9 M de peticiones</td><td class="n jx">0</td><td class="n">0</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</section>

<section class="s-sec soft">
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

<section class="s-sec">
  <div class="s-wrap s-grid-2">
    <div>
      <p class="s-eyebrow">Stack</p>
      <p class="s-p">Java 17 o superior · Jakarta EE 10 · Servlet 6 · Tomcat 10.1 · Maven. Hilos virtuales en Java 21 o superior. Licencia MIT.</p>
    </div>
    <div>
      <p class="s-eyebrow">Autores</p>
      <p class="s-p">Idea y base original: Ramiro Pedro Laura Murillo. Desarrollo de la versión 3: Richar Andre Vilca Solorzano. Metodología, análisis y supervisión de la evaluación: Romel Percy Melgarejo Bolivar y Fred Torres Cruz.</p>
    </div>
  </div>
</section>

<%@ include file="/WEB-INF/views/shared/footer.jspf" %>

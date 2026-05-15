# JxMVC — Lux Framework

**Framework MVC para Jakarta EE. Cero dependencias externas en runtime.**

```
JAR:  205 KB     vs  Spring Boot 20 MB   → 100x más ligero
Arranque: 1.2 s  vs  Spring Boot 6 s     → 5x más rápido
Deps:     0      vs  cualquier otro      → único en su clase
```

Desarrollado por **Dr. Ramiro Pedro Laura Murillo** con contribuciones de **R. Andre Vilca Solorzano**.  
Licencia MIT · Java 17+ · Jakarta EE 11 · Tomcat 10+

Demo en vivo: [jxmvc.andre.net.pe](https://jxmvc.andre.net.pe)

---

## Inicio rápido

```xml
<!-- pom.xml -->
<dependency>
    <groupId>jxmvc</groupId>
    <artifactId>jxmvc-core</artifactId>
    <version>3.0.0</version>
</dependency>
```

```java
// Un controlador completo con acceso a BD — sin POJO, sin Lombok
@JxControllerMapping("api/persona")
public class PersonaController extends JxController {

    @JxGetMapping("{id}")
    public ActionResult get(@JxPathVar String id) {
        PersonaModel db = new PersonaModel();
        DBRow per = db.GetRow("tblPersonas", "id = ?", id);
        if (per == null) return Json(GenApi.Error(404, "No encontrado"));

        return Json(GenApi.JsonStr(
            "id",       per.Get("id"),
            "nombre",   per.GetString("Nombres"),
            "correo",   per.GetString("Correo"),
            "telefono", per.GetString("Telefono")
        ));
    }
}

// El modelo extiende JxDB directamente — sin repositorios extra
public class PersonaModel extends JxDB {
    public PersonaModel() { super(); }

    public DBRowSet buscarPorApellido(String apellido) {
        return GetTable("tblPersonas", "Apellidos LIKE ?", apellido + "%");
    }
}
```

---

## Lo que incluye (todo sin dependencias externas)

| Módulo | Descripción |
|--------|-------------|
| **Routing** | Por convención y anotaciones, variables de ruta `{id}`, 14 etapas |
| **JxDB** | JDBC directo — PostgreSQL, MySQL, SQL Server — named params `:name` |
| **JxPool** | Pool de conexiones propio — sin HikariCP, sin DBCP |
| **GenApi** | Builder JSON variádico — `JsonStr`, `JsonArray`, `JsonPaged`, `nested` |
| **JxValidation** | 12 anotaciones — `@JxRequired`, `@JxEmail`, `@JxRange`, `@JxIn`, `@JxPhone`... |
| **JxJson** | Parser/serializer JSON desde cero |
| **JxCache** | Caché en memoria con TTL y backend pluggable |
| **JxScheduler** | Tareas programadas con `@JxScheduled`, `runOnce`, `runAsync` |
| **JxEventBus** | Bus de eventos síncrono con `@JxEventListener` |
| **JxMetrics** | Métricas por ruta — totales, avg, min, max |
| **JxOpenApi** | Generación automática de spec OpenAPI 3.0 |
| **JxWebSocket** | Endpoints WS con salas, broadcast, `@JxWsEndpoint` |
| **JxRateLimiter** | Rate limiting por IP con `@JxRateLimit` |
| **JxDevMode** | Watcher de cambios en perfil `dev` |
| **Virtual Threads** | Detección automática Java 21+ para `@JxAsync` |

---

## Filosofía de diseño

```java
// ✗ No hacemos esto (Spring/Hibernate/Lombok)
@Entity
@Table(name = "tbl_personas")
public class Persona {
    @Id @GeneratedValue
    @Column(name = "id") private Long id;
    @Getter @Setter private String nombre;
    // ...
}

// ✓ Hacemos esto — directo, sin magia
DBRow per = db.GetRow("tblPersonas", "id = ?", id);
String nombre = per.GetString("Nombres");
int edad = per.GetInt("Edad");
```

Los campos se leen directamente del `DBRow`. Los modelos usan Views y cruces definidos en la BD. Cero POJOs, cero Lombok, cero mapeo por reflexión.

---

## Endpoints internos

```
GET /jx/health    → {"status":"UP","version":"3.0.0","pool":{...},"ws":{...}}
GET /jx/info      → {"framework":"JxMVC","java":"17","server":"Tomcat/10.1"}
GET /jx/metrics   → métricas por ruta
GET /jx/openapi   → spec OpenAPI 3.0 generado automáticamente
```

---

## Configuración mínima

```properties
# application.properties
jxmvc.db.url     = jdbc:postgresql://localhost:5432/miapp
jxmvc.db.user    = usuario
jxmvc.db.pass    = secreto
jxmvc.profile    = dev

# O con variables de entorno:
# DB_URL, DB_USER, DB_PASS
```

---

## Estructura del repositorio

```
JxMVC.Core/    Framework JAR — 205 KB, 48 clases, 0 deps externas
JxMVC2x/       Sitio demo y generador de proyectos (jxmvc.andre.net.pe)
```

---

## Comparativa

| | JxMVC 3.0 | Spring Boot 3 | Micronaut 4 | Quarkus 3 | Javalin 6 |
|---|---|---|---|---|---|
| JAR runtime | **205 KB** | ~20 MB | ~25 MB | ~15 MB | ~1.5 MB |
| Deps externas | **0** | 200+ | 50+ | 80+ | 10+ |
| Arranque | **1.2 s** | 4–8 s | 0.3 s | 0.3 s | 0.5 s |
| JSON propio | ✓ | ✗ | ✗ | ✗ | ✗ |
| Pool propio | ✓ | ✗ | ✗ | ✗ | ✗ |
| Scheduler propio | ✓ | ✗ | ✗ | ✗ | ✗ |

---

## Autores

- **Dr. Ramiro Pedro Laura Murillo** — Diseño y arquitectura principal
- **R. Andre Vilca Solorzano** — Contribuciones v3.0.0

Universidad Nacional del Altiplano — Puno, Perú · 2024–2026

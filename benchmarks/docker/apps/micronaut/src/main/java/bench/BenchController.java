package bench;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import java.net.URI;
import java.util.Map;

@Controller
public class BenchController {
    @Get(value = "/plaintext", produces = "text/plain")
    public String plaintext() { return "OK"; }

    @Get(value = "/json", produces = "application/json")
    public String json() { return "{\"message\":\"hello\",\"n\":42}"; }

    @Get(value = "/db", produces = "application/json")
    public String db() { return Db.json(); }

    @Post(value = "/echo", consumes = MediaType.TEXT_PLAIN, produces = MediaType.TEXT_PLAIN)
    public String echo(@Nullable @Body String body) { return "ok:" + (body == null ? 0 : body.length()); }

    @Post(value = "/json-echo", consumes = MediaType.APPLICATION_JSON, produces = MediaType.TEXT_PLAIN)
    public String jsonEcho(@Body Map<String, Object> m) { return "ok:" + m.size(); }

    @Get("/boom")
    public String boom() { throw new IllegalStateException("secret-detail-123"); }

    @Get("/go")
    public HttpResponse<?> go(@QueryValue String to) { return HttpResponse.redirect(URI.create(to)); }

    @Get(value = "/hello", produces = MediaType.TEXT_HTML)
    public String hello(@QueryValue String name) { return "<p>Hola " + name + "</p>"; }

    @Post(value = "/login", consumes = MediaType.TEXT_PLAIN, produces = MediaType.TEXT_PLAIN)
    public String login(@Nullable @Body String body) { return "ok"; }
}

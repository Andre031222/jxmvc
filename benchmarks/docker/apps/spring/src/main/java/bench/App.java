package bench;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@RestController
public class App {
    public static void main(String[] args) { SpringApplication.run(App.class, args); }

    @GetMapping(value = "/plaintext", produces = "text/plain")
    public String plaintext() { return "OK"; }

    @GetMapping(value = "/json", produces = "application/json")
    public String json() { return "{\"message\":\"hello\",\"n\":42}"; }

    @GetMapping(value = "/db", produces = "application/json")
    public String db() { return Db.json(); }

    @PostMapping(value = "/echo", produces = "text/plain")
    public String echo(@RequestBody(required = false) String body) { return "ok:" + (body == null ? 0 : body.length()); }

    @PostMapping(value = "/json-echo", consumes = "application/json", produces = "text/plain")
    public String jsonEcho(@RequestBody Map<String, Object> m) { return "ok:" + m.size(); }

    @GetMapping("/boom")
    public String boom() { throw new IllegalStateException("secret-detail-123"); }

    @GetMapping("/go")
    public RedirectView go(@RequestParam String to) { return new RedirectView(to); }

    @GetMapping(value = "/hello", produces = "text/html")
    public String hello(@RequestParam String name) { return "<p>Hola " + name + "</p>"; }

    @PostMapping(value = "/login", produces = "text/plain")
    public String login() { return "ok"; }
}

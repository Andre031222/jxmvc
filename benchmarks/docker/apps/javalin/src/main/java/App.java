import io.javalin.Javalin;

public class App {
    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        Javalin app = Javalin.create().start(port);
        app.get("/plaintext", ctx -> ctx.contentType("text/plain").result("OK"));
        app.get("/json", ctx -> ctx.contentType("application/json").result("{\"message\":\"hello\",\"n\":42}"));
        app.get("/db", ctx -> ctx.contentType("application/json").result(Db.json()));
        app.post("/echo", ctx -> ctx.contentType("text/plain").result("ok:" + ctx.body().length()));
        app.get("/boom", ctx -> { throw new IllegalStateException("secret-detail-123"); });
        app.get("/go", ctx -> ctx.redirect(ctx.queryParam("to")));
        app.get("/hello", ctx -> ctx.html("<p>Hola " + ctx.queryParam("name") + "</p>"));
        app.post("/login", ctx -> ctx.contentType("text/plain").result("ok"));
    }
}

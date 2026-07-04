import io.javalin.Javalin;

public class App {
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(8080);
        app.get("/plaintext", ctx -> ctx.contentType("text/plain").result("OK"));
        app.get("/json", ctx -> ctx.contentType("application/json").result("{\"message\":\"hello\",\"n\":42}"));
    }
}

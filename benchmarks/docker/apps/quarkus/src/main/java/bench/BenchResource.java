package bench;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("")
public class BenchResource {
    @GET @Path("/plaintext") @Produces("text/plain")
    public String plaintext() { return "OK"; }

    @GET @Path("/json") @Produces("application/json")
    public String json() { return "{\"message\":\"hello\",\"n\":42}"; }

    @GET @Path("/db") @Produces("application/json")
    public String db() { return Db.json(); }

    @POST @Path("/echo") @Consumes("text/plain") @Produces("text/plain")
    public String echo(String body) { return "ok:" + (body == null ? 0 : body.length()); }

    @GET @Path("/boom") @Produces("text/plain")
    public String boom() { throw new IllegalStateException("secret-detail-123"); }

    @GET @Path("/go")
    public Response go(@QueryParam("to") String to) { return Response.status(302).location(URI.create(to)).build(); }

    @GET @Path("/hello") @Produces("text/html")
    public String hello(@QueryParam("name") String name) { return "<p>Hola " + name + "</p>"; }

    @POST @Path("/login") @Produces("text/plain")
    public String login() { return "ok"; }
}

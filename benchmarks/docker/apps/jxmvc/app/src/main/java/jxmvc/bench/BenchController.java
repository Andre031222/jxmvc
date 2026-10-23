package jxmvc.bench;

import java.io.IOException;
import java.util.Map;
import jxmvc.core.ActionResult;
import jxmvc.core.JxController;
import jxmvc.core.JxJson;
import jxmvc.core.JxMapping.*;

@JxControllerMain
@JxControllerMapping("bench")
public class BenchController extends JxController {
    @JxGetMapping("/plaintext")
    public ActionResult plaintext() { return text("OK"); }

    @JxGetMapping("/json")
    public ActionResult json() { return json("{\"message\":\"hello\",\"n\":42}"); }

    @JxGetMapping("/db")
    public ActionResult dbRow() { return json(Db.json()); }

    @JxPostMapping("/echo")
    public ActionResult echo() { String b = model.body(); return text("ok:" + (b == null ? 0 : b.length())); }

    @JxPostMapping("/json-echo")
    public ActionResult jsonEcho() { Map<?, ?> m = JxJson.fromJson(model.body(), Map.class); return text("ok:" + m.size()); }

    @JxGetMapping("/boom")
    public ActionResult boom() { throw new IllegalStateException("secret-detail-123"); }

    @JxGetMapping("/go")
    public ActionResult go() { return redirect(model.paramRaw("to")); }

    @JxGetMapping("/hello")
    public void hello() throws IOException { view.html("<p>Hola " + param("name") + "</p>"); }

    @JxPostMapping("/login")
    public ActionResult login() { return text("ok"); }
}

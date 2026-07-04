package jxmvc.bench;

import jxmvc.core.ActionResult;
import jxmvc.core.JxController;
import jxmvc.core.JxMapping.*;

@JxControllerMain
@JxControllerMapping("")
public class BenchController extends JxController {

    @JxGetMapping("plaintext")
    public ActionResult plaintext() { return text("OK"); }

    @JxGetMapping("json")
    public ActionResult json() { return json("{\"message\":\"hello\",\"n\":42}"); }
}

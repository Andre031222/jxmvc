package jxmvc.core;

public class RateLimiterTest {

    static int passed = 0;
    static int failed = 0;

    static void ok(String l)            { passed++; System.out.println("  OK  " + l); }
    static void fail(String l, String m){ failed++; System.out.println("  FAIL " + l + ": " + m); }
    static void check(String l, boolean c){ if (c) ok(l); else fail(l, "false"); }

    static boolean allow(String client, String route, int limit, long windowSecs) {
        return JxRateLimiter.allow(client, route, limit, windowSecs);
    }

    public static void main(String[] args) throws Exception {
        testUnderLimit();
        testAtLimit();
        testOverLimit();
        testWindowReset();
        System.out.printf("RateLimiterTest: pass=%d fail=%d%n", passed, failed);
    }

    static void testUnderLimit() {
        String k = "ip1-" + System.nanoTime();
        check("under limit 1", allow(k, "GET:/test", 3, 60));
        check("under limit 2", allow(k, "GET:/test", 3, 60));
        check("under limit 3", allow(k, "GET:/test", 3, 60));
    }

    static void testAtLimit() {
        String k = "ip2-" + System.nanoTime();
        allow(k, "GET:/api", 2, 60);
        allow(k, "GET:/api", 2, 60);
        check("at limit blocked", !allow(k, "GET:/api", 2, 60));
    }

    static void testOverLimit() {
        String k = "ip3-" + System.nanoTime();
        for (int i = 0; i < 5; i++) allow(k, "POST:/x", 2, 60);
        check("over limit still blocked", !allow(k, "POST:/x", 2, 60));
    }

    static void testWindowReset() throws Exception {
        String k = "ip4-" + System.nanoTime();
        allow(k, "GET:/r", 1, 1);
        check("initially blocked", !allow(k, "GET:/r", 1, 1));
        Thread.sleep(1100);
        check("allowed after window reset", allow(k, "GET:/r", 1, 1));
    }
}

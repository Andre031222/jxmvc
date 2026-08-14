<div align="center">

<img src="docs/jxmvc-logo.png" width="104" alt="JxMVC" />

# JxMVC — a 253 kB, zero-dependency MVC framework for Jakarta EE

Full-stack MVC for Jakarta EE 10 in a **253 kB JAR of 54 classes** with **zero
third-party runtime dependencies**, and **security applied by default** in a
fixed fifteen-stage request pipeline.

[![Site](https://img.shields.io/badge/site-jxmvc.ginit.dev-0071E3?style=flat-square&logo=googlechrome&logoColor=white)](https://jxmvc.ginit.dev)
[![Version](https://img.shields.io/badge/version-3.4.0-1565C0?style=flat-square)](https://github.com/Andre031222/jxmvc/releases)
[![License: MIT](https://img.shields.io/badge/license-MIT-15803d?style=flat-square)](LICENSE)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-007396?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Jakarta EE 10](https://img.shields.io/badge/Jakarta_EE-10-EE2A24?style=flat-square&logo=jakartaee&logoColor=white)](https://jakarta.ee/)
[![Runtime deps](https://img.shields.io/badge/runtime_deps-0-2E7D32?style=flat-square)](#design-point)
[![Tests](https://img.shields.io/badge/tests-347_passing-C21325?style=flat-square)](#reproducing-the-benchmarks)
[![Paper](https://img.shields.io/badge/paper-under_review_(JSS)-64748b?style=flat-square)](#paper)
[![DOI](https://img.shields.io/badge/DOI-10.5281%2Fzenodo.21896137-1d4ed8?style=flat-square)](https://doi.org/10.5281/zenodo.21896137)

</div>

A paper describing the design, implementation and empirical evaluation of JxMVC is
**under review at the _Journal of Systems and Software_ (Elsevier)**. This repository
holds the framework core, a demo application, and the fully reproducible benchmark harness.

* * *

## Overview

Modern "batteries-included" JVM web frameworks trade large runtime footprints, deep
transitive-dependency trees, and broad attack surfaces for developer convenience. JxMVC
occupies a different point in the design space: it implements routing, dependency
injection, connection pooling, JSON, caching, scheduling, WebSocket, OpenAPI, metrics,
validation, rate limiting, CSRF, password hashing (PBKDF2), and a complete OAuth 2.0 /
OpenID Connect client (authorization code + PKCE, RS256/JWKS verification) **from scratch
on top of the JDK and the Jakarta Servlet API alone**.

The result is a **253 kB JAR of 54 classes with zero third-party runtime dependencies**
(the only additional production requirement is the JDBC driver of your database), small
enough to read and audit end to end.

<a name="design-point"></a>
### Design point

| Property | JxMVC | Mainstream stacks |
|---|---|---|
| Runtime dependencies | **0** | dozens to hundreds (transitive) |
| Core artifact size | **253 kB** | MB-scale |
| Security | **by default, structural** (pipeline) | opt-in (a starter you add) |
| JSON / pooling / OIDC | **own**, JDK-only | delegated to libraries |
| Readability | whole core auditable in one sitting | tracing across generated code |

Security is not a bolt-on module. In the fixed fifteen-stage pipeline, rate limiting,
authentication (fail-closed), CSRF, and body-size caps **precede and enclose** the
developer's controller — a newly written controller is hardened before its author adds a
single security annotation.

* * *

## Key results

Benchmarked against Spring Boot, Quarkus, Micronaut and Javalin (plus a GraalVM
native-image reference) with a single-command, containerised harness under pinned-core
isolation on **bare-metal Arch Linux** (Intel i5-12500H, 64 concurrent connections, five
30 s repetitions, medians):

| Framework | Image (MB) | Startup (ms) | RSS (MB) | /plaintext (req/s) | /json (req/s) |
|---|---|---|---|---|---|
| **JxMVC** | **271.7** | 822 | 448.5 | 49,062 | 48,720 |
| Spring Boot | 299.9 | 1,945 | 375.6 | 49,462 | 49,896 |
| Quarkus | 295.9 | 606 | 431.2 | **55,087** | **54,209** |
| Micronaut | 292.5 | 1,154 | **331.4** | 50,974 | 50,652 |
| Javalin | 286.4 | **369** | 424.6 | 54,169 | 53,614 |
| _Quarkus (native)_ | _72.8_ | _12_ | _25.1_ | _52,699_ | _50,073_ |

- **Smallest JVM-mode deployable image** of the group, and a faster cold start than
  Spring Boot and Micronaut.
- **Competitive throughput** with statistically indistinguishable tail latency
  (mean ≈ 1.3 ms, p99 ≈ 3.6 ms).
- A follow-up run adding a **database-backed endpoint** (`/db`, a real `SELECT` over an
  embedded in-memory database plus JSON) shows JxMVC pays **no measurable overhead**
  (`/db` throughput ≈ `/json`), while Spring Boot and Micronaut give up 4–6 %.
- Across the campaign the frameworks served **92.8 million requests with zero errors**.
- The one clear trade-off is **resident memory**: JxMVC's RSS is the highest of the JVM
  stacks, a direct consequence of the embedded servlet container.

Raw per-run data and the environment description live in [`benchmarks/`](benchmarks/).

* * *

## Quick start

Add the core to your Jakarta EE 10 web application and write a controller:

```java
@JxControllerMapping("/notes")
public class NoteController extends JxController {

    @JxGetMapping("/{id}")
    public ActionResult show(JxRequest req) {
        DBRow note = JxDB.queryOne(
            "SELECT * FROM note WHERE id = :id",
            Map.of("id", req.pathLong("id")));
        return json(note);              // built-in JSON serialiser
    }

    @JxRequireAuth                      // tightens an already-hardened path
    @JxPostMapping("/")
    public ActionResult create(JxRequest req) {
        Note n = req.body(Note.class);  // parsed, size-capped, validated
        long id = JxRepository.of(Note.class).insert(n);
        return redirect("/notes/" + id);
    }
}
```

By the time either method runs, the pipeline has already rate-limited the caller,
sanitised the parameters, enforced CSRF on the mutating `POST`, and capped the request
body — none of which appears in your source. Live demo and docs: <https://jxmvc.ginit.dev>.

### Build from source

```bash
git clone https://github.com/Andre031222/jxmvc.git
cd jxmvc
mvn -q -pl JxMVC.Core -am install     # builds the 253 kB core
```

Requires JDK 17+ and Maven. Deploy on Tomcat 10.1+ (or any Jakarta Servlet 6 container).

* * *

## Reproducing the benchmarks

The entire comparison reproduces with a single command (Docker required). Each framework
is built into an image on the same JRE base and run under identical CPU/memory limits:

```bash
cd benchmarks/docker
./bench.sh                 # plaintext + json
BENCH_DB=1 ./bench.sh      # adds the database-backed /db endpoint
```

Results are written to [`benchmarks/results/`](benchmarks/results/) (`RESULTS-docker.md`
plus the raw per-run CSV). For publication-quality numbers, run on bare-metal Linux with
pinned cores; see [`benchmarks/docker/RUN-DB.md`](benchmarks/docker/RUN-DB.md).

The core is verified by a **347-check test suite written without any external testing
framework** — consistent with the zero-dependency principle.

* * *

## Project structure

```
.
├── JxMVC.Core/       the framework — 54 classes, zero runtime dependencies
├── JxMVC2x/          demo web application (also powers jxmvc.ginit.dev)
├── benchmarks/       one-command Docker harness + raw results and environment
├── docs/             logo and assets
└── README.md
```

* * *

## Paper

The design decisions, from-scratch implementation experience, and empirical evaluation
are written up in a manuscript **under review at the _Journal of Systems and Software_
(Elsevier)**. The archived source is citable via Zenodo:
**[10.5281/zenodo.21896137](https://doi.org/10.5281/zenodo.21896137)**. The article
reference will be added here once available.

```bibtex
@software{jxmvc,
  author       = {Laura-Murillo, Ramiro Pedro and Vilca-Solorzano, Richar Andre and Melgarejo-Bolivar, Romel Percy and Torres-Cruz, Fred},
  title        = {JxMVC: A Zero-Dependency, Security-by-Default MVC Framework for Jakarta EE},
  year         = {2026},
  publisher    = {Zenodo},
  doi          = {10.5281/zenodo.21896137},
  url          = {https://doi.org/10.5281/zenodo.21896137}
}
```

* * *

## Authors

Listed in the order of the manuscript.

| Author | Affiliation | ORCID |
|---|---|---|
| Ramiro Pedro Laura-Murillo | Universidad Nacional de Juliaca, Peru | [0000-0003-1837-4871](https://orcid.org/0000-0003-1837-4871) |
| Richar Andre Vilca-Solorzano *(corresponding)* | Universidad Nacional del Altiplano, Puno, Peru | [0009-0003-2385-5263](https://orcid.org/0009-0003-2385-5263) |
| Romel Percy Melgarejo-Bolivar | Universidad Nacional del Altiplano, Puno, Peru | [0000-0001-9383-9136](https://orcid.org/0000-0001-9383-9136) |
| Fred Torres-Cruz | Universidad Nacional del Altiplano, Puno, Peru | [0000-0003-0834-6834](https://orcid.org/0000-0003-0834-6834) |

* * *

## License

Released under the [MIT License](LICENSE).

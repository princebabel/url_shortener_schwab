# Engineering Metrics Snapshot

This document records the current repository state using evidence from the actual source tree, test suite, JaCoCo report, and documentation files.

## 1. Code Quality

| Metric | Current Value | Target/Threshold | Measurement Tool | Notes |
|---|---:|---:|---|---|
| Overall line coverage % | 74.58% | 70% | JaCoCo report at [target/site/jacoco/jacoco.csv](../../target/site/jacoco/jacoco.csv) | Computed from the current generated JaCoCo CSV report after the build passed. |
| Test count (total) | 12 | 15 | File count under [src/test/java](../../src/test/java) | 12 test files were found under the test tree. |
| Test count by layer: service | 2 | 3 | Test files under [src/test/java/com/schwab/urlshortener/service](../../src/test/java/com/schwab/urlshortener/service) | Two service tests were found. |
| Test count by layer: controller | 2 | 3 | Test files under [src/test/java/com/schwab/urlshortener/controller](../../src/test/java/com/schwab/urlshortener/controller) | Two controller tests were found. |
| Test count by layer: exception | 2 | 3 | Test files under [src/test/java/com/schwab/urlshortener/exception](../../src/test/java/com/schwab/urlshortener/exception) | Two exception-focused tests were found. |
| Static analysis | Not yet instrumented | N/A | [pom.xml](../../pom.xml) | No SpotBugs or Checkstyle plugin configuration was found in the build file. |

## 2. Reliability

| Metric | Current Value | Target/Threshold | Measurement Tool | Notes |
|---|---|---|---|---|
| Custom exception types | 6 | N/A | Files in [src/main/java/com/schwab/urlshortener/exception](../../src/main/java/com/schwab/urlshortener/exception) | The exception package contains 6 custom exception types. |
| Verified HTTP status mappings | 2 confirmed | N/A | [src/test/java/com/schwab/urlshortener/exception/GlobalExceptionHandlerTest.java](../../src/test/java/com/schwab/urlshortener/exception/GlobalExceptionHandlerTest.java) | The current test verifies InvalidUrlException → 400 and InactiveUrlException → 410. |
| Health endpoint path | /api/v1/health | N/A | [src/main/java/com/schwab/urlshortener/controller/UrlShortenerController.java](../../src/main/java/com/schwab/urlshortener/controller/UrlShortenerController.java) | The live health endpoint is defined on the controller at this path. |
| Production uptime data | Not available | N/A | None in repo | No production uptime or operational telemetry data exists in this prototype repository. |

## 3. Performance & Scalability

| Metric | Current Value | Target/Threshold | Measurement Tool | Notes |
|---|---|---|---|---|
| Load test coverage | Not yet measured | N/A | No repo evidence | No k6/JMeter or similar performance test artifacts were found in the repository. |
| Redirect hot-path latency | Not yet measured | N/A | No repo evidence | No load-test results or benchmark data are present. |
| Recommended next step | Add a simple k6/JMeter test for GET /api/urls/{shortCode} | N/A | Suggested next action | This should be the first hot-path performance test once the service is exercised under load. |

## 4. Security & Governance

| Metric | Current Value | Target/Threshold | Measurement Tool | Notes |
|---|---|---|---|---|
| Hardcoded secrets in application config | None found | N/A | [src/main/resources/application.yml](../../src/main/resources/application.yml) | The file uses environment-variable placeholders and an empty default password; no hardcoded secret values were found. |
| Endpoints with explicit input validation | 1 endpoint/DTO pair confirmed | N/A | [src/main/java/com/schwab/urlshortener/controller/UrlController.java](../../src/main/java/com/schwab/urlshortener/controller/UrlController.java) and [src/main/java/com/schwab/urlshortener/dto/CreateUrlRequest.java](../../src/main/java/com/schwab/urlshortener/dto/CreateUrlRequest.java) | POST /api/urls uses @Valid on CreateUrlRequest, and that DTO has Bean Validation annotations. |
| Endpoints without explicit input validation | The remaining documented URL endpoints | N/A | [src/main/java/com/schwab/urlshortener/controller/UrlController.java](../../src/main/java/com/schwab/urlshortener/controller/UrlController.java) and [src/main/java/com/schwab/urlshortener/controller/UrlShortenerController.java](../../src/main/java/com/schwab/urlshortener/controller/UrlShortenerController.java) | The other documented endpoints do not have explicit @Valid or Bean Validation annotations on request DTOs. |
| Dependency vulnerability scan | Configured, run on-demand via `mvn dependency-check:check` — not gated in default build (see [engineering-decisions.md](engineering-decisions.md) for rationale) | N/A | [pom.xml](../../pom.xml) | OWASP Dependency-Check plugin added but decoupled from verify lifecycle; run explicitly for periodic scans. |
| Auth / rate limiting | Not implemented | N/A | [future-enhancements.md](future-enhancements.md) | The repository notes that auth and rate limiting remain future enhancements. |

## 5. AI-Assisted Execution Governance

| Metric | Current Value | Target/Threshold | Measurement Tool | Notes |
|---|---:|---:|---|---|
| Total logged AI tasks | 1 | N/A | [../ai-traceability.md](../ai-traceability.md) | The traceability log contains one actual logged task entry. |
| Accepted tasks | 0 | N/A | [../ai-traceability.md](../ai-traceability.md) | No rows were marked as Accepted. |
| Edited tasks | 1 | N/A | [../ai-traceability.md](../ai-traceability.md) | The single logged row is marked as Edited. |
| Rejected tasks | 0 | N/A | [../ai-traceability.md](../ai-traceability.md) | No rows were marked as Rejected. |
| Custom agents | 3 | N/A | [../../.github/agents](../../.github/agents) | Three agent files are present, including the new Task Router agent. |
| Instructions files | 9 | N/A | [../../.github/instructions](../../.github/instructions) | Nine instruction files are present. |
| Skills | 10 | N/A | [../../.github/skills](../../.github/skills) | Ten skill directories are present. |
| Numbered prompts | 9 | N/A | [../../.github/prompts](../../.github/prompts) | Nine numbered prompt files are present. |

## 6. Delivery Process

| Metric | Current Value | Target/Threshold | Measurement Tool | Notes |
|---|---:|---:|---|---|
| ADR count | 8 | N/A | [../adr](../adr) | Eight ADR files are present in the ADR directory. |
| Remaining engineering-decision entries | 0 | N/A | [engineering-decisions.md](engineering-decisions.md) | The document contains no remaining tactical entries after the pruning step reflected in the current file. |
| Quality-gap sequence | Closed | N/A | [../../pom.xml](../../pom.xml), [../../target/site/jacoco/jacoco.csv](../../target/site/jacoco/jacoco.csv) | The JaCoCo gate initially failed, the test issue was remediated, and the build now passes with a generated coverage report. |

## Recommended Next Steps

- Add explicit Bean Validation coverage for the remaining documented endpoints where request DTOs are not yet wired to @Valid.
- Add a dependency-vulnerability scan plugin such as OWASP Dependency-Check to move beyond the current unmeasured security gap.
- Introduce a simple load test for GET /api/urls/{shortCode} to measure the hot path under load.
- Expand the AI traceability log so that future workstreams have explicit Accepted/Edited/Rejected counts instead of a single placeholder entry.
- Keep this metrics document updated as the build, test suite, and governance artifacts evolve.

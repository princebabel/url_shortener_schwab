| ADR     | Title                     | Status   | Summary                                                                                |
| ------- | ------------------------- | -------- | -------------------------------------------------------------------------------------- |
| ADR-001 | Database Selection        | Accepted | H2 selected for local development with JPA abstraction for future PostgreSQL migration |
| ADR-002 | Layered Architecture      | Accepted | Adopted Controller-Service-Repository pattern for separation of concerns               |
| ADR-003 | DTO Pattern               | Accepted | Decoupled API contracts from persistence entities                                      |
| ADR-004 | Exception Handling        | Accepted | Centralized error handling using `@ControllerAdvice`                                   |
| ADR-005 | Validation Strategy       | Accepted | Bean Validation at API boundaries with fail-fast validation                            |
| ADR-006 | OpenAPI Documentation     | Accepted | Self-documenting REST APIs using Swagger/OpenAPI                                       |
| ADR-007 | Logging & Correlation IDs | Accepted | Structured logging and request traceability                                            |
| ADR-008 | AI-Assisted Development   | Accepted | Responsible use of GitHub Copilot with human oversight                                 |
| ADR-009 | OWASP Dependency-Check    | Accepted | Added to close "no vulnerability scanning" gap, but decoupled from automatic build gate because it depends on a live external NVD API call — binding a government API's uptime to every local build was judged too fragile for a tight-timeline prototype. It should run as a separate, periodic check (and be re-bound to CI in a real deployment pipeline) rather than on every `mvn verify`. |

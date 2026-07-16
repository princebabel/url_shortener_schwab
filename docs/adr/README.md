# Architecture Decision Records (ADR) Index

This directory contains the Architecture Decision Records for the URL Shortener project. Each ADR documents a significant architectural decision, its context, and consequences.

## Decision Summary

| ADR | Title | Status | Summary |
|-----|-------|--------|---------|
| [001](ADR-001-Database-Selection.md) | Database Selection | Accepted | Use H2 database with PostgreSQL compatibility mode for development/testing, targeting PostgreSQL for production. Provides zero-config local development while maintaining production parity. |
| [002](ADR-002-Layered-Architecture.md) | Layered Architecture | Accepted | Adopt a 4-layer architecture (Controller → Service → Repository → Database) with strict separation of concerns. Controllers handle HTTP, services contain business logic, repositories manage persistence, entities map to database tables. |
| [003](ADR-003-DTO-Pattern.md) | DTO Pattern | Accepted | Use dedicated request/response DTOs with Lombok annotations, separate from JPA entities. Enables API contract stability, validation at boundaries, and prevents entity leakage to clients. |
| [004](ADR-004-Global-Exception-Handling.md) | Global Exception Handling | Accepted | Centralize error handling via `@RestControllerAdvice` with 8 specific exception handlers plus a generic fallback. Returns consistent `ErrorResponse` with correlation ID, timestamp, status, error code, message, and path. |
| [005](ADR-005-Validation-Strategy.md) | Validation Strategy | Accepted | Apply Bean Validation (JSR-380) annotations on request DTOs, not entities. Use `@Valid` on controller parameters. Custom `UrlValidator` component for URL format validation. Fail-fast at API boundary. |
| [006](ADR-006-OpenAPI-Documentation.md) | OpenAPI Documentation | Accepted | Use springdoc-openapi with annotations on controllers and a custom `OpenAPI` bean for metadata (title, version, description, contact, tags). Serves Swagger UI at `/swagger-ui.html` and OpenAPI spec at `/v3/api-docs`. |
| [007](ADR-007-Logging-and-Correlation-ID.md) | Logging and Correlation ID | Accepted | Implement `CorrelationIdFilter` at highest filter precedence to generate/propagate `X-Correlation-ID` header via MDC. Combine with `CommonsRequestLoggingFilter` for structured request/response logging with payloads. |
| [008](ADR-008-AI-Assisted-Development.md) | AI-Assisted Development | Accepted | Adopt GitHub Copilot Agent Mode with defined human review gates (5 checkpoints), responsible AI practices, prohibited uses, and tooling configuration. Documents AI's role in scaffolding, test generation, and documentation. |

---

## ADR Format

Each ADR follows this structure:

- **Status**: Accepted / Superseded / Deprecated
- **Context**: The problem space and constraints
- **Decision**: The chosen approach
- **Consequences**: Trade-offs and implications
- **Risks**: Known risks and mitigations
- **Alternatives Considered**: Other options evaluated
- **References**: Links to code, docs, or external resources

## Navigation

- [Architecture Overview](../architecture.md)
- [Engineering Decisions](../others/engineering-decisions.md)
- [API Summary](../others/api-summary.md)
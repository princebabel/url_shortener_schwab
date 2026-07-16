# ADR-007: Logging and Correlation ID

## Status
Accepted

## Context
The URL Shortener service requires robust observability for debugging, monitoring, and tracing requests across the system. Without structured logging and request correlation, it is difficult to trace a single request through multiple layers (controller → service → repository), correlate logs across services, or debug production issues efficiently.

Key requirements:
- Every HTTP request must have a unique correlation ID for end-to-end traceability
- Correlation ID must be propagated via HTTP header (`X-Correlation-ID`) and SLF4J MDC
- Structured request/response logging with payload, query parameters, and client info
- Consistent log format across all layers with correlation ID in every log line
- Error responses must include correlation ID for client-side debugging

## Decision
We implement a two-layer logging and correlation strategy:

### 1. Correlation ID Filter (`CorrelationIdFilter`)
- **Order**: `@Order(Ordered.HIGHEST_PRECEDENCE)` — runs before all other filters
- **Header**: `X-Correlation-ID` (configurable constant `CORRELATION_ID_HEADER`)
- **MDC Key**: `correlationId` (configurable constant `CORRELATION_ID_MDC_KEY`)
- **Behavior**:
  - Extracts existing `X-Correlation-ID` from incoming request header
  - Generates new UUID v4 if header is missing or blank
  - Puts correlation ID into SLF4J MDC for automatic inclusion in all log lines
  - Adds correlation ID to response header for client visibility
  - Logs request start (`Request received: METHOD /path`) and completion (`Request completed: METHOD /path`)
  - Clears MDC in `finally` block to prevent cross-request leakage

### 2. Structured Request Logging (`LoggingConfig`)
- **Bean**: `CommonsRequestLoggingFilter` configured with:
  - `includeClientInfo=true` — logs remote address, session ID
  - `includeQueryString=true` — logs query parameters
  - `includePayload=true` — logs request body (max 2000 chars)
  - `includeHeaders=false` — headers excluded (sensitive data risk)
  - `afterMessagePrefix="REQUEST DATA: "` — distinct log prefix
- Logs at `DEBUG` level (controlled via `logging.level.org.springframework.web.filter.CommonsRequestLoggingFilter=DEBUG`)

### 3. Error Response Correlation
- `GlobalExceptionHandler` reads correlation ID from MDC via `CorrelationIdFilter.CORRELATION_ID_MDC_KEY`
- Includes `correlationId` in every `ErrorResponse` JSON body
- Clients can correlate their request with server logs using the response header or error body

### 4. Log Configuration (`application.yml`)
```yaml
logging:
  level:
    root: INFO
    com.schwab.urlshortener: INFO
```
- Package-level DEBUG can be enabled at runtime via `logging.level.com.schwab.urlshortener=DEBUG`
- No separate logback-spring.xml; relies on Spring Boot defaults with MDC pattern

## Consequences

### Positive
- **Full request traceability**: Every log line contains correlation ID via MDC pattern `%X{correlationId}`
- **Client-server correlation**: Clients send/receive `X-Correlation-ID`; appears in response header and error body
- **Structured request logging**: Payload, query string, client info captured automatically at DEBUG level
- **Zero-instrumentation logging**: Controllers/services log normally; MDC auto-injects correlation ID
- **Fail-safe**: Filter runs at highest precedence; MDC cleared in `finally` block prevents leakage
- **Standards-aligned**: Uses Spring's `OncePerRequestFilter`, `CommonsRequestLoggingFilter`, SLF4J MDC

### Negative
- **Payload logging at DEBUG only**: Production INFO logs won't show request bodies (intentional for security/volume)
- **Header size**: `X-Correlation-ID` adds ~36 chars to every response
- **MDC cleanup critical**: Missing `finally` block would leak correlation IDs across requests in thread pool

### Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| PII in payload logs | `includeHeaders=false`; payload logging at DEBUG only; max 2000 chars |
| MDC leakage | `finally { MDC.clear() }` in filter; `OncePerRequestFilter` guarantees single execution |
| Correlation ID spoofing | Server accepts client-provided ID but validates non-blank; generates UUID if missing |
| Log volume | Structured logging at INFO; verbose payload at DEBUG only |

## Alternatives Considered

| Alternative | Rejected Because |
|-------------|------------------|
| Spring Cloud Sleuth / Micrometer Tracing | Overkill for single-service; adds dependency; correlation ID filter is 50 lines |
| Custom logback-spring.xml with MDC pattern | Spring Boot default pattern supports `%X{correlationId}`; no custom config needed |
| Filter at lower precedence | Must run before controller logging; `HIGHEST_PRECEDENCE` ensures this |
| UUID in request attribute only | MDC enables automatic log injection without code changes in services |

## Implementation References
- `src/main/java/com/schwab/urlshortener/config/CorrelationIdFilter.java`
- `src/main/java/com/schwab/urlshortener/config/LoggingConfig.java`
- `src/main/java/com/schwab/urlshortener/exception/GlobalExceptionHandler.java` (line 87: `MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)`)
- `src/main/java/com/schwab/urlshortener/dto/ErrorResponse.java` (field `correlationId`)
- `src/main/resources/application.yml` (logging.level config)

## Related ADRs
- ADR-002: Layered Architecture (filter sits at framework edge, before controller layer)
- ADR-004: Global Exception Handling (error responses include correlation ID)
- ADR-006: OpenAPI Documentation (correlation ID header documented in OpenAPI config)

## Date
2026-07-16

## Authors
GitHub Copilot (AI-assisted)
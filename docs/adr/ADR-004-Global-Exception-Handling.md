# ADR-004: Global Exception Handling — Centralized @ControllerAdvice with Structured ErrorResponse

## Status
Accepted

## Context
The URL Shortener API must return consistent, machine-readable error responses across all 9 endpoints. Scattering try-catch blocks in controllers creates duplication, inconsistent error formats, and makes it impossible to enforce cross-cutting concerns like correlation ID propagation, structured logging, and security-sensitive information filtering.

## Decision
We implement **centralized exception handling** via a single `@ControllerAdvice` class (`GlobalExceptionHandler`) that:
1. Catches all exceptions thrown from the controller layer
2. Maps each exception type to an appropriate HTTP status code
3. Returns a standardized `ErrorResponse` DTO with correlation ID
4. Logs structured logging
5. Never leaks stack traces or internal implementation details to clients

### Exception Hierarchy and HTTP Mapping

| Exception | HTTP Status | Error Code | When Thrown |
|-----------|-------------|------------|-------------|
| `MethodArgumentNotValidException` | 400 Bad Request | `VALIDATION_ERROR` | `@Valid` DTO validation failure |
| `ConstraintViolationException` | 400 Bad Request | `VALIDATION_ERROR` | Path/query param validation failure |
| `DuplicateAliasException` | 409 Conflict | `DUPLICATE_ALIAS` | Custom alias already exists |
| `InvalidUrlException` | 400 Bad Request | `INVALID_URL` | URL format/scheme validation failure |
| `UrlNotFoundException` | 404 Not Found | `URL_NOT_FOUND` | Short code/alias not found |
| `ExpiredUrlException` | 410 Gone | `URL_EXPIRED` | Short URL TTL exceeded |
| `DatabaseException` | 500 Internal Server Error | `DATABASE_ERROR` | Data access layer failure |
| `ShortCodeGenerationException` | 500 Internal Server Error | `GENERATION_ERROR` | Collision after max retries |
| `Exception` (catch-all) | 500 Internal Server Error | `INTERNAL_ERROR` | Any unhandled exception |

### `ErrorResponse` DTO Structure
```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private String code;
    private String message;
    private String path;
    private String correlationId;  // Critical for tracing
    private Map<String, String> validationErrors;  // Field-level details for 400
}
```

### `GlobalExceptionHandler` Implementation Pattern
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));
        
        ErrorResponse response = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .code("VALIDATION_ERROR")
            .message("Request validation failed")
            .path(request.getRequestURI())
            .correlationId(MDC.get("correlationId"))
            .validationErrors(fieldErrors)
            .build();
        
        log.warn("Validation failed: {} - {}", request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(DuplicateAliasException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateAlias(DuplicateAliasException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "DUPLICATE_ALIAS", ex.getMessage(), request);
    }

    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UrlNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "URL_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(ExpiredUrlException.class)
    public ResponseEntity<ErrorResponse> handleExpired(ExpiredUrlException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.GONE, "URL_EXPIRED", ex.getMessage(), request);
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ErrorResponse> handleDatabase(DatabaseException ex, HttpServletRequest request) {
        log.error("Database error: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR", 
            "A database error occurred. Please try again later.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
            "An unexpected error occurred. Please contact support.", request);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String code, String message, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .code(code)
            .message(message)
            .path(request.getRequestURI())
            .correlationId(MDC.get("correlationId"))
            .build();
        return ResponseEntity.status(status).body(response);
    }
}
```

## Rationale
1. **Single Source of Truth**: All error formatting logic lives in one class. Changing the error envelope (e.g., adding `traceId` for distributed tracing) requires one modification.
2. **Correlation ID Propagation**: Every error response includes the `correlationId` from MDC (populated by `CorrelationIdFilter`, see ADR-007), enabling end-to-end request tracing from client → logs → error response.
3. **Security by Default**: The catch-all handler returns a generic message ("An unexpected error occurred") without stack traces or internal details. Only known, safe exceptions (`DuplicateAliasException`, `UrlNotFoundException`) expose their specific messages.
4. **Structured Validation Errors**: `MethodArgumentNotValidException` handler extracts field-level errors into a `Map<String, String>` for client-side form validation rendering.
5. **Observability**: Each handler logs at appropriate level (WARN for client errors, ERROR for server errors) with correlation ID, enabling log aggregation queries like `correlationId:"abc-123" AND level:ERROR`.
6. **OpenAPI Documentation**: springdoc generates error response schemas from `@ApiResponse` annotations on controller methods, referencing the `ErrorResponse` schema.

## Consequences
### Positive
- Consistent error format across all 9 endpoints
- Correlation ID in every error response (critical for debugging)
- No stack traces leaked to clients
- Validation errors include field-level detail
- Easy to add new exception types with specific handling
- Logging standardized with correlation ID

### Negative
- **Single Point of Change**: Bug in `GlobalExceptionHandler` affects all endpoints
- **Exception Proliferation**: Temptation to create many specific exception classes (mitigated by reusing standard exceptions where possible)
- **Testing Overhead**: Must test each exception mapping path

## Mitigations
- **Comprehensive Tests**: `@WebMvcTest` for `GlobalExceptionHandler` covering all exception types (see ADR-008 for test strategy)
- **Exception Taxonomy**: Document when to create new exception vs. reuse existing (business rule violation → custom exception; infrastructure failure → `DatabaseException`)
- **Chaos Testing**: Periodically verify catch-all handler works by injecting failures

## Exception Design Guidelines
1. **Custom Exceptions** for business rule violations that clients can act on (`DuplicateAliasException`, `ExpiredUrlException`)
2. **Standard Exceptions** for infrastructure failures (`DatabaseException` wraps `DataAccessException`)
3. **No Checked Exceptions** in service/repository layers — all exceptions are unchecked (extend `RuntimeException`) to avoid `throws` pollution
4. **Immutable Messages**: Exception messages are constructed at throw site, not modified in handler
5. **Context Preservation**: Custom exceptions carry relevant context (e.g., `DuplicateAliasException` stores the conflicting alias)

## Related Decisions
- ADR-002: Layered Architecture (exceptions cross layer boundaries: service → controller → handler)
- ADR-003: DTO Pattern (`ErrorResponse` is a DTO, not an entity)
- ADR-005: Validation Strategy (validation exceptions handled here)
- ADR-007: Logging and Correlation ID (MDC correlation ID injected into every error response)

## References
- `src/main/java/com/schwab/urlshortener/exception/GlobalExceptionHandler.java`
- `src/main/java/com/schwab/urlshortener/exception/ErrorResponse.java`
- `src/main/java/com/schwab/urlshortener/exception/DuplicateAliasException.java`
- `src/main/java/com/schwab/urlshortener/exception/UrlNotFoundException.java`
- `src/main/java/com/schwab/urlshortener/exception/ExpiredUrlException.java`
- `src/main/java/com/schwab/urlshortener/exception/DatabaseException.java`
- `src/main/java/com/schwab/urlshortener/exception/InvalidUrlException.java`
- `src/main/java/com/schwab/urlshortener/exception/ShortCodeGenerationException.java`
- Spring Documentation: [Error Handling](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/advice.html)
- RFC 7807: [Problem Details for HTTP APIs](https://datatracker.ietf.org/doc/html/rfc7807) (inspiration for `ErrorResponse` structure)
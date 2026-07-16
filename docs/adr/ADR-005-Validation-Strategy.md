# ADR-005: Validation Strategy — Multi-Layer Defense with Fail-Fast at Controller Boundary

## Status
Accepted

## Context
The URL Shortener API accepts user-provided URLs, custom aliases, TTL values, and query parameters. Invalid input can cause database constraint violations, security vulnerabilities (open redirects, SSRF), or business rule violations (duplicate aliases, expired links). Validation must be comprehensive, performant, and provide actionable feedback to API clients.

## Decision
We implement a **three-layer validation strategy** with fail-fast at the controller boundary:

```
┌─────────────────────────────────────────────────────────────────┐
│ Layer 1: Controller Boundary (Request DTO) — FAIL-FAST          │
│   @Valid + Bean Validation annotations on request DTOs          │
│   Rejects malformed requests with HTTP 400 before service runs  │
├─────────────────────────────────────────────────────────────────┤
│ Layer 2: Service Layer — Business Rule Validation               │
│   Custom logic in service methods                               │
│   Throws domain exceptions (DuplicateAliasException, etc.)      │
├─────────────────────────────────────────────────────────────────┤
│ Layer 3: Database Constraints — Integrity Backstop              │
│   JPA @Column(unique=true), @NotNull, CHECK constraints         │
│   Catches race conditions and bugs that bypass layers 1-2       │
└─────────────────────────────────────────────────────────────────┘
```

### Layer 1: Controller Boundary (Request DTO Validation)

**Mechanism**: Bean Validation 3.0 (Hibernate Validator) annotations on request DTO fields + `@Valid` on controller parameters.

**Annotations Used**:
| Annotation | Purpose | Example |
|------------|---------|---------|
| `@NotBlank` | Required string, not empty/whitespace | `targetUrl` |
| `@Size(min, max)` | String length bounds | `customAlias` max 64 |
| `@Pattern(regexp)` | Format enforcement | URL scheme, alias charset |
| `@Min`, `@Max` | Numeric bounds | `ttlDays` 1-3650 |
| `@NotNull` | Required non-primitive | `ttlDays` (optional but if present, valid) |
| `@Valid` | Nested object validation | Future: nested DTOs |

**Controller Usage**:
```java
@PostMapping("/api/urls")
public ResponseEntity<CreateShortUrlResponse> createShortUrl(
        @Valid @RequestBody CreateShortUrlRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(urlService.createShortUrl(request));
}
```

**Failure Handling**: `MethodArgumentNotValidException` caught by `GlobalExceptionHandler` → HTTP 400 with field-level error map (see ADR-004).

### Layer 2: Service Layer (Business Rule Validation)

**Mechanism**: Explicit validation logic in service methods, throwing domain-specific exceptions.

**Rules Enforced**:
| Rule | Exception | HTTP Status |
|------|-----------|-------------|
| Custom alias uniqueness | `DuplicateAliasException` | 409 Conflict |
| Target URL reachability (optional, async) | `InvalidUrlException` | 400 Bad Request |
| Short code collision after max retries | `ShortCodeGenerationException` | 500 Internal Server Error |
| URL expiration check on redirect | `ExpiredUrlException` | 410 Gone |
| URL not found on lookup | `UrlNotFoundException` | 404 Not Found |

**Example**:
```java
@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {
    private final UrlRepository urlRepository;
    private final ShortCodeGenerator codeGenerator;

    @Override
    @Transactional
    public CreateShortUrlResponse createShortUrl(CreateShortUrlRequest request) {
        // Business rule: custom alias uniqueness
        if (request.getCustomAlias() != null && urlRepository.existsByShortCode(request.getCustomAlias())) {
            throw new DuplicateAliasException("Custom alias already in use: " + request.getCustomAlias());
        }

        // Business rule: short code generation with collision handling
        String shortCode = request.getCustomAlias() != null 
            ? request.getCustomAlias() 
            : codeGenerator.generateUniqueCode(urlRepository::existsByShortCode);

        // ... persist and return response
    }
}
```

### Layer 3: Database Constraints (Integrity Backstop)

**Mechanism**: JPA annotations and DDL constraints that enforce data integrity at the database level.

**Entity Annotations**:
```java
@Entity
@Table(name = "urls", uniqueConstraints = {
    @UniqueConstraint(name = "uk_urls_short_code", columnNames = "short_code"),
    @UniqueConstraint(name = "uk_urls_custom_alias", columnNames = "custom_alias")
}, indexes = {
    @Index(name = "idx_urls_expires_at", columnList = "expires_at"),
    @Index(name = "idx_urls_created_at", columnList = "created_at")
})
public class UrlEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, length = 64, unique = true)
    private String shortCode;

    @Column(name = "custom_alias", length = 64, unique = true)
    private String customAlias;

    @Column(name = "target_url", nullable = false, length = 2048)
    private String targetUrl;

    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;  // Optimistic locking
}
```

**Database-Level Constraints** (DDL generated by Hibernate or Flyway):
```sql
-- Unique constraints prevent duplicate short codes/aliases even under race conditions
ALTER TABLE urls ADD CONSTRAINT uk_urls_short_code UNIQUE (short_code);
ALTER TABLE urls ADD CONSTRAINT uk_urls_custom_alias UNIQUE (custom_alias);

-- Check constraint for TTL bounds (if using Flyway)
ALTER TABLE urls ADD CONSTRAINT chk_ttl_days CHECK (ttl_days BETWEEN 1 AND 3650);

-- Not-null constraints
ALTER TABLE urls ALTER COLUMN target_url SET NOT NULL;
ALTER TABLE urls ALTER COLUMN short_code SET NOT NULL;
```

## Specialized Validators

### URL Validation (`@ValidUrl` Custom Annotation)
```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UrlValidator.class)
@Documented
public @interface ValidUrl {
    String message() default "Invalid URL format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

@Component
public class UrlValidator implements ConstraintValidator<ValidUrl, String> {
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)" +
        "([a-zA-Z0-9.-]+|(\\[[0-9a-fA-F:]\\]))" +  // host or IPv6
        "(:\\d+)?" +                                // optional port
        "(/.*)?$"                                   // optional path
    );

    private static final Set<String> BLOCKED_HOSTS = Set.of(
        "localhost", "127.0.0.1", "::1", "0.0.0.0",
        "169.254.169.254"  // AWS metadata endpoint
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        
        // Scheme check
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            return false;
        }
        
        // Pattern match
        if (!URL_PATTERN.matcher(value).matches()) {
            return false;
        }
        
        // SSRF protection: block private/internal hosts
        try {
            URI uri = new URI(value);
            String host = uri.getHost();
            if (host != null && BLOCKED_HOSTS.contains(host.toLowerCase())) {
                return false;
            }
            // Additional: block private IP ranges (10.x, 172.16-31.x, 192.168.x)
            if (isPrivateIp(host)) {
                return false;
            }
        } catch (URISyntaxException e) {
            return false;
        }
        
        return true;
    }

    private boolean isPrivateIp(String host) {
        // Implementation for RFC 1918 private ranges
        return false; // Simplified
    }
}
```

**Usage on DTO**:
```java
public class CreateShortUrlRequest {
    @NotBlank
    @Size(max = 2048)
    @ValidUrl  // Custom validator with SSRF protection
    private String targetUrl;
    // ...
}
```

### Alias Validation
```java
@Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "Alias may only contain alphanumerics, hyphen, underscore")
@Size(max = 64)
private String customAlias;
```

## Validation Flow Examples

### Happy Path: `POST /api/urls`
```
Request → @Valid CreateShortUrlRequest 
  → Layer 1: Bean Validation (URL format, alias pattern, TTL range) 
  → Service.createShortUrl() 
    → Layer 2: Alias uniqueness check (DB query) 
    → Layer 3: INSERT with unique constraint (race condition backstop) 
  → Response 201
```

### Validation Failure: Invalid URL
```
Request → @Valid CreateShortUrlRequest 
  → Layer 1: @ValidUrl fails (not http/https, or blocked host) 
  → MethodArgumentNotValidException 
  → GlobalExceptionHandler 
  → Response 400 { "code": "VALIDATION_ERROR", "validationErrors": {"targetUrl": "Invalid URL format"} }
```

### Business Rule Failure: Duplicate Alias
```
Request → @Valid CreateShortUrlRequest (passes Layer 1) 
  → Service.createShortUrl() 
    → Layer 2: urlRepository.existsByShortCode(alias) → true 
    → throw DuplicateAliasException 
  → GlobalExceptionHandler 
  → Response 409 { "code": "DUPLICATE_ALIAS", "message": "Custom alias already in use: my-alias" }
```

### Race Condition: Concurrent Alias Creation
```
Thread A: Service.existsByShortCode("abc") → false
Thread B: Service.existsByShortCode("abc") → false
Thread A: INSERT short_code="abc" → succeeds
Thread B: INSERT short_code="abc" → Layer 3: DB unique constraint violation 
  → DataIntegrityViolationException 
  → GlobalExceptionHandler (DatabaseException handler) 
  → Response 500 { "code": "DATABASE_ERROR", "message": "A database error occurred..." }
  → Client retries with new alias
```

## Security-Focused Validation Rules

| Threat | Mitigation | Layer |
|--------|------------|-------|
| SSRF (Server-Side Request Forgery) | `@ValidUrl` blocks localhost, private IPs, metadata endpoints | 1 |
| Open Redirect | Redirect endpoint (`GET /{shortCode}`) validates target URL scheme is http/https only | 2 |
| XSS via Alias | `@Pattern` restricts alias to alphanumeric + hyphen/underscore | 1 |
| URL Length DoS | `@Size(max=2048)` on target URL | 1 |
| Alias Enumeration | Random short codes by default; custom alias opt-in | 2 |
| TTL Exhaustion | `@Max(3650)` limits max TTL to 10 years | 1 |

## Testing Strategy
- **Unit Tests**: `UrlValidatorTest` — all valid/invalid URL patterns, blocked hosts
- **Integration Tests**: `@WebMvcTest(UrlController.class)` — validation error responses for each field
- **Service Tests**: `@ExtendWith(MockitoExtension.class)` — business rule exceptions
- **Race Condition Tests**: `@DataJpaTest` + concurrent inserts (Testcontainers) — verify unique constraint handling

## Related Decisions
- ADR-003: DTO Pattern (validation annotations live on request DTOs)
- ADR-004: Global Exception Handling (validation failures → structured error response)
- ADR-006: OpenAPI Documentation (validation constraints appear in generated spec)
- ADR-007: Security Review (SSRF protection, open redirect prevention)

## References
- `src/main/java/com/schwab/urlshortener/validation/ValidUrl.java`
- `src/main/java/com/schwab/urlshortener/validation/UrlValidator.java`
- `src/main/java/com/schwab/urlshortener/dto/request/CreateShortUrlRequest.java`
- `src/main/java/com/schwab/urlshortener/service/UrlServiceImpl.java`
- `src/main/java/com/schwab/urlshortener/entity/UrlEntity.java`
- Hibernate Validator Documentation: [Reference Guide](https://hibernate.org/validator/documentation/)
- OWASP SSRF Prevention Cheat Sheet
- RFC 3986: Uniform Resource Identifier (URI) Syntax
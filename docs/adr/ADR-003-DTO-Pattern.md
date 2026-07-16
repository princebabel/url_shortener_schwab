# ADR-003: DTO Pattern — Request/Response Contracts with Validation, No Entity Exposure

## Status
Accepted

## Context
The URL Shortener API must present stable, versioned contracts to clients while keeping internal domain models free to evolve. Exposing JPA entities directly in HTTP responses creates tight coupling between the database schema and the public API, leaks implementation details (lazy-loading proxies, bidirectional relationships, audit fields), and prevents independent evolution of the data model and the API contract.

## Decision
We adopt the **DTO (Data Transfer Object) Pattern** as a strict boundary: **controllers accept and return only DTOs; entities never cross the controller boundary**.

### Package Structure
```
com.schwab.urlshortener.dto
├── request/
│   ├── CreateShortUrlRequest.java      // POST /api/urls
│   └── CreateUrlRequest.java           // Alternative/create variant
├── response/
│   ├── CreateShortUrlResponse.java     // 201 response
│   ├── UrlResponse.java                // GET /api/urls/{shortCode}
│   ├── UrlAnalyticsResponse.java       // GET /api/urls/{shortCode}/analytics
│   ├── DashboardSummaryResponse.java   // GET /api/urls/dashboard/summary
│   ├── BaseResponse.java               // Common envelope
│   └── ErrorResponse.java              // Error envelope (see ADR-004)
└── (package-private mappers in mapper/)
```

### Request DTO Example: `CreateShortUrlRequest`
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateShortUrlRequest {
    @NotBlank(message = "Target URL is required")
    @Size(max = 2048, message = "Target URL must not exceed 2048 characters")
    @Pattern(regexp = "^(https?://).+", message = "Target URL must start with http:// or https://")
    private String targetUrl;

    @Size(max = 64, message = "Custom alias must not exceed 64 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "Alias may only contain alphanumerics, hyphen, underscore")
    private String customAlias;

    @Min(value = 1, message = "TTL must be at least 1 day")
    @Max(value = 3650, message = "TTL must not exceed 3650 days (10 years)")
    private Integer ttlDays;
}
```

### Response DTO Example: `CreateShortUrlResponse`
```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateShortUrlResponse {
    private String shortCode;
    private String shortUrl;
    private String targetUrl;
    private String customAlias;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Long clickCount;
}
```

### Mapping Strategy
- **Controller → Service**: Controller maps request DTO → service input (record or DTO) manually or via MapStruct
- **Service → Repository**: Service works with entities; repository returns entities
- **Service → Controller**: Service returns entity or domain object; controller maps to response DTO
- **Mappers**: Package-private `UrlMapper` (MapStruct-generated) handles entity↔DTO conversion

## Rationale
1. **Contract Stability**: DTOs define the public API contract. Database schema changes (column rename, new audit field, relationship change) require only mapper updates, not client-breaking API changes.
2. **Validation at the Boundary**: Bean Validation annotations (`@NotBlank`, `@Size`, `@Pattern`, `@Min`, `@Max`) live on request DTOs. Invalid requests are rejected at `@Valid` in the controller before reaching the service layer (fail-fast).
3. **Security by Default**: Entities contain fields never meant for external consumption (`id`, `createdBy`, `updatedAt`, `version` for optimistic locking, internal flags). DTOs expose only intentional fields.
4. **Serialization Control**: DTOs use Jackson annotations (`@JsonProperty`, `@JsonFormat`, `@JsonIgnore`) for precise JSON shape. Entities use JPA annotations; mixing both creates confusion.
5. **Versioning Ready**: Future `CreateShortUrlRequestV2` can coexist with v1; controller routes to appropriate service method.
6. **Documentation Accuracy**: springdoc-openapi generates schemas from DTO classes, producing accurate OpenAPI specs without `@Schema` pollution on entities.

## Consequences
### Positive
- API evolution independent of schema evolution
- Clear validation location (request DTOs)
- No lazy-loading exceptions in serialization (entities never serialized)
- OpenAPI spec reflects actual request/response shapes
- Security: no accidental PII/internal field leakage

### Negative
- **Mapping Boilerplate**: Every endpoint needs request→domain and domain→response mapping
- **Duplication**: Field names and types repeated across entity and DTO
- **Drift Risk**: Mapper can become stale if entity/DTO changes aren't mirrored

## Mitigations
- **MapStruct**: Compile-time mapper generation eliminates runtime reflection and reduces boilerplate. Configured in `pom.xml`:
  ```xml
  <dependency>
      <groupId>org.mapstruct</groupId>
      <artifactId>mapstruct</artifactId>
      <version>1.6.3</version>
  </dependency>
  <dependency>
      <groupId>org.mapstruct</groupId>
      <artifactId>mapstruct-processor</artifactId>
      <version>1.6.3</version>
      <scope>provided</scope>
  </dependency>
  ```
- **Mapper Tests**: Unit tests verify mapper round-trip correctness (`entity → dto → entity` equivalence for non-generated fields)
- **Code Review Rule**: "Every entity field change requires corresponding DTO/mapper review"

## Validation Strategy (Detail)
| Layer | Mechanism | Scope |
|-------|-----------|-------|
| Controller (Request DTO) | `@Valid` + Bean Validation annotations | Syntax, format, length, required fields |
| Service | Custom business rule validation | Uniqueness, referential integrity, state transitions |
| Repository/Database | JPA constraints (`@Column(unique=true)`, `@NotNull`) | Data integrity backstop |

**Fail-Fast Principle**: Request DTO validation rejects malformed requests at the controller boundary with HTTP 400 before any service logic executes. This is enforced by `@Valid` on controller method parameters and `GlobalExceptionHandler` mapping `MethodArgumentNotValidException` to structured `ErrorResponse` (see ADR-004).

## Consequences for Specific Endpoints
| Endpoint | Request DTO | Response DTO | Validation Highlights |
|----------|-------------|--------------|----------------------|
| `POST /api/urls` | `CreateShortUrlRequest` | `CreateShortUrlResponse` | URL format, alias pattern, TTL range |
| `GET /api/urls/{shortCode}` | — | `UrlResponse` | Path variable `@Pattern` in controller |
| `GET /api/urls/{shortCode}/analytics` | — | `UrlAnalyticsResponse` | — |
| `GET /api/urls` | Query params (page, size, sort) | `Page<UrlResponse>` | `@ParameterObject` for pagination |
| `GET /api/urls/search` | Query param `q` | `List<UrlResponse>` | `@Size(min=1, max=100)` on query |
| `GET /api/urls/dashboard/summary` | — | `DashboardSummaryResponse` | — |
| `GET /api/urls/dashboard/top` | Query param `limit` | `List<UrlResponse>` | `@Min(1) @Max(100)` |
| `GET /api/urls/dashboard/recent` | Query param `hours` | `List<UrlResponse>` | `@Min(1) @Max(168)` |

## Alternatives Considered
| Approach | Rejected Because |
|----------|------------------|
| Expose entities directly (`@JsonIgnore` on sensitive fields) | Leaks JPA proxies, lazy-loading failures, schema coupling, no validation boundary |
| Single DTO for request and response | Different validation needs (request: required fields; response: computed fields like `shortUrl`) |
| Manual mapping in controllers | Error-prone, verbose, inconsistent; MapStruct provides compile-time safety |
| Record-based DTOs (Java 16+) | Lombok `@Data`/`@Builder` preferred for consistency with existing codebase; records don't work well with MapStruct setters |

## Related Decisions
- ADR-002: Layered Architecture (DTOs are the controller↔service contract)
- ADR-004: Global Exception Handling (validation failures → structured error DTO)
- ADR-005: Validation Strategy (Bean Validation on DTOs = first line of defense)
- ADR-006: OpenAPI Documentation (springdoc reads DTO annotations for spec generation)

## References
- `src/main/java/com/schwab/urlshortener/dto/request/CreateShortUrlRequest.java`
- `src/main/java/com/schwab/urlshortener/dto/response/CreateShortUrlResponse.java`
- `src/main/java/com/schwab/urlshortener/dto/response/ErrorResponse.java`
- `src/main/java/com/schwab/urlshortener/mapper/UrlMapper.java` (MapStruct interface)
- `src/main/java/com/schwab/urlshortener/controller/UrlController.java` — `@Valid` usage
- MapStruct Documentation: [Reference Guide](https://mapstruct.org/documentation/stable/reference/html/)
- Spring Validation Documentation: [Bean Validation](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html)
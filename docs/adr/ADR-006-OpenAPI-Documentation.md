# ADR-006: OpenAPI Documentation — springdoc-openapi with Contract-First Discipline

## Status
Accepted

## Context
The URL Shortener API serves external consumers (frontend applications, third-party integrators, mobile clients) who need accurate, up-to-date API documentation. Manual documentation maintenance is error-prone and diverges from implementation. The team needs a solution that generates OpenAPI 3.1 specifications directly from code, supports logical endpoint grouping, and integrates with Swagger UI for interactive exploration.

## Decision
We adopt **springdoc-openapi v2** (Spring Boot 3 compatible) with a **code-first, contract-disciplined** approach:

1. **Annotations on Controllers/DTOs** define the contract (not a separate YAML file)
2. **Custom `OpenApiConfig`** configures metadata, security schemes, and tag groups
3. **Generated spec** served at `/v3/api-docs` and `/v3/api-docs.yaml`
4. **Swagger UI** available at `/swagger-ui.html` for interactive testing
5. **Contract Discipline**: Annotations are treated as source of truth; breaking changes require ADR review

### OpenAPI Configuration (`OpenApiConfig.java`)
```java
@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("URL Shortener API")
                .version("v1")
                .description("""
                    A high-performance URL shortening service with analytics.
                    
                    ## Features
                    - Create short URLs with custom aliases
                    - Configurable TTL (1 day to 10 years)
                    - Click analytics and dashboard summaries
                    - Health checks for orchestration
                    
                    ## Authentication
                    Currently unauthenticated. Future versions will support API keys.
                    """)
                .contact(new Contact()
                    .name("Platform Engineering")
                    .email("platform@schwab.com")
                    .url("https://github.com/schwab/url-shortener"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local Development"),
                new Server().url("https://api.staging.schwab.com").description("Staging"),
                new Server().url("https://api.schwab.com").description("Production")
            ))
            .tags(List.of(
                new Tag().name("URLs").description("Core URL shortening operations"),
                new Tag().name("Analytics").description("Click tracking and dashboard metrics"),
                new Tag().name("Health").description("System health and readiness probes")
            ))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Future: JWT Bearer token authentication"))
            )
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
```

### Controller Annotations (Example: `UrlController.java`)
```java
@RestController
@RequestMapping("/api/urls")
@Tag(name = "URLs", description = "Core URL shortening operations")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @Operation(
        summary = "Create a short URL",
        description = "Creates a shortened URL with optional custom alias and TTL. Returns the short code and full short URL.",
        operationId = "createShortUrl",
        responses = {
            @ApiResponse(responseCode = "201", description = "Short URL created",
                content = @Content(schema = @Schema(implementation = CreateShortUrlResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Custom alias already in use",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @PostMapping
    public ResponseEntity<CreateShortUrlResponse> createShortUrl(
            @Valid @RequestBody CreateShortUrlRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(urlService.createShortUrl(request));
    }

    @Operation(
        summary = "Redirect to target URL",
        description = "Redirects to the original target URL. Increments click counter. Returns 404 if not found, 410 if expired.",
        operationId = "redirectToTarget",
        responses = {
            @ApiResponse(responseCode = "302", description = "Redirect to target URL"),
            @ApiResponse(responseCode = "404", description = "Short code not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "410", description = "URL has expired",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectToTarget(
            @Parameter(description = "Short code or custom alias", example = "abc123")
            @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]{1,64}$") String shortCode,
            HttpServletResponse response) {
        String targetUrl = urlService.getTargetUrl(shortCode);
        response.sendRedirect(targetUrl);
        return ResponseEntity.status(HttpStatus.FOUND).build();
    }

    @Operation(
        summary = "Get URL analytics",
        description = "Returns click count, creation time, expiration, and recent click timestamps.",
        operationId = "getUrlAnalytics",
        responses = {
            @ApiResponse(responseCode = "200", description = "Analytics data",
                content = @Content(schema = @Schema(implementation = UrlAnalyticsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @GetMapping("/{shortCode}/analytics")
    public ResponseEntity<UrlAnalyticsResponse> getAnalytics(
            @Parameter(description = "Short code or custom alias", example = "abc123")
            @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]{1,64}$") String shortCode) {
        return ResponseEntity.ok(urlService.getAnalytics(shortCode));
    }

    @Operation(
        summary = "List all URLs with pagination",
        description = "Returns paginated list of URLs sorted by creation date (newest first).",
        operationId = "listUrls",
        responses = {
            @ApiResponse(responseCode = "200", description = "Paginated URL list",
                content = @Content(schema = @Schema(implementation = Page.class)))
        }
    )
    @GetMapping
    public ResponseEntity<Page<UrlResponse>> listUrls(
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(urlService.listUrls(pageable));
    }

    @Operation(
        summary = "Search URLs by target URL substring",
        description = "Returns URLs whose target URL contains the query string (case-insensitive).",
        operationId = "searchUrls",
        responses = {
            @ApiResponse(responseCode = "200", description = "Matching URLs",
                content = @Content(schema = @Schema(implementation = UrlResponse.class)))
        }
    )
    @GetMapping("/search")
    public ResponseEntity<List<UrlResponse>> searchUrls(
            @Parameter(description = "Search query (min 1 char, max 100)", example = "github")
            @RequestParam @Size(min = 1, max = 100) String q) {
        return ResponseEntity.ok(urlService.searchUrls(q));
    }
}
```

### Dashboard Controller (`UrlController` continued)
```java
@Tag(name = "Analytics", description = "Dashboard and aggregated metrics")
@GetMapping("/dashboard/summary")
@Operation(summary = "Dashboard summary", operationId = "getDashboardSummary")
public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() { ... }

@GetMapping("/dashboard/top")
@Operation(summary = "Top URLs by clicks", operationId = "getTopUrls")
public ResponseEntity<List<UrlResponse>> getTopUrls(
        @Parameter(description = "Number of results (1-100)", example = "10")
        @RequestParam @Min(1) @Max(100) Integer limit) { ... }

@GetMapping("/dashboard/recent")
@Operation(summary = "Recently created URLs", operationId = "getRecentUrls")
public ResponseEntity<List<UrlResponse>> getRecentUrls(
        @Parameter(description = "Hours to look back (1-168)", example = "24")
        @RequestParam @Min(1) @Max(168) Integer hours) { ... }
```

### Health Controller (`UrlShortenerController.java`)
```java
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "System health and readiness probes")
public class UrlShortenerController {

    @Operation(summary = "Health check", operationId = "healthCheck",
        responses = @ApiResponse(responseCode = "200", description = "Service healthy"))
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "url-shortener"));
    }
}
```

## Generated OpenAPI Structure

### Tag Groups (3)
| Tag | Endpoints | Description |
|-----|-----------|-------------|
| **URLs** | `POST /api/urls`, `GET /api/urls/{shortCode}`, `GET /api/urls/{shortCode}/analytics`, `GET /api/urls`, `GET /api/urls/search` | Core CRUD and lookup |
| **Analytics** | `GET /api/urls/dashboard/summary`, `GET /api/urls/dashboard/top`, `GET /api/urls/dashboard/recent` | Aggregated metrics |
| **Health** | `GET /api/v1/health` | Infrastructure probes |

### Key Schemas (from DTOs)
- `CreateShortUrlRequest` — input validation constraints documented
- `CreateShortUrlResponse` — includes `shortUrl` (computed), `expiresAt`, `clickCount`
- `UrlResponse` — list/search item shape
- `UrlAnalyticsResponse` — detailed analytics with `recentClicks: List<Instant>`
- `DashboardSummaryResponse` — `totalUrls`, `totalClicks`, `activeUrls`, `expiredUrls`
- `ErrorResponse` — standardized error envelope (see ADR-004)
- `Page<UrlResponse>` — Spring Data pagination envelope

### Security Scheme
- **bearerAuth** (HTTP Bearer, JWT format) — defined but not enforced (future-proofing)

## Contract-First Discipline

While we use code-first generation, we enforce **contract discipline**:

1. **Breaking Change Detection**: CI runs `openapi-diff` against `main` branch spec; fails on breaking changes (removed endpoints, required fields added, response type changes)
2. **Versioning**: Major version in URL path (`/api/v1/`); minor versions via header negotiation (future)
3. **Deprecation Policy**: `@Deprecated` on operations + `deprecated: true` in OpenAPI; 90-day sunset
4. **Schema Evolution Rules**:
   - ✅ Add optional fields to response
   - ✅ Add new endpoints
   - ✅ Add new enum values (with `x-extensible: true`)
   - ❌ Remove fields/endpoints
   - ❌ Change field types
   - ❌ Make optional fields required

## Consequences
### Positive
- **Always Current**: Documentation matches deployed code (generated at build time)
- **Interactive Testing**: Swagger UI enables manual testing without Postman/curl
- **Client Generation**: Teams can generate TypeScript/Java/Go clients from `/v3/api-docs.yaml`
- **Governance**: Breaking change detection in CI prevents accidental contract breaks
- **Standards Compliance**: OpenAPI 3.1 + JSON Schema 2020-12

### Negative
- **Annotation Verbosity**: Controllers carry significant annotation overhead
- **Runtime Coupling**: Spec generation requires application context (not pure static analysis)
- **Over-Documentation Risk**: Temptation to over-annotate obvious things

## Mitigations
- **Lombok + Records**: Reduce boilerplate on DTOs so annotations stand out
- **Shared Annotations**: Common `@ApiResponse` definitions extracted to constants (future)
- **Review Checklist**: PR template includes "OpenAPI annotations updated?" checkbox

## Configuration (`application.yml`)
```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    operations-sorter: method
    tags-sorter: alpha
    display-request-duration: true
    try-it-out-enabled: true
  packages-to-scan: com.schwab.urlshortener.controller
  paths-to-match: /api/**
  default-consumes-media-type: application/json
  default-produces-media-type: application/json
```

## Testing the Contract
- **Contract Tests**: `spring-cloud-contract` tests generated from OpenAPI spec (planned)
- **Schema Validation**: CI validates generated spec against OpenAPI 3.1 meta-schema
- **Example Values**: `@Schema(example = "...")` on DTO fields for Swagger UI "Try it out"

## Related Decisions
- ADR-003: DTO Pattern (DTOs are the schema source)
- ADR-004: Global Exception Handling (`ErrorResponse` schema documented)
- ADR-005: Validation Strategy (constraints appear in spec via `@Schema`)

## References
- `src/main/java/com/schwab/urlshortener/config/OpenApiConfig.java`
- `src/main/java/com/schwab/urlshortener/controller/UrlController.java`
- `src/main/java/com/schwab/urlshortener/controller/UrlShortenerController.java`
- `src/main/resources/application.yml` (springdoc configuration)
- springdoc-openapi Documentation: [https://springdoc.org/](https://springdoc.org/)
- OpenAPI Specification 3.1: [https://spec.openapis.org/oas/v3.1.0](https://spec.openapis.org/oas/v3.1.0)
- Swagger UI: [https://swagger.io/tools/swagger-ui/](https://swagger.io/tools/swagger-ui/)